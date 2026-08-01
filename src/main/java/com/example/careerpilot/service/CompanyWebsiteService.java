package com.example.careerpilot.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.net.URI;

@Service
public class CompanyWebsiteService {

    private static final int TIMEOUT = 10_000;

    private static final int MAX_TEXT_LENGTH = 50_000;


    // =========================================================
    // NORMALIZE WEBSITE
    // =========================================================

    public String normalizeWebsite(String website) {

        if (website == null || website.isBlank()) {
            return null;
        }

        String value = website.trim();

        /*
         * If user enters:
         *
         * tcs.com
         *
         * convert it to:
         *
         * https://tcs.com
         */

        if (!value.startsWith("http://")
                && !value.startsWith("https://")) {

            value = "https://" + value;
        }


        try {

            URI uri = URI.create(value);

            String host = uri.getHost();


            if (host == null || host.isBlank()) {

                throw new IllegalArgumentException(
                        "Invalid company website"
                );
            }


            host = host.toLowerCase();


            /*
             * Remove www.
             *
             * www.tcs.com
             *
             * becomes:
             *
             * tcs.com
             */

            if (host.startsWith("www.")) {

                host = host.substring(4);
            }


            /*
             * Always use HTTPS.
             *
             * These:
             *
             * https://www.tcs.com
             * https://tcs.com
             * http://tcs.com
             * tcs.com
             *
             * all become:
             *
             * https://tcs.com
             */

            return "https://" + host;

        } catch (Exception exception) {

            throw new IllegalArgumentException(
                    "Invalid company website: "
                            + website,
                    exception
            );
        }
    }


    // =========================================================
    // EXTRACT WEBSITE TEXT
    // =========================================================

    public String extractWebsiteText(
            String website
    ) {

        String normalized =
                normalizeWebsite(website);


        if (normalized == null) {
            return "";
        }


        try {

            /*
             * Fetch website using Jsoup.
             *
             * Some websites reject basic Java HTTP
             * requests, so we provide normal browser-like
             * request headers.
             */

            Document document =
                    Jsoup.connect(normalized)

                            .userAgent(
                                    "Mozilla/5.0 "
                                            + "(Windows NT 10.0; Win64; x64) "
                                            + "AppleWebKit/537.36 "
                                            + "(KHTML, like Gecko) "
                                            + "Chrome/150.0.0.0 "
                                            + "Safari/537.36"
                            )

                            .header(
                                    "Accept",
                                    "text/html,"
                                            + "application/xhtml+xml,"
                                            + "application/xml;q=0.9,"
                                            + "*/*;q=0.8"
                            )

                            .header(
                                    "Accept-Language",
                                    "en-US,en;q=0.9"
                            )

                            .referrer(
                                    "https://www.google.com/"
                            )

                            .timeout(TIMEOUT)

                            .followRedirects(true)

                            .get();


            // =================================================
            // REMOVE UNNECESSARY HTML
            // =================================================

            document.select(
                    "script, "
                            + "style, "
                            + "nav, "
                            + "footer, "
                            + "noscript, "
                            + "svg, "
                            + "form"
            ).remove();


            // =================================================
            // EXTRACT TEXT
            // =================================================

            String text;


            if (document.body() != null) {

                text =
                        document
                                .body()
                                .text();

            } else {

                text = "";
            }


            if (text == null) {
                return "";
            }


            // =================================================
            // CLEAN WHITESPACE
            // =================================================

            text =
                    text
                            .replaceAll(
                                    "\\s+",
                                    " "
                            )

                            .trim();


            // =================================================
            // LIMIT SIZE
            // =================================================

            /*
             * Prevent accidentally sending huge pages
             * into the embedding model.
             */

            if (text.length()
                    > MAX_TEXT_LENGTH) {

                text =
                        text.substring(
                                0,
                                MAX_TEXT_LENGTH
                        );
            }


            return text;


        } catch (Exception exception) {

            /*
             * IMPORTANT:
             *
             * Company website RAG is optional.
             *
             * Websites such as TCS may return:
             *
             * 403 Forbidden
             *
             * That should NOT stop the interview.
             *
             * We return empty text and allow the
             * interview to continue using JD RAG.
             */

            System.err.println(
                    "Company website could not be fetched: "
                            + normalized
                            + " | "
                            + exception.getMessage()
            );


            return "";
        }
    }
}