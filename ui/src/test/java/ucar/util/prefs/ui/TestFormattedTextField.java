/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: TestFormattedTextField.java,v 1.1 2004/08/26 17:55:19 caron Exp $

package ucar.util.prefs.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.*;
import java.lang.invoke.MethodHandles;
import java.text.*;
import javax.swing.*;
import javax.swing.text.*;

public class TestFormattedTextField {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  static String pattern = "none";
  static JFormattedTextField d1;
  static JTextField patternTF;
  static NumberFormatter nf;
  static DecimalFormat decFormatter;
  static JCheckBox intOnly, allowsInvalid;
  static boolean intOnlyValue, allowsInvalidValue;

  public JPanel makePanel() {
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

    TestFormattedTextField ff = new TestFormattedTextField();
    frame.getContentPane().add( ff.makePanel());

    frame.pack();
    frame.setLocation(400, 300);
    frame.setVisible(true);
  }

}
