/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.util;

import ucar.httpservices.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.util.EscapeStrings;
import ucar.nc2.util.IO;
import ucar.nc2.util.ListenerManager;

import java.net.*;
import java.io.*;

/**
 * Starts up a server socket on the given port, and listens for messages sent to it.
 * Sends the contents of the message to anyone who is registered as a listener.
 */
public final class SocketMessage {
  private static final boolean debug = false, throwAway = false;
  private static final boolean testing = false;
  private static boolean raw = false;

  private ServerSocket server;
  private boolean isAlreadyRunning = false;
  private ListenerManager lm;

  /**
   * Try to start a listener on the given port. If that port is already used, send the given message to it.
   * @param port listen on this server port.
   * @param message send this message if port in use
   */
  public SocketMessage(int port, String message) {

    try {
      server = new ServerSocket(port, 1);
      if (debug) System.out.println("SocketMessage started on port " + server.getLocalPort());
      Thread listen = new ListenThread();
      listen.start();

      // manage Event Listener's
      lm = new ListenerManager(
        "ucar.nc2.ui.util.SocketMessage$EventListener",
        "ucar.nc2.ui.util.SocketMessage$Event",
        "setMessage");

    } catch (SocketException e) {
      if (message != null)
        sendMessage( port, message);
      isAlreadyRunning = true;

    } catch (IOException e) {
      System.out.println("SocketMessage IOException= " + e);
      e.printStackTrace();
    }
  }

  /**
   * Was the port already in use?
   * @return true if the port was already in use
   */
  public boolean isAlreadyRunning() { return isAlreadyRunning; }

  /**
   * Add a EventListener
   * @param l the listener
   */
  public void addEventListener( EventListener l) {
    lm.addListener( l);
  }

  /**
   * Remove an EventListener.
   * @param l the listener
   */
  public void removeEventListener( EventListener l) {
    lm.removeListener(l);
  }

  private void sendMessage(int port, String message) {
    try (Socket connection = new Socket("localhost", port)) {
      IO.writeContents(message, connection.getOutputStream());
      if (debug) System.out.println(" sent message " + message);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private class ListenThread extends Thread {
    String message;

    public void run()  {

      while (true) {
        Socket connection;
        if (debug) System.out.println("Listening for connections on port " + server.getLocalPort());
        try {
          connection = server.accept();

        } catch (IOException e) {
          System.out.println("SocketMessage accept= " + e);
          e.printStackTrace();
          return;
        }

        try {
          if (debug) System.out.println("SocketMessage Connection established with " + connection);
          if (throwAway) {
            IO.copy2null(connection.getInputStream(), -1);
            /* long count = IO.writeToFile(connection.getInputStream(), "C:/temp/save");
            if (debug) System.out.println("SocketMessage had length " + count/(1000*1000)+" Mb"); */
          } else if (raw) {
            InputStream in = connection.getInputStream();
            byte[] buffer = new byte[8000];
            int bytesRead = in.read(buffer);
            System.out.printf("%s == %s%n", bytesRead, new String(buffer,0,bytesRead, CDM.utf8Charset));
          } else {
            message = IO.readContents(connection.getInputStream());
            if (debug) System.out.println(" SocketMessage got message= "+message);
            lm.sendEvent(new Event( message));
          }

        } catch (IOException e) {
          System.out.println("SocketMessage IOException reading= " + e);
          e.printStackTrace();

        } finally {
          try {
            if (connection != null) connection.close();
            if (debug) System.out.println("connection done ");
          }
          catch (IOException e) { } // client closed first
        }

      } // loop
    } // run
  } // ListenThread

  public class Event extends java.util.EventObject {
    private String message;

    Event(String message) {
      super(SocketMessage.this);
      this.message = message;
    }

    public String getMessage() { return message; }
  }

  public interface EventListener {
     void setMessage(SocketMessage.Event event);
  }

  //////////////////////////////////////////////
  public static void main(String[] args) throws IOException {
    if (!testing) {
      new SocketMessage( 9999, "startNewServer");
      raw = true;

    } else {
      String url = "http://localhost:8080/thredds/test/it" // + EscapeStrings.escapeOGC("yabba/bad[0]/good")
               +"?"+EscapeStrings.escapeOGC("quuery[1]");
      System.out.printf("send '%s'%n", url);
      try (HTTPMethod method = HTTPFactory.Head(url)) {
          method.execute();
          int status = method.getStatusCode();
          System.out.printf("%d%n", status);
      } // close method, close method internal session
    }
  }
}
