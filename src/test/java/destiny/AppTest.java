package destiny;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
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

import org.apache.http.annotation.Experimental;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import junit.framework.AssertionFailedError;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;

/**
 * Unit test for Destiny App.
 */
public class AppTest {

    public static Map<Long, Item> unwantedItemList;
    public static Map<Long, Item> wantedItemList;

    public static final String wishlistTSourceFileName = "input//CustomDestinyWishList.txt";
    public static final String errorOutputFileName = WishlistGenerator.errorOutputFileName;

    @Before
    public void setup() {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.out)));
        unwantedItemList = new HashMap<>();
        wantedItemList = new HashMap<>();
        unwantedItemList.put(69420L, new Item(69420L));
        wantedItemList.put(69420L, new Item(69420L));
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
        try (Writer writer = new FileWriter(file, false)) {
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
     * Get an adept item's traits from the connected normal item's traits (if
     * that item exists)
     */
    @Test
    public void testAdeptConversion() {
        Long item = 2886339027L;
        Map<Long, Long> adeptMatchingList = new HashMap<>();
        if (!adeptMatchingList.containsKey(item)) {
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
        if (adeptMatchingList.containsValue(item)) {
            for (Map.Entry<Long, Long> entry : adeptMatchingList.entrySet()) {
                assertEquals(item, entry.getValue());
            }
        }
    }

    /**
     * Testing that each note string is being properly parsed and placed into a
     * list
     */
    @Test
    public void testMWPattern() {
        String note = "Testing   initial text. Recommended MW - Range. Testing middle text. Recommended MW: Stability. . Recommended MW: Range with Targeting Adjuster mod";
        String mwRegex = "(Recommended\\s|\\[)+MW((\\:\\s)|(\\s\\-\\s))";
        Pattern pattern = Pattern.compile(String.format("%s.*", mwRegex), Pattern.CASE_INSENSITIVE);
        List<String> mws = new ArrayList<>();
        List<String> notes = new ArrayList<>();

        note = note.replaceAll("\\s{2,}", " ");
        note = note.replace("\\s+.\\s*", "");
        try {
            for (String string : note.split("\\.[\\s]*|\"[\\s]*")) {
                Matcher matcher = pattern.matcher(string);
                if (matcher.matches()) {
                    if (!mws.contains(matcher.group().split(mwRegex)[1])) {
                        mws.add(matcher.group().split(mwRegex)[1]);
                    }
                } else if (!notes.contains(string) && !string.isEmpty()) {
                    notes.add(string);
                }
            }
        } catch (Exception e) {
            Matcher matcher = pattern.matcher(note);
            if (!mws.contains(matcher.group().split(mwRegex)[1])) {
                mws.add(matcher.group().split(mwRegex)[1]);
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

    @Test
    public void testAnalyzer() throws FileNotFoundException {
        String text = "What is the weather in California right now? I hope it is sunny.";
        List<List<HashMap<String, Object>>> analyzedText = new Summarizer(new PrintStream(new FileOutputStream(FileDescriptor.out))).sentenceAnalyzer(text);
        assert analyzedText.size() == 2;
        assert analyzedText.get(0).size() == 9;
        assert analyzedText.get(0).get(0).keySet().containsAll(List.of("pos", "ne", "lemma", "word"));
        assert analyzedText.get(1).size() == 6;
    }

    @Test
    public void testFrequencySummarizer() throws FileNotFoundException {
        String text = "Precision frame submachine guns are not good in PvE content, their low handling, reload speed, and stability are all important factors when mowing down waves of enemies. Subsistence and Feeding Frenzy both improve one of those limitations but I wouldnt invest into those to make up for it. Onslaught will make Adjudicator feel more like other archetypes but wont replace them. First-choice PvE pick for add clearing Minor Spec enemies in general content. Adjudicator has decent stats and, with a precision frame, you don't need to worry too much about the published Recoil Direction. A little extra stability helps keep bullets on target, and a little extra range is nice, so Smallbore or Corkscrew Rifling can help out, or Chambered Compensator can take Recoil Direction to 100 as well as adding stability, You'll be emptying the mag a lot, so reload speed is the magazine perk choice - Alloy Mag, Tactical Mag and Flared Magwell all help. In the first trait column as the support park, Feeding Frenzy, Threat Detector, Perpetual Motion and Subsistence all speed up reload as well as bumping up other useful stats, or delay the need to reload, for you to keep uptime for Onslaught, in the second trait column, to boost RPM for additional damage to mow down adds. The Crossing Over origin trait is useful all around for PvE, with bumps to range and handling in the top half of a magazine, and a small 3% damage boost at the bottom - Subsistence kills keeps feeding ammo into the bottom half of the mag to extend the time the extra damage is procced. First-choice PvE pick for add clearing Minor Spec enemies in end-game content. In the first trait column as the support park, Feeding Frenzy, Threat Detector and Subsistence all speed up reload as well as bumping up other useful stats, or delay the need to reload. With Frenzy procced in the second perk column, you'll have a 15% damage boost, snappy handling and lightning fast reloads. ";
        String analyzedText = new Summarizer(new PrintStream(new FileOutputStream(FileDescriptor.out))).sentenceAnalyzerUsingFrequency(text);
        assert text.length() > analyzedText.length();
    }

    /**
     * Method to test getting content from a url
     *
     * @throws UnirestException when unable to establish a connection to the url
     * @throws IOException      when unable to read from the text file
     */
    @Test
    public void testWishlistUrl() throws UnirestException, IOException {
        Unirest.config().reset();
        Unirest.config().connectTimeout(10000).socketTimeout(10000);
        HttpResponse<String> response = Unirest.get(WishlistGenerator.wishlistDSourceUrlName).asString();
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
     * Reading an armor item's information from a line, the same way an item's
     * information is obtained. That item is then formatted //TODO - finish this
     *
     * @throws Exception
     */
    @Test
    @Experimental
    public void testArmor() throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(wishlistTSourceFileName));
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
                    assertNotNull(returnInfo.getRollList().get(0).getPerkList());
                }
                if (line.contains("#notes:")) {
                    assertNotNull(returnInfo.getRollList().get(0).getNoteList());
                }
            }
        }
    }

    @Test
    public void testLineParserAllWithIgnore() {
        // Setup test values
        String line = "dimwishlist:item=69420&perks=-2172504645#notes:Sleight of Hand works while stowed, but gives stats you would want while using the gun. Not a good trait. ";
        Long itemId = 69420L;
        Item item = lineParser(itemId, line, "", false);
        // Test Results
        assertEquals(itemId, item.getItemId());
        assertEquals(1, item.getRollList().get(0).getPerkList().size());
        assertEquals(List.of("2172504645"), item.getRollList().get(0).getPerkList());
        assertEquals(List.of("Sleight of Hand works while stowed, but gives stats you would want while using the gun. Not a good trait"), item.getRollList().get(0).getNoteList());
        assertTrue(item.isIgnoreItem());
        // Check Item
        checkItemWanted(item);
        assertTrue(unwantedItemList.containsKey(item.getItemId()));
        assertTrue(unwantedItemList.get(item.getItemId()).getRollList().stream().map(Roll::getPerkList).toList().contains(List.of("2172504645")));
        System.out.printf("Test %s passed%n", new Object() {
        }.getClass().getEnclosingMethod().getName());
    }

    @Test
    public void testLineParserItemWithIgnore() {
        // Run related required tests
        testLineParserAllWithIgnore();  // add the perk to ignore
        testLineParserItem();           // test adding the item with different perks
        // Setup test values
        String line = "dimwishlist:item=768621510&perks=1392496348,2969185026,2172504645,438098033";
        Long itemId = 768621510L;
        Item item = lineParser(itemId, line, "", false);
        // Test Results
        assertEquals(itemId, item.getItemId());
        assertEquals(4, item.getRollList().get(0).getPerkList().size());
        assertEquals(List.of("1392496348", "2969185026", "2172504645", "438098033"), item.getRollList().get(0).getPerkList());
        assertEquals(List.of(""), item.getRollList().get(0).getNoteList());
        assertEquals(0, item.getRollList().get(0).getTagList().size());
        assertFalse(item.isIgnoreItem());
        // Check Item
        checkItemWanted(item);
        assertFalse(wantedItemList.get(item.getItemId()).getRollList().stream().map(Roll::getPerkList).toList().contains(List.of("1392496348", "2969185026", "2172504645", "438098033")));
        System.out.printf("Test %s passed%n", new Object() {
        }.getClass().getEnclosingMethod().getName());
    }

    @Test
    public void testLineParserItem() {
        // Setup test values
        String line = "dimwishlist:item=768621510&perks=1392496348,2969185026,3523296417,438098033";
        Long itemId = 768621510L;
        String currentNote = "//notes:Inspired by Destiny Massive Breakdowns from Podcast 251. PvP first choice roll for chaining (6s). Strong subtype with vertical recoil, very good stats, and very good perk combinations from the first ever legendary stasis (kinetic slot) fusion rifle. Looking to get both Range to 80 and Stability to 70 with Masterwork, barrel, and mag traits. Sleight of Hand and Harmony are an ideal pairing for chaining kills, after a kill with another weapon. Recommended MW: Range or Stability with Quick Access Sling or Targeting Adjuster mod depending on play style.";
        Item item = lineParser(itemId, line, currentNote, false);
        // Test Results
        assertEquals(itemId, item.getItemId());
        assertEquals(4, item.getRollList().get(0).getPerkList().size());
        assertEquals(List.of("1392496348", "2969185026", "3523296417", "438098033"), item.getRollList().get(0).getPerkList());
        assertEquals(List.of("//notes:PvP first choice roll for chaining (6s). Strong subtype with vertical recoil, very good stats, and very good perk combinations from the first ever legendary stasis (kinetic slot) fusion rifle. Looking to get both Range to 80 and Stability to 70 with Masterwork, barrel, and mag traits. Sleight of Hand and Harmony are an ideal pairing for chaining kills, after a kill with another weapon. Recommended MW: Range or Stability with Quick Access Sling or Targeting Adjuster mod depending on play style"), item.getRollList().get(0).getNoteList());
        assertFalse(item.isIgnoreItem());
        // Check Item
        checkItemWanted(item);
        assertTrue(wantedItemList.containsKey(item.getItemId()));
        assertTrue(wantedItemList.get(item.getItemId()).getRollList().stream().map(Roll::getPerkList).toList().contains(List.of("1392496348", "2969185026", "3523296417", "438098033")));
        System.out.printf("Test %s passed%n", new Object() {
        }.getClass().getEnclosingMethod().getName());
    }

    @Test
    public void testLineParserAll() {
        // Setup test values
        String line = "dimwishlist:item=-69420&perks=1168162263,1015611457#notes:Outlaw + Kill Clip is a classic reload + damage combination.|tags:pve,mkb,controller,pvp";
        Long itemId = 69420L;
        Item item = lineParser(itemId, line, "", true);
        // Test Results
        assertEquals(itemId, item.getItemId());
        assertEquals(2, item.getRollList().get(0).getPerkList().size());
        assertEquals(List.of("1168162263", "1015611457"), item.getRollList().get(0).getPerkList());
        assertEquals(List.of("Outlaw + Kill Clip is a classic reload + damage combination"), item.getRollList().get(0).getNoteList());
        assertEquals(List.of("pve", "mkb", "controller", "pvp"), item.getRollList().get(0).getTagList());
        assertFalse(item.isIgnoreItem());
        // Check Item
        checkItemWanted(item);
        assertTrue(wantedItemList.containsKey(item.getItemId()));
        assertTrue(wantedItemList.get(item.getItemId()).getRollList().stream().map(Roll::getPerkList).toList().contains(List.of("1168162263", "1015611457")));
        System.out.printf("Test %s passed%n", new Object() {
        }.getClass().getEnclosingMethod().getName());
    }

    @Test
    public void testLineParserItemPerksOnly() {
        // Setup test values
        String line = "dimwishlist:item=768621510&perks=1392496348,2969185026,3523296417,438098033";
        Long itemId = 768621510L;
        Item item = lineParser(itemId, line, "", false);
        // Test Results
        assertEquals(itemId, item.getItemId());
        assertEquals(4, item.getRollList().get(0).getPerkList().size());
        assertEquals(List.of("1392496348", "2969185026", "3523296417", "438098033"), item.getRollList().get(0).getPerkList());
        assertEquals(List.of(""), item.getRollList().get(0).getNoteList());
        assertEquals(0, item.getRollList().get(0).getTagList().size());
        assertFalse(item.isIgnoreItem());
        // Check Item
        checkItemWanted(item);
        assertTrue(wantedItemList.containsKey(item.getItemId()));
        assertTrue(wantedItemList.get(item.getItemId()).getRollList().stream().map(Roll::getPerkList).toList().contains(List.of("1392496348", "2969185026", "3523296417", "438098033")));
    }

    @Test
    public void testLineParserIgnoreItem() {
        // Setup test values
        String line = "dimwishlist:item=-3556999246#notes:Pleiades Corrector has no good perk combinations. Inferior to vision of confluence. ";
        Long itemId = 3556999246L;
        String currentNote = "";
        Item item = lineParser(itemId, line, currentNote, true);
        // Test Results
        assertEquals(itemId, item.getItemId());
        assertEquals(0, item.getRollList().get(0).getPerkList().size());
        assertEquals(List.of("Pleiades Corrector has no good perk combinations. Inferior to vision of confluence"), item.getRollList().get(0).getNoteList());
        assertEquals(0, item.getRollList().get(0).getTagList().size());
        assertTrue(item.isIgnoreItem());
        // Check Item
        checkItemWanted(item);
        assertTrue(unwantedItemList.containsKey(item.getItemId()));
        assertTrue(unwantedItemList.get(item.getItemId()).getRollList().stream().map(Roll::getPerkList).toList().contains(List.of("-")));
        System.out.printf("Test %s passed%n", new Object() {
        }.getClass().getEnclosingMethod().getName());
    }

    /**
     * Takes a line and extracts perk, note, and tag information
     *
     * @param itemId      Long of the item number
     * @param line        String of the complete line
     * @param currentNote if item is imported en masse, the note from a similar
     *                    previous item is used instead
     * @param ignoreItem  should an item (or its perk list) be excluded from the
     *                    list
     * @return Item with item number, perks, notes, and other various
     * information
     */
    public static Item lineParser(Long itemId, String line, String currentNote, boolean ignoreItem) {
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
        if (itemId == 69420L) { // -69420 is a key to apply a wanted/unwanted set of perks to all items, so this is simply to offset that negative
            ignoreItem = false;
        }
        // IS ANY ASPECT OF AN ITEM UNWANTED
        if (!perks.isEmpty() && perks.get(0).charAt(0) == '-') {
            // if holding an item with perks to ignore, remove the negative sign and prep to
            // add them to the ignore list
            for (int i = 0; i < perks.size(); i++) {
                perks.set(i, perks.get(i).substring(1));
            }
            ignoreItem = true;
        }
        // clean some notes to get rid of unnecessary fluff
        notes = Formatters.initialNoteFormatter(notes, currentNote);
        try {
            // BASIC TAGS
            String itemType = "pv[pe]|m.?kb|controller|gambit";
            Pattern pattern = Pattern.compile("\\((" + itemType + ")(\\s*[\\/\\s\\\\]+\\s*(" + itemType + "))*\\)(\\:\\s*)*", Pattern.CASE_INSENSITIVE); // tags in parentheses
            Matcher matcher = pattern.matcher(notes);
            while (matcher.find()) {
                String[] tagArray = matcher.group().replace("(", "").replaceAll("\\):\\s*", "").split("\\s*[\\/\\s\\\\]+\\s*");
                for (String tag : tagArray) {
                    tag = Formatters.tagFormatter(tag).toLowerCase();
                    if (!tags.contains(tag)) {
                        tags.add(tag);
                    }
                }
            }
            notes = pattern.matcher(notes).replaceAll("");
            pattern = Pattern.compile("\\|*tags:.*", Pattern.CASE_INSENSITIVE); // tags at end of noteList
            matcher = pattern.matcher(notes);
            while (matcher.find()) {
                String[] strArray = matcher.group().toLowerCase().split("tags:\\s*")[1].split("\\,");
                for (String str : strArray) {
                    str = Formatters.tagFormatter(str).toLowerCase();
                    if (!tags.contains(str)) {
                        tags.add(str);
                    }
                }
            }
            notes = pattern.matcher(notes).replaceAll("");
            notes = Formatters.noteFormatter(notes);
        } catch (Exception e) {
            throw new AssertionFailedError("Unable to get notes");
        }
        return new Item(itemId, List.of(new Roll(perks, List.of(notes), tags, new ArrayList<>())), ignoreItem);
    }

    /**
     * Add an item to the appropriate wanted/unwanted list
     *
     * @param returnInfo is the item to check after parsing
     */
    public void checkItemWanted(Item returnInfo) {
        // setup test variables
        boolean ignoreUnwantedItem = false;

        // 69420 is the key for all items. check if a perk should be ignored on all items
        for (Roll unwantedRoll : unwantedItemList.get(69420L).getRollList()) {
            if (new HashSet<>(returnInfo.getRollList().get(0).getPerkList()).containsAll(unwantedRoll.getPerkList())) {
                ignoreUnwantedItem = true;
                break;
            }
        }
        // check if ignoring a specific item or a singular perk-set
        if (unwantedItemList.containsKey(returnInfo.getItemId()) && !ignoreUnwantedItem) {
            for (Roll unwantedRoll : unwantedItemList.get(returnInfo.getItemId()).getRollList()) {
                if (new HashSet<>(returnInfo.getRollList().get(0).getPerkList()).containsAll(unwantedRoll.getPerkList())) {
                    ignoreUnwantedItem = true;
                    break;
                }
            }
            // are we ignoring an entire item
            if (ignoreUnwantedItem || unwantedItemList.get(returnInfo.getItemId()).getRollList().stream().map(Roll::getPerkList).toList().contains(List.of("-"))) {
                ignoreUnwantedItem = true;
            }
        }
        // ADD ITEM TO APPROPRIATE LIST
        if (!ignoreUnwantedItem) {
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
