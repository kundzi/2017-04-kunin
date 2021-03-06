package net.kundzi.messagesystem;

import net.kundzi.messagesystem.io.MessageV2Reader;
import net.kundzi.messagesystem.io.MessageV2Writer;
import net.kundzi.messagesystem.protocol.Address;
import net.kundzi.messagesystem.protocol.MessageV2;
import net.kundzi.nbserver.server.ClientConnection;
import net.kundzi.nbserver.server.SimpleReactorServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.kundzi.messagesystem.protocol.Reserved;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MessageSystemServer implements Closeable, SimpleReactorServer.IncomingMessageHandler<MessageV2> {

  private static final Logger LOG = LoggerFactory.getLogger(MessageSystemServer.class);

  private final ConcurrentHashMap<Address, ClientConnection<MessageV2>> addresseeMap = new ConcurrentHashMap<>();
  private final AtomicBoolean isActive = new AtomicBoolean(true);
  private final SimpleReactorServer<MessageV2> server;

  public static MessageSystemServer create(InetSocketAddress bindAddress) throws IOException {
    final SimpleReactorServer<MessageV2> server =
        SimpleReactorServer.start(bindAddress, new MessageV2Reader(), new MessageV2Writer());
    return new MessageSystemServer(server);
  }

  private MessageSystemServer(final SimpleReactorServer<MessageV2> server) {
    this.server = server;
    server.setIncomingMessageHandler(this);
  }

  public void addAddressee(ClientConnection<MessageV2> connection, MessageV2 message) {
    addresseeMap.put(message.from(), connection);
    final MessageV2 response = message.createResponseMessage(200, null);
    connection.send(response);
  }

  public void removeAddressee(final ClientConnection connection, MessageV2 message) {
    addresseeMap.remove(message.from());
    final MessageV2 response = message.createResponseMessage(200, null);
    connection.send(response);
  }

  public void stop() {
    isActive.set(false);
    LOG.info("Messaging System is shutting down");
    server.stop();
  }

  public void start() {
    isActive.set(true);
    LOG.info("Messaging System is active");
  }

  @Override
  public void close() throws IOException {
    stop();
  }

  @Override
  public void handle(final ClientConnection connection, final MessageV2 message) {
    if (!isActive.get()) {
      LOG.info("Ignoring, not active: " + message.toString());
      return;
    }
    final String topic = message.topic();
    switch (topic) {
      case Reserved.TYPE_REGISTER:
        // TODO validate address
        addAddressee(connection, message);
        LOG.info("{} registered as {}", connection.getRemoteAddress(), message.from());
        break;
      case Reserved.TYPE_UNREGISTER:
        // TODO validate address
        removeAddressee(connection, message);
        LOG.info("{} unregistered connection {}", connection.getRemoteAddress(), message.from());
        break;
      default:
        dispatch(message);
    }
  }

  private void dispatch(final MessageV2 message) {
    // TODO
    // What should we do when a client
    // is dead, but has not send "unregister" message?

    final Address to = message.to();
    final Address from = message.from();
    // both should be registered
    if (!addresseeMap.containsKey(to)) {
      // TODO send back not_found
      LOG.info("{} is not registered", to);
      return;
    }
    if (!addresseeMap.containsKey(from)) {
      // TODO send back must be registered
      LOG.info("{} must be registered first", from);
      return;
    }
    final ClientConnection<MessageV2> toConnection = addresseeMap.get(to);
    toConnection.send(message);
  }

  public void join() {
    server.join();
  }
}
