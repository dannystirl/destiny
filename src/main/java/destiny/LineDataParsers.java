package destiny;

import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.AssertionFailedError;

public class LineDataParsers {

    App.RunType runType;

    public Map<Long, Item> unwantedItemList;
    public Map<Long, Item> wantedItemList;
    public List<ArrayList<Object>> sourceList = new ArrayList<>();

    public static Summarizer sentenceAnalyzer;

    enum Masterwork {
        Range("Range"),
        Handling("Handling"),
        Stability("Stability"),
        Reload("Reload"),
        BlastRadius("Blast Radius"),
        Velocity("Velocity"),
        ChargeTime("Charge Time"),
        DrawTime("Draw Time"),
        Impact("Impact");

        final String name;

        Masterwork(String name) {
            this.name = name;
        }

        /**
         * Get the Masterwork by name
         * @param name
         * @return Masterwork || null
         */
        static Masterwork getMasterwork(String name) {
            return Arrays.stream(Masterwork.values()).filter(masterwork1 -> masterwork1.name.equalsIgnoreCase(name)).findFirst().orElse(null);
        }
    }

    static {
        try {
            sentenceAnalyzer = new Summarizer(Formatters.defaultPrintStream);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    LineDataParsers(App.RunType runType) {
        this.unwantedItemList = new HashMap<>();
        this.wantedItemList = new HashMap<>();
        this.runType = runType;
        // Add the default key for all items
        unwantedItemList.put(69420L, new Item(69420L));
        wantedItemList.put(69420L, new Item(69420L));
    }

    /**
     * Check if an item is wanted or unwanted, then add the item to the list it belongs to.
     *
     * @param itemToCheck - The Item to check the wanted status of
     * @throws Exception            - Unable to add item to wanted list
     * @throws AssertionFailedError - Test Only
     */
    public void addItemToWantedList(Item itemToCheck) throws Exception, AssertionFailedError {
        boolean ignoreUnwantedItem = false;
        // 69420 is the key for all items. check if a perk should be ignored on all items
        for (Roll unwantedRoll : unwantedItemList.get(69420L).getRollList()) {
            if (new HashSet<>(itemToCheck.getRollList().get(0).getPerkList()).containsAll(unwantedRoll.getPerkList())) {
                ignoreUnwantedItem = true;
                break;
            }
        }
        // check if ignoring a specific item or a singular perk-set
        if (unwantedItemList.containsKey(itemToCheck.getItemId()) && !ignoreUnwantedItem) {
            for (Roll unwantedRoll : unwantedItemList.get(itemToCheck.getItemId()).getRollList()) {
                if (new HashSet<>(itemToCheck.getRollList().get(0).getPerkList()).containsAll(unwantedRoll.getPerkList())) {
                    ignoreUnwantedItem = true;
                    break;
                }
            }
            // are we ignoring an entire item
            if (ignoreUnwantedItem || unwantedItemList.get(itemToCheck.getItemId()).getRollList().stream().map(Roll::getPerkList).toList().contains(List.of("-"))) {
                ignoreUnwantedItem = true;
            }
        }
        // ADD ITEM TO APPROPRIATE LIST
        if (!ignoreUnwantedItem) {
            try {
                if (itemToCheck.isIgnoreItem()) {
                    updateRollInWantedList(itemToCheck, unwantedItemList);
                } else {
                    updateRollInWantedList(itemToCheck, wantedItemList);
                }
            } catch (Exception listConstructorException) {
                if (runType == App.RunType.NORMAL) {
                    Formatters.errorPrint("Unable to generate list from item", listConstructorException);
                    throw new Exception(listConstructorException);
                } else {
                    throw new AssertionFailedError("Unable to generate list from item");
                }
            }
        }
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
    public Item lineParser(Long itemId, String line, String currentNote, boolean ignoreItem) {
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
                    if (runType == App.RunType.NORMAL) {
                        Formatters.errorPrint(line, missingInformation3);
                    } else {
                        throw new AssertionFailedError("Unable to get perks from line: " + line);
                    }
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
            Pattern pattern = Pattern.compile("\\((" + itemType + ")(\\s*[/\\s\\\\]+\\s*(" + itemType + "))*\\)(:\\s*)*", Pattern.CASE_INSENSITIVE); // tags in parentheses
            Matcher matcher = pattern.matcher(notes);
            while (matcher.find()) {
                String[] tagArray = matcher.group().replace("(", "").replaceAll("\\):\\s*", "").split("\\s*[/\\s\\\\]+\\s*");
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
                String[] strArray = matcher.group().toLowerCase().split("tags:\\s*")[1].split(",");
                for (String str : strArray) {
                    str = Formatters.tagFormatter(str).toLowerCase();
                    if (!tags.contains(str)) {
                        tags.add(str);
                    }
                }
            }
            notes = pattern.matcher(notes).replaceAll("");
        } catch (Exception e) {
            if (runType == App.RunType.NORMAL) {
                Formatters.errorPrint("Error with tags: " + notes, e);
            } else {
                throw new AssertionFailedError("Unable to get notes: " + notes);
            }
        }
        return new Item(itemId, List.of(new Roll(perks, List.of(notes), tags, new ArrayList<>())), ignoreItem);
    }

    /**
     * Takes an item and maps it to the appropriate item list. Excludes
     * duplicate perk sets, notes, and tags on duplicate perk sets, include
     * non-duplicate notes and tags
     *
     * @param item    An input item
     * @param itemMap A map of item ids to item names
     */
    public static void updateRollInWantedList(Item item, Map<Long, Item> itemMap) {
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
                return;
            } else { // Else add item and unwanted perks to perkList
                Roll returnRoll = pullRollFromItem(item, roll);
                itemRollList.add(returnRoll);
            }
        } else {
            // If the item's perk list contains the current perks, only add the notes, tags, and mws to the existing roll
            Roll returnRoll = pullRollFromItem(item, roll);
            itemRollList.set(perkListIndex, returnRoll);
        }
        item.setRollList(itemRollList);
        itemMap.put(item.getItemId(), item);
    }

    /**
     * A helper method to collect an item's information and ensure each
     * itemNumber has a unique set of information
     *
     * @param item the item to collect information from
     * @param roll the roll to collect information from, including notes, tags, and mws
     * @return a list of lists of notes, tags, and mws
     */
    public static Roll pullRollFromItem(Item item, Roll roll) {
        String note = item.getRollList().get(0).getNoteList().get(0);
        try {
            // TAGS
            for (String tag : item.getRollList().get(0).getTagList()) {
                if (!roll.getTagList().contains(tag)) {
                    roll.addTag(tag);
                }
            }
            for (String tag : note.split("\\|*tags:")[1].split("\\s*,\\s*")) {
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
            note = note.replace(" 3.0 ", " v30 ");
            String mwRegex = "(Recommended\\s|\\[){1,25}MW((:\\s)|(\\s-\\s))";
            Pattern pattern = Pattern.compile(String.format("%s[^\\.\\|\\n]*", mwRegex), Pattern.CASE_INSENSITIVE);
            for (String sentence : note.split("\\.\\s+|\"\\s*|]")) {
                // Format note
                Matcher matcher = pattern.matcher(sentence);
                List<Masterwork> formattedMWs = new ArrayList<>();
                String formattedNote = "";
                if (matcher.find()) {
                    // MW
                    String[] noteMwList = matcher.group().split(mwRegex)[1].split("[^\\x00-\\x7F]");
                    formattedMWs = Formatters.mwFormatter(noteMwList[0]);
                    // Note
                    if (noteMwList.length > 1) {
                        formattedNote = Formatters.noteFormatter(noteMwList[1]);
                    } else {
                        formattedNote = "";
                    }
                } else {
                    formattedNote = Formatters.noteFormatter(sentence);
                }
                // Add required items to appropriate lists
                formattedMWs.stream()
                        .filter(formattedMW -> !roll.getMWList().contains(formattedMW))
                        .forEach(roll::addMW);
                if (!(formattedNote.equals("") || roll.getNoteList().contains(formattedNote))) {
                    roll.addNote(formattedNote);
                }
            }
        }
        return roll;
    }
}
