/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.util;

import org.slf4j.Logger;

import javax.annotation.concurrent.ThreadSafe;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Helper class for managing event listeners.
 * It is thread safe, but better not to be adding/deleting listeners while sending events.
 * LOOK Probably could replace with guava eventbus
 *
 * Example:
 * <pre>
 *
  private void createListenerManager() {
    lm = new ListenerManager(
            "ucar.nc2.util.DatasetCollectionManager$EventListener",
            "ucar.nc2.util.DatasetCollectionManager$Event",
            "setMessage");
  }

  public void addEventListener(EventListener l) {
    lm.addListener(l);
  }

  public void removeEventListener(EventListener l) {
    lm.removeListener(l);
  }

  public class Event extends java.util.EventObject {
    private String message;

    Event(String message) {
      super(DatasetCollectionManager.this);
      this.message = message;
    }

    public String getMessage() {
      return message;
    }
  }

  public static interface EventListener {
    public void setMessage(DatasetCollectionManager.Event event);
  }

 lm.sendEvent(event);
 </pre>

 *
 * @author John Caron
 */
@ThreadSafe
public class ListenerManager {
  private static final Logger logger = org.slf4j.LoggerFactory.getLogger(ListenerManager.class);

  private final List<Object> listeners = new CopyOnWriteArrayList<>(); // cf http://www.ibm.com/developerworks/java/library/j-jtp07265/index.html
  private final java.lang.reflect.Method method;
  private boolean hasListeners = false;
  private boolean enabled = true;

  /**
   * Constructor.
   *
   * @param listener_class the name of the EventListener class, eg "ucar.unidata.ui.UIChangeListener"
   * @param event_class    the name of the Event class, eg "ucar.unidata.ui.UIChangeEvent"
   * @param method_name    the name of the EventListener method, eg "processChange". <pre>
   *                          This method must have the signature     public void method_name( event_class e) </pre>
   */
  public ListenerManager(String listener_class, String event_class, String method_name) {

    try {
      Class lc = Class.forName(listener_class);
      Class ec = Class.forName(event_class);
      Class[] params = new Class[1];
      params[0] = ec;
      this.method = lc.getMethod(method_name, params);

    } catch (Exception ee) {
      logger.error("ListenerManager failed on " + listener_class + "." + method_name + "( " + event_class + " )", ee);
      throw new RuntimeException(ee);
    }

  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean getEnabled() {
    return enabled;
  }

  /**
   * Add a listener.
   *
   * @param l listener must be of type "listener_class"
   */
  public synchronized void addListener(Object l) {
    if (!listeners.contains(l)) {
      listeners.add(l);
      hasListeners = true;
    } else
      logger.warn("ListenerManager.addListener already has Listener " + l);
  }

  /**
   * Remove a listener.
   * @param l listener must be of type "listener_class"
   */
  public synchronized void removeListener(Object l) {
    if (listeners.contains(l)) {
      listeners.remove(l);
      hasListeners = (listeners.size() > 0);
    } else
      logger.warn("ListenerManager.removeListener couldnt find Listener " + l);
  }

  public synchronized boolean hasListeners() {
    return hasListeners;
  }

  /**
   * Send an event to all registered listeners. If an exception is thrown, remove
   * the Listener from the list
   *
   * @param event the event to be sent: public void method_name( event_class event)
   */
  public synchronized void sendEvent(java.util.EventObject event) {
    if (!hasListeners || !enabled)
      return;

    Object[] args = new Object[1];
    args[0] = event;

    // send event to all listeners
    ListIterator iter = listeners.listIterator();
    while (iter.hasNext()) {
      Object client = iter.next();
      try {
        method.invoke(client, args);
      } catch (IllegalAccessException e) {
        logger.error("ListenerManager IllegalAccessException", e);
        iter.remove();
      } catch (IllegalArgumentException e) {
        logger.error("ListenerManager IllegalArgumentException", e);
        iter.remove();
      } catch (InvocationTargetException e) {
        // logger.error("ListenerManager InvocationTargetException on " + method+ " threw exception " + e.getTargetException(), e);
        throw new RuntimeException(e.getCause()); // pass exception to the caller of sendEvent()
      }
    }
  }

  /**
   * Send an event to all registered listeners, except the named one.
   *
   * @param event the event to be sent: public void method_name( event_class event)
   */
  public synchronized void sendEventExcludeSource(java.util.EventObject event) {
    if (!hasListeners || !enabled)
      return;

    Object source = event.getSource();
    Object[] args = new Object[1];
    args[0] = event;

    // send event to all listeners except the source
    for (Object client : listeners) {
      if (client == source) {
        continue;
      }

      try {
        method.invoke(client, args);
      } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
        e.printStackTrace();
        if (e.getCause() != null) {
          e.getCause().printStackTrace();
        }

        // iter.remove();
        logger.error("ListenerManager calling " + method + " threw exception ", e);
      }
    }
  }

}
