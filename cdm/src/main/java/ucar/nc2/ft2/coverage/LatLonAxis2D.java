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
public class LatLonAxis2D extends CoverageCoordAxis {

  public LatLonAxis2D(String name, String units, String description, DataType dataType, AxisType axisType, List<Attribute> attributes, DependenceType dependenceType,
                      String dependsOn, Spacing spacing, int ncoords, double startValue, double endValue, double resolution, double[] values, CoordAxisReader reader) {
    super(name, units, description, dataType, axisType, attributes, dependenceType, dependsOn, spacing, ncoords, startValue, endValue, resolution, values, reader);
  }

  @Override
  public void toString(Formatter f, Indent indent) {

  }

  @Override
  public CoverageCoordAxis subset(SubsetParams params) {
    return null;
  }

  @Override
  public Array getCoordsAsArray() {
    return null;
  }

  @Override
  public List<NamedObject> getCoordValueNames() {
    return null;
  }
}
