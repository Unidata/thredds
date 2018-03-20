/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dt.grid;

import ucar.nc2.*;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.*;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.util.NamedObject;
import ucar.nc2.units.*;

import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.geoloc.projection.VerticalPerspectiveView;
import ucar.unidata.geoloc.projection.RotatedPole;
import ucar.unidata.geoloc.projection.RotatedLatLon;
import ucar.unidata.geoloc.projection.sat.MSGnavigation;
import ucar.unidata.geoloc.projection.sat.Geostationary;
import ucar.unidata.geoloc.vertical.*;
import ucar.ma2.*;

import java.util.*;
import java.io.IOException;

import ucar.nc2.units.DateRange;

/**
 * A georeferencing "gridded" CoordinateSystem. This describes a "grid" of coordinates, which
 * implies a connected topology such that values next to each other in index space are next to
 * each other in coordinate space.
 * <p/>
 * This currently assumes that the CoordinateSystem
 * <ol>
 * <li> is georeferencing (has Lat, Lon or GeoX, GeoY axes)
 * <li> x, y are 1 or 2-dimensional axes.
 * <li> rt, z, e are 1-dimensional axes.
 * <li> t is 1 or 2 dimensional. if 2d, then rt exists
 * </ol>
 * <p/>
 * This is the common case for georeferencing coordinate systems. Mathematically it is a product set:
 * {X,Y} x {Z} x {T}. The x and y axes may be 1 or 2 dimensional.
 * <p/>
 * <p/>
 * A CoordinateSystem may have multiple horizontal and vertical axes. GridCoordSys chooses one
 * axis corresponding to X, Y, Z, and T. It gives preference to one dimensional axes (CoordinateAxis1D).
 *
 * @author caron
 */

public class GridCoordSys extends CoordinateSystem implements ucar.nc2.dt.GridCoordSystem {
  static private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GridCoordSys.class);
  static private final boolean warnUnits = false;

  /**
   * Determine if this CoordinateSystem can be made into a GridCoordSys. Optionally for a given variable.
   * This currently assumes that the CoordinateSystem:
   * <ol>
   * <li> is georeferencing (cs.isGeoReferencing())
   * <li> x, y are 1 or 2-dimensional axes.
   * <li> z, t, if they exist, are 1-dimensional axes.
   * <li> domain rank > 1
   * </ol>
   *
   * @param sbuff place information messages here, may be null
   * @param cs    the CoordinateSystem to test
   * @param v     can it be used for this variable; v may be null
   * @return true if it can be made into a GridCoordSys.
   * @see CoordinateSystem#isGeoReferencing
   */
  public static boolean isGridCoordSys(Formatter sbuff, CoordinateSystem cs, VariableEnhanced v) {
    // must be at least 2 axes
    if (cs.getRankDomain() < 2) {
      if (sbuff != null) {
        sbuff.format("%s: domain rank < 2%n", cs.getName());
      }
      return false;
    }

    // must be lat/lon or have x,y and projecction
    if (!cs.isLatLon()) {
      // do check for GeoXY ourself
      if ((cs.getXaxis() == null) || (cs.getYaxis() == null)) {
        if (sbuff != null) {
          sbuff.format("%s: NO Lat,Lon or X,Y axis%n", cs.getName());
        }
        return false;
      }
      if (null == cs.getProjection()) {
        if (sbuff != null) {
          sbuff.format("%s: NO projection found%n", cs.getName());
        }
        return false;
      }
    }

    // obtain the x,y or lat/lon axes. x,y normally must be convertible to km
    CoordinateAxis xaxis, yaxis;
    if (cs.isGeoXY()) {
      xaxis = cs.getXaxis();
      yaxis = cs.getYaxis();

      // change to warning
      ProjectionImpl p = cs.getProjection();
      if (!(p instanceof RotatedPole)) {
        if (!SimpleUnit.kmUnit.isCompatible(xaxis.getUnitsString())) {
          if (sbuff != null) {
            sbuff.format("%s: X axis units are not convertible to km%n", cs.getName());
          }
          //return false;
        }
        if (!SimpleUnit.kmUnit.isCompatible(yaxis.getUnitsString())) {
          if (sbuff != null) {
            sbuff.format("%s: Y axis units are not convertible to km%n", cs.getName());
          }
          //return false;
        }
      }
    } else {
      xaxis = cs.getLonAxis();
      yaxis = cs.getLatAxis();
    }

    // check x,y rank <= 2
    if ((xaxis.getRank() > 2) || (yaxis.getRank() > 2)) {
      if (sbuff != null)
        sbuff.format("%s: X or Y axis rank must be <= 2%n", cs.getName());
      return false;
    }

    // check that the x,y have at least 2 dimensions between them ( this eliminates point data)
    int xyDomainSize = CoordinateSystem.countDomain(new CoordinateAxis[]{xaxis, yaxis});
    if (xyDomainSize < 2) {
      if (sbuff != null)
        sbuff.format("%s: X and Y axis must have 2 or more dimensions%n", cs.getName());
      return false;
    }

    List<CoordinateAxis> testAxis = new ArrayList<>();
    testAxis.add(xaxis);
    testAxis.add(yaxis);

    //int countRangeRank = 2;

    CoordinateAxis z = cs.getHeightAxis();
    if ((z == null) || !(z instanceof CoordinateAxis1D)) z = cs.getPressureAxis();
    if ((z == null) || !(z instanceof CoordinateAxis1D)) z = cs.getZaxis();
    if ((z != null) && !(z instanceof CoordinateAxis1D)) {
      if (sbuff != null) {
        sbuff.format("%s: Z axis must be 1D%n", cs.getName());
      }
      return false;
    }
    if (z != null)
      testAxis.add(z);

    // tom margolis 3/2/2010
    // allow runtime independent of time
    CoordinateAxis t = cs.getTaxis();
    CoordinateAxis rt = cs.findAxis(AxisType.RunTime);

    // A runtime axis must be scalar or one-dimensional
    if (rt != null) {
      if (!rt.isScalar() && !(rt instanceof CoordinateAxis1D)) {
        if (sbuff != null) sbuff.format("%s: RunTime axis must be 1D%n", cs.getName());
          return false;
      }
    }

    // If time axis is two-dimensional...
    if ((t != null) && !(t instanceof CoordinateAxis1D) && (t.getRank() != 0)) {

      if (rt != null) {
        if (rt.getRank() != 1) {
          if (sbuff != null) sbuff.format("%s: Runtime axis must be 1D%n", cs.getName());
          return false;
        }

        // time first dimension must agree with runtime
        if (!rt.getDimension(0).equals(t.getDimension(0))) {
          if (sbuff != null) sbuff.format("%s: 2D Time axis first dimension must be runtime%n", cs.getName());
          return false;
        }
      }
    }

    if (t != null)
      testAxis.add(t);
    if (rt != null)
      testAxis.add(rt);

    CoordinateAxis ens = cs.getEnsembleAxis();
    if (ens != null)
      testAxis.add(ens);

    if (v != null) { // test to see that v doesnt have extra dimensions. LOOK RELAX THIS
      List<Dimension> testDomain = new ArrayList<>();
      for (CoordinateAxis axis : testAxis) {
        for (Dimension dim : axis.getDimensions()) {
          if (!testDomain.contains(dim))
            testDomain.add(dim);
        }
      }
      if (!CoordinateSystem.isSubset(v.getDimensionsAll(), testDomain)) {
        if (sbuff != null) sbuff.format(" NOT complete%n");
        return false;
      }
    }

    return true;
  }

  /**
   * Determine if the CoordinateSystem cs can be made into a GridCoordSys for the Variable v.
   *
   * @param sbuff put debug information into this StringBuffer; may be null.
   * @param cs    CoordinateSystem to check.
   * @param v     Variable to check.
   * @return the GridCoordSys made from cs, else null.
   */
  public static GridCoordSys makeGridCoordSys(Formatter sbuff, CoordinateSystem cs, VariableEnhanced v) {
    if (sbuff != null) {
      sbuff.format(" ");
      v.getNameAndDimensions(sbuff, false, true);
      sbuff.format(" check CS %s: ", cs.getName());
    }

    if (isGridCoordSys(sbuff, cs, v)) {
      GridCoordSys gcs = new GridCoordSys(cs, sbuff);
      if (sbuff != null) sbuff.format(" OK%n");
      return gcs;
    }

    return null;
  }


  /////////////////////////////////////////////////////////////////////////////
  private ProjectionImpl proj;
  private GridCoordinate2D g2d;
  private CoordinateAxis horizXaxis, horizYaxis;
  private CoordinateAxis1D vertZaxis, ensembleAxis;
  private CoordinateAxis1DTime timeTaxis, runTimeAxis;
  private VerticalCT vCT;
  private VerticalTransform vt;
  private Dimension timeDim;

  private boolean isLatLon = false;

  /**
   * Create a GridCoordSys from an existing Coordinate System.
   * This will choose which axes are the XHoriz, YHoriz, Vertical, Time, RunTIme, Ensemble.
   * If theres a Projection, it will set its map area
   *
   * @param cs    create from this Coordinate System
   * @param sbuff place information messages here, may be null
   */
  public GridCoordSys(CoordinateSystem cs, Formatter sbuff) {
    super();
    this.ds = cs.getNetcdfDataset();

    if (cs.isGeoXY()) {
      horizXaxis = xAxis = cs.getXaxis();
      horizYaxis = yAxis = cs.getYaxis();

      ProjectionImpl p = cs.getProjection();
      if (!(p instanceof RotatedPole) && !(p instanceof RotatedLatLon)) {
        // make a copy of the axes if they need to change
        horizXaxis = convertUnits(horizXaxis);
        horizYaxis = convertUnits(horizYaxis);
      }
    } else if (cs.isLatLon()) {
      horizXaxis = lonAxis = cs.getLonAxis();
      horizYaxis = latAxis = cs.getLatAxis();
      isLatLon = true;

      if (lonAxis instanceof CoordinateAxis1D) {
        ((CoordinateAxis1D) lonAxis).correctLongitudeWrap();
      }

    } else
      throw new IllegalArgumentException("CoordinateSystem is not geoReferencing");

    coordAxes.add(horizXaxis);
    coordAxes.add(horizYaxis);

    // set canonical area
    ProjectionImpl projOrig = cs.getProjection();
    if (projOrig != null) {
      proj = projOrig.constructCopy();
      proj.setDefaultMapArea(getBoundingBox());  // LOOK too expensive for 2D
    }

   // LOOK: require 1D vertical - need to generalize to nD vertical.
    CoordinateAxis z_oneD = hAxis = cs.getHeightAxis();
    if ((z_oneD == null) || !(z_oneD instanceof CoordinateAxis1D)) z_oneD = pAxis = cs.getPressureAxis();
    if ((z_oneD == null) || !(z_oneD instanceof CoordinateAxis1D)) z_oneD = zAxis = cs.getZaxis();
    if ((z_oneD != null) && !(z_oneD instanceof CoordinateAxis1D))
      z_oneD = null;

    CoordinateAxis z_best = hAxis;
    if (pAxis != null) {
      if ((z_best == null) || !(z_best.getRank() > pAxis.getRank())) z_best = pAxis;
    }
    if (zAxis != null) {
      if ((z_best == null) || !(z_best.getRank() > zAxis.getRank())) z_best = zAxis;
    }

    if ((z_oneD == null) && (z_best != null)) { // cant find one-d z but have nD z
      if (sbuff != null) sbuff.format("GridCoordSys needs a 1D Coordinate, instead has %s%n", z_best.getNameAndDimensions());
    }
    
    if (z_oneD != null) {
      vertZaxis = (CoordinateAxis1D) z_oneD;
      coordAxes.add(vertZaxis);
    } else {
      hAxis = pAxis = zAxis = null;
    }

    // timeTaxis must be CoordinateAxis1DTime
    CoordinateAxis t = cs.getTaxis();
    if (t != null) {

      if (t instanceof CoordinateAxis1D) {

        try {
          if (t instanceof CoordinateAxis1DTime)
            timeTaxis = (CoordinateAxis1DTime) t;
          else {
            timeTaxis = CoordinateAxis1DTime.factory(ds, t, sbuff);
          }

          tAxis = timeTaxis;
          coordAxes.add(timeTaxis);
          timeDim = t.getDimension(0);

        } catch (Exception e) {
          if (sbuff != null)
            sbuff.format("%s: Error reading time coord= %s err= %s%n", t.getDatasetLocation(), t.getFullName(), e.getMessage());
          log.error(t.getDatasetLocation()+": Error reading time coord= "+t.getFullName(), e);
          throw new IllegalStateException("CoordinateSystem does not have a usable time axis");
        }

      } else { // 2d

        tAxis = t;
        timeTaxis = null;
        coordAxes.add(t); // LOOK ??
      }
    }

    // look for special axes
    ensembleAxis = (CoordinateAxis1D) cs.findAxis(AxisType.Ensemble);
    if (null != ensembleAxis) coordAxes.add(ensembleAxis);

    CoordinateAxis rtAxis = cs.findAxis(AxisType.RunTime);
    if (null != rtAxis) {
      try {
        if (rtAxis instanceof CoordinateAxis1DTime)
          runTimeAxis = (CoordinateAxis1DTime) rtAxis;
        else
          runTimeAxis = CoordinateAxis1DTime.factory(ds, rtAxis, sbuff);

        coordAxes.add(runTimeAxis);

      } catch (IOException e) {
        if (sbuff != null) {
          sbuff.format("Error reading runtime coord= %s err= %s%n", rtAxis.getFullName(), e.getMessage());
        }
      }
    }

    // look for VerticalCT
    List<CoordinateTransform> list = cs.getCoordinateTransforms();
    for (CoordinateTransform ct : list) {
      if (ct instanceof VerticalCT) {
        vCT = (VerticalCT) ct;
        break;
      }
    }

    // make name based on coordinate
    Collections.sort(coordAxes, new CoordinateAxis.AxisComparator()); // canonical ordering of axes
    this.name = makeName(coordAxes);

    // copy all coordinate transforms into here
    this.coordTrans = new ArrayList<>(cs.getCoordinateTransforms());

    // collect dimensions
    for (CoordinateAxis axis : coordAxes) {
      List<Dimension> dims = axis.getDimensionsAll();
      for (Dimension dim : dims) {
        if (!domain.contains(dim))
          domain.add(dim);
      }
    }

    // WRF NMM
    Attribute att = getXHorizAxis().findAttribute(_Coordinate.Stagger);
    if (att != null)
      setHorizStaggerType(att.getStringValue());
  }

  /**
   * Create a GridCoordSys as a section of an existing GridCoordSys.
   * This will create sections of the corresponding CoordinateAxes.
   *
   * @param from    copy this GridCoordSys
   * @param t_range subset the time dimension, or null if you want all of it
   * @param z_range subset the vertical dimension, or null if you want all of it
   * @param y_range subset the y dimension, or null if you want all of it
   * @param x_range subset the x dimension, or null if you want all of it
   * @throws InvalidRangeException if any of the ranges are illegal
   */
  public GridCoordSys(GridCoordSys from, Range t_range, Range z_range, Range y_range, Range x_range) throws InvalidRangeException {
    this(from, null, null, t_range, z_range, y_range, x_range);
  }

  /**
   * Create a GridCoordSys as a section of an existing GridCoordSys.
   * This will create sections of the corresponding CoordinateAxes.
   *
   * @param from     copy this GridCoordSys
   * @param rt_range subset the runtime dimension, or null if you want all of it
   * @param e_range  subset the ensemble dimension, or null if you want all of it
   * @param t_range  subset the time dimension, or null if you want all of it
   * @param z_range  subset the vertical dimension, or null if you want all of it
   * @param y_range  subset the y dimension, or null if you want all of it
   * @param x_range  subset the x dimension, or null if you want all of it
   * @throws InvalidRangeException if any of the ranges are illegal
   */
  public GridCoordSys(GridCoordSys from, Range rt_range, Range e_range, Range t_range, Range z_range, Range y_range, Range x_range) throws InvalidRangeException {
    super();

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
        if (rtDim != null && tDim.getShortName().equals(rtDim.getShortName())) { // must use time subset range if dims match - FMRC 1D has this a lot
          runTimeAxis = (t_range == null) ? rtaxis : rtaxis.section(t_range);
        }
      }
      if (runTimeAxis == null) { // regular case of a run time axis
        runTimeAxis = (rt_range == null) ? rtaxis : rtaxis.section(rt_range);
      }

      coordAxes.add(runTimeAxis);
    }


    // make name based on coordinate
    Collections.sort(coordAxes, new CoordinateAxis.AxisComparator()); // canonical ordering of axes
    this.name = makeName(coordAxes);

    this.coordTrans = new ArrayList<>(from.getCoordinateTransforms());

    // collect dimensions
    for (CoordinateAxis axis : coordAxes) {
      List<Dimension> dims = axis.getDimensionsAll();
      for (Dimension dim : dims) {
        dim.setShared(true); // make them shared (section will make them unshared)
        if (!domain.contains(dim))
          domain.add(dim);
      }
    }
    
    setHorizStaggerType(from.getHorizStaggerType());        
  }

  private CoordinateAxis convertUnits(CoordinateAxis axis) {
    String units = axis.getUnitsString();
    SimpleUnit axisUnit = SimpleUnit.factory(units);
    if (axisUnit == null) {
      if (warnUnits) log.warn("cant parse unit= {}", units);
      return axis;
    }

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

  /**
   * Get the vertical transform function, or null if none
   *
   * @return the vertical transform function, or null if none
   */
  @Override
  public VerticalTransform getVerticalTransform() {
    return vt;
  }

  /**
   * Get the Coordinate Transform description.
   *
   * @return Coordinate Transform description, or null if none
   */
  @Override
  public VerticalCT getVerticalCT() {
    return vCT;
  }

  // we have to delay making these, since we dont identify the dimensions specifically until now
  void makeVerticalTransform(GridDataset gds, Formatter parseInfo) {
    if (vt != null) return; // already done
    if (vCT == null) return;  // no vt

    vt = vCT.makeVerticalTransform(gds.getNetcdfDataset(), timeDim);

    if (vt == null) {
      if (parseInfo != null)
        parseInfo.format("  - ERR can't make VerticalTransform = %s%n", vCT.getVerticalTransformType());
    } else {
      if (parseInfo != null) parseInfo.format("  - VerticalTransform = %s%n", vCT.getVerticalTransformType());
    }
  }

  /**
   * get the X Horizontal axis (either GeoX or Lon)
   */
  @Override
  public CoordinateAxis getXHorizAxis() {
    return horizXaxis;
  }

  /**
   * get the Y Horizontal axis (either GeoY or Lat)
   */
  @Override
  public CoordinateAxis getYHorizAxis() {
    return horizYaxis;
  }

  /**
   * get the Vertical axis (either Geoz, Height, or Pressure)
   */
  @Override
  public CoordinateAxis1D getVerticalAxis() {
    return vertZaxis;
  }

  /**
   * get the Time axis
   */
  @Override
  public CoordinateAxis getTimeAxis() {
    return tAxis;
  }

  /**
   * get the Time axis, if its 1-dimensional
   */
  @Override
  public CoordinateAxis1DTime getTimeAxis1D() {
    return timeTaxis;
  }

  /**
   * get the RunTime axis, else null
   */
  @Override
  public CoordinateAxis1DTime getRunTimeAxis() {
    return runTimeAxis;
  }

  /**
   * get the Ensemble axis, else null
   */
  @Override
  public CoordinateAxis1D getEnsembleAxis() {
    return ensembleAxis;
  }

  /**
   * get the projection
   */
  @Override
  public ProjectionImpl getProjection() {
    return proj;
  }

  @Override
  public void setProjectionBoundingBox() {
    // set canonical area
    if (proj != null) {
      proj.setDefaultMapArea(getBoundingBox());  // LOOK too expensive for 2D
    }
  }

  /**
   * is this a Lat/Lon coordinate system?
   */
  @Override
  public boolean isLatLon() {
    return isLatLon;
  }

  /**
   * Is this a global coverage over longitude ?
   * @return true if isLatLon and longitude wraps
   */
  @Override
  public boolean isGlobalLon() {
    if (!isLatLon) return false;
    if (!(horizXaxis instanceof CoordinateAxis1D)) return false;
    CoordinateAxis1D lon = (CoordinateAxis1D) horizXaxis;
    double first = lon.getCoordEdge(0);
    double last = lon.getCoordEdge((int) lon.getSize());
    double min = Math.min(first, last);
    double max =  Math.max(first, last);
    return (max - min) >= 360;
  }

  /**
   * true if increasing z coordinate values means "up" in altitude
   */
  @Override
  public boolean isZPositive() {
    if (vertZaxis == null) return false;
    if (vertZaxis.getPositive() != null) {
      return vertZaxis.getPositive().equalsIgnoreCase(ucar.nc2.constants.CF.POSITIVE_UP);
    }
    if (vertZaxis.getAxisType() == AxisType.Height) return true;
    return vertZaxis.getAxisType() != AxisType.Pressure;
  }

  /**
   * true if x and y axes are CoordinateAxis1D and are regular
   */
  @Override
  public boolean isRegularSpatial() {
    if (!isRegularSpatial(getXHorizAxis())) return false;
    if (!isRegularSpatial(getYHorizAxis())) return false;
    //if (!isRegularSpatial(getVerticalAxis())) return false; LOOK removed July 30, 2006 for WCS
    return true;
  }

  private boolean isRegularSpatial(CoordinateAxis axis) {
    if (axis == null) return true;
    if (!(axis instanceof CoordinateAxis1D)) return false;
    return ((CoordinateAxis1D) axis).isRegular();
  }
  
  private String horizStaggerType;

  @Override
  public String getHorizStaggerType() {
    return horizStaggerType;
  }

  public void setHorizStaggerType(String horizStaggerType) {
    this.horizStaggerType = horizStaggerType;
  }

  /**
   * Given a point in x,y coordinate space, find the x,y index in the coordinate system.
   *
   * @param x_coord position in x coordinate space.
   * @param y_coord position in y coordinate space.
   * @param result  put result (x,y) index in here, may be null
   * @return int[2], 0=x,1=y indices in the coordinate system of the point. These will be -1 if out of range.
   */
  @Override
  public int[] findXYindexFromCoord(double x_coord, double y_coord, int[] result) {
    if (result == null)
      result = new int[2];

    if ((horizXaxis instanceof CoordinateAxis1D) && (horizYaxis instanceof CoordinateAxis1D)) {
      result[0] = ((CoordinateAxis1D) horizXaxis).findCoordElement(x_coord);
      result[1] = ((CoordinateAxis1D) horizYaxis).findCoordElement(y_coord);
      return result;

    } else if ((horizXaxis instanceof CoordinateAxis2D) && (horizYaxis instanceof CoordinateAxis2D)) {
      if (g2d == null)
        g2d = new GridCoordinate2D((CoordinateAxis2D) horizYaxis, (CoordinateAxis2D) horizXaxis);
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
   */
  @Override
  public int[] findXYindexFromCoordBounded(double x_coord, double y_coord, int[] result) {
    if (result == null)
      result = new int[2];

    if ((horizXaxis instanceof CoordinateAxis1D) && (horizYaxis instanceof CoordinateAxis1D)) {
      result[0] = ((CoordinateAxis1D) horizXaxis).findCoordElementBounded(x_coord);
      result[1] = ((CoordinateAxis1D) horizYaxis).findCoordElementBounded(y_coord);
      return result;

    } else if ((horizXaxis instanceof CoordinateAxis2D) && (horizYaxis instanceof CoordinateAxis2D)) {
      if (g2d == null)
        g2d = new GridCoordinate2D((CoordinateAxis2D) horizYaxis, (CoordinateAxis2D) horizXaxis);

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
   */
  @Override
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
   */
  @Override
  public int[] findXYindexFromLatLonBounded(double lat, double lon, int[] result) {
    Projection dataProjection = getProjection();
    ProjectionPoint pp = dataProjection.latLonToProj(new LatLonPointImpl(lat, lon), new ProjectionPointImpl());

    return findXYindexFromCoordBounded(pp.getX(), pp.getY(), result);
  }

  /**
   * True if there is a Time Axis.
   */
  @Override
  public boolean hasTimeAxis() {
    return tAxis != null;
  }

  /**
   * True if there is a Time Axis and it is 1D.
   */
  @Override
  public boolean hasTimeAxis1D() {
    return timeTaxis != null;
  }

  /**
   * @deprecated doesnt work correctly for intervals
   */
  @Override
  public CoordinateAxis1DTime getTimeAxisForRun(int run_index) {
    if (!hasTimeAxis() || hasTimeAxis1D() || runTimeAxis == null) return null;
    int nruns = (int) runTimeAxis.getSize();
    if ((run_index < 0) || (run_index >= nruns))
      throw new IllegalArgumentException("getTimeAxisForRun index out of bounds= " + run_index);

    if (timeAxisForRun == null)
      timeAxisForRun = new CoordinateAxis1DTime[nruns];

    if (timeAxisForRun[run_index] == null)
      timeAxisForRun[run_index] = makeTimeAxisForRun(run_index);

    return timeAxisForRun[run_index];
  }

  private CoordinateAxis1DTime[] timeAxisForRun;

  private CoordinateAxis1DTime makeTimeAxisForRun(int run_index) {
    VariableDS section;
    try {
      section = (VariableDS) tAxis.slice(0, run_index);
      return CoordinateAxis1DTime.factory(ds, section, null);
    } catch (InvalidRangeException | IOException e) {
      e.printStackTrace();
    }

    return null;
  }

  private ProjectionRect mapArea = null;

  /**
   * Get the x,y bounding box in projection coordinates.
   */
  @Override
  public ProjectionRect getBoundingBox() {
    if (mapArea == null) {

      if ((horizXaxis == null) || !horizXaxis.isNumeric() || (horizYaxis == null) || !horizYaxis.isNumeric())
        return null; // impossible

      // x,y may be 2D
      if (!(horizXaxis instanceof CoordinateAxis1D) || !(horizYaxis instanceof CoordinateAxis1D)) {
        /*  could try to optimize this - just get cord=ners or something
        CoordinateAxis2D xaxis2 = (CoordinateAxis2D) horizXaxis;
        CoordinateAxis2D yaxis2 = (CoordinateAxis2D) horizYaxis;
        MAMath.MinMax
        */

        mapArea = new ProjectionRect(horizXaxis.getMinValue(), horizYaxis.getMinValue(),
                horizXaxis.getMaxValue(), horizYaxis.getMaxValue());

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
  @Override
  public LatLonPoint getLatLon(int xindex, int yindex) {
    double x, y;

    if (horizXaxis instanceof CoordinateAxis1D) {
      CoordinateAxis1D horiz1D = (CoordinateAxis1D) horizXaxis;
      x = horiz1D.getCoordValue(xindex);
    } else {
      CoordinateAxis2D horiz2D = (CoordinateAxis2D) horizXaxis;
      x = horiz2D.getCoordValue( yindex, xindex);
    }

    if (horizYaxis instanceof CoordinateAxis1D) {
      CoordinateAxis1D horiz1D = (CoordinateAxis1D) horizYaxis;
      y = horiz1D.getCoordValue(yindex);
    } else {
      CoordinateAxis2D horiz2D = (CoordinateAxis2D) horizYaxis;
      y = horiz2D.getCoordValue( yindex, xindex);
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
  @Override
  public LatLonRect getLatLonBoundingBox() {

    if (llbb == null) {

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

  /**
   * Get Index Ranges for the given lat, lon bounding box.
   *
   * @deprecated use getRangesFromLatLonRect.
   */
  public List<Range> getLatLonBoundingBox(LatLonRect rect) throws InvalidRangeException {
    return getRangesFromLatLonRect(rect);
  }

  /**
   * Get Index Ranges for the given lat, lon bounding box.
   * For projection, only an approximation based on latlon corners.
   * Must have CoordinateAxis1D or 2D for x and y axis.
   *
   * @param rect the requested lat/lon bounding box
   * @return list of 2 Range objects, first y then x.
   */
  @Override
  public List<Range> getRangesFromLatLonRect(LatLonRect rect) throws InvalidRangeException {
    double minx, maxx, miny, maxy;

    ProjectionImpl proj = getProjection();
    if (proj != null && !(proj instanceof VerticalPerspectiveView) && !(proj instanceof MSGnavigation)
         && !(proj instanceof Geostationary)) { // LOOK kludge - how to do this generrally ??
      // first clip the request rectangle to the bounding box of the grid
      LatLonRect bb = getLatLonBoundingBox();
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
      maxy = Math.max(ul.getY(), ur.getY()); */
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
 } */

  /**
   * String representation.
   */
  @Override
  public String toString() {
    Formatter buff = new Formatter();
    show(buff, false);
    return buff.toString();
  }

  @Override
  public void show(Formatter f, boolean showCoords) {
    f.format("Coordinate System (%s)%n", getName());

    if (getRunTimeAxis() != null) {
      f.format(" rt=%s (%s)", runTimeAxis.getNameAndDimensions(), runTimeAxis.getClass().getName());
      if (showCoords) showCoords(runTimeAxis, f);
      f.format("%n");
    }
    if (getEnsembleAxis() != null) {
      f.format(" ens=%s (%s)", ensembleAxis.getNameAndDimensions(), ensembleAxis.getClass().getName());
      if (showCoords) showCoords(ensembleAxis, f);
      f.format("%n");
    }
    if (getTimeAxis() != null) {
      f.format(" t=%s (%s)", tAxis.getNameAndDimensions(), tAxis.getClass().getName());
      if (showCoords) showCoords(tAxis, f);
      f.format("%n");
    }
    if (getVerticalAxis() != null) {
      f.format(" z=%s (%s)", vertZaxis.getNameAndDimensions(), vertZaxis.getClass().getName());
      if (showCoords) showCoords(vertZaxis, f);
      f.format("%n");
    }
    if (getYHorizAxis() != null) {
      f.format(" y=%s (%s)", horizYaxis.getNameAndDimensions(), horizYaxis.getClass().getName());
      if (showCoords) showCoords(horizYaxis, f);
      f.format("%n");
    }
    if (getXHorizAxis() != null) {
      f.format(" x=%s (%s)", horizXaxis.getNameAndDimensions(), horizXaxis.getClass().getName());
      if (showCoords) showCoords(horizXaxis, f);
      f.format("%n");
    }

    if (proj != null)
      f.format(" Projection: %s %s%n", proj.getName(), proj.paramsToString());
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
          for (int i=0; i<b1.length; i++) {
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

  /////////////////////////////////////////////////////////////////

  @Override
  public List<CalendarDate> getCalendarDates() {
    if (timeTaxis != null)
      return timeTaxis.getCalendarDates();

    else if (getRunTimeAxis() != null)
      return makeCalendarDates2D();

    else
      return new ArrayList<>();
  }

  @Override
  public CalendarDateRange getCalendarDateRange() {
    if (timeTaxis != null)
      return timeTaxis.getCalendarDateRange();

    else if (getRunTimeAxis() != null) {
      List<CalendarDate>  cd = makeCalendarDates2D();
      int last = cd.size();
      return (last > 0) ? CalendarDateRange.of(cd.get(0), cd.get(last-1)) : null;

    } else
      return null;
  }

  private List<CalendarDate> makeCalendarDates2D() {
    Set<CalendarDate> dates = new HashSet<>();

    CoordinateAxis1DTime rtaxis = getRunTimeAxis();
    List<CalendarDate> runtimes = rtaxis.getCalendarDates();
    for (int i = 0; i < runtimes.size(); i++) {
      CoordinateAxis1DTime taxis = getTimeAxisForRun(i);
      if (taxis == null) throw new IllegalStateException();
      List<CalendarDate> times = taxis.getCalendarDates();
      for (CalendarDate time : times) dates.add(time);
    }

    // sorted list
    int n = dates.size();
    CalendarDate[] dd = dates.toArray(new CalendarDate[n]);
    List<CalendarDate> dateList = Arrays.asList(dd);
    Collections.sort(dateList);

    return dateList;
  }



  //////////////////////////////////////////////////////////////////////////////////////
  // cruft

  /**
   * Get the list of level names, to be used for user selection.
   * The ith one refers to the ith level coordinate.
   *
   * @return List of ucar.nc2.util.NamedObject, or empty list.
   */
  public List<NamedObject> getLevels() {
    if (vertZaxis == null)
      return new ArrayList<>(0);

    int n = (int) vertZaxis.getSize();
    List<NamedObject> levels = new ArrayList<>(n);
    for (int i = 0; i < n; i++)
      levels.add(new ucar.nc2.util.NamedAnything(vertZaxis.getCoordName(i), vertZaxis.getUnitsString()));

    return levels;
  }

 /**
   * Get the String name for the ith level(z) coordinate.
   *
   * @param index which level coordinate
   * @return level name
   */
  public String getLevelName(int index) {
    if ((vertZaxis == null) || (index < 0) || (index >= vertZaxis.getSize()))
      throw new IllegalArgumentException("getLevelName = " + index);
    return vertZaxis.getCoordName(index).trim();
  }

  /**
   * Get the index corresponding to the level name.
   *
   * @param name level name
   * @return level index, or -1 if not found
   */
  public int getLevelIndex(String name) {
    if ((vertZaxis == null) || (name == null)) return -1;

    for (int i = 0; i < vertZaxis.getSize(); i++) {
      if (vertZaxis.getCoordName(i).trim().equals(name))
        return i;
    }
    return -1;
  }

  /**
   * Get the list of time names, to be used for user selection.
   * The ith one refers to the ith time coordinate.
   *
   * @return List of ucar.nc2.util.NamedObject, or empty list.
   */
  public List<NamedObject> getTimes() {
    List<CalendarDate> cdates = getCalendarDates();
    List<NamedObject> times = new ArrayList<>( cdates.size());
    for (CalendarDate cd: cdates) {
      times.add(new ucar.nc2.util.NamedAnything(cd.toString(), "calendar date"));
    }
    return times;
  }

   ///////////////////////////////////////////////////////////////////////////
  // deprecated

  /**
   * Given a point in x,y coordinate space, find the x,y index in the coordinate system.
   *
   * @deprecated use findXYindexFromCoord
   */
  public int[] findXYCoordElement(double x_coord, double y_coord, int[] result) {
    return findXYindexFromCoord(x_coord, y_coord, result);
  }

  /**
   * Get the date range
   * @return date range
   * @deprecated  use getCalendarDateRange
   */
  public DateRange getDateRange() {
    Date[] dates = getTimeDates();
    if (dates.length > 0)
      return new DateRange(dates[0], dates[dates.length - 1]);
    return null;
  }

  /**
   * Get the list of times as Dates.
   * If 2D, return list of unique dates.
   *
   * @return array of java.util.Date, or Date[0].
   * @deprecated  use getCalendarDates
   */
  public java.util.Date[] getTimeDates() {
    if ((timeTaxis != null) && (timeTaxis.getSize() > 0)) {
      return timeTaxis.getTimeDates();

    } else if ((tAxis != null) && (tAxis.getSize() > 0))  {
      return makeTimes2D();
    }

    return new Date[0];
  }

  private Date[] makeTimes2D() {
    Set<Date> dates = new HashSet<>();

    try {
      // common case: see if it has a valid udunits unit
      String units = tAxis.getUnitsString();
      if (units != null && SimpleUnit.isDateUnit(units) && tAxis.getDataType().isNumeric()) {
        DateUnit du = new DateUnit(units);
        Array data = tAxis.read();
        data.resetLocalIterator();
        while (data.hasNext()) {
          Date d = du.makeDate(data.nextDouble());
          dates.add(d);
        }

      } else if (tAxis.getDataType() == DataType.STRING) {
        // otherwise, see if its a String or CHAR, and if we can parse the values as an ISO date
        DateFormatter formatter = new DateFormatter();
        Array data = tAxis.read();
        data.resetLocalIterator();
        while (data.hasNext()) {
          Date d = formatter.getISODate((String) data.next());
          dates.add(d);
        }

      } else if (tAxis.getDataType() == DataType.CHAR) {
        DateFormatter formatter = new DateFormatter();
        ArrayChar data = (ArrayChar) tAxis.read();
        ArrayChar.StringIterator iter = data.getStringIterator();
        while (iter.hasNext()) {
          Date d = formatter.getISODate(iter.next());
          dates.add(d);
        }

      } else {
        return new Date[0];
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    // sorted list
    int n = dates.size();
    Date[] dd = dates.toArray(new Date[n]);
    List<Date> dateList = Arrays.asList(dd);
    Collections.sort(dateList);
    Date[] timeDates = new Date[n];
    int count=0;
    for (Date d : dateList)
      timeDates[count++] = d;

    return timeDates;
  }

  /* old way
  private boolean makeTimes1D() {
    int n = (int) timeTaxis.getSize();
    timeDates = new Date[n];

    // common case: see if it has a valid udunits unit
    try {
      DateUnit du = null;
      String units = timeTaxis.getUnitsString();
      if (units != null)
        du = new DateUnit(units);
      for (int i = 0; i < n; i++) {
        Date d = du.makeDate(timeTaxis.getCoordValue(i));
        timeDates[i] = d;
      }
      isDate = true;
      return true;
    } catch (Exception e) {
      // ok to fall through
    }

    // otherwise, see if its a String, and if we can parse the values as an ISO date
    if ((timeTaxis.getDataType() == DataType.STRING) || (timeTaxis.getDataType() == DataType.CHAR)) {
      DateFormatter formatter = new DateFormatter();
      for (int i = 0; i < n; i++) {
        String coordValue = timeTaxis.getCoordName(i);
        Date d = formatter.getISODate(coordValue);
        if (d == null) {
          isDate = false;
          return false;
        } else {
          timeDates[i] = d;
        }
      }
      isDate = true;
      return true;
    }

    return false;
  }  */



  /**
   * Get the string name for the ith time coordinate.
   *
   * @param index which time coordinate
   * @return time name.
   * @deprecated
   */
  public String getTimeName(int index) {
    List<CalendarDate> cdates = getCalendarDates();

    if ((index < 0) || (index >= cdates.size()))
      throw new IllegalArgumentException("getTimeName illegal index = " + index);

    return cdates.get(index).toString();
  }

  /**
   * Get the index corresponding to the time name.
   *
   * @param name time name
   * @return time index, or -1 if not found
   * @deprecated
   */
  public int getTimeIndex(String name) {
    List<CalendarDate> cdates = getCalendarDates();
    for (int i=0; i < cdates.size(); i++) {
      if( cdates.get(i).toString().equals(name)) return i;
    }

    return -1;
  }

  /**
   * Only works if coordsys has 1d time axis
   * @deprecated use CoordinateAxis1DTime.findTimeIndexFromDate
   */
  public int findTimeIndexFromDate(java.util.Date d) {
    if (timeTaxis == null) return -1;
    return timeTaxis.findTimeIndexFromDate(d);
  }

  ///////////////////////////////////////////////////////////////////////
  // experimental

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
      includesNorthPole = true;  */

    boolean includesSouthPole = false;
    /* int[] resultSP = new int[2];
    resultSP = findXYindexFromLatLon(-90.0, 0, null);
    if (resultSP[0] != -1 && resultSP[1] != -1)
      includesSouthPole = true;  */

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