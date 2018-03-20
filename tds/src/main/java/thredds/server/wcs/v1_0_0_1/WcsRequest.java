/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.wcs.v1_0_0_1;

import ucar.nc2.ft2.coverage.CoverageCoordAxis;
import ucar.nc2.ft2.coverage.CoverageCoordSys;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPoint;
import org.jdom2.Element;
import org.jdom2.Namespace;
import thredds.server.wcs.Request;

import javax.annotation.Nonnull;

/**
 * Represent the incoming WCS 1.0.0 request.
 *
 * @author edavis
 * @since 4.0
 */
public abstract class WcsRequest {
  protected static final Namespace wcsNS = Namespace.getNamespace("http://www.opengis.net/wcs");
  protected static final Namespace gmlNS = Namespace.getNamespace("gml", "http://www.opengis.net/gml");
  protected static final Namespace xlinkNS = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");

  // General request info
  private Request.Operation operation;
  private String version;

  // Dataset
  protected WcsDataset wcsDataset;

  WcsRequest(@Nonnull Request.Operation operation, String version, @Nonnull WcsDataset dataset) {
    this.operation = operation;
    this.version = version;
    this.wcsDataset = dataset;
  }

  public Request.Operation getOperation() {
    return operation;
  }

  public String getVersion() {
    return version;
  }

  public WcsDataset getWcsDataset() {
    return wcsDataset;
  }

  protected Element genCoverageOfferingBriefElem(String elemName,
                                                 String covName, String covLabel, String covDescription,
                                                 CoverageCoordSys gridCoordSys) {

    // <CoverageOfferingBrief>
    Element briefElem = new Element(elemName, wcsNS);

    // <CoverageOfferingBrief>/gml:metaDataProperty [0..*]
    // <CoverageOfferingBrief>/gml:description [0..1]
    // <CoverageOfferingBrief>/gml:name [0..*]
    // <CoverageOfferingBrief>/metadataLink [0..*]

    // <CoverageOfferingBrief>/description [0..1]
    // <CoverageOfferingBrief>/name [1]
    // <CoverageOfferingBrief>/label [1]
    if (covDescription != null && !covDescription.equals(""))
      briefElem.addContent(new Element("description", wcsNS).addContent(covDescription));
    briefElem.addContent(new Element("name", wcsNS).addContent(covName));
    briefElem.addContent(new Element("label", wcsNS).addContent(covLabel));

    // <CoverageOfferingBrief>/lonLatEnvelope [1]
    briefElem.addContent(genLonLatEnvelope(wcsDataset.getDataset(), gridCoordSys));

    // ToDo Add keywords capabilities.
    // <CoverageOfferingBrief>/keywords [0..*]  /keywords [1..*] and /type [0..1]

    return briefElem;
  }

  protected Element genLonLatEnvelope(CoverageCollection gcd, CoverageCoordSys gcs) {
    // <CoverageOfferingBrief>/lonLatEnvelope
    Element lonLatEnvelopeElem = new Element("lonLatEnvelope", wcsNS);
    lonLatEnvelopeElem.setAttribute("srsName", "urn:ogc:def:crs:OGC:1.3:CRS84");

    LatLonRect llbb = gcd.getLatlonBoundingBox();
    LatLonPoint llpt = llbb.getLowerLeftPoint();
    LatLonPoint urpt = llbb.getUpperRightPoint();

    // <CoverageOfferingBrief>/lonLatEnvelope/gml:pos
    String firstPosition = llpt.getLongitude() + " " + llpt.getLatitude();
    double lon = llpt.getLongitude() + llbb.getWidth();
    String secondPosition = lon + " " + urpt.getLatitude();
// ToDo WCS 1.0Plus - Add vertical (Deal with conversion to meters. Yikes!!)
//    CoordinateAxis1D vertAxis = gcs.getVerticalAxis();
//    if ( vertAxis != null )
//    {
//      // See verAxis.getUnitsString()
//      firstPosition += " " + vertAxis.getCoordValue( 0);
//      secondPostion += " " + vertAxis.getCoordValue( ((int)vertAxis.getSize()) - 1);
//    }

    lonLatEnvelopeElem.addContent(new Element("pos", gmlNS).addContent(firstPosition));
    lonLatEnvelopeElem.addContent(new Element("pos", gmlNS).addContent(secondPosition));

    // <CoverageOfferingBrief>/lonLatEnvelope/gml:timePostion [2]

    CoverageCoordAxis timeCoord = gcs.getTimeAxis();
    if (timeCoord != null) {
      CalendarDateRange dr = timeCoord.getDateRange();
      if (dr != null) {
        lonLatEnvelopeElem.addContent(new Element("timePosition", gmlNS).addContent(dr.getStart().toString()));
        lonLatEnvelopeElem.addContent(new Element("timePosition", gmlNS).addContent(dr.getEnd().toString()));
      }
    }

    return lonLatEnvelopeElem;
  }

}
