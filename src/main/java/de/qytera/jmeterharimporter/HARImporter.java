package de.qytera.jmeterharimporter;

import java.nio.file.Files;
import java.nio.file.Paths;
import org.json.JSONObject;
import java.io.IOException;

public class HARImporter {
    private JSONObject json;

    public HARImporter(String filePath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        this.json = new JSONObject(content);
        System.out.println(this.json.getJSONObject("log").getJSONArray("entries").getJSONObject(0).getJSONObject("request").getString("url"));
    }
}
