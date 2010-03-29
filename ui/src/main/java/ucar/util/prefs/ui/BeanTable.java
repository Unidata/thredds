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

import ucar.util.prefs.*;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.*;
import javax.swing.table.*;

/**
 * A JTable that uses JavaBeans to store the data.
 *
 * The columns of the JTable are the Properties of the Javabean, found through introspection.
 *
 * The properties may be editable if they have type primitive or String. and you list
 *   the editable properties in a static method editableProperties() in the bean, eg :
 * <pre>
 *     static public String editableProperties() { return "ID serverName active writeDirectory"; }
 * </pre>
 *
 *  You may hide properties by listing them in a static method hiddenProperties() in the bean, eg :
 * <pre>
 *   static public String hiddenProperties() { return "hideThisProperty DDDirectory"; }
 * </pre>
 *
 * The data can be made persistent through a PreferencesExt store.
 * The width and order of the columns is persistent.
 * The javabean class may add or delete properties, and the stored data will be reasonably intact.
 *
 * @see ucar.util.prefs.PreferencesExt
 * @author John Caron
 */

public class BeanTable extends JPanel {
  protected Class beanClass;
  protected PreferencesExt store;
  protected JTable jtable;
  protected JScrollPane scrollPane;
  protected EventListenerList listenerList = new EventListenerList();

  protected List<Object> beans;
  protected TableBeanModel model;

  protected boolean debug = false, debugStore = false, debugBean = false, debugSelected = false;

  public BeanTable( Class bc, PreferencesExt pstore, boolean canAddDelete) {
    this( bc, pstore, canAddDelete, null, null);
  }
  /**
   *  Constructor.
   * @param bc JavaBean class
   * @param pstore store data in this PreferencesExt store.
   * @param canAddDelete allow changes to the jtable - adds a New and Delete button to bottom panel
   * @param header optional header label
   */
  public BeanTable( Class bc, PreferencesExt pstore, boolean canAddDelete, String header, String tooltip) {
    this.beanClass = bc;
    this.store = pstore;

    beans = (store != null) ? (ArrayList<Object>) store.getBean("beanList", new ArrayList()) : new ArrayList<Object>();
    model = new TableBeanModel( beanClass);
    jtable = new JTable(model);
    //jtable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); default = multiple
    jtable.setAutoResizeMode( JTable.AUTO_RESIZE_NEXT_COLUMN);
    jtable.setDefaultRenderer(java.util.Date.class, new DateRenderer());

    ToolTipManager.sharedInstance().registerComponent( jtable);

    restoreState();

    //  set the header renderers
    TableColumnModel tcm = jtable.getColumnModel();
    for (int i=0; i<jtable.getColumnCount(); i++) {
      TableColumn tc = tcm.getColumn(i);
      int model_idx = tc.getModelIndex();
      tc.setHeaderRenderer( new HeaderRenderer( model_idx));
    }

    // editor/renderers
    jtable.setDefaultEditor(String.class, new DefaultCellEditor( new JTextField()));
    jtable.setDefaultEditor(Boolean.class, new DefaultCellEditor( new JCheckBox()));

      // UI
    setLayout(new BorderLayout());
    scrollPane = new JScrollPane(jtable);
    add(scrollPane, BorderLayout.CENTER);

    if (header != null) {
      JLabel headerLabel;
      if (tooltip != null) {
        headerLabel = new JLabel(header, SwingConstants.CENTER) {
          public JToolTip createToolTip() {
            return new thredds.ui.MultilineTooltip();
          }
        };
        headerLabel.setToolTipText(tooltip);
      } else {
        headerLabel = new JLabel(header, SwingConstants.CENTER);
      }
      add(headerLabel, BorderLayout.NORTH);
    }

    if (canAddDelete) {
        // button panel
      JPanel buttPanel = new JPanel();
      JButton newButton = new JButton("New");
      buttPanel.add(newButton, null);
      JButton deleteButton = new JButton("Delete");
      buttPanel.add(deleteButton, null);

      add(buttPanel, BorderLayout.SOUTH);

        // button listeners
      newButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          try {
            Object bean = beanClass.newInstance();
            addBean( bean);
          } catch ( Exception e) {
            e.printStackTrace();
          }

        }
      });

      deleteButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (JOptionPane.showConfirmDialog(null, "Do you want to delete all selected records",
            "Delete Records", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
              return;

          Iterator iter = getSelectedBeans().iterator();
          while (iter.hasNext()) {
            beans.remove( iter.next());
          }
          model.fireTableDataChanged();
        }
      });
    }

    // event management
    listenerList = new EventListenerList();

    ListSelectionModel rowSM = jtable.getSelectionModel();
    rowSM.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;  //Ignore extra messages.
        ListSelectionModel lsm = (ListSelectionModel)e.getSource();
        if (!lsm.isSelectionEmpty())
          fireEvent( e);
      }
    });

  }

  // debug
  public String getToolTipText(MouseEvent event) {
    String text = super.getToolTipText(event);
    System.out.println("BeanTable tooltip "+text);
    return text;
  }

  public void setProperty( String propertyName, String displayName, String toolTipText) {
    model.setProperty( propertyName, displayName, toolTipText);
  }

  public void setPropertyEditable( String propertyName, boolean isHidden) {

  }
  public void setPropertyHidden( String propertyName, boolean isHidden) {

  }

  /** Add listener: ListSelectionEvent sent when a new row is selected */
  public void addListSelectionListener(ListSelectionListener l) {
    listenerList.add(javax.swing.event.ListSelectionListener.class, l);
  }
  /** Remove listener */
  public void removeListSelectionListener(ListSelectionListener l) {
    listenerList.remove(javax.swing.event.ListSelectionListener.class, l);
  }

  private void fireEvent(javax.swing.event.ListSelectionEvent event) {
    Object[] listeners = listenerList.getListenerList();
    // Process the listeners last to first
    for (int i = listeners.length-2; i>=0; i-=2)
      ((javax.swing.event.ListSelectionListener)listeners[i+1]).valueChanged(event);
  }

  // so it can be ovverriden in BeanTableSorted
  protected int modelIndex(int viewIndex) {
    return viewIndex;
  }
  protected int viewIndex(int rowIndex) {
    return rowIndex;
  }

  /**
   * Get the currently selected bean, or null if none selected.
   * @return the currently selected bean, or null if none selected
   */
  public Object getSelectedBean() {
    int r = jtable.getSelectedRow();
    if ((r < 0) || (r >= beans.size())) return null;
    r = modelIndex(r);
    return (r < 0) || (r >= beans.size()) ? null : beans.get(r);
  }

  /**
   * Get the currently selected beans. Use this for multiple selection
   * @return ArrayList of currently selected beans (wont be null).
   * @see #setSelectionMode
   */
  public ArrayList getSelectedBeans() {
    ArrayList list = new ArrayList();
    int[] r = jtable.getSelectedRows();
    for (int i=0; i<r.length;i++) {
      int mr = modelIndex(r[i]);
      list.add( beans.get(mr));
      if (debugSelected) System.out.println(" bean selected= "+mr+" "+beans.get(mr) );
    }
     return list;
  }

  /**
   * Get the currently selected cells.
   * Use this for multiple row selection, when columnSelection is on
   * @return ArrayList of currently selected cells (wont be null).
   * @see BeanTable#setSelectionMode(int).
   */
  public ArrayList getSelectedCells() {
    ArrayList list = new ArrayList();
    int[] r = jtable.getSelectedRows();
    int[] c = jtable.getSelectedColumns();
    for (int i=0; i<r.length;i++)
      for (int j=0; i<c.length;j++) {
        int mr = modelIndex(r[i]);
        list.add( model.getValueAt(mr, c[j]));
      }

    return list;
  }

  /**
   * Set the currently selected cells (0, false or null).
   * Use this for multiple row selection, when columnSelection is on
   */
  public void clearSelectedCells() {
    int[] r = jtable.getSelectedRows();
    int[] c = jtable.getSelectedColumns();
    TableColumnModel tcm = jtable.getColumnModel();

    for (int j=0; j<c.length; j++) {
      TableColumn tc = tcm.getColumn(c[j]);
      int colModelIdx = tc.getModelIndex();

      Class colClass = jtable.getColumnClass(c[j]);
      //System.out.println("colClass "+colClass.getName());
      Object zeroValue = model.zeroValue( colClass);
      for (int i=0; i<r.length;i++) {
        int mr = modelIndex(r[i]);
        //System.out.println("clear "+r[i]+" "+colModelIdx+" "+zeroValue);
        model.setValueAt(zeroValue, mr, colModelIdx);
      }
    }
  }

  public void addBean( Object bean) {
    beans.add( bean);
    int row = beans.size()-1;
    model.fireTableRowsInserted(row, row);
  }

  public void addBeans( ArrayList newBeans) {
    this.beans.addAll(newBeans);
    int row = beans.size()-1;
    model.fireTableRowsInserted(row - newBeans.size(), row);  }

  public void setBeans( List beans) {
    if (beans == null)
      beans = new ArrayList(); // empty list
    this.beans = beans;
    model.fireTableDataChanged(); // this should make the jtable update
    revalidate();  // LOOK soemtimes it doesnt, ttry this
  }

  public List getBeans( ) { return beans; }
  public JTable getJTable( ) { return jtable; }

  public void setFontSize( int size) {
    jtable.setFont( jtable.getFont().deriveFont( (float) size));
  }

  /**
   * Set the selection mode on the JTable
   * @see JTable#setSelectionMode
   * @param mode : JTable.setSelectionMode
   */
  public void setSelectionMode( int mode) {
    jtable.setSelectionMode(mode);
  }

  /**
   * Set which row is selected.
   * @param bean select this one; must be in the list.
   */
  public void setSelectedBean( Object bean) {
    if (bean == null) return;
    int row = beans.indexOf( bean);
    int vr = viewIndex(row);
    if (row >= 0)
      jtable.getSelectionModel().setSelectionInterval(vr, vr);
    makeRowVisible( row);
  }

  public void clearSelection() {
    jtable.getSelectionModel().clearSelection();
  }

  /**
   * Set which rows are selected.
   * must also call setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
   * @param want select these
   */
  public void setSelectedBeans( List want) {
    jtable.getSelectionModel().clearSelection();
    for (int i = 0; i < want.size(); i++) {
      Object bean =  want.get(i);
      int row = beans.indexOf( bean);
      if (row >= 0) {
        int vr = viewIndex(row);
        jtable.getSelectionModel().addSelectionInterval(vr, vr);
      }
    }
  }

  private void makeRowVisible(int modelRow) {
    int row = viewIndex(modelRow);
    Rectangle visibleRect = jtable.getCellRect(row, 0, true);
    if (debugSelected) System.out.println("----ensureRowIsVisible = "+visibleRect);
    if (visibleRect != null)  {
      visibleRect.x = scrollPane.getViewport().getViewPosition().x;
      jtable.scrollRectToVisible(visibleRect);
      jtable.repaint();
    }
  }

  public void refresh() {
    jtable.repaint();
  }

  /**
   * Set the ColumnSelection is allowed (default false)
   * @param b allowed or not
   */
  public void setColumnSelectionAllowed( boolean b) {
    jtable.setColumnSelectionAllowed(b);
  }

  /**
   * Save state to the PreferencesExt.
   */
  public void saveState(boolean saveData) {
    if (store == null)
      return;

    try {
      // save data
      if (saveData) {
        store.putBeanCollection("beanList", beans);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    // save column widths and order
    ArrayList pcols = new ArrayList();
    TableColumnModel tcm = jtable.getColumnModel();
    for (int i=0; i<tcm.getColumnCount(); i++) {
      TableColumn tc = tcm.getColumn(i);
      PropertyCol pcol = new PropertyCol();
      pcol.setName( model.getColumnName(tc.getModelIndex()) );
      pcol.setWidth( tc.getWidth());
      pcol.setWidth( tc.getWidth());
      pcols.add ( pcol);
    }
    store.putBeanCollection( "propertyCol", pcols);
  }

  //public void repaint() {
 //   jtable.repaint();
  //}

  /**
    Restore state from PreferencesExt
  */
  protected void restoreState() {
    if (store == null)
      return;

    // restore column widths and order
    // tricky if cols have been added or deleted

    // process in the stored order
    ArrayList pcols = (ArrayList) store.getBean( "propertyCol", new ArrayList());
    TableColumnModel tcm = jtable.getColumnModel();
    int count = 0;
    for (int i=0; i<pcols.size(); i++) {
      PropertyCol pcol = (PropertyCol) pcols.get(i);

      int idx = model.getPropertyIndex( pcol.getName());
      if (idx >= 0) {  // still exists
        if (debugStore) System.out.println(count+"  has "+pcol.getName());
        TableColumn tc = tcm.getColumn(count++);
        tc.setModelIndex( idx);
        tc.setPreferredWidth( pcol.getWidth());
        tc.setHeaderValue( pcol.getName());
        tc.setIdentifier( pcol.getName());

      } else { //  property was deleted
        if (debugStore) System.out.println(count+"  col deleted "+pcol.getName());
      }
    }

        // see if there are any new properties that have been added
    if (model.getColumnCount() > count) {
      for (int col=0; col<model.getColumnCount(); col++) {
        if (!model.wasUsed(col)) {
          TableColumn tc = tcm.getColumn(count++);
          tc.setModelIndex( col);
          tc.setHeaderValue( model.getColumnName(col));
          tc.setIdentifier( model.getColumnName(col));
          if (debugStore) System.out.println(count+"  added "+model.getColumnName(col));
        }
      }
    }
  }

  /**
   * Should be private. This is to store the width for each table column.
   */
  static public class PropertyCol {
    private String name;
    private int modelIndex, width;

    public PropertyCol() { }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }
  }

  /** Renderer for Date type */
  static class DateRenderer extends DefaultTableCellRenderer {
    private java.text.SimpleDateFormat newForm, oldForm;
    private Date cutoff;

    DateRenderer() {
      oldForm = new java.text.SimpleDateFormat("yyyy MMM dd HH:mm z");
      oldForm.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
      newForm = new java.text.SimpleDateFormat("MMM dd, HH:mm z");
      newForm.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
      Calendar cal = Calendar.getInstance();
      cal.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
      cal.add(Calendar.YEAR, -1); // "now" time format within a year
      cutoff = cal.getTime();
    }

    public void setValue(Object value) {
      if (value == null)
        setText("");
      else {
        Date date = (Date) value;
        if (date.before(cutoff))
          setText(oldForm.format(date));
        else
          setText(newForm.format(date));
      }
    }
  }

  /** Does the reflection on the bean objects */
  protected class TableBeanModel extends AbstractTableModel {
    private ArrayList properties = new ArrayList(); // array of PropertyDescriptor
    private boolean[] used;

    protected TableBeanModel( Class beanClass) {

      // get bean info
      BeanInfo info = null;
      try {
        if (!beanClass.isInterface())
          info = Introspector.getBeanInfo(beanClass, Object.class);
        else
          info = Introspector.getBeanInfo(beanClass);  // allows interfaces to be beans
      } catch (IntrospectionException e) {
        e.printStackTrace();
      }

      if (debugBean)
        System.out.println( "Bean "+beanClass.getName());

      // see if editableProperties method exists
      String editableProperties = "";
      MethodDescriptor[] mds = info.getMethodDescriptors();
      for (int i=0; i< mds.length; i++) {
        Method m = mds[i].getMethod();
        if (m.getName().equals("editableProperties")) {
          try {
            editableProperties = (String) m.invoke( null, (Object []) null);
            if (debugBean) System.out.println(" editableProperties: "+editableProperties);
          } catch (Exception ee) {
            System.out.println("BeanTable: Bad editableProperties ");
            ee.printStackTrace();
          }
        }
      }

      // see if hiddenProperties method exists
      String hiddenProperties = "";
      for (int i=0; i< mds.length; i++) {
        Method m = mds[i].getMethod();
        if (m.getName().equals("hiddenProperties")) {
          try {
            hiddenProperties = (String) m.invoke( null, (Object []) null);
            if (debugBean) System.out.println(" hiddenProperties: "+hiddenProperties);
          } catch (Exception ee) {
            System.out.println("BeanTable: Bad hiddenProperties ");
            ee.printStackTrace();
          }
        }
      }

      // properties must have read method, not be hidden
      PropertyDescriptor[] pds = info.getPropertyDescriptors();
      for (int i=0; i< pds.length; i++) {
        if ((pds[i].getReadMethod() != null) && !isHidden(pds[i], hiddenProperties)) {
          properties.add( pds[i]);
          // preferred == editable
          setEditable( pds[i], editableProperties);
        }
      }

      if (debugBean) {
        System.out.println( "Properties:");
        System.out.println( "  display name  type   read()       write()         editable");
         for (int i=0; i< pds.length; i++) {
          String displayName = pds[i].getDisplayName();
          String name = pds[i].getName();
          Class type = pds[i].getPropertyType();
          Method rm = pds[i].getReadMethod();
          Method wm = pds[i].getWriteMethod();
          System.out.println( "  "+displayName+" "+name+" "+type.getName()+" "+rm+" "+wm+" "+pds[i].isPreferred());
        }
      }

      used = new boolean[ properties.size()];
    }

    public void setProperty( String propertyName, String displayName, String toolTipText) {
      PropertyDescriptor pd = getProperty( propertyName);
      if (pd != null) {


        if (displayName != null) {
          pd.setDisplayName( displayName);
          JLabel hl =  (JLabel) pd.getValue("Header");
          if (hl != null) hl.setText( displayName);
          //System.out.println("setDisplayName <"+displayName+"> on "+propertyName);
        }
        if (toolTipText != null) {
          pd.setShortDescription( toolTipText);
          JComponent jc =  (JComponent) pd.getValue("ToolTipComp");
          if (jc != null) jc.setToolTipText (toolTipText);
          //System.out.println("setToolTipText <"+toolTipText+"> on "+propertyName);
        }
      } else
        System.out.println("BeanTable.setProperty "+ beanClass.getName()+" no property named "+
          propertyName);
    }


    // AbstractTableModel methods
    public int getRowCount() { return beans.size(); }
    public int getColumnCount() { return properties.size(); }
    public String getColumnName(int col) {
      return ((PropertyDescriptor)properties.get(col)).getDisplayName();
    }

    public Object getValueAt (int row, int col) {
      Object bean =  beans.get( row);
      Object value = "N/A";
      PropertyDescriptor pd = (PropertyDescriptor) properties.get(col);
      try {
        Method m = pd.getReadMethod();
        value = m.invoke( bean, (Object []) null);
      } catch (Exception ee) {
        System.out.println("BeanTable: Bad getReadMethod "+row+" "+col+" "+beanClass.getName()+" "+pd.getDisplayName());
        ee.printStackTrace();
      }

      return value;
    }

      // for BeanTableSorted
    public Object getValueAt (Object bean, int col) {
      Object value = "N/A";
      try {
        Method m = ((PropertyDescriptor)properties.get(col)).getReadMethod();
        value = m.invoke( bean, (Object []) null);
      } catch (Exception ee) {
        System.out.println("BeanTable: Bad Bean "+bean+" "+col+" "+beanClass.getName());
        ee.printStackTrace();
      }
      return value;
    }

      // editing

    public Class getColumnClass(int col) {
      Class c = wrapPrimitives(((PropertyDescriptor)properties.get(col)).getPropertyType());
      //System.out.println( " "+col+" colClass = "+c);
      //checkColumnRenderer( col);
      return c;
    }

    private void checkColumnRenderer( int col) {
      TableColumnModel colModel = jtable.getColumnModel();
      TableColumn tabCol = colModel.getColumn( col);
      TableCellEditor editor = tabCol.getCellEditor();
      TableCellRenderer render = tabCol.getCellRenderer();
      System.out.println(col+"  editor = "+((editor == null) ? "null" : editor.getClass().getName()));
      System.out.println(col+"  render = "+((render == null) ? "null" : render.getClass().getName()));
    }

    public boolean isCellEditable(int row, int col) {
      PropertyDescriptor pd = (PropertyDescriptor) properties.get(col);
      if (!pd.isPreferred()) return false;
      Class type = pd.getPropertyType();
      return type.isPrimitive() || (type == String.class);
    }

    public void setValueAt(Object value, int row, int col) {
      Object bean =  beans.get( row);
      try {
        Object[] params = new Object[1];
        params[0] = value;
        Method m = ((PropertyDescriptor)properties.get(col)).getWriteMethod();
        if (m != null)
          m.invoke( bean, params);
      } catch (Exception ee) {
        ee.printStackTrace();
      }

      fireTableCellUpdated( row, col);
    }

    // extra stuff

    protected Class wrapPrimitives( Class c) {
      if (c == boolean.class) return Boolean.class;
      else if (c == int.class) return Integer.class;
      else if (c == float.class) return Float.class;
      else if (c == double.class) return Double.class;
      else if (c == short.class) return Short.class;
      else if (c == long.class) return Long.class;
      else if (c == byte.class) return Byte.class;
      else return c;
    }

    protected Object zeroValue( Class c) {
      if (c == Boolean.class) return Boolean.FALSE;
      else if (c == Integer.class) return new Integer(0);
      else if (c == Float.class) return new Float(0.0);
      else if (c == Double.class) return new Double(0.0);
      else if (c == Short.class) return new Short((short)0);
      else if (c == Long.class) return new Long(0);
      else if (c == Byte.class) return new Byte((byte)0);
      else return null;
    }



    // return model index with this property name, return -1 if not exists
    protected int getPropertyIndex( String wantName) {
      for (int i=0; i< properties.size(); i++) {
        String name = ((PropertyDescriptor)properties.get(i)).getName();
        if (name.equals(wantName)) {
          used[i] = true;
          return i;
        }
      }
      return -1;
    }

    // return PropertyDescriptor with this property name, return null if not exists
    protected PropertyDescriptor getProperty( String wantName) {
      for (int i=0; i< properties.size(); i++) {
        PropertyDescriptor p = (PropertyDescriptor) properties.get(i);
        if (p.getName().equals(wantName))
          return p;
      }
      return null;
    }

    // return PropertyDescriptor
    protected PropertyDescriptor getProperty( int idx) {
      return (PropertyDescriptor) properties.get(idx);
    }

    protected boolean wasUsed(int col) { return used[col]; }

    private ArrayList editP = null;
    private void setEditable( PropertyDescriptor pd, String editableProperties) {
      if (editP == null) {
        editP = new ArrayList();
        StringTokenizer toke = new StringTokenizer( editableProperties);
        while (toke.hasMoreTokens())
          editP.add( toke.nextToken());
      }

      pd.setPreferred( editP.contains( pd.getName()));
    }

    private ArrayList hiddenP = null;
    private boolean isHidden( PropertyDescriptor pd, String hiddenProperties) {
      if (hiddenP == null) {
        hiddenP = new ArrayList();
        StringTokenizer toke = new StringTokenizer( hiddenProperties);
        while (toke.hasMoreTokens())
          hiddenP.add( toke.nextToken());
      }

      return hiddenP.contains( pd.getName());
    }

  }

  protected class HeaderRenderer implements TableCellRenderer {
    protected int modelIdx;
    protected JPanel compPanel;
    protected JLabel headerLabel;

    protected HeaderRenderer(int modelIdx) {
      this.modelIdx = modelIdx;

      java.beans.PropertyDescriptor pd = model.getProperty(modelIdx);
      headerLabel = new JLabel( pd.getDisplayName());

      compPanel = new JPanel(new BorderLayout());
      compPanel.setBorder(new BevelBorder(BevelBorder.RAISED));
      compPanel.add(headerLabel, BorderLayout.CENTER);

      compPanel.setToolTipText( pd.getShortDescription());
      ToolTipManager.sharedInstance().registerComponent( compPanel);

      // store the label for calls to BeanTable.setProperty()
      pd.setValue("Header", headerLabel);
      pd.setValue("ToolTipComp", compPanel);
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
      boolean isSelected, boolean hasFocus, int row, int column) {
      //System.out.println("BeanTable.getTableCellRendererComponent "+compPanel.getToolTipText());
      return compPanel;
    }

  }


  // testing

  static public class TestBean {
    private String name, path, sbase, dtype, stype, ddhref;
    private boolean u;
    private int i;
    private Date now = new Date();

    public TestBean() { }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    // should not be editable
    public String getPath() { return path; }
    void setPath(String path) { this.path = path; }

    // should not appear
    String getServerBase() { return sbase; }
    void setServerBase(String sbase) { this.sbase = sbase; }

    // should not appear
    String getDataType() { return dtype; }
    public void setDataType(String dtype) { this.dtype = dtype; }

    public String getServerType() { return stype; }
    public void setServerType(String stype) { this.stype = stype; }

    public String getDDref() { return ddhref; }
    public void setDDref(String ddhref) { this.ddhref = ddhref; }

    public boolean getUse() { return u; }
    public void setUse(boolean u) { this.u = u; }

    public int getII() { return i; }
    public void setII(int i) { this.i = i; }

    public Date getNow() { return now; }
    public void setNow(Date now) { this.now = now; }

    static public String editableProperties() { return "name path serverbase serverType DDref use II now"; }
  }

  public static void main2(String args[]) {
    TestBean testBean = new TestBean();
    //Field.Text fld = new Field.Text("test", "label", "def", null);
    Class beanClass = testBean.getClass();

    try {
      BeanInfo info = Introspector.getBeanInfo(beanClass, Object.class);
      System.out.println( "Bean "+beanClass.getName());

      System.out.println( "Properties:");
      PropertyDescriptor[] pd = info.getPropertyDescriptors();
      for (int i=0; i< pd.length; i++) {
        System.out.println( " "+pd[i].getName()+" "+pd[i].getPropertyType().getName());
        String propName = pd[i].getName();
        char first = Character.toUpperCase(propName.charAt(0));
        String method_name = "get"+first + propName.substring(1);
        System.out.println( " "+propName+" "+first+" "+method_name);

          try {
            java.lang.reflect.Method method = beanClass.getMethod( method_name, (Class []) null);
            System.out.println( " method = "+method);
          } catch (Exception e) {
            e.printStackTrace();
          }
      }

      /* System.out.println( "Methods:");
      MethodDescriptor[] md = info.getMethodDescriptors();
      for (int i=0; i< md.length; i++) {
        System.out.println( " "+md[i].getMethod()+" "+md[i].getParameterDescriptors());
      } */

    } catch (IntrospectionException e) {
      e.printStackTrace();
    }
  }

  public static void main(String args[]) throws java.io.IOException {
    final XMLStore xstore;
    PreferencesExt store;

    Date now = new Date();
    java.text.DateFormat formatter = java.text.DateFormat.getDateInstance();
    System.out.println( "date : "+now+" // "+formatter.format(now));


    xstore = XMLStore.createFromFile("C:/dev/prefs/test/data/testBeanTable.xml", null);
    store = xstore.getPreferences();
    final BeanTable bt = new BeanTable(TestBean.class, store, true, "header", "header\ntooltip");

    JFrame frame = new JFrame("Test BeanTable");
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        try {
          bt.saveState( true);
          xstore.save();
          System.exit(0);
        } catch( java.io.IOException ee) {
          ee.printStackTrace();
        }
      }
    });

    frame.getContentPane().add(bt);
    bt.setPreferredSize(new Dimension(500, 200));

    frame.pack();
    frame.setLocation(300, 300);
    frame.setVisible(true);
  }
}
