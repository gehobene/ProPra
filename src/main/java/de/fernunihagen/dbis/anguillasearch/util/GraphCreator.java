package de.fernunihagen.dbis.anguillasearch.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.awt.image.BufferedImage;
import java.awt.Color;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.view.mxGraph;

import de.fernunihagen.dbis.anguillasearch.crawler.Crawler;
import de.fernunihagen.dbis.anguillasearch.crawler.WebsiteData;
import de.fernunihagen.dbis.anguillasearch.searching.SearchEngine;

/**
 * This class creates four cicrled graphs of some predefined net of urls which
 * are linked to one another. It then performes a search with a predefined
 * search query and creates four graphs, one with the nodes representing the
 * websites and the edges representing the links between those websites. The
 * second graph is almost the same but in addition shows on the labels of
 * the nodes the TFIDF scores for that website in regard to the search query.
 * It also has a caption node which shows the query tokens which were used
 * during the search. The 3rd graph scales the nodes according to their
 * page rank score, shows the flow of page rank between the nodes on the edges
 * and has an additional explanation in the caption. The fourth graph
 * highlights the top 3 scored nodes in regard to the combined cosine
 * similarity and page rank scores. This graphs are then saved to four
 * separate .png files in the folder figures.
 */
public final class GraphCreator {
    /**
     * A SearchEngine to perform a search.
     */
    private static SearchEngine searchEngine;
    /**
     * A constant that represents a string representation of the file name
     * of the json that will be used to retrieve seed urls and query tokens.
     */
    private static final String JSONFILE = "intranet/cheesy4-a31d2f0d.json";
    /**
     * A constant that represents a List of the parsed json objects of
     * the JSONFILE which gets initialized immediately, by calling the
     * method to parse the json file.
     */
    private static final List<JsonObject> JSONOBJECTS = parseJsons();

    /**
     * Logger for the output or info or error messages.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(
            GraphCreator.class);

    // ============================constructors===========================//

    private GraphCreator() {
    }

    // ==============================methods==============================//

    /**
     * Main method. Creates a picture of with nodes and edges for a predefined
     * net of websites. Where the websites are the nodes and the links
     * between them are the edges.
     *
     * @param args Command line arguments
     */
    public static void main(final String[] args) {
        initializeFields();
        createSimpleGraph();
        createGraphWithTfIdf();
        createGraphWithPageRank();
        createGraphWithCosineAndPageRank();
    }

    /**
     * Initializes a {@link SearchEngine} object with the seed urls and
     * a crawl limit of 16.
     */
    private static void initializeFields() {
        String[] arr = getSeedUrls(JSONOBJECTS);
        searchEngine = new SearchEngine(arr, 16);
    }

    /**
     * Creates a simple graph with nodes named after the urls of
     * the crawled websites and edges representing the links on the site
     * linking to other websites in the graph. The graph is then put
     * into a circle layout and saved to a file in the folder figures which
     * is contained in the topmost level of this project.
     */

    private static void createSimpleGraph() {
        Crawler crawler = searchEngine.getCrawler();

        /* create a mxGraph object and retrieve a parent object */
        mxGraph graph = new mxGraph();
        Object parentNode = graph.getDefaultParent();

        /*
         * initialize map to map url -> node every crawled site represents
         * a node
         */
        Map<String, Object> nodes = new HashMap<>();

        /*
         * map every url to a newly created corresponding node and put
         * it into the map nodes
         */
        for (WebsiteData websiteData : crawler.getCrawledDataAsList()) {
            Object node = graph.insertVertex(parentNode, null, websiteData.
            getUrlOfSite(), 50, 50, 250, 100);
            nodes.put(websiteData.getUrlOfSite(), node);
        }

        /*
         * for every node (url) in the map create an edge to every other
         * node that this node contains a link to.
         */
        for (WebsiteData websiteData : crawler.getCrawledDataAsList()) {
            Object startNode = nodes.get(websiteData.getUrlOfSite());
            /* iterate over every link of the node */
            for (String link : websiteData.getLinks()) {
                /*
                 * if the link target is present in the map add an edge
                 * to that node
                 */
                if (nodes.containsKey(link)) {
                    Object endNode = nodes.get(link);
                    /* create an edge between startNode and endNode */
                    graph.insertEdge(parentNode, null, "", startNode, endNode);
                }
            }
        }
        /*
         * create a circleLayout for the graph and execute it for all children
         * of the parentNode
         */
        mxCircleLayout layout = new mxCircleLayout(graph);
        layout.execute(parentNode);

        /* create a BufferedImage and save it to figures/net-graph.png */
        try {
            BufferedImage image = mxCellRenderer.createBufferedImage(graph,
                    null, 2.0, Color.WHITE, true, null);
            /* create output file */
            String path = "figures/net-graph.png";
            File outputfile = new File(path);
            /* write image to output file */
            ImageIO.write(image, "png", outputfile);

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format("Image was created "
                        + "and saved to net-graph.png"));
            }
        } catch (IOException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error(String.format("Failed to write Image: %s",
                        e.getMessage()));
            }
        }
    }

    /**
     * Creates a graph with nodes named after the urls of
     * the crawled websites and edges representing the links on the site
     * linking to other websites in the graph. A search with the query tokens
     * is performed on the website data and the labels on the nodes of
     * each website will get the corresponding TFIDF value for that search
     * printed on them. A label node showing the query tokens used in the
     * search is then added to the graph. The graph is put into a circle
     * layout and saved to a file in the folder figures which
     * is contained in the topmost level of this project.
     */

    private static void createGraphWithTfIdf() {
        String[] query = {"flavor"};
        List<String> queryTokens = Arrays.asList(query);
        Crawler crawler = searchEngine.getCrawler();
        /* create a mxGraph object and retrieve a parent object */
        mxGraph graph = new mxGraph();
        Object parentNode = graph.getDefaultParent();

        /*
         * initialize map to map url -> node every crawled site represents
         * a node
         */
        Map<String, Object> nodes = new HashMap<>();

        /*
         * get crawled WebsiteData and perform search with the query.
         * Then get a Map of url -> TFIDF score from the search engine
         */
        List<WebsiteData> crawledData = crawler.getCrawledDataAsList();
        searchEngine.searchQuery(query);
        Map<String, Double> tfIdfMap = searchEngine.getTfIdfMap();

        /*
         * map every url to a newly created corresponding node and put
         * it into the map nodes
         */
        for (WebsiteData websiteData : crawledData) {
            String url = websiteData.getUrlOfSite();
            /*
             * Extract the TFIDF score from the map. If it doesn't exist, means
             * it was 0,0 in the search engine and got removed already,
             * initialize the value with 0.0)
             */
            double tfIdfSum;
            if (tfIdfMap.get(url) == null) {
                tfIdfSum = 0.0;
            } else {
                tfIdfSum = tfIdfMap.get(url);
            }

            // create a String for the name and TFIDF value of the node
            String nodeName = url + "\nTFIDF Score: " + String.format("%.10f",
                    tfIdfSum);

            /* create node and put into the nodes map */
            Object node = graph.insertVertex(parentNode, null, nodeName,
                    50, 50, 250, 100);
            nodes.put(url, node);
        }

        /*
         * for every node (url) in the map create an edge to every other
         * node that this node contains a link to.
         */
        for (WebsiteData websiteData : crawler.getCrawledDataAsList()) {
            Object startNode = nodes.get(websiteData.getUrlOfSite());
            /* iterate over every link of the node */
            for (String link : websiteData.getLinks()) {
                /*
                 * if the link target is present in the map add an edge
                 * to that node
                 */
                if (nodes.containsKey(link)) {
                    Object endNode = nodes.get(link);
                    /* create an edge between startNode and endNode */
                    graph.insertEdge(parentNode, null, "", startNode, endNode);
                }
            }
        }

        /*
         * create a circleLayout for the graph and execute it for all children
         * of the parentNode
         */
        mxCircleLayout layout = new mxCircleLayout(graph);
        layout.execute(parentNode);

        /* create caption with searchquery */
        String caption = "Search query: " + String.join(" ", queryTokens);
        graph.insertVertex(parentNode, null, caption, 0, 0, 550,
                100);

        /*
         * create a BufferedImage and save it to figures/"search
         * query"-net-graph.png
         */
        try {
            BufferedImage image = mxCellRenderer.createBufferedImage(graph,
                    null, 2.0, Color.WHITE, true, null);
            /* create output file */
            String nameOfFile = String.join("-", queryTokens);
            String path = "figures/" + nameOfFile + "-net-graph.png";
            File outputfile = new File(path);
            /* write image to output file */
            ImageIO.write(image, "png", outputfile);

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format("Image was created "
                        + "and saved to %s", path));
            }
        } catch (IOException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error(String.format("Failed to write Image: %s",
                        e.getMessage()));
            }
        }
    }

    /**
     * Creates a graph with nodes named after the urls of
     * the crawled websites and edges representing the links on the site
     * linking to other websites in the graph. A search with the query tokens
     * is performed on the website data- The nodes get scaled according to
     * the pagerank of the node and the edges show the flow of page rank score
     * from one node to another. The graph is put into a circle
     * layout and saved to a file in the folder figures which
     * is contained in the topmost level of this project.
     */
    private static void createGraphWithPageRank() {
        /* initialize query */
        String[] query = {"flavor"};
        List<String> queryTokens = Arrays.asList(query);
        /* inizialize crawler and a list of crawled WebsiteData */
        Crawler crawler = searchEngine.getCrawler();
        List<WebsiteData> crawledData = crawler.getCrawledDataAsList();
        /* retrieve a map of url -> pagerank from the search engine */
        Map<String, Double> pageRankMap = searchEngine.getPageRank().
        getPageRanksPerUrl();
        /* create a mxGraph object and retrieve a parent object */
        mxGraph graph = new mxGraph();
        Object parentNode = graph.getDefaultParent();

        /*
         * initialize map to map url -> node every crawled site represents
         * a node
         */
        Map<String, Object> nodes = new HashMap<>();

        /*
         * get minimum and maximum pagerank values so to normalize
         * the nodesize
         */
        double minimum = Double.MAX_VALUE;
        double maximum = Double.MIN_VALUE;
        for (Map.Entry<String, Double> entry : pageRankMap.entrySet()) {
            double pageRank = entry.getValue();
            if (pageRank < minimum) {
                minimum = pageRank;
            }
            if (pageRank > maximum) {
                maximum = pageRank;
            }
        }

        /*
         * map every url to a newly created corresponding node and put
         * it into the map nodes
         */
        for (WebsiteData websiteData : crawledData) {
            String url = websiteData.getUrlOfSite();
            /*
             * if pagerank contains url set pagerank to the corresponding number
             * else leave it on 0.0
             */
            double pageRank = 0.0;
            if (pageRankMap.containsKey(url)) {
                pageRank = pageRankMap.get(url);
            }

            /* scale the nodes (with normalization) */
            int minimumSize = 50;
            double nodeLength = minimumSize + 300.0 * (pageRank - minimum)
                    / (maximum - minimum);

            /* label for the nodes */
            String nodeLabel = String.format("%s%nPagerank=%.5f", url,
                    pageRank);
            /* create nodes and put into node map */
            Object node = graph.insertVertex(parentNode, null, nodeLabel,
                    50, 50, nodeLength, nodeLength);
            nodes.put(url, node);
        }

        /*
         * for every node (url) in the map create an edge to every other
         * node that this node contains a link to.
         */
        for (WebsiteData websiteData : crawler.getCrawledDataAsList()) {
            String url = websiteData.getUrlOfSite();
            Object startNode = nodes.get(websiteData.getUrlOfSite());
            /* get page rank for this url */
            double pageRank = 0.0;
            if (pageRankMap.containsKey(url)) {
                pageRank = pageRankMap.get(url);
            }
            /* get amount of outgoing edges on this url */
            int outgoingEdges = websiteData.getLinks().size();

            /* calculate the outgoing page rank flow per edge for this node */
            double pageRankFlowPerEdge = 0.0;
            if (outgoingEdges > 0) {
                pageRankFlowPerEdge = pageRank / outgoingEdges;
            }

            /* iterate over every link of the node */
            for (String link : websiteData.getLinks()) {
                /*
                 * if the link target is present in the map add an edge
                 * to that node
                 */
                if (nodes.containsKey(link)) {
                    Object endNode = nodes.get(link);
                    /* create an edge between startNode and endNode */
                    String label = String.format("%.5f",
                            pageRankFlowPerEdge);
                    graph.insertEdge(parentNode, null, label, startNode,
                            endNode);
                }
            }
        }

        /*
         * create a circleLayout for the graph and execute it for all children
         * of the parentNode
         */
        mxCircleLayout layout = new mxCircleLayout(graph);
        layout.execute(parentNode);

        /*
         * create caption for the image with search query and a short
         * explanation
         */
        String caption = "Search query:" + String.join(" ", queryTokens)
                + "\nNode size scales with pagerank\n" + "Edges "
                + "show the flow of page rank score a->b\n";
        graph.insertVertex(parentNode, null, caption,
                0, 0, 550, 100);

        /*
         * create a BufferedImage and save it to figures/"search
         * query"-page-rank-net-graph.png
         */
        try {
            BufferedImage image = mxCellRenderer.createBufferedImage(graph,
                    null, 2.0, Color.WHITE, true, null);
            /* create output file */
            String nameOfFile = String.join("-", queryTokens);
            String path = "figures/" + nameOfFile
                    + "-page-rank-net-graph.png";
            File outputFile = new File(path);
            /* write image to output file */
            ImageIO.write(image, "png", outputFile);

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format("Image was created "
                        + "and saved to %s", path));
            }
        } catch (IOException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error(String.format("Failed to write Image: %s",
                        e.getMessage()));
            }
        }
    }

    /**
     * Creates a graph with nodes named after the urls of
     * the crawled websites and edges representing the links on the site
     * linking to other websites in the graph. A search with the query tokens
     * is performed on the website data with the comination of page rank
     * and cosine scores. The top 3 Nodes with the highest score
     * get emphasized by red color. The graph is put into a circle
     * layout and saved to a file in the folder figures which
     * is contained in the topmost level of this project.
     */
    private static void createGraphWithCosineAndPageRank() {
        /* initialize query */
        String[] query = {"flavor"};
        List<String> queryTokens = Arrays.asList(query);
        /* inizialize crawler and a list of crawled WebsiteData */
        Crawler crawler = searchEngine.getCrawler();
        List<WebsiteData> crawledData = crawler.getCrawledDataAsList();

        /*
         * Get list and map with the results from the search with cosine and
         * pagerank. list (url) and map (url -> score), both sorted
         * in descending order according to score.
         */
        List<String> sortedResults = searchEngine.
        searchQueryPageRankAndCosine(query);
        Map<String, Double> cosineAndPageRankMap = searchEngine.
        getCosineAndPagerankMap();

        /* Create a mxGraph object and retrieve a parent object. */
        mxGraph graph = new mxGraph();
        Object parentNode = graph.getDefaultParent();
        /*
         * Initialize map to map url -> node every crawled site represents
         * a node.
         */
        Map<String, Object> nodes = new HashMap<>();
        /*
         * Extract the top 3 websites from the sorted map of combined page
         * rank and cosine scores.
         */
        List<String> top3ScoredWebsites = new ArrayList<>();
        for (String url : sortedResults) {
            if (top3ScoredWebsites.size() < 3) {
                top3ScoredWebsites.add(url);
            } else {
                break;
            }
        }

        /*
         * map every url to a newly created corresponding node and put
         * it into the map nodes, label it with the corresponding score
         */
        for (WebsiteData websiteData : crawledData) {
            String url = websiteData.getUrlOfSite();
            double cosineAndPageRankScore = 0.0;
            if (cosineAndPageRankMap.containsKey(url)) {
                cosineAndPageRankScore = cosineAndPageRankMap.get(url);
            }
            /* label the nodes */
            String label = String.format("%s%nScore=%.5f", url,
             cosineAndPageRankScore);
            /*
             * if map is contained in top 3 set style to red color
             * else to white
             */
            String color = "fillColor=#FF9999";
            if (!top3ScoredWebsites.contains(url)) {
                color = "fillColor=#FFFFFF";
            }
            /* create node and put into the nodes map */
            Object node = graph.insertVertex(parentNode, null, label, 50,
             50, 250, 100, color);
            nodes.put(url, node);
        }

        /*
         * for every node (url) in the map create an edge to every other
         * node that this node contains a link to.
         */
        for (WebsiteData websiteData : crawledData) {
            Object startNode = nodes.get(websiteData.getUrlOfSite());
            /* iterate over every link of the node */
            for (String link : websiteData.getLinks()) {
                /*
                 * if the link target is present in the map add an edge
                 * to that node
                 */
                if (nodes.containsKey(link)) {
                    Object endNode = nodes.get(link);
                    /* create an edge between startNode and endNode */
                    graph.insertEdge(parentNode, null, "", startNode, endNode);
                }
            }
        }

        /*
         * create a circleLayout for the graph and execute it for all children
         * of the parentNode
         */
        mxCircleLayout layout = new mxCircleLayout(graph);
        layout.execute(parentNode);

        /*
         * create caption for the image with search query and a short
         * explanation
         */
        String caption = "Search query:" + String.join(" ", queryTokens)
                + "\nNode labels = combined score of cosine and pagerank\n"
                + "Top 3 scored Nodes are highlited in red\n";
        graph.insertVertex(parentNode, null, caption,
                0, 0, 550, 100);

        /*
         * create a BufferedImage and save it to figures/"search
         * query"-page-rank-net-graph.png
         */
        try {
            BufferedImage image = mxCellRenderer.createBufferedImage(graph,
                    null, 2.0, Color.WHITE, true, null);
            /* create output file */
            String nameOfFile = String.join("-", queryTokens);
            String path = "figures/" + nameOfFile
                    + "-top3-net-graph.png";
            File outputFile = new File(path);
            /* write image to output file */
            ImageIO.write(image, "png", outputFile);

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format("Image was created "
                        + "and saved to %s", path));
            }
        } catch (IOException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error(String.format("Failed to write Image: %s",
                        e.getMessage()));
            }
        }

    }

    /**
     * Parses a predefined json file into a {@link List} of {@link JsonObject}.
     *
     * @return the list of json objects.
     */
    private static List<JsonObject> parseJsons() {
        List<JsonObject> testJSONs = new ArrayList<>();
        try {
            testJSONs.add(Utils.parseJSONFile(JSONFILE));
        } catch (IOException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error(String.format("Failed to parse Jsons: %s",
                        e.getMessage()));
            }
        }
        return testJSONs;
    }

    /**
     * Extracts the seed urls from a {@link List} of {@link JsonObject}.
     *
     * @param testJSONs A list of {@link JsonObject} to be processed.
     * @return A {@code String[]} representing the seed urls of the
     *         provided {@link List} of {@link JsonObject}.
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

    /**
     * Extracts the query tokens from a {@link List} of {@link JsonObject}.
     *
     * @param testJSONs A list of {@link JsonObject} to be processed.
     * @return A {@code String[]} representing the query tokens of the
     *         provided {@link List} of {@link JsonObject}.
     */
    private static String[] getQueryTokens(final List<JsonObject> testJSONs) {
        List<String> urlList = new ArrayList<>();
        for (JsonObject testJSON : testJSONs) {
            String[] seedUrls = new Gson().fromJson(testJSON.get(
                    "Query-Token"), String[].class);
            urlList.addAll(Arrays.asList(seedUrls));
        }
        return urlList.toArray(new String[urlList.size()]);
    }
}
// ===========================getter/setter===========================//
