package ru.otus.kunin.app;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.slf4j.LoggerFactory;
import ru.otus.kunin.backend.BackendComponent;
import ru.otus.kunin.front.WebsocketConnectorServlet;
import net.kundzi.messagesystem.protocol.MessageV2;
import net.kundzi.messagesystem.protocol.Address;
import net.kundzi.messagesystem.MessageSystemClient;
import net.kundzi.messagesystem.MessageSystemContext;
import net.kundzi.messagesystem.MessageSystemServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {

  public static void main(String[] args) throws Exception {

    final MessageSystemServer messageSystemServer = MessageSystemServer.create(new InetSocketAddress("localhost", 9100));
    new ProcessRunner().start("FRONT_1", "java -jar ../frontend/target/frontend.jar -f front_1 -b backend_1");
    new ProcessRunner().start("FRONT_1", "java -jar ../backend/target/backend.jar -f front_1 -b backend_1");

    new ProcessRunner().start("FRONT_1", "java -jar ../frontend/target/frontend.jar -f front_2 -b backend_2");
    new ProcessRunner().start("FRONT_1", "java -jar ../backend/target/backend.jar -f front_2 -b backend_2");
    messageSystemServer.join();
    LoggerFactory.getLogger(Main.class).info("Components have been started ...");

    // demoRun();
  }

  private static void demoRun() throws Exception {
    final InetSocketAddress serverAddress = new InetSocketAddress("localhost", 9100);
    final MessageSystemContext messageSystemContext = MessageSystemContext.builder()
        .serverHostname("localhost")
        .serverPort(9100)
        .backendAddress(Address.create("cache_1"))
        .frontendAddress(Address.create("front_1"))
        .build();

    final MessageSystemServer msgSystem = MessageSystemServer.create(serverAddress);
    msgSystem.start();

    final Server server = createFrontend(messageSystemContext);
    createBackend(messageSystemContext);

    server.join();
    msgSystem.join();

    System.out.println("Done!");
  }

  private static Server createFrontend(final MessageSystemContext messageSystemContext) throws Exception {
    final ResourceHandler resourceHandler = new ResourceHandler();
    resourceHandler.setDirectoriesListed(true);
    resourceHandler.setBaseResource(Resource.newClassPathResource("./static/"));

    final ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
    final ServletHolder servletHolder = new ServletHolder(WebsocketConnectorServlet.create(messageSystemContext));
    servletContextHandler.addServlet(servletHolder, "/cache/websocket");
    final Server server = new Server(8090);
    server.setHandler(new HandlerList(resourceHandler,
                                      servletContextHandler));
    server.start();
    return server;
  }

  private static void createBackend(final MessageSystemContext messageSystemContext) throws IOException {
    BackendComponent.create(messageSystemContext);
  }

  private static void experiment(final InetSocketAddress serverAddress) throws IOException {
    final MessageSystemClient clientFoo = MessageSystemClient.connect(serverAddress, Address.create("foo"));
    final MessageSystemClient clientBar = MessageSystemClient.connect(serverAddress, Address.create("bar"));

    clientFoo.setMessageListener((client, message) ->
                                     System.out.println("foo got " + message.topic() + " from " + message.from()));
    clientBar.setMessageListener((client, message) ->
                                     System.out.println("bar got " + message.topic() + " from " + message.from()));

    for (int i = 1; i < 1_000_000; i++) {
      System.out.println("step=" + i);
      clientFoo.send(MessageV2.createRequest(i + " love message to [bar]", Address.create("foo"), Address.create("bar"), null));
      clientBar.send(MessageV2.createRequest(i + " love message to [foo]", Address.create("bar"), Address.create("foo"), null));
    }
  }

}
