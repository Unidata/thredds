/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dt.point;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateTransform;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.StationObsDataset;
import ucar.nc2.dt.TypedDatasetFactory;
import ucar.nc2.ncml.NcMLWriter;
import ucar.nc2.units.DateFormatter;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.Parameter;

/**
 * A helper class to StationObsDataset; creates XML documents.
 *
 * @deprecated use ucar.nc2.ft.point
 * @author caron
 */
public class StationObsDatasetInfo {
  private StationObsDataset sobs;
  private String path;

  public StationObsDatasetInfo(StationObsDataset sobs, String path) {
    this.sobs = sobs;
    this.path = path;
  }

  /**
   * Write stationObsDataset XML document
   */
  public String writeStationObsDatasetXML() {
    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    return fmt.outputString(makeStationObsDatasetDocument());
  }

  public void writeStationObsDatasetXML(OutputStream os) throws IOException {
    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    fmt.output(makeStationObsDatasetDocument(), os);
  }

  /**
   * Write stationCollection XML document
   */
  public String writeStationCollectionXML() throws IOException {
    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    return fmt.outputString(makeStationCollectionDocument());
  }

  public void writeStationCollectionXML(OutputStream os) throws IOException {
    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    fmt.output(makeStationCollectionDocument(), os);
  }

  /**
   * Create an XML document from this info
   */
  public Document makeStationCollectionDocument() throws IOException {
    Element rootElem = new Element("stationCollection");
    Document doc = new Document(rootElem);

    List stns = sobs.getStations();
    System.out.println("nstns = "+stns.size());
    for (int i = 0; i < stns.size(); i++) {
      ucar.unidata.geoloc.Station s = (ucar.unidata.geoloc.Station) stns.get(i);
      Element sElem = new Element("station");
      sElem.setAttribute("name",s.getName());
      if (s.getWmoId() != null)
        sElem.setAttribute("wmo_id",s.getWmoId());
      if (s.getDescription() != null)
        sElem.addContent(new Element("description").addContent(s.getDescription()));

      sElem.addContent(new Element("longitude").addContent( ucar.unidata.util.Format.d(s.getLongitude(), 6)));
      sElem.addContent(new Element("latitide").addContent( ucar.unidata.util.Format.d(s.getLatitude(), 6)));
      if (!Double.isNaN(s.getAltitude()))
        sElem.addContent(new Element("altitude").addContent( ucar.unidata.util.Format.d(s.getAltitude(), 6)));
      rootElem.addContent(sElem);
    }

    return doc;
  }

  /**
   * Create an XML document from this info
   */
  public Document makeStationObsDatasetDocument() {
    Element rootElem = new Element("stationObsDataset");
    Document doc = new Document(rootElem);
    rootElem.setAttribute("location", sobs.getLocationURI());
    if (null != path)
      rootElem.setAttribute("path", path);

    /* dimensions
    List dims = getDimensions(sobs);
    for (int j = 0; j < dims.size(); j++) {
      Dimension dim = (Dimension) dims.get(j);
      rootElem.addContent(ucar.nc2.ncml.NcMLWriter.writeDimension(dim, null));
    } */

    /* coordinate axes
    List coordAxes = getCoordAxes(sobs);
    for (int i = 0; i < coordAxes.size(); i++) {
      CoordinateAxis axis = (CoordinateAxis) coordAxes.get(i);
      rootElem.addContent(writeAxis(axis));
    } */

    // grids
    List vars = sobs.getDataVariables();
    Collections.sort(vars);
    for (int i = 0; i < vars.size(); i++) {
      VariableSimpleIF v = (VariableSimpleIF) vars.get(i);
      rootElem.addContent(writeVariable(v));
    }

    /* global attributes
    Iterator atts = sobs.getGlobalAttributes().iterator();
    while (atts.hasNext()) {
      ucar.nc2.Attribute att = (ucar.nc2.Attribute) atts.next();
      rootElem.addContent(ucar.nc2.ncml.NcMLWriter.writeAttribute(att, "attribute", null));
    } */

    // add lat/lon bounding box
    LatLonRect bb = sobs.getBoundingBox();
    if (bb != null)
      rootElem.addContent( writeBoundingBox( bb));

    // add date range
    Date start  = sobs.getStartDate();
    Date end  = sobs.getEndDate();
    if ((start != null) && (end != null)) {
      DateFormatter format = new DateFormatter();
      Element dateRange = new Element("TimeSpan");
      dateRange.addContent(new Element("begin").addContent(format.toDateTimeStringISO(start)));
      dateRange.addContent(new Element("end").addContent(format.toDateTimeStringISO(end)));
      rootElem.addContent( dateRange);
    }

    // add accept list
    Element elem = new Element("AcceptList");
    elem.addContent(new Element("accept").addContent("raw"));
    elem.addContent(new Element("accept").addContent("xml"));
    elem.addContent(new Element("accept").addContent("csv"));
    elem.addContent(new Element("accept").addContent("netcdf"));
    elem.addContent(new Element("accept").addContent("netcdfStream"));
    rootElem.addContent(elem);

    return doc;
  }

  /* private List getCoordAxes(StationObsDataset gds) {
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
  } */

  /* private List getDimensions(StationObsDataset gds) {
    HashSet dimHash = new HashSet();
    List vars = gds.getDataVariables();
    for (int i = 0; i < vars.size(); i++) {
      VariableSimpleIF v = (VariableSimpleIF) vars.get(i);
      List dims = v.getDimensions();
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
    varElem.setAttribute("shape", axis.getDimensionsString());

    ucar.ma2.DataType dt = axis.getDataType();
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
  } */

  private Element writeBoundingBox(LatLonRect bb) {
    Element bbElem = new Element("LatLonBox");
    bbElem.addContent(new Element("west").addContent(ucar.unidata.util.Format.dfrac(bb.getLonMin(), 4)));
    bbElem.addContent(new Element("east").addContent(ucar.unidata.util.Format.dfrac(bb.getLonMax(), 4)));
    bbElem.addContent(new Element("south").addContent(ucar.unidata.util.Format.dfrac(bb.getLatMin(), 4)));
    bbElem.addContent(new Element("north").addContent(ucar.unidata.util.Format.dfrac(bb.getLatMax(), 4)));
    return bbElem;
  }

  private Element writeCoordSys(GridCoordSystem cs) {
    Element csElem = new Element("coordSys");
    csElem.setAttribute("name", cs.getName());
    List axes = cs.getCoordinateAxes();
    for (int i = 0; i < axes.size(); i++) {
      CoordinateAxis axis = (CoordinateAxis) axes.get(i);
      Element axisElem = new Element("axisRef");
      axisElem.setAttribute("name", axis.getFullName());
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

  private Element writeVariable(VariableSimpleIF v) {
    NcMLWriter ncmlWriter = new NcMLWriter();

    Element varElem = new Element("variable");
    varElem.setAttribute("name", v.getShortName());

    ucar.ma2.DataType dt = v.getDataType();
    if (dt != null)
      varElem.setAttribute("type", dt.toString());

    // attributes
    Iterator atts = v.getAttributes().iterator();
    while (atts.hasNext()) {
      ucar.nc2.Attribute att = (ucar.nc2.Attribute) atts.next();
      varElem.addContent(ncmlWriter.makeAttributeElement(att));
    }

    return varElem;
  }


  /**
   * debug
   */
  public static void main(String args[]) throws IOException {
    String url = "C:/data/metars/Surface_METAR_20060326_0000.nc";

    StationObsDataset ncd = (StationObsDataset) TypedDatasetFactory.open(FeatureType.STATION, url, null, new StringBuilder());
    StationObsDatasetInfo info = new StationObsDatasetInfo(ncd, null);
    FileOutputStream fos2 = new FileOutputStream("C:/TEMP/stationCollection.xml");
    GZIPOutputStream zout =  new GZIPOutputStream( fos2);

    info.writeStationObsDatasetXML(System.out);
    info.writeStationCollectionXML(zout);

    zout.close();
    File f = new File("C:/TEMP/stationCollection.xml");
    System.out.println(" size="+f.length());
  }

}

