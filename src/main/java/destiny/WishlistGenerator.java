package destiny;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

public class WishlistGenerator implements AutoCloseable {

    public static App.RunType runType = App.RunType.NORMAL;
    public static App.SummarizerType summarizerType = App.SummarizerType.NONE;

    public static int sourceNum;
    public static BufferedReader br;
    public static PrintStream errorOutputFile;
    public static PrintStream scriptedWishlistFile;

    static LineDataParsers lineDataParsers;
    static BungieDataParsers bungieDataParsers;

    public static final String wishlistOutputFileName = "output//WishListScripted.txt";
    public static final String wishlistOutputTestFileName = "output//WishListScriptedTest.txt";
    public static final String enhancedMappingFileName = "src//main//data//destiny//enhancedMapping.csv";
    public static final String nameMappingFileName = "src//main//data//destiny//nameMapping.csv";
    public static final String wishListCSourceFileName = "input//CustomDestinyWishlist.txt";
    public static final String wishlistDSourceFileName = "input//CompleteDestinyWishList.txt";
    public static final String wishlistDSourceUrlName = "https://raw.githubusercontent.com/48klocs/dim-wish-list-sources/master/voltron.txt";

    /**
     * the main method reads through the original file and collects data on each
     * roll, concatenating notes and eliminating duplicates
     *
     * @param args any args needed for the main method, most likely to be an input
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        // Create the error file and output file
        try {
            errorOutputFile = new PrintStream(Formatters.errorOutputFileName);
            if (runType == App.RunType.TEST) {
                scriptedWishlistFile = new PrintStream(wishlistOutputTestFileName);
            } else {
                scriptedWishlistFile = new PrintStream(wishlistOutputFileName);
            }
        } catch (FileNotFoundException e) {
            System.out.println("Error creating error file: " + e);
            throw new FileNotFoundException();
        }
        // Try to read in existing enhanced -> normal perk mappings
        try (BufferedReader reader = new BufferedReader(
                new FileReader(enhancedMappingFileName))) {
            reader.readLine(); // skip the header line
            while (reader.ready()) {
                String item = reader.readLine();
                BungieDataParsers.enhancedPerkList.put(item.split(",")[0], item.split(",")[1]);
                BungieDataParsers.checkedItemList.add(item.split(",")[0]);
                BungieDataParsers.checkedItemList.add(item.split(",")[1]);
            }
        } catch (Exception e) {
            Formatters.errorPrint("Unable to read in existing item matching list", e);
            String eol = System.getProperty("line.separator");
            try (Writer writer = new FileWriter(enhancedMappingFileName, false)) {
                writer.append("From")
                        .append(',')
                        .append("To")
                        .append(eol);
                writer.flush();
            } catch (Exception er) {
                Formatters.errorPrint("Unable to save listGenerators.itemMatchingList to .\\data", er);
            }
        }
        Map<String, String> originalEnhancedMappingList = BungieDataParsers.enhancedPerkList.entrySet().stream().collect(Collectors.toMap(Entry::getKey, Entry::getValue));

        // Try to read in item -> name mappings
        try (BufferedReader reader = new BufferedReader(
                new FileReader(nameMappingFileName))) {
            reader.readLine(); // skip the header line
            while (reader.ready()) {
                String item = reader.readLine();
                BungieDataParsers.itemNamingList.put(item.split(",")[0], item.split(",")[1]);
                BungieDataParsers.checkedItemList.add(item.split(",")[0]);
                BungieDataParsers.checkedItemList.add(item.split(",")[1]);
            }
        } catch (Exception e) {
            Formatters.errorPrint("Unable to read in existing item naming list", e);
            String eol = System.getProperty("line.separator");
            try (Writer writer = new FileWriter(nameMappingFileName, false)) {
                writer.append("Item ID")
                        .append(',')
                        .append("Name")
                        .append(eol);
                writer.flush();
            } catch (Exception er) {
                Formatters.errorPrint("Unable to save itemNamingList to .\\data", er);
            }
        }
        Map<String, String> originalItemNamingList = BungieDataParsers.itemNamingList.entrySet().stream().collect(Collectors.toMap(Entry::getKey, Entry::getValue));

        Unirest.config().reset();
        Unirest.config().connectTimeout(5000).socketTimeout(5000).concurrency(10, 5);

        lineDataParsers = new LineDataParsers(runType);
        bungieDataParsers = new BungieDataParsers(runType);

        try {
            br = new BufferedReader(new FileReader(wishListCSourceFileName));
            loopRead(br);
        } catch (FileNotFoundException e) {
            Formatters.errorPrint("Error reading custom wishlist file", e);
        }

        try {
            try {
                HttpResponse<String> response = Unirest.get(wishlistDSourceUrlName).asString();
                PrintWriter out = new PrintWriter(wishlistDSourceFileName);
                out.println(response.getBody());
                out.close();
            } catch (Exception e) {
                Formatters.errorPrint("Unable to get updated contents from " + Formatters.errorOutputFileName, e);
            }
            br = new BufferedReader(new FileReader(wishlistDSourceFileName));
            loopRead(br);
        } catch (Exception e) {
            Formatters.errorPrint("Error reading default wishlist from url", e);
        }

        // SORTING ITEMS
        lineDataParsers.wantedItemList = new ItemSorter(runType).sortItems(lineDataParsers.wantedItemList);
        new Formatters(runType, summarizerType).withStreams(scriptedWishlistFile, errorOutputFile).withData(lineDataParsers).printWishlist();

        // Print the enhancedPerkList to a file, so I don't need to call HTTP.GET every time I run the script
        String eol = System.getProperty("line.separator");
        try (Writer writer = new FileWriter(enhancedMappingFileName, true)) {
            for (Map.Entry<String, String> entry : BungieDataParsers.enhancedPerkList.entrySet()) {
                if (!(originalEnhancedMappingList.containsKey(entry.getKey()) && originalEnhancedMappingList.get(entry.getKey()).equals(entry.getValue()))) {
                    writer.append(entry.getKey())
                            .append(',')
                            .append(entry.getValue())
                            .append(eol);
                }
            }
            writer.flush();
        } catch (Exception e) {
            Formatters.errorPrint("Unable to save itemMatchingList to .\\data", e);
        }
        // Print the itemNamingList to a file so I don't need to call HTTP.GET every time I run the script
        // TODO - First in Last out is adding an extra column to the name. Github copilot gave a reason (csv reader issue) but im not sure it's actually correct
        try (Writer writer = new FileWriter(nameMappingFileName, true)) {
            for (Map.Entry<String, String> entry : BungieDataParsers.itemNamingList.entrySet()) {
                if (!(originalItemNamingList.containsKey(entry.getKey()) && originalItemNamingList.get(entry.getKey()).equals(entry.getValue()))) {
                    writer.append(entry.getKey())
                            .append(',')
                            .append(entry.getValue())
                            .append(eol);
                }
            }
            writer.flush();
        } catch (Exception e) {
            Formatters.errorPrint("Unable to save itemNamingList to .\\data", e);
        }
        errorOutputFile.close();
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
            if (line.length() > 0 && line.charAt(0) == '@') {
                line = line.replace("@", "");
            }
            switch (line.split(":")[0]) {
                case "title":
                    td = new ArrayList<>();
                    td.add(sourceNum++);
                    td.add(line.split(":")[1]);
                    System.out.printf("Reading items from %s, list #%d.%n", line.split(":")[1], sourceNum);
                    break;
                case "description":
                    td.add(line.split(":")[1]);
                    lineDataParsers.sourceList.add(td);
                    // bug: is taking "//" as a new line character instead of a string
                    break;
                case "dimwishlist":
                    int startKey = 17; // where the item id lies
                    boolean ignoreItem = false;
                    if (line.charAt(startKey) == '-') {
                        ignoreItem = true;
                        startKey = 18;
                    }
                    // GATHERING LINE INFORMATION (ITEM, PERKS, NOTES)
                    long itemId = Long.parseLong(line.substring(startKey).split("&")[0].split("#")[0]);
                    if (runType == App.RunType.TEST) {
                        // Update to check specific item if runType == test
                        if (!List.of(1681583613L, 1801007332L, 3947966653L).contains(itemId)) {
                            break;
                        }
                    }
                    Item returnItem = lineDataParsers.lineParser(bungieDataParsers.convertItemToNonAdept(itemId), line, currentNote, ignoreItem);
                    // ADD ITEM TO APPROPRIATE WANTED LIST
                    lineDataParsers.addItemToWantedList(returnItem);
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

    @Override
    public void close() throws Exception {
        br.close();
    }
}