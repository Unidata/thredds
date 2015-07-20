/* Copyright */
package ucar.nc2.ft2.coverage;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.constants.AxisType;
import ucar.nc2.time.Calendar;
import ucar.nc2.util.Indent;
import ucar.nc2.util.NamedObject;

import java.util.Formatter;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 7/15/2015
 */
public class FmrcTimeAxis2D extends CoverageCoordAxis {
  private int[] shape;

  public FmrcTimeAxis2D(String name, String units, String description, DataType dataType, AxisType axisType, List<Attribute> attributes,
                           DependenceType dependenceType, List<String> dependsOn, Spacing spacing, int ncoords, double startValue, double endValue, double resolution,
                           double[] values, CoordAxisReader reader) {

    super(name, units, description, dataType, axisType, attributes, dependenceType, dependsOn, spacing, ncoords, startValue, endValue, resolution, values, reader);
  }

  @Override
  protected void setDataset(CoordSysContainer dataset) {
    if (shape != null) throw new RuntimeException("Cant change axis once set");
    shape = new int[2];
    int count = 0;
    for (String axisName : dependsOn) {
      CoverageCoordAxis axis = dataset.findCoordAxis(axisName);
      shape[count++] = axis.getNcoords();
    }
  }

  @Override
  public void toString(Formatter f, Indent indent) {
    super.toString(f, indent);
  }

  @Override
  public FmrcTimeAxis2D copy() {
    return new FmrcTimeAxis2D(name, units, description, dataType, axisType, attributes.getAttributes(), dependenceType,
                          dependsOn, spacing, ncoords, startValue, endValue, resolution, values, reader);
  }

  @Override
  public FmrcTimeAxis2D subset(SubsetParams params) {    // LOOK wrong
    return new FmrcTimeAxis2D(name, units, description, dataType, axisType, attributes.getAttributes(), dependenceType,
                          dependsOn, spacing, ncoords, startValue, endValue, resolution, values, reader);
  }

  @Override
  public FmrcTimeAxis2D subset(double minValue, double maxValue) {
    return this; // LOOK
  }

  @Override
  public Array getCoordsAsArray() {
    double[] values = getValues();
    return Array.factory(DataType.DOUBLE, shape, values);
  }

  @Override
  public Array getCoordBoundsAsArray() {
    return null; // LOOK
  }

  @Override
  public List<NamedObject> getCoordValueNames() {
    return null;
  }
}
