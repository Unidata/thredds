// $Id: TestEvents.java,v 1.1.1.1 2002/12/20 16:40:27 john Exp $
/*
 * Copyright 1997-2000 Unidata Program Center/University Corporation for
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

package ucar.util.prefs;

import junit.framework.*;
import java.io.*;
import java.util.prefs.*;

public class TestEvents extends TestCase {

  private boolean debug = false;

  public TestEvents( String name) {
    super(name);
  }

  public void testNodeChange() {
    System.out.println("***TestEvent");
    try {
      Preferences userRoot = Preferences.userRoot();

      userRoot.addNodeChangeListener( new NodeChangeListener () {
        public void childAdded(NodeChangeEvent evt) {
          System.out.println("childAdded = "+evt.getParent().name()+" "+evt.getChild().name());
        }
        public void childRemoved(NodeChangeEvent evt) {
          System.out.println("childRemoved = "+evt.getParent().name()+" "+evt.getChild().name());
        }
      });

      Preferences node = userRoot.node("testAdd");
      node.removeNode();

    } catch (Exception e) {
      System.out.println(e);
      e.printStackTrace();
    }
  }

  public void testPreferenceChange() {
    try {
      Preferences userRoot = Preferences.userRoot();
      Preferences node = userRoot.node("testAdd");

      node.addPreferenceChangeListener(new PreferenceChangeListener () {
        public void preferenceChange(PreferenceChangeEvent evt) {
          System.out.println(" node "+ evt.getNode().name()+" key = <"+evt.getKey()+"> val= <"+evt.getNewValue()+">");
          if (evt.getKey().equals("love"))
            assert evt.getNewValue().equals("ok") : evt.getNewValue();
          else if (evt.getKey().equals("love2"))
            assert evt.getNewValue().equals("not ok") : evt.getNewValue();
        }
      });

      node.put("love","ok");
      node.put("love2","not ok");

    } catch (Exception e) {
      System.out.println(e);
      e.printStackTrace();
    }
  }

}
/* Change History:
   $Log: TestEvents.java,v $
   Revision 1.1.1.1  2002/12/20 16:40:27  john
   start new cvs root: prefs

*/