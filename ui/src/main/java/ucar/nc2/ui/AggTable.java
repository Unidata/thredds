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

import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTableSorted;
import ucar.nc2.iosp.bufr.DataDescriptor;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.*;
import ucar.nc2.util.CompareNetcdf;
import ucar.nc2.util.CancelTask;
import ucar.nc2.ncml.Aggregation;
import ucar.ma2.StructureData;
import ucar.ma2.Array;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import thredds.ui.TextHistoryPane;
import thredds.ui.IndependentWindow;
import thredds.ui.BAMutil;
import thredds.ui.FileManager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.*;
import java.io.*;
import java.util.*;

/**
 * ToolsUI/NcML/Aggregation
 *
 * @author caron
 * @since Aug 15, 2008
 */
public class AggTable extends JPanel {
  private PreferencesExt prefs;

  private BeanTableSorted messageTable, obsTable, ddsTable;
  private JSplitPane split, split2;

  private TextHistoryPane infoTA, aggTA;
  private IndependentWindow infoWindow;

  private StructureTable dataTable;
  private IndependentWindow dataWindow;

  private NetcdfDataset current;

  public AggTable(PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;

    messageTable = new BeanTableSorted(DatasetBean.class, (PreferencesExt) prefs.node("DatasetBean"), false);
    messageTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        ddsTable.setBeans(new ArrayList());
        obsTable.setBeans(new ArrayList());

        DatasetBean mb = (DatasetBean) messageTable.getSelectedBean();
      }
    });

    obsTable = new BeanTableSorted(ObsBean.class, (PreferencesExt) prefs.node("ObsBean"), false);
    obsTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        ObsBean csb = (ObsBean) obsTable.getSelectedBean();
      }
    });

    ddsTable = new BeanTableSorted(DdsBean.class, (PreferencesExt) prefs.node("DdsBean"), false);
    ddsTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        DdsBean csb = (DdsBean) ddsTable.getSelectedBean();
      }
    });

    thredds.ui.PopupMenu varPopup = new thredds.ui.PopupMenu(messageTable.getJTable(), "Options");
    varPopup.addAction("Open as NetcdfFile", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        DatasetBean dsb = (DatasetBean) messageTable.getSelectedBean();
        if (dsb == null) return;
        AggTable.this.firePropertyChange("openNetcdfFile", null, dsb.acquireFile());
      }
    });

    varPopup.addAction("Check CoordSystems", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        DatasetBean dsb = (DatasetBean) messageTable.getSelectedBean();
        if (dsb == null) return;
        AggTable.this.firePropertyChange("openCoordSystems", null, dsb.acquireFile());
      }
    });

    varPopup.addAction("Open as GridDataset", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        DatasetBean dsb = (DatasetBean) messageTable.getSelectedBean();
        if (dsb == null) return;
        AggTable.this.firePropertyChange("openGridDataset", null, dsb.acquireFile());
      }
    });

    AbstractButton compareButton = BAMutil.makeButtcon("Select", "Check files", false);
    compareButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Formatter f = new Formatter();
        compare(f);
        checkAggCoordinate(f);

        infoTA.setText(f.toString());
        infoTA.gotoTop();
        infoWindow.show();
      }
    });
    buttPanel.add(compareButton);

    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, ddsTable, obsTable);
    split2.setDividerLocation(prefs.getInt("splitPos2", 800));

    aggTA = new TextHistoryPane();

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, messageTable, aggTA);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(split, BorderLayout.CENTER);
  }

  private void makeDataTable() {
    // the data Table
    dataTable = new StructureTable((PreferencesExt) prefs.node("structTable"));
    dataWindow = new IndependentWindow("Data Table", BAMutil.getImage("netcdfUI"), dataTable);
    dataWindow.setBounds((Rectangle) prefs.getBean("dataWindow", new Rectangle(50, 300, 1000, 600)));
  }

  public void save() {
    messageTable.saveState(false);
    ddsTable.saveState(false);
    obsTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putInt("splitPos", split.getDividerLocation());
    prefs.putInt("splitPos2", split2.getDividerLocation());
  }

  public void setAggDataset(NetcdfDataset ncd) throws IOException {
    current = ncd;

    Aggregation agg = ncd.getAggregation();
    java.util.List<DatasetBean> beanList = new ArrayList<DatasetBean>();
    for (Aggregation.Dataset dataset : agg.getDatasets()) {
      beanList.add(new DatasetBean(dataset));
    }

    messageTable.setBeans(beanList);

    Formatter f = new Formatter();
    agg.getDetailInfo(f);
    aggTA.setText(f.toString());
  }

  private void checkAggCoordinate(Formatter f) {
    if (null == current) return;

    try {
      Aggregation agg = current.getAggregation();
      String aggDimName = agg.getDimensionName();
      Variable aggCoord = current.findVariable(aggDimName);
      Array data = aggCoord.read();
      f.format("   Aggregated coordinate variable %s%n", aggCoord);
      f.format(NCdumpW.printArray(data, aggDimName, null));

      for (Object bean : messageTable.getBeans()) {
        DatasetBean dbean = (DatasetBean) bean;
        Aggregation.Dataset ads = dbean.ds;

        NetcdfFile aggFile = ads.acquireFile(null);
        f.format("   Component file %s%n", aggFile.getLocation());
        Variable aggCoordp = aggFile.findVariable(aggDimName);
        if (aggCoordp == null) {
          f.format("   doesnt have coordinate variable%n");
        } else {
          data = aggCoordp.read();
          f.format(NCdumpW.printArray(data, aggCoordp.getNameAndDimensions() +" ("+aggCoordp.getUnitsString()+")", null));
        }
      }
    } catch (Throwable t) {
      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      t.printStackTrace(new PrintStream(bos));
      f.format(bos.toString());
    }
  }

  private void compare(Formatter f) {
    try {
      NetcdfFile org = null;
      for (Object bean : messageTable.getBeans()) {
        DatasetBean dbean = (DatasetBean) bean;
        Aggregation.Dataset ads = dbean.ds;

        NetcdfFile ncd = ads.acquireFile(null);
        if (org == null)
          org = ncd;
        else {
          CompareNetcdf cn = new CompareNetcdf(false, false, false);
          cn.compareVariables(org, ncd, f);
          ncd.close();
          f.format("--------------------------------%n");
        }
      }
      if (org != null) org.close();
    } catch (Throwable t) {
      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      t.printStackTrace(new PrintStream(bos));
      f.format(bos.toString());
    }
  }

  public class DatasetBean {
    Aggregation.Dataset ds;

    protected NetcdfFile acquireFile() {
      try {
        return ds.acquireFile(null);
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      }
    }

    // no-arg constructor
    public DatasetBean() {
    }

    // create from a dataset
    public DatasetBean(Aggregation.Dataset ds) {
      this.ds = ds;
    }

    public String getLocation() throws IOException {
      return ds.getLocation();
    }

    public String getCacheLocation() {
      return ds.getCacheLocation();
    }

    public String getId() {
      return ds.getId();
    }

  }

  public class DdsBean {
    DataDescriptor dds;
    int seq;

    // no-arg constructor
    public DdsBean() {
    }

    // create from a dataset
    public DdsBean(DataDescriptor dds, int seq) {
      this.dds = dds;
      this.seq = seq;
    }

    public String getFxy() {
      return dds.getFxyName();
    }

    public String getName() {
      return dds.getName();
    }

    public String getUnits() {
      return dds.getUnits();
    }

    public int getBitWidth() {
      return dds.getBitWidth();
    }

    public int getScale() {
      return dds.getScale();
    }

    public int getReference() {
      return dds.getRefVal();
    }

    public int getSeq() {
      return seq;
    }

    public String getLocal() {
      return dds.isLocal() ? "true" : "false";
    }

  }


  public class ObsBean {
    double lat = Double.NaN, lon = Double.NaN, alt = Double.NaN;
    int year = -1, month = -1, day = -1, hour = -1, minute = -1, sec = -1;
    Date time;
    int wmo_block = -1, wmo_id = -1;
    String stn = null;

    // no-arg constructor
    public ObsBean() {
    }

    // create from a dataset
    public ObsBean(Structure obs, StructureData sdata) {
      // first choice
      for (Variable v : obs.getVariables()) {
        Attribute att = v.findAttribute("BUFR:TableB_descriptor");
        if (att == null) continue;
        String val = att.getStringValue();
        if (val.equals("0-5-1") && Double.isNaN(lat)) {
          lat = sdata.convertScalarDouble(v.getShortName());
        } else if (val.equals("0-6-1") && Double.isNaN(lon)) {
          lon = sdata.convertScalarDouble(v.getShortName());
        } else if (val.equals("0-7-30") && Double.isNaN(alt)) {

          alt = sdata.convertScalarDouble(v.getShortName());
        } else if (val.equals("0-4-1") && (year < 0)) {
          year = sdata.convertScalarInt(v.getShortName());
        } else if (val.equals("0-4-2") && (month < 0)) {
          month = sdata.convertScalarInt(v.getShortName());
        } else if (val.equals("0-4-3") && (day < 0)) {
          day = sdata.convertScalarInt(v.getShortName());
        } else if (val.equals("0-4-4") && (hour < 0)) {
          hour = sdata.convertScalarInt(v.getShortName());
        } else if (val.equals("0-4-5") && (minute < 0)) {
          minute = sdata.convertScalarInt(v.getShortName());
        } else if (val.equals("0-4-6") && (sec < 0)) {
          sec = sdata.convertScalarInt(v.getShortName());

        } else if (val.equals("0-1-1") && (wmo_block < 0)) {
          wmo_block = sdata.convertScalarInt(v.getShortName());
        } else if (val.equals("0-1-2") && (wmo_id < 0)) {
          wmo_id = sdata.convertScalarInt(v.getShortName());

        } else if ((stn == null) &&
                (val.equals("0-1-7") || val.equals("0-1-194") || val.equals("0-1-11") || val.equals("0-1-18"))) {
          if (v.getDataType().isString())
            stn = sdata.getScalarString(v.getShortName());
          else
            stn = Integer.toString(sdata.convertScalarInt(v.getShortName()));
        }
      }

      // second choice
      for (Variable v : obs.getVariables()) {
        Attribute att = v.findAttribute("BUFR:TableB_descriptor");
        if (att == null) continue;
        String val = att.getStringValue();
        if (val.equals("0-5-2") && Double.isNaN(lat)) {
          lat = sdata.convertScalarDouble(v.getShortName());
        } else if (val.equals("0-6-2") && Double.isNaN(lon)) {
          lon = sdata.convertScalarDouble(v.getShortName());
        } else if (val.equals("0-7-1") && Double.isNaN(alt)) {
          alt = sdata.convertScalarDouble(v.getShortName());
        } else if ((val.equals("0-4-7")) && (sec < 0)) {
          sec = sdata.convertScalarInt(v.getShortName());
        }
      }

      // third choice
      for (Variable v : obs.getVariables()) {
        Attribute att = v.findAttribute("BUFR:TableB_descriptor");
        if (att == null) continue;
        String val = att.getStringValue();
        if (val.equals("0-7-10") && Double.isNaN(alt)) {
          alt = sdata.convertScalarDouble(v.getShortName());
        } else if (val.equals("0-7-2") && Double.isNaN(alt)) {
          alt = sdata.convertScalarDouble(v.getShortName());
        }

      }

    }

    public double getLat() {
      return lat;
    }

    public double getLon() {
      return lon;
    }

    public double getHeight() {
      return alt;
    }

    public int getYear() {
      return year;
    }

    public int getMonth() {
      return month;
    }

    public int getDay() {
      return day;
    }

    public int getHour() {
      return hour;
    }

    public int getMinute() {
      return minute;
    }

    public int getSec() {
      return sec;
    }

    public String getWmoId() {
      return wmo_block + "/" + wmo_id;
    }

    public String getStation() {
      return stn;
    }
  }

}
