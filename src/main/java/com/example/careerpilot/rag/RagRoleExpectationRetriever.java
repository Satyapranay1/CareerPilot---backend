package com.example.careerpilot.rag;

import com.example.careerpilot.dto.RoleExpectation;
import com.example.careerpilot.exception.InsufficientRoleContextException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RagRoleExpectationRetriever
        implements RoleExpectationRetriever {

    private static final int TOP_K = 15;

    private static final int MINIMUM_EXPECTATIONS = 3;

    private final VectorStore vectorStore;

    @Override
    public List<RoleExpectation> retrieve(
            String company,
            String role
    ) {

        validateInput(company, role);

        String query = buildQuery(company, role);

        try {

            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(TOP_K)
                    .build();

            List<Document> documents =
                    vectorStore.similaritySearch(request);

            if (documents == null || documents.isEmpty()) {
                throw insufficient(company, role);
            }

            List<RoleExpectation> expectations =
                    extractExpectations(documents, company, role);

            if (expectations.size() < MINIMUM_EXPECTATIONS) {
                throw insufficient(company, role);
            }

            return expectations;

        } catch (InsufficientRoleContextException exception) {

            throw exception;

        } catch (Exception exception) {

            throw new InsufficientRoleContextException(
                    "Unable to retrieve reliable role expectations "
                            + "for the requested company and role.",
                    exception
            );
        }
    }

    private String buildQuery(
            String company,
            String role
    ) {

        
        return """
                Company: %s
                Role: %s
                Required skills, technologies, engineering expectations,
                responsibilities and interview competencies.
                """.formatted(
                company.trim(),
                role.trim()
        );
    }

    private List<RoleExpectation> extractExpectations(
            List<Document> documents,
            String company,
            String role
    ) {

        List<RoleExpectation> result = new ArrayList<>();

        for (Document document : documents) {

            RoleExpectation expectation =
                    toExpectation(document, company, role);

            if (expectation != null
                    && !containsKeyword(
                    result,
                    expectation.getKeyword()
            )) {

                result.add(expectation);
            }
        }

        return result;
    }

    private RoleExpectation toExpectation(
            Document document,
            String requestedCompany,
            String requestedRole
    ) {

        Map<String, Object> metadata =
                document.getMetadata();

        
        String keyword = metadataString(
                metadata,
                "keyword"
        );

        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        String documentCompany =
                metadataString(metadata, "company");

        String documentRole =
                metadataString(metadata, "role");

        
        if (!matchesRequestedContext(
                requestedCompany,
                requestedRole,
                documentCompany,
                documentRole
        )) {
            return null;
        }

        String category = metadataString(
                metadata,
                "category"
        );

        if (category == null || category.isBlank()) {
            category = "OTHER";
        }

        int importance = parseImportance(
                metadata.get("importance")
        );

        String description = document.getText();

        return new RoleExpectation(
                keyword.trim(),
                category.trim().toUpperCase(Locale.ROOT),
                importance,
                description == null
                        ? ""
                        : description.trim()
        );
    }

    private boolean matchesRequestedContext(
            String requestedCompany,
            String requestedRole,
            String documentCompany,
            String documentRole
    ) {

        
        if (documentCompany == null
                || documentRole == null) {
            return false;
        }

        boolean companyMatches =
                normalize(documentCompany)
                        .equals(normalize(requestedCompany));

        boolean roleMatches =
                normalize(documentRole)
                        .equals(normalize(requestedRole));

        return companyMatches && roleMatches;
    }

    private boolean containsKeyword(
            List<RoleExpectation> expectations,
            String keyword
    ) {

        String normalizedKeyword =
                normalize(keyword);

        return expectations.stream()
                .anyMatch(expectation ->
                        normalize(expectation.getKeyword())
                                .equals(normalizedKeyword)
                );
    }

    private int parseImportance(Object value) {

        if (value == null) {
            return 1;
        }

        try {

            int importance =
                    Integer.parseInt(value.toString());

            return Math.max(
                    1,
                    Math.min(5, importance)
            );

        } catch (NumberFormatException exception) {

            return 1;
        }
    }

    private String metadataString(
            Map<String, Object> metadata,
            String key
    ) {

        if (metadata == null) {
            return null;
        }

        Object value = metadata.get(key);

        if (value == null) {
            return null;
        }

        return value.toString();
    }

    private String normalize(String value) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private void validateInput(
            String company,
            String role
    ) {

        if (company == null || company.isBlank()) {
            throw new IllegalArgumentException(
                    "Company is required."
            );
        }

        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException(
                    "Role is required."
            );
        }
    }

    private InsufficientRoleContextException insufficient(
            String company,
            String role
    ) {

        return new InsufficientRoleContextException(
                "Insufficient knowledge-base context for company '"
                        + company
                        + "' and role '"
                        + role
                        + "'."
        );
    }
}