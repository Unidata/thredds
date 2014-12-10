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

package ucar.nc2.ui;

import ucar.ma2.*;
import ucar.nc2.NCdumpW;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarPeriod;
import ucar.nc2.time.CalendarTimeZone;
import ucar.nc2.ui.table.*;
import ucar.nc2.ui.widget.*;
import ucar.nc2.ui.widget.PopupMenu;
import ucar.nc2.util.HashMapLRU;
import ucar.nc2.util.Indent;
import ucar.util.prefs.PreferencesExt;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * This puts the data values of a 1D Structure or Sequence into a JTable.
 * The columns are the members of the Structure.
 *
 * @author caron
 */
public class StructureTable extends JPanel {
  private PreferencesExt prefs;
  private StructureTableModel dataModel;

  private JTable jtable;
  private PopupMenu popup;
  private FileManager fileChooser; // for exporting
  private TextHistoryPane dumpTA;
  private IndependentWindow dumpWindow;

  public StructureTable(PreferencesExt prefs) {
    this.prefs = prefs;

    jtable = new JTable();
    setLayout(new BorderLayout());
    jtable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

    ToolTipManager.sharedInstance().registerComponent(jtable);

    //JScrollPane sp =  new JScrollPane(jtable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    add(new JScrollPane(jtable), BorderLayout.CENTER);

    // other widgets
    dumpTA = new TextHistoryPane(false);
    dumpWindow = new IndependentWindow("Show Data", BAMutil.getImage("netcdfUI"), dumpTA);
    if (prefs != null)
      dumpWindow.setBounds((Rectangle) prefs.getBean("DumpWindowBounds", new Rectangle(300, 300, 600, 600)));
    else
      dumpWindow.setBounds(new Rectangle(300, 300, 600, 600));

    PreferencesExt fcPrefs = (prefs == null) ? null : (PreferencesExt) prefs.node("FileManager");
    fileChooser = new FileManager(null, null, "csv", "comma seperated values", fcPrefs);
  }

  private EventListenerList listeners = new EventListenerList();

  /**
   * Add listener: ListSelectionEvent sent when a new row is selected
   *
   * @param l the listener
   */
  public void addListSelectionListener(ListSelectionListener l) {
    listeners.add(javax.swing.event.ListSelectionListener.class, l);
  }

  /**
   * Remove listener
   *
   * @param l the listener
   */
  public void removeListSelectionListener(ListSelectionListener l) {
    listeners.remove(javax.swing.event.ListSelectionListener.class, l);
  }

  private void fireEvent(javax.swing.event.ListSelectionEvent event) {
    Object[] llist = listeners.getListenerList();
    // Process the listeners last to first
    for (int i = llist.length - 2; i >= 0; i -= 2)
      ((javax.swing.event.ListSelectionListener) llist[i + 1]).valueChanged(event);
  }

  public void addActionToPopupMenu(String title, AbstractAction act) {
    if (popup == null) popup = new PopupMenu(jtable, "Options");
    popup.addAction(title, act);
  }

  // clear the table

  public void clear() {
    if (dataModel != null)
      dataModel.clear();
  }

  // save state

  public void saveState() {
    fileChooser.save();
    if (prefs != null) prefs.getBean("DumpWindowBounds", dumpWindow.getBounds());
  }

  public void setStructure(Structure s) throws IOException {
    if (s.getDataType() == DataType.SEQUENCE)
      dataModel = new SequenceModel(s, true);
    else
      dataModel = new StructureModel(s);

    initTable(dataModel);
  }

  /**
   * Set the data as a collection of StructureData.
   *
   * @param structureData List of type StructureData
   * @throws IOException on io error
   */
  public void setStructureData(List<StructureData> structureData) throws IOException {
    dataModel = new StructureDataModel(structureData);
    initTable(dataModel);
  }

  public void setStructureData(ArrayStructure as) {
    dataModel = new ArrayStructureModel(as);
    initTable(dataModel);
  }

  public void setSequenceData(Structure s, ArraySequence seq) {
    dataModel = new ArraySequenceModel(s, seq);
    initTable(dataModel);
  }

  /**
   * Set the data as a collection of PointFeature.
   *
   * @param obsData List of type PointFeature
   * @throws IOException on io error
   */
  public void setPointFeatureData(List<PointFeature> obsData) throws IOException {
    dataModel = new PointFeatureDataModel(obsData);
    initTable(dataModel);
  }

  private void initTable(StructureTableModel m) {
    TableColumnModel tcm = new HidableTableColumnModel(m);
    jtable = new JTable(m, tcm);
    jtable.setRowSorter(new UndoableRowSorter<>(m));

    // Fixes this bug: http://stackoverflow.com/questions/6601994/jtable-boolean-cell-type-background
    ((JComponent) jtable.getDefaultRenderer(Boolean.class)).setOpaque(true);

    // Set the preferred column widths so that they're big enough to display all data without truncation.
    ColumnWidthsResizer resizer = new ColumnWidthsResizer(jtable);
    jtable.getModel().addTableModelListener(resizer);
    jtable.getColumnModel().addColumnModelListener(resizer);

    // Left-align every cell, including header cells.
    TableAligner aligner = new TableAligner(jtable, SwingConstants.LEADING);
    jtable.getColumnModel().addColumnModelListener(aligner);

    // Don't resize the columns to fit the available space. We do this because there may be a LOT of columns, and
    // auto-resize would cause them to be squished together to the point of uselessness. For an example, see
    // Q:/cdmUnitTest/ft/stationProfile/noaa-cap/XmadisXdataXLDADXprofilerXnetCDFX20100501_0200
    jtable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

    ListSelectionModel rowSM = jtable.getSelectionModel();
    rowSM.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;  //Ignore extra messages.
        ListSelectionModel lsm = (ListSelectionModel) e.getSource();
        if (!lsm.isSelectionEmpty())
          fireEvent(e);
      }
    });

    if (m.wantDate) {
      jtable.getColumnModel().getColumn(0).setCellRenderer(new DateRenderer());
      jtable.getColumnModel().getColumn(1).setCellRenderer(new DateRenderer());
    }

    // reset popup
    popup = null;
    addActionToPopupMenu("Show", new AbstractAction() {
      public void actionPerformed(java.awt.event.ActionEvent e) {
        showData();
      }
    });

    addActionToPopupMenu("Export", new AbstractAction() {
      public void actionPerformed(java.awt.event.ActionEvent e) {
        export();
      }
    });

    addActionToPopupMenu("Show Internal", new AbstractAction() {
      public void actionPerformed(java.awt.event.ActionEvent e) {
        showDataInternal();
      }
    });

    // add any subtables from inner Structures
    for (Structure s : m.subtables) {
      addActionToPopupMenu("Data Table for " + s.getShortName(), new SubtableAbstractAction(s));
    }

    removeAll();

    // Create a button that will popup a menu containing options to configure the appearance of the table.
    JButton cornerButton = new JButton(new TableAppearanceAction(jtable));
    cornerButton.setHideActionText(true);
    cornerButton.setContentAreaFilled(false);

    // Install the button in the upper-right corner of the table's scroll pane.
    JScrollPane scrollPane = new JScrollPane(jtable);
    scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, cornerButton);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

    // This keeps the corner button visible even when the table is empty (or all columns are hidden).
    scrollPane.setColumnHeaderView(new JViewport());
    scrollPane.getColumnHeader().setPreferredSize(jtable.getTableHeader().getPreferredSize());

    add(scrollPane, BorderLayout.CENTER);

    revalidate();
  }

  // display subtables
  private static HashMap<String, IndependentWindow> windows = new HashMap<>();

  private class SubtableAbstractAction extends AbstractAction {
    Structure s;
    StructureTable dataTable;
    IndependentWindow dataWindow;

    SubtableAbstractAction(Structure s) {
      this.s = s;
      dataTable = new StructureTable(null);
      dataWindow = windows.get(s.getFullName());
      if (dataWindow == null) {
        dataWindow = new IndependentWindow("Data Table", BAMutil.getImage("netcdfUI"), dataTable);
        windows.put(s.getFullName(), dataWindow);
      } else {
        dataWindow.setComponent(dataTable);
      }
    }

    public void actionPerformed(java.awt.event.ActionEvent e) {
      StructureData sd = getSelectedStructureData();
      if (sd == null) return;
      StructureMembers.Member m = sd.findMember(s.getShortName());
      if (m == null)
        throw new IllegalStateException("cant find member = " + s.getShortName());

      if (m.getDataType() == DataType.STRUCTURE) {
        ArrayStructure as = sd.getArrayStructure(m);
        dataTable.setStructureData(as);

      } else if (m.getDataType() == DataType.SEQUENCE) {
        ArraySequence seq = sd.getArraySequence(m);
        dataTable.setSequenceData(s, seq);

      } else throw new IllegalStateException("data type = " + m.getDataType());

      dataWindow.show();
    }
  }

  private void export() {
    String filename = fileChooser.chooseFilename();
    if (filename == null) return;
    try {
      PrintWriter pw = new PrintWriter(new File(filename),
              CDM.utf8Charset.name());

      TableModel model = jtable.getModel();
      for (int col = 0; col < model.getColumnCount(); col++) {
        if (col > 0) pw.print(",");
        pw.print(model.getColumnName(col));
      }
      pw.println();

      for (int row = 0; row < model.getRowCount(); row++) {
        for (int col = 0; col < model.getColumnCount(); col++) {
          if (col > 0) pw.print(",");
          pw.print(model.getValueAt(row, col).toString());
        }
        pw.println();
      }
      pw.close();
      JOptionPane.showMessageDialog(this, "File successfully written");
    } catch (IOException ioe) {
      JOptionPane.showMessageDialog(this, "ERROR: " + ioe.getMessage());
      ioe.printStackTrace();
    }

  }

  private void showData() {
    StructureData sd = getSelectedStructureData();
    if (sd == null) return;

    StringWriter sw = new StringWriter(10000);
    try {
      NCdumpW.printStructureData(new PrintWriter(sw), sd);
    } catch (IOException e) {
      String mess = e.getMessage();
      sw.write(mess);
    }
    dumpTA.setText(sw.toString());
    dumpWindow.setVisible(true);
  }

  private void showDataInternal() {
    StructureData sd = getSelectedStructureData();
    if (sd == null) return;

    Formatter f = new Formatter();
    sd.showInternalMembers(f, new Indent(2));
    f.format("%n");
    sd.showInternal(f, new Indent(2));
    dumpTA.setText(f.toString());
    dumpWindow.setVisible(true);
  }

  private StructureData getSelectedStructureData() {
    int viewRowIdx = jtable.getSelectedRow();
    if (viewRowIdx < 0) return null;
    int modelRowIndex = jtable.convertRowIndexToModel(viewRowIdx);

    try {
      return dataModel.getStructureData(modelRowIndex);
    } catch (InvalidRangeException e) {
      e.printStackTrace();
    } catch (IOException e) {
      JOptionPane.showMessageDialog(this, "ERROR: " + e.getMessage());
      e.printStackTrace();
    }
    return null;
  }

  public Object getSelectedRow() {
    int viewRowIdx = jtable.getSelectedRow();
    if (viewRowIdx < 0) return null;
    int modelRowIndex = jtable.convertRowIndexToModel(viewRowIdx);

    try {
      return dataModel.getRow(modelRowIndex);
    } catch (InvalidRangeException | IOException e) {
      JOptionPane.showMessageDialog(this, "ERROR: " + e.getMessage());
      e.printStackTrace();
    }
    return null;
  }

  ////////////////////////////////////////////////////////////////////////////////////////

  private abstract class StructureTableModel extends AbstractTableModel {
    protected HashMapLRU rowHash = new HashMapLRU(500, 500); // cache 500 rows
    protected StructureMembers members;
    protected boolean wantDate = false;
    protected List<Structure> subtables = new ArrayList<>();

    // subclasses implement these

    abstract public StructureData getStructureData(int row) throws InvalidRangeException, IOException;

    // remove all data

    abstract public void clear();

    // if we know how to extract the date for this data, add two extra columns

    abstract public CalendarDate getObsDate(int row);

    abstract public CalendarDate getNomDate(int row);

    public Object getRow(int row) throws InvalidRangeException, IOException {
      return getStructureData(row);
    }

    public int getColumnCount() {
      if (members == null) return 0;
      return members.getMembers().size() + (wantDate ? 2 : 0);
    }

    public String getColumnName(int columnIndex) {
      // if (columnIndex == 0)
      //   return "hash";
      if (wantDate && (columnIndex == 0))
        return "obsDate";
      if (wantDate && (columnIndex == 1))
        return "nomDate";
      int memberCol = wantDate ? columnIndex - 2 : columnIndex;
      return members.getMember(memberCol).getName();
    }

    // get row data if in the cache, otherwise read it

    public StructureData getStructureDataHash(int row) throws InvalidRangeException, IOException {
      StructureData sd = (StructureData) rowHash.get(row);
      if (sd == null) {
        sd = getStructureData(row);
        rowHash.put(row, sd);
      }
      return sd;
    }

    public Object getValueAt(int row, int column) {
      /* if (column == 0) {
        try {
          return Long.toHexString( getStructureData(row).hashCode());
        } catch (Exception e) {
          return "ERROR";
        }
      } */
      if (wantDate && (column == 0))
        return getObsDate(row);
      if (wantDate && (column == 1))
        return getNomDate(row);

      StructureData sd;
      try {
        sd = getStructureDataHash(row);
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        return "ERROR " + e.getMessage();
      } catch (IOException e) {
        e.printStackTrace();
        return "ERROR " + e.getMessage();
      }

      String colName = getColumnName(column);
      return sd.getArray(colName);
    }

    String enumLookup(StructureMembers.Member m, Number val) {
      return "sorry";
    }

  }

  ////////////////////////////////////////////////////////////////////////
  // handles Structures

  private class StructureModel extends StructureTableModel {
    private Structure struct;

    StructureModel(Structure s) {
      this.struct = s;
      this.members = s.makeStructureMembers();
      for (Variable v : s.getVariables()) {
        if (v instanceof Structure)
          subtables.add((Structure) v);
      }
    }

    public CalendarDate getObsDate(int row) {
      return null;
    }

    public CalendarDate getNomDate(int row) {
      return null;
    }

    public int getRowCount() {
      if (struct == null) return 0;
      return (int) struct.getSize();
    }

    public StructureData getStructureData(int row) throws InvalidRangeException, IOException {
      return struct.readStructure(row);
    }

    public void clear() {
      struct = null;
      fireTableDataChanged();
    }

    String enumLookup(StructureMembers.Member m, Number val) {
      Variable v = struct.findVariable(m.getName());
      return v.lookupEnumString(val.intValue());
    }

  }

  // handles Sequences

  private class SequenceModel extends StructureModel {
    protected List<StructureData> sdataList;

    SequenceModel(Structure seq, boolean readData) {
      super(seq);

      if (readData) {
        sdataList = new ArrayList<>();
        try {
          StructureDataIterator iter = seq.getStructureIterator();
          try {
            while (iter.hasNext())
              sdataList.add(iter.next());
          } finally {
            iter.finish();
          }

        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    public CalendarDate getObsDate(int row) {
      return null;
    }

    public CalendarDate getNomDate(int row) {
      return null;
    }

    public int getRowCount() {
      return sdataList.size();
    }

    public StructureData getStructureData(int row) throws InvalidRangeException, IOException {
      return sdataList.get(row);
    }

    // LOOK does this have to override ?

    public Object getValueAt(int row, int column) {
      StructureData sd = sdataList.get(row);

      /* if (column == 0) {
        try {
          return Long.toHexString( sd.hashCode());
        } catch (Exception e) {
          return "ERROR";
        }
      } */
      return sd.getScalarObject(sd.getStructureMembers().getMember(column));
    }

    public void clear() {
      sdataList = new ArrayList<>();
      fireTableDataChanged();
    }
  }

  private class ArraySequenceModel extends SequenceModel {

    ArraySequenceModel(Structure s, ArraySequence seq) {
      super(s, false);

      this.members = seq.getStructureMembers();

      sdataList = new ArrayList<>();
      try {
        StructureDataIterator iter = seq.getStructureDataIterator();
        try {
          while (iter.hasNext())
            sdataList.add(iter.next());  // LOOK lame -read at all once
        } finally {
          iter.finish();
        }

      } catch (IOException e) {
        JOptionPane.showMessageDialog(null, "ERROR: " + e.getMessage());
        e.printStackTrace();
      }
    }
  }

  ////////////////////////////////////////////////////////////////////////

  private class StructureDataModel extends StructureTableModel {
    private List<StructureData> structureData;

    StructureDataModel(List<StructureData> structureData) {
      this.structureData = structureData;
      if (structureData.size() > 0) {
        StructureData sd = structureData.get(0);
        this.members = sd.getStructureMembers();
      }
    }

    public CalendarDate getObsDate(int row) {
      return null;
    }

    public CalendarDate getNomDate(int row) {
      return null;
    }

    public int getRowCount() {
      return structureData.size();
    }

    public StructureData getStructureData(int row) throws InvalidRangeException, IOException {
      return structureData.get(row);
    }

    public void clear() {
      structureData = new ArrayList<>(); // empty list
      fireTableDataChanged();
    }

  }

  ////////////////////////////////////////////////////////////////////////

  private class ArrayStructureModel extends StructureTableModel {
    private ArrayStructure as;

    ArrayStructureModel(ArrayStructure as) {
      this.as = as;
      this.members = as.getStructureMembers();
    }

    public CalendarDate getObsDate(int row) {
      return null;
    }

    public CalendarDate getNomDate(int row) {
      return null;
    }

    public int getRowCount() {
      return (as == null) ? 0 : (int) as.getSize();
    }

    public StructureData getStructureData(int row) throws InvalidRangeException, IOException {
      return as.getStructureData(row);
    }

    public void clear() {
      as = null;
      fireTableDataChanged();
    }

  }

  /*

  private class TrajectoryModel extends StructureTableModel {
    private TrajectoryObsDatatype traj;

    TrajectoryModel(TrajectoryObsDatatype traj) throws IOException {
      this.traj = traj;
      StructureData sd;
      if (traj.getNumberPoints() > 0) {
        try {
          sd = traj.getData(0);
          this.members = sd.getStructureMembers();
        } catch (InvalidRangeException e) {
          JOptionPane.showMessageDialog(null, "ERROR: " + e.getMessage());
          throw new IOException(e.getMessage());
        }
      }
      wantDate = true;
    }

    public Date getObsDate(int row) {
      try {
        return traj.getTime(row);
      } catch (IOException e) {
        return null;
      }
    }

    public Date getNomDate(int row) {
      return null;
    }

    public Date getDate(int row) {
      try {
        return traj.getTime(row);
      } catch (IOException e) {
        return null;
      }
    }

    public int getRowCount() {
      return (traj == null) ? 0 : traj.getNumberPoints();
    }

    public Object getRow(int row) throws InvalidRangeException, IOException {
      return traj.getPointObsData(row); // PointObsDatatype
    }

    public StructureData getStructureData(int row) throws InvalidRangeException, IOException {
      return traj.getData(row);
    }

    public void clear() {
      traj = null;
      fireTableDataChanged();
    }

  }

  private class PointObsDataModel extends StructureTableModel {
    private List<PointObsDatatype> obsData;

    PointObsDataModel(List<PointObsDatatype> obsData) throws IOException {
      wantDate = true;

      this.obsData = obsData;
      if (obsData.size() > 0) {
        StructureData sd;
        try {
          sd = getStructureData(0);
        } catch (InvalidRangeException e) {
          JOptionPane.showMessageDialog(null, "ERROR: " + e.getMessage());
          throw new IOException(e.getMessage());
        }
        this.members = sd.getStructureMembers();
      }
    }

    public Date getObsDate(int row) {
      PointObsDatatype obs = obsData.get(row);
      return obs.getObservationTimeAsDate();
    }

    public Date getNomDate(int row) {
      PointObsDatatype obs = obsData.get(row);
      return obs.getNominalTimeAsDate();
    }

    public int getRowCount() {
      return obsData.size();
    }

    public StructureData getStructureData(int row) throws InvalidRangeException, IOException {
      PointObsDatatype obs = obsData.get(row);
      return obs.getData();
    }

    public Object getRow(int row) throws InvalidRangeException, IOException {
      return obsData.get(row); // PointObsDatatype
    }

    public void clear() {
      obsData = new ArrayList<PointObsDatatype>(); // empty list
      fireTableDataChanged();
    }
  }  */

  ////////////////////////////////////////////////////////////////////////

  private class PointFeatureDataModel extends StructureTableModel {
    private List<PointFeature> obsData;

    PointFeatureDataModel(List<PointFeature> obsData) throws IOException {
      wantDate = true;

      this.obsData = obsData;
      if (obsData.size() > 0) {
        StructureData sd;
        try {
          sd = getStructureData(0);
        } catch (InvalidRangeException e) {
          JOptionPane.showMessageDialog(null, "ERROR: " + e.getMessage());
          throw new IOException(e.getMessage());
        }
        this.members = sd.getStructureMembers();
      }
    }

    public CalendarDate getObsDate(int row) {
      PointFeature obs = obsData.get(row);
      return obs.getObservationTimeAsCalendarDate();
    }

    public CalendarDate getNomDate(int row) {
      PointFeature obs = obsData.get(row);
      return obs.getNominalTimeAsCalendarDate();
    }

    public int getRowCount() {
      return obsData.size();
    }

    public StructureData getStructureData(int row) throws InvalidRangeException, IOException {
      PointFeature obs = obsData.get(row);
      return obs.getFeatureData();
    }

    public Object getRow(int row) throws InvalidRangeException, IOException {
      return obsData.get(row); // PointObsDatatype
    }

    public void clear() {
      obsData = new ArrayList<>(); // empty list
      fireTableDataChanged();
    }
  }

  /* private class SequenceTableModel extends TableModelWithDesc {
    private Sequence seq;
    private Structure struct;
    private ArrayObject.D1 data;

    SequenceTableModel( Sequence s) throws IOException {
      this.seq = s;
      this.struct = s.getInternalStructure();
      // for now, we have to read entire sequence into memory !!
      data = (ArrayObject.D1) seq.read();
    }

    public int getRowCount() {
      return (seq == null) ? 0 : (int) data.getSize();
    }
    public int getColumnCount() {
      return (seq == null) ? 0 : struct.getVariables().size();
    }

    public String getColumnName(int columnIndex) {
      return (String) struct.getVariableNames().get( columnIndex);
    }

    public String getColumnDesc(int columnIndex) {
      return "test "+columnIndex;
    }

    public Object getValueAt(int row, int column) {
      StructureData sd = (StructureData) data.get( row);
      List arrays = sd.getMembers();
      StructureData.Member gr = (StructureData.Member) arrays.get( column);
      Array a = gr.data;
      Index ima = a.getIndex();
      return a.getObject(ima);
    }
  } */

  /**
   * Renderer for Date type
   */
  static class DateRenderer extends DefaultTableCellRenderer {
    private CalendarDateFormatter newForm, oldForm;
    private CalendarDate cutoff;

    DateRenderer() {
      super();

      oldForm = new CalendarDateFormatter("yyyy MMM dd HH:mm", CalendarTimeZone.UTC);
      newForm = new CalendarDateFormatter("MMM dd, HH:mm", CalendarTimeZone.UTC);

      CalendarDate now =  CalendarDate.present();
      cutoff = now.add(-1, CalendarPeriod.Field.Year); // "now" time format within a year
    }

    public void setValue(Object value) {
      if (value == null)
        setText("");
      else {
        CalendarDate date = (CalendarDate) value;
        if (date.isBefore(cutoff))
          setText(oldForm.toString(date));
        else
          setText(newForm.toString(date));
      }
    }
  }
}
