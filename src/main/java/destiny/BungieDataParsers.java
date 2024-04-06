package destiny;

import junit.framework.AssertionFailedError;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BungieDataParsers {

    App.RunType runType;

    public static Map<String, String> itemMatchingList = new HashMap<>(); // todo: this is printing duplicate values. probably need a second list that tracks which values were added while running, and then only print those values
    public static Map<Long, Long> adeptMatchingList = new HashMap<>(); // todo: this is printing duplicate values. probably need a second list that tracks which values were added while running, and then only print those values
    public static Map<String, String> itemNamingList = new HashMap<>(); // todo: this is printing duplicate values. probably need a second list that tracks which values were added while running, and then only print those values
    public static List<String> checkedItemList = new ArrayList<>();

    public static final String bungieItemSearchUrl = "https://www.bungie.net/Platform/Destiny2/Armory/Search/DestinyInventoryItemDefinition/{searchTerm}/";
    public static final String bungieItemDefinitionUrl = "https://www.bungie.net/Platform/Destiny2/Manifest/DestinyInventoryItemDefinition/{hashIdentifier}/";

    BungieDataParsers(App.RunType runType) {
        this.runType = runType;
    }

    /*
     * API METHODS
     */

    /**
     * @param hashIdentifier - the unique hash value for an api item
     * @return JSONObject - the display properties of an item from the database
     */
    public static JSONObject bungieItemDefinitionJSONObject(String hashIdentifier) {
        Unirest.config().reset();
        Unirest.config().connectTimeout(10000).socketTimeout(10000);
        HttpResponse<String> response = Unirest.get(bungieItemDefinitionUrl).header("X-API-KEY", DATA.APIKEY)
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
        HttpResponse<String> response = Unirest.get(bungieItemSearchUrl).header("X-API-KEY", DATA.APIKEY)
                .routeParam("searchTerm", name.split("\s\\(Adept\\)")[0]).asString();

        JSONObject mJsonObject = new JSONObject(response.getBody());
        JSONObject userJObject = mJsonObject.getJSONObject("Response");
        JSONObject statusJObject = userJObject.getJSONObject("results");
        return statusJObject.getJSONArray("results");
    }

    /**
     * Takes an item id and returns the associated name as a string
     *
     * @param itemId - The item id to get the name of
     * @return the name of the item as a string
     * @throws UnirestException
     * @throws AssertionFailedError
     */
    public String getName(Long itemId, PrintStream currentPrintStream) throws UnirestException, AssertionFailedError {
        String hashIdentifier = itemId.toString();
        if (itemMatchingList.containsKey(hashIdentifier)) {
            return itemMatchingList.get(hashIdentifier);
        } else {
            try {
                JSONObject itemDefinition = bungieItemDefinitionJSONObject(hashIdentifier);
                itemNamingList.put(hashIdentifier, itemDefinition.getString("name"));
                return itemDefinition.getString("name");
            } catch (Exception e) {
                if (runType == App.RunType.NORMAL) {
                    Formatters.errorPrint("Unable to get name for item " + itemId, e, currentPrintStream);
                    return "";
                } else {
                    throw new AssertionFailedError("Unable to get name for item " + itemId);
                }
            }
        }
    }

    /**
     * Convert an item to a non-adept version, since both versions have the same perks and notes
     * When printing the wishlist, items should also be converted back to adept version
     *
     * @param itemId - ItemId of the Adept Item to check, and then convert is possible
     * @return Long
     */
    public Long convertItemToNonAdept(Long itemId) {
        if (!adeptMatchingList.containsKey(itemId)) {
            Long oldItem = itemId;
            try {
                String name = getName(itemId, Formatters.defaultPrintStream);
                if (name.contains("(Adept)")) {
                    // After checking if the item is adept, find the normal version and convert
                    JSONArray resultSet = bungieItemHashSetJSONArray(name);
                    for (Object object : resultSet) {
                        JSONObject jsonObject = (JSONObject) object;
                        JSONObject itemDefinition = jsonObject.getJSONObject("displayProperties");
                        if (!itemDefinition.getString("name").contains("(Adept)")) {
                            itemId = jsonObject.getLong("hash");
                        }
                    }
                }
            } catch (JSONException e) {
                if (runType == App.RunType.NORMAL) {
                    Formatters.errorPrint(String.format("Error checking for adept version of %s. Probably occurs when checking item type instead of item", itemId), e);
                } else {
                    throw new AssertionFailedError(String.format("Error checking for adept version of %s. Probably occurs when checking item type instead of item", itemId));
                }
            } finally {
                // used to get the normal version of an item from the adept version
                adeptMatchingList.put(oldItem, itemId);
            }
        } else {
            itemId = adeptMatchingList.get(itemId);
        }
        return itemId;
    }

    /**
     * TRANSLATE  https://www.light.gg/db/all/?page=1&f=4(3),10(Trait)  TO  https://www.light.gg/db/all/?page=1&f=4(2),10(Trait)
     *
     * @param enhancedPerkList - List of enhanced perks to convert to normal perks
     * @return List<String> of normal perks
     */
    public static List<String> convertEnhancedPerksToNormal(List<String> enhancedPerkList) {
        List<String> tempPerkList = new ArrayList<>(enhancedPerkList);
        int j = 0;
        if (enhancedPerkList.size() == 4) {
            j = 2;
        }
        for (int i = j; i < enhancedPerkList.size(); i++) {
            if (!checkedItemList.contains(enhancedPerkList.get(i))) {
                try {
                    checkPerk(enhancedPerkList.get(i));
                } catch (Exception e) {
                    // Really could be any number of reasons for this to happen, but it's probably a timeout.
                    Formatters.errorPrint("HTTP Error", e);
                }
            }
            // if itemMatchingList contains itemPerkList.get(i), set tempPerkList to the itemMatchingList (convert dead / incorrect perks to the correct / normal version)
            if (itemMatchingList.containsKey(enhancedPerkList.get(i))) {
                tempPerkList.set(i, itemMatchingList.get(enhancedPerkList.get(i)));
            }
        }
        return tempPerkList;
    }

    /**
     * @param hashIdentifier - the hash of the perk to be checked
     */
    public static void checkPerk(String hashIdentifier) {
        try {
            hardCodedCases(hashIdentifier);
            return;
        } catch (Exception e) {
            // For some reason the api doesn't work for the values in here, so I'm just gonna hard code it and ignore the error
            // this really should only occur once for each hashIdentifier
            Formatters.errorPrint(hashIdentifier + " is not hard coded", e);
        }

        JSONObject itemDefinition = bungieItemDefinitionJSONObject(hashIdentifier);
        JSONArray resultSet = bungieItemHashSetJSONArray(itemDefinition.getString("name"));
        Long normal = null, enhanced = null;
        for (Object object : resultSet) {
            JSONObject jsonObject = (JSONObject) object;
            if (jsonObject.getJSONObject("displayProperties").length() == itemDefinition.length()
                    && jsonObject.getJSONObject("displayProperties").getString("name")
                    .equals(itemDefinition.getString("name"))) {
                if (normal == null) {
                    normal = jsonObject.getLong("hash");
                } else {
                    enhanced = jsonObject.getLong("hash");
                }
            }
        }
        // add entry to itemMatchingList at key
        if (enhanced != null) {
            itemMatchingList.put(enhanced.toString(), normal.toString());
            checkedItemList.add(enhanced.toString());
        }
        if (normal != null) {
            checkedItemList.add(normal.toString());
            // TODO - this probably removes a lot of the edge cases, so maybe go through and remove them?
            checkedItemList.add(hashIdentifier);
            if (enhanced == null) {
                itemMatchingList.put(hashIdentifier, normal.toString());
            }
        }
    }

    /*
     * HELPER METHODS
     */

    /**
     * Helper method because some stuff in the api isn't matching up this seems
     * to be mostly accurate except for frames that were turned into perks (ex.
     * Disruption Break) have more than two entries high impact reserves and
     * Ambitious Assassin also seems to have an issue (only returning one
     * value), but I think that's more an issue with the api, not my code. Some
     * of these values could be removed now? (Stuff with only two entries) since
     * I changed some stuff in the checkPerk() method
     *
     * @param perk - the perk to be checked
     * @throws Exception an exception for error handling, shouldn't be a problem
     */
    public static void hardCodedCases(String perk) throws Exception {
        switch (perk) {
            case "3528046508": {
                // Auto-Loading Holster
                itemMatchingList.put("3528046508", "3300816228");
                checkedItemList.add("3528046508");
                checkedItemList.add("3300816228");
                break;
            }
            case "2717805783": {
                // Moving Target
                itemMatchingList.put("2717805783", "588594999");
                checkedItemList.add("2717805783");
                checkedItemList.add("588594999");
                break;
            }
            case "2014892510": {
                // Perpetual Motion (Has E in the name)
                itemMatchingList.put("2014892510", "1428297954");
                checkedItemList.add("2014892510");
                checkedItemList.add("1428297954");
                break;
            }
            case "288411554": {
                // Rampage (Duplicate in API)
                itemMatchingList.put("288411554", "3425386926");
                checkedItemList.add("288411554");
                checkedItemList.add("3425386926");
                break;
            }
            case "1523649716": {
                // Tap the Trigger
                itemMatchingList.put("1523649716", "1890422124");
                checkedItemList.add("1523649716");
                checkedItemList.add("1890422124");
                break;
            }
            case "3797647183": {
                // Ambitious Assassin
                itemMatchingList.put("3797647183", "2010801679");
                checkedItemList.add("3797647183");
                checkedItemList.add("2010801679");
                break;
            }
            case "2002547233": {
                // High-Impact Reserves
                itemMatchingList.put("2002547233", "2213355989");
                checkedItemList.add("2002547233");
                checkedItemList.add("2213355989");
                break;
            }
            case "494941759": {
                // Threat Detector
                itemMatchingList.put("494941759", "4071163871");
                checkedItemList.add("494941759");
                checkedItemList.add("4071163871");
                break;
            }
            case "3143051906": {
                // repeating case
            }
            case "25606670": {
                // Dual Loader (I really only want the enhanced version and this seems much simpler than making a lot of changes to the code)
                itemMatchingList.put("25606670", "3143051906");
                itemMatchingList.put("3143051906", "3143051906");
                checkedItemList.add("25606670");
                checkedItemList.add("3143051906");
                break;
            }
            case "3076459908": {
                // repeating case
            }
            case "598607952": {
                // repeating case
            }
            case "2396489472": {
                // Chain Reaction (Comes up as sandbox perk)
                itemMatchingList.put("598607952", "2396489472");
                itemMatchingList.put("3076459908", "2396489472");
                checkedItemList.add("598607952");
                checkedItemList.add("3076459908");
                checkedItemList.add("2396489472");
                break;
            }
            case "2848615171": {
                // repeating case
            }
            case "169755979": {
                // repeating case
            }
            case "3865257976": {
                // Dragonfly
                itemMatchingList.put("3865257976", "2848615171");
                itemMatchingList.put("169755979", "2848615171");
                checkedItemList.add("2848615171");
                checkedItemList.add("169755979");
                checkedItemList.add("3865257976");
                break;
            }
            case "3513791699": {
                // repeating case
            }
            case "162561147": {
                // repeating case
            }
            case "3337692349": {
                // Dragonfly
                itemMatchingList.put("3513791699", "3337692349");
                itemMatchingList.put("162561147", "3337692349");
                checkedItemList.add("3513791699");
                checkedItemList.add("162561147");
                checkedItemList.add("3337692349");
                break;
            }
            case "2216471363": {
                // repeating case
            }
            case "1683379515": {
                // repeating case
            }
            case "3871884143": {
                // Disruption Break (Barrel)
                itemMatchingList.put("2216471363", "1683379515");
                itemMatchingList.put("3871884143", "1683379515");
                checkedItemList.add("2216471363");
                checkedItemList.add("3871884143");
                checkedItemList.add("1683379515");
                break;
            }
            case "2360754333": {
                // repeating case
            }
            case "2459015849": {
                // repeating case
            }
            case "806159697": {
                // Trench Barrel (Barrel)
                itemMatchingList.put("2459015849", "2360754333");
                itemMatchingList.put("806159697", "2360754333");
                checkedItemList.add("2459015849");
                checkedItemList.add("806159697");
                checkedItemList.add("2360754333");
                break;
            }
            default: {
                throw new Exception();
            }
        }
    }
}
