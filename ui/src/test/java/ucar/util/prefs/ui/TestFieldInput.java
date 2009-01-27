// $Id: TestFieldInput.java,v 1.1 2004/08/26 17:55:19 caron Exp $
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

import ucar.util.prefs.*;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import javax.swing.*;

import junit.framework.*;


public class TestFieldInput extends TestCase {
  private static XMLStore xstore;
  private static PreferencesExt store;

  private Field tf, intf, d1, d2;

  public TestFieldInput( String name) {
    super(name);
  }

  public void setUp() {
    try {
      xstore = XMLStore.createFromFile(TestAllPrefs.dir+"testField.xml", null);
    } catch (java.io.IOException e) {}
    store = xstore.getPreferences();
  }


  public void testFieldInput() {
    PreferencesExt node = (PreferencesExt) store.node("test22");
    PrefPanel.Dialog d = new PrefPanel.Dialog(null, false, "title", null);
    PrefPanel pp = d.getPrefPanel();

    tf = pp.addTextField("text", "name", "defValue");
    tf.addPropertyChangeListener(new PropertyChangeListener () {
      int count = 0;
      public void propertyChange(PropertyChangeEvent evt) {
        System.out.println("Text TestFieldInput: old val= <"+evt.getOldValue()+"> new val= <"+evt.getNewValue()+">");
        if (count == 0) {
          assert ((String) evt.getOldValue()).equals("defValue") : "<"+evt.getOldValue()+">";
          assert ((String) evt.getNewValue()).equals("better value") : "<"+evt.getNewValue()+">";
        } else if (count == 1) {
          assert ((String) evt.getOldValue()).equals("better value") : "<"+evt.getOldValue()+">";
          assert ((String) evt.getNewValue()).equals("best value") : "<"+evt.getNewValue()+">";
        }
        count++;
      }
    });
    ((Field.Text)tf).setText("better value");

    intf = pp.addIntField("int", "int", 66);
    intf.addPropertyChangeListener(new PropertyChangeListener () {
      int count = 0;
      public void propertyChange(PropertyChangeEvent evt) {
        System.out.println("Int TestFieldInput:  old val= <"+evt.getOldValue()+"> new val= <"+evt.getNewValue()+">");
        if (count == 0) {
          assert evt.getOldValue() instanceof Integer;
          assert ((Integer) evt.getOldValue()).intValue() == 66 : "<"+evt.getOldValue()+">";
          assert ((Integer) evt.getNewValue()).intValue() == 666 : "<"+evt.getNewValue()+">";
        } if (count == 1) {
          assert evt.getOldValue() instanceof Number;
          assert evt.getNewValue() instanceof Number;
          assert ((Number) evt.getOldValue()).intValue() == 666 : "<"+evt.getOldValue()+">";
          assert ((Number) evt.getNewValue()).intValue() == 6666 : "<"+evt.getNewValue()+">";
        }
        count++;
       //System.out.println("   getInt = "+ ((Field.Int)intf).getInt());
      }
    });
    ((Field.Int)intf).setInt(666);


    d1 = pp.addDoubleField("min", "min", -999.99);
    d1.addPropertyChangeListener(new PropertyChangeListener () {
      public void propertyChange(PropertyChangeEvent evt) {
       System.out.println("Double 1 TestFieldInput: old val= <"+evt.getOldValue()+"> new val= <"+evt.getNewValue()+">");
     }
    });

    d2 = pp.addDoubleField("max", "max", 10000.0);
    d2.addPropertyChangeListener(new PropertyChangeListener () {
      public void propertyChange(PropertyChangeEvent evt) {
       System.out.println("Double 2: TestFieldInput: old val= <"+evt.getOldValue()+"> new val= <"+evt.getNewValue()+">");
     }
    });
    d.finish();
    d.show();

    node.put("text", "best value");
    node.putInt("int", 6666);
    node.putDouble("min", 0.0);
  }


  public static void main(String[] args) {
    try {
      xstore = XMLStore.createFromFile(TestAllPrefs.dir+"TestFieldInput.xml", null);
    } catch (java.io.IOException e) {}
    store = xstore.getPreferences();

    new TestFieldInput("TestFieldInput").testFieldInput();

    JFrame frame = new JFrame("TestFieldInput");
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {System.exit(0);}
    });

    JPanel main = new JPanel( new BorderLayout()); // new FlowLayout());
    frame.getContentPane().add(main);
    JFormattedTextField d1 = new JFormattedTextField();
    d1.setValue(new Double(123.987));
    JFormattedTextField d2 = new JFormattedTextField();
    d2.setValue(new Double(999.123));
    main.add( d1, BorderLayout.NORTH);
    main.add( d2, BorderLayout.SOUTH);

    frame.pack();
    frame.setLocation(400, 300);
    frame.setVisible(true);
  }



}
/* Change History:
   $Log: TestFieldInput.java,v $
   Revision 1.1  2004/08/26 17:55:19  caron
   no message

   Revision 1.2  2002/12/24 22:04:54  john
   add bean, beanObject methods

   Revision 1.1.1.1  2002/12/20 16:40:27  john
   start new cvs root: prefs

*/