package destiny;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import kong.unirest.GetRequest;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;

public class WishlistGenerator implements AutoCloseable {

    public static int sourceNum;
    public static List<ArrayList<Object>> sourceList = new ArrayList<>();
    public static Map<Long, Item> itemList = new HashMap<>();
    public static Map<Long, Item> unwantedItemList = new HashMap<>();
    public static Map<String, String> itemMatchingList = new HashMap<>();
    public static Map<Long, Long> adeptMatchingList = new HashMap<>();
    public static Map<String, String> itemNamingList = new HashMap<>();
    public static List<String> checkedItemList = new ArrayList<>();
    public static BufferedReader br;
    public static PrintStream errorOutputFile;
    public static PrintStream scriptedWishlistFile; 

    /**
     * the main method reads through the original file and collects data on each
     * roll, concating notes and eliminating duplicates
     *
     * @param args any args needed for the main method, most likely to be a
     * input
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        // Create the error file and output file
        try {
            errorOutputFile = new PrintStream("bin//errors.txt");
            scriptedWishlistFile = new PrintStream("output//WishlistAutomated.txt");
        } catch (FileNotFoundException e) {
            System.out.println("Error creating error file: " + e);
            throw new FileNotFoundException();
        }
        // Try to read in existing enhanced -> normal perk mappings
        try ( BufferedReader reader = new BufferedReader(
                new FileReader(new File("src//main//data//destiny//enhancedMapping.csv")));) {
            reader.readLine(); // skip the header line
            while (reader.ready()) {
                String item = reader.readLine();
                itemMatchingList.put(item.split(",")[0], item.split(",")[1]);
                checkedItemList.add(item.split(",")[0]);
                checkedItemList.add(item.split(",")[1]);
            }
        } catch (Exception e) {
            errorPrint("Unable to read in existing item matching list", e);
            String eol = System.getProperty("line.separator");
            try ( Writer writer = new FileWriter("src/main/data/destiny/enhancedMapping.csv", false);) {
                writer.append("From")
                        .append(',')
                        .append("To")
                        .append(eol);
                writer.flush();
            } catch (Exception er) {
                errorPrint("Unable to save itemMatchingList to .\\data", er);
            }
        }
        // Try to read in item -> name mappings
        try ( BufferedReader reader = new BufferedReader(
                new FileReader(new File("src//main//data//destiny//nameMapping.csv")));) {
            reader.readLine(); // skip the header line
            while (reader.ready()) {
                String item = reader.readLine();
                itemNamingList.put(item.split(",")[0], item.split(",")[1]);
                checkedItemList.add(item.split(",")[0]);
                checkedItemList.add(item.split(",")[1]);
            }
        } catch (Exception e) {
            errorPrint("Unable to read in existing item naming list", e);
            String eol = System.getProperty("line.separator");
            try ( Writer writer = new FileWriter("src/main/data/destiny/nameMapping.csv", false);) {
                writer.append("Item ID")
                        .append(',')
                        .append("Name")
                        .append(eol);
                writer.flush();
            } catch (Exception er) {
                errorPrint("Unable to save itemNamingList to .\\data", er);
            }
        }

        Unirest.config().reset();
        Unirest.config().connectTimeout(5000).socketTimeout(5000).concurrency(10, 5);

        unwantedItemList.put(69420L, new Item(69420L));
        itemList.put(69420L, new Item(69420L));

        try {
            br = new BufferedReader(new FileReader(new File("input//CustomDestinyWishlist.txt")));
            loopRead(br);
        } catch (FileNotFoundException e) {
            errorPrint("Error reading custom withlist file", e);
        }

        HttpResponse<String> response = Unirest
                .get("https://raw.githubusercontent.com/48klocs/dim-wish-list-sources/master/voltron.txt").asString();
        try {
            br = new BufferedReader(new StringReader(response.getBody()));
            loopRead(br);
        } catch (Exception e) {
            errorPrint("Error reading default wishlist from url", e);
        }

        // SORTING ITEMS
        // sort each item in itemList by the perkList, starting with the final entry in each perkList
        // then reorder the noteList, tagList, and mwList accordingly
        for (Map.Entry<Long, Item> item : itemList.entrySet()) {
            // each value has a list of the original list position and the new list position
            List<List<String>> listD = new ArrayList<>(item.getValue().getFullList(1));
            Map<List<String>, Integer> mapPositions = new HashMap<>(); // original ordering. used to reorder the lists
            List<List<String>> tempPerkList = new ArrayList<>();
            List<List<String>> tempNoteList = new ArrayList<>();
            List<List<String>> tempTagsList = new ArrayList<>();
            List<List<String>> tempMWsList = new ArrayList<>();
            for (int i = 0; i < listD.size(); i++) {
                mapPositions.put(listD.get(i), i);
                tempPerkList.add(new ArrayList<>());
                tempNoteList.add(new ArrayList<>());
                tempTagsList.add(new ArrayList<>());
                tempMWsList.add(new ArrayList<>());
            }

            listD.sort((List<String> o1, List<String> o2) -> {
                // we only need to sort by getFullList(1) (the perkSet list), but since the order of perkSets will change, so will the order of notes etc, so the whole item needs to be sorted and then looped through
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

            // you can't sort by note list here becuase notes aren't unique entries so theres no way to map them back to the original list
            // also dim already does this on import, so it would really be for a minor file size reduction
        }

        // TODO - would love to add a second sort here to organize by notes again (happens to be how it's sorted without the above sorting method) to reduce output file size. ideally by size of note so the ones with more information (generally the ones that lists had originally) would be at the top of the list, and therefor easier to see in dim. this would also put anything without notes (usually just collections of perks) at the bottom. could also sort inversely by the number of perksets under each note to achieve a similar affect. would need to see this in action BUT i'm not even sure I need to do this since dim already does this.
        printWishlist();

        // Print the itemMatchingList to a file so I don't need to call HTTP.GET every time I run the script
        // TODO - this is adding items every time even if theyre already in the file, might be best to wipe it and print everything
        String eol = System.getProperty("line.separator");
        try ( Writer writer = new FileWriter("src/main/data/destiny/enhancedMapping.csv", true);) {
            for (Map.Entry<String, String> entry : itemMatchingList.entrySet()) {
                writer.append(entry.getKey())
                        .append(',')
                        .append(entry.getValue())
                        .append(eol);
            }
            writer.flush();
        } catch (Exception e) {
            errorPrint("Unable to save itemMatchingList to .\\data", e);
        }
        // Print the itemNamingList to a file so I don't need to call HTTP.GET every time I run the script
        // TODO - "First in Last out is adding an extra column to the name. Github copilot gave a reason (csv reader issue) but im not sure it's actually correct"
        try ( Writer writer = new FileWriter("src/main/data/destiny/nameMapping.csv", true);) {
            for (Map.Entry<String, String> entry : itemNamingList.entrySet()) {
                writer.append(entry.getKey())
                        .append(',')
                        .append(entry.getValue())
                        .append(eol);
            }
            writer.flush();
        } catch (Exception e) {
            errorPrint("Unable to save itemNamingList to .\\data", e);
        }
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
                    boolean ignoreitem = false;
                    boolean ignoreUnwanteditem = false;
                    if (line.charAt(startKey) == '-') {
                        ignoreitem = true;
                        startKey = 18;
                    }
                    // GATHERING LINE INFORMATION (ITEM, PERKS, NOTES)
                    Long item = Long.parseLong(line.substring(startKey).split("&")[0].split("#")[0]);
                    //? TESTING ONLY
                    if (item!= 69420L
                    && item != 768621510L && item != 999767358L 
                    && item != 2943293195L && item != 2886339027L
                    && item != 1621558458L) {
                        break;
                    }
                    // Convert from adept to normal so they all have the same perks and notes. Convert back when printing so adepts and normals are next to each other in the file
                    if(!adeptMatchingList.containsKey(item)) {
                        Long oldItem = item;
                        try {
                            String name = getName(item.toString());
                            if(name.contains("(Adept)")) {
                                // After checking if the item is adept, find the normal version and convert
                                GetRequest get = Unirest.get(
                                                "https://www.bungie.net/Platform/Destiny2/Armory/Search/DestinyInventoryItemDefinition/{searchTerm}/")
                                        .header("X-API-KEY", "735ad4372078466a8b68a09ff9c02edb");
                                HttpResponse<String> response = get.routeParam("searchTerm", name.split("\s\\(Adept\\)")[0]).asString();

                                JSONObject mJsonObject = new JSONObject(response.getBody());
                                JSONObject userJObject = mJsonObject.getJSONObject("Response");
                                JSONObject statusJObject = userJObject.getJSONObject("results");
                                JSONArray resultSet = statusJObject.getJSONArray("results");
                                for (Object object : resultSet) {
                                    JSONObject jsonObject = (JSONObject) object;
                                    JSONObject itemDefinition = jsonObject.getJSONObject("displayProperties");
                                    if(!itemDefinition.getString("name").contains("(Adept)")) {
                                        item = jsonObject.getLong("hash");
                                    }
                                }
                            }
                        } catch (JSONException e) {
                            errorPrint("Error checking for adept version of item. Probably occurs when checking item type instead of item", e);
                        } finally {
                            // used to get the normal version of an item from the adept version
                            adeptMatchingList.put(oldItem, item);
                        }
                    } else {
                        item = adeptMatchingList.get(item);
                    }
                    Item returnInfo = lineParser(item, line, currentNote, ignoreitem);

                    // TRANSLATE  https://www.light.gg/db/all/?page=1&f=4(3),10(Trait)  TO  https://www.light.gg/db/all/?page=1&f=4(2),10(Trait)
                    List<String> itemPerkList = returnInfo.getItemList(1); // reduce calls of getItemList()
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
                                errorPrint("HTTP Error", e);
                            }
                        }
                        // if itemMatchingList contains itemPerkList.get(i), set tempPerkList to the itemMatchingList (convert dead / incorrect perks to the correct / normal version)
                        if (itemMatchingList.containsKey(itemPerkList.get(i))) {
                            tempPerkList.set(i, itemMatchingList.get(itemPerkList.get(i)));
                        }
                    }
                    returnInfo.setItemList(1, new ArrayList<>(tempPerkList));

                    // CHECK IF ITEM PROPERTIES ARE UNWANTED
                    // 69420 is the key for all items. check if a perk should be ignored on all items
                    for (List<String> unwantedPerkList : unwantedItemList.get(69420L).getFullList(1)) {
                        if (new HashSet<>(returnInfo.getItemList(1)).containsAll(unwantedPerkList)) {
                            ignoreUnwanteditem = true;
                            break;
                        }
                    }
                    // check if ignoring a specific item or a singular perkset
                    if (unwantedItemList.containsKey(item) && !ignoreUnwanteditem) {
                        for (List<String> unwantedPerkList : unwantedItemList.get(item).getFullList(1)) {
                            if (new HashSet<>(returnInfo.getItemList(1)).containsAll(unwantedPerkList)) {
                                ignoreUnwanteditem = true;
                                break;
                            }
                        }
                        // are we ignoring an entire item
                        if (ignoreUnwanteditem || unwantedItemList.get(item).getFullList(1).contains(List.of("-"))) {
                            ignoreUnwanteditem = true;
                        }
                    }
                    // ADD ITEM TO APPROPRIATE LIST
                    if (!ignoreUnwanteditem) {
                        try {
                            if (returnInfo.isIgnoreItem()) {
                                constructLists(returnInfo, unwantedItemList);
                            } else {
                                constructLists(returnInfo, itemList);
                            }
                        } catch (Exception listConstructorException) {
                            errorPrint("Error on line " + line, listConstructorException);
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
    public static void printWishlist() {
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
                    if(!keysList.contains(entry.getKey()))
                        keysList.add(entry.getKey());
                }
            }
            for (Long k: keysList) {
                printWishlistInner(item, k);
            }
        }
        // reset output to console
        scriptedWishlistFile.close();
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
        System.setErr(errorOutputFile);
    }

    /**
     * A helper method for printing, allowing to loop for adept and normal versions of items
     * 
     * @param item the original item to compare to
     * @param key the item id to check for similarity from
     */
    public static void printWishlistInner(Map.Entry<Long, Item> item, Long key) {
        List<List<String>> itemPerkList = item.getValue().getFullList(1);
        List<List<String>> itemNotesList = item.getValue().getFullList(2);
        List<List<String>> itemTagsList = item.getValue().getFullList(3);
        List<List<String>> itemMWsList = item.getValue().getFullList(4);
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
                errorPrint("Unable to get name for item " + key, e, scriptedWishlistFile);
            }
        }
        // ITEM VALUES
        System.out.printf("%n//item %s: %s%n", key, name);
        for (int itemNumber = 0; itemNumber < itemPerkList.size(); itemNumber++) {
            // TAGS
            // gamemode is in beginning, input type is at end
            java.util.Collections.sort(itemTagsList.get(itemNumber), java.util.Collections.reverseOrder());
            // some final formatting change that shouldnt even be necessary but somewhere I'm adding a ' ' or '/' instead of an empty list
            for (int i = 0; i < itemTagsList.get(itemNumber).size(); i++) {
                itemTagsList.get(itemNumber).set(i, itemTagsList.get(itemNumber).get(i).replace(" ", ""));
            }
            for (int k = 0; k < itemNotesList.get(itemNumber).size(); k++) {
                if (itemNotesList.get(itemNumber).get(k).length() < 3) {
                    itemNotesList.get(itemNumber).set(k, "");
                }
            }

            // ITEM DOCUMENTATION IS DIFFERENT
            if (!currentNoteFull.equals(itemNotesList.get(itemNumber))
             || !currentTagsFull.equals(itemTagsList.get(itemNumber))
             || !currentMWsFull.equals(itemMWsList.get(itemNumber))) {
                currentNoteFull = itemNotesList.get(itemNumber);
                currentTagsFull = itemTagsList.get(itemNumber);
                currentMWsFull = itemMWsList.get(itemNumber);
                // NOTES
                System.out.print("//notes:");
                for (int i = 0; i < currentNoteFull.size(); i++) {
                    String note = currentNoteFull.get(i);
                    if (!note.equals("")) {
                        if (note.charAt(0) == (' ')) {
                            note = note.substring(1);
                        }
                        note = note.replace("\"", "");
                        note = note.replace("\\s+", " ");
                        // ascii formatting
                        note = note.replaceAll("[^\\p{ASCII}]", ""); // replace any characters printed as ?
                        note = note.replace("�", "\'");
                        // reverse the outlier changes made earlier
                        note = note.replace("lightggg", "light.gg");
                        note = note.replace("elipsez", "...");
                        note = note.replace("v30", "3.0");
                        System.out.print(note);
                        if (note.charAt(note.length() - 1) == '.') {
                            System.out.print(' ');
                        } else {
                            System.out.print(". ");
                        }
                    }
                }
                // MWS
                if (!itemMWsList.get(itemNumber).isEmpty()) {
                    System.out.print("Recommended MW: ");
                    for (int i = 0; i < itemMWsList.get(itemNumber).size() - 1; i++) {
                        System.out.print(itemMWsList.get(itemNumber).get(i) + ", ");
                    }
                    System.out.print(itemMWsList.get(itemNumber).get(itemMWsList.get(itemNumber).size() - 1) + ". ");
                }
                try {
                    // TAGS
                    if (!currentTagsFull.get(0).equals("")) {
                        // hashsetis a fast way to remove duplicates, however they may have gotten there
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
            if (!itemPerkList.get(itemNumber).isEmpty()) {
                // ITEM
                System.out.print("&perks=");
                // check if there is an item in itemPerkList.get(itemNumber)
                for (int i = 0; i < itemPerkList.get(itemNumber).size() - 1; i++) {
                    System.out.printf("%s,", itemPerkList.get(itemNumber).get(i));
                }
                System.out.printf("%s%n", itemPerkList.get(itemNumber).get(itemPerkList.get(itemNumber).size() - 1));
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
        Unirest.config().reset();
        Unirest.config().connectTimeout(10000).socketTimeout(10000);
        HttpResponse<String> response = Unirest
                .get("https://www.bungie.net/Platform/Destiny2/Manifest/DestinyInventoryItemDefinition/{hashIdentifier}/")
                .header("X-API-KEY", "735ad4372078466a8b68a09ff9c02edb")
                .routeParam("hashIdentifier", hashIdentifier)
                .asString();

        JSONObject itemDefinition = new JSONObject(response.getBody());
        itemDefinition = itemDefinition.getJSONObject("Response");
        itemDefinition = itemDefinition.getJSONObject("displayProperties");

        return itemDefinition.getString("name");
    }

    /**
     * Takes an item and maps it to the appropriate item list. Excludes
     * duplicate perk sets, notes, and tags on duplicate perk sets, include
     * non-duplicate notes and tags
     *
     * @param item An input item
     * @param itemMap A map of item ids to item names
     * @return A mapping of the appropriate item list
     * @throws Exception
     */
    public static Map<Long, Item> constructLists(Item item, Map<Long, Item> itemMap) throws Exception {
        // get the full lists of the given item
        List<List<String>> itemPerks = new ArrayList<>();
        List<List<String>> itemNotes = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        List<List<String>> itemTags = new ArrayList<>();
        List<String> tags = new ArrayList<>();
        List<List<String>> itemMWs = new ArrayList<>();
        List<String> mws = new ArrayList<>();
        if (itemMap.containsKey(item.getItemId())) {
            itemPerks = itemMap.get(item.getItemId()).getFullList(1);
            itemNotes = itemMap.get(item.getItemId()).getFullList(2);
            itemTags = itemMap.get(item.getItemId()).getFullList(3);
            itemMWs = itemMap.get(item.getItemId()).getFullList(4);
        }
        List<String> itemPerkList = item.getItemList(1); // reduce calls of getItemList()

        // translate  https://www.light.gg/db/all/?page=1&f=4(3),10(Trait)  to  https://www.light.gg/db/all/?page=1&f=4(2),10(Trait)
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
                    errorPrint("HTTP Error", e);
                }
            }
            // if itemMatchingList contains itemPerkList.get(i), set tempPerkList to the itemMatchingList (convert dead / incorrect perks to the correct / normal version)
            if (itemMatchingList.containsKey(itemPerkList.get(i))) {
                tempPerkList.set(i, itemMatchingList.get(itemPerkList.get(i)));
            }
        }
        itemPerkList = new ArrayList<>(tempPerkList);

        // is the current perk list stored on the item already
        int perkListIndex = -1;
        for (List<String> perkList : itemPerks) {
            if (perkList.containsAll(itemPerkList)) {
                perkListIndex = itemPerks.indexOf(perkList);
                break;
            }
        }

        // perkListIndex == -1 means item with perks is not already in perkList
        if (perkListIndex == -1) {
            // PERKS
            // if entire item is unwanted, set the perk list to '-'
            // otherwise add item and unwanted perks to perkList
            if (item.isIgnoreItem() && itemPerkList.isEmpty()) {
                item.put(Arrays.asList("-"));
                itemMap.put(item.getItemId(), item);
                return itemMap;
            }
            itemPerks.add(itemPerkList);
            List<List<String>> returnList = constructListsInner(item, notes, tags, mws);
            itemNotes.add(returnList.get(0));
            itemTags.add(returnList.get(1));
            itemMWs.add(returnList.get(2));
        } else {
            // if the item's perk list contains the current perks, only add the notes as an addition to the note list
            notes = itemNotes.get(perkListIndex);
            tags = itemTags.get(perkListIndex);
            mws = itemMWs.get(perkListIndex);
            List<List<String>> returnList = constructListsInner(item, notes, tags, mws);
            itemNotes.set(perkListIndex, returnList.get(0));
            itemTags.set(perkListIndex, returnList.get(1));
            itemMWs.set(perkListIndex, returnList.get(2));
        }
        Item returnItem = new Item(item.getItemId(), itemPerks, itemNotes, itemTags, itemMWs, item.isIgnoreItem());
        itemMap.put(item.getItemId(), returnItem);
        return itemMap;
    }

    /**
     * A helper method to collect an item's information and ensure each
     * itemNumber has a unique set of information
     *
     * @param item the item to collect information from
     * @param notes the list of notes to add to
     * @param tags the list of tags to add to
     * @param mws the list of mws to add to
     * @return a list of lists of notes, tags, and mws
     */
    public static @NotNull List<List<String>> constructListsInner(Item item, List<String> notes, List<String> tags, List<String> mws) {
        List<List<String>> returnList = new ArrayList<>();
        String note = item.getItemList(2).get(0);
        try {
            // TAGS
            for (String string : Arrays.asList(note.split("\\|*tags:")[1].split("\\s*\\,\\s*"))) {
                if (string.equalsIgnoreCase("m+kb")) {
                    string = "mkb";
                }
                // these next two if statements arent an ideal fix, but they work for now
                if (string.charAt(string.length() - 1) == ')') {
                    string = string.substring(0, string.length() - 1);
                }
                if (!tags.contains(string)) {
                    tags.add(string);
                }
            }
            note = note.split("\\|*tags:")[0];
        } catch (Exception notesError) {
            try {
                tags.add(note.split("\\|tags:")[1]);
                note = note.split("\\|*tags:")[0];
            } catch (Exception tagsError) {
                // not an error. just item has no tags
            }
        } finally {
            // NOTES & MW
            Pattern pattern = Pattern.compile("Recommended\\sMW((\\:\\s)|(\\s\\-\\s))[^.]*",
                    Pattern.CASE_INSENSITIVE);
            note = note.replaceAll("\\s{2,}", " ");
            note = note.replace("\\s+.\\s*", "");
            try {
                note = note.replace("light.gg", "lightggg");
                note = note.replace("...", "elipsez");
                note = note.replace("3.0", "v30");
                for (String string : Arrays.asList(note.split("\\.[\\s]+|\"[\\s]*"))) {
                    Matcher matcher = pattern.matcher(string);
                    if (matcher.matches()) {
                        // MW
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
                returnList.add(notes);
                returnList.add(tags);
                returnList.add(mws);
            }
        }
        return returnList;
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
     * @return Item with item number, perks, notes, and other various information
     * @throws Exception acts as a method of catching notes without tags. should
     * never actually throw an exception
     */
    public static Item lineParser(Long item, String line, String currentNote, boolean ignoreitem) throws Exception {
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
                    errorPrint(line, missingInformation3);
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
                // not an error. just item has no tags
            }
        }
        try {
            // NOTES CLEANING FOR FORMATTING
            Matcher matcher = Pattern.compile("Inspired by[^\\.]*\\.\\s*", Pattern.CASE_INSENSITIVE).matcher(notes);
            notes = matcher.replaceAll("");
            if (notes.contains("[YeezyGT")) {
                notes = notes.split("(\\[YeezyGT).*\\]")[1];
            } else if (notes.contains("pandapaxxy")) {
                notes = notes.split("pandapaxxy")[1];
            }
            if (notes.length() > 0 && notes.charAt(0) == (' ')) {
                notes = notes.substring(1);
            }

            // BASIC TAGS
            String itemType = "pv[pe]|m.?kb|controller|gambit";
            Pattern pattern = Pattern.compile("\\((" + itemType + ")(\\s*\\/+\\s*(" + itemType + "))*\\)", Pattern.CASE_INSENSITIVE); // tags in parenthesis
            matcher = pattern.matcher(notes);
            while (matcher.find()) {
                List<String> strArray = Arrays.asList(
                        matcher.group().subSequence(1, matcher.group().length() - 1).toString().split("\\s*\\/\\s*"));
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
            if (!tags.isEmpty()) {
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
            errorPrint("Error with notes " + notes, e);
            throw e;
        }
        Item returnItem = new Item(item);
        returnItem.put(perks, Arrays.asList(notes), tags, new ArrayList<>(), ignoreitem);
        return returnItem;
    }

    /**
     * @param hashIdentifier - the hash of the perk to be checked
     * @throws Exception
     */
    public static void checkPerk(String hashIdentifier) throws Exception {
        try {
            hardCodedCases(hashIdentifier);
            return;
        } catch (Exception e) {
            // For some reason the api doesn't work for the values in here, so I'm just gonna hard code it and ignore the error
            // this really should only occur once for each hashIdentifier
            //errorPrint("Error getting " + hashIdentifier, e);
        }

        HttpResponse<String> response = Unirest
                .get("https://www.bungie.net/Platform/Destiny2/Manifest/DestinyInventoryItemDefinition/{hashIdentifier}/")
                .header("X-API-KEY", "735ad4372078466a8b68a09ff9c02edb")
                .routeParam("hashIdentifier", hashIdentifier)
                .asString();
        JSONObject itemDefinition = new JSONObject(response.getBody());
        itemDefinition = itemDefinition.getJSONObject("Response");
        itemDefinition = itemDefinition.getJSONObject("displayProperties");

        response = Unirest.get(
                "https://www.bungie.net/Platform/Destiny2/Armory/Search/DestinyInventoryItemDefinition/{searchTerm}/")
                .header("X-API-KEY", "735ad4372078466a8b68a09ff9c02edb")
                .routeParam("searchTerm", itemDefinition.getString("name"))
                .asString();

        JSONObject searchDefinition = new JSONObject(response.getBody());
        searchDefinition = searchDefinition.getJSONObject("Response");
        searchDefinition = searchDefinition.getJSONObject("results");
        JSONArray resultSet = searchDefinition.getJSONArray("results");
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
     * value), but I think thats more an issue with the api, not my code. Some
     * of these values could be removed now? (Stuff with only two entries) since
     * I changed some stuff in the checkPerk() method
     *
     * @param perk - the perk to be checked
     *
     * @throws Exception an exception for error handling, shoudln't be a problem
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
                itemMatchingList.put("2848615171", "3865257976");
                itemMatchingList.put("169755979", "3865257976");
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

    /**
     * print any errors to bin\errors folder
     *
     * @param err the (possible) reason for the error
     * @param e the error being thrown
     */
    public static void errorPrint(String err, Exception e) {
        errorPrint(err, e, new PrintStream(new FileOutputStream(FileDescriptor.out))); 
    }

    /**
     * print any errors to bin\errors folder
     *
     * @param err the (possible) reason for the error
     * @param e the error being thrown
     * @param oldStream the stream to return to
     */
    public static void errorPrint(String err, Exception e, PrintStream oldStream) {
        System.setOut(errorOutputFile);
        System.setErr(errorOutputFile);

        System.out.println(err + ": " + e.getMessage());
        e.printStackTrace();
        System.out.println("\n");

        // reset errorOutputFile to console
        errorOutputFile.close();
        System.setOut(oldStream);
        System.setErr(oldStream);
    }
}
