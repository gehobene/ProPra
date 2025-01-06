package de.fernunihagen.dbis.anguillasearch.indexing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.fernunihagen.dbis.anguillasearch.crawler.WebsiteData;
import de.fernunihagen.dbis.anguillasearch.util.StringTokenizer;

/**
 * The {@code IndexBuilder} class will calculate forward, reverse indexes
 * and TFIDF scores for a collection of websites.
 *
 * <p>
 * The class will do the following:
 * <ul>
 * <li>Calculates the forward index, which maps each website url to a list
 * of its tokenized and lemmatized website content.</li>
 * <li>Calculates the reverse index without TFIDF scores, and maps each token
 * to the set of website urls where it occurs.</li>
 * <li>Calculates the TFIDF scores for all tokens in the reverse index and
 * puts them into a new map.</li>
 * </ul>
 *
 * <p>
 * The indices are constructed at initialization.
 *
 * <p>
 * Exceptions are thrown if the input list is null or empty, or if an error
 * occurs during index building.
 */
public class IndexBuilder {

    /**
     * the forward index (a map of urls mapped to a list of tokens which are
     * the tokenized and lemmatized content of the corresponding site
     * (title,header,body)).
     */
    private Map<String, List<String>> forwardIndex;

    /**
     * the forward index with TFIDF scores (stores for each url a mapping from
     * the token to the
     * TFIDF score for that url).
     */

    private Map<String, Map<String, Double>> forwardIndexTfIdf;
    /**
     * the reverse index without TFIDF scores(a token gets mapped to a set of
     * urls on which sites the token occurs).
     */
    private Map<String, Set<String>> reverseIndexHelper;

    /**
     * the reverse index (stores for each token a mapping from the url to the
     * TFIDF score for that token).
     */
    private Map<String, Map<String, Double>> reverseIndex;

    /**
     * total number of indexed websites.
     */
    private int totalWebsites;

    /**
     * list of website data to be indexed.
     */
    private List<WebsiteData> dataToIndex;

    /**
     * Set of all Tokens that occur in the indexed data.
     */
    private Set<String> setOfAllTokens;

    // ============================constructors===========================//

    /**
     * Constructs an {@code IndexBuilder} instance for the list of
     * {@code WebsiteData}.
     *
     * @param data a list of {@code WebsiteData} objects with the websites
     *             to index and calculate the TFIDF score for.
     * @throws IllegalArgumentException if the provided list is null or empty.
     * @throws IllegalStateException    if an error occurs during the building
     *                                  of the indexes and score calculation.
     */
    public IndexBuilder(final List<WebsiteData> data) {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException(
                    "the provided List can not be empty");
        }
        /* initialization of fields */
        this.dataToIndex = new ArrayList<>(data);
        this.forwardIndex = new HashMap<>();
        this.reverseIndexHelper = new HashMap<>();
        this.reverseIndex = new HashMap<>();
        this.totalWebsites = data.size();
        this.forwardIndexTfIdf = new HashMap<>();
        this.setOfAllTokens = new HashSet<>();
        /*
         * calculation of indexes, try catch in case something goes
         * wrong. Mitigation of half initialized object.
         */
        try {
            calculateForwardIndex();
            calculateReverseIndex();
            calculateTFIDFScore();
            setOfAllTokens.addAll(reverseIndexHelper.keySet());
            calculateForwardIndexTfIdf();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "IndexBuilder could not be initialized",
                    e);
        }
    }

    // ==============================methods==============================//

    /**
     * Calculates the forward index, which maps each website url to a list of
     * its tokenized and lemmatized website content.
     */
    private void calculateForwardIndex() {
        for (WebsiteData data : dataToIndex) {
            forwardIndex.put(
                    data.getUrlOfSite(),
                    StringTokenizer.tokenizeAndLemmatize(data));
        }
    }

    /**
     * Calculates the reverse index without TFIDF scores, and maps each token
     * to the set of website urls where it occurs.
     */
    private void calculateReverseIndex() {
        for (Map.Entry<String, List<String>> entry : forwardIndex.entrySet()) {
            String forwardKey = entry.getKey();
            List<String> forwardValues = entry.getValue();

            // Iteriere über die Liste der Strings im forwardIndex
            for (String value : forwardValues) {
                /*
                 * Prüfe, ob der String bereits als Key im reverseIndex
                 * existiert
                 */
                reverseIndexHelper.computeIfAbsent(
                        value, val -> new HashSet<>());
                /*
                 * Füge den aktuellen Key des forwardIndex zur Liste im
                 * reverseIndex hinzu
                 */
                reverseIndexHelper.get(value).add(forwardKey);
            }
        }
    }

    /**
     * Calculates the TFIDF scores for all tokens in the reverse index.
     */
    private void calculateTFIDFScore() {
        // retrieve Set of all Tokens
        Set<String> tokens = reverseIndexHelper.keySet();
        if (!tokens.isEmpty()) {
            // iterate over all tokens and calculate the TFIDF score for every
            // url where that token appears
            for (String token : tokens) {
                calculateTFIDFScoreHelper(token);
            }
        }
    }

    /**
     * Helper method to calculate the TFIDF score for a token for the
     * reverse index.
     *
     * @param token the token for which the score shall be calculated.
     */
    private void calculateTFIDFScoreHelper(final String token) {
        List<String> urlsWhereTokenOccurs;
        Double idfScore;
        // if token exists in the reverse index helper
        if (reverseIndexHelper.containsKey(token)) {
            // calculate IDF score for that token
            idfScore = calculateIDFScore(token);
            // get all urls where the token occurs
            urlsWhereTokenOccurs = new ArrayList<>(
                    reverseIndexHelper.get(token));
            // iterate over all urls
            for (String url : urlsWhereTokenOccurs) {
                /*
                 * calculate tf and idf scores for the url in combination with
                 * the token
                 */
                Double tfScore = calculateTFScore(token, url);
                Double tfIdfScore = tfScore * idfScore;
                // initialize a new map for the token if it doesn't exist yet
                reverseIndex.computeIfAbsent(token, val -> new HashMap<>());
                // add the url and corresponding score to the map for the token
                reverseIndex.get(token).put(url, tfIdfScore);
            }
        }
    }

    /**
     * Calculates the IDF score for a given token.
     *
     * Formula: IDF(token) = log(N / df(token))
     * - N is the total number of documents (size of dataToIndex).
     * - df(token) is the document frequency of the token (number of documents
     * containing the token).
     *
     * @param token the word or term for which the IDF score shall be
     *              calculated.
     * @return the IDF value of the token.
     * @throws IllegalArgumentException if the token doesn't exist in any of
     *                                  the documents that got indexed.
     */
    public Double calculateIDFScore(final String token) {
        if (reverseIndexHelper.containsKey(token)
                && !reverseIndexHelper.get(token).isEmpty()) {
            // calculate and return the score
            return Math.log(
                    (double) totalWebsites / reverseIndexHelper.get(
                            token).size());
        } else {
            throw new IllegalArgumentException(
                    "Token doesn't exist in any document");
        }
    }

    /**
     * Calculates the TF score for a token on a website.
     *
     * Formula:
     * TF(token, website) = frequency of token in website / total number of
     * tokens in website.
     *
     * @param token   the token for which the TF score shall be calculated.
     * @param website the website in which the TF score shall be calculated.
     * @return the TF score as a double. Returns 0.0 if the website does not
     *         exist in the forward index or if the token does not exist in
     *         website.
     */
    private double calculateTFScore(final String token, final String website) {
        // get all tokens for the webiste
        List<String> tokensInWebsite = forwardIndex.get(website);
        // if the website doesn't exist or the token isn't contained inside
        // return 0.0 as score
        if (tokensInWebsite == null || !tokensInWebsite.contains(token)) {
            return 0.0;
        }
        // count how often the token occurs in the website
        int frequency = Collections.frequency(tokensInWebsite, token);
        // calculate the Score
        return (double) frequency / tokensInWebsite.size();
    }

    /**
     * Maps the TFIDF scores from the reverse index to the forward index
     * with TFIDF. For every url a Map of (token -> TFIDF) is created
     * and filled accordingly.
     */
    private void calculateForwardIndexTfIdf() {
        /* iterates over all urls in the forwardindex */
        for (String url : forwardIndex.keySet()) {
            /*
             * initializes a map for the vector of TFIDF scores for the
             * current url. (token -> TFIDF)
             */
            Map<String, Double> urlVector = new HashMap<>();
            /*
             * iterates over the set of all occuring tokens in the indexed
             * data
             */
            for (String token : setOfAllTokens) {
                /*
                 * if the token exists for this url put the value mapped to
                 * the token into the urlVector else map a 0.0 to that token
                 */
                Double tfIdf = reverseIndex.get(token).get(url);
                if (tfIdf != null) {
                    urlVector.put(token, tfIdf);
                } else {
                    urlVector.put(token, 0.0);
                }

            }

            /*
             * map the urlvector to the corresponding url in the forward
             * index
             */
            forwardIndexTfIdf.put(url, urlVector);
        }
    }

    // ===========================getter/setter===========================//

    /**
     * Returns the forward index.
     *
     * @return the map with the forward index of all processed data.
     */
    public Map<String, List<String>> getForwardIndex() {
        return new HashMap<>(forwardIndex);
    }

    /**
     * Returns the total number of processed websites.
     *
     * @return the total number processed websites.
     */
    public int getTotalWebsites() {
        return totalWebsites;
    }

    /**
     * Returns the reverse index with TFIDF scores.
     *
     * @return the map with the tokens mapped to urls and their corresponding
     *         TFIDF scores.
     *         (Token ->(url->TFIDF)).
     */
    public Map<String, Map<String, Double>> getReverseIndex() {
        return new HashMap<>(reverseIndex);
    }

    /**
     * Returns a Set of all tokens of the crawled and indexed websites.
     *
     * @return the map with the tokens mapped to urls and their corresponding
     *         TFIDF scores.
     *         (Token ->(url->TFIDF)).
     */

    public Set<String> getSetOfAllTokens() {
        return new HashSet<>(setOfAllTokens);
    }

    /**
     * Returns the forward index with TFIDF scores.
     *
     * @return the map with the urls mapped to tokens and their corresponding
     *         TFIDF scores.
     *         (url ->(Token->TFIDF)).
     */
    public Map<String, Map<String, Double>> getForwardIndexTfIdf() {
        return new HashMap<>(forwardIndexTfIdf);
    }
}
