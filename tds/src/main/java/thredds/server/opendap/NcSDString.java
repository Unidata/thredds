// $Id: NcSDString.java 51 2006-07-12 17:13:13Z caron $
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

import opendap.Server.*;

import java.io.IOException;
import java.io.DataOutputStream;

import ucar.ma2.*;
import ucar.nc2.*;

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
      super((v.getShortName()));
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
        localVal = new String(b);
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
