package destiny;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;

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
     *
     * @param note the note to be formatted
     * @return a formatted note
     */
    public static String noteFormatter(String note) {
        if (note.length() < 3) {
            return "";
        }
        List<String> creators = List.of("(\\[YeezyGT).*\\]", "pandapaxxy", "Mercules904", "Chevy.*[(\\.)(\\-)]");
        for(String creator : creators) {
            if (note.contains(creator))
                note = note.split(creator)[1];
        }
        if (note.length() > 0 && note.charAt(0) == (' ')) {
            note = note.substring(1);
        }

        note = note.replaceAll("[^\\p{ASCII}]", ""); // replace any characters printed as �
        note = note.replace("�", "'");
        note = note.replace("“", "\"");
        note = note.replace("\"", "");
        note = note.replaceAll("\\s+", " ");
        if (note.length() < 3) {
            return "";
        }
        if (note.charAt(note.length() - 1) == ' ') {
            note = note.substring(0, note.length() - 1);
        }
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
