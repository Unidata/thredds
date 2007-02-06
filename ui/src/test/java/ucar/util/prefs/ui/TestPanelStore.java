// $Id: TestPanel.java,v 1.2 2003/05/29 23:33:28 john Exp $
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
package ucar.util.prefs.ui;

import junit.framework.*;
import ucar.util.prefs.*;

import java.awt.event.*;
import java.util.*;
import javax.swing.*;

public class TestPanelStore extends TestCase {
  static private XMLStore xstore;
  static private PreferencesExt store;

  public TestPanelStore( String name) {
    super(name);
    try {
      xstore = XMLStore.createFromFile(TestAllPrefs.dir+"panelStore.xml", null);
    } catch (java.io.IOException e) {}
    store = xstore.getPreferences();
  }

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

    TestPanelStore tp = new TestPanelStore("fake");
    PrefPanel pp = tp.makePP();

    frame.getContentPane().add(pp);
    frame.pack();
    frame.setLocation(300, 300);
    frame.setVisible(true);
  }


}
/* Change History:
   $Log: TestPanel.java,v $
   Revision 1.2  2003/05/29 23:33:28  john
   latest release

   Revision 1.1.1.1  2002/12/20 16:40:27  john
   start new cvs root: prefs

*/