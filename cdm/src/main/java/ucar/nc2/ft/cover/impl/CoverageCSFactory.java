package ucar.nc2.ft.cover.impl;

import ucar.nc2.Dimension;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.*;
import ucar.nc2.ft.cover.CoverageCS;
import ucar.nc2.units.SimpleUnit;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.projection.RotatedPole;

import java.util.*;

/**
 * Factory for Coverage Coordinate Systems.
 * Replaces complexity in ucar.nc2.dt.grid.GridCoordSys.isGridCoordSys()
 *
 * @author John
 * @since 12/23/12
 */
public class CoverageCSFactory {

  public static CoverageCS make(NetcdfDataset ds, CoordinateSystem cs, Formatter errlog) {
    CoverageCSFactory fac = new CoverageCSFactory();
    fac.type = fac.classify(ds, cs, errlog);
    if (fac.type == null) return null;

    switch (fac.type) {
      case Curvilinear:
      case Coverage:
        return new CoverageCSImpl(ds, cs, fac);
      case Grid:
        return new GridCSImpl(ds, cs, fac);
      case Fmrc:
        return new FmrcCSImpl(ds, cs, fac);
      case Swath:
        return new CurvilinearCSImpl(ds, cs, fac);
    }
    return null;
  }

  public static String describe(Formatter f, NetcdfDataset ds) {
    CoverageCSFactory fac = new CoverageCSFactory();
    fac.type = fac.classify(ds, f);
    return fac.toString();
  }

  public static String describe(Formatter f, CoordinateSystem cs) {
    CoverageCSFactory fac = new CoverageCSFactory();
    fac.type = fac.classify(null, cs, f);
    return fac.toString();
  }

  /////////

  //NetcdfDataset ds;
  CoverageCS.Type type;
  CoordinateAxis vertAxis;
  CoordinateAxis timeAxis;
  List<CoordinateAxis> standardAxes;
  List<CoordinateAxis> otherAxes;

  CoverageCSFactory() {
  }

  CoverageCS.Type classify(NetcdfDataset ds, Formatter errlog) {
    if (errlog != null) errlog.format("CoverageFactory for '%s'%n", ds.getLocation());

    // sort by largest size first
    List<CoordinateSystem> css = new ArrayList<>(ds.getCoordinateSystems());
    Collections.sort(css, new Comparator<CoordinateSystem>() {
      public int compare(CoordinateSystem o1, CoordinateSystem o2) {
        return o2.getCoordinateAxes().size() - o1.getCoordinateAxes().size();
      }
    });

    CoverageCS.Type isMine = null;
    for (CoordinateSystem cs : css) {
      isMine = classify(ds, cs, errlog);
      if (isMine != null) break;
    }
    if (errlog != null) errlog.format("coverage = %s%n", isMine);
    return isMine;
  }


  CoverageCS.Type classify(NetcdfDataset ds, CoordinateSystem cs, Formatter errlog) {
    // must be at least 2 axes
    if (cs.getRankDomain() < 2) {
      if (errlog != null) errlog.format("CoordinateSystem '%s': domain rank < 2%n", cs.getName());
      return null;
    }

    // must be lat/lon or have x,y and projection
    if (!cs.isLatLon()) {
      // do check for GeoXY
      if ((cs.getXaxis() == null) || (cs.getYaxis() == null)) {
        if (errlog != null) errlog.format("%s: NO Lat,Lon or X,Y axis%n", cs.getName());
        return null;
      }
      if (null == cs.getProjection()) {
        if (errlog != null) errlog.format("%s: NO projection found%n", cs.getName());
        return null;
      }
    }

    // obtain the x,y or lat/lon axes. x,y normally must be convertible to km
    CoordinateAxis xaxis, yaxis;
    if (cs.isGeoXY()) {
      xaxis = cs.getXaxis();
      yaxis = cs.getYaxis();

      ProjectionImpl p = cs.getProjection();
      if (!(p instanceof RotatedPole)) {
        if (!SimpleUnit.kmUnit.isCompatible(xaxis.getUnitsString())) {
          if (errlog != null) errlog.format("%s: X axis units are not convertible to km%n", cs.getName());
          //return false;
        }
        if (!SimpleUnit.kmUnit.isCompatible(yaxis.getUnitsString())) {
          if (errlog != null) errlog.format("%s: Y axis units are not convertible to km%n", cs.getName());
          //return false;
        }
      }
    } else {
      xaxis = cs.getLonAxis();
      yaxis = cs.getLatAxis();
    }

    // check x,y rank <= 2
    if ((xaxis.getRank() > 2) || (yaxis.getRank() > 2)) {
      if (errlog != null) errlog.format("%s: X and Y axis rank must be <= 2%n", cs.getName());
      return null;
    }

    // check x,y with size 1
    if ((xaxis.getSize() < 2) || (yaxis.getSize() < 2)) {
      if (errlog != null) errlog.format("%s: X and Y axis size must be >= 2%n", cs.getName());
      return null;
    }

    // check that the x,y have at least 2 dimensions between them ( this eliminates point data)
    List<Dimension> xyDomain = CoordinateSystem.makeDomain(new CoordinateAxis[]{xaxis, yaxis});
    if (xyDomain.size() < 2) {
      if (errlog != null) errlog.format("%s: X and Y axis must have 2 or more dimensions%n", cs.getName());
      return null;
    }

    standardAxes = new ArrayList<>();
    standardAxes.add(xaxis);
    standardAxes.add(yaxis);

    //int countRangeRank = 2;

    vertAxis = cs.getHeightAxis();
    if ((vertAxis == null) || (vertAxis.getRank() > 1)) {
      if (cs.getPressureAxis() != null) vertAxis = cs.getPressureAxis();
    }
    if ((vertAxis == null) || (vertAxis.getRank() > 1)) {
      if (cs.getZaxis() != null) vertAxis = cs.getZaxis();
    }
    if (vertAxis != null)
      standardAxes.add(vertAxis);

    // tom margolis 3/2/2010
    // allow runtime independent of time
    CoordinateAxis t = cs.getTaxis();
    CoordinateAxis rt = cs.findAxis(AxisType.RunTime);

    // A runtime axis must be scalar or one-dimensional
    if (rt != null) {
      if (!rt.isScalar() && !(rt instanceof CoordinateAxis1D)) {
        if (errlog != null) errlog.format("%s: RunTime axis must be 1D or scalar%n", cs.getName());
        return null;
      }
      // LOOK turn it into 1D ??
    }

    // If time axis is two-dimensional...
    if ((t != null) && !(t instanceof CoordinateAxis1D) && (t.getRank() != 0)) {

      if (rt != null) {
        if (rt.getRank() != 1) { // LOOK turn it into 1D ??
          if (errlog != null) errlog.format("%s: Runtime axis must be 1D%n", cs.getName());
          return null;
        }

        // time first dimension must agree with runtime
        if (!rt.getDimension(0).equals(t.getDimension(0))) {
          if (errlog != null) errlog.format("%s: 2D Time axis first dimension must be runtime%n", cs.getName());
          return null;
        }
      }
    }

    // convert time axis if possible
    if (ds != null && t != null) {   // LOOK can ds really be null ??

      if (t instanceof CoordinateAxis1D) {

        try {
          if (t instanceof CoordinateAxis1DTime)
            timeAxis = t;
          else {
            t = timeAxis = CoordinateAxis1DTime.factory(ds, t, errlog);
          }

        } catch (Exception e) {
          if (errlog != null)
            errlog.format("%s: Error reading time coord= %s err= %s%n", t.getDatasetLocation(), t.getFullName(), e.getMessage());
        }

      } else { // 2d
        timeAxis = t;
      }
    }

    // Set the primary temporal axis - either Time or Runtime LOOK
    if (t != null) {
      standardAxes.add(t);
    } else if (rt != null) {
      standardAxes.add(rt);
    }

    // construct list of non standard axes
    List<CoordinateAxis> css = cs.getCoordinateAxes();
    if (standardAxes.size() < css.size()) {
      otherAxes = new ArrayList<>(3);
      for (CoordinateAxis axis : css)
        if (!standardAxes.contains(axis)) otherAxes.add(axis);
    }

    // now to classify
    CoverageCS.Type result = null;

    // 2D x,y
    if (cs.isLatLon() && (xaxis.getRank() == 2) && (yaxis.getRank() == 2)) {
      if ((rt != null) && (t != null && t.getRank() == 2))  // fmrc with curvilinear coordinates
        result = CoverageCS.Type.Fmrc;

      else if (t != null) {  // is t independent or not
        if (CoordinateSystem.isSubset(t.getDimensionsAll(), xyDomain))
          result = CoverageCS.Type.Swath;
        else
          result = CoverageCS.Type.Curvilinear;
      } else
        result = CoverageCS.Type.Curvilinear;   // if no time coordinate. call it curvilinear

    } else {
      if ((xaxis.getRank() == 1) && (yaxis.getRank() == 1) && (vertAxis == null || vertAxis.getRank() == 1)) {
        if ((rt != null) && (t != null && t.getRank() == 2))
          result = CoverageCS.Type.Fmrc;
        else
          result = CoverageCS.Type.Grid;
      } else {
        result = CoverageCS.Type.Coverage;
      }
    }

    return result;
  }

  public String toString() {
    if (type == null) return "";
    Formatter f2 = new Formatter();
    f2.format("%s", type);

    f2.format("(");
    int count = 0;
    Collections.sort(standardAxes, new Comparator<CoordinateAxis>() {  // sort by axis type
      public int compare(CoordinateAxis o1, CoordinateAxis o2) {
        AxisType t1 = o1.getAxisType();
        AxisType t2 = o2.getAxisType();
        if (t1 != null && t2 != null)
          return t1.axisOrder() - t2.axisOrder();
        return (t1 == null) ? ((t2 == null) ? 0 : -1) : 1;
      }
    });
    for (CoordinateAxis axis : standardAxes) {
      if (count++ > 0) f2.format(",");
      f2.format("%s", axis.getAxisType() == null ? "none" : axis.getAxisType().getCFAxisName());
    }
    f2.format(")");

    if (otherAxes != null && otherAxes.size() > 0) {
      f2.format(": ");
      count = 0;
      for (CoordinateAxis axis : otherAxes) {
        if (count++ > 0) f2.format(",");
        f2.format("%s", axis.getShortName());
      }
    }
    return f2.toString();
  }

}
