// $Id: BeanTableSorted.java,v 1.4 2004/10/22 00:59:45 caron Exp $
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

package ucar.util.prefs.ui;

import ucar.util.prefs.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.*;
import javax.swing.table.*;

/**
 * BeanTableSorted adds sorting functionality to a BeanTable.
 * It throws a UIChangeEvent, property = "sort" just before a sort is going to happen.
 *
 * @see ucar.util.prefs.ui.BeanTable
 * @see ucar.util.prefs.PreferencesExt
 * @author John Caron
 * @version $Revision: 1.4 $ $Date: 2004/10/22 00:59:45 $
 */

public class BeanTableSorted extends BeanTable {
      // for HeaderRenderer
  //private Icon sortDownIcon = getIcon( "/resources/icons/SortDown.gif");
  //private Icon sortUpIcon = getIcon( "/resources/icons/SortUp.gif");

  //private boolean reverse = false;
 // private int sortCol = -1;

  private TableSorter sortedModel;

  public BeanTableSorted( Class bc, PreferencesExt pstore, boolean canAddDelete) {
    super( bc, pstore, canAddDelete);

    sortedModel = new TableSorter(model);
    jtable.setModel( sortedModel);
    sortedModel.setTableHeader(jtable.getTableHeader());

    restoreState(); // ??
  }

    // so it can be ovverriden in BeanTableSorted
  protected int modelIndex(int viewIndex) {
    return sortedModel.modelIndex(viewIndex);
  }
  protected int viewIndex(int rowIndex) {
    return sortedModel.viewIndex(rowIndex);
  }

}

    /*  set the header renderers
    TableColumnModel tcm = jtable.getColumnModel();
    for (int i=0; i<jtable.getColumnCount(); i++) {
      TableColumn tc = tcm.getColumn(i);
      int model_idx = tc.getModelIndex();
      tc.setHeaderRenderer( new SortedHeaderRenderer( model_idx));
    }

    // set sorting behavior
    JTableHeader hdr = jtable.getTableHeader();
    hdr.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {

        TableColumnModel tcm2 = jtable.getColumnModel();
        int colIdx = tcm2.getColumnIndexAtX(e.getX());
        int colNo = jtable.convertColumnIndexToModel(colIdx);

          /* keep current selection selected
        int selidx = jtable.getSelectedRow();
        Object selected = null;
        if (selidx >= 0)
          selected = list.get( selidx);

          // notify listsners of impending sort
        if (lm.hasListeners())
          lm.sendEvent( new UIChangeEvent(this, "sort", null, new Integer(colNo)));

          // sort
        sort(colNo);

          /* keep current selection selected
        if (selidx >= 0) {
          int newSelectedRow = list.indexOf( selected);
          jtable.setRowSelectionInterval(newSelectedRow, newSelectedRow);
          ensureRowIsVisible(newSelectedRow);
        }

        repaint();
      }
    });

    if (store != null) {
      sortCol = store.getInt( "SortOnCol", 0);
      reverse = store.getBoolean( "SortReverse", false);
    }
    sort( sortCol, reverse);
  }

  public void setBeans( ArrayList beans) {
    this.beans = beans;
    resort();
    jtable.revalidate();
    jtable.repaint();
  }

  public void saveState(boolean saveData) {
    if (store == null)
      return;

    super.saveState(saveData);
    store.putInt( "SortOnCol", sortCol);
    store.putBoolean( "SortReverse", reverse);
  }

  private ImageIcon getIcon( String fullIconName) {
    ImageIcon icon = null;
    java.net.URL iconR = getClass().getResource(fullIconName);
    if (iconR != null)
      icon = new ImageIcon(iconR);
    //System.out.println("icon loaded ="+fullIconName+" -> "+icon);
    return icon;
  }


 /**
    Sort the rowList: note rowList changed, not a copy of it.
    @param int colNo sort on this column
    @param boolean reverse if true, reverse sort
  *
   public void sort(int colNo, boolean reverse) {
    model.sort(colNo, reverse);
    jtable.setRowSelectionInterval(0, 0);
    ensureRowIsVisible(0);
  }


  /** add UIChangeEvent listener
  public void addUIChangeListener( UIChangeListener l) {
    lm.addListener(l);
  }
  /** remove UIChangeEvent listener
  public void removeUIChangeListener( UIChangeListener l) {
    lm.removeListener(l);
  }

  // public int getRowCount() { return table.getRowCount(); }
  public int getSelectedRowIndex() { return jtable.getSelectedRow(); }   // for SuperComboBox
  public void setSortOK(boolean sortOK) { this.sortOK = sortOK; }       // for SuperComboBox

  /**
    Get the currently selected row.
    @return selected TableRow
  *
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
  *
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
  *
  public void incrSelected(boolean increment) {
    if (list.size() == 0)
      return;
    int curr = jtable.getSelectedRow();
    if (increment && (curr < list.size()-1))
      setSelected(curr+1);
    else if (!increment && (curr > 0))
      setSelected(curr-1);
  }

  /** Get the JTable delegate so you can do nasty things to it *
  public JTable getTable() { return jtable; }

  /** for each column, get the model index *
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
  } *
  void resort() {
    sort( this.sortCol, this.reverse);
  }

  void sort( int sortCol) {
    if (sortCol == this.sortCol)
      reverse = !reverse;
    else
      reverse = false;
    sort(sortCol, reverse);
  }

  void sort( int sortCol, boolean reverse) {
    this.reverse = reverse;
    if (sortCol >= 0) {
      java.util.Collections.sort( beans, new SortList(sortCol, reverse));
    }
    this.sortCol = sortCol; // keep track of last sort
    setSortCol( sortCol, reverse);
  }

  private void setSortCol( int sortCol, boolean reverse) {
    TableColumnModel tcm = jtable.getColumnModel();
    for (int i=0; i<jtable.getColumnCount(); i++) {
      TableColumn tc = tcm.getColumn(i);
      SortedHeaderRenderer shr = (SortedHeaderRenderer) tc.getHeaderRenderer();
      shr.setSortCol( sortCol, reverse);
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
      Object v1 = model.getValueAt (o1, col);
      Object v2 = model.getValueAt (o2, col);

      if ((v1 == null) && (v2 == null)) return 0;
      if (v1 == null) return reverse ? 1 : -1;
      if (v2 == null) return reverse ? -1 : 1;

      if (v1 instanceof Comparable) {
        Comparable c1 = (Comparable) v1;
        Comparable c2 = (Comparable) v2;
        return reverse ? c2.compareTo(c1) : c1.compareTo(c2);

      }  else if (v1 instanceof Boolean) {
        boolean b1 = ((Boolean) v1).booleanValue();
        boolean b2 = ((Boolean) v2).booleanValue();

        if (b1 == b2) return 0;
        if (b1) return reverse ? -1 : 1;
        return reverse ? 1 : -1;
      }

      System.out.println("Illegal class to sort on="+v1.getClass().getName()+"; must implement Comparable");
      return 0;
    }

    public boolean equals(Object obj) { return this.equals(obj); }
  }


  /* add tooltips
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


  protected class SortedHeaderRenderer extends BeanTable.HeaderRenderer {
    JLabel upLabel, downLabel;
    boolean hasSortIndicator = false;
    boolean reverse = false;

    protected SortedHeaderRenderer(int modelIdx) {
      super( modelIdx);

      upLabel = new JLabel(sortUpIcon);
      downLabel = new JLabel(sortDownIcon);
    }

    void setSortCol( int sortCol, boolean reverse) {
      //System.out.println("setSortCol on "+modelCol+" "+sortCol+" "+reverse);

      if (sortCol == modelIdx) {

        if (!hasSortIndicator)
          compPanel.add(reverse? upLabel : downLabel, BorderLayout.EAST);
        else if (reverse != this.reverse) {
          compPanel.remove(1);
          compPanel.add(reverse? upLabel : downLabel, BorderLayout.EAST);
        }

        this.reverse = reverse;
        hasSortIndicator = true;

        //System.out.println("setSortCol SET on "+modelCol+" "+sortCol+" "+reverse);
      } else if (hasSortIndicator) {
        compPanel.remove(1);
        hasSortIndicator = false;
        //System.out.println("setSortCol SET off "+modelCol+" "+sortCol+" "+reverse);
      }
    }

/*    public Component getTableCellRendererComponent(JTable table, Object value,
      boolean isSelected, boolean hasFocus, int row, int column) {
      System.out.println("BeanTableSorted.getTableCellRendererComponent "+compPanel.getToolTipText());
      return compPanel;
    }

  }

  /* private class ThreadHeaderRenderer extends SortedHeaderRenderer {
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
/* Change History:
   $Log: BeanTableSorted.java,v $
   Revision 1.4  2004/10/22 00:59:45  caron
   get selection correct when sorting

   Revision 1.3  2004/08/26 17:55:17  caron
   no message

   Revision 1.2  2003/04/08 17:51:53  john
   misc

   Revision 1.1.1.1  2002/12/20 16:40:25  john
   start new cvs root: prefs

*/