// $Id:NexradStationDB.java 63 2006-07-12 21:50:51Z edavis $
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

package ucar.nc2.iosp.nexrad2;

import ucar.nc2.util.TableParser;

import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.io.IOException;
import java.io.InputStream;

import org.jdom.*;
import org.jdom.input.*;

/**
 *
 * @author caron
 * @version $Revision:63 $ $Date:2006-07-12 21:50:51Z $
 */
public class NexradStationDB {

  private static boolean showStations = false;
  private static HashMap stationTableHash = null;

  public static synchronized void init() throws IOException {
    if (stationTableHash == null)
      readStationTableXML();
  }

  public static Station get(String id) { return (Station) stationTableHash.get(id); }

  private static void readStationTableXML() throws IOException {
    stationTableHash = new HashMap();

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
    stationTableHash = new HashMap();

    ClassLoader cl = Level2VolumeScan.class.getClassLoader();
    InputStream is = cl.getResourceAsStream("resources/nj22/tables/nexrad.tbl");

    List recs = TableParser.readTable(is, "3,15,46, 54,60d,67d,73d", 50000);
    for (int i = 0; i < recs.size(); i++) {
      TableParser.Record record = (TableParser.Record) recs.get(i);
      Station s = new Station();
      s.id = "K"+record.get(0);
      s.name = record.get(2) + " " + record.get(3);
      s.lat = ((Double) record.get(4)).doubleValue() * .01;
      s.lon = ((Double) record.get(5)).doubleValue()* .01;
      s.elev = ((Double) record.get(6)).doubleValue();

      stationTableHash.put(s.id, s);
      if (showStations) System.out.println(" station= "+s);
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
