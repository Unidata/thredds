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

import ucar.nc2.ft.*;
import ucar.nc2.ui.point.StationRegionDateChooser;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.DateRange;
import ucar.nc2.NCdumpW;

import ucar.util.prefs.*;
import ucar.util.prefs.ui.*;
import ucar.unidata.geoloc.LatLonRect;
import ucar.ma2.StructureData;
import thredds.ui.*;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.*;
import java.beans.PropertyChangeListener;

import javax.swing.*;
import javax.swing.event.*;

/**
 * A Swing widget to view the contents of a ucar.nc2.dt2.PointObsDataset.
 * <p/>
 * If its a StationObsDataset, the available Stations are shown in a BeanTable.
 * The obs are shown in a StructureTabel.
 *
 * @author caron
 */

public class PointObsViewer2 extends JPanel {
  private PreferencesExt prefs;

  private PointFeatureCollection pds;

  private StationRegionDateChooser chooser;
  private BeanTableSorted stnTable;
  private JSplitPane splitH = null;
  private IndependentDialog infoWindow;
  private DateFormatter df = new DateFormatter();

  private TextHistoryPane dumpTA;
  private IndependentDialog dumpWindow;

  private boolean eventsOK = true;
  private boolean debugStationRegionSelect = false, debugStationDatsets = false, debugQuery = false;

  public PointObsViewer2(PreferencesExt prefs) {
    this.prefs = prefs;

    df = new DateFormatter();

    chooser = new StationRegionDateChooser();
    chooser.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(java.beans.PropertyChangeEvent e) {
        if (e.getPropertyName().equals("Station")) {
          StationImpl selectedStation = (StationImpl) e.getNewValue();
          if (debugStationRegionSelect) System.out.println("selectedStation= " + selectedStation.getName());
          eventsOK = false;
          stnTable.setSelectedBean(selectedStation);
          eventsOK = true;
        }
      }
    });

    // do the query
    AbstractAction queryAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        if (pds == null) return;

        // is the date window showing ?
        DateRange dateRange = chooser.getDateRange();
        if (debugQuery) System.out.println("date range=" + dateRange);
        LatLonRect geoRegion = chooser.getGeoSelectionLL();
        if (debugQuery) System.out.println("geoRegion=" + geoRegion);

        try {
          PointFeatureCollection subset = pds.subset(geoRegion, dateRange);
          setObservations(subset);

        } catch (IOException e1) {
          e1.printStackTrace();
        }
      }
    };
    BAMutil.setActionProperties(queryAction, "query", "query for data", false, 'Q', -1);
    chooser.addToolbarAction(queryAction);

    // get all data
    AbstractAction getAllAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        if (pds == null) return;
        try {
          setObservations(pds);
          //JOptionPane.showMessageDialog(PointObsViewer2.this, "GetAllData not implemented");

        } catch (IOException e1) {
          e1.printStackTrace();
        }
      }
    };
    BAMutil.setActionProperties(getAllAction, "GetAll", "get ALL data", false, 'A', -1);
    chooser.addToolbarAction(getAllAction);

    // station table
    stnTable = new BeanTableSorted(PointObsBean.class, (PreferencesExt) prefs.node("PointBeans"), false);
    stnTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        PointObsBean sb = (PointObsBean) stnTable.getSelectedBean();
        if (debugStationRegionSelect) System.out.println("stnTable selected= " + sb.getName());
        if (eventsOK) chooser.setSelectedStation(sb.getName());
        if (sb != null) showData(sb.pobs);
      }
    });

    // the obs table
    //obsTable = new StructureTable( (PreferencesExt) prefs.node("ObsBean"));

    // the info window
    TextHistoryPane infoTA = new TextHistoryPane();
    infoWindow = new IndependentDialog(null, true, "Station Information", infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    // layout
    splitH = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, stnTable, chooser);
    splitH.setDividerLocation(prefs.getInt("splitPosH", 400));

    //splitV = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, splitH, obsTable);
    //splitV.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(splitH, BorderLayout.CENTER);

    // other widgets
    dumpTA = new TextHistoryPane(false);
    dumpWindow = new IndependentDialog(null, false, "Show Data", dumpTA);
    dumpWindow.setBounds((Rectangle) prefs.getBean("DumpWindowBounds", new Rectangle(300, 300, 300, 200)));
  }

  public void setDataset(FeatureDatasetPoint dataset) throws IOException {
    this.pds = (PointFeatureCollection) dataset.getPointFeatureCollectionList().get(0); // LOOK kludge

    if (debugStationDatsets)
      System.out.println("PointObsViewer open type " + dataset.getClass().getName());
    
    if (dataset.getDateRange() != null) {
      Date startDate = dataset.getDateRange().getStart().getDate();
      Date endDate = dataset.getDateRange().getEnd().getDate();
      if ((startDate != null) && (endDate != null))
        chooser.setDateRange(new DateRange(startDate, endDate));
    }

    // clear
    setObservations( null);
  }

  public void setObservations(PointFeatureCollection pobsDataset) throws IOException {
    List<PointObsBean> pointBeans = new ArrayList<PointObsBean>();
    int count = 0;

    if (pobsDataset != null)  {
      PointFeatureIterator iter = pobsDataset.getPointFeatureIterator(-1);
      while (iter.hasNext()) {
        PointFeature pob = iter.next();
        pointBeans.add(new PointObsBean(count++, pob));
      }
    }

    stnTable.setBeans(pointBeans);
    chooser.setStations( pointBeans);
    stnTable.clearSelectedCells();
  }

  public PreferencesExt getPrefs() {
    return prefs;
  }

  public void save() {
    stnTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putInt("splitPosH", splitH.getDividerLocation());
    prefs.putBeanObject("DumpWindowBounds", dumpWindow.getBounds());
  }

  private void showData(PointFeature pobs) {
    ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
    try {
      StructureData sd = pobs.getData();
      NCdumpW.printStructureData(new PrintWriter(bos), sd);
    } catch (IOException e) {
      e.printStackTrace(new PrintStream(bos));
    }

    dumpTA.setText(bos.toString());
    dumpWindow.setVisible(true);
  }

  static public String hiddenProperties() {
    return "description wmoId";
  } // for prefs.BeanTable LOOK

  public class PointObsBean implements ucar.nc2.dt.Station {  // fake Station, so we can use StationRegionChooser
    private PointFeature pobs;
    private String timeObs;
    private int id;

    public PointObsBean(int id, PointFeature obs) {
      this.id = id;
      this.pobs = obs;
      setTime(obs.getObservationTimeAsDate());
    }

    public String getTime() {
      return timeObs;
    }

    public void setTime(Date timeObs) {
      this.timeObs = df.toDateTimeString(timeObs);
    }

    public String getName() {
      return Integer.toString(id);
    }

    public String getDescription() {
      return null;
    }

    public String getWmoId() {
      return null;
    }

    public double getLatitude() {
      return pobs.getLocation().getLatitude();
    }

    public double getLongitude() {
      return pobs.getLocation().getLongitude();
    }

    public double getAltitude() {
      return pobs.getLocation().getAltitude();
    }

    public int compareTo(Object o) {
      StationImpl so = (StationImpl) o;
      return getName().compareTo( so.getName());
    }
  }

}