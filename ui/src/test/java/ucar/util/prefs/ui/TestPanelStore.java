// $Id: TestPanel.java,v 1.2 2003/05/29 23:33:28 john Exp $
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