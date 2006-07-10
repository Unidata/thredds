// $Id: NcSDCharArray.java,v 1.10 2006/04/20 22:25:21 caron Exp $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

package dods.servers.netcdf;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.unidata.util.StringUtil;

import dods.dap.Server.*;
import dods.dap.*;

import java.io.IOException;
import java.io.EOFException;
import java.io.DataOutputStream;
import java.util.*;

/**
 * Wraps a netcdf char variable with rank > 1 as an SDArray.
 *
 * @author jcaron
 * @version $Revision: 1.10 $
 */
public class NcSDCharArray extends SDArray implements HasNetcdfVariable {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NcSDCharArray.class);

  private boolean debugRead = false, debugReadDetail = false;
  private Variable ncVar = null;
  private int strLen = 1;

  /**
   * Constructor: Wraps a netcdf char variable (rank > 1) in a DODS SDArray.
   *
   * @param v : netcdf Variable
   */
  NcSDCharArray(Variable v) {
    super(NcDDS.escapeName(v.getShortName()));
    this.ncVar = v;
    if (v.getRank() < 1)
      throw new IllegalArgumentException("NcSDCharArray: rank must be > 1, var = " + v.getName());

    // set dimensions, eliminate last one
    List dims = v.getDimensions();
    for (int i = 0; i < dims.size(); i++) {
      Dimension dim = (Dimension) dims.get(i);
      if (i < dims.size() - 1)
        appendDim(dim.getLength(), dim.getName());
      else
        strLen = dim.getLength();
    }

    // set String type
    addVariable(new NcSDString(v.getShortName(), null));
  }

  public Variable getVariable() { return ncVar; }


  /**
   * Read the data values (parameters are ignored).
   * Use the start, stop and stride values that were set by the constraint evaluator.
   *
   * @param datasetName not used
   * @param specialO    not used
   * @return false (no more data to be read)
   * @throws IOException
   * @throws EOFException
   */
  public boolean read(String datasetName, Object specialO) throws IOException, EOFException {
    boolean hasStride = false;
    Array a;
    try {

      if (debugRead) {
        System.out.println("NcSDCharArray read " + ncVar.getName());
        for (int i = 0; i < numDimensions(); i++) {
          DArrayDimension d = getDimension(i);
          System.out.println(" " + d.getName() + " " + getStart(i) + " " + getStop(i) + " " + getStride(i));
        }
      }

      // set up the netcdf read
      int n = numDimensions();
      int[] origin = new int[n + 1];
      int[] shape = new int[n + 1];

      for (int i = 0; i < n; i++) {
        origin[i] = getStart(i);
        shape[i] = getStop(i) - getStart(i) + 1;
        hasStride = hasStride && (getStride(i) > 1);
      }
      origin[n] = 0;
      shape[n] = strLen;

      a = (Array) ncVar.read(origin, shape);
      if (debugRead) System.out.println("  Read = " + a.getSize() + " elems of type = " + a.getElementType());

      // deal with strides using a section
      if (hasStride) {
        ArrayList ranges = new ArrayList();
        for (int i = 0; i < n; i++) {
          int s = getStride(i);
          if (s > 1) {  // otherwise null, means "take all elements"
            ranges.add(new Range(0, shape[i], s));
            if (debugRead) System.out.println(" Section dim " + i + " stride = " + s);
          }
        }
        ranges.add(null); //  get all
        a = (Array) a.section(ranges);
        if (debugRead) System.out.println("   section size " + a.getSize());
      }

    } catch (InvalidParameterException e) {
      log.error("read char array", e);
      throw new IllegalStateException("NcSDCharArray InvalidParameterException");
    } catch (InvalidRangeException e) {
      log.error("read char array", e);
      throw new IllegalStateException("NcSDCharArray InvalidRangeException");
    }

    setData(a);
    return (false);
  }

  public void setData(Array data) {
    PrimitiveVector pv = getPrimitiveVector();
    if (debugRead)
      System.out.println(" PrimitiveVector type = " + pv.getTemplate() +
          " pv type = " + pv.getClass().getName());

    // this is the case of netcdf char arrays with rank > 1;
    // these become DODS Arrays of Strings
    ArrayChar ca = (ArrayChar) data;
    ArrayChar.StringIterator siter = ca.getStringIterator();
    int nelems = siter.getNumElems();
    if (debugRead) System.out.println(" set Strings = " + nelems);

    BaseTypePrimitiveVector btpv = (BaseTypePrimitiveVector) pv;
    btpv.setLength(nelems);
    for (int i = 0; i < nelems; i++) {
      String val = siter.next();
      NcSDString ds = new NcSDString("", val);
      btpv.setValue(i, ds);
      if (debugReadDetail) System.out.println("  s = " + val + " == " + ds.getValue());
    }
    if (debugRead) System.out.println("  PrimitiveVector len = " + pv.getLength() + " type = " + pv.getTemplate());

    setRead(true);
  }

  public void serialize(DataOutputStream sink, StructureData sdata, StructureMembers.Member m) throws IOException {
    setData( sdata.getArray( m));
    externalize( sink);
  }
}