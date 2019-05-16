/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.event;

import ucar.util.ListenerManager;

/** ActionSourceListeners are used by objects that are both source and listener for
 *  a particular type of ActionValue events. They register themselves with the
 *  ActionCoordinator of that type of event. They send events
 *  by calling fireActionValueEvent().
 *  They recieve others' events through their actionPerformed() method.
 *
 * @see ActionCoordinator
 * @author John Caron
 */

public abstract class ActionSourceListener implements ActionValueListener {
  public static final String SELECTED = "selected";

  private ListenerManager lm;
  private String eventType;

  public ActionSourceListener(String eventType) {
    this.eventType = eventType;

    // manage ActionValueEvent Listeners
    lm = new ListenerManager(
        "ucar.ui.event.ActionValueListener",
        "ucar.ui.event.ActionValueEvent",
        "actionPerformed");
  }
  public String getEventTypeName() { return eventType; }

  public void fireActionValueEvent(String command, Object value) {
    lm.sendEvent(new ActionValueEvent(this, command, value));
  }
  public void addActionValueListener( ActionValueListener l) {
    lm.addListener(l);
  }
  public void removeActionValueListener( ActionValueListener l) {
    lm.removeListener(l);
  }
  public abstract void actionPerformed( ActionValueEvent event);
}