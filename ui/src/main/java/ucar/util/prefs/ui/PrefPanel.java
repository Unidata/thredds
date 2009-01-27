// $Id: PrefPanel.java,v 1.15 2006/07/06 21:35:46 caron Exp $
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

import java.util.prefs.Preferences;
import ucar.util.prefs.*;

import com.jgoodies.forms.layout.*;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.builder.PanelBuilder;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.EventListenerList;

/**
 *  Create a User Preferences Panel or Dialog.
 *
 *  A PrefPanel manages a set of Fields with convenience methods for rapidly creating
 *  User Dialogs whose values are made persistent by a Preferences Store. All Fields
 *  contained in the PrefPanel share the same Preferences, and so must have unique names.
 *
 * Send ActionEvent when "accept" button is pressed. Can also listen on individual Fields.
 * You must call one finish() method exactly once, when you are done adding Fields.
 *
 * <p> Example of use:
  <pre>
    PreferencesExt store = null;
    try {
      xstore = XMLStore.createFromFile("E:/dev/prefs/test/panel/panel.xml", null);
      store = xstore.getPreferences();
    } catch( Exception e) {
      System.out.println(e);
      System.exit( 1);
    }
    PrefPanel pp = new PrefPanel("test", store);
    pp.addTextField("name", "name", "defValue");
    pp.newColumn();
    pp.addTextField("name2", "name2", "defValue22");
    pp.newColumn();
    pp.addTextField("name3", "name3", "defValue22 asd jalskdjalksjd");
    pp.finish();

    pp.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        // accept was called
      }
    });
  </pre>
 *
 * <h3> Form layout</h3>
 * The PrefPanel is layed out with the jgoodies FormLayout layout manager and PanelBuilder, which use a row, colummn grid.
 * Fields in the same column are aligned.
 *
 * <p>There are 2 ways to do form layout: implicit and explicit. With implicit, the position is implicitly specified by the
 *  order the fields are added, using, for example:
 * <pre>  addDoubleField(String fldName, String label, double defValue) </pre>
 *
 * The fields are all added in a column. To start a new column, use setCursor().
 * <p>With explicit, you specify the row and col, and an optional constraint:
 * <pre>  addDoubleField(String fldName, String label, double defValue, int col, int row, String constraint) </pre>
 *
 * Row and column numbers are 0 based. Each field has a width of 2 columns (one for the label and one for the
 *  component) and a height of 1 row, unless you specify otherwise using a constraint.
 * A heading takes up an entire row, spanning all columns
 *
 * @author John Caron
 * @version $Id: PrefPanel.java,v 1.15 2006/07/06 21:35:46 caron Exp $
 */

public class PrefPanel extends JPanel {
  static private KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();

  private String name;
  private Preferences prefs;
  private PersistenceManager storeData;
  private String helpTarget = null;

  private boolean finished = false;
  private HashMap flds = new HashMap(40);
  // private ArrayList currentComps, colList; // track columns of components
  private ArrayList layoutComponents; // use with form layout
  private int cursorRow = 0, cursorCol = 0; // current row and column

  private JPanel mainPanel;
  private ArrayList auxButtons = new ArrayList();

  // event handling
  private EventListenerList listenerList = new EventListenerList();

  private boolean debugLayout = false;

  /**
   * Constructor.
   * @param name may be null.
   * @param prefs keep values in here; may be null.
   */
  public PrefPanel(String name, PreferencesExt prefs) {
    this( name, prefs, prefs);
  }
  /**
   * Constructor.
   * @param name may be null.
   * @param storeData keep values in here; may be null.
   */
  public PrefPanel(String name, Preferences prefs, PersistenceManager storeData) {
    this.name = name;
    this.prefs = prefs;
    this.storeData = storeData;

    //colList = new ArrayList( 5);
    //currentComps = new ArrayList( 10);
    //colList.add( currentComps);
    layoutComponents = new ArrayList(20);

    /* manager.addPropertyChangeListener( "focusOwner", new PropertyChangeListener() {
       public void propertyChange(PropertyChangeEvent evt) {
         Object val = evt.getNewValue();
         String sval = (val == null) ? "null" : val.getClass().getName();
         Component own = manager.getFocusOwner();
         String sown = (own == null) ? "null" : own.getClass().getName();
         System.out.println("focusOwner val="+sval+" own="+sown);
       }
     });
    manager.addPropertyChangeListener( "permanentFocusOwner", new PropertyChangeListener() {
       public void propertyChange(PropertyChangeEvent evt) {
         Object val = evt.getNewValue();
         String sval = (val == null) ? "null" : val.getClass().getName();
         Component pown = manager.getPermanentFocusOwner();
         String sown = (pown == null) ? "null" : pown.getClass().getName();
         System.out.println("permanentFocusOwner val="+sval+" own="+sown);
       }
     }); */
   }   

  /* public void setPersistenceManager (PersistenceManager storeData) {
    this.storeData = storeData;
    Iterator iter = flds.values().iterator();
    while (iter.hasNext()) {
      Field f = (Field) iter.next();
      f.setPersistenceManager(storeData);
    }
  }       */

  /** Add listener: action event sent if "apply" button is pressed */
  public void addActionListener(ActionListener l) {
    listenerList.add(java.awt.event.ActionListener.class, l);
  }
  /** Remove listener */
  public void removeActionListener(ActionListener l) {
    listenerList.remove(java.awt.event.ActionListener.class, l);
  }

  private void fireEvent(java.awt.event.ActionEvent event) {
    // Guaranteed to return a non-null array
    Object[] listeners = listenerList.getListenerList();
    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length-2; i>=0; i-=2) {
      ((java.awt.event.ActionListener)listeners[i+1]).actionPerformed(event);
    }
  }

  /** Call Field.accept() on all Fields. This puts any edits into the Store,
   *  and fires PropertyChangeEvents if any values change, and sends an
   *  ActionEvent to any listeners.
   */
  public boolean accept() {
    StringBuffer buff = new StringBuffer();
    boolean ok = true;
    Iterator iter = flds.values().iterator();
    while (iter.hasNext())
      ok &= ((Field) iter.next()).accept(buff);

    if (!ok) {
      try { JOptionPane.showMessageDialog(PrefPanel.findActiveFrame(), buff.toString()); }
      catch (HeadlessException e) { }
      return false;
    }

    /* store the text widths if they exist
    if (storeData != null) {
      Preferences substore = prefs.node("sizes");
      iter = flds.values().iterator();
      while (iter.hasNext()) {
        Field fld = (Field) iter.next();
        JComponent comp = fld.getEditComponent();
        substore.putInt(fld.getName(), (int) comp.getPreferredSize().getWidth());
      }
    } */
    fireEvent(new ActionEvent(this, 0, "Accept"));
    return true;
  }

  /**
   * Set enabled on all the fields in the prefPanel
   * @param enable
   */
  public void setEnabled( boolean enable) {
    Iterator iter = flds.values().iterator();
    while (iter.hasNext())
      ((Field) iter.next()).setEnabled(enable);
  }

  /** Return the name of the PrefPanel. */
  public String getName() { return name; }

  /** Iterator over the fields */
  public Iterator getFields() { return flds.values().iterator(); }

  /**
   * Find the field with the specified name.
   * @param name of Field
   * @return Field or null if not found
   */
  public Field getField(String name) {
    Field fld = (Field) flds.get(name);
    if (fld == null) return null;
    return (fld instanceof FieldResizable) ? ((FieldResizable)fld).getDelegate() : fld;
  }

  /**
   * Get current value of the named field
   * @param name of field
   * @return value of named field
   */
  public Object getFieldValue(String name) {
    Field fld = getField(name);
    if (fld == null) throw new IllegalArgumentException("no field named "+name);
    return fld.getValue();
  }

  /**
   * Set the current value of the named field
   * @param name of field
   * @param value of field
   */
  public void setFieldValue(String name, Object value) {
    Field fld = getField(name);
    if (fld == null) throw new IllegalArgumentException("no field named "+name);
    fld.setValue(value);
  }

  /** Add a button to the button panel */
  public void addButton( JComponent b) { auxButtons.add(b); }

  /**
   * Add a field created by the user.
   * @param fld add this field.
   */
  public Field addField(Field fld) {
    addField( fld, cursorCol, cursorRow, null);
    cursorRow++;
    return fld;
  }

  public Field addField(Field fld, int col, int row, String constraint) {
    if (null != flds.get(fld.getName()))
      throw new IllegalArgumentException("PrefPanel: already have field named "+fld.getName());

    //currentComps.add( fld);
    flds.put( fld.getName(), fld);
    layoutComponents.add( new LayoutComponent( fld, col, row, constraint));

    fld.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange( PropertyChangeEvent e) { revalidate(); }
    });

    return fld;
  }


  public Field.BeanTable addBeanTableField(String fldName, String label, java.util.ArrayList beans, Class beanClass,
               int col, int row, String constraint) {
    Field.BeanTable fld = new Field.BeanTable(fldName, label, beans, beanClass, (PreferencesExt) prefs, (PersistenceManager) storeData);
    addField( fld, col, row, constraint);
    return fld;
  }

  /**
   * Add a boolean field as a checkbox.
   * @param fldName the name to store the data in the PersistenceManagerData
   * @param label used as the label on the panel
   * @param defValue default value
   */
  public Field.CheckBox addCheckBoxField(String fldName, String label, boolean defValue) {
    Field.CheckBox fld = new Field.CheckBox(fldName, label, defValue, storeData);
    addField( fld);
    return fld;
  }

  public Field.CheckBox addCheckBoxField(String fldName, String label, boolean defValue, int col, int row) {
    Field.CheckBox fld = new Field.CheckBox(fldName, label, defValue, storeData);
    addField( fld, col, row, null);
    return fld;
  }

  /**
   * add a boolean field to turn a field on/off
   * @param fldName: the name to store the data in the PersistenceManagerData
   * @param defvalue: default value
   * @param enabledField: the InputField to enable/disable; must already be added
   *
  public Field.BooleanEnabler addEnablerField(String fldName, boolean defValue, Field enabledField) {
    Field.BooleanEnabler enabler = new Field.BooleanEnabler(fldName, defValue, enabledField, storeData);
    // flds.add( enabledField);
    enabledField.hasEnabler = true;

    flds.add( enabler);
    return enabler;
  } */

  /**
   * Add a field that edits a date
   * @param fldName the name to store the data in the PersistenceManagerData
   * @param label used as the label on the panel
   * @param defValue default value
   */
  public Field.Date addDateField(String fldName, String label, Date defValue) {
    Field.Date fld = new Field.Date(fldName, label, defValue, storeData);
    addField( new FieldResizable(fld, this));
    return fld;
  }

  public Field.Date addDateField(String fldName, String label, Date defValue,
        int col, int row, String constraint) {
    Field.Date fld = new Field.Date(fldName, label, defValue, storeData);
    addField( fld, col, row, constraint);
    return fld;
  }

  /**
   * Add a field that edits a double
   * @param fldName the name to store the data in the PersistenceManagerData
   * @param label used as the label on the panel
   * @param defValue default value
   */
  public Field.Double addDoubleField(String fldName, String label, double defValue) {
    Field.Double fld = new Field.Double(fldName, label, defValue, -1, storeData);
    addField( new FieldResizable(fld, this));
    return fld;
  }

  public Field.Double addDoubleField(String fldName, String label, double defValue,
        int col, int row, String constraint) {
    Field.Double fld = new Field.Double(fldName, label, defValue, -1, storeData);
    addField( fld, col, row, constraint);
    return fld;
  }

  public Field.Double addDoubleField(String fldName, String label, double defValue, int nfracDig,
        int col, int row, String constraint) {
    Field.Double fld = new Field.Double(fldName, label, defValue, nfracDig, storeData);
    addField( fld, col, row, constraint);
    return fld;
  }


  public Field.EnumCombo addEnumComboField(String fldName, String label, java.util.Collection defValues,
               boolean editable, int col, int row, String constraint) {
    Field.EnumCombo fld = new Field.EnumCombo(fldName, label, defValues, (PersistenceManager) storeData);
    addField( fld, col, row, constraint);
    fld.setEditable( editable);
    return fld;
  }

  public Field.EnumCombo addEnumComboField(String fldName, String label, java.util.Collection defValues, boolean editable) {
    Field.EnumCombo fld = new Field.EnumCombo(fldName, label, defValues, (PersistenceManager) storeData);
    addField( fld);
    fld.setEditable( editable);
    return fld;
  }

  /**
   * Add a field that edits a formatted text field
   * NOTE: to use this directly, you must use a PersistenceManagerExt object.
   * @param fldName the name to store the data in the PersistenceManagerData
   * @param label used as the label on the panel
   * @param defValue default value
   *
  public Field.TextFormatted addTextFormattedField(String fldName, String label,
         JFormattedTextField tf, Object defValue) {
    Field.TextFormatted fld = new Field.TextFormatted(fldName, label, tf, defValue, storeData);
    addField( new FieldResizable(fld, this));
    return fld;
  } */

  /**
   * Add a field that edits an integer
   * @param fldName the name to store the data in the PersistenceManagerData
   * @param label used as the label on the panel
   * @param defValue default value
   */
  public Field.Int addIntField(String fldName, String label, int defValue) {
    Field.Int fld = new Field.Int(fldName, label, defValue, storeData);
    addField( new FieldResizable(fld, this));
    return fld;
  }

  /**
   * Add an integer field with units.
   * @param fldName: the name to store the data in the PersistenceManagerData
   * @param label: used as the label on the panel
   * @param defvalue: default value
   * @param units: optional unit label
   *
  public Field.Int addIntField(String fldName, String label, int defValue, String units) {
    Field.Int fld = new Field.IntUnits(fldName, label, units, defValue, storeData);
    flds.add( fld);
    return fld;
  } */

  /**
   * Add a password text field.
   * @param fldName the name to store the data in the PersistenceManagerData
   * @param label used as the label on the panel
   * @param defValue default value
   */
  public Field.Password addPasswordField(String fldName, String label, String defValue) {
    Field.Password fld = new Field.Password(fldName, label, defValue, storeData);
    addField( new FieldResizable(fld, this));
    return fld;
  }

  /**
   * Add a text field.
   * @param fldName the name to store the data in the PersistenceManagerData
   * @param label used as the label on the panel
   * @param defValue default value
   * @return the Field.Text object that was added
   */
  public Field.Text addTextField(String fldName, String label, String defValue) {
    Field.Text fld = new Field.Text(fldName, label, defValue, storeData);
    addField( new FieldResizable(fld, this));
    return fld;
  }

  public Field.Text addTextField(String fldName, String label, String defValue, int col, int row, String constraint ) {
    Field.Text fld = new Field.Text(fldName, label, defValue, storeData);
    addField( fld, col, row, constraint);
    return fld;
  }

  /**
   * Add a text combobox field.
   * @param fldName the name to store the data in the PersistenceManagerData
   * @param label used as the label on the panel
   * @param defValues list of default values (Strings) to include in the comboBox. May be null.
   *    These are added to the combobox (at the end) no matter how many there are.
   * @param nKeep number of most recently used values to keep
   * @param editable whether the user can add new entries the list to select from.
   */
  public Field.TextCombo addTextComboField(String fldName, String label, java.util.Collection defValues, int nKeep, boolean editable) {
    Field.TextCombo fld = new Field.TextCombo(fldName, label, defValues, nKeep, (PersistenceManager) storeData);
    addField( fld);
    fld.setEditable( editable);
    return fld;
  }

  public Field.TextCombo addTextComboField(String fldName, String label, java.util.Collection defValues, int nKeep,
               boolean editable, int col, int row, String constraint) {
    Field.TextCombo fld = new Field.TextCombo(fldName, label, defValues, nKeep, (PersistenceManager) storeData);
    addField( fld, col, row, constraint);
    fld.setEditable( editable);
    return fld;
  }


  /**
   * Add a TextArea field.
   * @param fldName the name to store the data in the PersistenceManagerData
   * @param label used as the label on the panel
   * @param def default value
   * @param nrows number of rows
   */
  public Field.TextArea addTextAreaField(String fldName, String label, String def, int nrows) {
    Field.TextArea fld = new Field.TextArea(fldName, label, def, nrows, (PersistenceManager) storeData);
    addField( fld);
    return fld;
  }

  public Field.TextArea addTextAreaField(String fldName, String label, String def, int nrows,
                                         int col, int row, String constraint) {
    Field.TextArea fld = new Field.TextArea(fldName, label, def, nrows, (PersistenceManager) storeData);
    addField( fld, col, row, constraint);
    return fld;
  }

  /**
   * Add a text combobox field.
   * @param fldName the name to store the data in the PersistenceManagerData
   * @param label used as the label on the panel
   *  @param defValues list of default values to include in the comboBox. May be null.
   *    These are added to the combobox (at the end) no matter how many there are.
   *  @param nKeep number of most recently used values to keep
   *
  public Field.Combo addComboField(String fldName, String label, java.util.Collection defValues, int nKeep) {
    Field.Combo fld = new Field.Combo(fldName, label, defValues, nKeep, (PersistenceManagerExt) storeData);
    addField( fld);
    return fld;
  } */

  /** Add a heading that takes no input */
  public void addHeading(String heading) {
    addHeading(heading, cursorRow);
    cursorRow++;
  }

  /** Add a heading at the specified row. this spans all columns */
  public void addHeading(String heading, int row) {
    layoutComponents.add( new LayoutComponent( heading, 0, row, null));
  }

  /** Add a Component. */
  public void addComponent(Component comp, int col, int row, String constraint) {
    layoutComponents.add( new LayoutComponent( comp, col, row, constraint));
  }

  /** Add a seperator after the last field added. */
  public void addSeparator() {
    addEmptyRow( cursorRow++, 15);
  }

  /** Add a seperator after the last field added. */
  public void addEmptyRow(int row, int size) {
    layoutComponents.add( new LayoutComponent( null, size, row, null));
  }

  /** Start a new column.
   *  Everything added goes into a vertical column until another call to newColumn().
   */
  public void setCursor(int col, int row) {
    //currentComps = new ArrayList(10);
    //colList.add( currentComps);
    cursorCol = col;
    cursorRow = row;
  }

  /** Call this when you have finish constructing the panel, adding buttons in default spot */
  public void finish() { finish( true); }

  /**
   * Call this when you have finish constructing the panel.
   * @param addButtons if true, add buttons in default spot
   */
  public void finish( boolean addButtons) { finish( addButtons, BorderLayout.SOUTH); }

  /**
   * Call this when you have finish constructing the panel.
   * @param addButtons if true, add buttons
   * @param where to add the buttons BorderLayout.NORTH, SOUTH, EAST, WEST, CENTER
   *
  public void finish( boolean addButtons, String where) {
    if (finished)
      throw new IllegalStateException("PrefPanel "+name+": already called finish()");

    mainPanel = new JPanel();
    mainPanel.setLayout( new LayoutM("main"));

    // fetch the stored widths if they exists
    if (storeData != null) {
      Preferences substore = prefs.node("sizes");
      Iterator iter = flds.values().iterator();
      while (iter.hasNext()) {
        Field fld = (Field) iter.next();
        int width = substore.getInt(fld.getName(), 0);
        if (width > 0) {
          JComponent comp = fld.getEditComponent();
          Dimension prefDim = comp.getPreferredSize();
          // System.out.println("pref = "+prefDim+" "+width);
          comp.setPreferredSize(new Dimension( width, (int) prefDim.getHeight()));
        }
      }
    }

    // make each JPanel column
    boolean multiColumn = colList.size() > 1;
    int count = 0;
    Iterator iter = colList.iterator();
    JPanel lastPanel = null;
    while (iter.hasNext()) {
      //if (!first)
      //  mainPanel.add( new JSeparator( SwingConstants.VERTICAL));
      //first = false;

      ArrayList cs = (ArrayList) iter.next();
      JPanel panel =  makeColumnPanel( "panel"+count, cs);
      if (multiColumn)
        mainPanel.add( panel, new LayoutM.Constraint( lastPanel, 10, 0));
      else
        mainPanel = panel;

      lastPanel = panel;
      count++;
    }

      // button panel
    JPanel buttPanel = new JPanel();
    JButton acceptButton = new JButton("Apply");
    buttPanel.add(acceptButton, null);
    for (int i=0; i<auxButtons.size(); i++)
      buttPanel.add((JComponent) auxButtons.get(i), null);

      // button listeners
    acceptButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        accept();
      }
    });

    /* if (helpTarget != null) {
      JButton helpButton = new JButton("Help");
      buttPanel.add(helpButton, null);
      helpButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          System.out.println(" call help taget = "+ helpTarget);
          ucar.unidata.ui.Help.getDefaultHelp().gotoTarget(helpTarget);
          System.out.println(" complete help taget = "+ helpTarget);
        }
      });
    }

    //BoxLayout bl2 = new BoxLayout( this, BoxLayout.Y_AXIS);
    setLayout( new BorderLayout());
    add(mainPanel, BorderLayout.CENTER);

    if (addButtons) {
      if (where == BorderLayout.SOUTH) {
        JPanel south = new JPanel();
        south.setLayout(new BoxLayout( south, BoxLayout.Y_AXIS));
        south.add( new JSeparator(SwingConstants.HORIZONTAL));
        south.add( buttPanel);
        add( south, BorderLayout.SOUTH);
      } else
        add(buttPanel, where);
    }

    finished = true;
  }

  // layout a column, based on maximum label width
  private JPanel makeColumnPanel(String name, ArrayList comps) {
    JPanel parent = new JPanel( new LayoutM(name));

    // find max widths
    double maxLabelWidth = 0.0;
    double maxFieldWidth = 0.0;
    for (int i = 0; i < comps.size(); i++) {
      Object o = comps.get(i);
      if (o instanceof JLabel) {
        JLabel lab = (JLabel) o;
        Dimension prefDim = lab.getPreferredSize();
        maxLabelWidth = Math.max( maxLabelWidth, prefDim.getWidth());
      } else if (o instanceof Field) {
        Field fld = (Field) o;
        JLabel lab = new JLabel(fld.getLabel()+" :");
        Dimension prefDim = lab.getPreferredSize();
        maxLabelWidth = Math.max( maxLabelWidth, prefDim.getWidth());
        prefDim = fld.getEditComponent().getPreferredSize();
        maxFieldWidth = Math.max( maxFieldWidth, prefDim.getWidth());
      }
    }
    int totalWidth = (int) (maxLabelWidth+maxFieldWidth);

    // do the layout; the labels are right justified, the fields left justified
    ArrayList sepFlds = new ArrayList();
    JComponent lastComp = null;
    for (int i = 0; i < comps.size(); i++) {
      Object o = comps.get(i);

      if (o instanceof JLabel) {
        JLabel lab = (JLabel) o;
        parent.add( lab, new LayoutM.Constraint(lastComp, (int) -maxLabelWidth-20, 10));
        lastComp = lab;

      } else if (o instanceof Field) {
        Field fld = (Field) o;
        JLabel lab = new JLabel(fld.getLabel()+" :");
        JComponent comp = fld.getEditComponent();

        parent.add( lab, new LayoutM.Constraint(lastComp, (int) -maxLabelWidth, 10));
        lastComp = lab;
        parent.add( comp, new LayoutM.Constraint(lastComp, 5, 0));

      }  else if (o instanceof JSeparator) {
        JSeparator sep = (JSeparator) o;
        parent.add( sep, new LayoutM.Constraint(lastComp, (int) -totalWidth, 10));
        lastComp = sep;

        Dimension prefDim = sep.getPreferredSize();
        sep.setPreferredSize(new Dimension( totalWidth, (int) prefDim.getHeight()));
      }

    }

    return parent;
  } */

  /**
   * Call when finished adding components to the PrefPanel.
   * @param addButtons if true, add buttons
   * @param where BorderLayout.NORTH, SOUTH, EAST, WEST
   */
  public void finish( boolean addButtons, String where) {
    if (finished)
      throw new IllegalStateException("PrefPanel "+name+": already called finish()");  

    StringBuffer sbuff = new StringBuffer();

    // column layout, first sort by col
    Collections.sort(layoutComponents, new Comparator() {
      public int compare(Object o1, Object o2) {
        LayoutComponent lc1 = (LayoutComponent) o1;
        LayoutComponent lc2 = (LayoutComponent) o2;
        return lc1.col - lc2.col;
      }
      public boolean equals(Object o1) { return o1 == this; }
    });

    // now create column layout spec and x cell constraint
    sbuff.setLength(0);
    int currCol = -1;
    Iterator iter = layoutComponents.iterator();
    while (iter.hasNext()) {
      LayoutComponent lc = (LayoutComponent) iter.next();
      if (lc.col > currCol) {
        if (currCol >= 0)
          sbuff.append(", 5dlu, ");
        else
          sbuff.append("3dlu, ");
        sbuff.append( "right:default, 3dlu, default:grow");
        currCol += 2;
      }
      lc.ccLabel.gridX = 2*lc.col+2;
      lc.cc.gridX = 2*lc.col+4;
    }
    String colSpec = sbuff.toString();
    if (debugLayout) System.out.println(" column layout = "+ colSpec);
    int ncols = 2*currCol;

    // row layout, first sort by row
    Collections.sort(layoutComponents, new Comparator() {
      public int compare(Object o1, Object o2) {
        LayoutComponent lc1 = (LayoutComponent) o1;
        LayoutComponent lc2 = (LayoutComponent) o2;
        return lc1.row - lc2.row;
      }
      public boolean equals(Object o1) { return o1 == this; }
    });

    // now adjust for any headings, put into y cell constraint
    int incr = 0;
    iter = layoutComponents.iterator();
    while (iter.hasNext()) {
      LayoutComponent lc = (LayoutComponent) iter.next();
      if ((lc.comp instanceof String) && (lc.row > 0)) // its a header, not in first position
        incr++; // leave space by adding a row

      lc.cc.gridY = lc.row + incr + 1; // adjust downward
      lc.ccLabel.gridY = lc.cc.gridY;
      if (debugLayout) System.out.println(lc+" constraint = "+ lc.cc);
    }

    // now create row layout spec
    sbuff.setLength(0);
    int currRow = -1;
    iter = layoutComponents.iterator();
    while (iter.hasNext()) {
      LayoutComponent lc = (LayoutComponent) iter.next();
      while (lc.row > currRow) {
        if ((lc.comp instanceof String) && (lc.row > 0)) {
          sbuff.append( ", 5dlu, default");
        } else if ((lc.comp == null)) {
          sbuff.append(", ").append(lc.col).append("dlu");
        } else {
          if (currRow >= 0) sbuff.append(", ");
          sbuff.append("default");
        }
        currRow++;
      }
    }
    String rowSpec = sbuff.toString();
    if (debugLayout) System.out.println(" row layout = "+ rowSpec);

    // the jgoodies form layout
    FormLayout layout = new FormLayout( colSpec, rowSpec);

    PanelBuilder builder = new PanelBuilder(layout);
    builder.setDefaultDialogBorder();

    CellConstraints cc = new CellConstraints();

    // now add each component with correct constraint
    iter = layoutComponents.iterator();
    while (iter.hasNext()) {
      LayoutComponent lc = (LayoutComponent) iter.next();

      if (lc.comp instanceof Field) {
        Field fld = (Field) lc.comp;
        builder.addLabel( fld.getLabel()+":", lc.ccLabel);
        Component comp = fld.getEditComponent();
        if (lc.comp instanceof Field.TextArea)
          comp= new JScrollPane( comp);
        builder.add( comp,  lc.cc);
      } else if (lc.comp instanceof String) {
         String header = (String) lc.comp;
         builder.addSeparator(header, cc.xyw(1, lc.cc.gridY, ncols));
      }  else if (lc.comp instanceof Component) {
         builder.add( (Component) lc.comp,  lc.cc);
      }
    }

    mainPanel =  builder.getPanel();

      // button panel
    JPanel buttPanel = new JPanel();
    JButton acceptButton = new JButton("Apply");
    buttPanel.add(acceptButton, null);
    for (int i=0; i<auxButtons.size(); i++)
      buttPanel.add((JComponent) auxButtons.get(i), null);

      // button listeners
    acceptButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        accept();
      }
    });

    setLayout( new BorderLayout());
    add(mainPanel, BorderLayout.CENTER);

    if (addButtons) {
      if (where.equals( BorderLayout.SOUTH)) {
        JPanel south = new JPanel();
        south.setLayout(new BoxLayout( south, BoxLayout.Y_AXIS));
        south.add( new JSeparator(SwingConstants.HORIZONTAL));
        south.add( buttPanel);
        add( south, BorderLayout.SOUTH);
      } else
        add(buttPanel, where);
    }

    finished = true;
  }

  // helper class to use jgoodies form to do layout.
  // Each field gets one of these
  private class LayoutComponent {
    Object comp;
    String constraint;
    int row, col;
    CellConstraints cc, ccLabel;

    LayoutComponent(Object comp, int col, int row, String constraint ) {
      this.comp = comp;
      this.constraint = constraint;
      this.row = row;
      this.col = col;

      this.cc = new CellConstraints( 1+", "+1+" "+(constraint == null ? "" : constraint));
      if (cc.gridWidth > 1)
        cc.gridWidth = cc.gridWidth * 2 - 1;
      this.ccLabel = new CellConstraints();
    }

    public String toString() {
      if (comp == null)
        return "empty row";
      if (comp instanceof Field) return ((Field)comp).getName();
      return comp.getClass().getName();
    }

  }

  public class TabbedPanel {

  }

  /**
   * A convenience class for constructing a standalone JDialog window that has a PrefPanel inside it.
   * To show it on screen, call dialog.show().
   * Example:
   * <pre>
   *
    PrefPanel.Dialog d = new PrefPanel.Dialog( frame, true, "testDialogue",
                     (PersistenceManagerExt) store.node("dialog"));
    PrefPanel pp2 = d.getPrefPanel();
    pp2.addHeading("This is Not Your Life:");
    pp2.addTextField("name", "name", "defValue");
    pp2.addTextField("name2", "name2", "defValue22");
    pp2.addTextField("name3", "name3", "defValue22 asd jalskdjalksjd");
    pp2.addSeparator();
    pp2.addHeading("Part Two:");
    pp2.addPasswordField("password", "password", "secret");
    pp2.addIntField("testInt", "testInt", 1234);
    pp2.addDoubleField("testD", "testD", 1234.45);
    pp2.addCheckBoxField("testB", "testB", true);
    pp2.newColumn();
    pp2.addHeading("Another Column:");
    pp2.addDateField("date", "date", new Date());
    try {
      pp2.addTextFormattedField("ff", "ff", new javax.swing.text.MaskFormatter("(###) ###-####"), "(303) 497-1234");
    } catch (java.text.ParseException e) { }
    ArrayList list = new ArrayList(5);
    list.add("this");
    list.add("is");
    list.add("new");
    list.add("but");
    list.add("really too longs");
    pp2.addTextComboField("combo", "combo", list, 5);

    d.finish();
    d.show();

  </pre>
   */
  static public class Dialog extends JDialog {
    private PrefPanel pp;
    private PreferencesExt substore = null;

    /** constructor
       @param parent      JFrame (application) or JApplet (applet)
       @param modal     true is modal (must finish editing before can do anything else)
       @param title       title of window
       @param prefs       PersistenceManagerExt store: keep values in here; may be null.
     */
    public Dialog(RootPaneContainer parent, boolean modal, String title, PreferencesExt prefs) {
      this(parent, modal, title, prefs, prefs);
    }

    /** constructor
       @param parent      JFrame (application) or JApplet (applet)
       @param modal     true is modal (must finish editing before can do anything else)
       @param title       title of window
       @param prefs       PersistenceManagerExt store: keep values in here; may be null.
     */
    public Dialog(RootPaneContainer parent, boolean modal, String title, Preferences prefs, PersistenceManager storeData) {
      super((parent != null) && (parent instanceof JFrame) ? (JFrame) parent : findActiveFrame());
      setModal(modal);
      if (title != null)
        setTitle(title);
      if (prefs != null)
        substore = (PreferencesExt) prefs.node("Dialog");

      if (substore != null) {
        Rectangle r = (Rectangle) substore.getBean("Bounds", null);
        if (r != null) setBounds(r);
      }

      // L&F may change
      UIManager.addPropertyChangeListener( new PropertyChangeListener() {
        public void propertyChange( PropertyChangeEvent e) {
          if (e.getPropertyName().equals("lookAndFeel"))
            SwingUtilities.updateComponentTreeUI( Dialog.this);
        }
      });

      Container cp = getContentPane();
      pp = new PrefPanel( title, prefs, storeData);
      cp.add(pp, BorderLayout.CENTER);

        // add a dismiss button
      JButton dismiss = new JButton("Cancel");
      dismiss.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          setVisible( false);
        }
      });
      pp.addButton( dismiss);

      // watch for accept
      pp.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          setVisible( false);
        }
      });

        // catch move, resize events
      addComponentListener( new ComponentAdapter() {
        public void componentMoved( ComponentEvent e) {
          if (substore != null)
            substore.putBeanObject("Bounds", getBounds());
        }
         public void componentResized( ComponentEvent e) {
          if (substore != null)
            substore.putBeanObject("Bounds", getBounds());
        }
      });

    }

    /** Get the PrefPanel */
    public PrefPanel getPrefPanel() { return pp; }

    /**
     * Find the field with the specified name.
     * @param name of Field
     * @return Field or null if not found
     */
    public Field getField(String name) {
      return pp.getField(name);
    }

    /** Call this when done adding Fields to the prefPanel, instead of calling
     *  pp.finish().
     */
    public void finish() {
      pp.finish();
      pack();

        // persistent state
      if (substore != null) {
        Rectangle b = (Rectangle) substore.getBean("Bounds", null);
        if (b != null)
          setBounds( b);
        substore.putBeanObject("Bounds", getBounds());
      }
    }
  }

  // thanks to Heinz M. Kabutz
  static public Frame findActiveFrame() {
    Frame[] frames = JFrame.getFrames();
    for (int i = 0; i < frames.length; i++) {
      Frame frame = frames[i];
      if (frame.isVisible()) {
        return frame;
      }
    }
    return null;
  }

}