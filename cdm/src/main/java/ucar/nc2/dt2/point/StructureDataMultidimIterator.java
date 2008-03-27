/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.dt2.point;

import ucar.nc2.dt2.PointFeatureIterator;
import ucar.nc2.dt2.PointFeature;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.ma2.*;

import java.io.IOException;
import java.util.List;

/**
 * A StructureDataIterator using the "multidimensional representation".
 *
 * @author caron
 * @since Mar 26, 2008
 */
public abstract class StructureDataMultidimIterator implements PointFeatureIterator {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StructureDataMultidimIterator.class);

  private List<Variable> vars;
  private StructureMembers members;
  private int outerIndex;

  private Filter filter;
  private int count, npts;
  private PointFeature feature;

  protected abstract PointFeature makeFeature(int recnum, StructureData sdata) throws IOException;

  public StructureDataMultidimIterator(String name, List<Variable> vars, int outerIndex, Filter filter) {
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

  public boolean hasNext() throws IOException {
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

  public PointFeature nextData() throws IOException {
    return feature;
  }

  private StructureData nextStructureData() throws IOException {
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

      } catch (InvalidRangeException e) {
        e.printStackTrace();
      }

    }

    return sdata;
  }

  public void setBufferSize(int bytes) {
    // no op
  }
}

