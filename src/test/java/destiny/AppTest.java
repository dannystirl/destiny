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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.junit.Before;
import org.junit.Test;

import junit.framework.AssertionFailedError;
import kong.unirest.GetRequest;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;

/**
 * Unit test for Destiny App.
 */
public class AppTest {

    public static Map<Long, Item> unwantedItemList; 
    public static Map<Long, Item> wantedItemList; 

    @Before
    public void setup() {
        unwantedItemList = new HashMap<>();
        wantedItemList = new HashMap<>();
        unwantedItemList.put(69420L, new Item(69420L));
        wantedItemList.put(69420L, new Item(69420L));
    }

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
     * @throws UnirestException
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

    /**
     * Get an adept item's traits from the connected normal item's traits (if that item exists)
     */
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
        assertEquals((Long) 999767358L, item);
        if(adeptMatchingList.containsValue(item)) {
            for (Map.Entry<Long, Long> entry : adeptMatchingList.entrySet()) {
                assertEquals(item, entry.getValue());
            }
        }
    }

    /**
     * Testing that each note string is being properly parsed and placed into a list
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
            System.out.println("Error reading default wishlist from url: ");
            e.printStackTrace();
        }
        System.out.printf("Test %s passed%n", new Object() {
        }.getClass().getEnclosingMethod().getName());
    }

    /**
     * Reading an armor item's information from a line, the same way an item's information is obtained. 
     * That item is then formatted //TODO - finish this
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
                    assertNotNull(returnInfo.getItemList(1));
                }
                if (line.contains("#notes:")) {
                    assertNotNull(returnInfo.getItemList(2));
                }
            }
        }
    }

    @Test
    public void testLineParserAllWithIgnore() throws Exception {
        // Setup test values
        String line = "dimwishlist:item=69420&perks=-2172504645#notes:Sleight of Hand works while stowed, but gives stats you would want while using the gun. Not a good trait. "; 
        Long itemId = 69420L; 
        String currentNote = ""; 
        boolean ignoreitem = false;
        Item item = lineParser(itemId, line, currentNote, ignoreitem); 
        // Test Results
        assertEquals(itemId, item.getItemId());
        assertEquals(1, item.getFullList(1).get(0).size());
        assertEquals(List.of("2172504645"), item.getFullList(1).get(0)); 
        assertEquals(List.of("Sleight of Hand works while stowed, but gives stats you would want while using the gun. Not a good trait. "), item.getFullList(2).get(0));
        assertTrue(item.isIgnoreItem());
        // Check Item
        checkItemWanted(item); 
        assertTrue(unwantedItemList.containsKey(item.getItemId())); 
        assertTrue(unwantedItemList.get(item.getItemId()).getFullList(1).contains(List.of("2172504645"))); 
        System.out.printf("Test %s passed%n", new Object() {
        }.getClass().getEnclosingMethod().getName());
    }

    @Test
    public void testLineParserItemWithIgnore() throws Exception {
        // Run related required tests
        testLineParserAllWithIgnore();  // add the perk to ignore
        testLineParserItem();           // test adding the item with different perks
        // Setup test values
        String line = "dimwishlist:item=768621510&perks=1392496348,2969185026,2172504645,438098033";
        Long itemId = 768621510L;
        String currentNote = "";
        boolean ignoreitem = false;
        Item item = lineParser(itemId, line, currentNote, ignoreitem);
        // Test Results
        assertEquals(itemId, item.getItemId());
        assertEquals(4, item.getFullList(1).get(0).size());
        assertEquals(List.of("1392496348", "2969185026", "2172504645", "438098033"), item.getFullList(1).get(0));
        assertEquals(List.of(""), item.getFullList(2).get(0));
        assertEquals(0, item.getFullList(3).get(0).size());
        assertFalse(item.isIgnoreItem());
        // Check Item
        checkItemWanted(item);
        assertFalse(wantedItemList.get(item.getItemId()).getFullList(1).contains(List.of("1392496348", "2969185026", "2172504645", "438098033")));
        System.out.printf("Test %s passed%n", new Object() {
        }.getClass().getEnclosingMethod().getName());
    }

    @Test
    public void testLineParserItem() throws Exception {
        // Setup test values
        String line = "dimwishlist:item=768621510&perks=1392496348,2969185026,3523296417,438098033";
        Long itemId = 768621510L;
        String currentNote = "//notes:Inspired by Destiny Massive Breakdowns from Podcast 251. PvP first choice roll for chaining (6s). Strong subtype with vertical recoil, very good stats, and very good perk combinations from the first ever legendary stasis (kinetic slot) fusion rifle. Looking to get both Range to 80 and Stability to 70 with Masterwork, barrel, and mag traits. Sleight of Hand and Harmony are an ideal pairing for chaining kills, after a kill with another weapon. Recommended MW: Range or Stability with Quick Access Sling or Targeting Adjuster mod depending on play style.";
        boolean ignoreitem = false;
        Item item = lineParser(itemId, line, currentNote, ignoreitem);
        // Test Results
        assertEquals(itemId, item.getItemId());
        assertEquals(4, item.getFullList(1).get(0).size());
        assertEquals(List.of("1392496348","2969185026","3523296417","438098033"), item.getFullList(1).get(0));
        assertEquals(List.of("//notes:PvP first choice roll for chaining (6s). Strong subtype with vertical recoil, very good stats, and very good perk combinations from the first ever legendary stasis (kinetic slot) fusion rifle. Looking to get both Range to 80 and Stability to 70 with Masterwork, barrel, and mag traits. Sleight of Hand and Harmony are an ideal pairing for chaining kills, after a kill with another weapon. Recommended MW: Range or Stability with Quick Access Sling or Targeting Adjuster mod depending on play style."), item.getFullList(2).get(0));
        assertFalse(item.isIgnoreItem());
        // Check Item
        checkItemWanted(item);
        assertTrue(wantedItemList.containsKey(item.getItemId()));
        assertTrue(wantedItemList.get(item.getItemId()).getFullList(1).contains(List.of("1392496348", "2969185026", "3523296417", "438098033")));
        System.out.printf("Test %s passed%n", new Object() {
        }.getClass().getEnclosingMethod().getName());
    }

    @Test
    public void testLineParserAll() throws Exception {
        // Setup test values
        String line = "dimwishlist:item=-69420&perks=1168162263,1015611457#notes:Outlaw + Kill Clip is a classic reload + damage combination.|tags:pve,mkb,controller,pvp";
        Long itemId = 69420L;
        String currentNote = "";
        boolean ignoreitem = true;
        Item item = lineParser(itemId, line, currentNote, ignoreitem);
        // Test Results
        assertEquals(itemId, item.getItemId());
        assertEquals(2, item.getFullList(1).get(0).size());
        assertEquals(List.of("1168162263", "1015611457"), item.getFullList(1).get(0));
        assertEquals(List.of("Outlaw + Kill Clip is a classic reload + damage combination."), item.getFullList(2).get(0));
        assertEquals(List.of("pve", "mkb", "controller", "pvp"), item.getFullList(3).get(0));
        assertFalse(item.isIgnoreItem());
        // Check Item
        checkItemWanted(item);
        assertTrue(wantedItemList.containsKey(item.getItemId()));
        assertTrue(wantedItemList.get(item.getItemId()).getFullList(1).contains(List.of("1168162263", "1015611457")));
        System.out.printf("Test %s passed%n", new Object() {
        }.getClass().getEnclosingMethod().getName());
    }

    @Test
    public void testLineParserItemPerksOnly() throws Exception {
        // Setup test values
        String line = "dimwishlist:item=768621510&perks=1392496348,2969185026,3523296417,438098033";
        Long itemId = 768621510L;
        String currentNote = "";
        boolean ignoreitem = false;
        Item item = lineParser(itemId, line, currentNote, ignoreitem);
        // Test Results
        assertEquals(itemId, item.getItemId());
        assertEquals(4, item.getFullList(1).get(0).size());
        assertEquals(List.of("1392496348", "2969185026", "3523296417", "438098033"), item.getFullList(1).get(0));
        assertEquals(List.of(""), item.getFullList(2).get(0));
        assertEquals(0, item.getFullList(3).get(0).size());
        assertFalse(item.isIgnoreItem());
        // Check Item
        checkItemWanted(item);
        assertTrue(wantedItemList.containsKey(item.getItemId()));
        assertTrue(wantedItemList.get(item.getItemId()).getFullList(1).contains(List.of("1392496348", "2969185026", "3523296417", "438098033")));
    }

    @Test
    public void testLineParserIgnoreItem() throws Exception {
        // Setup test values
        String line = "dimwishlist:item=-3556999246#notes:Pleiades Corrector has no good perk combinations. Inferior to vision of confluence. ";
        Long itemId = 3556999246L;
        String currentNote = "";
        boolean ignoreitem = true;
        Item item = lineParser(itemId, line, currentNote, ignoreitem);
        // Test Results
        assertEquals(itemId, item.getItemId());
        assertEquals(0, item.getFullList(1).get(0).size());
        assertEquals(List.of("Pleiades Corrector has no good perk combinations. Inferior to vision of confluence. "), item.getFullList(2).get(0));
        assertEquals(0, item.getFullList(3).get(0).size());
        assertTrue(item.isIgnoreItem());
        // Check Item
        checkItemWanted(item);
        assertTrue(unwantedItemList.containsKey(item.getItemId()));
        assertTrue(unwantedItemList.get(item.getItemId()).getFullList(1).contains(List.of("-")));
        System.out.printf("Test %s passed%n", new Object() {
        }.getClass().getEnclosingMethod().getName());
    }

    /**
     * Takes a line and extracts perk, note, and tag information
     *
     * @param item Long of the item number
     * @param line String of the complete line
     * @param currentNote if item is imported en mass, the note from a similar
     * previous item is used instead
     * @param ignoreitem should an item or it's perk list be excluded from the
     * list
     * @return Item with item number, perks, notes, and other various
     * information
     * @throws Exception acts as a method of catching notes without tags. should
     * never actually throw an exception
     */
    public Item lineParser(Long item, String line, String currentNote, boolean ignoreitem) throws Exception {
        // Begin testing line
        List<String> perks = new ArrayList<>();
        String notes = null;
        List<String> tags = new ArrayList<>();
        try {
            perks = Arrays.asList(line.split("&perks=")[1].split("#notes:")[0].split(",")); // desired perks
            notes = line.split("#notes:")[1]; // notes
        } catch (Exception missingInformation) {
            try {
                perks = Arrays.asList(line.split("&perks=")[1].split(",")); // desired perks with no notes
            } catch (Exception missingInformation2) {
                try {
                    notes = line.split("#notes:")[1]; // desired perks with no notes
                } catch (Exception missingInformation3) {
                    throw new AssertionFailedError("Unable to get perks");
                }
            }
        }
        if (perks.size() == 5) {
            // get rid of origin traits since they're static and just clog up the perk list
            perks = perks.subList(0, 4);
        }
        if (item == 69420L) { // -69420 is a key to apply a wanted/unwanted set of perks to all items, so this is simply to offset that negative
            ignoreitem = false;
        }
        // IS ANY ASPECT OF AN ITEM UNWANTED
        if (!perks.isEmpty() && perks.get(0).charAt(0) == '-') {
            // if holding an item with perks to ignore, remove the negative sign and prep to
            // add them to the ignore list
            for (int i = 0; i < perks.size(); i++) {
                perks.set(i, perks.get(i).substring(1));
            }
            ignoreitem = true;
        }
        // clean some notes to get rid of unnecessary fluff
        if (notes == null) {
            notes = currentNote;
        }
        if (notes.contains("auto generated")) {
            try {
                notes = "\\|tags:" + notes.split("\\|*tags:")[1];
            } catch (Exception notesError) {
                // not an error. just item has no notes
            }
        }
        try {
            // NOTES CLEANING FOR FORMATTING
            Matcher matcher = Pattern.compile("Inspired by[^\\.]*\\.\\s*", Pattern.CASE_INSENSITIVE).matcher(notes);
            notes = matcher.replaceAll("");
            if (notes.length() > 0 && notes.charAt(0) == (' ')) {
                notes = notes.substring(1);
            }
            // BASIC TAGS
            String itemType = "pv[pe]|m.?kb|controller|gambit";
            Pattern pattern = Pattern.compile("\\((" + itemType + ")(\\s*\\/+\\s*(" + itemType + "))*\\)", Pattern.CASE_INSENSITIVE); // tags in parenthesis
            matcher = pattern.matcher(notes);
            while (matcher.find()) {
                List<String> strArray = Arrays.asList(matcher.group().subSequence(1, matcher.group().length() - 1).toString().split("\\s*\\/\\s*"));
                for (String str : strArray) {
                    if (str.equalsIgnoreCase("m+kb")) {
                        str = "mkb";
                    }
                    if (!tags.contains(str.toLowerCase())) {
                        tags.add(str.toLowerCase());
                    }
                }
            }
            pattern = Pattern.compile("tags:.*", Pattern.CASE_INSENSITIVE); // tags at end of note
            matcher = pattern.matcher(notes);
            while (matcher.find()) {
                List<String> strArray = Arrays.asList(matcher.group().split("tags:\\s*")[1].split("\\,"));
                for (String str : strArray) {
                    if (str.equalsIgnoreCase("m+kb")) {
                        str = "mkb";
                    }
                    if (!tags.contains(str.toLowerCase())) {
                        tags.add(str.toLowerCase());
                    }
                }
            }
            if(!tags.isEmpty()) {
                notes = notes.split("\\|*tags")[0]; 
            }
            StringBuilder temp = new StringBuilder();
            for (String string : notes.split("(?i)\\((" + itemType + ")(\\s*\\/+\\s*(" + itemType + "))*\\):*")) {
                temp.append(string);
            }
            notes = temp.toString();

            if (notes.length() > 0 && notes.charAt(0) == (' ')) {
                notes = notes.substring(1);
            }
            notes = notes.replace("\\s+", "\\s");
        } catch (Exception e) {
            throw e;
        }
        Item returnItem = new Item(item);
        returnItem.put(perks, Arrays.asList(notes), tags, new ArrayList<>(), ignoreitem);
        return returnItem;
    }

    /**
     * Add an item to the appropriate wanted/unwanted list
     * 
     * @param returnInfo is the item to check after parsing
     * @throws Exception
     */
    public void checkItemWanted(Item returnInfo) throws Exception {
        // setup test variables
        boolean ignoreUnwanteditem = false; 

        // 69420 is the key for all items. check if a perk should be ignored on all items
        for (List<String> unwantedPerkList : unwantedItemList.get(69420L).getFullList(1)) {
            if (new HashSet<>(returnInfo.getItemList(1)).containsAll(unwantedPerkList)) {
                ignoreUnwanteditem = true;
                break;
            }
        }
        // check if ignoring a specific item or a singular perkset
        if (unwantedItemList.containsKey(returnInfo.getItemId()) && !ignoreUnwanteditem) {
            for (List<String> unwantedPerkList : unwantedItemList.get(returnInfo.getItemId()).getFullList(1)) {
                if (new HashSet<>(returnInfo.getItemList(1)).containsAll(unwantedPerkList)) {
                    ignoreUnwanteditem = true;
                    break;
                }
            }
            // are we ignoring an entire item
            if (ignoreUnwanteditem || unwantedItemList.get(returnInfo.getItemId()).getFullList(1).contains(List.of("-"))) {
                ignoreUnwanteditem = true;
            }
        }
        // ADD ITEM TO APPROPRIATE LIST
        if (!ignoreUnwanteditem) {
            try {
                if (returnInfo.isIgnoreItem()) {
                    WishlistGenerator.constructLists(returnInfo, unwantedItemList);
                } else {
                    WishlistGenerator.constructLists(returnInfo, wantedItemList);
                }
            } catch (Exception listConstructorException) {
                throw listConstructorException; 
            }
        }
    }
}
