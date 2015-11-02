/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.server.ncss.controller;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import thredds.server.ncss.dataservice.FeatureDatasetService;
import thredds.server.ncss.dataservice.NcssShowFeatureDatasetInfo;
import thredds.server.ncss.params.NcssParamsBean;
import thredds.server.ncss.util.NcssRequestUtils;
import thredds.util.ContentType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.point.writer.FeatureDatasetPointXML;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

@Controller
@Scope("request")
class NcssDatasetInfoController extends AbstractNcssController {

  @Autowired
  private NcssShowFeatureDatasetInfo ncssShowDatasetInfo;

  @Autowired
  FeatureDatasetService datasetService;

  /* @RequestMapping("/ncss/grid/**")
  public String forwardGrid(HttpServletRequest req) {
    String reqString = req.getServletPath();
    assert reqString.startsWith("/ncss/grid");
    reqString = reqString.substring(10);
    String forwardString = "forward:/ncss" + reqString;  // strip off '?/grid
    if (null != req.getQueryString())
      forwardString += "?"+req.getQueryString();

     return forwardString;
  } */

  @RequestMapping(
          value = {"/ncss/**/dataset.html", "/ncss/**/dataset.xml",
                  "/ncss/**/pointDataset.html", "/ncss/**/pointDataset.xml"},
          params = {"!var"})
  void getDatasetDescription(HttpServletRequest req, HttpServletResponse res) throws IOException, TransformerException, JDOMException {
    if (!req.getParameterMap().isEmpty())
      throw new IllegalArgumentException("Invalid info request.");

    // the forms and dataset description
    String path = req.getServletPath();
    boolean wantXML = path.endsWith("/dataset.xml") || path.endsWith("/pointDataset.xml");
    boolean showPointForm = path.endsWith("/pointDataset.html");
    String datasetPath = getDatasetPath(path);

    try (FeatureDataset fd = datasetService.findDatasetByPath(req, res, datasetPath)) {
      if (fd == null)
        return; // restricted dataset

      String strResponse = ncssShowDatasetInfo.showForm(fd, buildDatasetUrl(datasetPath), wantXML, showPointForm);

      if (wantXML)
        res.setContentType(ContentType.xml.getContentHeader());
      else
        res.setContentType(ContentType.html.getContentHeader());

      thredds.servlet.ServletUtil.setResponseContentLength(res, strResponse);
      writeResponse(strResponse, res);
    }
  }

  @RequestMapping(value = {"/ncss/**/station.xml"})
  void getStations(HttpServletRequest req, HttpServletResponse res, NcssParamsBean params) throws IOException {

    String path = req.getServletPath();
    String datasetPath = getDatasetPath(path);
    try (FeatureDataset fd = datasetService.findDatasetByPath(req, res, datasetPath)) {

      if (fd == null)
        throw new FileNotFoundException("Could not find Dataset "+datasetPath);

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

      res.setContentType(ContentType.xml.getContentHeader());
      writeResponse(infoString, res);
    }
  }

  private String buildDatasetUrl(String path) {
    if (path.startsWith("/")) path = path.substring(1);
    return NcssRequestUtils.getTdsContext().getContextPath() + NcssController.getNCSSServletPath() + path;
  }

  /* void extractRequestPathInfo(String requestPathInfo) {

    // the forms and dataset description
    wantXML = requestPathInfo.endsWith("/dataset.xml") || requestPathInfo.endsWith("/pointDataset.xml");
    showForm = requestPathInfo.endsWith("/dataset.html");
    showPointForm = requestPathInfo.endsWith("/pointDataset.html");

    this.datasetPath =  getDatasetPath(requestPathInfo);
  }  */

  /**
   * Writes out the responseStr to the response object
   */
  private void writeResponse(String responseStr, HttpServletResponse response) throws IOException {
    PrintWriter pw = response.getWriter();
    pw.write(responseStr);
    pw.flush();
    response.flushBuffer();
  }

}
