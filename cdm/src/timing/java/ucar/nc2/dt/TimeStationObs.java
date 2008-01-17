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

package ucar.nc2.dt;

import ucar.ma2.StructureData;
import ucar.ma2.Array;
import ucar.ma2.StructureMembers;
import ucar.nc2.constants.DataType;

import java.io.*;
import java.util.Iterator;
import java.util.ArrayList;


public class TimeStationObs {
  static private boolean debug = false;

  private static long timeToScan = 0;

  public static void scanStation(String url, String station, Predicate p, Action a, Limit limit) throws IOException {

    StationObsDataset sod = null;
    try {
      if (debug) System.out.println("scanStation open "+url);
      sod = (StationObsDataset) TypedDatasetFactory.open(DataType.STATION, url, null, new StringBuffer());

      Station s = sod.getStation(station);
      if (s == null) return;

      DataIterator iter = sod.getDataIterator(s);
      while (iter.hasNext()) {
        StationObsDatatype dtype = (StationObsDatatype) iter.nextData();
        StructureData sdata = dtype.getData();
        if (p.match(sdata)) {
          a.act(sdata);
          limit.matches++;
        }

        limit.count++;
        if (limit.count > limit.limit) break;
      }

    } finally {
      if (null != sod)
        sod.close();
    }
  }

  public static void scanAll(String url, Predicate p, Action a, Limit limit) throws IOException {

    PointObsDataset dataset = null;
    try {
      if (debug) System.out.println("scanAll open "+url);
      dataset = (PointObsDataset) TypedDatasetFactory.open(DataType.POINT, url, null, new StringBuffer());

      DataIterator iter = dataset.getDataIterator(0);
      while (iter.hasNext()) {
        PointObsDatatype pobs = (PointObsDatatype) iter.nextData();

        StructureData sdata = pobs.getData();
        if (p.match(sdata)) {
          a.act(sdata);
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
    void act(StructureData sdata) throws IOException;
  }

  private class Limit {
    int count;
    int limit = Integer.MAX_VALUE;
    int matches;
  }

  public void timeNetcdf() throws IOException {

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
      String s = (String) fileList.get(i);
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

    for (int i = 0; i < fileList.size(); i++) {
      String s = (String) fileList.get(i);
      scanStation(s, "ACK", p, act, limit);
    }

    long took = System.currentTimeMillis() - start;
    double mps = 1000 * limit.count / took;
    System.out.println("\ntimeNetcdfStation successfully read " + limit.count + " records; found " + limit.matches + " matches");
    System.out.println("that took = " + took + " msecs; obs/sec= "+mps);
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
      String s = (String) fileList.get(i);
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
      String s = (String) fileList.get(i);
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

  static private ArrayList fileList = new ArrayList();

  static private void getFiles(String dirName) {
    double size = 0.0;
    int count = 0;
    File dir = new File(dirName);
    File[] files = dir.listFiles();
    for (int i = 0; i < files.length; i++) {
      File file = files[i];
      String fileS = file.getAbsolutePath();
      if (fileS.contains("Surface") && fileS.endsWith(".nc")) {
        fileList.add(fileS);
        size += file.length();
        count++;
      }
    }
    System.out.println("Reading directory " + dirName + " # files = "+count+" total file sizes = " + size/1000/1000+" Mb");
  }

  static public void main(String args[]) throws IOException {
    //getFiles("R:/testdata/station/ldm/metar/");
    getFiles("C:/data/metars/");

    TimeStationObs t = new TimeStationObs();
    t.timeNetcdf();
    t.writeXML();
    t.writeRaw();

    t.timeNetcdfStation();
  }

}
