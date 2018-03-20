/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dods;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.Group;
import ucar.nc2.dods.DODSNode;


import opendap.dap.*;
import ucar.nc2.constants.CDM;

import java.util.*;

/**
 * A read-only DODS-netCDF Variable. Same as a ucar.nc2.Variable except that
 * it might have type boolean or long. Note that DODS DUInt32 widened to long and
 * DODS DUInt16 widened to int.
 *
 * @see ucar.nc2.Variable
 * @author caron
 */

public class DODSVariable extends ucar.nc2.Variable implements DODSNode {
  protected String CE;        // projection is allowed
  protected DODSNetcdfFile dodsfile; // so we dont have to cast everywhere

  // used by subclasses and the other constructors
  DODSVariable( DODSNetcdfFile dodsfile, Group parentGroup, Structure parentStructure, String dodsShortName, DodsV dodsV) {
    super(dodsfile, parentGroup, parentStructure, DODSNetcdfFile.makeShortName(dodsShortName));
    setDODSName(DODSNetcdfFile.makeDODSName(dodsShortName));
    this.dodsfile = dodsfile;
    setSPobject(dodsV);
  }

    // use when a dods variable is a scalar
  DODSVariable( DODSNetcdfFile dodsfile, Group parentGroup, Structure parentStructure, String dodsShortName, opendap.dap.BaseType dodsScalar, DodsV dodsV) {
    super(dodsfile, parentGroup, parentStructure, DODSNetcdfFile.makeShortName(dodsShortName));
    setDODSName(DODSNetcdfFile.makeDODSName(dodsShortName));
    this.dodsfile = dodsfile;
    setDataType( dodsV.getDataType());
    /* if (DODSNetcdfFile.isUnsigned( dodsScalar)) {
      addAttribute(new DODSAttribute(CDM.UNSIGNED, "true"));
    } */

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

    // name is already properly decoded: super(dodsfile, parentGroup, parentStructure, DODSNetcdfFile.makeNetcdfName( dodsShortName));
    super(dodsfile, parentGroup, parentStructure,dodsShortName);
    setDODSName(DODSNetcdfFile.makeDODSName(dodsShortName));
    this.dodsfile = dodsfile;
    setDataType( dodsV.getDataType());
    /* if (DODSNetcdfFile.isUnsigned( elemType)) {
      // create _Unsigned attribute; may be overridden when attributes are read
      addAttribute(new DODSAttribute(CDM.UNSIGNED, "true"));
    } */

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
    setDODSName(from.getDODSName());

    dodsfile = from.dodsfile;
    CE = from.CE;
  }


  // need package access
  // protected void calcIsCoordinateVariable() { super.calcIsCoordinateVariable(); }

  protected void setCE( String CE ){ this.CE = CE; }
  protected boolean hasCE(){ return CE != null; }
  protected String nameWithCE() { return hasCE() ? getShortName() + CE : getShortName(); }

  /**
   * Instances which have same content are equal.
   */
  @Override
  public boolean equals(Object oo) {
    if (this == oo) return true;
    if (!(oo instanceof DODSVariable)) return false;
    DODSVariable o = (DODSVariable) oo;
    if(this.CE == null  ^ o.CE == null)
        return false;
    return super.equals(oo);
  }

    public int hashCode()
    {
        int supercode = super.hashCode();
        if(CE != null)
            supercode += (37 * CE.hashCode());
        return supercode;
    }

  //////////////////////////////////////////////////
  // DODSNode Interface
  String dodsName = null;
  public String getDODSName() {return dodsName;}
  public void setDODSName(String name) {this.dodsName = name;}

}
