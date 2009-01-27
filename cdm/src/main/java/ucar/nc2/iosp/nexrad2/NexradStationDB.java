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

package ucar.nc2.iosp.nexrad2;

import ucar.nc2.util.TableParser;

import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Map;
import java.io.IOException;
import java.io.InputStream;

import org.jdom.*;
import org.jdom.input.*;

/**
 * Manage Nexrad Stations "database"
 * @author caron
 */
public class NexradStationDB {

  private static boolean showStations = false;
  private static Map<String,Station> stationTableHash = null;
  private static Map<String,Station> stationTableHash1 = null;

  public static synchronized void init() throws IOException {
    if (stationTableHash == null)
      readStationTableXML();
  }

  public static Station get(String id) { return stationTableHash.get(id); }

  public static Station getByIdNumber(String idn) { return stationTableHash1.get(idn); }

  private static void readStationTableXML() throws IOException {
    stationTableHash = new HashMap<String,Station>();
    stationTableHash1 = new HashMap<String,Station>();
    ClassLoader cl = Level2VolumeScan.class.getClassLoader();
    InputStream is = cl.getResourceAsStream("resources/nj22/tables/nexradstns.xml");

    Document doc;
    SAXBuilder saxBuilder = new SAXBuilder();
    try {
      doc = saxBuilder.build(is);
    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }

    Element root = doc.getRootElement();
    List children = root.getChildren("station");
    for (int i = 0; i < children.size(); i++) {
      Element sElem =  (Element)children.get(i);
      String idn = sElem.getAttributeValue("idn");
      String id = sElem.getAttributeValue("id");
      String name = sElem.getAttributeValue("name");
      String st = sElem.getAttributeValue("st");
      String co = sElem.getAttributeValue("co");
      String lat = sElem.getAttributeValue("lat");
      String lon = sElem.getAttributeValue("lon");
      String elev = sElem.getAttributeValue("elev");

      Station s = new Station();
      s.id = "K"+id;
      s.name = name + "," + st+ "," + co;
      s.lat = parseDegree(lat);
      s.lon = parseDegree(lon);
      s.elev = Double.parseDouble(elev);

      stationTableHash.put(s.id, s);
      stationTableHash1.put(idn, s);
      if (showStations) System.out.println(" station= "+s);
    }
  }

    private static double parseDegree( String s) {
      StringTokenizer stoke = new StringTokenizer(s, ":");
      String degS = stoke.nextToken();
      String minS = stoke.nextToken();
      String secS = stoke.nextToken();

      try {
        double deg = Double.parseDouble( degS);
        double min = Double.parseDouble( minS);
        double sec = Double.parseDouble( secS);
        if (deg < 0)
          return deg - min/60 - sec/3600;
        else
          return deg + min/60 + sec/3600;
      } catch (NumberFormatException e) {
        e.printStackTrace();
      }
      return 0.0;
    }

  // this is the old Gempak table, not as precise
   private static void readStationTable() throws IOException {
    stationTableHash = new HashMap<String,Station>();

    ClassLoader cl = Level2VolumeScan.class.getClassLoader();
    InputStream is = cl.getResourceAsStream("resources/nj22/tables/nexrad.tbl");

    List<TableParser.Record> recs = TableParser.readTable(is, "3,15,46, 54,60d,67d,73d", 50000);
    for (TableParser.Record record : recs) {
      Station s = new Station();
      s.id = "K" + record.get(0);
      s.name = record.get(2) + " " + record.get(3);
      s.lat = (Double) record.get(4) * .01;
      s.lon = (Double) record.get(5) * .01;
      s.elev = (Double) record.get(6);

      stationTableHash.put(s.id, s);
      if (showStations) System.out.println(" station= " + s);
    }
  }

  public static class Station {
    public String id, name;
    public double lat;
    public double lon;
    public double elev;

    public String toString() { return id +" <"+ name +">   "+ lat +" "+ lon +" "+ elev; }
  }

}
