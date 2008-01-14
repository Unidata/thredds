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
package ucar.nc2.iosp.hdf5;

import ucar.nc2.NetcdfFile;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.Dimension;
import ucar.nc2.iosp.hdf4.ODLparser2;

import java.io.IOException;
import java.util.List;

import org.jdom.Element;

/**
 * Parse structural metadata from HDF-EOS.
 * This allows us to use shared dimensions.
 * @author caron
 * @since Jul 23, 2007
 * @deprecated use hdf4.HdfEos
 */
public class H5eos {

  static public void amendFromODL(NetcdfFile ncfile, String structMetadata) throws IOException {
    Group rootg = ncfile.getRootGroup();

    ODLparser2 parser = new ODLparser2();
    Element root = parser.parseFromString( structMetadata);

    // now we have the ODL in JDOM elements

    // LOOK this assumes a swath
    Element swathStructure = root.getChild("SwathStructure");
    if (swathStructure == null) return;

    Element swath1 = swathStructure.getChild("SWATH_1");
    if (swath1 == null) return;

    Element swathNameElem = swath1.getChild("SwathName");
    if (swathNameElem == null) return;
    String swathName = swathNameElem.getText();

    // global Dimensions
    Element d = swath1.getChild("Dimension");
    List<Element> dims = (List<Element>) d.getChildren();
    for (Element elem : dims) {
      String name = elem.getChild("DimensionName").getText();
      String sizeS = elem.getChild("Size").getText();
      int length = Integer.parseInt(sizeS);
      Dimension dim = new Dimension(name, length);
      rootg.addDimension(dim);
    }

    Group hdfeosG = rootg.findGroup("HDFEOS");
    if (hdfeosG == null) return;
    Group eosG = hdfeosG.findGroup("SWATHS");
    if (eosG == null) return;
    Group swathNameG = eosG.findGroup(swathName);
    if (swathNameG == null) return;

        // Geolocation Variables
    Group geoFieldsG = swathNameG.findGroup("Geolocation Fields");
    if (geoFieldsG != null) {

      Element floc = swath1.getChild("GeoField");
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
    Group dataG = swathNameG.findGroup("Data Fields");
    if (dataG != null) {

      Element f = swath1.getChild("DataField");
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

}
