/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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
