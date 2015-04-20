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
package ucar.unidata.geoloc.ogc;

import org.jdom2.output.XMLOutputter;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;

import java.io.PrintWriter;
import java.io.IOException;

/**
 * Helper class to write a CRS in GML given a ucar.nc2.CoordinateSystem.
 *
 * @author edavis
 * @since 4.0
 */
public class CoordRefSysToGML {
  private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CoordRefSysToGML.class);

  protected static final Namespace gmlNS = Namespace.getNamespace("http://www.opengis.net/gml");
  protected static final Namespace xlinkNS = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");


  public static void writeCoordRefSysAsGML(PrintWriter pw, ucar.nc2.dataset.CoordinateSystem coordSys)
          throws IOException {
    XMLOutputter xmlOutputter = new XMLOutputter(org.jdom2.output.Format.getPrettyFormat());
    xmlOutputter.output(genCoordRefSysAsGML(coordSys), pw);
  }

  public static Document genCoordRefSysAsGML(ucar.nc2.dataset.CoordinateSystem coordSys) {
    if (coordSys == null)
      throw new IllegalArgumentException("CoordinateSystem must be non-null.");
    if (!coordSys.isGeoReferencing())
      throw new IllegalArgumentException("CoordinateSystem must be a georeferencing CS.");

    /*Element xyCrsElem;
    if ( coordSys.isGeoXY())
    {
      xyCrsElem = genProjectedCRS( coordSys.getProjection());
    } */
    /* if (!coordSys.isGeoXY()) {
      coordSys.getLatAxis();
      coordSys.getLonAxis();
    } */
    Element rootElem = new Element("CompoundCRS", gmlNS);

    rootElem.addContent("");

    rootElem.addNamespaceDeclaration(gmlNS);
    rootElem.addNamespaceDeclaration(xlinkNS);
    // rootElem.setAttribute( "version", this.getVersion() );

    return new Document(rootElem);

  }

  public static Element genProjectedCRS(ucar.unidata.geoloc.Projection proj) {
    Element projElem = new Element("ProjectedCRS", gmlNS);
    projElem.setAttribute("id", "", gmlNS);
    projElem.addContent(
            new Element("identifier", gmlNS)
                    .setAttribute("codeSpace", "", gmlNS)
                    .addContent(""));
    projElem.addContent(new Element("name", gmlNS).addContent(proj.getName()));
    projElem.addContent(new Element("scope", gmlNS).addContent(""));
    projElem.addContent(new Element("conversion", gmlNS).addContent(""));

    return projElem;
  }
}
