package de.fernunihagen.dbis.anguillasearch;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code PageRank} class calculates the page rank for a list
 * of crawled websites which it takes as an argument in the form of a
 * {@link List} of {@link WebsiteData} objects . It calculates the imporance
 * of the website baed on the amount of incoming links to this site
 * iteratively. The algorithm stops when two consecutive iterations
 * differ by 0.0001 in total.
 */
public class PageRank {

    /**
     * Logger for the output or info or error messages.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(
            AnguillaSearch.class);

    /**
     * A map of urls and their corresponding page rank.
     * url -> page rank
     */
    private Map<String, Double> pageRanksPerUrl;

    /**
     * A map of urls and their sets of corresponding incoming urls.
     * (urls that link to the key url) url -> set(sites that link to url)
     */
    private Map<String, Set<String>> incomingLinksPerUrl;

    /**
     * A map of urls and their count of outgoing links.
     * map -> amount of outgoing links
     */
    private Map<String, Integer> outgoingLinksCount;

    /**
     * A list of {@link WebsiteData} that contains all the crawled Data.
     */
    private List<WebsiteData> crawledData;

    // ============================constructors===========================//
    /**
     * Creates a new {@code PageRank} object for a given list of
     * {@link WebsiteData}.
     *
     * @param websites the List of crawled website content in the form of
     *                 {@link WebsiteData} objects.
     */
    public PageRank(final List<WebsiteData> websites) {
        /* initialization of fields */
        this.crawledData = new ArrayList<>(websites);
        this.incomingLinksPerUrl = new HashMap<>();
        this.outgoingLinksCount = new HashMap<>();
        this.pageRanksPerUrl = new HashMap<>();
        /*
         * calculation of page rank, try catch in case something goes
         * wrong. Mitigation of half initialized object.
         */
        try {
            calculateIncomingLinks();
            initializePageRank();
            calculatePageRanks();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "IndexBuilder could not be initialized",
                    e);
        }
    }

    // ==============================methods==============================//
    /**
     * Creates a map of incoming links for every url in the crawled data.
     */
    private void calculateIncomingLinks() {
        /*
         * initializes the incoming links map for every url to make sure
         * that even urls without incoming links are present
         */
        for (WebsiteData site : crawledData) {
            incomingLinksPerUrl.put(site.getUrlOfSite(), new HashSet<>());
        }

        /*
         * iterates over every site and maps its url to all existing incoming
         * links to that site in the crawled data. url -> set(sites that
         * linkto url).
         * Also maps every site to the amount of outgoing links of that
         * site. url -> amount of links on that site.
         */
        for (WebsiteData site : crawledData) {
            String url = site.getUrlOfSite();
            /* maps url to the amount of outgoing links of that url */
            int outgoingLinksAmount = site.getLinks().size();
            /* only if the site has at least 1 outgoing link */
            if (outgoingLinksAmount >= 1) {
                outgoingLinksCount.put(url, outgoingLinksAmount);
            }

            /*
             * map url as incoming link to every site that url links to
             * (outgoing links). site -> set(url as incoming link) for
             * every outgoing link of url.
             */
            for (String outgoingLink : site.getLinks()) {
                /*
                 * if the outgoing link doesnt exist in the map yet
                 * initialize an entry
                 */
                incomingLinksPerUrl.computeIfAbsent(outgoingLink, k -> new
                HashSet<>());
                /*
                 * map outgoing link of url -> url. For outgoing link of url
                 * url is now an incoming link.
                 */
                incomingLinksPerUrl.get(outgoingLink).add(url);
            }
        }
    }

    /**
     * Initialized the pagerank for every website with 1/N with N being the
     * total amount of websites crawled.
     */
    private void initializePageRank() {
        int n = crawledData.size();
        double pageRank = 1.0 / n;

        for (WebsiteData url : crawledData) {
            pageRanksPerUrl.put(url.getUrlOfSite(), pageRank);
        }
    }

    /**
     * calculates the page rank iteratively until the difference between two
     * iterations is less than 0.0001.
     * Formula:
     * PageRank of Site (i) = sum of all incoming links to i(PageRank of
     *  Site (j) / amounts of sites that j links to)
     */
    private void calculatePageRanks() {
        /* initialize with 1 so that while loop starts */
        double diffBetweenIterations = 1.0;

        while (diffBetweenIterations > 0.0001) {

            Map<String, Double> newPageRanks = new HashMap<>();

            /*
             * calculates the page rank for every url and puts it into
             * newPageRanks map (url -> pagerank).
             */
            for (Map.Entry<String, Set<String>> entry : incomingLinksPerUrl.
            entrySet()) {
                String url = entry.getKey();
                Set<String> incomingLinks = entry.getValue();
                double pageRank = 0.0;
                /*
                 * iterates over every incoming link for the current url
                 * and calculates the pagerank for url. empty sets of
                 * urls that don't have any incoming urls get ignored-
                 */
                for (String incomingLink : incomingLinks) {
                    int amountOutgoingLinks;
                    /*
                     * gets the amount of outgoing links of that incoming
                     * link of url
                     */
                    amountOutgoingLinks = outgoingLinksCount.get(incomingLink);
                    /*
                     * calculates the total pagerank for for url by
                     * summarizing (pagerank of incoming link / amount
                     * of outgoing links of that link)
                     */
                    pageRank += pageRanksPerUrl.get(incomingLink)
                    / amountOutgoingLinks;
                }
                /* puts the newlycalculated pagerank to the temporary map */
                newPageRanks.put(url, pageRank);
            }

            /*
             * calculates a convergence value between the previous and
             * current iteration if it is less than 0.0001 the loop will exit.
             */
            diffBetweenIterations = calculateConvergence(newPageRanks);

            /*
             * print the pageranks for this net and the total difference
             * of pageranks. Iterantions stop if the total difference of
             * pageranks from the previous iteration minus the current
             * iteration is less than 0.0001.
             */
            printText(diffBetweenIterations, newPageRanks);

            /* update values in the pernanent pageranks map */
            pageRanksPerUrl.clear();
            pageRanksPerUrl.putAll(newPageRanks);

        }
    }

    /**
     * Calculates the total difference between the current and previous
     * page rank calculations.
     *
     * @param pageRanks A map of the current iteration of urls mapped to the
     *                  pagerank. (url -> pagerank)
     * @return The total difference the two iterations.
     */

    private double calculateConvergence(final Map<String, Double> pageRanks) {
        double diffBetweenIterations = 0.0;
        /* iterates over every url of the current pagerank iteration map */
        for (Map.Entry<String, Double> entry : pageRanks.entrySet()) {
            String url = entry.getKey();
            /*
             * sets the value for current page rank iteration for the url
             * and for the previous one
             */
            double newPagerank = entry.getValue();
            double oldPagerank = pageRanksPerUrl.get(url);
            /* calculates the absolute difference between the two */
            diffBetweenIterations += Math.abs(newPagerank - oldPagerank);
        }
        return diffBetweenIterations;

    }

    /**
     * Prints out the total difference between the current and last iteration
     * in pagerankscore, as well as the acutall pagerank for every url in the
     * form of url --> pagerank.
     * Method is used during each iteration of pagerank calculation
     *
     * @param difference The total difference between two iterations.
     * @param pageRanks  A map of urls mapped to their current page rank
     *                   values. (url -> pagerank)
     */

    private void printText(final double difference, final Map<String, Double>
    pageRanks) {
        if (LOGGER.isInfoEnabled()) {
            /*
             * prints out the difference between 2 consecutive iterations
             * of pagerank calculation
             */
            LOGGER.info(String.format("Difference: %.5f", difference));
            /* iterates over every url and prints out url --> pagerank */
            for (Map.Entry<String, Double> entry : pageRanks.entrySet()) {
                String url = entry.getKey();
                double pagerank = entry.getValue();
                LOGGER.info(String.format("   %s --> Pagerank=%.5f", url,
                 pagerank));
            }
        }

    }
    // ============================getter/setter============================//

    /**
     * Returns a mapping of urls to their respective page rank.
     *
     * @return A map with a mapping url -> page rank
     */
    public Map<String, Double> getPageRanksPerUrl() {
        return new HashMap<>(pageRanksPerUrl);
    }
}
