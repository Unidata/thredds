// $Id: TreeTableModelSorted.java 50 2006-07-12 16:30:06Z caron $
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import javax.swing.tree.TreePath;

public class TreeTableModelSorted extends TreeTableModelAbstract {
  private boolean treeSort = false;
  private RowSorter rowSorter = null;
  private ThreadSorter threadSorter = null;

  private String[] colName;     // column names
  private ArrayList rowList;    // row data
  private ArrayList treeList;   // divided into tree

  private boolean useThreads = false; // are we in "thread mode"?
  private boolean threadsOn = false;  // are threads currently toggled on ?
  private int threadCol = -1;         // thread column
  private int indentCol = -1;         // column to indent, to indicate child

  private int sortCol = 0;            // column to sort on
  private boolean reverse = false;    // reverse sort
  private boolean debug = false, debugTM = false, debugSort = false, showNodeName = false;

  /**
   * This uses the mode where the selected column becomes the root of the tree.
   *
   * @param colName: list of column names, must have length > 0.
   * @param rowList: array of rows that implement TableRow interface, may be empty but not null.
   */
  public TreeTableModelSorted(String[] colName, ArrayList rows) {
    this(null, colName, rows);
    treeSort = true;
  }

  /**
   * This is the mode that adds a column for threads.
   *
   * @param ThreadSorter threadSorter: if non-null, add thread column.
   * @param colName: list of column names, must have length > 0.
   * @param rowList: array of rows that implement TableRow interface, may be empty but not null.
   *   We make a copy of the Array, but these point to the original objects.
   */
  public TreeTableModelSorted(ThreadSorter threadSorter, String[] colName, ArrayList rows) {
    super(null);
    this.threadSorter = threadSorter;
    this.useThreads = (threadSorter != null);
    this.colName = colName;

    // add a column if it uses Threads; note thread column is always last
    if (useThreads) {
      String[] newColName = new String[ colName.length + 1];
      for (int i=0; i<colName.length; i++)
        newColName[i] = colName[i];

      threadCol = colName.length;
      newColName[threadCol] = "Threads";
      this.colName = newColName;

      indentCol = threadSorter.getIndentCol();
    }

    this.rowList = new ArrayList(rows); // a new rowList !!
    sort();
    root = this;
  }

    // accessors
  public boolean isTreeSort() { return treeSort; }
  public boolean useThreads() { return useThreads; }
  public boolean isThreadsOn( ) { return threadsOn; }
  public void setThreadsOn( boolean threadsOn) { this.threadsOn = threadsOn; }

  public boolean getReverse() { return reverse; }
  public void setReverse(boolean reverse) { this.reverse = reverse; }
  public int getSortCol() { return sortCol; }
  public void setSortCol(int sortCol) { this.sortCol = sortCol; }

  public void setSorter(RowSorter sorter) { this.rowSorter = sorter; }

  /* public TableRow getRow( int row) {
    if ((row < 0) || (row >= rowList.size()))
      return null;
    else
      return (TableRow) rowList.get( row);
  } */

  /**
   * Set a new rowlist. This will automaticaly sort.
   * We make a copy of the Array, but these point to the original objects.
   * @param rows: array of rows that implement TableRow interface
   */
  public ArrayList getRows( ) { return rowList; }
  public void setRows( ArrayList rows) {
    this.rowList = new ArrayList(rows);
    sort();
  }
  public int getRowCount() { return rowList.size(); }

  /** sort using the current sortCol and reverse */
  public void sort() {
    sort( sortCol, reverse);
  }

  /** sort using the current sortCol; toggle reverse */
  public boolean sort( int sortCol) {
    if (sortCol == this.sortCol)
      reverse = !reverse;
    else
      reverse = false;
    sort( sortCol, reverse);
    return reverse;
  }

  /** sort using the named sortCol and reverse */
  public void sort( int sortCol, boolean reverse) {
    this.sortCol = sortCol;
    this.reverse = reverse;
    if (debugSort) System.out.println("sortCol "+ sortCol+" threads = "+threadsOn+
      " #rows = "+ rowList.size());

    if (rowSorter != null) {
      if (debugSort) System.out.println("sortExternal");
      sortExternal( sortCol, reverse);
      return;
    }

    if (threadsOn) {
      if (debugSort) System.out.println("sortThread");
      sortThread( sortCol, reverse);
      return;
    }

    if (debugSort) System.out.println("standard sort");

    // standard sort on the selected col
    java.util.Collections.sort( rowList, new TableRowAbstract.Sorter(sortCol, reverse));

    if (treeSort)
      makeTreeList( sortCol, rowList);
    else
      treeList = rowList;
  }

  /** Sort using the rowSorter; tree heirarchy based on rowSorter.isBreak
   */
  private void sortExternal( int sortCol, boolean reverse) {
      // external sort
    rowList = rowSorter.sort( sortCol, reverse, rowList);

      // divide the rowList into pieces
    treeList = new ArrayList();
    SortNode currentNode = null;
    TableRow last = null;
    int count = 0;
    for (int i=0; i< rowList.size(); i++) {
      TableRow row = (TableRow) rowList.get(i);
      if ((last == null) || rowSorter.isBreak( last, row)) {
        if (null != currentNode) currentNode.count = count;
        currentNode = new SortNode(i);
        treeList.add( currentNode);
        count = 0;
      }
      count++;
      last = row;
    }
    if (null != currentNode)
      currentNode.count = count;
  }

  /** Sort 1) using the threadSorter to find the threads, and then
   *  2) sort the top thread rows (threadSorter.isTopThread( row)) by
   *    the named sortCol and reverse.
   */
  private void sortThread( int sortCol, boolean reverse) {
      // sort by thread
    rowList = threadSorter.sort( sortCol, reverse, rowList);

      // divide the rowList into pieces based on thread
    treeList = new ArrayList();
    SortNode currentNode = null;
    int count = 0;
    for (int i=0; i< rowList.size(); i++) {
      TableRow row = (TableRow) rowList.get(i);
      if (threadSorter.isTopThread( row)) {
        if (null != currentNode) currentNode.count = count;
        currentNode = new SortNode(i);
        treeList.add( currentNode);
        count = 0;
      }
      count++;
    }
    if (null != currentNode)
      currentNode.count = count;

    // sort unique threads by actual sortCol
    java.util.Collections.sort( treeList);
  }

  /** create the tree heirarchy out of the rows, by
   *  watching when the value of the colNo changes.
   */
  private void makeTreeList( int colNo, ArrayList rowList) {
      // divide the rowList into pieces based on the sort column
    treeList = new ArrayList();
    SortNode currentNode = null;
    String current = "";
    int count = 0;
    for (int i=0; i< rowList.size(); i++) {
      TableRow row = (TableRow) rowList.get(i);
      String value = (String) row.getValueAt(colNo);  // STRING !
      //if (debug) System.out.println(i+" "+value+" "+row.getValueAt(3));
      if (!value.equals( current)) {
        if (null != currentNode) currentNode.count = count;
        currentNode = new SortNode(i);
        treeList.add( currentNode);
        current = value;
        count = 0;
      }
      count++;
    }
    if (null != currentNode)
      currentNode.count = count;
  }

  /////////////// TreeModel
  public int getChildCount(Object parent) {
    int ret = 0;
    if (parent instanceof TreeTableModelSorted)
      ret = treeList.size();
    else if (parent instanceof SortNode)
      ret = ((SortNode) parent).count-1;
    if (debugTM) System.out.println(" getChildCount <"+parent+"> "+ret);
    return ret;
  }

  public Object getChild(Object parent, int index) {
    Object ret = null;
    if (parent instanceof TreeTableModelSorted)
      ret = treeList.get(index);
    else if (parent instanceof SortNode) {
      SortNode node = (SortNode) parent;
      ret = rowList.get( node.start + index + 1);
    }
    if (debugTM) System.out.println(" getChild <"+parent+"> "+index+" = <"+ret+">");
    return ret;
  }

  public TableRow getRow(Object node) {
    TableRow ret = null;
    if (node instanceof TreeTableModelSorted)
      ret = (TableRow) treeList.get(0);
    else if (node instanceof SortNode) {
      SortNode snode = (SortNode) node;
      ret = snode.row;
    } else if (node instanceof TableRow) {
      ret = (TableRow) node;
      if (debug) System.out.println(" getRow <"+ret+">");
    }
    return ret;
  }


  ////////////////// TreeTableModel methods
  public int getColumnCount() { return colName.length; }
  public String getColumnName(int col) { return colName[col]; }
//    return (col < colName.length) ? colName[col] : "none" ; }

  public Object getValueAt(Object node, int col) {
    Object ret = null;
    if (node instanceof TreeTableModelSorted)
      ret = (col == 0) ? "root2 " : "";
    else if (node instanceof SortNode) {
      SortNode snode = (SortNode) node;
      TableRow row = (TableRow) rowList.get(snode.start);
      ret = getValueAt(snode.row, col);
    } else if (node instanceof TableRow) {
      ret = getValueAt((TableRow) node, col);
    }

    if (debugTM) System.out.println("   getValueAt <"+node+"> "+col+" = <"+ret+">");
    return ret;
  }

  private Object getValueAt(TableRow row, int col) {
    if (debug) System.out.println(" getValueAt <"+row+">"+col);
    if (useThreads && (col == threadCol))
      return "T";
    else if (threadsOn && (col == indentCol) && (!threadSorter.isTopThread( row)))
      return "    " + row.getValueAt(col);
    else
      return row.getValueAt(col);
  }

  public Class getColumnClass(int column) {
    if ( (treeSort && (column == sortCol)) || (useThreads && (column == threadCol)) )
      return TreeTableModel.class;
    else
      return Object.class;
  }

  public TableRow getRow( int rowno) { return (TableRow) rowList.get(rowno); }

  //////////////// Path to Row
  public TableRow getRow( TreePath path) {
    if (path == null)
      return null;
    Object node = path.getLastPathComponent();
    if (node instanceof TableRow)
      return (TableRow) node;
    else
      return ((SortNode) node).row;
  }

  // add a row specified by a TreePath to the list
  // if the row represents a thread, add all of its rows to the list
  // probable bug of adding twice
  public void addRowsToSetFromPath( javax.swing.JTree tree, TreePath path, Set set) {
    if (path == null)
      return;
    Object node = path.getLastPathComponent();
    if (node instanceof TableRow)
      set.add(node);
    else {
      SortNode snode = (SortNode) node;
      if (tree.isExpanded(path)) { // if epanded, add only this row
        set.add( rowList.get( snode.start));
      } else {  // if collapsed, add all of the children
        for (int i=0; i<snode.count; i++)
          set.add( rowList.get( snode.start + i));
      }
    }
  }

  // Row to Path
  public TreePath getPath( TableRow row) {
    int rowno = rowList.indexOf( row);
    if (rowno < 0) return null;

    if (!threadsOn && !treeSort) {
      Object[] path = new Object[2];
      path[0] = root;
      path[1] = row;
      return new TreePath( path);
    }
      // note this returns path to the SortNode only
    for (int i=0; i< treeList.size(); i++) {
      SortNode snode = (SortNode) treeList.get(i);
     /* if (rowno == snode.start) {
        Object[] path = new Object[2];
        path[0] = root;
        path[1] = snode;
        return new TreePath( path);
      } */
      if ((rowno >= snode.start) && (rowno < snode.start + snode.count)) {
        Object[] path = new Object[2];
        path[0] = root;
        path[1] = snode;
        return new TreePath( path);
      }
    }
    System.out.println("getPath didnt find row "+rowno+" = "+row);
    return null;
  }

  public String toString() { return "root"; }

  // debug
  void dumpAll( ) {
    boolean save = debugTM;
    debugTM = false;
    System.out.println( "model = ");
    dump( getRoot());
    System.out.println( "----");
    debugTM = save;
  }
  private void dump( Object node) {
    System.out.println( "  node = "+node.toString());
    int n = getChildCount( node);
    for (int i=0; i< n; i++)
      dump( getChild( node, i));
  }

  private class SortNode implements Comparable {
    int start;
    int count;
    TableRow row;

    SortNode( int start) { this (start, 0); }
    SortNode( int start, int count) {
      this.start = start;
      this.count = count;
      row = (TableRow) rowList.get(start);
      if (debug) System.out.println("new sort node "+toString());
    }

    public int compareTo(Object o) {
      TableRow otherRow = ((SortNode) o).row;
      return reverse ? otherRow.compare(row, sortCol) : row.compare(otherRow, sortCol);
    }

    public String toString() {
      return (treeSort) ? getValueAt(row, sortCol).toString() :
             (showNodeName) ? " node "+row : " "; // Jtree insists on using object name
    }
  }

}
