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

package thredds.datatype.prefs;

import ucar.nc2.units.*;

import ucar.util.prefs.ui.PersistenceManager;
import ucar.util.prefs.ui.FldInputVerifier;

import javax.swing.*;
import javax.swing.text.*;

/**
 * Input field for thredds.datatype.TimeDuration, part of ucar.util.prefs.ui.
 *
 * @author John Caron
 * @see ucar.util.prefs.ui.Field
 */

public class DurationField extends ucar.util.prefs.ui.Field {

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
      public JToolTip createToolTip() { return new thredds.ui.MultilineTooltip(); }
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
    if (storeData != null) {
      if (value == null)
        storeData.putObject(name, value); // LOOK null ok ??
      else
        storeData.putObject(name, new TimeDuration((TimeDuration) value));
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
    return new TimeDuration((TimeDuration) value);
  }
}
