// $Id: ActionCoordinator.java 50 2006-07-12 16:30:06Z caron $
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
  private thredds.util.ListenerManager lm;
  private String eventType;

  public ActionCoordinator(String eventType) {
    this.eventType = eventType;

    // manage Action Listeners
    lm = new thredds.util.ListenerManager(
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
