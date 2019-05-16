/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.prefs;

import java.awt.HeadlessException;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.XMLStore;

import java.beans.*;
import java.lang.invoke.MethodHandles;
import java.util.*;
import javax.swing.*;

@RunWith(JUnit4.class)
public class TestPanel2 {

  private static final Logger logger = LoggerFactory
      .getLogger(MethodHandles.lookup().lookupClass());

  @ClassRule
  public static TemporaryFolder tempFolder = new TemporaryFolder();

  static {
    System
        .setProperty("java.util.prefs.PreferencesFactory", "ucar.util.prefs.PreferencesExtFactory");
  }

  private static XMLStore xstore;
  private static PreferencesExt store;

  @BeforeClass
  public static void setUp() {
    try {
      xstore = XMLStore.createFromFile(tempFolder.newFile().getAbsolutePath(), null);
    } catch (java.io.IOException e) {
    }
    store = xstore.getPreferences();
  }

  @Test
  public void testPanel() {
    makeTestPanel((PreferencesExt) store.node("test"));
  }

  private PrefPanel makeTestPanel(PreferencesExt prefs) {
    PrefPanel pp = new PrefPanel("title", prefs);
    pp.addTextField("name", "name", "defValue", 0, 0, null);
    Field.Text longF = pp.addTextField("Longname", "Longname",
        "defValue really long name for to be with starting value gotta adjust the thing", 0, 1,
        null);
    pp.addHeading("Adult Material", 2);
    pp.addTextField("nude", "nude", "nudeCOlumn", 0, 3, null);
    pp.finish();
    return pp;
  }

  private PrefPanel makeTestPanelOld(PreferencesExt prefs) {
    PrefPanel pp = new PrefPanel("title", prefs);
    pp.addTextField("name", "name", "defValue");
    pp.addTextField("Longname", "Longname",
        "defValue really long name for to be with starting value gotta adjust the thing");
    pp.addHeading("Adult Material");
    pp.addTextField("nude", "nude", "nudeCOlumn");
    pp.finish();
    return pp;
  }

  private PrefPanel makeTestPanelFirstHeading(PreferencesExt prefs) {
    PrefPanel pp = new PrefPanel("title", prefs);
    int row = 0;
    pp.addHeading("Basics", row++);
    pp.addTextField("name", "name", "defValue", 0, row++, null);
    Field.Text longF = pp.addTextField("Longname", "Longname",
        "defValue really long name for to be with starting value gotta adjust the thing", 0, row++,
        null);
    pp.addHeading("Adult Material", row++);
    pp.addTextField("nude", "nude", "nudeCOlumn", 0, row++, null);
    pp.finish();
    return pp;
  }

  @Test
  public void testPanelDup() {
    PrefPanel pp2 = new PrefPanel("title", (PreferencesExt) store.node("dup"));
    pp2.addTextField("name", "name", "defValue");
    Field.Text longF = pp2.addTextField("Longname", "Longname",
        "defValue really long name for to be with starting value gotta adjust the thing");

    // test duplicate field name
    try {
      pp2.addTextField("name", "name", "defValue");
      pp2.finish();
      assert (false);
    } catch (IllegalArgumentException e) {
      System.out.println(e.getMessage());
      assert (true);
    }
  }

  @Test
  public void testPanel2() {
    try {
      PrefPanel.Dialog preferenceDialog = new PrefPanel.Dialog(null, false, "Preference Dialog",
          (PreferencesExt) store.node("dialog"));
      PrefPanel pp = preferenceDialog.getPrefPanel();
      pp.addTextField("textField", "textField", "defValue");
      Field.Text tt = pp.addTextField("hasaToolTip", "hasaToolTip", "glob");
      tt.setToolTipText("i told you!");
      pp.addPasswordField("password", "password", "glombulate");
      pp.addTextComboField("combo", "combo", new ArrayList(), 20, true);
      Field.Text at = pp.addTextField("acceptListener", "acceptListener", "hitEnterOrAccept");
      at.addPropertyChangeListener(new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
          System.out
              .println("got new <" + evt.getNewValue() + "> old = <" + evt.getOldValue() + ">");
        }
      });

      pp.addSeparator();
      pp.addHeading("This is the Heading");
      pp.addIntField("intField", "intField", 22);
      pp.addSeparator();
      pp.addCheckBoxField("checkitout", "checkitout", true);
      Field.Int iu = pp.addIntField("testIU", "number of times to barf", 2);

      preferenceDialog.finish();
      preferenceDialog.setVisible(true);

      store.putInt("myInt", 42);
      assert (store.getInt("myInt", 43) == 42);

      pp.addActionListener(e -> {
        System.out.println("got accept");
        //xstore.save();
      });

    } catch (HeadlessException e) {
      // ok to fail if there is no display
    }
  }

  @Test
  public void testOtherPanels() {
    try {
      makeComboBox();
      makeDialog();

      makeTestPanelOld(null);
      makeTestPanel(null);
      makeTestPanelFirstHeading(null);
      make3columns();
      make3columnsOneLong();
    } catch (HeadlessException e) {
      // ok to fail if there is no display
    }
  }

  private PrefPanel makeComboBox() {

    PrefPanel pp = new PrefPanel("testCombo", null);
    Field.TextCombo fcb = pp
        .addTextComboField("datatypesText", "Datatypes Text", DataType.getTypeNames(), 20, true);
    Field.TextCombo fcb2 = pp
        .addTextComboField("datatypesObjects", "Datatypes Objects", DataType.getTypes(), 20, true);
    Field.TextArea fta = pp.addTextAreaField("textArea", "Text Area",
        "4 score and seventeen long gloriuos and longer nights ago.", 3);
    pp.finish();

    return pp;
  }

  private PrefPanel make3columns() {

    PrefPanel pp = new PrefPanel("test", null);

    int row = 0;
    pp.addHeading("Heading", row++);
    pp.addTextField("name", "column1", "def", 0, row, null);
    pp.addTextField("name2", "column2", "defValue22", 2, row, null);
    pp.addTextField("name3", "column3", "defValue22 and jalskdjalksjd", 4, row++, null);
    pp.addTextField("name32", "column32", "asdkjssdsd", 4, row++, null);
    pp.addTextField("name33", "column33", "zogitwhen", 4, row++, null);

    pp.finish();
    return pp;
  }

  private PrefPanel make3columnsOneLong() {

    PrefPanel pp = new PrefPanel("test", null);

    int row = 0;
    pp.addHeading("Heading", row++);
    pp.addTextField("name", "column1", "def", 0, row++, "3,1");
    pp.addTextField("name2", "column2", "defValue22", 2, row, null);
    pp.addTextField("name3", "column3", "defValue22 and jalskdjalksjd", 4, row++, null);
    pp.addTextField("name32", "column32", "asdkjssdsd", 4, row++, null);
    pp.addTextField("name33", "column33", "zogitwhen", 4, row++, null);

    pp.finish();
    return pp;
  }


  private PrefPanel.Dialog makeDialog() {
    PrefPanel.Dialog d = new PrefPanel.Dialog(null, false, "testDialogue", null);
    PrefPanel pp2 = d.getPrefPanel();
    pp2.addHeading("This is Not Your Life!");
    pp2.addTextField("name", "name", "defValue");
    pp2.addTextField("name2", "name2", "defValue22");
    pp2.addTextField("name3", "name3", "defValue22 asd jalskdjalksjd");
    pp2.addSeparator();
    pp2.addHeading("Part Two");
    pp2.addPasswordField("password", "password", "secret");
    pp2.addIntField("testInt", "testInt", 1234);
    pp2.addDoubleField("testD", "testD", 1234.45);
    pp2.addCheckBoxField("testB", "testB", true);
    pp2.setCursor(2, 1);

/*    pp2.addHeading("Another Column:");
    pp2.addDateField("date", "date", new Date());
    try {
      JFormattedTextField tf = new JFormattedTextField(new javax.swing.text.MaskFormatter("(###) ###-####"));
      pp2.addTextFormattedField("ff", "ff", tf, "(303) 497-1234");
    } catch (java.text.ParseException e) { } */

    ArrayList list = new ArrayList(5);
    list.add("this");
    list.add("is");
    list.add("new");
    list.add("but");
    list.add("really too longs");
    pp2.addTextComboField("combo", "combo", list, 5, true);
    pp2.addEnumComboField("datatypes", "Datatypes", DataType.getTypes(), false);

    return d;
  }


  @Test
  public void testit() {
    try {
      JFrame frame = new JFrame("Test PrefPanel");
      frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

      TestPanel2 tp = new TestPanel2();
      //PrefPanel pp = tp.makeTestPanelOld(null);
      //PrefPanel pp = tp.makeTestPanel(null);
      //PrefPanel pp = tp.makeTestPanelFirstHeading(null);
      //PrefPanel pp = tp.make3columns();
      PrefPanel pp = tp.make3columnsOneLong();

      //PrefPanel pp =  tp.makeComboBox();
      //PrefPanel pp =  tp.makeDialog().getPrefPanel();

      frame.getContentPane().add(pp);
      frame.pack();
      frame.setLocation(300, 300);
      //frame.setSize(300, 300);
      frame.setVisible(true);

      PrefPanel.Dialog d = tp.makeDialog();
      d.finish();
      d.setVisible(true);
    } catch (HeadlessException e) {
      // ok to fail if there is no display
    }
  }
}
