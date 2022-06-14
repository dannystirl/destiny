import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class wishlistGenerator {
	public static Map<Long, Item> itemList = new HashMap<>();
	public static Map<Long, Item> unwantedItemList = new HashMap<>();

	/**
	 * the main method reads through the original file and collects data on each
	 * roll, concating notes and eliminating duplicates
	 * 
	 * @param item
	 *            the destiny api item number, used as the hash key
	 * @param args
	 *            any args needed for the main method, most likely to be a input
	 *            file
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		unwantedItemList.put(69420L, new Item(69420L, 0));
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(new File("CompleteDestinyWishList.txt")));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new FileNotFoundException();
		}
		Item sources = new Item(0, "A list of desirable rolls created by the community",
				"This list was generated from a collection of community rolls. Individual sources and users can be found within the list files. ")
		int sourceNum = 0; // stores how many rolls a given source has
		String currentNote = ""; // used to store an item's notes, either per roll or per item
		do {
			String line = br.readLine();
			switch (line.split(":")[0]) {
				case "title":
					td = new ArrayList<>();
					td.add(sourceNum++);
					td.add(line.split(":")[1]);
					break;
				case "description":
					td.add(line.split(":")[1]);
					sourceList.add(td);
					// bug: is taking "//" as a new line character instead of a string
					break;
				case "dimwishlist":
					int startKey = 17; // where the item id lies
					// the naming of these seems to be backwards, but I think ignoreItem is for any
					// unwanted item and ignoreUnwantedItem is for any item we only want the notes
					// for
					boolean ignoreitem = false;
					boolean ignoreUnwanteditem = false;
					if (line.charAt(startKey) == '-') {
						ignoreitem = true;
						startKey = 18;
					}
					// GATHERING LINE INFORMATION (ITEM, PERKS, NOTES)
					Long item = Long.parseLong(line.substring(startKey).split("&")[0].split("#")[0]);
					List<List<String>> returnInfo = lineParser(item, line, currentNote, ignoreitem);

					if (item != 1216319404L)
						break;

					// roll perks
					List<String> perks = new ArrayList<>();
					perks.addAll(returnInfo.get(0));

					// roll notes
					String notes = returnInfo.get(1).get(0);
					List<String> tags = new ArrayList<>();
					tags.addAll(returnInfo.get(2));
					// unwanted roll/item ?
					ignoreitem = Boolean.parseBoolean(returnInfo.get(3).get(0));
					// 69420 is the key for all items. check if a perk should be ignored on all
					for (List<String> tempList : unwantedItems.get(69420L)) {
						if (perks.containsAll(tempList)) {
							ignoreUnwanteditem = true;
							break;
						}
					}
					// check if ignoring a specific item or a singular perkset
					if (unwantedItems.containsKey(item) && !ignoreUnwanteditem) {
						for (List<String> tempList : unwantedItems.get(item)) {
							if (perks.containsAll(tempList)) {
								ignoreUnwanteditem = true;
								break;
							}
						}
						if (ignoreUnwanteditem || unwantedItems.get(item).contains(Arrays.asList("-"))) {
							ignoreUnwanteditem = true;
						}
					}
					// ADD ITEM TO APPROPRIATE LIST
					try {
						if (ignoreitem) {
							List<Map<Long, ArrayList<List<String>>>> returnList = constructLists(item, perks, notes, tags, ignoreUnwanteditem,
									true, unwantedItems, unwantedRollsNotes);
							unwantedItems = returnList.get(0);
							unwantedRollsNotes = returnList.get(1);
						} else if (!ignoreUnwanteditem && !ignoreitem) {
							List<Map<Long, ArrayList<List<String>>>> returnList = constructLists(item, perks, notes, tags, ignoreUnwanteditem,
									false, itemAndRolls, itemRollsNotes);
							itemAndRolls = returnList.get(0);
							itemRollsNotes = returnList.get(1);
						}
					} catch (Exception listConstructorException) {
						System.out.printf("Error %s on line %s%n", listConstructorException.getMessage(), line);
						throw new Exception(listConstructorException);
					}
					break;
				case "//notes":
					startKey = 8;
					if (line.charAt(8) == (' '))
						startKey = 9;
					currentNote = line.substring(startKey);
					break;
				default:
					break;
			}
		} while (br.ready());

		System.out.printf("title:%s%n", sourceList.get(0).get(1));
		System.out.printf("description:%s%n%n", sourceList.get(0).get(2));
		// print wishlist rolls
		// trashlist rolls don't need to be printed since they're all excluded during
		// list creation
		for (Map.Entry<Long, ArrayList<List<String>>> item : itemAndRolls.entrySet()) {
			Long key = item.getKey();
			ArrayList<List<String>> itemPerkList = itemAndRolls.get(key);
			ArrayList<List<String>> itemNotesList = itemRollsNotes.get(key);
			ArrayList<List<String>> itemTagsList = itemTags.get(key);
			List<String> currentNoteFull = new ArrayList<>();
			List<String> currentTagsFull = new ArrayList<>();

			System.out.printf("%n//item %s: %n", key);
			for (int j = 0; j < itemPerkList.size(); j++) {
				try {
					java.util.Collections.sort(itemTagsList.get(j), java.util.Collections.reverseOrder());
				} catch (java.lang.IndexOutOfBoundsException e) {
					/* no tags */
				}
				// BUG: doesnt seem to keep current note for the first few items. only tags.
				if (!currentNoteFull.equals(itemNotesList.get(j)) && !currentTagsFull.equals(itemTagsList.get(j))) {
					currentNoteFull = itemNotesList.get(j);
					currentTagsFull = itemTagsList.get(j);
					System.out.print("//notes:");
					for (int i = 0; i < itemNotesList.get(j).size(); i++) {
						String note = itemNotesList.get(j).get(i);
						if (!note.equals("") && note.length() > 2 && !currentNote.equals(note)) {
							currentNote = note;
							if (note.charAt(0) == (' '))
								note = note.substring(1);
							if (note.contains("\\s\\s"))
								itemNotesList.get(j).set(i, note.replace("\\s\\s", "\\s"));
							System.out.print(note);
							if (note.charAt(note.length() - 1) == '.')
								System.out.print(' ');
							if (note.charAt(note.length() - 1) != '.')
								System.out.print(". ");
						}
					}
					try {
						if (!itemTagsList.get(j).get(0).equals("")) {
							System.out.print("|tags:");
							for (int i = 0; i < itemTagsList.get(j).size() - 1; i++) {
								System.out.printf("%s, ", itemTagsList.get(j).get(i));
							}
							System.out.printf("%s", itemTagsList.get(j).get(itemTagsList.get(j).size() - 1));

						}
					} catch (IndexOutOfBoundsException e) {
						// item has no tags
					} finally {
						System.out.println();
					}
				}
				System.out.printf("dimwishlist:item=%s&perks=", key);
				for (int i = 0; i < itemPerkList.get(j).size() - 1; i++) {
					System.out.printf("%s,", itemPerkList.get(j).get(i));
				}
				System.out.printf("%s%n", itemPerkList.get(j).get(itemPerkList.get(j).size() - 1));
			}
		}
	}

	public static List<Map<Long, ArrayList<List<String>>>> constructLists(Long item, List<String> perks, String notes, List<String> tags,
			boolean ignoreUnwanteditem,
			boolean ignoreItem, Map<Long, ArrayList<List<String>>> itemRolls, Map<Long, ArrayList<List<String>>> itemNotes) {
		// gets the list of perksets of an unwanted item
		if (!itemRolls.containsKey(item)) {
			itemRolls.put(item, new ArrayList<>());
		}
		ArrayList<List<String>> rollList = itemRolls.get(item);
		// gets the list of notes of an item
		if (!itemNotes.containsKey(item)) {
			itemNotes.put(item, new ArrayList<>());
		}
		ArrayList<List<String>> noteList = itemNotes.get(item);
		// gets the list of tags of an item
		if (!itemTags.containsKey(item)) {
			itemTags.put(item, new ArrayList<>());
		}
		ArrayList<List<String>> tagList = itemTags.get(item);

		// is the current perk list stored on the item already
		int perkListIndex = -1;
		for (List<String> perkList : rollList) {
			if (perkList.containsAll(perks)) {
				ignoreUnwanteditem = true;
				perkListIndex = itemRolls.get(item).indexOf(perkList);
				break;
			}
		}

		// perkListIndex == -1 means item is not already in perkList
		if (perkListIndex == -1) {
			// if entire item is unwanted, set the perk list to '-'
			// otherwise add item and unwanted perks to perkList
			if (ignoreItem) {
				if (perks.isEmpty())
					perks = Arrays.asList("-");
				// if an entire item is being ignored, we dont need to add specific perks"
				if (rollList.contains(Arrays.asList("-"))) {
					List<Map<Long, ArrayList<List<String>>>> returnList = new ArrayList<>();
					returnList.add(itemRolls);
					returnList.add(itemNotes);
					return returnList;
				}
			}
			// the issue seems to be that sometimes there arent any tags, so if nothing gets
			// added, the ordering gets messed up.
			rollList.add(perks);
			itemRolls.put(item, rollList);
			// add notes to a new note list
			List<String> newNotes = new ArrayList<>();
			try {
				if (notes.split("\\|tags:")[1].contains(",")) {
					for (String string : Arrays.asList(notes.split("\\|tags:")[1].split(","))) {
						if (string.equalsIgnoreCase("m+kb"))
							string = "mkb";
						if (!tags.contains(string))
							tags.add(string);
					}
				} else {
					if (!tags.contains(notes.split("\\|tags:")[1]))
						tags.add(notes.split("\\|tags:")[1]);
				}
				tagList.add(rollList.indexOf(perks), tags);
				itemTags.put(item, tagList);
				notes = notes.split("\\|tags:")[0];
			} catch (Exception notesError) {
				// no tags in notes. not an error.
				tagList.add(tags);
				itemTags.put(item, tagList);
			} finally {
				notes = notes.replace("\\s*.\\s*", "");
				try {
					for (String string : Arrays.asList(notes.split("^(light\\.gg)(\\.[^\\.])"))) {
						if (string.equalsIgnoreCase("m+kb"))
							string = "mkb";
						if (!newNotes.contains(string))
							newNotes.add(string);
					}
				} catch (Exception e) {
					newNotes.add(notes);
				} finally {
					noteList.add(rollList.indexOf(perks), newNotes);
					itemNotes.put(item, noteList);
				}
			}
		} else {
			// if the item's perk list contains the current perks, only add the notes as an
			// addition to the note list
			List<String> oldNotes = noteList.get(perkListIndex);
			try {
				if (notes.split("\\|tags:")[1].contains(",")) {
					for (String string : Arrays.asList(notes.split("\\|tags:")[1].split(","))) {
						if (!tags.contains(string))
							tags.add(string);
					}
				} else {
					if (!tags.contains(notes.split("\\|tags:")[1]))
						tags.add(notes.split("\\|tags:")[1]);
				}
				tagList.set(perkListIndex, tags);
				itemTags.put(item, tagList);
				notes = notes.split("\\|tags:")[0];
			} catch (Exception notesError) {
				// no tags in notes. not an error.
				tagList.set(perkListIndex, new ArrayList<>());
				itemTags.put(item, tagList);
			} finally {
				notes = notes.replace("\\s*.\\s*", "");
				try {
					for (String string : Arrays.asList(notes.split("^(light\\.gg)(\\.[^\\.])"))) {
						if (!oldNotes.contains(string))
							oldNotes.add(string);
					}
				} catch (Exception e) {
					oldNotes.add(notes);
				} finally {
					noteList.set(perkListIndex, oldNotes);
					itemNotes.put(item, noteList);
				}
			}
		}

		List<Map<Long, ArrayList<List<String>>>> returnList = new ArrayList<>();
		returnList.add(itemRolls);
		returnList.add(itemNotes);
		return returnList;
	}

	public static List<List<String>> lineParser(Long item, String line, String currentNote, boolean ignoreitem) throws Exception {
		List<String> perks = new ArrayList<>();
		String notes;
		List<String> tags = new ArrayList<>();
		try {
			perks = Arrays.asList(line.split("&perks=")[1].split("#notes:")[0].split(",")); // desired perks
			notes = line.split("#notes:")[1]; // notes
		} catch (Exception missingInformation) {
			try {
				perks = Arrays.asList(line.split("&perks=")[1].split(",")); // desired perks with no notes
				notes = null;
			} catch (Exception missingInformation2) {
				try {
					notes = line.split("#notes:")[1]; // desired notes of item with no perks
				} catch (Exception missingInformation3) {
					System.out.println("/" + "/Unable to format " + line + " in perk collection");
					throw new Exception();
				}
			}
		}
		if (perks.size() == 5) {
			// get rid of origin traits since they're static and just clog up the perk list
			perks = perks.subList(0, 4);
		}
		if (item == 69420L)
			// -69420 is a key to apply a wanted set of perks to all items, so this is
			// simply to offset that negative
			ignoreitem = false;
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
		if (notes == null)
			notes = currentNote;
		if (notes.contains("auto generated")) {
			try {
				notes = "|tags:" + notes.split("\\|tags:")[1];
			} catch (Exception notesError) {
				// System.out.println("/" + "/Unable to format notes: " + notes);
			}
		}
		try {
			if (notes.contains("Inspired by")) {
				notes = notes.split("Inspired by.*[.]")[1];
			} else if (notes.contains("[YeezyGT")) {
				notes = notes.split("(\\[YeezyGT).*[]]")[1];
			} else if (notes.contains("pandapaxxy")) {
				notes = notes.split("pandapaxxy")[1];
			}
			if (notes.charAt(0) == (' '))
				notes = notes.substring(1);
			if (notes.contains("’"))
				notes = notes.replace("’", "\'");

			String itemType = "pv[pe]|m.{0,1}kb|controller|gambit";
			Pattern pattern = Pattern.compile("\\((" + itemType + ")(\\s*[/]+\\s*(" + itemType + "))*\\)", Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(notes);
			while (matcher.find()) {
				List<String> strArray = Arrays.asList(matcher.group().subSequence(1, matcher.group().length() - 1).toString().split("\\s*[/]\\s*"));
				for (String str : strArray) {
					if (str.equalsIgnoreCase("m+kb"))
						str = "mkb";
					if (!tags.contains(str.toLowerCase())) {
						tags.add(str.toLowerCase());
					}
				}
			}
			String temp = "";
			for (String string : notes.split("(?i)\\((" + itemType + ")(\\s*[/]+\\s*(" + itemType + "))*\\):*")) {
				temp += string;
			}
			notes = temp;

			if (notes.charAt(0) == (' '))
				notes = notes.substring(1);
			if (notes.contains("\\s\\s"))
				notes = notes.replace("\\s\\s", "\\s");
		} catch (Exception e) {

		}

		List<List<String>> returnList = new ArrayList<>();
		returnList.add(perks);
		returnList.add(Arrays.asList(notes));
		returnList.add(tags);
		returnList.add(Arrays.asList(String.valueOf(ignoreitem)));
		return returnList;
	}
}