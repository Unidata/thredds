/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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

import ucar.nc2.units.DateUnit;
import ucar.nc2.dt.*;
import ucar.nc2.dt.Station;
import ucar.nc2.dt.point.StationObsDatatypeImpl;
import ucar.nc2.dt.point.decode.MetarParseReport;
import ucar.nc2.util.CancelTask;
import ucar.ma2.*;

import java.util.*;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.HttpURLConnection;

import thredds.catalog.*;
import thredds.catalog.query.*;

/**
 * This implements a StationObsDataset with a DQC.
 *
 * @author John Caron
 */

public class DqcStationObsDataset extends ucar.nc2.dt.point.StationObsDatasetImpl {

  static public DqcStationObsDataset factory(InvDataset ds, String dqc_location, StringBuffer errlog) throws IOException {
    return factory(ds.getDocumentation("summary"), dqc_location, errlog);
  }

  static public DqcStationObsDataset factory(String desc, String dqc_location, StringBuffer errlog) throws IOException {

    DqcFactory dqcFactory = new DqcFactory(true);
    QueryCapability dqc = dqcFactory.readXML(dqc_location);
    if (dqc.hasFatalError()) {
      errlog.append(dqc.getErrorMessages());
      return null;
    }

    // have a look at what selectors there are before proceeding
    SelectStation selStation = null;
    SelectRangeDate selDate = null;
    SelectService selService = null;
    SelectGeoRegion selRegion = null;

    ArrayList selectors = dqc.getSelectors();
    for (int i = 0; i < selectors.size(); i++) {
      Selector s =  (Selector) selectors.get(i);
      if (s instanceof SelectStation)
        selStation = (SelectStation) s;
      if (s instanceof SelectRangeDate)
        selDate = (SelectRangeDate) s;
      if (s instanceof SelectService)
        selService = (SelectService) s;
      if (s instanceof SelectGeoRegion)
        selRegion = (SelectGeoRegion) s;
     }

    // gotta have these
    if (selService == null) {
      errlog.append("DqcStationObsDataset must have Service selector");
      return null;
    }
    if (selStation == null) {
      errlog.append("DqcStationObsDataset must have Station selector");
      return null;
    }
    if (selDate == null) {
      errlog.append("DqcStationObsDataset must have Date selector");
      return null;
    }
    if (selRegion == null) {
      errlog.append("DqcStationObsDataset must have GeoRegion selector");
      return null;
    }

    // decide on which service
    SelectService.ServiceChoice wantServiceChoice = null;
    List services = selService.getChoices();
    for (int i = 0; i < services.size(); i++) {
      SelectService.ServiceChoice serviceChoice =  (SelectService.ServiceChoice) services.get(i);
      if (serviceChoice.getService().equals("HTTPServer") && serviceChoice.getDataFormat().equals("text/plain")
         && serviceChoice.getReturns().equals("data")     ) // LOOK kludge
        wantServiceChoice = serviceChoice;
    }

    if (wantServiceChoice == null){
      errlog.append("DqcStationObsDataset must have HTTPServer Service with DataFormat=text/plain, and returns=data");
      return null;
    }

    return new DqcStationObsDataset( desc, dqc, selService, wantServiceChoice, selStation, selRegion, selDate);
  }

  //////////////////////////////////////////////////////////////////////////////////

  //private InvDataset ds;
  private QueryCapability dqc;
  private SelectService selService;
  private SelectStation selStation;
  private SelectRangeDate selDate;
  private SelectGeoRegion selRegion;
  private SelectService.ServiceChoice service;

  private boolean debugQuery = false;

  private StructureMembers members;
  private MetarParseReport parser;

  private DqcStationObsDataset(String desc, QueryCapability dqc, SelectService selService, SelectService.ServiceChoice service,
      SelectStation selStation, SelectGeoRegion selRegion, SelectRangeDate selDate) {
    super();
    this.desc = desc;
    this.dqc = dqc;
    this.selService = selService;
    this.selStation = selStation;
    this.selRegion = selRegion;
    this.selDate = selDate;
    this.service = service;

    ArrayList stationList = selStation.getStations();
    for (int i = 0; i < stationList.size(); i++) {
      thredds.catalog.query.Station station = (thredds.catalog.query.Station) stationList.get(i);
      stations.add( new DqcStation( station));
    }

    startDate = new Date();
    endDate = new Date();

    try {
      timeUnit = new DateUnit("hours since 1991-01-01T00:00");
    } catch (Exception e) {
      e.printStackTrace();
    }

    parser = new MetarParseReport();

    // LOOK need to add typed variables

     members = new StructureMembers( "fake");
     members.addMember("line", null, null, ucar.ma2.DataType.STRING, new int [] {1});

  }

  protected void setTimeUnits() { }
  protected void setStartDate() { }
  protected void setEndDate() { }
  protected void setBoundingBox() { }

  public String getTitle() { return dqc.getName(); }
  public String getLocationURI() {return dqc.getCreateFrom(); }
  public String getDescription() { return desc; }

  public List getData( Station s, CancelTask cancel) throws IOException {
    return ((DqcStation)s).getObservations();
  }

  private ArrayList makeQuery( Station s) throws IOException {

     // construct the query
     StringBuffer queryb = new StringBuffer();
     queryb.append( dqc.getQuery().getUriResolved().toString());

     ArrayList choices = new ArrayList();
     //choices.add("{service}");
     //choices.add(service.getValue());
     //selService.appendQuery( queryb, choices);
     queryb.append( "returns=text&dtime=3day&");

     choices.clear();
     choices.add("{value}");
     choices.add(s.getName());
     selStation.appendQuery( queryb, choices);

     /* if (selDate != null) {
       choices = new ArrayList();
       choices.add("{value}");
       choices.add("all");
       selDate.appendQuery( queryb, choices);
     } */

     String queryString = queryb.toString();
     if (debugQuery) {
       System.out.println("dqc makeQuery= "+queryString);
     }

     return readText(s, queryString);

     /* fetch the catalog
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
     } */
   }

  private ArrayList readText(Station s, String urlString) throws IOException {
    ArrayList obsList = new ArrayList();

    System.out.println("readText= "+urlString);

    URL url;
    java.io.InputStream is = null;
    try {
      url =  new URL( urlString);
    } catch (MalformedURLException e) {
      throw new IOException( "** MalformedURLException on URL <"+urlString+">\n"+e.getMessage()+"\n");
    }

    try {
      java.net.URLConnection connection = url.openConnection();

      if (connection instanceof HttpURLConnection) {
        java.net.HttpURLConnection httpConnection = (HttpURLConnection) connection;
        // check response code is good
        int responseCode = httpConnection.getResponseCode();
        if (responseCode/100 != 2)
          throw new IOException( "** Cant open URL <"+urlString+">\n Response code = " +responseCode
              +"\n"+httpConnection.getResponseMessage()+"\n");
      }

      // read it
      boolean first = true;
      is = connection.getInputStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));
      while (true) {
        String line = reader.readLine();
        if (line == null) break;

        if (first) show(line);
        first = false;

        obsList.add( new DqcObsImpl(s, 0, line));
      }


    } catch (java.net.ConnectException e) {
        throw new IOException( "** ConnectException on URL: <"+urlString+">\n"+
          e.getMessage()+"\nServer probably not running");

    } finally {
      if (is != null) is.close();
    }

    return obsList;
  }

  private void show(String line) {
    System.out.println(line);
    if( ! parser.parseReport(line) )
        return;

    LinkedHashMap map = parser.getFields();
    Iterator ii = map.keySet().iterator();
    while (ii.hasNext()) {
      Object key = ii.next();
      Object value=map.get(key);
      System.out.println(" "+key +"==("+value+") ");
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

    // LOOK: currently implementing only "get all"
    protected ArrayList readObservations()  throws IOException {
      return makeQuery( this);
    }

  }

 /* public Class getDataClass() {
    return DqcObsImpl.class;
  } */

  public List getData(CancelTask cancel) throws IOException {
    return null;
  }

  public int getDataCount() {
    return -1;
  }

  public DataIterator getDataIterator(int bufferSize) throws IOException {
    return null;
  }

  public class DqcObsImpl extends ucar.nc2.dt.point.StationObsDatatypeImpl {
    String[] lineData;
    ucar.ma2.StructureDataW sdata;

    private DqcObsImpl( Station station, double dateValue, String line) {
      super(station, dateValue, dateValue);
      this.lineData = new String[1];
      lineData[0] = line;
    }

    public ucar.ma2.StructureData getData() throws IOException {
      if (sdata == null) {
        sdata = new ucar.ma2.StructureDataW( members);
        Array array = Array.factory( String.class, new int[] {1}, lineData);
        sdata.setMemberData("line", array);
      }
      return sdata;
    }

    public Date getNominalTimeAsDate() {
      return timeUnit.makeDate( getNominalTime());
    }

    public Date getObservationTimeAsDate() {
      return timeUnit.makeDate( getObservationTime());
    }
  }

  public static void main(String args[]) throws IOException {
    StringBuffer errlog = new StringBuffer();
    String dqc_location = "file:///C:/data/dqc/metarNew.xml";
    DqcStationObsDataset ds = factory("test", dqc_location, errlog);
    System.out.println(" errs= "+errlog);

    List stns = ds.getStations();
    System.out.println(" nstns= "+stns.size());

    Station stn = (Station) stns.get(13);
    List data = ds.getData( stn, null);
    for (int i = 0; i < data.size(); i++) {
      StationObsDatatypeImpl obs=  (StationObsDatatypeImpl) data.get(i);
      StructureData sdata = obs.getData();
      System.out.println(i+" "+sdata.getScalarString("line"));
    }

  }


}