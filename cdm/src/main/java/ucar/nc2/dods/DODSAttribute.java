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
package ucar.nc2.dods;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.unidata.util.StringUtil;

import java.util.*;

/**
 * Adapter for dods.dap.Atribute.
 * Byte attributes are widened to short because DODS has Bytes as unsigned,
 *  but in Java they are signed.
 *
 * @see ucar.nc2.Attribute
 *
 * @author caron
 * @version $Revision$ $Date$
 */


public class DODSAttribute extends ucar.nc2.Attribute {
  //private dods.dap.Attribute att;

  /** constructor: adapter around dods.dap.Attribute */
  public DODSAttribute( String name, dods.dap.Attribute att) {
    super( NetcdfFile.createValidNetcdfObjectName( StringUtil.unescape( name)));

    DataType ncType = DODSNetcdfFile.convertToNCType( att.getType());

    int nvals = 0;
    String[] vals = null;

    Enumeration es = att.getValues();
    while(es.hasMoreElements()) {
      es.nextElement();
      nvals++;
    }

    vals = new String[nvals];
    es = att.getValues();
    int count = 0;
    while(es.hasMoreElements()) {
      String val = (String) es.nextElement();
      if (val.charAt(0) == '"')
        val = val.substring(1);
      int n = val.length();
      if ((n > 0) && (val.charAt(n-1) == '"'))
        val = val.substring(0, n-1);

      vals[count++] = unescapeAttributeStringValues( val);
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
        System.out.println("ILLEGAL NUMERIC VALUE");
      }
    }
    setValues( data);
  }

  protected DODSAttribute( String name, String val) {
    super( StringUtil.unescape( name), val);
  }

  static private String[] escapeAttributeStrings = {"\\", "\"" };
  static private String[] substAttributeStrings = {"\\\\", "\\\"" };
  private String unescapeAttributeStringValues( String value) {
    return StringUtil.substitute(value, substAttributeStrings, escapeAttributeStrings);
  }
}
