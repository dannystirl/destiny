package destiny;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;

public class WishlistGenerator implements AutoCloseable {

    public static int sourceNum;
    public static List<ArrayList<Object>> sourceList = new ArrayList<>();
    public static Map<Long, Item> itemList = new HashMap<>();
    public static Map<Long, Item> unwantedItemList = new HashMap<>();
    public static Map<String, String> itemMatchingList = new HashMap<>(); // todo: this is printing duplicate values. probably need a second list that tracks which values were added while running, and then only print those values
    public static Map<Long, Long> adeptMatchingList = new HashMap<>(); // todo: this is printing duplicate values. probably need a second list that tracks which values were added while running, and then only print those values
    public static Map<String, String> itemNamingList = new HashMap<>(); // todo: this is printing duplicate values. probably need a second list that tracks which values were added while running, and then only print those values
    public static List<String> checkedItemList = new ArrayList<>();
    public static BufferedReader br;
    public static PrintStream errorOutputFile;
    public static PrintStream scriptedWishlistFile;

    public static final String wishlistOutputFileName = "output//WishListScripted.txt";
    public static final String errorOutputFileName = "bin//errors.txt";
    public static final String enhancedMappingFileName = "src//main//data//destiny//enhancedMapping.csv";
    public static final String nameMappingFileName = "src//main//data//destiny//nameMapping.csv";
    public static final String wishlistDSourceFileName = "input//CompleteDestinyWishList.txt";
    public static final String wishlistDSourceUrlName = "https://raw.githubusercontent.com/48klocs/dim-wish-list-sources/master/voltron.txt";
    public static final String bungieItemSearchUrl = "https://www.bungie.net/Platform/Destiny2/Armory/Search/DestinyInventoryItemDefinition/{searchTerm}/";
    public static final String bungieItemDefinitionUrl = "https://www.bungie.net/Platform/Destiny2/Manifest/DestinyInventoryItemDefinition/{hashIdentifier}/";

    /**
     * the main method reads through the original file and collects data on each
     * roll, concatenating notes and eliminating duplicates
     *
     * @param args any args needed for the main method, most likely to be an input
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        // Create the error file and output file
        try {
            errorOutputFile = new PrintStream(errorOutputFileName);
            scriptedWishlistFile = new PrintStream(wishlistOutputFileName);
        } catch (FileNotFoundException e) {
            System.out.println("Error creating error file: " + e);
            throw new FileNotFoundException();
        }
        // Try to read in existing enhanced -> normal perk mappings
        try (BufferedReader reader = new BufferedReader(
                new FileReader(enhancedMappingFileName))) {
            reader.readLine(); // skip the header line
            while (reader.ready()) {
                String item = reader.readLine();
                itemMatchingList.put(item.split(",")[0], item.split(",")[1]);
                checkedItemList.add(item.split(",")[0]);
                checkedItemList.add(item.split(",")[1]);
            }
        } catch (Exception e) {
            Formatters.errorPrint("Unable to read in existing item matching list", e);
            String eol = System.getProperty("line.separator");
            try (Writer writer = new FileWriter(enhancedMappingFileName, false)) {
                writer.append("From")
                        .append(',')
                        .append("To")
                        .append(eol);
                writer.flush();
            } catch (Exception er) {
                Formatters.errorPrint("Unable to save itemMatchingList to .\\data", er);
            }
        }
        Map<String, String> originalItemMatchingList = itemMatchingList.entrySet().stream().collect(Collectors.toMap(Entry::getKey, Entry::getValue));

        // Try to read in item -> name mappings
        try (BufferedReader reader = new BufferedReader(
                new FileReader(nameMappingFileName))) {
            reader.readLine(); // skip the header line
            while (reader.ready()) {
                String item = reader.readLine();
                itemNamingList.put(item.split(",")[0], item.split(",")[1]);
                checkedItemList.add(item.split(",")[0]);
                checkedItemList.add(item.split(",")[1]);
            }
        } catch (Exception e) {
            Formatters.errorPrint("Unable to read in existing item naming list", e);
            String eol = System.getProperty("line.separator");
            try (Writer writer = new FileWriter(nameMappingFileName, false)) {
                writer.append("Item ID")
                        .append(',')
                        .append("Name")
                        .append(eol);
                writer.flush();
            } catch (Exception er) {
                Formatters.errorPrint("Unable to save itemNamingList to .\\data", er);
            }
        }
        Map<String, String> originalItemNamingList = itemNamingList.entrySet().stream().collect(Collectors.toMap(Entry::getKey, Entry::getValue));

        Unirest.config().reset();
        Unirest.config().connectTimeout(5000).socketTimeout(5000).concurrency(10, 5);

        unwantedItemList.put(69420L, new Item(69420L));
        itemList.put(69420L, new Item(69420L));

        try {
            br = new BufferedReader(new FileReader("input//CustomDestinyWishlist.txt"));
            loopRead(br);
        } catch (FileNotFoundException e) {
            Formatters.errorPrint("Error reading custom wishlist file", e);
        }

        try {
            try {
                HttpResponse<String> response = Unirest.get(wishlistDSourceUrlName).asString();
                PrintWriter out = new PrintWriter(wishlistDSourceFileName);
                out.println(response.getBody());
                out.close();
            } catch (Exception e) {
                Formatters.errorPrint("Unable to get updated contents from " + errorOutputFileName, e);
            }
            br = new BufferedReader(new FileReader(wishlistDSourceFileName));
            loopRead(br);
        } catch (Exception e) {
            Formatters.errorPrint("Error reading default wishlist from url", e);
        }

        // SORTING ITEMS
        // Sort each item in itemList by the perkList, starting with the final entry in each perkList
        for (Map.Entry<Long, Item> item : itemList.entrySet()) {
            try {
                item.getValue().getRollList().sort((Roll roll1, Roll roll2) -> {
                    List<String> roll1PerkList = roll1.getPerkList();
                    List<String> roll2PerkList = roll2.getPerkList();
                    int roll1perkSize = roll1PerkList.size();
                    int roll2perkSize = roll2PerkList.size();
                    for (int i = 0; i < Math.min(roll1perkSize, roll2perkSize); i++) {
                        if (!roll1PerkList.get(roll1perkSize - i - 1).equals(roll2PerkList.get(roll2perkSize - i - 1))) {
                            return roll1PerkList.get(roll1perkSize - i - 1).compareTo(roll2PerkList.get(roll2perkSize - i - 1));
                        }
                    }
                    return roll1PerkList.get(0).compareTo(roll2PerkList.get(0));
                });
            } catch (Exception e) {
                Formatters.errorPrint("Error sorting item " + item.getKey(), e);
            }
        }

        /*for (Map.Entry<Long, Item> item : itemList.entrySet()) {
            // each value has a list of the original list position and the new list position
            List<List<String>> listD = new ArrayList<>(item.getValue().getFullList(1));
            Map<List<String>, Integer> mapPositions = new HashMap<>(); // original ordering. used to reorder the lists
            List<List<String>> tempPerkList = new ArrayList<>();
            List<List<String>> tempNoteList = new ArrayList<>();
            List<List<String>> tempTagsList = new ArrayList<>();
            List<List<String>> tempMWsList = new ArrayList<>();
            for (int i = 0; i < listD.size(); i++) {k
                mapPositions.put(listD.get(i), i);
                tempPerkList.add(new ArrayList<>());
                tempNoteList.add(new ArrayList<>());
                tempTagsList.add(new ArrayList<>());
                tempMWsList.add(new ArrayList<>());
            }

            listD.sort((List<String> o1, List<String> o2) -> {
                // we only need to sort by getFullList(1) (the perkSet list), but since the order of perkSets will change, so will the order of notes etc., so the whole item needs to be sorted and then looped through
                // compare getItemList(1) on each index, starting at the last index
                for (int i = 0; i < Math.min(o1.size(), o2.size()); i++) {
                    if (!o1.get(o1.size() - i - 1).equals(o2.get(o2.size() - i - 1))) {
                        return o1.get(o1.size() - i - 1).compareTo(o2.get(o2.size() - i - 1));
                    }
                }
                return o1.get(0).compareTo(o2.get(0));
            });

            for (int i = 0; i < listD.size(); i++) {
                // map positions is the original map positions of items. should use this map to get values from fullLists 2..4
                // listD is the sorted position of each item. value at index i (a perk list) should be the key for the original space in map positions
                tempPerkList.set(i, item.getValue().getFullList(1).get(mapPositions.get(listD.get(i))));
                tempNoteList.set(i, item.getValue().getFullList(2).get(mapPositions.get(listD.get(i))));
                tempTagsList.set(i, item.getValue().getFullList(3).get(mapPositions.get(listD.get(i))));
                tempMWsList.set(i, item.getValue().getFullList(4).get(mapPositions.get(listD.get(i))));
            }
            item.getValue().setFullList(1, tempPerkList);
            item.getValue().setFullList(2, tempNoteList);
            item.getValue().setFullList(3, tempTagsList);
            item.getValue().setFullList(4, tempMWsList);
        }*/

        /*
         * TODO - Would love to add a second sort here to organize by notes again (happens to be how it's sorted without the above sorting method) to reduce output file size.
         * Ideally by size of note so the ones with more information (generally the ones that lists had originally) would be at the top of the list, and therefor easier to see in dim.
         * This would also put anything without notes (usually just collections of perks) at the bottom.
         *
         * However, you can't sort by note list here because notes aren't unique entries so there's no way to map them back to the original list
         *
         * Alternatively, I could also sort inversely by the number of perk-sets under each note to achieve a similar affect.
         * Would need to see this in action BUT I'm not even sure I need to do this since dim already does this.
         * It would really just be for a minor file size reduction.
         */
        printWishlist();

        // Print the itemMatchingList to a file so I don't need to call HTTP.GET every time I run the script
        String eol = System.getProperty("line.separator");
        try (Writer writer = new FileWriter(enhancedMappingFileName, true)) {
            for (Map.Entry<String, String> entry : itemMatchingList.entrySet()) {
                if (!(originalItemMatchingList.containsKey(entry.getKey()) && originalItemMatchingList.get(entry.getKey()).equals(entry.getValue()))) {
                    writer.append(entry.getKey())
                            .append(',')
                            .append(entry.getValue())
                            .append(eol);
                }
            }
            writer.flush();
        } catch (Exception e) {
            Formatters.errorPrint("Unable to save itemMatchingList to .\\data", e);
        }
        // Print the itemNamingList to a file so I don't need to call HTTP.GET every time I run the script
        // TODO - First in Last out is adding an extra column to the name. Github copilot gave a reason (csv reader issue) but im not sure it's actually correct
        try (Writer writer = new FileWriter(nameMappingFileName, true)) {
            for (Map.Entry<String, String> entry : itemNamingList.entrySet()) {
                if (!(originalItemNamingList.containsKey(entry.getKey()) && originalItemNamingList.get(entry.getKey()).equals(entry.getValue()))) {
                    writer.append(entry.getKey())
                            .append(',')
                            .append(entry.getValue())
                            .append(eol);
                }
            }
            writer.flush();
        } catch (Exception e) {
            Formatters.errorPrint("Unable to save itemNamingList to .\\data", e);
        }
        errorOutputFile.close();
    }

    /**
     * Reads a wishlist file and adds it to the appropriate lists.
     *
     * @param br buffered reader to read the file
     * @throws Exception
     */
    public static void loopRead(BufferedReader br) throws Exception {
        ArrayList<Object> td = new ArrayList<>();
        sourceNum = 0; // stores how many rolls a given source has
        String currentNote = ""; // used to store an item's notes, either per roll or per item
        String line;
        while ((line = br.readLine()) != null) {
            if (line.length() > 0 && line.charAt(0) == '@') {
                line = line.replace("@", "");
            }
            switch (line.split(":")[0]) {
                case "title":
                    td = new ArrayList<>();
                    td.add(sourceNum++);
                    td.add(line.split(":")[1]);
                    System.out.printf("Reading items from %s, list #%d.%n", line.split(":")[1], sourceNum);
                    break;
                case "description":
                    td.add(line.split(":")[1]);
                    sourceList.add(td);
                    // bug: is taking "//" as a new line character instead of a string
                    break;
                case "dimwishlist":
                    int startKey = 17; // where the item id lies
                    boolean ignoreItem = false;
                    boolean ignoreUnwantedItem = false;
                    if (line.charAt(startKey) == '-') {
                        ignoreItem = true;
                        startKey = 18;
                    }
                    // GATHERING LINE INFORMATION (ITEM, PERKS, NOTES)
                    Long itemId = Long.parseLong(line.substring(startKey).split("&")[0].split("#")[0]);
                    //? TESTING ONLY
                    /* if (itemId != 3407395594L) {
                        break;
                    } */
                    // Convert from adept to normal so they all have the same perks and notes. Convert back when printing so adepts and normals are next to each other in the file
                    if (!adeptMatchingList.containsKey(itemId)) {
                        Long oldItem = itemId;
                        try {
                            String name = getName(itemId.toString());
                            if (name.contains("(Adept)")) {
                                // After checking if the item is adept, find the normal version and convert
                                JSONArray resultSet = Formatters.bungieItemHashSetJSONArray(name);
                                for (Object object : resultSet) {
                                    JSONObject jsonObject = (JSONObject) object;
                                    JSONObject itemDefinition = jsonObject.getJSONObject("displayProperties");
                                    if (!itemDefinition.getString("name").contains("(Adept)")) {
                                        itemId = jsonObject.getLong("hash");
                                    }
                                }
                            }
                        } catch (JSONException e) {
                            Formatters.errorPrint(String.format("Error checking for adept version of %s. Probably occurs when checking item type instead of item", itemId), e);
                        } finally {
                            // used to get the normal version of an item from the adept version
                            adeptMatchingList.put(oldItem, itemId);
                        }
                    } else {
                        itemId = adeptMatchingList.get(itemId);
                    }
                    Item returnItem = lineParser(itemId, line, currentNote, ignoreItem);

                    // TRANSLATE  ADEPT                                                 TO  NORMAL
                    // TRANSLATE  https://www.light.gg/db/all/?page=1&f=4(3),10(Trait)  TO  https://www.light.gg/db/all/?page=1&f=4(2),10(Trait)
                    List<String> itemPerkList = returnItem.getRollList().get(0).getPerkList();
                    List<String> tempPerkList = new ArrayList<>(itemPerkList);
                    int j = 0;
                    if (itemPerkList.size() == 4) {
                        j = 2;
                    }
                    for (int i = j; i < itemPerkList.size(); i++) {
                        if (!checkedItemList.contains(itemPerkList.get(i))) {
                            try {
                                checkPerk(itemPerkList.get(i));
                            } catch (Exception e) {
                                // Really could be any number of reasons for this to happen, but it's probably a timeout. 
                                Formatters.errorPrint("HTTP Error", e);
                            }
                        }
                        // if itemMatchingList contains itemPerkList.get(i), set tempPerkList to the itemMatchingList (convert dead / incorrect perks to the correct / normal version)
                        if (itemMatchingList.containsKey(itemPerkList.get(i))) {
                            tempPerkList.set(i, itemMatchingList.get(itemPerkList.get(i)));
                        }
                    }
                    returnItem.addRoll(new Roll(tempPerkList));

                    // CHECK IF ITEM PROPERTIES ARE UNWANTED
                    // 69420 is the key for all items. check if a perk should be ignored on all items
                    for (Roll unwantedRoll : unwantedItemList.get(69420L).getRollList()) {
                        if (new HashSet<>(returnItem.getRollList().get(0).getPerkList()).containsAll(unwantedRoll.getPerkList())) {
                            ignoreUnwantedItem = true;
                            break;
                        }
                    }
                    // check if ignoring a specific item or a singular perk-set
                    if (unwantedItemList.containsKey(itemId) && !ignoreUnwantedItem) {
                        for (Roll unwantedRoll : unwantedItemList.get(itemId).getRollList()) {
                            if (new HashSet<>(returnItem.getRollList().get(0).getPerkList()).containsAll(unwantedRoll.getPerkList())) {
                                ignoreUnwantedItem = true;
                                break;
                            }
                        }
                        // are we ignoring an entire item
                        if (ignoreUnwantedItem || unwantedItemList.get(itemId).getRollList().stream().map(Roll::getPerkList).toList().contains(List.of("-"))) {
                            ignoreUnwantedItem = true;
                        }
                    }
                    // ADD ITEM TO APPROPRIATE LIST
                    if (!ignoreUnwantedItem) {
                        try {
                            if (returnItem.isIgnoreItem()) {
                                constructLists(returnItem, unwantedItemList);
                            } else {
                                constructLists(returnItem, itemList);
                            }
                            System.out.print("");
                        } catch (Exception listConstructorException) {
                            Formatters.errorPrint("Error on line " + line, listConstructorException);
                            throw new Exception(listConstructorException);
                        }
                    }
                    break;
                case "//notes":
                    startKey = 8;
                    if (line.charAt(8) == (' ')) {
                        startKey = 9;
                    }
                    currentNote = line.substring(startKey);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * PRINTING WISHLIST
     */
    public static void printWishlist() throws FileNotFoundException {
        System.setOut(scriptedWishlistFile);
        System.setErr(errorOutputFile);

        // print overall title and description
        System.out.printf("title:%s%n", sourceList.get(0).get(1));
        System.out.printf("description:%s%n%n", sourceList.get(0).get(2));
        // print wishlist rolls
        for (Map.Entry<Long, Item> item : itemList.entrySet()) {
            Long normalItemId = item.getKey();
            List<Long> keysList = new ArrayList<>(List.of(normalItemId));

            // Convert back any items that have adept versions and print both
            for (Map.Entry<Long, Long> entry : adeptMatchingList.entrySet()) {
                if (Objects.equals(normalItemId, entry.getValue())) {
                    if (!keysList.contains(entry.getKey())) {
                        keysList.add(entry.getKey());
                    }
                }
            }
            for (Long k : keysList) {
                printWishlistInner(item, k);
            }
        }
        // reset output to console
        scriptedWishlistFile.close();
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
        System.setErr(errorOutputFile);
    }

    /**
     * A helper method for printing, allowing to loop for adept and normal
     * versions of items
     *
     * @param item the original item to compare to
     * @param key  the item id to check for similarity from
     */
    public static void printWishlistInner(Map.Entry<Long, Item> item, Long key) throws FileNotFoundException {
        List<Roll> itemRollList = item.getValue().getRollList();
        List<String> currentNoteFull = new ArrayList<>();
        List<String> currentTagsFull = new ArrayList<>();
        List<String> currentMWsFull = new ArrayList<>();

        System.setOut(scriptedWishlistFile);
        System.setErr(errorOutputFile);

        // ITEM NAME
        String name = "";
        if (itemNamingList.containsKey(key.toString())) {
            name = itemNamingList.get(key.toString());
        } else {
            try {
                name = getName(key.toString());
                itemNamingList.put(key.toString(), name);
            } catch (Exception e) {
                Formatters.errorPrint("Unable to get name for item " + key, e, scriptedWishlistFile);
            }
        }
        // ITEM VALUES
        Summarizer summarizer = new Summarizer(scriptedWishlistFile);
        System.out.printf("%n//item %s: %s%n", key, name);
        for (Roll itemRoll : itemRollList) {
            // TAGS
            // game-mode is in beginning, input type is at end
            itemRoll.getTagList().sort(Collections.reverseOrder());

            // ITEM DOCUMENTATION IS DIFFERENT
            if (!currentNoteFull.equals(itemRoll.getNoteList())
                    || !currentTagsFull.equals(itemRoll.getTagList())
                    || !currentMWsFull.equals(itemRoll.getMWList())) {
                currentNoteFull = itemRoll.getNoteList();
                currentTagsFull = itemRoll.getTagList();
                currentMWsFull = itemRoll.getMWList();
                // NOTES
                System.out.print("//notes:");
                for (String note : currentNoteFull) {
                    if (!note.equals("")) {
                        // reverse the outlier changes made earlier
                        note = note.replace("lightggg", "light.gg");
                        note = note.replace("elipsez", "...");
                        note = note.replace("v30", "3.0");
                        // format note
                        note = Formatters.noteFormatter(note);
                        // TODO: Probably a good point to add a summarizer (If I ever get around to it)
                        System.out.print(summarizer.sentenceAnalyzerUsingFrequency(note));
                        //System.out.print(note);
                        System.out.print(". ");
                    }
                }
                // MWS
                if (!itemRoll.getMWList().isEmpty()) {
                    System.out.print("Recommended MW: ");
                    for (int i = 0; i < itemRoll.getMWList().size() - 1; i++) {
                        System.out.print(itemRoll.getMWList().get(i) + ", ");
                    }
                    System.out.print(itemRoll.getMWList().get(itemRoll.getMWList().size() - 1) + ". ");
                }
                try {
                    // TAGS
                    if (!currentTagsFull.get(0).equals("")) {
                        // hashset is a fast way to remove duplicates, however they may have gotten there
                        LinkedHashSet<String> linkedHashSet = new LinkedHashSet<>(currentTagsFull);
                        System.out.print("|tags:");
                        for (int i = 0; i < linkedHashSet.size(); i++) {
                            if (i == linkedHashSet.size() - 1) {
                                System.out.print(linkedHashSet.toArray()[i]);
                            } else {
                                System.out.printf("%s,", linkedHashSet.toArray()[i]);
                            }
                        }
                    }
                } catch (IndexOutOfBoundsException e) {
                    // item has no tags
                } finally {
                    System.out.println();
                }
            }
            if (key == 69420L) {
                key = -69420L;
            }
            System.out.printf("dimwishlist:item=%s", key);
            if (!itemRoll.getPerkList().isEmpty()) {
                // ITEM
                System.out.print("&perks=");
                // check if there is an item in itemRoll.getPerkList()
                for (int i = 0; i < itemRoll.getPerkList().size() - 1; i++) {
                    System.out.printf("%s,", itemRoll.getPerkList().get(i));
                }
                System.out.printf("%s%n", itemRoll.getPerkList().get(itemRoll.getPerkList().size() - 1));
            }
        }

        // reset errorOutputFile to console
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.out)));
    }

    /**
     * Takes an item id and returns the associated name as a string
     *
     * @param hashIdentifier the item id to get the name of
     * @return the name of the item as a string
     * @throws UnirestException
     */
    public static String getName(String hashIdentifier) throws UnirestException {
        if (itemNamingList.containsKey(hashIdentifier)) {
            return itemNamingList.get(hashIdentifier);
        }

        JSONObject itemDefinition = Formatters.bungieItemDefinitionJSONObject(hashIdentifier);

        itemNamingList.put(hashIdentifier, itemDefinition.getString("name"));
        return itemDefinition.getString("name");
    }

    /**
     * Takes an item and maps it to the appropriate item list. Excludes
     * duplicate perk sets, notes, and tags on duplicate perk sets, include
     * non-duplicate notes and tags
     *
     * @param item    An input item
     * @param itemMap A map of item ids to item names
     * @return A mapping of the appropriate item list
     */
    public static Map<Long, Item> constructLists(Item item, Map<Long, Item> itemMap) {
        // Get the rolls of the current item
        List<Roll> itemRollList = new ArrayList<>();
        Roll roll = new Roll(item.getRollList().get(0).getPerkList());
        int perkListIndex = -1;
        if (itemMap.containsKey(item.getItemId())) {
            itemRollList = itemMap.get(item.getItemId()).getRollList();
            roll = itemMap.get(item.getItemId()).getRoll(item.getRollList().get(0).getPerkList());
            perkListIndex = itemRollList.indexOf(roll);
        }
        // perkListIndex == -1 means item with perks is not already in perkList
        if (perkListIndex == -1) {
            // PERKS
            // If entire item is unwanted, set the perk list to '-'
            if (item.isIgnoreItem() && roll.getPerkList().isEmpty()) {
                item.addRoll(new Roll(List.of("-")));
                itemMap.put(item.getItemId(), item);
                return itemMap;
            } else { // Else add item and unwanted perks to perkList
                Roll returnRoll = constructListsInner(item, roll);
                itemRollList.add(returnRoll);
            }
        } else {
            // If the item's perk list contains the current perks, only add the notes, tags, and mws to the existing roll
            Roll returnRoll = constructListsInner(item, roll);
            itemRollList.set(perkListIndex, returnRoll);
        }
        item.setRollList(itemRollList);
        itemMap.put(item.getItemId(), item);
        return itemMap;
    }

    /**
     * A helper method to collect an item's information and ensure each
     * itemNumber has a unique set of information
     *
     * @param item the item to collect information from
     * @param roll the roll to collect information from, including notes, tags, and mws
     * @return a list of lists of notes, tags, and mws
     */
    public static @NotNull
    Roll constructListsInner(Item item, Roll roll) {
        String note = item.getRollList().get(0).getNoteList().get(0);
        try {
            // TAGS
            for (String tag : item.getRollList().get(0).getTagList()) {
                if (!roll.getTagList().contains(tag)) {
                    roll.addTag(tag);
                }
            }
            for (String tag : note.split("\\|*tags:")[1].split("\\s*\\,\\s*")) {
                tag = Formatters.tagFormatter(tag);
                if (!roll.getTagList().contains(tag)) {
                    roll.addTag(tag);
                }
            }
            note = note.split("\\|*tags:")[0];
        } catch (Exception notesError) {
            try {
                roll.addTag(Formatters.tagFormatter(note.split("\\|tags:")[1]));
                note = note.split("\\|*tags:")[0];
            } catch (Exception tagsError) {
                // not an error. just item has no tags
            }
        } finally {
            // NOTES & MW
            note = note.replace("light.gg", "lightggg");
            note = note.replace("...", "elipsez");
            note = note.replace("3.0", "v30");
            String mwRegex = "(Recommended\\s|\\[){1,25}MW((\\:\\s)|(\\s\\-\\s))";
            Pattern pattern = Pattern.compile(String.format("%s[^\\.\\|\\n]*", mwRegex), Pattern.CASE_INSENSITIVE);
            for (String mwToFormat : note.split("\\.\\s*|\"\\s*|\\]")) {
                // Format note
                Matcher matcher = pattern.matcher(mwToFormat);
                String formattedMW = "";
                String formattedNote = "";
                if (matcher.matches()) {
                    // MW
                    String[] noteMwlist = matcher.group().split(mwRegex)[1].split("[^\\x00-\\x7F]");
                    formattedMW = Formatters.noteFormatter(noteMwlist[0]);
                    // Note
                    if (noteMwlist.length > 1) {
                        formattedNote = Formatters.noteFormatter(noteMwlist[1]);
                    }
                } else {
                    formattedNote = Formatters.noteFormatter(mwToFormat);
                }
                // Add required items to appropriate lists
                if (!(formattedMW.equals("") || roll.getMWList().contains(formattedMW))) {
                    roll.addMW(formattedMW);
                }
                if (!(formattedNote.equals("") || roll.getNoteList().contains(formattedNote))) {
                    roll.addNote(formattedNote);
                }
            }
        }
        return roll;
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
                    Formatters.errorPrint(line, missingInformation3);
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
        } catch (Exception e) {
            Formatters.errorPrint("Error with notes: " + notes, e);
        }
        return new Item(itemId, List.of(new Roll(perks, List.of(notes), tags, new ArrayList<>())), ignoreItem);
    }

    /**
     * @param hashIdentifier - the hash of the perk to be checked
     */
    public static void checkPerk(String hashIdentifier) {
        try {
            hardCodedCases(hashIdentifier);
            return;
        } catch (Exception e) {
            // For some reason the api doesn't work for the values in here, so I'm just gonna hard code it and ignore the error
            // this really should only occur once for each hashIdentifier
            //errorPrint(hashIdentifier + " is not hard coded", e);
        }

        JSONObject itemDefinition = Formatters.bungieItemDefinitionJSONObject(hashIdentifier);
        JSONArray resultSet = Formatters.bungieItemHashSetJSONArray(itemDefinition.getString("name"));
        Long normal = null, enhanced = null;
        for (Object object : resultSet) {
            JSONObject jsonObject = (JSONObject) object;
            if (jsonObject.getJSONObject("displayProperties").length() == itemDefinition.length()
                    && jsonObject.getJSONObject("displayProperties").getString("name")
                    .equals(itemDefinition.getString("name"))) {
                if (normal == null) {
                    normal = jsonObject.getLong("hash");
                } else {
                    enhanced = jsonObject.getLong("hash");
                }
            }
        }
        // add entry to itemMatchingList at key
        if (enhanced != null) {
            itemMatchingList.put(enhanced.toString(), normal.toString());
            checkedItemList.add(enhanced.toString());
        }
        if (normal != null) {
            checkedItemList.add(normal.toString());
            // TODO - this probably removes a lot of the edge cases, so maybe go through and remove them?
            checkedItemList.add(hashIdentifier);
            if (enhanced == null) {
                itemMatchingList.put(hashIdentifier, normal.toString());
            }
        }
    }

    /**
     * Helper method because some stuff in the api isn't matching up this seems
     * to be mostly accurate except for frames that were turned into perks (ex.
     * Disruption Break) have more than two entries high impact reserves and
     * Ambitious Assassin also seems to have an issue (only returning one
     * value), but I think that's more an issue with the api, not my code. Some
     * of these values could be removed now? (Stuff with only two entries) since
     * I changed some stuff in the checkPerk() method
     *
     * @param perk - the perk to be checked
     * @throws Exception an exception for error handling, shouldn't be a problem
     */
    public static void hardCodedCases(String perk) throws Exception {
        switch (perk) {
            case "3528046508": {
                // Auto-Loading Holster
                itemMatchingList.put("3528046508", "3300816228");
                checkedItemList.add("3528046508");
                checkedItemList.add("3300816228");
                break;
            }
            case "2717805783": {
                // Moving Target 
                itemMatchingList.put("2717805783", "588594999");
                checkedItemList.add("2717805783");
                checkedItemList.add("588594999");
                break;
            }
            case "2014892510": {
                // Perpetual Motion (Has E in the name)
                itemMatchingList.put("2014892510", "1428297954");
                checkedItemList.add("2014892510");
                checkedItemList.add("1428297954");
                break;
            }
            case "288411554": {
                // Rampage (Duplicate in API)
                itemMatchingList.put("288411554", "3425386926");
                checkedItemList.add("288411554");
                checkedItemList.add("3425386926");
                break;
            }
            case "1523649716": {
                // Tap the Trigger
                itemMatchingList.put("1523649716", "1890422124");
                checkedItemList.add("1523649716");
                checkedItemList.add("1890422124");
                break;
            }
            case "3797647183": {
                // Ambitious Assassin
                itemMatchingList.put("3797647183", "2010801679");
                checkedItemList.add("3797647183");
                checkedItemList.add("2010801679");
                break;
            }
            case "2002547233": {
                // High-Impact Reserves
                itemMatchingList.put("2002547233", "2213355989");
                checkedItemList.add("2002547233");
                checkedItemList.add("2213355989");
                break;
            }
            case "494941759": {
                // Threat Detector
                itemMatchingList.put("494941759", "4071163871");
                checkedItemList.add("494941759");
                checkedItemList.add("4071163871");
                break;
            }
            case "3143051906": {
                // repeating case
            }
            case "25606670": {
                // Dual Loader (I really only want the enhanced version and this seems much simpler than making a lot of changes to the code)
                itemMatchingList.put("25606670", "3143051906");
                itemMatchingList.put("3143051906", "3143051906");
                checkedItemList.add("25606670");
                checkedItemList.add("3143051906");
                break;
            }
            case "3076459908": {
                // repeating case
            }
            case "598607952": {
                // repeating case
            }
            case "2396489472": {
                // Chain Reaction (Comes up as sandbox perk)
                itemMatchingList.put("598607952", "2396489472");
                itemMatchingList.put("3076459908", "2396489472");
                checkedItemList.add("598607952");
                checkedItemList.add("3076459908");
                checkedItemList.add("2396489472");
                break;
            }
            case "2848615171": {
                // repeating case
            }
            case "169755979": {
                // repeating case
            }
            case "3865257976": {
                // Dragonfly
                itemMatchingList.put("3865257976", "2848615171");
                itemMatchingList.put("169755979", "2848615171");
                checkedItemList.add("2848615171");
                checkedItemList.add("169755979");
                checkedItemList.add("3865257976");
                break;
            }
            case "3513791699": {
                // repeating case
            }
            case "162561147": {
                // repeating case
            }
            case "3337692349": {
                // Dragonfly
                itemMatchingList.put("3513791699", "3337692349");
                itemMatchingList.put("162561147", "3337692349");
                checkedItemList.add("3513791699");
                checkedItemList.add("162561147");
                checkedItemList.add("3337692349");
                break;
            }
            case "2216471363": {
                // repeating case
            }
            case "1683379515": {
                // repeating case
            }
            case "3871884143": {
                // Disruption Break (Barrel)
                itemMatchingList.put("2216471363", "1683379515");
                itemMatchingList.put("3871884143", "1683379515");
                checkedItemList.add("2216471363");
                checkedItemList.add("3871884143");
                checkedItemList.add("1683379515");
                break;
            }
            case "2360754333": {
                // repeating case
            }
            case "2459015849": {
                // repeating case
            }
            case "806159697": {
                // Trench Barrel (Barrel)
                itemMatchingList.put("2459015849", "2360754333");
                itemMatchingList.put("806159697", "2360754333");
                checkedItemList.add("2459015849");
                checkedItemList.add("806159697");
                checkedItemList.add("2360754333");
                break;
            }
            default: {
                throw new Exception();
            }
        }
    }

    @Override
    public void close() throws Exception {
        br.close();
    }
}
