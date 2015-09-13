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
 *
 */
package ucar.nc2.ft2.coverage;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IsMissingEvaluator;
import ucar.ma2.Section;

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

  public GeoReferencedArray(String coverageName, DataType dataType, Array data, List<CoverageCoordAxis> axes, List<CoverageTransform> transforms, CoverageCoordSys.Type type) {
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
