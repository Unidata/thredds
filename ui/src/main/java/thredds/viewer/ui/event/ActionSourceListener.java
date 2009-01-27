// $Id: ActionSourceListener.java 50 2006-07-12 16:30:06Z caron $
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
package thredds.viewer.ui.event;

import ucar.nc2.ui.util.ListenerManager;

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

  private ListenerManager lm;
  private String eventType;

  public ActionSourceListener(String eventType) {
    this.eventType = eventType;

    // manage ActionValueEvent Listeners
    lm = new ListenerManager(
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