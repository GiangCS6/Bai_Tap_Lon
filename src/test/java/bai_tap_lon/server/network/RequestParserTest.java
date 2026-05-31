package bai_tap_lon.server.network;

import bai_tap_lon.common.network.Request;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RequestParserTest {

    @Test
    void parseRequestExtractsActionAndPayload() {
        Request request = RequestParser.parseRequest("""
                {
                  "action": "LOGIN",
                  "payload": {
                    "username": "alice",
                    "password": "secret"
                  }
                }
                """);

        assertEquals("LOGIN", request.getAction());
        assertEquals("alice", request.getPayload().get("username").getAsString());
        assertEquals("secret", request.getPayload().get("password").getAsString());
    }

    @Test
    void parseRequestRejectsMissingAction() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> RequestParser.parseRequest("""
                        {"payload": {}}
                        """)
        );

        assertTrue(ex.getMessage().contains("action"));
    }

    @Test
    void parseRequestRejectsBlankAction() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> RequestParser.parseRequest("""
                        {"action": "   ", "payload": {}}
                        """)
        );

        assertTrue(ex.getMessage().contains("action"));
    }

    @Test
    void parseRequestRejectsMissingPayload() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> RequestParser.parseRequest("""
                        {"action": "LOGIN"}
                        """)
        );

        assertTrue(ex.getMessage().contains("payload"));
    }

    @Test
    void parseRequestRejectsMalformedJson() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> RequestParser.parseRequest("{")
        );

        assertTrue(ex.getMessage().contains("JSON"));
    }
}
