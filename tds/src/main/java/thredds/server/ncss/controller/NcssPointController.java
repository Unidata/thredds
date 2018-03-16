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
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import thredds.core.StandardService;
import thredds.core.TdsRequestedDataset;
import thredds.server.ncss.exception.NcssException;
import thredds.server.ncss.format.SupportedFormat;
import thredds.server.ncss.format.SupportedOperation;
import thredds.server.ncss.params.NcssPointParamsBean;
import thredds.server.ncss.view.dsg.DsgSubsetWriter;
import thredds.server.ncss.view.dsg.DsgSubsetWriterFactory;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.DsgFeatureCollection;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.point.writer.FeatureDatasetCapabilitiesWriter;
import ucar.nc2.ft2.coverage.SubsetParams;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles all Ncss Point Requests
 *
 * @author caron
 * @since 4/29/2015
 */
@Controller
@RequestMapping("/ncss/point")
public class NcssPointController extends AbstractNcssController {
  protected String getBase() {
    return StandardService.netcdfSubsetPoint.getBase();
  }

  @RequestMapping("**")
  public void handleRequest(HttpServletRequest req, HttpServletResponse res, @Valid NcssPointParamsBean params,
                            BindingResult validationResult) throws Exception {
    if (validationResult.hasErrors())
      throw new BindException(validationResult);

    String datasetPath = getDatasetPath(req);
    try (FeatureDatasetPoint fdp = TdsRequestedDataset.getPointDataset(req, res, datasetPath)) {
      if (fdp == null) return;

      Formatter errs = new Formatter();
      if (!params.intersectsTime(fdp.getCalendarDateRange(), errs)) {
        handleValidationErrorMessage(res, HttpServletResponse.SC_BAD_REQUEST, errs.toString());
        return;
      }

      FeatureType ft = fdp.getFeatureType();
      if (ft != FeatureType.POINT && ft != FeatureType.STATION) {
        throw new NcssException("Dataset Feature Type is " + ft.toString() + " but request is for Points or Stations");
      }

      SubsetParams ncssParams = params.makeSubset();
      SupportedFormat format = getSupportedOperation(fdp).getSupportedFormat(params.getAccept());

      DsgSubsetWriter pds = DsgSubsetWriterFactory.newInstance(
              fdp, ncssParams, ncssDiskCache, res.getOutputStream(), format);
      setResponseHeaders(res, pds.getHttpHeaders(datasetPath, format.isStream() ));
      pds.respond(res, fdp, datasetPath, ncssParams, format);
    }
  }

  @RequestMapping(value = { "**/dataset.xml", "**/pointDataset.xml" })
  public ModelAndView getDatasetDescriptionXml(HttpServletRequest req, HttpServletResponse res) throws IOException {
    String datasetPath = getDatasetPath(req);

    try (FeatureDatasetPoint fdp = TdsRequestedDataset.getPointDataset(req, res, datasetPath)) {
      if (fdp == null) return null; // restricted dataset

      String datasetUrlPath = buildDatasetUrl(datasetPath);
      SupportedOperation supportedOperation = getSupportedOperation(fdp);

      FeatureDatasetCapabilitiesWriter xmlWriter = new FeatureDatasetCapabilitiesWriter(fdp, datasetUrlPath);
      Document doc = xmlWriter.getCapabilitiesDocument();
      Element root = doc.getRootElement();
      root.setAttribute("location", datasetUrlPath);
      root.addContent(makeAcceptXML(supportedOperation)); // must add the accept elements

      return new ModelAndView("threddsXmlView", "Document", doc);
    }
  }

  @RequestMapping(value = { "**/dataset.html", "**/pointDataset.html" })
  public ModelAndView getDatasetDescriptionHtml(HttpServletRequest req, HttpServletResponse res) throws IOException {
    String datasetPath = getDatasetPath(req);

    try (FeatureDatasetPoint fdp = TdsRequestedDataset.getPointDataset(req, res, datasetPath)) {
      if (fdp == null) return null; // restricted dataset

      String datasetUrlPath = buildDatasetUrl(datasetPath);
      SupportedOperation supportedOperation = getSupportedOperation(fdp);

      Map<String, Object> model = new HashMap<>();
      model.put("fdp", fdp);
      model.put("datasetPath", datasetUrlPath);
      model.put("accept", makeAcceptList(supportedOperation));

      List<DsgFeatureCollection> dsgFeatCols = fdp.getPointFeatureCollectionList();
      if (dsgFeatCols.size() != 1) {
        throw new AssertionError(String.format(
                "Expected dataset to contain exactly 1 DsgFeatureCollection, but instead it has %d. We " +
                        "(the THREDDS developers) weren't certain that such datasets actually existed in the wild! " +
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

  @RequestMapping(value = { "**/station.xml" })
  public ModelAndView getStations(HttpServletRequest req, HttpServletResponse res, NcssPointParamsBean params)
          throws IOException {
    String datasetPath = getDatasetPath(req);
    try (FeatureDatasetPoint fd = TdsRequestedDataset.getPointDataset(req, res, datasetPath)) {
      if (fd == null) return null;

      if (fd.getFeatureType() != FeatureType.STATION)
        throw new java.lang.UnsupportedOperationException(
                "Station list request is only supported for Station features");

      FeatureDatasetCapabilitiesWriter xmlWriter = new FeatureDatasetCapabilitiesWriter(
              fd, buildDatasetUrl(datasetPath));

      String[] stnsList = new String[]{};
      if (params.getStns() != null)
        stnsList = params.getStns().toArray(stnsList);
      else
        stnsList = null;

      LatLonRect llrect = null;
      if (params.getNorth() != null && params.getSouth() != null &&
              params.getEast() != null && params.getWest() != null)
        llrect = new LatLonRect(new LatLonPointImpl(params.getSouth(), params.getWest()),
                new LatLonPointImpl(params.getNorth(), params.getEast()));

      Document doc = xmlWriter.makeStationCollectionDocument(llrect, stnsList);
      return new ModelAndView("threddsXmlView", "Document", doc);
    }
  }

  public static SupportedOperation getSupportedOperation(FeatureDataset fd) {
    switch (fd.getFeatureType()) {
      case POINT:   return SupportedOperation.POINT_REQUEST;
      case STATION: return SupportedOperation.STATION_REQUEST;
      default:      throw new UnsupportedOperationException(String.format(
              "'%s' format not currently supported for DSG subset writing.", fd.getFeatureType()));
    }
  }
}
