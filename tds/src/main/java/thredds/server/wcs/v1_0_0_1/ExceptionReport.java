/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.wcs.v1_0_0_1;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;

/**
 * Generates a WCS 1.1.0 Exception Report.
 *
 * @author edavis
 * @since 4.0
 */
public class ExceptionReport {
  protected static final Namespace ogcNS = Namespace.getNamespace("http://www.opengis.net/ogc");

  private Document exceptionReport;

  public ExceptionReport(WcsException exception) {
    this(Collections.singletonList(exception));
  }

  public ExceptionReport(List<WcsException> exceptions) {
    Element rootElem = new Element("ServiceExceptionReport", ogcNS);
    rootElem.addNamespaceDeclaration(ogcNS);
    rootElem.setAttribute("version", "1.2.0");

    if (exceptions != null)
      for (WcsException curException : exceptions) {
        Element exceptionElem = new Element("ServiceException", ogcNS);
        if (curException.getCode() != null && !curException.getCode().equals(WcsException.Code.UNKNOWN))
          exceptionElem.setAttribute("code", curException.getCode().toString());
        if (curException.getLocator() != null && !curException.getLocator().equals(""))
          exceptionElem.setAttribute("locator", curException.getLocator());

        if (curException.getTextMessages() != null) {
          for (String curMessage : curException.getTextMessages()) {
            // ToDo - somehow seperate multiple text messages.
            exceptionElem.addContent(curMessage);
          }
        }
        rootElem.addContent(exceptionElem);
      }

    exceptionReport = new Document(rootElem);
  }

  public Document getExceptionReport() {
    return exceptionReport;
  }

  public void writeExceptionReport(PrintWriter pw)
          throws IOException {
    XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
    xmlOutputter.output(exceptionReport, pw);
  }
}
