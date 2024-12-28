package de.fernunihagen.dbis.anguillasearch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The {@code SearchEngine} class performs a search on an Array of seed urls
 * according to a search query. It looks which search results are the most
 * fitting and returns a List of urls in descending order (from best result
 * to worst).
 * It uses a {@link Crawler} to gather website data (title, header, content)
 * in the form of a List of {@link WebsiteData} objects from a list of seed
 * urls (following outgoing links aswell), processes the data using an
 * {@link IndexBuilder} which tokenizes and lemmatizes the data and removes
 * stop words and emojis. Then it calculates a forward and reverse index and
 * saves a map with all tokens from the website data and an inner map with
 * corresponding urls and TFIDF scores.
 * The {@code SearchEngine} class then uses the search query, tokenizes and
 * lemmatizes it and compares the tokens to the reverseindex map from the
 * {@link IndexBuilder} to determine which urls should be shown as relevant
 * searchr esults.
 */
public class SearchEngine {

    /**
     * An instance of IndexBuilder that holds the reverse index data.
     */
    private IndexBuilder indexBuilder;

    /**
     * Performs a search on the given URLs based on the provided query.
     *
     * @param query an array of strings which is the search query.
     * @param urls  an array of urls to be crawled, indexed and searched.
     * @return a list of URLs sorted by their relevance to the search query in
     *         descending order.
     */
    // ============================constructors===========================//
    // ==============================methods==============================//

    public List<String> search(final String[] query, final String[] urls) {
        Crawler crawler = new Crawler(1024);
        crawler.crawl(Arrays.asList(urls));
        indexBuilder = new IndexBuilder(crawler.getCrawledDataAsList());
        Map<String, Double> sortedUrls = searchQuery(query);
        return extractUrlsToList(sortedUrls);
    }

    /**
     * Processes the search query to to calculate the relevance of urls fitting
     * the query. It tokenizes and lemmatizes the query and then for every url
     * in
     * the reverse index of the {@link #indexBuilder} and for every token of
     * the
     * search query the matching url TFIDF scores are added up so that every
     * url
     * has once final total score matching the relevance of the site for the
     * given
     * search query.
     *
     * @param query an array of strings which is the search query.
     * @return a map of urls and their corresponding relevance scores, sorted by
     *         score in descending order according to the scores.
     */
    private Map<String, Double> searchQuery(final String[] query) {
        /*
         * joins the strings in the given array to a single string with
         * whitespaces inbetween
         */
        String queryString = String.join(" ", query);
        /*
         * tokenizes and lemmatizes the string (search query) and removes stop
         * words and emojis
         */
        List<String> queryTokens = StringTokenizer.tokenizeAndLemmatize(
                queryString);
        // retrieves the calculated reverseindex
        Map<String, Map<String, Double>> reverseIndex = indexBuilder
                .getReverseIndex();
        /*
         * initializes a Map for adding up TFIDF scores per url who match
         * the tokens
         */
        Map<String, Double> addedScoresPerUrl = new HashMap<>();

        for (String token : queryTokens) {
            // gets the map with urls and scores for the token
            Map<String, Double> tokenScores = reverseIndex.get(token);
            // if token exists in map
            if (tokenScores != null) {
                /* add up the scores for every url for this token */
                addScores(addedScoresPerUrl, tokenScores);
                /* sort the map */
            }
        }
        return sortScores(addedScoresPerUrl);
    }

    /**
     * Adds the TFIDF scores for the given token to the cumulative scores per
     * url.
     *
     * @param addedScoresPerUrl a map of urls and their added scores so far.
     * @param tokenScores       a map of urls and scores for the current token.
     */
    private void addScores(
            Map<String, Double> addedScoresPerUrl,
            final Map<String, Double> tokenScores) {
        /*
         * iterates over the map with urls and scores and adds up the scores to
         * the provided map which may or may not contain the url with some added
         * up scores for earlier tokens of the search
         */
        for (Map.Entry<String, Double> entry : tokenScores.entrySet()) {
            String url = entry.getKey();
            Double tfidfScore = entry.getValue();
            /*
             * if the url already exists in the map, get the value and add the
             * new
             * value for the current token ontop, otherwise create a new entry
             * and put the url as key and score as value
             */
            if (addedScoresPerUrl.containsKey(url)) {
                addedScoresPerUrl.put(
                        url,
                        addedScoresPerUrl.get(url) + tfidfScore);
            } else {
                addedScoresPerUrl.put(url, tfidfScore);
            }
        }
    }

    /**
     * Sorts the urls by their relevance scores (total sum of TFIDF scores of
     * matching tokens) in descending order.
     *
     * @param addedScoresPerUrl a map of urls and their added up scores.
     * @return a sorted map of urls and scores in descending order of scores.
     */
    private Map<String, Double> sortScores(
            final Map<String, Double> addedScoresPerUrl) {
        /*
         * retrievs a list of scores (values) from the given map of urls and
         * values
         * and sorts them in descending order
         */
        List<Double> values = new ArrayList<>(addedScoresPerUrl.values());
        values.sort((value1, value2) -> Double.compare(value2, value1));
        /*
         * sorts the map in the same order (descending by score) as the earlier
         * list by adding the orls and corresponding scores to a new map in
         * the right order by iterating over the list values (which is sorted
         * in descending order)
         */
        Map<String, Double> sortedMap = new HashMap<>();
        /* iterates over the list */
        for (Double value : values) {
            /* iterates over the given map */
            for (Map.Entry<String, Double> entry : addedScoresPerUrl
                    .entrySet()) {
                /*
                 * if the score in the map equals the score in the list and
                 * the url
                 * isn't already in the list put it in, otherwise skip this url
                 * (makes sure every url is added only once while equal values
                 * could be mapped to different urls
                 */
                if (entry.getValue().equals(value)
                        && !sortedMap.containsKey(entry.getKey())) {
                    sortedMap.put(entry.getKey(), value);
                    break;
                }
            }
        }
        return sortedMap;
    }

    /**
     * Extracts the urls from the sorted map into a list.
     *
     * @param sortedMapWithUrlsAndScores a map of urls and scores, sorted by
     *                                   scores
     *                                   in descending order.
     * @return a list of urls sorted by relevance according to the scores in
     *         descending order.
     */
    private List<String> extractUrlsToList(
            final Map<String, Double> sortedMapWithUrlsAndScores) {
        List<String> sortedUrls = new ArrayList<>();
        /* extracts the keys from the map and puts them into a list */
        for (Map.Entry<String, Double> entry : sortedMapWithUrlsAndScores
                .entrySet()) {
            sortedUrls.add(entry.getKey());
        }
        return sortedUrls;
    }

}

// ============================getter/setter============================//
