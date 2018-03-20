/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: TestEvents.java,v 1.1.1.1 2002/12/20 16:40:27 john Exp $


package ucar.util.prefs;

import junit.framework.*;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.prefs.*;

public class TestEvents extends TestCase {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  
    static {
        System.setProperty("java.util.prefs.PreferencesFactory", "ucar.util.prefs.PreferencesExtFactory");
    }

  private boolean debug = false;

  public TestEvents( String name) {
    super(name);
  }

  public void testNodeChange() {
    try {
      Preferences userRoot = Preferences.userRoot();

      userRoot.addNodeChangeListener( new NodeChangeListener () {
        public void childAdded(NodeChangeEvent evt) {
          if (debug)
            System.out.println("childAdded = "+evt.getParent().name()+" "+
                    evt.getChild().name());
        }
        public void childRemoved(NodeChangeEvent evt) {
          if (debug)
            System.out.println("childRemoved = "+evt.getParent().name()+" " +
                   evt.getChild().name());
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
          if (evt.getKey().equals("love"))
            Assert.assertEquals(" node "+ evt.getNode().name()+" key = <" +
                    evt.getKey()+"> val= <"+evt.getNewValue()+">", "ok",
                    evt.getNewValue());
          else if (evt.getKey().equals("love2"))
              Assert.assertEquals(" node "+ evt.getNode().name()+" key = <" +
                              evt.getKey()+"> val= <"+evt.getNewValue()+">",
                      "not ok", evt.getNewValue());
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
