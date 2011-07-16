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

import opendap.dap.DAPNode;
import ucar.nc2.*;
import ucar.ma2.DataType;

import opendap.Server.*;
import opendap.dap.BaseType;

import java.util.*;

/**
 * NcDDS is a specialization of ServerDDS for netcdf files.
 * This creates a ServerDDS from the netcdf file.
 *
 * @author jcaron
 */

public class NcDDS extends ServerDDS {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NcDDS.class);
  static private String DODScharset = "_!~*'-\""; // Chars (other than alphanum)
                                                  // that are legal in opendap names

  //private HashMap<String, BaseType> coordHash = new HashMap<String, BaseType>(50); // non grid coordiinate variables
  // Track various subsets of the variables
  private Hashtable<String, Variable> coordvars = new Hashtable<String, Variable>(50);
  private Vector<Variable> ddsvars = new Vector<Variable>(50);   // list of currently active variables
  private Hashtable<String, Variable> gridarrays = new Hashtable<String, Variable>(50);
  private Hashtable<String, Variable> used = new Hashtable<String, Variable>(50);

  private Variable findvariable(String name)
  {
      for (Variable v: ddsvars) {
          if(v.getFullName().equals(name)) return v;
      }
      return null;
  }
  /**
   * Constructor
   *
   * @param name   name of the dataset, at bottom of DDS
   * @param ncfile create DDS from this
   */
  public NcDDS(String name, NetcdfFile ncfile) {
     super((name));

    // dup the variable set
    for (Object o : ncfile.getVariables()) {
      Variable v = (Variable) o;
      ddsvars.add(v);
    }

    // get coordinate variables
    for (Object o : ncfile.getDimensions()) {
      Dimension dim = (Dimension) o;
      Variable cv = findvariable(dim.getName());
      if ((cv != null) && cv.isCoordinateVariable()) {
        coordvars.put(dim.getName(),cv);
        if (log.isDebugEnabled())
          log.debug(" NcDDS adding coordinate variable " + cv.getFullName() + " for dimension " + dim.getName());
      }
    }

     // collect grid array variables and set of used (in grids) coordinate variables
     for (Variable v : ddsvars) {
            boolean isgridarray = (v.getRank() > 1) && (v.getDataType() != DataType.STRUCTURE) && (v.getParentStructure() == null);
            if(!isgridarray) continue;
            Iterator iter = v.getDimensions().iterator();
            while (isgridarray && iter.hasNext()) {
                Dimension dim = (Dimension) iter.next();
                if (dim.getName() == null)
                  isgridarray = false;
                else {
                  Variable gv = coordvars.get(dim.getName());
                  if (gv == null)
                     isgridarray = false;
		}
            }
            if(isgridarray)   {
                gridarrays.put(v.getFullName(),v);
                for(iter=v.getDimensions().iterator();iter.hasNext();) {
                    Dimension dim = (Dimension) iter.next();
                    Variable gv = coordvars.get(dim.getName());
                    if (gv != null)
                        used.put(gv.getFullName(),gv);
                }
            }
     }
      // remove the used coord vars from ddsvars (wrong for now; keep so that coord vars are top-level also)
     // for(Variable v: used.values()) ddsvars.remove(v);

    // Create the set of variables
    for (Object o1 : ddsvars) {
      Variable cv = (Variable) o1;
      BaseType bt = null;

      if (false && cv.isCoordinateVariable()) {
        if ((cv.getDataType() == DataType.CHAR))
          bt = (cv.getRank() > 1) ? new NcSDCharArray(cv) : new NcSDString(cv);
        else
          bt = new NcSDArray(cv, createScalarVariable(ncfile, cv));
      }
      //if (bt == null)
        bt = createVariable(ncfile, cv);
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

  private BaseType createScalarVariable(NetcdfFile ncfile, Variable v) {
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
      throw new UnsupportedOperationException("NcDDS Variable data type = " + dt);
  }

  private BaseType createArray(NetcdfFile ncfile, Variable v) {
    // all dimensions must have coord vars to be a grid, also must have the same name
    boolean isGrid = (gridarrays.get(v.getFullName()) != null);
    NcSDArray arr = new NcSDArray(v, createScalarVariable(ncfile, v));
    if (!isGrid)
        return arr;

     // isgrid == true
    ArrayList<BaseType> list = new ArrayList<BaseType>();
    list.add(arr); // Array is first element in the list
    for(Iterator iter = v.getDimensions().iterator();iter.hasNext();) {
      Dimension dim = (Dimension) iter.next();
      Variable v1 = used.get(dim.getName());
      assert(v1 != null);
      BaseType bt = null;
      if ((v1.getDataType() == DataType.CHAR))
        bt = (v1.getRank() > 1) ? new NcSDCharArray(v1) : new NcSDString(v1);
      else
        bt = new NcSDArray(v1, createScalarVariable(ncfile, v1));
      assert(bt != null);
      list.add(bt) ;
    }
    return new NcSDGrid(v.getShortName(), list);
  }

  private BaseType createStructure(NetcdfFile ncfile, Structure s) {
    ArrayList<BaseType> list = new ArrayList<BaseType>();
    for (Object o : s.getVariables()) {
      Variable nested = (Variable) o;
      list.add(createVariable(ncfile, nested));
    }
    return new NcSDStructure(s, list);
  }

  /*
  public static String escapeName(String vname) {
    // vname = StringUtil.replace(vname, '-', "_"); // LOOK Temporary workaround until opendap code fixed
    String newname = StringUtil.escape(vname, NcDDS.DODScharset);
      return newname;
  }
  */

  /**
   * Returns a clone of this <code>?</code>.
   * See BaseType.cloneDAG()
   *
   * @param map track previously cloned nodes
   * @return a clone of this object.
   */
  public DAPNode cloneDAG(CloneMap map)
          throws CloneNotSupportedException {
    NcDDS d = (NcDDS) super.cloneDAG(map);
    d.coordvars = coordvars;
    return d;
  }

}
