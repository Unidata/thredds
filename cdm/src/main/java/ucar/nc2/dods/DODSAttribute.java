// $Id: DODSAttribute.java,v 1.8 2005/01/20 00:55:32 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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
 * @version $Revision: 1.8 $ $Date: 2005/01/20 00:55:32 $
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

/* Change History:
   $Log: DODSAttribute.java,v $
   Revision 1.8  2005/01/20 00:55:32  caron
   *** empty log message ***

   Revision 1.7  2005/01/14 21:37:17  caron
   escape/unescape names

   Revision 1.6  2005/01/12 01:20:44  caron
   use unsigned types instead of widening
   make attribute names valid

   Revision 1.5  2004/09/22 18:44:33  caron
   move common to ucar.unidata

   Revision 1.4  2004/08/17 19:20:05  caron
   2.2 alpha (2)

   Revision 1.3  2004/07/12 23:40:18  caron
   2.2 alpha 1.0 checkin

   Revision 1.2  2004/07/06 19:28:11  caron
   pre-alpha checkin

   Revision 1.1.1.1  2003/12/04 21:05:27  caron
   checkin 2.2

   Revision 1.6  2003/06/03 20:06:14  caron
   fix javadocs

   Revision 1.5  2003/04/08 15:06:31  caron
   nc2 version 2.1

   Revision 1.4  2001/09/14 21:34:29  caron
   minor doc improvements, clean up debug flags

   Revision 1.3  2001/08/10 21:17:32  caron
   various changes part of Catalog/Agg Server

 */
