package de.fernunihagen.dbis.anguillasearch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * The {@code SearchEngine} class performs a search on an Array of seed urls
 * according to a search query. It looks which search results are the most
 * fitting and returns a List of urls in descending order (from best result
 * to worst).
 * It uses a {@link Crawler} to gather website data (title, header, content)
 * in the form of a List of {@link WebsiteData} objects from a list of seed
 * urls (following outgoing links aswell), processes the data using an
 * {@link IndexBuilder} which tokenizes and lemmatizes the data and removes
 * stop words and emojis. Then it calculates a forward index and reverse index.
 * The {@code SearchEngine} class can provide search results according to
 * the combined TFIDF score of the tokens are contained within the site
 * of a specific url or
 * according to the cosine similarity between the vector of an url and the
 * vector of the query or a combination of those two scores are used to
 * determine which results are most fitting according to the query.
 * The results will be sorted in descending order starting
 * from the most relevant one.
 */
public class SearchEngine {

    /**
     * An instance of IndexBuilder that holds multiple different indexes
     * and the TFIDF scores.
     */
    private IndexBuilder indexBuilder;
    /**
     * An instance of Crawler that holds the crawled website data.
     */
    private Crawler crawler;
    /**
     * An instance of PageRank that holds a map of urls mapped to the
     * corresponding page rank. (url -> page rank)
     */
    private PageRank pageRank;

    // ============================constructors===========================//
    /**
     * Creates a new instanze of {@link SearchEngine}. Initializes a new
     * {@link Crawler} and crawls the given {@link String[]} of
     * urls. Then the field {@link #indexBuilder} is initialized with a new
     * {@link IndexBuilder} and all crawled data gets processed by the
     * {@link IndexBuilder} to create all necessary indexes, like
     * forward index, reverse index and forward index with TFIDF scores.
     * Afterwards the {@link #pageRank} field is initialized and the
     * PageRank object calculates all page ranks for the urls provided
     * by the crawler. Once initialized the object can be used to perform
     * different types of searches on the crawled websites.
     *
     * @param seedUrls an array of urls which are the seed urls for the crawler
     *             of this SearchEngine.
     */

    public SearchEngine(final String[] seedUrls) {
        this.crawler = new Crawler(1024);
        crawler.crawl(Arrays.asList(seedUrls));
        this.indexBuilder = new IndexBuilder(crawler.getCrawledDataAsList());
        this.pageRank = new PageRank(crawler.getCrawledDataAsList());

    }
    // ==============================methods==============================//

    /**
     * Tokenizes and lemmatizes the search query by using the
     * {@link StringTokenizer} class. Joins the query {@link String[]} to
     * a {@link String} and feeds it to the {@link StringTokenizer}.
     *
     * @param query an array of strings which is the search query.
     * @return a {@link List} of the tokens of the query.
     */

    private List<String> tokenizeQuery(final String[] query) {
        /*
         * joins the strings in the given array to a single string with
         * whitespaces inbetween
         */
        String queryString = String.join(" ", query);
        /*
         * tokenizes and lemmatizes the string (search query) and removes stop
         * words and emojis
         */
        return StringTokenizer.tokenizeAndLemmatize(
                queryString);
    }

    /**
     * Searches the given urls and outgoing links up to a total of maximum
     * 1024 websites in total for the search query terms and spits out a
     * result list which is sorted in descending order by relevance.
     *
     * @param query an array of query tokens to search for in the
     *              processed url websites. (the search request)
     *
     * @return a list of urls sorted in descending order by a combination
     *         of cosine similarity and page rank.
     */

    public List<String> searchQueryPageRankAndCosine(final String[] query) {
        Map<String, Double> cosineMap = processQueryAndSearchCosine(query);
        Map<String, Double> pageRankMap = pageRank.getPageRanksPerUrl();
        Map<String, Double> combinedScores = new HashMap<>();
        for (Map.Entry<String, Double> entry : cosineMap.entrySet()) {
            String url = entry.getKey();
            Double similarityScore = entry.getValue();
            Double combinedScore = similarityScore * pageRankMap.get(url);
            combinedScores.put(url, combinedScore);
        }
        Map<String, Double> sortedUrls = sortScores(
                combinedScores);
        return extractUrlsToList(sortedUrls);
    }

    /**
     * Searches the given urls and outgoing links up to a total of maximum
     * 1024 websites in total for the search query terms and spits out a
     * result list which is sorted in descending order by relevance.
     *
     * @param query an array of query tokens to search for in the
     *              processed url websites. (the search request)
     * @return a list of urls sorted in descending order by TFIDF scores
     *         in regard to the query.
     */

    public List<String> searchQuery(final String[] query) {
        Map<String, Double> sortedUrls = sortScores(
                processQueryAndSearchTFIDF(query));
        return extractUrlsToList(sortedUrls);
    }

    /**
     * Searches the given urls and outgoing links up to a total of maximum
     * 1024 websites in total for the search query terms and spits out a
     * result list which is sorted in descending order by relevance.
     *
     * @param query an array of query tokens to search for in the
     *              processed url websites. (the search request)
     * @return a list of urls sorted in descending order by cosine similarity
     *         in regard to the query.
     */

    public List<String> searchQueryCosine(final String[] query) {
        Map<String, Double> sortedUrls = sortScores(
                processQueryAndSearchCosine(query));
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
     * has once final total score representing the relevance of the site for
     * the given search query.
     *
     * @param query an array of strings which is the search query.
     * @return a map of urls and their corresponding relevance scores.
     */
    private Map<String, Double> processQueryAndSearchTFIDF(final String[]
     query) {
        List<String> queryTokens = tokenizeQuery(query);
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

        Iterator<Map.Entry<String, Double>> iterator = addedScoresPerUrl.
        entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue() == 0.0) {
                iterator.remove();
            }
        }


        return addedScoresPerUrl;
    }

    /**
     * Processes the search query to to calculate the relevance of urls fitting
     * the query. It tokenizes and lemmatizes the query. It then creates a
     * vector ({@link TokenVector}) for the search query. Then for every url
     * in
     * the forward index with TFIDF scores of the {@link #indexBuilder} creates
     * a vector ({@link TokenVector}) of all the tokens and their TFIDF scores.
     * Then it calculates the cosine similarity of the query vector and the
     * url vector for every url and returns a map of (url -> cosine similarity)
     *
     * @param query an array of strings which is the search query.
     * @return a map of urls and their corresponding relevance scores
     */
    private Map<String, Double> processQueryAndSearchCosine(final String[]
     query) {
        /* tokenizes and lemmatizes the query */
        List<String> queryTokens = tokenizeQuery(query);
        /*
         * creates a TokenVector for the query (calculates the vector of the
         * query)
         */
        TokenVector queryVector = calculateQueryVector(queryTokens);


        /* gets the forward indexes ith TFIDF scores of the crawled websites */
        Map<String, Map<String, Double>> urlVectorsMap = indexBuilder.
        getForwardIndexTfIdf();

        /*
         * creates a result map for urls and cosine similarity scores
         * (url -> (cosine similarity of url and query))
         */
        Map<String, Double> similarityScores = new HashMap<>();
        /*
         * iterates over every url in the index and creates a tokenvector for
         * that url. Then the cosine similarity of the url vector and the
         * query vector gets calculated and the result gets put into the
         * map. (url -> (cosine similarity of url and query))
         */
        for (Entry<String, Map<String, Double>> url : urlVectorsMap.entrySet(
        )) {
            TokenVector urlVector = new TokenVector(urlVectorsMap.get(url.
            getKey()));
            /* calculates the cosine similarity for */
            double cosineSimilarity = queryVector.computeCosineSimilarity(
                    urlVector);
            similarityScores.put(url.getKey(), cosineSimilarity);
        }
        /*
         * iterates over the resulting map and removes every entry
         * that has a value of 0 so that only the results remain in
         * the map that have a cosine similarity > 0.0.
         */
        Iterator<Map.Entry<String, Double>> iterator = similarityScores.
        entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue() == 0.0) {
                iterator.remove();
            }
        }

        return similarityScores;
    }

    /**
     * Helper method to calculate a {@link TokenVector} vector for the query.
     * It gets a {@link Set} of all tokens from the {@link #indexBuilder},
     * then for every Token of that set it puts an entry into a {@link Map}
     * with the value 0.0 if this token doesn't exist in the query and 1.0 if
     * it does. Then it creates a {@link TokenVector} object with this map and
     * returns it.
     *
     * @param queryTokens a List of Srings which is the search query.
     * @return a {@link TokenVector} object which represents the vector for
     *         the search query.
     */
    private TokenVector calculateQueryVector(final List<String> queryTokens) {
        /*
         * get a set of all tokens from the indexbuilder and
         * initialize a map for the query vector.
         */
        Set<String> allTokens = indexBuilder.getSetOfAllTokens();
        Map<String, Double> queryVector = new HashMap<>();

        /*
         * map amount of occurences of the tokens in the query to the token
         * for later code expansion of this method. This maps keyset can
         * then be used like a set of the tokens of queryTokens of the search.
         */
        Map<String, Integer> frequencyInQuery = new HashMap<>();
        /*
         * iterates over all query tokens and puts them into the map with a
         * value of 1. If the entry already exists the value will be incremented
         * by 1.
         */
        for (String token : queryTokens) {
            if (!queryTokens.isEmpty()) {
                if (frequencyInQuery.containsKey(token)) {
                    frequencyInQuery.put(token, frequencyInQuery.get(token)
                            + 1);
                } else {
                    frequencyInQuery.put(token, 1);
                }
            }
        }

        /*
         * iterate over every token of the reverse index which represents
         * every possible token in all crawled websites.
         */
        if (!queryTokens.isEmpty()) {
            for (String token : allTokens) {
                /* if the query was empty */
                if (!frequencyInQuery.containsKey(token)) {
                    queryVector.put(token, 0.0);

                } else {
                    queryVector.put(token, 1.0);
                }
            }
        }
        return new TokenVector(queryVector);
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
         * up scores for earlier processed tokens of the search
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
     * Sorts the map by values in descending order.
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
        Map<String, Double> sortedMap = new LinkedHashMap<>();
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

    /**
     * Creates a short snippet of the website body that contains
     * one of the tokens of the query.
     *
     * @param data the {@link WebsiteData} object that contains the body
     * for the snippet.
     * @param queryTokens the search query tokens that is looked for in
     * the body of the website.
     * @return a short snippet of the website body
     */
    public static String createTextForSearchResult(final WebsiteData data,
            final List<String> queryTokens) {
        /*
         * get the body of the website as toLowerCase to increase
         * chances of matching.
         */
        String body = data.getBody().toLowerCase(Locale.ROOT);
        /*
         * iterates over every querytoken and looks for the first
         * index in the string where token occurs
         */
        for (String token : queryTokens) {
            int index = body.indexOf(token.toLowerCase(Locale.ROOT));
            if (index != -1) {
                int startingPoint = Math.max(0, index - 50);
                int endPoint = Math.min(body.length(), index + 50);
                /*
                 * creates a substring between start and endpoint
                 * of the body which contains the querytoken.
                 */
                String text = body.substring(startingPoint, endPoint);
                return "..." + text + "...";
            }
        }
        /* if no querytoken matches the body return empty string */
        return "";
    }

    // ============================getter/setter============================//

    /**
     * Retrieves a map of website data of all crawled sites from the
     * internal crawler.
     * The key is the url and the value is the data in a structured format
     * in the datastructure class {@link WebsiteData}
     *
     * @return a copy of the map of website data of the internal
     *         crawler
     */
    public Map<String, WebsiteData> getCrawledData() {
        return new HashMap<>(crawler.getCrawledData());
    }
}
