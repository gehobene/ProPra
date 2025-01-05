package de.fernunihagen.dbis.anguillasearch;

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

/**
 * This class creates a cicrled graph of some predefined net of urls which
 * are linked to one another. The nodes represent the websites and the edges
 * represent the links between those websites. This graph is then saved to a
 * .png file in the folder figures.
 */
public final class GraphCreator {

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
        /* create crawler with crawl limit 16 */
        Crawler crawler = new Crawler(16);
        /*
         * parse json, retrieve seedurls as a list, give it to crawler and then
         * crawl the pages
         *
         */
        crawler.crawl(Arrays.asList(getSeedUrls(parseJsons())));

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
            String path = "home/vscode/workspace/figures/net-graph.png";
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
     * Parses a predefined json file int a {@link List} of {@link JsonObject}.
     *
     * @return the list of json objects.
     */
    private static List<JsonObject> parseJsons() {
        List<JsonObject> testJSONs = new ArrayList<>();
        try {
            testJSONs.add(Utils.parseJSONFile("intranet/"
                    + "cheesy4-a31d2f0d.json"));
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
// ===========================getter/setter===========================//
