package ucar.nc2.ui.coverage2;

import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.constants.AxisType;
//import ucar.nc2.dataset.*;
//import ucar.nc2.ft.fmrc.GridDatasetInv;
//import ucar.nc2.ft.cover.Coverage;
//import ucar.nc2.ft.cover.CoverageCS;
//import ucar.nc2.ft.cover.CoverageDataset;
//import ucar.nc2.ft.cover.impl.CoverageDatasetImpl;
import ucar.nc2.ft2.coverage.grid.*;
import ucar.nc2.ui.dialog.NetcdfOutputChooser;
import ucar.nc2.ui.widget.*;
import ucar.nc2.ui.widget.PopupMenu;
//import ucar.unidata.geoloc.ProjectionImpl;
import ucar.nc2.util.Indent;
import ucar.nc2.util.NamedObject;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTable;

import javax.swing.*;
//import java.awt.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Bean Table for GridCoverage
 *
 * @author John
 * @since 12/25/12
 */
public class CoverageTable extends JPanel {
  private PreferencesExt prefs;
  private GridCoverageDataset gridDataset;

  private BeanTable gridTable, csTable, axisTable, transTable;
  private JSplitPane split = null, split2 = null, split3 = null;
  private TextHistoryPane infoTA;
  private IndependentWindow infoWindow;
  private NetcdfOutputChooser outChooser;

  public CoverageTable(PreferencesExt prefs) {
    this.prefs = prefs;

    gridTable = new BeanTable(GridBean.class, (PreferencesExt) prefs.node("GeogridBeans"), false, "GridCoverage", "ucar.nc2.ft2.coverage.GridCoverage", null);
    csTable = new BeanTable(CoordSysBean.class, (PreferencesExt) prefs.node("GeoCoordinateSystemBean"), false, "GridCoordSys", "ucar.nc2.ft2.coverage.GridCoordSys", null);
    axisTable = new BeanTable(AxisBean.class, (PreferencesExt) prefs.node("GeoCoordinateAxisBean"), false, "GridCoordAxis", "ucar.nc2.ft2.coverage.GridCoordAxis", null);
    transTable = new BeanTable(CoordTransBean.class, (PreferencesExt) prefs.node("GeoCoordinateTransformBean"), false, "GridCoordTransform", "ucar.nc2.ft2.coverage.GridCoordTransform", null);

    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Variable Information", BAMutil.getImage("netcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    // layout
    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, gridTable, csTable);
    split.setDividerLocation(prefs.getInt("splitPos", 300));

    split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, split, transTable);
    split2.setDividerLocation(prefs.getInt("splitPos2", 200));

    split3 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, split2, axisTable);
    split3.setDividerLocation(prefs.getInt("splitPos3", 200));

    setLayout(new BorderLayout());
    add(split3, BorderLayout.CENTER);

    // context menu
    JTable jtable = gridTable.getJTable();
    PopupMenu csPopup = new ucar.nc2.ui.widget.PopupMenu(jtable, "Options");
    csPopup.addAction("Show Declaration", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        GridBean vb = (GridBean) gridTable.getSelectedBean();
        infoTA.clear();
        infoTA.appendLine(vb.geogrid.toString());
        infoTA.gotoTop();
        infoWindow.show();
      }
    });

    jtable = axisTable.getJTable();
    csPopup = new ucar.nc2.ui.widget.PopupMenu(jtable, "Options");
    csPopup.addAction("Show Axis", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        AxisBean bean = (AxisBean) axisTable.getSelectedBean();
        infoTA.clear();
        infoTA.appendLine(bean.axis.toString());
        infoTA.gotoTop();
        infoWindow.show();
      }
    });
    csPopup.addAction("Show Coord Value differences", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        AxisBean bean = (AxisBean) axisTable.getSelectedBean();
        infoTA.clear();
        infoTA.appendLine(bean.showCoordValueDiffs());
        infoTA.gotoTop();
        infoWindow.show();
      }
    });

  }

  /* public void addExtra(JPanel buttPanel, final FileManager fileChooser) {

    AbstractButton infoButton = BAMutil.makeButtcon("Information", "Parse Info", false);
    infoButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if ((gridDataset != null) && (gridDataset instanceof ucar.nc2.dt.grid.GridDataset)) {
          ucar.nc2.dt.grid.GridDataset gdsImpl = (ucar.nc2.dt.grid.GridDataset) gridDataset;
          infoTA.clear();
          infoTA.appendLine(gdsImpl.getDetailInfo());
          infoTA.gotoTop();
          infoWindow.show();
        }
      }
    });
    buttPanel.add(infoButton);

    /*

    /* AbstractAction netcdfAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        if (gridDataset == null) return;
        List<String> gridList = getSelectedGrids();
        if (gridList.size() == 0) {
          JOptionPane.showMessageDialog(CoverageTable.this, "No Grids are selected");
          return;
        }

        if (outChooser == null) {
          outChooser = new NetcdfOutputChooser((Frame) null);
          outChooser.addPropertyChangeListener("OK", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
              writeNetcdf((NetcdfOutputChooser.Data) evt.getNewValue());
            }
          });
        }
        outChooser.setOutputFilename(gridDataset.getLocation());
        outChooser.setVisible(true);
      }
    };
    BAMutil.setActionProperties(netcdfAction, "netcdf", "Write netCDF-CF file", false, 'S', -1);
    BAMutil.addActionToContainer(buttPanel, netcdfAction);  */

    /*
 AbstractAction writeAction = new AbstractAction() {
   public void actionPerformed(ActionEvent e) {
     if (gridDataset == null) return;
     List<String> gridList = getSelectedGrids();
     if (gridList.size() == 0) {
       JOptionPane.showMessageDialog(GeoGridTable.this, "No Grids are selected");
       return;
     }
     String location = gridDataset.getLocationURI();
     if (location == null) location = "test";
     String suffix = (location.endsWith(".nc") ? ".sub.nc" : ".nc");
     int pos = location.lastIndexOf(".");
     if (pos > 0)
       location = location.substring(0, pos);

     String filename = fileChooser.chooseFilenameToSave(location + suffix);
     if (filename == null) return;

     try {
       NetcdfCFWriter.makeFileVersioned(filename, gridDataset, gridList, null, null);
       JOptionPane.showMessageDialog(GeoGridTable.this, "File successfully written");
     } catch (Exception ioe) {
       JOptionPane.showMessageDialog(GeoGridTable.this, "ERROR: " + ioe.getMessage());
       ioe.printStackTrace();
     }
   }
 };
 BAMutil.setActionProperties(writeAction, "netcdf", "Write netCDF-CF file", false, 'W', -1);
 BAMutil.addActionToContainer(buttPanel, writeAction);
  //; }

  private void showCoordinates(CoverageBean vb, Formatter f) {
    CoverageCS gcs = vb.geogrid.getCoordinateSystem();
    gcs.show(f, true);
  }

  /* private void writeNetcdf(NetcdfOutputChooser.Data data) {
    if (data.version == NetcdfFileWriter.Version.ncstream) return;

    try {
      NetcdfCFWriter.makeFileVersioned(data.outputFilename, gridDataset, getSelectedGrids(), null, null, data.version);
      JOptionPane.showMessageDialog(this, "File successfully written");
    } catch (Exception ioe) {
      JOptionPane.showMessageDialog(this, "ERROR: " + ioe.getMessage());
      ioe.printStackTrace();
    }
  } */

  public void clear() {
    gridTable.clearBeans();
    csTable.clearBeans();
    axisTable.clearBeans();
    transTable.clearBeans();
  }

  public void save() {
    gridTable.saveState(false);
    csTable.saveState(false);
    axisTable.saveState(false);
    transTable.saveState(false);

    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putInt("splitPos", split.getDividerLocation());
    prefs.putInt("splitPos2", split2.getDividerLocation());
    prefs.putInt("splitPos3", split3.getDividerLocation());
  }

  /* public void setDataset(NetcdfDataset ds, Formatter parseInfo) throws IOException {
    this.gridDataset = new CoverageDatasetImpl(ds, parseInfo);

    List<CoverageBean> beanList = new ArrayList<CoverageBean>();
    java.util.List<Coverage> list = gridDataset.getCoverages();
    for (Coverage g : list)
      beanList.add(new CoverageBean(g));
    varTable.setBeans(beanList);

    if (csTable != null) {
      List<CoverageCSBean> csList = new ArrayList<CoverageCSBean>();
      List<AxisBean> axisList;
      axisList = new ArrayList<AxisBean>();
      for (CoverageDataset.CoverageSet gset : gridDataset.getCoverageSets()) {
        csList.add(new CoverageCSBean(gset));
        CoverageCS gsys = gset.getCoverageCS();
        List<CoordinateAxis> axes = gsys.getCoordinateAxes();
        for (int i = 0; i < axes.size(); i++) {
          CoordinateAxis axis = axes.get(i);
          AxisBean axisBean = new AxisBean(axis);
          if (!contains(axisList, axisBean.getName()))
            axisList.add(axisBean);
        }
      }
      csTable.setBeans(csList);
      axisTable.setBeans(axisList);
    }
  }  */

  public void setDataset(GridCoverageDataset gds) throws IOException {
    this.gridDataset = gds;

    List<GridBean> beanList = new ArrayList<>();
    for (GridCoverage g : gridDataset.getGrids())
      beanList.add(new GridBean(g));
    gridTable.setBeans(beanList);

    List<CoordSysBean> csList = new ArrayList<>();
    for (GridCoordSys gcs : gridDataset.getCoordSys())
      csList.add(new CoordSysBean(gcs));
    csTable.setBeans(csList);

    List<CoordTransBean> transList = new ArrayList<>();
    for (GridCoordTransform t : gridDataset.getCoordTransforms())
      transList.add(new CoordTransBean(t));
    transTable.setBeans(transList);

    List<AxisBean> axisList = new ArrayList<>();
    for (GridCoordAxis axis : gridDataset.getCoordAxes())
      axisList.add(new AxisBean(axis));
    axisTable.setBeans(axisList);
  }

  private boolean contains(List<AxisBean> axisList, String name) {
    for (AxisBean axis : axisList)
      if (axis.getName().equals(name)) return true;
    return false;
  }

  public GridCoverageDataset getCoverageDataset() {
    return gridDataset;
  }

  public List<GridBean> getGridBeans() {
    return (List<GridBean>) gridTable.getBeans();
  }

  public List<String> getSelectedGrids() {
    List grids = gridTable.getSelectedBeans();
    List<String> result = new ArrayList<>();
    for (Object bean : grids) {
      GridBean gbean = (GridBean) bean;
      result.add(gbean.getName());
    }
    return result;
  }

  public class GridBean implements NamedObject {
    // static public String editableProperties() { return "title include logging freq"; }

    GridCoverage geogrid;
    String name, desc, units, coordSysName;
    DataType dataType;

    // no-arg constructor
    public GridBean() {
    }

    // create from a dataset
    public GridBean(GridCoverage geogrid) {
      this.geogrid = geogrid;
      setName(geogrid.getName());
      setDescription(geogrid.getDescription());
      setUnits(geogrid.getUnits());
      setDataType(geogrid.getDataType());
      setCoordSysName(geogrid.getCoordSysName());

      /* collect dimensions
      StringBuffer buff = new StringBuffer();
      java.util.List dims = geogrid.getDimensions();
      for (int j = 0; j < dims.size(); j++) {
        ucar.nc2.Dimension dim = (ucar.nc2.Dimension) dims.get(j);
        if (j > 0) buff.append(",");
        buff.append(dim.getLength());
      }
      setShape(buff.toString());

      CoverageCS gcs = geogrid.getCoordinateSystem();
      x = getAxisName(gcs.getXHorizAxis());
      y = getAxisName(gcs.getYHorizAxis());
      z = getAxisName(gcs.getVerticalAxis());
      t = getAxisName(gcs.getTimeAxis());

      Formatter f = new Formatter();
      List<ucar.nc2.Dimension> domain = gcs.getDomain();
      int count = 0;
      for (ucar.nc2.Dimension d : geogrid.getDimensions()) {
        if (!domain.contains(d)) {
          if (count++ > 0) f.format(",");
          f.format("%s",d.getShortName());
        }
      }
      extra = f.toString();  */
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getDescription() {
      return desc;
    }

    public void setDescription(String desc) {
      this.desc = desc;
    }

    public String getUnits() {
      return units;
    }

    public void setUnits(String units) {
      this.units = units;
    }

    public String getCoordSysName() {
      return coordSysName;
    }

    public void setCoordSysName(String coordSysName) {
      this.coordSysName = coordSysName;
    }

    public DataType getDataType() {
      return dataType;
    }

    public void setDataType(DataType dataType) {
      this.dataType = dataType;
    }
  }

  public class CoordSysBean {
    private GridCoordSys gcs;
    private String coordTrans, axisNames;

    // no-arg constructor
    public CoordSysBean() {
    }

    public CoordSysBean(GridCoordSys gcs) {
      this.gcs = gcs;

      Formatter buff = new Formatter();
      for (String ct : gcs.getTransformNames())
        buff.format("%s,", ct);
      setCoordTransforms(buff.toString());

      Formatter f = new Formatter();
      for (String ax : gcs.getAxisNames())
        f.format("%s,", ax);
      setAxisNames(f.toString());
    }

    public String getName() {
      return gcs.getName();
    }

    public String getType() {
      GridCoordSys.Type type = gcs.getType();
      return (type == null) ? "" : type.toString();
    }

    public int getNAxes() {
      return gcs.getAxisNames().size();
    }

    public String getAxisNames() {
      return axisNames;
    }

    public void setAxisNames(String axisNames) {
      this.axisNames = axisNames;
    }

    public String getCoordTransforms() {
      return coordTrans;
    }

    public void setCoordTransforms(String coordTrans) {
      this.coordTrans = coordTrans;
    }
  }

  public class CoordTransBean {
    private GridCoordTransform gcs;
    String params;
    boolean isHoriz;

    // no-arg constructor
    public CoordTransBean() {
    }

    public CoordTransBean(GridCoordTransform gcs) {
      this.gcs = gcs;
      this.isHoriz = gcs.isHoriz();

      Formatter buff = new Formatter();
      for (Attribute att : gcs.getAttributes())
        buff.format("%s, ", att);
      params = buff.toString();
    }

    public String getName() {
      return gcs.getName();
    }

    public String getParams() {
      return params;
    }

    public String getIsHoriz() {
      return Boolean.toString(isHoriz);
    }
  }

  public class AxisBean {
    GridCoordAxis axis;
    String name, desc, units;
    DataType dataType;
    AxisType axisType;
    long nvalues;
    double resolution;
    boolean indepenent;

    // no-arg constructor
    public AxisBean() {
    }

    // create from a dataset
    public AxisBean(GridCoordAxis v) {
      this.axis = v;

      setName(v.getName());
      setDataType(v.getDataType());
      setAxisType(v.getAxisType());
      setUnits(v.getUnits());
      setDescription(v.getDescription());
      setNvalues(v.getNvalues());
      setResolution(v.getResolution());
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getAxisType() {
      return axisType.name();
    }

    public void setAxisType(AxisType axisType) {
      this.axisType = axisType;
    }

    public String getDescription() {
      return desc;
    }

    public void setDescription(String desc) {
      this.desc = desc;
    }

    public String getUnits() {
      return units;
    }

    public void setUnits(String units) {
      this.units = (units == null) ? "null" : units;
    }

    public DataType getDataType() {
      return dataType;
    }

    public void setDataType(DataType dataType) {
      this.dataType = dataType;
    }

    public String getSpacing() {
      return axis.getSpacing().toString();
    }

    public long getNvalues() {
      return nvalues;
    }

    public void setNvalues(long nvalues) {
      this.nvalues = nvalues;
    }

    public String getStartValue() {
      return String.format("%8.3f", axis.getStartValue());
    }

    public String getEndValue() {
      return String.format("%8.3f", axis.getEndValue());
    }

    public String getResolution() {
      return String.format("%8.3f", resolution);
    }

    public void setResolution(double resolution) {
      this.resolution = resolution;
    }

    public boolean getHasData() {
      return axis.getValues() != null;
    }

    public String getDependsOn() {
      if (axis.getDependenceType() == GridCoordAxis.DependenceType.dependent)
        return axis.getDependenceType()+": "+axis.getDependsOn();
      else
        return axis.getDependenceType().toString();
    }

    String showCoordValueDiffs() {
      Formatter f = new Formatter();
      double[] values = axis.getValues();
      int n = values.length;
      switch (axis.getSpacing()) {
        case irregularPoint:
        case contiguousInterval:
          f.format("%n%s (npts=%d)%n", axis.getSpacing(), n);
          for (int i=0; i<n-1; i++) {
            double diff = values[i+1] - values[i];
            f.format("%10f %10f == %10f%n", values[i], values[i+1], diff);
          }
          f.format("%n");
          break;

        case discontiguousInterval:
           f.format("%ndiscontiguous intervals (npts=%d)%n",n);
           for (int i=0; i<n; i+=2) {
             double diff = values[i + 1] - values[i];
             f.format("(%10f,%10f) = %10f%n", values[i], values[i + 1], diff);
           }
           f.format("%n");
           break;
       }
      return f.toString();
    }
  }

  /**
   * Wrap this in a JDialog component.
   *
   * @param parent JFrame (application) or JApplet (applet) or null
   * @param title  dialog window title
   * @param modal  modal dialog or not
   * @return JDialog
   */
  public JDialog makeDialog(RootPaneContainer parent, String title, boolean modal) {
    return new Dialog(parent, title, modal);
  }

  private class Dialog extends JDialog {

    private Dialog(RootPaneContainer parent, String title, boolean modal) {
      super(parent instanceof Frame ? (Frame) parent : null, title, modal);

      // L&F may change
      UIManager.addPropertyChangeListener(new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent e) {
          if (e.getPropertyName().equals("lookAndFeel"))
            SwingUtilities.updateComponentTreeUI(CoverageTable.Dialog.this);
        }
      });

      /* add a dismiss button
      JButton dismissButton = new JButton("Dismiss");
      buttPanel.add(dismissButton, null);

      dismissButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          setVisible(false);
        }
      }); */

      // add it to contentPane
      Container cp = getContentPane();
      cp.setLayout(new BorderLayout());
      cp.add(CoverageTable.this, BorderLayout.CENTER);
      pack();
    }
  }
}
