// $Id: NcDAS.java 51 2006-07-12 17:13:13Z caron $
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

package thredds.server.opendap;

import ucar.nc2.*;
import ucar.nc2.dods.*;
import ucar.unidata.util.StringUtil;

import java.util.Iterator;
import java.util.HashMap;
import java.util.List;

import thredds.server.opendap.NcDDS;

/**
 * Netcdf DAS object
 *
 * @version $Revision: 51 $
 * @author jcaron
 */

public class NcDAS extends dods.dap.DAS {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NcDAS.class);

  HashMap usedDims = new HashMap();

  /** Create a DAS for this netcdf file */
  NcDAS( NetcdfFile ncfile ) {

    // Variable attributes
    Iterator iter = ncfile.getVariables().iterator();
    while (iter.hasNext()) {
      Variable v = (Variable) iter.next();
      doVariable(v, null);
    }

    // Global attributes
    dods.dap.AttributeTable gtable = new dods.dap.AttributeTable("NC_GLOBAL");
    int count = addAttributes(gtable, null, ncfile.getGlobalAttributes().iterator());
    if (count > 0)
      addAttributeTable("NC_GLOBAL", gtable);

    // unlimited  dimension
    iter = ncfile.getDimensions().iterator();
    while (iter.hasNext()) {
      Dimension d = (Dimension) iter.next();
      if (d.isUnlimited()) {
        dods.dap.AttributeTable table = new dods.dap.AttributeTable("DODS_EXTRA");
        try {
          table.appendAttribute("Unlimited_Dimension", dods.dap.Attribute.STRING, d.getName());
          addAttributeTable("DODS_EXTRA", table);
        } catch (Exception e) {
          log.error("Error adding Unlimited_Dimension ="+e);
        }
        break;
      }
    }

    // unused dimensions
    dods.dap.AttributeTable dimTable = null;
    iter = ncfile.getDimensions().iterator();
    while (iter.hasNext()) {
      Dimension d = (Dimension) iter.next();
      if (null == usedDims.get(d.getName())) {
        if (dimTable == null) dimTable = new dods.dap.AttributeTable("EXTRA_DIMENSION");
        try {
          dimTable.appendAttribute(d.getName(), dods.dap.Attribute.INT32, Integer.toString(d.getLength()));
        } catch (Exception e) {
          log.error("Error adding Unlimited_Dimension ="+e);
        }
      }
    }
    if (dimTable != null)
      addAttributeTable("EXTRA_DIMENSION", dimTable);

  }

  private void doVariable( Variable v, dods.dap.AttributeTable parentTable) {

    List dims = v.getDimensions();
    for (int i = 0; i < dims.size(); i++) {
      Dimension dim = (Dimension) dims.get(i);
      usedDims.put( dim.getName(), dim);
    }

    //if (v.getAttributes().size() == 0) return; // LOOK DAP 2 say must have empty

    String name = NcDDS.escapeName(v.getShortName());
    dods.dap.AttributeTable table;

    if (parentTable == null) {
      table = new dods.dap.AttributeTable(name);
      addAttributeTable(name, table);
    } else {
      table =  parentTable.appendContainer(name);
    }

    addAttributes(table, v, v.getAttributes().iterator());

    if (v instanceof Structure) {
      Structure s = (Structure) v;
      List nested = s.getVariables();
      for (int i = 0; i < nested.size(); i++) {
        Variable nv = (Variable) nested.get(i);
        doVariable( nv, table);
      }
    }


  }

  private int addAttributes(dods.dap.AttributeTable table, Variable v, Iterator iter) {
    int count = 0;

    // add attribute table for this variable
    while (iter.hasNext()) {
      Attribute att = (Attribute) iter.next();

      try {
        String attName = NcDDS.escapeName(att.getName());
        if (att.isString()) {
          String value = escapeAttributeStringValues(att.getStringValue());
          table.appendAttribute(attName, DODSNetcdfFile.convertToDODSType( att.getDataType(), false),
             "\""+value+"\"");
        } else {
          for (int i=0; i< att.getLength(); i++)
            table.appendAttribute( attName, DODSNetcdfFile.convertToDODSType( att.getDataType(), false),
              att.getNumericValue(i).toString());
        }
        count++;

      } catch (Exception e) {
        log.error("Error appending attribute "+att.getName()+" = "+att.getStringValue()+"\n"+e);
      }
    } // loop over variable attributes

    // kludgy thing to map char arrays to DODS Strings
    if ((v != null) && (v.getDataType().getPrimitiveClassType() == char.class)) {
      int rank = v.getRank();
      int strlen = (rank == 0) ? 0 : v.getShape()[rank-1];
      Dimension dim = (rank == 0) ? null : v.getDimension( rank-1);
      try {
        dods.dap.AttributeTable dodsTable = table.appendContainer("DODS");
        dodsTable.appendAttribute("strlen", dods.dap.Attribute.INT32, Integer.toString(strlen));
        if (dim != null)
          dodsTable.appendAttribute("dimName", dods.dap.Attribute.STRING, dim.getName());
        count++;
      } catch (Exception e) {
        log.error("Error appending attribute strlen\n"+e);
      }
    }

    return count;
  }

  static private String[] escapeAttributeStrings = {"\\", "\"" };
  static private String[] substAttributeStrings = {"\\\\", "\\\"" };
  private String escapeAttributeStringValues( String value) {
    return StringUtil.substitute(value, escapeAttributeStrings, substAttributeStrings);
  }

  private String unescapeAttributeStringValues( String value) {
    return StringUtil.substitute(value, substAttributeStrings, escapeAttributeStrings);
  }

}

/* Change History:
   $Log: NcDAS.java,v $
   Revision 1.11  2006/04/20 22:25:21  caron
   dods server: handle name escaping consistently
   rename, reorganize servlets
   update Paths doc

   Revision 1.10  2006/01/20 20:42:02  caron
   convert logging
   use nj22 libs

   Revision 1.9  2005/11/11 02:17:27  caron
   NcML Aggregation

   Revision 1.8  2005/08/26 22:48:31  caron
   fix catalog.xsd 
   bug in NcDAS: addds extra dimensions
   add "ecutiry" debug page

   Revision 1.7  2005/07/27 23:25:37  caron
   ncdods refactor, add Structure (2)

   Revision 1.6  2005/07/24 01:28:13  caron
   clean up logging
   add variable description
   move WCSServlet to thredds.wcs.servlet

   Revision 1.5  2005/04/12 21:58:40  caron
   add EXTRA_DIMENSION attribute group

   Revision 1.4  2005/01/21 00:58:11  caron
   *** empty log message ***

   Revision 1.3  2005/01/07 02:08:44  caron
   use nj22, commons logging, clean up javadoc

   Revision 1.2  2004/09/24 03:26:25  caron
   merge nj22

   Revision 1.1.1.1  2004/03/19 19:48:31  caron
   move AS code here

   Revision 1.3  2002/12/20 20:42:03  caron
   catalog, bug fixes

   Revision 1.2  2002/09/13 21:16:44  caron
   version 0.6

   Revision 1.1.1.1  2001/09/26 15:34:30  caron
   checkin beta1


 */
