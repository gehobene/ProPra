package de.fernunihagen.dbis.anguillasearch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for cosine similarity.
 */
class CosineTests {

    @Test
    void equalVectors() {
        // Create two vector with random positive double values;
        double[] vectorA = { 0.1, 0.2, 0.3, 0.4, 0.5 };
        double[] vectorB = { 0.1, 0.2, 0.3, 0.4, 0.5 };

        //create maps
        Map<String, Double> map1 = new HashMap<>();
        Map<String, Double> map2 = new HashMap<>();

        // iterate over arrays and put them into the maps with ascending keys
        String keyString = null;
        int key = 1;
        for (double value : vectorA) {
           keyString = String.valueOf(key++);
            map1.put(keyString, value);
        }
        String keyString2 = null;
        int key2 = 1;
        for (double value : vectorB) {
           keyString2 = String.valueOf(key2++);
            map2.put(keyString2, value);
        }
        //generate TokenVectors from the maps
        TokenVector tokenVectorA = new TokenVector(map1);
        TokenVector tokenVectorB = new TokenVector(map2);

        // The cosine similarity of two equal vectors should be 1.0
        // Replace the cosineSimilarity method with your implementation
        assertEquals(1.0, tokenVectorA.computeCosineSimilarity(tokenVectorB));

    }

    @Test
    void orthogonalVectors() {
        // Create two orthogonal vectors
        double[] vectorA = { 1.0, 0.0, 0.0 };
        double[] vectorB = { 0.0, 1.0, 0.0 };

        //create maps
        Map<String, Double> map1 = new HashMap<>();
        Map<String, Double> map2 = new HashMap<>();

        // iterate over arrays and put them into the maps with ascending keys
        String keyString = null;
        int key = 1;
        for (double value : vectorA) {
           keyString = String.valueOf(key++);
            map1.put(keyString, value);
        }
        String keyString2 = null;
        int key2 = 1;
        for (double value : vectorB) {
           keyString2 = String.valueOf(key2++);
            map2.put(keyString2, value);
        }
        //generate TokenVectors from the maps
        TokenVector tokenVectorA = new TokenVector(map1);
        TokenVector tokenVectorB = new TokenVector(map2);
        // The cosine similarity of two orthogonal vectors should be 0.0
        // Replace the cosineSimilarity method with your implementation
        assertEquals(0.0, tokenVectorA.computeCosineSimilarity(tokenVectorB));

     
    }

    @Test
    void randomVectors() {
        // Create two random vectors
        double[] vectorA = { 0.1, 0.2, 0.3, 0.4, 0.5 };
        double[] vectorB = { 0.5, 0.4, 0.3, 0.2, 0.1 };
        // The cosine similarity of two random positive vectors should be between 0.0
        // and 1.0

         //create maps
         Map<String, Double> map1 = new HashMap<>();
         Map<String, Double> map2 = new HashMap<>();
 
         // iterate over arrays and put them into the maps with ascending keys
         String keyString = null;
         int key = 1;
         for (double value : vectorA) {
            keyString = String.valueOf(key++);
             map1.put(keyString, value);
         }
         String keyString2 = null;
         int key2 = 1;
         for (double value : vectorB) {
            keyString2 = String.valueOf(key2++);
             map2.put(keyString2, value);
         }
         //generate TokenVectors from the maps
         TokenVector tokenVectorA = new TokenVector(map1);
         TokenVector tokenVectorB = new TokenVector(map2);
        // Replace the cosineSimilarity method with your implementation
        assertTrue(tokenVectorA.computeCosineSimilarity(tokenVectorB) > 0.0);
        assertTrue(tokenVectorA.computeCosineSimilarity(tokenVectorB) < 1.0);

    }

    @Test
    void specificResults() {
        // Create two vectors with specific values
        double[] vectorA = { 0.1, 0.2, 0.3, 0.4, 0.5 };
        double[] vectorB = { 0.5, 0.4, 0.3, 0.2, 0.1 };

         //create maps
         Map<String, Double> map1 = new HashMap<>();
         Map<String, Double> map2 = new HashMap<>();
 
         // iterate over arrays and put them into the maps with ascending keys
         String keyString = null;
         int key = 1;
         for (double value : vectorA) {
            keyString = String.valueOf(key++);
             map1.put(keyString, value);
         }
         String keyString2 = null;
         int key2 = 1;
         for (double value : vectorB) {
            keyString2 = String.valueOf(key2++);
             map2.put(keyString2, value);
         }
         //generate TokenVectors from the maps
         TokenVector tokenVectorA = new TokenVector(map1);
         TokenVector tokenVectorB = new TokenVector(map2);
        // The cosine similarity of these vectors should be 0.7
        // Replace the cosineSimilarity method with your implementation
        assertTrue(Math.abs(tokenVectorA.computeCosineSimilarity(tokenVectorB) - 0.6364) < 0.0001);

      
    }
}