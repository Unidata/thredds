/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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
package ucar.nc2.iosp.hdf4;

import ucar.nc2.*;
import ucar.nc2.iosp.hdf5.ODLparser2;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.DataType;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.jdom.Document;
import org.jdom.Element;

/**
 * Parse structural metadata from HDF4-EOS.
 * This allows us to use shared dimensions.
 * @author caron
 * @since Jul 23, 2007
 */
public class H4eos {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(H4eos.class);

  private NetcdfFile ncfile;
  private Group rootg;
  public void amendFromODL(NetcdfFile ncfile, String structMetadata) throws IOException {
    this.ncfile = ncfile;
    this.rootg = ncfile.getRootGroup();

    ODLparser2 parser = new ODLparser2();
    Element root = parser.parseFromString(structMetadata); // now we have the ODL in JDOM elements

    // SWATH
    Element swathStructure = root.getChild("SwathStructure");
    if (swathStructure != null){
      List<Element> swaths = (List<Element>) swathStructure.getChildren();
      for (Element elemSwath : swaths) {
        Element swathNameElem = elemSwath.getChild("SwathName");
        if (swathNameElem == null) {
          log.warn("No SwathName element in "+elemSwath.getName());
          continue;
        }
        String swathName = swathNameElem.getText();
        Group swathGroup = findGroupNested(rootg, swathName);
        if (swathGroup != null) {
          amendSwath(elemSwath, swathGroup);
        } else {
          log.warn("Cant find swath group "+swathName);
        }
      }
    }

    // GRID
    Element gridStructure = root.getChild("GridStructure");
    if (gridStructure != null){
      List<Element> grids = (List<Element>) gridStructure.getChildren();
      for (Element elemGrid : grids) {
        Element gridNameElem = elemGrid.getChild("GridName");
        if (gridNameElem == null) {
          log.warn("Ne GridName element in "+elemGrid.getName());
          continue;
        }
        String gridName = gridNameElem.getText();
        Group gridGroup = findGroupNested(rootg, gridName);
        if (gridGroup != null) {
          amendGrid(elemGrid, gridGroup);
        } else {
          log.warn("Cant find Grid group "+gridName);
        }
      }
    }

  }

  private void amendSwath(Element swathElem, Group parent) {

    // Dimensions
    Element d = swathElem.getChild("Dimension");
    List<Element> dims = (List<Element>) d.getChildren();
    for (Element elem : dims) {
      String name = elem.getChild("DimensionName").getText();
      String sizeS = elem.getChild("Size").getText();
      int length = Integer.parseInt(sizeS);
      Dimension dim = new Dimension(name, length);
      parent.addDimension(dim);
    }

    // Dimensions
    Element dmap = swathElem.getChild("DimensionMap");
    List<Element> dimMaps = (List<Element>) dmap.getChildren();
    for (Element elem : dimMaps) {
      String geoDimName = elem.getChild("GeoDimension").getText();
      String dataDimName = elem.getChild("DataDimension").getText();
      String offsetS = elem.getChild("Offset").getText();
      String incrS = elem.getChild("Increment").getText();
      int offset = Integer.parseInt(offsetS);
      int incr = Integer.parseInt(incrS);

      // make new variable for this dimension map
      Variable v = new Variable(ncfile, parent, null, dataDimName);
      v.setDimensions(geoDimName);
      v.setDataType( DataType.INT);
      int npts = (int) v.getSize();
      Array data = Array.makeArray(v.getDataType(), npts, offset, incr);
      v.setCachedData(data, true);
      v.addAttribute( new Attribute("_DimensionMap",""));
      parent.addVariable(v);
    }

        // Geolocation Variables
    Group geoFieldsG = parent.findGroup("Geolocation Fields");
    if (geoFieldsG != null) {

      Element floc = swathElem.getChild("GeoField");
      List<Element> varsLoc = (List<Element>) floc.getChildren();
      for (Element elem : varsLoc) {
        String varname = elem.getChild("GeoFieldName").getText();
        Variable v = geoFieldsG.findVariable( varname);
        assert v != null : varname;

        StringBuffer sbuff = new StringBuffer();
        Element dimList = elem.getChild("DimList");
        List<Element> values = (List<Element>) dimList.getChildren("value");
        for (Element value : values) {
          sbuff.append( value.getText());
          sbuff.append( " ");
        }
        v.setDimensions( sbuff.toString()); // livin dangerously
      }
    }

    // Data Variables
    Group dataG = parent.findGroup("Data Fields");
    if (dataG != null) {

      Element f = swathElem.getChild("DataField");
      List<Element> vars = (List<Element>) f.getChildren();
      for (Element elem : vars) {
        String varname = elem.getChild("DataFieldName").getText();
        Variable v = dataG.findVariable( varname);
        assert v != null : varname;

        StringBuffer sbuff = new StringBuffer();
        Element dimList = elem.getChild("DimList");
        List<Element> values = (List<Element>) dimList.getChildren("value");
        for (Element value : values) {
          sbuff.append( value.getText());
          sbuff.append( " ");
        }
        v.setDimensions( sbuff.toString()); // livin dangerously
      }
    }

  }


  private void amendGrid(Element gridElem, Group parent) {
    // always has x and y dimension
    String xdimSizeS = gridElem.getChild("XDim").getText();
    String ydimSizeS = gridElem.getChild("YDim").getText();
    int xdimSize = Integer.parseInt(xdimSizeS);
    int ydimSize = Integer.parseInt(ydimSizeS);
    parent.addDimension( new Dimension("XDim", xdimSize));
    parent.addDimension( new Dimension("YDim", ydimSize));

    // global Dimensions
    Element d = gridElem.getChild("Dimension");
    List<Element> dims = (List<Element>) d.getChildren();
    for (Element elem : dims) {
      String name = elem.getChild("DimensionName").getText();
      String sizeS = elem.getChild("Size").getText();
      int length = Integer.parseInt(sizeS);
      Dimension dim = new Dimension(name, length);
      parent.addDimension(dim);
    }

        // Geolocation Variables
    Group geoFieldsG = parent.findGroup("Geolocation Fields");
    if (geoFieldsG != null) {

      Element floc = gridElem.getChild("GeoField");
      List<Element> varsLoc = (List<Element>) floc.getChildren();
      for (Element elem : varsLoc) {
        String varname = elem.getChild("GeoFieldName").getText();
        Variable v = geoFieldsG.findVariable( varname);
        assert v != null : varname;

        StringBuffer sbuff = new StringBuffer();
        Element dimList = elem.getChild("DimList");
        List<Element> values = (List<Element>) dimList.getChildren("value");
        for (Element value : values) {
          sbuff.append( value.getText());
          sbuff.append( " ");
        }
        v.setDimensions( sbuff.toString()); // livin dangerously
      }
    }

    // Data Variables
    Group dataG = parent.findGroup("Data Fields");
    if (dataG != null) {

      Element f = gridElem.getChild("DataField");
      List<Element> vars = (List<Element>) f.getChildren();
      for (Element elem : vars) {
        String varname = elem.getChild("DataFieldName").getText();
        Variable v = dataG.findVariable( varname);
        assert v != null : varname;

        StringBuffer sbuff = new StringBuffer();
        Element dimList = elem.getChild("DimList");
        List<Element> values = (List<Element>) dimList.getChildren("value");
        for (Element value : values) {
          sbuff.append( value.getText());
          sbuff.append( " ");
        }
        v.setDimensions( sbuff.toString()); // livin dangerously
      }
    }

  }

  // look for a group with the given name. recurse into subgroups if needed. breadth first
  private Group findGroupNested(Group parent, String name) {
    for (Group g : parent.getGroups()) {
      if (g.getShortName().equals(name))
        return g;
    }
    for (Group g : parent.getGroups()) {
      Group result = findGroupNested(g, name);
      if (result != null)
        return result;
    }
    return null;
  }

}
