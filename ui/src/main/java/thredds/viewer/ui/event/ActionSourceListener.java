// $Id: ActionSourceListener.java 50 2006-07-12 16:30:06Z caron $
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
package thredds.viewer.ui.event;

/** ActionSourceListeners are used by objects that are both source and listener for
 *  a particular type of ActionValue events. They register themselves with the
 *  ActionCoordinator of that type of event. They send events
 *  by calling fireActionValueEvent().
 *  They recieve others' events through their actionPerformed() method.
 *
 * @see ActionCoordinator
 * @author John Caron
 * @version $Id: ActionSourceListener.java 50 2006-07-12 16:30:06Z caron $
 */

public abstract class ActionSourceListener implements ActionValueListener {
  static public final String SELECTED = "selected";

  private thredds.util.ListenerManager lm;
  private String eventType;

  public ActionSourceListener(String eventType) {
    this.eventType = eventType;

    // manage ActionValueEvent Listeners
    lm = new thredds.util.ListenerManager(
        "thredds.viewer.ui.event.ActionValueListener",
        "thredds.viewer.ui.event.ActionValueEvent",
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