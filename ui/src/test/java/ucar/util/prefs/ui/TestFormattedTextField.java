// $Id: TestFormattedTextField.java,v 1.1 2004/08/26 17:55:19 caron Exp $
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

import java.awt.*;
import java.awt.event.*;
import java.text.*;
import javax.swing.*;
import javax.swing.text.*;

import junit.framework.*;

public class TestFormattedTextField extends TestCase {
  static String pattern = "none";
  static JFormattedTextField d1;
  static JTextField patternTF;
  static NumberFormatter nf;
  static DecimalFormat decFormatter;
  static JCheckBox intOnly, allowsInvalid;
  static boolean intOnlyValue, allowsInvalidValue;

  public TestFormattedTextField( String name) {
    super(name);
  }

  private JPanel makePanel() {
    JPanel main = new JPanel();
    main.setLayout( new BoxLayout(main, BoxLayout.Y_AXIS));

    NumberFormat realFormatter = NumberFormat.getNumberInstance();
    realFormatter.setMinimumFractionDigits(2);
    realFormatter.setMaximumFractionDigits(4);
    JFormattedTextField.AbstractFormatterFactory formatterFactory = new DefaultFormatterFactory( new NumberFormatter(realFormatter));

    JPanel p1 = new JPanel();
    p1.add( new JLabel("JFormattedTextField:"));
    d1 = new JFormattedTextField(formatterFactory);
    d1.setValue(new Double(123.987));
    Dimension prefDim = d1.getPreferredSize();
    d1.setPreferredSize(new Dimension( 100, (int) prefDim.getHeight()));
    p1.add(d1);

    JFormattedTextField.AbstractFormatter ff = d1.getFormatter();
    System.out.println("AbstractFormatter  = "+ff.getClass().getName());
    Object val = d1.getValue();
    System.out.println(" Value  = "+val.getClass().getName());
    if (ff instanceof NumberFormatter) {
      nf = (NumberFormatter) ff;
      allowsInvalidValue = nf.getAllowsInvalid();
      Format f = nf.getFormat();
      System.out.println(" Format for = "+f.getClass().getName());
      if (f instanceof NumberFormat) {
        NumberFormat nfat = (NumberFormat) f;
        System.out.println(" getMinimumIntegerDigits="+nfat.getMinimumIntegerDigits());
        System.out.println(" getMaximumIntegerDigits="+nfat.getMaximumIntegerDigits());
        System.out.println(" getMinimumFractionDigits="+nfat.getMinimumFractionDigits());
        System.out.println(" getMaximumFractionDigits="+nfat.getMaximumFractionDigits());
      }
      if (f instanceof DecimalFormat) {
        decFormatter = (DecimalFormat) f;
        System.out.println(" Pattern = "+decFormatter.toPattern());
        pattern = decFormatter.toPattern();
        intOnlyValue = decFormatter.isParseIntegerOnly();
      }
    }

    JPanel p2 = new JPanel();
    p2.add( new JLabel("Pattern:"));
    patternTF = new JTextField();
    patternTF.setText(pattern);
    patternTF.setColumns( 20);
    patternTF.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        System.out.println("pattern actionevent "+patternTF.getText());
        decFormatter.applyPattern( patternTF.getText());
      }
    });
    p2.add(patternTF);

    intOnly = new JCheckBox("IntegerOnly", intOnlyValue);
    intOnly.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        boolean val = intOnly.getModel().isSelected();
        System.out.println("intOnly actionevent "+val);
        decFormatter.setParseIntegerOnly( val);
      }
    });

    allowsInvalid = new JCheckBox("AllowsInvalid", allowsInvalidValue);
    allowsInvalid.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        boolean val = allowsInvalid.getModel().isSelected();
        System.out.println("allowsInvalid actionevent "+val);
        nf.setAllowsInvalid( val);
      }
    });

    JButton gv = new JButton("Get Value");
    gv.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        boolean val = intOnly.getModel().isSelected();
        System.out.println("value= "+d1.getValue()+" "+d1.getValue().getClass().getName());
      }
    });

    main.add( p1);
    main.add( p2);
    main.add( intOnly);
    main.add( allowsInvalid);
    main.add( gv);
    return main;
  }

  public static void main(String[] args) {

    JFrame frame = new JFrame("TestFormattedTextField");
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {System.exit(0);}
    });

    TestFormattedTextField ff = new TestFormattedTextField( "fake");
    frame.getContentPane().add( ff.makePanel());

    frame.pack();
    frame.setLocation(400, 300);
    frame.setVisible(true);
  }



}
/* Change History:
   $Log: TestFormattedTextField.java,v $
   Revision 1.1  2004/08/26 17:55:19  caron
   no message

   Revision 1.2  2002/12/24 22:04:54  john
   add bean, beanObject methods

   Revision 1.1.1.1  2002/12/20 16:40:27  john
   start new cvs root: prefs

*/