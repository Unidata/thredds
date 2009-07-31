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

import ucar.nc2.*;
import ucar.nc2.Attribute;
import ucar.nc2.constants._Coordinate;

import java.util.*;
import java.io.IOException;

/**
 * A DODS Grid.
 * A Grid has a Variable and associated coordinate variables, called maps.
 *
 * @see ucar.nc2.Variable
 * @author caron
 */

public class DODSGrid extends DODSVariable {
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DODSGrid.class);

  DODSGrid( DODSNetcdfFile dodsfile, Group parentGroup, Structure parentStructure, String dodsShortName, DodsV dodsV) throws IOException {
    super(dodsfile, parentGroup, parentStructure, DODSNetcdfFile.makeNetcdfName( dodsShortName), dodsV);

    this.dodsShortName = dodsShortName;

    DodsV array = dodsV.children.get(0);
    /* if (!shortName.equals(array.bt.getName())) {
      this.shortName = shortName + "-" + array.bt.getName(); // LOOK whats this ??
    }
    // so we just map the netcdf grid variable to the dods grid.array
    this.dodsShortName = shortName + "." + array.bt.getName(); */

    // the common case is that the map vectors already exist as a top level variables
    List<Dimension> dims = new ArrayList<Dimension>();
    Formatter sbuff = new Formatter();
    for (int i = 1; i < dodsV.children.size(); i++) {
      DodsV map = dodsV.children.get(i);
      String name = DODSNetcdfFile.makeNetcdfName( map.bt.getName());
      Dimension dim = parentGroup.findDimension(name);
      if (dim == null) {
        logger.warn("DODSGrid cant find dimension = <"+name+">");
      } else  {
        dims.add( dim);
        sbuff.format("%s ", name);
      }
    }

    setDimensions( dims);
    setDataType( DODSNetcdfFile.convertToNCType(array.bt));
    if (DODSNetcdfFile.isUnsigned( array.bt)) {
      addAttribute(new Attribute("_Unsigned", "true"));
    }

    DODSAttribute att = new DODSAttribute(_Coordinate.Axes, sbuff.toString());
    this.addAttribute( att);
  }

    // for section, slice
  @Override
  protected Variable copy() {
    return new DODSGrid(this);
  }

  private DODSGrid(DODSGrid from) {
    super(from);
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
