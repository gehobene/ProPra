package de.fernunihagen.dbis.anguillasearch;

import java.util.HashMap;
import java.util.Map;

/**
 * This class represents a vector of tokens mapped to their corresponding
 * TFIDF values.
 *
 * <p>
 * This class can calculate the cosine similarity between two vectors.
 * </p>
 *
 */

public final class TokenVector {

    /**
     * Contains a mapping of every Token to its corresponding TFIDF value.
     */
    private final Map<String, Double> tfIdfPerToken;

    // ============================constructors===========================//

    /**
     * Creates a new {@code TokenVector} object for a given map
     * of tokens -> TFIDF scores.
     *
     * @param tokenTfIdf the pageLimit at which the crawler stops crawling
     *                  outgoing links recursively.
     */

    public TokenVector(final Map<String, Double> tokenTfIdf) {
        this.tfIdfPerToken = new HashMap<>(tokenTfIdf);
    }
    // ==============================methods==============================//

    /**
     * This method calculates the cosine similarity between two given vectors
     * the formula is as follows.
     *
     * <pre>
     * Formula:
     * cosinue similarity = (a·b) / (||a|| * ||b||).
     * a·b = sum( a_i * b_i )
     * ||a|| = sqrt(sum((a_i)^2))
     * i = 1 - n
     * </pre>
     *
     * @param vectorB the second vector for the calculation.
     *                If vectorB is {@code null}, the method returns 0.0.
     *
     * @return the cosine similarity between the current vector (this) and
     *         {@code vectorB}.
     *         Returns 0.0 if {@code vectorB} is null or one of the vectors
     *         has a denominator of zero.
     */
    public double computeCosineSimilarity(final TokenVector vectorB) {
        /*
         * if vector b is null then return 0.0 as the value for the
         * cosine similarity.
         */
        if (vectorB == null) {
            return 0.0;
        }
        /* retrieve the map (vector) for vector B */
        Map<String, Double> tfIdfPerTokenVectorB = vectorB.getTfIdfMap();

        /* calculates the dot product of vector A and vector B */
        double dotProduct = 0.0;
        /* for every token in vector A (this) */
        for (Map.Entry<String, Double> entry : tfIdfPerToken.entrySet()) {
            /* get key and value of vector A for this entry */
            String token = entry.getKey();
            double tfIdfA = entry.getValue();
            double tfIdfB;
            /*
             * if this token also exists in vector B initialize it with
             * the corresponding value else initialize it with 0.0
             */
            if (tfIdfPerTokenVectorB.containsKey(token)) {
                tfIdfB = tfIdfPerTokenVectorB.get(token);
            } else {
                tfIdfB = 0.0;
            }
            /*
             * calculate the dotproduct for this token and add it to the
             * total sum
             */
            dotProduct += (tfIdfA * tfIdfB);
        }

        /* calculate the denominator of the formula */
        double amountA = calculateDenominator(tfIdfPerToken);
        double amountB = calculateDenominator(tfIdfPerTokenVectorB);
        /*
         * if any one of the denominators is 0.0 return 0.0
         */
        if (amountA == 0.0 || amountB == 0.0) {
            return 0.0;
        }
        /* calculate and return the cosine similarity */
        return dotProduct / (amountA * amountB);
    }

    /**
     * Calculates the amouont of a vector who is represented as a map
     * (token -> TFIDF scores).
     *
     * The amount (||v||) of a vector is calculated as follows.
     *
     * <pre>
     * Formula:
     * ||v|| = sqrt(sum((w_i)^2))
     *  i = 1 - n
     * </pre>
     *
     * @param tfIdfTokenMap a map where keys are tokens and values are their
     *                      corresponding TFIDF scores.
     * @return the amount of the vector as a double.
     */
    private double calculateDenominator(final Map<String, Double>
    tfIdfTokenMap) {
        double result = 0.0;
        /*
         * for every value in the map multiply with itself and add to
         * the total result
         */
        for (double w : tfIdfTokenMap.values()) {
            result += w * w;
        }
        /* return the square root of result */
        return Math.sqrt(result);
    }
    // ============================getter/setter============================//

    /**
     * Returns a mapping of every Token to its corresponding TFIDF value.
     * Basically the vector.
     *
     * @return A map with a mapping token -> TFIDF value. (the vector)
     */
    public Map<String, Double> getTfIdfMap() {
        return new HashMap<>(tfIdfPerToken);
    }

}
