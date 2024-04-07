package destiny;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores an item and all of its rolls
 */
public class Item {
    // Item information
    private final Long itemId;
    private int countOfItem;
    private final boolean ignoreItem;

    private List<Roll> rolls;

    /**
     * Used to store each item's information
     *
     * @param itemId itemID
     */
    Item(Long itemId) {
        this(itemId, new ArrayList<>(), false);
    }

    /**
     * @param itemId     - Item ID
     * @param rolls      - List of rolls
     * @param ignoreItem - Should the rolls on this item be ignored?
     */
    Item(Long itemId, List<Roll> rolls, Boolean ignoreItem) {
        this.itemId = itemId;
        this.ignoreItem = ignoreItem;
        this.rolls = new ArrayList<>(rolls);
        countOfItem = rolls.size();
    }

    /**
     * Add a roll to the item's roll list
     *
     * @param roll - Roll to add
     */
    public void addRoll(Roll roll) {
        rolls.add(roll);
        countOfItem++;
    }

    /**
     * Find a roll with matching perks if it exists.
     * If it doesn't exist, return a new roll with the perks
     *
     * @param perks - The perks to search for
     * @return Roll
     */
    public Roll getRoll(List<String> perks) {
        return getRollList().stream().filter(roll -> roll.getPerkList().equals(perks)).findFirst().orElse(new Roll(perks));
    }

    /**
     * Get the list of rolls of an item
     *
     * @return List<Roll>
     */
    public List<Roll> getRollList() {
        return rolls;
    }

    /**
     * Set the list of rolls of an item
     *
     * @param rolls - List of rolls
     */
    public void setRollList(List<Roll> rolls) {
        this.rolls = rolls;
    }

    /**
     * @return the ignoreItem
     */
    public boolean isIgnoreItem() {
        return ignoreItem;
    }

    /**
     * @return the itemId
     */
    public Long getItemId() {
        return itemId;
    }

    /**
     * @return the itemNumber
     */
    public int getCountOfItem() {
        return countOfItem;
    }

    /**
     * Print an item and all of its attributes
     */
    public void print() {
        System.out.printf("Item %s [%d occurrences]%n", itemId, countOfItem);
        System.out.println(getRollList().stream().map(Roll::getPerkList));
        System.out.println(getRollList().stream().map(Roll::getNoteList));
        System.out.println(getRollList().stream().map(Roll::getTagList));
        System.out.println();
    }
}

/**
 * Stores a single roll for an item, including the perks, notes, tags, and masterworks
 */
class Roll {
    List<String> perkList;
    List<String> noteList;
    List<String> tagList;
    List<String> mwList;

    Roll(List<String> perks) {
        this(perks, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    Roll(List<String> perks, List<String> notes, List<String> tags, List<String> mw) {
        // TRANSLATE ENHANCED TO NORMAL PERKS
        try {
            perkList = BungieDataParsers.convertEnhancedPerksToNormal(perks);
        } catch (Exception e) {
            Formatters.errorPrint("Error converting enhanced perks to normal perks", e);
            perkList = perks;
        }
        noteList = notes;
        tagList = tags;
        mwList = mw;
    }

    public List<String> getPerkList() {
        return perkList;
    }

    public List<String> getNoteList() {
        return noteList;
    }

    public void addNote(String note) {
        noteList.add(note);
    }

    public List<String> getTagList() {
        return tagList;
    }

    public void addTag(String tag) {
        tagList.add(tag);
    }

    public List<String> getMWList() {
        return mwList;
    }

    public void addMW(String mw) {
        mwList.add(mw);
    }
}