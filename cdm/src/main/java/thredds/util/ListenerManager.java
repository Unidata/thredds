// $Id$
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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
package thredds.util;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.ListIterator;

/** Helper class for event listeners.
 * @author John Caron
 * @version $Id$
 */
public class ListenerManager {
    private ArrayList listeners = new ArrayList();
    private java.lang.reflect.Method method = null;
    private boolean hasListeners = false;
    private boolean enabled = true;

    /** Constructor.
     * @param listener_class    the name of the EventListener class, eg "ucar.unidata.ui.UIChangeListener"
     * @param event_class       the name of the Event class, eg "ucar.unidata.ui.UIChangeEvent"
     * @param method_name       the name of the EventListener method, eg "processChange". <pre>
     *    This method must have the signature     public void method_name( event_class e) </pre>
     */
    public ListenerManager (String listener_class, String event_class, String method_name) {

        try {
            Class lc = Class.forName(listener_class);
            Class ec = Class.forName(event_class);
            Class [] params = new Class[1];
            params[0] = ec;
            Method lm = lc.getMethod(method_name, params);
            this.method = lm;

        } catch (Exception ee) {
            System.err.println("ListenerManager failed on " +
                listener_class + "." + method_name + "( " +
                event_class + " )");
            ee.printStackTrace( System.err);
        }

    }

    public void setEnabled( boolean enabled) { this.enabled = enabled; }
    public boolean getEnabled( ) { return enabled; }

      /** Add a listener.
       * @param l listener: must be of type "listener_class"
       */
    public synchronized void addListener (Object l) {
        if (!listeners.contains(l)) {
            listeners.add(l);
            hasListeners = true;
        } else
          System.out.println("ListenerManager already has Listener "+ l);
     }
       /** Remove a listener. */
    public synchronized void removeListener (Object l) {
        if (listeners.contains(l)) {
            listeners.remove(l);
            hasListeners = (listeners.size() > 0);
        } else
          System.out.println("ListenerManager couldnt find Listener "+ l);
    }

    public boolean hasListeners() { return hasListeners; }

      /** Send an event to all registered listeners. If an exception is thrown, remove
       * the Listener from the list
       * @param event: the event to be sent: public void method_name( event_class event)
       */
    public void sendEvent( java.util.EventObject event) {
      if (!hasListeners || !enabled)
        return;

      Object [] args = new Object[1];
      args[0] = event;

      // send event to all listeners
      ListIterator iter = listeners.listIterator();
      while (iter.hasNext()) {
        Object client = iter.next();
        try {
          method.invoke( client, args);
        } catch (IllegalAccessException e) {
          iter.remove();
          System.err.println("ListenerManager IllegalAccessException = "+ e);
        } catch (IllegalArgumentException e) {
          iter.remove();
          System.err.println("ListenerManager IllegalArgumentException = "+ e);
        } catch (InvocationTargetException e) {
          iter.remove();
          System.err.println("ListenerManager InvocationTargetException on "+ method);
          System.err.println("   threw exception "+ e.getTargetException());
          e.printStackTrace();
        } /*catch (Exception e) {
          System.err.println("ListenerManager sendEvent failed "+ e);
          iter.remove();
        } */
      }
    }

      /** Send an event to all registered listeners, except the named one.
       * @param event: the event to be sent: public void method_name( event_class event)
       */
    public void sendEventExcludeSource( java.util.EventObject event) {
      if (!hasListeners || !enabled)
        return;

      Object source = event.getSource();
      Object [] args = new Object[1];
      args[0] = event;

      // send event to all listeners except the source
      ListIterator iter = listeners.listIterator();
      while (iter.hasNext()) {
        Object client = iter.next();
        if (client == source)
          continue;

        try {
          method.invoke( client, args);
        } catch (IllegalAccessException e) {
          iter.remove();
          System.err.println("ListenerManager IllegalAccessException = "+ e);
        } catch (IllegalArgumentException e) {
          iter.remove();
          System.err.println("ListenerManager IllegalArgumentException = "+ e);
        } catch (InvocationTargetException e) {
          iter.remove();
          System.err.println("ListenerManager InvocationTargetException on "+ method);
          System.err.println("   threw exception "+ e.getTargetException());
          e.printStackTrace();
        }
      }
    }

}

/* Change History:
   $Log: ListenerManager.java,v $
   Revision 1.3  2004/09/24 03:26:36  caron
   merge nj22

   Revision 1.2  2004/02/20 05:02:54  caron
   release 1.3

   Revision 1.1.1.1  2002/11/23 17:49:48  caron
   thredds reorg

   Revision 1.1.1.1  2002/02/15 00:01:48  caron
   import sources

*/
