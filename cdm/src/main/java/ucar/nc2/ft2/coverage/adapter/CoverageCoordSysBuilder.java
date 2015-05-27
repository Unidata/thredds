/* Copyright */
package ucar.nc2.ft2.coverage.adapter;

import ucar.nc2.Dimension;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.*;
import ucar.nc2.ft2.coverage.grid.GridCoordSys;
import ucar.nc2.units.SimpleUnit;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.projection.RotatedPole;

import java.util.*;

/**
 * Describe
 *
 * @author caron
 * @since 5/26/2015
 */
public class CoverageCoordSysBuilder {

  // classify based on largest coordinate system
  public static CoverageCoordSysBuilder classify(NetcdfDataset ds, Formatter errlog) {
    if (errlog != null) errlog.format("CoverageFactory for '%s'%n", ds.getLocation());

    // sort by largest size first
    List<CoordinateSystem> css = new ArrayList<>(ds.getCoordinateSystems());
    Collections.sort(css, new Comparator<CoordinateSystem>() {
      public int compare(CoordinateSystem o1, CoordinateSystem o2) {
        return o2.getCoordinateAxes().size() - o1.getCoordinateAxes().size();
      }
    });

    CoverageCoordSysBuilder fac = null;
    for (CoordinateSystem cs : css) {
      fac = new CoverageCoordSysBuilder(ds, cs, errlog);
      if (fac.type != null) break;
    }
    if (fac == null) return null;
    if (errlog != null) errlog.format("coverage = %s%n", fac.type);
    return fac;
  }

  public static String describe(Formatter f, NetcdfDataset ds) {
    CoverageCoordSysBuilder fac = classify(ds, f);
    return fac.type == null ? "" : fac.toString();
  }

  ////////////////////////////
  GridCoordSys.Type type;
  CoordinateAxis vertAxis;
  CoordinateAxis timeAxis;
  List<CoordinateAxis> standardAxes;
  List<CoordinateAxis> otherAxes;

  CoverageCoordSysBuilder(NetcdfDataset ds, CoordinateSystem cs, Formatter errlog) {

    // must be at least 2 axes
    if (cs.getRankDomain() < 2) {
      if (errlog != null) errlog.format("CoordinateSystem '%s': domain rank < 2%n", cs.getName());
      return;
    }

    // must be lat/lon or have x,y and projection
    if (!cs.isLatLon()) {
      // do check for GeoXY
      if ((cs.getXaxis() == null) || (cs.getYaxis() == null)) {
        if (errlog != null) errlog.format("%s: NO Lat,Lon or X,Y axis%n", cs.getName());
        return;
      }
      if (null == cs.getProjection()) {
        if (errlog != null) errlog.format("%s: NO projection found%n", cs.getName());
        return;
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
      return;
    }

    // check x,y with size 1
    if ((xaxis.getSize() < 2) || (yaxis.getSize() < 2)) {
      if (errlog != null) errlog.format("%s: X and Y axis size must be >= 2%n", cs.getName());
      return;
    }

    // check that the x,y have at least 2 dimensions between them ( this eliminates point data)
    List<Dimension> xyDomain = CoordinateSystem.makeDomain(new CoordinateAxis[]{xaxis, yaxis});
    if (xyDomain.size() < 2) {
      if (errlog != null) errlog.format("%s: X and Y axis must have 2 or more dimensions%n", cs.getName());
      return;
    }

    standardAxes = new ArrayList<>();
    standardAxes.add(xaxis);
    standardAxes.add(yaxis);

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
        return;
      }
      // LOOK turn it into 1D ??
    }

    // If time axis is two-dimensional...
    if ((t != null) && !(t instanceof CoordinateAxis1D) && (t.getRank() != 0)) {

      if (rt != null && rt.getRank() == 1) {
        // time first dimension must agree with runtime
        if (!rt.getDimension(0).equals(t.getDimension(0))) {
          if (errlog != null) errlog.format("%s: 2D Time axis first dimension must be runtime%n", cs.getName());
          return;
        }
      }
    }

    // convert time axis if possible
    if (t != null) {

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

    // Set the independent temporal axis LOOK not right
    if (t != null && t.isCoordinateVariable()) {
      standardAxes.add(t);
    } else if (rt != null && rt.isCoordinateVariable()) {
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

    // 2D x,y
    if (cs.isLatLon() && (xaxis.getRank() == 2) && (yaxis.getRank() == 2)) {
      if ((rt != null) && (t != null && t.getRank() == 2))  // fmrc with curvilinear coordinates
        this.type = GridCoordSys.Type.Fmrc;

      else if (t != null) {  // is t independent or not
        if (CoordinateSystem.isSubset(t.getDimensionsAll(), xyDomain))
          this.type = GridCoordSys.Type.Swath;
        else
          this.type = GridCoordSys.Type.Curvilinear;
      } else
        this.type = GridCoordSys.Type.Curvilinear;   // if no time coordinate. call it curvilinear

    } else {
      if ((xaxis.getRank() == 1) && (yaxis.getRank() == 1) && (vertAxis == null || vertAxis.getRank() == 1)) {
        if ((rt != null) && (t != null && t.getRank() == 2))
          this.type = GridCoordSys.Type.Fmrc;
        else
          this.type = GridCoordSys.Type.Grid;
      } else {
        this.type = GridCoordSys.Type.Coverage;
      }
    }
  }

  public CoverageCoordSys makeCoordSys(CoordinateSystem cs) {
    switch (type) {
      case Grid:
        return new GridCS(this, cs);
      case Fmrc:
        return new FmrcCS(this, cs);
      case Curvilinear:
        return new CurvilinearCS(this, cs);
      case Swath:
        return new SwathCS(this, cs);
    }

    return new CoverageCoordSys(this, cs);
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
