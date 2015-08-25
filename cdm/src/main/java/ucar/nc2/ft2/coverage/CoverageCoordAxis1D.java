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
 */
package ucar.nc2.ft2.coverage;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.AxisType;
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
 * Coverage CoordAxis 1D case
 *
 * @author caron
 * @since 7/15/2015
 */
public class CoverageCoordAxis1D extends CoverageCoordAxis {

  // subset ??
  protected int minIndex, maxIndex; // closed interval [minIndex, maxIndex] ie minIndex to maxIndex are included, nvalues = max-min+1.
  protected int stride = 1;
  protected boolean isTime2D;

  public CoverageCoordAxis1D(String name, String units, String description, DataType dataType, AxisType axisType, AttributeContainer atts,
                                DependenceType dependenceType, String dependsOn, Spacing spacing, int ncoords, double startValue, double endValue,
                                double resolution, double[] values, CoordAxisReader reader, boolean isSubset) {

    super(name, units, description, dataType, axisType, atts, dependenceType, dependsOn, spacing, ncoords, startValue, endValue, resolution, values, reader, isSubset);

    this.minIndex = 0;
    this.maxIndex = ncoords-1;
  }

  //public CoverageCoordAxis1D copy() {
  //  return new CoverageCoordAxis1D(name, units, description, dataType, axisType, attributes.getAttributes(), dependenceType, dependsOn, spacing, ncoords, startValue, endValue, resolution, values, reader);
  //}


  public boolean isTime2D() {
    return isTime2D;
  }

  public void setIsTime2D() {
    this.isTime2D = true;
  }

  public int getStride() {
    return stride;
  }

  // for subsetting - these are the indexes reletive to original - note cant compose !!
  void setIndexRange(int minIndex, int maxIndex, int stride) {
    this.minIndex = minIndex;
    this.maxIndex = maxIndex;
    this.stride = stride;
  }

  public int getMinIndex() {
    return minIndex;
  }

  public int getMaxIndex() {
    return maxIndex;
  }

  @Override
  public void toString(Formatter f, Indent indent) {
    super.toString(f, indent);
    f.format("%s  minIndex=%d maxIndex=%d stride=%d isTime2D=%s isSubset=%s%n", indent, minIndex, maxIndex, stride, isTime2D(), isSubset());
  }


  ///////////////////////////////////////////////////////////////////
  // Spacing

  /*
   * regular: regularly spaced points or intervals (start, end, npts), edges halfway between coords
   * irregularPoint: irregular spaced points (values, npts), edges halfway between coords
   * contiguousInterval: irregular contiguous spaced intervals (values, npts), values are the edges, and there are npts+1, coord halfway between edges
   * discontinuousInterval: irregular discontiguous spaced intervals (values, npts), values are the edges, and there are 2*npts: low0, high0, low1, high1...
   */

  public boolean isAscending() {
    getValues();
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

  public double getCoordMidpoint(int index) {
    switch (spacing) {
      case regular:
      case irregularPoint:
        return getCoord(index);

      case contiguousInterval:
      case discontiguousInterval:
        return (getCoordEdge1(index)+getCoordEdge2(index))/2;
    }
    throw new IllegalStateException("Unknown spacing=" + spacing);
  }

  public double getCoord(int index) {
    getValues();
    if (index <0 || index >= getNcoords())
      throw new IllegalArgumentException("Index out of range=" + index);

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
    getValues();
    if (index <0 || index >= getNcoords())
      throw new IllegalArgumentException("Index out of range=" + index);

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
    getValues();
    if (index <0 || index >= getNcoords())
      throw new IllegalArgumentException("Index out of range=" + index);

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
      default:
        result = Array.factory(getDataType(), new int[] { ncoords});
        break;
    }

    for (int i=0; i< ncoords; i++)
      result.setDouble(i, getCoord(i));
    return result;
  }

  @Override
  public Array getCoordBoundsAsArray() {
    getValues();
    Array result = Array.factory(getDataType(), new int[] { ncoords, 2});

    int count = 0;
    for (int i=0; i<ncoords; i++) {
      result.setDouble(count++, getCoordEdge1(i));
      result.setDouble(count++, getCoordEdge2(i));
    }
    return result;
  }

  @Override
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

  public List<NamedObject> getCoordValueNames() {
    getValues();  // read in if needed
    if (timeHelper != null)
      return timeHelper.getCoordValueNames(this);

    List<NamedObject> result = new ArrayList<>();
    for (int i = 0; i < ncoords; i++) {
      Object value = null;
      switch (spacing) {
        case regular:
        case irregularPoint:
          value = Format.d(getCoord(i), 3);
          break;

        case contiguousInterval:
        case discontiguousInterval:
          value = new CoordInterval(getCoordEdge1(i), getCoordEdge2(i), 3);
          break;
      }
      result.add(new NamedAnything(value, value + " " + getUnits()));
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
          return helper.subsetClosest(dval);
        }
        break;

      case Ensemble:
         Double eval = params.getDouble(SubsetParams.ensCoord);
         if (eval != null) {
           return helper.subsetClosest(eval);
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
          return this;
        if (params.isTrue(SubsetParams.latestTime))
          return helper.subsetLatest();

        CalendarDate date = (CalendarDate) params.get(SubsetParams.time);
        if (date != null)
          return helper.subset(date);

        CalendarDateRange dateRange = (CalendarDateRange) params.get(SubsetParams.timeRange);
        if (dateRange != null)
          return helper.subset(dateRange);
        break;

      case RunTime:
        CalendarDate rundate = (CalendarDate) params.get(SubsetParams.runtime);
        if (rundate != null)
          return helper.subset(rundate);

        CalendarDateRange rundateRange = (CalendarDateRange) params.get(SubsetParams.runtimeRange);
        if (rundateRange != null)
          return helper.subset(rundateRange);

        // default is latest
        return helper.subsetLatest();
    }

    // otherwise take the entire axis
    return this;
  }

  @Override
  public CoverageCoordAxis subsetDependent(CoverageCoordAxis1D dependsOn) {
    CoordAxisHelper helper = new CoordAxisHelper(this);
    return helper.subsetValues(dependsOn.getMinIndex(), dependsOn.getMaxIndex()); // LOOK not dealing with stride
  }

  CoverageCoordAxis1D subset(int ncoords, double start, double end, double[] values) {
    return new CoverageCoordAxis1D(this.getName(), this.getUnits(), this.getDescription(), this.getDataType(), this.getAxisType(),
            this.getAttributeContainer(), this.getDependenceType(), this.getDependsOn(), this.getSpacing(),
            ncoords, start, end, this.getResolution(), values, this.reader, true);
  }


}

