package destiny;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;

import kong.unirest.GetRequest;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;

/** Unit test for Destiny App. */
public class AppTest {
    /*
     * Ensure the connection to the destiny api is working and getting a response
     */
    @Test
    public void testResponse() throws UnirestException {
        Unirest.config().reset();
        Unirest.config().connectTimeout(10000).socketTimeout(10000);
        HttpResponse<String> response = Unirest
                .get("https://www.bungie.net/Platform/Destiny2/Manifest/DestinyInventoryItemDefinition/{hashIdentifier}/")
                .header("X-API-KEY", "735ad4372078466a8b68a09ff9c02edb")
                .routeParam("hashIdentifier", "3523296417")
                .asString();

        JSONObject itemDefinition = new JSONObject(response.getBody());
        itemDefinition = itemDefinition.getJSONObject("Response");
        itemDefinition = itemDefinition.getJSONObject("displayProperties");

        GetRequest get = Unirest.get(
                "https://www.bungie.net/Platform/Destiny2/Armory/Search/DestinyInventoryItemDefinition/{searchTerm}/")
                .header("X-API-KEY", "735ad4372078466a8b68a09ff9c02edb");
        response = get.routeParam("searchTerm", itemDefinition.getString("name")).asString();

        JSONObject mJsonObject = new JSONObject(response.getBody());
        JSONObject userJObject = mJsonObject.getJSONObject("Response");
        JSONObject statusJObject = userJObject.getJSONObject("results");
        JSONArray resultSet = statusJObject.getJSONArray("results");
        Long key = null, entry = null;
        // ensure that there is only a basic and enhanced trait definiton
        assertEquals(2, resultSet.length());
        for (Object object : resultSet) {
            JSONObject jsonObject = (JSONObject) object;
            if (key == null) {
                key = jsonObject.getLong("hash");
            } else {
                entry = jsonObject.getLong("hash");
            }
        }
        // assert that key and entry are not null
        assertNotNull(key);
        assertNotNull(entry);
        System.out.println("Test testResponse() passed");
    }

    /*
     * Try getting a specific item's name
     */
    @Test
    public void testGetName() throws UnirestException {
        Unirest.config().reset();
        Unirest.config().connectTimeout(10000).socketTimeout(10000);
        HttpResponse<String> response = Unirest
                .get("https://www.bungie.net/Platform/Destiny2/Manifest/DestinyInventoryItemDefinition/{hashIdentifier}/")
                .header("X-API-KEY", "735ad4372078466a8b68a09ff9c02edb")
                .routeParam("hashIdentifier", "4083045006")
                .asString();

        JSONObject itemDefinition = new JSONObject(response.getBody());
        itemDefinition = itemDefinition.getJSONObject("Response");
        itemDefinition = itemDefinition.getJSONObject("displayProperties");

        assertEquals("Persuader", itemDefinition.getString("name"));
        System.out.println("Test testGetName() passed");
    }

    /** Ensure the ability to write to a file is working
     *
     * @throws Exception */
    @Test
    public void writeHashMapToCsv() throws Exception {
        String eol = System.getProperty("line.separator");

        Map<String, String> map = new HashMap<>();
        map.put("Key", "Value");
        map.put("abc", "aabbcc");
        map.put("def", "ddeeff");

        Writer writer = new FileWriter("src/test/data/destiny/test.csv");

        for (Map.Entry<String, String> entry : map.entrySet()) {
            writer.append(entry.getKey())
                    .append(',')
                    .append(entry.getValue())
                    .append(eol);
        }
        writer.flush();
        assertNotNull(writer);
        System.out.println("Test writeHashMapToCsv() passed");
    }

    @Test
    public void testInput() throws Exception {
        Map<String, String> itemMatchingList = new HashMap<>();
        List<String> checkedItemList = new ArrayList<>();
        File file = new File("src/test/data/destiny/mapTest.csv");
        try (Writer writer = new FileWriter(file, false);) {
            writer.append("From").append(',').append("To")
                    .append(System.getProperty("line.separator"));
            writer.flush();
        } catch (Exception er) {
            fail();
        }
        BufferedReader reader = new BufferedReader(new FileReader(file));
        reader.readLine(); // skip the header line
        while (reader.ready()) {
            String item = reader.readLine();
            itemMatchingList.put(item.split(",")[0], item.split(",")[1]);
            checkedItemList.add(item.split(",")[0]);
            checkedItemList.add(item.split(",")[1]);
        }
        assertFalse(itemMatchingList.containsKey("From"));
        assertFalse(itemMatchingList.containsValue("TO"));
        reader.close();
        // delete the file src/test/data/destiny/mapTest.csv
        if (file.exists()) {
            file.delete();
        }
    }
}