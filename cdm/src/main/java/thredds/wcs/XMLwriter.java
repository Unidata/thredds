// $Id: XMLwriter.java,v 1.7 2006/04/20 22:13:16 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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

package thredds.wcs;

import ucar.nc2.dataset.grid.*;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.DateFormatter;
import ucar.unidata.geoloc.*;
import ucar.unidata.util.StringUtil;

import org.jdom.*;
import java.util.*;

import thredds.catalog.XMLEntityResolver;

public class XMLwriter {
  protected static final Namespace wcsNS = Namespace.getNamespace("http://www.opengis.net/wcs");
  protected static final Namespace xlinkNS = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");
  protected static final Namespace gmlNS = Namespace.getNamespace("gml", "http://www.opengis.net/gml");

  protected static final String wcsLocation = "http://schemas.opengis.net/wcs/1.0.0/wcsCapabilities.xsd";
  protected static final String dcLocation = "http://schemas.opengis.net/wcs/1.0.0/describeCoverage.xsd";

  public static String seqDate;
  static {
    // will get incremented each time we start up
    DateFormatter formatter = new DateFormatter();
    seqDate = formatter.toDateTimeStringISO(new Date());
  }
  //////////////////////////////////////////////////////////////////////////////////

  private boolean useTimeRange = false;

  public Document makeCapabilities(String serverURL, GridDataset dataset, SectionType section) {
    Element rootElem = new Element("WCS_Capabilities", wcsNS);
    Document doc = new Document(rootElem);
    addNamespaces( rootElem, wcsLocation);

    if (section == null) {
      rootElem.addContent(makeService( dataset));
      rootElem.addContent(makeCapability( serverURL, dataset));
      rootElem.addContent(makeContentMetadata( dataset));

    } else if (section == SectionType.Service) {
      rootElem.addContent(makeService( dataset));

    } else if (section == SectionType.Capability) {
      rootElem.addContent(makeCapability( serverURL, dataset));

    } else if (section == SectionType.ContentMetadata) {
      rootElem.addContent(makeContentMetadata( dataset));
    }

    rootElem.setAttribute( "updateSequence", seqDate);
    return doc;
  }

  private void addNamespaces( Element rootElem, String location) {
    rootElem.addNamespaceDeclaration(wcsNS);
    rootElem.addNamespaceDeclaration(xlinkNS);
    rootElem.addNamespaceDeclaration(gmlNS);
    rootElem.addNamespaceDeclaration( XMLEntityResolver.xsiNS);
    rootElem.setAttribute("version", "1.0.0");
    // rootElem.setAttribute("schemaLocation", "http://www.opengis.net/wcs " + location, XMLEntityResolver.xsiNS);
    rootElem.setAttribute("schemaLocation", wcsNS.getURI()+" "+location, XMLEntityResolver.xsiNS);
  }


  private Element makeService(GridDataset dataset) {
    Element serviceElem = new Element("Service", wcsNS);

    addElement( serviceElem, "description", "Experimental THREDDS/WCS server for CDM gridded datasets");
    addElement( serviceElem, "name", dataset.getNetcdfDataset().getLocation());
    addElement( serviceElem, "label", "Experimental THREDDS/WCS for "+dataset.getNetcdfDataset().getLocation());
    Element keywords = addElement( serviceElem, "keywords", null);
    addElement( keywords, "keyword", null);
    addElement( serviceElem, "fees", "NONE");
    addElement( serviceElem, "accessConstraints", "NONE");

    return serviceElem;
  }

  private Element makeCapability(String serverURL, GridDataset dataset) {
    //String urlString = serverURL + "?"; // dataset="+ dataset.getNetcdfDataset().getLocation();

    Element capabilityElem = new Element("Capability", wcsNS);
    Element reqElem = addElement(capabilityElem, "Request", null);
    Element capElem = addElement(reqElem, "GetCapabilities", null);
    capElem.addContent( makeDCP( serverURL));

    Element descElem = addElement(reqElem, "DescribeCoverage", null);
    descElem.addContent(makeDCP(serverURL));

    Element getElem = addElement(reqElem, "GetCoverage", null);
    getElem.addContent(makeDCP(serverURL));

    Element exElem = addElement(capabilityElem, "Exception", null);
    addElement(exElem, "Format", "application/vnd.ogc.se_xml");

    return capabilityElem;
  }

  private Element makeDCP (String link) {
    Element dcpElem = new Element("DCPType", wcsNS);
    Element httpElem = addElement(dcpElem, "HTTP", null);
    Element getElem = addElement(httpElem, "Get", null);
    Element onlineElem = addElement(getElem, "OnlineResource", null);
    onlineElem.setAttribute("href", link, xlinkNS);
    onlineElem.setAttribute("type", "simple", xlinkNS);

    return dcpElem;
  }

  private Element makeContentMetadata(GridDataset dataset) {
    Element contentElem = new Element("ContentMetadata", wcsNS);

    Collection grids = dataset.getGrids();
    for (Iterator iterator = grids.iterator(); iterator.hasNext();) {
      GeoGrid grid = (GeoGrid) iterator.next();
      Element elem = makeCoverageOfferingBrief(grid);
      if (null != elem)
        contentElem.addContent(elem);
    }

    return contentElem;
  }

  private Element makeCoverageOfferingBrief(GeoGrid grid) {
    Element briefElem = new Element("CoverageOfferingBrief", wcsNS);

    addElement(briefElem, "name",grid.getName());
    addElement(briefElem, "label",grid.getDescription());

    GridCoordSys gcs = grid.getCoordinateSystem();
    if (!gcs.isRegularSpatial()) {
      System.out.println("**Coordinate System not regular for "+grid.getName());
      return null;
    }

    Element llElem = new Element("lonLatEnvelope", wcsNS);
    addLonLatEnvelope(llElem, gcs);
    briefElem.addContent( llElem);
    return briefElem;
  }

  private void addLonLatEnvelope(Element elem, GridCoordSys gcs) {
    elem.setAttribute("srsName", "WGS84(DD)");

    LatLonRect llbb = gcs.getLatLonBoundingBox();
    LatLonPoint llpt = llbb.getLowerLeftPoint();
    LatLonPoint urpt = llbb.getUpperRightPoint();

    elem.addContent( makePos(llpt));
    double lon  = llpt.getLongitude() + llbb.getWidth();
    elem.addContent(makePos( urpt.getLatitude(), lon));
  }

  //////////////////////////////////////////////////////////////////////////////////

  public Document makeDescribeCoverage(GridDataset dataset, String[] gridNames) {
    Element rootElem = new Element("CoverageDescription", wcsNS);
    Document doc = new Document(rootElem);
    addNamespaces( rootElem, dcLocation);

    if (gridNames == null) {
      Collection grids = dataset.getGrids();
      for (Iterator iterator = grids.iterator(); iterator.hasNext();) {
        GeoGrid grid = (GeoGrid) iterator.next();
        Element elem = makeCoverageDescription(grid);
        rootElem.addContent(elem);
      }
    } else {
      for (int i = 0; i < gridNames.length; i++) {
        GeoGrid grid = dataset.findGridByName(gridNames[i]);
        Element elem = makeCoverageDescription(grid);
        rootElem.addContent(elem);
      }
    }

    return doc;
  }

  private Element makeCoverageDescription(GeoGrid grid) {
    GridCoordSys gcs = grid.getCoordinateSystem();

    Element offeringElem = makeCoverageOfferingBrief(grid);
    offeringElem.setName("CoverageOffering");

    offeringElem.addContent(makeDomainSet(grid));
    offeringElem.addContent(makeRangeSet(grid));
    offeringElem.addContent(makeSupportedCRS(gcs));
    offeringElem.addContent(makeSupportedFormats());
    offeringElem.addContent(makeSupportedInterpolations());

    return offeringElem;
  }

  private Element makeDomainSet(GeoGrid grid) {
    Element domainElem = new Element("domainSet", wcsNS);

    domainElem.addContent( makeSpatialDomain( grid));

    GridCoordSys gcs = grid.getCoordinateSystem();
    if (gcs.isDate()) {
      java.util.Date[] dates = gcs.getTimeDates();
      int n = dates.length;
      if (useTimeRange)
        domainElem.addContent( makeTemporalDomainRange( dates[0], dates[n-1]));
      else
        domainElem.addContent( makeTemporalDomain( dates));
    }
    return domainElem;
  }

  private Element makePos(LatLonPoint pt) {
    Element posElem = new Element("pos", gmlNS);
    posElem.addContent(pt.getLongitude()+" "+ pt.getLatitude());
    return posElem;
  }

  private Element makePos(double lat, double lon) {
    Element posElem = new Element("pos", gmlNS);
    posElem.addContent(lon+" "+ lat);
    return posElem;
  }

  public Element makeRangeSet(GeoGrid grid) {
    Element rangeElem = new Element("rangeSet", wcsNS);
    Element RangeElem = addElement(rangeElem, "RangeSet", null);
    addElement(RangeElem, "name", "RangeSetName");
    addElement(RangeElem, "label", "RangeSetLabel");

    GridCoordSys gcs = grid.getCoordinateSystem();
    CoordinateAxis zaxis = gcs.getVerticalAxis();
    if (zaxis != null) {
      Element axisElem = addElement(RangeElem, "axisDescription", null);
      Element AxisElem = addElement(axisElem, "AxisDescription", null);
      addElement(AxisElem, "name", "Vertical");
      addElement(AxisElem, "label", zaxis.getName());

      Element valueElem = addElement(AxisElem, "values", null);
      for (int z=0; z<zaxis.getSize(); z++)
        addElement(valueElem, "singleValue", gcs.getLevelName(z).trim());
    }

    if (grid.hasMissingData()) {
      Element nullElem = addElement(RangeElem, "nullValues", null);
      addElement(nullElem, "singleValue", "NaN");
    }

    return rangeElem;
  }

  private Element makeRectifiedGrid(GridCoordSys gcs) {
    Element gridElem = new Element("RectifiedGrid", gmlNS);

    CoordinateAxis1D xaxis = (CoordinateAxis1D) gcs.getXHorizAxis();
    CoordinateAxis1D yaxis = (CoordinateAxis1D) gcs.getYHorizAxis();
    CoordinateAxis1D zaxis = (CoordinateAxis1D) gcs.getVerticalAxis();

    int ndim = (zaxis != null) ? 3 : 2;
    gridElem.setAttribute("dimension", Integer.toString(ndim));

    // limits
    int[] minValues = new int[ndim];
    int[] maxValues = new int[ndim];

    maxValues[0] = (int) (xaxis.getSize()-1);
    maxValues[1] = (int) (yaxis.getSize()-1);
    if (zaxis != null)
      maxValues[2] = (int) (zaxis.getSize()-1);

    Element limitsElem = addElement(gridElem, "limits", null, gmlNS);
    Element gridEnvelopeElem = addElement(limitsElem, "GridEnvelope", null, gmlNS);
    addElement(gridEnvelopeElem, "low", toString(minValues), gmlNS);
    addElement(gridEnvelopeElem, "high", toString(maxValues), gmlNS);

    // list of axes
    addElement(gridElem, "axisName", "x", gmlNS);
    addElement(gridElem, "axisName", "y", gmlNS);
    if (zaxis != null)
      addElement(gridElem, "axisName", "z", gmlNS);

    // origin
    double[] origin = new double[ndim];
    origin[0] = xaxis.getStart();
    origin[1] = yaxis.getStart();
    if (zaxis != null)
      origin[2] = zaxis.getStart();

    Element originElem = addElement(gridElem, "origin", null, gmlNS);
    addElement(originElem, "pos", toString(origin), gmlNS);

    // offsets
    double[] xoffset = new double[ndim];
    xoffset[0] = xaxis.getIncrement();
    addElement(gridElem, "offsetVector", toString(xoffset), gmlNS);

    double[] yoffset = new double[ndim];
    yoffset[1] = yaxis.getIncrement();
    addElement(gridElem, "offsetVector", toString(yoffset), gmlNS);

    if (zaxis != null) {
      double[] zoffset = new double[ndim];
      zoffset[2] = zaxis.getIncrement();
      addElement(gridElem, "offsetVector", toString(zoffset), gmlNS);
    }

    return gridElem;
  }

  private Element makeSpatialDomain(GeoGrid grid) {
    Element spatialElem = new Element("spatialDomain", wcsNS);
    GridCoordSys gcs = grid.getCoordinateSystem();

    Element envElem = new Element("Envelope", gmlNS);
    addLonLatEnvelope(envElem, gcs);
    spatialElem.addContent( envElem);

    spatialElem.addContent( makeRectifiedGrid( gcs));

    return spatialElem;
  }

  private Element makeSupportedCRS(GridCoordSys gcs) {
    Element elem = new Element("supportedCRSs", wcsNS);
    addElement(elem, "requestCRSs", "EPSG:4326");
    addElement(elem, "responseCRSs", "EPSG:4326");
    if (gcs.isLatLon())
      addElement(elem, "nativeCRSs", "EPSG:4326");
    return elem;
  }

  private Element makeSupportedFormats() {
    Element elem = new Element("supportedFormats", wcsNS);
    addElement(elem, "formats", "GeoTIFF");
    addElement(elem, "formats", "GeoTIFFfloat");
    addElement(elem, "formats", "NetCDF3");
    return elem;
  }

  private Element makeSupportedInterpolations() {
    Element elem = new Element("supportedInterpolations", wcsNS);
    addElement(elem, "interpolationMethod", "none");
    return elem;
  }


  private Element makeTemporalDomainRange(Date start, Date end) {
    Element temporalElem = new Element("temporalDomain", wcsNS);
    Element timeElem = new Element("timePeriod", wcsNS);
    temporalElem.addContent( timeElem);

    DateFormatter formatter = new DateFormatter();
    addElement( timeElem, "beginPosition", formatter.toDateTimeStringISO(start));
    addElement( timeElem, "endPosition", formatter.toDateTimeStringISO(end));

    return temporalElem;
  }

  private Element makeTemporalDomain(Date[] dates) {
    Element temporalElem = new Element("temporalDomain", wcsNS);

    DateFormatter formatter = new DateFormatter();
    for (int i = 0; i < dates.length; i++) {
      addElement( temporalElem, "timePosition", formatter.toDateTimeStringISO(dates[i]), gmlNS);
    }

    return temporalElem;
  }


  /////////////////////////////////////////////////////////////////////////////

  private Element addElement(Element parent, String name, String value) {
    Element elem = new Element(name, wcsNS);
    if (value != null) {
      elem.addContent(value);
    }
    parent.addContent(elem);
    return elem;
  }

  private Element addElement(Element parent, String name, String value, Namespace ns) {
    Element elem = new Element(name, ns);
    if (value != null) {
      elem.addContent(value);
    }
    parent.addContent(elem);
    return elem;
  }

  private String toString( int[] values) {
    StringBuffer buff = new StringBuffer();
    for (int i = 0; i < values.length; i++) {
      int value = values[i];
      buff.append( value);
      buff.append( ' ');
    }
    return buff.toString();
  }

  private String toString( double[] values) {
    StringBuffer buff = new StringBuffer();
    for (int i = 0; i < values.length; i++) {
      double value = values[i];
      buff.append( value);
      buff.append( ' ');
    }
    return buff.toString();
  }

  /*
   *
   * @param dataset
   * @param gridset
   * @param title
   * @return
   *
  private Element makeRectifiedGridCoverageLayer(GridDataset dataset,
                                        GridDataset.Gridset gridset,
                                        String title) {
    Element layerElem = new Element("GridCoverageLayer", wcsNS);

    GridCoordSys gcs = gridset.getGeoCoordSys();
    addElement(layerElem, "LayerID", dataset.getNetcdfDataset().getLocation());
    addElement(layerElem, "Title",
               title == null ? "title for " + gcs.getName() : title);

    LatLonRect llbb = gcs.getLatLonBoundingBox();
    layerElem.addContent(makeLatLonBoundingBox(llbb));
    addElement(layerElem, "SRS", "EPSG:4326");
    layerElem.addContent(makeSupportedFormatList());

    layerElem.addContent(makeSupportedInterpolationList());

    layerElem.addContent(makeRectifiedGridExtentDescription(gcs));

    Element rangeElem = addElement(layerElem, "RangeSetDescription", null);
    Iterator iter = gridset.getGrids().iterator();
    while (iter.hasNext()) {
      GeoGrid grid = (GeoGrid) iter.next();
      rangeElem.addContent(makeGridRangeDescription(grid));
    }
    return layerElem;
  }



  private Element makeLatLonBoundingBox(LatLonRect llbb) {
    Element llbbElem = new Element("LatLonBoundingBox", wcsNS);
    LatLonPoint ll = llbb.getLowerLeftPoint();
    LatLonPoint ur = llbb.getUpperRightPoint();

    llbbElem.setAttribute("minx", Double.toString(ll.getLongitude()));
    llbbElem.setAttribute("miny", Double.toString(ll.getLatitude()));
    llbbElem.setAttribute("maxx",
                          Double.toString(ll.getLongitude() + llbb.getWidth()));
    llbbElem.setAttribute("maxy", Double.toString(ur.getLatitude()));

    return llbbElem;
  }


  private Element makeSupportedFormatList() {
    Element elem = new Element("SupportedFormatList", wcsNS);
    elem.setAttribute("nativeFormat", "GeoTIFF");
    elem.addContent(makeFormat("GeoTIFF", "GeoTIFF 1.0", "image/tif"));
    return elem;
  }


  private Element makeFormat(String name, String desc, String mimeType) {
    Element elem = new Element("Format", wcsNS);
    addElement(elem, "FormatName", name);
    addElement(elem, "description", desc);
    addElement(elem, "MIMEType", mimeType);
    return elem;
  }


  private Element makeGridExtentDescription(GridCoordSys gcs) {
    Element elem = new Element("GridExtentDescription", wcsNS);
    elem.addContent(makeSpatialExtent(gcs));
    if (gcs.getTimeAxis() != null) {
      elem.addContent(makeTemporalExtent(gcs));
      // tElem.addContent( makeExtent("Interval", gcs.getTimeAxis()));
    }
    elem.addContent(makeGridAxisDescription(gcs));
    elem.addContent(makeGrid(gcs));
    return elem;
  }


  private Element makeRectifiedGridExtentDescription(GridCoordSys gcs) {
    Element elem = new Element("GridExtentDescription", wcsNS);
    elem.addContent(makeSpatialExtent(gcs));
    if (gcs.getTimeAxis() != null) {
      elem.addContent(makeTemporalExtent(gcs));
      // tElem.addContent( makeExtent("Interval", gcs.getTimeAxis()));
    }
    elem.addContent(makeGridAxisDescription(gcs));
    elem.addContent(makeGridSpacing("0.5"));
    elem.addContent(makeRectifiedGrid(gcs));
    return elem;
  }


  private Element makeGridSpacing(String value) {
    Element elem = new Element("GridSpacing", wcsNS);
    addElement(elem, "resolution", value);
    addElement(elem, "resolution", value);
    return elem;
  }


  private Element makeSpatialExtent(GridCoordSys gcs) {
    Element elem = new Element("SpatialExtent", wcsNS);
    elem.setAttribute("srsName", "EPSG:4326");

    elem.addContent(makeExtent("XExtent", gcs.getXHorizAxis()));
    elem.addContent(makeExtent("YExtent", gcs.getYHorizAxis()));
    if (gcs.getVerticalAxis() != null) {
      elem.addContent(makeExtent("ZExtent", gcs.getVerticalAxis()));
    }
    return elem;
  }


  private Element makeExtent(String name, CoordinateAxis axis) {
    Element elem = new Element(name, wcsNS);
    addElement(elem, "min", Double.toString(axis.getMinValue()));
    addElement(elem, "max", Double.toString(axis.getMaxValue()));
    return elem;
  }


  private Element makeTemporalExtent(GridCoordSys gcs) {
    Element elem = new Element("TemporalExtent", wcsNS);

    java.util.ArrayList times = gcs.getTimes();
    for (int i = 0; i < times.size(); i++) {
      ucar.nc2.util.NamedObject no = (ucar.nc2.util.NamedObject) times.get(i);
      addElement(elem, "SingleValue", no.getName());
    }
    return elem;
  }


  private Element makeGridAxisDescription(GridCoordSys gcs) {
    Element elem = new Element("GridAxisDescription", wcsNS);
    elem.addContent(makeGridAxis("X", gcs, (CoordinateAxis1D) gcs.getXHorizAxis()));
    elem.addContent(makeGridAxis("Y", gcs, (CoordinateAxis1D) gcs.getYHorizAxis()));
    if (null != gcs.getVerticalAxis()) {
      elem.addContent(makeGridAxis("Z", gcs, gcs.getVerticalAxis()));
    }
    if (null != gcs.getTimeAxis()) {
      elem.addContent(makeGridAxis("T", gcs, gcs.getTimeAxis()));
    }
    return elem;
  }


  private Element makeGridAxis(String name, GridCoordSys gcs,
                               CoordinateAxis1D axis) {
    Element elem = new Element("GridAxis", wcsNS);
    addElement(elem, "Name", name);

    String desc = axis.getDescription();
    if (desc != null) {
      addElement(elem, "description", desc);

      // stupid orientation element
    }
    String orient = null;
    int n = (int) axis.getSize();
    if (name.equals("X")) {
      orient = "right"; // (axis.getCoordValue(0) > axis.getCoordValue(n-1)) ? "left" : "right";
    }
    if (name.equals("Y")) {
      orient = "up"; // (axis.getCoordValue(0) > axis.getCoordValue(n-1)) ? "down" : "up";
    }
    if (name.equals("Z")) {
      orient = gcs.isZPositive() ? "up" : "down"; // (axis.getCoordValue(0) > axis.getCoordValue(n-1)) ? "down" : "up";
        }
    if (name.equals("T")) {
      orient = "back";
    }
    if (orient != null) {
      addElement(elem, "orientation", orient);

    }
    return elem;
  }


  private Element makeGrid(GridCoordSys gcs) {
    Element elem = new Element("Grid", wcsNS);
    int ndim = 2;
    if (gcs.getVerticalAxis() != null) {
      ndim++;
    }
    if (gcs.getTimeAxis() != null) {
      ndim++;
    }
    elem.setAttribute("dimension", Integer.toString(ndim));
    elem.setAttribute("type", "centre");

    Element rangeElem = addElement(elem, "GridRange", null);

    Element lowElem = addElement(rangeElem, "low", null);
    Element hiElem = addElement(rangeElem, "high", null);

    lowElem.addContent(makeOrdinate(0));
    hiElem.addContent(makeOrdinate( (int) gcs.getXHorizAxis().getSize()));
    lowElem.addContent(makeOrdinate(0));
    hiElem.addContent(makeOrdinate( (int) gcs.getYHorizAxis().getSize()));
    if (gcs.getVerticalAxis() != null) {
      lowElem.addContent(makeOrdinate(0));
      hiElem.addContent(makeOrdinate( (int) gcs.getVerticalAxis().getSize()));
    }
    if (gcs.getTimeAxis() != null) {
      lowElem.addContent(makeOrdinate(0));
      hiElem.addContent(makeOrdinate( (int) gcs.getTimeAxis().getSize()));
    }

    return elem;
  }


  private Element makeRectifiedGrid(GridCoordSys gcs) {
    Element elem = new Element("RectifiedGrid", wcsNS);
    int ndim = 2;
    if (gcs.getVerticalAxis() != null) {
      ndim++;
    }
    if (gcs.getTimeAxis() != null && ndim == 3) {
      ndim++; // (x,y,t) shapes are not Possible!!
    }
    elem.setAttribute("dimension", Integer.toString(ndim));
    elem.setAttribute("type", "centre");
    elem.setAttribute("srsName", "EPSG:4326");

    Element rangeElem = addElement(elem, "GridRange", null);

    Element lowElem = addElement(rangeElem, "low", null);
    Element hiElem = addElement(rangeElem, "high", null);

    lowElem.addContent(makeOrdinate(0));
    hiElem.addContent(makeOrdinate( (int) gcs.getXHorizAxis().getSize()));
    lowElem.addContent(makeOrdinate(0));
    hiElem.addContent(makeOrdinate( (int) gcs.getYHorizAxis().getSize()));
    if (gcs.getVerticalAxis() != null) {
      lowElem.addContent(makeOrdinate(0));
      hiElem.addContent(makeOrdinate( (int) gcs.getVerticalAxis().getSize()));
    }
    if (gcs.getTimeAxis() != null) {
      lowElem.addContent(makeOrdinate(0));
      hiElem.addContent(makeOrdinate( (int) gcs.getTimeAxis().getSize()));
    }

    //origin elements
    Element origin = addElement(elem, "origin", null);
    addElement(origin, "X", "0");
    addElement(origin, "Y", "0");

    //origin elements
    Element offsets = addElement(elem, "offsets", null);
    Element offsetX = addElement(offsets, "offset", null);
    addElement(offsetX, "endX", "1");
    addElement(offsetX, "endY", "0");

    Element offsetY = addElement(offsets, "offset", null);
    addElement(offsetY, "endX", "0");
    addElement(offsetY, "endY", "1");

    return elem;
  }


  private Element makeOrdinate(int value) {
    Element elem = new Element("ordinate", wcsNS);
    elem.addContent(Integer.toString(value));
    return elem;
  }


  private Element makeGridRangeDescription(GeoGrid grid) {
    Element elem = new Element("GridRangeDescription", wcsNS);

    addElement(elem, "RangeID", grid.getName());
    addElement(elem, "title", grid.getDescription());
    Element obsElem = addElement(elem, "Observable", null);
    addElement(obsElem, "name", grid.getName());
    Element refElem = addElement(obsElem, "referenceSystem", null);
    Element uomElem = addElement(refElem, "UnitOfMeasure", null);
    addElement(uomElem, "name", grid.getUnitsString());

    return elem;
  }


  private Element makeSupportedInterpolationList() {
    Element elem = new Element("SupportedInterpolationList", wcsNS);
    elem.setAttribute("default", "none");
    addElement(elem, "InterpolationMethod", "none");
    return elem;
  }

  /* private Element makeElement(String name, String value) {
    Element elem = new Element(name, wcsNS);
    elem.addContent( value);
    return elem;
     }





  private void addInvDataset(Element layerlistElem, InvDataset invDataset) throws
      IOException {
    if (invDataset.hasAccess()) {
      InvAccess invAccess = invDataset.getAccess(ServiceType.NETCDF);
      if (invAccess == null) {
        invAccess = invDataset.getAccess(ServiceType.DODS);
      }
      if (invAccess != null) {
        GridDataset gridDataset = GridDataset.open(invAccess.getStandardUrlName());
        addGridCoverageLayers(layerlistElem, gridDataset, invDataset.getName());
      }
    }

    List list = invDataset.getDatasets();
    for (int i = 0; i < list.size(); i++) {
      InvDataset ds = (InvDataset) list.get(i);
      addInvDataset(layerlistElem, ds); // recurse
    }
  }


  private void addGridCoverageLayers(Element layerlistElem, GridDataset dataset,
                                     String title) {
    boolean rectified = true;


    Iterator iter = dataset.getGridSets().iterator();
    while (iter.hasNext()) {
      GridDataset.Gridset gridset = (GridDataset.Gridset) iter.next();

      //TODO: decide whether it a Rectified Grid or not
      if (rectified) {
        layerlistElem.addContent(makeRectifiedGridCoverageLayer(dataset, gridset, title));
      } //else
        // layerlistElem.addContent(makeGridCoverageLayer(dataset, gridset, title));
    }
  }
*/

}

/*
   29 Aug. 2003  Nativi
   added rectified grid methods.
   Changed addGridCoverageLayers to introduce RectifiedGrid option
*/

/* Change History:
   $Log: XMLwriter.java,v $
   Revision 1.7  2006/04/20 22:13:16  caron
   improve DL record extraction
   CatalogCrawler improvements

   Revision 1.6  2006/03/28 19:56:56  caron
   remove DateUnit static methods - not thread safe
   bugs in ForecasstModelRun interactions with external indexer

   Revision 1.5  2005/06/23 19:18:51  caron
   no message

   Revision 1.4  2005/03/23 22:34:44  caron
   wcs improvements

   Revision 1.3  2005/03/16 17:08:31  caron
   add WCS capabilities to GribTable
   fix WCS output

   Revision 1.2  2005/01/30 22:10:43  caron
   *** empty log message ***

   Revision 1.1  2005/01/21 00:59:24  caron
   *** empty log message ***

   Revision 1.3  2004/09/24 03:26:43  caron
   merge nj22

   Revision 1.2  2003/12/04 22:27:49  caron
   *** empty log message ***

   Revision 1.1.1.1  2003/09/29 13:35:36  villoresi
   My new CVS module.

   Revision 1.1  2003/07/11 18:30:51  caron
   add wcs package
 */
