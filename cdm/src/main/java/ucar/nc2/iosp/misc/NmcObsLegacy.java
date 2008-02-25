/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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
package ucar.nc2.iosp.misc;

import ucar.unidata.io.RandomAccessFile;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.*;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.AxisType;
import ucar.nc2.util.CancelTask;
import ucar.ma2.Array;
import ucar.ma2.Section;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.DataType;

import java.io.IOException;
import java.io.EOFException;
import java.util.*;

/**
 * NMC Office Note 29
 *
 * @author caron
 * @since Feb 22, 2008
 */
public class NmcObsLegacy extends AbstractIOServiceProvider {
  private RandomAccessFile raf;
  private Map<String, List<Report>> map = new HashMap<String, List<Report>>();

  private int nobs = 0, nstations = 0;
  private Calendar cal = null;
  private DateFormatter dateFormatter = new DateFormatter();
  private Date refDate;
  private String refString; // debug

  private boolean showObs = false, showSkip = false, showOverflow = false, showData = false,
      showHeader = false, showTime = false;
  private boolean readData = false, summarizeData = false, showTimes = false;
  private boolean checkType = false, checkSort = false, checkPositions = false;

  public boolean isValidFile(RandomAccessFile raf) throws IOException {
    byte[] h = raf.readBytes(60);

    short hour = Short.parseShort(new String(h, 0, 2));
    short minute = Short.parseShort(new String(h, 2, 2));
    short year = Short.parseShort(new String(h, 4, 2));
    short month = Short.parseShort(new String(h, 6, 2));
    short day = Short.parseShort(new String(h, 8, 2));

    return true;
  }

  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    this.raf = raf;
    init();
    make(ncfile);
  }

  private void make(NetcdfFile ncfile) {

    ncfile.addDimension(null, new Dimension("station", nstations));

    Structure top = new Structure(ncfile, null, null, "stationProfiles");
    top.setDimensions("station");
    ncfile.addVariable(null, top);

    try {
      Variable v = top.addMemberVariable(new Variable(ncfile, null, top, "stationName", DataType.STRING, ""));
      v.addAttribute(new Attribute("long_name", "name of station"));

      v = top.addMemberVariable(new Variable(ncfile, null, top, "lat", DataType.FLOAT, ""));
      v.addAttribute(new Attribute("units", "degrees_north"));
      v.addAttribute(new Attribute("long_name", "geographic latitude"));
      v.addAttribute(new Attribute("accuracy", "degree/100"));
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));

      v = top.addMemberVariable(new Variable(ncfile, null, top, "lon", DataType.FLOAT, ""));
      v.addAttribute(new Attribute("units", "degrees_east"));
      v.addAttribute(new Attribute("long_name", "geographic longitude"));
      v.addAttribute(new Attribute("accuracy", "degree/100"));
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));

      v = top.addMemberVariable(new Variable(ncfile, null, top, "elev", DataType.FLOAT, ""));
      v.addAttribute(new Attribute("units", "meters"));
      v.addAttribute(new Attribute("long_name", "station elevation above MSL"));
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Height.toString()));

      Sequence obs = new Sequence(ncfile, null, top, "report");
      top.addMemberVariable(obs);

      v = obs.addMemberVariable(new Variable(ncfile, null, top, "time", DataType.INT, ""));
      v.addAttribute(new Attribute("units", "secs since 1970-01-01 00:00"));
      v.addAttribute(new Attribute("long_name", "observation time"));
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));

      v = obs.addMemberVariable(new Variable(ncfile, null, top, "reportType", DataType.SHORT, ""));
      v.addAttribute(new Attribute("long_name", "report type from Table R.1"));

      // only for ON29
      v = obs.addMemberVariable(new Variable(ncfile, null, top, "instType", DataType.SHORT, ""));
      v.addAttribute(new Attribute("long_name", "instrument type from Table R.2"));

      v = obs.addMemberVariable(new Variable(ncfile, null, top, "reserved", DataType.BYTE, ""));
      v.setDimensionsAnonymous(new int[]{7});
      v.addAttribute(new Attribute("long_name", "reserved characters"));

      Sequence mandl = new Sequence(ncfile, null, top, "mandatoryLevels");
      obs.addMemberVariable(mandl);

      v = mandl.addMemberVariable(new Variable(ncfile, null, top, "pressure", DataType.FLOAT, ""));
      v.addAttribute(new Attribute("units", "mbars"));
      v.addAttribute(new Attribute("long_name", "pressure level"));
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Pressure.toString()));

      v = mandl.addMemberVariable(new Variable(ncfile, null, top, "geopotential", DataType.FLOAT, ""));
      v.addAttribute(new Attribute("units", "meter"));
      v.addAttribute(new Attribute("long_name", "geopotential"));

      v = mandl.addMemberVariable(new Variable(ncfile, null, top, "temperature", DataType.FLOAT, ""));
      v.addAttribute(new Attribute("units", "celsius"));
      v.addAttribute(new Attribute("long_name", "temperature"));
      v.addAttribute(new Attribute("accuracy", "celsius/10"));

      v = mandl.addMemberVariable(new Variable(ncfile, null, top, "dewpoint", DataType.FLOAT, ""));
      v.addAttribute(new Attribute("units", "celsius"));
      v.addAttribute(new Attribute("long_name", "dewpoint depression"));
      v.addAttribute(new Attribute("accuracy", "celsius/10"));

      v = mandl.addMemberVariable(new Variable(ncfile, null, top, "windDir", DataType.SHORT, ""));
      v.addAttribute(new Attribute("units", "degrees"));
      v.addAttribute(new Attribute("long_name", "wind direction"));

      v = mandl.addMemberVariable(new Variable(ncfile, null, top, "windSpeed", DataType.SHORT, ""));
      v.addAttribute(new Attribute("units", "knots"));
      v.addAttribute(new Attribute("long_name", "wind speed"));

    } catch (InvalidRangeException e) {
      e.printStackTrace();
    }
  }

  public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public void close() throws IOException {
    raf.close();
  }

  private void init() throws IOException {
    int badPos = 0;
    int badType = 0;
    short firstType = -1;

    readHeader(raf);

    Report firstReport = null;
    while (true) {
      Report obs = new Report();
      if (!obs.readId(raf)) break;
      if (firstReport == null) {
        firstReport = obs;
        firstType = firstReport.reportType;
      }

      if (checkType && (obs.reportType != firstType)) {
        System.out.println(obs.stationId + " type: " + obs.reportType + " not " + firstType);
        badType++;
      }

      List<Report> reports = map.get(obs.stationId);
      if (reports == null) {
        reports = new ArrayList<Report>();
        map.put(obs.stationId, reports);

      } else if (checkPositions) {
        Report first = reports.get(0);
        if (first.lat != obs.lat) {
          System.out.println(obs.stationId + " lat: " + first.lat + " !=" + obs.lat);
          badPos++;
        }
        if (first.lon != obs.lon)
          System.out.println(obs.stationId + " lon: " + first.lon + " !=" + obs.lon);
        if (first.elevMeters != obs.elevMeters)
          System.out.println(obs.stationId + " elev: " + first.elevMeters + " !=" + obs.elevMeters);
      }

      reports.add(obs);
    }

    if (checkPositions)
      System.out.println("\nnon matching lats= " + badPos);
    if (checkType)
      System.out.println("\nnon matching reportTypes= " + badType);

    //System.out.println(firstReport);
    //firstReport.show();
    //firstReport.readData();

    Set<String> keys = map.keySet();
    if (showTimes || readData || checkSort) {
      for (String key : keys) {
        List<Report> reports = map.get(key);
        if (showTimes) System.out.print("Station " + key + ": ");
        if (summarizeData) System.out.println("Station " + key + " :");
        Date last = null;
        for (Report r : reports) {
          if ((last != null) && last.after(r.date))
            System.out.println("***BAD " + key + " last=" + dateFormatter.toDateTimeStringISO(last) + " next =" + dateFormatter.toDateTimeStringISO(r.date));
          last = r.date;

          if (showTimes) System.out.print(dateFormatter.toDateTimeStringISO(r.date) + " ");
          if (readData) r.readData();
          if (summarizeData) {
            System.out.print("  " + r.obsTime + ": (");
            for (Category cat : r.cats)
              System.out.print(cat.code + "/" + cat.nlevels + " ");
            System.out.println(")");
          }

        }
        if (showTimes) System.out.println();
      }
    }
    nstations = keys.size();

    System.out.println("\nnobs= " + nobs + " nstations= " + nstations);
  }

  private class Report {
    double lat, lon, elevMeters;
    String stationId, reserved;
    short reportType, instType, obsTime;
    int reportLen;
    long filePos;
    Date date;
    String rString; // refString

    List<Category> cats;

    boolean readId(RandomAccessFile raf) throws IOException {

      filePos = raf.getFilePointer();
      byte[] reportId = raf.readBytes(40);
      String latS = new String(reportId, 0, 5);

      if (latS.equals("END R")) {
        raf.skipBytes(-40);
        endRecord(raf);

        filePos = raf.getFilePointer();
        reportId = raf.readBytes(40);
        latS = new String(reportId, 0, 5);
      }
      if (latS.equals("ENDOF")) {
        raf.skipBytes(-40);
        if (!endFile(raf)) return false;

        filePos = raf.getFilePointer();
        reportId = raf.readBytes(40);
        latS = new String(reportId, 0, 5);
      }

      //System.out.println("ReportId start at " + start);
      try {
        lat = .01 * Double.parseDouble(latS);
        lon = 360.0 - .01 * Double.parseDouble(new String(reportId, 5, 5));

        stationId = new String(reportId, 10, 6);
        obsTime = Short.parseShort(new String(reportId, 16, 4));
        reserved = new String(reportId, 20, 7);
        reportType = Short.parseShort(new String(reportId, 27, 3));
        elevMeters = Double.parseDouble(new String(reportId, 30, 5));
        instType = Short.parseShort(new String(reportId, 35, 2));
        reportLen = 10 * Integer.parseInt(new String(reportId, 37, 3));

        cal.setTime(refDate);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        if (obsTime / 100 > hour + 2) // if greater than 2 hours from reference time
          cal.add(Calendar.DAY_OF_MONTH, -1); // subtract a day LOOK
        cal.set(Calendar.HOUR_OF_DAY, obsTime / 100);
        cal.set(Calendar.MINUTE, 6 * (obsTime % 100));
        date = cal.getTime();
        rString = refString; // temp debugg

        if (showObs) System.out.println(this);
        else if (showTime) System.out.print("  time=" + obsTime + " date= " + dateFormatter.toDateTimeString(date));

        nobs++;
        raf.skipBytes(reportLen - 40);
        return reportLen < 30000;

      } catch (Exception e) {
        System.out.println("BAD reportId=" + new String(reportId));
        System.out.println("ReportId start at " + filePos);
        e.printStackTrace();
        System.exit(1);
        return false;
      }
    }

    public String toString() {
      return "Report " + " stationId=" + stationId + " lat=" + lat + " lon=" + lon +
          " obsTime=" + obsTime + " date= " + dateFormatter.toDateTimeStringISO(date) +
          " reportType=" + reportType + " elevMeters=" + elevMeters + " instType=" + instType + " reserved=" + reserved +
          " start=" + filePos + " reportLen=" + reportLen;
    }

    void readData() throws IOException {
      cats = new ArrayList<Category>();

      raf.seek(filePos + 40);
      byte[] b = raf.readBytes(reportLen - 40);
      if (showData) System.out.println("\n" + new String(b));
      if (showData) System.out.println(this);

      int offset = 0;
      while (true) {
        Category cat = new Category();
        offset = cat.read(b, offset);
        cats.add(cat);
        if (cat.next >= reportLen / 10) break;
      }
    }

    void show(RandomAccessFile raf) throws IOException {
      raf.seek(filePos);
      byte[] b = raf.readBytes(40);
      System.out.println(new String(b));

    }
  }

  private class Category {
    int code, next, nlevels, nbytes;

    int read(byte[] b, int offset) throws IOException {

      code = Integer.parseInt(new String(b, offset, 2));
      next = Integer.parseInt(new String(b, offset + 2, 3));
      nlevels = Integer.parseInt(new String(b, offset + 5, 2));
      nbytes = readIntWithOverflow(b, offset + 7, 3);
      if (showData) System.out.println("\n" + this);

      offset += 10;

      if (code == 1) {
        if (showData) System.out.println(catNames[1] + ":");
        Cat01[] c1 = new Cat01[nlevels];
        for (int i = 0; i < nlevels; i++) {
          c1[i] = new Cat01(b, offset, i);
          if (showData) System.out.println(" " + i + ": " + c1[i]);
          offset += 22;
        }
      } else if (code == 2) {
        if (showData) System.out.println(catNames[2] + ":");
        Cat02[] c2 = new Cat02[nlevels];
        for (int i = 0; i < nlevels; i++) {
          c2[i] = new Cat02(b, offset);
          if (showData) System.out.println(" " + i + ": " + c2[i]);
          offset += 15;
        }
      } else if (code == 3) {
        if (showData) System.out.println(catNames[3] + ":");
        Cat03[] c3 = new Cat03[nlevels];
        for (int i = 0; i < nlevels; i++) {
          c3[i] = new Cat03(b, offset);
          if (showData) System.out.println(" " + i + ": " + c3[i]);
          offset += 13;
        }
      } else if (code == 4) {
        if (showData) System.out.println(catNames[4] + ":");
        Cat04[] c4 = new Cat04[nlevels];
        for (int i = 0; i < nlevels; i++) {
          c4[i] = new Cat04(b, offset);
          if (showData) System.out.println(" " + i + ": " + c4[i]);
          offset += 13;
        }
      } else if (code == 5) {
        if (showData) System.out.println(catNames[5] + ":");
        Cat05[] c5 = new Cat05[nlevels];
        for (int i = 0; i < nlevels; i++) {
          c5[i] = new Cat05(b, offset);
          if (showData) System.out.println(" " + i + ": " + c5[i]);
          offset += 22;
        }
      } else if (code == 7) {
        if (showData) System.out.println(catNames[7] + ":");
        Cat07[] c7 = new Cat07[nlevels];
        for (int i = 0; i < nlevels; i++) {
          c7[i] = new Cat07(b, offset);
          if (showData) System.out.println(" " + i + ": " + c7[i]);
          offset += 10;
        }
      } else if (code == 8) {
        if (showData) System.out.println(catNames[8] + ":");
        Cat08[] c8 = new Cat08[nlevels];
        for (int i = 0; i < nlevels; i++) {
          c8[i] = new Cat08(b, offset);
          if (showData) System.out.println(" " + i + ": " + c8[i]);
          offset += 10;
        }
      } else if (code == 51) {
        if (showData) System.out.println(catNames[10] + ":");
        Cat51[] c51 = new Cat51[nlevels];
        for (int i = 0; i < nlevels; i++) {
          c51[i] = new Cat51(b, offset);
          if (showData) System.out.println(" " + i + ": " + c51[i]);
          offset += 60;
        }
      } else if (code == 52) {
        if (showData) System.out.println(catNames[10] + ":");
        Cat52[] c52 = new Cat52[nlevels];
        for (int i = 0; i < nlevels; i++) {
          c52[i] = new Cat52(b, offset);
          if (showData) System.out.println(" " + i + ": " + c52[i]);
          offset += 40;
        }
      } else {
        throw new UnsupportedOperationException("code= " + code);
      }

      // must be multiple of 10
      int skip = offset % 10;
      if (skip > 0)
        offset += (10 - skip);
      return offset;
    }

    public String toString() {
      return "Category/Group " + " code=" + code + " next= " + next + " nlevels=" + nlevels + " nbytes=" + nbytes;
    }
  }

  private int readIntWithOverflow(byte[] b, int offset, int len) {

    String s = new String(b, offset, len);
    try {
      return Integer.parseInt(s);
    } catch (Exception e) {
      if (showOverflow) System.out.println("OVERFLOW=" + s);
      return 0;
    }

  }

  private String[] catNames = new String[]{"", "Mandatory constant presure data", "Variable pressure temperature/dewpoint",
      "Variable pressure wind", "Variable height wind", "Tropopause data", "", "Cloud cover", "Additional data", "",
      "Surface Data", "Ship surface Data"};
  private static float[] mandPressureLevel = new float[]{1000, 850, 700, 500, 400, 300, 250, 200, 150, 100,
      70, 50, 30, 20, 10, 7, 5, 3, 2, 1};

  private class Cat01 {
    short windDir, windSpeed;
    float geopot, press, temp, dewp;
    byte[] quality;
    String qs;

    Cat01(byte[] b, int offset, int level) throws IOException {
      press = mandPressureLevel[level];
      geopot = Float.parseFloat(new String(b, offset, 5));
      temp = .1f * Float.parseFloat(new String(b, offset + 5, 4));
      dewp = .1f * Float.parseFloat(new String(b, offset + 9, 3));
      windDir = Short.parseShort(new String(b, offset + 12, 3));
      windSpeed = Short.parseShort(new String(b, offset + 15, 3));
      quality = new byte[4];
      System.arraycopy(b, offset + 18, quality, 0, 4);
      qs = new String(quality);
    }

    public String toString() {
      return "Cat01: press= " + press + " geopot=" + geopot + " temp= " + temp + " dewp=" + dewp + " windDir=" + windDir +
          " windSpeed=" + windSpeed + " qs=" + qs;
    }
  }

  private class Cat02 {
    float press, temp, dewp;
    byte[] quality;
    String qs;

    Cat02(byte[] b, int offset) throws IOException {
      press = .1f * Float.parseFloat(new String(b, offset, 5));
      temp = .1f * Float.parseFloat(new String(b, offset + 5, 4));
      dewp = .1f * Float.parseFloat(new String(b, offset + 9, 3));
      quality = new byte[3];
      System.arraycopy(b, offset + 12, quality, 0, 3);
      qs = new String(quality);
    }

    public String toString() {
      return "Cat02: press=" + press + " temp= " + temp + " dewp=" + dewp + " qs=" + qs;
    }
  }

  private class Cat03 {
    float press;
    short windDir, windSpeed;
    byte[] quality;
    String qs;

    Cat03(byte[] b, int offset) throws IOException {
      press = .1f * Float.parseFloat(new String(b, offset, 5));
      windDir = Short.parseShort(new String(b, offset + 5, 3));
      windSpeed = Short.parseShort(new String(b, offset + 8, 3));
      quality = new byte[2];
      System.arraycopy(b, offset + 11, quality, 0, 2);
      qs = new String(quality);
    }

    public String toString() {
      return "Cat03: press=" + press + " windDir=" + windDir + " windSpeed=" + windSpeed + " qs=" + qs;
    }
  }

  private class Cat04 {
    float geopot;
    short windDir, windSpeed;
    byte[] quality;
    String qs;

    Cat04(byte[] b, int offset) throws IOException {
      geopot = Float.parseFloat(new String(b, offset, 5));
      windDir = Short.parseShort(new String(b, offset + 5, 3));
      windSpeed = Short.parseShort(new String(b, offset + 8, 3));
      quality = new byte[2];
      System.arraycopy(b, offset + 11, quality, 0, 2);
      qs = new String(quality);
    }

    public String toString() {
      return "Cat04: geopot=" + geopot + " windDir=" + windDir + " windSpeed=" + windSpeed + " qs=" + qs;
    }
  }

  private class Cat05 {
    float press, temp, dewp;
    short windDir, windSpeed;
    byte[] quality;
    String qs;

    Cat05(byte[] b, int offset) throws IOException {
      press = .1f * Float.parseFloat(new String(b, offset, 5));
      temp = .1f * Float.parseFloat(new String(b, offset + 5, 4));
      dewp = .1f * Float.parseFloat(new String(b, offset + 9, 3));
      windDir = Short.parseShort(new String(b, offset + 12, 3));
      windSpeed = Short.parseShort(new String(b, offset + 15, 3));
      quality = new byte[4];
      System.arraycopy(b, offset + 18, quality, 0, 4);
      qs = new String(quality);
    }

    public String toString() {
      return "Cat05: press= " + press + " temp= " + temp + " dewp=" + dewp + " windDir=" + windDir +
          " windSpeed=" + windSpeed + " qs=" + qs;
    }
  }

  private class Cat07 {
    float press;
    short percentClouds;
    byte[] quality;
    String qs;

    Cat07(byte[] b, int offset) throws IOException {
      press = .1f * Float.parseFloat(new String(b, offset, 5));
      percentClouds = Short.parseShort(new String(b, offset + 5, 3));
      quality = new byte[2];
      System.arraycopy(b, offset + 8, quality, 0, 2);
      qs = new String(quality);
    }

    public String toString() {
      return "Cat07: press=" + press + " percentClouds=" + percentClouds + " qs=" + qs;
    }
  }

  private class Cat08 {
    int data;
    short table101code;
    byte[] quality;
    String qs;

    Cat08(byte[] b, int offset) throws IOException {
      data = Integer.parseInt(new String(b, offset, 5));
      table101code = Short.parseShort(new String(b, offset + 5, 3));
      quality = new byte[2];
      System.arraycopy(b, offset + 8, quality, 0, 2);
      qs = new String(quality);
    }

    public String toString() {
      return "Cat08: data=" + data + " table101code=" + table101code + " qs=" + qs;
    }
  }

  private class Cat51 {
    short windDir, windSpeed;
    float pressSeaLevel, pressStation, geopot, press, temp, dewp, maxTemp, minTemp, pressureTendency;
    byte[] quality = new byte[4];
    byte pastWeatherW2, pressureTendencyChar;
    String qs;
    byte[] horizVis = new byte[3];
    byte[] presentWeather = new byte[3];
    byte[] pastWeatherW1 = new byte[2];
    byte[] fracCloudN = new byte[2];
    byte[] fracCloudNh = new byte[2];
    byte[] cloudCl = new byte[2];
    byte[] cloudBaseHeight = new byte[2];
    byte[] cloudCm = new byte[2];
    byte[] cloudCh = new byte[2];

    Cat51(byte[] b, int offset) throws IOException {
      pressSeaLevel = Float.parseFloat(new String(b, offset, 5));
      pressStation = Float.parseFloat(new String(b, offset + 5, 5));
      windDir = Short.parseShort(new String(b, offset + 10, 3));
      windSpeed = Short.parseShort(new String(b, offset + 13, 3));
      temp = .1f * Float.parseFloat(new String(b, offset + 16, 4));
      dewp = .1f * Float.parseFloat(new String(b, offset + 20, 3));
      maxTemp = .1f * Float.parseFloat(new String(b, offset + 23, 4));
      minTemp = .1f * Float.parseFloat(new String(b, offset + 27, 4));
      System.arraycopy(b, offset + 31, quality, 0, 4);
      qs = new String(quality);

      pastWeatherW2 = b[offset + 35];
      System.arraycopy(b, offset + 36, horizVis, 0, 3);
      System.arraycopy(b, offset + 39, presentWeather, 0, 3);
      System.arraycopy(b, offset + 42, pastWeatherW1, 0, 2);
      System.arraycopy(b, offset + 44, fracCloudN, 0, 2);
      System.arraycopy(b, offset + 46, fracCloudNh, 0, 2);
      System.arraycopy(b, offset + 48, cloudCl, 0, 2);
      System.arraycopy(b, offset + 50, cloudBaseHeight, 0, 2);
      System.arraycopy(b, offset + 52, cloudCm, 0, 2);
      System.arraycopy(b, offset + 54, cloudCh, 0, 2);
      pressureTendencyChar = b[offset + 56];
      pressureTendency = .1f * Float.parseFloat(new String(b, offset + 57, 3));
    }

    public String toString() {
      return "Cat51: press= " + press + " geopot=" + geopot + " temp= " + temp + " dewp=" + dewp + " windDir=" + windDir +
          " windSpeed=" + windSpeed + " qs=" + qs + " pressureTendency=" + pressureTendency;
    }
  }

  private class Cat52 {
    short snowDepth, wavePeriod, waveHeight, waveSwellPeriod, waveSwellHeight;
    float precip6hours, precip24hours, sst, waterEquiv;
    byte precipDuration, shipCourse;
    byte[] waveDirection = new byte[2];
    byte[] special = new byte[2];
    byte[] special2 = new byte[2];
    byte[] shipSpeed = new byte[2];

    Cat52(byte[] b, int offset) throws IOException {
      precip6hours = .01f * Float.parseFloat(new String(b, offset, 4));
      snowDepth = Short.parseShort(new String(b, offset + 4, 3));
      precip24hours = .01f * Float.parseFloat(new String(b, offset + 7, 4));
      precipDuration = b[offset + 11];
      wavePeriod = Short.parseShort(new String(b, offset + 12, 2));
      waveHeight = Short.parseShort(new String(b, offset + 14, 2));
      System.arraycopy(b, offset + 16, waveDirection, 0, 2);
      waveSwellPeriod = Short.parseShort(new String(b, offset + 18, 2));
      waveSwellHeight = Short.parseShort(new String(b, offset + 20, 2));
      sst = .1f * Float.parseFloat(new String(b, offset + 22, 4));
      System.arraycopy(b, offset + 26, special, 0, 2);
      System.arraycopy(b, offset + 28, special2, 0, 2);
      shipCourse = b[offset + 30];
      System.arraycopy(b, offset + 31, shipSpeed, 0, 2);
      waterEquiv = .001f * Float.parseFloat(new String(b, offset + 33, 7));
    }

    public String toString() {
      return "Cat52: precip6hours= " + precip6hours + " precip24hours=" + precip24hours + " sst= " + sst + " waterEquiv=" + waterEquiv +
          " snowDepth=" + snowDepth + " wavePeriod=" + wavePeriod + " waveHeight=" + waveHeight +
          " waveSwellPeriod=" + waveSwellPeriod + " waveSwellHeight=" + waveSwellHeight;
    }
  }


  private void showPos(String what, long start) throws IOException {
    long rel = raf.getFilePointer() - start;
    System.out.println(what + " has pos=" + raf.getFilePointer() + " reletive pos= " + rel);
  }

  private boolean endRecord(RandomAccessFile raf) throws IOException {
    if (showSkip) System.out.print(" endRecord start at " + raf.getFilePointer());

    int skipped = 0;
    String endRecord = new String(raf.readBytes(10));
    while (endRecord.equals("END RECORD")) {
      endRecord = new String(raf.readBytes(10));
      skipped++;
    }
    if (showSkip) System.out.println(" last 10 chars= " + endRecord + " skipped= " + skipped);
    return true;
  }

  private boolean endFile(RandomAccessFile raf) throws IOException {
    if (showSkip) System.out.println(" endFile start at " + raf.getFilePointer());

    String endRecord = new String(raf.readBytes(10));
    while (endRecord.equals("ENDOF FILE")) {
      endRecord = new String(raf.readBytes(10));
    }

    try {
      int b;
      while ((b = raf.read()) != (int) 'X') ; //find where X's start
      while ((b = raf.read()) == (int) 'X') ; //skip X's till you run out
      raf.skipBytes(-1); // go back one
      readHeader(raf);
      return true;

    } catch (EOFException e) {
      return false;
    }
  }

  private void readHeader(RandomAccessFile raf) throws IOException {
    byte[] h = raf.readBytes(60);

    // 12 00 070101
    short hour = Short.parseShort(new String(h, 0, 2));
    short minute = Short.parseShort(new String(h, 2, 2));
    short year = Short.parseShort(new String(h, 4, 2));
    short month = Short.parseShort(new String(h, 6, 2));
    short day = Short.parseShort(new String(h, 8, 2));

    if (cal == null) {
      cal = Calendar.getInstance();
      cal.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    cal.clear();
    cal.set(2000 + year, month - 1, day, hour, minute);
    refDate = cal.getTime();
    refString = new String(h, 0, 10);

    if (showHeader) System.out.println("\nhead=" + new String(h) +
        " date= " + dateFormatter.toDateTimeString(refDate));

    int b, count = 0;
    while ((b = raf.read()) == (int) 'X') count++;
    char c = (char) b;
    if (showSkip) System.out.println(" b=" + b + " c=" + c + " at " + raf.getFilePointer() + " skipped= " + count);
    raf.skipBytes(-1); // go back one
  }

  static class MyNetcdfFile extends NetcdfFile {
  }

  static public void main(String args[]) throws IOException {
    String filename = "C:/data/cadis/tempting";
    //String filename = "C:/data/cadis/Y94179";
    //String filename = "C:/data/cadis/Y94132";
    NmcObsLegacy iosp = new NmcObsLegacy();
    RandomAccessFile raf = new RandomAccessFile(filename, "r");
    NetcdfFile ncfile = new MyNetcdfFile();
    ncfile.setLocation(filename);
    iosp.open(raf, ncfile, null);
    System.out.println("\n" + ncfile);
    iosp.close();
  }
}
