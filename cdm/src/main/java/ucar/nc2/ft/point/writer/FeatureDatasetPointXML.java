/*
 * Copyright (c) 1998 - 2014. University Corporation for Atmospheric Research/Unidata
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

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.*;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.ncml.NcMLWriter;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateUnit;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Station;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

/**
 * generate capabilities XML for a FeatureDatasetPoint / StationTimeSeriesFeatureCollection
 *
 * @author caron
 * @since Aug 19, 2009
 */
public class FeatureDatasetPointXML {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FeatureDatasetPointXML.class);

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
    FeatureCollection fc = list.get(0);

    if (!(fc instanceof StationTimeSeriesFeatureCollection)) {
      throw new UnsupportedOperationException(fc.getClass().getName() + " not a StationTimeSeriesFeatureCollection");
    }
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
      FeatureType ft = fdp.getFeatureType();
      elem.setAttribute("type", ft.toString().toLowerCase());
      String url = path.replace("dataset.xml", ft.toString().toLowerCase() + ".xml");
      elem.setAttribute("url", url);
      rootElem.addContent(elem);
    }

    //rootElem.addContent(new Element("TimeUnit").addContent(fdp.getTimeUnit()));
    //rootElem.addContent(new Element("AltitudeUnits").addContent(fdp.getAltUnits()));

    // data variables
    List<? extends VariableSimpleIF> vars = fdp.getDataVariables();
    Collections.sort(vars);
    for (VariableSimpleIF v : vars) {
      rootElem.addContent(writeVariable(v));
    }

    // add lat/lon bounding box
    try {
      fdp.calcBounds();
    } catch (IOException e) {
      //e.printStackTrace();
      log.warn("Unable to compute bounds for dataset " + fdp.getTitle(), e);
    }

    LatLonRect bb = fdp.getBoundingBox();
    if (bb != null)
      rootElem.addContent(writeBoundingBox(bb));

    // add date range
    CalendarDateRange dateRange = fdp.getCalendarDateRange();
    if (dateRange != null) {
      Element drElem = new Element("TimeSpan"); // from KML
      drElem.addContent(new Element("begin").addContent(dateRange.getStart().toString()));
      drElem.addContent(new Element("end").addContent(dateRange.getEnd().toString()));
      if (dateRange.getResolution() != null)
        drElem.addContent(new Element("resolution").addContent(dateRange.getResolution().toString()));

      rootElem.addContent(drElem);
    }

    // add accept list
    Element elem = new Element("AcceptList");
    //elem.addContent(new Element("accept").addContent("raw"));
    elem.addContent(new Element("accept").addContent("csv").setAttribute("displayName", "csv"));
    elem.addContent(new Element("accept").addContent("text/csv").setAttribute("displayName", "csv (file)"));
    elem.addContent(new Element("accept").addContent("geocsv").setAttribute("displayName", "geocsv"));
    elem.addContent(new Element("accept").addContent("text/geocsv").setAttribute("displayName", "geocsv (file)"));
    elem.addContent(new Element("accept").addContent("xml").setAttribute("displayName", "xml"));
    elem.addContent(new Element("accept").addContent("text/xml").setAttribute("displayName", "xml (file)"));
    elem.addContent(new Element("accept").addContent("waterml2").setAttribute("displayName", "WaterML 2.0"));
    elem.addContent(new Element("accept").addContent("netcdf").setAttribute("displayName", "CF/NetCDF-3"));
    //elem.addContent(new Element("accept").addContent("ncstream"));
    rootElem.addContent(elem);

    return doc;
  }

  private Element writeBoundingBox(LatLonRect bb) {
    int decToKeep = 6;
    double bbExpand = Math.pow(10, -decToKeep);

    // extend the bbox to make sure the implicit rounding does not result in a bbox that does not contain
    //   any points (can happen when you have a single station with very precise lat/lon values)
    //   See https://github.com/Unidata/thredds/issues/470
    // This accounts for the implicit rounding errors that result from the use of
    //   ucar.unidata.util.Format.dfrac when writing out the lat/lon box on the NCSS for Points dataset.html
    //   page
    LatLonPointImpl extendNorthEast = new LatLonPointImpl(bb.getLatMax() + bbExpand, bb.getLonMax() + bbExpand);
    LatLonPointImpl extendSouthWest = new LatLonPointImpl(bb.getLatMin() - bbExpand, bb.getLonMin() - bbExpand);
    bb.extend(extendNorthEast);
    bb.extend(extendSouthWest);

    Element bbElem = new Element("LatLonBox"); // from KML

    bbElem.addContent(new Element("west").addContent(ucar.unidata.util.Format.dfrac(bb.getLonMin(), decToKeep)));
    bbElem.addContent(new Element("east").addContent(ucar.unidata.util.Format.dfrac(bb.getLonMax(), decToKeep)));
    bbElem.addContent(new Element("south").addContent(ucar.unidata.util.Format.dfrac(bb.getLatMin(), decToKeep)));
    bbElem.addContent(new Element("north").addContent(ucar.unidata.util.Format.dfrac(bb.getLatMax(), decToKeep)));
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

  public static CalendarDateRange getTimeSpan(Document doc) throws IOException {
    Element root = doc.getRootElement();
    Element timeSpan = root.getChild("TimeSpan");
    if (timeSpan == null) return null;

    String beginS = timeSpan.getChildText("begin");
    String endS = timeSpan.getChildText("end");
//    String resS = timeSpan.getChildText("resolution");
    if ((beginS == null) || (endS == null)) return null;

    try {
      CalendarDate start = CalendarDateFormatter.isoStringToCalendarDate(null, beginS);
      CalendarDate end = CalendarDateFormatter.isoStringToCalendarDate(null, endS);
      if ((start == null) || (end == null)) {
        return null;
      }

      CalendarDateRange dr = CalendarDateRange.of(start, end);

      // LOOK if (resS != null)
      //  dr.setResolution(new TimeDuration(resS));

      return dr;

    } catch (Exception e) {
      return null;
    }
  }

  public static DateUnit getTimeUnit(Document doc) throws IOException {
    Element root = doc.getRootElement();
    String timeUnitS = root.getChildText("TimeUnit");
    if (timeUnitS == null) return null;

    try {
      return new DateUnit(timeUnitS);
    } catch (Exception e) {
      log.error("Illegal date unit {}", timeUnitS);
      return null;
    }
  }

  public static String getAltUnits(Document doc) throws IOException {
    Element root = doc.getRootElement();
    return root.getChildText("AltitudeUnits");
  }

  public static List<VariableSimpleIF> getDataVariables(Document doc) throws IOException {
    Element root = doc.getRootElement();

    List<VariableSimpleIF> dataVars = new ArrayList<>();
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

      atts = new ArrayList<>();
      List<Element> attElems = velem.getChildren("attribute");
      for (Element attElem : attElems) {
        String attName = attElem.getAttributeValue("name");
        ucar.ma2.Array values = NcMLReader.readAttributeValues(attElem);
        atts.add(new Attribute(attName, values));
      }

      for (Attribute att : atts) {
        if (att.getShortName().equals(CDM.UNITS))
          units = att.getStringValue();
        if (att.getShortName().equals(CDM.LONG_NAME))
          desc = att.getStringValue();
        if ((desc == null) && att.getShortName().equals("description"))
          desc = att.getStringValue();
        if ((desc == null) && att.getShortName().equals("standard_name"))
          desc = att.getStringValue();
      }
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String getFullName() {
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
        if (att.getShortName().equalsIgnoreCase(name))
          return att;
      }
      return null;
    }

    @Override
    public int compareTo(VariableSimpleIF o) {
      return name.compareTo(o.getShortName()); // ??
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      VariableSimple that = (VariableSimple) o;
      return name.equals(that.name);
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }
  }

  public static void doOne(String location, String path, String result) throws IOException {
    FeatureDataset fd = FeatureDatasetFactoryManager.open(FeatureType.ANY_POINT, location, null, new Formatter(System.out));
    FeatureDatasetPointXML xml = new FeatureDatasetPointXML((FeatureDatasetPoint) fd, path);
    xml.getCapabilities(System.out);

    File f = new File(result);
    FileOutputStream fos = new FileOutputStream(f);
    xml.getCapabilities(fos);
    fos.close();
    System.out.printf("%s written%n", f.getPath());
  }

  // debug
  public static void main(String args[]) throws IOException {
    /* doOne( "Q:/cdmUnitTest/ft/station/Surface_METAR_20080205_0000.nc",
          "http://motherlode.ucar.edu:9080/thredds/cdmremote/idd/metar/gempak/collection",
          "C:/tmp/stationCapabilities.xml"
    ); */

    doOne("Q:/cdmUnitTest/ft/point/ship/nc/Surface_Buoy_20090920_0000.nc",
            "http://thredds.ucar.edu/thredds/cdmremote/idd/buoy/collection",
            "C:/tmp/pointCapabilities.xml"
    );


    /*
    File f = new File("C:/TEMP/stationCollection.xml");
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


