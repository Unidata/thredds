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
package ucar.nc2.dods;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.Attribute;
import opendap.dap.*;

import java.util.*;

/**
 * A read-only DODS-netCDF Variable. Same as a ucar.nc2.Variable except that
 * it might have type boolean or long. Note that DODS DUInt32 widened to long and
 * DODS DUInt16 widened to int.
 *
 * @see ucar.nc2.Variable
 * @author caron
 */

public class DODSVariable extends ucar.nc2.Variable {
  protected String CE;        // projection is allowed
  protected DODSNetcdfFile dodsfile; // so we dont have to cast everywhere
  protected String dodsShortName;

  // used by subclasses and the other constructors
  DODSVariable( DODSNetcdfFile dodsfile, Group parentGroup, Structure parentStructure, String dodsShortName, DodsV dodsV) {
    super(dodsfile, parentGroup, parentStructure, DODSNetcdfFile.makeNetcdfName( dodsShortName));
    this.dodsfile = dodsfile;
    this.dodsShortName = dodsShortName;
    setSPobject(dodsV);
  }

    // use when a dods variable is a scalar
  DODSVariable( DODSNetcdfFile dodsfile, Group parentGroup, Structure parentStructure, String dodsShortName, opendap.dap.BaseType dodsScalar, DodsV dodsV) {
    super(dodsfile, parentGroup, parentStructure, DODSNetcdfFile.makeNetcdfName( dodsShortName));
    this.dodsfile = dodsfile;
    this.dodsShortName = dodsShortName;

    setDataType( DODSNetcdfFile.convertToNCType( dodsScalar));
    if (DODSNetcdfFile.isUnsigned( dodsScalar)) {
      addAttribute(new Attribute("_Unsigned", "true"));
    }

    // check for netcdf char array
    Dimension strlenDim;
    if ((dataType == DataType.STRING) && (null != (strlenDim = dodsfile.getNetcdfStrlenDim( this)))) {

      List<Dimension> dims = new ArrayList<Dimension>();
      if (strlenDim.getLength() != 0)
        dims.add( dodsfile.getSharedDimension( parentGroup, strlenDim));
      setDimensions(dims);
      setDataType( DataType.CHAR);

    } else {
      shape = new int[0];
    }

    setSPobject(dodsV);
  }

   // use when a dods variable is an Array, rank > 0
  DODSVariable( DODSNetcdfFile dodsfile, Group parentGroup, Structure parentStructure, String dodsShortName, DArray dodsArray,
                opendap.dap.BaseType elemType, DodsV dodsV ) {

    super(dodsfile, parentGroup, parentStructure, DODSNetcdfFile.makeNetcdfName( dodsShortName));
    this.dodsfile = dodsfile;
    this.dodsShortName = dodsShortName;

    setDataType( DODSNetcdfFile.convertToNCType( elemType));
    if (DODSNetcdfFile.isUnsigned( elemType)) {
      addAttribute(new Attribute("_Unsigned", "true"));
    }

    List<Dimension> dims = dodsfile.constructDimensions( parentGroup, dodsArray);

    // check for netcdf char array
    Dimension strlenDim;
    if ((dataType == DataType.STRING) &&
        (null != (strlenDim = dodsfile.getNetcdfStrlenDim( this)))) {

      if (strlenDim.getLength() != 0)
        dims.add( dodsfile.getSharedDimension( parentGroup, strlenDim));
      setDataType( DataType.CHAR);
    }

    setDimensions(dims);
    setSPobject(dodsV);
  }

  // for section, slice
  @Override
  protected Variable copy() {
    return new DODSVariable(this);
  }

  protected DODSVariable(DODSVariable from) {
    super(from);

    dodsfile = from.dodsfile;
    dodsShortName = from.dodsShortName;
    CE = from.CE;
  }


  // need package access
  // protected void calcIsCoordinateVariable() { super.calcIsCoordinateVariable(); }

  protected void setCE( String CE ){ this.CE = CE; }
  protected boolean hasCE(){ return CE != null; }
  protected String nameWithCE() { return hasCE() ? getShortName() + CE : getShortName(); }

  protected String getDODSshortName() { return dodsShortName; }
}