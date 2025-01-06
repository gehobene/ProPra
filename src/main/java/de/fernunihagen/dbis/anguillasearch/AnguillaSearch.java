package de.fernunihagen.dbis.anguillasearch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import de.fernunihagen.dbis.anguillasearch.crawler.WebsiteData;
import de.fernunihagen.dbis.anguillasearch.searching.SearchEngine;
import de.fernunihagen.dbis.anguillasearch.util.Utils;

/**
 * Main class of the AnguillaSearch project.
 */
public final class AnguillaSearch {

    /**
     * Logger for the output or info or error messages.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(
            AnguillaSearch.class);
    /**
     * A Searchengine which gets immediately initialized with the seed urls
     * from the parsed json files.
     */
    private static SearchEngine searchEngine = new SearchEngine(getSeedUrls(
            parseJsons()), 1024);
    // ============================constructors===========================//

    private AnguillaSearch() {
    }

    // ==============================methods==============================//
    /**
     * Main method.
     *
     * @param args Command line arguments
     */
    public static void main(final String[] args) {

        // Print start message to logger
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Starting AnguillaSearch...");
        }

        /*
         * Set the java.awt.headless property to true to prevent awt from
         * opening windows.
         * If the property is not set to true, the program will throw
         * an exception when trying to
         * generate the graph visualizations in a headless environment.
         */
        System.setProperty("java.awt.headless", "true");
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Java awt GraphicsEnvironment headless: {}",
                    java.awt.GraphicsEnvironment.isHeadless());
        }
        /*
         * start the input loop
         */
        inputLoop();

    }

    /**
     * This Method will start an input loop reading user input from the
     * console. It will make sure the user selects a program mode first:
     * <ul>
     * <li>(1) For a search with TFIDF scores</li>
     * <li>(2) For a search with cosine similarity</li>
     * <li>(3) For a search with a combination of cosine similarity
     * and pagerank</li>
     * </ul>
     * After successfully selecting a program mode the user then can
     * enter his search query. And the search gets performed by calling
     * {@link AnguillaSearch#search(String,String) search()}.
     */

    private static void inputLoop() {
        /* try with resources and initialization of a new BufferedReader */
        try (BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            /* endless loop */
            while (true) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(String.format("Please write (1) for search"
                            + "with TFIDF score, (2) for search with"
                            + "cosine similarity or (3) for search with"
                            + "a combination of cosine similarity and"
                            + "page rank"));
                }
                /*
                 * Read line for program mode selection from console and remove
                 * unneccessary spaces at the end.
                 */
                String programMode = bufferedReader.readLine().trim();
                /*
                 * if line doesn't equal "1","2" or "3" the loop starts from
                 * beginning
                 */
                switch (programMode) {
                    case "1":
                    case "2":
                    case "3":
                        break;
                    default:
                        if (LOGGER.isInfoEnabled()) {
                            LOGGER.info("Select a mode first.");
                        }
                        continue;
                }
                /*
                 * Program asks user to perform search query or exit program.
                 */
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(String.format("Please enter your search"
                            + "query or type 'exit' to exit the program"));
                }
                /*
                 * Read the search query from console and remove
                 * unneccessary spaces at the end.
                 */
                String searchQuery = bufferedReader.readLine().trim();
                /*
                 * if text equals "exit" break the loop and exit the method
                 */
                if ("exit".equalsIgnoreCase(searchQuery)) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("Exiting Anguillasearch");
                    }
                    break;
                }
                /*
                 * if searchquery is empty start from the beginning of the loop
                 * else perform the search by calling the appropriate Method.
                 */
                if (!searchQuery.isEmpty()) {
                    search(programMode, searchQuery);
                } else {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("Empty message is not allowed.");
                    }

                }
            }
        } catch (IOException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error(String.format("Failed to read line from"
                        + "keyboard: %s",
                        e.getMessage()));
            }
        }
    }
    /**
     *
     * @param programMode
     * @param searchQuery
     */

    private static void search(final String programMode, final String
    searchQuery) {
        /*
         * split search query on one space characters (one or more between
         * words)
         */
        String[] queryTokens = searchQuery.split("\\s+");
        /*
         * puts the query array into a list and initializes a list
         * for the search result
         */
        List<String> queryTokensList = Arrays.asList(queryTokens);
        List<String> searchResults;
        /*
         * switch over the program modes and call the appropriate
         * method on the search engine. (default needed or else
         * field searchResults might not be initialized)
         */
        switch (programMode) {
            case "1":
                searchResults = searchEngine.searchQuery(queryTokens);
                break;
            case "2":
                searchResults = searchEngine.searchQueryCosine(queryTokens);
                break;
            default:
                searchResults = searchEngine.
                searchQueryPageRankAndCosine(queryTokens);
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("___Search Results___: found"
                    + " %s results", searchResults.size()));
        }
        /*
         * if search results are empty prints error message
         * else iterates over the list of results and prints
         * url + title + text snippet from webpage
         */
        if (searchResults.isEmpty()) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format("No results found for : %s",
                        searchQuery));
            }
        } else {
            for (String url : searchResults) {
                /*
                 * gets WebsiteData object with all content
                 * for the url
                 */
                WebsiteData websiteData = searchEngine.getCrawledData().
                get(url);
                /* print title, url and part of the website */
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(String.format("-> %s", url));
                    LOGGER.info(String.format("Title: %s",
                            websiteData.getTitle()));
                    /*
                     * creates snippet from webpage and prints it
                     */
                    String text = SearchEngine.createTextForSearchResult(
                            websiteData, queryTokensList);
                    LOGGER.info(String.format("%s", text));
                    LOGGER.info("******************************");
                }

            }
        }

    }

    /**
     * Parses a predefined json file int a {@link List} of {@link JsonObject}.
     *
     * @return the list of json objects.
     */
    private static List<JsonObject> parseJsons() {
        List<JsonObject> testJSONs = new ArrayList<>();
        try {
            testJSONs.add(Utils.parseJSONFile("intranet/"
                    + "cheesy1-f126d0d3.json"));
        } catch (IOException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error(String.format("Failed to parse Jsons: %s",
                        e.getMessage()));
            }
        }
        return testJSONs;
    }

    /**
     * Extracts the seed urls from a {@link List} of {@link List}.
     *
     * @param testJSONs A list of {@link JsonObject} to be processed.
     * @return A {@code Strring[]} representing the seed urls of the
     *         provided {@link List} of {@link List}.
     */
    private static String[] getSeedUrls(final List<JsonObject> testJSONs) {
        List<String> urlList = new ArrayList<>();
        for (JsonObject testJSON : testJSONs) {
            String[] seedUrls = new Gson().fromJson(testJSON.get(
                    "Seed-URLs"), String[].class);
            urlList.addAll(Arrays.asList(seedUrls));
        }
        return urlList.toArray(new String[urlList.size()]);
    }
}
