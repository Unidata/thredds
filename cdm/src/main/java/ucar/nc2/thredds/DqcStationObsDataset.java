// $Id:DqcStationObsDataset.java 63 2006-07-12 21:50:51Z edavis $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

package ucar.nc2.thredds;

import ucar.nc2.*;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.dt.*;
import ucar.nc2.dt.Station;
import ucar.nc2.util.CancelTask;

import java.util.*;
import java.io.IOException;

import thredds.catalog.*;
import thredds.catalog.query.*;

/**
 * This implements a StationObsDataset with a DQC.
 *
 * @author John Caron
 * @version $Id:DqcStationObsDataset.java 63 2006-07-12 21:50:51Z edavis $
 */

public class DqcStationObsDataset extends ucar.nc2.dt.point.StationObsDatasetImpl {

  static public DqcStationObsDataset factory(InvDataset ds, QueryCapability dqc) {
    // have a look at what selectors there are before proceeding
    SelectStation ss = null;
    SelectRangeDate sd = null;
    SelectService sss = null;
    ArrayList selectors = dqc.getSelectors();
    for (int i = 0; i < selectors.size(); i++) {
      Selector s =  (Selector) selectors.get(i);
      if (s instanceof SelectStation)
        ss = (SelectStation) s;
      if (s instanceof SelectRangeDate)
        sd = (SelectRangeDate) s;
      if (s instanceof SelectService)
        sss = (SelectService) s;
    }
    if (ss == null)
      return null;

    // for the moment, only doing XML
    SelectService.ServiceChoice wantServiceChoice = null;
    List services = sss.getChoices();
    for (int i = 0; i < services.size(); i++) {
      SelectService.ServiceChoice serviceChoice =  (SelectService.ServiceChoice) services.get(i);
      if (serviceChoice.getService().equals("HTTPServer") && serviceChoice.getDataFormat().equals("text/xml"))
        wantServiceChoice = serviceChoice;
    }
    if (wantServiceChoice == null)
      return null;

    return new DqcStationObsDataset( ds, dqc, sss, ss, sd, wantServiceChoice);
  }

  //////////////////////////////////////////////////////////////////////////////////

  private InvDataset ds;
  private QueryCapability dqc;
  private SelectService selService;
  private SelectStation selStation;
  private SelectRangeDate selDate;
  private SelectService.ServiceChoice service;

  private boolean debugQuery = false;

  private DqcStationObsDataset(InvDataset ds, QueryCapability dqc, SelectService selService, SelectStation selStation,
      SelectRangeDate selDate, SelectService.ServiceChoice service) {
    super();
    this.ds = ds;
    this.dqc = dqc;
    this.selService = selService;
    this.selStation = selStation;
    this.selDate = selDate;
    this.service = service;

    ArrayList stationList = selStation.getStations();
    for (int i = 0; i < stationList.size(); i++) {
      thredds.catalog.query.Station station = (thredds.catalog.query.Station) stationList.get(i);
      stations.add( new DqcStation( station));
    }

    startDate = new Date();
    endDate = new Date();

    //startDate = sd.getStart();
    //endDate = sd.getEnd();

    // LOOK need to add typed variables

  }

  protected void setTimeUnits() { }
  protected void setStartDate() { }
  protected void setEndDate() { }
  protected void setBoundingBox() { }

  public String getTitle() { return dqc.getName(); }
  public String getLocationURI() {return dqc.getCreateFrom(); }
  public String getDescription() { return ds.getDocumentation("summary"); }
  public List getDataVariables() {return null; } // LOOK !!
  public VariableSimpleIF getDataVariable(String name) {return null; }

  public int getStationDataCount( Station s) {
    DqcStation si = (DqcStation)s;
    return si.getNumObservations();
  }

  public List getData( Station s, CancelTask cancel) throws IOException {
    return ((DqcStation)s).getObservations();
  }

  private void makeQuery( Station s) {
     // construct the query
     StringBuffer queryb = new StringBuffer();
     queryb.append( dqc.getQuery().getUriResolved().toString());

     ArrayList choices = new ArrayList();
     choices.add("{value}");
     choices.add(s.getName());
     selStation.appendQuery( queryb, choices);

     if (selDate != null) {
       choices = new ArrayList();
       choices.add("{value}");
       choices.add("all");
       selDate.appendQuery( queryb, choices);
     }

     String queryString = queryb.toString();
     if (debugQuery) {
       System.out.println("dqc makeQuery= "+queryString);
     }

     // fetch the catalog
     InvCatalogFactory factory = InvCatalogFactory.getDefaultFactory(true); // use default factory
     InvCatalog catalog = factory.readXML( queryString);
     StringBuffer buff = new StringBuffer();
     if (!catalog.check( buff)) {
       // javax.swing.JOptionPane.showMessageDialog(this, "Invalid catalog "+ buff.toString());
       System.out.println("Invalid catalog "+buff.toString());
       return;
     }
     if (debugQuery) {
       System.out.println("dqc/showQueryResult catalog check msgs= "+buff.toString());
       System.out.println("  query result =\n"+thredds.util.IO.readURLcontents(queryString));
     }
   }

  private class DqcStation extends StationImpl {

    private DqcStation( thredds.catalog.query.Station s) {
      this.name = s.getValue();
      this.desc = s.getName();
      //InvDocumentation doc = s.getDescription();
      //this.desc = (doc == null) ? "" : doc.getInlineContent();
      this.lat = s.getLocation().getLatitude();
      this.lon = s.getLocation().getLongitude();
      this.alt = s.getLocation().getElevation();
    }

    protected ArrayList readObservations()  throws IOException {
      ArrayList obs = new ArrayList();
      makeQuery( this);
      return obs;
    }

  }

  public Class getDataClass() {
    return DqcObsImpl.class;
  }

  public List getData(CancelTask cancel) throws IOException {
    return null;
  }

  public int getDataCount() {
    return -1;
  }

  public class DqcObsImpl extends ucar.nc2.dt.point.StationObsDatatypeImpl {
    private int recno;

    private DqcObsImpl( DqcStation station, double dateValue, int recno) {
      super(station, dateValue, dateValue);
      this.recno = recno;
    }

    public ucar.ma2.StructureData getData() throws IOException {
      return null;
    }

    public Date getNominalTimeAsDate() {
      return timeUnit.makeDate( getNominalTime());
    }

    public Date getObservationTimeAsDate() {
      return timeUnit.makeDate( getObservationTime());
    }
  }


  public DataIterator getDataIterator(int bufferSize) throws IOException {
    return null;
  }

}