package ucar.nc2.ui;

import ucar.ma2.*;
import ucar.nc2.*;
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

/**
 * This puts the data values of a 1D Structure or Sequence into a JTable.
 * The columns are the members of the Structure.
 */
public class StructureTable extends JPanel {
  private PreferencesExt mainPrefs;
  private StructureTableModel dataModel;

  private JTable jtable;
  private PopupMenu popup;
  private FileManager fileChooser; // for exporting
  private TextHistoryPane dumpTA;
  private IndependentDialog dumpWindow;

  public StructureTable(PreferencesExt prefs) {
    this.mainPrefs = prefs;

    jtable = new JTable();
    setLayout( new BorderLayout());
    jtable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

    ToolTipManager.sharedInstance().registerComponent( jtable);

    // reset popup
    popup = null;
    addActionToPopupMenu("Show", new AbstractAction() {
      public void actionPerformed( java.awt.event.ActionEvent e) {
        showData();
      }
    });

    addActionToPopupMenu("Export", new AbstractAction() {
      public void actionPerformed( java.awt.event.ActionEvent e) {
        export();
      }
    });

    //JScrollPane sp =  new JScrollPane(jtable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    add( new JScrollPane(jtable), BorderLayout.CENTER);

    // other widgets
    dumpTA = new TextHistoryPane( false);
    dumpWindow = new IndependentDialog(null, true, "Show Data", dumpTA);
    dumpWindow.setBounds( (Rectangle) prefs.getBean("DumpWindowBounds", new Rectangle( 300, 300, 300, 200)));

    PreferencesExt fcPrefs = (PreferencesExt) prefs.node("FileManager");
    fileChooser = new FileManager(null, null, "csv", "comma seperated values", fcPrefs);
  }

  private EventListenerList listeners = new EventListenerList();

    /** Add listener: ListSelectionEvent sent when a new row is selected */
  public void addListSelectionListener(ListSelectionListener l) {
    listeners.add(javax.swing.event.ListSelectionListener.class, l);
  }
  /** Remove listener */
  public void removeListSelectionListener(ListSelectionListener l) {
    listeners.remove(javax.swing.event.ListSelectionListener.class, l);
  }

  private void fireEvent(javax.swing.event.ListSelectionEvent event) {
    Object[] llist = listeners.getListenerList();
    // Process the listeners last to first
    for (int i = llist.length-2; i>=0; i-=2)
      ((javax.swing.event.ListSelectionListener)llist[i+1]).valueChanged(event);
  }

  public void addActionToPopupMenu( String title, AbstractAction act) {
    if (popup == null) popup = new PopupMenu(jtable, "Options");
    popup.addAction( title, act);
  }

  // clear the table
  public void clear() {
    if (dataModel != null)
      dataModel.clear();
  }

  // save state
  public void saveState() {
    fileChooser.save();
  }

  /** This is used when we have a Structure */
  public void setStructure( Structure s) throws IOException {
    //cache = new HashMap();
    dataModel = new StructureModel(s);
    initTable( dataModel);
  }

  /**
   * Set the data as a collection of StructureData.
   * @param structureData List of type StructureData
   * @throws IOException
   */
  public void setStructureData( List structureData) throws IOException {
    dataModel = new StructureDataModel( structureData);
    initTable( dataModel);
  }

   /**
   * This is used for a trajectory.
   * @param traj treajectory
   * @throws IOException
   */
  public void setTrajectory( TrajectoryObsDatatype traj) throws IOException {
    dataModel = new TrajectoryModel( traj);
    initTable( dataModel);
  }

  /**
   * Set the data as a collection of PointObsDatatype.
   * @param obsData List of type PointObsDatatype
   * @throws IOException
   */
  public void setPointObsData( List obsData) throws IOException {
    dataModel = new PointObsDataModel( obsData);
    initTable( dataModel);
    jtable.getColumnModel().getColumn(0).setCellRenderer(new DateRenderer());
  }

  private void initTable2( StructureTableModel m) {
    jtable.setModel(m);
  }

  private void initTable( StructureTableModel m) {
    jtable = new MyJTable(m);

    ListSelectionModel rowSM = jtable.getSelectionModel();
    rowSM.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;  //Ignore extra messages.
        ListSelectionModel lsm = (ListSelectionModel)e.getSource();
        if (!lsm.isSelectionEmpty())
          fireEvent( e);
      }
    });

    if (m.wantDate)
      jtable.getColumnModel().getColumn(0).setCellRenderer(new DateRenderer());
    // jtable.setDefaultRenderer(Date.class, new DateRenderer()); LOOK this doesnt work!
    jtable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

    // reset popup
    popup = null;
    addActionToPopupMenu("Show", new AbstractAction() {
      public void actionPerformed( java.awt.event.ActionEvent e) {
        showData();
      }
    });

    addActionToPopupMenu("Export", new AbstractAction() {
      public void actionPerformed( java.awt.event.ActionEvent e) {
        export();
      }
    });

    //JScrollPane sp =  new JScrollPane(jtable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    removeAll();
    add( new JScrollPane(jtable), BorderLayout.CENTER);

    revalidate();

    /* see "How to use Tables"
    sortedModel = new TableSorter(m);
    jtable.setModel( sortedModel);
    sortedModel.setTableHeader(jtable.getTableHeader()); */
  }

  private void export() {
    String filename = fileChooser.chooseFilename();
    if (filename == null) return;
    try {
      PrintStream ps = new PrintStream( new FileOutputStream( new File(filename)));

      TableModel model = jtable.getModel();
      for (int col=0; col<model.getColumnCount(); col++) {
        if (col > 0) ps.print(",");
        ps.print(model.getColumnName(col));
      }
      ps.println();

      for (int row=0; row<model.getRowCount(); row++) {
        for (int col=0; col<model.getColumnCount(); col++) {
          if (col > 0) ps.print(",");
          ps.print(model.getValueAt(row, col).toString());
        }
        ps.println();
      }
      ps.close();
      try { JOptionPane.showMessageDialog(this, "File successfully written"); } catch (HeadlessException e) { }
    } catch (IOException ioe) {
      try { JOptionPane.showMessageDialog(this, "ERROR: "+ioe.getMessage()); } catch (HeadlessException e) { }
      ioe.printStackTrace();
    }

  }

  private void showData() {
    StructureData sd = getSelectedStructureData();
    if (sd == null) return;

    ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
    NCdump.printStructureData(new PrintStream( bos), sd);
    dumpTA.setText( bos.toString());
    dumpWindow.show();
  }

  private StructureData getSelectedStructureData() {
    int viewIdx = jtable.getSelectedRow();
    if (viewIdx < 0) return null;
    int modelIdx = viewIdx; // sortedModel.modelIndex( viewIdx); LOOK sort
    try {
      return dataModel.getStructureData(modelIdx);
    } catch (InvalidRangeException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    } catch (IOException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
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
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    } catch (IOException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
    return null;
  }

  ////////////////////////////////////////////////////////////////////////////////////////
  private abstract class StructureTableModel extends AbstractTableModel {
    protected HashMapLRU rowHash = new HashMapLRU(500, 500); // cache 500 rows
    protected StructureMembers members;
    protected boolean wantDate = false;

    // subclasses implement these
    abstract public StructureData getStructureData(int row) throws InvalidRangeException, IOException;

    // remove all data
    abstract public void clear();

    // if we know how to extract the date for this data, add an extra column
    abstract public Date getDate(int row);

    public Object getRow(int row) throws InvalidRangeException, IOException {
      return getStructureData( row);
    }

    public int getColumnCount() {
      if (members == null) return 0;
      return members.getMembers().size() + (wantDate ? 1 : 0);
    }

    public String getColumnName(int columnIndex) {
      if (wantDate && (columnIndex == 0))
        return "date";
      int memberCol = wantDate ? columnIndex-1 : columnIndex;
      return members.getMember(memberCol).getName();
    }


    public String getColumnDesc(int columnIndex) {
      if (wantDate && (columnIndex == 0))
        return "Date of observation";
      int memberCol = wantDate ? columnIndex-1 : columnIndex;
      StructureMembers.Member m = members.getMember(memberCol);
      return m.getUnitsString();
    }

    /** get row data if in the cache, otherwise read it */
    public StructureData getStructureDataHash(int row) throws InvalidRangeException, IOException {
      Integer key =  new Integer(row);
      StructureData sd = (StructureData) rowHash.get( key);
      if (sd == null) {
        sd = getStructureData(row);
        rowHash.put( key, sd);
      }
      return sd;
    }

    public Object getValueAt(int row, int column) {
      if (wantDate && (column == 0))
        return getDate(row);
      int memberCol = wantDate ? column-1 : column;

      StructureData sd;
      try {
        sd = getStructureDataHash(row);
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        return "ERROR "+e.getMessage();
      } catch (IOException e) {
        e.printStackTrace();
        return "ERROR "+e.getMessage();
      }
      return sd.getScalarObject(sd.getStructureMembers().getMember(memberCol));
    }

  }

  // handles Structures and Sequences.
  private class StructureModel extends StructureTableModel {
    private Structure struct;
    private ArrayStructure seqData;

    StructureModel( Structure s) {
      this.struct = s;
      this.members = s.makeStructureMembers();

      // are any parents variable length ??
      boolean isVariableLength = false;
      boolean hasParent = false;
      Variable v = struct;
      while (v != null) {
        if (v.isVariableLength())
          isVariableLength = true;
        v = v.getParentStructure();
        if (v != null)
          hasParent = true;
      }

      if (isVariableLength) {
        // for now, we have to read entire sequence into memory !!
        try {
          Array seqDataArray = hasParent ? struct.readAllStructures(null, true) : struct.read();
          seqData = (ArrayStructure) seqDataArray;

        } catch (InvalidRangeException e) {
          e.printStackTrace();
          throw new RuntimeException( e.getMessage()); // cant happen

        } catch (IOException ex) {
          ex.printStackTrace();
          throw new RuntimeException( ex.getMessage());
        }
      }
    }

    public Date getDate(int row) { return null; }

    public int getRowCount() {
      if (struct == null) return 0;
      if (seqData != null) return (int) seqData.getSize();
      return (int) struct.getSize();
    }

    public StructureData getStructureData(int row) throws InvalidRangeException, IOException {
       if (seqData != null) { // its a sequence
        return seqData.getStructureData(row);
      }
      StructureData data = struct.readStructure(row);
      return data;
    }

    public Object getValueAt(int row, int column) {
      if (seqData != null) { // its a sequence
        StructureData sd = seqData.getStructureData(row);
        return sd.getScalarObject(sd.getStructureMembers().getMember(column));
     }

      return super.getValueAt(row, column);
    }

    public void clear() {
      struct = null;
      fireTableDataChanged();
    }
  }

  private class StructureDataModel extends StructureTableModel {
    private List structureData;

    StructureDataModel( List structureData) {
      this.structureData = structureData;
      if (structureData.size() > 0) {
        StructureData sd = (StructureData) structureData.get(0);
        this.members = sd.getStructureMembers();
      }
    }

     public Date getDate(int row) { return null; }

    public int getRowCount() {
      return structureData.size();
    }

    public StructureData getStructureData(int row) throws InvalidRangeException, IOException {
      return (StructureData) structureData.get( row);
    }

    public void clear() {
      structureData = new ArrayList(); // empty list
      fireTableDataChanged();
    }
  }

  private class TrajectoryModel extends StructureTableModel {
    private TrajectoryObsDatatype traj;

    TrajectoryModel( TrajectoryObsDatatype traj) throws IOException {
      this.traj = traj;
      StructureData sd = null;
      if (traj.getNumberPoints() > 0) {
        try {
          sd = traj.getData( 0);
          this.members = sd.getStructureMembers();
        } catch (InvalidRangeException e) {
          throw new IOException( e.getMessage());
        }
      }
      wantDate = true;
    }

    public Date getDate(int row) {
      try {
        return traj.getTime( row);
      } catch (IOException e) {
        return null;
      }
    }

    public int getRowCount() {
      return (traj == null) ? 0 : traj.getNumberPoints();
    }

    public Object getRow(int row) throws InvalidRangeException, IOException {
      return traj.getPointObsData( row); // PointObsDatatype
    }

    public StructureData getStructureData(int row) throws InvalidRangeException, IOException {
      return traj.getData( row);
    }

    public void clear() {
      traj = null;
      fireTableDataChanged();
    }
  }

  private class PointObsDataModel extends StructureTableModel {
    private List obsData;

    PointObsDataModel( List obsData) throws IOException {
      wantDate = true;

      this.obsData = obsData;
      if (obsData.size() > 0) {
        StructureData sd = null;
        try {
          sd = getStructureData(0);
        } catch (InvalidRangeException e) {
          throw new IOException( e.getMessage());
        }
        this.members = sd.getStructureMembers();
      }
    }

    public Date getDate(int row) {
      PointObsDatatype obs = (PointObsDatatype) obsData.get( row);
      Date d = obs.getObservationTimeAsDate();
      return d;
    }

    public int getRowCount() {
      return obsData.size();
    }

    public StructureData getStructureData(int row) throws InvalidRangeException, IOException {
      PointObsDatatype obs = (PointObsDatatype) obsData.get( row);
      return obs.getData();
    }

     public Object getRow(int row) throws InvalidRangeException, IOException {
      return obsData.get( row); // PointObsDatatype
    }

    public void clear() {
      obsData = new ArrayList(); // empty list
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
      StructureData.Member m = (StructureData.Member) arrays.get( column);
      Array a = m.data;
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

  /** Renderer for Date type */
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

        //Implement table header tool tips.
  static class MyJTable extends JTable {
    MyJTable( TableModel m) {
      super(m);
    }

    protected JTableHeader createDefaultTableHeader() {
      return new MyJTableHeader( columnModel, (StructureTableModel) getModel());
    }

    class MyJTableHeader extends javax.swing.table.JTableHeader {
      StructureTableModel tm;
      MyJTableHeader( TableColumnModel tcm, StructureTableModel tm) {
        super(tcm);
        this.tm = tm;
      }

      public String getToolTipText(MouseEvent e) {
        java.awt.Point p = e.getPoint();
        int index = columnModel.getColumnIndexAtX(p.x);
        int realIndex = columnModel.getColumn(index).getModelIndex();
        return tm.getColumnDesc(realIndex);
      }
    }

  }

}