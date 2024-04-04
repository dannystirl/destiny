package destiny;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;

import org.json.JSONArray;
import org.json.JSONObject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;

public class ConnectionTests {
	
	@Before
    public void setup() {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.out)));
    }

    /**
     * Verify the connection betwen the App and the Bungie API is active
     *
     * @throws UnirestException
     */
    @Test
    public void testConnection() throws UnirestException {
        Unirest.config().reset();
        Unirest.config().connectTimeout(10000).socketTimeout(10000);
        HttpResponse<String> response = Unirest.get(WishlistGenerator.bungieItemDefinitionUrl).header("X-API-KEY", DATA.APIKEY).routeParam("hashIdentifier", "3523296417").asString();
        assertEquals(1, new JSONObject(response.getBody()).get("ErrorCode"));
    }

    /**
     * Ensure the API is making a connection between normal and enhanced perks
     *
     * @throws UnirestException
     */
    @Test
    public void testResponse() throws UnirestException {
        JSONObject itemDefinition = Formatters.bungieItemDefinitionJSONObject("3523296417");
        JSONArray resultSet = Formatters.bungieItemHashSetJSONArray(itemDefinition.getString("name"));
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
        // assert that key and entry are not null`
        assertNotNull(key);
        assertNotNull(entry);
        System.out.printf("Test %s passed%n", new Object() {
        }.getClass().getEnclosingMethod().getName());
    }

    /**
     * Ensure the api is making a connection between normal and adept weapons
     * when there is one
     *
     * @throws UnirestException
     */
    @Test
    public void testAdeptConnection() throws UnirestException {
        JSONObject itemDefinition = Formatters.bungieItemDefinitionJSONObject("2886339027");
        assertEquals("Cataclysmic", itemDefinition.getString("name").split("\s\\(Adept\\)")[0]);

        JSONArray resultSet = Formatters.bungieItemHashSetJSONArray(itemDefinition.getString("name").split("\s\\(Adept\\)")[0]);
        Long normal = null, adept = null;
        // ensure that there are only two versions of the gun
        assertEquals(2, resultSet.length());
        for (Object object : resultSet) {
            JSONObject jsonObject = (JSONObject) object;
            if (normal == null) {
                normal = jsonObject.getLong("hash");
            } else {
                adept = jsonObject.getLong("hash");
            }
        }
        // assert that key and entry are not null
        assertNotNull(normal);
        assertNotNull(adept);
        System.out.printf("Test %s passed%n", new Object() {
        }.getClass().getEnclosingMethod().getName());
    }

    /**
     * Ensure the api is making a connection between normal and adept weapons
     * when there isn't one
     *
     * @throws UnirestException
     */
    @Test
    public void testNonAdeptConnection() throws UnirestException {
        JSONObject itemDefinition = Formatters.bungieItemDefinitionJSONObject("2886339027");
        assertTrue(itemDefinition.getString("name").contains("(Adept)"));
        assertEquals("Cataclysmic", itemDefinition.getString("name").split("\s\\(Adept\\)")[0]);

        JSONArray resultSet = Formatters.bungieItemHashSetJSONArray(itemDefinition.getString("name").split("\s\\(Adept\\)")[0]);
        // ensure that there are only two versions of the gun
        assertEquals(2, resultSet.length());
        for (Object object : resultSet) {
            JSONObject jsonObject = (JSONObject) object;
            itemDefinition = jsonObject.getJSONObject("displayProperties");
            if (!itemDefinition.getString("name").contains("(Adept)")) {
                assertEquals("Cataclysmic", itemDefinition.getString("name"));
                assertEquals(999767358, jsonObject.getLong("hash"));
            }
        }
        // assert that key and entry are not null
        System.out.printf("Test %s passed%n", new Object() {
        }.getClass().getEnclosingMethod().getName());
    }

    /**
     * Try getting a specific item's name
     *
     * @throws UnirestException
     */
    @Test
    public void testGetName() throws UnirestException {
        JSONObject itemDefinition = Formatters.bungieItemDefinitionJSONObject("4083045006");
        assertEquals("Persuader", itemDefinition.getString("name"));
        System.out.printf("Test %s passed%n", new Object() {
        }.getClass().getEnclosingMethod().getName());
    }
}
