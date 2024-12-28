package de.fernunihagen.dbis.anguillasearch;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

/**
 * This is a utility class which can tokenize and lemmatize text data
 * using the StanfordCoreNLP library and then removes stop words from
 * the resulting string.
 */

public final class StringTokenizer {

    /**
     * A static set with stop words which will get removed from the tokenized
     * and lemmatized strings after processing.
     */

    private static final Set<String> STOP_WORDS = Set.of(
            "i", "me", "my", "myself", "we", "our", "ours", "ourselves",
            "you", "your", "yours", "yourself", "yourselves", "he",
            "him", "his", "himself", "she", "her", "hers", "herself",
            "it", "its", "itself", "they", "them", "their", "theirs",
            "themselves", "what", "which", "who", "whom", "this", "that",
            "these", "those", "am", "is", "are", "was", "were", "be",
            "been", "being", "have", "has", "had", "having", "do", "does",
            "did", "doing", "a", "an", "the", "and", "but", "if", "or",
            "because", "as", "until", "while", "of", "at", "by", "for",
            "with", "about", "against", "between", "into", "through",
            "during", "before", "after", "above", "below", "to", "from",
            "up", "down", "in", "out", "on", "off", "over", "under",
            "again", "further", "then", "once", "here", "there", "when",
            "where", "why", "how", "all", "any", "both", "each", "few",
            "more", "most", "other", "some", "such", "no", "nor", "not",
            "only", "own", "same", "so", "than", "too", "very", "s", "t",
            "can", "will", "just", "don", "should", "now", ",", ".", "!",
            "?", ";", ":", "'", "\"", "-", "_", "(", ")", "[", "]", "{",
            "}", "<", ">", "/", "\\", "|", "@", "#", "$", "%", "^", "&",
            "*", "~", "`", "=", "+");

    /**
     * A static instance of the StanfordCoreNLP pipeline for tokenization and
     * lemmatization.
     */
    private static final StanfordCoreNLP PIPELINE;

    // ============================constructors===========================//
    /*
     * A static block where the pipeline gets initialized.
     */
    static {
        Properties properties = new Properties();
        properties.setProperty("annotators", "tokenize,ssplit,pos,lemma");
        PIPELINE = new StanfordCoreNLP(properties);
    }

    private StringTokenizer() {

    }

    // ==============================methods==============================//

    /**
     * Tokenizes and lemmatizes the text data contained in a
     * {@link WebsiteData}
     * object (title,header,body).
     * First it joins all text parts contained in {@link WebsiteData}
     * together in a string then lowercases it. After that the String gets
     * tokenized, lemmatized and lastly stop words are removed.
     *
     * @param data the {@link WebsiteData} object which shall be processed
     * @return a list of processed words (tokens).
     */

    public static List<String> tokenizeAndLemmatize(final WebsiteData data) {
        /*
         * Joining the title, body and header parts of the website to a single
         * string and casting all letters to lowercase
         */
        String titleHeaderBody = String.join(" ", data.getTitle().trim().
        toLowerCase(Locale.ROOT),
                data.getHeader().trim().toLowerCase(Locale.ROOT),
                data.getBody().trim().toLowerCase(Locale.ROOT));
        List<String> tokens = new ArrayList<>();
        /* tokenizes and lemmatizes the String */
        CoreDocument document = PIPELINE.processToCoreDocument(
                titleHeaderBody);
        /* adds only the lemma values of the labels to the tokenlist */
        for (CoreLabel token : document.tokens()) {
            tokens.add(token.lemma());
        }
        tokens.removeAll(STOP_WORDS); // remove stop words
        /* filter out emojis like the cheese emoji (surrogate pairs) */
        tokens.replaceAll(s -> s.replaceAll(
                "[\\uD800-\\uDBFF\\uDC00-\\uDFFF]", ""));
        tokens.removeIf(String::isEmpty);
        return tokens;
    }

    /**
     * Tokenizes and lemmatizes a String.
     * First it lowercases it. After that the String gets
     * tokenized, lemmatized and lastly stop words are removed.
     *
     * @param data the {@link String} object which shall be processed
     * @return a list of processed words (tokens).
     */

    public static List<String> tokenizeAndLemmatize(final String data) {
        String dataToLowerCase = data.trim().toLowerCase(Locale.ROOT);
        List<String> tokens = new ArrayList<>();
        /* tokenizes and lemmatizes the String */
        CoreDocument document = PIPELINE.processToCoreDocument(dataToLowerCase);
        /* adds only the lemma values of the labels to the tokenlist */
        for (CoreLabel token : document.tokens()) {
            tokens.add(token.lemma());
        }
        tokens.removeAll(STOP_WORDS); // remove stop words
        /* filter out emojis like the cheese emoji (surrogate pairs) */
        tokens.replaceAll(s -> s.replaceAll(
                "[\\uD800-\\uDBFF\\uDC00-\\uDFFF]", ""));
        tokens.removeIf(String::isEmpty);
        return tokens;
    }

}
// ===========================getter/setter===========================//
