package de.fernunihagen.dbis.anguillasearch;

import java.util.HashSet;
import java.util.Set;

/**
 * The {@code WebsiteData} class is a datastructure which stores the
 * extracted data from a crawled html page.
 * It stores the Url of the page, its title, header, body content, and a
 * set of absolute links found on the page.
 * It is possible to set and get the page's title, header,
 * body and a set of links.
 * Create an instance of {@code WebsiteData} with
 * the Url of the web page as argument, and
 * use the methods to set or get the data.
 */

public class WebsiteData {

    /**
 * URL of the website an instancs of this class represents.
 */
private String urlOfSite;

/**
 * Title of the website an instancs of this class represents.
 */
private String title;

/**
 * Header of the website an instancs of this class represents.
 */
private String header;

/**
 * Body of the website an instancs of this class represents.
 */
private String body;

/**
 * Links on the website an instancs of this class represents.
 */
private Set<String> links;

    // ============================constructors===========================//

    /**
     * Creates a new {@code WebsiteData} object for a given Url.
     *
     * @param url the Url of the html page which this object stands for.
     */

    public WebsiteData(final String url) {
        this.urlOfSite = url;
        this.links = new HashSet<>();
    }

    // ==============================methods==============================//

    // ============================getter/setter============================//

    /**
     * Sets the title of the website.
     *
     * @param newTitle the new title to be set
     */
    public void setTitle(final String newTitle) {
        this.title = newTitle;
    }

    /**
     * Sets the header of the website.
     *
     * @param newHeader the new header to be set
     */
    public void setHeader(final String newHeader) {
        this.header = newHeader;
    }

    /**
     * Sets the body of the website.
     *
     * @param newContent the new body content to be set
     */
    public void setBody(final String newContent) {
        this.body = newContent;
    }

    /**
     * Adds a new link to the set of links.
     *
     * @param newLink the new link to be added
     */
    public void addLink(final String newLink) {
        this.links.add(newLink);
    }

    /**
     * Retrieves the Url of the site.
     *
     * @return the Url of the site
     */
    public String getUrlOfSite() {
        return urlOfSite;
    }

    /**
     * Retrieves the title of the content.
     *
     * @return the title of the content
     */
    public String getTitle() {
        return title;
    }

    /**
     * Retrieves the header of the website.
     *
     * @return the header of the website
     */
    public String getHeader() {
        return header;
    }

    /**
     * Retrieves the body of the website.
     *
     * @return the body of the website
     */
    public String getBody() {
        return body;
    }

    /**
     * Retrieves a set of all links.
     *
     * @return a copy of the set of links
     */
    public Set<String> getLinks() {
        return new HashSet<>(links);
    }
}
