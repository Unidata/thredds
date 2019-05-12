/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.geoloc;

import java.awt.Dimension;
import java.io.*;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import ucar.nc2.util.ListenerManager;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.util.prefs.PreferencesExt;

/**
 *  Consider this a private inner class of ProjectionManager.
 *
 * @author John Caron
 * @version revived /20/2012
 */

public class JTableProjection extends JTable {
  private PreferencesExt store;
  private ProjectionTableModel model;
  private ArrayList list;
  private boolean debug = false;
  private int selectedRow = 0;  // JTable doesnt handle selections correctly
  private ListenerManager lm;

  private static final String STORE_NAME = "ProjectionTableModel";

  public JTableProjection( PreferencesExt pstore) {
    this.store = pstore;

    if (store == null)
      model = new ProjectionTableModel();
    else {
      model = (ProjectionTableModel) store.getObject( STORE_NAME);
      if (model == null)
        model = new ProjectionTableModel();
    }

    list = model.getList();

    setModel(model);
    setAutoResizeMode( JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
    setPreferredScrollableViewportSize( new Dimension(400, 200));
    getTableHeader().setReorderingAllowed(true);
    model.adjustColumns(getColumnModel());

    // manage the selection
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    // have to manage selectedRow ourselves, due to bugs
    getSelectionModel().addListSelectionListener( new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        if (debug) System.out.println(" ListSelectionListener= "+ e);
        if (!e.getValueIsAdjusting()) {
          selectedRow = getSelectedRow();
          if (debug) System.out.println("     selectd= "+ selectedRow);
          lm.sendEvent(new NewProjectionEvent(this, getSelected()));
        }
      }
    });

    // manage NewProjectionListener's
    lm = new ListenerManager(
        "ucar.nc2.ui.geoloc.NewProjectionListener",
        "ucar.nc2.ui.geoloc.NewProjectionEvent",
        "actionPerformed");
  }

  public void addProjection(ProjectionImpl proj) {
    int rowno = model.addProjection(proj);
    setRowSelectionInterval(rowno, rowno);
    selectedRow = rowno;
  }

  public void replaceProjection(ProjectionImpl proj) {
    int rowno = model.replaceProjection(proj);
    setRowSelectionInterval(rowno, rowno);
    selectedRow = rowno;
  }

  public boolean contains(ProjectionImpl proj) {
    return (model.search(proj) >= 0);
  }

  public boolean contains(String id) {
    return (model.search(id) >= 0);
  }

  public ProjectionImpl getSelected() {
    int len = list.size();
    if ((0 > selectedRow) || (len <= selectedRow))
      return null;
    else
      return (ProjectionImpl) list.get(selectedRow);
  }

  public void deleteSelected() {
    int len = list.size();
    if ((0 > selectedRow) || (len <= selectedRow))
      return;

    model.deleteRow( selectedRow);

      // bugs in list selection code
    len = list.size();
    if (len == 0) {
      clearSelection();
      selectedRow = -1;
    } else {
      if (selectedRow > len-1) selectedRow = len-1;
      setRowSelectionInterval(selectedRow, selectedRow);
      if (debug) System.out.println(" set selection to "+selectedRow);
    }

    if (debug) System.out.println(" selection now= "+getSelectedRow()+ " really= "+ selectedRow);
    lm.sendEvent(new NewProjectionEvent(this, getSelected()));
    repaint();
  }

  public boolean isEmpty() {
    return (model.getRowCount() == 0);
  }

  public void storePersistentData() {
    if (store != null)
      store.putObject( STORE_NAME, model);
  }

  public void setMapArea( ProjectionRect bb) {
    if (0 > selectedRow)
      return;
    model.setMapArea(selectedRow, bb);
    if (debug) System.out.println(" PTsetMapArea = "+ bb+ " on "+ selectedRow);
  }

    // set current projection if found, else deselect
  public void setCurrentProjection(ProjectionImpl proj) {
    int row;
    if (0 <= (row = model.search(proj))) {
      if (debug) System.out.println(" PTsetCurrentProjection found = "+ row);
      selectedRow = row;
      setRowSelectionInterval(row, row);
    } else {
      if (debug) System.out.println(" PTsetCurrentProjection not found = "+ row);
      selectedRow = -1;
      clearSelection();
    }
  }

    // event listener managagment
  public void addNewProjectionListener( NewProjectionListener l) {
    lm.addListener(l);
  }
  public void removeNewProjectionListener( NewProjectionListener l) {
    lm.removeListener(l);
  }


  // inner class must be static because JTable not Serializable
  private static class ProjectionTableModel extends AbstractTableModel implements java.io.Serializable {
    private static String[] colName = {"Name", "Type", "Parameters", "Default Zoom"};
    private ArrayList list = new ArrayList(20);

    // AbstractTableModel methods
    public int getRowCount() { return list.size(); }
    public int getColumnCount() { return colName.length; }
    public String getColumnName(int col) { return colName[col]; }
    public Object getValueAt (int row, int col) {
      ProjectionImpl proj = (ProjectionImpl) list.get( row);
      switch (col) {
        case 0: return proj.getName();
        case 1: return proj.getClassName();
        case 2: return proj.paramsToString();
        case 3: return proj.getDefaultMapArea();
      }
      return "error";
    }
    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return (columnIndex == 0);
    }
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
      ProjectionImpl proj = (ProjectionImpl) list.get( rowIndex);
      proj.setName((String) aValue);
    }

      // do our own listener management to get around serialization bug
      // can delete this section when bug is fixed
    ProjectionTableModel() { constructLM(); }
    private transient ListenerManager lm;
    private void constructLM() {
      lm = new ListenerManager("javax.swing.event.TableModelListener",
       "javax.swing.event.TableModelEvent", "tableChanged");
    }
    public void addTableModelListener(TableModelListener l) {
      lm.addListener(l);
    }
    public void removeTableModelListener(TableModelListener l) {
      lm.removeListener(l);
    }
    public void fireTableChanged(TableModelEvent e) {
      lm.sendEvent(e);
    }
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
      s.defaultReadObject();
      constructLM();
    }

    // added methods
    int addProjection(ProjectionImpl proj) {
      list.add(proj);
      int count = list.size() - 1;
      fireTableRowsInserted(count, count);
      return count;
    }

    int replaceProjection(ProjectionImpl proj) {
      int rowno = search( proj);
      if (rowno < 0)
        return -1;
      list.set( rowno, proj);
      return rowno;
    }

    void adjustColumns( TableColumnModel colModel) {
      for (int i=0; i<colName.length; i++) {
        colModel.getColumn(i).setMinWidth(50);
        colModel.getColumn(i).setPreferredWidth(100);
      }
    }

    ArrayList getList() { return list; }

    void deleteRow(int row) {
      int len = list.size();
      if (row < len)
        list.remove(row);
      else return;
      fireTableRowsDeleted(row, row);
    }

    int search( ProjectionImpl proj) {
      for (int row=0; row<list.size(); row++) {
        ProjectionImpl test = (ProjectionImpl) list.get( row);
        if (proj.getName().equals(test.getName()))
          return row;
      }
      return -1;
    }

    int search( String projName) {
      for (int row=0; row<list.size(); row++) {
        ProjectionImpl test = (ProjectionImpl) list.get( row);
        if (projName.equals(test.getName()))
          return row;
      }
      return -1;
    }


    void setMapArea( int row, ProjectionRect bb) {
      int len = list.size();
      if (row >= len)
        return;
      ProjectionImpl proj = (ProjectionImpl) list.get(row);
      proj.getDefaultMapArea().setRect( bb);
      fireTableRowsUpdated(row, row);
    }
  }
}

