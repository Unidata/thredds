/* Copyright Unidata */
package ucar.nc2.ft2.coverage;

import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.util.StringUtil2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CoverageCoordAxisBuilder {

  public String name;
  public String description;
  public DataType dataType;
  public AxisType axisType;    // ucar.nc2.constants.AxisType ordinal
  public AttributeContainer attributes;
  public CoverageCoordAxis.DependenceType dependenceType;
  public List<String> dependsOn;

  public int ncoords;            // number of coordinates (not values)
  public CoverageCoordAxis.Spacing spacing;
  public double startValue;
  public double endValue;
  public double resolution;
  public CoordAxisReader reader;
  public boolean isSubset;

  public TimeHelper timeHelper; // AxisType = Time, RunTime only
  public String units;

  // may be lazy eval
  public double[] values;

  // 1D only
  public int minIndex, maxIndex; // closed interval [minIndex, maxIndex] ie minIndex to maxIndex are included, nvalues = max-min+1.
  public int stride = 1;
  public boolean isTime2D;

  // 2d only
  public int[] shape;
  public CoverageCoordAxis[] dependentAxes;
  public CoverageCoordAxis1D runCoord;

  public CoverageCoordAxisBuilder() {}

  public CoverageCoordAxisBuilder(String name, String units, String description, DataType dataType, AxisType axisType, AttributeContainer atts,
                              CoverageCoordAxis.DependenceType dependenceType, String dependsOn, CoverageCoordAxis.Spacing spacing,
                              int ncoords, double startValue, double endValue, double resolution, double[] values,
                              CoordAxisReader reader, boolean isSubset) {
    this.name = name;
    this.units = units;
    this.description = description;
    this.dataType = dataType;
    this.axisType = axisType;
    this.attributes = atts;
    this.dependenceType = dependenceType;
    this.setDependsOn(dependsOn);
    this.spacing = spacing;
    this.ncoords = ncoords;
    this.startValue = startValue;
    this.endValue = endValue;
    this.resolution = resolution;
    this.values = values;
    this.reader = reader;
    this.isSubset = isSubset;

    this.minIndex = 0;
    this.maxIndex = ncoords-1;
    this.isTime2D = (axisType == AxisType.RunTime && dependenceType != CoverageCoordAxis.DependenceType.dependent);
  }

  CoverageCoordAxisBuilder(CoverageCoordAxis from) {
    this.name = from.name;
    this.units = from.units;
    this.description = from.description;
    this.dataType = from.dataType;
    this.axisType = from.axisType;
    this.attributes = from.attributes;
    this.dependenceType = from.dependenceType;
    this.spacing = from.spacing;
    this.values = from.values;
    this.reader = from.reader; // used only if values == null
    this.dependsOn = from.dependsOn;
    this.startValue = from.startValue;
    this.endValue = from.endValue;
    this.resolution = from.resolution;

    this.ncoords = from.ncoords;
    this.isSubset = from.isSubset;
    this.timeHelper = from.timeHelper;

    this.minIndex = 0;
    this.maxIndex = ncoords-1;
    this.isTime2D = (axisType == AxisType.RunTime && dependenceType != CoverageCoordAxis.DependenceType.dependent);
  }

  public CoverageCoordAxisBuilder setDependsOn(String dependsOn) {
    if (dependsOn != null && dependsOn.trim().length() > 0) {
      List<String> temp = new ArrayList<>();
      Collections.addAll(temp, StringUtil2.splitString(dependsOn));
      this.dependsOn = Collections.unmodifiableList(temp);
    } else {
      this.dependsOn = Collections.emptyList();
    }
    return this;
  }

  CoverageCoordAxisBuilder subset(String dependsOn, CoverageCoordAxis.Spacing spacing, int ncoords, double[] values) {
    assert values != null;
    if (dependsOn != null) {
      this.dependenceType = CoverageCoordAxis.DependenceType.dependent;
      setDependsOn(dependsOn);
    }
    this.spacing = spacing;
    this.ncoords = ncoords;
    this.reader = null;
    this.values = values;
    this.isSubset = true;

    return this;
  }

  CoverageCoordAxisBuilder subset(int ncoords, double startValue, double endValue, double[] values) {
    this.ncoords = ncoords;
    this.startValue = startValue;
    this.endValue = endValue;
    this.values = values;
    this.isSubset = true;

    return this;
  }

  CoverageCoordAxisBuilder setIndexRange(int minIndex, int maxIndex, int stride) {
    this.minIndex = minIndex;
    this.maxIndex = maxIndex;
    this.stride = stride;
    this.isSubset = true;
    return this;
  }

  void setReferenceDate(CalendarDate refDate) {
    this.timeHelper = timeHelper.setReferenceDate(refDate);
    this.units = timeHelper.getUdUnit();
    if (attributes != null) {
      attributes.addAttribute(new Attribute(CDM.UNITS, this.units));
    }
  }

}
