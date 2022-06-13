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
		HashMap<Long, ArrayList<String>> itemRollsNotes = new HashMap();
		// used to hold each unwanted roll, where the key is the item id
		HashMap<Long, ArrayList<List<String>>> unwantedItems = new HashMap();
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(new File("CompleteDestinyWishList.txt")));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new FileNotFoundException();
		}
		ArrayList td = new ArrayList<>();
		int sourceNum = 0;
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
					if (notes == null)
						notes = currentNote;
					try {
						// gets the list of perksets of an item
						if (!itemAndRolls.containsKey(item)) {
							itemAndRolls.put(item, new ArrayList<List<String>>());
						}
						ArrayList<List<String>> rollList = itemAndRolls.get(item);
						// gets the list of notes of an item
						if (!itemRollsNotes.containsKey(item)) {
							itemRollsNotes.put(item, new ArrayList<String>());
						}
						ArrayList<String> noteList = itemRollsNotes.get(item);
						// gets the list of perksets of an unwanted item
						if (!unwantedItems.containsKey(item)) {
							unwantedItems.put(item, new ArrayList<List<String>>());
						}
						ArrayList<List<String>> unwantedList = unwantedItems.get(item);

						if (ignoreitem) {
							noteList.add(notes);
							unwantedList.add(perks);
							if (!unwantedList.contains(perks)) {
								// if the perk list does not contain the current perks, add them as a list to
								// the item
								unwantedItems.put(item, unwantedList);
								itemRollsNotes.put(item, noteList);
							} else {
								// if the item's perk list contains the current perks, only add the notes as an
								// addition to the note list
								noteList = itemRollsNotes.get(item);
								itemRollsNotes.put(item, noteList);
							}
						} else {
							noteList.add(notes);
							rollList.add(perks);
							if (!rollList.contains(perks)) {
								// if the perk list does not contain the current perks, add them as a list to
								// the item
								itemAndRolls.put(item, rollList);
								itemRollsNotes.put(item, noteList);
							} else {
								// if the item's perk list contains the current perks, only add the notes as an
								// addition to the note list
								noteList = itemRollsNotes.get(item);
								itemRollsNotes.put(item, noteList);
							}
						}
					} catch (ArrayIndexOutOfBoundsException e) {
						System.out.printf("Error %s on line %s%n", e.getMessage(), line);
						if (e.getMessage().contains("1")) {
							// either no perks or no notes set.
						} else
							System.out.println("Unable to format " + line);
					}
					break;
				case "//notes":
					currentNote = line.split(":")[1];
					break;
				default:
					break;
			}
		} while (br.ready());
		itemAndRolls.forEach((key, value) -> {
			System.out.printf("item: %s %nperks:%n", key);
			for (List<String> perkList : value) {
				System.out.printf("\t%s%n", perkList);
			}
		});		
		br.close();
		for (ArrayList source : sourceList) {
			System.out.println(source);
		}
	}
}