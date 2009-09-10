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
import ucar.nc2.*;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.util.HashMapLRU;
import ucar.nc2.dt.TrajectoryObsDatatype;
import ucar.nc2.dt.PointObsDatatype;

import thredds.ui.*;
import thredds.ui.PopupMenu;

import java.io.*;
import java.util.*;
import java.util.List;

import java.awt.*;
import java.awt.event.MouseEvent;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.*;

import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.TableSorter;

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
  private IndependentDialog dumpWindow;

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
    dumpWindow = new IndependentDialog(null, false, "Show Data", dumpTA);
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
   * @param l the listener
   */
  public void addListSelectionListener(ListSelectionListener l) {
    listeners.add(javax.swing.event.ListSelectionListener.class, l);
  }

  /**
   * Remove listener
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
    dataModel = new ArraySequenceModel( s, seq);
    initTable(dataModel);
  }

  /**
   * This is used for a trajectory.
   *
   * @param traj treajectory
   * @throws IOException on io error
   */
  public void setTrajectory(TrajectoryObsDatatype traj) throws IOException {
    dataModel = new TrajectoryModel(traj);
    initTable(dataModel);
  }

  /**
   * Set the data as a collection of PointObsDatatype.
   *
   * @param obsData List of type PointObsDatatype
   * @throws IOException  on io error
   */
  public void setPointObsData(List<PointObsDatatype> obsData) throws IOException {
    dataModel = new PointObsDataModel(obsData);
    initTable(dataModel);
    //jtable.getColumnModel().getColumn(0).setCellRenderer(new DateRenderer());
  }

  /**
   * Set the data as a collection of PointFeature.
   *
   * @param obsData List of type PointFeature
   * @throws IOException  on io error
   */
  public void setPointFeatureData(List<PointFeature> obsData) throws IOException {
    dataModel = new PointFeatureDataModel(obsData);
    initTable(dataModel);
    //jtable.getColumnModel().getColumn(0).setCellRenderer(new DateRenderer());
  }

  private void initTable(StructureTableModel m) {
    jtable = setModel(m);

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
    // jtable.setDefaultRenderer(Date.class, new DateRenderer()); // LOOK this doesnt work!
    jtable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

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

    // add any subtables from inner Structures
    for (Structure s : m.subtables) {
      addActionToPopupMenu("Data Table for "+s.getShortName(), new SubtableAbstractAction(s));
    }

    //JScrollPane sp =  new JScrollPane(jtable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    removeAll();
    add(new JScrollPane(jtable), BorderLayout.CENTER);

    revalidate();

    /* see "How to use Tables"
    sortedModel = new TableSorter(gr);
    jtable.setModel( sortedModel);
    sortedModel.setTableHeader(jtable.getTableHeader()); */
  }

  // display subtables
  private static HashMap<String,IndependentWindow> windows = new HashMap<String,IndependentWindow>();
  private class SubtableAbstractAction extends AbstractAction {
    Structure s;
    StructureTable dataTable;
    IndependentWindow dataWindow;

    SubtableAbstractAction(Structure s) {
      this.s = s;
      dataTable = new StructureTable( null);
      dataWindow = windows.get(s.getName());
      if (dataWindow == null) {
        dataWindow = new IndependentWindow("Data Table", BAMutil.getImage( "netcdfUI"), dataTable);
        windows.put(s.getName(), dataWindow);
      } else {
        dataWindow.setComponent(dataTable);
      }
    }

    public void actionPerformed(java.awt.event.ActionEvent e) {
      StructureData sd = getSelectedStructureData();
      StructureMembers.Member m = sd.findMember( s.getShortName());
      if (m.getDataType() == DataType.STRUCTURE) {
        ArrayStructure as = sd.getArrayStructure( m);
        dataTable.setStructureData( as);

      } else if (m.getDataType() == DataType.SEQUENCE) {
        ArraySequence seq = sd.getArraySequence( m);
        dataTable.setSequenceData( s, seq);

      } else throw new IllegalStateException("data type = "+m.getDataType());

      dataWindow.show();
    }
  }

  private void export() {
    String filename = fileChooser.chooseFilename();
    if (filename == null) return;
    try {
      PrintStream ps = new PrintStream(new FileOutputStream(new File(filename)));

      TableModel model = jtable.getModel();
      for (int col = 0; col < model.getColumnCount(); col++) {
        if (col > 0) ps.print(",");
        ps.print(model.getColumnName(col));
      }
      ps.println();

      for (int row = 0; row < model.getRowCount(); row++) {
        for (int col = 0; col < model.getColumnCount(); col++) {
          if (col > 0) ps.print(",");
          ps.print(model.getValueAt(row, col).toString());
        }
        ps.println();
      }
      ps.close();
        JOptionPane.showMessageDialog(this, "File successfully written");
    } catch (IOException ioe) {
        JOptionPane.showMessageDialog(this, "ERROR: " + ioe.getMessage());
      ioe.printStackTrace();
    }

  }

  private void showData() {
    StructureData sd = getSelectedStructureData();
    if (sd == null) return;

    ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
    try {
      NCdumpW.printStructureData(new PrintWriter(bos), sd);
    } catch (IOException e) {
      String mess = e.getMessage();
      bos.write( mess.getBytes(), 0, mess.length());
    }
    dumpTA.setText(bos.toString());
    dumpWindow.setVisible(true);
  }

  private StructureData getSelectedStructureData() {
    int viewIdx = jtable.getSelectedRow();
    if (viewIdx < 0) return null;
    int modelIdx = viewIdx; // sortedModel.modelIndex( viewIdx); LOOK sort
    try {
      return dataModel.getStructureData(modelIdx);
    } catch (InvalidRangeException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public Object getSelectedRow() {
    int viewIdx = jtable.getSelectedRow();
    if (viewIdx < 0) return null;
    int modelIdx = viewIdx; // sortedModel.modelIndex( viewIdx); LOOK sort
    try {
      return dataModel.getRow(modelIdx);
    } catch (InvalidRangeException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  ////////////////////////////////////////////////////////////////////////////////////////
  private abstract class StructureTableModel extends AbstractTableModel {
    protected HashMapLRU rowHash = new HashMapLRU(500, 500); // cache 500 rows
    protected StructureMembers members;
    protected boolean wantDate = false;
    protected List<Structure> subtables = new ArrayList<Structure>();

    // subclasses implement these
    abstract public StructureData getStructureData(int row) throws InvalidRangeException, IOException;

    // remove all data
    abstract public void clear();

    // if we know how to extract the date for this data, add two extra columns
    abstract public Date getObsDate(int row);
    abstract public Date getNomDate(int row);

    public Object getRow(int row) throws InvalidRangeException, IOException {
      return getStructureData(row);
    }

    public int getColumnCount() {
      if (members == null) return 0;
      return members.getMembers().size() + (wantDate ? 2 : 0);
    }

    public String getColumnName(int columnIndex) {
      if (wantDate && (columnIndex == 0))
        return "obsDate";
      if (wantDate && (columnIndex == 1))
        return "nomDate";
      int memberCol = wantDate ? columnIndex - 2 : columnIndex;
      return members.getMember(memberCol).getName();
    }


    public String getColumnDesc(int columnIndex) {
      if (wantDate && (columnIndex == 0))
        return "Date of observation";
      if (wantDate && (columnIndex == 1))
        return "Nominal Date of collection";
      int memberCol = wantDate ? columnIndex - 2 : columnIndex;
      StructureMembers.Member m = members.getMember(memberCol);
      return m.getUnitsString();
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
      return sd.getScalarObject(colName);
    }

    String enumLookup(StructureMembers.Member m, Number val) { return "sorry"; }
    
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

    public Date getObsDate(int row) {
      return null;
    }

    public Date getNomDate(int row) {
      return null;
    }

    public int getRowCount() {
      if (struct == null) return 0;
      return (int) struct.getSize();
    }

    public StructureData getStructureData(int row) throws InvalidRangeException, IOException {
      return struct.readStructure(row);
    }

    public Object getValueAt(int row, int column) {
      return super.getValueAt(row, column);
    }

    public void clear() {
      struct = null;
      fireTableDataChanged();
    }

    String enumLookup(StructureMembers.Member m, Number val) {
      Variable v = struct.findVariable(m.getName());
      return v.lookupEnumString( val.intValue());
    }

  }

  // handles Sequences
  private class SequenceModel extends StructureModel {
    protected List<StructureData> sdataList;

    SequenceModel(Structure seq, boolean readData) {
      super(seq);

      if (readData) {
        sdataList = new ArrayList<StructureData>();
        try {
          StructureDataIterator iter = seq.getStructureIterator();
          while (iter.hasNext())
            sdataList.add( iter.next());

        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    public Date getObsDate(int row) {
      return null;
    }

    public Date getNomDate(int row) {
      return null;
    }

    public int getRowCount() {
      return sdataList.size();
    }

    public StructureData getStructureData(int row) throws InvalidRangeException, IOException {
      return sdataList.get(row);
    }

    public Object getValueAt(int row, int column) {
      StructureData sd = sdataList.get(row);
      return sd.getScalarObject( sd.getStructureMembers().getMember( column));
    }

    public void clear() {
      sdataList = new ArrayList<StructureData>();
      fireTableDataChanged();
    }
  }

  private class ArraySequenceModel extends SequenceModel {

    ArraySequenceModel(Structure s, ArraySequence seq) {
      super(s, false);

      this.members = seq.getStructureMembers();

      sdataList = new ArrayList<StructureData>();
      try {
        StructureDataIterator iter = seq.getStructureDataIterator();
        while (iter.hasNext())
          sdataList.add( iter.next());

      } catch (IOException e) {
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

    public Date getObsDate(int row) {
      return null;
    }

    public Date getNomDate(int row) {
      return null;
    }

    public int getRowCount() {
      return structureData.size();
    }

    public StructureData getStructureData(int row) throws InvalidRangeException, IOException {
      return structureData.get(row);
    }

    public void clear() {
      structureData = new ArrayList<StructureData>(); // empty list
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

    public Date getObsDate(int row) {
      return null;
    }

    public Date getNomDate(int row) {
      return null;
    }

    public int getRowCount() {
      return (as == null) ? 0 : (int) as.getSize();
    }

    public StructureData getStructureData(int row) throws InvalidRangeException, IOException {
      return as.getStructureData( row);
    }

    public void clear() {
      as = null;
      fireTableDataChanged();
    }

  }

  ////////////////////////////////////////////////////////////////////////
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

  ////////////////////////////////////////////////////////////////////////
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
  }

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
          throw new IOException(e.getMessage());
        }
        this.members = sd.getStructureMembers();
      }
    }

    public Date getObsDate(int row) {
      PointFeature obs = obsData.get(row);
      return obs.getObservationTimeAsDate();
    }

    public Date getNomDate(int row) {
      PointFeature obs = obsData.get(row);
      return obs.getNominalTimeAsDate();
    }

     public int getRowCount() {
      return obsData.size();
    }

    public StructureData getStructureData(int row) throws InvalidRangeException, IOException {
      PointFeature obs = obsData.get(row);
      return obs.getData();
    }

    public Object getRow(int row) throws InvalidRangeException, IOException {
      return obsData.get(row); // PointObsDatatype
    }

    public void clear() {
      obsData = new ArrayList<PointFeature>(); // empty list
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

  /* private class SortedHeaderRenderer implements TableCellRenderer {
   private int modelIdx;
   private JPanel compPanel;
   private JLabel headerLabel;
   private boolean hasSortIndicator = false;
   private boolean reverse = false;

   SortedHeaderRenderer(int modelIdx, String header, String tooltip) {
     this.modelIdx = modelIdx;

     // java.beans.PropertyDescriptor pd = model.getProperty(modelIdx);
     //headerLabel = new JLabel(pd.getDisplayName());

     headerLabel = new JLabel(header);
     compPanel = new JPanel(new BorderLayout());
     compPanel.setBorder(new BevelBorder(BevelBorder.RAISED));
     compPanel.add( headerLabel, BorderLayout.CENTER);

     if (null != tooltip)
       compPanel.setToolTipText(tooltip);
     ToolTipManager.sharedInstance().registerComponent(compPanel);
   }

   public Component getTableCellRendererComponent(JTable table, Object value,
       boolean isSelected, boolean hasFocus, int row, int column) {
     // System.out.println("getTableCellRendererComponent= "+headerLabel.getText());
     return compPanel;
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
 } */

  /**
   * Renderer for Date type
   */
  static class DateRenderer extends DefaultTableCellRenderer {
    private java.text.SimpleDateFormat newForm, oldForm;
    private Date cutoff;

    DateRenderer() {
      super();

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

  private ucar.util.prefs.ui.TableSorter sortedModel;
  private JTable setModel(TableModel m) {
    sortedModel = new TableSorter(m);
    MyJTable jtable = new MyJTable(sortedModel);
    sortedModel.setTableHeader( jtable.getTableHeader());
    return jtable;
  }

  protected int modelIndex(int viewIndex) {
    return sortedModel.modelIndex(viewIndex);
  }

  protected int viewIndex(int rowIndex) {
    return sortedModel.viewIndex(rowIndex);
  }

  //Implement table header tool tips.
  static private class MyJTable extends JTable {

    MyJTable(TableModel m) {
      super( m);
    }

    protected JTableHeader createDefaultTableHeader() {
      return new MyJTableHeader(columnModel, getModel());
    }

    class MyJTableHeader extends javax.swing.table.JTableHeader {
      TableModel tm;

      MyJTableHeader(TableColumnModel tcm, TableModel tm) {
        super(tcm);
        this.tm = tm;
      }

      public String getToolTipText(MouseEvent e) {
        java.awt.Point p = e.getPoint();
        int index = columnModel.getColumnIndexAtX(p.x);
        int realIndex = columnModel.getColumn(index).getModelIndex();
        if (realIndex >= 0)
          return tm.getColumnName(realIndex);
        else
          return null;
      }
    }

  } 

}