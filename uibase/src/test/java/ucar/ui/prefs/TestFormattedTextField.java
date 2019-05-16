/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.prefs;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.*;
import java.lang.invoke.MethodHandles;
import java.text.*;
import javax.swing.*;
import javax.swing.text.*;

@RunWith(JUnit4.class)
public class TestFormattedTextField {

  private static final Logger logger = LoggerFactory
      .getLogger(MethodHandles.lookup().lookupClass());

  static String pattern = "none";
  static JFormattedTextField d1;
  static JTextField patternTF;
  static NumberFormatter nf;
  static DecimalFormat decFormatter;
  static JCheckBox intOnly, allowsInvalid;
  static boolean intOnlyValue, allowsInvalidValue;

  private JPanel makePanel() {
    JPanel main = new JPanel();
    main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));

    NumberFormat realFormatter = NumberFormat.getNumberInstance();
    realFormatter.setMinimumFractionDigits(2);
    realFormatter.setMaximumFractionDigits(4);
    JFormattedTextField.AbstractFormatterFactory formatterFactory = new DefaultFormatterFactory(
        new NumberFormatter(realFormatter));

    JPanel p1 = new JPanel();
    p1.add(new JLabel("JFormattedTextField:"));
    d1 = new JFormattedTextField(formatterFactory);
    d1.setValue(new Double(123.987));
    Dimension prefDim = d1.getPreferredSize();
    d1.setPreferredSize(new Dimension(100, (int) prefDim.getHeight()));
    p1.add(d1);

    JFormattedTextField.AbstractFormatter ff = d1.getFormatter();
    logger.info("AbstractFormatter  = {}", ff.getClass().getName());
    Object d1val = d1.getValue();
    logger.info(" Value  = {}", d1val.getClass().getName());

    if (ff instanceof NumberFormatter) {
      nf = (NumberFormatter) ff;
      allowsInvalidValue = nf.getAllowsInvalid();
      Format f = nf.getFormat();
      logger.info(" Format for = {}", f.getClass().getName());
      if (f instanceof NumberFormat) {
        NumberFormat nfat = (NumberFormat) f;
        logger.info(" getMinimumIntegerDigits={}", nfat.getMinimumIntegerDigits());
        logger.info(" getMaximumIntegerDigits={}", nfat.getMaximumIntegerDigits());
        logger.info(" getMinimumFractionDigits={}", nfat.getMinimumFractionDigits());
        logger.info(" getMaximumFractionDigits={}", nfat.getMaximumFractionDigits());
      }
      if (f instanceof DecimalFormat) {
        decFormatter = (DecimalFormat) f;
        logger.info(" Pattern = {}", decFormatter.toPattern());
        pattern = decFormatter.toPattern();
        intOnlyValue = decFormatter.isParseIntegerOnly();
      }
    }

    JPanel p2 = new JPanel();
    p2.add(new JLabel("Pattern:"));
    patternTF = new JTextField();
    patternTF.setText(pattern);
    patternTF.setColumns(20);
    patternTF.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        logger.info("pattern actionevent {}", patternTF.getText());
        decFormatter.applyPattern(patternTF.getText());
      }
    });
    p2.add(patternTF);

    intOnly = new JCheckBox("IntegerOnly", intOnlyValue);
    intOnly.addActionListener(e -> {
      boolean val = intOnly.getModel().isSelected();
      logger.info("intOnly actionevent {}", val);
      decFormatter.setParseIntegerOnly(val);
    });

    allowsInvalid = new JCheckBox("AllowsInvalid", allowsInvalidValue);
    allowsInvalid.addActionListener(e -> {
      boolean val = allowsInvalid.getModel().isSelected();
      logger.info("allowsInvalid actionevent {}", val);
      nf.setAllowsInvalid(val);
    });

    JButton gv = new JButton("Get Value");
    gv.addActionListener(e -> {
      boolean val = intOnly.getModel().isSelected();
      logger.info("value= {} {}", d1.getValue(), d1.getValue().getClass().getName());
    });

    main.add(p1);
    main.add(p2);
    main.add(intOnly);
    main.add(allowsInvalid);
    main.add(gv);
    return main;
  }

  @Test
  public void testit() {
    try {
      JFrame frame = new JFrame("TestFormattedTextField");
      frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          System.exit(0);
        }
      });

      TestFormattedTextField ff = new TestFormattedTextField();
      frame.getContentPane().add(ff.makePanel());

      frame.pack();
      frame.setLocation(400, 300);
      frame.setVisible(true);
    } catch (HeadlessException e) {
      // ok to fail if there is no display
    }
  }

}
