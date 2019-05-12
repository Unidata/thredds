/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.util.prefs;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import java.util.prefs.*;

@RunWith(JUnit4.class)
public class TestEvents {

  static {
    System
        .setProperty("java.util.prefs.PreferencesFactory", "ucar.util.prefs.PreferencesExtFactory");
  }

  private boolean debug = false;

  @Test
  public void testNodeChange() throws BackingStoreException {
    Preferences userRoot = Preferences.userRoot();

    userRoot.addNodeChangeListener(new NodeChangeListener() {
      public void childAdded(NodeChangeEvent evt) {
        if (debug) {
          System.out.println("childAdded = " + evt.getParent().name() + " " +
              evt.getChild().name());
        }
      }

      public void childRemoved(NodeChangeEvent evt) {
        if (debug) {
          System.out.println("childRemoved = " + evt.getParent().name() + " " +
              evt.getChild().name());
        }
      }
    });

    Preferences node = userRoot.node("testAdd");
    node.removeNode();
  }

  @Test
  public void testPreferenceChange() {
    Preferences userRoot = Preferences.userRoot();
    Preferences node = userRoot.node("testAdd");

    node.addPreferenceChangeListener(new PreferenceChangeListener() {
      public void preferenceChange(PreferenceChangeEvent evt) {
        if (evt.getKey().equals("love")) {
          Assert.assertEquals(" node " + evt.getNode().name() + " key = <" +
                  evt.getKey() + "> val= <" + evt.getNewValue() + ">", "ok",
              evt.getNewValue());
        } else if (evt.getKey().equals("love2")) {
          Assert.assertEquals(" node " + evt.getNode().name() + " key = <" +
                  evt.getKey() + "> val= <" + evt.getNewValue() + ">",
              "not ok", evt.getNewValue());
        }
      }
    });

    node.put("love", "ok");
    node.put("love2", "not ok");
  }
}