/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.ft.remote;

import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPSession;
import ucar.nc2.ft.*;
import ucar.nc2.ft.point.remote.PointDatasetRemote;
import ucar.nc2.ft.point.writer.FeatureDatasetCapabilitiesWriter;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.ft2.coverage.FeatureDatasetCoverage;
import ucar.nc2.ft2.coverage.remote.CdmrfReader;
import ucar.nc2.stream.CdmRemote;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.util.Optional;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.jdom2.input.SAXBuilder;

/**
 * Factory for FeatureDataset using cdmrFeature protocol.
 * This object represents the client, connecting to a remote dataset.
 * This handles both coverages (now in ucar.nc2.ft2) and point (ucar.nc2.ft) feature types.
 *
 * @author caron
 * @since May 19, 2009
 */
public class CdmrFeatureDataset {
  static public final String PROTOCOL = "cdmrFeature";
  static public final String SCHEME = PROTOCOL + ":";

  static private boolean debug = false;
  static private boolean showXML = false;

  // all CdmrFeatureDatasets must return their featureType - use as a fail-fast test of the endpoint
  public static FeatureType isCdmrfEndpoint(String endpoint) throws IOException {

    HTTPSession httpClient = HTTPFactory.newSession(endpoint);
    String url = endpoint + "?req=featureType";

    // get the header
    try (HTTPMethod method = HTTPFactory.Get(httpClient, url)) {
      method.setFollowRedirects(true);
      int statusCode = method.execute();
      if (statusCode != 200) return null;
      String content = method.getResponseAsString();
      return FeatureType.getType(content);

    } catch (Throwable t) {
      t.printStackTrace();
      return null;
    }
  }

  static public Optional<FeatureDataset> factory(FeatureType wantFeatureType, String endpoint) throws IOException {
    if (endpoint.startsWith(SCHEME))
      endpoint = endpoint.substring(SCHEME.length());

    FeatureType featureType;
    try {
      featureType = isCdmrfEndpoint(endpoint);
      if (featureType == null) return Optional.empty("Not a valid CdmrFeatureDataset endpoint="+endpoint);

    } catch (IOException ioe) {
      return Optional.empty(String.format("Error opening CdmrFeatureDataset endpoint=%s err=%s", endpoint, ioe.getMessage()));
    }

    if (!FeatureDatasetFactoryManager.featureTypeOk(wantFeatureType, featureType))
      return Optional.empty(String.format("Not a compatible featureType=%s, want=%s, endpoint=%s", featureType, wantFeatureType, endpoint));

    if (featureType.isCoverageFeatureType()) {
      CdmrfReader reader = new CdmrfReader(endpoint);
      CoverageCollection covColl = reader.open();
      return Optional.of( new FeatureDatasetCoverage(endpoint, covColl, covColl));
    }

    if (featureType.isPointFeatureType()) {
      Document doc = getCapabilities(endpoint);
      Element root = doc.getRootElement();
      Element elem = root.getChild("featureDataset");
      String fType = elem.getAttribute("type").getValue();  // LOOK, may be multiple types

      endpoint = elem.getAttribute("url").getValue();
      wantFeatureType = FeatureType.getType(fType);
      if (debug) System.out.printf("CdmrFeatureDataset endpoint %s%n ftype= '%s' url=%s%n", endpoint, fType, endpoint);

      List<VariableSimpleIF> dataVars = FeatureDatasetCapabilitiesWriter.getDataVariables(doc);
      LatLonRect bb = FeatureDatasetCapabilitiesWriter.getSpatialExtent(doc);
      CalendarDateRange dr = FeatureDatasetCapabilitiesWriter.getTimeSpan(doc);
      CalendarDateUnit timeUnit = FeatureDatasetCapabilitiesWriter.getTimeUnit(doc);
      String altUnits = FeatureDatasetCapabilitiesWriter.getAltUnits(doc);

      return Optional.of(new PointDatasetRemote(wantFeatureType, endpoint, timeUnit, altUnits, dataVars, bb, dr));
    }

    return Optional.empty(String.format("Unimplemented featureType=%s, want=%s, endpoint=%s", featureType, wantFeatureType, endpoint));
  }

  static private org.jdom2.Document getCapabilities(String endpoint) throws IOException {
    org.jdom2.Document doc;
    InputStream in = null;
    try {
      in = CdmRemote.sendQuery(null, endpoint, "req=capabilities");
      SAXBuilder builder = new SAXBuilder();
      doc = builder.build(in);  // LOOK closes in when done ??

    } catch (Throwable t) {
      throw new IOException(t);

    } finally {
      //if (in != null)
      //  in.close();
    }

    if (showXML) {
      System.out.printf("*** endpoint = %s %n", endpoint);
      XMLOutputter xmlOut = new XMLOutputter();
      System.out.printf("*** CdmrFeatureDataset/showParsedXML = %n %s %n", xmlOut.outputString(doc));
    }

    return doc;
  }


}
