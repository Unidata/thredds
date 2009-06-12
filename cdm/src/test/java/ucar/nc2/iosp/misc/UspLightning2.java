// $Id: $
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

package ucar.nc2.iosp.misc;

import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.*;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;
import ucar.ma2.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Date;
import java.text.ParseException;

/**
 * UspLightning2
 *
 * @author caron
 */
public class UspLightning2  extends AbstractIOServiceProvider {

  private static final String MAGIC = "USPLN-LIGHTNING";

  public boolean isValidFile(RandomAccessFile raf) throws IOException {
    raf.seek(0);
    int n = MAGIC.length();
    byte[] b = new byte[n];
    raf.read(b);
    String got = new String(b);
    return got.equals(MAGIC);
  }

  public String getFileTypeId() {
    return "USPLN/example2";
  }

  public String getFileTypeDescription() {
    return "USPLN/example2";
  }

  private long[] offsets;
  private double lat_min, lat_max;
  private double lon_min, lon_max;
  private int time_min, time_max;

  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    this.raf = raf;

    int n;
    try {
      n = readAllData(raf);
    } catch (ParseException e) {
      e.printStackTrace();
      throw new IOException("bad data");
    }

    Dimension recordDim = new Dimension("record", n);
    ncfile.addDimension( null, recordDim);

    Variable date = new Variable(ncfile, null, null, "date");
    date.setDimensions("record");
    date.setDataType(DataType.INT);
    String timeUnit = "seconds since 1970-01-01 00:00:00";
    date.addAttribute( new Attribute("long_name", "date of strike"));
    date.addAttribute( new Attribute("units", timeUnit));
    date.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));
    ncfile.addVariable( null, date);
    date.setSPobject( new IospData(0));

    Variable lat = new Variable(ncfile, null, null, "lat");
    lat.setDimensions("record");
    lat.setDataType(DataType.DOUBLE);
    lat.addAttribute( new Attribute("long_name", "latitude"));
    lat.addAttribute( new Attribute("units", "degrees_north"));
    lat.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
    ncfile.addVariable( null, lat);
    lat.setSPobject( new IospData(1));

    Variable lon = new Variable(ncfile, null, null, "lon");
    lon.setDimensions("record");
    lon.setDataType(DataType.DOUBLE);
    lon.addAttribute( new Attribute("long_name", "longitude"));
    lon.addAttribute( new Attribute("units", "degrees_east"));
    lon.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));
    ncfile.addVariable( null, lon);
    lon.setSPobject( new IospData(2));

    Variable amp = new Variable(ncfile, null, null, "strikeAmplitude");
    amp.setDimensions("record");
    amp.setDataType(DataType.DOUBLE);
    amp.addAttribute( new Attribute("long_name", "amplitude of strike"));
    amp.addAttribute( new Attribute("units", "kAmps"));
    amp.addAttribute( new Attribute("missing_value", new Double(999)));
    ncfile.addVariable( null, amp);
    amp.setSPobject( new IospData(3));

    Variable nstrokes = new Variable(ncfile, null, null, "strokeCount");
    nstrokes.setDimensions("record");
    nstrokes.setDataType(DataType.INT);
    nstrokes.addAttribute( new Attribute("long_name", "number of strokes per flash"));
    nstrokes.addAttribute( new Attribute("units", ""));
    ncfile.addVariable( null, nstrokes);
    nstrokes.setSPobject( new IospData(4));

    ncfile.addAttribute(null, new Attribute("title", "USPN Lightning Data"));
    ncfile.addAttribute(null, new Attribute("history","Read directly by Netcdf Java IOSP"));

    ncfile.addAttribute(null, new Attribute("Conventions","Unidata Observation Dataset v1.0"));
    ncfile.addAttribute(null, new Attribute("cdm_data_type", FeatureType.POINT.toString()));
    ncfile.addAttribute(null, new Attribute("observationDimension","record"));

    ncfile.addAttribute(null, new Attribute("time_coverage_start", time_min +" "+timeUnit));
    ncfile.addAttribute(null, new Attribute("time_coverage_end", time_max +" "+timeUnit));

    ncfile.addAttribute(null, new Attribute("geospatial_lat_min", new Double(lat_min)));
    ncfile.addAttribute(null, new Attribute("geospatial_lat_max", new Double(lat_max)));

    ncfile.addAttribute(null, new Attribute("geospatial_lon_min", new Double(lon_min)));
    ncfile.addAttribute(null, new Attribute("geospatial_lon_max", new Double(lon_max)));

    ncfile.finish();
  }

  // 2006-10-23T17:59:39,18.415434,-93.480526,-26.8,1
  int readAllData(RandomAccessFile raf) throws IOException, NumberFormatException, ParseException {
    ArrayList offsetList = new ArrayList();

    java.text.SimpleDateFormat isoDateTimeFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    isoDateTimeFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));

    lat_min = 1000.0;
    lat_max = -1000.0;
    lon_min = 1000.0;
    lon_max = -1000.0;
    time_min = Integer.MAX_VALUE;
    time_max = 0;

    raf.seek(0);
    int count = 0;
    while (true) {
      long offset = raf.getFilePointer();
      String line = raf.readLine();
      if (line == null) break;
      if (line.startsWith(MAGIC)) continue;

      StringTokenizer stoker = new StringTokenizer( line, ",\r\n");
      while (stoker.hasMoreTokens()) {
        Date d = isoDateTimeFormat.parse(stoker.nextToken());
        double lat = Double.parseDouble(stoker.nextToken());
        double lon = Double.parseDouble(stoker.nextToken());
        double amp = Double.parseDouble(stoker.nextToken());
        int nstrikes = Integer.parseInt(stoker.nextToken());

        Strike s = new Strike(d, lat, lon, amp, nstrikes);
        lat_min = Math.min( lat_min, s.lat);
        lat_max = Math.max( lat_max, s.lat);
        lon_min = Math.min( lon_min, s.lon);
        lon_max = Math.max( lon_max, s.lon);
        time_min = Math.min( time_min, s.d);
        time_max = Math.max( time_max, s.d);
      }

      offsetList.add(new Long(offset));
      count++;
    }

    offsets = new long[count];
    for (int i = 0; i < offsetList.size(); i++) {
      Long off = (Long) offsetList.get(i);
      offsets[i] = off.longValue();
    }

    System.out.println("processed "+count+" records");
    return count;
  }

  private class Strike {
    int d;
    double lat, lon, amp;
    int n;

    Strike( long offset, java.text.SimpleDateFormat isoDateTimeFormat) throws IOException, ParseException {
      raf.seek(offset);
      String line = raf.readLine();
      if ((line == null) || line.startsWith(MAGIC))
        throw new IllegalStateException();

      StringTokenizer stoker = new StringTokenizer( line, ",\r\n");
      Date d = isoDateTimeFormat.parse(stoker.nextToken());
      makeDate(d);
      lat = Double.parseDouble(stoker.nextToken());
      lon = Double.parseDouble(stoker.nextToken());
      amp = Double.parseDouble(stoker.nextToken());
      n = Integer.parseInt(stoker.nextToken());
    }

    Strike( Date d, double lat, double lon, double amp, int n ) {
      makeDate(d);
      this.lat = lat;
      this.lon = lon;
      this.amp = amp;
      this.n = n;
    }

    void makeDate( Date date) {
      this.d = (int) (date.getTime() / 1000);
    }

    public String toString() {
      return lat+" "+lon+" "+amp+" "+n;
    }

  }

  private class IospData {
    int varno;
    IospData( int varno) {
      this.varno = varno;
    }
  }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

  public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {
    IospData iospd = (IospData) v2.getSPobject();

    java.text.SimpleDateFormat isoDateTimeFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    isoDateTimeFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));

    int[] sectionShape = section.getShape();
    Array data = Array.factory(v2.getDataType(), sectionShape);
    Index ima = data.getIndex();

    int count = 0;
    Range r = section.getRange(0);
    Range.Iterator riter = r.getIterator();
    while (riter.hasNext()) {
      int index = riter.next();
      long offset = offsets[index];
      Strike s;
      try {
        s = new Strike(offset, isoDateTimeFormat);
      } catch (ParseException e) {
        throw new IOException(e.getMessage());
      }

      ima.set(count);
      switch (iospd.varno) {
        case 0: data.setInt(ima, s.d);
          break;
        case 1: data.setDouble(ima, s.lat);
          break;
        case 2: data.setDouble(ima, s.lon);
          break;
        case 3: data.setDouble(ima, s.amp);
          break;
        case 4: data.setInt(ima, s.n);
          break;
      }

      count++;
    }

    return data;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////

  public static void main(String args[]) throws IOException, IllegalAccessException, InstantiationException, InvalidRangeException {
    NetcdfFile.registerIOProvider(UspLightning2.class);
    NetcdfFile ncfile = NetcdfFile.open( TestAll.testdataDir + "lightning/uspln/uspln_20061023.18");
    System.out.println("ncfile = \n"+ncfile);

    Variable v = ncfile.findVariable("lat");
    Array data = v.read();
    assert data.getSize() == v.getSize() : data.getSize();
    data = v.read("0:99");
    assert data.getSize() == 100 : data.getSize();
    data = v.read("0:99:3");
    assert data.getSize() == 34 : data.getSize();
  }

}
