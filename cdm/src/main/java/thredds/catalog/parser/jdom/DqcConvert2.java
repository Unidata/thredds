// $Id: DqcConvert2.java,v 1.8 2006/01/17 01:46:51 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package thredds.catalog.parser.jdom;

import thredds.catalog.query.*;

import org.jdom.*;
import org.jdom.output.*;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * DQC Converter of DOM to object model.
 * Reads DQC.xml files, constructs object representation.
 *
 * @author John Caron
 * @version $Id: DqcConvert2.java,v 1.8 2006/01/17 01:46:51 caron Exp $
 */

public class DqcConvert2 implements DqcConvertIF {
  public static boolean debugURL = false, debugXML = false, debugDBurl = false;
  public static boolean debugXMLopen = false, showParsedXML = false, showXMLoutput = false;

  protected static final Namespace defNS = Namespace.getNamespace(thredds.catalog.XMLEntityResolver.DQC_NAMESPACE_02);
  protected static final Namespace xlinkNS = Namespace.getNamespace("xlink", thredds.catalog.XMLEntityResolver.XLINK_NAMESPACE);

  // private DOMBuilder builder = new DOMBuilder();

  /************************************************************************/
  // Create Java objects from XML

  /**
   * Create an QueryCapability from an XML document at a named URL.
   *
   * @param uri : the URI that the XML doc is at.
   * @return an QueryCapability object
   */
  public QueryCapability parseXML( DqcFactory fac, org.jdom.Document jdomDoc, URI uri) throws IOException {

    // convert to JDOM document
    //Document doc = builder.build(domDoc);

    if (showParsedXML) {
      XMLOutputter xmlOut = new XMLOutputter();
      System.out.println ("*** queryFactory/showParsedXML = \n"+xmlOut.outputString(jdomDoc)+"\n*******");
    }

    QueryCapability qc = readQC( jdomDoc.getRootElement(), uri);

    if (showXMLoutput) {
      System.out.println ("*** queryFactory/showXMLoutput");
      writeXML(qc, System.out);
    }

    return qc;
  }

  /////////////////////////////////////////////////////////////////////////////

  private QueryCapability readQC( Element qcElem, URI uri) {

     // read attributes
    String name = qcElem.getAttributeValue("name");
    String version = qcElem.getAttributeValue("version");
    QueryCapability qc = new QueryCapability( uri.toString(), name, version);

    // read elements
    qc.setQuery( readQuery( qcElem.getChild("query", defNS), uri));

        // look for selectors
    Element selections = qcElem.getChild("selections", defNS);
    java.util.List list = selections.getChildren();
    for (int j=0; j< list.size(); j++) {
      Element child = (Element) list.get(j);
      String childName = child.getName();
      if (childName.equals("selectList"))
        qc.addSelector( readSelectList( qc, child));
      if (childName.equals("selectStation"))
        qc.addSelector( readSelectStation( qc, child));
    }

    return qc;
  }

  public Document makeQC(QueryCapability qc) {
    Element rootElem = new Element("queryCapability", defNS);
    Document doc = new Document(rootElem);

    // attributes
    if (null != qc.getName())
      rootElem.setAttribute("name", qc.getName());
    rootElem.setAttribute("version", qc.getVersion());

    // content
    rootElem.addContent( makeQuery( qc.getQuery()));

    Element elem = new Element("selections", defNS);
    List selectors = qc.getSelectors();
    for (int i=0; i<selectors.size(); i++) {
      Selector s = (Selector) selectors.get(i);
      if (s instanceof SelectList)
        elem.addContent( makeSelectList( (SelectList) s));
      else if (s instanceof SelectStation)
        elem.addContent( makeSelectStation( (SelectStation) s));
    }
    rootElem.addContent( elem);

    return doc;
  }

  private Query readQuery( Element s, URI base) {
    String action = s.getAttributeValue("action");
    String construct = s.getAttributeValue("construct");
    String returns = s.getAttributeValue("returns");

    URI uri = base.resolve( action);
    return new Query( action, uri, construct);
  }

  private Element makeQuery( Query q) {
    Element elem = new Element("query", defNS);
    elem.setAttribute("action", q.getBase());
    elem.setAttribute("construct", q.getConstruct());
    elem.setAttribute("returns", q.getReturns());
    return elem;
  }

  private SelectList readSelectList( QueryCapability qc, Element s) {
    String label = s.getAttributeValue("label");
    String id = s.getAttributeValue("id");
    String template = s.getAttributeValue("template");
    String selectType = s.getAttributeValue("selectType");
    String required = s.getAttributeValue("required");

    // Select Type is gone from data model; needed to fix it for multiple: erd 2004/07/30
    String multiple = "false";
    if ( selectType.equals( "multiple") ) multiple = "true";
    //if ( selectType.equals( "single") ) multiple = "false";
    SelectList slist = new SelectList( label, id, template, required, multiple);
    qc.addUniqueSelector( slist);

    // look for choices
    java.util.List choices = s.getChildren("choice", defNS);
    for (int j=0; j< choices.size(); j++) {
      ListChoice choice = readChoice( qc, slist, (Element) choices.get(j));
      slist.addChoice( choice);
     }

     return slist;
  }

  private Element makeSelectList( SelectList s) {
    Element elem = new Element("selectList", defNS);
    elem.setAttribute("label", s.getTitle());
    if (null != s.getId())
      elem.setAttribute("id", s.getId());
    if (null != s.getTemplate())
      elem.setAttribute("template", s.getTemplate());
    elem.setAttribute("selectType", s.getSelectType());
    elem.setAttribute("required", Boolean.toString(s.isRequired()));

    List choices = s.getChoices();
    for (int i=0; i<choices.size(); i++)
      elem.addContent( makeChoice( (ListChoice) choices.get(i)));
    return elem;
  }

  private SelectStation readSelectStation( QueryCapability qc, Element s) {
    String label = s.getAttributeValue("label");
    String id = s.getAttributeValue("id");
    String template = s.getAttributeValue("template");
    String selectType = s.getAttributeValue("selectType");
    String required = s.getAttributeValue("required");

    // Select Type is gone from data model; needed to fix it for multiple: erd 2004/07/30
    String multiple = "false";
    if ( selectType.equals( "multiple" ) ) multiple = "true";
    //if ( selectType.equals( "single") ) multiple = "false";
    SelectStation ss = new SelectStation( label, id, template, required, multiple);
    qc.addUniqueSelector( ss);

    // look for stations
    java.util.List stations = s.getChildren("station", defNS);
    for (int j=0; j< stations.size(); j++) {
      Station station = readStation( ss, (Element) stations.get(j));
      ss.addStation( station);
     }
     return ss;
  }

  private Element makeSelectStation( SelectStation s) {
    Element elem = new Element("selectStation", defNS);
    elem.setAttribute("label", s.getTitle());
    if (null != s.getId())
      elem.setAttribute("id", s.getId());
    if (null != s.getTemplate())
      elem.setAttribute("template", s.getTemplate());
    elem.setAttribute("selectType", s.getSelectType());
    elem.setAttribute("required", Boolean.toString(s.isRequired()));

    List stations = s.getStations();
    for (int i=0; i<stations.size(); i++)
      elem.addContent( makeStation( (Station) stations.get(i)));
    return elem;
  }

  private ListChoice readChoice( QueryCapability qc, Selector parent, Element elem) {
    String name = elem.getAttributeValue("name");
    String value = elem.getAttributeValue("value");
    String description = elem.getAttributeValue("description");

    ListChoice c = new ListChoice( parent, name, value, description);

    // look for subchoices
    java.util.List children = elem.getChildren();
    for (int j=0; j< children.size(); j++) {
      Element child = (Element) children.get(j);
      String childName = child.getName();
      if (childName.equals("selectList"))
        c.addNestedSelector( readSelectList( qc, child));
     }
     return c;
  }

  private Element makeChoice( ListChoice c) {
    Element elem = new Element("choice", defNS);
    elem.setAttribute("name", c.getName());
    elem.setAttribute("value", c.getValue());

    List selectors = c.getNestedSelectors();
    for (int i=0; i<selectors.size(); i++) {
      Selector s = (Selector) selectors.get(i);
      if (s instanceof SelectList)
        elem.addContent( makeSelectList( (SelectList) s));
    }
    return elem;
  }

  private Station readStation( Selector parent, Element elem) {
    String name = elem.getAttributeValue("name");
    String value = elem.getAttributeValue("value");
    String description = elem.getAttributeValue("description");

    Station station = new Station( parent, name, value, description);

    // get location
    Element locationElem = elem.getChild("location", defNS);
    if (null == locationElem)
      locationElem = elem.getChild("location3D", defNS);
    String latitude = locationElem.getAttributeValue("latitude");
    String longitude = locationElem.getAttributeValue("longitude");
    String elevation = locationElem.getAttributeValue("elevation");
    String latitude_units = locationElem.getAttributeValue("latitude_units");
    String longitude_units = locationElem.getAttributeValue("longitude_units");
    String elevation_units = locationElem.getAttributeValue("elevation_units");

    Location location = new Location( latitude, longitude, elevation, latitude_units,
       longitude_units, elevation_units);

    station.setLocation( location);
    return station;
  }

  private Element makeStation( Station c) {
    Element elem = new Element("station", defNS);
    elem.setAttribute("name", c.getName());
    elem.setAttribute("value", c.getValue());
   // if (null != c.getDescription())
     // elem.setAttribute("description", c.getDescription());

    Location l = c.getLocation();
    Element locationElem = new Element(l.hasElevation() ? "location3D" : "location", defNS);
    locationElem.setAttribute("latitude", Double.toString(l.getLatitude()));
    locationElem.setAttribute("longitude", Double.toString(l.getLongitude()));
    if (null != l.getLatitudeUnits())
      locationElem.setAttribute("latitude_units", l.getLatitudeUnits());
    if (null != l.getLongitudeUnits())
      locationElem.setAttribute("longitude_units", l.getLongitudeUnits());
    if (l.hasElevation()) {
      locationElem.setAttribute("elevation", Double.toString(l.getElevation()));
    if (null != l.getElevationUnits())
      locationElem.setAttribute("elevation_units", l.getElevationUnits());
    }

    elem.addContent( locationElem);
    return elem;
  }


  /************************************************************************/
  // Writing XML from objects

  /**
   * Write the catalog as an XML document to the specified stream.
   *
   * @param qc write this QueryCapability
   * @param os write to this OutputStream
   * @throws IOException
   */
  public void writeXML(QueryCapability qc, OutputStream os) throws IOException {
    // Output the document, use standard formatter
    XMLOutputter fmt = new XMLOutputter();
    fmt.output(makeQC(qc), os);
  }

}


/* Change History:
   $Log: DqcConvert2.java,v $
   Revision 1.8  2006/01/17 01:46:51  caron
   use jdom instead of dom everywhere

   Revision 1.7  2005/04/20 00:05:36  caron
   *** empty log message ***

   Revision 1.6  2004/09/24 03:26:28  caron
   merge nj22

   Revision 1.5  2004/08/23 16:45:19  edavis
   Update DqcServlet to work with DQC spec v0.3 and InvCatalog v1.0. Folded DqcServlet into the THREDDS server framework/build/distribution. Updated documentation (DqcServlet and THREDDS server).

   Revision 1.4  2004/06/19 00:45:42  caron
   redo nested select list

   Revision 1.3  2004/06/18 21:54:26  caron
   update dqc 0.3

   Revision 1.2  2004/06/12 02:01:09  caron
   dqc 0.3

   Revision 1.1  2004/05/11 23:30:28  caron
   release 2.0a

 */