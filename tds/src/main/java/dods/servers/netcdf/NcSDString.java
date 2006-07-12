// $Id$
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

package dods.servers.netcdf;

import dods.dap.Server.*;

import java.io.IOException;
import java.io.DataOutputStream;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.unidata.util.StringUtil;

/**
 * Wraps a netcdf scalar or 1D char variable.
 *
 * @author jcaron
 * @version $Revision$
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
    super(NcDDS.escapeName(v.getShortName()));
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
        ArrayChar.D0 a = (ArrayChar.D0) data;
        byte[] b = new byte[1];
        b[0] = (byte) a.get();
        localVal = new String(b);
      } else {
        // 1D
        ArrayChar.D1 a = (ArrayChar.D1) data;
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
