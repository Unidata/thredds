// $Id: NcDDS.java 51 2006-07-12 17:13:13Z caron $
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

package thredds.server.opendap;

import ucar.nc2.*;
import ucar.ma2.DataType;
import ucar.unidata.util.StringUtil;

import opendap.dap.Server.*;
import opendap.dap.BaseType;

import java.util.*;

/**
 * NcDDS is a specialization of ServerDDS for netcdf files.
 * This creates a ServerDDS from the netcdf file.
 *
 *   @version $Revision: 51 $
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
      List cvs = dim.getCoordinateVariables();
      if (cvs.size() > 0) {
        Variable cv = (Variable) cvs.get(0); // just taking the first one
        BaseType bt = new NcSDArray(cv, createScalarVariable(cv));
        if ((cv.getDataType() == DataType.CHAR) && (cv.getRank() > 1))
          bt = new NcSDCharArray(cv);
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
        bt = createVariable(v);
      addVariable(bt);
    }
  }

  // turn Variable into opendap variable
  private BaseType createVariable(Variable v) {
    BaseType bt;

    if (v.getRank() == 0)  // scalar
      bt = createScalarVariable(v);

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
      bt = createArray(v);

    return bt;

  }

  private BaseType createScalarVariable( Variable v) {
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
      return createStructure((Structure) v);
    else
      throw new UnsupportedOperationException("NcDDS Variable data type = "+dt);
  }

  private BaseType createArray( Variable v) {
    // all dimensions must have coord vars to be a grid, also must have the same name
    boolean isGrid = (v.getRank() > 1) && (v.getDataType() != DataType.STRUCTURE) && (v.getParentStructure() == null);
    Iterator iter = v.getDimensions().iterator();
    while (isGrid && iter.hasNext()) {
      Dimension dim = (Dimension) iter.next();
      if (dim.getCoordinateVariables().size() == 0)
        isGrid = false;
      else {
        Variable cv = (Variable) dim.getCoordinateVariables().get(0);
        if (!cv.getName().equals(dim.getName()))
          isGrid = false;
      }
    }

    NcSDArray arr = new NcSDArray( v, createScalarVariable(v));
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

  private BaseType createStructure( Structure s) {
    ArrayList<BaseType> list = new ArrayList<BaseType>();
    for (Object o : s.getVariables()) {
      Variable nested = (Variable) o;
      list.add(createVariable(nested));
    }
    return new NcSDStructure( s, list);
  }

  public static String escapeName(String vname) {
    // vname = StringUtil.replace(vname, '-', "_"); // LOOK Temporary workaround until opendap code fixed
    return StringUtil.escape(vname, NcDDS.DODScharset);
  }

}
