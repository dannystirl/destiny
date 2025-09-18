package destiny;

import junit.framework.AssertionFailedError;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;

public class BungieDataParsers {

        App.RunType runType;

        // Map the IDs of Enhanced || 1.0 Perks -> Normal Perks
        public static Map<String, String> enhancedPerkList = new HashMap<>(); // todo: this is printing duplicate values. probably need a second list that tracks which values were added while running, and then only print those values
        // Map the IDs of Adept Items -> Normal Items
        public static Map<Long, Long> adeptMatchingList = new HashMap<>(); // todo: this is printing duplicate values. probably need a second list that tracks which values were added while running, and then only print those values
        // Map the IDs of Items -> Name
        public static Map<String, String> itemNamingList = new HashMap<>(); // todo: this is printing duplicate values. probably need a second list that tracks which values were added while running, and then only print those values
        // List of Items that have been checked for their normal version
        public static List<String> checkedItemList = new ArrayList<>();
        // API Items
        private static JSONObject manifest = null;

        public static final String bungieNetUrl = "https://www.bungie.net";
        @Deprecated // https://github.com/Bungie-net/api/issues/1922
        public static final String bungieItemSearchUrl = bungieNetUrl + "/Platform/Destiny2/Armory/Search/DestinyInventoryItemDefinition/{searchTerm}/";
        public static final String bungieManifestUrl = bungieNetUrl + "/Platform/Destiny2/Manifest";
        public static final String bungieItemDefinitionUrl = bungieManifestUrl + "/DestinyInventoryItemDefinition/{hashIdentifier}/";

        BungieDataParsers(App.RunType runType) {
                this.runType = runType;
        }

        /*
         * API METHODS
         */

        /**
         * Is the Bungie API down for maintenance?
         *
         * @param response
         * @return boolean
         */
        public static boolean isSystemDisabled(@NotNull JSONObject response) {
                return response.get("ErrorStatus").equals("SystemDisabled");
        }

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
                if (isSystemDisabled(itemDefinition)) {
                        return null;
                }
                itemDefinition = itemDefinition.getJSONObject("Response");
                itemDefinition = itemDefinition.getJSONObject("displayProperties");
                return itemDefinition;
        }

        /**
         * Get the Display Properties of an Item from the Database
         *
         * @param name - the unique name of an api item
         * @return JSONArray - an array of the display properties of an item from the database
         * @Deprecated <a href="https://github.com/Bungie-net/api/issues/1922">Github Issue Link</a>
         */
        @Deprecated
        public static JSONArray bungieItemHashSetJSONArray(String name) {
                HttpResponse<String> response = Unirest.get(bungieItemSearchUrl).header("X-API-KEY", DATA.APIKEY)
                        .routeParam("searchTerm", name.split("\s\\(Adept\\)")[0]).asString();

                JSONObject mJsonObject = new JSONObject(response.getBody());
                if (isSystemDisabled(mJsonObject)) {
                        return new JSONArray();
                }
                JSONObject userJObject = mJsonObject.getJSONObject("Response");
                JSONObject statusJObject = userJObject.getJSONObject("results");
                return statusJObject.getJSONArray("results");
        }

        /**
         * Download the Manifest from the JSON Url
         *
         * @param currentPrintStream
         * @return JSONObject
         * @throws IOException
         */
        public static JSONObject getManifest(PrintStream currentPrintStream) {
                if (manifest != null) {
                        return manifest;
                }
                HttpResponse<String> response = Unirest.get(bungieManifestUrl)
                        .header("X-API-KEY", DATA.APIKEY)
                        .asString();
                JSONObject mJsonObject = new JSONObject(response.getBody());
                if (isSystemDisabled(mJsonObject)) {
                        return null;
                }
                JSONObject manifestResponse = mJsonObject.getJSONObject("Response");
                String jsonText = "";
                try {
                        HttpURLConnection conn = (HttpURLConnection) new URL(
                                bungieNetUrl + manifestResponse.getJSONObject("jsonWorldComponentContentPaths")
                                .getJSONObject("en")
                                .getString("DestinyInventoryItemDefinition")
                        ).openConnection();

                        conn.setRequestProperty("Accept-Encoding", "gzip");

                        try (InputStream is = "gzip".equals(conn.getContentEncoding())
                                ? new GZIPInputStream(conn.getInputStream())
                                : conn.getInputStream();
                                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

                                jsonText = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                                jsonText = reader.lines().collect(Collectors.joining());
                        }
                } catch (IOException e) {
                        Formatters.errorPrint("Unable to get manifest",
                                new Exception("Unable to get Destiny Manifest. Check API Status."),
                                currentPrintStream);
                        return null;
                }
                manifest = new JSONObject(jsonText);
                return manifest;
        }

        /**
         * Get the Weapon Hashes that match the given Name
         *
         * @param name
         * @param currentPrintStream
         * @return JSONArray
         */
        public static JSONArray getBungieWeaponHashes(String name, PrintStream currentPrintStream) {
                JSONArray weapons = new JSONArray();
                JSONObject manifest = getManifest(currentPrintStream);
                if (manifest == null) {
                        return weapons;
                }
                Iterator<String> keys = manifest.keys();
                while (keys.hasNext()) {
                        String key = keys.next();
                        JSONObject item = manifest.optJSONObject(key);
                        if (item == null) {
                                continue;
                        }
                        if (item.keySet().containsAll(List.of("equippable", "traitIds", "defaultDamageTypeHash"))) {
                                if (item.getBoolean("equippable") && item.getJSONArray("traitIds").toList().stream().anyMatch(id -> id.toString().contains("item.weapon"))) {
                                        weapons.put(item);
                                }
                        }
                }
                weapons = new JSONArray(
                        IntStream.range(0, weapons.length())
                                .mapToObj(weapons::getJSONObject)
                                .sorted(Comparator.comparing(it -> it.getJSONObject("displayProperties").getString("name")))
                                .collect(Collectors.toList())
                );
                return new JSONArray(
                        weapons.toList().stream()
                                .filter(it -> ((Map) ((Map) it).get("displayProperties"))
                                        .get("name").toString().contains(name))
                                .collect(Collectors.toList())
                );
        }

        /**
         * Get the Trait Hashes that match the give Name
         *
         * @param name
         * @param currentPrintStream
         * @return JSONArray
         */
        public static JSONArray getBungieTraitHashes(String name, PrintStream currentPrintStream) {
                JSONArray traits = new JSONArray();
                JSONObject manifest = getManifest(currentPrintStream);
                if (manifest == null) {
                        return traits;
                }
                Iterator<String> keys = manifest.keys();
                while (keys.hasNext()) {
                        String key = keys.next();
                        JSONObject item = manifest.optJSONObject(key);
                        if (item == null) {
                                continue;
                        }
                        if (item.keySet().containsAll(List.of("equippable", "itemTypeDisplayName","displayProperties"))) {
                                if (item.getString("itemTypeDisplayName").contains("Trait") && item.getJSONObject("displayProperties").getString("name") != "") {
                                        traits.put(item);
                                }
                        }
                }
                traits = new JSONArray(
                        IntStream.range(0, traits.length())
                                .mapToObj(traits::getJSONObject)
                                .sorted(Comparator.comparing(it -> it.getJSONObject("displayProperties").getString("name")))
                                .collect(Collectors.toList())
                );
                return new JSONArray(
                        traits.toList().stream()
                                .filter(it -> ((Map) ((Map) it).get("displayProperties"))
                                        .get("name").toString().contains(name))
                                .collect(Collectors.toList())
                );
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
                if (itemNamingList.containsKey(hashIdentifier)) {
                        return itemNamingList.get(hashIdentifier);
                } else if (itemId == 69420L) {
                        // This ItemID is reserved for a roll on any gun
                        itemNamingList.put(hashIdentifier, "Any");
                        return "";
                } else if (hashIdentifier.length() < 3) {
                        Formatters.errorPrint("Unable to get name for item type " + itemId, new InvalidObjectException("Item is not a valid item. Check item categories and types."), currentPrintStream);
                        return "";
                } else {
                        try {
                                JSONObject itemDefinition = bungieItemDefinitionJSONObject(hashIdentifier);
                                if (itemDefinition == null) {
                                        return "";
                                }
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
                                        // TODO: Get weapons that contain the same name from array
                                        JSONArray resultSet = getBungieWeaponHashes(name, Formatters.defaultPrintStream);
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
         * TRANSLATE  <a href="https://www.light.gg/db/all/?page=1&f=4(3),10(Trait)">...</a>  TO  <a href="https://www.light.gg/db/all/?page=1&f=4(2),10(Trait)">...</a>
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
                if (tempPerkList.equals(List.of("-"))) {
                        // When we want to ignore every roll on an item, the perk list is just a single "-"
                        return tempPerkList;
                }
                for (int i = j; i < enhancedPerkList.size(); i++) {
                        if (!checkedItemList.contains(enhancedPerkList.get(i))) {
                                try {
                                        checkPerk(enhancedPerkList.get(i));
                                } catch (Exception e) {
                                        // Really could be any number of reasons for this to happen, but it's probably a timeout.
                                        Formatters.errorPrint(String.format("HTTP Error when checking perk %s", enhancedPerkList.get(i)), e);
                                }
                        }
                        // if enhancedPerkList contains itemPerkList.get(i), set tempPerkList to the itemMatchingList (convert dead / incorrect perks to the correct / normal version)
                        if (BungieDataParsers.enhancedPerkList.containsKey(enhancedPerkList.get(i))) {
                                tempPerkList.set(i, BungieDataParsers.enhancedPerkList.get(enhancedPerkList.get(i)));
                        }
                }
                return tempPerkList;
        }

        /**
         * Check a perk to see if it has an enhanced version
         *
         * @param hashIdentifier - the hash of the perk to be checked
         */
        public static void checkPerk(String hashIdentifier) {
                try {
                        hardCodedCases(hashIdentifier);
                        return;
                } catch (Exception e) {
                        // For some reason the api doesn't work for the values in here, so I'm just gonna hard code it and ignore the error
                        // this really should only occur once for each hashIdentifier
                }

                JSONObject itemDefinition = bungieItemDefinitionJSONObject(hashIdentifier);
                if (itemDefinition == null) {
                        return;
                }
                JSONArray resultSet = getBungieTraitHashes(itemDefinition.getString("name"), Formatters.defaultPrintStream);
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
                // add entry to enhancedPerkList at key
                if (enhanced != null) {
                        enhancedPerkList.put(enhanced.toString(), normal.toString());
                        checkedItemList.add(enhanced.toString());
                }
                if (normal != null) {
                        checkedItemList.add(normal.toString());
                        // TODO - this probably removes a lot of the edge cases, so maybe go through and remove them?
                        checkedItemList.add(hashIdentifier);
                        if (enhanced == null) {
                                enhancedPerkList.put(hashIdentifier, normal.toString());
                        }
                }
        }

        /*
         * HELPER METHODS
         */

        /**
         * Helper method because some stuff in the api isn't matching up
         * <p>
         * This seems to be mostly accurate except for frames that were turned into perks because they have more than two entries.
         * (ex. Disruption Break)
         * There's a separate issue with a few perks (ex. High Impact Reserves and Ambitious Assassin)
         * that are only returning one value, but I think that's more an issue with the api, not my code.
         * <p>
         * Some of these values could be removed now? (Stuff with only two entries) since
         * I changed some stuff in the checkPerk() method
         *
         * @param perk - the perk to be checked
         * @throws Exception an exception for error handling, shouldn't be a problem
         */
        public static void hardCodedCases(String perk) throws Exception {
                switch (perk) {
                        case "3528046508": {
                                // Auto-Loading Holster
                                enhancedPerkList.put("3528046508", "3300816228");
                                checkedItemList.add("3528046508");
                                checkedItemList.add("3300816228");
                                break;
                        }
                        case "2717805783": {
                                // Moving Target
                                enhancedPerkList.put("2717805783", "588594999");
                                checkedItemList.add("2717805783");
                                checkedItemList.add("588594999");
                                break;
                        }
                        case "2014892510": {
                                // Perpetual Motion (Has E in the name)
                                enhancedPerkList.put("2014892510", "1428297954");
                                checkedItemList.add("2014892510");
                                checkedItemList.add("1428297954");
                                break;
                        }
                        case "288411554": {
                                // Rampage (Duplicate in API)
                                enhancedPerkList.put("288411554", "3425386926");
                                checkedItemList.add("288411554");
                                checkedItemList.add("3425386926");
                                break;
                        }
                        case "1523649716": {
                                // Tap the Trigger
                                enhancedPerkList.put("1523649716", "1890422124");
                                checkedItemList.add("1523649716");
                                checkedItemList.add("1890422124");
                                break;
                        }
                        case "3797647183": {
                                // Ambitious Assassin
                                enhancedPerkList.put("3797647183", "2010801679");
                                checkedItemList.add("3797647183");
                                checkedItemList.add("2010801679");
                                break;
                        }
                        case "2002547233": {
                                // High-Impact Reserves
                                enhancedPerkList.put("2002547233", "2213355989");
                                checkedItemList.add("2002547233");
                                checkedItemList.add("2213355989");
                                break;
                        }
                        case "494941759": {
                                // Threat Detector
                                enhancedPerkList.put("494941759", "4071163871");
                                checkedItemList.add("494941759");
                                checkedItemList.add("4071163871");
                                break;
                        }
                        case "3143051906": {
                                // repeating case
                        }
                        case "25606670": {
                                // Dual Loader (I really only want the enhanced version and this seems much simpler than making a lot of changes to the code)
                                enhancedPerkList.put("25606670", "3143051906");
                                enhancedPerkList.put("3143051906", "3143051906");
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
                                enhancedPerkList.put("598607952", "2396489472");
                                enhancedPerkList.put("3076459908", "2396489472");
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
                                enhancedPerkList.put("3865257976", "2848615171");
                                enhancedPerkList.put("169755979", "2848615171");
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
                                enhancedPerkList.put("3513791699", "3337692349");
                                enhancedPerkList.put("162561147", "3337692349");
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
                                enhancedPerkList.put("2216471363", "1683379515");
                                enhancedPerkList.put("3871884143", "1683379515");
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
                                enhancedPerkList.put("2459015849", "2360754333");
                                enhancedPerkList.put("806159697", "2360754333");
                                checkedItemList.add("2459015849");
                                checkedItemList.add("806159697");
                                checkedItemList.add("2360754333");
                                break;
                        }
                        default: {
                                throw new Exception("Hash Identifier not found in Hard Coded Cases");
                        }
                }
        }
}
