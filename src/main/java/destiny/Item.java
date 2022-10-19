package destiny;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Item {
	// item information
	private final Long itemId;
	private int itemNumber;
	private boolean ignoreItem;

	// List<String> is for a specific perk set
	// List<> is a list of every perk set

	// used to hold each roll, where the key is the item id
	private List<List<String>> itemPerkList;
	// used to hold each roll's notes, where the key is the item id
	private List<List<String>> itemNoteList;
	// used to hold each roll's tags, where the key is the item id
	private List<List<String>> itemTagList;
	// used to hold each roll's masterworks, where the key is the item id
	private List<List<String>> itemMWList;

	/** Used to store each items information
	 * 
	 * @param num itemID */
	Item(Long num) {
		itemId = num;
		itemNumber = 0;
		ignoreItem = false;
		itemPerkList = new ArrayList<>();
		itemNoteList = new ArrayList<>();
		itemTagList = new ArrayList<>();
		itemMWList = new ArrayList<>();
	}

	/** 
	 * @param num itemID
	 * @param perks
	 * @param notes
	 * @param tags
	 * @param bool should the item be ignored */
	Item(Long num, List<List<String>> perks, List<List<String>> notes, List<List<String>> tags, List<List<String>> mw,
			Boolean bool) {
		itemId = num;
		itemNumber = 0;
		ignoreItem = bool;
		itemPerkList = perks;
		itemNoteList = notes;
		itemTagList = tags;
		itemMWList = mw;
	}

	/** Set an item's info on an existing perkset
	 * 
	 * @param num where should the individual item's properties be placed in the array
	 * @param perks will likely be unchanged
	 * @param notes
	 * @param tags
	 * @param mw
	 * @param bool
	 *            should the item be ignored */
	public void put(int num, List<String> perks, List<String> notes, List<String> tags, List<String> mw, Boolean bool) {
		itemPerkList.set(num, perks);
		itemNoteList.set(num, notes);
		itemTagList.set(num, tags);
		itemMWList.set(num, mw);
		ignoreItem = bool;
		itemNumber++;
	}

	/** Set an item's info on a new item
	 * 
	 * @param perks
	 * @param notes
	 * @param tags
	 * @param mw
	 * @param bool should the item go in the ignore list */
	public void put(List<String> perks, List<String> notes, List<String> tags, List<String> mw, Boolean bool) {
		itemPerkList.add(perks);
		itemNoteList.add(notes);
		itemTagList.add(tags);
		itemMWList.add(mw);
		ignoreItem = bool;
		itemNumber++;
	}

	/** Puts an item's perks on a new item, as well as a new note and tag list in the
	 * associated spot
	 * 
	 * @param perks */
	public void put(List<String> perks) {
		put(itemNumber - 1, perks, Arrays.asList(""), Arrays.asList(""), Arrays.asList(""), ignoreItem);
	}

	/** Returns a list of an item properties list
	 * 
	 * @param num is not an index, but is used for a switch statement:
	 *            1 returns the perk list |
	 *            2 returns the note list |
	 *            3 returns the tags list |
	 *            4 returns the mw list
	 * @param list is the list to set to the item's properties
	 */
	public void setFullList(int num, List<List<String>> list) {
		switch (num) {
			case 1 -> itemPerkList = list;
			case 2 -> itemNoteList = list;
			case 3 -> itemTagList = list;
			case 4 -> itemMWList = list;
			default -> {
				
			}
		}
	}

	/** returns a list of an item properties list
	 * 
	 * @param num is not an index, but is used for a switch statement:
	 *            1 returns the perk list |
	 *            2 returns the note list |
	 *            3 returns the tags list |
	 *            4 returns the mw list
	 * @return */
	public List<List<String>> getFullList(int num) {
		return switch (num) {
			case 1 -> itemPerkList;
			case 2 -> itemNoteList;
			case 3 -> itemTagList;
			case 4 -> itemMWList;
			default -> new ArrayList<>();
		}; 
	}

	/** returns a list of a singular item's properties
	 * 
	 * @param num is not an index, but is used for a switch statement:
	 *            1 returns the perk list |
	 *            2 returns the note list |
	 *            3 returns the tags list |
	 *            4 returns the mw list
	 * @return */

	public List<String> getItemList(int num) {
		return switch (num) {
			case 1 -> itemPerkList.get(0);
			case 2 -> itemNoteList.get(0);
			case 3 -> itemTagList.get(0);
			case 4 -> itemMWList.get(0);
			default -> new ArrayList<>();
		}; 
	}

	/** @return the ignoreItem */
	public boolean isIgnoreItem() {
		return ignoreItem;
	}

	/** @return the itemId */
	public Long getItemId() {
		return itemId;
	}

	/** @return the itemNumber */
	public int getItemNumber() {
		return itemNumber;
	}

	/** Print an item and all of its attributes */
	public void print() {
		System.out.printf("Item %s [%d occurances]%n", itemId, itemNumber);
		System.out.println(itemPerkList);
		System.out.println(itemNoteList);
		System.out.println(itemTagList);
		System.out.println();
	}
}