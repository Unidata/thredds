// $Id: TestField.java,v 1.4 2005/08/22 01:15:06 caron Exp $
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

import java.beans.*;

public class TestField extends TestCase {
  static private XMLStore xstore;
  static private PreferencesExt store;

  private int gotEvent1, gotEvent2;
  public TestField( String name) {
    super(name);
  }

  public void setUp() {
    try {
      xstore = XMLStore.createFromFile(TestAllPrefs.dir+"testField.xml", null);
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
    d.show();

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
/* Change History:
   $Log: TestField.java,v $
   Revision 1.4  2005/08/22 01:15:06  caron
   no message

   Revision 1.3  2004/08/26 17:55:19  caron
   no message

   Revision 1.2  2002/12/24 22:04:54  john
   add bean, beanObject methods

   Revision 1.1.1.1  2002/12/20 16:40:27  john
   start new cvs root: prefs

*/