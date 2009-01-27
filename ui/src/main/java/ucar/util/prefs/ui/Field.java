// $Id: Field.java,v 1.12 2005/10/11 19:36:56 caron Exp $
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

import java.util.prefs.*;
import ucar.util.prefs.PreferencesExt;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.text.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;

/**
 *  Data input fields, with an optional backing store.
 *
 *  <p> Each Field has a name, a JLabel and a JComponent for user editing/input. The name  must be unique within the
 *  PersistenceManager and/or PrefPanel. A Field can optionally have a tooltip, and can be enabled/disabled from
 *  allowing editing.
 *
 * <p> Each Field has a current "valid value" object. The JComponent has an "edit value", which may be invalid.
 *  When the field loses focus, or accept() is called, the value is validated and transferred to the validValue.
 *  If not valid, the user cannot leave the field. The edit value is thus not visible until it has been accepted.
 *  If the new value is different from the previously valid value, a PropertyChangeEvent is thrown, and the canonical
 *  form is written back to the edit value.
 *
 * <p> Each Field optionally has a PersistenceManager, which may contain the "stored value" of the field. If it
 * exists, it is used as the starting value, otherwise, the "default value" is used.  The default value may be null.
 *
 * <p> When accept() is called, validate() is called on the edit value to ensure the field has a valid format.
 *  If so, the value is compared to the previously accepted value, and if different, a PropertyChangeEvent is sent,
 *  and the store is updated.
 *
 * <p> The specialized set/get like setText() or setDouble() set both the edit value and the store value. The
 *  setters throw an event if the new value is different from previous.
 *
 * <p> When the PersistenceManager sends a PreferenceChangeEvent, the field gets the new value and
 *  displays it. If different from previous value, a PropertyChangeEvent is sent.
 *  Thus the PersistenceManager object is the "model" and the Field is the "view", and they
 *  automatically stay in synch.
 *
 * <p> A PreferencesExt object is optionally used to save state about the Field UI.
 *
 * @see PrefPanel
 * @author John Caron
 * @version $Id: Field.java,v 1.12 2005/10/11 19:36:56 caron Exp $
 */

public abstract class Field {
  protected String name;
  protected PersistenceManager storeData;
  protected javax.swing.event.EventListenerList listenerList = null;

  protected String label;
  protected Object validValue;      // the current valid value
  protected Object previousValue;   // the previous valid value, use in propertyChangeEvent
                                    // also getEditValue() : value in the editComponent()
                                    // also getStoreValue() : value in the preference store

  protected boolean showFormat = true;

  protected static final boolean debugPersistence = false;

  /** Constructor
   *  @param name of the field; must be unique within the store
   *  @param label to display to the user
   *  @param storeData store/fetch data from here, may be null.
   */
  protected Field(String name, String label, PersistenceManager storeData) {
    this.name = name;
    this.label = label;
    this.storeData = storeData;

    // listen for changes to this value
    if (storeData != null) {
      storeData.addPreferenceChangeListener(new PreferenceChangeListener () {
        public void preferenceChange(PreferenceChangeEvent evt) {
          if (evt.getKey().equals(getName())) {
            //System.out.println("Field: node listener on "+ evt.getNode().name()+" key = <"+evt.getKey()+"> val= <"+evt.getNewValue()+">");
            // the value in the store has change: update the edit component
            // send event if its different from previous
            setNewValueFromStore();
          }
        }
      });
    }
  }

  protected void finish() {
    addStandardPopups();
    /* getDeepEditComponent().addPropertyChangeListener(new PropertyChangeListener () {
        public void propertyChange(PropertyChangeEvent evt) {
          System.out.println(name+" got propertyChange "+evt.getPropertyName());
        }
    }); */
  }

  //// public methods

  /** Return the name of the field */
  public String getName() { return name; }
  /** Return the JLabel component */
  public String getLabel() { return label; }
  /** Return the PersistenceManager component */
  public PersistenceManager getPersistenceManager() { return storeData; }
  /* Set the PersistenceManager component
  public void setPersistenceManager(PersistenceManager storeData) {
    this.storeData = storeData;
    Object value = getStoreValue( null);
    if (value != null) {
      if (debugPersistence) System.out.println(name+" setEditValue "+value+" ("+value.getClass().getName()+")");
      setEditValue( value);     LOOK need addPreferenceChangeListener
    }
  }   */

  /** Return whether the field is enabled */
  public boolean isEnabled( ) { return getEditComponent().isEnabled(); }
  /** Set whether the field is enabled */
  public void setEnabled( boolean enable) { getEditComponent().setEnabled( enable); }

  /** Return whether the field is editable, default == enabled */
  public boolean isEditable() { return isEnabled(); };
  /** Set whether the field is editable, default == enabled */
  public void setEditable(boolean editable) { setEnabled( editable); };

  /** Set the tooltip */
  public void setToolTipText( String tip) { getEditComponent().setToolTipText( tip); }
  /** Get the tooltip */
  public String getToolTipText() { return getEditComponent().getToolTipText(); }

  /** Register for PropertyChange events when the value of the Field changes.
   *  When accept() is called,
   *  you will get a new PropertyChangeEvent(this, fldName, oldValue, newValue), where
   *  the oldValue, newValue will be String, Integer, Boolean, etc.
   */
  public void addPropertyChangeListener( PropertyChangeListener pcl) {
    if (listenerList == null)
      listenerList = new javax.swing.event.EventListenerList();
    listenerList.add(PropertyChangeListener.class, pcl);
  }
  /** Deregister for when the value changes */
  public void removePropertyChangeListener(PropertyChangeListener pcl) {
    listenerList.remove(PropertyChangeListener.class, pcl);
  }

  //// subclasses must implement these
  //// Note: object types of store and edit must match, subclass does translation to component class
  ////   if needed.

  /** Return the editing JComponent */
  abstract public JComponent getEditComponent();

  /** See if edit value is valid, put error message in buff. */
  abstract protected boolean _validate( StringBuffer buff);

   /** Get current value from editComponent */
  abstract protected Object getEditValue();

  /** Set current value of editComponent */
  abstract protected void setEditValue(Object value);

  /** Get value from Store. Must be immutable or return a copy */
  abstract protected Object getStoreValue( Object defValue);

  /** Put new value into Store. Must be immutable or make a copy */
  abstract protected void setStoreValue(Object newValue);

  //// these are utility routines that should work for subclasses,
  //// but may need to be overridden

  public JComponent getDeepEditComponent() { return getEditComponent(); }

  /** Get valid value as String, Double, Integer, etc. may be null if default value was null.
   * You may want to call accept() first to send to Store.
   */
  public Object getValue() { return validValue; }

  /** Set the current valid and edit value. No events are thrown. */
  public void setValue(Object newValue) {
    previousValue = getValue();
    validValue = newValue;
    setEditValue(newValue);
  }

  private ArrayList validators = new ArrayList();  // list of FieldValidator

  /**
   * Add a validator to this field.
   * @param v an implementation of FieldValidator.
   */
  public void addValidator( FieldValidator v)  { validators.add(v); }

  /** See if edit value is valid, put error message in buff. */
  protected boolean validate( StringBuffer buff) {
    if (!_validate(buff)) return false;
    Object editValue = getEditValue();
    Iterator iter = validators.iterator();
    while (iter.hasNext()) {
      FieldValidator v = (FieldValidator) iter.next();
      if (!v.validate(this, editValue, buff)) return false;
    }

    if (acceptIfDifferent( editValue)) {
      setEditValue( validValue);
      sendEvent();
    }
    return true;
  }

  /** Get current value from editComponent, save to store.
   *  If different from old value, fire PropertyChangeEvent.
   *  Return false if invalid format, add error message to buff if not null.
   */
   protected boolean accept(StringBuffer buff){
     if (!validate(buff)) return false;
     if (acceptIfDifferent( getEditValue())) {
      setStoreValue( validValue);
      sendEvent();
     }
     return true;
   }

  /** See if this value is different from current accepted value (using equals());
    If so, set old value to accepted value, then accepted value to this value.
    @return true if its different.
   */
  protected boolean acceptIfDifferent(Object newValue) {
    // System.out.println("isDifferent "+newValue+" "+value);
    if ((newValue == null) && (validValue == null)) return false;
    if ((validValue != null) && validValue.equals( newValue)) return false;
    previousValue = getValue();
    validValue = newValue;
    return true;
  }

  /* See if this value is different from current accepted value (using equals());
    If so, set old value to accepted value, then accepted value to this value.
    @return true if its different.
   *
  protected boolean checkIfValid(StringBuffer buff) {
    if (buff == null) buff = new StringBuffer();
    if (!validate( buff)) {
      try { JOptionPane.showMessageDialog(PrefPanel.findActiveFrame(), buff.toString()); }
      catch (HeadlessException e) { }
      return false;
    }
    return true;
  }     */

    /* Get value from store, put value into editComponent */
  protected void restoreValue( Object defValue) {
    if (storeData != null) {
      validValue = getStoreValue( defValue);
      setEditValue( validValue);
    }
  }

  /** The value in the store has changed: update the edit component
   *  send event if its different from previous
   */
  protected void setNewValueFromStore() {
    Object newValue = getStoreValue(validValue);
    if (acceptIfDifferent(newValue)) {
      setEditValue(newValue);
      sendEvent();
    }
  }

  // send PropertyChangeEvent
  protected void sendEvent() {
    if (listenerList != null) {
      PropertyChangeEvent event = new PropertyChangeEvent(this, name, previousValue, getValue());
      Object[] listeners = listenerList.getListenerList();
      for (int i=listeners.length-2; i>=0; i-=2)
        ((PropertyChangeListener)listeners[i+1]).propertyChange(event);
    }
  }

  // send focus to next component
  protected void next() {
    java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent( getEditComponent());
  }

  protected void addStandardPopups() {
    addPopupMenuAction( "restore", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Field fld = (Field) e.getSource();
        fld.setEditValue( validValue);
      }
    });
  }

  protected JPopupMenu popupMenu;
  public void addPopupMenuAction( String name, AbstractAction action) {
    if (popupMenu == null) {
      popupMenu = new JPopupMenu();
      getDeepEditComponent().addMouseListener( new PopupTriggerListener() {
        public void showPopup(java.awt.event.MouseEvent e) {
          popupMenu.show(getEditComponent(), e.getX(), e.getY());
        }
      });
    }
    popupMenu.add( new ActionWrapper( name, action));
  }

  private class ActionWrapper extends AbstractAction {
    private AbstractAction orgAct;
    ActionWrapper( String name, AbstractAction act) {
      this.orgAct = act;
      putValue( Action.NAME, name);
    }
    public void actionPerformed(ActionEvent e) {
      ActionEvent me = new ActionEvent(Field.this, e.getID(), e.getActionCommand(), e.getWhen(), e.getModifiers());
      orgAct.actionPerformed(me);
    }
  }

  private static abstract class PopupTriggerListener extends MouseAdapter {
    public void mouseReleased (MouseEvent e) {
      if(e.isPopupTrigger())
        showPopup(e);
    }
    public abstract void showPopup(MouseEvent e);
  }

  /////////////////////////////////////////////////////////////////////////////////////////
  // subclasses

  /**
   * String input field.
   */
  static public class Text extends Field {
    protected JTextComponent tf;

    /** Constructor for subclasses.
     *  @param name of the field; must be unique within the store
     *  @param label to display to the user
     *  @param storeData store/fetch data from here, may be null.
     */
    protected Text(String name, String label, PersistenceManager storeData) {
      super(name, label, storeData);
    }

    /** Constructor.
     *  @param name of the field; must be unique within the store
     *  @param label to display to the user
     *  @param defValue defau;lt value to start with.
     *  @param storeData store/fetch data from here, may be null.
     *  @see PrefPanel#addTextField
     */
    public Text(String name, String label, String defValue, PersistenceManager storeData) {
      this(name, label, storeData);
      validValue = getStoreValue( defValue);
      tf = new JTextField( (String) validValue);
      //tf.getDocument().addDocumentListener( new DocumentListener() {

      //});
      finish();
    }

    // return the editing JComponent
    public JComponent getEditComponent() { return tf; }

    /** Can the user edit this field. */
    public boolean isEditable() { return tf.isEditable(); }
    public void setEditable( boolean isEditable) { tf.setEditable( isEditable); }

   /** See if edit value is valid, put error message in buff. */
    protected boolean _validate( StringBuffer buff) { return true; }

    // get current value from editComponent
    protected Object getEditValue() {
      String editValue = tf.getText().trim();
      if (editValue.length() == 0) return null; // empty ok
      return normalize(editValue);
    }

    // set current value of editComponent
    protected void setEditValue(Object value) {
      if (value == null)
        tf.setText("");
      else
        tf.setText((String) value);
    }

    // set a new value into the Store
    protected void setStoreValue( Object value) {
      if (storeData != null)
        storeData.put(name, (String) value);
    }

    /** Get value from Store, if it exists, else return default value.
     * @param defValue default value
     * @return stored value, or defValue if no store or value not in store
     */
    protected Object getStoreValue( Object defValue) {
      if (storeData != null)
        return storeData.get(name, (String) defValue);
      else
        return defValue;
    }

    /** Return the current accepted value */
    public String getText() { return normalize( (String) getValue()); }

    /** Set value of text; if different from current value, store in PersistenceManager and
     *  send event. */
    public void setText(String newValue) {
      setValue(newValue);
      //if (newValue == null) newValue="";
      //setEditValue(normalize(newValue));
      //accept(null);
    }

    protected String normalize(String s) {
      if (s == null) return null;
      String trimValue = s.trim();
      return (trimValue.length() > 0) ? trimValue : s;
    }

  }

  /////////////////////////////////////////////////////////////////////////////////////////
  /**
   * String input field using a TextArea.
   */
  static public class TextArea extends Text {

    /** Constructor.
     *  @param name of the field; must be unique within the store
     *  @param label to display to the user
     *  @param defValue defau;lt value to start with.
     *  @param storeData store/fetch data from here, may be null.
     *  @see PrefPanel#addTextField
     */
    public TextArea(String name, String label, String defValue, int nrows, PersistenceManager storeData) {
      super(name, label, storeData);
      validValue = getStoreValue( defValue); // immutable
      JTextArea ta = new JTextArea( (String) validValue);

      ta.setLineWrap( true);
      ta.setWrapStyleWord(true);
      ta.setRows( nrows);

      tf = ta;
      finish();
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////
  /**
   * A text input field which doesnt echo the input, for passwords.
   */
  static public class Password extends Text {

    /** Constructor.
     *  @param name of the field; must be unique within the store
     *  @param label to display to the user
     *  @param defValue defau;lt value to start with.
     *  @param storeData store/fetch data from here, may be null.
     *  @see PrefPanel#addPasswordField
     */
    public Password(String name, String label, String defValue, PersistenceManager storeData) {
      super(name, label, storeData);
      validValue = this.getStoreValue( defValue);
      tf = new JPasswordField( (String) validValue);
      finish();
    }

    /** Return the current value as char array */
    public char[] getPassword() { return ((JPasswordField)tf).getPassword(); }
  }

  /////////////////////////////////////////////////////////////////////////////////////////
  /** Data input for double.
   *  Stored object is a String, exactly as user input.
   *  Use get/set Double to deal as a double.
   */
  static public class Double extends Field {
    private JTextField tf;
    private int nfracDig = 3;

    /** Constructor.
     *  @param name of the field; must be unique within the store
     *  @param label to display to the user
     *  @param defValue default value to start with.
     *  @param nfracDig number of fractional digits to display
     *  @param storeData store/fetch data from here, may be null.
     *  @see PrefPanel#addDoubleField
     */
    public Double(String name, String label, double defValue, int nfracDig, PersistenceManager storeData) {
      super(name, label, storeData);
      if (nfracDig >= 0)
        this.nfracDig = nfracDig;

      validValue = getStoreValue( new java.lang.Double(defValue));

      tf = new JTextField();
      setEditValue( validValue);
      tf.setInputVerifier( new FldInputVerifier(tf, this));
      tf.setHorizontalAlignment(JTextField.RIGHT);

      finish();
    }

    /** See if edit value is valid, put error message in buff. */
    protected boolean _validate( StringBuffer buff) {
      String editValue = tf.getText().trim();
      if (editValue.length() == 0) return true; // empty ok
      try {
        new java.lang.Double(editValue);
      } catch (NumberFormatException e) {
        if (buff != null) buff.append(label).append(" has invalid format: must be floating point number\n");
        return false;
      }
      return true;
    }

    // return the editing JComponent
    public JComponent getEditComponent() { return tf; }

    // get current value from editComponent
    protected Object getEditValue() {
      String editValue = tf.getText().trim();
      if (editValue.length() == 0) return null; // empty ok
      try {
        return new java.lang.Double( editValue);
      } catch (NumberFormatException e) {
        return null;
      }
    }

    // set current value of editComponent
    protected void setEditValue(Object value) {
      if (value == null)
        tf.setText("");
      else {
        double dv = ((java.lang.Double) value).doubleValue();
        tf.setText( dfrac(dv, nfracDig));
      }
    }

    // set a new value into the Store
    protected void setStoreValue( Object value) {
      if (storeData != null)
        storeData.putDouble(name, ((java.lang.Double) value).doubleValue());
    }

    /* get value from store, make copy */
    protected Object getStoreValue( Object defValue) {
      if (storeData != null) {
        double def = java.lang.Double.NaN;
        double value = storeData.getDouble(name, def);
        if (!java.lang.Double.isNaN(value))
          return new java.lang.Double( value);
      }
      return new java.lang.Double(((java.lang.Double) defValue).doubleValue());
    }

    /** Return the current value */
    public double getDouble() {
      return ((java.lang.Double) getValue()).doubleValue();
    }

    public void setDouble(double value) {
      setValue( new java.lang.Double(value));
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////
  /** Data input for double.
   *  Stored object is a String, exactly as user input.
   *  Use get/set Double to deal as a double.
   */
  static public class Int extends Field {
    private JTextField tf;

    /** Constructor.
     *  @param name of the field; must be unique within the store
     *  @param label to display to the user
     *  @param defValue default value to start with.
     *  @param storeData store/fetch data from here, may be null.
     *  @see PrefPanel#addDoubleField
     */
    public Int(String name, String label, int defValue, PersistenceManager storeData) {
      super(name, label, storeData);

      validValue = getStoreValue( new java.lang.Integer(defValue)); // returns copy

      tf = new JTextField();
      setEditValue( validValue);
      tf.setInputVerifier( new FldInputVerifier(tf, this));
      tf.setHorizontalAlignment(JTextField.RIGHT);

      finish();
    }

    /** See if edit value is valid, put error message in buff. */
    protected boolean _validate( StringBuffer buff) {
      String editValue = tf.getText().trim();
      if (editValue.length() == 0) return true; // empty ok
      try {
        java.lang.Integer.parseInt(tf.getText());
      } catch (NumberFormatException e) {
        buff.append(label).append(" has invalid format: must be an integer number\n");
        return false;
      }
      return true;
    }

    // return the editing JComponent
    public JComponent getEditComponent() { return tf; }

    // get current value from editComponent
    protected Object getEditValue() {
      String editValue = tf.getText().trim();
      if (editValue.length() == 0) return null; // empty ok
      try {
        return new java.lang.Integer( tf.getText());
      } catch (NumberFormatException e) {
        return null;
      }
    }

    // set current value of editComponent
    protected void setEditValue(Object value) {
      if (value == null)
        tf.setText( "");
      else
        tf.setText( value.toString());
    }

    // set a new value into the Store
    protected void setStoreValue( Object value) {
      if (storeData != null)
        storeData.putInt(name, ((java.lang.Integer) value).intValue());
    }

    /* get value from store, put value into editComponent */
    protected Object getStoreValue( Object defValue) {
      if (storeData != null) {
        int def = java.lang.Integer.MAX_VALUE;
        int value = storeData.getInt(name, def);
        if (value != def) return new java.lang.Integer( value);
      }
      return new java.lang.Integer(((java.lang.Integer) defValue).intValue());
    }

    /** Return the current value */
    public int getInt() {
      return ((java.lang.Integer) getValue()).intValue();
    }

    public void setInt(int value) {
      setValue( new java.lang.Integer(value));
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////
  /** Data input for Date */
  static public class Date extends Field {
    protected JFormattedTextField tf;

    /** Constructor.
     *  @param name of the field; must be unique within the store
     *  @param label to display to the user
     *  @param defValue default value to start with.
     *  @param storeData store/fetch data from here, may be null.
     *  @see PrefPanel#addDateField
     */
    public Date(String name, String label, java.util.Date defValue, PersistenceManager storeData) {
      super(name, label, storeData);

      tf = new JFormattedTextField();
      validValue = getStoreValue( defValue);
      if (null == validValue) validValue = new java.util.Date();
      tf.setValue( validValue);
      tf.setInputVerifier( new FldInputVerifier(tf, this));

      try {
        JFormattedTextField.AbstractFormatter format = tf.getFormatter();
        setToolTipText( "eg "+format.valueToString(new java.util.Date(234098876)));
      } catch (java.text.ParseException e) {}

      finish();
    }

    /** return the editing JComponent */
    public JComponent getEditComponent() { return tf; }

    /** See if edit value is valid, put error message in buff. */
    protected boolean _validate( StringBuffer buff) {
      try {
        tf.commitEdit();
      } catch (java.text.ParseException e) {
        buff.append(label).append(" has invalid format: should be a date\n");
        return false;
      }
      return true;
    }

    // get current value from editComponent
    protected Object getEditValue() {
      String editValue = tf.getText().trim();
      if (editValue.length() == 0) return null; // empty ok
      return tf.getValue();
    }

    // set current value of editComponent - must be a Date
    protected void setEditValue(Object value) {
      if (value == null)
        tf.setValue("");
      else
        tf.setValue(value);
    }

    /** Can user edit. Default is true. */
    public boolean isEditable() { return tf.isEditable(); }
    public void setEditable( boolean isEditable) { tf.setEditable( isEditable); }


    /** Set value; if different from current value, store in PersistenceManager and
     *  send event.
    public void setValue(Object newValue) {
      if (acceptIfDifferent(newValue)) {
        setEditValue(newValue);
        setStoreValue(newValue);
        sendEvent();
      }
    } */

      // set a new value into the Store
    protected void setStoreValue( Object value) {
      if (storeData != null) {
        java.util.Date valueAsDate = (java.util.Date) value;
        storeData.putObject(name, new java.util.Date(valueAsDate.getTime()));
      }
    }

    // get value from store
    protected Object getStoreValue( Object defValue) {
      Object value = defValue;
      if (storeData != null) {
        Object value2 = storeData.getObject(name);
        if (value2 != null)
          value =  value2;
      }
      if ((value == null) || !(value instanceof Date)) return null;
      java.util.Date valueAsDate = (java.util.Date) value;
      return new java.util.Date(valueAsDate.getTime());
    }

    /** Return the current value */
    public java.util.Date getDate() { return (java.util.Date) getValue(); }

    /** Set value; if different from current value, store in PersistenceManager and
     *  send event. */
    public void setDate(java.util.Date newValue) {
      setValue( newValue);
    }
  }

  /**
   * General class for formatted input field using JFormattedTextField (jdk 1.4).
   * NOTE: to use this directly, you must use a PersistenceManagerExt object.
   * @see JFormattedTextField
   *
  static public class TextFormatted extends Field {
    protected JFormattedTextField tf;
    //protected JFormattedTextField.AbstractFormatter format;

    /** Constructor.
     *  @param name of the field; must be unique within the store
     *  @param label to display to the user
     *  @param defValue default value to start with; type is used by JFormattedTextField
     *    to determine how to format
     *  @param storeData store/fetch data from here, may be null.
     *
    public TextFormatted(String name, String label, JFormattedTextField tf, Object defValue, PersistenceManager storeData) {
      super(name, label, storeData);
      setFormattedTextField( tf, defValue);
    }

    public TextFormatted(String name, String label, PersistenceManager storeData) {
      super(name, label, storeData);
    }

    /**
     * Set the JFormattedTextField used by this Field. Really part of the constructor.
     * @param tf JFormattedTextField or null if use default for type of defValue.
     * @param defValue
     *
    public void setFormattedTextField(JFormattedTextField tf, Object defValue) {

      if (tf != null)
        this.tf = new JFormattedTextField();
      else
        this.tf = tf;

      acceptedValue = getStoreValue( defValue);
      this.tf.setValue( acceptedValue);

      this.tf.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          // System.out.println("Field.TextFormatted: got Action event on "+getName());
          accept();
          next();
        }
      });
    }

    /** return the editing JComponent
    public JComponent getEditComponent() { return tf; }

    // get current value from editComponent
    protected Object getEditValue() {
      try {
        tf.commitEdit();
        return tf.getValue();
      } catch (java.text.ParseException e) {
        java.awt.Toolkit.getDefaultToolkit().beep();
        System.out.println("Field.TextFormatted: invalid format "+ getName()+" = "+tf.getText());
        return null;
      }
    }

    // set current value of editComponent
    protected void setEditValue(Object value) { tf.setValue(value); }

    /** Can user edit. Default is true.
    public boolean isEditable() { return tf.isEditable(); }
    public void setEditable( boolean isEditable) { tf.setEditable( isEditable); }

    // set a new value into the Store
    protected void setStoreValue( Object value) {
      if (storeData != null)
        ((PersistenceManagerExt)storeData).putBeanObject(name, value);
    }

    /* get value from store
    protected Object getStoreValue( Object defValue) {
      if (storeData != null)
        return ((PersistenceManagerExt)storeData).getBean(name, defValue);
      else
        return defValue;
    }

    /** Return the current value LOOK
    public Object getValue() { return acceptedValue; } */

    /** Set value; if different from current value, store in PersistenceManager and
     *  send event.
    public void setValue(Object newValue) {
      if (acceptIfDifferent(newValue)) {
        setEditValue(newValue);
        setStoreValue(newValue);
        sendEvent();
      }
    }
  } */

  /** Data input for int
  static public class Int extends TextFormatted {

    /** Constructor.
     *  @param name of the field; must be unique within the store
     *  @param label to display to the user
     *  @param defValue default value to start with.
     *  @param storeData store/fetch data from here, may be null.
     *  @see PrefPanel#addIntField
     *
    public Int(String name, String label, int defValue, PersistenceManager storeData) {
      super(name, label, storeData);

      NumberFormat format = NumberFormat.getIntegerInstance();
      JFormattedTextField.AbstractFormatterFactory formatter =
          new DefaultFormatterFactory(new NumberFormatter(format));
      JFormattedTextField tf = new JFormattedTextField(formatter);

      setFormattedTextField(tf, new Integer(defValue));
      if (showFormat) showFormatInfo( tf);
    }

    // set a new value into the Store
    protected void setStoreValue( Object value) {
      if (storeData != null)
        storeData.putInt(name, ((Number) value).intValue());
    }

    /* get value from store, put value into editComponent
    protected Object getStoreValue( Object defValue) {
      if (storeData == null) return defValue;
      int ival = (defValue == null) ? 0 : ((Number) defValue).intValue();
      ival = storeData.getInt(name, ival);
      return new Integer( ival);
    }

    /** Return the current value
    public int getInt() { return ((Number) acceptedValue).intValue(); }

    /** Set value; if different from current value, store in PersistenceManager and
     *  send event.
    public void setInt(int newValue) {
      super.setValue( new Integer(newValue));
    }
  } */

  /* static public class Double extends TextFormatted {
    private DecimalFormat decf;

    /** Constructor.
     *  @param name of the field; must be unique within the store
     *  @param label to display to the user
     *  @param defValue default value to start with.
     *  @param storeData store/fetch data from here, may be null.
     *  @see PrefPanel#addDoubleField
     *
    public Double(String name, String label, double defValue, PersistenceManager storeData) {
      super(name, label, storeData);

      NumberFormat format = NumberFormat.getNumberInstance();
      format.setMinimumFractionDigits(2);
      format.setMaximumFractionDigits(4);
      JFormattedTextField.AbstractFormatterFactory formatter =
          new DefaultFormatterFactory(new NumberFormatter(format));
      setFormattedTextField(new JFormattedTextField(formatter), new java.lang.Double(defValue));

      if (showFormat) showFormatInfo( tf);

      //DecimalFormat decf = new DecimalFormat();
      //decf.setMaximumFractionDigits( 5);
      //tf.setFormatterFactory(new DoubleFormatterFactory(decf));

      /* System.out.println("getMaximumIntegerDigits="+decf.getMaximumIntegerDigits());
      System.out.println("getMinimumIntegerDigits="+decf.getMinimumIntegerDigits());
      System.out.println("getMaximumFractionDigits="+decf.getMaximumFractionDigits());
      System.out.println("getMinimumFractionDigits="+decf.getMinimumFractionDigits());
    }

    /**
     * Set max fractional digits to display
     * @param maximumFractionDigits
     * @see java.text.DecimalFormat
     *
    public void setMaximumFractionDigits( int maximumFractionDigits) {
      decf.setMaximumFractionDigits(maximumFractionDigits);
    }

        // set a new value into the Store
    protected void setStoreValue( Object value) {
      if (storeData != null)
        storeData.putDouble(name, ((Number) value).doubleValue());
    }

    /* get value from store, put value into editComponent
    protected Object getStoreValue( Object defValue) {
      if (storeData == null) return defValue;
      double dval = (defValue == null) ? 0.0 : ((Number) defValue).doubleValue();
      dval = storeData.getDouble(name, dval);
      return new java.lang.Double( dval);
    }

    /** Return the current value
    public double getDouble() { return ((Number) acceptedValue).doubleValue(); }

    /** Set value; if different from current value, store in PersistenceManager and
     *  send event.
    public void setDouble(double newValue) {
      super.setValue( new java.lang.Double(newValue));
    }

    // a lot of bloody trouble just to cover DecimalFormat!!
    private class DoubleFormatter extends JFormattedTextField.AbstractFormatter {
      private DecimalFormat dformat;
      DoubleFormatter(DecimalFormat dformat) { this.dformat = dformat; }
      public String valueToString(Object value) { return "    " + dformat.format(value); }
      public Object stringToValue(String text) throws java.text.ParseException {
        return dformat.parseObject(text);
      }
    }
    private class DoubleFormatterFactory extends JFormattedTextField.AbstractFormatterFactory {
      private DoubleFormatter dformat;
      DoubleFormatterFactory(DecimalFormat decf) { dformat = new DoubleFormatter(decf); }
      public JFormattedTextField.AbstractFormatter getFormatter(JFormattedTextField tf) {
        return dformat;
      }
    }
  } */


  /////////////////////////////////////////////////////////////////////////////////////////
  /**
   * A boolean input box using a checkbox.
   */
  static public class CheckBox extends Field {
    private JCheckBox checkbox;

    /** Constructor.
     *  @param fldName name of the field; must be unique within the store
     *  @param label label to display to the user
     *  @param defValue default value to start with.
     *  @param storeData store/fetch data from here, may be null.
     *  @see PrefPanel#addCheckBoxField
     */
    public CheckBox( String fldName, String label, boolean defValue, PersistenceManager storeData ) {
      super( fldName, label, storeData);

      validValue = getStoreValue( new Boolean(defValue));
      checkbox = new JCheckBox();
      checkbox.setSelected( isSelected());
      finish();
    }

   /** See if edit value is valid, put error message in buff. */
    protected boolean _validate( StringBuffer buff) { return true; }

    /** return the editing JComponent */
    public JComponent getEditComponent() { return checkbox; }

    // get current value from editComponent
    protected Object getEditValue() {
      return new Boolean(checkbox.isSelected());
    }

    // set current value of editComponent
    protected void setEditValue(Object value) {
      boolean bv = (value == null) ? false : ((Boolean) value).booleanValue();
      checkbox.setSelected(bv);
    }

    // set a new value into the Store
    // return true if different from old value
    protected void setStoreValue( Object value) {
      if (storeData != null)
        storeData.putBoolean(name, ((Boolean) value).booleanValue());
    }

    /* get value from store, put value into editComponent */
    protected Object getStoreValue( Object defValue) {
      if (storeData == null)
        return defValue;
      boolean def = (defValue == null) ? false : ((Boolean) defValue).booleanValue();
      boolean bv = storeData.getBoolean(name, def);
      return new Boolean( bv);
    }

    /** Return the current value */
    public boolean isSelected() { return ((Boolean) validValue).booleanValue(); }

    /** Set value; if different from current value, store in PersistenceManager and
     *  send event. */
    public void setSelected(boolean v) {
      setValue(new java.lang.Boolean(v));
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////
  /**
   * A text input field which keeps track of recent choices in a combobox. The order they appear
   * in the combobox choices reflects how recently they were chosen.
   * NOTE: to use this, you must use a PreferencesExt.
   * <p>
   * The actual stored object type is an ArrayList of Strings.
   * The current choice is the first one in the list, use getText() to obtain it.
   *
   * @see ComboBox
   */
  static public class TextCombo extends Field {
    protected ComboBox combo;
    private boolean eventsOK = true;
    private boolean debugCombo = false;

    /** Constructor.
     *  @param fldName name of the field, must be unique within the store.
     *  @param label to display to the user
     *  @param defValues list of default values to include in the comboBox. May be null.
     *    These are added to the combobox (at the end) no matter how many there are.
     *  @param n  number of most recently used values to keep
     *  @param storeData store/fetch data from here, may be null.
     *  @see PrefPanel#addTextComboField
     */
    public TextCombo(String fldName, String label, java.util.Collection defValues, int n, PersistenceManager storeData) {
      super( fldName, label, storeData);

      combo = new ComboBox(storeData, n); //  == null ? null : (PersistenceManager) storeData.node(name+"_ComboBox"));
      java.util.ArrayList prevChoices = combo.getItemList();

      // add defaults : only added if not already present
      if (defValues != null) {
        Iterator iter = defValues.iterator();
        while (iter.hasNext()) {
          Object o = iter.next();
          if (!prevChoices.contains(o))
            prevChoices.add( o);
        }
        combo.setItemList(prevChoices);
      }

      // first one is the current choice
      if (combo.getItemCount() > 0) {
        combo.setSelectedIndex(0);
        validValue = combo.getItemAt(0);
      }

      finish();
    }

   /** See if edit value is valid, put error message in buff. */
    protected boolean _validate( StringBuffer buff) { return true; }

        /** return the editing JComponent */
    public JComponent getEditComponent() { return combo; }

    /** Get current value from editComponent, will be an ArrayList */
    protected Object getEditValue() { return combo.getItemList(); }
    public JComponent getDeepEditComponent() { return combo.getDeepEditComponent(); }

    /** set value of editComponent, must be a List */
    protected void setEditValue(Object value) {
      if (value == null) return;
      eventsOK = false;
      combo.setItemList( (java.util.List) value);
      eventsOK = true;
    }

    /** Get value from Store, will be an ArrayList or null */
    protected Object getStoreValue(Object defValue) {
      return combo.getStoreValue(defValue);
    }

   /** Put new value into Store, must be a Collection of Strings */
    protected void setStoreValue(Object newValue) {
      combo.setStoreValue( (java.util.List) newValue);
    }

    /* get current value from editComponent, save to store.
     *  if different from old value, fire PropertyChangeEvent
    protected boolean accept(){
      eventsOK = false;
      Object newValue = combo.getSelectedItem();
      combo.addItem( newValue); // put on top
      if (acceptIfDifferent( getEditValue())) {
        setEditValue( validValue); // reorder
        setStoreValue( validValue);
        sendEvent();
      }
      eventsOK = true;
      return true;
    }  */

    /** Return the current selected value as a String */
    public String getText() {
      Object current = combo.getSelectedItem();
      return current.toString();
    }

    /** Set current selected value of text; send event. */
    public void setText(String newValue) {
      newValue = newValue.trim();
      combo.addItem( newValue);
      accept( null);
    }

    /** Set edit value as an Object. */
    public void setValue(Object value) {
      combo.addItem( value);
      accept( null);
    }

    /** can user edit? */
    public boolean isEditable() { return combo.isEditable(); }
    public void setEditable( boolean isEditable) { combo.setEditable( isEditable); }
  }

  /////////////////////////////////////////////////////////////////////////////////////////
  /**
   * This allows user to make a choice from a collection of "type-safe enumeration" objects.
   * <p>
   * The actual stored object type is an object that should be equal to one of the choices.
   */
  static public class EnumCombo extends Field {
    protected ComboBox combo;

    /** Constructor.
     *  @param fldName name of the field, must be unique within the store.
     *  @param label to display to the user
     *  @param choices list of enumerations.
     *  @param storeData store/fetch data from here, may be null.
     *  @see PrefPanel#addTextComboField
     */
    public EnumCombo(String fldName, String label, java.util.Collection choices, PersistenceManager storeData) {
      super( fldName, label, storeData);

      combo = new ComboBox(null, 0);
      combo.setItemList( choices);
      setEditValue( getStoreValue( null));
      finish();

      combo.addActionListener( new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          validate(null); // transfer to valid, send event
        }
      });
    }

   /** See if edit value is valid, put error message in buff. */
    protected boolean _validate( StringBuffer buff) { return true; }

        /** return the editing JComponent */
    public JComponent getEditComponent() { return combo; }
    public JComponent getDeepEditComponent() { return combo.getDeepEditComponent(); }

    /** Get current value from editComponent */
    protected Object getEditValue() {
      Object item = combo.getSelectedItem();
      if (item.equals("")) return null;
      return item;
    }

    /** set value of editComponent */
    protected void setEditValue(Object value) {
      if (value == null)
        combo.setSelectedItem( "");
      else
        combo.setSelectedItem( value);
    }

    /** Get value from Store, will be an item to be placed in the list */
    protected Object getStoreValue(Object defValue) {
      Object val = null;
      if (storeData != null)
        val = storeData.getObject(name);
      return (val == null) ? defValue : val;
    }

   /** Put new value into Store, will be an item from the list  */
    protected void setStoreValue(Object newValue) {
      if (storeData != null)
        storeData.putObject( name, newValue);
    }

    /* Set current selected value as an Object.
    public void setValue(Object value) {
      if (value == null)
        combo.addItem( "");
      else
        combo.addItem( value);
      accept(null);
    } */

    /** can user edit? */
    public boolean isEditable() { return combo.isEditable(); }
    public void setEditable( boolean isEditable) { combo.setEditable( isEditable); }
  }

  /////////////////////////////////////////////////////////////////////////////////////////
  /**
   * This uses a BeanTableSorted to display a list of beans.
   * <p>
   * The actual stored object type is an ArrayList of Objects.
   */
  static public class BeanTable extends Field {
    protected BeanTableSorted table;

    /** Constructor.
     *  @param fldName name of the field, must be unique within the store.
     *  @param label to display to the user
     *  @param defBeans list of default beans to ues. May be null.
     *  @param beanClass class type of the beans
     *  @param storeData store/fetch data from here, may be null.
     *  @see PrefPanel#addTextComboField
     */
    public BeanTable(String fldName, String label, java.util.ArrayList defBeans, Class beanClass,
                     PreferencesExt prefs, PersistenceManager storeData) {
      super( fldName, label, storeData);
      table = new BeanTableSorted(beanClass, prefs, true);
      if (storeData != null)
        setEditValue( getStoreValue( defBeans));

      finish();
    }

   /** See if edit value is valid, put error message in buff. */
    protected boolean _validate( StringBuffer buff) { return true; }

        /** return the editing JComponent */
    public JComponent getEditComponent() { return table; }

    public JComponent getDeepEditComponent() { return table.getJTable(); }

    /** Get current value from editComponent, will be an List of beanClass */
    protected Object getEditValue() { return table.getBeans(); }

    /** set value of editComponent, must be List of beanClass */
    protected void setEditValue(Object value) {
      if (value == null) return;
      table.setBeans( (ArrayList) value);
    }

    /** Get value from Store, will be a List of beanClass, or null
     * @param defValue use this as the default
     * @return the stored object: will be an List of beanClass, or null
     */
    protected Object getStoreValue(Object defValue) {
      if (storeData == null) return defValue;
      return storeData.getList( name, (java.util.List) defValue);
    }

   /** Put new value into Store, must be a List ob objects of type beanClass */
    protected void setStoreValue(Object newValue) {
     if (storeData != null)
       storeData.putList( name, (java.util.List) newValue);
    }

    /** Set edit value as an Object.
    public void setValue(Object value) {
      setEditValue( value);
      sendEvent();
    } */

  }



  /**
   * An integer input field with an associated "units" label.
   *
  static public class IntUnits extends Int implements UnitsField {
    private String units;
    IntUnits(String name, String label, String units, int defValue, PersistenceManager storeData) {
      super(name, label, defValue, storeData);
      this.units = units;
    }
    public String getUnits() { return units; }
  }


  /**
   * A boolean input box that is used to enable/disable another field.
   *
  static public class BooleanEnabler extends YesNo {
    private JRadioButton enabler;
    private Field enabledField;

    BooleanEnabler(String fldName, boolean initValue, Field enField, PersistenceManager storeData) {
      super( fldName, "", initValue, storeData);
      this.enabledField = enField;

      enabler = new JRadioButton();
      enabler.setSelected( ((Boolean)valueObj).booleanValue());
      editComp = (JComponent) enabler;

      enabledField.setEnabled(initValue);
      enabler.addItemListener( new ItemListener() {
        public void itemStateChanged(ItemEvent e) {
          boolean isOn = (e.getStateChange() == ItemEvent.SELECTED);
          enabledField.setEnabled(isOn);
        }
      });
    }

    protected void setValue(Object newValue) {
      boolean bvalue = ((Boolean)newValue).booleanValue();
      enabler.setSelected(bvalue);
      enabledField.setEnabled(bvalue);
      _setValue( newValue);
    }

    public boolean isSelected() { return enabler.isSelected(); }
    public void setSelected(boolean v) { enabler.setSelected(v); }

    Field getEnabledField() { return enabledField; }
  } */


    static private void showFormatInfo( JFormattedTextField tf) {
      JFormattedTextField.AbstractFormatter ff = tf.getFormatter();
      System.out.println("AbstractFormatter  " +  ff.getClass().getName());
      if (ff instanceof NumberFormatter) {
        NumberFormatter nf = (NumberFormatter) ff;
        Format f = nf.getFormat();
        System.out.println(" Format  = " + f.getClass().getName());
        if (f instanceof NumberFormat) {
          NumberFormat nfat = (NumberFormat) f;
          System.out.println(" getMinimumIntegerDigits=" +
                             nfat.getMinimumIntegerDigits());
          System.out.println(" getMaximumIntegerDigits=" +
                             nfat.getMaximumIntegerDigits());
          System.out.println(" getMinimumFractionDigits=" +
                             nfat.getMinimumFractionDigits());
          System.out.println(" getMaximumFractionDigits=" +
                             nfat.getMaximumFractionDigits());
        }
        if (f instanceof DecimalFormat) {
          DecimalFormat df = (DecimalFormat) f;
          System.out.println(" Pattern  = " + df.toPattern());
        }
      }
    }

  /** Double value formatting with fixed number of digits to the right of the decimal point.
   *
   * @param d the number to format.
   * @param fixed_decimals number of digits to the right of the decimal point
   * @return string representation, with specified number of decimal places
   */
  public static String dfrac( double d, int fixed_decimals) {
    return formatDouble( d, 100, fixed_decimals).trim();
    //String s = Double.toString( d);
    //s = sigfigFix( s, 100, num_dec);
    //return s.trim();
  }

/* This dorks with Double.toString():
 *
 * From Double.toString() (m = magnitude of the number):
 *
 *  If m is greater than or equal to 10^-3 but less than 10^7, then it is represented as the
 *  integer part of m, in decimal form with no leading zeroes, followed by '.' (.),
 *  followed by one or more decimal digits representing the fractional part of m.
 *
 *  If m is less than 10^-3 or greater than 10^7, then it is represented in scientific notation.
 *  Let n be the unique integer such that 10n<=m<10n+1; then let a be the mathematically exact
 *  quotient of m and 10n so that 1<=a<10. The magnitude is then represented as the integer part
 *  of a, as a single decimal digit, followed by '.' (.), followed by decimal digits representing
 *  the fractional part of a, followed by the letter 'E' (E), followed by a representation of n
 *  as a decimal integer, as produced by the method Integer.toString(int).
 *
 *  How many digits must be printed for the fractional part of m or a? There must be
 *  at least one digit to represent the fractional part, and beyond that as many,
 *  but only as many, more digits as are needed to uniquely distinguish the argument
 *  value from adjacent values of type double. That is, suppose that x is the exact
 *  mathematical value represented by the decimal representation produced by this method
 *  for a finite nonzero argument d. Then d must be the double value nearest to x; or if
 *  two double values are equally close to x, then d must be one of them and the least
 *  significant bit of the significand of d must be 0.
 */

  private static String formatDouble( double d, int min_sigFigs, int fixed_decimals) {
    String s = java.lang.Double.toString( d);

    if (java.lang.Double.isNaN(d)) return s;

    // extract the sign
    String sign;
    String unsigned;
    if ( s.startsWith( "-" ) || s.startsWith( "+" ) ) {
      sign = s.substring( 0, 1 );
      unsigned = s.substring( 1 );
    } else {
      sign = "";
      unsigned = s;
    }

    // deal with exponential notation
    String mantissa;
    String exponent;
    int eInd = unsigned.indexOf( 'E' );
    if ( eInd == -1 )
      eInd = unsigned.indexOf( 'e' );
    if ( eInd == -1 ) {
      mantissa = unsigned;
      exponent = "";
    } else {
      mantissa = unsigned.substring( 0, eInd );
      exponent = unsigned.substring( eInd );
    }

    // deal with decimal point
    StringBuffer number, fraction;
    int dotInd = mantissa.indexOf( '.' );
    if ( dotInd == -1 ) {
      number = new StringBuffer( mantissa );
      fraction = new StringBuffer( "" );
    } else {
      number = new StringBuffer( mantissa.substring( 0, dotInd ) );
      fraction = new StringBuffer( mantissa.substring( dotInd + 1 ) );
    }

    // number of significant figures
    int numFigs = number.length();
    int fracFigs = fraction.length();

    // can do either fixed_decimals or min_sigFigs
    if (fixed_decimals != -1) {
      if (fixed_decimals == 0) {
        fraction.setLength( 0 );
      } else if (fixed_decimals > fracFigs) {
        int want = fixed_decimals - fracFigs;
        for (int i=0; i<want; i++)
          fraction.append("0");
      } else if (fixed_decimals < fracFigs) {
        int chop = fracFigs - fixed_decimals;   // LOOK should round !!
        fraction.setLength( fraction.length() - chop );
      }
      fracFigs = fixed_decimals;

    } else {
            // Don't count leading zeros in the fraction, if no number
      if ( ( numFigs == 0 || number.toString().equals( "0" ) ) && fracFigs > 0 ) {
        numFigs = 0;
        number = new StringBuffer( "" );
        for ( int i = 0; i < fraction.length(); ++i ) {
          if ( fraction.charAt( i ) != '0' )
            break;
          --fracFigs;
        }
      }
        // Don't count trailing zeroes in the number if no fraction
      if ( ( fracFigs == 0) && numFigs > 0 ) {
        for ( int i=number.length()-1; i > 0; i-- ) {
          if ( number.charAt( i ) != '0' )
            break;
          --numFigs;
        }
      }
        // deal with min sig figures
      int sigFigs = numFigs + fracFigs;
      if (sigFigs > min_sigFigs) {
          // Want fewer figures in the fraction; chop (should round? )
        int chop = Math.min(sigFigs - min_sigFigs, fracFigs);
        fraction.setLength( fraction.length() - chop );
        fracFigs -= chop;
      }
    }


    /*int sigFigs = numFigs + fracFigs;
    if (sigFigs > max_sigFigs) {

      if (numFigs >= max_sigFigs) {  // enough sig figs in just the number part
        fraction.setLength( 0 );
        for ( int i=max_sigFigs; i<numFigs; ++i )
          number.setCharAt( i, '0' );  // should round?
      } else {

        // Want fewer figures in the fraction; chop (should round? )
        int chop = sigFigs - max_sigFigs;
        fraction.setLength( fraction.length() - chop );
      }
    }


    /* may want a fixed decimal place
    if (dec_places != -1) {

      if (dec_places == 0) {
        fraction.setLength( 0 );
        fracFigs = 0;
      } else if (dec_places > fracFigs) {
        int want = dec_places - fracFigs;
        for (int i=0; i<want; i++)
          fraction.append("0");
      } else if (dec_places < fracFigs) {
        int chop = fracFigs - dec_places;
        fraction.setLength( fraction.length() - chop );
        fracFigs = dec_places;
      }

    } */

    if ( fraction.length() == 0 )
      return sign + number + exponent;
    else
      return sign + number + "." + fraction + exponent;
  }

}

/* Change History:
   $Log: Field.java,v $
   Revision 1.12  2005/10/11 19:36:56  caron
   NcML add Records bug fixes
   iosp.isValidFile( ) throws IOException
   release 2.2.11

   Revision 1.11  2005/08/22 21:57:43  caron
   no message

   Revision 1.10  2005/08/22 17:13:58  caron
   minor fixes from intelliJ analysis

   Revision 1.9  2005/08/22 01:12:29  caron
   DatasetEditor

   Revision 1.8  2005/08/17 18:36:27  caron
   no message

   Revision 1.7  2005/08/17 00:13:58  caron
   Dataset Editor

   Revision 1.6  2004/08/26 17:55:18  caron
   no message

   Revision 1.5  2003/05/29 23:33:28  john
   latest release

   Revision 1.4  2003/01/14 19:32:10  john
   add Password.getPassword()

   Revision 1.3  2003/01/06 19:37:04  john
   new tests

   Revision 1.2  2002/12/24 22:04:49  john
   add bean, beanObject methods

   Revision 1.1.1.1  2002/12/20 16:40:26  john
   start new cvs root: prefs

   Revision 1.6  2002/03/09 01:51:54  caron
   add BeanTable, fix FieldResizable

   Revision 1.5  2002/03/01 23:01:38  caron
   resize Fields; replace LayoutManager for PrefPanel

   Revision 1.4  2002/02/15 21:57:53  caron
   minor fixes

   Revision 1.3  2001/11/14 19:48:07  caron
   TextFormatted bug

   Revision 1.2  2001/11/12 19:36:14  caron
   version 0.3

   Revision 1.1.1.1  2001/11/10 16:01:24  caron
   checkin prefs

*/

