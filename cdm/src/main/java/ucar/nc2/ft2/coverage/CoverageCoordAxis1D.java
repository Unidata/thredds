/* Copyright */
package ucar.nc2.ft2.coverage;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.constants.AxisType;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.util.Indent;
import ucar.nc2.util.NamedAnything;
import ucar.nc2.util.NamedObject;
import ucar.unidata.util.Format;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 7/15/2015
 */
public class CoverageCoordAxis1D extends CoverageCoordAxis {

  // subset ??
  protected long minIndex, maxIndex; // closed interval [minIndex, maxIndex] ie minIndex to maxIndex are included, nvalues = max-min+1.
  protected int stride = 1;

  public CoverageCoordAxis1D(String name, String units, String description, DataType dataType, AxisType axisType, List<Attribute> attributes,
                                DependenceType dependenceType, String dependsOn, Spacing spacing, int ncoords, double startValue, double endValue,
                                double resolution, double[] values, CoordAxisReader reader) {

    super(name, units, description, dataType, axisType, attributes, dependenceType, dependsOn, spacing, ncoords, startValue, endValue, resolution, values, reader);

    this.minIndex = 0;
    this.maxIndex = ncoords-1;
  }

  public CoverageCoordAxis1D copy(CoordAxisReader reader) {
    return new CoverageCoordAxis1D(name, units, description, dataType, axisType, attributes.getAttributes(), dependenceType, dependsOn, spacing, ncoords, startValue, endValue, resolution, values, reader);
  }

  public int getStride() {
    return stride;
  }

  // ??
  void setIndexRange(int minIndex, int maxIndex, int stride) {
    this.minIndex = minIndex;
    this.maxIndex = maxIndex;
    this.stride = stride;
  }

  @Override
  public void toString(Formatter f, Indent indent) {
    indent.incr();
    f.format("%s CoordAxis '%s' axisType=%s dataType=%s", indent, name, axisType, dataType);
    f.format(" npts: %d [%f,%f] '%s' spacing=%s", ncoords, startValue, endValue, units, spacing);
    if (getResolution() != 0.0)
      f.format(" resolution=%f", resolution);
    f.format(" %s", getDependenceType());
    if (getDependsOn() != null)
      f.format(": %s", getDependsOn());
    f.format("%n");

    if (values != null) {
      int n = values.length;
      switch (spacing) {
        case irregularPoint:
        case contiguousInterval:
          f.format("%ncontiguous (%d)=", n);
          for (double v : values)
            f.format("%f,", v);
          f.format("%n");
          break;

        case discontiguousInterval:
          f.format("%ndiscontiguous (%d)=", n);
          for (int i = 0; i < n; i += 2)
            f.format("(%f,%f) ", values[i], values[i + 1]);
          f.format("%n");
          break;
      }
    }

    indent.decr();
  }



  ///////////////////////////////////////////////////////////////////
  // Spacing

  /*
   * regular: regularly spaced points or intervals (start, end, npts), edges halfway between coords
   * irregularPoint: irregular spaced points (values, npts), edges halfway between coords
   * contiguousInterval: irregular contiguous spaced intervals (values, npts), values are the edges, and there are npts+1, coord halfway between edges
   * discontinuousInterval: irregular discontiguous spaced intervals (values, npts), values are the edges, and there are 2*npts: low0, high0, low1, high1...
   */

  // LOOK question: is this applicable to anyD or only 1D ??

  public boolean isAscending() {
    switch (spacing) {
      case regular:
        return getResolution() > 0;

      case irregularPoint:
        return values[0] <= values[ncoords - 1];

      case contiguousInterval:
        return values[0] <= values[ncoords];

      case discontiguousInterval:
        return values[0] <= values[2*ncoords-1];
    }
    throw new IllegalStateException("unknown spacing"+spacing);
  }

  public double getCoord(int index) {
    switch (spacing) {
      case regular:
        if (index < 0 || index >= ncoords) throw new IllegalArgumentException("Index out of range " + index);
        return startValue + index * getResolution();

      case irregularPoint:
        return values[index];

      case contiguousInterval:
        return (values[index] + values[index + 1]) / 2;

      case discontiguousInterval:
        return (values[2 * index] + values[2 * index + 1]) / 2;
    }
    throw new IllegalStateException("Unknown spacing=" + spacing);
  }

  public double getCoordEdge1(int index) {
    switch (spacing) {
      case regular:
        if (index < 0 || index >= ncoords) throw new IllegalArgumentException("Index out of range " + index);
        return startValue + (index - .5) * getResolution();

      case irregularPoint:
        if (index > 0)
          return (values[index - 1] + values[index]) / 2;
        else
          return values[0] - (values[1] - values[0]) / 2;

      case contiguousInterval:
        return values[index];

      case discontiguousInterval:
        return values[2 * index];
    }
    throw new IllegalStateException("Unknown spacing=" + spacing);
  }

  public double getCoordEdge2(int index) {
    switch (spacing) {
      case regular:
        if (index < 0 || index >= ncoords) throw new IllegalArgumentException("Index out of range " + index);
        return startValue + (index + .5) * getResolution();

      case irregularPoint:
        if (index < ncoords - 1)
          return (values[index] + values[index + 1]) / 2;
        else
          return values[index] + (values[index] - values[index - 1]) / 2;

      case contiguousInterval:
        return values[index + 1];

      case discontiguousInterval:
        return values[2 * index + 1];
    }
    throw new IllegalStateException("Unknown spacing=" + spacing);
  }

  public double getCoordEdgeLast() {
    return getCoordEdge2( ncoords - 1);
  }

  @Override
  public Array getCoordsAsArray() {
    getValues();
    Array result;
    switch (dependenceType) {
      case scalar:
        result = Array.factory(getDataType(), new int[0]);
        break;
      case twoD:
        result = Array.factory(getDataType(), new int[0]);
        break;
      default:
        result = Array.factory(getDataType(), new int[] { ncoords});
        break;
    }

    for (int i=0; i< ncoords; i++)
      result.setDouble(i, getCoord(i));
    return result;
  }

  public CoverageCoordAxis subset(double minValue, double maxValue) {
    CoordAxisHelper helper = new CoordAxisHelper(this);
    return helper.subset(minValue, maxValue);
  }

 /* public Array getCoordEdge1() {
    getValues();
    double[] vals = new double[ ncoords];
    for (int i=0; i< ncoords; i++)
      vals[i] = getCoordEdge1(i);
    return Array.makeFromJavaArray(vals);
  }

  public Array getCoordEdge2() {
    getValues();
    double[] vals = new double[ ncoords];
    for (int i=0; i< ncoords; i++)
      vals[i] = getCoordEdge2(i);
    return Array.makeFromJavaArray(vals);
  } */

  @Override
  public List<NamedObject> getCoordValueNames() {
    getValues();  // read in if needed
    List<NamedObject> result = new ArrayList<>();
    for (int i = 0; i < ncoords; i++) {
      String valName = "";
      switch (spacing) {
        case regular:
        case irregularPoint:
          valName = Format.d(getCoord(i), 3);
          break;

        case contiguousInterval:
        case discontiguousInterval:
          valName = Format.d(getCoordEdge1(i), 3) + "," + Format.d(getCoordEdge2(i), 3);
          break;

      }
      result.add(new NamedAnything(valName, valName + " " + getUnits()));
    }

    return result;
  }

    // LOOK  incomplete handling of subsetting params
  // create a copy of the axis, with the values subsetted by the params as needed
  @Override
  public CoverageCoordAxis subset(SubsetParams params) {
    CoordAxisHelper helper = new CoordAxisHelper(this);

    switch (getAxisType()) {
      case GeoZ:
      case Pressure:
      case Height:
        Double dval = params.getDouble(SubsetParams.vertCoord);
        if (dval != null) {
          // LOOK problems when vertCoord doesnt match any coordinates in the axes
          return helper.subsetClosest(dval);
        }
        break;

      // x,y gets seperately subsetted
      case GeoX:
      case GeoY:
      case Lat:
      case Lon:
        return null;

      case Time:
        if (params.isTrue(SubsetParams.allTimes))
          return this.copy(this.reader);
        if (params.isTrue(SubsetParams.latestTime))
          return helper.subsetLatest( timeHelper.cal);

        CalendarDate date = (CalendarDate) params.get(SubsetParams.date);
        if (date != null)
          return helper.subset( timeHelper.cal, date);

        CalendarDateRange dateRange = (CalendarDateRange) params.get(SubsetParams.dateRange);
        if (dateRange != null)
          return helper.subset( timeHelper.cal, dateRange);
        break;
    }

    // otherwise take the entire axis
    return this.copy( this.reader);
  }

}
