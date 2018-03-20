/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: NcSDCharArray.java 51 2006-07-12 17:13:13Z caron $


package thredds.server.opendap;

import ucar.ma2.*;
import ucar.nc2.*;

import opendap.dap.*;
import opendap.servers.*;

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

  private static final boolean debugRead = false, debugReadDetail = false;
  private Variable ncVar = null;
  private int strLen = 1;

  /**
   * Constructor: Wraps a netcdf char variable (rank > 1) in a DODS SDArray.
   *
   * @param v : netcdf Variable
   */
  NcSDCharArray(Variable v) {
      super(Variable.getDAPName(v));
    this.ncVar = v;
    if (v.getRank() < 1)
      throw new IllegalArgumentException("NcSDCharArray: rank must be > 1, var = " + v.getFullName());

    // set dimensions, eliminate last one
    List dims = v.getDimensions();
    for (int i = 0; i < dims.size(); i++) {
      Dimension dim = (Dimension) dims.get(i);
      if (i < dims.size() - 1)
        appendDim(dim.getLength(), dim.getShortName());
      else
        strLen = dim.getLength();
    }

    // set String type
    addVariable(new NcSDString(Variable.getDAPName(v), null));
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
  public boolean read(String datasetName, Object specialO) throws IOException {
    boolean hasStride = false;
    Array a;
    try {

      if (debugRead) {
        System.out.println("NcSDCharArray read " + ncVar.getFullName());
        for (int i = 0; i < numDimensions(); i++) {
          DArrayDimension d = getDimension(i);
          System.out.println(" " + d.getEncodedName() + " " + getStart(i) + " " + getStop(i) + " " + getStride(i));
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
        List<Range> ranges = new ArrayList<>();
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

    } catch (InvalidDimensionException e) {
      log.error("read char array", e);
      throw new IllegalStateException("NcSDCharArray InvalidDimensionException");
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
