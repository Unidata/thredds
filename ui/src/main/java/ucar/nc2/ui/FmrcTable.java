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
import ucar.nc2.Variable;
import ucar.nc2.Structure;
import ucar.nc2.Attribute;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.dt.fmrc.FmrcImpl;
import ucar.ma2.StructureData;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import thredds.ui.TextHistoryPane;
import thredds.ui.IndependentWindow;
import thredds.ui.BAMutil;

import java.awt.event.ActionEvent;
import java.awt.*;
import java.io.*;
import java.util.*;

/**
 * ToolsUI/NcML/Aggregation
 *
 * @author caron
 * @since Aug 15, 2008
 */
public class FmrcTable extends JPanel {
  private PreferencesExt prefs;

  private BeanTableSorted messageTable, obsTable, ddsTable;
  private JSplitPane split, split2;

  private TextHistoryPane infoTA;
  private IndependentWindow infoWindow;

  private CoordSysTable coordSysTable;

  public FmrcTable(PreferencesExt prefs) {
    this.prefs = prefs;

    messageTable = new BeanTableSorted(DatasetBean.class, (PreferencesExt) prefs.node("DatasetBean"), false);
    messageTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        ddsTable.setBeans(new ArrayList());
        obsTable.setBeans(new ArrayList());

        DatasetBean dsb = (DatasetBean) messageTable.getSelectedBean();
        if (dsb != null)
          coordSysTable.setDataset(dsb.ds);
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
        FmrcTable.this.firePropertyChange("openNetcdfFile", null, dsb.ds);
      }
    });

    varPopup.addAction("Check CoordSystems", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        DatasetBean dsb = (DatasetBean) messageTable.getSelectedBean();
        if (dsb == null) return;
        FmrcTable.this.firePropertyChange("openCoordSystems", null, dsb.ds);
      }
    });

    varPopup.addAction("Open as GridDataset", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        DatasetBean dsb = (DatasetBean) messageTable.getSelectedBean();
        if (dsb == null) return;
        FmrcTable.this.firePropertyChange("openGridDataset", null, dsb.ds);
      }
    });


    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    coordSysTable = new CoordSysTable( (PreferencesExt) prefs.node("CoordSys"));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, messageTable, coordSysTable);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(split, BorderLayout.CENTER);
  }

  public void save() {
    messageTable.saveState(false);
    coordSysTable.save();
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putInt("splitPos", split.getDividerLocation());
  }

  public void setFmrc(FmrcImpl fmrc) throws IOException {
    java.util.List<DatasetBean> beanList = new ArrayList<DatasetBean>();
    DateFormatter df = new DateFormatter();
    beanList.add(new DatasetBean("Fmrc2d", fmrc.getFmrcDataset()));
    beanList.add(new DatasetBean("best", fmrc.getBestTimeSeries()));
    for (Date runDate : fmrc.getRunDates()) {
      beanList.add(new DatasetBean("runDate "+df.toDateTimeString(runDate), fmrc.getRunTimeDataset(runDate)));
    }
    for (Date forecastDate : fmrc.getForecastDates()) {
      beanList.add(new DatasetBean("forecast "+df.toDateTimeString(forecastDate), fmrc.getForecastTimeDataset(forecastDate)));
    }
    for (Double offset : fmrc.getForecastOffsets()) {
      beanList.add(new DatasetBean("offset "+offset, fmrc.getForecastOffsetDataset(offset)));
    }

    messageTable.setBeans(beanList);
    obsTable.setBeans(new ArrayList());
    ddsTable.setBeans(new ArrayList());
  }

  public class DatasetBean {
    String name;
    NetcdfDataset ds;

    // no-arg constructor
    public DatasetBean() {
    }

    // create from a dataset
    public DatasetBean(String name, NetcdfDataset ds) {
      this.ds = ds;
      this.name = name;
    }

    public String getLocation() throws IOException {
      return ds.getLocation();
    }

    public String getName() {
      return name;
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
         } else if (val.equals("0-4-1") && (year<0)) {
           year = sdata.convertScalarInt(v.getShortName());
         } else if (val.equals("0-4-2")&& (month<0)) {
           month = sdata.convertScalarInt(v.getShortName());
         } else if (val.equals("0-4-3")&& (day<0)) {
           day = sdata.convertScalarInt(v.getShortName());
         } else if (val.equals("0-4-4")&& (hour<0)) {
           hour = sdata.convertScalarInt(v.getShortName());
         } else if (val.equals("0-4-5")&& (minute<0)) {
           minute = sdata.convertScalarInt(v.getShortName());
         } else if (val.equals("0-4-6")&& (sec<0)) {
           sec = sdata.convertScalarInt(v.getShortName());

         } else if (val.equals("0-1-1")&& (wmo_block<0)) {
           wmo_block = sdata.convertScalarInt(v.getShortName());
         } else if (val.equals("0-1-2")&& (wmo_id<0)) {
           wmo_id = sdata.convertScalarInt(v.getShortName());

         } else if ((stn == null) &&
             (val.equals("0-1-7") || val.equals("0-1-194") || val.equals("0-1-11") || val.equals("0-1-18") )) {
           if (v.getDataType().isString())
             stn = sdata.getScalarString(v.getShortName());
           else
             stn = Integer.toString( sdata.convertScalarInt(v.getShortName()));
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
         } else if ((val.equals("0-4-7")) && (sec<0)) {
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
      return wmo_block+"/"+wmo_id;
    }

    public String getStation() {
      return stn;
    }
  }

}
