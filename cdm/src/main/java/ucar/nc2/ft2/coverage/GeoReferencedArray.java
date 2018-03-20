/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft2.coverage;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IsMissingEvaluator;
import ucar.ma2.Section;
import ucar.nc2.constants.FeatureType;

import java.util.Formatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A data array with GeoReferencing.
 * See CoverageReader.readData()
 *
 * @author caron
 * @since 7/11/2015
 */
public class GeoReferencedArray implements IsMissingEvaluator, CoordSysContainer {
  private String coverageName;
  private DataType dataType;
  private Array data;
  private CoverageCoordSys csSubset;
  private List<CoverageCoordAxis> axes;
  private List<CoverageTransform> transforms;

  public GeoReferencedArray(String coverageName, DataType dataType, Array data, List<CoverageCoordAxis> axes,
                            List<CoverageTransform> transforms, FeatureType type) {
    this.coverageName = coverageName;
    this.dataType = dataType;
    this.data = data;
    this.axes = axes;
    this.transforms = transforms;
    List<String> names = axes.stream().map(CoverageCoordAxis::getName).collect(Collectors.toList());

    this.csSubset = new CoverageCoordSys(null, names, null, type);
    this.csSubset.setDataset(this);
    this.csSubset.setHorizCoordSys(this.csSubset.makeHorizCoordSys());

    // check consistency
    Section cs = new Section(csSubset.getShape());
    Section sdata = new Section(data.getShape());
    assert cs.conformal(sdata);

    // reshape data if needed
    if (!cs.equalShape(sdata))
      this.data = data.reshape(csSubset.getShape());
  }

  public GeoReferencedArray(String coverageName, DataType dataType, Array data, CoverageCoordSys csSubset) {
    this.coverageName = coverageName;
    this.dataType = dataType;
    this.data = data;
    this.csSubset = csSubset;
    this.axes = csSubset.getAxes();
    this.transforms = csSubset.getTransforms();
  }

  public String getCoverageName() {
    return coverageName;
  }

  public DataType getDataType() {
    return dataType;
  }

  public Array getData() {
    return data;
  }

  public CoverageCoordSys getCoordSysForData() {
    return csSubset;
  }

  @Override
  public boolean hasMissing() {
    return true;
  }

  @Override
  public boolean isMissing(double val) {
    return Double.isNaN(val);
  }

  @Override
  public String toString() {
    Formatter f = new Formatter();
    f.format("GeoReferencedArray {%n");
    f.format(" coverageName='%s'%n", coverageName);
    f.format(" dataType=%s%n", dataType);
    f.format(" csSubset=%s%n", csSubset);
    for (CoverageCoordAxis axis : axes)
      f.format("%n%s", axis);
    f.format("}");
    return f.toString();
  }

  @Override
  public CoverageTransform findCoordTransform(String want) {
    for (CoverageTransform t : transforms)
      if (t.getName().equals(want)) return t;
    return null;
  }

  @Override
  public CoverageCoordAxis findCoordAxis(String want) {
    for (CoverageCoordAxis axis : axes)
      if (axis.getName().equals(want)) return axis;
    return null;
  }
}
