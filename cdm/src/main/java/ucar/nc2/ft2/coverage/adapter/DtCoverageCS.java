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
package ucar.nc2.ft2.coverage.adapter;

import ucar.nc2.Attribute;
import ucar.nc2.NCdumpW;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.*;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.*;

import java.io.IOException;
import java.util.*;

/**
 * fork ucar.nc2.dt.grid.GridCoordSys for adaption of GridCoverage.
 * Minimalist, does not do subsetting, vertical transform.
 *
 * @author caron
 * @since 5/26/2015
 */
public class DtCoverageCS {
  //static private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DtCoverageCS.class);
  //static private final boolean warnUnits = false;

  /////////////////////////////////////////////////////////////////////////////
  protected DtCoverageCSBuilder builder;
  private String name;
  private ProjectionImpl proj;
  private GeoGridCoordinate2D g2d;
  private boolean isLatLon = false;

  /**
   * Create a GeoGridCoordSys from an existing Coordinate System.
   * This will choose which axes are the XHoriz, YHoriz, Vertical, Time, RunTIme, Ensemble.
   * If theres a Projection, it will set its map area
   *
   * @param builder create from this
   */
  public DtCoverageCS(DtCoverageCSBuilder builder) {
    super();
    this.builder = builder;

    // make name based on coordinate
    this.name = CoordinateSystem.makeName(builder.allAxes);

    // WRF NMM
    Attribute att = getXHorizAxis().findAttribute(_Coordinate.Stagger);
    if (att != null)
      setHorizStaggerType(att.getStringValue());

    if (builder.orgProj != null) {
      proj = builder.orgProj.constructCopy();
    }
  }

  public String getName() {
    return name;
  }

  public FeatureType getCoverageType() {
    return builder.type;
  }

  public List<CoordinateAxis> getCoordAxes() {
    return builder.allAxes;
  }

  public CoordinateAxis findCoordAxis(String shortName) {
    for (CoordinateAxis axis : builder.allAxes) {
      if (axis.getShortName().equals(shortName)) return axis;
    }
    return null;
  }

  public List<CoordinateTransform> getCoordTransforms() {
    return builder.coordTransforms;
  }

  /**
   * get the X Horizontal axis (either GeoX or Lon)
   */
  public CoordinateAxis getXHorizAxis() {
    return builder.xaxis;
  }

  /**
   * get the Y Horizontal axis (either GeoY or Lat)
   */
  public CoordinateAxis getYHorizAxis() {
    return builder.yaxis;
  }

  /**
   * get the Vertical axis (either Geoz, Height, or Pressure)
   */
  public CoordinateAxis1D getVerticalAxis() {
    return builder.vertAxis;
  }


  public CoordinateAxis getTimeAxis() {
    return builder.timeAxis;
  }

  public CoordinateAxis1DTime getRunTimeAxis() {
    return builder.rtAxis;
  }

  public CoordinateAxis1D getEnsembleAxis() {
    return builder.ensAxis;
  }

  public ProjectionImpl getProjection() {
    return proj;
  }

  /**
   * is this a Lat/Lon coordinate system?
   */
  public boolean isLatLon() {
    return isLatLon;
  }

  /**
   * Is this a global coverage over longitude ?
   *
   * @return true if isLatLon and longitude wraps
   */
  public boolean isGlobalLon() {
    if (!isLatLon) return false;
    if (!(getXHorizAxis() instanceof CoordinateAxis1D)) return false;
    CoordinateAxis1D lon = (CoordinateAxis1D) getXHorizAxis();
    double first = lon.getCoordEdge(0);
    double last = lon.getCoordEdge((int) lon.getSize());
    double min = Math.min(first, last);
    double max = Math.max(first, last);
    return (max - min) >= 360;
  }

  /**
   * true if x and y axes are CoordinateAxis1D and are regular
   */
  public boolean isRegularSpatial() {
    if (!isRegularSpatial(getXHorizAxis())) return false;
    if (!isRegularSpatial(getYHorizAxis())) return false;
    return true;
  }

  private boolean isRegularSpatial(CoordinateAxis axis) {
    if (axis == null) return true;
    if (!(axis instanceof CoordinateAxis1D)) return false;
    return ((CoordinateAxis1D) axis).isRegular();
  }

  private String horizStaggerType;

  public String getHorizStaggerType() {
    return horizStaggerType;
  }

  public void setHorizStaggerType(String horizStaggerType) {
    this.horizStaggerType = horizStaggerType;
  }

  private ProjectionRect mapArea = null;

  /**
   * Get the x,y bounding box in projection coordinates.
   */
  public ProjectionRect getBoundingBox() {
    if (mapArea == null) {

      CoordinateAxis horizXaxis = getXHorizAxis();
      CoordinateAxis horizYaxis = getYHorizAxis();
      if ((horizXaxis == null) || !horizXaxis.isNumeric() || (horizYaxis == null) || !horizYaxis.isNumeric())
        return null; // impossible

      // x,y may be 2D
      if ((horizXaxis instanceof CoordinateAxis2D) && (horizYaxis instanceof CoordinateAxis2D)) {
        // could try to optimize this - just get corners or something
        CoordinateAxis2D xaxis2 = (CoordinateAxis2D) horizXaxis;
        CoordinateAxis2D yaxis2 = (CoordinateAxis2D) horizYaxis;

        mapArea = null; // getBBfromCorners(xaxis2, yaxis2);  LOOK LOOK

        // mapArea = new ProjectionRect(horizXaxis.getMinValue(), horizYaxis.getMinValue(),
        //         horizXaxis.getMaxValue(), horizYaxis.getMaxValue());

      } else {

        CoordinateAxis1D xaxis1 = (CoordinateAxis1D) horizXaxis;
        CoordinateAxis1D yaxis1 = (CoordinateAxis1D) horizYaxis;

        /* add one percent on each side if its a projection. WHY?
        double dx = 0.0, dy = 0.0;
        if (!isLatLon()) {
          dx = .01 * (xaxis1.getCoordEdge((int) xaxis1.getSize()) - xaxis1.getCoordEdge(0));
          dy = .01 * (yaxis1.getCoordEdge((int) yaxis1.getSize()) - yaxis1.getCoordEdge(0));
        }

        mapArea = new ProjectionRect(xaxis1.getCoordEdge(0) - dx, yaxis1.getCoordEdge(0) - dy,
            xaxis1.getCoordEdge((int) xaxis1.getSize()) + dx,
            yaxis1.getCoordEdge((int) yaxis1.getSize()) + dy); */

        mapArea = new ProjectionRect(xaxis1.getCoordEdge(0), yaxis1.getCoordEdge(0),
                xaxis1.getCoordEdge((int) xaxis1.getSize()),
                yaxis1.getCoordEdge((int) yaxis1.getSize()));
      }
    }

    return mapArea;
  }

  /**
   * Get the Lat/Lon coordinates of the midpoint of a grid cell, using the x,y indices
   *
   * @param xindex x index
   * @param yindex y index
   * @return lat/lon coordinate of the midpoint of the cell
   */
  public LatLonPoint getLatLon(int xindex, int yindex) {
    double x, y;

    CoordinateAxis horizXaxis = getXHorizAxis();
    CoordinateAxis horizYaxis = getYHorizAxis();
    if (horizXaxis instanceof CoordinateAxis1D) {
      CoordinateAxis1D horiz1D = (CoordinateAxis1D) horizXaxis;
      x = horiz1D.getCoordValue(xindex);
    } else {
      CoordinateAxis2D horiz2D = (CoordinateAxis2D) horizXaxis;
      x = horiz2D.getCoordValue(yindex, xindex);
    }

    if (horizYaxis instanceof CoordinateAxis1D) {
      CoordinateAxis1D horiz1D = (CoordinateAxis1D) horizYaxis;
      y = horiz1D.getCoordValue(yindex);
    } else {
      CoordinateAxis2D horiz2D = (CoordinateAxis2D) horizYaxis;
      y = horiz2D.getCoordValue(yindex, xindex);
    }

    return isLatLon() ? new LatLonPointImpl(y, x) : getLatLon(x, y);
  }

  public LatLonPoint getLatLon(double xcoord, double ycoord) {
    Projection dataProjection = getProjection();
    return dataProjection.projToLatLon(new ProjectionPointImpl(xcoord, ycoord), new LatLonPointImpl());
  }

  private LatLonRect llbb = null;

  /**
   * Get horizontal bounding box in lat, lon coordinates.
   *
   * @return lat, lon bounding box.
   */
  public LatLonRect getLatLonBoundingBox() {

    if (llbb == null) {

      if ((getXHorizAxis() instanceof CoordinateAxis2D) && (getYHorizAxis() instanceof CoordinateAxis2D)) {
        return null;
      }

      CoordinateAxis horizXaxis = getXHorizAxis();
      CoordinateAxis horizYaxis = getYHorizAxis();
      if (isLatLon()) {
        double startLat = horizYaxis.getMinValue();
        double startLon = horizXaxis.getMinValue();

        double deltaLat = horizYaxis.getMaxValue() - startLat;
        double deltaLon = horizXaxis.getMaxValue() - startLon;

        LatLonPoint llpt = new LatLonPointImpl(startLat, startLon);
        llbb = new LatLonRect(llpt, deltaLat, deltaLon);

      } else {
        ProjectionImpl dataProjection = getProjection();
        ProjectionRect bb = getBoundingBox();
        if (bb != null)
          llbb = dataProjection.projToLatLonBB(bb);
      }
    }

    return llbb;

      /*  // look at all 4 corners of the bounding box
        LatLonPointImpl llpt = (LatLonPointImpl) dataProjection.projToLatLon(bb.getLowerLeftPoint(), new LatLonPointImpl());
        LatLonPointImpl lrpt = (LatLonPointImpl) dataProjection.projToLatLon(bb.getLowerRightPoint(), new LatLonPointImpl());
        LatLonPointImpl urpt = (LatLonPointImpl) dataProjection.projToLatLon(bb.getUpperRightPoint(), new LatLonPointImpl());
        LatLonPointImpl ulpt = (LatLonPointImpl) dataProjection.projToLatLon(bb.getUpperLeftPoint(), new LatLonPointImpl());

        // Check if grid contains poles.
        boolean includesNorthPole = false;
        int[] resultNP;
        resultNP = findXYindexFromLatLon(90.0, 0, null);
        if (resultNP[0] != -1 && resultNP[1] != -1)
          includesNorthPole = true;
        boolean includesSouthPole = false;
        int[] resultSP;
        resultSP = findXYindexFromLatLon(-90.0, 0, null);
        if (resultSP[0] != -1 && resultSP[1] != -1)
          includesSouthPole = true;

        if (includesNorthPole && !includesSouthPole) {
          llbb = new LatLonRect(llpt, new LatLonPointImpl(90.0, 0.0)); // ??? lon=???
          llbb.extend(lrpt);
          llbb.extend(urpt);
          llbb.extend(ulpt);
          // OR
          //llbb.extend( new LatLonRect( llpt, lrpt ));
          //llbb.extend( new LatLonRect( lrpt, urpt ) );
          //llbb.extend( new LatLonRect( urpt, ulpt ) );
          //llbb.extend( new LatLonRect( ulpt, llpt ) );
        } else if (includesSouthPole && !includesNorthPole) {
          llbb = new LatLonRect(llpt, new LatLonPointImpl(-90.0, -180.0)); // ??? lon=???
          llbb.extend(lrpt);
          llbb.extend(urpt);
          llbb.extend(ulpt);
        } else {
          double latMin = Math.min(llpt.getLatitude(), lrpt.getLatitude());
          double latMax = Math.max(ulpt.getLatitude(), urpt.getLatitude());

          // longitude is a bit tricky as usual
          double lonMin = getMinOrMaxLon(llpt.getLongitude(), ulpt.getLongitude(), true);
          double lonMax = getMinOrMaxLon(lrpt.getLongitude(), urpt.getLongitude(), false);

          llpt.set(latMin, lonMin);
          urpt.set(latMax, lonMax);

          llbb = new LatLonRect(llpt, urpt);
        }
      }
    }  */

  }

  @Override
  public String toString() {
    Formatter buff = new Formatter();
    show(buff, false);
    return buff.toString();
  }

  public void show(Formatter f, boolean showCoords) {
    f.format("Coordinate System (%s)%n", getName());

    showCoordinateAxis(getRunTimeAxis(), f, showCoords);
    showCoordinateAxis(getEnsembleAxis(), f, showCoords);
    showCoordinateAxis(getTimeAxis(), f, showCoords);
    showCoordinateAxis(getVerticalAxis(), f, showCoords);
    showCoordinateAxis(getYHorizAxis(), f, showCoords);
    showCoordinateAxis(getXHorizAxis(), f, showCoords);

    if (proj != null)
      f.format(" Projection: %s %s%n", proj.getName(), proj.paramsToString());
  }

  private void showCoordinateAxis(CoordinateAxis axis, Formatter f, boolean showCoords) {
    if (axis == null) return;
    f.format(" rt=%s (%s)", axis.getNameAndDimensions(), axis.getClass().getName());
    if (showCoords) showCoords(axis, f);
    f.format("%n");
  }

  private void showCoords(CoordinateAxis axis, Formatter f) {
    try {
      if (axis instanceof CoordinateAxis1D && axis.isNumeric()) {
        CoordinateAxis1D axis1D = (CoordinateAxis1D) axis;
        if (!axis1D.isInterval()) {
          double[] e = axis1D.getCoordEdges();
          for (double anE : e) {
            f.format("%f,", anE);
          }
        } else {
          double[] b1 = axis1D.getBound1();
          double[] b2 = axis1D.getBound2();
          for (int i = 0; i < b1.length; i++) {
            f.format("(%f,%f) = %f%n", b1[i], b2[i], b2[i] - b1[i]);
          }
        }
      } else {
        f.format("%s", NCdumpW.printVariableData(axis, null));
      }
    } catch (IOException ioe) {
      f.format(ioe.getMessage());
    }
    f.format(" %s%n", axis.getUnitsString());
  }

  public CalendarDateRange getCalendarDateRange() {
    CoordinateAxis timeTaxis = getTimeAxis();
    if (timeTaxis != null && timeTaxis instanceof CoordinateAxis1DTime)
      return ((CoordinateAxis1DTime) timeTaxis).getCalendarDateRange();

    CoordinateAxis1DTime rtaxis = getRunTimeAxis();
    if (rtaxis != null) {
      return rtaxis.getCalendarDateRange();
    }

    return null;
  }

  public int getDomainRank() {
    return CoordinateSystem.makeDomain(builder.independentAxes).size();
  }

  public int getRangeRank() {
    return builder.allAxes.size(); // not right
  }
}

  ///////////////////////////////////////////////////////////////////////
  /* experimental

  static private double getMinOrMaxLon(double lon1, double lon2, boolean wantMin) {
    double midpoint = (lon1 + lon2) / 2;
    lon1 = LatLonPointImpl.lonNormal(lon1, midpoint);
    lon2 = LatLonPointImpl.lonNormal(lon2, midpoint);

    return wantMin ? Math.min(lon1, lon2) : Math.max(lon1, lon2);
  }

  static public LatLonRect getLatLonBoundingBox(Projection proj, double startx, double starty, double endx, double endy) {

    if (proj instanceof LatLonProjection) {
      double deltaLat = endy - starty;
      double deltaLon = endx - startx;

      LatLonPoint llpt = new LatLonPointImpl(starty, startx);
      return new LatLonRect(llpt, deltaLat, deltaLon);

    }

    ProjectionRect bb = new ProjectionRect(startx, starty, endx, endy);

    // look at all 4 corners of the bounding box
    LatLonPointImpl llpt = (LatLonPointImpl) proj.projToLatLon(bb.getLowerLeftPoint(), new LatLonPointImpl());
    LatLonPointImpl lrpt = (LatLonPointImpl) proj.projToLatLon(bb.getLowerRightPoint(), new LatLonPointImpl());
    LatLonPointImpl urpt = (LatLonPointImpl) proj.projToLatLon(bb.getUpperRightPoint(), new LatLonPointImpl());
    LatLonPointImpl ulpt = (LatLonPointImpl) proj.projToLatLon(bb.getUpperLeftPoint(), new LatLonPointImpl());

    // Check if grid contains poles. LOOK disabled
    boolean includesNorthPole = false;
    /* int[] resultNP = new int[2];
    resultNP = findXYindexFromLatLon(90.0, 0, null);
    if (resultNP[0] != -1 && resultNP[1] != -1)
      includesNorthPole = true;  //

    boolean includesSouthPole = false;
    /* int[] resultSP = new int[2];
    resultSP = findXYindexFromLatLon(-90.0, 0, null);
    if (resultSP[0] != -1 && resultSP[1] != -1)
      includesSouthPole = true;  //

    LatLonRect llbb;

    if (includesNorthPole && !includesSouthPole) {
      llbb = new LatLonRect(llpt, new LatLonPointImpl(90.0, 0.0)); // ??? lon=???
      llbb.extend(lrpt);
      llbb.extend(urpt);
      llbb.extend(ulpt);

    } else if (includesSouthPole && !includesNorthPole) {
      llbb = new LatLonRect(llpt, new LatLonPointImpl(-90.0, -180.0)); // ??? lon=???
      llbb.extend(lrpt);
      llbb.extend(urpt);
      llbb.extend(ulpt);

    } else {
      double latMin = Math.min(llpt.getLatitude(), lrpt.getLatitude());
      double latMax = Math.max(ulpt.getLatitude(), urpt.getLatitude());

      // longitude is a bit tricky as usual
      double lonMin = getMinOrMaxLon(llpt.getLongitude(), ulpt.getLongitude(), true);
      double lonMax = getMinOrMaxLon(lrpt.getLongitude(), urpt.getLongitude(), false);

      llpt.set(latMin, lonMin);
      urpt.set(latMax, lonMax);

      llbb = new LatLonRect(llpt, urpt);
    }
    return llbb;
  }

}

/*

  public List<CalendarDate> getCalendarDates() {
    CoordinateAxis timeTaxis = getTimeAxis();
    if (timeTaxis != null && timeTaxis instanceof CoordinateAxis1DTime)
      return ((CoordinateAxis1DTime) timeTaxis).getCalendarDates();

    CoordinateAxis1DTime rtaxis = getRunTimeAxis();
    if (rtaxis != null) {
      return rtaxis.getCalendarDates();
    }

    return null;
  }


 */

 /**
   * Get the list of time names, to be used for user selection.
   * The ith one refers to the ith time coordinate.
   *
   * @return List of ucar.nc2.util.NamedObject, or empty list.
   *
public List<NamedObject> getTimes() {
  List<CalendarDate> cdates = getCalendarDates();
  if (cdates == null) throw new IllegalStateException("No CalendarDates");
  List<NamedObject> times = new ArrayList<>(cdates.size());
  for (CalendarDate cd : cdates) {
    times.add(new ucar.nc2.util.NamedAnything(cd.toString(), "calendar date"));
  }
  return times;
}


*/

  /**
   * Get the list of level names, to be used for user selection.
   * The ith one refers to the ith level coordinate.
   *
   * @return List of ucar.nc2.util.NamedObject, or empty list.
   *
public List<NamedObject> getLevels() {
  CoordinateAxis1D vertZaxis = getVerticalAxis();
  if (vertZaxis == null)
    return new ArrayList<>(0);

  int n = (int) vertZaxis.getSize();
  List<NamedObject> levels = new ArrayList<>(n);
  for (int i = 0; i < n; i++)
    levels.add(new ucar.nc2.util.NamedAnything(vertZaxis.getCoordName(i), vertZaxis.getUnitsString()));

  return levels;
}


*/
  /**
   * Get the String name for the ith level(z) coordinate.
   *
   * @param index which level coordinate
   * @return level name
   *
public String getLevelName(int index) {
  CoordinateAxis1D vertZaxis = getVerticalAxis();
  if ((vertZaxis == null) || (index < 0) || (index >= vertZaxis.getSize()))
    throw new IllegalArgumentException("getLevelName = " + index);
  return vertZaxis.getCoordName(index).trim();
}

  /**
   * Get the index corresponding to the level name.
   *
   * @param name level name
   * @return level index, or -1 if not found
   *
  public int getLevelIndex(String name) {
    CoordinateAxis1D vertZaxis = getVerticalAxis();
    if ((vertZaxis == null) || (name == null)) return -1;

    for (int i = 0; i < vertZaxis.getSize(); i++) {
      if (vertZaxis.getCoordName(i).trim().equals(name))
        return i;
    }
    return -1;
  }


*/
  /*
   * Create a GeoGridCoordSys as a section of an existing GeoGridCoordSys.
   * This will create sections of the corresponding CoordinateAxes.
   *
   * @param rt_range subset the runtime dimension, or null if you want all of it
   * @param e_range  subset the ensemble dimension, or null if you want all of it
   * @param t_range  subset the time dimension, or null if you want all of it
   * @param z_range  subset the vertical dimension, or null if you want all of it
   * @param y_range  subset the y dimension, or null if you want all of it
   * @param x_range  subset the x dimension, or null if you want all of it
   * @throws InvalidRangeException if any of the ranges are illegal
   *
  public DtCoverageCS subset(Range rt_range, Range e_range, Range t_range, Range z_range, Range y_range, Range x_range) throws InvalidRangeException {
    /* super();

    CoordinateAxis xaxis = from.getXHorizAxis();
    CoordinateAxis yaxis = from.getYHorizAxis();

    if ((xaxis instanceof CoordinateAxis1D) && (yaxis instanceof CoordinateAxis1D)) {
      CoordinateAxis1D xaxis1 = (CoordinateAxis1D) xaxis;
      CoordinateAxis1D yaxis1 = (CoordinateAxis1D) yaxis;

      horizXaxis = (x_range == null) ? xaxis1 : xaxis1.section(x_range);
      horizYaxis = (y_range == null) ? yaxis : yaxis1.section(y_range);

    } else if ((xaxis instanceof CoordinateAxis2D) && (yaxis instanceof CoordinateAxis2D) && from.isLatLon()) {
      CoordinateAxis2D lon_axis = (CoordinateAxis2D) xaxis;
      CoordinateAxis2D lat_axis = (CoordinateAxis2D) yaxis;

      horizXaxis = lon_axis.section(y_range, x_range);
      horizYaxis = lat_axis.section(y_range, x_range);

    } else
      throw new IllegalArgumentException("must be 1D or 2D/LatLon ");

    if (from.isGeoXY()) {
      xAxis = horizXaxis;
      yAxis = horizYaxis;
    } else {
      lonAxis = horizXaxis;
      latAxis = horizYaxis;
      isLatLon = true;
    }

    coordAxes.add(horizXaxis);
    coordAxes.add(horizYaxis);

    // set canonical area
    ProjectionImpl projOrig = from.getProjection();
    if (projOrig != null) {
      proj = projOrig.constructCopy();
      proj.setDefaultMapArea(getBoundingBox()); // LOOK expensive for 2D
    }

    CoordinateAxis1D zaxis = from.getVerticalAxis();
    if (zaxis != null) {
      vertZaxis = (z_range == null) ? zaxis : zaxis.section(z_range);
      coordAxes.add(vertZaxis);
      // LOOK assign hAxis, pAxis or zAxis ??
    }

    if (from.getVerticalCT() != null) {
      VerticalTransform vtFrom = from.getVerticalTransform(); // LOOK may need to make sure this exists?
      if (vtFrom != null)
        vt = vtFrom.subset(t_range, z_range, y_range, x_range);

      vCT = from.getVerticalCT();
    }

    CoordinateAxis1D eaxis = from.getEnsembleAxis();
    if (eaxis != null) {
      ensembleAxis = (e_range == null) ? eaxis : eaxis.section(e_range);
      coordAxes.add(ensembleAxis);
    }

    CoordinateAxis taxis = from.getTimeAxis();
    CoordinateAxis1DTime taxis1D = null;
    if (taxis != null) {
      if (taxis instanceof CoordinateAxis1DTime) {
        taxis1D = (CoordinateAxis1DTime) taxis;
        tAxis = timeTaxis = (t_range == null) ? taxis1D : taxis1D.section(t_range);
        coordAxes.add(timeTaxis);
        timeDim = timeTaxis.getDimension(0);
      } else {
        if ((rt_range == null) && (t_range == null))
          tAxis = taxis;
        else {
          Section timeSection = new Section().appendRange(rt_range).appendRange(t_range);
          tAxis = (CoordinateAxis) taxis.section(timeSection);
        }
        coordAxes.add(tAxis);
      }
    }

    CoordinateAxis1DTime rtaxis = from.getRunTimeAxis();
    if (rtaxis != null) {
      if (taxis1D != null) {
        Dimension tDim = taxis1D.getDimension(0);
        Dimension rtDim = rtaxis.getDimension(0);
        if (tDim.getShortName().equals(rtDim.getShortName())) { // must usetime subset range if dims match - FMRC 1D has this a lot
          runTimeAxis = (t_range == null) ? rtaxis : rtaxis.section(t_range);
        }
      }
      if (runTimeAxis == null)  // regular case of a run tim axis
        runTimeAxis = (rt_range == null) ? rtaxis : rtaxis.section(rt_range);

      coordAxes.add(runTimeAxis);
    }


    // make name based on coordinate
    Collections.sort(builder.standardAxes, new CoordinateAxis.AxisComparator()); // canonical ordering of axes
    this.name = makeName(builder.standardAxes);

    this.coordTrans = new ArrayList<>(from.getCoordinateTransforms());

    // collect dimensions
    for (CoordinateAxis axis : coordAxes) {
      List<Dimension> dims = axis.getDimensions();
      for (Dimension dim : dims) {
        dim.setShared(true); // make them shared (section will make them unshared)
        if (!domain.contains(dim))
          domain.add(dim);
      }
    }

    setHorizStaggerType(from.getHorizStaggerType()); //

    return null;
  } */

  /*
  private CoordinateAxis convertUnits(CoordinateAxis axis) {
    String units = axis.getUnitsString();
    SimpleUnit axisUnit = SimpleUnit.factory(units);
    if (axisUnit == null) return axis;

    double factor;
    try {
      factor = axisUnit.convertTo(1.0, SimpleUnit.kmUnit);
    } catch (IllegalArgumentException e) {
      if (warnUnits) log.warn("convertUnits failed", e);
      return axis;
    }
    if (factor == 1.0) return axis;

    Array data;
    try {
      data = axis.read();
    } catch (IOException e) {
      log.warn("convertUnits read failed", e);
      return axis;
    }

    DataType dtype = axis.getDataType();
    if (dtype.isFloatingPoint()) {
      IndexIterator ii = data.getIndexIterator();
      while (ii.hasNext())
        ii.setDoubleCurrent(factor * ii.getDoubleNext());

      CoordinateAxis newAxis = axis.copyNoCache();
      newAxis.setCachedData(data, false);
      newAxis.setUnitsString("km");
      return newAxis;

    } else {  // convert to DOUBLE
      Array newData = Array.factory(DataType.DOUBLE, axis.getShape());
      IndexIterator newi = newData.getIndexIterator();
      IndexIterator ii = data.getIndexIterator();
      while (ii.hasNext() && newi.hasNext())
        newi.setDoubleNext(factor * ii.getDoubleNext());

      CoordinateAxis newAxis = axis.copyNoCache();
      newAxis.setDataType(DataType.DOUBLE);
      newAxis.setCachedData(newData, false);
      newAxis.setUnitsString("km");
      return newAxis;
    }
  }

  /*
   * Given a point in x,y coordinate space, find the x,y index in the coordinate system.
   *
   * @param x_coord position in x coordinate space.
   * @param y_coord position in y coordinate space.
   * @param result  put result (x,y) index in here, may be null
   * @return int[2], 0=x,1=y indices in the coordinate system of the point. These will be -1 if out of range.
   *
  public int[] findXYindexFromCoord(double x_coord, double y_coord, int[] result) {
    if (result == null)
      result = new int[2];

    CoordinateAxis horizXaxis = getXHorizAxis();
    CoordinateAxis horizYaxis = getYHorizAxis();
    if ((horizXaxis instanceof CoordinateAxis1D) && (horizYaxis instanceof CoordinateAxis1D)) {
      result[0] = ((CoordinateAxis1D) horizXaxis).findCoordElement(x_coord);
      result[1] = ((CoordinateAxis1D) horizYaxis).findCoordElement(y_coord);
      return result;

    } else if ((horizXaxis instanceof CoordinateAxis2D) && (horizYaxis instanceof CoordinateAxis2D)) {
      if (g2d == null)
        g2d = new GeoGridCoordinate2D((CoordinateAxis2D) horizYaxis, (CoordinateAxis2D) horizXaxis);
      int[] result2 = new int[2];
      boolean found = g2d.findCoordElement(y_coord, x_coord, result2);
      if (found) {
        result[0] = result2[1];
        result[1] = result2[0];
      } else {
        result[0] = -1;
        result[1] = -1;
      }
      return result;
    }

    // cant happen
    throw new IllegalStateException("GridCoordSystem.findXYindexFromCoord");
  }

  /**
   * Given a point in x,y coordinate space, find the x,y index in the coordinate system.
   * If outside the range, the closest point is returned, eg, 0 or n-1 depending on if the coordinate is too small or too large.
   *
   * @param x_coord position in x coordinate space.
   * @param y_coord position in y coordinate space.
   * @param result  put result in here, may be null
   * @return int[2], 0=x,1=y indices in the coordinate system of the point.
   *
  public int[] findXYindexFromCoordBounded(double x_coord, double y_coord, int[] result) {
    if (result == null)
      result = new int[2];

    CoordinateAxis horizXaxis = getXHorizAxis();
    CoordinateAxis horizYaxis = getYHorizAxis();
    if ((horizXaxis instanceof CoordinateAxis1D) && (horizYaxis instanceof CoordinateAxis1D)) {
      result[0] = ((CoordinateAxis1D) horizXaxis).findCoordElementBounded(x_coord);
      result[1] = ((CoordinateAxis1D) horizYaxis).findCoordElementBounded(y_coord);
      return result;

    } else if ((horizXaxis instanceof CoordinateAxis2D) && (horizYaxis instanceof CoordinateAxis2D)) {
      if (g2d == null)
        g2d = new GeoGridCoordinate2D((CoordinateAxis2D) horizYaxis, (CoordinateAxis2D) horizXaxis);

      int[] result2 = new int[2];
      g2d.findCoordElement(y_coord, x_coord, result2); // returns best guess
      result[0] = result2[1];
      result[1] = result2[0];
      return result;
    }

    // cant happen
    throw new IllegalStateException("GridCoordSystem.findXYindexFromCoord");
  }

  /**
   * Given a lat,lon point, find the x,y index in the coordinate system.
   *
   * @param lat    latitude position.
   * @param lon    longitude position.
   * @param result put result in here, may be null
   * @return int[2], 0=x,1=y indices in the coordinate system of the point. These will be -1 if out of range.
   *
  public int[] findXYindexFromLatLon(double lat, double lon, int[] result) {
    Projection dataProjection = getProjection();
    ProjectionPoint pp = dataProjection.latLonToProj(new LatLonPointImpl(lat, lon), new ProjectionPointImpl());

    return findXYindexFromCoord(pp.getX(), pp.getY(), result);
  }

  /**
   * Given a lat,lon point, find the x,y index in the coordinate system.
   * If outside the range, the closest point is returned
   *
   * @param lat    latitude position.
   * @param lon    longitude position.
   * @param result put result in here, may be null
   * @return int[2], 0=x,1=y indices in the coordinate system of the point.
   *
  public int[] findXYindexFromLatLonBounded(double lat, double lon, int[] result) {
    Projection dataProjection = getProjection();
    ProjectionPoint pp = dataProjection.latLonToProj(new LatLonPointImpl(lat, lon), new ProjectionPointImpl());

    return findXYindexFromCoordBounded(pp.getX(), pp.getY(), result);
  }

    /**
   * Get Index Ranges for the given lat, lon bounding box.
   * For projection, only an approximation based on latlon corners.
   * Must have CoordinateAxis1D or 2D for x and y axis.
   *
   * @param rect the requested lat/lon bounding box
   * @return list of 2 Range objects, first y then x.
   *
  public List<Range> getRangesFromLatLonRect(LatLonRect rect) throws InvalidRangeException {
    double minx, maxx, miny, maxy;

    ProjectionImpl proj = getProjection();
    if (proj != null && !(proj instanceof VerticalPerspectiveView) && !(proj instanceof MSGnavigation)
            && !(proj instanceof Geostationary)) { // LOOK kludge - how to do this generrally ??
      // first clip the request rectangle to the bounding box of the grid
      LatLonRect bb = getLatLonBoundingBox();
      if (bb == null) throw new IllegalStateException("No LatLonBoundingBox");
      LatLonRect rect2 = bb.intersect(rect);
      if (null == rect2)
        throw new InvalidRangeException("Request Bounding box does not intersect Grid ");
      rect = rect2;
    }

    CoordinateAxis xaxis = getXHorizAxis();
    CoordinateAxis yaxis = getYHorizAxis();

    if (isLatLon()) {

      LatLonPointImpl llpt = rect.getLowerLeftPoint();
      LatLonPointImpl urpt = rect.getUpperRightPoint();
      LatLonPointImpl lrpt = rect.getLowerRightPoint();
      LatLonPointImpl ulpt = rect.getUpperLeftPoint();

      minx = getMinOrMaxLon(llpt.getLongitude(), ulpt.getLongitude(), true);
      miny = Math.min(llpt.getLatitude(), lrpt.getLatitude());
      maxx = getMinOrMaxLon(urpt.getLongitude(), lrpt.getLongitude(), false);
      maxy = Math.min(ulpt.getLatitude(), urpt.getLatitude());

      // normalize to [minLon,minLon+360]
      double minLon = xaxis.getMinValue();
      minx = LatLonPointImpl.lonNormalFrom( minx, minLon);
      maxx = LatLonPointImpl.lonNormalFrom( maxx, minLon);

    } else {
      ProjectionRect prect = getProjection().latLonToProjBB(rect); // allow projection to override
      minx = prect.getMinPoint().getX();
      miny = prect.getMinPoint().getY();
      maxx = prect.getMaxPoint().getX();
      maxy = prect.getMaxPoint().getY();

      /*
      see ProjectionImpl.latLonToProjBB2()
      Projection dataProjection = getProjection();
      ProjectionPoint ll = dataProjection.latLonToProj(llpt, new ProjectionPointImpl());
      ProjectionPoint ur = dataProjection.latLonToProj(urpt, new ProjectionPointImpl());
      ProjectionPoint lr = dataProjection.latLonToProj(lrpt, new ProjectionPointImpl());
      ProjectionPoint ul = dataProjection.latLonToProj(ulpt, new ProjectionPointImpl());

      minx = Math.min(ll.getX(), ul.getX());
      miny = Math.min(ll.getY(), lr.getY());
      maxx = Math.max(ur.getX(), lr.getX());
      maxy = Math.max(ul.getY(), ur.getY());
    }

    if ((xaxis instanceof CoordinateAxis1D) && (yaxis instanceof CoordinateAxis1D)) {
      CoordinateAxis1D xaxis1 = (CoordinateAxis1D) xaxis;
      CoordinateAxis1D yaxis1 = (CoordinateAxis1D) yaxis;

      int minxIndex = xaxis1.findCoordElementBounded(minx);
      int minyIndex = yaxis1.findCoordElementBounded(miny);

      int maxxIndex = xaxis1.findCoordElementBounded(maxx);
      int maxyIndex = yaxis1.findCoordElementBounded(maxy);

      List<Range> list = new ArrayList<>();
      list.add(new Range(Math.min(minyIndex, maxyIndex), Math.max(minyIndex, maxyIndex)));
      list.add(new Range(Math.min(minxIndex, maxxIndex), Math.max(minxIndex, maxxIndex)));
      return list;

    } else if ((xaxis instanceof CoordinateAxis2D) && (yaxis instanceof CoordinateAxis2D) && isLatLon()) {
      CoordinateAxis2D lon_axis = (CoordinateAxis2D) xaxis;
      CoordinateAxis2D lat_axis = (CoordinateAxis2D) yaxis;
      int shape[] = lon_axis.getShape();
      int nj = shape[0];
      int ni = shape[1];

      int mini = Integer.MAX_VALUE, minj = Integer.MAX_VALUE;
      int maxi = -1, maxj = -1;

      // margolis 2/18/2010
      //minx = LatLonPointImpl.lonNormal( minx ); // <-- THIS IS NEW
      //maxx = LatLonPointImpl.lonNormal( maxx ); // <-- THIS IS NEW

      // brute force, examine every point LOOK BAD
      for (int j = 0; j < nj; j++) {
        for (int i = 0; i < ni; i++) {
          double lat = lat_axis.getCoordValue(j, i);
          double lon = lon_axis.getCoordValue(j, i);
          //lon = LatLonPointImpl.lonNormal( lon ); // <-- THIS IS NEW

          if ((lat >= miny) && (lat <= maxy) && (lon >= minx) && (lon <= maxx)) {
            if (i > maxi) maxi = i;
            if (i < mini) mini = i;
            if (j > maxj) maxj = j;
            if (j < minj) minj = j;
            //System.out.println(j+" "+i+" lat="+lat+" lon="+lon);
          }
        }
      }

      // this is the case where no points are included
      if ((mini > maxi) || (minj > maxj)) {
        mini = 0;
        minj = 0;
        maxi = -1;
        maxj = -1;
      }

      ArrayList<Range> list = new ArrayList<>();
      list.add(new Range(minj, maxj));
      list.add(new Range(mini, maxi));
      return list;

    } else {
      throw new IllegalArgumentException("must be 1D or 2D/LatLon ");
    }

  }

  /* private int getCrossing(CoordinateAxis2D axis) {
    for (int i=0; i<n; i++)
      if (axis.getCoordValue(i,j) > min) return i;
  } */

  /* private GeneralPath bbShape = null;
 public Shape getLatLonBoundingShape() {
   if (isLatLon())
     return getBoundingBox();

   if (bbShape == null) {
     ProjectionRect bb = getBoundingBox();
     Projection displayProjection = displayMap.getProjection();
     Projection dataProjection = getProjection();

     bbShape = new GeneralPath();

     LatLonPoint llpt = dataProjection.projToLatLon( bb.getX(), bb.getY());
     ProjectionPoint pt = displayProjection.latLonToProj( llpt);
     bbShape.lineTo(pt.getX(), pt.getY());

     llpt = dataProjection.projToLatLon( bb.getX(), bb.getY()+bb.getHeight());
     pt = displayProjection.latLonToProj( llpt);
     bbShape.lineTo(pt.getX(), pt.getY());

     llpt = dataProjection.projToLatLon( bb.getX()+bb.getWidth(), bb.getY()+bb.getHeight());
     pt = displayProjection.latLonToProj( llpt);
     bbShape.lineTo(pt.getX(), pt.getY());

     llpt = dataProjection.projToLatLon( bb.getX()+bb.getWidth(), bb.getY());
     pt = displayProjection.latLonToProj( llpt);
     bbShape.lineTo(pt.getX(), pt.getY());


     bbShape.closePath();
   }

   return bbShape;
 }


*/
