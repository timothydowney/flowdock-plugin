package com.flowdock.jenkins;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.flowdock.jenkins.exception.FlowdockException;

/**
 * Unit tests for {@link FlowdockAPI}
 *
 */
public class FlowdockAPITest {

    @Mock
    private HttpURLConnection connection;

    private String url = "http://localhost";

    private String token = "the token";

    private FlowdockAPI api;

    @Mock
    private TeamInboxMessage teamInboxMessage;

    @Mock
    private ChatMessage chatMessage;

    private ByteArrayOutputStream outputStream;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        outputStream = new ByteArrayOutputStream();

        api = new FlowdockAPI(url, token) {
            @Override
            protected HttpURLConnection getConnection(URL url) throws IOException {
                assertTrue(url.toString().startsWith("http://localhost"));
                return connection;
            }
        };

        when(teamInboxMessage.asPostData()).thenReturn("team-message");
        when(chatMessage.asPostData()).thenReturn("chat-message");

        when(connection.getOutputStream()).thenReturn(outputStream);
    }

    @Test
    public void testPushTeamInboxMessageSuccess() throws Exception {
        api = new FlowdockAPI(url, token) {
            @Override
            protected HttpURLConnection getConnection(URL url) throws IOException {
                assertEquals("http://localhost/messages/team_inbox/thetoken", url.toString());
                return connection;
            }
        };

        when(connection.getResponseCode()).thenReturn(200);

        api.pushTeamInboxMessage(teamInboxMessage);

        verify(connection).setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        verify(connection).setRequestProperty("Content-Length", String.valueOf("team-message".length()));
        verify(connection).setRequestMethod("POST");

        assertEquals(teamInboxMessage.asPostData(), new String(outputStream.toByteArray(), "UTF-8"));
    }

    @Test
    public void testPushChatMessageSuccess() throws Exception {
        api = new FlowdockAPI(url, token) {
            @Override
            protected HttpURLConnection getConnection(URL url) throws IOException {
                assertEquals("http://localhost/messages/chat/thetoken", url.toString());
                return connection;
            }
        };

        when(connection.getResponseCode()).thenReturn(200);

        api.pushChatMessage(chatMessage);

        verify(connection).setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        verify(connection).setRequestProperty("Content-Length", String.valueOf("chat-message".length()));
        verify(connection).setRequestMethod("POST");

        assertEquals(chatMessage.asPostData(), new String(outputStream.toByteArray(), "UTF-8"));
    }

    @Test(expected = FlowdockException.class)
    public void testPushTeamInboxMessageFailure() throws Exception {
        api = new FlowdockAPI(url, token) {
            @Override
            protected HttpURLConnection getConnection(URL url) throws IOException {
                assertEquals("http://localhost/messages/team_inbox/thetoken", url.toString());
                return connection;
            }
        };

        when(connection.getResponseCode()).thenReturn(500);

        api.pushTeamInboxMessage(teamInboxMessage);
    }

}
