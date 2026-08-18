package com.easychat.webSocket.netty;

import com.easychat.service.auth.WebSocketConnectionTicketService;
import com.easychat.webSocket.LocalChannelRegistry;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.ReferenceCountUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WebSocketTicketAuthHandlerTest {
    @Test
    public void validTicketBindsUserBeforeRequestIsForwarded() {
        WebSocketConnectionTicketService ticketService = mock(WebSocketConnectionTicketService.class);
        when(ticketService.consume("ticket-value")).thenReturn(1001);
        EmbeddedChannel channel = new EmbeddedChannel(new WebSocketTicketAuthHandler(ticketService, new LocalChannelRegistry()));

        assertTrue(channel.writeInbound(new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/ws?ticket=ticket-value")));
        assertEquals(Integer.valueOf(1001), channel.attr(LocalChannelRegistry.USER_ID_KEY).get());
        verify(ticketService).consume("ticket-value");
        ReferenceCountUtil.release(channel.readInbound());
        channel.finishAndReleaseAll();
    }

    @Test
    public void missingOrRepeatedTicketIsRejectedBeforeUpgrade() {
        WebSocketConnectionTicketService ticketService = mock(WebSocketConnectionTicketService.class);
        EmbeddedChannel channel = new EmbeddedChannel(new WebSocketTicketAuthHandler(ticketService, new LocalChannelRegistry()));

        assertFalse(channel.writeInbound(new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/ws?ticket=a&ticket=b")));
        FullHttpResponse response = channel.readOutbound();
        assertNotNull(response);
        assertEquals(HttpResponseStatus.UNAUTHORIZED, response.status());
        ReferenceCountUtil.release(response);
        channel.finishAndReleaseAll();
    }
}
