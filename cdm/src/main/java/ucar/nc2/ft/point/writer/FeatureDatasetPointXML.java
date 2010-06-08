/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.ft.point.writer;

import ucar.nc2.VariableSimpleIF;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.ncml.NcMLWriter;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.ft.*;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.TimeDuration;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Station;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.ma2.DataType;

import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.Document;
import org.jdom.Element;

import java.io.OutputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.File;
import java.util.*;

/**
 * generate capabilities XML for a FeatureDatasetPoint / StationTimeSeriesFeatureCollection
 *
 * @author caron
 * @since Aug 19, 2009
 */
public class FeatureDatasetPointXML {
  private FeatureDatasetPoint fdp;
  private String path;
  //private PointFeatureCollection pfc;
  //private NestedPointFeatureCollection pfc;

  public FeatureDatasetPointXML(FeatureDatasetPoint fdp, String path) {
    this.fdp = fdp;
    this.path = path;
  }

  public String getCapabilities() {
    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    return fmt.outputString(getCapabilitiesDocument());
  }

  public void getCapabilities(OutputStream os) throws IOException {
    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    fmt.output(getCapabilitiesDocument(), os);
  }

  /*
   * Write stationCollection XML document
   *
  public String writeStationCollectionXML(LatLonRect bb, String[] names) throws IOException {
    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    return fmt.outputString(makeStationCollectionDocument(bb, names));
  }

  public void writeStationCollectionXML(OutputStream os) throws IOException {
    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    fmt.output(makeStationCollectionDocument(), os);
  } */

  /**
   * Create an XML document for the stations in this dataset.
   *
   * @param bb    restrict stations to this bounding box, may be null
   * @param names restrict stations to these names, may be null
   * @return XML document for the stations
   * @throws IOException on read error
   */
  public Document makeStationCollectionDocument(LatLonRect bb, String[] names) throws IOException {

    List<FeatureCollection> list = fdp.getPointFeatureCollectionList();
    FeatureCollection fc = (NestedPointFeatureCollection) list.get(0);

    if (!(fc instanceof StationTimeSeriesFeatureCollection))
      throw new UnsupportedOperationException( fc.getClass().getName()+" not a StationTimeSeriesFeatureCollection");

    StationTimeSeriesFeatureCollection sobs = (StationTimeSeriesFeatureCollection) fc;

    Element rootElem = new Element("stationCollection");
    Document doc = new Document(rootElem);

    List<Station> stations;
    if (bb != null)
      stations = sobs.getStations(bb);
    else if (names != null)
      stations = sobs.getStations(Arrays.asList(names));
    else
      stations = sobs.getStations();

    for (Station s : stations) {
      Element sElem = new Element("station");
      sElem.setAttribute("name", s.getName());
      if (s.getWmoId() != null)
        sElem.setAttribute("wmo_id", s.getWmoId());
      if ((s.getDescription() != null) && (s.getDescription().length() > 0))
        sElem.addContent(new Element("description").addContent(s.getDescription()));

      sElem.addContent(new Element("longitude").addContent(Double.toString(s.getLongitude())));
      sElem.addContent(new Element("latitide").addContent(Double.toString(s.getLatitude())));
      if (!Double.isNaN(s.getAltitude()))
        sElem.addContent(new Element("altitude").addContent(Double.toString(s.getAltitude())));
      rootElem.addContent(sElem);
    }

    return doc;
  }

  /**
   * Create the capabilities XML document for this datasets
   *
   * @return capabilities XML document
   */
  public Document getCapabilitiesDocument() {
    Element rootElem = new Element("capabilities");
    Document doc = new Document(rootElem);
    if (null != path) {
      rootElem.setAttribute("location", path);
      Element elem = new Element("featureDataset");
      elem.setAttribute("type", fdp.getFeatureType().toString().toLowerCase());
      elem.setAttribute("url", path + "/" + fdp.getFeatureType().toString().toLowerCase());
      rootElem.addContent(elem);
    }

    // data variables
    List<? extends VariableSimpleIF> vars = fdp.getDataVariables();
    Collections.sort(vars);
    for (VariableSimpleIF v : vars) {
      rootElem.addContent(writeVariable(v));
    }

    // add lat/lon bounding box
    LatLonRect bb = fdp.getBoundingBox();
    if (bb != null)
      rootElem.addContent(writeBoundingBox(bb));

    // add date range
    DateRange dateRange = fdp.getDateRange();
    if (dateRange != null) {
      DateFormatter format = new DateFormatter();
      Element drElem = new Element("TimeSpan"); // from KML
      drElem.addContent(new Element("begin").addContent(dateRange.getStart().toDateTimeStringISO()));
      drElem.addContent(new Element("end").addContent(dateRange.getEnd().toDateTimeStringISO()));
      if (dateRange.getResolution() != null)
        drElem.addContent(new Element("resolution").addContent(dateRange.getResolution().toString()));

      rootElem.addContent(drElem);
    }

    // add accept list
    Element elem = new Element("AcceptList");
    //elem.addContent(new Element("accept").addContent("raw"));
    elem.addContent(new Element("accept").addContent("csv"));
    elem.addContent(new Element("accept").addContent("xml"));
    elem.addContent(new Element("accept").addContent("netcdf"));
    elem.addContent(new Element("accept").addContent("ncstream"));
    rootElem.addContent(elem);

    return doc;
  }

  private Element writeBoundingBox(LatLonRect bb) {
    Element bbElem = new Element("LatLonBox"); // from KML
    bbElem.addContent(new Element("west").addContent(ucar.unidata.util.Format.dfrac(bb.getLonMin(), 6)));
    bbElem.addContent(new Element("east").addContent(ucar.unidata.util.Format.dfrac(bb.getLonMax(), 6)));
    bbElem.addContent(new Element("south").addContent(ucar.unidata.util.Format.dfrac(bb.getLatMin(), 6)));
    bbElem.addContent(new Element("north").addContent(ucar.unidata.util.Format.dfrac(bb.getLatMax(), 6)));
    return bbElem;
  }

  private Element writeVariable(VariableSimpleIF v) {
    Element varElem = new Element("variable");
    varElem.setAttribute("name", v.getShortName());

    ucar.ma2.DataType dt = v.getDataType();
    if (dt != null)
      varElem.setAttribute("type", dt.toString());

    // attributes
    for (Attribute att : v.getAttributes()) {
      varElem.addContent(NcMLWriter.writeAttribute(att, "attribute", null));
    }

    return varElem;
  }

  /////////////////////////////////////////////
  public static LatLonRect getSpatialExtent(Document doc) throws IOException {
    Element root = doc.getRootElement();
    Element latlonBox = root.getChild("LatLonBox");
    if (latlonBox == null) return null;

    String westS = latlonBox.getChildText("west");
    String eastS = latlonBox.getChildText("east");
    String northS = latlonBox.getChildText("north");
    String southS = latlonBox.getChildText("south");
    if ((westS == null) || (eastS == null) || (northS == null) || (southS == null)) return null;

    try {
      double west = Double.parseDouble(westS);
      double east = Double.parseDouble(eastS);
      double south = Double.parseDouble(southS);
      double north = Double.parseDouble(northS);
      return new LatLonRect(new LatLonPointImpl(south, east), new LatLonPointImpl(north, west));

    } catch (Exception e) {
      return null;
    }
  }

  public static DateRange getTimeSpan(Document doc) throws IOException {
    Element root = doc.getRootElement();
    Element timeSpan = root.getChild("TimeSpan");
    if (timeSpan == null) return null;

    String beginS = timeSpan.getChildText("begin");
    String endS = timeSpan.getChildText("end");
    String resS = timeSpan.getChildText("resolution");
    if ((beginS == null) || (endS == null)) return null;

    DateFormatter format = new DateFormatter();
    try {
      Date start = format.getISODate(beginS);
      Date end = format.getISODate(endS);
      DateRange dr = new DateRange(start, end);
      
      if (resS != null)
        dr.setResolution(new TimeDuration(resS));

      return dr;

    } catch (Exception e) {
      return null;
    }
  }

  public static List<VariableSimpleIF> getDataVariables(Document doc) throws IOException {
    Element root = doc.getRootElement();

    List<VariableSimpleIF> dataVars = new ArrayList<VariableSimpleIF>();
    List<Element> varElems = root.getChildren("variable");
    for (Element varElem : varElems) {
      dataVars.add(new VariableSimple(varElem));
    }
    return dataVars;
  }

  private static class VariableSimple implements VariableSimpleIF {
    String name, desc, units;
    DataType dt;
    List<Attribute> atts;

    VariableSimple(Element velem) {
      name = velem.getAttributeValue("name");
      String type = velem.getAttributeValue("type");
      dt = DataType.getType(type);

      atts = new ArrayList<Attribute>();
      List<Element> attElems = velem.getChildren("attribute");
      for (Element attElem : attElems) {
        String attName = attElem.getAttributeValue("name");
        ucar.ma2.Array values = NcMLReader.readAttributeValues(attElem);
        atts.add(new Attribute(attName, values));
      }

      for (Attribute att : atts) {
        if (att.getName().equals("units"))
          units = att.getStringValue();
        if (att.getName().equals("long_name"))
          desc = att.getStringValue();
        if ((desc == null) && att.getName().equals("description"))
          desc = att.getStringValue();
        if ((desc == null) && att.getName().equals("standard_name"))
          desc = att.getStringValue();
      }
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String getShortName() {
      return name;
    }

    @Override
    public String getDescription() {
      return desc;
    }

    @Override
    public String getUnitsString() {
      return units;
    }

    @Override
    public int getRank() {
      return 0;
    }

    @Override
    public int[] getShape() {
      return new int[0];
    }

    @Override
    public List<Dimension> getDimensions() {
      return null;
    }

    @Override
    public DataType getDataType() {
      return dt;
    }

    @Override
    public List<Attribute> getAttributes() {
      return atts;
    }

    @Override
    public Attribute findAttributeIgnoreCase(String name) {
      for (Attribute att : atts) {
        if (att.getName().equalsIgnoreCase(name))
          return att;
      }
      return null;
    }

    @Override
    public int compareTo(VariableSimpleIF o) {
      return name.compareTo(o.getName()); // ??
    }
  }

  // debug
  public static void main(String args[]) throws IOException {
    String location = "D:/datasets/metars/Surface_METAR_20070516_0000.nc";
    String path = "http://motherlode.ucar.edu:9080/thredds/cdmremote/idd/metar/gempak/collection";

    FeatureDataset fd = FeatureDatasetFactoryManager.open(FeatureType.ANY_POINT, location, null, new Formatter(System.out));
    FeatureDatasetPointXML xml = new FeatureDatasetPointXML((FeatureDatasetPoint) fd, path);
    xml.getCapabilities(System.out);

    File f = new File("C:/TEMP/stationCapabilities.xml");
    FileOutputStream fos = new FileOutputStream(f);
    xml.getCapabilities(fos);
    fos.close();

    /* File f = new File("C:/TEMP/stationCollection.xml");
    FileOutputStream fos = new FileOutputStream(f);
    xml.writeStationCollectionXML(fos);
    fos.close();
    System.out.println(" size xml=" + f.length());
    long s1 = f.length();

    f = new File("C:/TEMP/stationCollection.xml.gzip");
    fos = new FileOutputStream(f);
    GZIPOutputStream zout = new GZIPOutputStream(fos);
    xml.writeStationCollectionXML(zout);
    zout.close();
    double s2 = (double) f.length();
    System.out.printf(" size xml zipped=%d ratio=%f%n", f.length(), s1/s2);  */
  }

}


