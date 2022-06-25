package destiny;

import org.json.JSONArray;
import org.json.JSONObject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;

/** Unit test for simple App. */
public class AppTest {
    /*
     * Ensure the connection to the destiny api is working and getting a response
     */
    @Test
    public void testResponse() throws UnirestException {
        Unirest.config()
                .socketTimeout(0)
                .connectTimeout(0);
        HttpResponse<String> response = Unirest
                .get("https://www.bungie.net/Platform/Destiny2/Manifest/DestinyInventoryItemDefinition/3523296417/")
                .header("X-API-KEY", "735ad4372078466a8b68a09ff9c02edb")
                .asString();
        JSONObject itemDefinition = new JSONObject(response.getBody());
        itemDefinition = itemDefinition.getJSONObject("Response");
        itemDefinition = itemDefinition.getJSONObject("displayProperties");

        response = Unirest.get(
                "https://www.bungie.net/Platform/Destiny2/Armory/Search/DestinyInventoryItemDefinition/{searchTerm}/")
                .header("X-API-KEY", "735ad4372078466a8b68a09ff9c02edb")
                .routeParam("searchTerm", itemDefinition.getString("name"))
                .asString();

        JSONObject mJsonObject = new JSONObject(response.getBody());
        JSONObject userJObject = mJsonObject.getJSONObject("Response");
        JSONObject statusJObject = userJObject.getJSONObject("results");
        JSONArray resultSet = statusJObject.getJSONArray("results");
        Long key = null, entry = null;
        // ensure that there is only a basic and enhanced trait definiton
        assertEquals(2, resultSet.length());
        for (Object object : resultSet) {
            JSONObject jsonObject = (JSONObject) object;
            if (key == null) {
                key = jsonObject.getLong("hash");
            } else {
                entry = jsonObject.getLong("hash");
            }
        }
        // assert that key and entry are not null
        assertNotNull(key);
        assertNotNull(entry);
    }
}