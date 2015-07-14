/* Copyright */
package ucar.nc2.ft2.coverage;

import net.jcip.annotations.Immutable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.util.Indent;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.projection.LatLonProjection;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * A Coverage CoordSys
 *
 * @author caron
 * @since 7/11/2015
 */
@Immutable
public class CoverageCoordSys {

  public enum Type {Coverage, Curvilinear, Grid, Swath, Fmrc}

  //////////////////////////////////////////////////
  protected CoverageDataset dataset; // almost immutable, need to wire these in after the constructor
  private CoverageCoordSysHoriz horizCoordSys;

  private final String name;
  private final List<String> axisNames;
  private final List<String> transformNames;
  private final Type type;

  public CoverageCoordSys(String name, List<String> axisNames, List<String> transformNames, Type type) {
    this.name = name;
    this.axisNames = axisNames;
    this.transformNames = transformNames;
    this.type = type;
  }

  // copy constructor
  public CoverageCoordSys(CoverageCoordSys from) {
    this.name = from.getName();
    this.axisNames = from.getAxisNames();
    this.transformNames = from.getTransformNames();
    this.type = from.getType();
  }

  void setDataset(CoverageDataset dataset) {
    if (this.dataset != null) throw new RuntimeException("Cant change dataset once set");
    this.dataset = dataset;
  }

  void setHorizCoordSys(CoverageCoordSysHoriz horizCoordSys) {
    if (this.horizCoordSys != null) throw new RuntimeException("Cant change horizCoordSys once set");
    this.horizCoordSys = horizCoordSys;
  }

  ///////////////////////////////////////////////

  public String getName() {
    return name;
  }

  public List<String> getTransformNames() {
    return (transformNames != null) ? transformNames : new ArrayList<>();
  }

  public CoverageTransform getHorizTransform() {
    for (String name : getTransformNames()) {
      CoverageTransform ct = dataset.findCoordTransform(name);
      if (ct.isHoriz()) return ct;
    }
    return null;
  }

  public CoverageCoordSysHoriz getHorizCoordSys() {
    return horizCoordSys;
  }

  public Type getType() {
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
    if (transformNames != null) {
      f.format("; has transforms:");
      for (String t : transformNames)
        f.format("%s, ", t);
    }
    f.format("%n");

    indent.decr();
  }

  ////////////////////////////////////////////////////////////////////

  public CoverageCoordAxis getXAxis() {
    for (String axisName : getAxisNames()) {
      CoverageCoordAxis axis = dataset.findCoordAxis(axisName);
       if (axis.getAxisType() == AxisType.GeoX)
         return axis;
     }
    for (String axisName : getAxisNames()) {
      CoverageCoordAxis axis = dataset.findCoordAxis(axisName);
       if (axis.getAxisType() == AxisType.Lon)
         return axis;
     }
    throw new IllegalArgumentException("Cant find X axis for coordsys "+getName());
  }

  public CoverageCoordAxis getYAxis() {
    for (String axisName : getAxisNames()) {
      CoverageCoordAxis axis = dataset.findCoordAxis(axisName);
       if (axis.getAxisType() == AxisType.GeoY)
         return axis;
     }
    for (String axisName : getAxisNames()) {
      CoverageCoordAxis axis = dataset.findCoordAxis(axisName);
       if (axis.getAxisType() == AxisType.Lat)
         return axis;
     }
    throw new IllegalArgumentException("Cant find Y axis for coordsys "+getName());
  }

  public CoverageCoordAxis getZAxis() {
    for (String axisName : getAxisNames()) {
      CoverageCoordAxis axis = dataset.findCoordAxis(axisName);
       if (axis.getAxisType() == AxisType.GeoZ || axis.getAxisType() == AxisType.Height || axis.getAxisType() == AxisType.Pressure)
         return axis;
     }
    return null;
  }

  public CoverageCoordAxisTime getTimeAxis() {
    for (String axisName : getAxisNames()) {
      CoverageCoordAxis axis = dataset.findCoordAxis(axisName);
       if (axis.getAxisType() == AxisType.Time) {
         return (CoverageCoordAxisTime) axis;
       }
     }
    return null;
  }

  public CoverageCoordAxis getAxis(String axisName) {
    return dataset.findCoordAxis(axisName);
  }

  public CoverageCoordAxis getAxis(AxisType type) {
    for (String axisName : getAxisNames()) {
      CoverageCoordAxis axis = dataset.findCoordAxis(axisName);
       if (axis.getAxisType() == type) {
         return axis;
       }
     }
    return null;
  }

  public List<CoverageCoordAxis> getAxes() {
    List<CoverageCoordAxis> result = new ArrayList<>();
    for (String axisName : getAxisNames()) {
      CoverageCoordAxis axis = dataset.findCoordAxis(axisName);
      result.add(axis);
     }
    return result;
  }

  public boolean isRegularSpatial() {
    CoverageCoordAxis xaxis = getXAxis();
    CoverageCoordAxis yaxis = getYAxis();
    if (xaxis == null || !xaxis.isRegular()) return false;
    if (yaxis == null || !yaxis.isRegular()) return false;
    return true;
  }

  public String getIndependentAxisNames() {
    StringBuilder sb = new StringBuilder();
    for (String axisName : getAxisNames()) {
      CoverageCoordAxis axis = dataset.findCoordAxis(axisName);
      if (axis == null)
        throw new RuntimeException("Cant find axis "+axisName);
      if (!(axis.getDependenceType() == CoverageCoordAxis.DependenceType.independent)) continue;
      sb.append(axisName);
      sb.append(" ");
    }
    return sb.toString();
  }

  public ProjectionImpl getProjection() {
    for (String ctName : getTransformNames()) {
      CoverageTransform ct = dataset.findCoordTransform(ctName);
      if (ct != null && ct.isHoriz())
        return ct.getProjection();
    }
    return new LatLonProjection();
  }

}
