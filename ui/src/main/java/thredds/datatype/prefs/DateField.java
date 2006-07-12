// $Id$
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

package thredds.datatype.prefs;

import thredds.datatype.*;

import ucar.util.prefs.ui.PersistenceManager;
import ucar.util.prefs.ui.FldInputVerifier;

import javax.swing.*;
import javax.swing.text.*;

/**
 * Input field for thredds.datatype.DateType, part of ucar.util.prefs.ui.
 *
 * @author John Caron
 * @version $Id$
 * @see ucar.util.prefs.ui.Field
 */

public class DateField extends ucar.util.prefs.ui.Field {

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
      public JToolTip createToolTip() {
        return new thredds.ui.MultilineTooltip();
      }
    };
    tf.setToolTipText("Formats:\n 'present'\n CCYY-MM-DD\n CCYY-MM-DDThh:mm:ss\n udunits date string");
    tf.setInputVerifier(new FldInputVerifier(tf, this));

    if (validValue != null)
      tf.setText(validValue.toString());
        
    finish();
  }

  // return the editing JComponent
  public JComponent getEditComponent() {
    return tf;
  }

  /**
   * See if edit value is valid, put error message in buff.
   */
  protected boolean _validate(StringBuffer buff) {
    try {
      DateType tryit = new DateType(tf.getText(), null, null);
      return true;
    } catch (java.text.ParseException e) {
      if (null != buff) buff.append(name + ": " + e.getMessage());
      return false;
    }
  }

  // get current value from editComponent
  protected Object getEditValue() {
    try {
      DateType tryit = new DateType(tf.getText(), null, null);
      return tryit;
    } catch (java.text.ParseException e) {
      return null;
    }
  }

  // get current value
  public DateType getDate() {
    return (DateType) getValue();
  }

  // set current value of editComponent
  protected void setEditValue(Object value) {
    if (value == null)
      tf.setText("");
    else
      tf.setText(value.toString());
  }

  protected void setStoreValue(Object value) {
    if (storeData != null) {
      if (value == null)
        storeData.putObject(name, value); // LOOK ??
      else
        storeData.putObject(name, new DateType((DateType) value));
    }
  }

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