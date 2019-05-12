/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.ui.datatype.prefs;

import ucar.ui.widget.MultilineTooltip;
import ucar.nc2.units.DateType;
import ucar.util.prefs.PersistenceManager;
import ucar.ui.prefs.FldInputVerifier;

import javax.swing.*;
import javax.swing.text.JTextComponent;

/**
 * Input field for thredds.datatype.DateType, part of ucar.ui.prefs.
 *
 * @author John Caron
 * @see ucar.ui.prefs.Field
 */

public class DateField extends ucar.ui.prefs.Field {

  protected JTextComponent tf;

  /**
   * Constructor.
   *
   * @param name      of the field; must be unique within the store
   * @param label     to display to the user
   * @param defValue  default value to start with.
   * @param storeData store/fetch data from here, may be null.
   */
  public DateField(String name, String label, DateType defValue, PersistenceManager storeData) {
    super(name, label, storeData);
    validValue = getStoreValue(defValue);
    tf = new JTextField() {
      @Override
      public JToolTip createToolTip() {
        return new MultilineTooltip();
      }
    };
    tf.setToolTipText("Formats:\n 'present'\n CCYY-MM-DD\n CCYY-MM-DDThh:mm:ss\n udunits date string");
    tf.setInputVerifier(new FldInputVerifier(tf, this));

    if (validValue != null)
      tf.setText(validValue.toString());
        
    finish();
  }

  // return the editing JComponent
  @Override
  public JComponent getEditComponent() {
    return tf;
  }

  /**
   * See if edit value is valid, put error message in buff.
   */
  @Override
  protected boolean _validate(StringBuffer buff) {
    try {
      new DateType(tf.getText(), null, null);
      return true;
    } catch (java.text.ParseException e) {
      if (null != buff) buff.append(name).append(": ").append(e.getMessage());
      return false;
    }
  }

  // get current value from editComponent
  @Override
  protected Object getEditValue() {
    try {
      return new DateType(tf.getText(), null, null);
    } catch (java.text.ParseException e) {
      return null;
    }
  }

  // get current value
  public DateType getDate() {
    return (DateType) getValue();
  }

  // set current value of editComponent
  @Override
  protected void setEditValue(Object value) {
    if (value == null)
      tf.setText("");
    else
      tf.setText(value.toString());
  }

  @Override
  protected void setStoreValue(Object value) {
    if (storeData != null) {
      if (value != null)
        storeData.putObject(name, new DateType((DateType) value));
    }
  }

  @Override
  protected Object getStoreValue(Object defValue) {
    Object value = defValue;
    if (storeData != null) {
      Object value2 = storeData.getObject(name);
      if (value2 != null)
        value = value2;
    }
    if (value == null) return null;
    return new DateType((DateType) value);
  }
}
