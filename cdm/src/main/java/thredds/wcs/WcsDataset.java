// $Id:WcsDataset.java 63 2006-07-12 21:50:51Z edavis $
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
package thredds.wcs;

import ucar.nc2.geotiff.GeotiffWriter;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.util.DiskCache;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dataset.grid.GeoGrid;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.ma2.Array;
import ucar.ma2.Range;
import ucar.ma2.InvalidRangeException;

import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.Document;

import java.io.*;
import java.util.List;

/**
 * Encapsolates a GridDataset.
 * @author caron
 * @version $Revision:63 $ $Date:2006-07-12 21:50:51Z $
 */

public class WcsDataset {
  private String serverURL = "http://localhost:8080/thredds/wcs?";

  private GridDataset gridDataset;
  private String datasetPath;
  private boolean isRemote;
  private XMLOutputter fmt;

  // private boolean useGreyscale = true;
  private boolean showDoc = false, debug = false;

  public WcsDataset( GridDataset gridDataset, String datasetPath, boolean isRemote) {
    this.gridDataset = gridDataset;
    this.datasetPath = datasetPath;
    this.isRemote = isRemote;

    //fmt = new XMLOutputter("  ", true);
    //fmt.setLineSeparator("\n");
    fmt = new XMLOutputter( Format.getPrettyFormat());
  }

  public void close() throws IOException {
    if (null != gridDataset)
      gridDataset.close();
  }

  public void setServerBase( String serverBase) {
    this.serverURL = serverBase+"?";
    if (isRemote)
      this.serverURL = this.serverURL + "dataset="+datasetPath +"&";
  }

  // public void setUseGreyscale( boolean b) { useGreyscale = b; }

  public String getCapabilities() throws IOException {
    XMLwriter writer = new XMLwriter();
    return fmt.outputString( writer.makeCapabilities( serverURL, gridDataset, null)); // LOOK  should cache
  }

  public void getCapabilities(java.io.OutputStream out, SectionType section) throws IOException {
    XMLwriter writer = new XMLwriter();
    Document doc = writer.makeCapabilities( serverURL, gridDataset, section);
    fmt.output( doc, out);
    if (showDoc)
      fmt.output( doc, System.out);
  }

  public boolean hasCoverage( String coverageName) {
    return gridDataset.findGridDatatype( coverageName) != null;
  }

  public String describeCoverage() throws IOException {
    XMLwriter writer = new XMLwriter();
    return fmt.outputString( writer.makeDescribeCoverage( gridDataset, null)); // LOOK  should cache
  }

  public String describeCoverage(String gridName) throws IOException {
    String[] coverageNames = new String[1];
    coverageNames[0] = gridName;
    XMLwriter writer = new XMLwriter();
    return fmt.outputString( writer.makeDescribeCoverage( gridDataset, coverageNames)); // LOOK  should cache
  }

  public void describeCoverage(java.io.OutputStream out, String[] coverageNames) throws IOException {
    XMLwriter writer = new XMLwriter();
    Document doc = writer.makeDescribeCoverage( gridDataset, coverageNames);
    fmt.output( doc, out);
    if (showDoc)
      fmt.output( doc, System.out);
  }

  public String checkCoverageParameters( GetCoverageRequest req) throws IOException {
    String vname = req.getCoverage();
    GridDatatype geogrid = gridDataset.findGridDatatype(vname);
    GridCoordSystem gcs = geogrid.getGridCoordSystem();
    CoordinateAxis1D vaxis = gcs.getVerticalAxis();

    int z = 0, t = 0;
    String vertical = req.getVertical();
    if (vertical != null) {
      z = vaxis.getIndex( vertical);
      if (z < 0) return "Level= "+vertical;
    }

    String time = req.getTime();
    if (time != null) {
      t = findTimeIndex( gcs, time);
      if (t < 0) return "Time= "+time;
    }

    return null;
  }

  public String getCoverage( GetCoverageRequest req) throws IOException, InvalidRangeException {
    String vname = req.getCoverage();
    GridDatatype geogrid = gridDataset.findGridDatatype(vname);
    GridCoordSystem gcs = geogrid.getGridCoordSystem();
    Range t_range = null, z_range = null;
    Range y_range = null, x_range = null;

    String time = req.getTime();
    if (time != null) {
      int t = findTimeIndex( gcs, time);
      t_range = new Range(t,t);
    }

    String vertical = req.getVertical();
    if (vertical != null) {
      CoordinateAxis1D vaxis = gcs.getVerticalAxis();
      int z = vaxis.getIndex( vertical);
      z_range = new Range(z,z);
    }

    if (null != req.getBoundingBox()) {
      List ranges = gcs.getRangesFromLatLonRect(req.getBoundingBox());
      y_range = (Range) ranges.get(0);
      x_range = (Range) ranges.get(1);
    }

    if (debug) {
      System.out.println(" bb="+req.getBoundingBox());
      System.out.println(" y_range="+y_range);
      System.out.println(" x_range="+x_range);
    }

    GridDatatype subset = geogrid.makeSubset(t_range, z_range, y_range, x_range);
    Array data = subset.readDataSlice(0, 0, -1, -1);
    // Array data = geogrid.readDataSlice(t, z, -1, -1); // 2D plane, all x, y

    if (req.getFormat() == GetCoverageRequest.Format.GeoTIFF || req.getFormat() == GetCoverageRequest.Format.GeoTIFFfloat) {
      //String dname = (datasetURL != null) ? datasetURL : datasetPath;
      File tifFile = DiskCache.getCacheFile(datasetPath+"-"+vname+".tif");
      if (debug) System.out.println(" tifFile="+tifFile.getPath());

      GeotiffWriter writer = new GeotiffWriter(tifFile.getPath());
      writer.writeGrid( gridDataset, subset, data, req.getFormat() == GetCoverageRequest.Format.GeoTIFF);

      writer.close();
      return tifFile.getPath();

    } else if (req.getFormat() == GetCoverageRequest.Format.NetCDF3) {

      //String dname = (datasetURL != null) ? datasetURL : datasetPath;
      File ncFile = DiskCache.getCacheFile(datasetPath+"-"+vname+".nc");
      if (debug) System.out.println(" ncFile="+ncFile.getPath());

      // LOOK - break encapsolation
      ((GeoGrid)subset).writeFile( ncFile.getPath());
      return ncFile.getPath();

    } else
      return null;
  }

  private int findTimeIndex( GridCoordSystem gcs, String timeName) {
    CoordinateAxis1DTime taxis = gcs.getTimeAxis();
    java.util.Date[] dates = taxis.getTimeDates();

    DateFormatter formatter = new DateFormatter();
    for (int i = 0; i < dates.length; i++) {
      String name = formatter.toDateTimeStringISO(dates[i]);
      if (name.equals(timeName))
        return i;
    }
    return -1;
  }

  static public void main(String[] args) throws IOException {
    String name = "C:/data/grib2/ndfd.wmo";
    GridDataset gd = ucar.nc2.dataset.grid.GridDataset.open(name);
    WcsDataset wcs = new WcsDataset(gd, "ndfd.wmo", false);
    System.out.println(wcs.getCapabilities());
    System.out.println(wcs.describeCoverage());
  }

}