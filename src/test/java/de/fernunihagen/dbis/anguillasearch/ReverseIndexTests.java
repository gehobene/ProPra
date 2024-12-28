package de.fernunihagen.dbis.anguillasearch;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Unit tests for the reverse index.
 */
class ReverseIndexTests {

    static List<JsonObject> testPages;
    static JsonObject correctReverseIdex;
    static Map<String, WebsiteData> crawledData;
    static IndexBuilder indexBuilder;

    @BeforeAll
    static void setUp() throws IOException {

        testPages = Utils.parseAllJSONFiles(java.util.Optional.of("src/test/resources/tf-idf/pages"));
        correctReverseIdex = Utils.parseJSONFile("src/test/resources/tf-idf/index.json");

        // Add your code here to create your reverse index
        crawledData = new HashMap<>();
        for (JsonObject jsonObject : testPages) {
            String urlOfSite = jsonObject.get("url").getAsString();
            WebsiteData data = new WebsiteData(urlOfSite);
            data.setTitle(jsonObject.get("title").getAsString());
            data.setHeader(jsonObject.get("headings").getAsString());
            data.setBody(jsonObject.get("paragraphs").getAsString());
            JsonArray outgoingLinks = jsonObject.getAsJsonArray("outgoingLinks");
            for (JsonElement link : outgoingLinks) {
                data.addLink(link.getAsString());
            }
            crawledData.put(urlOfSite, data);

        }
        indexBuilder = new IndexBuilder(new ArrayList<>(crawledData.values()));

    }

    @Test
    void reverseIdexTFIDF() {

        for (Entry<String, JsonElement> entry : correctReverseIdex.entrySet()) {
            // The token of the reverse index
            String token = entry.getKey();
            JsonObject pagesMap = entry.getValue().getAsJsonObject();
            for (Entry<String, JsonElement> pageEntry : pagesMap.entrySet()) {

                // The URL of the page
                String url = pageEntry.getKey();
                // The TF-IDF value of the token in the page
                Double tfidf = pageEntry.getValue().getAsDouble();

                // Add your code here to compare the TF-IDF values of your reverse index with
                // the correct values

                // Check if the reverse index contains the token
                assertTrue(indexBuilder.getReverseIndex().containsKey(token));

                // Get the map of pages for the token
                Map<String,Double> urlList = new HashMap<>();
                if (indexBuilder.getReverseIndex().containsKey(token)) {
                    urlList = indexBuilder.getReverseIndex().get(token);
                }

                // Check if the URL exists for that token
                assertTrue(urlList.containsKey(url));

                // Get the TF-IDF value for the URL from your reverse index
                Double indexTfidf;
        
                indexTfidf = indexBuilder.getReverseIndex().get(token).get(url);
                // Check if the TF-IDF value is correct
                assertTrue(Math.abs(tfidf - indexTfidf) < 0.0001);


            }
        }
    }
}