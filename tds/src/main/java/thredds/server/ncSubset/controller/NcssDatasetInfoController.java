/*
 * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
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
package thredds.server.ncSubset.controller;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.jdom2.Document;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;

import thredds.server.ncSubset.dataservice.FeatureDatasetService;
import thredds.server.ncSubset.dataservice.NcssShowFeatureDatasetInfo;
import thredds.server.ncSubset.params.NcssParamsBean;
import thredds.server.ncSubset.util.NcssRequestUtils;
import thredds.util.ContentType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.point.writer.FeatureDatasetPointXML;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

@Controller
@Scope("request")
class NcssDatasetInfoController extends AbstractNcssController {
  static private final Logger log = LoggerFactory.getLogger(NcssDatasetInfoController.class);

  private boolean wantXML = false;
  private boolean showForm = false;
  private boolean showPointForm = false;
  private String requestPathInfo;

  @Autowired
  private NcssShowFeatureDatasetInfo ncssShowDatasetInfo;

  @Autowired
  FeatureDatasetService datasetService;

  @RequestMapping(value = {"/ncss/**/dataset.html", "/ncss/**/dataset.xml", "/ncss/**/pointDataset.html"}, params = {"!var"})
  void getDatasetDescription(HttpServletRequest req, HttpServletResponse res) throws IOException {

    if (!req.getParameterMap().isEmpty()) {
      //This is a 400
      throw new UnsupportedOperationException("Invalid info request.");
    }

    String datasetPath = getDatasetPath(req);
    requestPathInfo = extractRequestPathInfo(datasetPath);
    FeatureDataset fd = null;

    try {
      fd = datasetService.findDatasetByPath(req, res, requestPathInfo);

      if (fd == null)
        throw new UnsupportedOperationException("Feature Type not supported");

      String strResponse = ncssShowDatasetInfo.showForm(fd, buildDatasetUrl(datasetPath), wantXML, showPointForm);

      res.setContentLength(strResponse.length());

      if (wantXML)
        res.setContentType(ContentType.xml.toString());
      else
        res.setContentType(ContentType.html.toString());

      writeResponse(strResponse, res);

    } finally {
      if (fd != null) fd.close();
    }
  }


  @RequestMapping(value = {"/ncss/**/station.xml"})
  void getStations(HttpServletRequest req, HttpServletResponse res, @Valid NcssParamsBean params,
                   BindingResult validationResult) throws IOException {

    if (validationResult.hasErrors()) {
      handleValidationErrorsResponse(res, HttpServletResponse.SC_BAD_REQUEST, validationResult);

    } else {
      String datasetPath = getDatasetPath(req);
      requestPathInfo = extractRequestPathInfo(datasetPath);
      FeatureDataset fd = null;
      try {
        fd = datasetService.findDatasetByPath(req, res, requestPathInfo);

        if (fd == null)
          throw new UnsupportedOperationException("Feature Type not supported");

        if (fd.getFeatureType() != FeatureType.STATION)
          throw new UnsupportedOperationException("Station list request is only supported for Station features");

        FeatureDatasetPointXML xmlWriter = new FeatureDatasetPointXML((FeatureDatasetPoint) fd, buildDatasetUrl(datasetPath));

        String[] stnsList = new String[]{};
        if (params.getStns() != null)
          stnsList = params.getStns().toArray(stnsList);
        else
          stnsList = null;

        LatLonRect llrect = null;
        if (params.getNorth() != null && params.getSouth() != null && params.getEast() != null && params.getWest() != null)
          llrect = new LatLonRect(new LatLonPointImpl(params.getSouth(), params.getWest()), new LatLonPointImpl(params.getNorth(), params.getEast()));

        Document doc = xmlWriter.makeStationCollectionDocument(llrect, stnsList);
        XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
        String infoString = fmt.outputString(doc);

        res.setContentType(ContentType.xml.toString());
        writeResponse(infoString, res);

      } finally {
        if (fd != null) fd.close();
      }
    }
  }


  private String buildDatasetUrl(String path) {
    if (path.startsWith("/")) path = path.substring(1);
    return NcssRequestUtils.getTdsContext().getContextPath() + NcssController.getNCSSServletPath() + "/" + path;
  }

  String extractRequestPathInfo(String requestPathInfo) {

    // the forms and dataset description
    wantXML = requestPathInfo.endsWith("/dataset.xml");
    showForm = requestPathInfo.endsWith("/dataset.html");
    showPointForm = requestPathInfo.endsWith("/pointDataset.html");

    if (wantXML || showForm || showPointForm) {
      int len = requestPathInfo.length();
      if (wantXML)
        requestPathInfo = requestPathInfo.substring(0, len - 12);
      else if (showForm)
        requestPathInfo = requestPathInfo.substring(0, len - 13);
      else if (showPointForm)
        requestPathInfo = requestPathInfo.substring(0, len - 18);

      if (requestPathInfo.startsWith("/"))
        requestPathInfo = requestPathInfo.substring(1);
    }


    this.requestPathInfo = requestPathInfo;

    return requestPathInfo;
  }

  /**
   * Writes out the responseStr to the response object
   *
   * @param responseStr
   * @param response
   * @throws IOException
   */
  private void writeResponse(String responseStr, HttpServletResponse response) throws IOException {

    PrintWriter pw = response.getWriter();
    pw.write(responseStr);
    pw.flush();
    response.flushBuffer();
  }

}
