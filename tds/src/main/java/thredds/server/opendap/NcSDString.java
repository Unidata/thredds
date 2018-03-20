/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: NcSDString.java 51 2006-07-12 17:13:13Z caron $


package thredds.server.opendap;

import opendap.servers.*;

import java.io.IOException;
import java.io.DataOutputStream;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;

/**
 * Wraps a netcdf scalar or 1D char variable.
 *
 * @author jcaron
 */
public class NcSDString extends SDString implements HasNetcdfVariable {
  private Variable ncVar;
  private String localVal = null;

  /**
   * Constructor
   *
   * @param v : the netcdf Variable
   */
  NcSDString(Variable v) {
      super(Variable.getDAPName(v));
    this.ncVar = v;
  }


  /**
   * Constructor
   *
   * @param name: name of variable
   * @param val:  the value.
   */
  NcSDString(String name, String val) {
    super(name);
    this.localVal = val;
    if (val != null)
      setValue(val);
  }

  /**
   * Read the value (parameters are ignored).
   */
  public boolean read(String datasetName, Object specialO) throws IOException {
    if (localVal == null) // read first time
      setData(ncVar.read());

    setValue(localVal);
    setRead(true);
    return (false);
  }


  public void setData(Array data) {

    if (ncVar.getDataType() == DataType.STRING) {
      localVal = (String) data.getObject(data.getIndex());

    } else { // gotta be a CHAR

      if (ncVar.getRank() == 0) {
        // scalar char - convert to a String
        ArrayChar a = (ArrayChar) data;
        byte[] b = new byte[1];
        b[0] = (byte) a.getChar(0);
        localVal = new String(b, CDM.utf8Charset);
      } else {
        // 1D
        ArrayChar a = (ArrayChar) data;
        localVal = a.getString(a.getIndex()); // fetches the entire String
      }
    }

    setValue(localVal);
    setRead(true);
  }

  public Variable getVariable() { return ncVar; }
  public void serialize(DataOutputStream sink, StructureData sdata, StructureMembers.Member m) throws IOException {
    localVal = sdata.getScalarString(m);
    setValue(localVal);
    externalize( sink);
  }
}
