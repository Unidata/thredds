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

import opendap.dap.Server.*;
import opendap.dap.BaseType;
import opendap.dap.DArrayDimension;
import opendap.dap.PrimitiveVector;

import java.io.IOException;
import java.io.EOFException;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Wraps a netcdf variable with rank > 0 as an SDArray.
 * For char arrays, use NcSDString (rank 0 or 1) or NcSDCharArray (rank > 1).
 *
 * @author jcaron
 * @see NcSDCharArray
 */
public class NcSDArray extends SDArray implements HasNetcdfVariable {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NcSDArray.class);

  private boolean debug = false, debugRead = false;
  private Variable ncVar = null;
  protected BaseType elemType;

  /**
   * Constructor: Wraps a netcdf variable in a DODS SDArray.
   *
   * @param v  : netcdf Variable
   * @param bt : DODS element type
   */
  NcSDArray(Variable v, BaseType bt) {
    super(NcDDS.escapeName(v.getShortName()));
    this.ncVar = v;

    // set dimensions
    for (Dimension dim : v.getDimensions()) {
      appendDim(dim.getLength(), dim.getName());
    }

    // this seems to be how you set the type
    // it creates the "primitive vector"
    addVariable(bt);
    this.elemType = bt;
  }

  public Variable getVariable() {
    return ncVar;
  }

  /**
   * Read the data values (parameters are ignored).
   * Use the start, stop and stride values, typically set by the constraint evaluator.
   *
   * @param datasetName not used
   * @param specialO    not used
   * @return false (no more data to be read)
   * @throws IOException
   * @throws EOFException
   */
  public boolean read(String datasetName, Object specialO) throws IOException {
    long tstart = System.currentTimeMillis();

    Array a;
    try {
      if (log.isDebugEnabled())
        log.debug(getRequestedRange());

      // set up the netcdf read
      int n = numDimensions();
      List<Range> ranges = new ArrayList<Range>(n);
      for (int i = 0; i < n; i++)
        ranges.add(new Range(getStart(i), getStop(i), getStride(i)));

      try {
        a = ncVar.read(ranges);

      } catch (java.lang.ArrayIndexOutOfBoundsException t) {
        log.error(getRequestedRange(), t);
        throw new RuntimeException("NcSDArray java.lang.ArrayIndexOutOfBoundsException=" + t.getMessage()+
            " for request= "+ getRequestedRange()+" dataset= "+ datasetName);
      }

      if (debug)
        System.out.println("  NcSDArray Read " + getName() + " " + a.getSize() + " elems of type = " + a.getElementType());
      if (debugRead) System.out.println("  Read = " + a.getSize() + " elems of type = " + a.getElementType());
      if (log.isDebugEnabled()) {
        long tookTime = System.currentTimeMillis() - tstart;
        log.debug("NcSDArray read array: " + tookTime * .001 + " seconds");
      }

    } catch (InvalidParameterException e) {
      log.error(getRequestedRange(), e);
      throw new IllegalStateException("NcSDArray InvalidParameterException=" + e.getMessage());

    } catch (InvalidRangeException e) {
      log.error(getRequestedRange(), e);
      throw new IllegalStateException("NcSDArray InvalidRangeException=" + e.getMessage());
    }
    setData(a);

    if (debugRead) System.out.println(" PrimitiveVector len = " + getPrimitiveVector().getLength() +
            " type = " + getPrimitiveVector().getTemplate());

    return (false);
  }

  private String getRequestedRange() {
    try {
      StringBuilder sbuff = new StringBuilder();
      sbuff.append("NcSDArray read " + ncVar.getName());
      for (int i = 0; i < numDimensions(); i++) {
        DArrayDimension d = getDimension(i);
        sbuff.append(" " + d.getName() + "(" + getStart(i) + "," + getStride(i) + "," + getStop(i) + ")");
      }
      return sbuff.toString();

    } catch (InvalidParameterException e) {
      e.printStackTrace();
      return e.getMessage();
    }
  }

  public void setData(Array data) {
    PrimitiveVector pv = getPrimitiveVector();
    if (debugRead)
      System.out.println(" PrimitiveVector type = " + pv.getTemplate() +
              " pv type = " + pv.getClass().getName());

    if (ncVar.getDataType() == DataType.STRING) {
      int size = (int) data.getSize();
      NcSDString[] dodsBT = new NcSDString[size];
      IndexIterator ii = data.getIndexIterator();
      int count = 0;
      while (ii.hasNext()) {
        dodsBT[count++] = new NcSDString(ncVar.getShortName(), (String) ii.getObjectNext());
      }
      pv.setInternalStorage(dodsBT);

    } else if (ncVar.getDataType() == DataType.STRUCTURE) {
      NcSDStructure sds = (NcSDStructure) pv.getTemplate();

      int size = (int) data.getSize();
      NcSDStructure[] dodsBT = new NcSDStructure[size];

      IndexIterator ii = data.getIndexIterator();
      int count = 0;
      while (ii.hasNext()) {
        StructureData sdata = (StructureData) ii.getObjectNext();
        dodsBT[count] = new NcSDStructure(sds, sdata); // stupid replication - need to override externalize
        count++;
      }
      pv.setInternalStorage(dodsBT);

    } else {

      // copy the data into the PrimitiveVector
      // this is optimized to (possibly) eliminate the copy
      Object pa = data.get1DJavaArray(data.getElementType());
      pv.setInternalStorage(pa);
    }

    setRead(true);
  }

  public void serialize(DataOutputStream sink, StructureData sdata, StructureMembers.Member m) throws IOException {
    long tstart = System.currentTimeMillis();

    setData(sdata.getArray(m));
    externalize(sink);

    if (log.isDebugEnabled()) {
      long tookTime = System.currentTimeMillis() - tstart;
      log.debug("NcSDArray serialize: " + tookTime * .001 + " seconds");
    }
  }

}
