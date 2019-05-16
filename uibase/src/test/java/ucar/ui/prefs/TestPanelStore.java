/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.prefs;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.XMLStore;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Date;

@RunWith(JUnit4.class)
public class TestPanelStore {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @ClassRule public static TemporaryFolder tempFolder = new TemporaryFolder();

  private static PreferencesExt store;
  private static XMLStore xstore;

  @BeforeClass
  public static void setup() throws IOException {
    System.setProperty("java.util.prefs.PreferencesFactory", "ucar.util.prefs.PreferencesExtFactory");

    xstore = XMLStore.createFromFile(tempFolder.newFile().getAbsolutePath(), null);
    store = xstore.getPreferences();
    //store = new PreferencesExt(null,"");
    Debug.setStore( store.node("Debug"));
  }

  @Test
  public void testPanelStore() {
    makePP();
  }

  private PrefPanel makePP() {
    PrefPanel pp2 = new PrefPanel( "TestPanelStoreName", (PreferencesExt) store.node("TestPanelStoreNode"));
    pp2.addHeading("Some Input Fileds");
    pp2.addTextField("text1", "text1", "text1");
    pp2.addTextField("nullDefault", "nullDefault", null);
    pp2.addCheckBoxField("testCheckBox", "testCheckBox", true);
    pp2.addPasswordField("password", "password", "secret");
    pp2.addSeparator();
    pp2.addHeading("Formatted Text Input");
    pp2.addIntField("testInt", "testInt", 1234);
    pp2.addDoubleField("testDouble", "testDouble", 1234.45);
    pp2.addDateField("testDate", "testDate", new Date());

    pp2.setCursor(1, 1);

    ArrayList list = new ArrayList(5);
    list.add("this");
    list.add("is");
    list.add("new");
    list.add("but");
    list.add("really quite long");
    pp2.addTextComboField("combo", "combo", list, 5, true);

    pp2.addTextComboField("comboNoedit", "comboNoedit", DataType.getTypeNames(), 5, false);


/*    pp2.addHeading("Another Column:");
    pp2.addDateField("date", "date", new Date());
    try {
      JFormattedTextField tf = new JFormattedTextField(new javax.swing.text.MaskFormatter("(###) ###-####"));
      pp2.addTextFormattedField("ff", "ff", tf, "(303) 497-1234");
    } catch (java.text.ParseException e) { } */


    pp2.finish();
    return pp2;
  }

  /** test */
  public static void main(String args[]) {
    JFrame frame = new JFrame("Test PrefPanelStore");
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        try {
          xstore.save();
          System.out.println("write xstore");
        } catch (java.io.IOException ioe) { ioe.printStackTrace(); }
        System.exit(0);
      }
    });

    TestPanelStore tp = new TestPanelStore();
    PrefPanel pp = tp.makePP();

    frame.getContentPane().add(pp);
    frame.pack();
    frame.setLocation(300, 300);
    frame.setVisible(true);
  }
}
