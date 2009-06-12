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

import ucar.nc2.iosp.*;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.IO;
import ucar.unidata.io.RandomAccessFile;
import ucar.ma2.*;

import java.io.IOException;
import java.io.File;
import java.util.StringTokenizer;

/**
 * GTOPO 30 sec elevation data from USGS
 * http://edc.usgs.gov/products/elevation/gtopo30/gtopo30.html
 *
 * @author caron
 */
public class GtopoIosp extends AbstractIOServiceProvider {

  public boolean isValidFile(RandomAccessFile raf) throws IOException {
    String location = raf.getLocation();
    if (!location.endsWith(".DEM")) return false;

    int pos = location.lastIndexOf(".");
    String stub = location.substring(0, pos);
    File hdrFile = new File(stub + ".HDR");
    return hdrFile.exists();
  }

  public String getFileTypeId() {
    return "GTOPO";
  }

  public String getFileTypeDescription() {
    return "USGS GTOPO digital elevation model";
  }


  private int nlats = 6000;
  private int nlons = 4800;
  private float incr = .008333333333333333f;
  private float startx, starty;

  private RandomAccessFile raf;

  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    this.raf = raf;

    readHDR();

    ncfile.addDimension(null, new Dimension("lat", nlats));
    ncfile.addDimension(null, new Dimension("lon", nlons));

    Variable elev = new Variable(ncfile, null, null, "elevation");
    elev.setDataType(DataType.SHORT);
    elev.setDimensions("lat lon");

    elev.addAttribute(new Attribute("units", "m"));
    elev.addAttribute(new Attribute("units_desc", "meters above sea level"));
    elev.addAttribute(new Attribute("long_name", "digital elevation in meters above mean sea level"));
    elev.addAttribute(new Attribute("missing_value", (short) -9999));
    ncfile.addVariable(null, elev);

    Variable lat = new Variable(ncfile, null, null, "lat");
    lat.setDataType(DataType.FLOAT);
    lat.setDimensions("lat");
    lat.addAttribute(new Attribute("units", "degrees_north"));
    ncfile.addVariable(null, lat);
    Array data = Array.makeArray(DataType.FLOAT, nlats, starty, -incr);
    lat.setCachedData(data, false);

    Variable lon = new Variable(ncfile, null, null, "lon");
    lon.setDataType(DataType.FLOAT);
    lon.setDimensions("lon");
    lon.addAttribute(new Attribute("units", "degrees_east"));
    ncfile.addVariable(null, lon);
    Array lonData = Array.makeArray(DataType.FLOAT, nlons, startx, incr);
    lon.setCachedData(lonData, false);

    ncfile.addAttribute(null, new Attribute("Conventions", "CF-1.0"));
    ncfile.addAttribute(null, new Attribute("History", "Direct read by Netcdf-Java CDM library"));
    ncfile.addAttribute(null, new Attribute("Source", "http://eros.usgs.gov/products/elevation/gtopo30.html"));

    ncfile.finish();
  }

  private void readHDR() throws IOException {
    String location = raf.getLocation();
    int pos = location.lastIndexOf(".");
    String HDRname = location.substring(0, pos) + ".HDR";
    String HDRcontents = IO.readFile(HDRname);
    StringTokenizer stoke = new StringTokenizer(HDRcontents);
    while (stoke.hasMoreTokens()) {
      String key = stoke.nextToken();
      if (key.equals("ULXMAP"))
        startx = Float.parseFloat(stoke.nextToken());
      else if (key.equals("ULYMAP"))
        starty = Float.parseFloat(stoke.nextToken());
      else
        stoke.nextToken();
    }
  }

  public Array readData(Variable v2, Section wantSection) throws IOException, InvalidRangeException {
    // raf.seek(0);
    raf.order(RandomAccessFile.BIG_ENDIAN);

    int size = (int) wantSection.computeSize();
    short[] arr = new short[size];

    LayoutRegular indexer = new LayoutRegular(0, v2.getElementSize(), v2.getShape(), wantSection);
    while (indexer.hasNext()) {
      Layout.Chunk chunk = indexer.next();
      raf.seek(chunk.getSrcPos());
      raf.readShort(arr, (int) chunk.getDestElem(), chunk.getNelems()); // copy into primitive array
    }

    return Array.factory(v2.getDataType().getPrimitiveClassType(), wantSection.getShape(), arr);
  }

  public void close() throws IOException {
    raf.close();
  }
}
