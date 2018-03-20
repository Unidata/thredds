/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: HasNetcdfVariable.java 51 2006-07-12 17:13:13Z caron $


package thredds.server.opendap;

import ucar.nc2.Variable;
import ucar.ma2.Array;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Tag that Object has a netcdf variable, and the data can be set externally by an Array.
 *
 * @version $Revision: 51 $
 * @author jcaron
 */
public interface HasNetcdfVariable {

    /** reset the underlying proxy */
  public void setData(Array data);

  /** get the underlying proxy */
  public Variable getVariable();

  // for structure members
  public void serialize(DataOutputStream sink, StructureData sdata, StructureMembers.Member m) throws IOException;
}
