/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.ui.util;

import ucar.nc2.util.IO;
import ucar.unidata.util.Format;

import java.net.*;
import java.io.*;

/**
 * Starts up a server socket on the given port, and listens for messages sent to it.
 * Sends the contents of the message to anyone who is registered as a listener.
 */
public class SocketMessage {
  static private final boolean debug = true, throwAway = true;

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

    } catch (java.net.BindException e) {
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

  // public void exit() { listen.exit(); }

  private void sendMessage(int port, String message) {
    Socket connection = null;
    try {
      connection = new Socket("localhost", port);
      IO.writeContents(message, connection.getOutputStream());
      if (debug) System.out.println(" sent message " + message);

    }  catch (IOException e) {
      System.err.println(e);
      e.printStackTrace();

    } finally {
      try { if (connection != null) connection.close(); }
      catch (IOException e) {}
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
            IO.copy2null(connection.getInputStream());
            /* long count = IO.writeToFile(connection.getInputStream(), "C:/temp/save");
            if (debug) System.out.println("SocketMessage had length " + count/(1000*1000)+" Mb"); */
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

  public static interface EventListener {
     public void setMessage(SocketMessage.Event event);
  }

  //////////////////////////////////////////////
  public static void main(String[] args) throws IOException {
    new SocketMessage( 8080, "no");
    new SocketMessage( 8080, "testing");
  }

}
