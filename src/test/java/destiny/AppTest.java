package destiny;

import static org.junit.Assert.assertTrue;
import org.junit.Test;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

/** Unit test for simple App. */
public class AppTest {
    /** Rigorous Test :-) */
    @Test
    public void shouldAnswerWithTrue() {
        assertTrue(true);
    }

    @Test
    public void testResponse() throws UnirestException {

        Unirest.setTimeouts(0, 0);
        HttpResponse<String> response = Unirest
                .get("https://www.bungie.net/Platform")
                .asString();

    }
}
