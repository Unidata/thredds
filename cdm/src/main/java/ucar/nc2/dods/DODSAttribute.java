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
import ucar.unidata.util.StringUtil;

import java.util.*;

import net.jcip.annotations.Immutable;

/**
 * Adapter for dods.dap.Attribute into a ucar.nc2.Attribute.
 * Byte attributes are widened to short because DODS has Bytes as unsigned,
 *  but in Java they are signed.
 *
 * @see ucar.nc2.Attribute
 * @author caron
 */

@Immutable
public class DODSAttribute extends ucar.nc2.Attribute {

  /** constructor: adapter around dods.dap.Attribute
   *
   * @param dodsName the attribute name
   * @param att the dods attribute
   */
  public DODSAttribute( String dodsName, opendap.dap.Attribute att) {
    super( DODSNetcdfFile.makeNetcdfName( dodsName));

    DataType ncType = DODSNetcdfFile.convertToNCType( att.getType());

    // count number
    int nvals = 0;
    Iterator iter = att.getValuesIterator();
    while(iter.hasNext()) {
      iter.next();
      nvals++;
    }

    // DAS parser is now assumed to handle escaping and remove  "" from strings
    /*
    String[] vals = new String[nvals];
    iter = att.getValuesIterator();
    int count = 0;
    while(iter.hasNext()) {
      String val = (String) iter.next();
      if (val.length() == 0)
        System.out.println("HEY");
      if (val.charAt(0) == '"')
        val = val.substring(1);
      int n = val.length();
      if ((n > 0) && (val.charAt(n-1) == '"'))
        val = val.substring(0, n-1);

      vals[count++] = unescapeAttributeStringValues( val);
    } */

    // need String[]
    String[] vals = new String[nvals];
    iter = att.getValuesIterator();
    int count = 0;
    while(iter.hasNext()) {
      vals[count++] = (String) iter.next();
    }

    Array data = null;
    if (ncType == DataType.STRING)
      data = Array.factory( ncType.getPrimitiveClassType(), new int[] { nvals}, vals);
    else {
      try {
        // create an Array of the correct type
        data = Array.factory(ncType.getPrimitiveClassType(), new int[] {nvals});
        Index ima = data.getIndex();
        for (int i = 0; i < nvals; i++) {
          double dval = Double.parseDouble(vals[i]);
          data.setDouble(ima.set(i), dval);
        }
      }
      catch (NumberFormatException e) {
        throw new IllegalArgumentException("Illegal Numeric Value for Attribute Value for " + dodsName);
      }
    }
    setValues( data);
  }

  protected DODSAttribute( String dodsName, String val) {
    super( DODSNetcdfFile.makeNetcdfName( dodsName), val);
  }

  static private String[] escapeAttributeStrings = {"\\", "\"" };
  static private String[] substAttributeStrings = {"\\\\", "\\\"" };
  private String unescapeAttributeStringValues( String value) {
    return StringUtil.substitute(value, substAttributeStrings, escapeAttributeStrings);
  }
}
