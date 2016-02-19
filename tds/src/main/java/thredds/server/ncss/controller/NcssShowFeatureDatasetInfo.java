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
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.point.writer.FeatureDatasetCapabilitiesWriter;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.ft2.coverage.writer.CoverageBoundariesExtractor;
import ucar.nc2.ft2.coverage.writer.CoverageDatasetCapabilities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NcssShowFeatureDatasetInfo {

  @Autowired
  TdsContext tdsContext;

  public ModelAndView showForm(FeatureDataset fd, String datasetUrlPath, boolean wantXml, boolean isPoint)
          throws IOException {

    FeatureType ft = fd.getFeatureType();
    switch (ft) {
      case STATION:
        return showPointForm((FeatureDatasetPoint) fd, SupportedOperation.STATION_REQUEST, datasetUrlPath, wantXml, "ncss/ncssSobs.xsl");

      case POINT:
        return showPointForm((FeatureDatasetPoint) fd, SupportedOperation.POINT_REQUEST, datasetUrlPath, wantXml, "ncss/ncssPobs.xsl");

      default:
        throw new IllegalStateException("Unsupported feature type " + ft);
    }
  }

  private ModelAndView showPointForm(FeatureDatasetPoint fp, SupportedOperation ops, String datasetUrlPath, boolean wantXml, String xslt)
          throws IOException {

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

  // the NCSS grid from using thymeleaf template, called from NcssGridController
  public ModelAndView showGridFormTh(CoverageCollection gcd, String datasetUrlPath, boolean wantXml) throws IOException {
    if (wantXml) {
      CoverageDatasetCapabilities writer = new CoverageDatasetCapabilities(gcd, "path");
      Document doc = writer.makeDatasetDescription();
      Element root = doc.getRootElement();
      root.setAttribute("location", datasetUrlPath);
      root.addContent( makeAcceptXML(SupportedOperation.GRID_REQUEST));
      return new ModelAndView("threddsXmlView", "Document", doc);

    } else {
      Map<String, Object> model = new HashMap<>();
      model.put("gcd", gcd);
      model.put("datasetPath", datasetUrlPath);
      model.put("gridWKT", CoverageBoundariesExtractor.getDatasetBoundariesWKT(gcd));
      model.put("accept", makeAcceptList(SupportedOperation.GRID_REQUEST));
      return new ModelAndView("templates/ncssGridth", model);
    }
  }

  public ModelAndView showGridForm(CoverageCollection gcd, String datasetUrlPath, boolean wantXml, boolean isPoint) throws IOException {
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
      Element accept = new Element("accept").addContent(sf.getFormatName()).setAttribute("displayName", sf.getFormatName());
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
