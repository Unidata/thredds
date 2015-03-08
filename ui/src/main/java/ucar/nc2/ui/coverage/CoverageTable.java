package ucar.nc2.ui.coverage;

import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.*;
import ucar.nc2.ft.fmrc.GridDatasetInv;
import ucar.nc2.ft.cover.Coverage;
import ucar.nc2.ft.cover.CoverageCS;
import ucar.nc2.ft.cover.CoverageDataset;
import ucar.nc2.ft.cover.impl.CoverageDatasetImpl;
import ucar.nc2.ui.dialog.NetcdfOutputChooser;
import ucar.nc2.ui.widget.*;
import ucar.nc2.ui.widget.PopupMenu;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTable;

import javax.swing.*;
//import java.awt.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Description
 *
 * @author John
 * @since 12/25/12
 */
public class CoverageTable extends JPanel {
  private PreferencesExt prefs;
  private CoverageDataset gridDataset;

  private BeanTable varTable, csTable = null, axisTable = null;
  private JSplitPane split = null, split2 = null;
  private TextHistoryPane infoTA;
  private IndependentWindow infoWindow;
  private NetcdfOutputChooser outChooser;

  public CoverageTable(PreferencesExt prefs) {
    this.prefs = prefs;

    varTable = new BeanTable(CoverageBean.class, (PreferencesExt) prefs.node("GeogridBeans"), false);
    JTable jtable = varTable.getJTable();

    PopupMenu csPopup = new ucar.nc2.ui.widget.PopupMenu(jtable, "Options");
    csPopup.addAction("Show Declaration", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        CoverageBean vb = (CoverageBean) varTable.getSelectedBean();
        //Variable v = vb.geogrid.getVariable();
        infoTA.clear();
        infoTA.appendLine("Coverage " + vb.geogrid.getName() + " :");
        infoTA.appendLine(vb.geogrid.toString());
        infoTA.gotoTop();
        infoWindow.show();
      }
    });

    csPopup.addAction("Show Coordinates", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        CoverageBean vb = (CoverageBean) varTable.getSelectedBean();
        Formatter f = new Formatter();
        showCoordinates(vb, f);
        infoTA.setText(f.toString());
        infoTA.gotoTop();
        infoWindow.show();
      }
    });

    /* csPopup.addAction("WCS DescribeCoverage", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        GeoGridBean vb = (GeoGridBean) varTable.getSelectedBean();
        if (gridDataset.findGridDatatype(vb.getName()) != null) {
          List<String> coverageIdList = Collections.singletonList(vb.getName());
          try {
            DescribeCoverage descCov =
                    ((thredds.wcs.v1_0_0_1.DescribeCoverageBuilder)
                            thredds.wcs.v1_0_0_1.WcsRequestBuilder
                                    .newWcsRequestBuilder("1.0.0",
                                            thredds.wcs.Request.Operation.DescribeCoverage,
                                            gridDataset, ""))
                            .setCoverageIdList(coverageIdList)
                            .buildDescribeCoverage();
            String dc = descCov.writeDescribeCoverageDocAsString();
            infoTA.clear();
            infoTA.appendLine(dc);
            infoTA.gotoTop();
            infoWindow.show();
          } catch (WcsException e1) {
            e1.printStackTrace();
          } catch (IOException e1) {
            e1.printStackTrace();
          }
        }
      }
    });  */

    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Variable Information", BAMutil.getImage("netcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    // optionally show coordinate systems and axis
    csTable = new BeanTable(CoverageCSBean.class, (PreferencesExt) prefs.node("GeoCoordinateSystemBean"), false);
    axisTable = new BeanTable(AxisBean.class, (PreferencesExt) prefs.node("GeoCoordinateAxisBean"), false);

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, varTable, csTable);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, split, axisTable);
    split2.setDividerLocation(prefs.getInt("splitPos2", 500));

    setLayout(new BorderLayout());
    add(split2, BorderLayout.CENTER);
  }

  public void addExtra(JPanel buttPanel, final FileManager fileChooser) {

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

    /* JButton wcsButton = new JButton("WCS");
    wcsButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (gridDataset != null) {
          URI gdUri = null;
          try {
            gdUri = new URI("http://none.such.server/thredds/wcs/dataset");
          } catch (URISyntaxException e1) {
            e1.printStackTrace();
            return;
          }
          GetCapabilities getCap =
                  ((thredds.wcs.v1_0_0_1.GetCapabilitiesBuilder)
                          thredds.wcs.v1_0_0_1.WcsRequestBuilder
                                  .newWcsRequestBuilder("1.0.0",
                                          thredds.wcs.Request.Operation.GetCapabilities,
                                          gridDataset, ""))
                          .setServerUri(gdUri)
                          .setSection(GetCapabilities.Section.All)
                          .buildGetCapabilities();
          try {
            String gc = getCap.writeCapabilitiesReportAsString();
            infoTA.setText(gc);
            infoTA.gotoTop();
            infoWindow.show();
          } catch (WcsException e1) {
            e1.printStackTrace();
          }
        }
      }
    });
    buttPanel.add(wcsButton);  */

    JButton invButton = new JButton("GridInv");
    invButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (gridDataset == null) return;
        if (!(gridDataset instanceof ucar.nc2.dt.grid.GridDataset)) return;
        GridDatasetInv inv = new GridDatasetInv((ucar.nc2.dt.grid.GridDataset) gridDataset, null);
        try {
          infoTA.setText(inv.writeXML(new Date()));
          infoTA.gotoTop();
          infoWindow.show();
        } catch (Exception e1) {
          e1.printStackTrace();
        }
      }
    });
    buttPanel.add(invButton);

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
 BAMutil.addActionToContainer(buttPanel, writeAction);  */
  }

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

  public PreferencesExt getPrefs() {
    return prefs;
  }

  public void save() {
    varTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    if (split != null) prefs.putInt("splitPos", split.getDividerLocation());
    if (split2 != null) prefs.putInt("splitPos2", split2.getDividerLocation());
    if (csTable != null) csTable.saveState(false);
    if (axisTable != null) axisTable.saveState(false);
  }

  public void setDataset(NetcdfDataset ds, Formatter parseInfo) throws IOException {
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
  }

  public void setDataset(CoverageDataset gds) throws IOException {
    this.gridDataset = gds;

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
  }

  private boolean contains(List<AxisBean> axisList, String name) {
    for (AxisBean axis : axisList)
      if (axis.getName().equals(name)) return true;
    return false;
  }

  public CoverageDataset getCoverageDataset() {
    return gridDataset;
  }

  public List<String> getSelectedGrids() {
    List grids = varTable.getSelectedBeans();
    List<String> result = new ArrayList<String>();
    for (Object bean : grids) {
      CoverageBean gbean = (CoverageBean) bean;
      result.add(gbean.getName());
    }
    return result;
  }


  public Coverage getGrid() {
    CoverageBean vb = (CoverageBean) varTable.getSelectedBean();
    if (vb == null) {
      List<Coverage> grids = gridDataset.getCoverages();
      if (grids.size() > 0)
        return grids.get(0);
      else
        return null;
    }
    return gridDataset.findCoverage(vb.getName());
  }

  public class CoverageBean {
    // static public String editableProperties() { return "title include logging freq"; }

    Coverage geogrid;
    String name, desc, units, extra;
    String dims, x, y, z, t; // , ens, rt;

    // no-arg constructor
    public CoverageBean() {
    }

    // create from a dataset
    public CoverageBean(Coverage geogrid) {
      this.geogrid = geogrid;
      setName(geogrid.getName());
      setDescription(geogrid.getDescription());
      setUnits(geogrid.getUnitsString());

      // collect dimensions
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
      extra = f.toString();
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

    public String getCoordSystem() {
      return geogrid.getCoordinateSystem().toString();
    }

    public String getX() {
      return x;
    }

    public String getY() {
      return y;
    }

    public String getZ() {
      return z;
    }

    public String getT() {
      return t;
    }

    public String getShape() {
      return dims;
    }

    public void setShape(String dims) {
      this.dims = dims;
    }

    public String getExtraDim() {
      return extra;
    }

    private String getAxisName(CoordinateAxis axis) {
      if (axis != null)
        return (axis.isCoordinateVariable()) ? axis.getShortName() : axis.getNameAndDimensions(false);
      return "";
    }
  }

  public class CoverageCSBean {
    private CoverageCS gcs;
    private String proj, coordTrans;
    private int ngrids = -1;

    // no-arg constructor
    public CoverageCSBean() {
    }

    public CoverageCSBean(CoverageDataset.CoverageSet gset) {
      gcs = gset.getCoverageCS();

      setNGrids(gset.getCoverages().size());

      ProjectionImpl proj = gcs.getProjection();
      if (proj != null)
        setProjection(proj.getClassName());

      int count = 0;
      StringBuffer buff = new StringBuffer();
      List ctList = gcs.getCoordinateTransforms();
      for (int i = 0; i < ctList.size(); i++) {
        CoordinateTransform ct = (CoordinateTransform) ctList.get(i);
        if (count > 0) buff.append("; ");
        // buff.append( ct.getTransformType());
        if (ct instanceof VerticalCT) {
          buff.append(((VerticalCT) ct).getVerticalTransformType());
          count++;
        }
        if (ct instanceof ProjectionCT) {
          ProjectionCT pct = (ProjectionCT) ct;
          if (pct.getProjection() == null) { // only show CT if theres no projection
            buff.append("-").append(pct.getName());
            count++;
          }
        }
      }
      setCoordTransforms(buff.toString());
    }

    public String getName() {
      return gcs.getName();
    }

    public String getCoverage() {
      return gcs.toString();
    }

    public int getDomainRank() {
      return gcs.getDomain().size();
    }

    public int getRangeRank() {
      return gcs.getCoordinateAxes().size();
    }

    public int getNGrids() {
      return ngrids;
    }

    public void setNGrids(int ngrids) {
      this.ngrids = ngrids;
    }

    public String getProjection() {
      return proj;
    }

    public void setProjection(String proj) {
      this.proj = proj;
    }

    public String getCoordTransforms() {
      return coordTrans;
    }

    public void setCoordTransforms(String coordTrans) {
      this.coordTrans = coordTrans;
    }
  }

  public class AxisBean {
    // static public String editableProperties() { return "title include logging freq"; }

    CoordinateAxis axis;
    CoordinateSystem firstCoordSys = null;
    String name, desc, units, axisType = "", positive = "", incr = "";
    String dims, shape, csNames;
    boolean isCoordVar;

    // no-arg constructor
    public AxisBean() {
    }

    // create from a dataset
    public AxisBean(CoordinateAxis v) {
      this.axis = v;

      setName(v.getFullName());
      setCoordVar(v.isCoordinateVariable());
      setDescription(v.getDescription());
      setUnits(v.getUnitsString());

      // collect dimensions
      StringBuffer lens = new StringBuffer();
      StringBuffer names = new StringBuffer();
      java.util.List dims = v.getDimensions();
      for (int j = 0; j < dims.size(); j++) {
        ucar.nc2.Dimension dim = (ucar.nc2.Dimension) dims.get(j);
        if (j > 0) {
          lens.append(",");
          names.append(",");
        }
        String name = dim.isShared() ? dim.getShortName() : "anon";
        names.append(name);
        lens.append(dim.getLength());
      }
      setDims(names.toString());
      setShape(lens.toString());

      AxisType at = v.getAxisType();
      if (at != null)
        setAxisType(at.toString());
      String p = v.getPositive();
      if (p != null)
        setPositive(p);

      if (v instanceof CoordinateAxis1D) {
        CoordinateAxis1D v1 = (CoordinateAxis1D) v;
        if (v1.isRegular())
          setRegular(Double.toString(v1.getIncrement()));
      }
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public boolean isCoordVar() {
      return isCoordVar;
    }

    public void setCoordVar(boolean isCoordVar) {
      this.isCoordVar = isCoordVar;
    }

    public String getShape() {
      return shape;
    }

    public void setShape(String shape) {
      this.shape = shape;
    }

    public String getAxisType() {
      return axisType;
    }

    public void setAxisType(String axisType) {
      this.axisType = axisType;
    }

    public String getDims() {
      return dims;
    }

    public void setDims(String dims) {
      this.dims = dims;
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

    public String getPositive() {
      return positive;
    }

    public void setPositive(String positive) {
      this.positive = positive;
    }

    public String getRegular() {
      return incr;
    }

    public void setRegular(String incr) {
      this.incr = incr;
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
