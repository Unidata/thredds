/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: TestField.java,v 1.4 2005/08/22 01:15:06 caron Exp $

package ucar.util.prefs.ui;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.XMLStore;

import java.beans.*;
import java.lang.invoke.MethodHandles;

public class TestField {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  static {
      System.setProperty("java.util.prefs.PreferencesFactory", "ucar.util.prefs.PreferencesExtFactory");
  }
  static private XMLStore xstore;
  static private PreferencesExt store;

  private int gotEvent1, gotEvent2;

  public void setUp() {
    try {
      xstore = XMLStore.createFromFile(tempFolder.newFile().getAbsolutePath(), null);
    } catch (java.io.IOException e) {}
    store = xstore.getPreferences();
  }

  public void testPropertyChange() {
    System.out.println("****TestField");
    PreferencesExt node = (PreferencesExt) store.node("test22");
    PrefPanel.Dialog d = new PrefPanel.Dialog(null, false, "title", node);
    PrefPanel pp = d.getPrefPanel();
    Field.Text tf = pp.addTextField("name", "name", "defValue");
    final Field.Int intf = pp.addIntField("int", "int", 66);

      tf.addPropertyChangeListener(new PropertyChangeListener () {
        public void propertyChange(PropertyChangeEvent evt) {
          gotEvent1++;
          System.out.println("testField: (tf) PropertyChangeListener old val= <"+evt.getOldValue()+"> new val= <"+evt.getNewValue()+"> "+ gotEvent1);
        }
      });

      intf.addPropertyChangeListener(new PropertyChangeListener () {
        public void propertyChange(PropertyChangeEvent evt) {
          gotEvent2++;
          System.out.println("testField: (intf) PropertyChangeListener old val= <"+evt.getOldValue()+"> new val= <"+evt.getNewValue()+">");
          System.out.println("   getInt = "+intf.getInt());
        }
      });

    d.finish();
    d.setVisible(true);

    assert gotEvent1 == 0;
    tf.setEditValue("better value");
    tf.accept(null);
    assert gotEvent1 == 1 : gotEvent1;
    node.put("name", "best value");
    // assert gotEvent1 == 2 : gotEvent1; race condition

    assert gotEvent2 == 0;
    intf.setInt(666);
    assert gotEvent2 == 0 : gotEvent2;
  }

  public void testComboText() {
    PreferencesExt node = (PreferencesExt) store.node("testCombo");
    PrefPanel pp = new PrefPanel("testCombo", node);
    Field.TextCombo fcb = pp.addTextComboField("datatypes", "Datatypes", DataType.getTypeNames(), 20, false);
    pp.finish();

    fcb.setText("newbie");
    String v = fcb.getText();
    assert( v.equals("newbie"));

    pp.accept();
    try {
      xstore.save();
    } catch (java.io.IOException ioe) {
      ioe.printStackTrace();
      assert false;
    }
  }

  public void testComboObjects() {
    PreferencesExt node = (PreferencesExt) store.node("testComboObjects");
    PrefPanel pp = new PrefPanel("testCombo", node);
    Field.TextCombo fcb = pp.addTextComboField("datatypes", "Datatypes", DataType.getTypes(), 20, false);
    pp.finish();

    fcb.setText("newbie");
    String v = fcb.getText();
    assert( v.equals("newbie"));

    pp.accept();
    try {
      xstore.save();
    } catch (java.io.IOException ioe) {
      assert false;
    }
  }

  public void testEnumCombo() {
    PreferencesExt node = (PreferencesExt) store.node("testComboObjects");
    PrefPanel pp = new PrefPanel("testCombo", node);
    Field.EnumCombo fcb = pp.addEnumComboField("datatypes", "Datatypes", DataType.getTypes(), false);
    pp.finish();

    fcb.setValue("newbie");
    Object v = fcb.getValue();
    assert( v.equals("newbie"));

    DataType t = new DataType("newboo");
    fcb.setValue(t);
    Object vt = fcb.getValue();
    assert( vt == t) : vt;

    pp.accept();
    try {
      xstore.save();
    } catch (java.io.IOException ioe) {
      assert false;
    }
  }

}
