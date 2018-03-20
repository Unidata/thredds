/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.writer;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.*;
import ucar.nc2.ft.point.CollectionInfo;
import ucar.nc2.ft.point.DsgCollectionHelper;
import ucar.nc2.ft.point.StationFeature;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.ncml.NcMLWriter;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Station;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * generate capabilities XML for a FeatureDatasetPoint / StationTimeSeriesFeatureCollection
 *
 * @author caron
 * @since Aug 19, 2009
 */
public class FeatureDatasetCapabilitiesWriter {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FeatureDatasetCapabilitiesWriter.class);

  private FeatureDatasetPoint fdp;
  private String path;

  public FeatureDatasetCapabilitiesWriter(FeatureDatasetPoint fdp, String path) {
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

  /**
   * Create an XML document for the stations in this dataset, possible subsetted by bb.
   * Must be a station dataset.
   *
   * @param bb    restrict stations to this bounding box, may be null
   * @param names restrict stations to these names, may be null
   * @return XML document for the stations
   * @throws IOException on read error
   */
  public Document makeStationCollectionDocument(LatLonRect bb, String[] names) throws IOException {

    List<DsgFeatureCollection> list = fdp.getPointFeatureCollectionList();
    DsgFeatureCollection fc = list.get(0); // LOOK maybe should pass in the dsg?

    if (!(fc instanceof StationTimeSeriesFeatureCollection)) {
      throw new UnsupportedOperationException(fc.getClass().getName() + " not a StationTimeSeriesFeatureCollection");
    }
    StationTimeSeriesFeatureCollection sobs = (StationTimeSeriesFeatureCollection) fc;

    Element rootElem = new Element("stationCollection");
    Document doc = new Document(rootElem);

    List<StationFeature> stations;
    if (bb != null)
      stations = sobs.getStationFeatures(bb);
    else if (names != null)
      stations = sobs.getStationFeatures(Arrays.asList(names));
    else
      stations = sobs.getStationFeatures();

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
   * Create the capabilities XML document for this dataset
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

    List<DsgFeatureCollection> list = fdp.getPointFeatureCollectionList();
    DsgFeatureCollection fc = list.get(0); // LOOK maybe should pass in the dsg?

    rootElem.addContent(writeTimeUnit(fc.getTimeUnit()));
    rootElem.addContent(new Element("AltitudeUnits").addContent(fc.getAltUnits()));

    // data variables
    List<? extends VariableSimpleIF> vars = fdp.getDataVariables();
    Collections.sort(vars);
    for (VariableSimpleIF v : vars) {
      rootElem.addContent(writeVariable(v));
    }

    /* CollectionInfo info;
    try {
      info = new DsgCollectionHelper(fc).calcBounds();
    } catch (IOException e) {
      throw new RuntimeException(e);
    } */

    LatLonRect bb = fc.getBoundingBox();
    if (bb != null)
      rootElem.addContent(writeBoundingBox(bb));

    // add date range
    CalendarDateRange dateRange = fc.getCalendarDateRange();
    if (dateRange != null) {
      Element drElem = new Element("TimeSpan"); // from KML
      drElem.addContent(new Element("begin").addContent(dateRange.getStart().toString()));
      drElem.addContent(new Element("end").addContent(dateRange.getEnd().toString()));
      if (dateRange.getResolution() != null)
        drElem.addContent(new Element("resolution").addContent(dateRange.getResolution().toString()));

      rootElem.addContent(drElem);
    }

    /* add accept list
    Element elem = new Element("AcceptList");
    //elem.addContent(new Element("accept").addContent("raw"));
    elem.addContent(new Element("accept").addContent("csv").setAttribute("displayName", "csv"));
    elem.addContent(new Element("accept").addContent("text/csv").setAttribute("displayName", "csv (file)"));
    elem.addContent(new Element("accept").addContent("xml").setAttribute("displayName", "xml"));
    elem.addContent(new Element("accept").addContent("text/xml").setAttribute("displayName", "xml (file)"));
    elem.addContent(new Element("accept").addContent("waterml2").setAttribute("displayName", "WaterML 2.0"));
    elem.addContent(new Element("accept").addContent("netcdf").setAttribute("displayName", "CF/NetCDF-3"));
    //elem.addContent(new Element("accept").addContent("ncstream"));
    rootElem.addContent(elem); */

    return doc;
  }

  private Element writeTimeUnit(CalendarDateUnit dateUnit) {
    Element elem = new Element("TimeUnit");
    elem.addContent(dateUnit.getUdUnit());
    elem.setAttribute("calendar", dateUnit.getCalendar().toString());
    return elem;
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
    NcMLWriter ncMLWriter = new NcMLWriter();
    Element varElem = new Element("variable");
    varElem.setAttribute("name", v.getShortName());

    ucar.ma2.DataType dt = v.getDataType();
    if (dt != null)
      varElem.setAttribute("type", dt.toString());

    // attributes
    for (Attribute att : v.getAttributes()) {
      varElem.addContent(ncMLWriter.makeAttributeElement(att));
    }

    return varElem;
  }

  /////////////////////////////////////////////
  //

  public Document readCapabilitiesDocument(InputStream in) throws JDOMException, IOException {
    SAXBuilder builder = new SAXBuilder();
    return builder.build(in);
  }

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

  public static CalendarDateUnit getTimeUnit(Document doc) {
    Element root = doc.getRootElement();
    Element timeUnitE = root.getChild("TimeUnit");
    if (timeUnitE == null) return null;

    String cal = timeUnitE.getAttributeValue("calendar");
    String timeUnitS = timeUnitE.getTextNormalize();

    try {
      return CalendarDateUnit.of(cal, timeUnitS);
    } catch (Exception e) {
      log.error("Illegal date unit {} in FeatureDatasetCapabilitiesXML", timeUnitS);
      return null;
    }
  }

  public static String getAltUnits(Document doc) throws IOException {
    Element root = doc.getRootElement();
    String altUnits = root.getChildText("AltitudeUnits");
    if (altUnits == null || altUnits.length() == 0) return null;
    return altUnits;
  }

  public static List<VariableSimpleIF> getDataVariables(Document doc) throws IOException {
    Element root = doc.getRootElement();

    List<VariableSimpleIF> dataVars = new ArrayList<>();
    List<Element> varElems = root.getChildren("variable");
    for (Element varElem : varElems) {
      dataVars.add(new VariableSimpleAdapter(varElem));
    }
    return dataVars;
  }

  private static class VariableSimpleAdapter implements VariableSimpleIF {
    String name, desc, units;
    DataType dt;
    List<Attribute> atts;

    VariableSimpleAdapter(Element velem) {
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

      VariableSimpleAdapter that = (VariableSimpleAdapter) o;
      return name.equals(that.name);
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }
  }


}


