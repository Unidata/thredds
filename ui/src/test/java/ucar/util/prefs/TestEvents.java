// $Id: TestEvents.java,v 1.1.1.1 2002/12/20 16:40:27 john Exp $
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