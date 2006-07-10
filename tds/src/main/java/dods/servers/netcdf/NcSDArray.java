// $Id: NcSDArray.java,v 1.12 2006/04/20 22:25:21 caron Exp $
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
import java.util.Iterator;
import java.util.ArrayList;

/**
 * Wraps a netcdf variable with rank > 0 as an SDArray.
 * For char arrays, use NcSDString (rank 0 or 1) or NcSDCharArray (rank > 1).
 *
 * @author jcaron
 * @version $Revision: 1.12 $
 * @see NcSDCharArray
 */
public class NcSDArray extends SDArray implements HasNetcdfVariable {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NcSDArray.class);

  private boolean debug = false, debugRead = false, showTiming = false;
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
    Iterator iter = v.getDimensions().iterator();
    while (iter.hasNext()) {
      Dimension dim = (Dimension) iter.next();
      appendDim(dim.getLength(), dim.getName());
    }

    // this seems to be how you set the type
    // it creates the "primitive vector"
    addVariable(bt);
    this.elemType = bt;
  }

  public Variable getVariable() { return ncVar; }

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
  public boolean read(String datasetName, Object specialO) throws IOException, EOFException {
    long tstart = System.currentTimeMillis();

    Array a;
    try {
      if (log.isDebugEnabled())
        log.debug(getRequestedRange());

      // set up the netcdf read
      int n = numDimensions();
      ArrayList ranges = new ArrayList(n);
      for (int i = 0; i < n; i++)
        ranges.add( new Range(getStart(i), getStop(i), getStride(i)));

      try {
        a = ncVar.read(ranges);

       } catch (java.lang.ArrayIndexOutOfBoundsException t) {
        log.error(getRequestedRange(), t);
        throw new RuntimeException("NcSDArray ArrayIndexOutOfBoundsException="+t.getMessage());
      }

      if (debug) System.out.println("  NcSDArray Read " + getName() + " " + a.getSize() + " elems of type = " + a.getElementType());
      if (debugRead) System.out.println("  Read = " + a.getSize() + " elems of type = " + a.getElementType());
      if (log.isDebugEnabled()) {
        long tookTime = System.currentTimeMillis() - tstart;
        log.debug("NcSDArray read array: " + tookTime * .001 + " seconds");
      }

    } catch (InvalidParameterException e) {
      log.error(getRequestedRange(), e);
      throw new IllegalStateException("NcSDArray InvalidParameterException="+e.getMessage());

    } catch (InvalidRangeException e) {
      log.error(getRequestedRange(), e);
      throw new IllegalStateException("NcSDArray InvalidRangeException="+e.getMessage());
    }
    setData(a);

    if (debugRead) System.out.println(" PrimitiveVector len = " + getPrimitiveVector().getLength() +
        " type = " + getPrimitiveVector().getTemplate());

    return (false);
  }

  private String getRequestedRange() {
    try {
      StringBuffer sbuff = new StringBuffer();
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
      // this is optimized to eliminate the copy
      // note we have to expose the internal ma2.Array storage :(
      pv.setInternalStorage(data.getStorage());
    }

    setRead(true);
  }

   public void serialize(DataOutputStream sink, StructureData sdata, StructureMembers.Member m) throws IOException {
     long tstart = System.currentTimeMillis();

     setData( sdata.getArray( m));
     externalize( sink);

    if (log.isDebugEnabled()) {
      long tookTime = System.currentTimeMillis() - tstart;
      log.debug("NcSDArray serialize: " + tookTime * .001 + " seconds");
    }
  }

}
