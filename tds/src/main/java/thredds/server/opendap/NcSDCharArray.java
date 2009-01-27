// $Id: NcSDCharArray.java 51 2006-07-12 17:13:13Z caron $
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

package thredds.server.opendap;

import ucar.ma2.*;
import ucar.nc2.*;

import opendap.dap.*;
import opendap.dap.Server.*;

import java.io.IOException;
import java.io.EOFException;
import java.io.DataOutputStream;
import java.util.*;

/**
 * Wraps a netcdf char variable with rank > 1 as an SDArray.
 *
 * @author jcaron
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
        hasStride = hasStride || (getStride(i) > 1);
      }
      origin[n] = 0;
      shape[n] = strLen;

      a = ncVar.read(origin, shape);
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
        a = a.section(ranges);
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