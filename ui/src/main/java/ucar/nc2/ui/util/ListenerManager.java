/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.ui.util;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.ListIterator;

/** Helper class for event listeners.
 * @author John Caron
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
       * @param l listener must be of type "listener_class"
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
       * @param event the event to be sent: public void method_name( event_class event)
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
       * @param event the event to be sent: public void method_name( event_class event)
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
