// $Id: JTableSorted.java 50 2006-07-12 16:30:06Z caron $
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
package thredds.viewer.ui.table;

import thredds.ui.BAMutil;
import thredds.viewer.ui.event.*;
import ucar.nc2.ui.util.ListenerManager;
import ucar.nc2.util.NamedObject;

import java.util.ArrayList;
import java.util.Iterator;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;  // LOOK

  /**
    JTableSorted adds sorting functionality to a JTable.
    It also wraps the JTable in a JScrollPane.
    Note that JTableSorted is a JPanel, and has-a JTable.
    It throws ListSelectionEvent events when the selection changes.
    It throws a UIChangeEvent, property = "sort" just before a sort is going to happen.
  */

public class JTableSorted extends JPanel {
      // for HeaderRenderer
  static private Icon sortDownIcon = BAMutil.getIcon( "SortDown", true);
  static private Icon sortUpIcon = BAMutil.getIcon( "SortUp", true);
  static private Icon threadSortIcon = BAMutil.getIcon( "ThreadSorted", true);
  static private Icon threadUnSortIcon = BAMutil.getIcon( "ThreadUnsorted", true);

  private ArrayList list;
  private String[] colName;
  private String objectName;

  private JTable jtable;
  private JScrollPane scrollPane;
  private TableRowModel model;
  private thredds.ui.PopupMenu popupMenu = null;
  private PopupAction[] acts;

  private boolean debug = false;
  private boolean sortOK = true;
  private ThreadSorter threadSorter = null;
  private int threadCol = -1;

  private ListenerManager lm;

  /**
    Constructor.

    @param String [] colName: list of column names
    @param ArrayList list : list of rows. This must contain objects that implement
      the TableRow interface. May be null or empty.
  */
  public JTableSorted(String[] colName, ArrayList listRT) {
    this( colName, listRT, false, null);
  }

  /**
    Constructor.

    @param String [] colName: list of column names
    @param ArrayList list : list of rows. This must contain objects that implement
      the TableRow interface. May be null or empty.
    @param boolean enableColumnManipulation : allow columns to be added, deleted via click-right popup
    @param ucar.unidata.ui.ThreadSorter threadSorter : if not null, add a "thread sorting" column

  */
  public JTableSorted(String[] columnName, ArrayList listRT, boolean enableColumnManipulation,
    ThreadSorter threadSorter) {
    this.colName = columnName;
    this.list = (listRT == null) ? new ArrayList() : listRT;
    this.threadSorter = threadSorter;

      // create the ui
    jtable = new JTable();
    jtable.setDefaultRenderer(Object.class, new MyTableCellRenderer() );
    ToolTipManager.sharedInstance().registerComponent(jtable);

    model = new TableRowModel();

    setLayout(new BorderLayout());
    scrollPane = new JScrollPane(jtable);
    add(scrollPane, BorderLayout.CENTER);
    //add(jtable, BorderLayout.CENTER);

      // add a column if it has a ThreadSorter
    boolean hasThreads = (threadSorter != null);
    if (hasThreads) {
      String[] newColName = new String[ colName.length + 1];
      for (int i=0; i<colName.length; i++)
        newColName[i] = colName[i];

      threadCol = colName.length;
      newColName[threadCol] = "Threads";
      colName = newColName;
    }

    jtable.setModel( model);
    jtable.setSelectionMode( ListSelectionModel.SINGLE_SELECTION);
    jtable.setAutoResizeMode( JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
    setColumnWidths( null);

        // now set the header renderers
    TableColumnModel tcm = jtable.getColumnModel();
    int ncolwt = hasThreads ? jtable.getColumnCount()-1 : jtable.getColumnCount();
    for (int i=0; i<ncolwt; i++) {
      TableColumn tc = tcm.getColumn(i);
      tc.setHeaderRenderer( new SortedHeaderRenderer(colName[i], i));
    }
    if (hasThreads) {
      TableColumn tc = tcm.getColumn(ncolwt);
      tc.setHeaderRenderer( new ThreadHeaderRenderer(ncolwt));
    }

    if (enableColumnManipulation) {
      // popupMenu
      popupMenu = new thredds.ui.PopupMenu(jtable.getTableHeader(), "Visible");
      int ncols = colName.length;
      acts = new PopupAction[ncols];
      for (int i=0; i<ncols; i++) {
        acts[i] = new PopupAction(colName[i]);
        popupMenu.addActionCheckBox( this.colName[i], acts[i], true);
      }
    }

    // set sorting behavior
    JTableHeader hdr = jtable.getTableHeader();
    hdr.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if (!sortOK)
          return;
        TableColumnModel tcm2 = jtable.getColumnModel();
        int colIdx = tcm2.getColumnIndexAtX(e.getX());
        int colNo = jtable.convertColumnIndexToModel(colIdx);

          // keep current selection selected
        int selidx = jtable.getSelectedRow();
        Object selected = null;
        if (selidx >= 0)
          selected = list.get( selidx);

          // notify listsners of impending sort
        if (lm.hasListeners())
          lm.sendEvent( new UIChangeEvent(this, "sort", null, new Integer(colNo)));

          // sort
        model.sort(colNo);

          /* keep current selection selected */
        if (selidx >= 0) {
          int newSelectedRow = list.indexOf( selected);
          jtable.setRowSelectionInterval(newSelectedRow, newSelectedRow);
          ensureRowIsVisible(newSelectedRow);
        }
        repaint();
      }
    });

    // event manager
    lm = new ListenerManager(
        "thredds.viewer.ui.event.UIChangeListener",
        "thredds.viewer.ui.event.UIChangeEvent",
        "processChange");

  }

  /**
    Set the state from the last saved in the PersistentStore.
    @param String name  object name
    @param PersistentStore store ok if null or empty
  *
  public void restoreStateFromStore(String objectName, PersistentStore store) {
    if (store == null)
      return;
    this.objectName = objectName;

    int [] modelIndex = (int []) store.get(objectName+"ColumnOrder" );
    if (modelIndex == null)
      return;

    // make invisible any not stored
    int ncols = jtable.getColumnCount();
    boolean [] visible = new boolean[ncols];
    for (int i=0; i<modelIndex.length; i++)
      if (modelIndex[i] < ncols)
        visible[ modelIndex[i]] = true;
    for (int i=0; i<ncols; i++)
      if (!visible[i]) {
        //System.out.println( colName[i]+" hide "+i);
        acts[i].hideColumn();
        acts[i].putValue(BAMutil.STATE, new Boolean(false));
      }

    // now set the header order
    TableColumnModel tcm = jtable.getColumnModel();
    int n = Math.min( modelIndex.length, jtable.getColumnCount());
    for (int i=0; i<n; i++) {
      TableColumn tc = tcm.getColumn(i);
      tc.setModelIndex(modelIndex[i]);
      String name = colName[modelIndex[i]];
      tc.setHeaderValue(name);
      tc.setIdentifier(name);
      if (modelIndex[i] == threadCol)
        tc.setHeaderRenderer( new ThreadHeaderRenderer(threadCol));
      else
        tc.setHeaderRenderer( new SortedHeaderRenderer(name, modelIndex[i]));
    }

    // set the column widths
    int [] size = (int []) store.get(objectName+"ColumnWidths" );
    if (size != null)
      setColumnWidths( size);
    if (debug) {
      System.out.println(objectName+" read widths = ");
      for (int i=0; i<size.length; i++)
        System.out.print(" "+size[i]);
      System.out.println();
    }

    if ( null != store.get( objectName+"SortOnCol")) {
      model.sortCol = ((Integer) store.get( objectName+"SortOnCol")).intValue();
      model.reverse = ((Boolean) store.get( objectName+"SortReverse")).booleanValue();
      model.sort();
    }
  }

  public void setFontSize( int size) {
    jtable.setFont( jtable.getFont().deriveFont( (float) size));
  } */

  /**
    Save the state in the PersistentStore passed to getState().
  *
  public void saveState(PersistentStore store) {
    if (store == null)
      return;

    int ncols = jtable.getColumnCount();
    int [] size = new int[ncols];
    int [] modelIndex = new int[ncols];

    TableColumnModel tcm = jtable.getColumnModel();
    for (int i=0; i<ncols; i++) {
      TableColumn tc = tcm.getColumn(i);
      size[i] = tc.getWidth();
      modelIndex[i] = tc.getModelIndex();
    }
    store.put( objectName+"ColumnWidths", size);
    store.put( objectName+"ColumnOrder", modelIndex);

    store.put( objectName+"SortOnCol", new Integer(model.sortCol));
    store.put( objectName+"SortReverse", new Boolean(model.reverse));

    store.put( objectName+"SortReverse", new Boolean(model.reverse));


    if (debug) {
      System.out.println(objectName+" store widths = ");
      for (int i=0; i<size.length; i++)
        System.out.print(" "+size[i]);
      System.out.println();
    }

  } */

  /**
    Sort the rowList: note rowList changed, not a copy of it.
    @param int colNo sort on this column
    @param boolean reverse if true, reverse sort
  */
   public void sort(int colNo, boolean reverse) {
    model.sort(colNo, reverse);
    jtable.setRowSelectionInterval(0, 0);
    ensureRowIsVisible(0);
  }

  /**
    Replace the rowList with this one.
    @param ArrayList rowList
  */
  public void setList ( ArrayList rowList) {
    this.list = rowList;
    if (list.size() > 0)
      jtable.setRowSelectionInterval(0, 0);
    else
      jtable.clearSelection();

    model.sort();
    jtable.revalidate();
  }

  /**
    Remove elem from rowList, update the table.
    Searches for match using object identity (==)
    @param Object elem
  */
  public void removeRow ( Object elem) {
    Iterator iter = list.iterator();
    while (iter.hasNext()) {
      Object row = iter.next();
      if (row == elem) {
        iter.remove();
        break;
      }
    }
    jtable.revalidate();
  }

  /** add ListSelectionEvent listener */
  public void addListSelectionListener( ListSelectionListener l) {
    jtable.getSelectionModel().addListSelectionListener(l);
  }
  /** remove ListSelectionEvent listener */
  public void removeListSelectionListener( ListSelectionListener l) {
    jtable.getSelectionModel().removeListSelectionListener(l);
  }

  /** add UIChangeEvent listener */
  public void addUIChangeListener( UIChangeListener l) {
    lm.addListener(l);
  }
  /** remove UIChangeEvent listener */
  public void removeUIChangeListener( UIChangeListener l) {
    lm.removeListener(l);
  }

  // public int getRowCount() { return table.getRowCount(); }
  public int getSelectedRowIndex() { return jtable.getSelectedRow(); }   // for SuperComboBox
  public void setSortOK(boolean sortOK) { this.sortOK = sortOK; }       // for SuperComboBox

  /**
    Get the currently selected row.
    @return selected TableRow
  */
  public TableRow getSelected() {
    if (list.size() == 0)
      return null;
    int sel = jtable.getSelectedRow();
    if (sel >= 0)
      return (TableRow) list.get(sel);
    else
      return null;
  }

  /**
    Set the current selection to this row.
    @param int row index into rowList
  */
  public void setSelected(int row) {
    if ((row < 0) || (row >= list.size()))
      return;
    if (debug) System.out.println("JTableSorted setSelected "+row);
    jtable.setRowSelectionInterval(row, row);
    ensureRowIsVisible(row);
  }

  /**
    Increment or decrement the current selection by one row.
    @param boolean increment true=increment, false=decrement
  */
  public void incrSelected(boolean increment) {
    if (list.size() == 0)
      return;
    int curr = jtable.getSelectedRow();
    if (increment && (curr < list.size()-1))
      setSelected(curr+1);
    else if (!increment && (curr > 0))
      setSelected(curr-1);
  }

  /** Get the JTable delegate so you can do nasty things to it */
  public JTable getTable() { return jtable; }

  /** for each column, get the model index */
  public int[] getModelIndex() {
    int [] modelIndex = new int[colName.length];

    TableColumnModel tcm = jtable.getColumnModel();
    for (int i=0; i<colName.length; i++) {
      TableColumn tc = tcm.getColumn(i);
      modelIndex[i] = tc.getModelIndex();
    }

    return modelIndex;
  }


/////////////////////////////////////////////////////////////////////////////////
  private void ensureRowIsVisible(int nRow) {
    Rectangle visibleRect = jtable.getCellRect(nRow, 0, true);
    if (visibleRect != null)  {
      visibleRect.x = scrollPane.getViewport().getViewPosition().x;
      jtable.scrollRectToVisible(visibleRect);
      jtable.repaint();
    }
  }

  private void setColumnWidths(int[] sizes) {
    TableColumnModel tcm = jtable.getColumnModel();
    for (int i=0; i< jtable.getColumnCount(); i++) {
      TableColumn tc = tcm.getColumn(i);
      int maxw = ((sizes == null) || (i >= sizes.length))  ?
        model.getPreferredWidthForColumn(tc) : sizes[i];
      tc.setPreferredWidth(maxw);
    }
    //table.sizeColumnsToFit(0);     //  must be called due to a JTable bug
  }

  private void setSortCol( int sortCol, boolean reverse) {
    TableColumnModel tcm = jtable.getColumnModel();
    for (int i=0; i<jtable.getColumnCount(); i++) {
      TableColumn tc = tcm.getColumn(i);
      SortedHeaderRenderer shr = (SortedHeaderRenderer) tc.getHeaderRenderer();
      shr.setSortCol( sortCol, reverse);
    }
  }

  private class PopupAction extends AbstractAction {
    private String id;
    private TableColumn tc = null;

    PopupAction(String id) { this.id = id; }

    public void actionPerformed(ActionEvent e) {
      boolean state = ((Boolean) getValue(BAMutil.STATE)).booleanValue();
      TableColumnModel tcm = jtable.getColumnModel();

      if (state)
        tcm.addColumn(tc);
      else
        hideColumn();

      JTableSorted.this.revalidate();
      //System.out.println(id+" "+state);
    }

    public void hideColumn() {
      TableColumnModel tcm = jtable.getColumnModel();
      int idx = tcm.getColumnIndex(id);
      tc = tcm.getColumn(idx);
      tcm.removeColumn(tc);
    }
  }

  private class TableRowModel extends AbstractTableModel {
    private boolean reverse = false;
    private int sortCol = -1;

    // AbstractTableModel methods
    public int getRowCount() { return list.size(); }
    public int getColumnCount() { return colName.length; }
    public String getColumnName(int col) { return colName[col]; }
    public Object getValueAt (int row, int col) {
      TableRow selectedRow = (TableRow) list.get( row);

      if (col == threadCol) {
        if (null == threadSorter)
          return "";
        else
          return threadSorter.isTopThread(selectedRow) ? " * " : ""; // ??
      }

      return selectedRow.getValueAt(col);
    }
      // sort using current
    void sort() {
      sort(sortCol, reverse);
    }

    void sort( int sortCol) {
      if (sortCol == this.sortCol)
        reverse = (!reverse);
      else
        reverse = false;
      sort(sortCol, reverse);
    }

    void sort( int sortCol, boolean reverse) {
      this.reverse = reverse;
      if ((sortCol == threadCol) && (threadSorter != null)) {
        list = threadSorter.sort( sortCol, reverse, list);
      } else if (sortCol >= 0) {
        java.util.Collections.sort( list, new SortList(sortCol, reverse));
      }
      JTableSorted.this.setSortCol( sortCol, reverse);
      this.sortCol = sortCol; // keep track of last sort
    }

      // trying to get the auto - size right: not very successful
    public int getPreferredWidthForColumn(TableColumn col) {
      int hw = columnHeaderWidth(col);   // hw = header width
      int cw = widestCellInColumn(col);  // cw = column width

      return hw > cw ? hw : cw;
    }
    private int columnHeaderWidth(TableColumn col) {
      TableCellRenderer renderer = col.getHeaderRenderer();
      if (renderer == null) return 10;
      Component comp = renderer.getTableCellRendererComponent(
        jtable, col.getHeaderValue(), false, false, 0, 0);

      return comp.getPreferredSize().width;
    }
    private int widestCellInColumn(TableColumn col) {
      int c = col.getModelIndex(), width=0, maxw=0;

      for(int r=0; r < getRowCount(); ++r) {
        TableCellRenderer renderer = jtable.getCellRenderer(r,c);
        Component comp = renderer.getTableCellRendererComponent(
                jtable, getValueAt(r,c), false, false, r, c);
        width = comp.getPreferredSize().width;
              maxw = width > maxw ? width : maxw;
      }
      return maxw;
    }
  }

  private class SortList implements java.util.Comparator {
    private int col;
    private boolean reverse;

    SortList( int col, boolean reverse) {
      this.col = col;
      this.reverse = reverse;
    }

    public int compare(Object o1, Object o2) {
      TableRow row1 = (TableRow) o1;
      TableRow row2 = (TableRow) o2;
      return reverse ? row2.compare(row1, col) : row1.compare(row2, col);
    }

    public boolean equals(Object obj) { return this == obj; }
  }

  // add tooltips
  private class MyTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {

    public Component getTableCellRendererComponent( JTable table, Object value, boolean isSelected,
        boolean hasFocus, int row, int column) {

      Component c = super.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column);

      // System.out.println(" MyTableCellRenderer comp = "+c.hashCode()+" "+c.getClass().getName());
      if ((c instanceof JComponent) && (value instanceof NamedObject)) {
        ((JComponent)c).setToolTipText(((NamedObject)value).getDescription());
      } // LOOK!! should turn tip off if there is none !!
      return c;
    }

    public Point getToolTipLocation(MouseEvent e) {
      System.out.println(" cellR getToolTipLocation "+e.getPoint());
      return e.getPoint();
    }
  }

    // add tooltips
  private class MyJTable extends javax.swing.JTable {

    public Point getToolTipLocation(MouseEvent e) {
      // System.out.println(" JTable getToolTipLocation "+e.getPoint());
      return e.getPoint();
    }
  }


  private class SortedHeaderRenderer implements TableCellRenderer {
    int modelCol;
    Component comp;

    JPanel compPanel;
    JLabel upLabel, downLabel;
    boolean hasSortIndicator = false;
    boolean reverse = false;

    protected SortedHeaderRenderer(int modelCol) {
      this.modelCol = modelCol;
    }

    SortedHeaderRenderer(String name, int modelCol) {
      this.modelCol = modelCol;
      upLabel = new JLabel(sortUpIcon);
      downLabel = new JLabel(sortDownIcon);

      compPanel = new JPanel(new BorderLayout());
      compPanel.setBorder(new BevelBorder(BevelBorder.RAISED));
      compPanel.add(new JLabel( name), BorderLayout.CENTER);
      comp = compPanel;
    }

    void setSortCol( int sortCol, boolean reverse) {
      if (sortCol == modelCol) {

        if (!hasSortIndicator)
          compPanel.add(reverse? upLabel : downLabel, BorderLayout.EAST);
        else if (reverse != this.reverse) {
          compPanel.remove(1);
          compPanel.add(reverse? upLabel : downLabel, BorderLayout.EAST);
        }

        this.reverse = reverse;
        hasSortIndicator = true;

        //System.out.println("setSortCol on "+modelCol+" "+sortCol+" "+reverse);
      } else if (hasSortIndicator) {
        compPanel.remove(1);
        hasSortIndicator = false;
        //System.out.println("setSortCol off "+modelCol+" "+sortCol+" "+reverse);
      }
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
      boolean isSelected, boolean hasFocus, int row, int column) {

      return comp;
    }

  }

  private class ThreadHeaderRenderer extends SortedHeaderRenderer {
    JLabel threadHead;
    JPanel sort, unsort;

    ThreadHeaderRenderer(int modelCol) {
      super( modelCol);

      sort = new JPanel(new BorderLayout());
      sort.setBorder(new BevelBorder(BevelBorder.RAISED));
      sort.add(new JLabel( threadSortIcon), BorderLayout.CENTER);

      unsort = new JPanel(new BorderLayout());
      unsort.setBorder(new BevelBorder(BevelBorder.RAISED));
      unsort.add(new JLabel( threadUnSortIcon), BorderLayout.CENTER);

      comp = unsort;
    }

   void setSortCol( int sortCol, boolean reverse) {
      comp = (sortCol == modelCol) ? sort : unsort;
    }

  }

}
