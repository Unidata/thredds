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

package thredds.server.ncss.controller;

import org.jdom2.Document;
import org.jdom2.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.ModelAndView;
import thredds.server.config.TdsContext;
import thredds.server.ncss.format.SupportedFormat;
import thredds.server.ncss.format.SupportedOperation;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.DsgFeatureCollection;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.point.writer.FeatureDatasetCapabilitiesWriter;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.ft2.coverage.writer.CoverageDatasetCapabilities;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NcssShowFeatureDatasetInfo {

  @Autowired
  TdsContext tdsContext;

  public ModelAndView showForm(FeatureDataset fd, String datasetUrlPath, boolean wantXml, boolean isPoint) {
    FeatureType ft = fd.getFeatureType();
    switch (ft) {
      case STATION:
        return showPointForm((FeatureDatasetPoint) fd, SupportedOperation.STATION_REQUEST, datasetUrlPath, wantXml,
                "ncss/ncssSobs.xsl");

      case POINT:
        return showPointForm((FeatureDatasetPoint) fd, SupportedOperation.POINT_REQUEST, datasetUrlPath, wantXml,
                "ncss/ncssPobs.xsl");

      default:
        throw new IllegalStateException("Unsupported feature type " + ft);
    }
  }

  private ModelAndView showPointForm(FeatureDatasetPoint fp, SupportedOperation ops, String datasetUrlPath,
          boolean wantXml, String xslt) {

    FeatureDatasetCapabilitiesWriter xmlWriter = new FeatureDatasetCapabilitiesWriter(fp, datasetUrlPath);
    Document doc = xmlWriter.getCapabilitiesDocument();
    Element root = doc.getRootElement();
    root.setAttribute("location", datasetUrlPath);
    root.addContent(makeAcceptXML(ops)); // must add the accept elements

    if (wantXml) {
      return new ModelAndView("threddsXmlView", "Document", doc);

    } else {
      Map<String, Object> model = new HashMap<>();
      model.put("Document", doc);
      model.put("Transform", xslt);
      return new ModelAndView("threddsXsltView", model); // use xslt to transform into web page
    }
  }

  public ModelAndView showPointFormTh(FeatureDatasetPoint fdp, String datasetUrlPath, String servletPath) {
    // Throws an exception if fdp's FeatureType isn't POINT or STATION.
    SupportedOperation supportedOperation = NcssPointController.getSupportedOperation(fdp);

    if (servletPath.endsWith(".xml")) {
      FeatureDatasetCapabilitiesWriter xmlWriter = new FeatureDatasetCapabilitiesWriter(fdp, datasetUrlPath);
      Document doc = xmlWriter.getCapabilitiesDocument();
      Element root = doc.getRootElement();
      root.setAttribute("location", datasetUrlPath);
      root.addContent(makeAcceptXML(supportedOperation)); // must add the accept elements

      return new ModelAndView("threddsXmlView", "Document", doc);
    } else {
      Map<String, Object> model = new HashMap<>();
      model.put("fdp", fdp);
      model.put("datasetPath", datasetUrlPath);
      model.put("accept", makeAcceptList(supportedOperation));


      List<DsgFeatureCollection> dsgFeatCols = fdp.getPointFeatureCollectionList();
      if (dsgFeatCols.size() != 1) {
        throw new AssertionError(String.format(
                "Expected dataset to contain exactly 1 DsgFeatureCollection, but instead it has %d. " +
                        "We (the THREDDS developers) weren't certain that such datasets actually existed in the wild! " +
                        "Please tell us about it at support-thredds@unidata.ucar.edu.", dsgFeatCols.size()));
        // John Caron's comment on the topic from 2015-09-23 was:
        //     It would be nice if FeatureDatasetPoint could only return a single DsgFeatureCollection, but I'm
        //     skeptical, even though it's the common case. A Dataset is a file, and a file could contain multiple
        //     collection types. We do see that occasionally.
        // However, we don't have any such datasets in our test suite. Furthermore , I don't believe that it's possible
        // to construct one in a CF-compliant manner. So, if there really are multi-DSG datasets out there, hopefully
        // this message will prompt users to send them to us.
      }

      DsgFeatureCollection dsgFeatCol = dsgFeatCols.get(0);

      LatLonRect boundingBox = dsgFeatCol.getBoundingBox();
      if (boundingBox == null) {
        boundingBox = new LatLonRect(new LatLonPointImpl(-90, -180), new LatLonPointImpl(90, 180));  // Whole earth.
      }
      model.put("boundingBox", boundingBox);

      String horizExtentWKT = String.format("POLYGON((%f %f, %f %f, %f %f, %f %f, %f %f))",
              boundingBox.getLonMin(), boundingBox.getLatMax(),
              boundingBox.getLonMax(), boundingBox.getLatMax(),
              boundingBox.getLonMax(), boundingBox.getLatMin(),
              boundingBox.getLonMin(), boundingBox.getLatMin(),
              boundingBox.getLonMin(), boundingBox.getLatMax());
      model.put("horizExtentWKT", horizExtentWKT);

      CalendarDateRange calendarDateRange = dsgFeatCol.getCalendarDateRange();
      if (calendarDateRange == null) {
        CalendarDate start = CalendarDate.of(null, 0000, 1, 1, 0, 0, 0);  // 0000-01-01T00:00:00.00Z
        CalendarDate end   = CalendarDate.present();
        calendarDateRange  = CalendarDateRange.of(start, end);
      }
      model.put("calendarDateRange", calendarDateRange);

      if (supportedOperation == SupportedOperation.POINT_REQUEST) {
        return new ModelAndView("templates/ncssPoint", model);
      } else {
        return new ModelAndView("templates/ncssStation", model);
      }
    }
  }

  // the NCSS grid form using thymeleaf template, called from NcssGridController
  public ModelAndView showGridFormTh(CoverageCollection gcd, String datasetUrlPath, String servletPath)
          throws IOException {
    if (servletPath.endsWith(".xml")) {
      CoverageDatasetCapabilities writer = new CoverageDatasetCapabilities(gcd, "path");
      Document doc = writer.makeDatasetDescription();
      Element root = doc.getRootElement();
      root.setAttribute("location", datasetUrlPath);
      root.addContent(makeAcceptXML(SupportedOperation.GRID_REQUEST));

      return new ModelAndView("threddsXmlView", "Document", doc);
    } else {
      Map<String, Object> model = new HashMap<>();
      model.put("gcd", gcd);
      model.put("datasetPath", datasetUrlPath);
      model.put("horizExtentWKT", gcd.getHorizCoordSys().getLatLonBoundaryAsWKT(50, 100));

      if (servletPath.endsWith("dataset.html")) {
        model.put("accept", makeAcceptList(SupportedOperation.GRID_REQUEST));
        return new ModelAndView("templates/ncssGrid", model);
      } else {
        model.put("accept", makeAcceptList(SupportedOperation.GRID_AS_POINT_REQUEST));
        return new ModelAndView("templates/ncssGridAsPoint", model);
      }
    }
  }

  public ModelAndView showGridForm(CoverageCollection gcd, String datasetUrlPath, boolean wantXml, boolean isPoint)
          throws IOException {
    CoverageDatasetCapabilities writer = new CoverageDatasetCapabilities(gcd, "path");

    Document doc = writer.makeDatasetDescription();
    Element root = doc.getRootElement();
    root.setAttribute("location", datasetUrlPath);
    root.addContent(makeAcceptXML(SupportedOperation.GRID_REQUEST));

    if (wantXml) {
      return new ModelAndView("threddsXmlView", "Document", doc);

    } else {
      String xslt = isPoint ? "ncss/ncssGridAsPoint.xsl" : "ncss/ncssGrid.xsl";   // see XsltForHtmlView
      Map<String, Object> model = new HashMap<>();
      model.put("Document", doc);
      model.put("Transform", xslt);
      return new ModelAndView("threddsXsltView", model); // use xslt to transform into web page
    }
  }

  private Element makeAcceptXML(SupportedOperation ops) {
    Element acceptList = new Element("AcceptList");
    for (SupportedFormat sf : ops.getSupportedFormats()) {
      Element accept =
              new Element("accept").addContent(sf.getFormatName()).setAttribute("displayName", sf.getFormatName());
      acceptList.addContent(accept);
    }

    return acceptList;
  }

  private List<String> makeAcceptList(SupportedOperation ops) {
    List<String> result = new ArrayList<>();
    for (SupportedFormat sf : ops.getSupportedFormats()) {
      result.add(sf.getFormatName());
    }

    return result;
  }
}
