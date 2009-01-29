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
package ucar.nc2.iosp.adde;

import ucar.nc2.util.CancelTask;

import java.io.*;
import java.util.*;
import java.net.URL;

import edu.wisc.ssec.mcidas.adde.AddePointDataReader;
import edu.wisc.ssec.mcidas.adde.AddeException;
import edu.wisc.ssec.mcidas.McIDASUtil;

/**
 * Reads station table ascii output from mcidas STNLIST command
 */
public class StationDB {
  static private boolean debugOpen = false, debugCall = false, debugParse = false;

  private ArrayList stations = new ArrayList();

  /**
   * Reads station table ascii output from mcidas STNLIST command
   */
  public StationDB(String urlString) throws IOException {
    long start = System.currentTimeMillis();

    InputStream ios = null;
    if (urlString.startsWith("http:")) {
      URL url = new URL(urlString);
      ios = url.openStream();
      if (debugOpen) System.out.println("opened URL " + urlString);
    } else if (urlString.startsWith("resource:")) {
      ClassLoader cl = getClass().getClassLoader();
      ios = cl.getResourceAsStream(urlString.substring(9));
      if (debugOpen) System.out.println("opened resource " + urlString);
    } else {
      ios = new FileInputStream(urlString);
      if (debugOpen) System.out.println("opened file " + urlString);
    }

    // DataInputStream dataIS = new DataInputStream( new BufferedInputStream(ios, 20000));
    BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios));

    dataIS.readLine();
    dataIS.readLine();
    int count = 0;
    while (true) {
      String line = dataIS.readLine();
      if (line == null) break;
      if (line.length() < 85) break;

      String idn = line.substring(0, 5);
      String id = line.substring(6, 10);
      String name = line.substring(13, 33);
      String type = line.substring(34, 51);
      String state = line.substring(52, 54);
      String country = line.substring(55, 57);
      String latS = line.substring(58, 68);
      String lonS = line.substring(69, 79);
      String elevS = line.substring(80, 85);
      Station s = new Station(idn, id, name, type, state, country, latS, lonS, elevS);
      stations.add(s);

      if (debugParse) System.out.println(s);
      count++;
      // if (count > 1) break;
    }

    dataIS.close();

    if (debugCall) {
      long took = System.currentTimeMillis() - start;
      System.out.println(" read " + urlString + " count=" + stations.size() + " took=" + took + " msec ");
    }
  }

  /**
   * Reads station info by making a call to ADDE server: very slow!!!
   */
  StationDB(String location, CancelTask cancel) throws IOException {
    HashMap hashStations = new HashMap(5000);

    try {
      if (debugOpen) System.out.println("Call ADDE Server " + location);
      long start = System.currentTimeMillis();
      AddePointDataReader reader = AddeStationObsDataset.callAdde(location + "&num=all&param=ID LAT LON ZS"); // LOOK
      long took = System.currentTimeMillis() - start;
      if (debugOpen) System.out.println(" time = " + took);

      int[][] stationData = reader.getData();
      int nparams = stationData.length;
      int nstations = stationData[0].length;
      System.out.println(" nparams= " + nparams + " nstations=" + nstations);
      System.out.println(" size= " + (nparams * nstations * 4) + " bytes");

      int[] scales = reader.getScales();
      double[] scaleFactor = new double[scales.length];
      for (int i = 0; i < nparams; i++) {
        scaleFactor[i] = (scales[i] == 0) ? 1.0 : 1.0 / Math.pow(10.0, (double) scales[i]);
      }

      if ((cancel != null) && cancel.isCancel()) return;

      int last = 0;
      for (int i = 0; i < nstations; i++) {
        String stnId = McIDASUtil.intBitsToString(stationData[0][i]);
        if (!hashStations.containsKey(stnId)) {
          last = i;
          double lat = stationData[1][i] * scaleFactor[1];
          double lon = stationData[2][i] * scaleFactor[2];
          double elev = stationData[3][i] * scaleFactor[3];
          hashStations.put(stnId, new Station(stnId, "", lat, lon, elev));
        }
        if ((cancel != null) && cancel.isCancel()) return;
      }
      if (debugCall) System.out.println(" hashStations count= " + hashStations.size() + " last = " + last);
      List stationList = new ArrayList(hashStations.keySet());
      Collections.sort(stationList);
      stations = new ArrayList();
      for (int i = 0; i < stationList.size(); i++) {
        String id = (String) stationList.get(i);
        stations.add(hashStations.get(id));
        if ((cancel != null) && cancel.isCancel()) return;
      }

    } catch (AddeException e) {
      e.printStackTrace();
      throw new IOException(e.getMessage());
    }
  }

  public ArrayList getStations() {
    return stations;
  }

  public class Station extends ucar.unidata.geoloc.StationImpl {
    String idn, id, name, type, state, country, desc;
    double lat, lon, elev;

    Station(String idn, String id, String name, String type, String state, String country, String latS,
            String lonS, String elevS) {
      if (debugParse)
        System.out.println("-" + idn + "-" + id + "-" + name + "-" + type + "-" + state + "-" + country + "-" + latS + "-" + lonS + "-" + elevS);

      this.idn = idn.trim();
      this.id = id.trim();
      this.name = name.trim();
      this.type = type;
      this.state = state.trim();
      this.country = country.trim();
      this.lat = parseDegree(latS);
      this.lon = -1.0 * parseDegree(lonS); // LOOK : degrees west ?? !!

      if (this.state.length() > 0)
        desc = this.name + ", " + this.state + ", " + this.country;
      else
        desc = this.name + ", " + this.country;

      try {
        elev = Double.parseDouble(elevS);
      } catch (NumberFormatException e) {
        e.printStackTrace();
      }
    }

    private double parseDegree(String s) {
      String degS = s.substring(0, 4);
      String minS = s.substring(5, 7);
      String secS = s.substring(8, 10);

      try {
        double deg = Double.parseDouble(degS);
        double min = Double.parseDouble(minS);
        double sec = Double.parseDouble(secS);
        if (deg < 0)
          return deg - min / 60 - sec / 3600;
        else
          return deg + min / 60 + sec / 3600;
      } catch (NumberFormatException e) {
        e.printStackTrace();
      }
      return 0.0;
    }

    Station(String id, String desc, double lat, double lon, double elev) {
      this.id = id;
      this.desc = desc;
      this.lat = lat;
      this.lon = lon;
      this.elev = elev;
    }

    public String toString() {
      return idn + " " + id + " " + name + " " + type + " " + state + " " + country + " " + lat + " " + lon + " " + elev;
    }

    public String getName() {
      return id;
    }

    public String getDescription() {
      return desc;
    }

    public String getWmoId() {
      return idn; // ??
    }

    public double getLatitude() {
      return lat;
    }

    public double getLongitude() {
      return lon;
    }

    public double getAltitude() {
      return elev;
    }

    public int compareTo(Station so) {
      return name.compareTo(so.getName());
    }
  }

  static String testName = "C:/data/station/adde/STNDB.TXT";
  //static String testName = "M:/temp/STNDB.TXT";
  static String testName2 = "http://localhost:8080/test/STNDB.TXT";

  static public void main(String[] args) throws IOException {
    long start = System.currentTimeMillis();
    StationDB stnDB = new StationDB(testName);
    long took = System.currentTimeMillis() - start;
    List list = stnDB.getStations();
    double sum = 0.0;
    for (int i = 0; i < list.size(); i++) {
      Station s = (Station) list.get(i);
      sum += s.getLatitude();
    }
    System.out.println(" read " + testName + " count=" + list.size() + " took=" + took + " msec " + " sum= " + sum);
  }
}
