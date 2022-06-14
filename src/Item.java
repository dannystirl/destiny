import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Item {
	// which source is the item from
	private int source;
	private String sourceTitle; 
	private String sourceDescription; 

	// item information 
	private Long itemId;
	private int itemNumber;
	// used to hold each roll, where the key is the item id
	private List<List<String>> itemPerkList;
	// used to hold each roll's notes, where the key is the item id
	private List<List<String>> itemNoteList;
	// used to hold each roll, where the key is the item id
	private List<List<String>> itemTagList;

	/** 
	 * Used to store each items information, including source
	 */
	Item(Long num, int source) {
		itemId = num;
		itemNumber = 0; 
		itemPerkList = new ArrayList<>();
		itemNoteList = new ArrayList<>();
		itemTagList = new ArrayList<>();
	}

	/**
	 * Used to store source information. 
	 */
	Item(int source, String title, String description) {
		itemPerkList = new ArrayList<>();
		itemNoteList = new ArrayList<>();
		itemTagList = new ArrayList<>();
	}

	public void put(int slot, List<String> perks, List<String> notes, List<String> tags) {
		itemPerkList.set(slot, perks);
		itemNoteList.set(slot, notes);
		itemTagList.set(slot, tags);
	}

	public List<List<String>> get(int num) {
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
}
