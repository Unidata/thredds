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

import java.io.*;
import java.util.Iterator;

import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.Document;
import org.jdom.Element;


public class TimeStationObs {
  static boolean showMemm = false, writeRaw = false, showXML = true;
  static Runtime runtime = Runtime.getRuntime();

  static DataOutputStream xout;

  public static void scanStation(String url, String station, Predicate p, Action a, Limit limit) throws IOException {

    StationObsDataset sod = null;
    try {
      sod = (StationObsDataset) TypedDatasetFactory.open(thredds.catalog.DataType.STATION, url, null, new StringBuffer());

      Station s = sod.getStation(station);
      if (s == null) return;

      DataIterator iter = sod.getDataIterator(s);
      while (iter.hasNext()) {
        StationObsDatatype dtype = (StationObsDatatype) iter.nextData();
        StructureData sdata = dtype.getData();
        if (p.match(sdata))
          a.act(sdata);

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
      dataset = (PointObsDataset) TypedDatasetFactory.open(thredds.catalog.DataType.POINT, url, null, new StringBuffer());

      DataIterator iter = dataset.getDataIterator(0);
      while (iter.hasNext()) {
        PointObsDatatype pobs = (PointObsDatatype) iter.nextData();

        StructureData sdata = pobs.getData();
        if (p.match(sdata))
          a.act(sdata);

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
    void act(StructureData sdata);
  }

  private class Limit {
    int count;
    int limit = Integer.MAX_VALUE;
  }

  int matches = 0;

  public void timeNetcdf() throws IOException {

    Limit limit = new Limit();
    Predicate p = new Predicate() {
      public boolean match(StructureData sdata) {
        return (sdata.getScalarFloat("wind_peak_speed") > 10);
      }
    };
    Action act = new Action() {
      public void act(StructureData sdata) {
        matches++;
      }
    };

    long start = System.currentTimeMillis();

    scanAll("C:/data/metars/Surface_METAR_20070326_0000.nc", p, act, limit);
    scanAll("C:/data/metars/Surface_METAR_20070329_0000.nc", p, act, limit);
    scanAll("C:/data/metars/Surface_METAR_20070330_0000.nc", p, act, limit);
    scanAll("C:/data/metars/Surface_METAR_20070331_0000.nc", p, act, limit);

    long took = System.currentTimeMillis() - start;
    double mps = 1000 * limit.count / took;
    System.out.println("successfully read " + limit.count + " records; found " + matches + " matches");
    System.out.println("that took = " + took + " msecs; messages/sec = " + mps);
  }

  public void timeNetcdfStation() throws IOException {

    Limit limit = new Limit();
    Predicate p = new Predicate() {
      public boolean match(StructureData sdata) {
        return (sdata.getScalarFloat("wind_peak_speed") > 10);
      }
    };
    Action act = new Action() {
      public void act(StructureData sdata) {
        matches++;
      }
    };

    long start = System.currentTimeMillis();

    scanStation("C:/data/metars/Surface_METAR_20070326_0000.nc", "ACK", p, act, limit);
    scanStation("C:/data/metars/Surface_METAR_20070329_0000.nc", "ACK", p, act, limit);
    scanStation("C:/data/metars/Surface_METAR_20070330_0000.nc", "ACK", p, act, limit);
    scanStation("C:/data/metars/Surface_METAR_20070331_0000.nc", "ACK", p, act, limit);
    /* scanAll("C:/data/metars/Surface_METAR_20070329_0000.nc", p, act, limit);
    scanAll("C:/data/metars/Surface_METAR_20070330_0000.nc", p, act, limit);
    scanAll("C:/data/metars/Surface_METAR_20070331_0000.nc", p, act, limit); */

    long took = System.currentTimeMillis() - start;
    double mps = 1000 * limit.count / took;
    System.out.println("successfully read " + limit.count + " records; found " + matches + " matches");
    System.out.println("that took = " + took + " msecs; messages/sec = " + mps);
  }

  public static int doOne(int count, String url, OutputStream fout) throws IOException {
    PointObsDataset dataset = null;
    try {
      dataset = (PointObsDataset) TypedDatasetFactory.open(thredds.catalog.DataType.POINT, url, null, new StringBuffer());

      DataIterator iter = dataset.getDataIterator(0);
      while (iter.hasNext()) {
        PointObsDatatype pobs = (PointObsDatatype) iter.nextData();

        StructureData sdata = pobs.getData();
        String report = sdata.getScalarString("report");

        if (fout != null) {
          fout.write(report.getBytes());
          fout.write((int) '\n');
        }

        if (showXML && count < 10000)
          addData(sdata);
        if (showXML && count > 10000)
          break;

        if (showMemm && count % 1000 == 0)
          showMem(count);
        count++;
      }

    } catch (OutOfMemoryError e) {
      e.printStackTrace();
      System.err.println("OutOfMemoryError after reading " + count + " records (of " + dataset.getDataCount() + ")");

    } finally {
      if (null != dataset)
        dataset.close();
    }

    return count;
  }


  private static void showMem(int count) {
    System.out.println(count + " free = " + runtime.freeMemory() * .001 * .001 +
        " total= " + runtime.totalMemory() * .001 * .001 +
        " max= " + runtime.maxMemory() * .001 * .001 +
        " Mb");
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

  public static void addData(StructureData sdata) throws IOException {
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

  public static void main2(String args[]) throws IOException {
    int count = 0;
    OutputStream fout = null;

    if (writeRaw && args.length > 0) {
      fout = new BufferedOutputStream(new FileOutputStream(args[0]), 10000);
    }

    if (showXML) {
      xout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream("C:/temp/temp2.xml"), 10000));
      xout.writeBytes("<?xml version='1.0' encoding='UTF-8'?>\n");
      xout.writeBytes("<metarCollection dataset='name'>\n");
    }

    long start = System.currentTimeMillis();
    try {
      //String url = "http://localhost:8080/thredds/dodsC/metarCollection/Surface_METAR_20060629_0000.nc";
      // String url = "dods://motherlode.ucar.edu:8080/thredds/dodsC/station/metar/Surface_METAR_20060629_0000.nc";

      count = doOne(count, "C:/data/metars/Surface_METAR_20070326_0000.nc", fout);
      //count = doOne(count, "C:/data/metars/Surface_METAR_20070329_0000.nc", fout);
      //count = doOne(count, "C:/data/metars/Surface_METAR_20070330_0000.nc", fout);
      //count = doOne(count, "C:/data/metars/Surface_METAR_20070331_0000.nc", fout);

      if (showXML) {
        xout.writeBytes("</metarCollection>\n");
        xout.close();

        /* XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
        BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream("C:/temp/temp.xml"), 10000);       
        fmt.output(doc, os);  */
      }

      long took = System.currentTimeMillis() - start;
      System.out.println("that took = " + took + " msecs");


    } finally {
      if (fout != null)
        fout.close();
    }

    System.out.println("successfully read " + count + " records");
    showMem(count);
  }

  static public void main(String args[]) throws IOException {
    TimeStationObs t = new TimeStationObs();
    t.timeNetcdfStation();
  }

}
