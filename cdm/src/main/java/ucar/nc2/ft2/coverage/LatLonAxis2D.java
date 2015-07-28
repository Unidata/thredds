/* Copyright */
package ucar.nc2.ft2.coverage;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.constants.AxisType;
import ucar.nc2.time.Calendar;
import ucar.nc2.util.Indent;
import ucar.nc2.util.Misc;
import ucar.nc2.util.NamedObject;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;

/**
 * LatLon axes : lat(y,x), lon(y,x)
 *
 * @author caron
 * @since 7/15/2015
 */
public class LatLonAxis2D extends CoverageCoordAxis {
  private int[] shape;
  private CoverageCoordAxis[] dependentAxes;

  public LatLonAxis2D(String name, String units, String description, DataType dataType, AxisType axisType, List<Attribute> attributes, DependenceType dependenceType,
                      List<String> dependsOn, int[] shape, Spacing spacing, int ncoords, double startValue, double endValue, double resolution, double[] values,
                      CoordAxisReader reader) {
    super(name, units, description, dataType, axisType, attributes, dependenceType, dependsOn, spacing, ncoords, startValue, endValue, resolution, values, reader);
    this.shape = shape;
  }

  @Override
  protected void setDataset(CoordSysContainer dataset) {
    if (dependentAxes != null)
      throw new RuntimeException("Cant change axis once set");
    dependentAxes = new CoverageCoordAxis[2];
    int count = 0;
    for (String axisName : dependsOn) {
      CoverageCoordAxis axis = dataset.findCoordAxis(axisName);
      if (axis != null)
        dependentAxes[count++] = axis;
    }
  }

  public int[] getShape() {
    return shape;
  }

  @Override
  public void toString(Formatter f, Indent indent) {
    super.toString(f, indent);
    f.format("%s  %s%n", indent, Misc.showInts(shape));
  }

  @Override
  public LatLonAxis2D copy() {
    return new LatLonAxis2D(name, units, description, dataType, axisType, attributes.getAttributes(), dependenceType, dependsOn, shape,
            spacing, ncoords, startValue, endValue, resolution, values, reader);
  }

  @Override
  public LatLonAxis2D subset(SubsetParams params) {  // LOOK wrong
    return new LatLonAxis2D(name, units, description, dataType, axisType, attributes.getAttributes(), dependenceType, dependsOn, shape,
            spacing, ncoords, startValue, endValue, resolution, values, reader);
  }

  @Override
  public LatLonAxis2D subset(double minValue, double maxValue) {
    return this; // LOOK
  }

  @Override
  public Array getCoordsAsArray() throws IOException {
    double[] values = getValues();
    return Array.factory(DataType.DOUBLE, shape, values);
  }

  @Override
  public Array getCoordBoundsAsArray() {
    return null;   // LOOK
  }
}
