// $Id: ActionCoordinator.java 50 2006-07-12 16:30:06Z caron $
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

/** An ActionCoordinator helps manage the set of objects that send and receive
 *  an ActionValueEvent. It is assumed that each event generator is also
 *  interested in recieving the event if its from someone else; these objects are
 *  of type ActionSourceListener.
 *
 * For each kind of event, an ActionCoordinator
 *  object is created. When it gets an event, it sends it to all others who have
 *  registered except not to the event source.
 *
 * @see ActionValueEvent
 * @see ActionSourceListener
 *
 * @author John Caron
 * @version $Id: ActionCoordinator.java 50 2006-07-12 16:30:06Z caron $
 */

public class ActionCoordinator implements ActionValueListener {
  private ListenerManager lm;
  private String eventType;

  public ActionCoordinator(String eventType) {
    this.eventType = eventType;

    // manage Action Listeners
    lm = new ListenerManager(
        "thredds.viewer.ui.event.ActionValueListener",
        "thredds.viewer.ui.event.ActionValueEvent",
        "actionPerformed");
  }

  public void actionPerformed( ActionValueEvent e) {
    lm.sendEventExcludeSource(e);
  }

        /** add an ActionSource listener */
  public void addActionSourceListener( ActionSourceListener l) {
    if (!eventType.equals(l.getEventTypeName()))
      throw new IllegalArgumentException("ActionCoordinator: tried to add ActionSourceListener for wrong kind of Action "+
         eventType+ " != "+ l.getEventTypeName());

    lm.addListener(l);
    l.addActionValueListener(this);
  }
      /** remove an ActionSource listener */
  public void removeActionSourceListener( ActionSourceListener l) {
    lm.removeListener(l);
    l.removeActionValueListener(this);
  }

    /** add an ActionValue listener
  public void addActionValueListener( ActionValueListener l) {
    lm.addListener(l);
  }
    /** remove an ActionValue listener
  public void removeActionValueListener( ActionValueListener l) {
    lm.removeListener(l);
  } */

  static public void main( String[] argv) {
    ActionCoordinator ac = new ActionCoordinator("test");
/*    System.out.println("failure test------------");
    try {
      ac.addActionSourceListener(new ActionSourceListener("that") {
        public void actionPerformed( java.awt.event.ActionEvent e) {
          System.out.println(" event ok ");
        }
      });
      System.out.println("good dog!");
    } catch (IllegalArgumentException e) {
      System.out.println("bad dog! = "+e);
    }

    System.out.println("next test------------");  */
    ActionSourceListener as1 = new ActionSourceListener("test") {
      public void actionPerformed( ActionValueEvent e) {
        System.out.println(" first listener got event "+e.getValue());
      }
    };
    ac.addActionSourceListener(as1);

    ActionSourceListener as2 = new ActionSourceListener("test") {
      public void actionPerformed( ActionValueEvent e) {
        System.out.println(" second listener got event "+e.getValue());
      }
    };
    ac.addActionSourceListener(as2);

    ActionSourceListener as3 = new ActionSourceListener("test") {
      public void actionPerformed( ActionValueEvent e) {
        System.out.println(" third listener got event "+e.getValue());
      }
    };
    ac.addActionSourceListener(as3);

    as1.fireActionValueEvent("testing", "newValue 1");
    as2.fireActionValueEvent("testing", "newValue 2");
  }
}
