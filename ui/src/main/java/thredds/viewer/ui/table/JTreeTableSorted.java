// $Id: JTreeTableSorted.java 50 2006-07-12 16:30:06Z caron $
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
import thredds.ui.MyMouseAdapter;
import ucar.util.prefs.PreferencesExt;
import ucar.nc2.ui.util.ListenerManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;

import javax.swing.tree.TreePath;

  /**
    JTreeTableSorted adds sorting functionality to a JTreeTable.
    JTreeTable is a class that combines a JTable with a JTree.
    Note that JTreeTableSorted is a JPanel, and has-a JTreeTable.
    It throws ListSelectionEvent events when the selection changes.
    It throws a UIChangeEvent, property = "sort" just before a sort is going to happen.
  */

public class JTreeTableSorted extends JPanel {
      // for HeaderRenderer
  static private Icon sortDownIcon = BAMutil.getIcon( "SortDown", true);
  static private Icon sortUpIcon = BAMutil.getIcon( "SortUp", true);
  static private Icon threadSortIcon = BAMutil.getIcon( "ThreadSorted", true);
  static private Icon threadUnSortIcon = BAMutil.getIcon( "ThreadUnsorted", true);

    // main stuff
  private JTreeTable table;
  private TreeTableModelSorted model;

  private ThreadHeaderRenderer threadHeaderRenderer = null;
  private int threadCol = -1;
  private TableRow selectedRow;

  private JScrollPane scrollPane;
  private thredds.ui.PopupMenu popupMenu = null;
  private PopupAction[] acts;

  private boolean treeSort;
  private boolean useThreads;

  private ListenerManager lm;
  private ListSelectionEvent listSelectionEvent = null;

  private MouseAdapter allowSortColChangeMouseListener;
  private boolean allowSortColChange = false;

  private boolean debug = false, debugSetPath = false, debugEvent = false;

 /**
    Constructor.
    @param m TreeTableModelSorted m
  */
  public JTreeTableSorted(TreeTableModelSorted m, boolean allowSortColChange) {

    this.model = m;
    this.useThreads = model.useThreads();
    this.treeSort = model.isTreeSort();

      // create the ui
    table = new JTreeTable(model);
    setLayout(new BorderLayout());
    scrollPane = new JScrollPane(table);
    add(scrollPane, BorderLayout.CENTER);

    //table.setSelectionMode( ListSelectionModel.SINGLE_SELECTION);
    table.setAutoResizeMode( JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
    //table.setFont( table.getFont().deriveFont( Font.BOLD));

        // now set the header renderers
    TableColumnModel tcm = table.getColumnModel();
    int ncolwt = useThreads ? table.getColumnCount()-1 : table.getColumnCount();
    for (int i=0; i<ncolwt; i++) {
      TableColumn tc = tcm.getColumn(i);
      tc.setHeaderRenderer( new SortedHeaderRenderer(model.getColumnName(i), i));
    }
    if (useThreads) {
      threadCol = ncolwt;
      threadHeaderRenderer = new ThreadHeaderRenderer(threadCol);
      tcm.getColumn(threadCol).setHeaderRenderer( threadHeaderRenderer);
    }

    // popupMenu
    popupMenu = new thredds.ui.PopupMenu(table.getTableHeader(), "Visible");
    int ncols = model.getColumnCount();
    acts = new PopupAction[ncols];
    for (int i=0; i<ncols; i++) {
      acts[i] = new PopupAction(model.getColumnName(i));
      popupMenu.addActionCheckBox( model.getColumnName(i), acts[i], true);
    }

    // listen for list selection
    table.getSelectionModel().addListSelectionListener( new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting() && lm.hasListeners() && (listSelectionEvent == null)) {
          listSelectionEvent = e;
          if (debugEvent) System.out.println(" JTreeTableSorted message selected = "+e);
          SwingUtilities.invokeLater(new Runnable() {  // gotta do this after the dust settles
            public void run() {
              lm.sendEvent( listSelectionEvent);
              listSelectionEvent = null;  // dont like this
            }
          }); // new Runnable
        }
      }
    }); // new ListSelectionListener

    // listen for mouse clicks on the column header
    allowSortColChangeMouseListener = new MyMouseAdapter() {
      public void click(MouseEvent e) {
        TableColumnModel tcm2 = table.getColumnModel();
        int colIdx = tcm2.getColumnIndexAtX(e.getX());
        int colNo = table.convertColumnIndexToModel(colIdx);

          // keep track of selection
        selectedRow = getSelectedRow();
        if (debug) System.out.println("----selectedRow = "+selectedRow);

        if (colNo == threadCol) { // toggle threads
          threadHeaderRenderer.setOn(!threadHeaderRenderer.isOn);
          model.setThreadsOn(threadHeaderRenderer.isOn);
          model.sort();
        } else {
          boolean reverse = model.sort(colNo);
          setSortCol( colNo, reverse);
        }

        table.fireDataChanged();
        invokeSetPath();
      }
    };
    allowSortColChange( allowSortColChange);

    // event manager for ListSelection
    lm = new ListenerManager(
        "javax.swing.event.ListSelectionListener",
        "javax.swing.event.ListSelectionEvent",
        "valueChanged");

    // default sort
    setSortCol( model.getSortCol(), model.getReverse());
  }

   /** add ListSelectionEvent listener */
  public void addListSelectionListener( ListSelectionListener l) {
    lm.addListener(l);
  }
  /** remove ListSelectionEvent listener */
  public void removeListSelectionListener( ListSelectionListener l) {
    lm.removeListener(l);
  }

  public void allowSortColChange( boolean allow) {
    JTableHeader hdr = table.getTableHeader();
    if (allow && !allowSortColChange)
      hdr.addMouseListener( allowSortColChangeMouseListener);
    else if (!allow && allowSortColChange)
      hdr.removeMouseListener( allowSortColChangeMouseListener);
    allowSortColChange = allow;
  }

  public TreeTableModelSorted getModel() { return model; }
  public JTable getTable() { return table; }

  public TableRow getRow( int row) {
    return model.getRow( table.getPathForRow(row));
  }

  /**
    Set the state from the last saved in the PreferencesExt.
    @param String name  object name
    @param PersistentStore store ok if null or empty
  */
  public void restoreState( PreferencesExt store) {
    if (store == null)
      return;

    int ncols = table.getColumnCount();

      // stored column order
    int [] modelIndex = (int []) store.getBean("ColumnOrder", null);

    if ((modelIndex != null) && (modelIndex.length == ncols)) { // what about invisible ??

      // make invisible any not stored
      boolean [] visible = new boolean[ncols];
      for (int i=0; i<modelIndex.length; i++)
        if (modelIndex[i] < ncols)
          visible[ modelIndex[i]] = true;

      // modify popup menu
      for (int i=0; i<ncols; i++)
        if (!visible[i]) {
          //System.out.println( colName[i]+" hide "+i);
          acts[i].hideColumn();
          acts[i].putValue(BAMutil.STATE, new Boolean(false));
        }

      // now set the header order
      TableColumnModel tcm = table.getColumnModel();
      int n = Math.min( modelIndex.length, table.getColumnCount());
      for (int i=0; i<n; i++) {
        TableColumn tc = tcm.getColumn(i);
        tc.setModelIndex(modelIndex[i]);
        String name = model.getColumnName(modelIndex[i]);
        tc.setHeaderValue(name);
        tc.setIdentifier(name);
        if (useThreads && (modelIndex[i] == threadCol)) {
          threadHeaderRenderer = new ThreadHeaderRenderer(threadCol);
          tc.setHeaderRenderer( threadHeaderRenderer);
        } else
          tc.setHeaderRenderer( new SortedHeaderRenderer(name, modelIndex[i]));
      }
    }

    // set the column widths
    Object colWidths = store.getBean("ColumnWidths", null);
    if (colWidths == null)
      return;
    int [] size = (int [] ) colWidths;

    if (size != null)
      setColumnWidths( size);
    if (debug) {
      System.out.println(" read widths = ");
      for (int i=0; i<size.length; i++)
        System.out.print(" "+size[i]);
      System.out.println();
    }

    boolean isThreadsOn = store.getBoolean( "isThreadsOn", false);
    if (useThreads) {
      model.setThreadsOn( isThreadsOn);
      threadHeaderRenderer.setOn(isThreadsOn);
    }

    int colNo = store.getInt( "SortOnCol", 0);
    boolean reverse = store.getBoolean( "SortReverse", false);
    model.setSortCol( colNo);
    model.setReverse( reverse);
    setSortCol( colNo, reverse);

    model.sort();
    table.fireDataChanged();
  }

  private void setColumnWidths(int[] sizes) {
    TableColumnModel tcm = table.getColumnModel();
    for (int i=0; i< table.getColumnCount(); i++) {
      TableColumn tc = tcm.getColumn(i);
      int maxw = ((sizes == null) || (i >= sizes.length))  ? 10 : sizes[i];
   //     model.getPreferredWidthForColumn(tc) : sizes[i];
      tc.setPreferredWidth(maxw);
    }
    //table.sizeColumnsToFit(0);     //  must be called due to a JTable bug
  }

  public void setColOn( int colno, boolean state, int pos) {
    // System.out.println("setColOn "+colno+" "+state+" "+pos);
    acts[colno].putValue(BAMutil.STATE, new Boolean(state));
    if (state)
      acts[colno].addAtPos(pos);
    else
      acts[colno].hideColumn();
  }

  public void registerKeyboardAction(ActionListener act,  KeyStroke key, int when) {
    table.registerKeyboardAction(act, key, when);
  }

 public void setFontSize( int size) {
    table.setFont( table.getFont().deriveFont( (float) size));
  }

  /**
   * Save state to the PreferencesExt.
   */
  public void saveState(PreferencesExt store) {
    if (store == null)
      return;

    int ncols = table.getColumnCount();
    int [] size = new int[ncols];
    int [] modelIndex = new int[ncols];

    TableColumnModel tcm = table.getColumnModel();
    for (int i=0; i<ncols; i++) {
      TableColumn tc = tcm.getColumn(i);
      size[i] = tc.getWidth();
      modelIndex[i] = tc.getModelIndex();
    }
    store.putBeanObject( "ColumnWidths", size);
    store.putBeanObject( "ColumnOrder", modelIndex);

    store.putInt( "SortOnCol", model.getSortCol());
    store.putBoolean( "SortReverse", model.getReverse());
    store.putBoolean( "isThreadsOn", model.isThreadsOn());

    if (debug) {
      System.out.println(" store widths = ");
      for (int i=0; i<size.length; i++)
        System.out.print(" "+size[i]);
      System.out.println();
    }

  }

  /**
    Replace the rowList with this one.
    @param ArrayList rowList
  */
  public ArrayList getRows( ) { return model.getRows(); }
  public void setRows( ArrayList rows) {
    model.setRows( rows);

/*    if (rowList.size() > 0)
      table.setRowSelectionInterval(0, 0);
    else
      table.clearSelection(); */

    //table.clearSelection();
    table.fireDataChanged();
  }

  /**
    Remove elem from rowList, update the table.
    Searches for match using object identity (==)
    @param Object elem
  *
  public void removeRow ( Object elem) {
    Iterator iter = rowList.iterator();
    while (iter.hasNext()) {
      Object row = iter.next();
      if (row == elem) {
        iter.remove();
        break;
      }
    }
    table.revalidate();
  } */

   // public int getRowCount() { return table.getRowCount(); }
  //int getSelectedRowIndex() { return table.getSelectedRow(); }   // for SuperComboBox
  //void setSortOK(boolean sortOK) { this.sortOK = sortOK; }       // for SuperComboBox

  /**
    Get the currently selected row.
    @return selected TableRow
  */
  public TableRow getSelectedRow() {
    return model.getRow(table.getSelectionPath());
  }

  /**
    Get the currently selected rows.
    @return an Iterator whose objects are TableRow
  */
  public Iterator getSelectedRows() {
    TreePath[] paths = table.getSelectionPaths();
    if ((paths == null) || (paths.length < 1))
      return null;

    HashSet set = new HashSet(2*paths.length);
    for (int i=0; i< paths.length; i++) {
      model.addRowsToSetFromPath( table.getTree(), paths[i], set);
    }

    return set.iterator();
  }

  /**
    Set the current selection to this row.
    @param int row index into rowList
  */
  public void setSelectedRow(int rowno) {
    if ((rowno < 0) || (rowno >= model.getRowCount()))
      return;
    if (debugSetPath) System.out.println("TreeTableSorted setSelected "+rowno);

    selectedRow = model.getRow(rowno);
    TreePath path = model.getPath( selectedRow);
    if (path != null) table.setSelectionPath( path);

    // for mysterious reasons, gotta do it again later
    invokeSetPath();
    ensureRowIsVisible( rowno);
  }

  private void invokeSetPath() {
    // gotta do this after the dust settles
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        TreePath path = model.getPath( selectedRow);
        if (path != null) {
          int rowno = table.setSelectionPath( path);
          if (rowno >= 0) ensureRowIsVisible(rowno);
          if (debugSetPath) System.out.println("----reset selectedRow = "+rowno+" "+path);
        }
      }
    });
  }

  /**
    Increment or decrement the current selection by one row.
    @param boolean increment true=increment, false=decrement
  */
  public void incrSelected(boolean increment) {
    int rowno = table.incrSelected(increment);
    if (rowno > 0) ensureRowIsVisible( rowno);
  }

  // Get the JTable delegate so you can do nasty things to it
  public void setDefaultRenderer(Class columnClass, TableCellRenderer renderer) {
    table.setDefaultRenderer(columnClass, renderer);
  }

  /** this array translates the column index to the model index */
  public int[] getModelIndex() {
    int [] modelIndex = new int[model.getColumnCount()];

    try {
      TableColumnModel tcm = table.getColumnModel();
      for (int i=0; i<model.getColumnCount(); i++) {
        TableColumn tc = tcm.getColumn(i);
        modelIndex[i] = tc.getModelIndex();
      }
    } catch (java.lang.ArrayIndexOutOfBoundsException e) {
      //can happen when model size increases
    }

    return modelIndex;
  }


/////////////////////////////////////////////////////////////////////////////////
  private void ensureRowIsVisible(int nRow) {
    Rectangle visibleRect = table.getCellRect(nRow, 0, true);
    if (debugSetPath) System.out.println("----ensureRowIsVisible = "+visibleRect);
    if (visibleRect != null)  {
      visibleRect.x = scrollPane.getViewport().getViewPosition().x;
      table.scrollRectToVisible(visibleRect);
      table.repaint();
    }
  }

 public void setSortCol( int sortCol, boolean reverse) {
    TableColumnModel tcm = table.getColumnModel();
    for (int i=0; i<table.getColumnCount(); i++) {
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
      TableColumnModel tcm = table.getColumnModel();

      if (state) {
        if (tc != null) tcm.addColumn(tc);
      } else
        hideColumn();

      JTreeTableSorted.this.revalidate();
      //System.out.println(id+" "+state);
    }

    public void addAtPos(int pos) {
      if (tc == null) return;

      TableColumnModel tcm = table.getColumnModel();

      // make sure it doesnt already exist
      try {
        tcm.addColumn(tc);
        int idx = tcm.getColumnIndex(id);
        tcm.moveColumn( idx, 0);
      } catch (Exception e) {
        System.out.println("addAtPos failed"+e);
      }

    }

    public void hideColumn() {
      //System.out.println("hideColumn "+id);
      TableColumnModel tcm = table.getColumnModel();
      try {
        int idx = tcm.getColumnIndex(id);
        tc = tcm.getColumn(idx);
        tcm.removeColumn(tc);
      } catch (Exception e) {
        //System.out.println("hideColumn didnt find"+id);
      }
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
    boolean isOn = false;

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

    void setOn( boolean setOn) {
      isOn = setOn;
      comp = (isOn) ? sort : unsort;
    }

    void setSortCol( int sortCol, boolean reverse) { }
  }
}
