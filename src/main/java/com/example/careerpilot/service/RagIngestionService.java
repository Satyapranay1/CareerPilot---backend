package com.example.careerpilot.service;

import com.example.careerpilot.model.CompanyKnowledge;
import com.example.careerpilot.model.InterviewSession;
import com.example.careerpilot.repo.CompanyKnowledgeRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;

import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class RagIngestionService {


    // =========================================================
    // CHUNK CONFIGURATION
    // =========================================================

    private static final int COMPANY_CHUNK_SIZE = 1200;

    private static final int JD_CHUNK_SIZE = 1000;


    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final VectorStore vectorStore;

    private final CompanyKnowledgeRepository
            companyKnowledgeRepository;

    private final CompanyWebsiteService
            companyWebsiteService;


    // =========================================================
    // PREPARE COMPANY KNOWLEDGE
    // =========================================================

    @Transactional
    public CompanyKnowledge prepareCompany(
            String companyName,
            String website
    ) {

        /*
         * Website is optional.
         *
         * If no website is supplied,
         * we simply skip company RAG.
         */

        if (website == null
                || website.isBlank()) {

            return null;
        }


        // =====================================================
        // NORMALIZE WEBSITE
        // =====================================================

        String normalizedWebsite =
                companyWebsiteService
                        .normalizeWebsite(
                                website
                        );


        // =====================================================
        // CHECK COMPANY CACHE
        // =====================================================

        Optional<CompanyKnowledge> existing =
                companyKnowledgeRepository
                        .findByNormalizedWebsite(
                                normalizedWebsite
                        );


        // =====================================================
        // EXISTING COMPANY
        // =====================================================

        if (existing.isPresent()) {

            CompanyKnowledge company =
                    existing.get();


            /*
             * Already successfully indexed.
             *
             * DO NOT:
             *
             * fetch website
             * chunk website
             * generate embeddings
             *
             * Reuse existing vectors.
             */

            if (Boolean.TRUE.equals(
                    company.getIndexed()
            )) {

                return company;
            }


            /*
             * Company exists but indexing was
             * unsuccessful previously.
             *
             * For development we retry.
             *
             * Later we can add:
             *
             * last_index_attempt
             * index_status
             * last_index_error
             *
             * and implement retry cooldown.
             */

            indexCompany(company);


            return company;
        }


        // =====================================================
        // CREATE NEW COMPANY
        // =====================================================

        CompanyKnowledge company =
                CompanyKnowledge
                        .builder()

                        .companyName(
                                clean(companyName)
                        )

                        .website(
                                website.trim()
                        )

                        .normalizedWebsite(
                                normalizedWebsite
                        )

                        .indexed(false)

                        .build();


        company =
                companyKnowledgeRepository
                        .save(company);


        // =====================================================
        // ATTEMPT COMPANY RAG INDEXING
        // =====================================================

        /*
         * IMPORTANT:
         *
         * indexCompany() does not throw merely because
         * the website could not be fetched.
         *
         * Therefore:
         *
         * TCS → 403
         *
         * does NOT prevent interview creation.
         */

        indexCompany(company);


        return company;
    }


    // =========================================================
    // INDEX COMPANY WEBSITE
    // =========================================================

    private void indexCompany(
            CompanyKnowledge company
    ) {


        // =====================================================
        // EXTRACT WEBSITE
        // =====================================================

        String content =
                companyWebsiteService
                        .extractWebsiteText(
                                company.getWebsite()
                        );


        // =====================================================
        // WEBSITE UNAVAILABLE
        // =====================================================

        /*
         * Possible reasons:
         *
         * 403
         * 404
         * timeout
         * Cloudflare
         * anti-bot protection
         * network problem
         *
         * Company RAG is OPTIONAL.
         *
         * Keep indexed=false and continue.
         */

        if (content == null
                || content.isBlank()) {


            company.setIndexed(false);


            companyKnowledgeRepository
                    .save(company);


            System.out.println(
                    "Skipping company RAG for: "
                            + company.getCompanyName()
            );


            return;
        }


        // =====================================================
        // CONTENT HASH
        // =====================================================

        String contentHash =
                generateHash(
                        content
                );


        // =====================================================
        // CHUNK WEBSITE
        // =====================================================

        List<String> chunks =
                chunkText(
                        content,
                        COMPANY_CHUNK_SIZE
                );


        // =====================================================
        // CREATE SPRING AI DOCUMENTS
        // =====================================================

        List<Document> documents =
                new ArrayList<>();


        for (int i = 0;
             i < chunks.size();
             i++) {


            Map<String, Object> metadata =
                    new HashMap<>();


            /*
             * Used later during RAG retrieval.
             */

            metadata.put(
                    "scope",
                    "COMPANY"
            );


            metadata.put(
                    "companyKnowledgeId",
                    company
                            .getId()
                            .toString()
            );


            metadata.put(
                    "company",
                    safe(
                            company.getCompanyName()
                    )
            );


            metadata.put(
                    "sourceType",
                    "COMPANY_WEBSITE"
            );


            metadata.put(
                    "sourceUrl",
                    company.getNormalizedWebsite()
            );


            metadata.put(
                    "contentHash",
                    contentHash
            );


            metadata.put(
                    "chunkIndex",
                    i
            );


            Document document =
                    new Document(
                            chunks.get(i),
                            metadata
                    );


            documents.add(document);
        }


        // =====================================================
        // STORE EMBEDDINGS
        // =====================================================

        /*
         * Spring AI:
         *
         * Document
         *      ↓
         * nomic-embed-text
         *      ↓
         * embedding
         *      ↓
         * PGVector
         */

        if (!documents.isEmpty()) {

            vectorStore.add(
                    documents
            );
        }


        // =====================================================
        // MARK COMPANY AS INDEXED
        // =====================================================

        company.setContentHash(
                contentHash
        );


        company.setIndexed(
                true
        );


        company.setIndexedAt(
                LocalDateTime.now()
        );


        companyKnowledgeRepository
                .save(company);


        System.out.println(
                "Company RAG indexed successfully: "
                        + company.getCompanyName()
                        + " | chunks="
                        + documents.size()
        );
    }


    // =========================================================
    // INGEST JOB DESCRIPTION
    // =========================================================

    public String ingestJobDescription(
            InterviewSession session
    ) {


        String jobDescription =
                session.getJobDescription();


        // =====================================================
        // NO JD
        // =====================================================

        if (jobDescription == null
                || jobDescription.isBlank()) {

            return null;
        }


        // =====================================================
        // JD HASH
        // =====================================================

        String contentHash =
                generateHash(
                        jobDescription
                );


        // =====================================================
        // CHUNK JD
        // =====================================================

        List<String> chunks =
                chunkText(
                        jobDescription,
                        JD_CHUNK_SIZE
                );


        // =====================================================
        // CREATE DOCUMENTS
        // =====================================================

        List<Document> documents =
                new ArrayList<>();


        for (int i = 0;
             i < chunks.size();
             i++) {


            Map<String, Object> metadata =
                    new HashMap<>();


            /*
             * This makes JD vectors specific
             * to the current interview.
             */

            metadata.put(
                    "scope",
                    "INTERVIEW"
            );


            metadata.put(
                    "sessionId",
                    session
                            .getId()
                            .toString()
            );


            metadata.put(
                    "sourceType",
                    "JOB_DESCRIPTION"
            );


            metadata.put(
                    "company",
                    safe(
                            session.getCompanyName()
                    )
            );


            metadata.put(
                    "role",
                    safe(
                            session.getJobRole()
                    )
            );


            metadata.put(
                    "contentHash",
                    contentHash
            );


            metadata.put(
                    "chunkIndex",
                    i
            );


            Document document =
                    new Document(
                            chunks.get(i),
                            metadata
                    );


            documents.add(document);
        }


        // =====================================================
        // STORE JD VECTORS
        // =====================================================

        if (!documents.isEmpty()) {

            vectorStore.add(
                    documents
            );
        }


        System.out.println(
                "JD RAG indexed successfully"
                        + " | session="
                        + session.getId()
                        + " | chunks="
                        + documents.size()
        );


        return contentHash;
    }


    // =========================================================
    // TEXT CHUNKING
    // =========================================================

    private List<String> chunkText(
            String text,
            int chunkSize
    ) {


        List<String> chunks =
                new ArrayList<>();


        if (text == null
                || text.isBlank()) {

            return chunks;
        }


        // =====================================================
        // CLEAN TEXT
        // =====================================================

        String cleaned =
                text
                        .replaceAll(
                                "\\s+",
                                " "
                        )

                        .trim();


        int start = 0;


        // =====================================================
        // CREATE CHUNKS
        // =====================================================

        while (start
                < cleaned.length()) {


            int end =
                    Math.min(

                            start
                                    + chunkSize,

                            cleaned.length()
                    );


            /*
             * Avoid splitting in the middle
             * of a word.
             */

            if (end < cleaned.length()) {


                int lastSpace =
                        cleaned.lastIndexOf(
                                ' ',
                                end
                        );


                if (lastSpace > start) {

                    end =
                            lastSpace;
                }
            }


            String chunk =
                    cleaned
                            .substring(
                                    start,
                                    end
                            )

                            .trim();


            if (!chunk.isBlank()) {

                chunks.add(
                        chunk
                );
            }


            start =
                    end;


            /*
             * Skip whitespace before
             * next chunk.
             */

            while (
                    start < cleaned.length()

                            && cleaned.charAt(
                            start
                    ) == ' '
            ) {

                start++;
            }
        }


        return chunks;
    }


    // =========================================================
    // SHA-256 HASH
    // =========================================================

    private String generateHash(
            String content
    ) {


        try {


            MessageDigest digest =
                    MessageDigest
                            .getInstance(
                                    "SHA-256"
                            );


            byte[] bytes =
                    digest.digest(

                            content.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );


            StringBuilder builder =
                    new StringBuilder();


            for (byte value : bytes) {


                builder.append(

                        String.format(
                                "%02x",
                                value
                        )
                );
            }


            return builder
                    .toString();


        } catch (Exception exception) {


            throw new RuntimeException(
                    "Unable to generate content hash",
                    exception
            );
        }
    }


    // =========================================================
    // CLEAN STRING
    // =========================================================

    private String clean(
            String value
    ) {


        if (value == null) {

            return null;
        }


        String cleaned =
                value.trim();


        return cleaned.isEmpty()
                ? null
                : cleaned;
    }


    // =========================================================
    // NULL SAFE STRING
    // =========================================================

    private String safe(
            String value
    ) {


        return value == null
                ? ""
                : value;
    }
}