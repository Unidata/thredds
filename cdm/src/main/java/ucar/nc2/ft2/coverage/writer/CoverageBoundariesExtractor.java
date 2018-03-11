/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */
package ucar.nc2.ft2.coverage.writer;

import ucar.nc2.ft2.coverage.*;
import ucar.unidata.geoloc.LatLonPointNoNormalize;

import java.util.Formatter;
import java.util.List;

/**
 * Provides methods for extracting the coverage boundaries in standard GIS text formats: WKT & GeoJSON.
 *
 * @author caron
 * @since 10/7/2015.
 */
public class CoverageBoundariesExtractor {
  /**
   * Takes a CoverageCollection and returns the boundary as a polygon in WKT,
   * used in OpenLayers widget in NCSS
   *
   * @return WKT string that is a polyline outlining the horizontal boundary
   */
  public static String getDatasetBoundariesWKT(CoverageCollection cc) {
    CoverageBoundariesExtractor extract = new CoverageBoundariesExtractor(cc);
    return extract.getBoundaryAsWKT();
  }

  private CoverageCollection gridDataset;
  private double minLon = 0;
  private double maxLon = 0;
  private double maxDiffLon = 0;
  private boolean crossesDateLine = false;

  public CoverageBoundariesExtractor(CoverageCollection gridDataset) {
    this.gridDataset = gridDataset;
  }

  public String getBoundaryAsWKT() {
    List<LatLonPointNoNormalize> latlons = gridDataset.getHorizCoordSys().calcConnectedLatLonBoundaryPoints(50, 100);

    //Build string from lists
    //Crosses dateLine?
    //Assuming grid cells don't extend more than 270 deg.
    if ((maxLon > 0 && minLon < 0) && maxDiffLon > 270) {
      //either crosses 0 or -180
      crossesDateLine = true;
    }

    Formatter result = new Formatter();
    result.format("POLYGON((");

    int nPoints = latlons.size();
    for (int i = 0; i < nPoints; i++) {
      double lon = latlons.get(i).getLongitude();
      if (crossesDateLine && lon < 0) lon += 360; // LOOK
      if (i > 0) result.format(",");
      result.format("%f %f", lon, latlons.get(i).getLatitude());
    }

    result.format("))");
    return result.toString();
  }

  /*
  public String getDatasetBoundariesGeoJSON() {

    StringBuilder polygonJSON = new StringBuilder("{\"type\":\"Polygon\", \"coordinates\":[ [ ");

    List<Double> polLons = new ArrayList<>();
    List<Double> polLats = new ArrayList<>();
    getLatLonsForPolygon(polLons, polLats);

    //Build string from lists
    //Crosses dateLine?
    //Assuming grid cells don't extend more than 270 deg.
    if ((maxLon > 0 && minLon < 0) && maxDiffLon > 270) {
      //either crosses 0 or -180
      crossesDateLine = true;
    }

    int nPoints = polLats.size();
    for (int i = 0; i < nPoints; i++) {

      double lon = polLons.get(i);

      if (crossesDateLine && lon < 0) lon += 360;

      if (i < nPoints - 1)
        polygonJSON.append("[").append(lon).append(", ").append(polLats.get(i)).append("],");
      else
        polygonJSON.append("[").append(lon).append(", ").append(polLats.get(i)).append("]");
    }


    polygonJSON.append(" ] ]}");

    return polygonJSON.toString();
  } */

}
