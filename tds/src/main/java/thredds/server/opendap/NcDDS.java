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

import ucar.nc2.*;
import ucar.ma2.DataType;

import opendap.dap.Server.*;
import opendap.dap.BaseType;
import ucar.unidata.util.StringUtil;

import java.util.*;

/**
 * NcDDS is a specialization of ServerDDS for netcdf files.
 * This creates a ServerDDS from the netcdf file.
 *
 *   @author jcaron
 */

public class NcDDS extends ServerDDS implements Cloneable {
  static protected org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NcDDS.class);
  static String DODScharset = "_!~*'-\"";

  private HashMap<String,BaseType> dimHash = new HashMap<String,BaseType>(50); // not copied on clone operation

  /** Constructor
   * @param name name of the dataset, at bottom of DDS
   * @param ncfile  create DDS from this
   */
  NcDDS( String name, NetcdfFile ncfile) {
    super( StringUtil.escape( name, ""));

    // get coordinate variables
    // LOOK: this should get optimized to store data once
    for (Object o : ncfile.getDimensions()) {
      Dimension dim = (Dimension) o;
      Variable cv = ncfile.findVariable(dim.getName()); // LOOK WRONG
      if ((cv != null) && cv.isCoordinateVariable()) {
        BaseType bt = null;
        if ((cv.getDataType() == DataType.CHAR))
          bt = (cv.getRank() > 1) ? new NcSDCharArray(cv) : new NcSDString(cv);
        else
          bt = new NcSDArray(cv, createScalarVariable(ncfile, cv));

        dimHash.put(dim.getName(), bt);
        if (log.isDebugEnabled())
          log.debug(" NcDDS adding coordinate variable " + cv.getName() + " for dimension " + dim.getName());
      }
    }

    // add variables
    for (Object o1 : ncfile.getVariables()) {
      Variable v = (Variable) o1;
      BaseType bt = null;

      if (v.isCoordinateVariable()) {
        bt = dimHash.get(v.getName());
        if (bt == null)
          log.error("NcDDS: Variable " + v.getName() + " missing coordinate variable in hash; dataset=" + name);
      }

      if (bt == null)
        bt = createVariable(ncfile, v);
      addVariable(bt);
    }
  }

  // turn Variable into opendap variable
  private BaseType createVariable(NetcdfFile ncfile, Variable v) {
    BaseType bt;

    if (v.getRank() == 0)  // scalar
      bt = createScalarVariable(ncfile, v);

    else if (v.getDataType() == DataType.CHAR) {
      if (v.getRank() > 1)
        bt = new NcSDCharArray(v);
      else
        bt = new NcSDString(v);

    } else if (v.getDataType() == DataType.STRING) {
      if (v.getRank() == 0)
        bt = new NcSDString(v);
      else
        bt = new NcSDArray(v, new NcSDString(v));

    } else  // non-char multidim array
      bt = createArray(ncfile, v);

    return bt;

  }

  private BaseType createScalarVariable( NetcdfFile ncfile, Variable v) {
    DataType dt = v.getDataType();
    if (dt == DataType.DOUBLE)
       return new NcSDFloat64(v);
    else if (dt == DataType.FLOAT)
      return new NcSDFloat32(v);
    else if (dt == DataType.INT)
      return v.isUnsigned() ? new NcSDUInt32(v) : new NcSDInt32(v);
    else if (dt == DataType.SHORT)
      return v.isUnsigned() ? new NcSDUInt16(v) : new NcSDInt16(v);
    else if (dt == DataType.BYTE)
      return new NcSDByte(v);
    else if (dt == DataType.CHAR)
        return new NcSDString(v);
    else if (dt == DataType.STRING)
      return new NcSDString(v);
    else if (dt == DataType.STRUCTURE)
      return createStructure(ncfile, (Structure) v);
    else
      throw new UnsupportedOperationException("NcDDS Variable data type = "+dt);
  }

  private BaseType createArray( NetcdfFile ncfile, Variable v) {
    // all dimensions must have coord vars to be a grid, also must have the same name
    boolean isGrid = (v.getRank() > 1) && (v.getDataType() != DataType.STRUCTURE) && (v.getParentStructure() == null);
    Iterator iter = v.getDimensions().iterator();
    while (isGrid && iter.hasNext()) {
      Dimension dim = (Dimension) iter.next();
      Variable cv = ncfile.findVariable(dim.getName());  // LOOK WRONG
      if ((cv == null) || !cv.isCoordinateVariable())
        isGrid = false;
    }

    NcSDArray arr = new NcSDArray( v, createScalarVariable(ncfile, v));
    if (!isGrid)
      return arr;

    ArrayList<BaseType> list = new ArrayList<BaseType>();
    list.add( arr);
    iter = v.getDimensions().iterator();
    while (iter.hasNext()) {
      Dimension dim = (Dimension) iter.next();
      list.add (dimHash.get( dim.getName()));
    }

    return new NcSDGrid( v.getShortName(), list);
  }

  private BaseType createStructure( NetcdfFile ncfile, Structure s) {
    ArrayList<BaseType> list = new ArrayList<BaseType>();
    for (Object o : s.getVariables()) {
      Variable nested = (Variable) o;
      list.add(createVariable(ncfile, nested));
    }
    return new NcSDStructure( s, list);
  }

  public static String escapeName(String vname) {
    // vname = StringUtil.replace(vname, '-', "_"); // LOOK Temporary workaround until opendap code fixed
    return StringUtil.escape(vname, NcDDS.DODScharset);
  }

}
