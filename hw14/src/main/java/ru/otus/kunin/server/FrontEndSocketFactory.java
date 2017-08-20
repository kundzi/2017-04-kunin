package ru.otus.kunin.server;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class FrontEndSocketFactory implements WebSocketCreator {

  private static final Logger LOG = LoggerFactory.getLogger(FrontEndSocketFactory.class);

  @WebSocket
  public static class FrontEndSocket {

    @OnWebSocketConnect
    public void onConnect(Session session) throws IOException {
      LOG.info("Connecting {}", session);
      session.getRemote().sendString("Handshake OK");
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
      LOG.info("Closing {} {} {}", statusCode, reason, session);
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String text) {
      LOG.info("Message {} {}", session, text);
    }

  }

  @Override
  public Object createWebSocket(final ServletUpgradeRequest req, final ServletUpgradeResponse resp) {
    LOG.info("Creating socket {}", req);
    return new FrontEndSocket();
  }

}
