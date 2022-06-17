import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class wishlistGenerator {
	public static int sourceNum;
	public static List<ArrayList<Object>> sourceList = new ArrayList<>();
	public static Map<Long, Item> itemList = new HashMap<>();
	public static Map<Long, Item> unwantedItemList = new HashMap<>();

	/** the main method reads through the original file and collects data on each
	 * roll, concating notes and eliminating duplicates
	 * 
	 * @param item
	 *            the destiny api item number, used as the hash key
	 * @param args
	 *            any args needed for the main method, most likely to be a input
	 *            file
	 * @throws Exception */
	public static void main(String[] args) throws Exception {
		unwantedItemList.put(69420L, new Item(69420L, 0));
		itemList.put(69420L, new Item(69420L, 0));
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(new File("CompleteDestinyWishList.txt")));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new FileNotFoundException();
		}

		ArrayList<Object> td = new ArrayList<>();
		sourceNum = 0; // stores how many rolls a given source has
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
					boolean ignoreitem = false;
					boolean ignoreUnwanteditem = false;
					if (line.charAt(startKey) == '-') {
						ignoreitem = true;
						startKey = 18;
					}
					// GATHERING LINE INFORMATION (ITEM, PERKS, NOTES)
					Long item = Long.parseLong(line.substring(startKey).split("&")[0].split("#")[0]);
					Item returnInfo = lineParser(item, line, currentNote, ignoreitem);

					// 69420 is the key for all items. check if a perk should be ignored on all
					for (List<String> tempList : unwantedItemList.get(69420L).getFullList(1)) {
						if (returnInfo.getItemList(1).containsAll(tempList)) {
							ignoreUnwanteditem = true;
							break;
						}
					}
					// check if ignoring a specific item or a singular perkset
					if (unwantedItemList.containsKey(item) && !ignoreUnwanteditem) {
						for (List<String> tempList : unwantedItemList.get(item).getFullList(1)) {
							if (returnInfo.getItemList(1).containsAll(tempList)) {
								ignoreUnwanteditem = true;
								break;
							}
						}
						// are we ignoring an entire item
						if (ignoreUnwanteditem
								|| unwantedItemList.get(item).getFullList(1).contains(Arrays.asList("-"))) {
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
							System.out.printf("Error %s on line %s%n", listConstructorException.getMessage(), line);
							throw new Exception(listConstructorException);
						}
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

		for (Map.Entry<Long, Item> item : itemList.entrySet()) {
			Long key = item.getKey();
			List<List<String>> itemPerkList = item.getValue().getFullList(1);
			List<List<String>> itemNotesList = item.getValue().getFullList(2);
			List<List<String>> itemTagsList = item.getValue().getFullList(3);
			List<List<String>> itemMWsList = item.getValue().getFullList(4);
			List<String> currentNoteFull = new ArrayList<>();
			List<String> currentTagsFull = new ArrayList<>();
			List<String> currentMWsFull = new ArrayList<>();

			System.out.printf("%n//item %s: %n", key);
			for (int j = 0; j < itemPerkList.size(); j++) {
				// gamemode is in beginning, input type is at end
				java.util.Collections.sort(itemTagsList.get(j), java.util.Collections.reverseOrder());
				// some final formatting change that shouldnt even be necessary but somewhere i'm adding a '/' instead of an empty list
				try {
					for (int k = 0; k < itemTagsList.get(j).size(); k++) {
						itemTagsList.get(j).set(k, itemTagsList.get(j).get(k).replaceAll("\\s", ""));
					}
				} catch (Exception noSpaces) {
				}
				for (int k = 0; k < itemNotesList.get(j).size(); k++) {
					if (itemNotesList.get(j).get(k).length() < 2)
						itemNotesList.get(j).set(k, "");
				}

				// NOTES
				if (!currentNoteFull.equals(itemNotesList.get(j)) || !currentTagsFull.equals(itemTagsList.get(j))
						|| !currentMWsFull.equals(itemMWsList.get(j))) {
					System.out.print("//notes:");
					currentTagsFull = itemTagsList.get(j);
					currentNoteFull = itemNotesList.get(j);
					currentMWsFull = itemMWsList.get(j);
					for (int i = 0; i < itemNotesList.get(j).size(); i++) {
						String note = itemNotesList.get(j).get(i);
						if (!note.equals("") && note.length() > 2) {
							if (note.charAt(0) == ('\"'))
								note = note.substring(1);
							if (note.length() > 0 && note.charAt(0) == (' '))
								note = note.substring(1);
							if (note.contains("\\s\\s"))
								itemNotesList.get(j).set(i, note.replace("\\s\\s", "\\s"));
							if (note.contains("lightggg"))
								note = note.replace("lightggg", "light.gg");
							if (note.contains("elipsez"))
								note = note.replace("elipsez", "...");
							System.out.print(note);
							if (note.charAt(note.length() - 1) == '.')
								System.out.print(' ');
							if (note.charAt(note.length() - 1) != '.')
								System.out.print(". ");
						}
					}
					try {
						for (int i = 0; i < itemMWsList.get(j).size(); i++) {
							System.out.print(itemMWsList.get(j).get(i) + ". ");
						}
					} catch (Exception noMWs) {
						// not an error, just a roll that has no mw information
					}
					try {
						// TAGS
						try {
							for (int k = 0; k < currentTagsFull.size(); k++) {
								currentTagsFull.set(k, currentTagsFull.get(k).replaceAll("\\s", ""));
							}
						} catch (Exception noSpaces) {
						} finally {
							if (!currentTagsFull.get(0).equals("")) {
								// hashsetis a fast way to remove duplicates, however they may have gotten there
								LinkedHashSet<String> linkedHashSet = new LinkedHashSet<>(currentTagsFull);
								System.out.print("|tags:");
								for (int i = 0; i < linkedHashSet.size(); i++) {
									if (i == linkedHashSet.size() - 1)
										System.out.print(linkedHashSet.toArray()[i]);
									else
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
				if (key == 69420L)
					key = -69420L;
				// ITEM
				System.out.printf("dimwishlist:item=%s&perks=", key);
				for (int i = 0; i < itemPerkList.get(j).size() - 1; i++) {
					System.out.printf("%s,", itemPerkList.get(j).get(i));
				}
				System.out.printf("%s%n", itemPerkList.get(j).get(itemPerkList.get(j).size() - 1));
			}
		}
	}

	/** Takes an item and maps it to the appropriate item list.
	 * Excludes duplicate perk sets, notes, and tags
	 * On duplicate perk sets, include non-duplicate notes and tags
	 * 
	 * @param item
	 * @param itemMap
	 * @return */
	public static Map<Long, Item> constructLists(Item item, Map<Long, Item> itemMap) {
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

		// is the current perk list stored on the item already
		int perkListIndex = -1;
		List<String> itemPerkList = item.getItemList(1); // reduce calls of getItemList()
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
			itemPerks.add(item.getItemList(1));

			List<List<String>> returnList = createInnerLists(item, notes, tags, mws);
			itemNotes.add(returnList.get(0));
			itemTags.add(returnList.get(1));
			itemMWs.add(returnList.get(2));
		} else {
			notes = itemNotes.get(perkListIndex);
			tags = itemTags.get(perkListIndex);
			// if the item's perk list contains the current perks, only add the notes as an
			// addition to the note list

			List<List<String>> returnList = createInnerLists(item, notes, tags, mws);
			itemNotes.set(perkListIndex, returnList.get(0));
			itemTags.set(perkListIndex, returnList.get(1));
			itemMWs.set(perkListIndex, returnList.get(2));
		}
		Item returnItem = new Item(item.getItemId(), itemPerks, itemNotes, itemTags, itemMWs, item.isIgnoreItem());
		itemMap.put(item.getItemId(), returnItem);
		return itemMap;
	}

	/** A helper method to collect an item's information ensure each itemNumber has a unique set of information */
	public static List<List<String>> createInnerLists(Item item, List<String> notes, List<String> tags,
			List<String> mws) {
		List<List<String>> returnList = new ArrayList<>();
		String note = item.getItemList(2).get(0);
		try {
			// TAGS
			for (String string : Arrays.asList(note.split("\\|*tags:")[1].split("\\s*\\,\\s*"))) {
				if (string.equalsIgnoreCase("m+kb"))
					string = "mkb";
				// these next two if statements arent an ideal fix, but they work for now
				if (string.charAt(string.length() - 1) == ')')
					string = string.substring(0, string.length() - 1);
				if (!tags.contains(string))
					tags.add(string);
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
			Pattern pattern = Pattern.compile("Recommended\\sMW(\\:\\s|\\s\\-\\s)[^.]*", Pattern.CASE_INSENSITIVE);
			note = note.replace("\\s+.\\s*", "");
			try {
				note = note.replace("light.gg", "lightggg");
				note = note.replace("...", "elipsez");
				for (String string : Arrays.asList(note.split("\\.\\s|\"\\s|\\.|\""))) {
					Matcher matcher = pattern.matcher(string);
					if (matcher.matches()) {
						mws.add(matcher.group());
					} else if (!notes.contains(string))
						notes.add(string);
				}
			} catch (Exception e) {
				Matcher matcher = pattern.matcher(note);
				if (matcher.matches()) {
					mws.add(matcher.group());
				} else if (!notes.contains(note))
					notes.add(note);
			} finally {
				returnList.add(notes);
				returnList.add(tags);
				returnList.add(mws);
			}
		}
		return returnList;
	}

	/** Takes a line and extracts perk, note, and tag information
	 * 
	 * @param item
	 * @param line
	 * @param currentNote
	 *            if item is imported en mass, the note from a similar previous item
	 *            is used instead
	 * @param ignoreitem
	 *            should an item or it's perk list be excluded from the list
	 * @return
	 * @throws Exception
	 *             acts as a method of catching notes without tags. should never
	 *             actually throw an exception */
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
				notes = null;
			} catch (Exception missingInformation2) {
				try {
					notes = line.split("#notes:")[1]; // desired notes of item with no perks
				} catch (Exception missingInformation3) {
					// System.out.println("/" + "/Unable to format "+line+" in perk collection");
				}
			}
		}
		if (perks.size() == 5) {
			// get rid of origin traits since they're static and just clog up the perk list
			perks = perks.subList(0, 4);
		}
		if (item == 69420L)
			// -69420 is a key to apply a wanted/unwanted set of perks to all items, so this
			// is simply to offset that negative
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
				notes = "\\|tags:" + notes.split("\\|*tags:")[1];
			} catch (Exception notesError) {
				// System.out.println("/" + "/Unable to format notes: " + notes);
			}
		}
		try {
			Matcher matcher = Pattern.compile("Inspired by[^\\.]*\\.", Pattern.CASE_INSENSITIVE).matcher(notes);
			notes = matcher.replaceAll("");
			if (notes.contains("[YeezyGT")) {
				notes = notes.split("(\\[YeezyGT).*[\\]]")[1];
			} else if (notes.contains("pandapaxxy")) {
				notes = notes.split("pandapaxxy")[1];
			}
			if (notes.length() > 0 && notes.charAt(0) == (' '))
				notes = notes.substring(1);
			if (notes.contains("’"))
				notes = notes.replace("’", "\'");

			String itemType = "pv[pe]|m.{0,1}kb|controller|gambit";
			Pattern pattern = Pattern.compile("\\((" + itemType + ")(\\s*[/]+\\s*(" + itemType + "))*\\)",
					Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(notes);
			while (matcher.find()) {
				List<String> strArray = Arrays.asList(
						matcher.group().subSequence(1, matcher.group().length() - 1).toString().split("\\s*[/]\\s*"));
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

			if (notes.length() > 0 && notes.charAt(0) == (' '))
				notes = notes.substring(1);
			if (notes.contains("\\s\\s"))
				notes = notes.replace("\\s\\s", "\\s");
		} catch (Exception e) {
			System.out.println(e + ": " + notes);
			throw e;
		}
		Item returnItem = new Item(item, sourceNum);
		returnItem.put(perks, Arrays.asList(notes), tags, new ArrayList<>(), ignoreitem);
		return returnItem;
	}
}