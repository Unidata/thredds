// $Id: $
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

package thredds.servlet.ncSubset;

import ucar.ma2.StructureData;
import ucar.ma2.Array;
import ucar.nc2.dt.*;
import ucar.nc2.VariableIF;
import ucar.nc2.units.DateFormatter;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.util.Format;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Date;

import thredds.datatype.DateRange;

public class StationObsCollection {
  private static boolean debug = true;
  private static long timeToScan = 0;

  private ArrayList<Dataset> datasetList = new ArrayList<Dataset>();

  public StationObsCollection(String dirName) {

    double size = 0.0;
    int count = 0;
    File dir = new File(dirName);
    File[] files = dir.listFiles();
    for (int i = 0; i < files.length; i++) {
      File file = files[i];
      String fileS = file.getAbsolutePath();
      if (fileS.endsWith(".nc")) {
        datasetList.add( new Dataset(fileS));
        size += file.length();
        count++;
      }
    }
    if (debug)
      System.out.println("Reading directory " + dirName + " # files = " + count + " total file sizes = " + size / 1000 / 1000 + " Mb");
  }

  private class Dataset {
    String filename;
    Date time_start;
    Date time_end;

    Dataset( String filename) {
      this.filename = filename;
    }
  }

  ///////////////////////////////////////
  // station handling
  private List<Station> stationList;
  private HashMap<String, Station> stationMap;

  /**
   * Determine if any of the given station names are actually in the dataset.
   * @param stns  List of station names
   * @return  true if list is empty, ie no names are in the actual station list
   * @throws IOException
   */
  public boolean isStationListEmpty(List<String> stns) throws IOException {
    HashMap<String, Station> map = getStationMap();
    for (String stn : stns) {
      if (map.get(stn) != null) return false;
    }
    return true;
  }

  private List<Station> getStationList() throws IOException {
    if (null == stationList) {
      String url = datasetList.get(0).filename;
      StationObsDataset sod = null;
      try {
        sod = (StationObsDataset) TypedDatasetFactory.open(thredds.catalog.DataType.STATION, url, null, new StringBuffer());
        if (null != sod)
          stationList = new ArrayList(sod.getStations());

      } finally {
        if (null != sod)
          sod.close();
      }
    }

    return stationList;
  }

  private HashMap<String, Station> getStationMap() throws IOException {
    if (null == stationMap) {
      stationMap = new HashMap<String, Station>();
      List<Station> list = getStationList();
      for (Station station : list) {
        stationMap.put(station.getName(), station);
      }
    }
    return stationMap;
  }

  /**
   * Get the list of station names that are contained within the bounding box.
   * @param boundingBox lat/lon bounding box
   * @return list of station names contained within the bounding box
   * @throws IOException
   */
  public List<String> getStationNames(LatLonRect boundingBox) throws IOException {
    LatLonPointImpl latlonPt = new LatLonPointImpl();
    ArrayList<String> result = new ArrayList<String>();
    List stations = getStationList();
    for (int i = 0; i < stations.size(); i++) {
      Station s = (Station) stations.get(i);
      latlonPt.set(s.getLatitude(), s.getLongitude());
      if (boundingBox.contains(latlonPt))
        result.add(s.getName());
    }
    return result;
  }

  /**
   * Find the station closest to the specified point.
   * The metric is (lat-lat0)**2 + (cos(lat0)*(lon-lon0))**2
   *
   * @param lat  latitude value
   * @param lon  longitude value
   * @return name of station closest to the specified point
   * @throws IOException
   */
  public String findClosestStation(double lat, double lon) throws IOException {
    double cos = Math.cos(Math.toRadians(lat));
    List stations = getStationList();
    Station min_station = (Station) stations.get(0);
    double min_dist = Double.MAX_VALUE;

    for (int i = 0; i < stations.size(); i++) {
      Station s = (Station) stations.get(i);
      double lat1 = s.getLatitude();
      double lon1 = LatLonPointImpl.lonNormal(s.getLongitude(), lon);
      double dy = Math.toRadians(lat - lat1);
      double dx = cos * Math.toRadians(lon - lon1);
      double dist = dy * dy + dx * dx;
      if (dist < min_dist) {
        min_dist = dist;
        min_station = s;
      }
    }
    return min_station.getName();
  }

  ////////////////////////////////////////////////////////
  // scanning

  private void scanStations(Dataset ds, List<String> stns, DateRange range, Predicate p, Action a, Limit limit) throws IOException {

    StationObsDataset sod = null;
    try {
      sod = (StationObsDataset) TypedDatasetFactory.open(thredds.catalog.DataType.STATION, ds.filename, null, new StringBuffer());
      if (null == ds.time_start) {
        ds.time_start = sod.getStartDate();
        ds.time_end = sod.getEndDate();
        if (debug) System.out.println("scanStation open " + ds.filename+" start= "+ds.time_start+" end= "+ds.time_end);
      }

      for (String stn : stns) {
        Station s = sod.getStation(stn);
        if (s == null) {
          System.out.println("Cant find station " + s);
          continue;
        }

        DataIterator iter = sod.getDataIterator(s);
        while (iter.hasNext()) {
          StationObsDatatype sobs = (StationObsDatatype) iter.nextData();

          // date filter
          if (null != range) {
            Date obs = sobs.getObservationTimeAsDate();
            if (!range.included(obs))
              continue;
          }

          // general predicate filter
          StructureData sdata = sobs.getData();
          if ((p == null) || p.match(sdata)) {
            a.act(sod, sobs, sdata);
            limit.matches++;
          }

          limit.count++;
          if (limit.count > limit.limit) break;
        }
      }

    } finally {
      if (null != sod)
        sod.close();
    }
  }

  private void scanAll(String url, Predicate p, Action a, Limit limit) throws IOException {

    StationObsDataset dataset = null;
    try {
      if (debug) System.out.println("scanAll open " + url);
      dataset = (StationObsDataset) TypedDatasetFactory.open(thredds.catalog.DataType.STATION, url, null, new StringBuffer());

      DataIterator iter = dataset.getDataIterator(0);
      while (iter.hasNext()) {
        StationObsDatatype sobs = (StationObsDatatype) iter.nextData();

        StructureData sdata = sobs.getData();
        if ((p == null) || p.match(sdata)) {
          a.act(dataset, sobs, sdata);
          limit.matches++;
        }

        limit.count++;
        if (limit.count > limit.limit) break;
      }

    } finally {
      if (null != dataset)
        dataset.close();
    }
  }

  private interface Predicate {
    boolean match(StructureData sdata);
  }

  private interface Action {
    void act(StationObsDataset sod, StationObsDatatype sobs, StructureData sdata) throws IOException;
  }

  private class Limit {
    int count;
    int limit = Integer.MAX_VALUE;
    int matches;
  }


  ////////////////////////////////////////////////////////////////
  // writing in different formats

  private List<Dataset> filterDataset( DateRange range) {
    if (range == null)
      return datasetList;

    List<Dataset> result = new ArrayList<Dataset>();
    for (Dataset ds : datasetList) {
      if ((null == ds.time_start) || range.intersect(ds.time_start, ds.time_end))
        result.add(ds);
    }
    return result;

  }

  public void write(List<String> vars, List<String> stns, DateRange range, String type, java.io.PrintWriter pw) throws IOException {
    long start = System.currentTimeMillis();
    Limit counter = new Limit();

    Writer w = null;
    if (type.equals(StationObsServlet.RAW)) {
      w = new WriterRaw(vars, pw);
    } else if (type.equals(StationObsServlet.XML)) {
      w = new WriterXML(vars, pw);
    } else if (type.equals(StationObsServlet.CSV)) {
      w = new WriterCSV(vars, pw);
    }

    Action act = w.getAction();
    w.header();
    List<Dataset> need = filterDataset( range);
    for (Dataset ds : need) {
      scanStations(ds, stns, range, null, act, counter);
    }
    w.trailer();

    pw.flush();

    if (debug) {
      long took = System.currentTimeMillis() - start;
      System.out.println("\nread " + counter.count + " records; match and write " + counter.matches + " raw records");
      System.out.println("that took = " + took + " msecs");

      if (timeToScan > 0) {
        long writeTime = took - timeToScan;
        double mps = 1000 * counter.matches / writeTime;
        System.out.println("  writeTime = " + writeTime + " msecs; write messages/sec = " + mps);
      }
    }
  }

  abstract class Writer {
    abstract void header();

    abstract Action getAction();

    abstract void trailer();

    List<String> vars;
    java.io.PrintWriter writer;
    DateFormatter format = new DateFormatter();

    Writer(List<String> vars, final java.io.PrintWriter writer) {
      this.vars = vars;
      this.writer = writer;
    }

    List<String> getVarNames(List<String> vars, List dataVariables) {
      List<String> result = new ArrayList();
      for (int i = 0; i < dataVariables.size(); i++) {
        VariableIF v = (VariableIF) dataVariables.get(i);
        if ((vars == null) || vars.contains(v.getName()))
          result.add(v.getName());
      }
      return result;
    }
  }


  class WriterRaw extends Writer {

    WriterRaw(List<String> vars, final java.io.PrintWriter writer) {
      super(vars, writer);
    }

    public void header() { }
    public void trailer() { }

    Action getAction() {
      return new Action() {
        public void act(StationObsDataset sod, StationObsDatatype sobs, StructureData sdata) throws IOException {
          String report = sdata.getScalarString("report");
          writer.println(report);
        }
      };
    }
  }


  class WriterXML extends Writer {

    WriterXML(List<String> vars, final java.io.PrintWriter writer) {
      super(vars, writer);
    }

    public void header() {
      writer.println("<?xml version='1.0' encoding='UTF-8'?>");
      writer.println("<metarCollection dataset='name'>\n");
    }

    public void trailer() {
      writer.println("</metarCollection>");
    }

    Action getAction() {
      return new Action() {
        public void act(StationObsDataset sod, StationObsDatatype sobs, StructureData sdata) throws IOException {
          Station s= sobs.getStation();

          writer.print("  <metar date='");
          writer.print(  format.toDateTimeStringISO( sobs.getObservationTimeAsDate()));
          writer.println("'>");

          writer.print("    <station name='" + s.getName() +
                  "' latitude='" + Format.dfrac(s.getLatitude(), 3) +
                  "' longitude='" + Format.dfrac(s.getLongitude(), 3));
          if (!Double.isNaN(s.getAltitude()))
            writer.print("' altitude='" + Format.dfrac(s.getAltitude(),0));
          writer.println("'/>");

          List<String> varNames = getVarNames(vars, sod.getDataVariables());
          for (int i = 0; i < varNames.size(); i++) {
            String name = varNames.get(i);
            writer.print("    <data name='" + name + "'>");
            Array sdataArray = sdata.getArray(name);
            writer.println(sdataArray.toString() + "</data>");
          }
          writer.println("  </metar>");
        }
      };
    }
  }

  class WriterCSV extends Writer {
    boolean headerWritten = false;
    List<String> validVarNames;

    WriterCSV(List<String> stns, final java.io.PrintWriter writer) {
      super(stns, writer);
    }

    public void header() {}
    public void trailer() {}

    Action getAction() {
      return new Action() {
        public void act(StationObsDataset sod, StationObsDatatype sobs, StructureData sdata) throws IOException {
          if (!headerWritten) {
            writer.print("time,station,latitude,longitude");
            validVarNames = getVarNames(vars, sod.getDataVariables());
            for (String name : validVarNames) {
              writer.print(",");
              writer.print(name);
            }
            writer.println();
            headerWritten = true;
          }

          Station s= sobs.getStation();

          writer.print(  format.toDateTimeStringISO( sobs.getObservationTimeAsDate()));
          writer.print(',');
          writer.print(s.getName());
          writer.print(',');
          writer.print(Format.dfrac(s.getLatitude(),3));
          writer.print(',');
          writer.print(Format.dfrac(s.getLongitude(),3));

          for (String name : validVarNames) {
            writer.print(',');
            Array sdataArray = sdata.getArray(name);
            writer.print(sdataArray.toString());
          }
          writer.println();
        }
      };
    }
  }

  ///////////////////////////////////////////////////////////

  /* public void timeNetcdf() throws IOException {

    Limit limit = new Limit();
    Predicate p = new Predicate() {
      public boolean match(StructureData sdata) {
        return (sdata.getScalarFloat("wind_peak_speed") > 10);
      }
    };
    Action act = new Action() {
      public void act(StructureData sdata) {
      }
    };

    long start = System.currentTimeMillis();
    for (int i = 0; i < fileList.size(); i++) {
      String s = fileList.get(i);
      scanAll(s, p, act, limit);
    }

    long took = System.currentTimeMillis() - start;
    double mps = 1000 * limit.count / took;
    System.out.println("\nscanAllNetcdf successfully read " + limit.count + " records; found " + limit.matches + " matches");
    System.out.println("that took = " + took + " msecs; messages/sec = " + mps);

    timeToScan = took;
  }

  public void timeNetcdfStation() throws IOException {

    Limit limit = new Limit();
    Predicate p = new Predicate() {
      public boolean match(StructureData sdata) {
        return true;
      }
    };
    Action act = new Action() {
      public void act(StructureData sdata) {
      }
    };

    long start = System.currentTimeMillis();
    List<String> stns = new ArrayList<String>();
    stns.add("ACK");

    for (int i = 0; i < fileList.size(); i++) {
      String s = fileList.get(i);
      scanStations(s, stns, p, act, limit);
    }

    long took = System.currentTimeMillis() - start;
    double mps = 1000 * limit.count / took;
    System.out.println("\ntimeNetcdfStation successfully read " + limit.count + " records; found " + limit.matches + " matches");
    System.out.println("that took = " + took + " msecs; obs/sec= " + mps);
  }

  private static DataOutputStream xout;

  public void writeXML() throws IOException {
    xout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream("C:/temp/temp2.xml"), 10000));
    xout.writeBytes("<?xml version='1.0' encoding='UTF-8'?>\n");
    xout.writeBytes("<metarCollection dataset='name'>\n");

    Limit limit = new Limit();
    Predicate p = new Predicate() {
      public boolean match(StructureData sdata) {
        return (sdata.getScalarFloat("wind_peak_speed") > 10);
      }
    };

    Action act = new Action() {
      public void act(StructureData sdata) throws IOException {
        xout.writeBytes("  <metar>\n");
        for (Iterator iter = sdata.getMembers().iterator(); iter.hasNext();) {
          StructureMembers.Member m = (StructureMembers.Member) iter.next();
          xout.writeBytes("    <data name='" + m.getName() + "'>");

          Array sdataArray = sdata.getArray(m);
          // System.out.println(m.getName()+" "+m.getDataType()+" "+sdataArray.toString());
          xout.writeBytes(sdataArray.toString() + "</data>\n");
        }

        xout.writeBytes("  </metar>\n");
      }
    };

    long start = System.currentTimeMillis();

    for (int i = 0; i < fileList.size(); i++) {
      String s = fileList.get(i);
      scanAll(s, p, act, limit);
    }

    xout.writeBytes("</metarCollection>\n");
    xout.close();

    long took = System.currentTimeMillis() - start;
    System.out.println("\nscanAllNetcdf  read " + limit.count + " records; match and write " + limit.matches + " XML records");
    System.out.println("that took = " + took + " msecs");

    long writeTime = took - timeToScan;
    double mps = 1000 * limit.matches / writeTime;
    System.out.println("  writeTime = " + writeTime + " msecs; write messages/sec = " + mps);
  }

  private BufferedOutputStream fout;

  public void writeRaw() throws IOException {
    fout = new BufferedOutputStream(new FileOutputStream("C:/temp/raw.txt"), 10000);

    Limit limit = new Limit();
    Predicate p = new Predicate() {
      public boolean match(StructureData sdata) {
        return (sdata.getScalarFloat("wind_peak_speed") > 10);
      }
    };

    Action act = new Action() {
      public void act(StructureData sdata) throws IOException {
        String report = sdata.getScalarString("report");
        fout.write(report.getBytes());
        fout.write((int) '\n');
      }
    };

    long start = System.currentTimeMillis();

    for (int i = 0; i < fileList.size(); i++) {
      String s = fileList.get(i);
      scanAll(s, p, act, limit);
    }

    fout.close();

    long took = System.currentTimeMillis() - start;
    System.out.println("\nscanAllNetcdf  read " + limit.count + " records; match and write " + limit.matches + " raw records");
    System.out.println("that took = " + took + " msecs");

    long writeTime = took - timeToScan;
    double mps = 1000 * limit.matches / writeTime;
    System.out.println("  writeTime = " + writeTime + " msecs; write messages/sec = " + mps);
  }

  /* public static void  addData(StructureData sdata) {

   Element elem = new Element("metar");
   for (Iterator iter = sdata.getMembers().iterator(); iter.hasNext();) {
     StructureMembers.Member m = (StructureMembers.Member) iter.next();
     Element dataElem = new Element("data");
     dataElem.setAttribute("name", m.getName());
     elem.addContent(dataElem);

     Array sdataArray = sdata.getArray(m);
     // System.out.println(m.getName()+" "+m.getDataType()+" "+sdataArray.toString());
     dataElem.addContent(sdataArray.toString());
   }

   rootElem.addContent(elem);
 } */


  static public void main(String args[]) throws IOException {
    //getFiles("R:/testdata/station/ldm/metar/");
    StationObsCollection soc = new StationObsCollection("C:/data/metars/");
  }

}
