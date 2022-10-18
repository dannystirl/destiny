package destiny;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

import kong.unirest.GetRequest;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;

/**
 * Unit test for Destiny App.
 */
public class AppTest {

    /**
     * Ensure the connection to the destiny api is working and getting a response, as well as connecting normal and enhanced perks
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
        System.out.printf("Test %s passed%n", new Object() {
        }.getClass().getEnclosingMethod().getName());
    }

    /**
     * Ensure the api is making a connection between normal and adept weapons when there is one
     * @throws UnirestException
     */
    @Test
    public void testAdeptConnection() throws UnirestException {
        Unirest.config().reset();
        Unirest.config().connectTimeout(10000).socketTimeout(10000);
        HttpResponse<String> response = Unirest
                .get("https://www.bungie.net/Platform/Destiny2/Manifest/DestinyInventoryItemDefinition/{hashIdentifier}/")
                .header("X-API-KEY", "735ad4372078466a8b68a09ff9c02edb")
                .routeParam("hashIdentifier", "2886339027")
                .asString();

        JSONObject itemDefinition = new JSONObject(response.getBody());
        itemDefinition = itemDefinition.getJSONObject("Response");
        itemDefinition = itemDefinition.getJSONObject("displayProperties");
        assertEquals("Cataclysmic", itemDefinition.getString("name").split("\s\\(Adept\\)")[0]);

        GetRequest get = Unirest.get(
                        "https://www.bungie.net/Platform/Destiny2/Armory/Search/DestinyInventoryItemDefinition/{searchTerm}/")
                .header("X-API-KEY", "735ad4372078466a8b68a09ff9c02edb");
        response = get.routeParam("searchTerm", itemDefinition.getString("name").split("\s\\(Adept\\)")[0]).asString();

        JSONObject mJsonObject = new JSONObject(response.getBody());
        JSONObject userJObject = mJsonObject.getJSONObject("Response");
        JSONObject statusJObject = userJObject.getJSONObject("results");
        JSONArray resultSet = statusJObject.getJSONArray("results");
        Long normal = null, adept = null;
        // ensure that there are only two versions of the gun
        assertEquals(2, resultSet.length());
        for (Object object : resultSet) {
            JSONObject jsonObject = (JSONObject) object;
            if (normal == null) {
                normal = jsonObject.getLong("hash");
            } else {
                adept = jsonObject.getLong("hash");
            }
        }
        // assert that key and entry are not null
        assertNotNull(normal);
        assertNotNull(adept);
        System.out.printf("Test %s passed%n", new Object() {
        }.getClass().getEnclosingMethod().getName());
    }

    /**
     * Ensure the api is making a connection between normal and adept weapons when there isn't one
     * @throws UnirestException
     */
    @Test
    public void testNonAdeptConnection() throws UnirestException {
        Unirest.config().reset();
        Unirest.config().connectTimeout(10000).socketTimeout(10000);
        HttpResponse<String> response = Unirest
                .get("https://www.bungie.net/Platform/Destiny2/Manifest/DestinyInventoryItemDefinition/{hashIdentifier}/")
                .header("X-API-KEY", "735ad4372078466a8b68a09ff9c02edb")
                .routeParam("hashIdentifier", "2886339027")
                .asString();

        JSONObject itemDefinition = new JSONObject(response.getBody());
        itemDefinition = itemDefinition.getJSONObject("Response");
        itemDefinition = itemDefinition.getJSONObject("displayProperties");
        assertTrue(itemDefinition.getString("name").contains("(Adept)"));
        assertEquals("Cataclysmic", itemDefinition.getString("name").split("\s\\(Adept\\)")[0]);

        GetRequest get = Unirest.get(
                        "https://www.bungie.net/Platform/Destiny2/Armory/Search/DestinyInventoryItemDefinition/{searchTerm}/")
                .header("X-API-KEY", "735ad4372078466a8b68a09ff9c02edb");
        response = get.routeParam("searchTerm", itemDefinition.getString("name").split("\s\\(Adept\\)")[0]).asString();

        JSONObject mJsonObject = new JSONObject(response.getBody());
        JSONObject userJObject = mJsonObject.getJSONObject("Response");
        JSONObject statusJObject = userJObject.getJSONObject("results");
        JSONArray resultSet = statusJObject.getJSONArray("results");
        // ensure that there are only two versions of the gun
        assertEquals(2, resultSet.length());
        for (Object object : resultSet) {
            JSONObject jsonObject = (JSONObject) object;
            itemDefinition = jsonObject.getJSONObject("displayProperties");
            if(!itemDefinition.getString("name").contains("(Adept)")) {
                assertEquals("Cataclysmic", itemDefinition.getString("name"));
                assertEquals(999767358, jsonObject.getLong("hash"));
            }
        }
        // assert that key and entry are not null
        System.out.printf("Test %s passed%n", new Object() {
        }.getClass().getEnclosingMethod().getName());
    }

    /**
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
        System.out.printf("Test %s passed%n", new Object() {
        }.getClass().getEnclosingMethod().getName());
    }

    /**
     * Ensure the ability to write to a file is working
     *
     * @throws Exception
     */
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
        System.out.printf("Test %s passed%n", new Object() {
        }.getClass().getEnclosingMethod().getName());
    }

    /**
     * Ensure the ability to read from a file is working
     *
     * @throws Exception
     */
    @Test
    public void testInput() throws Exception {
        Map<String, String> itemMatchingList = new HashMap<>();
        List<String> checkedItemList = new ArrayList<>();
        File file = new File("src/test/data/destiny/mapTest.csv");
        try ( Writer writer = new FileWriter(file, false);) {
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
        System.out.printf("Test %s passed%n", new Object() {
        }.getClass().getEnclosingMethod().getName());

    }

    @Test
    public void testAdeptConversion() {
        Long item = 2886339027L;
        Map<Long, Long> adeptMatchingList = new HashMap<>();
        if(!adeptMatchingList.containsKey(item)) {
            Long oldItem = item;
            item = 999767358L;

            // used to get the normal version of an item from the adept version
            adeptMatchingList.put(oldItem, item);
            assertTrue(adeptMatchingList.containsKey(2886339027L));
        } else {
            assertTrue(adeptMatchingList.containsKey(2886339027L));
            item = adeptMatchingList.get(item);
        }
        assertTrue(item==999767358L);

        if(adeptMatchingList.containsValue(item)) {
            for (Map.Entry<Long, Long> entry : adeptMatchingList.entrySet()) {
                assertTrue(Objects.equals(item, entry.getValue()));
            }
        }
    }

    /**
     * Testing that each note string is being properly parsed and placed into a
     * list
     */
    @Test
    public void testMWPattern() {
        String note = "Testing   initial text. Recommended MW - Range. Testing middle text. Recommended MW: Stability. . Recommended MW: Range with Targeting Adjuster mod. ";
        Pattern pattern = Pattern.compile("Recommended\\sMW((\\:\\s)|(\\s\\-\\s))[^.]*", Pattern.CASE_INSENSITIVE);
        List<String> mws = new ArrayList<>();
        List<String> notes = new ArrayList<>();

        note = note.replaceAll("\\s{2,}", " ");
        note = note.replace("\\s+.\\s*", "");
        try {
            for (String string : Arrays.asList(note.split("\\.[\\s]*|\"[\\s]*"))) {
                Matcher matcher = pattern.matcher(string);
                if (matcher.matches()) {
                    if (!mws.contains(matcher.group().split("Recommended\\sMW((\\:\\s)|(\\s\\-\\s))")[1])) {
                        mws.add(matcher.group().split("Recommended\\sMW((\\:\\s)|(\\s\\-\\s))")[1]);
                    }
                } else if (!notes.contains(string) && !string.isEmpty()) {
                    notes.add(string);
                }
            }
        } catch (Exception e) {
            Matcher matcher = pattern.matcher(note);
            if (!mws.contains(matcher.group().split("Recommended\\sMW((\\:\\s)|(\\s\\-\\s))")[1])) {
                mws.add(matcher.group().split("Recommended\\sMW((\\:\\s)|(\\s\\-\\s))")[1]);
            } else if (!notes.contains(note) && !note.isEmpty()) {
                notes.add(note);
            }
        } finally {
            // add notes, tags, and mws to returnList
            assertTrue(notes.contains("Testing initial text"));
            assertTrue(notes.contains("Testing middle text"));
            assertTrue(mws.contains("Range"));
            assertTrue(mws.contains("Stability"));
            assertTrue(mws.contains("Range with Targeting Adjuster mod"));
        }
        System.out.printf("Test %s passed%n", new Object() {
        }.getClass().getEnclosingMethod().getName());
    }

    /**
     * Method to test getting content from a url
     *
     * @throws UnirestException when unable to establish a connection to the url
     * @throws IOException when unable to read from the text file
     */
    @Test
    public void testWishlistUrl() throws UnirestException, IOException {
        Unirest.config().reset();
        Unirest.config().connectTimeout(10000).socketTimeout(10000);
        HttpResponse<String> response = Unirest
                .get("https://raw.githubusercontent.com/48klocs/dim-wish-list-sources/master/voltron.txt")
                .asString();
        assertNotEquals("404: Not Found", response.getBody());

        try {
            BufferedReader reader = new BufferedReader(new StringReader(response.getBody()));
            int lineCount = 0;
            while (reader.ready() && lineCount < 15) {
                assertNotNull(reader.readLine());
                lineCount++;
            }
        } catch (FileNotFoundException e) {
            System.out.println("Error reading default wishlist from url: " + e.getStackTrace());
        }
        System.out.printf("Test %s passed%n", new Object() {
        }.getClass().getEnclosingMethod().getName());
    }

    /**
     * @throws Exception
     */
    @Test
    public void testArmor() throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(new File("input//TestDestinyWishlist.txt")));
        String line;
        while ((line = br.readLine()) != null) {
            if (line.split(":")[0].equals("dimwishlist")) {
                int startKey = 17; // where the item id lies
                boolean ignoreitem = false;
                if (line.charAt(startKey) == '-') {
                    ignoreitem = true;
                    startKey = 18;
                }
                // GATHERING LINE INFORMATION (ITEM, PERKS, NOTES)
                Long item = Long.parseLong(line.substring(startKey).split("&")[0].split("#")[0]);
                Item returnInfo = WishlistGenerator.lineParser(item, line, "", ignoreitem);
                
                if (line.contains("&perks=")) {
                    assertTrue(returnInfo.getItemList(1) != null);
                }
                if (line.contains("#notes:")) {
                    assertTrue(returnInfo.getItemList(2) != null);
                }
            }
        }
    }
}
