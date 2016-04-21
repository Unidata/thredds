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
package ucar.nc2.ft2.coverage;

import ucar.ma2.RangeIterator;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.util.*;
import ucar.nc2.util.Optional;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.projection.LatLonProjection;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A Coverage Coordinate System
 * Immutable after setImmutable is called
 *
 * @author caron
 * @since 7/11/2015
 */
public class CoverageCoordSys {

  public static String makeCoordSysName(List<String> axisName) {
    Formatter fname = new Formatter();
    for (String axis : axisName)
      fname.format(" %s", axis);
    return fname.toString();
  }

  //////////////////////////////////////////////////
  protected CoordSysContainer dataset;  // almost immutable, need to wire these in after the constructor
  private HorizCoordSys horizCoordSys;  // required
  private Time2DCoordSys time2DCoordSys;// optional
  private Map<String, List<CoverageCoordAxis>> dependentMap;
  private boolean isConstantForecast, immutable;

  private final String name;
  private final List<String> axisNames;        // must be in order
  private final List<String> transformNames;
  private final FeatureType type;

  public CoverageCoordSys(String name, List<String> axisNames, List<String> transformNames, FeatureType type) {
    this.name = name != null ? name : makeCoordSysName(axisNames);
    this.axisNames = axisNames;
    this.transformNames = transformNames;
    this.type = type;
  }

  // copy constructor
  public CoverageCoordSys(CoverageCoordSys from) {
    this.name = from.getName();
    this.axisNames = from.getAxisNames();
    this.transformNames = from.getTransformNames();
    this.type = from.getCoverageType();
  }

  public boolean isConstantForecast() {
    return isConstantForecast;
  }

  void setIsConstantForecast(boolean isConstantForecast) {
    if (immutable)
      throw new RuntimeException("Cant change CoverageCoordSys dataset once set immutable");
    this.isConstantForecast = isConstantForecast;
  }

  void setImmutable() {
    this.immutable = true;
  }

  public void setDataset(CoordSysContainer dataset) {
    if (immutable)
      throw new RuntimeException("Cant change CoverageCoordSys dataset once set immutable");
    this.dataset = dataset;

    // find dependent axes
    dependentMap = new HashMap<>();
    for (CoverageCoordAxis axis : getAxes()) {
      if (axis.getDependenceType() == CoverageCoordAxis.DependenceType.dependent) {
        for (String indAxisName : axis.dependsOn) {
          CoverageCoordAxis independentAxis = dataset.findCoordAxis(indAxisName);
          if (independentAxis == null)
            throw new RuntimeException("Dependent axis " + axis.getName() + " depends on " + indAxisName + " not in Dataset");
          if (!axisNames.contains(indAxisName))
            throw new RuntimeException("Dependent axis " + axis.getName() + " depends on " + indAxisName + " not in CoordSys");

          List<CoverageCoordAxis> dependents = dependentMap.get(indAxisName);
          if (dependents == null) {
            dependents = new ArrayList<>();
            dependentMap.put(indAxisName, dependents);
          }
          dependents.add(axis);
        }
      }
    }

    // see if we need a Time2DCoordSys
    TimeAxis2DFmrc time2DAxis = null;
    TimeOffsetAxis timeOffsetAxis = null;
    CoverageCoordAxis1D runtimeAxis = null;

    for (CoverageCoordAxis axis : getAxes()) {
      if (axis.getAxisType() == AxisType.TimeOffset) {
        if (timeOffsetAxis != null)
          throw new RuntimeException("Cant have multiple TimeOffset Axes in a CoverageCoordSys");
        if (axis instanceof TimeOffsetAxis)
          timeOffsetAxis = (TimeOffsetAxis) axis;
      }
      if (axis.getAxisType() == AxisType.RunTime) {
        if (runtimeAxis != null)
          throw new RuntimeException("Cant have multiple RunTime axes in a CoverageCoordSys");
        runtimeAxis = (CoverageCoordAxis1D) axis;
      }
      if (axis instanceof TimeAxis2DFmrc) {
        if (time2DAxis != null)
          throw new RuntimeException("Cant have multiple TimeAxis2D axes in a CoverageCoordSys");
        time2DAxis = (TimeAxis2DFmrc) axis;
      }
    }

    // LOOK would be better maybe to share time2DCoordSys across CoordSys
    if (timeOffsetAxis != null) {
      if (runtimeAxis == null)
        throw new RuntimeException("TimeOffset Axis must have a RunTime axis in a CoverageCoordSys");
      if (immutable && this.time2DCoordSys != null)
        throw new RuntimeException("Cant have multiple Time2DCoordSys in a CoverageCoordSys");
      time2DCoordSys = new Time2DOffsetCoordSys(runtimeAxis, timeOffsetAxis);
    }

    if (time2DAxis != null) {
      if (runtimeAxis == null)
        throw new RuntimeException("TimeAxis2D Axis must have a RunTime axis in a CoverageCoordSys");
      if (immutable && this.time2DCoordSys != null)
        throw new RuntimeException("Cant have multiple Time2DCoordSys in a CoverageCoordSys");
      time2DCoordSys = new Time2DCoordSys(runtimeAxis, time2DAxis);
    }
  }

  public void setHorizCoordSys(HorizCoordSys horizCoordSys) {
    if (immutable) throw new RuntimeException("Cant change CoverageCoordSys horizCoordSys once set immutable");
    this.horizCoordSys = horizCoordSys;
  }

  public HorizCoordSys makeHorizCoordSys() {
    CoverageCoordAxis xaxis = getAxis(AxisType.GeoX);
    CoverageCoordAxis yaxis = getAxis(AxisType.GeoY);
    CoverageCoordAxis lataxis = getAxis(AxisType.Lat);
    CoverageCoordAxis lonaxis = getAxis(AxisType.Lon);

    CoverageTransform hct = getHorizTransform();
    return HorizCoordSys.factory((CoverageCoordAxis1D) xaxis, (CoverageCoordAxis1D) yaxis, lataxis, lonaxis, hct);
  }

  ///////////////////////////////////////////////

  public String getName() {
    return name;
  }

  public List<String> getTransformNames() {
    return (transformNames != null) ? transformNames : new ArrayList<>();
  }

  public List<CoverageTransform> getTransforms() {
    List<CoverageTransform> result = new ArrayList<>();
    for (String name : getTransformNames()) {
      result.add(dataset.findCoordTransform(name));
    }
    return result;
  }

  public CoverageTransform getHorizTransform() {
    for (String name : getTransformNames()) {
      CoverageTransform ct = dataset.findCoordTransform(name);
      if (ct != null && ct.isHoriz()) return ct;
    }
    return null;
  }

  public HorizCoordSys getHorizCoordSys() {
    return horizCoordSys;
  }

  public FeatureType getCoverageType() {
    return type;
  }

  public List<String> getAxisNames() {
    return axisNames;
  }

  @Override
  public String toString() {
    Formatter f = new Formatter();
    Indent indent = new Indent(2);
    toString(f, indent);
    return f.toString();
  }

  public void toString(Formatter f, Indent indent) {
    indent.incr();
    f.format("%s CoordSys '%s' type %s", indent, name, type);
    f.format(" has coordVars:");
    for (String v : axisNames)
      f.format("%s, ", v);
    f.format(" (shape=[%s])", Misc.showInts(getShape()));
    if (transformNames != null && !transformNames.isEmpty()) {
      f.format("; has transforms:");
      for (String t : transformNames)
        f.format("%s, ", t);
    }
    indent.decr();
  }

  ////////////////////////////////////////////////////////////////////

  public CoverageCoordAxis getXAxis() {
    CoverageCoordAxis result = getAxis(AxisType.GeoX);
    if (result == null)
      result = getAxis(AxisType.Lon);
    if (result == null)
      throw new IllegalArgumentException("Cant find X axis for coordsys " + getName());
    return result;
  }

  public CoverageCoordAxis getYAxis() {
    CoverageCoordAxis result = getAxis(AxisType.GeoY);
    if (result == null)
      result = getAxis(AxisType.Lat);
    if (result == null)
      throw new IllegalArgumentException("Cant find Y axis for coordsys " + getName());
    return result;
  }

  public CoverageCoordAxis getZAxis() {
    for (String axisName : getAxisNames()) {
      CoverageCoordAxis axis = dataset.findCoordAxis(axisName);
      if (axis == null)
        throw new IllegalStateException("Cant find axis with name "+axisName);
      if (axis.getAxisType() == AxisType.GeoZ || axis.getAxisType() == AxisType.Height || axis.getAxisType() == AxisType.Pressure)
        return axis;
    }
    return null;
  }

  public CoverageCoordAxis getTimeAxis() {
    CoverageCoordAxis result = getAxis(AxisType.Time);
    if (result == null)
      result = getAxis(AxisType.TimeOffset);
    return result;
  }

  public CoverageCoordAxis getAxis(AxisType type) {
    for (String axisName : getAxisNames()) {
      CoverageCoordAxis axis = dataset.findCoordAxis(axisName);
      if (axis == null)
        throw new IllegalStateException("Cant find axis with name "+axisName);
      else if (axis.getAxisType() == type) {
        return axis;
      }
    }
    return null;
  }

  public CoverageCoordAxis getAxis(String axisName) {
    return dataset.findCoordAxis(axisName);
  }

  public List<CoverageCoordAxis> getAxes() {
    List<CoverageCoordAxis> result = new ArrayList<>();
    for (String axisName : getAxisNames()) {
      CoverageCoordAxis axis = dataset.findCoordAxis(axisName);
      if (axis == null)
        throw new IllegalStateException("Cant find " + axisName);
      result.add(axis);
    }
    return result;
  }

  /**
   * Using independent axes only.
   * Note this depends on order of axes.
   */
  public int[] getShape() {
    int rank = 2; // always 2 horiz
    if (time2DCoordSys != null)
      rank += time2DCoordSys.getShape().length; // might have scalar runtime

    for (CoverageCoordAxis axis : getAxes()) {
      if (axis.getAxisType().isHoriz()) continue;
      if (isTime2D(axis)) continue;
      if (axis.getDependenceType() == CoverageCoordAxis.DependenceType.independent) rank++;
    }

    int[] result = new int[rank];
    int count = 0;
    if (time2DCoordSys != null) {
      int[] timeShape = time2DCoordSys.getShape();
      System.arraycopy(timeShape, 0, result, count, timeShape.length);
      count += timeShape.length;
    }

    for (CoverageCoordAxis axis : getAxes()) {
      if (axis.getAxisType().isHoriz()) continue;
      if (isTime2D(axis)) continue;
      if (axis.getDependenceType() == CoverageCoordAxis.DependenceType.independent)
        result[count++] = axis.getNcoords();
    }

    // the x,y shapes must be gotten from horizCoordSys
    for (RangeIterator ri : horizCoordSys.getRanges())
      result[count++] = ri.length();

    return result;
  }

  public List<RangeIterator> getRanges() {
    List<RangeIterator> result = new ArrayList<>();
    for (CoverageCoordAxis axis : getAxes()) {
      if (axis.getAxisType().isHoriz()) continue;
      if (axis.getDependenceType() == CoverageCoordAxis.DependenceType.independent)
        result.add(axis.getRangeIterator());
    }

    result.addAll(horizCoordSys.getRanges()); // may be 2D
    return result;
  }


  public boolean isRegularSpatial() {
    CoverageCoordAxis xaxis = getXAxis();
    CoverageCoordAxis yaxis = getYAxis();
    if (xaxis == null || !(xaxis instanceof CoverageCoordAxis1D) || !xaxis.isRegular()) return false;
    if (yaxis == null || !(yaxis instanceof CoverageCoordAxis1D) || !yaxis.isRegular()) return false;
    return true;
  }

  public ProjectionImpl getProjection() {
    for (String ctName : getTransformNames()) {
      CoverageTransform ct = dataset.findCoordTransform(ctName);
      if (ct != null && ct.isHoriz())
        return ct.getProjection();
    }
    return new LatLonProjection();
  }

  private static class MyCoordSysContainer implements CoordSysContainer {
    public List<CoverageCoordAxis> axes;
    public List<CoverageTransform> transforms;

    private MyCoordSysContainer(List<CoverageCoordAxis> axes, List<CoverageTransform> transforms) {
      this.axes = axes;
      this.transforms = transforms;
    }

    @Override
    public CoverageTransform findCoordTransform(String transformName) {
      for (CoverageTransform ct : transforms)
        if (ct.getName().equalsIgnoreCase(transformName)) return ct;
      return null;
    }

    @Override
    public CoverageCoordAxis findCoordAxis(String axisName) {
      for (CoverageCoordAxis axis : axes) {
        if (axis.getName().equalsIgnoreCase(axisName)) return axis;
      }
      return null;
    }
  }

  ////////////////////////////////////////////////

  public boolean isTime2D( CoverageCoordAxis axis) {
    if (time2DCoordSys == null) return false;
    if (axis instanceof TimeOffsetAxis) return true;
    if (axis instanceof TimeAxis2DFmrc) return true;
    return (axis.getAxisType() == AxisType.RunTime) && (axis.getDependenceType() != CoverageCoordAxis.DependenceType.dependent);
  }

  public Optional<CoverageCoordSys> subset(SubsetParams params) {
    return subset(params, false, true);
  }

  public Optional<CoverageCoordSys> subset(SubsetParams params, boolean makeCFcompliant, boolean finish) {
    Formatter errMessages = new Formatter();
    List<CoverageCoordAxis> subsetAxes = new ArrayList<>();
    for (CoverageCoordAxis axis : getAxes()) {
      if (axis.getDependenceType() == CoverageCoordAxis.DependenceType.dependent) continue;
      if (axis.getAxisType().isHoriz() || isTime2D(axis)) continue;

      ucar.nc2.util.Optional<CoverageCoordAxis> axiso = axis.subset(params);
      if (!axiso.isPresent())
        errMessages.format("%s: %s;%n", axis.getName(), axiso.getErrorMessage());
      else {
        CoverageCoordAxis1D subsetInd = (CoverageCoordAxis1D) axiso.get(); // independent always 1D
        subsetAxes.add(subsetInd);

        // subset any dependent axes
        for (CoverageCoordAxis dependent : getDependentAxes(subsetInd)) {
          Optional<CoverageCoordAxis> depo = dependent.subsetDependent(subsetInd);
          if (depo.isPresent()) subsetAxes.add(depo.get());
          else errMessages.format("%s;%n", depo.getErrorMessage());
        }
      }
    }

    AtomicBoolean isConstantForecast = new AtomicBoolean(false); // need a mutable boolean
    if (time2DCoordSys != null) {
      ucar.nc2.util.Optional<List<CoverageCoordAxis>> time2Do = time2DCoordSys.subset(params, isConstantForecast, makeCFcompliant);
      if (!time2Do.isPresent())
        errMessages.format("%s;%n", time2Do.getErrorMessage());
      else
        subsetAxes.addAll(time2Do.get());
    }

    Optional<HorizCoordSys> horizo = horizCoordSys.subset(params);
    if (!horizo.isPresent())
      errMessages.format("%s;%n", horizo.getErrorMessage());
    else {
      HorizCoordSys subsetHcs = horizo.get();
      subsetAxes.addAll(subsetHcs.getCoordAxes());
    }

    String errs = errMessages.toString();
    if (errs.length() > 0)
      return Optional.empty(errs);

    Collections.sort(subsetAxes);

    List<String> names = new ArrayList<>();
    for (CoverageCoordAxis axis : subsetAxes)
      names.add(axis.getName());

    CoverageCoordSys resultCoordSys = new CoverageCoordSys(null, names, this.getTransformNames(), this.getCoverageType());
    MyCoordSysContainer fakeDataset = new MyCoordSysContainer(subsetAxes, getTransforms());
    resultCoordSys.setDataset(fakeDataset);
    resultCoordSys.setHorizCoordSys(resultCoordSys.makeHorizCoordSys());
    resultCoordSys.setIsConstantForecast(isConstantForecast.get());
    if (finish) resultCoordSys.setImmutable();

    return Optional.of(resultCoordSys);
  }

  public List<CoverageCoordAxis> getDependentAxes(CoverageCoordAxis indAxis) {
    List<CoverageCoordAxis> result = dependentMap.get(indAxis.getName());
    return (result == null) ? new ArrayList<>() : result;
  }

}
