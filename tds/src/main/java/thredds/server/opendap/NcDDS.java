/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.server.opendap;

import opendap.dap.DAPNode;
import ucar.nc2.*;
import ucar.ma2.DataType;

import opendap.servers.*;
import opendap.dap.BaseType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.NetcdfDataset;

import java.util.*;

/**
 * NcDDS is a specialization of ServerDDS for netcdf files.
 * This creates a ServerDDS from the netcdf file.
 *
 * @author jcaron
 */

public class NcDDS extends ServerDDS {

  // Handle the case of potential grids when the array has duplicate dims
  static protected final boolean HANDLE_DUP_DIM_GRIDS = true;

  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NcDDS.class);

  //private HashMap<String, BaseType> coordHash = new HashMap<String, BaseType>(50); // non grid coordinate variables
  // Track various subsets of the variables
  private Map<String, Variable> coordvars = new HashMap<>(50);
  private List<Variable> ddsvars = new ArrayList<>(50);   // list of currently active variables
  private Map<String, Variable> gridarrays = new HashMap<>(50);
  private Map<String, Variable> used = new HashMap<>(50);

  private Variable findVariable(String name) {
    for (Variable v : ddsvars) {
      if (v.getFullName().equals(name)) return v;
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

    if (ncfile instanceof NetcdfDataset)
      createFromDataset((NetcdfDataset) ncfile);
    else
      createFromFile(ncfile);
  }

  private void createFromFile(NetcdfFile ncfile) {
    // dup the variable set
    ddsvars = new ArrayList<>(ncfile.getVariables());

    // get coordinate variables
    for (Object o : ncfile.getDimensions()) {
      Dimension dim = (Dimension) o;
      Variable cv = findVariable(dim.getShortName());
      if ((cv != null) && cv.isCoordinateVariable()) {
        coordvars.put(dim.getShortName(), cv);
        if (log.isDebugEnabled())
          log.debug(" NcDDS adding coordinate variable " + cv.getFullName() + " for dimension " + dim.getShortName());
      }
    }

    // collect grid array variables and set of used (in grids) coordinate variables
    for (Variable v : ddsvars) {
      boolean isgridarray = (v.getRank() > 1) && (v.getDataType() != DataType.STRUCTURE) && (v.getParentStructure() == null);
      if (!isgridarray) continue;
      List<Dimension> dimset = v.getDimensions();
      int rank = dimset.size();
      for (int i = 0; isgridarray && i < rank; i++) {
        Dimension dim = dimset.get(i);
        if (dim.getShortName() == null)
          isgridarray = false;
        else {
          Variable gv = coordvars.get(dim.getShortName());
          if (gv == null)
            isgridarray = false;
        }
        if (HANDLE_DUP_DIM_GRIDS) {
          // Check for duplicate dims
          for (int j = i + 1; isgridarray && j < rank; j++) {
            if (dimset.get(j) == dim)
              isgridarray = false;
          }
        }
      }
      if (isgridarray) {
        gridarrays.put(v.getFullName(), v);
        for (Dimension dim : dimset) {
          Variable gv = coordvars.get(dim.getShortName());
          if (gv != null)
            used.put(gv.getFullName(), gv);
        }
      }
    }
    // remove the used coord vars from ddsvars (wrong for now; keep so that coord vars are top-level also)
    // for(Variable v: used.values()) ddsvars.remove(v);

    // Create the set of variables
    for (Object o1 : ddsvars) {
      Variable cv = (Variable) o1;
      BaseType bt;

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

  // take advantage of the work already done by NetcdfDataset
  private void createFromDataset(NetcdfDataset ncd) {
        // get coordinate variables, disjunct from variables
    for (CoordinateAxis axis : ncd.getCoordinateAxes()) {
      coordvars.put(axis.getShortName(), axis);
    }

    // dup the variable set
    ddsvars = new ArrayList<>(50);

    // collect grid array variables and set of coordinate variables used in grids
    for (Variable v : ncd.getVariables()) {
      if (coordvars.containsKey(v.getShortName())) continue;  // skip coordinate variables
      ddsvars.add(v);

      boolean isgridarray = (v.getRank() > 1) && (v.getDataType() != DataType.STRUCTURE) && (v.getParentStructure() == null);
      if (!isgridarray) continue;
      List<Dimension> dimset = v.getDimensions();
      int rank = dimset.size();
      for (int i = 0; isgridarray && i < rank; i++) {
        Dimension dim = dimset.get(i);
        if (dim.getShortName() == null)
          isgridarray = false;
        else {
          Variable gv = coordvars.get(dim.getShortName());
          if (gv == null)
            isgridarray = false;
        }
      }
      if (isgridarray) {
        gridarrays.put(v.getFullName(), v);
        for (Dimension dim : dimset) {
          Variable gv = coordvars.get(dim.getShortName());
          if (gv != null)
            used.put(gv.getFullName(), gv);
        }
      }
    }

        // Create the set of coordinates
    for (Variable cv : ncd.getCoordinateAxes()) {
      BaseType bt = createVariable(ncd, cv);
      addVariable(bt);
    }

    // Create the set of variables
    for (Variable cv : ddsvars) {
      BaseType bt = createVariable(ncd, cv);
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
    // no dim can be duplicated.
    boolean isGrid = (gridarrays.get(v.getFullName()) != null);
    NcSDArray arr = new NcSDArray(v, createScalarVariable(ncfile, v));
    if (isGrid) {
      ArrayList<BaseType> list = new ArrayList<>();
      list.add(arr); // Array is first element in the list
      List<Dimension> dimset = v.getDimensions();
      for (Dimension dim : dimset) {
        Variable v1 = used.get(dim.getShortName());
        assert (v1 != null);
        BaseType bt;
        if ((v1.getDataType() == DataType.CHAR))
          bt = (v1.getRank() > 1) ? new NcSDCharArray(v1) : new NcSDString(v1);
        else
          bt = new NcSDArray(v1, createScalarVariable(ncfile, v1));
        list.add(bt);
      }
      return new NcSDGrid(v.getShortName(), list);

    } else
      return arr;
  }

  private BaseType createStructure(NetcdfFile ncfile, Structure s) {
    ArrayList<BaseType> list = new ArrayList<>();
    for (Object o : s.getVariables()) {
      Variable nested = (Variable) o;
      list.add(createVariable(ncfile, nested));
    }
    return new NcSDStructure(s, list);
  }

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
