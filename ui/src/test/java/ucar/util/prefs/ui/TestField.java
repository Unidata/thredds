// $Id: TestField.java,v 1.4 2005/08/22 01:15:06 caron Exp $
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