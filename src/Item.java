import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Item {
	// which source is the item from
	private int source;

	// item information
	private Long itemId;
	private int itemNumber;
	private boolean ignoreItem;

	// List<String> is for a specific perk set
	// List<> is a list of every perk set

	// used to hold each roll, where the key is the item id
	private List<List<String>> itemPerkList;
	// used to hold each roll's notes, where the key is the item id
	private List<List<String>> itemNoteList;
	// used to hold each roll, where the key is the item id
	private List<List<String>> itemTagList;

	/**
	 * Used to store each items information, including source
	 * 
	 * @param num
	 * @param source
	 */
	Item(Long num, int source) {
		itemId = num;
		this.source = source;
		itemNumber = 0;
		ignoreItem = false;
		itemPerkList = new ArrayList<>();
		itemNoteList = new ArrayList<>();
		itemTagList = new ArrayList<>();
	}

	/**
	 * @param num
	 * @param perks
	 * @param notes
	 * @param tags
	 * @param bool
	 */
	Item(Long num, List<List<String>> perks, List<List<String>> notes, List<List<String>> tags, Boolean bool) {
		itemId = num;
		itemNumber = 0;
		ignoreItem = bool;
		itemPerkList = perks;
		itemNoteList = notes;
		itemTagList = tags;
	}

	/**
	 * Set an item's info on an existing perkset
	 * 
	 * @param num
	 *            where should the individual item's properties be placed in the
	 *            array
	 * @param perks
	 *            will likely be unchanged
	 * @param notes
	 * @param tags
	 * @param bool
	 *            should the item be ignored
	 */
	public void put(int num, List<String> perks, List<String> notes, List<String> tags, Boolean bool) {
		itemPerkList.set(num, perks);
		itemNoteList.set(num, notes);
		itemTagList.set(num, tags);
		ignoreItem = bool;
		itemNumber++;
	}

	/**
	 * 
	 * Set an item's info on a new item
	 * 
	 * @param perks test
	 * @param notes
	 * @param tags
	 * @param bool
	 *            should the item go in the ignore list
	 */
	public void put(List<String> perks, List<String> notes, List<String> tags, Boolean bool) {
		itemPerkList.add(perks);
		itemNoteList.add(notes);
		itemTagList.add(tags);
		ignoreItem = bool;
		itemNumber++;
	}

	/**
	 * Puts an item's perks on a new item, as well as a new note and tag list in the
	 * associated spot
	 * 
	 * @param perks
	 */
	public void put(List<String> perks) {
		put(itemNumber - 1, perks, Arrays.asList(""), Arrays.asList(""), ignoreItem);
	}

	/**
	 * returns a list of an item properties list
	 * 
	 * @param num
	 *            1 returns the perk list |
	 *            2 returns the note list |
	 *            3 returns the tag list
	 * @return
	 */
	public List<List<String>> getFullList(int num) {
		switch (num) {
			case 1:
				return itemPerkList;
			case 2:
				return itemNoteList;
			case 3:
				return itemTagList;
			default:
				return new ArrayList<>();
		}
	}

	/**
	 * returns a list of a singular item's properties
	 * 
	 * @param num
	 *            1 returns the perk list |
	 *            2 returns the note list |
	 *            3 returns the tag list
	 * @return
	 */

	public List<String> getItemList(int num) {
		switch (num) {
			case 1:
				return itemPerkList.get(0);
			case 2:
				return itemNoteList.get(0);
			case 3:
				return itemTagList.get(0);
			default:
				return new ArrayList<>();
		}
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
	public int getItemNumber() {
		return itemNumber;
	}

	/**
	 * Print an item and all of its attributes
	 */
	public void print() {
		System.out.printf("Item %s [%d occurances]%n", itemId, itemNumber);
		System.out.println(itemPerkList);
		System.out.println(itemNoteList);
		System.out.println(itemTagList);
	}
}