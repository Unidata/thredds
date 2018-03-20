/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: NcSDByte.java 51 2006-07-12 17:13:13Z caron $


package thredds.server.opendap;

import opendap.servers.*;

import java.io.IOException;
import java.io.DataOutputStream;

import ucar.ma2.*;
import ucar.nc2.*;

/**
 * Wraps a netcdf scalar byte variable.
 *
 * @author jcaron
 */
public class NcSDByte extends SDByte implements HasNetcdfVariable {
  private Variable ncVar;

  /**
   * Constructor
   *
   * @param ncVar : the netcdf Variable
   */
  NcSDByte(Variable ncVar) {
      super(Variable.getDAPName(ncVar));
      this.ncVar = ncVar;
  }

  public Variable getVariable() { return ncVar; }


  /**
   * Read the value (parameters are ignored).
   */
  public boolean read(String datasetName, Object specialO) throws IOException {
    setData(ncVar.read());
    return (false);
  }

  public void setData(Array data) {
    setValue(data.getByte(data.getIndex()));     
    setRead(true);
  }

  public void serialize(DataOutputStream sink, StructureData sdata, StructureMembers.Member m) throws IOException {
    setValue( sdata.getScalarByte(m));
    externalize(sink);
  }
}
