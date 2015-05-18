/* Copyright */
package ucar.nc2.ft2.coverage.grid;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
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
 * GridCoordAxis
 * always one dimensional
 * could make immutable and use a builder pattern
 * <p/>
 * storage
 * immediate (cached)
 * maybe can be regular with missing values ??
 * huge - do not try to cache
 *
 * @author caron
 * @since 5/4/2015
 */
public class GridCoordAxis {
  public enum Spacing {regular, irregularPoint, contiguousInterval, discontiguousInterval} // see Spacing below
  public enum DependenceType {independent, dependent, twoD}

  /*
  message CoordAxis {
    required string name = 1;          // must be unique in dataset
    required DataType dataType = 2;
    required int32 axisType = 3;       // ucar.nc2.constants.AxisType ordinal
    required string units = 4;
    optional string description = 5;
    required int64 nvalues = 6;
    required int32 spacing = 7;         // GridCoordAxis.Spacing ordinal
    required double startValue = 8;
    required double endValue = 9;
    optional double resolution = 10;     // resolution = (max-min) / (nvalues-1)
    optional bytes values = 11;          // "immediate" - store small non-regular data in header
  }
   */
  private String name;
  private DataType dataType;
  private AxisType axisType;    // ucar.nc2.constants.AxisType ordinal
  private String units, description;
  private AttributeContainer attributes;
  private DependenceType dependenceType;
  private String dependsOn;

  private long nvalues, minIndex, maxIndex; // closed interval [minIndex, maxIndex] ie minIndex to maxIndex are included, nvalues = max-min+1.
  private int stride;
  private Spacing spacing;
  private double startValue;
  private double endValue;
  private double resolution;
  private double[] values;     // null if isRegular,

  public GridCoordAxis() {
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public DataType getDataType() {
    return dataType;
  }

  public void setDataType(DataType dataType) {
    this.dataType = dataType;
  }

  public AxisType getAxisType() {
    return axisType;
  }

  public void setAxisType(AxisType type) {
    this.axisType = type;
  }

  public List<Attribute> getAttributes() {
    return attributes.getAttributes();
  }

  public void setAttributes(AttributeContainer attributes) {
    this.attributes = attributes;
  }

  public long getNvalues() {
    return nvalues;
  }

  public void setNvalues(long nvalues) {
    this.nvalues = nvalues;
  }

  public long getMinIndex() {
    return minIndex;
  }

  public void setMinIndex(long minIndex) {
    this.minIndex = minIndex;
  }

  public long getMaxIndex() {
    return maxIndex;
  }

  public void setMaxIndex(long maxIndex) {
    this.maxIndex = maxIndex;
  }

  public int getStride() {
    return stride;
  }

  public void setStride(int stride) {
    this.stride = stride;
  }

  public Spacing getSpacing() {
    return spacing;
  }

  public void setSpacing(Spacing spacing) {
    this.spacing = spacing;
  }

  public boolean isRegular() {
    return (spacing == Spacing.regular);
  }

  public double getResolution() {
    if (resolution == 0.0 && isRegular() && nvalues > 1)
      resolution = (endValue - startValue) / (nvalues - 1);
    return resolution;
  }

  public void setResolution(double resolution) {
    this.resolution = resolution;
  }

  public double getStartValue() {
    return startValue;
  }

  public void setStartValue(double startValue) {
    this.startValue = startValue;
  }

  public double getEndValue() {
    return endValue;
  }

  public void setEndValue(double endValue) {
    this.endValue = endValue;
  }

  public String getUnits() {
    return units;
  }

  public void setUnits(String units) {
    this.units = units;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public DependenceType getDependenceType() {
    return dependenceType;
  }

  public void setDependenceType(DependenceType dependenceType) {
    this.dependenceType = dependenceType;
  }

  public String getDependsOn() {
    return dependsOn;
  }

  public void setDependsOn(String dependsOn) {
    this.dependsOn = dependsOn;
  }

  // to override
  protected double[] readValues() {
    return null;
  }

  public double[] getValues() {
    if (values == null) values = readValues();
    return values;
  }

  public void setValues(double[] values) {
    this.values = values;
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
    f.format("%s CoordAxis '%s' axisType=%s dataType=%s", indent, name, axisType, dataType);
    f.format(" npts: %d [%f,%f] '%s' spacing=%s", nvalues, startValue, endValue, units, spacing);
    if (getResolution() != 0.0)
      f.format(" resolution=%f", resolution);
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

  GridCoordAxis finish() {
    getResolution();
    getValues();
    return this;
  }

  ///////////////////////////////////////////////////////////////////
  // Spacing

  /*
   * regular: regularly spaced points or intervals (start, end, npts), edges halfway between coords
   * irregularPoint: irregular spaced points (values, npts), edges halfway between coords
   * contiguousInterval: irregular contiguous spaced intervals (values, npts), values are the edges, and there are npts+1, coord halfway between edges
   * discontinuousInterval: irregular discontiguous spaced intervals (values, npts), values are the edges, and there are 2*npts: low0, high0, low1, high1...
   */

  public double getCoordEdge1(int index) {
    if (index >= nvalues) throw new IllegalArgumentException("Index must be <" + nvalues);
    switch (spacing) {
      case regular:
        return startValue + (index - .5) * resolution;

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
    if (index >= nvalues) throw new IllegalArgumentException("Index must be <" + nvalues);
    switch (spacing) {
      case regular:
        return startValue + (index + .5) * resolution;

      case irregularPoint:
        if (index < nvalues - 1)
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
    return getCoordEdge2((int) nvalues - 1);
  }

  public double getCoord(int index) {
    if (index >= nvalues) throw new IllegalArgumentException("Index must be <" + nvalues);
    switch (spacing) {
      case regular:
        return startValue + index * resolution;

      case irregularPoint:
        return values[index];

      case contiguousInterval:
        return (values[index] + values[index + 1]) / 2;

      case discontiguousInterval:
        return (values[2 * index] + values[2 * index + 1]) / 2;
    }
    throw new IllegalStateException("Unknown spacing=" + spacing);
  }

  public Array getCoords() {
    Array arr = Array.factory(getDataType(), new int[] {(int)nvalues});
    for (int i=0; i<nvalues; i++)
      arr.setDouble(i, getCoord(i));
    return arr;
  }

  public Array getCoordEdge1() {
    double[] vals = new double[(int) nvalues];
    for (int i=0; i<nvalues; i++)
      vals[i] = getCoordEdge1(i);
    return Array.makeFromJavaArray(vals);
  }

  public Array getCoordEdge2() {
    double[] vals = new double[(int) nvalues];
    for (int i=0; i<nvalues; i++)
      vals[i] = getCoordEdge2(i);
    return Array.makeFromJavaArray(vals);
  }

  public List<NamedObject> getCoordValueNames() {
    List<NamedObject> result = new ArrayList<>();
    for (int i = 0; i < nvalues; i++) {
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

  /////////////////////////////////////////////////////////////////////////
  // subsetting

  protected GridCoordAxis(GridCoordAxis from) {
    setAttributes(from.attributes);
    setAxisType(from.getAxisType());
    setDataType(from.getDataType());
    setDependenceType(from.getDependenceType());
    setDependsOn(from.getDependsOn());
    setDescription(from.getDescription());
    setName(from.getName());
    setSpacing(from.getSpacing());
    setResolution(from.getResolution());
    setUnits(from.getUnits());
  }

  public GridCoordAxis subset(double minValue, double maxValue) {
    GridCoordAxis result = new GridCoordAxis(this);
    subsetValues(result, minValue, maxValue);
    return result.finish();
  }

  public GridCoordAxis subsetLatest() {
    GridCoordAxis result = new GridCoordAxis(this);
    subsetValuesLatest(result);
    return result.finish();
  }

  public GridCoordAxis subset(Calendar cal, CalendarDate date) {
    GridCoordAxisTime result = new GridCoordAxisTime(this, cal);
    double want = result.convert(date);
    subsetValuesClosest(result, want);
    return result.finish();
  }

  public GridCoordAxis subset(Calendar cal, CalendarDateRange dateRange) {
     GridCoordAxisTime result = new GridCoordAxisTime(this, cal);
     double min  = result.convert(dateRange.getStart());
     double max  = result.convert(dateRange.getEnd());
     subsetValues(result, min, max);
     return result.finish();
   }

  private void subsetValues(GridCoordAxis result, double minValue, double maxValue) {
    double[] subsetValues;
    double resolution = getResolution();
    int minIndex, maxIndex;
    double subsetStart, subsetEnd;
    int count=0, count2=0;

    switch (spacing) {
      case regular:                     // LOOK need to deal with longitude wrapping, latitude decreasing
        if (minValue > endValue)
          throw new IllegalArgumentException("no points in subset: min > end");
        if (maxValue < startValue)
          throw new IllegalArgumentException("no points in subset: max < start");

        if (minValue <= startValue) {
          minIndex = 0;
          subsetStart = startValue;
        } else {
          minIndex = 1 + (int) ((minValue - startValue) / resolution);
          subsetStart = startValue + minIndex * resolution;
        }
        if (maxValue >= endValue) {
          maxIndex = (int) nvalues - 1;
          subsetEnd = endValue;
        } else {
          maxIndex = (int) ((maxValue - startValue) / resolution);
          subsetEnd = startValue + maxIndex * resolution;
        }
        result.setNvalues(maxIndex - minIndex + 1);
        result.setMinIndex(minIndex);
        result.setMaxIndex(maxIndex);
        result.setStartValue(subsetStart);
        result.setEndValue(subsetEnd);
        break;

      case irregularPoint:
        minIndex = 0;
        maxIndex = 0;
        for (double val : getValues()) {  // LOOK linear
          if (val > maxValue) break;
          if (val < minValue) minIndex++;
          maxIndex++;
        }
        count = maxIndex - minIndex;
        if (count == 0)
          throw new IllegalArgumentException("no points in subset: min > end");

        subsetValues = new double[count];
        for (int i=minIndex; i<=maxIndex; i++) {
          subsetValues[i] = getCoord(i);
        }

        result.setNvalues(count);
         result.setMinIndex(minIndex);
        result.setMaxIndex(maxIndex);
        result.setValues(subsetValues);
        result.setStartValue(subsetValues[0]);
        result.setEndValue(subsetValues[subsetValues.length - 1]);
        break;

      case contiguousInterval:
        minIndex = -1;
        count = 0;
        for (int i=0; i<values.length; i++) {                       // these are edges
          if (values[i] <= maxValue && values[i+1] >= minValue) {   // count number of intervals that pass
            count++;
            if (minIndex < 0) minIndex = i;  // record first one that passes
          }
        }
        if (count == 0)
          throw new IllegalArgumentException("no points in subset: min > end");
        maxIndex = minIndex+count-1;

        subsetValues = new double[count+1];            // need npts+1
        for (int i=minIndex; i<=maxIndex; i++)
          subsetValues[count2++] = values[i];

        result.setNvalues(count);
        result.setMinIndex(minIndex);
        result.setMaxIndex(maxIndex);
        result.setValues(subsetValues);
        result.setStartValue(subsetValues[0]);
        result.setEndValue(subsetValues[subsetValues.length-1]);
        break;

      case discontiguousInterval:
        minIndex = -1;
        count = 0;
        for (int i=0; i<values.length; i+=2) {                       // these are bounds (low, high)1, (low,high)2, ...
          if (values[i] <= maxValue && values[i+1] >= minValue) {
            count++;     // count number of intervals that pass
            if (minIndex < 0) minIndex = i;  // record first one that passes
          }
        }
        if (count == 0)
          throw new IllegalArgumentException("no points in subset: min > end");
        maxIndex = minIndex+count-1;

        count2 = 0;
        subsetValues = new double[2*count];                   // need 2*npts
        for (int i=minIndex; i<=maxIndex; i+=2) {
          subsetValues[count2++] = values[i];
          subsetValues[count2++] = values[i+1];
        }

        result.setNvalues(count);
         result.setMinIndex(minIndex);
        result.setMaxIndex(maxIndex);
        result.setValues(subsetValues);
        result.setStartValue(subsetValues[0]);
        result.setEndValue(subsetValues[subsetValues.length-1]);
        break;
    }
  }

  /*
   * regular: regularly spaced points or intervals (start, end, npts), edges halfway between coords
   * irregularPoint: irregular spaced points (values, npts), edges halfway between coords
   * contiguousInterval: irregular contiguous spaced intervals (values, npts), values are the edges, and there are npts+1, coord halfway between edges
   * discontinuousInterval: irregular discontiguous spaced intervals (values, npts), values are the edges, and there are 2*npts: low0, high0, low1, high1...
   */
  private void subsetValuesClosest(GridCoordAxis result, double want) {
    double[] subsetValues;
    double subsetStart;
    int want_index = -1;
    double resolution = getResolution();

    switch (spacing) {
      case regular:
        if (want >= endValue)
          want_index = (int) nvalues-1;
        else if (want <= startValue)
          want_index = 0;
        else
          want_index = (int) (.5 + (want - startValue) / resolution);

        result.setNvalues(1);
         result.setMinIndex(want_index);
        result.setMaxIndex(want_index);
        subsetStart = startValue + want_index * resolution;
        result.setStartValue(subsetStart);
        result.setEndValue(subsetStart);
        break;

      case contiguousInterval:
      case irregularPoint:
        if (want > endValue)
          want_index = (int) nvalues-1;
        else if (want < startValue)
          want_index = 0;
        else {
          int idx = 0;
          for (double val : getValues()) {
            if (val > want) break;
            idx++;
          }
          if (spacing == Spacing.irregularPoint) {
            double lower = want - getCoord(idx);
            double upper = getCoord(idx + 1) - want;
            want_index = (lower < upper) ? idx : idx + 1;
          } else {
            want_index = idx;
          }
        }

        subsetStart = getCoord(want_index);
        if (spacing == Spacing.irregularPoint) {
          subsetValues = new double[1];
          subsetValues[0] = subsetStart;
        } else {
          subsetValues = new double[2];
          subsetValues[0] = getCoordEdge1(want_index);
          subsetValues[1] = getCoordEdge2(want_index);
        }
        result.setValues(subsetValues);

        result.setNvalues(1);
        result.setMinIndex(want_index);
        result.setMaxIndex(want_index);
        result.setStartValue(subsetStart);
        result.setEndValue(subsetStart);
        break;

      case discontiguousInterval:
        if (want > endValue)
          want_index = (int) nvalues-1;
        else if (want < startValue)
          want_index = 0;
        else {
          for (int i = 0; i < values.length; i += 2) {                      // these are bounds (low, high)1, (low,high)2, ...
            if (values[i] <= want && values[i+1] >= want) {
              want_index = i;
              break;
            }
          }
        }

        subsetValues = new double[2];
        subsetValues[0] = getCoordEdge1(want_index);
        subsetValues[1] = getCoordEdge2(want_index);

        result.setNvalues(1);
        result.setMinIndex(want_index);
        result.setMaxIndex(want_index);
        result.setValues(subsetValues);
        result.setStartValue(subsetValues[0]);
        result.setEndValue(subsetValues[1]);
        break;
    }

  }

  private void subsetValuesLatest(GridCoordAxis result) {
    double[] subsetValues;

    result.setNvalues(1);
    result.setMinIndex(nvalues- 1);
    result.setMaxIndex(nvalues-1);
    result.setStartValue(endValue);
    result.setEndValue(endValue);

    switch (spacing) {
      case irregularPoint:
        subsetValues = new double[1];
        double[] values = getValues();
        subsetValues[0] = values[values.length-1];
        result.setValues(subsetValues);
        break;

      case discontiguousInterval:
      case contiguousInterval:
        subsetValues = new double[2];
        values = getValues();
        subsetValues[0] = values[values.length-2];
        subsetValues[1] = values[values.length - 1];
        result.setValues(subsetValues);
        break;
    }

  }

}
