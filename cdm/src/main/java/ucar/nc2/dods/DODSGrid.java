// $Id: DODSGrid.java,v 1.17 2006/02/16 23:02:35 caron Exp $
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
 * along with this library; if not, strlenwrite to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.dods;

import ucar.nc2.*;
import ucar.nc2.Attribute;
import ucar.unidata.util.StringUtil;
import dods.dap.*;

import java.util.*;
import java.io.IOException;

/**
 * A DODS-netCDF Grid..
 * A Grid has a Variable and associated coordinate variables, called maps.
 *
 * @see ucar.nc2.Variable
 * @author caron
 * @version $Revision: 1.17 $ $Date: 2006/02/16 23:02:35 $
 */

public class DODSGrid extends DODSVariable {

  DODSGrid( DODSNetcdfFile dodsfile, Group parentGroup, Structure parentStructure, String shortName,
            DodsV dodsV) throws IOException {
    super(dodsfile, parentGroup, parentStructure, StringUtil.unescape( shortName));

    this.shortName = dodsV.getNetcdfShortName();
    this.dodsShortName = dodsV.getDodsShortName();

    DodsV array = (DodsV) dodsV.children.get(0);
    /* if (!shortName.equals(array.bt.getName())) {
      this.shortName = shortName + "-" + array.bt.getName(); // LOOK whats this ??
    }
    // so we just map the netcdf grid variable to the dods grid.array
    this.dodsShortName = shortName + "." + array.bt.getName(); */

    ArrayList maps = new ArrayList();
    StringBuffer sbuff = new  StringBuffer();
    for (int i = 1; i < dodsV.children.size(); i++) {
      DodsV map = (DodsV) dodsV.children.get(i);
      maps.add(map);
      sbuff.append( map.bt.getName()+" ");
    }

    // the common case is that the map vectors already exist as a top level variables
    setDimensions( sbuff.toString());
    setDataType( DODSNetcdfFile.convertToNCType(array.bt));
    if (DODSNetcdfFile.isUnsigned( array.bt)) {
      addAttribute(new Attribute("_unsigned", "true"));
    }

    DODSAttribute att = new DODSAttribute("_coordinateSystem", sbuff.toString());
    this.addAttribute( att);
  }

  /* private void connectMaps(Group parentGroup, List maps) {

    // first one is the array, the rest are maps
    DODSVariable array = (DODSVariable) vars.get(0);
    List maps = vars.subList(1, vars.size());

    // do the maps first - LOOK for now assume that they are 1D coord vars
    ArrayList mapDimensions = new ArrayList();
    for (int i=0; i<maps.size(); i++) {
      DODSVariable map = (DODSVariable) maps.get(i);

      // see if it already exists LOOK not general case; assume names are unique!
      DODSVariable existingMap = (DODSVariable) parentGroup.findVariable( map.getShortName());
      Dimension dim;
      if (existingMap != null) {
        dim = existingMap.getDimension( 0);
        if (DODSNetcdfFile.debugConstruct) System.out.println("  already have coordinate map = "+map.getName());
      } else {
        dim = convertToCoordinateVariable( map);
        parentGroup.addDimension( dim);
        parentGroup.addVariable( map);
        if (DODSNetcdfFile.debugConstruct) System.out.println("  added coordinate map = "+map.getName());
      }

      mapDimensions.add( dim);
      array.replaceDimension(dim);
    }

    StringBuffer sbuff = new  StringBuffer();
    for (int i=0; i<mapDimensions.size(); i++) {
      Dimension d = (Dimension) mapDimensions.get(i);
      if (i > 0) sbuff.append(" ");
      sbuff.append(d.getName());
    }
    setDimensions( array.getDimensions());
    setDataType( array.getDataType());

    DODSAttribute att = new DODSAttribute("_coordinateSystem", sbuff.toString());
    this.addAttribute( att);
  }

  private Dimension convertToCoordinateVariable ( DODSVariable v) {
    Dimension oldDimension = v.getDimension(0);
    Dimension newDimension = new Dimension( v.getShortName(), oldDimension.getLength(), true);
    newDimension.setCoordinateVariable( v);
    v.setDimension( 0, newDimension);
    v.calcIsCoordinateVariable();

    return newDimension;

  } */

}

/* Change History:
   $Log: DODSGrid.java,v $
   Revision 1.17  2006/02/16 23:02:35  caron
   *** empty log message ***

   Revision 1.16  2006/02/03 00:59:31  caron
   ArrayStructure refactor.
   DODS parsing refactor.

   Revision 1.15  2006/01/23 21:14:43  caron
   start refactor of DODS parsing
   sequence reading
   allow ".' in netcdf object name (??)

   Revision 1.14  2006/01/13 19:57:14  caron
   * Point/StationObs: check altitude units, convert values to meters.
     * DODS: unescape dimension, structure names; dodsName dont change.

   Revision 1.13  2005/06/23 19:18:43  caron
   no message

   Revision 1.12  2005/04/18 23:45:55  caron
   _unsigned
   FileCache
   minFileLength

   Revision 1.11  2005/01/12 01:20:44  caron
   use unsigned types instead of widening
   make attribute names valid

   Revision 1.10  2004/11/07 03:00:50  caron
   *** empty log message ***

   Revision 1.9  2004/11/07 02:55:11  caron
   no message

   Revision 1.8  2004/11/04 00:38:18  caron
   no message

   Revision 1.7  2004/09/30 20:48:24  caron
   GDS grids

   Revision 1.6  2004/09/28 21:24:26  caron
   Grid subclass of Variable

   Revision 1.5  2004/08/19 21:38:12  caron
   no message

   Revision 1.4  2004/08/17 19:20:05  caron
   2.2 alpha (2)

   Revision 1.3  2004/08/16 20:53:51  caron
   2.2 alpha (2)

   Revision 1.2  2004/07/12 23:40:18  caron
   2.2 alpha 1.0 checkin

   Revision 1.1  2004/07/06 19:28:12  caron
   pre-alpha checkin

   Revision 1.1.1.1  2003/12/04 21:05:27  caron
   checkin 2.2

   Revision 1.1  2003/04/08 15:06:31  caron
   nc2 version 2.1

 */
