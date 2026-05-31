package bai_tap_lon.server.network;

import bai_tap_lon.common.network.Response;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResponseFactoryTest {

    @Test
    void okWithDataCreatesSuccessfulResponse() {
        JsonObject data = new JsonObject();
        data.addProperty("auctionId", "A1");

        Response response = ResponseFactory.ok("GET_AUCTION", data);

        assertEquals("GET_AUCTION", response.getAction());
        assertTrue(response.isSuccess());
        assertSame(data, response.getData());
        assertNull(response.getError());
        assertNull(response.getErrorMessage());
    }

    @Test
    void okWithoutDataCreatesSuccessfulResponse() {
        Response response = ResponseFactory.ok("PING");

        assertEquals("PING", response.getAction());
        assertTrue(response.isSuccess());
        assertNull(response.getData());
        assertNull(response.getError());
        assertNull(response.getErrorMessage());
    }

    @Test
    void errorCreatesFailureResponse() {
        Response response = ResponseFactory.error(
                "PLACE_BID",
                "BID_TOO_LOW",
                "Bid must be higher than current price"
        );

        assertEquals("PLACE_BID", response.getAction());
        assertFalse(response.isSuccess());
        assertNull(response.getData());
        assertEquals("BID_TOO_LOW", response.getError());
        assertEquals("Bid must be higher than current price", response.getErrorMessage());
    }
}
