// $Id: DurationField.java,v 1.6 2005/08/22 01:12:26 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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
 * Input field for thredds.datatype.TimeDuration, part of ucar.util.prefs.ui.
 *
 * @author John Caron
 * @version $Id: DurationField.java,v 1.6 2005/08/22 01:12:26 caron Exp $
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
      TimeDuration tryit = new TimeDuration( tf.getText());
      return true;
    } catch (java.text.ParseException e) {
      buff.append( label+": "+e.getMessage());
      return false;
    }
  }

  // get current value from editComponent
  protected Object getEditValue() {
    String editValue = tf.getText().trim();
    if (editValue.length() == 0) return null; // empty ok

    try {
      TimeDuration tryit = new TimeDuration( editValue);
      return tryit;
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
