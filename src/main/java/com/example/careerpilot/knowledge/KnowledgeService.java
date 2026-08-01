package com.example.careerpilot.knowledge;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final KnowledgeProperties properties;

    public List<Document> loadDocuments() {

        List<Document> documents = new ArrayList<>();

        try {

            PathMatchingResourcePatternResolver resolver =
                    new PathMatchingResourcePatternResolver();

            Resource[] resources =
                    resolver.getResources(
                            properties.getPath()
                    );

            if (resources.length == 0) {

                log.warn("No knowledge files found.");

                return documents;
            }

            TokenTextSplitter splitter =
                    new TokenTextSplitter();

            for (Resource resource : resources) {

                log.info(
                        "Loading {}",
                        resource.getFilename()
                );

                TextReader reader =
                        new TextReader(resource);

                List<Document> fileDocuments =
                        reader.get();

                documents.addAll(
                        splitter.apply(fileDocuments)
                );
            }

            log.info(
                    "{} knowledge chunks created.",
                    documents.size()
            );

            return documents;

        } catch (Exception exception) {

            throw new RuntimeException(
                    "Unable to load knowledge files.",
                    exception
            );
        }
    }
}