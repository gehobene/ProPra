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
 * This class creates two cicrled graphs of some predefined net of urls which
 * are linked to one another. It then performes a search with a predefined
 * search query and creates two graphs, one with the nodes representing the
 * websites and the edges representing the links between those websites. The
 * second graph is almost the same but in addition shows on the labels of
 * the nodes the TFIDF scores for that website in regard to the search query.
 * It also has a caption node which shows the query tokens which were used
 * during the search. This graphs are then saved to two separate .png file in
 * the folder figures.
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

        /* create a BufferedImage and save it to figures/net-graph.png */
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
