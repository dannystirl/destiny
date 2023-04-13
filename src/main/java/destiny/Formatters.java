package destiny;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

public class Formatters {

    /**
     * print any errors to bin\errors folder
     *
     * @param err the (possible) reason for the error
     * @param e the error being thrown
     */
    public static void errorPrint(String err, Exception e) {
        errorPrint(err, e, new PrintStream(new FileOutputStream(FileDescriptor.out)));
    }

    /**
     * print any errors to bin\errors folder
     *
     * @param err the (possible) reason for the error
     * @param e the error being thrown
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

    /**
     *
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
     * @param note: the note to be formatted
     * @param currentNote: the previous note of a simliar item
     * @return String
     */
    public static String initialNoteFormatter(String note, String currentNote) {
        if (note == null) {
            note = currentNote;
        }
        Matcher matcher = Pattern.compile("Inspired by.*?(\\.[^A-Za-z0-9])", Pattern.CASE_INSENSITIVE).matcher(note);
        note = matcher.replaceAll("");
        List<String> creators = List.of("(\\[YeezyGT)[^\\]]*\\]\\s*", "pandapaxxy\\s*", "Mercules904\\s*", "Chevy.*[(\\.)(\\-)]\\s*", "Shapeable.");
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
     * @param note the note to be formatted
     * @return String
     */
    public static String noteFormatter(String note) {
        if (note.length() < 3) {
            return "";
        }
        if (note.length() > 0 && note.charAt(0) == (' ')) {
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

    /**
     * @param hashIdentifier - the unique hash value for an api item
     * @return JSONObject - the display properties of an item from the database
     */
    public static JSONObject bungieItemDefinitionJSONObject(String hashIdentifier) {
        Unirest.config().reset();
        Unirest.config().connectTimeout(10000).socketTimeout(10000);
        HttpResponse<String> response = Unirest.get(WishlistGenerator.bungieItemDefinitionUrl).header("X-API-KEY", "735ad4372078466a8b68a09ff9c02edb")
                .routeParam("hashIdentifier", hashIdentifier).asString();

        JSONObject itemDefinition = new JSONObject(response.getBody());
        itemDefinition = itemDefinition.getJSONObject("Response");
        itemDefinition = itemDefinition.getJSONObject("displayProperties");
        return itemDefinition;
    }

    /**
     * @param name - the unique name of an api item
     * @return JSONArray - an array of the display properties of an item from the database
     */
    public static JSONArray bungieItemHashSetJSONArray(String name) {
        HttpResponse<String> response = Unirest.get(WishlistGenerator.bungieItemSearchUrl).header("X-API-KEY", "735ad4372078466a8b68a09ff9c02edb")
                .routeParam("searchTerm", name.split("\s\\(Adept\\)")[0]).asString();

        JSONObject mJsonObject = new JSONObject(response.getBody());
        JSONObject userJObject = mJsonObject.getJSONObject("Response");
        JSONObject statusJObject = userJObject.getJSONObject("results");
        return statusJObject.getJSONArray("results");
    }
}
