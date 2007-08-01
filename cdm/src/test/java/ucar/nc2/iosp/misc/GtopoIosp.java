/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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

import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.iosp.RegularLayout;
import ucar.nc2.iosp.Indexer;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;
import ucar.ma2.Array;
import ucar.ma2.Section;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.DataType;

import java.io.IOException;

/**
 * Class Description.
 *
 * @author caron
 */
public class GtopoIosp extends AbstractIOServiceProvider {
  public boolean isValidFile(RandomAccessFile raf) throws IOException {
    return raf.getLocation().endsWith(".DEM");
  }


  int nlats = 6000;
  int nlons = 4800;
  float incr = .008333333333333333f;

  private RandomAccessFile raf;

  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    this.raf = raf;

    ncfile.addDimension(null, new Dimension("lat", nlats, true));
    ncfile.addDimension(null, new Dimension("lon", nlons, true));

    Variable elev = new Variable(ncfile, null, null, "elevation");
    elev.setDataType(DataType.SHORT);
    elev.setDimensions("lat lon");

    elev.addAttribute(new Attribute("units", "m"));
    elev.addAttribute(new Attribute("long_name", "digital elevation in meters above mean sea level"));
    elev.addAttribute(new Attribute("missing_value", (short) -9999));
    ncfile.addVariable(null, elev);

    Variable lat = new Variable(ncfile, null, null, "lat");
    lat.setDataType(DataType.FLOAT);
    lat.setDimensions("lat");
    lat.addAttribute(new Attribute("units", "degrees_north"));
    ncfile.addVariable(null, lat);
    Array data = NetcdfDataset.makeArray(DataType.FLOAT, nlats, 90.0, -incr);
    lat.setCachedData(data, false);

    Variable lon = new Variable(ncfile, null, null, "lon");
    lon.setDataType(DataType.FLOAT);
    lon.setDimensions("lon");
    lon.addAttribute(new Attribute("units", "degrees_east"));
    ncfile.addVariable(null, lon);
    Array lonData = NetcdfDataset.makeArray(DataType.FLOAT, nlons, -140.0, incr);
    lon.setCachedData(lonData, false);

    ncfile.addAttribute(null, new Attribute("Conventions", "CF-1.0"));
    ncfile.addAttribute(null, new Attribute("History", "Direct read by Netcdf-Java CDM library"));
    ncfile.addAttribute(null, new Attribute("Source", "http://eros.usgs.gov/products/elevation/gtopo30.html"));

    ncfile.finish();
  }

  public Array readData(Variable v2, Section wantSection) throws IOException, InvalidRangeException {
    raf.seek(0);
    raf.order(RandomAccessFile.BIG_ENDIAN);

    int size = (int) v2.getSize();
    short[] arr = new short[size];

    RegularLayout indexer = new RegularLayout(0, v2.getElementSize(), -1, v2.getShape(), wantSection);
    while (indexer.hasNext()) {
      Indexer.Chunk chunk = indexer.next();
      raf.seek(chunk.getFilePos());
      raf.readShort(arr, (int) chunk.getStartElem(), chunk.getNelems()); // copy into primitive array
    }

    return Array.factory(DataType.SHORT.getPrimitiveClassType(), v2.getShape(), arr);
  }

  void doNothing() {
  }

  public void close() throws IOException {
    raf.close();
  }
}
