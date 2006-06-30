// $Id: DqcConvert3.java,v 1.11 2006/04/20 22:13:14 caron Exp $
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

import thredds.catalog.*;
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
 * @version $Id: DqcConvert3.java,v 1.11 2006/04/20 22:13:14 caron Exp $
 */

public class DqcConvert3 implements DqcConvertIF {
  public static boolean debugURL = false, debugXML = false, debugDBurl = false;
  public static boolean debugXMLopen = false, showParsedXML = false, showXMLoutput = false;

  protected static final Namespace defNS = Namespace.getNamespace(thredds.catalog.XMLEntityResolver.DQC_NAMESPACE_03);
  protected static final Namespace xlinkNS = Namespace.getNamespace("xlink", thredds.catalog.XMLEntityResolver.XLINK_NAMESPACE);

  //private DOMBuilder builder = new DOMBuilder();
  private DqcFactory factory;
  private URI docURI;

  /************************************************************************/
  // Create Java objects from XML

  /**
   * Create an QueryCapability from an XML document at a named URL.
   *
   * @param uri : the URI that the XML doc is at.
   * @return an QueryCapability object
   */
  public QueryCapability parseXML( DqcFactory fac, org.jdom.Document jdomDoc, URI uri) throws IOException {
    this.factory = fac;
    this.docURI = uri;

    // convert to JDOM document
    //Document doc = builder.build(domDoc);

    if (showParsedXML) {
      XMLOutputter xmlOut = new XMLOutputter();
      System.out.println ("*** DqcConvert3/showParsedXML = \n"+xmlOut.outputString(jdomDoc)+"\n*******");
    }

    QueryCapability qc = readQC( jdomDoc.getRootElement());

    if (showXMLoutput) {
      System.out.println ("*** DqcConvert3/showXMLoutput");
      writeXML(qc, System.out);
    }

    return qc;
  }

  /////////////////////////////////////////////////////////////////////////////

  private ListChoice readChoice( QueryCapability qc, Selector parent, Element elem) {
    String name = elem.getAttributeValue("name");
    String value = elem.getAttributeValue("value");

    ListChoice c = new ListChoice( parent, name, value, null);

    // look for subchoices
    java.util.List children = elem.getChildren();
    for (int j=0; j< children.size(); j++) {
      Element child = (Element) children.get(j);
      String childName = child.getName();
      if (childName.equals("selectList"))
        c.addNestedSelector( readSelectList( qc, child));
     }

    Element descElem = elem.getChild( "description", defNS);
    if (descElem != null)
      c.setDescription( readDocumentation( descElem));

     return c;
  }

  private Element writeChoice( ListChoice c) {
    Element elem = new Element("choice", defNS);
    elem.setAttribute("name", c.getName());
    elem.setAttribute("value", c.getValue());

    if (c.getDescription() != null)
      elem.addContent( writeDocumentation(c.getDescription(), "description") );

    List selectors = c.getNestedSelectors();
    for (int i=0; i<selectors.size(); i++) {
      Selector s = (Selector) selectors.get(i);
      if (s instanceof SelectList)
        elem.addContent( writeSelectList( (SelectList) s));
    }


    return elem;
  }

  // same as InvCatalogFactory10.readDocumentation
  protected InvDocumentation readDocumentation( Element s) {
    String href = s.getAttributeValue("href", xlinkNS);
    String title = s.getAttributeValue("title", xlinkNS);
    String type = s.getAttributeValue("type"); // not XLink type
    String content = s.getTextNormalize();

    // LOOK im not so sure this should be resolved!
    URI uriResolved = null;
    if (href != null) {
      try {
        uriResolved = docURI.resolve( href);
      } catch (Exception e) {
        factory.appendErr(" ** Invalid documentation href = "+href+" "+e.getMessage()+"\n");
      }
    }

    return new InvDocumentation( href, uriResolved, title, type, content);
  }

  private Element writeDocumentation( InvDocumentation doc, String name) {
    Element docElem = new Element(name, defNS);
    if (doc.getType() != null)
      docElem.setAttribute("type", doc.getType());

    if (doc.hasXlink()) {
      docElem.setAttribute("href", doc.getURI().toString(), xlinkNS);
      if (!doc.getXlinkTitle().equals( doc.getURI().toString()))
        docElem.setAttribute("title", doc.getXlinkTitle(), xlinkNS);
    }

    String inline = doc.getInlineContent();
    if (inline != null)
      docElem.addContent(inline);
    return docElem;
  }

  private Location readLocation( Element locationElem) {
    if (locationElem == null)
      return null;
    String latitude = locationElem.getAttributeValue("latitude");
    String longitude = locationElem.getAttributeValue("longitude");
    String elevation = locationElem.getAttributeValue("elevation");
    String latitude_units = locationElem.getAttributeValue("latitude_units");
    String longitude_units = locationElem.getAttributeValue("longitude_units");
    String elevation_units = locationElem.getAttributeValue("elevation_units");

    Location location = new Location( latitude, longitude, elevation, latitude_units,
       longitude_units, elevation_units);

    return location;
  }

  private Element writeLocation( Location l, String elemName) {
    Element locationElem = new Element(l.hasElevation() ? "location3D" : elemName, defNS);
    locationElem.setAttribute("latitude", Double.toString(l.getLatitude()));
    locationElem.setAttribute("longitude", Double.toString(l.getLongitude()));
    if (!l.isDefaultLatitudeUnits())
      locationElem.setAttribute("latitude_units", l.getLatitudeUnits());
    if (!l.isDefaultLongitudeUnits())
      locationElem.setAttribute("longitude_units", l.getLongitudeUnits());
    if (l.hasElevation()) {
      locationElem.setAttribute("elevation", Double.toString(l.getElevation()));
      if (!l.isDefaultElevationUnits())
        locationElem.setAttribute("elevation_units", l.getElevationUnits());
    }
    return locationElem;
  }

  private QueryCapability readQC( Element qcElem) {
    java.util.List list;
    Selector s;

     // read attributes
    String name = qcElem.getAttributeValue("name");
    String version = qcElem.getAttributeValue("version");
    QueryCapability qc = new QueryCapability( docURI.toString(), name, version);

    // read elements
    qc.setQuery( readQuery( qcElem.getChild("query", defNS)));

    if (null != (s = readSelectService( qcElem.getChild("selectService", defNS)))) {
       qc.addUniqueSelector( s);
       qc.setServiceSelector( s);
    }

    // look for the various selectors
    list = qcElem.getChildren("selectList", defNS);
    for (int j=0; j< list.size(); j++) {
      if (null != (s = readSelectList( qc, (Element) list.get(j)))) {
        qc.addUniqueSelector(s);
      }
   }

    list = qcElem.getChildren("selectStation", defNS);
    for (int j=0; j< list.size(); j++) {
      if (null != (s = readSelectStation( (Element) list.get(j))))
        qc.addUniqueSelector( s);
   }

    list = qcElem.getChildren("selectFromRange", defNS);
    for (int j=0; j< list.size(); j++) {
      if (null != (s = readSelectRange( (Element) list.get(j))))
        qc.addUniqueSelector( s);
   }

    list = qcElem.getChildren("selectFromDateRange", defNS);
    for (int j=0; j< list.size(); j++) {
      if (null != (s = readSelectRangeDate( (Element) list.get(j))))
        qc.addUniqueSelector( s);
   }

    list = qcElem.getChildren("selectFromGeoRegion", defNS);
    for (int j=0; j< list.size(); j++) {
      if (null != (s = readSelectGeoRegion( (Element) list.get(j))))
        qc.addUniqueSelector( s);
   }

    // look for user interface elements : just pass the JDOM element along
    list = qcElem.getChildren("userInterface", defNS);
    for (int j=0; j< list.size(); j++) {
      Element child = (Element) list.get(j);
      qc.addUserInterface( child);
    }

    return qc;
  }

  public Document writeQC(QueryCapability qc) {
    Element rootElem = new Element("queryCapability", defNS);
    Document doc = new Document(rootElem);

    // attributes
    if (null != qc.getName())
      rootElem.setAttribute("name", qc.getName());
    rootElem.setAttribute("version", qc.getVersion());

    // content
    rootElem.addContent( writeQuery( qc.getQuery()));

    // select service must be first
    Selector s = qc.getServiceSelector();
    Element elem =  writeSelectService( (SelectService) s);
    if (elem != null)
      rootElem.addContent( elem);

    List selectors = qc.getSelectors();
    for (int i=0; i<selectors.size(); i++) {
      s = (Selector) selectors.get(i);
      if (s instanceof SelectList)
        rootElem.addContent( writeSelectList( (SelectList) s));
      else if (s instanceof SelectRange)
        rootElem.addContent( writeSelectRange( (SelectRange) s));
      else if (s instanceof SelectRangeDate)
        rootElem.addContent( writeSelectRangeDate( (SelectRangeDate) s));
      else if (s instanceof SelectGeoRegion)
        rootElem.addContent( writeSelectGeoRegion( (SelectGeoRegion) s));
      else if (s instanceof SelectService)
        continue; // already did
      else if (s instanceof SelectStation)
        rootElem.addContent( writeSelectStation( (SelectStation) s));
    }

    return doc;
  }

  private Query readQuery( Element s) {
    String base = s.getAttributeValue("base");

    URI uriResolved = null;
    if (base != null) {
      try {
        uriResolved = docURI.resolve( base);
      } catch (Exception e) {
        factory.appendFatalErr(" ** Invalid query base = "+base+" "+e.getMessage()+"\n");
      }
    }

    return new Query( base, uriResolved, null);
  }

  private Element writeQuery( Query q) {
    Element elem = new Element("query", defNS);
    elem.setAttribute("base", q.getBase());
    return elem;
  }

  private void readSelector( Element elem, Selector s) {
    s.setTitle( elem.getAttributeValue("title"));
    s.setId( elem.getAttributeValue("id"));
    s.setTemplate( elem.getAttributeValue("template"));
    s.setRequired( elem.getAttributeValue("required"));
    s.setMultiple( elem.getAttributeValue("multiple"));

    Element descElem = elem.getChild( "description", defNS);
    if (descElem != null)
      s.setDescription( readDocumentation( descElem));
  }

  private void writeSelector( Element elem, Selector s) {
    if (s.getId() != null)
      elem.setAttribute( "id", s.getId());
    if (s.getTitle() != null)
      elem.setAttribute( "title", s.getTitle());
    if (s.getTemplate() != null)
      elem.setAttribute( "template",  s.getTemplate());
    if (!s.isRequired())
      elem.setAttribute( "required", "false");
    if (s.isMultiple())
      elem.setAttribute( "multiple", "true");

    if (s.getDescription() != null)
      elem.addContent( writeDocumentation(s.getDescription(), "description") );
  }

  private SelectList readSelectList( QueryCapability qc, Element elem) {
    SelectList slist = new SelectList();
    readSelector( elem, slist);

    // look for choices
    java.util.List choices = elem.getChildren("choice", defNS);
    for (int j=0; j< choices.size(); j++) {
       ListChoice choice = readChoice( qc, slist, (Element) choices.get(j));
       slist.addChoice( choice);
     }
     return slist;
  }

  private Element writeSelectList( SelectList s) {
    Element elem = new Element("selectList", defNS);
    writeSelector( elem, s);

    List choices = s.getChoices();
    for (int i=0; i<choices.size(); i++)
      elem.addContent( writeChoice( (ListChoice) choices.get(i)));
    return elem;
  }

  private SelectRange readSelectRange( Element elem) {
    String min = elem.getAttributeValue("min");
    String max = elem.getAttributeValue("max");
    String units = elem.getAttributeValue("units");
    String modulo = elem.getAttributeValue("modulo");
    String resolution = elem.getAttributeValue("resolution");
    String selectType = elem.getAttributeValue("selectType");

    SelectRange sr = new SelectRange( min, max, units, modulo, resolution, selectType);
    readSelector( elem, sr);

    return sr;
 }

  private Element writeSelectRange( SelectRange s) {
    Element elem = new Element("selectFromRange", defNS);
    writeSelector( elem, s);

    if (s.getMin() != null)
      elem.setAttribute( "min", s.getMin());
    if (s.getMax() != null)
      elem.setAttribute( "max", s.getMax());
    if (s.getUnits() != null)
      elem.setAttribute( "units", s.getUnits());
    if (s.isModulo())
      elem.setAttribute( "modulo", "true");
    if (s.getResolution() != null)
      elem.setAttribute( "resolution", s.getResolution());
    if (s.getSelectType() != null)
      elem.setAttribute( "selectType", s.getSelectType());

    return elem;
  }

  private SelectRangeDate readSelectRangeDate( Element elem) {
    String start = elem.getAttributeValue("start");
    String end = elem.getAttributeValue("end");
    String duration = elem.getAttributeValue("duration");
    String resolution = elem.getAttributeValue("resolution");
    String selectType = elem.getAttributeValue("selectType");

    SelectRangeDate srd = new SelectRangeDate( start, end, duration, resolution, selectType);
    readSelector( elem, srd);

    return srd;
 }

  private Element writeSelectRangeDate( SelectRangeDate s) {
    Element elem = new Element("selectFromDateRange", defNS);
    writeSelector( elem, s);

    if (s.getStart() != null)
      elem.setAttribute( "start", s.getStart());
    if (s.getEnd() != null)
      elem.setAttribute( "end", s.getEnd());
    if (s.getDuration() != null)
      elem.setAttribute( "duration", s.getDuration());
    if (s.getResolution() != null)
      elem.setAttribute( "resolution", s.getResolution());
    if (s.getSelectType() != null)
      elem.setAttribute( "selectType", s.getSelectType());

    return elem;
  }

  private SelectGeoRegion readSelectGeoRegion( Element elem) {
    Element geoBB = elem.getChild("geoBoundingBox", defNS);
    if (geoBB == null) {
      factory.appendErr("No geoBoundingBox in selectFromGeoRegion");
      return null;
    }
    Location lowerLeft = readLocation( geoBB.getChild("lowerLeft", defNS));
    Location upperRight = readLocation( geoBB.getChild("upperRight", defNS));

    SelectGeoRegion sr = new SelectGeoRegion( lowerLeft, upperRight);
    readSelector( elem, sr);

    return sr;
 }

  private Element writeSelectGeoRegion( SelectGeoRegion s) {
    Element elem = new Element("selectFromGeoRegion", defNS);
    writeSelector( elem, s);

    Element geoBB = new Element("geoBoundingBox", defNS);
    elem.addContent(geoBB);

    if (s.getLowerLeft() != null)
      geoBB.addContent( writeLocation( s.getLowerLeft(), "lowerLeft"));
    if (s.getUpperRight() != null)
      geoBB.addContent( writeLocation( s.getUpperRight(), "upperRight"));

    return elem;
  }

  private SelectService readSelectService( Element elem) {
    if (elem == null) return null;

    SelectService ss = new SelectService( null, null);
    readSelector( elem, ss);

    // look for service
    java.util.List choices = elem.getChildren("serviceType", defNS);
    for (int j=0; j< choices.size(); j++) {
      Element choice = (Element) choices.get(j);
      String name = choice.getText();
      String title = choice.getAttributeValue("title");
      String dataFormatType = choice.getAttributeValue("dataFormatType");
      ss.addServiceChoice( name, title, dataFormatType);
    }

    return ss;
  }

  private Element writeSelectService( SelectService ss) {
    if (ss == null) return null;
    Element elem = new Element("selectService", defNS);
    writeSelector( elem, ss);

    List choices = ss.getChoices();
    for (int i=0; i<choices.size(); i++) {
      SelectService.ServiceChoice s = (SelectService.ServiceChoice) choices.get(i);
      Element selem = new Element( "serviceType", defNS);
      if (s.getTitle() != null) selem.setAttribute( "title", s.getTitle());
      if (s.getDataFormat() != null) selem.setAttribute( "dataFormatType", s.getDataFormat());
      selem.addContent(s.getService());
      elem.addContent( selem);
    }
    return elem;
  }


  private SelectStation readSelectStation( Element elem) {
    SelectStation ss = new SelectStation();
    readSelector( elem, ss);

    // look for stations
    java.util.List stations = elem.getChildren("station", defNS);
    for (int j=0; j< stations.size(); j++) {
      Station station = readStation( ss, (Element) stations.get(j));
      ss.addStation( station);
     }
     return ss;
  }

  private Element writeSelectStation( SelectStation s) {
    Element elem = new Element("selectStation", defNS);
    writeSelector( elem, s);

    List stations = s.getStations();
    for (int i=0; i<stations.size(); i++)
      elem.addContent( writeStation( (Station) stations.get(i)));
    return elem;
  }

  private Station readStation( Selector parent, Element elem) {
    String name = elem.getAttributeValue("name");
    String value = elem.getAttributeValue("value");

    Station station = new Station( parent, name, value, null);
    Element locationElem = elem.getChild("location", defNS);
    if (null == locationElem)
      locationElem = elem.getChild("location3D", defNS);
    station.setLocation( readLocation( locationElem));

    Element descElem = elem.getChild( "description", defNS);
    if (descElem != null)
      station.setDescription( readDocumentation( descElem));

    return station;
  }

  private Element writeStation( Station c) {
    Element elem = new Element("station", defNS);
    elem.setAttribute("name", c.getName());
    elem.setAttribute("value", c.getValue());

    if (c.getDescription() != null)
      elem.addContent( writeDocumentation(c.getDescription(), "description") );

    elem.addContent( writeLocation( c.getLocation(), "location"));
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
    fmt.output(writeQC(qc), os);
  }

}


/* Change History:
   $Log: DqcConvert3.java,v $
   Revision 1.11  2006/04/20 22:13:14  caron
   improve DL record extraction
   CatalogCrawler improvements

   Revision 1.10  2006/01/17 01:46:51  caron
   use jdom instead of dom everywhere

   Revision 1.9  2005/04/20 00:05:37  caron
   *** empty log message ***

   Revision 1.8  2004/09/24 03:26:28  caron
   merge nj22

   Revision 1.7  2004/06/19 00:45:42  caron
   redo nested select list

   Revision 1.6  2004/06/18 21:54:26  caron
   update dqc 0.3

   Revision 1.5  2004/06/12 04:12:42  caron
   *** empty log message ***

   Revision 1.4  2004/06/12 02:01:09  caron
   dqc 0.3

   Revision 1.3  2004/06/09 00:27:26  caron
   version 2.0a release; cleanup javadoc

   Revision 1.2  2004/05/21 05:57:31  caron
   release 2.0b

   Revision 1.1  2004/05/11 23:30:29  caron
   release 2.0a

 */