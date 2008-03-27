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
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.ArrayChar;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.jdom.Element;

/**
 * Parse structural metadata from HDF4-EOS.
 * This allows us to use shared dimensions.
 *
 * @author caron
 * @since Jul 23, 2007
 */
public class HdfEos {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HdfEos.class);
  static private boolean showTypes = false, debug = true;

  static public void amendFromODL(NetcdfFile ncfile, Group eosGroup) throws IOException {
    StringBuffer sbuff = null;
    String structMetadata = null;

    int n = 0;
    while (true) {
      Variable structMetadataVar = eosGroup.findVariable("StructMetadata." + n);
      if (structMetadataVar == null) break;
      if ((structMetadata != null) && (sbuff == null)) { // more than 1 StructMetadata
        sbuff = new StringBuffer(64000);
        sbuff.append(structMetadata);
      }

      // read and parse the ODL
      Array A = structMetadataVar.read();
      ArrayChar ca = (ArrayChar) A;
      structMetadata = ca.getString(); // common case only StructMetadata.0, avoid extra copy

      if (sbuff != null)
        sbuff.append(structMetadata);
      n++;
    }
    if (sbuff != null)
      structMetadata = sbuff.toString();
    if (structMetadata != null)
      new HdfEos().amendFromODL(ncfile, structMetadata);
  }

  private NetcdfFile ncfile;

  public void amendFromODL(NetcdfFile ncfile, String structMetadata) throws IOException {
    this.ncfile = ncfile;
    Group rootg = ncfile.getRootGroup();

    ODLparser parser = new ODLparser();
    Element root = parser.parseFromString(structMetadata); // now we have the ODL in JDOM elements

    // SWATH
    boolean isSwath = false;
    Element swathStructure = root.getChild("SwathStructure");
    if (swathStructure != null) {
      List<Element> swaths = (List<Element>) swathStructure.getChildren();
      for (Element elemSwath : swaths) {
        Element swathNameElem = elemSwath.getChild("SwathName");
        if (swathNameElem == null) {
          log.warn("No SwathName element in " + elemSwath.getName());
          continue;
        }
        String swathName = swathNameElem.getText();
        Group swathGroup = findGroupNested(rootg, swathName);
        if (swathGroup != null) {
          amendSwath(elemSwath, swathGroup);
          isSwath = true;
        } else {
          log.warn("Cant find swath group " + swathName);
        }
      }
      if (isSwath) {
        if (showTypes) System.out.println("***EOS SWATH");
        rootg.addAttribute(new Attribute("cdm_data_type", FeatureType.SWATH.toString()));
      }
    }

    // GRID
    boolean isGrid = false;
    Element gridStructure = root.getChild("GridStructure");
    if (gridStructure != null) {
      List<Element> grids = (List<Element>) gridStructure.getChildren();
      for (Element elemGrid : grids) {
        Element gridNameElem = elemGrid.getChild("GridName");
        if (gridNameElem == null) {
          log.warn("Ne GridName element in " + elemGrid.getName());
          continue;
        }
        String gridName = gridNameElem.getText();
        Group gridGroup = findGroupNested(rootg, gridName);
        if (gridGroup != null) {
          amendGrid(elemGrid, gridGroup);
          isGrid = true;
        } else {
          log.warn("Cant find Grid group " + gridName);
        }
      }
      if (isGrid) {
        if (showTypes) System.out.println("***EOS GRID");
        rootg.addAttribute(new Attribute("cdm_data_type", FeatureType.GRID.toString()));
      }
    }

    // POINT
    boolean isPoint = false;
    Element pointStructure = root.getChild("PointStructure");
    if (pointStructure != null) {
      List<Element> pts = (List<Element>) pointStructure.getChildren();
      for (Element elem : pts) {
        Element nameElem = elem.getChild("PointName");
        if (nameElem == null) {
          log.warn("No PointName element in " + elem.getName());
          continue;
        }
        String name = nameElem.getText();
        Group ptGroup = findGroupNested(rootg, name);
        if (ptGroup != null) {
          isPoint = true;
        } else {
          log.warn("Cant find Point group " + name);
        }
      }
      if (isPoint) {
        if (showTypes) System.out.println("***EOS POINT");
        rootg.addAttribute(new Attribute("cdm_data_type", FeatureType.POINT.toString()));
      }
    }

  }

  private void amendSwath(Element swathElem, Group parent) {
    List<Dimension> unknownDims = new ArrayList<Dimension>();

    // Dimensions
    Element d = swathElem.getChild("Dimension");
    List<Element> dims = (List<Element>) d.getChildren();
    for (Element elem : dims) {
      String name = elem.getChild("DimensionName").getText();
      if (name.equalsIgnoreCase("scalar"))
        continue;
      String sizeS = elem.getChild("Size").getText();
      int length = Integer.parseInt(sizeS);
      if (length > 0) {
        Dimension dim = new Dimension(name, length);
        parent.addDimension(dim);
      } else {
        log.warn("Dimension "+name+" has size "+sizeS);
        Dimension udim = new Dimension(name, 1);
        udim.setGroup(parent);
        unknownDims.add( udim);
      }
    }

    // Dimension Maps
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
      v.setDataType(DataType.INT);
      int npts = (int) v.getSize();
      Array data = Array.makeArray(v.getDataType(), npts, offset, incr);
      v.setCachedData(data, true);
      v.addAttribute(new Attribute("_DimensionMap", ""));
      parent.addVariable(v);
    }

    // Geolocation Variables
    Group geoFieldsG = parent.findGroup("Geolocation Fields");
    if (geoFieldsG != null) {

      Element floc = swathElem.getChild("GeoField");
      List<Element> varsLoc = (List<Element>) floc.getChildren();
      for (Element elem : varsLoc) {
        String varname = elem.getChild("GeoFieldName").getText();
        Variable v = geoFieldsG.findVariable(varname);
        assert v != null : varname;
        addAxisType(v);

        StringBuffer sbuff = new StringBuffer();
        Element dimList = elem.getChild("DimList");
        List<Element> values = (List<Element>) dimList.getChildren("value");
        setSharedDimensions( v, values, unknownDims);
      }
    }

    // Data Variables
    Group dataG = parent.findGroup("Data Fields");
    if (dataG != null) {

      Element f = swathElem.getChild("DataField");
      List<Element> vars = (List<Element>) f.getChildren();
      for (Element elem : vars) {
        Element dataFieldNameElem = elem.getChild("DataFieldName");
        if (dataFieldNameElem == null) continue;
        String varname = dataFieldNameElem.getText();
        Variable v = dataG.findVariable(varname);
        if (v == null) {
          log.error("Cant find "+varname);
          continue;
        }

        StringBuffer sbuff = new StringBuffer();
        Element dimList = elem.getChild("DimList");
        List<Element> values = (List<Element>) dimList.getChildren("value");
        setSharedDimensions( v, values, unknownDims);
      }
    }

  }

  private void addAxisType(Variable v) {
    String name = v.getShortName();
    if (name.equalsIgnoreCase("Latitude") || name.equalsIgnoreCase("GeodeticLatitude")) {
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
      v.addAttribute(new Attribute("units", "degrees_north"));
    } else if (name.equalsIgnoreCase("Longitude")) {
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));
      v.addAttribute(new Attribute("units", "degrees_east"));
    } else if (name.equalsIgnoreCase("Time")) {
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));
      //v.addAttribute(new Attribute("units", "unknown"));
    }
  }


  private void amendGrid(Element gridElem, Group parent) {
    List<Dimension> unknownDims = new ArrayList<Dimension>();

    // always has x and y dimension
    String xdimSizeS = gridElem.getChild("XDim").getText();
    String ydimSizeS = gridElem.getChild("YDim").getText();
    int xdimSize = Integer.parseInt(xdimSizeS);
    int ydimSize = Integer.parseInt(ydimSizeS);
    parent.addDimension(new Dimension("XDim", xdimSize));
    parent.addDimension(new Dimension("YDim", ydimSize));

    // global Dimensions
    Element d = gridElem.getChild("Dimension");
    List<Element> dims = (List<Element>) d.getChildren();
    for (Element elem : dims) {
      String name = elem.getChild("DimensionName").getText();
      if (name.equalsIgnoreCase("scalar"))
        continue;

      String sizeS = elem.getChild("Size").getText();
      int length = Integer.parseInt(sizeS);
      Dimension old = parent.findDimension(name);
      if ((old == null) || (old.getLength() != length)) {
        if (length > 0) {
          Dimension dim = new Dimension(name, length);
          parent.addDimension(dim);
        } else {
          log.warn("Dimension "+name+" has size "+sizeS);
          Dimension udim = new Dimension(name, 1);
          udim.setGroup(parent);
          unknownDims.add( udim);
        }
      }
    }

    // Geolocation Variables
    Group geoFieldsG = parent.findGroup("Geolocation Fields");
    if (geoFieldsG != null) {

      Element floc = gridElem.getChild("GeoField");
      List<Element> varsLoc = (List<Element>) floc.getChildren();
      for (Element elem : varsLoc) {
        String varname = elem.getChild("GeoFieldName").getText();
        Variable v = geoFieldsG.findVariable(varname);
        assert v != null : varname;

        StringBuffer sbuff = new StringBuffer();
        Element dimList = elem.getChild("DimList");
        List<Element> values = (List<Element>) dimList.getChildren("value");
        setSharedDimensions( v, values, unknownDims);
      }
    }

    // Data Variables
    Group dataG = parent.findGroup("Data Fields");
    if (dataG != null) {

      Element f = gridElem.getChild("DataField");
      List<Element> vars = (List<Element>) f.getChildren();
      for (Element elem : vars) {
        String varname = elem.getChild("DataFieldName").getText();
        Variable v = dataG.findVariable(varname);
        assert v != null : varname;

        StringBuffer sbuff = new StringBuffer();
        Element dimList = elem.getChild("DimList");
        List<Element> values = (List<Element>) dimList.getChildren("value");
        setSharedDimensions( v, values, unknownDims);
      }
    }

  }

  // convert to shared dimensions
  private void setSharedDimensions(Variable v, List<Element> values, List<Dimension> unknownDims) {
    if (values.size() == 0)
      return;

    // remove the "scalar" dumbension
    Iterator<Element> iter = values.iterator();
    while (iter.hasNext()) {
      Element value = iter.next();
      String dimName = value.getText();
      if (dimName.equalsIgnoreCase("scalar"))
        iter.remove();
    }

    // gotta have same number of dimensions
    List<Dimension> oldDims = v.getDimensions();
    if (oldDims.size() != values.size()) {
      log.error("Different number of dimensions for "+ v);
      return;
    }

    List<Dimension> newDims = new ArrayList<Dimension>();
    Group group = v.getParentGroup();

    for (int i=0; i<values.size(); i++) {
      Element value = values.get(i);
      String dimName = value.getText();
      Dimension dim = group.findDimension(dimName);
      Dimension oldDim = oldDims.get(i);
      if (dim == null)
        dim = checkUnknownDims(dimName, unknownDims, oldDim);

      if (dim == null) {
        log.error("Unknown Dimension= "+dimName+" for variable = "+v.getName());
        return;
      }
      if (dim.getLength() != oldDim.getLength()) {
        log.error("Shared dimension ("+dim.getName()+") has different length than data dimension ("+oldDim.getName()+
            ") shared="+ dim.getLength() + " org=" + oldDim.getLength() + " for "+ v);
        return;
      }
      newDims.add(dim);
    }
    v.setDimensions(newDims);
  }

  // look if the wanted dimension is in the  unknownDims list.
  private Dimension checkUnknownDims(String wantDim, List<Dimension> unknownDims, Dimension oldDim) {
    for (Dimension dim : unknownDims) {
      if (dim.getName().equals(wantDim)) {
        int len = oldDim.getLength();
        if (len == 0)
          dim.setUnlimited( true); // allow zero length dimension !!
        dim.setLength(len); // use existing (anon) dimension
        Group parent = dim.getGroup();
        parent.addDimension(dim);  // add to the parent
        unknownDims.remove(dim); // remove from list LOOK is this ok?
        log.warn("unknownDim " + wantDim+" length set to "+oldDim.getLength());
        return dim;
      }
    }
    return null;
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
