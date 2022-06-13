import java.util.*;
import java.io.*;

public class wishlistGenerator {
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
		ArrayList<ArrayList> sourceList = new ArrayList(); // used to place each source and their description
		// used to hold each roll, where the key is the item id
		HashMap<Long, ArrayList<List<String>>> itemAndRolls = new HashMap();
		// used to hold each roll's notes, where the key is the item id
		HashMap<Long, ArrayList<List<String>>> itemRollsNotes = new HashMap();
		// used to hold each unwanted roll, where the key is the item id
		HashMap<Long, ArrayList<List<String>>> unwantedItems = new HashMap();
		// used to hold each roll's notes, where the key is the item id
		HashMap<Long, ArrayList<List<String>>> unwantedRollsNotes = new HashMap();
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(new File("CompleteDestinyWishList.txt")));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new FileNotFoundException();
		}
		ArrayList td = new ArrayList<>();
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
					boolean ignoreitem = false;
					if (line.charAt(startKey) == '-') {
						ignoreitem = true;
						startKey = 18;
					}
					Long item = Long.parseLong(line.substring(startKey).split("&")[0].split("#")[0]);
					List<String> perks = new ArrayList();
					String notes = null;
					try {
						perks = Arrays.asList(line.split("&perks=")[1].split("#notes:")[0].split(",")); // desired perks
						notes = line.split("#notes:")[1]; // notes
					} catch (Exception missingInformation) {
						try {
							perks = Arrays.asList(line.split("&perks=")[1].split(",")); // desired perks with no notes
						} catch (Exception missingInformation2) {
							try {
								notes = line.split("#notes:")[1]; // desired notes of item with no perks
							} catch (Exception missingInformation3) {
								System.out.println("Unable to format " + line + " in perk collection");
								throw new Exception();
							}
						}
					}
					if (perks.size() == 5) {
						// get rid of origin traits since they're static and just clog up the perk list
						perks = perks.subList(0, 3);
					}
					if (notes == null)
						notes = currentNote;
					try {
						if (ignoreitem) {
							// gets the list of perksets of an unwanted item
							if (!unwantedItems.containsKey(item)) {
								unwantedItems.put(item, new ArrayList<List<String>>());
							}
							ArrayList<List<String>> unwantedList = unwantedItems.get(item);
							// gets the list of notes of an item
							if (!unwantedRollsNotes.containsKey(item)) {
								unwantedRollsNotes.put(item, new ArrayList<List<String>>());
							}
							ArrayList<List<String>> unwantedNoteList = unwantedRollsNotes.get(item);

							if (!unwantedList.contains(perks)) {
								// if the perk list does not contain the current perks, add them as a list to
								// the item
								unwantedList.add(perks);
								unwantedItems.put(item, unwantedList);
								// add notes to a new note list
								if (!notes.contains("auto generated")) {
									List<String> newNotes = new ArrayList();
									newNotes.add(notes);
									unwantedNoteList.add(unwantedList.indexOf(perks), newNotes);
								} else {
									unwantedNoteList.add(unwantedList.indexOf(perks), new ArrayList());
								}
								itemRollsNotes.put(item, unwantedNoteList);
							} else {
								// if the item's perk list contains the current perks, only add the notes as an
								// addition to the note list
								List<String> oldNotes = unwantedNoteList.get(unwantedList.indexOf(perks)); // each perkset needs
								// to have its own
								// note position
								if (!oldNotes.contains(notes) && !notes.contains("auto generated")) {
									oldNotes.add(notes);
									unwantedNoteList.set(unwantedList.indexOf(perks), oldNotes);
								}
								itemRollsNotes.put(item, unwantedNoteList);
							}
						} else {
							// gets the list of perksets of an item
							if (!itemAndRolls.containsKey(item)) {
								itemAndRolls.put(item, new ArrayList<List<String>>());
							}
							ArrayList<List<String>> rollList = itemAndRolls.get(item);
							// gets the list of notes of an item
							if (!itemRollsNotes.containsKey(item)) {
								itemRollsNotes.put(item, new ArrayList<List<String>>());
							}
							ArrayList<List<String>> noteList = itemRollsNotes.get(item);

							if (!rollList.contains(perks)) {
								// if the perk list does not contain the current perks, add them as a list to
								// the item
								rollList.add(perks);
								itemAndRolls.put(item, rollList);
								// add notes to a new note list
								if (!notes.contains("auto generated")) {
									List<String> newNotes = new ArrayList();
									newNotes.add(notes);
									noteList.add(rollList.indexOf(perks), newNotes);
								} else {
									noteList.add(rollList.indexOf(perks), new ArrayList());
								}
								itemRollsNotes.put(item, noteList);
							} else {
								// if the item's perk list contains the current perks, only add the notes as an
								// addition to the note list
								List<String> oldNotes = noteList.get(rollList.indexOf(perks)); // each perkset needs
																								// to have its own
																								// note position
								if (!oldNotes.contains(notes) && !notes.contains("auto generated")) {
									oldNotes.add(notes);
									noteList.set(rollList.indexOf(perks), oldNotes);
									itemRollsNotes.put(item, noteList);
								}
							}
						}
					} catch (Exception e) {
						System.out.printf("Error %s on line %s%n", e.getMessage(), line);
						if (e.getMessage().contains("1")) {
							// either no perks or no notes set.
						} else
							System.out.println("Unable to format " + line);
						throw new Exception(e);
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
		/* for (Map.Entry<Long, ArrayList<List<String>>> item : itemAndRolls.entrySet()) {
			Long key = item.getKey();
			ArrayList<List<String>> itemPerkList = item.getValue();
			ArrayList<List<String>> itemNotesList = itemRollsNotes.get(key);
			System.out.printf("item: %s %n", key);
			for (int i = 0; i < itemPerkList.size() - 1; i++) {
				// print item and perks
				System.out.printf("dimwishlist:item=%s&perks=", key);
				for (int j = 0; j < itemPerkList.get(i).size() - 1; j++) {
					System.out.print(itemPerkList.get(i).get(j) + ",");
				}
				System.out.print(itemPerkList.get(i).get(itemPerkList.get(i).size() - 1) + "#notes:");

				// print notes
				for (int j = 0; j < itemNotesList.get(i).size() - 1; j++) {
					System.out.print(itemNotesList.get(i).get(j) + ". ");
				}
				System.out.println();
			}
		} */
	}
}