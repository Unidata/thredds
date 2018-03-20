/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.misc;

import ucar.nc2.constants.CDM;
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

    int pos = location.lastIndexOf('.');
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

  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    super.open(raf, ncfile, cancelTask);

    readHDR();

    ncfile.addDimension(null, new Dimension("lat", nlats));
    ncfile.addDimension(null, new Dimension("lon", nlons));

    Variable elev = new Variable(ncfile, null, null, "elevation");
    elev.setDataType(DataType.SHORT);
    elev.setDimensions("lat lon");

    elev.addAttribute(new Attribute(CDM.UNITS, "m"));
    elev.addAttribute(new Attribute("units_desc", "meters above sea level"));
    elev.addAttribute(new Attribute(CDM.LONG_NAME, "digital elevation in meters above mean sea level"));
    elev.addAttribute(new Attribute(CDM.MISSING_VALUE, (short) -9999));
    ncfile.addVariable(null, elev);

    Variable lat = new Variable(ncfile, null, null, "lat");
    lat.setDataType(DataType.FLOAT);
    lat.setDimensions("lat");
    lat.addAttribute(new Attribute(CDM.UNITS, CDM.LAT_UNITS));
    ncfile.addVariable(null, lat);
    Array data = Array.makeArray(DataType.FLOAT, nlats, starty, -incr);
    lat.setCachedData(data, false);

    Variable lon = new Variable(ncfile, null, null, "lon");
    lon.setDataType(DataType.FLOAT);
    lon.setDimensions("lon");
    lon.addAttribute(new Attribute(CDM.UNITS, CDM.LON_UNITS));
    ncfile.addVariable(null, lon);
    Array lonData = Array.makeArray(DataType.FLOAT, nlons, startx, incr);
    lon.setCachedData(lonData, false);

    ncfile.addAttribute(null, new Attribute(CDM.CONVENTIONS, "CF-1.0"));
    ncfile.addAttribute(null, new Attribute("History", "Direct read by Netcdf-Java CDM library"));
    ncfile.addAttribute(null, new Attribute("Source", "http://eros.usgs.gov/products/elevation/gtopo30.html"));

    ncfile.finish();
  }

  private void readHDR() throws IOException {
    String location = raf.getLocation();
    int pos = location.lastIndexOf('.');
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

    return Array.factory(v2.getDataType(), wantSection.getShape(), arr);
  }

}
