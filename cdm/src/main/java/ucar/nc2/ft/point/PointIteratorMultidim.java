/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.ft.point;

import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.Variable;
import ucar.ma2.*;

import java.io.IOException;
import java.util.List;

/**
 * A PointFeatureIterator using the "multidimensional representation".
 * Not currently used.
 * 
 * @author caron
 * @since Mar 26, 2008
 */
public abstract class PointIteratorMultidim implements PointFeatureIterator {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PointIteratorMultidim.class);

  protected abstract PointFeature makeFeature(int recnum, StructureData sdata) throws IOException;

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

  public PointFeature next() throws IOException {
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

