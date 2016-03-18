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

import com.beust.jcommander.internal.Lists;
import ucar.nc2.Dimension;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.*;
import ucar.nc2.units.SimpleUnit;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.projection.RotatedPole;

import java.util.*;

/**
 * Coverage CS classification and builder, using ucar.nc2.dataset.CoordinateSystem
 *
 * @author caron
 * @since 5/26/2015
 */
public class DtCoverageCSBuilder {

  // classify based on largest coordinate system
  public static DtCoverageCSBuilder classify(NetcdfDataset ds, Formatter errlog) {
    if (errlog != null) errlog.format("CoverageFactory for '%s'%n", ds.getLocation());

    // sort by largest size first
    List<CoordinateSystem> css = new ArrayList<>(ds.getCoordinateSystems());
    Collections.sort(css, (o1, o2) -> o2.getCoordinateAxes().size() - o1.getCoordinateAxes().size());

    DtCoverageCSBuilder builder = null;
    for (CoordinateSystem cs : css) {
      builder = new DtCoverageCSBuilder(ds, cs, errlog);
      if (builder.type != null) break;
    }
    if (builder == null) return null;
    if (errlog != null) errlog.format("coverage = %s%n", builder.type);
    return builder;
  }

  public static String describe(NetcdfDataset ds, Formatter errlog) {
    DtCoverageCSBuilder fac = classify(ds, errlog);
    return (fac == null || fac.type == null) ? "" : fac.showSummary();
  }

  public static String describe(NetcdfDataset ds, CoordinateSystem cs, Formatter errlog) {
    DtCoverageCSBuilder fac = new DtCoverageCSBuilder(ds, cs, errlog);
    return fac.type == null ? "" : fac.showSummary();
  }

  ////////////////////////////////////////////////////////////////////////////////////
  FeatureType type;

  boolean isLatLon;
  CoordinateAxis xaxis, yaxis, timeAxis;
  CoordinateAxis1D vertAxis, ensAxis, timeOffsetAxis;
  CoordinateAxis1DTime rtAxis;
  List<CoordinateAxis> independentAxes;
  List<CoordinateAxis> otherAxes;
  List<CoordinateAxis> allAxes;
  List<CoordinateTransform> coordTransforms;
  ProjectionImpl orgProj;

  DtCoverageCSBuilder(NetcdfDataset ds, CoordinateSystem cs, Formatter errlog) {

    // must be at least 2 dimensions
    if (cs.getRankDomain() < 2) {
      if (errlog != null) errlog.format("CoordinateSystem '%s': domain rank < 2%n", cs.getName());
      return;
    }

    //////////////////////////////////////////////////////////////
    // horiz
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
      isLatLon = true;
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
    int xyDomainSize = CoordinateSystem.countDomain(new CoordinateAxis[]{xaxis, yaxis});
    if (xyDomainSize < 2) {
      if (errlog != null) errlog.format("%s: X and Y axis must have 2 or more dimensions%n", cs.getName());
      return;
    }

    allAxes = new ArrayList<>(cs.getCoordinateAxes());
    Collections.sort(allAxes, new CoordinateAxis.AxisComparator()); // canonical ordering of axes

    independentAxes = new ArrayList<>();
    otherAxes = new ArrayList<>();
    for (CoordinateAxis axis : cs.getCoordinateAxes()) {
      // skip x,y if no projection
      if ((axis.getAxisType() == AxisType.GeoX || axis.getAxisType() == AxisType.GeoY) && isLatLon) continue;
      if (axis.isIndependentCoordinate()) independentAxes.add(axis);
      else otherAxes.add(axis);
    }
    Collections.sort(independentAxes, (o1, o2) -> {
      AxisType t1 = o1.getAxisType();
      AxisType t2 = o2.getAxisType();
      if (t1 != null && t2 != null)
        return t1.axisOrder() - t2.axisOrder();
      return (t1 == null) ? ((t2 == null) ? 0 : -1) : 1;
    });

    //////////////////////////////////////////////////////////////
    // vert
    CoordinateAxis zAxis = cs.getHeightAxis();
    if ((zAxis == null) || (zAxis.getRank() > 1)) {
      if (cs.getPressureAxis() != null) zAxis = cs.getPressureAxis();
    }
    if ((zAxis == null) || (zAxis.getRank() > 1)) {
      if (cs.getZaxis() != null) zAxis = cs.getZaxis();
    }
    if (zAxis != null) {
      if (zAxis instanceof CoordinateAxis1D)
        vertAxis = (CoordinateAxis1D) zAxis;
    }

    //////////////////////////////////////////////////////////////
    // time
    CoordinateAxis rt = cs.findAxis(AxisType.RunTime);
    if (rt != null) {
      if (!rt.isScalar() && !(rt instanceof CoordinateAxis1D)) {   // A runtime axis must be scalar or one-dimensional
        if (errlog != null) errlog.format("%s: RunTime axis must be 1D or scalar%n", cs.getName());
        return;
      }
      if (!(rt instanceof CoordinateAxis1DTime)) {    // convert to CoordinateAxis1DTime
        try {
          rtAxis = CoordinateAxis1DTime.factory(ds, rt, errlog);
          int index = allAxes.indexOf(rt); // replace
          allAxes.set(index, rtAxis);
        } catch (Exception e) {
          if (errlog != null)
            errlog.format("%s: Error reading runtime coord= %s err= %s%n", rt.getDatasetLocation(), rt.getFullName(), e.getMessage());
          return;
        }
      } else {
        rtAxis = (CoordinateAxis1DTime) rt;
      }
    }

    CoordinateAxis t = cs.getTaxis();
    if ((t != null) && t.getRank() > 1) {  // If time axis is two-dimensional...
      if (rtAxis != null && rtAxis.getRank() == 1) {
        // time first dimension must agree with runtime
        if (!rtAxis.getDimension(0).equals(t.getDimension(0))) {
          if (errlog != null) errlog.format("%s: 2D Time axis first dimension must be runtime%n", cs.getName());
          return;
        }
      }
    }

    if (t != null) {
      if (t instanceof CoordinateAxis1D && !(t instanceof CoordinateAxis1DTime)) {  // convert time axis into CoordinateAxis1DTime if possible
        try {
          timeAxis = CoordinateAxis1DTime.factory(ds, t, errlog);
          int index = allAxes.indexOf(t); // replace
          allAxes.set(index, timeAxis);
        } catch (Exception e) {
          if (errlog != null)
            errlog.format("%s: Error reading time coord= %s err= %s%n", t.getDatasetLocation(), t.getFullName(), e.getMessage());
          return;
        }
      } else {
        timeAxis = t;
      }
    }

    CoordinateAxis toAxis = cs.findAxis(AxisType.TimeOffset);
    if (toAxis != null) {
      if (toAxis.getRank() == 1)
        timeOffsetAxis = (CoordinateAxis1D) toAxis;
    }

    if (t == null && rtAxis != null && timeOffsetAxis != null) {
      // LOOK create time coord ??
    }

    CoordinateAxis eAxis = cs.findAxis(AxisType.Ensemble);
    if (eAxis != null) {
      if (eAxis instanceof CoordinateAxis1D)
        ensAxis = (CoordinateAxis1D) eAxis;
    }

    this.type = classify();
    this.coordTransforms = new ArrayList<>(cs.getCoordinateTransforms());
    this.orgProj = cs.getProjection();
  }

  private FeatureType classify () {

    // now to classify
    boolean is2Dtime = (rtAxis != null) && (timeOffsetAxis != null || (timeAxis != null && timeAxis.getRank() == 2));
    if (is2Dtime) {
      return FeatureType.FMRC;   // LOOK this would allow 2d horiz
    }

    boolean is2Dhoriz = isLatLon && (xaxis.getRank() == 2) && (yaxis.getRank() == 2);
    if (is2Dhoriz) {
      Set<Dimension> xyDomain = CoordinateSystem.makeDomain(Lists.newArrayList(xaxis, yaxis));
      if (timeAxis != null && CoordinateSystem.isSubset(timeAxis.getDimensionsAll(), xyDomain))
        return FeatureType.SWATH;   // LOOK prob not exactly right
      else
        return FeatureType.CURVILINEAR;
    }

    // what makes it a grid?
    // each dimension must have its own coordinate variable
    Set<Dimension> indDimensions = CoordinateSystem.makeDomain(independentAxes);
    Set<Dimension> allDimensions = CoordinateSystem.makeDomain(allAxes);
    if (indDimensions.size() == allDimensions.size()) {
      return FeatureType.GRID;
    }

    // default
    return FeatureType.COVERAGE;
  }

  public FeatureType getCoverageType() {
    return type;
  }

  public DtCoverageCS makeCoordSys() {
    if (type == null) return null;

    switch (type) {
      case GRID:
        return new GridCS(this);
      case FMRC:
        return new FmrcCS(this);
      case CURVILINEAR:
        return new CurvilinearCS(this);
      case SWATH:
        return new SwathCS(this);
    }
    return new DtCoverageCS(this);
  }

  @Override
  public String toString() {
    Formatter f2 = new Formatter();
    f2.format("%s", type == null ? "" : type.toString());
    f2.format("%n xAxis=  %s", xaxis == null ? "" : xaxis.getNameAndDimensions());
    f2.format("%n yAxis=  %s", yaxis == null ? "" : yaxis.getNameAndDimensions());
    f2.format("%n zAxis=  %s", vertAxis == null ? "" : vertAxis.getNameAndDimensions());
    f2.format("%n tAxis=  %s", timeAxis == null ? "" : timeAxis.getNameAndDimensions());
    f2.format("%n rtAxis= %s", rtAxis == null ? "" : rtAxis.getNameAndDimensions());
    f2.format("%n toAxis= %s", timeOffsetAxis == null ? "" : timeOffsetAxis.getNameAndDimensions());
    f2.format("%n ensAxis=%s", ensAxis == null ? "" : ensAxis.getNameAndDimensions());
    if (type == null) return f2.toString();

    f2.format("%n%n independentAxes=(");
    for (CoordinateAxis axis : independentAxes)
      f2.format("%s, ", axis.getShortName());
    f2.format(") {");
    for (Dimension dim : CoordinateSystem.makeDomain(independentAxes))
      f2.format("%s, ", dim.getShortName());
    f2.format("}");
    f2.format("%n otherAxes=(");
    for (CoordinateAxis axis : otherAxes)
      f2.format("%s, ", axis.getShortName());
    f2.format(") {");
    for (Dimension dim : CoordinateSystem.makeDomain(otherAxes))
      f2.format("%s, ", dim.getShortName());
    f2.format("}");
    f2.format("%n allAxes=(");
    for (CoordinateAxis axis : allAxes)
      f2.format("%s, ", axis.getShortName());
    f2.format(") {");
    for (Dimension dim : CoordinateSystem.makeDomain(allAxes))
      f2.format("%s, ", dim.getShortName());
    f2.format("}%n");

    return f2.toString();
  }

  public String showSummary() {
    if (type == null) return "";

    Formatter f2 = new Formatter();
    f2.format("%s", type.toString());

    f2.format("(");
    int count = 0;
    for (CoordinateAxis axis : independentAxes) {
      if (count++ > 0) f2.format(",");
      f2.format("%s", axis.getAxisType() == null ? axis.getShortName() : axis.getAxisType().getCFAxisName());
    }
    f2.format(")");

    if (otherAxes.size() > 0) {
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
