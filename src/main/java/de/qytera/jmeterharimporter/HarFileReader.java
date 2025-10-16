package de.qytera.jmeterharimporter;

import de.sstoehr.harreader.HarReader;
import de.sstoehr.harreader.HarReaderException;
import de.sstoehr.harreader.model.Har;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for reading HAR (HTTP Archive) files from disk.
 */
public final class HarFileReader {

    private static final Logger LOGGER = Logger.getLogger(HarFileReader.class.getName());

    private HarFileReader() {
        // Private constructor to prevent instantiation
    }

    /**
     * Reads a HAR file from the specified file path and parses it into a {@link Har} object.
     *
     * @param filePath the path to the HAR file
     * @return the parsed {@link Har} object
     * @throws RuntimeException if reading or parsing the HAR file fails
     */
    public static Har readHarFromFile(String filePath) {
        File file = new File(filePath);
        try {
            LOGGER.fine("Reading HAR file from: " + file.getAbsolutePath());
            return new HarReader().readFromFile(file);
        } catch (HarReaderException e) {
            LOGGER.log(Level.SEVERE, "Failed to read HAR file: " + file.getAbsolutePath(), e);
            throw new RuntimeException("Failed to read HAR file: " + filePath, e);
        }
    }
}
