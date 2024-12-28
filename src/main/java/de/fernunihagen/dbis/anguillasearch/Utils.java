package de.fernunihagen.dbis.anguillasearch;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.io.File;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.Optional;

/**
 * This is a utility class that provides some helper methods for the tests.
 */
public final class Utils {

    private Utils() {
    }

    /**
     * This method parses a JSON file.
     *
     * @param fileName the Name of the File to be parsed
     *
     * @return the parsed object as a JsonObject
     *
     * @throws IOException If file can't be read.
     */
    public static JsonObject parseJSONFile(final String fileName)
            throws IOException {
        Path path = Paths.get(fileName);
        // Read the file content using try-with-resources
        String content = Files.readString(path);
        // Parse the JSON content
        return new Gson().fromJson(content, JsonObject.class);
    }

    /**
     * This method parses multiple JSON files from a folder.
     *
     * @param folderName The Name of the Folder where the Files should be parsed
     *
     * @return the parsed files as a List of JsonObjects.
     *
     * @throws IOException If files in the folder can't be read, the folder
     *                     doesn't exist or something similar.
     */
    public static List<JsonObject> parseAllJSONFiles(
            final Optional<String> folderName) throws IOException {
        String fN = folderName.orElse("intranet");
        List<JsonObject> jsonObjects = new ArrayList<>();
        // Get all files in the folder
        File folder = new File(fN);
        File[] files = folder.listFiles();
        // Iterate over all files
        if (files != null) {
            for (File file : files) {
                // Parse the JSON file
                jsonObjects.add(parseJSONFile(file.getAbsolutePath()));
            }
        }
        return jsonObjects;
    }
}
