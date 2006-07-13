// $Id:DODSVariable.java 51 2006-07-12 17:13:13Z caron $
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
 * along with this library; if not, strlenwrite to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.dods;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.Attribute;
import ucar.unidata.util.StringUtil;
import dods.dap.*;

import java.util.*;

/**
 * A read-only DODS-netCDF Variable. Same as a ucar.nc2.Variable except that
 * it might have type boolean or long. Note that DODS DUInt32 widened to long and
 * DODS DUInt16 widened to int.
 *
 * @see ucar.nc2.Variable
 * @author caron
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */

public class DODSVariable extends ucar.nc2.Variable {
  protected String CE;        // projection is allowed
  protected DODSNetcdfFile dodsfile; // so we dont have to cast everywhere
  protected String dodsShortName;

  // used by subclasses and the other constructors
  DODSVariable( DODSNetcdfFile dodsfile, Group parentGroup, Structure parentStructure, String shortName) {
    super(dodsfile, parentGroup, parentStructure, StringUtil.unescape( shortName));
    this.dodsfile = dodsfile;
    this.dodsShortName = shortName;
  }

    /* copy constructor, display name change
  DODSVariable( DODSVariable from) {
    super( from.getName(), from);
  } */

    // use when a dods variable is a scalar
  DODSVariable( DODSNetcdfFile dodsfile, Group parentGroup, Structure parentStructure, String shortName, dods.dap.BaseType dodsScalar) {
    super(dodsfile, parentGroup, parentStructure, StringUtil.unescape( shortName));
    this.dodsfile = dodsfile;
    this.dodsShortName = shortName;

    setDataType( DODSNetcdfFile.convertToNCType( dodsScalar));
    if (DODSNetcdfFile.isUnsigned( dodsScalar)) {
      addAttribute(new Attribute("_unsigned", "true"));
    }

    // check for netcdf char array
    Dimension strlenDim = null;
    if ((dataType == DataType.STRING) &&
        (null != (strlenDim = dodsfile.getNetcdfStrlenDim( this)))) {

      ArrayList dims = new ArrayList();
      if (strlenDim.getLength() != 0)
        dims.add( dodsfile.getSharedDimension( parentGroup, strlenDim));
      setDimensions(dims);
      setDataType( DataType.CHAR);

    } else {
      shape = new int[0];
    }
  }

   // use when a dods variable is an Array, rank > 0
  DODSVariable( DODSNetcdfFile dodsfile, Group parentGroup, Structure parentStructure, String shortName, DArray dodsArray,
                dods.dap.BaseType elemType ) {

    super(dodsfile, parentGroup, parentStructure, StringUtil.unescape( shortName));
    this.dodsfile = dodsfile;
    this.dodsShortName = shortName;

    setDataType( DODSNetcdfFile.convertToNCType( elemType));
    if (DODSNetcdfFile.isUnsigned( elemType)) {
      addAttribute(new Attribute("_unsigned", "true"));
    }

    ArrayList dims = dodsfile.constructDimensions( parentGroup, dodsArray);

    // check for netcdf char array
    Dimension strlenDim = null;
    if ((dataType == DataType.STRING) &&
        (null != (strlenDim = dodsfile.getNetcdfStrlenDim( this)))) {

      if (strlenDim.getLength() != 0)
        dims.add( dodsfile.getSharedDimension( parentGroup, strlenDim));
      setDataType( DataType.CHAR);
    }

    setDimensions(dims);
  }

  // need package access
  protected void calcIsCoordinateVariable() { super.calcIsCoordinateVariable(); }
 
  protected void setCE( String CE ){ this.CE = CE; }
  protected boolean hasCE(){ return CE != null; }
  protected String nameWithCE() { return hasCE() ? getShortName() + CE : getShortName(); }

  protected String getDODSshortName() { return dodsShortName; }

}