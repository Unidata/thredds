/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.ui.datatype.prefs;

import ucar.ui.widget.MultilineTooltip;
import ucar.nc2.units.*;

import ucar.util.prefs.PersistenceManager;
import ucar.ui.prefs.FldInputVerifier;

import javax.swing.*;
import javax.swing.text.*;

/**
 * Input field for thredds.datatype.TimeDuration, part of ucar.ui.prefs.
 *
 * @author John Caron
 * @see ucar.ui.prefs.Field
 */

public class DurationField extends ucar.ui.prefs.Field {

  protected JTextComponent tf;

  /** Constructor.
   *  @param name of the field; must be unique within the store
   *  @param label to display to the user
   *  @param defValue default value to start with.
   *  @param storeData store/fetch data from here, may be null.
   */
  public DurationField(String name, String label, TimeDuration defValue, PersistenceManager storeData) {
    super(name, label, storeData);
    validValue = getStoreValue( defValue);
    tf = new JTextField() {
      public JToolTip createToolTip() { return new MultilineTooltip(); }
    };
    tf.setToolTipText("Formats:\n udunits time duration string");
    tf.setInputVerifier( new FldInputVerifier(tf, this));

    if (validValue != null)
      tf.setText(validValue.toString());


    finish();
  }

  // return the editing JComponent
  public JComponent getEditComponent() { return tf; }

  public TimeDuration getTimeDuration() { return (TimeDuration) getValue(); }

 /** See if edit value is valid, put error message in buff. */
  protected boolean _validate( StringBuffer buff) {
    String editValue = tf.getText().trim();
    if (editValue.length() == 0) return true; // empty ok

    try {
      new TimeDuration( tf.getText());
      return true;
    } catch (java.text.ParseException e) {
      buff.append(label).append(": ").append(e.getMessage());
      return false;
    }
  }

  // get current value from editComponent
  protected Object getEditValue() {
    String editValue = tf.getText().trim();
    if (editValue.length() == 0) return null; // empty ok

    try {
      return new TimeDuration( editValue);
    } catch (java.text.ParseException e) {
      return null;
    }
 }

  // set current value of editComponent
  protected void setEditValue(Object value) {
    if (value == null)
      tf.setText("");
    else
      tf.setText(value.toString());
    // tf.repaint();
  }

  protected void setStoreValue(Object value) {
    if (storeData != null)
      storeData.putObject(name, value); // TimeDurations are immutable
  }

  protected Object getStoreValue(Object defValue) {
    Object value = defValue;
    if (storeData != null) {
      Object value2 = storeData.getObject(name);
      if (value2 != null)
        value = value2;
    }
    return value;
  }
}
