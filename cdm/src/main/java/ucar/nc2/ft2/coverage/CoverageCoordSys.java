/* Copyright */
package ucar.nc2.ft2.coverage;

import net.jcip.annotations.Immutable;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.constants.AxisType;
import ucar.nc2.util.Indent;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.projection.LatLonProjection;

import java.util.ArrayList;
import java.util.Arrays;
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

  public enum Type {General, Curvilinear, Grid, Swath, Fmrc}

  //////////////////////////////////////////////////
  protected CoordSysContainer dataset; // almost immutable, need to wire these in after the constructor
  private HorizCoordSys horizCoordSys;

  private final String name;
  private final List<String> axisNames;        // note not in order (?)
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

  public void setDataset(CoordSysContainer dataset) {
    if (this.dataset != null) throw new RuntimeException("Cant change dataset once set");
    this.dataset = dataset;
  }

  void setHorizCoordSys(HorizCoordSys horizCoordSys) {
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
      if (ct.isHoriz()) return ct;
    }
    return null;
  }

  public HorizCoordSys getHorizCoordSys() {
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

  public CoverageCoordAxis getTimeAxis() {
    for (String axisName : getAxisNames()) {
      CoverageCoordAxis axis = dataset.findCoordAxis(axisName);
       if (axis.getAxisType() == AxisType.Time) {
         return axis;
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

  ////////////////////////////////////////////////

  public CoverageCoordSys subset(SubsetParams params) throws InvalidRangeException {

    List<CoverageCoordAxis> subsetAxes = new ArrayList<>();
    for (CoverageCoordAxis axis : getAxes()) {
      if (!axis.getAxisType().isHoriz())
        subsetAxes.add(axis.subset(params));
    }
    HorizCoordSys orgHcs = getHorizCoordSys();
    HorizCoordSys subsetHcs = orgHcs.subset(params);
    subsetAxes.addAll(subsetHcs.getCoordAxes());

    CoverageCoordSys result = new CoverageCoordSys(this);
    MyCoordSysContainer  fakeDataset = new MyCoordSysContainer(subsetAxes, getTransforms());
    result.setDataset(fakeDataset);
    result.setHorizCoordSys(subsetHcs);

    return result;
  }

  private class MyCoordSysContainer implements CoordSysContainer {
    public List<CoverageCoordAxis> axes;
    public List<CoverageTransform> transforms;

    public MyCoordSysContainer(List<CoverageCoordAxis> axes, List<CoverageTransform> transforms) {
      this.axes = axes;
      this.transforms = transforms;
    }

    public CoverageTransform findCoordTransform(String transformName) {
     for (CoverageTransform ct : transforms)
       if (ct.getName().equalsIgnoreCase(transformName)) return ct;
     return null;
   }

   public CoverageCoordAxis findCoordAxis(String axisName) {
     for (CoverageCoordAxis axis : axes) {
       if (axis.getName().equalsIgnoreCase(axisName)) return axis;
     }
     return null;
   }
  }


}
