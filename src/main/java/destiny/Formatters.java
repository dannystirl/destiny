package destiny;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Formatters {

    public static final PrintStream defaultPrintStream = new PrintStream(new FileOutputStream(FileDescriptor.out));

    PrintStream outputStream;
    PrintStream errorStream;

    App.RunType runType;

    LineDataParsers lineDataParsers;
    BungieDataParsers bungieDataParsers;

    Formatters(App.RunType runType) {
        this.runType = runType;
        this.outputStream = defaultPrintStream;
        this.errorStream = defaultPrintStream;
        lineDataParsers = new LineDataParsers(runType);
        bungieDataParsers = new BungieDataParsers(runType);
    }

    Formatters withStreams(PrintStream outputStream, PrintStream errorStream) {
        this.outputStream = outputStream;
        this.errorStream = errorStream;
        return this;
    }

    Formatters withData(LineDataParsers lineDataParsers) {
        this.lineDataParsers = lineDataParsers;
        return this;
    }

    /*
     * ERRORS
     */

    /**
     * print any errors to bin\errors folder
     *
     * @param err the (possible) reason for the error
     * @param e   the error being thrown
     */
    public static void errorPrint(String err, Exception e) {
        errorPrint(err, e, defaultPrintStream);
    }

    /**
     * print any errors to bin\errors folder
     *
     * @param err       the (possible) reason for the error
     * @param e         the error being thrown
     * @param oldStream the stream to return to
     */
    public static void errorPrint(String err, Exception e, PrintStream oldStream) {
        System.setOut(WishlistGenerator.errorOutputFile);
        System.setErr(WishlistGenerator.errorOutputFile);

        System.out.println(err + ": " + e.getMessage());
        e.printStackTrace();
        System.out.println("\n");

        // reset errorOutputFile to console
        System.setOut(oldStream);
        System.setErr(oldStream);
    }

    /*
     * ITEM FORMATTING
     */

    /**
     * @param tag the tag to be formatted
     * @return a formatted tag
     */
    public static String tagFormatter(String tag) {
        tag = tag.replace(")", "");
        tag = tag.replace("+", "");
        tag = tag.replace("|", "");
        tag = tag.replace(" ", "");
        return tag;
    }

    /**
     * Clean the notes
     *
     * @param note:        the note to be formatted
     * @param currentNote: the previous note of a similar item
     * @return String
     */
    public static String initialNoteFormatter(String note, String currentNote) {
        if (note == null) {
            note = currentNote;
        }
        Matcher matcher = Pattern.compile("Inspired by.*?(\\.[^A-Za-z0-9])", Pattern.CASE_INSENSITIVE).matcher(note);
        note = matcher.replaceAll("");
        List<String> creators = List.of("(\\[YeezyGT)[^\\]]*\\]\\s*", "pandapaxxy\\s*", "Mercules904\\s*", "Chevy.*[(\\.)(\\-)]\\s*", "Shapeable.", "Clegmir\\s*", "SirStallion_\\s*", "\\(\\?+  \\?+\\)\\: ");
        for (String creator : creators) {
            matcher = Pattern.compile(creator, Pattern.CASE_INSENSITIVE).matcher(note);
            note = matcher.replaceAll("");
        }
        if (note.contains("auto generated")) {
            try {
                note = "\\|tags:" + note.split("\\|*tags:")[1];
            } catch (Exception noteError) {
                // not an error. just item has no tags
            }
        }
        return note;
    }

    /**
     * Format the notes to remove unwanted information
     *
     * @param note the note to be formatted
     * @return String
     */
    public static String noteFormatter(String note) {
        if (note.length() < 3) {
            return "";
        }
        if (note.charAt(0) == ' ') {
            note = note.substring(1);
        }
        note = note.replace("“", "");
        note = note.replace("\"", "");
        note = note.replaceAll("\\s+", " ");
        note = note.replaceAll("[^\\p{ASCII}]", ""); // replace any characters printed as �
        if (note.length() < 3) {
            return "";
        }
        note = note.trim();
        if (note.charAt(note.length() - 1) == '.') {
            note = note.substring(0, note.length() - 1);
        }
        if (note.length() < 3) {
            return "";
        }
        return note;
    }

    /*
     * PRINTING
     */

    /**
     * PRINTING WISHLIST
     * <p>
     * Some rules for exporting to DIM:
     * 1. DIM auto-sorts items by notes, generally grouping by tags
     * 2. Rolls with normal perks will be auto-added as enhanced perks too, but not vice versa
     * 3. If there is more than one roll for an item, DIM will show all versions of that roll, breaking the auto-sort by notes
     */
    public void printWishlist() throws FileNotFoundException {
        System.setOut(outputStream);
        System.setErr(errorStream);

        // print overall title and description
        if (lineDataParsers.sourceList.isEmpty()) {
            throw new IndexOutOfBoundsException("No source list found");
        }
        System.out.printf("title:%s%n", lineDataParsers.sourceList.get(0).get(1));
        System.out.printf("description:%s%n%n", lineDataParsers.sourceList.get(0).get(2));
        // print wishlist rolls
        for (Map.Entry<Long, Item> item : lineDataParsers.wantedItemList.entrySet()) {
            Long normalItemId = item.getKey();
            List<Long> keysList = new ArrayList<>(List.of(normalItemId));

            // Convert back any items that have adept versions and print both
            for (Map.Entry<Long, Long> entry : BungieDataParsers.adeptMatchingList.entrySet()) {
                if (Objects.equals(normalItemId, entry.getValue())) {
                    if (!keysList.contains(entry.getKey())) {
                        keysList.add(entry.getKey());
                    }
                }
            }
            for (Long k : keysList) {
                printWishlistItem(item, k);
            }
        }
        // reset output to console
        outputStream.close();
        System.setOut(Formatters.defaultPrintStream);
        System.setErr(errorStream);
    }

    /**
     * A helper method for printing, allowing to loop for adept and normal
     * versions of items
     *
     * @param item the original item to compare to
     * @param key  the item id to check for similarity from
     */
    public void printWishlistItem(Map.Entry<Long, Item> item, Long key) throws FileNotFoundException {
        List<Roll> itemRollList = item.getValue().getRollList();
        List<String> currentNoteFull = new ArrayList<>();
        List<String> currentTagsFull = new ArrayList<>();
        List<String> currentMWsFull = new ArrayList<>();

        System.setOut(outputStream);
        System.setErr(errorStream);

        // ITEM NAME
        String name = bungieDataParsers.getName(key, outputStream);
        // ITEM VALUES
        Summarizer summarizer = new Summarizer(outputStream);
        System.out.printf("%n//item %s: %s%n", key, name);
        for (Roll itemRoll : itemRollList) {
            // TAGS
            // game-mode is in beginning, input type is at end
            itemRoll.getTagList().sort(Collections.reverseOrder());

            // ITEM DOCUMENTATION IS DIFFERENT
            if (!currentNoteFull.equals(itemRoll.getNoteList())
                    || !currentTagsFull.equals(itemRoll.getTagList())
                    || !currentMWsFull.equals(itemRoll.getMWList())) {
                currentNoteFull = itemRoll.getNoteList();
                currentTagsFull = itemRoll.getTagList();
                currentMWsFull = itemRoll.getMWList();
                // NOTES
                System.out.print("//notes:");
                for (int i = 0; i < currentNoteFull.size(); i++) {
                    if (!currentNoteFull.get(i).equals("")) {
                        currentNoteFull.set(i, Formatters.noteFormatter(currentNoteFull.get(i)));
                    }
                }
                if (!currentNoteFull.isEmpty() && !currentNoteFull.equals(List.of(""))) {
                    String summarizedNote = summarizer.sentenceAnalyzerUsingFrequency(String.join(". ", currentNoteFull) + ". ", List.of("first-choice", "backup", "best in slot"));
                    summarizedNote = summarizedNote.replace("lightggg", "light.gg");
                    summarizedNote = summarizedNote.replace("elipsez", "...");
                    summarizedNote = summarizedNote.replace(" v30 ", " 3.0 ");
                    System.out.print((summarizedNote + " ").replaceAll(" {2}", " "));
                }
                // MWS
                if (!itemRoll.getMWList().isEmpty()) {
                    System.out.print("Recommended MW: ");
                    for (int i = 0; i < itemRoll.getMWList().size() - 1; i++) {
                        System.out.print(itemRoll.getMWList().get(i) + ", ");
                    }
                    System.out.print(itemRoll.getMWList().get(itemRoll.getMWList().size() - 1) + ". ");
                }
                try {
                    // TAGS
                    if (!currentTagsFull.get(0).equals("")) {
                        // hashset is a fast way to remove duplicates, however they may have gotten there
                        LinkedHashSet<String> linkedHashSet = new LinkedHashSet<>(currentTagsFull);
                        System.out.print("|tags:");
                        for (int i = 0; i < linkedHashSet.size(); i++) {
                            if (i == linkedHashSet.size() - 1) {
                                System.out.print(linkedHashSet.toArray()[i]);
                            } else {
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
            if (key == 69420L) {
                key = -69420L;
            }
            System.out.printf("dimwishlist:item=%s", key);
            if (!itemRoll.getPerkList().isEmpty()) {
                // ITEM
                System.out.print("&perks=");
                // check if there is an item in itemRoll.getPerkList()
                for (int i = 0; i < itemRoll.getPerkList().size() - 1; i++) {
                    System.out.printf("%s,", itemRoll.getPerkList().get(i));
                }
                System.out.printf("%s%n", itemRoll.getPerkList().get(itemRoll.getPerkList().size() - 1));
            }
        }

        // reset errorOutputFile to console
        System.setOut(Formatters.defaultPrintStream);
        System.setErr(Formatters.defaultPrintStream);
    }
}
