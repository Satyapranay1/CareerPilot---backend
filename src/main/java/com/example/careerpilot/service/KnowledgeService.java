package com.example.careerpilot.service;

import com.example.careerpilot.dto.KnowledgeContext;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final VectorStore vectorStore;

    public KnowledgeContext getKnowledge(String company,
                                         String jobRole,
                                         String jobDescription) {

        String query = company + " " + jobRole;

        List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(1)
                        .build()
        );

        if (documents != null && !documents.isEmpty()) {

            return new KnowledgeContext(
                    "RAG",
                    documents.getFirst().getText()
            );
        }

        if (jobDescription != null && !jobDescription.isBlank()) {

            saveKnowledge(company, jobRole, jobDescription);

            return new KnowledgeContext(
                    "JOB_DESCRIPTION",
                    jobDescription
            );
        }

        return new KnowledgeContext(
                "GENERIC",
                ""
        );
    }

    private void saveKnowledge(String company,
                               String jobRole,
                               String content) {

        Document document = new Document(
                content,
                java.util.Map.of(
                        "company", company,
                        "jobRole", jobRole
                )
        );

        vectorStore.add(List.of(document));
    }

}