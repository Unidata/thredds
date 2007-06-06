// $Id: GridDatasetInfo.java 48 2006-07-12 16:15:40Z caron $
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

package ucar.nc2.dt.grid;

import ucar.ma2.DataType;
import ucar.nc2.Dimension;

import ucar.nc2.dataset.*;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.util.Parameter;

import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.Document;
import org.jdom.Element;

import java.util.*;
import java.io.OutputStream;
import java.io.IOException;
import java.io.FileOutputStream;

/**
 * A helper class to GridDataset; creates a GridDataset XML document.
 * This is a candidate for the XML representation of the Grid SDT.
 *
 * ForecastModelRunInventory.makeDocument is currently being used in NetcdfServer.
 *
 * @author caron
 * @version $Revision: 48 $ $Date: 2006-07-12 16:15:40Z $
 */
public class GridDatasetInfo {
  private ucar.nc2.dt.GridDataset gds;
  private String path;

  public GridDatasetInfo(ucar.nc2.dt.GridDataset gds, String path) {
    this.gds = gds;
    this.path = path;
  }

  /**
   * Write the information as an XML document
   */
  public String writeXML(Document doc) {
    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    return fmt.outputString(doc);
  }

  public void writeXML(Document doc, OutputStream os) throws IOException {
    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    fmt.output(doc, os);
  }

  /**
   * Create the "Dataset Description" XML document from this GridDataset
   */
  public Document makeDatasetDescription() {
    Element rootElem = new Element("gridDataset");
    Document doc = new Document(rootElem);
    rootElem.setAttribute("location", gds.getLocationURI());
    if (null != path)
      rootElem.setAttribute("path", path);

    /* dimensions
    List dims = getDimensions(gds);
    for (int j = 0; j < dims.size(); j++) {
      Dimension dim = (Dimension) dims.get(j);
      rootElem.addContent(ucar.nc2.ncml.NcMLWriter.writeDimension(dim, null));
    } */

    // coordinate axes
    List coordAxes = getCoordAxes(gds);
    for (int i = 0; i < coordAxes.size(); i++) {
      CoordinateAxis axis = (CoordinateAxis) coordAxes.get(i);
      rootElem.addContent(writeAxis(axis));
    }

    /* grids
    List grids = gds.getGrids();
    Collections.sort(grids, new GridComparator());
    for (int i = 0; i < grids.size(); i++) {
      GeoGrid grid = (GeoGrid) grids.get(i);
      rootElem.addContent(writeGrid(grid));
    } */

    /* coordinate systems
    List gridSets = gds.getGridsets();
    for (int i = 0; i < gridSets.size(); i++) {
      GridDataset.Gridset gridset = (GridDataset.Gridset) gridSets.get(i);
      rootElem.addContent(writeCoordSys(gridset.getGeoCoordSystem()));
    } */

    // gridSets
    List gridSets = gds.getGridsets();
    Collections.sort( gridSets, new GridSetComparator());
    for (int i = 0; i < gridSets.size(); i++) {
      GridDataset.Gridset gridset = (GridDataset.Gridset) gridSets.get(i);
      rootElem.addContent(writeGridSet(gridset));
    }

    // coordinate transforms
    List coordTransforms = getCoordTransforms(gds);
    for (int i = 0; i < coordTransforms.size(); i++) {
      CoordinateTransform ct = (CoordinateTransform) coordTransforms.get(i);
      rootElem.addContent(writeCoordTransform(ct));
    }

    /* global attributes
    Iterator atts = gds.getGlobalAttributes().iterator();
    while (atts.hasNext()) {
      ucar.nc2.Attribute att = (ucar.nc2.Attribute) atts.next();
      rootElem.addContent(ucar.nc2.ncml.NcMLWriter.writeAttribute(att, "attribute", null));
    } */

        // add lat/lon bounding box
    LatLonRect bb = gds.getBoundingBox();
    if (bb != null)
      rootElem.addContent( writeBoundingBox( bb));

    // add date range
    Date start  = gds.getStartDate();
    Date end  = gds.getEndDate();
    if ((start != null) && (end != null)) {
      DateFormatter format = new DateFormatter();
      Element dateRange = new Element("TimeSpan");
      dateRange.addContent(new Element("begin").addContent(format.toDateTimeStringISO(start)));
      dateRange.addContent(new Element("end").addContent(format.toDateTimeStringISO(end)));
      rootElem.addContent( dateRange);
    }

    // add accept list
    Element elem = new Element("AcceptList");
    elem.addContent(new Element("accept").addContent("xml"));
    elem.addContent(new Element("accept").addContent("csv"));
    elem.addContent(new Element("accept").addContent("netcdf"));
    rootElem.addContent(elem);
    return doc;
  }

  /**
   * Create the "Grid Form" XML document from this GridDataset.
   * Used to create the Grid HTML form, cause I dont know XSLT
   */
  public Document makeGridForm() {
    Element rootElem = new Element("gridForm");
    Document doc = new Document(rootElem);
    rootElem.setAttribute("location", gds.getLocationURI());
    if (null != path)
      rootElem.setAttribute("path", path);

    // its all about grids
    List grids = gds.getGrids();
    Collections.sort(grids, new GridComparator()); // sort by time axis, vert axis, grid name


    CoordinateAxis currentTime = null;
    CoordinateAxis currentVert = null;
    Element timeElem = null;
    Element vertElem = null;
    boolean newTime = true;

    for (int i = 0; i < grids.size(); i++) {
      GeoGrid grid = (GeoGrid) grids.get(i);
      GridCoordSystem gcs = grid.getCoordinateSystem();
      CoordinateAxis time = gcs.getTimeAxis();
      CoordinateAxis vert = gcs.getVerticalAxis();

      /* System.out.println(" grid "+grid.getName()
              +" time="+(time == null ? " null" : time.hashCode())
              +" vert="+(vert == null ? " null" : vert.hashCode())); */

      if ((i == 0) || !compareAxis(time, currentTime)) {
        timeElem = new Element("timeSet");
        rootElem.addContent(timeElem);
        Element timeAxisElement = writeAxis2(time, "time");
        if (timeAxisElement != null)
          timeElem.addContent(timeAxisElement);
        currentTime = time;
        newTime = true;
      } else {
        newTime = false;
      }

      if (newTime || !compareAxis(vert, currentVert)) {
        vertElem = new Element("vertSet");
        timeElem.addContent(vertElem);
        Element vertAxisElement = writeAxis2(vert, "vert");
        if (vertAxisElement != null)
          vertElem.addContent(vertAxisElement);
        currentVert = vert;
      }

      vertElem.addContent(writeGrid(grid));
    }

        // add lat/lon bounding box
    LatLonRect bb = gds.getBoundingBox();
    if (bb != null)
      rootElem.addContent( writeBoundingBox( bb));

    // add date range
    Date start  = gds.getStartDate();
    Date end  = gds.getEndDate();
    if ((start != null) && (end != null)) {
      DateFormatter format = new DateFormatter();
      Element dateRange = new Element("TimeSpan");
      dateRange.addContent(new Element("begin").addContent(format.toDateTimeStringISO(start)));
      dateRange.addContent(new Element("end").addContent(format.toDateTimeStringISO(end)));
      rootElem.addContent( dateRange);
    }

    // add accept list
    Element elem = new Element("AcceptList");
    elem.addContent(new Element("accept").addContent("xml"));
    elem.addContent(new Element("accept").addContent("csv"));
    elem.addContent(new Element("accept").addContent("netcdf"));
    rootElem.addContent(elem);
    return doc;
  }

  private Element writeAxis2(CoordinateAxis axis, String name) {
    if (axis == null) return null;

    Element varElem = new Element(name);
    varElem.setAttribute("name", axis.getName());
    varElem.setAttribute("shape", getShapeString(axis.getShape())); // axis.getDimensionsString());

    DataType dt = axis.getDataType();
    varElem.setAttribute("type", dt.toString());

    AxisType axisType = axis.getAxisType();
    if (null != axisType)
      varElem.setAttribute("axisType", axisType.toString());

    // attributes
    Iterator atts = axis.getAttributes().iterator();
    while (atts.hasNext()) {
      ucar.nc2.Attribute att = (ucar.nc2.Attribute) atts.next();
      varElem.addContent(ucar.nc2.ncml.NcMLWriter.writeAttribute(att, "attribute", null));
    }

    varElem.addContent(ucar.nc2.ncml.NcMLWriter.writeValues(axis, null, false));

    return varElem;
  }

    private boolean compareAxis(CoordinateAxis axis1, CoordinateAxis axis2) {
      if (axis1 == axis2)
        return true;

      if (axis1 == null) return false;
      if (axis2 == null) return false;

      return axis1.equals( axis2);
    }

  // sort by time, then vert, then name
  private class GridComparator implements Comparator {

    // Returns a -1, 0, 1 if the first argument is less than, equal to, or greater than the second.
    public int compare(Object o1, Object o2) {
      GeoGrid grid1 = (GeoGrid) o1;
      GeoGrid grid2 = (GeoGrid) o2;
      GridCoordSystem gcs1 = grid1.getCoordinateSystem();
      GridCoordSystem gcs2 = grid2.getCoordinateSystem();

      CoordinateAxis time1 = gcs1.getTimeAxis();
      CoordinateAxis time2 = gcs2.getTimeAxis();
      int ret = compareAxis( time1, time2);
      if (ret != 0) return ret;

      CoordinateAxis vert1 = gcs1.getVerticalAxis();
      CoordinateAxis vert2 = gcs2.getVerticalAxis();
      ret = compareAxis( vert1, vert2);
      if (ret != 0) return ret;

      return grid1.getName().compareTo( grid2.getName());
    }

    private int compareAxis(CoordinateAxis axis1, CoordinateAxis axis2) {
      if (axis1 == axis2)
        return 0;

      if (axis1 == null) return -1;
      if (axis2 == null) return 1;

      return axis1.getName().compareTo( axis2.getName());
    }

  }

  private List getCoordAxes(ucar.nc2.dt.GridDataset gds) {
    HashSet axesHash = new HashSet();
    List gridSets = gds.getGridsets();
    for (int i = 0; i < gridSets.size(); i++) {
      GridDataset.Gridset gridset = (GridDataset.Gridset) gridSets.get(i);
      GridCoordSystem gcs = gridset.getGeoCoordSystem();
      List axes = gcs.getCoordinateAxes();
      for (int j = 0; j < axes.size(); j++)
        axesHash.add(axes.get(j));
    }

    List list = Arrays.asList(axesHash.toArray());
    Collections.sort(list);
    return list;
  }

  private List getCoordTransforms(ucar.nc2.dt.GridDataset gds) {
    HashSet ctHash = new HashSet();
    List gridSets = gds.getGridsets();
    for (int i = 0; i < gridSets.size(); i++) {
      GridDataset.Gridset gridset = (GridDataset.Gridset) gridSets.get(i);
      GridCoordSystem gcs = gridset.getGeoCoordSystem();
      List ct = gcs.getCoordinateTransforms();
      for (int j = 0; j < ct.size(); j++)
        ctHash.add(ct.get(j));
    }

    List list = Arrays.asList(ctHash.toArray());
    Collections.sort(list);
    return list;
  }

  private List getDimensions(ucar.nc2.dt.GridDataset gds) {
    HashSet dimHash = new HashSet();
    List grids = gds.getGrids();
    for (int i = 0; i < grids.size(); i++) {
      GeoGrid grid = (GeoGrid) grids.get(i);
      List dims = grid.getDimensions();
      for (int j = 0; j < dims.size(); j++) {
        Dimension dim = (Dimension) dims.get(j);
        dimHash.add(dim);
      }
    }
    List list = Arrays.asList(dimHash.toArray());
    Collections.sort(list);
    return list;
  }

  private Element writeAxis(CoordinateAxis axis) {

    Element varElem = new Element("axis");
    varElem.setAttribute("name", axis.getName());
    varElem.setAttribute("shape", getShapeString(axis.getShape())); // axis.getDimensionsString());

    DataType dt = axis.getDataType();
    varElem.setAttribute("type", dt.toString());

    AxisType axisType = axis.getAxisType();
    if (null != axisType)
      varElem.setAttribute("axisType", axisType.toString());

    // attributes
    Iterator atts = axis.getAttributes().iterator();
    while (atts.hasNext()) {
      ucar.nc2.Attribute att = (ucar.nc2.Attribute) atts.next();
      varElem.addContent(ucar.nc2.ncml.NcMLWriter.writeAttribute(att, "attribute", null));
    }

    if (axis.getRank() < 2)
      varElem.addContent(ucar.nc2.ncml.NcMLWriter.writeValues(axis, null, true));

    return varElem;
  }

  /** display name plus the dimensions */
   public String getShapeString(int[] shape) {
    StringBuffer buf = new StringBuffer();
     for (int i=0; i<shape.length; i++) {
       if (i!=0) buf.append(" ");
       buf.append(shape[i]);

     }
     return buf.toString();
   }


  private Element writeBoundingBox(LatLonRect bb) {
    Element bbElem = new Element("LatLonBox");
    LatLonPoint llpt = bb.getLowerLeftPoint();
    LatLonPoint urpt = bb.getUpperRightPoint();
    bbElem.addContent(new Element("west").addContent(ucar.unidata.util.Format.dfrac(llpt.getLongitude(), 4)));
    bbElem.addContent(new Element("east").addContent(ucar.unidata.util.Format.dfrac(urpt.getLongitude(), 4)));
    bbElem.addContent(new Element("south").addContent(ucar.unidata.util.Format.dfrac(llpt.getLatitude(), 4)));
    bbElem.addContent(new Element("north").addContent(ucar.unidata.util.Format.dfrac(urpt.getLatitude(), 4)));
    return bbElem;
  }

  private Element writeGridSet(GridDataset.Gridset gridset) {
    Element csElem = new Element("gridSet");
    GridCoordSystem cs =  gridset.getGeoCoordSystem();

    csElem.setAttribute("name", cs.getName());
    List axes = cs.getCoordinateAxes();
    for (int i = 0; i < axes.size(); i++) {
      CoordinateAxis axis = (CoordinateAxis) axes.get(i);
      Element axisElem = new Element("axisRef");
      axisElem.setAttribute("name", axis.getName());
      csElem.addContent(axisElem);
    }

    List cts = cs.getCoordinateTransforms();
    for (int j = 0; j < cts.size(); j++) {
      CoordinateTransform ct = (CoordinateTransform) cts.get(j);
      Element elem = new Element("coordTransRef");
      elem.setAttribute("name", ct.getName());
      csElem.addContent(elem);
    }

    List grids = gridset.getGrids();
    Collections.sort(grids);
    for (int i = 0; i < grids.size(); i++) {
      GeoGrid grid = (GeoGrid) grids.get(i);
      csElem.addContent(writeGrid(grid));
    }

    return csElem;
  }

  private Element writeCoordSys(GridCoordSystem cs) {
    Element csElem = new Element("coordSys");
    csElem.setAttribute("name", cs.getName());
    List axes = cs.getCoordinateAxes();
    for (int i = 0; i < axes.size(); i++) {
      CoordinateAxis axis = (CoordinateAxis) axes.get(i);
      Element axisElem = new Element("axisRef");
      axisElem.setAttribute("name", axis.getName());
      csElem.addContent(axisElem);
    }
    List cts = cs.getCoordinateTransforms();
    for (int j = 0; j < cts.size(); j++) {
      CoordinateTransform ct = (CoordinateTransform) cts.get(j);
      Element elem = new Element("coordTransRef");
      elem.setAttribute("name", ct.getName());
      csElem.addContent(elem);
    }
    return csElem;
  }

  private Element writeCoordTransform(CoordinateTransform ct) {
    Element ctElem = new Element("coordTransform");
    ctElem.setAttribute("name", ct.getName());
    ctElem.setAttribute("transformType", ct.getTransformType().toString());
    List params = ct.getParameters();
    for (int i = 0; i < params.size(); i++) {
      Parameter param = (Parameter) params.get(i);
      Element pElem = new Element("parameter");
      pElem.setAttribute("name", param.getName());
      pElem.setAttribute("value", param.getStringValue());
      ctElem.addContent(pElem);
    }
    return ctElem;
  }

  private Element writeGrid(GridDatatype grid) {

    Element varElem = new Element("grid");
    varElem.setAttribute("name", grid.getName());

    StringBuffer buff = new StringBuffer();
    List dims = grid.getDimensions();
    for (int i = 0; i < dims.size(); i++) {
      Dimension dim = (Dimension) dims.get(i);
      if (i > 0) buff.append(" ");
      if (dim.isShared())
        buff.append(dim.getName());
      else
        buff.append(dim.getLength());
    }
    if (buff.length() > 0)
      varElem.setAttribute("shape", buff.toString());

    DataType dt = grid.getDataType();
    if (dt != null)
      varElem.setAttribute("type", dt.toString());

    //GridCoordSystem cs = grid.getCoordinateSystem();
    //varElem.setAttribute("coordSys", cs.getName());

    // attributes
    Iterator atts = grid.getAttributes().iterator();
    while (atts.hasNext()) {
      ucar.nc2.Attribute att = (ucar.nc2.Attribute) atts.next();
      varElem.addContent(ucar.nc2.ncml.NcMLWriter.writeAttribute(att, "attribute", null));
    }

    return varElem;
  }

  // sort by domain size, then name
  private class GridSetComparator implements Comparator {

    public int compare(Object o1, Object o2) {
      GridDataset.Gridset gridset1 = (GridDataset.Gridset) o1;
      GridDataset.Gridset gridset2 = (GridDataset.Gridset) o2;
      GridCoordSystem cs1 =  gridset1.getGeoCoordSystem();
      GridCoordSystem cs2 =  gridset2.getGeoCoordSystem();
      if (cs1.getDomain().size() != cs2.getDomain().size())
        return cs1.getDomain().size() - cs2.getDomain().size();
      return cs1.getName().compareTo(cs2.getName());
    }
  }


  /**
   * debug
   */
  public static void main(String args[]) throws IOException {
    // String url = "C:/data/test2.nc";

    String url = "C:/data/NAM_CONUS_12km_20060227_1200.grib2";

    // String url = "http://motherlode.ucar.edu:8080/thredds/dodsC/fmrc/NCEP/NDFD/CONUS_5km/NDFD-CONUS_5km_best.ncd";

    GridDataset ncd = GridDataset.open(url);
    GridDatasetInfo info = new GridDatasetInfo(ncd, null);
    FileOutputStream fos2 = new FileOutputStream("C:/TEMP/gridInfo.xml");
    info.writeXML( info.makeGridForm(), fos2);
    fos2.close();

    String infoString = info.writeXML(info.makeGridForm());
    System.out.println(infoString);
  }

}

