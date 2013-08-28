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

package ucar.nc2.iosp.misc;

import ucar.nc2.IOServiceProvider;
import ucar.nc2.*;
import ucar.nc2.dataset.conv._Coordinate;
import ucar.nc2.dataset.AxisType;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;
import ucar.ma2.*;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Date;
import java.text.ParseException;

/**
 * UspLightning1
 *
 * @author caron
 */
public class UspLightning1 implements IOServiceProvider {

/*  USPLN data format:

Each 1 minute packet sent has an ASCII header, followed by a record for
each lightning detection during the past 1 minute.

Header
The ASCII header provides information on the creation time of the one
minute packet and ending date and time of the file.

Sample Header:
USPLN-LIGHTNING,2004-10-11T20:45:02,2004-10-11T20:45:02
Description:
Name of Product: USPLN-LIGHTNING
Creation of 1 min Packet (yyyy-mm-ddThh:mm:ss): 2004-10-11T20:45:02
Ending of 1 min Packet (yyyy-mm-ddThh:mm:ss): 2004-10-11T20:45:02

NOTE: All times in UTC

Strike Record Following the header, an individual record is provided for
each lightning strike in a comma delimited format.

Sample Strike Records:
2004-10-11T20:44:02,32.6785331,-105.4344587,-96.1,1
2004-10-11T20:44:05,21.2628231,-86.9596634,53.1,1
2004-10-11T20:44:05,21.2967119,-86.9702106,50.3,1
2004-10-11T20:44:06,19.9044769,-100.7082608,43.1,1
2004-10-11T20:44:11,21.4523434,-82.5202274,-62.8,1
2004-10-11T20:44:11,21.8155306,-82.6708778,80.9,1

Description:

Strike Date/Time (yyyy-mm-ddThh:mm:ss): 2004-10-11T20:44:02

Strike Latitude (deg): 32.6785331
Strike Longitude (deg): -105.4344587
Strike Amplitude (kAmps, see note below): -96.1
Stroke Count (number of strokes per flash): 1

Note: At the present time USPLN data are only provided in stroke format,
so the stroke count will always be 1.

Notes about Decoding Strike Amplitude
The amplitude field is utilized to indicate the amplitude of strokes and
polarity of strokes for all Cloud-to- Ground Strokes.

For other types of detections this field is utilized to provide
information on the type of stroke detected.

The amplitude number for Cloud-to-Ground strokes provides the amplitude
of the stroke and the sign (+/-) provides the polarity of the stroke.

An amplitude of 0 indicates USPLN Cloud Flash Detections rather than
Cloud-to-Ground detections.

Cloud flash detections include cloud-to-cloud, cloud-to-air, and
intra-cloud flashes.

An amplitude of -999 or 999 indicates a valid cloud-to-ground stroke
detection in which an amplitude was not able to be determined. Typically
these are long-range detections.
*/

  private static final String MAGIC = "USPLN-LIGHTNING";

  public boolean isValidFile(RandomAccessFile raf) throws IOException {
    raf.seek(0);
    int n = MAGIC.length();
    byte[] b = new byte[n];
    raf.read(b);
    String got = new String(b);
    return got.equals(MAGIC);
  }

  private  ArrayInt.D1 dateArray;
  private  ArrayDouble.D1 latArray;
  private  ArrayDouble.D1 lonArray;
  private  ArrayDouble.D1 ampArray;
  private  ArrayInt.D1 nstrokesArray;

  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    int n;

    try {
      n = readAllData(raf);
    } catch (ParseException e) {
      e.printStackTrace();
      throw new IOException("bad data");
    }
    raf.close();

    Dimension recordDim = new Dimension("record", n, true);
    ncfile.addDimension( null, recordDim);

    Variable date = new Variable(ncfile, null, null, "date");
    date.setDimensions("record");
    date.setDataType(DataType.INT);
    String timeUnit = "seconds since 1970-01-01 00:00:00";
    date.addAttribute( new Attribute("long_name", "date of strike"));
    date.addAttribute( new Attribute("units", timeUnit));
    date.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));
    date.setCachedData(dateArray, false);
    ncfile.addVariable( null, date);

    Variable lat = new Variable(ncfile, null, null, "lat");
    lat.setDimensions("record");
    lat.setDataType(DataType.DOUBLE);
    lat.addAttribute( new Attribute("long_name", "latitude"));
    lat.addAttribute( new Attribute("units", "degrees_north"));
    lat.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
    lat.setCachedData(latArray, false);
    ncfile.addVariable( null, lat);

    Variable lon = new Variable(ncfile, null, null, "lon");
    lon.setDimensions("record");
    lon.setDataType(DataType.DOUBLE);
    lon.addAttribute( new Attribute("long_name", "longitude"));
    lon.addAttribute( new Attribute("units", "degrees_east"));
    lon.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));
    lon.setCachedData(lonArray, false);
    ncfile.addVariable( null, lon);

    Variable amp = new Variable(ncfile, null, null, "strikeAmplitude");
    amp.setDimensions("record");
    amp.setDataType(DataType.DOUBLE);
    amp.addAttribute( new Attribute("long_name", "amplitude of strike"));
    amp.addAttribute( new Attribute("units", "kAmps"));
    amp.addAttribute( new Attribute("missing_value", new Double(999)));
    amp.setCachedData(ampArray, false);
    ncfile.addVariable( null, amp);

    Variable nstrokes = new Variable(ncfile, null, null, "strokeCount");
    nstrokes.setDimensions("record");
    nstrokes.setDataType(DataType.INT);
    nstrokes.addAttribute( new Attribute("long_name", "number of strokes per flash"));
    nstrokes.addAttribute( new Attribute("units", ""));
    nstrokes.setCachedData(nstrokesArray, false);
    ncfile.addVariable( null, nstrokes);

    ncfile.addAttribute(null, new Attribute("title", "USPN Lightning Data"));
    ncfile.addAttribute(null, new Attribute("history","Read directly by Netcdf Java IOSP"));

    ncfile.addAttribute(null, new Attribute("Conventions","Unidata Observation Dataset v1.0"));
    ncfile.addAttribute(null, new Attribute("cdm_data_type","Point"));
    ncfile.addAttribute(null, new Attribute("observationDimension","record"));

    MAMath.MinMax mm = MAMath.getMinMax(dateArray);
    ncfile.addAttribute(null, new Attribute("time_coverage_start", ((int)mm.min) +" "+timeUnit));
    ncfile.addAttribute(null, new Attribute("time_coverage_end", ((int)mm.max) +" "+timeUnit));

    mm = MAMath.getMinMax(latArray);
    ncfile.addAttribute(null, new Attribute("geospatial_lat_min", new Double(mm.min)));
    ncfile.addAttribute(null, new Attribute("geospatial_lat_max", new Double(mm.max)));

    mm = MAMath.getMinMax(lonArray);
    ncfile.addAttribute(null, new Attribute("geospatial_lon_min", new Double(mm.min)));
    ncfile.addAttribute(null, new Attribute("geospatial_lon_max", new Double(mm.max)));

    ncfile.finish();
  }

  // 2006-10-23T17:59:39,18.415434,-93.480526,-26.8,1
  int readAllData(RandomAccessFile raf) throws IOException, NumberFormatException, ParseException {
    ArrayList records = new ArrayList();

    java.text.SimpleDateFormat isoDateTimeFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    isoDateTimeFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));

    //java.io.RandomAccessFile jraf = raf.getRandomAccessFile();

    raf.seek(0);
    int count = 0;
    while (true) {
      String line = raf.readLine();
      if (line == null) break;
      if (line.startsWith(MAGIC)) continue;

      StringTokenizer stoker = new StringTokenizer( line, ",\r\n");
      while (stoker.hasMoreTokens()) {
        Date d = isoDateTimeFormat.parse(stoker.nextToken());
        double lat = Double.parseDouble(stoker.nextToken());
        double lon = Double.parseDouble(stoker.nextToken());
        double amp = Double.parseDouble(stoker.nextToken());
        String tok = stoker.nextToken();
        int nstrikes = Integer.parseInt(tok);

        Strike s = new Strike(d, lat, lon, amp, nstrikes);
        records.add( s);
        if (count < 10)
          System.out.println(count+" "+ isoDateTimeFormat.format(d)+" "+s);
      }

      count++;
    }

    System.out.println("processed "+count+" records");

    int n = records.size();
    int[] shape = new int[] {n};
    dateArray = (ArrayInt.D1) Array.factory(DataType.INT, shape);
    latArray = (ArrayDouble.D1) Array.factory(DataType.DOUBLE, shape);
    lonArray = (ArrayDouble.D1) Array.factory(DataType.DOUBLE, shape);
    ampArray = (ArrayDouble.D1) Array.factory(DataType.DOUBLE, shape);
    nstrokesArray = (ArrayInt.D1) Array.factory(DataType.INT, shape);

    for (int i = 0; i < records.size(); i++) {
      Strike strike = (Strike) records.get(i);
      dateArray.set(i, strike.d);
      latArray.set(i, strike.lat);
      lonArray.set(i, strike.lon);
      ampArray.set(i, strike.amp);
      nstrokesArray.set(i, strike.n);
    }

    return n;
  }

  private class Strike {
    int d;
    double lat, lon, amp;
    int n;

    Strike( Date d, double lat, double lon, double amp, int n ) {
      this.d = (int) (d.getTime() / 1000);
      this.lat = lat;
      this.lon = lon;
      this.amp = amp;
      this.n = n;
    }

    public String toString() {
      return lat+" "+lon+" "+amp+" "+n;
    }

  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////

  public Array readData(Variable v2, List section) throws IOException, InvalidRangeException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public Array readNestedData(Variable v2, List section) throws IOException, InvalidRangeException {
    return null;
  }

  public void close() throws IOException {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public boolean syncExtend() throws IOException {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public boolean sync() throws IOException {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public void setSpecial(Object special) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public String toStringDebug(Object o) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public String getDetailInfo() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public static void main(String args[]) throws IOException, IllegalAccessException, InstantiationException {
    NetcdfFile.registerIOProvider(UspLightning1.class);
    NetcdfFile ncfile = NetcdfFile.open("R:/testdata/lightning/uspln/uspln_20061023.18");
    System.out.println("ncfile = \n"+ncfile);
  }

}
