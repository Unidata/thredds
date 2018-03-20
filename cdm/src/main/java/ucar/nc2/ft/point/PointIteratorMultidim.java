/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.point;

import java.io.IOException;
import java.util.List;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataW;
import ucar.ma2.StructureMembers;
import ucar.nc2.Variable;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureIterator;

/**
 * A PointFeatureIterator using the "multidimensional representation".
 * Not currently used.
 * 
 * @author caron
 * @since Mar 26, 2008
 */
public abstract class PointIteratorMultidim implements PointFeatureIterator {
  // static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PointIteratorMultidim.class);

  protected abstract PointFeature makeFeature(int recnum, StructureData sdata);

  private List<Variable> vars;
  private StructureMembers members;
  private int outerIndex;

  private Filter filter;
  private int count, npts;
  private PointFeature feature;


  public PointIteratorMultidim(String name, List<Variable> vars, int outerIndex, Filter filter) {
    this.vars = vars;
    this.outerIndex = outerIndex;
    this.filter = filter;

    Variable v = vars.get(0);
    npts = v.getDimension(1).getLength();

    members = new StructureMembers(name);
    for (Variable var : vars) {
      int[] shape = var.getShape();
      int[] newShape = new int[shape.length - 2];
      System.arraycopy(shape, 2, newShape, 0, shape.length - 2);
      members.addMember(var.getShortName(), var.getDescription(), var.getUnitsString(), var.getDataType(), newShape);
    }
  }

  @Override
  public boolean hasNext() {
    while (count < npts) {
      StructureData sdata = nextStructureData();
      feature = makeFeature(count, sdata);
      count++;
      if (filter != null && !filter.filter(feature)) continue;
      return true;
    }
    feature = null;
    return false;
  }

  @Override
  public PointFeature next() {
    return feature;
  }

  private StructureData nextStructureData() {
    StructureDataW sdata = new StructureDataW(members);

    for (Variable var : vars) {
      Section s = new Section();
      try {
        s.appendRange(outerIndex, outerIndex);
        s.appendRange(count, count);
        for (int i = 2; i < var.getRank(); i++)
          s.appendRange(null);
        Array data = var.read(s);
        sdata.setMemberData(var.getShortName(), data);

      } catch (InvalidRangeException | IOException e) {
        throw new RuntimeException(e);
      }

    }

    return sdata;
  }

}

