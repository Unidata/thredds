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
import ucar.ma2.StructureMembers;
import ucar.nc2.dt.*;
import ucar.nc2.VariableIF;
import ucar.nc2.util.CancelTask;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPointImpl;

import java.io.*;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.Document;
import org.jdom.Element;


public class StationObsCollection {
  private static boolean debug = true;
  private static long timeToScan = 0;

  private ArrayList<String> fileList = new ArrayList<String>();

  public StationObsCollection(String dirName) {
    double size = 0.0;
    int count = 0;
    File dir = new File(dirName);
    File[] files = dir.listFiles();
    for (int i = 0; i < files.length; i++) {
      File file = files[i];
      String fileS = file.getAbsolutePath();
      if (fileS.endsWith(".nc")) {
        fileList.add(fileS);
        size += file.length();
        count++;
      }
    }
    if (debug)
      System.out.println("Reading directory " + dirName + " # files = " + count + " total file sizes = " + size / 1000 / 1000 + " Mb");
  }


  private List<Station> stationList;
  private HashMap<String, Station> stationMap;

  private List<Station> getStationList() throws IOException {
    if (null == stationList) {
      String url = fileList.get(0);
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

  public String findClosestStation(double lat, double lon) throws IOException {
    double min_dist = Double.MAX_VALUE;

    double cos = Math.cos(Math.toRadians(lat));
    List stations = getStationList();
    Station min_station = (Station) stations.get(0);

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

  private void scanStations(String url, List<String> stns, Predicate p, Action a, Limit limit) throws IOException {

    StationObsDataset sod = null;
    try {
      if (debug) System.out.println("scanStation open " + url);
      sod = (StationObsDataset) TypedDatasetFactory.open(thredds.catalog.DataType.STATION, url, null, new StringBuffer());

      for (String stn : stns) {
        Station s = sod.getStation(stn);
        if (s == null) {
          System.out.println("Cant find station " + s);
          continue;
        }

        DataIterator iter = sod.getDataIterator(s);
        while (iter.hasNext()) {
          StationObsDatatype dtype = (StationObsDatatype) iter.nextData();
          StructureData sdata = dtype.getData();
          if ((p == null) || p.match(sdata)) {
            a.act(sod, dtype.getStation(), sdata);
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
        StationObsDatatype sdtype = (StationObsDatatype) iter.nextData();

        StructureData sdata = sdtype.getData();
        if ((p == null) || p.match(sdata)) {
          a.act(dataset, sdtype.getStation(), sdata);
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
    void act(StationObsDataset sod, Station s, StructureData sdata) throws IOException;
  }

  private class Limit {
    int count;
    int limit = Integer.MAX_VALUE;
    int matches;
  }

  public void write(List<String> vars, List<String> stns, Predicate p, String type, java.io.PrintWriter writer) throws IOException {
    if (type.equals(StationObsServlet.RAW)) {
      writeRaw(stns, p, writer);

    } else if (type.equals(StationObsServlet.XML)) {
      writeXML(vars, stns, p, writer);

    } else if (type.equals(StationObsServlet.CSV)) {
      writeCSV(vars, stns, p, writer);

    }
  }


  public void writeRaw(List<String> stns, Predicate p, final java.io.PrintWriter writer) throws IOException {

    Limit limit = new Limit();
    Action act = new Action() {
      public void act(StationObsDataset sod, Station s, StructureData sdata) throws IOException {
        String report = sdata.getScalarString("report");
        writer.println(report);
      }
    };

    long start = System.currentTimeMillis();

    for (String filename : fileList) {
      scanStations(filename, stns, p, act, limit);
    }

    writer.close();

    if (debug) {
      long took = System.currentTimeMillis() - start;
      System.out.println("\nscanAllNetcdf  read " + limit.count + " records; match and write " + limit.matches + " raw records");
      System.out.println("that took = " + took + " msecs");

      long writeTime = took - timeToScan;
      double mps = 1000 * limit.matches / writeTime;
      System.out.println("  writeTime = " + writeTime + " msecs; write messages/sec = " + mps);
    }
  }

  private List<String> getVarNames(List<String> vars, List dataVariables) {
    List<String> result = new ArrayList();
    for (int i = 0; i < dataVariables.size(); i++) {
      VariableIF v = (VariableIF) dataVariables.get(i);
      if ((vars == null) || vars.contains(v.getName()))
        result.add(v.getName());
    }
    return result;
  }

  public void writeXML(final List<String> vars, List<String> stns, Predicate p, final java.io.PrintWriter writer) throws IOException {
    writer.println("<?xml version='1.0' encoding='UTF-8'?>");
    writer.println("<metarCollection dataset='name'>\n");

    Limit limit = new Limit();

    Action act = new Action() {
      public void act(StationObsDataset sod, Station s, StructureData sdata) throws IOException {
        writer.println("  <metar>");
        writer.print("    <station name='" + s.getName() + "' latitude='" + s.getLatitude() + "' longitude='" + s.getLongitude());
        if (!Double.isNaN(s.getAltitude()))
          writer.print("' altitude='" + s.getAltitude());
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

    long start = System.currentTimeMillis();

    for (String filename : fileList) {
      scanStations(filename, stns, p, act, limit);
    }

    writer.println("</metarCollection>");
    writer.close();

    if (debug) {
      long took = System.currentTimeMillis() - start;
      System.out.println("\nscanAllNetcdf  read " + limit.count + " records; match and write " + limit.matches + " XML records");
      System.out.println("that took = " + took + " msecs");

      long writeTime = took - timeToScan;
      double mps = 1000 * limit.matches / writeTime;
      System.out.println("  writeTime = " + writeTime + " msecs; write messages/sec = " + mps);
    }
  }

  public void writeCSV(final List<String> vars, List<String> stns, Predicate p, final java.io.PrintWriter writer) throws IOException {

    Limit limit = new Limit();

    Action act = new Action() {
      boolean headerWritten = false;
      List<String> varNames;

      public void act(StationObsDataset sod, Station s, StructureData sdata) throws IOException {
        if (!headerWritten) {
          writer.print("station,latitude,longitude");
          varNames = getVarNames(vars, sod.getDataVariables());
          for (int i = 0; i < varNames.size(); i++) {
            String name = varNames.get(i);
            writer.print(",");
            writer.print(name);
          }
          writer.println();
          headerWritten = true;
        }

        writer.print(s.getName());
        writer.print(',');
        writer.print(s.getLatitude());
        writer.print(',');
        writer.print(s.getLongitude());

        for (int i = 0; i < varNames.size(); i++) {
          String name = varNames.get(i);
          writer.print(',');

          Array sdataArray = sdata.getArray(name);
          writer.print(sdataArray.toString());
        }
        writer.println();
      }
    };

    long start = System.currentTimeMillis();

    for (String filename : fileList) {
      scanStations(filename, stns, p, act, limit);
    }

    writer.close();

    if (debug) {
      long took = System.currentTimeMillis() - start;
      System.out.println("\nscanAllNetcdf  read " + limit.count + " records; match and write " + limit.matches + " XML records");
      System.out.println("that took = " + took + " msecs");

      long writeTime = took - timeToScan;
      double mps = 1000 * limit.matches / writeTime;
      System.out.println("  writeTime = " + writeTime + " msecs; write messages/sec = " + mps);
    }
  }

  public boolean isStationListEmpty(List<String> stns) throws IOException {
    HashMap<String, Station> map = getStationMap();
    for (String stn : stns) {
      if (map.get(stn) != null) return false;
    }
    return true;
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
