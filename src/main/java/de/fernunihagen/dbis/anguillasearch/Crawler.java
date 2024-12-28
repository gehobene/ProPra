package de.fernunihagen.dbis.anguillasearch;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * The {@code Crawler} class capable of crawling html pages starting from a
 * given list of seed Urls.
 * It processes each page by extracting its title, header, body content, and
 * links, and stores the extracted
 * data in a structured format in the datastructure class {@link WebsiteData}.
 * The crawling process is limited by
 * a maximum number of pages to be crawled ({@link #crawlLimit}).
 * Create an instance of the {@code Crawler} class with a specified crawl limit,
 * and call the {@link #crawl(List)} method
 * with a list of seed Urls. The extracted data can be called using the
 * {@link #getCrawledData()} method.
 */

public class Crawler {

    /**
     * Logger for the output or info or error messages.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Crawler.class);
    /**
     * A queue to which the seed-Urls and consequently the links found
     * on crawled sites from the seed-Urls are added. each link will be removed
     * and processed (crawled) indivudially.
     */
    private Queue<String> urlQueue;
    /**
     * Keeps track of the visited Urls during the crawling process so
     * visited sites don't get crawled again.
     */
    private Set<String> visitedUrls;
    /**
     * A map which contains the Url of the crawled sites as key and
     * the crawled data in a structured format
     * in the datastructure class {@link WebsiteData} as value.
     */
    private Map<String, WebsiteData> crawledData;
    /**
     * The limit of how much websites this crawler is allowed to crawl
     * in total.
     */
    private int crawlLimit;

    // ============================constructors===========================//
    /**
     * Creates a new {@code Crawler} object for a given pageLimit.
     *
     * @param pageLimit the pageLimit at which the crawler stops crawling
     *                  outgoing links recursively.
     */

    public Crawler(final int pageLimit) {
        this.urlQueue = new LinkedList<>();
        this.visitedUrls = new HashSet<>();
        this.crawledData = new HashMap<>();
        this.crawlLimit = pageLimit;
    }

    // ==============================methods==============================//

    /**
     * Crawls the given urls and recursively the embedded urls in the sites
     * aswell until the total crawl limit set by {@link crawlLimit}.
     * Extracts the data into the datastructure {@link WebsiteData} which holds
     * title, header, body and links in separate fields.
     *
     * @param seedUrls a List of Urls to start crawling from, they are added
     *                 to a queue to be processed.
     * @return the number of urls crawled.
     *
     *         <p>
     *         The crawling process:
     *         </p>
     *         <ul>
     *         <li>Urls from the provided list are added to a queue.</li>
     *         <li>Each Url in the queue is processed if it hasn't been visited
     *         yet.</li>
     *         <li>Data from the page is extracted and stored in a map,
     *         with the Url as the key and data as value.</li>
     *         <li>New links found on the crawled page are added to the queue if
     *         they haven't been visited.</li>
     *         </ul>
     */

    public int crawl(final List<String> seedUrls) {
        // adds all provided urls from the list to the queue
        urlQueue.addAll(seedUrls);

        while (!urlQueue.isEmpty() && visitedUrls.size() < crawlLimit) {
            // retrieves the first element of the queue
            String url = urlQueue.poll();

            // skips current while iteration if
            // the provided url from the queue was already visited
            if (visitedUrls.contains(url)) {
                continue;
            }

            try {
                Document doc = Jsoup.connect(url).get();

                // extract data from html and put it into the datastructure
                WebsiteData data = parsePage(doc, url);

                // store the extracted data in a map with the source url
                // as key and the datastructure as value
                crawledData.put(url, data);

                // mark the processed url as visited
                visitedUrls.add(url);

                // add new urls from the crawled site to the queue
                for (String link : data.getLinks()) {
                    if (!visitedUrls.contains(link)) {
                        urlQueue.add(link);
                    }
                }
            } catch (MalformedURLException e) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error(String.format("Malformed Url: %s - %s",
                            url, e.getMessage()));
                }
            } catch (HttpStatusException e) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error(String.format("http error: %s - %s",
                            url, e.getMessage()));
                }
            } catch (UnsupportedMimeTypeException e) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error(String.format("Unsupported Mime-type: %s - %s",
                            url, e.getMessage()));
                }
            } catch (SocketTimeoutException e) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error(String.format("Connection-Timeout: %s - %s",
                            url, e.getMessage()));
                }
            } catch (IOException e) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error(String.format("Failed to crawl: %s - %s",
                            url, e.getMessage()));
                }
            }
        }
        return visitedUrls.size(); // returns the number of crawled urls
    }

    /**
     * Parses the given html document and extracts data (title, header,
     * body content, links).
     * The extracted data is stored in a {@link WebsiteData} object.
     *
     * @param doc the html document to parse.
     * @param url The URL of the document being parsed.
     * @return A {@link WebsiteData} object containing the extracted data.
     */

    private WebsiteData parsePage(final Document doc, final String url) {
        WebsiteData data = new WebsiteData(url);

        // extracts title of given site
        String title = doc.title();
        data.setTitle(title);

        // extracts header of given document
        String header = doc.select("header h1").text();
        data.setHeader(header);

        // extracts content of given document
        String content = doc.select("main p").text();
        data.setBody(content);

        // extracts links of given document
        List<Element> links = doc.select("a[href]"); // List of Element
        for (Element link : links) {
            String linkUrl = link.attr("href");
            /*
             * makes sure that the string is not empty
             * or not starting with http
             */
            if (!linkUrl.isEmpty() && linkUrl.startsWith("http")) {

                data.addLink(linkUrl);
            }
        }

        return data;
    }

    // =============================getter/setter=============================//
    /**
     * Retrieves a map of website data of all crawled sites.
     * The key is the url and the value is the data in in a structured format
     * in the datastructure class {@link WebsiteData}
     *
     * @return a copy of the map of website data
     */
    public Map<String, WebsiteData> getCrawledData() {
        return new HashMap<>(crawledData);
    }
}
