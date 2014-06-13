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

package thredds.server.ncss.dataservice;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.transform.JDOMResult;
import org.jdom2.transform.JDOMSource;
import org.jdom2.xpath.XPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.support.ServletContextResource;
import thredds.server.config.FormatsAvailabilityService;
import thredds.server.ncss.format.SupportedFormat;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.grid.GridDatasetInfo;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.point.writer.FeatureDatasetPointXML;

import javax.servlet.ServletContext;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;

@Service
public class NcssShowDatasetInfoImpl implements NcssShowFeatureDatasetInfo, ServletContextAware {

  static private final Logger log = LoggerFactory.getLogger(NcssShowDatasetInfoImpl.class);

  private ServletContext servletContext;

  public String showForm(FeatureDataset fd, String datasetUrlPath, boolean wantXml, boolean isPoint) {

    FeatureType ft = fd.getFeatureType();
    String strResponse = "";

    switch (ft) {
      case GRID:
        strResponse = showGridForm((GridDataset) fd, datasetUrlPath, wantXml, isPoint);
        break;

      case STATION:
        strResponse = showPointForm((FeatureDatasetPoint) fd, datasetUrlPath, wantXml, "/WEB-INF/xsl/ncssSobs.xsl");
        break;

      case POINT:
        strResponse = showPointForm((FeatureDatasetPoint) fd, datasetUrlPath, wantXml, "/WEB-INF/xsl/ncssPobs.xsl");
        break;

      default:
        // Feature not supported
        break;
    }

    return strResponse;
  }

  private String showPointForm(FeatureDatasetPoint fp, String datasetUrlPath, boolean wantXml, String xslt) {

    FeatureDatasetPointXML xmlWriter = new FeatureDatasetPointXML(fp, datasetUrlPath);
    String infoString = "";
    Document doc = xmlWriter.getCapabilitiesDocument();

    if (FormatsAvailabilityService.isFormatAvailable(SupportedFormat.NETCDF4)) {
      String xPathForGridElement = "capabilities/AcceptList";
      addElement(doc, xPathForGridElement, new Element("accept").addContent("netcdf4").setAttribute("displayName", "CF/NetCDF-4"));
      addElement(doc, xPathForGridElement, new Element("accept").addContent("netcdf4ext").setAttribute("displayName", "NetCDF-4 Extended"));
    }

    if (wantXml) {
      XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
      infoString = fmt.outputString(doc);

    } else {

      try {

        InputStream is = getXSLT(xslt);
        //XSLTransformer transformer = new XSLTransformer(is);

        Transformer transformer = TransformerFactory.newInstance().newTransformer(new StreamSource(is));
        String context = servletContext.getContextPath();
        transformer.setParameter("tdsContext", context);

        JDOMSource in = new JDOMSource(doc);
        JDOMResult out = new JDOMResult();
        transformer.transform(in, out);
        Document html = out.getDocument();

        XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
        infoString = fmt.outputString(html);

      } catch (IOException ioe) {
        log.error("IO error opening xsl", ioe);
      } catch (TransformerConfigurationException e) {
        log.error("ForecastModelRunServlet internal error", e);
      } catch (TransformerException e) {
        log.error("ForecastModelRunServlet internal error", e);
      }
    }

    return infoString;
  }

  private String showGridForm(GridDataset gds, String datsetUrlPath, boolean wantXml, boolean isPoint) {

    boolean formatAvailable = FormatsAvailabilityService.isFormatAvailable(SupportedFormat.NETCDF4);

    String infoString = null;
    GridDatasetInfo writer = new GridDatasetInfo(gds, "path");

    Document datsetDescription;

    if (wantXml) {

      datsetDescription = writer.makeDatasetDescription();

      if (formatAvailable) {
        addNetcdf4Format(datsetDescription, "/gridDataset");
      }
      infoString = writer.writeXML(datsetDescription);

    } else {

      try {

        InputStream xslt = getXSLT(isPoint ? "/WEB-INF/xsl/ncssGridAsPoint.xsl" : "/WEB-INF/xsl/ncssGrid.xsl");
        Document doc = writer.makeGridForm();

        if (formatAvailable) {
          addNetcdf4Format(doc, "/gridForm");
        }

        Element root = doc.getRootElement();
        root.setAttribute("location", datsetUrlPath);
        Transformer xslTransformer = TransformerFactory.newInstance().newTransformer(new StreamSource(xslt));
        String context = servletContext.getContextPath();
        xslTransformer.setParameter("tdsContext", context);

        xslTransformer.setParameter("gridWKT", writer.getDatasetBoundariesWKT());

        JDOMSource in = new JDOMSource(doc);
        JDOMResult out = new JDOMResult();
        xslTransformer.transform(in, out);
        Document html = out.getDocument();

        XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
        infoString = fmt.outputString(html);
        // infoString = fmt.outputString(doc);

      } catch (IOException ioe) {
        log.error("IO error opening xsl", ioe);
      } catch (Throwable e) {
        log.error("ForecastModelRunServlet internal error", e);

        // if (!res.isCommitted())
        // res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
        // "ForecastModelRunServlet internal error");
        // return;
      }
    }

    return infoString;

  }

  private InputStream getXSLT(String xslName) throws IOException {
    ServletContextResource r = new ServletContextResource(servletContext, xslName);
    return r.getInputStream();
  }

  @Override
  public void setServletContext(ServletContext servletContext) {
    this.servletContext = servletContext;
  }

  private void addNetcdf4Format(Document datasetDescriptionDoc, String rootElementName) {

    String xPathForGridElement = rootElementName + "/AcceptList/Grid";
    addElement(datasetDescriptionDoc, xPathForGridElement, new Element("accept").addContent("netcdf4").setAttribute("displayName", "netcdf4"));

    String xPathForGridAsPointElement = rootElementName + "/AcceptList/GridAsPoint";
    addElement(datasetDescriptionDoc, xPathForGridAsPointElement, new Element("accept").addContent("netcdf4").setAttribute("displayName", "netcdf4"));
  }

  private void addElement(Document datasetDescriptionDoc, String xPath, Element element) {

    try {
      XPath gridXpath = XPath.newInstance(xPath);
      Element acceptListParent = (Element) gridXpath
              .selectSingleNode(datasetDescriptionDoc);
      acceptListParent.addContent(element);

    } catch (JDOMException e) {
      // Log the error...

    }
  }

}
