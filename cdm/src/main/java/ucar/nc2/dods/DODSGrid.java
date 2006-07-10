// $Id: DODSGrid.java,v 1.17 2006/02/16 23:02:35 caron Exp $
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
