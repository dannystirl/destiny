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
import java.util.HashMap;
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

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;

/**
 * Unit test for Destiny App.
 */
public class AppTest {

    static LineDataParsers lineDataParsers;
    static BungieDataParsers bungieDataParsers;

    public static final String wishlistTSourceFileName = "input//CustomDestinyWishList.txt";
    public static final String errorOutputFileName = WishlistGenerator.errorOutputFileName;

    @Before
    public void setup() {
        System.setOut(Formatters.defaultPrintStream);
        System.setErr(Formatters.defaultPrintStream);
        lineDataParsers = new LineDataParsers(App.RunType.TEST);
        bungieDataParsers = new BungieDataParsers(App.RunType.TEST);
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
            BungieDataParsers.itemMatchingList.put(item.split(",")[0], item.split(",")[1]);
            BungieDataParsers.checkedItemList.add(item.split(",")[0]);
            BungieDataParsers.checkedItemList.add(item.split(",")[1]);
        }
        assertFalse(BungieDataParsers.itemMatchingList.containsKey("From"));
        assertFalse(BungieDataParsers.itemMatchingList.containsValue("TO"));
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
        if (!BungieDataParsers.adeptMatchingList.containsKey(item)) {
            Long oldItem = item;
            item = 999767358L;

            // used to get the normal version of an item from the adept version
            BungieDataParsers.adeptMatchingList.put(oldItem, item);
            assertTrue(BungieDataParsers.adeptMatchingList.containsKey(2886339027L));
        } else {
            assertTrue(BungieDataParsers.adeptMatchingList.containsKey(2886339027L));
            item = BungieDataParsers.adeptMatchingList.get(item);
        }
        assertEquals((Long) 999767358L, item);
        if (BungieDataParsers.adeptMatchingList.containsValue(item)) {
            for (Map.Entry<Long, Long> entry : BungieDataParsers.adeptMatchingList.entrySet()) {
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
        List<List<HashMap<String, Object>>> analyzedText = new Summarizer(Formatters.defaultPrintStream).sentenceAnalyzer(text);
        assert analyzedText.size() == 2;
        assert analyzedText.get(0).size() == 9;
        assert analyzedText.get(0).get(0).keySet().containsAll(List.of("pos", "ne", "lemma", "word"));
        assert analyzedText.get(1).size() == 6;
    }

    @Test
    public void testFrequencySummarizer() throws FileNotFoundException {
        String text = "Precision frame submachine guns are not good in PvE content, their low handling, reload speed, and stability are all important factors when mowing down waves of enemies. Subsistence and Feeding Frenzy both improve one of those limitations but I wouldnt invest into those to make up for it. Onslaught will make Adjudicator feel more like other archetypes but wont replace them. First-choice PvE pick for add clearing Minor Spec enemies in general content. Adjudicator has decent stats and, with a precision frame, you don't need to worry too much about the published Recoil Direction. A little extra stability helps keep bullets on target, and a little extra range is nice, so Smallbore or Corkscrew Rifling can help out, or Chambered Compensator can take Recoil Direction to 100 as well as adding stability, You'll be emptying the mag a lot, so reload speed is the magazine perk choice - Alloy Mag, Tactical Mag and Flared Magwell all help. In the first trait column as the support park, Feeding Frenzy, Threat Detector, Perpetual Motion and Subsistence all speed up reload as well as bumping up other useful stats, or delay the need to reload, for you to keep uptime for Onslaught, in the second trait column, to boost RPM for additional damage to mow down adds. The Crossing Over origin trait is useful all around for PvE, with bumps to range and handling in the top half of a magazine, and a small 3% damage boost at the bottom - Subsistence kills keeps feeding ammo into the bottom half of the mag to extend the time the extra damage is procced. First-choice PvE pick for add clearing Minor Spec enemies in end-game content. In the first trait column as the support park, Feeding Frenzy, Threat Detector and Subsistence all speed up reload as well as bumping up other useful stats, or delay the need to reload. With Frenzy procced in the second perk column, you'll have a 15% damage boost, snappy handling and lightning fast reloads. ";
        String analyzedText = new Summarizer(Formatters.defaultPrintStream).sentenceAnalyzerUsingFrequency(text);
        assert text.length() > analyzedText.length();
    }

    @Test
    public void testFrequencySummarizerLarge() throws FileNotFoundException {
        String text = "First-choice PvE pick for burst damage against Major spec enemies, or fallback damage against Boss spec in end-game content. Scatter Signal is perhaps one of the best weapons for burst damage as at v7. 3.0. 5 - great all-round stats as well as offering excellent trait combinations. Controlling recoil, with nine bolts, is important, as is snappiness - Arrowhead Brake helps tidy up recoil direction, but you might consider Fluted Barrel for the combination of handling and stability instead. Enhanced Battery gives the same number of charges (8) as Ionized so, with one eye on Overflow, maximises the battery size. Enhanced Overflow will take your battery to 18 charges - you'll have one left in reserve. Adding two Reserve mods takes this to three reserve shots, and with Overflow topping up the battery, for just picking up an ammo brick, you might never need to reload. Controlled Burst reduces charge time to 414 and boosts damage by 20% for hitting all your bolts. The Dragon's Vengeance origin trait can be useful, although it only procs once your health bar turns red or an ally dies. You'll get a reload and 11s of a boost to range and either charge rate or handling. Oh hey, a new strand fusion rifle. Will Scatter Signal be better than Pressurized Precision? Well neither are great as Strand fusion rifles. But for a Kinetic fusion rifle I think Scatter Signal rivals and possibly beats out Riptide for best in slot fusion. Overflow and Controlled Burst for added damage throughout the magazine. Youll only need one magazine booster to hit the fusion cap of 8 shots or you can opt for an even faster firing fusion with Accelerated Coils. Overflow will double your magazine capacity giving you 16 shots, 15 of which will have an extra 20% damage increase. If you opt for Extended Battery for the 16 total shots you can slot in Major or Boss spec for an additional 7.77% damage to every shot. ";
        String analyzedText = new Summarizer(Formatters.defaultPrintStream).sentenceAnalyzerUsingFrequency(text, List.of("first-choice", "backup"));
        assert text.length() > analyzedText.length();
        assert analyzedText.contains("First-choice");
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
                boolean ignoreItem = false;
                if (line.charAt(startKey) == '-') {
                    ignoreItem = true;
                    startKey = 18;
                }
                // GATHERING LINE INFORMATION (ITEM, PERKS, NOTES)
                Long item = Long.parseLong(line.substring(startKey).split("&")[0].split("#")[0]);
                Item returnInfo = lineDataParsers.lineParser(item, line, "", ignoreItem);

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
    public void testLineParserAllWithIgnore() throws Exception {
        // Setup test values
        String line = "dimwishlist:item=69420&perks=-2172504645#notes:Sleight of Hand works while stowed, but gives stats you would want while using the gun. Not a good trait. ";
        Long itemId = 69420L;
        Item item = lineDataParsers.lineParser(itemId, line, "", false);
        // Test Results
        assertEquals(itemId, item.getItemId());
        assertEquals(1, item.getRollList().get(0).getPerkList().size());
        assertEquals(List.of("2172504645"), item.getRollList().get(0).getPerkList());
        assertEquals(List.of("Sleight of Hand works while stowed, but gives stats you would want while using the gun. Not a good trait. "), item.getRollList().get(0).getNoteList());
        assertTrue(item.isIgnoreItem());
        // Check Item
        lineDataParsers.addItemToWantedList(item);
        assertTrue(lineDataParsers.unwantedItemList.containsKey(item.getItemId()));
        assertTrue(lineDataParsers.unwantedItemList.get(item.getItemId()).getRollList().stream().map(Roll::getPerkList).toList().contains(List.of("2172504645")));
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
        Item item = lineDataParsers.lineParser(itemId, line, "", false);
        // Test Results
        assertEquals(itemId, item.getItemId());
        assertEquals(4, item.getRollList().get(0).getPerkList().size());
        assertEquals(List.of("1392496348", "2969185026", "2172504645", "438098033"), item.getRollList().get(0).getPerkList());
        assertEquals(List.of(""), item.getRollList().get(0).getNoteList());
        assertEquals(0, item.getRollList().get(0).getTagList().size());
        assertFalse(item.isIgnoreItem());
        // Check Item
        lineDataParsers.addItemToWantedList(item);
        assertFalse(lineDataParsers.wantedItemList.get(item.getItemId()).getRollList().stream().map(Roll::getPerkList).toList().contains(List.of("1392496348", "2969185026", "2172504645", "438098033")));
        System.out.printf("Test %s passed%n", new Object() {
        }.getClass().getEnclosingMethod().getName());
    }

    @Test
    public void testLineParserItem() throws Exception {
        // Setup test values
        String line = "dimwishlist:item=768621510&perks=1392496348,2969185026,3523296417,438098033";
        Long itemId = 768621510L;
        String currentNote = "//notes:Inspired by Destiny Massive Breakdowns from Podcast 251. PvP first choice roll for chaining (6s). Strong subtype with vertical recoil, very good stats, and very good perk combinations from the first ever legendary stasis (kinetic slot) fusion rifle. Looking to get both Range to 80 and Stability to 70 with Masterwork, barrel, and mag traits. Sleight of Hand and Harmony are an ideal pairing for chaining kills, after a kill with another weapon. Recommended MW: Range or Stability with Quick Access Sling or Targeting Adjuster mod depending on play style.";
        Item item = lineDataParsers.lineParser(itemId, line, currentNote, false);
        // Test Results
        assertEquals(itemId, item.getItemId());
        assertEquals(4, item.getRollList().get(0).getPerkList().size());
        assertEquals(List.of("1392496348", "2969185026", "3523296417", "438098033"), item.getRollList().get(0).getPerkList());
        assertEquals(List.of("//notes:PvP first choice roll for chaining (6s). Strong subtype with vertical recoil, very good stats, and very good perk combinations from the first ever legendary stasis (kinetic slot) fusion rifle. Looking to get both Range to 80 and Stability to 70 with Masterwork, barrel, and mag traits. Sleight of Hand and Harmony are an ideal pairing for chaining kills, after a kill with another weapon. Recommended MW: Range or Stability with Quick Access Sling or Targeting Adjuster mod depending on play style."), item.getRollList().get(0).getNoteList());
        assertFalse(item.isIgnoreItem());
        // Check Item
        lineDataParsers.addItemToWantedList(item);
        assertTrue(lineDataParsers.wantedItemList.containsKey(item.getItemId()));
        assertTrue(lineDataParsers.wantedItemList.get(item.getItemId()).getRollList().stream().map(Roll::getPerkList).toList().contains(List.of("1392496348", "2969185026", "3523296417", "438098033")));
        System.out.printf("Test %s passed%n", new Object() {
        }.getClass().getEnclosingMethod().getName());
    }

    @Test
    public void testLineParserAll() throws Exception {
        // Setup test values
        String line = "dimwishlist:item=-69420&perks=1168162263,1015611457#notes:Outlaw + Kill Clip is a classic reload + damage combination.|tags:pve,mkb,controller,pvp";
        Long itemId = 69420L;
        Item item = lineDataParsers.lineParser(itemId, line, "", true);
        // Test Results
        assertEquals(itemId, item.getItemId());
        assertEquals(2, item.getRollList().get(0).getPerkList().size());
        assertEquals(List.of("1168162263", "1015611457"), item.getRollList().get(0).getPerkList());
        assertEquals(List.of("Outlaw + Kill Clip is a classic reload + damage combination."), item.getRollList().get(0).getNoteList());
        assertEquals(List.of("pve", "mkb", "controller", "pvp"), item.getRollList().get(0).getTagList());
        assertFalse(item.isIgnoreItem());
        // Check Item
        lineDataParsers.addItemToWantedList(item);
        assertTrue(lineDataParsers.wantedItemList.containsKey(item.getItemId()));
        assertTrue(lineDataParsers.wantedItemList.get(item.getItemId()).getRollList().stream().map(Roll::getPerkList).toList().contains(List.of("1168162263", "1015611457")));
        System.out.printf("Test %s passed%n", new Object() {
        }.getClass().getEnclosingMethod().getName());
    }

    @Test
    public void testLineParserItemPerksOnly() throws Exception {
        // Setup test values
        String line = "dimwishlist:item=768621510&perks=1392496348,2969185026,3523296417,438098033";
        Long itemId = 768621510L;
        Item item = lineDataParsers.lineParser(itemId, line, "", false);
        // Test Results
        assertEquals(itemId, item.getItemId());
        assertEquals(4, item.getRollList().get(0).getPerkList().size());
        assertEquals(List.of("1392496348", "2969185026", "3523296417", "438098033"), item.getRollList().get(0).getPerkList());
        assertEquals(List.of(""), item.getRollList().get(0).getNoteList());
        assertEquals(0, item.getRollList().get(0).getTagList().size());
        assertFalse(item.isIgnoreItem());
        // Check Item
        lineDataParsers.addItemToWantedList(item);
        assertTrue(lineDataParsers.wantedItemList.containsKey(item.getItemId()));
        assertTrue(lineDataParsers.wantedItemList.get(item.getItemId()).getRollList().stream().map(Roll::getPerkList).toList().contains(List.of("1392496348", "2969185026", "3523296417", "438098033")));
    }

    @Test
    public void testLineParserIgnoreItem() throws Exception {
        // Setup test values
        String line = "dimwishlist:item=-3556999246#notes:Pleiades Corrector has no good perk combinations. Inferior to vision of confluence. ";
        Long itemId = 3556999246L;
        String currentNote = "";
        Item item = lineDataParsers.lineParser(itemId, line, currentNote, true);
        // Test Results
        assertEquals(itemId, item.getItemId());
        assertEquals(0, item.getRollList().get(0).getPerkList().size());
        assertEquals(List.of("Pleiades Corrector has no good perk combinations. Inferior to vision of confluence. "), item.getRollList().get(0).getNoteList());
        assertEquals(0, item.getRollList().get(0).getTagList().size());
        assertTrue(item.isIgnoreItem());
        // Check Item
        lineDataParsers.addItemToWantedList(item);
        assertTrue(lineDataParsers.unwantedItemList.containsKey(item.getItemId()));
        assertTrue(lineDataParsers.unwantedItemList.get(item.getItemId()).getRollList().stream().map(Roll::getPerkList).toList().contains(List.of("-")));
        System.out.printf("Test %s passed%n", new Object() {
        }.getClass().getEnclosingMethod().getName());
    }
}
