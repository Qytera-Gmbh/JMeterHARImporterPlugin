package de.qytera.jmeterharimporter;

import de.sstoehr.harreader.HarReader;
import de.sstoehr.harreader.HarReaderException;
import de.sstoehr.harreader.model.Har;
import java.io.File;

public class HarFileReader {
    public static Har readHarFromFile(String filePath) {
        try {
            return new HarReader().readFromFile(new File(filePath));
        } catch (HarReaderException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to read HAR file: " + filePath, e);
        }
    }
}
