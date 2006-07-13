// $Id: InvCatalogFactory6.java 48 2006-07-12 16:15:40Z caron $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

import org.jdom.*;
import org.jdom.input.*;
import org.jdom.output.*;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Inventory Catalog Factory, version 6.
 * Reads InvCatalog.xml files, constructs object representation.
 *
 * @author John Caron
 * @version $Id: InvCatalogFactory6.java 48 2006-07-12 16:15:40Z caron $
 */

public class InvCatalogFactory6 implements InvCatalogConvertIF, MetadataConverterIF {

  private static final String dtdDefault = "http://www.unidata.ucar.edu/projects/THREDDS/xml/InvCatalog.0.6.dtd";
  private static final Namespace defNS = Namespace.getNamespace(XMLEntityResolver.CATALOG_NAMESPACE_06);
  private static final Namespace xlinkNS = Namespace.getNamespace("xlink", XMLEntityResolver.XLINK_NAMESPACE);

  /************************************************************************/
  // Create Java objects from XML

  private InvCatalogFactory factory;
  //private DOMBuilder builder = new DOMBuilder();
  //private DOMOutputter domOut = null;

  /* public InvCatalogImpl parseXML( InvCatalogFactory fac, org.w3c.dom.Document domDoc, URI uri) {
     this.factory = fac;

     // convert to JDOM document
     Document doc = builder.build(domDoc);

     if (InvCatalogFactory.showParsedXML) {
       XMLOutputter xmlOut = new XMLOutputter();
       System.out.println ("*** catalog/showParsedXML = \n"+xmlOut.outputString(doc)+"\n*******");
     }

     //String dtdID = doc.getDocType().getSystemID();
     //if (!dtdID.equals( dtdDefault) && !dtdID.equals(dtdAgg) && !dtdID.equals(dtdGen))
       //return new InvCatalogImpl("** InvCatalogFactory6 catalog DTD is <"+dtdID +">\n  must be <"+ dtdDefault+">\n");

     InvCatalogImpl catalog = readCatalog( doc.getRootElement(), uri);
     //catalog.setCreateFrom( uri.toString());
     //catalog.setDTDid( dtdID);

     return catalog;
   } */

  public InvCatalogImpl parseXML( InvCatalogFactory fac, org.jdom.Document jdomDoc, URI uri) {
     this.factory = fac;
     return readCatalog( jdomDoc.getRootElement(), uri);
   }


  private HashMap metadataHash = new HashMap(10);
  public void registerMetadataConverter(MetadataType type, MetadataConverterIF converter) {
    metadataHash.put(type, converter);
  }

  /////////////////////////////////////////////////////////////////////////////

  protected InvAccessImpl readAccess( InvDatasetImpl dataset, Element accessElem) {
    String urlPath = accessElem.getAttributeValue("urlPath");
    String serviceName = accessElem.getAttributeValue("serviceName");
    String serviceType = accessElem.getAttributeValue("serviceType");
    InvAccessImpl access = new InvAccessImpl( dataset, urlPath, serviceName, serviceType, null, 0.0);

    return access;
  }

  protected InvCatalogImpl readCatalog( Element catalogElem, URI baseURI) {
    String name = catalogElem.getAttributeValue("name");
    String version = catalogElem.getAttributeValue("version");
    InvCatalogImpl catalog = new InvCatalogImpl( name, version, baseURI);

    // read top-level dataset
    Element dsElement = catalogElem.getChild("dataset", defNS);
    if (dsElement == null) { // no top element : fatal error
      InvCatalogImpl cat = new InvCatalogImpl( baseURI.toString() ,version, baseURI);
      cat.appendErrorMessage( "** InvCatalogFactory6.readXML no top dataset element; URL= ("+
        baseURI.toString()+")\n", true);
      return cat;
    }
    InvDatasetImpl topDataset = readDataset( catalog, null, dsElement, baseURI);
    catalog.addDataset( topDataset);
    topDataset.setCatalog( catalog);
    // catalog.finish();

    return catalog;
  }

  protected InvCatalogRef readCatalogRef( InvDatasetImpl parent, Element catRefElem) {
    String title = catRefElem.getAttributeValue("title", xlinkNS);
    String href = catRefElem.getAttributeValue("href", xlinkNS);

    InvCatalogRef catRef = new InvCatalogRef( parent, title, href);

    // look for documentation
    java.util.List docList = catRefElem.getChildren("documentation", defNS);
    for (int j=0; j< docList.size(); j++) {
      InvDocumentation doc = readDocumentation(parent.getParentCatalog(), (Element) docList.get(j));
      catRef.addDocumentation( doc);
     }

    return catRef;
  }

    // read a dataset element
  protected InvDatasetImpl readDataset( InvCatalogImpl catalog, InvDatasetImpl parent, Element dsElem, URI base) {

      // deal with aliases
    String name = dsElem.getAttributeValue("name");
    String alias = dsElem.getAttributeValue("alias");
    if (alias != null) {
      InvDatasetImpl ds = (InvDatasetImpl) catalog.findDatasetByID( alias);
      if (ds == null)
        factory.appendErr(" ** Parse error: dataset named "+name+" has illegal alias = "+alias+"\n");
      return new InvDatasetImplProxy(name, ds);
    }

     // read attributes
    String dataTypeName = dsElem.getAttributeValue("dataType");
    String authority = dsElem.getAttributeValue("authority");
    String id = dsElem.getAttributeValue("ID");
    String serviceName = dsElem.getAttributeValue("serviceName");
    String urlPath = dsElem.getAttributeValue("urlPath");

    DataType dataType = null;
    if (dataTypeName != null) {
      dataType = DataType.getType( dataTypeName);
      if (dataType == null) {
        dataType = new DataType( dataTypeName);
        factory.appendWarning(" ** warning: non-standard data type = "+dataTypeName);
      }
    }

    // the dataType, serviceName are set in inherited metadata below
    InvDatasetImpl dataset = new InvDatasetImpl( parent, name, null, null, urlPath);
    if (id != null) dataset.setID( id);
    catalog.addDatasetByID( dataset); // need to do immed for alias

        // look for services
    java.util.List serviceList = dsElem.getChildren("service", defNS);
    for (int j=0; j< serviceList.size(); j++) {
      InvService s = readService( (Element) serviceList.get(j), base);
      // If this is the top level dataset, add any services to the catalog.
      //if ( parent == null )  {
      //  catalog.addService( s);
      //}
      // Otherwise, add them to this dataset. REMOVE 071504 JC
      //else {
        dataset.addService( s);
      //}
    }

     // need to have services added before completing the constuction LOOK
     // dataset.finish();

    // look for non-inherited metadata
    ThreddsMetadata tmg = dataset.getLocalMetadata();
    java.util.List list = dsElem.getChildren("documentation", defNS);
    for (int j=0; j< list.size(); j++) {
      InvDocumentation doc = readDocumentation( catalog, (Element) list.get(j));
      tmg.addDocumentation( doc);
     }
    java.util.List mList = dsElem.getChildren("metadata", defNS);
    for (int j=0; j< mList.size(); j++) {
      InvMetadata m = readMetadata( catalog, dataset, (Element) mList.get(j));
      if (m != null) tmg.addMetadata( m);
     }

     // look for access elements
    java.util.List aList = dsElem.getChildren("access", defNS);
    for (int j=0; j< aList.size(); j++) {
      InvAccessImpl a = readAccess( dataset, (Element) aList.get(j));
      dataset.addAccess( a);
     }

     // look for nested dataset and catalogRefs elements (keep them in order)
    java.util.List allChildren = dsElem.getChildren();
    for (int j=0; j< allChildren.size(); j++) {
      Element e = (Element) allChildren.get(j);
      if (e.getName().equals("dataset")) {
        InvDatasetImpl ds = readDataset( catalog, dataset, e, base);
        dataset.addDataset( ds);
      } else if (e.getName().equals("catalogRef")) {
        InvDatasetImpl ds = readCatalogRef( dataset, e);
        dataset.addDataset( ds);
      }
     }

    // look for inherited metadata
    ThreddsMetadata tmgi = dataset.getCat6Metadata();
    if (dataTypeName != null)
      tmgi.setDataType( DataType.getType(dataTypeName));
    if (serviceName != null)
      tmgi.setServiceName( serviceName);
    if (authority != null)
      tmgi.setAuthority( authority);

    // look for property metadata
    java.util.List properties = dsElem.getChildren("property", defNS);
    if (properties.size() > 0) {
      for (int j = 0; j < properties.size(); j++) {
        InvProperty p = readProperty( (Element) properties.get(j));
        tmgi.addProperty(p);
        //dataset.addProperty( p);
      }
    }

    if (InvCatalogFactory.debugXML) System.out.println (" Dataset added: "+ dataset.dump());
    return dataset;
  }

  protected InvDocumentation readDocumentation( InvCatalog catalog, Element s) {
    String href = s.getAttributeValue("href", xlinkNS);
    String title = s.getAttributeValue("title", xlinkNS);
    String content = s.getTextNormalize();

    URI uri = null;
    if (href != null) {
      try {
        uri = catalog.resolveUri(href);
      } catch (Exception e) {
        factory.appendErr(" ** Invalid documentation href = "+href+" "+e.getMessage());
      }
    }

    InvDocumentation doc = new InvDocumentation( href, uri, title, null, content);

    if (InvCatalogFactory.debugXML) System.out.println (" Documentation added: "+ doc);
    return doc;
  }

  protected InvMetadata readMetadata( InvCatalog catalog, InvDatasetImpl dataset, Element mdataElement) {
    // there are 6 cases to deal with: threddsNamespace vs not & inline vs Xlink & hasConverter or not
    // (the hasConverter only applies when its not threddsNamespace, giving 6 cases)
    // this factory is the converter for threddsNamespace metadata
    //  and also handles non-threddsNamespace when there is no converter, in which case it just
    //   propagates the inline dom elements

    String mtype = mdataElement.getAttributeValue("metadataType");
    String href = mdataElement.getAttributeValue("href", xlinkNS);
    String title = mdataElement.getAttributeValue("title", xlinkNS);
    boolean inherited = false;

    boolean isThreddsNamespace = ((mtype == null) || mtype.equals("THREDDS"));

    // see if theres a converter for it.
    MetadataConverterIF metaConverter = factory.getMetadataConverter( mtype);
    if (metaConverter != null) {
      // see if theres any inline content
      Object contentObj = null;
      if (href == null) {
        contentObj = metaConverter.readMetadataContent( dataset, mdataElement);
        return new InvMetadata( dataset, mtype, null, null,
                              inherited, false, metaConverter, contentObj);

      } else { // otherwise it  must be an Xlink; defer reading
        return new InvMetadata(dataset, href, title, mtype, null, null,
                               inherited, false, metaConverter);
      }
    }

    // the case where i theres no converter
      if (href == null) {
        // just hold onto the jdom elements as the "content" LOOK should be DOM?
        return new InvMetadata( dataset, mtype, null, null,
                              inherited, false, this, mdataElement);

      } else { // otherwise it must be an Xlink, never read
        return new InvMetadata(dataset, href, title, mtype, null, null,
                               inherited, false, null);
      }

    // theres a simplicfication that InvMetaddata never contains thredds metadata

    /* the case where its ThreddsMetadata
    if (uri == null) {
      ThreddsMetadata tmg = new ThreddsMetadata(false);
      readThreddsMetadata( catalog, dataset, mdataElement, tmg);
      return new InvMetadata( dataset, mtype, null, null,
                            inherited, true, this, tmg);

    } else { // otherwise it  must be an Xlink; defer reading
      return new InvMetadata(dataset, uri, title, mtype, null, null,
        inherited, true, this);
    } */

  }

  /* MetadataConverterIF
  public Object readMetadataContent( InvDataset dataset, org.w3c.dom.Element mdataElement) {
    InvMetadata m = readMetadata( dataset.getParentCatalog(), (InvDatasetImpl) dataset, toJDOM( mdataElement));
    return m.getThreddsMetadata();
  }

     // this is only called for ThredddsMetadata
  public Object readMetadataContentFromURL( InvDataset dataset, java.net.URI uri) throws java.io.IOException {
    // open and read the referenced catalog XML
    if (debugMetadataRead) System.out.println(" readMetadataContentFromURL = " + uri);
    org.w3c.dom.Element mdataElement = factory.readOtherXML( uri);
    if (mdataElement == null) {
      factory.appendErr(" ** failed to read thredds metadata at = "+uri+" for dataset"+dataset.getName()+"\n");
      return null;
    }

    Object contentObject = readMetadataContent( dataset, mdataElement);
    if (debugMetadataRead) System.out.println(" convert to " + contentObject.getClass().getName());
    return contentObject;
  } */

  /* protected InvMetadata readMetadata( InvCatalogImpl catalog, InvDataset dataset, Element mdataElement) {
    String href = mdataElement.getAttributeValue("href", xlinkNS);
    String stype = mdataElement.getAttributeValue("metadataType");
    String content = mdataElement.getTextNormalize();
    //System.out.println("metadata type = "+stype+" content = <"+content+">");

    // get factory for reading in content
    MetadataType type = MetadataType.getType( stype);
    MetadataConverterIF mfactory = (MetadataConverterIF) metadataHash.get( type);
    if (mfactory == null) mfactory = this;
    //System.out.println(this+" "+metadataHash+"readMetadata type = <"+stype+"> mfactory = <"+mfactory+">");

    InvMetadata mdata = null;
    /* LOOK if (href != null) {
      // XLink deferred read
      mdata = new InvMetadata( dataset, href, stype, mfactory);
    } else {
      org.w3c.dom.Element dome = toDOM( mdataElement);
      Object contentObject = mfactory.readMetadataContent( dataset, dome);
      StringBuffer mess = new StringBuffer();
      if (mfactory.validateMetadataContent( contentObject, mess))
        mdata = new InvMetadata( dataset, stype, contentObject, mfactory);
      else {
        catalog.appendErrorMessage( mess.toString(), false); // doesnt make it invalid
        catalog.appendErrorMessage( " Metadata element removed from catalog", false);
      }
    }

    return mdata;
  } */

  // MetadataConverterIF dummy
  public Object readMetadataContentFromURL( InvDataset dataset, String urlString) { return null; }
  public Object readMetadataContent( InvDataset dataset, org.jdom.Element mdataElement) { return null; }
  public void addMetadataContent( org.jdom.Element mdataElement, Object contentObject) { }
  public boolean validateMetadataContent(Object contentObject, StringBuffer out) { return true; }
  public Object readMetadataContentFromURL( InvDataset dataset, java.net.URI uri) throws java.io.IOException { return null; }


  protected InvProperty readProperty( Element s) {
    String name = s.getAttributeValue("name");
    String value = s.getAttributeValue("value");

    InvProperty p = new InvProperty( name, value);

    if (InvCatalogFactory.debugXML) System.out.println (" Property added: "+ p);
    return p;
  }

  protected InvService readService( Element s, URI baseURI) {
    String name = s.getAttributeValue("name");
    String type = s.getAttributeValue("serviceType");
    String serviceBase = s.getAttributeValue("base");
    String suffix = s.getAttributeValue("suffix");

    /* 05/28/03 CHANGE: resolve reletive access in access.getStandardURL
    try {
      URI uri= baseURI.resolve( serviceBase);
      if (debugXMLopen) {
        System.out.println("readService base = "+serviceBase);
        System.out.println(" absolute = "+uri.toString());
      }
      serviceBase = uri.toString();
    } catch (Exception e) {
      e.printStackTrace();
    } */

    InvService service = new InvService( name, type, serviceBase, suffix, null);


    java.util.List propertyList = s.getChildren("property", defNS);
    for (int j=0; j< propertyList.size(); j++) {
      InvProperty p = readProperty( (Element) propertyList.get(j));
      service.addProperty( p);
     }

    // recurse
    java.util.List serviceList = s.getChildren("service", defNS);
    for (int j=0; j< serviceList.size(); j++) {
      InvService ss = readService( (Element) serviceList.get(j), baseURI);
      service.addService( ss);
     }

    if (InvCatalogFactory.debugXML) System.out.println (" Service added: "+ service);
    return service;
  }

  /* protected void readThreddsMetadata( InvCatalog catalog, InvDatasetImpl dataset, ThreddsMetadata tmg, Element parent) {

     // look for documentation
    List list = parent.getChildren("documentation", defNS);
    for (int j=0; j< list.size(); j++) {
      InvDocumentation doc = readDocumentation( catalog, (Element) list.get(j));
      tmg.addDocumentation( doc);
     }

    // look for metadata
    java.util.List mList = parent.getChildren("metadata", defNS);
    for (int j=0; j< mList.size(); j++) {
      InvMetadata m = readMetadata( catalog, dataset, (Element) mList.get(j));
      if (m != null) tmg.addMetadata( m);
     }

     // look for properties
    list = parent.getChildren("property", defNS);
    for (int j=0; j< list.size(); j++) {
      InvProperty p = readProperty( (Element) list.get(j));
      tmg.addProperty( p);
     }
  } */

  /************************************************************************/
  // Writing XML from objects


  public void writeXML(InvCatalogImpl catalog, OutputStream os, boolean raw) throws IOException {
    writeXML( catalog, os);
  }

  /**
   * Write the catalog as an XML document to the specified stream.
   *
   * @param catalog write this catalog
   * @param os write to this OutputStream
   * @throws IOException
   */
  public void writeXML(InvCatalogImpl catalog, OutputStream os) throws IOException {
    // Output the document, use standard formatter
    //XMLOutputter fmt = new XMLOutputter();
    //fmt.setNewlines(true);
    //fmt.setIndent("  ");
    XMLOutputter fmt = new XMLOutputter( Format.getPrettyFormat());
    fmt.output(makeCatalog(catalog), os);
  }

  public Document makeCatalog(InvCatalogImpl cat) {
    String dtd = cat.getDTDid();
    if (dtd == null) dtd = dtdDefault;

    DocType docType = new DocType("catalog", dtd);
    Element rootElem = new Element("catalog", defNS);
    Document doc = new Document(rootElem);
    doc.setDocType( docType);

    // attributes
    rootElem.setAttribute("name", cat.getName());
    rootElem.setAttribute("version", "0.6");
    rootElem.addNamespaceDeclaration(xlinkNS);

    // Make the top level dataset element.
    Element topDsElem = makeDataset( cat, null, (InvDatasetImpl) cat.getDataset() );
    rootElem.addContent( topDsElem );

    return doc;
  }

  private Element makeAccess( InvAccessImpl access) {
    Element accessElem = new Element("access", defNS);
    accessElem.setAttribute("urlPath", access.getUrlPath());
    if (access.getServiceType() != null)
      accessElem.setAttribute("serviceType", access.getServiceType().toString());
    if (access.getServiceName() != null)
      accessElem.setAttribute("serviceName", access.getServiceName());
    return accessElem;
  }

  private Element makeCatalogRef( InvCatalogRef catRef) {
    Element catrefElem = new Element("catalogRef", defNS);
    catrefElem.setAttribute("href", catRef.getXlinkHref(), xlinkNS);
    catrefElem.setAttribute("title", catRef.getName(), xlinkNS);
    return catrefElem;
  }

  private Element makeDataset( InvCatalogImpl catalog, InvDatasetImpl p, InvDatasetImpl ds) {
    Element dsElem = new Element("dataset", defNS);
    if (ds == null) return dsElem;

    if (ds instanceof InvDatasetImplProxy) {
      dsElem.setAttribute("name", ((InvDatasetImplProxy)ds).getAliasName());
      dsElem.setAttribute("alias", ds.getID());
      return dsElem;
    }

    dsElem.setAttribute("name", ds.getName());


    // other attributes
    if (ds.getID() != null)
      dsElem.setAttribute("ID", ds.getID());
    if (ds.getUrlPath() != null)
      dsElem.setAttribute("urlPath", ds.getUrlPath());

    // services (local only)
    Iterator services = ds.getServicesLocal().iterator();
    while ( services.hasNext()) {
      InvService service = (InvService) services.next();
      dsElem.addContent( makeService( service));
    }

    // Check if there are services in catalog as per object model (and 1.0 spec).
    // Add to top level dataset as per 0.6 spec.
    if (p == null) {
      List topLevelServices = catalog.getServices();
      Iterator it = topLevelServices.iterator();
      while (it.hasNext()) {
        InvService curService = (InvService) it.next();
        dsElem.addContent(makeService(curService));
      }
    }

    // documentation
    Iterator docs = ds.getDocumentation().iterator();
    while ( docs.hasNext()) {
      InvDocumentation doc = (InvDocumentation) docs.next();
      dsElem.addContent( makeDocumentation( doc));
    }

    // metadata
    Iterator mdata = ds.getMetadata().iterator();
    while ( mdata.hasNext()) {
      InvMetadata m = (InvMetadata) mdata.next();
      dsElem.addContent( makeMetadata( m));
    }

    ThreddsMetadata tm = ds.getLocalMetadata();
    ThreddsMetadata tmi = ds.getCat6Metadata();
    if (tmi.getDataType() != null)
      dsElem.setAttribute("dataType", tmi.getDataType().toString());
    else if (tm.getDataType() != null)
      dsElem.setAttribute("dataType", tm.getDataType().toString());

    if (tmi.getServiceName() != null)
      dsElem.setAttribute("serviceName", tmi.getServiceName());
    else if (tm.getServiceName() != null)
      dsElem.setAttribute("serviceName", tm.getServiceName());

    if (tmi.getAuthority() != null)
      dsElem.setAttribute("authority", tmi.getAuthority());
    else if (tm.getAuthority() != null)
      dsElem.setAttribute("authority", tm.getAuthority());

    List list = tmi.getProperties();
    for (int j=0; j< list.size(); j++) {
      InvProperty pr = (InvProperty) list.get(j);
      dsElem.addContent( makeProperty(pr));
    }

    list = tm.getProperties();
    for (int j=0; j< list.size(); j++) {
      InvProperty pr = (InvProperty) list.get(j);
      dsElem.addContent( makeProperty(pr));
    }

    // access  (local only)
    Iterator access = ds.getAccessLocal().iterator();
    while ( access.hasNext()) {
      InvAccessImpl a = (InvAccessImpl) access.next();
      dsElem.addContent( makeAccess( a));
    }

    // nested datasets
    Iterator datasets = ds.getDatasets().iterator();
    while ( datasets.hasNext()) {
      InvDatasetImpl nested = (InvDatasetImpl) datasets.next();
      if (nested instanceof InvCatalogRef)
        dsElem.addContent( makeCatalogRef( (InvCatalogRef) nested));
      else
        dsElem.addContent( makeDataset( catalog, ds, nested));
    }

    return dsElem;
  }

  private Element makeDocumentation( InvDocumentation doc) {
    Element docElem = new Element("documentation", defNS);
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

  /* private Element makeMetadata( InvMetadata mdata) {
    Element mdataElem = new Element("metadata", defNS);
    if (mdata.getMetadataType() != null)
      mdataElem.setAttribute("metadataType", mdata.getMetadataType().toString());
    if (mdata.hasXlink()) {
      mdataElem.setAttribute("href", mdata.getXlinkHref().toString(), xlinkNS);
      mdataElem.setAttribute("title", mdata.getXlinkTitle(), xlinkNS);
    } else {

        // get factory and read in content
      MetadataConverterIF mfactory = (MetadataConverterIF) metadataHash.get( mdata.getMetadataType());
      //System.out.println(this+" "+metadataHash+"makeMetadata type = <"+mdata.getMetadataType()+"> mfactory = <"+mfactory+">");
      if (mfactory != null) {
        org.w3c.dom.Element dome = toDOM( mdataElem);
        mfactory.addMetadataContent(dome, mdata.getContentObject());
      }
    }
    return mdataElem;
  } */

  private Element makeMetadata( InvMetadata mdata) {
    Element mdataElem = new Element("metadata", defNS);
    if (mdata.getMetadataType() != null)
      mdataElem.setAttribute("metadataType", mdata.getMetadataType());
    if (mdata.isInherited())
      mdataElem.setAttribute("inherited", "true");

    if (mdata.hasXlink()) {
      mdataElem.setAttribute("href", mdata.getXlinkHref().toString(), xlinkNS);
      if (mdata.getXlinkTitle() != null)
        mdataElem.setAttribute("title", mdata.getXlinkTitle(), xlinkNS);

    } else {
        // inline non-thredds case
      MetadataConverterIF converter = mdata.getConverter();
      if ((converter != null) && mdata.getContentObject() != null) {
        if (mdata.getContentObject() instanceof Element) { // special case
          Element mdataOrg = (Element) mdata.getContentObject();
          List children = mdataOrg.getChildren();
          for (int i=0; i<children.size(); i++) {
            Element child = (Element) children.get(i);
            mdataElem.addContent( (Element) child.clone());
          }
        } else {
          //org.w3c.dom.Element dome = toDOM(mdataElem);
          converter.addMetadataContent(mdataElem, mdata.getContentObject());
          //mdataElem = toJDOM(dome);
          mdataElem.detach();
        }
      }
    }

    return mdataElem;
  }

  private Element makeProperty( InvProperty prop) {
    Element propElem = new Element("property", defNS);
    propElem.setAttribute("name", prop.getName());
    propElem.setAttribute("value", prop.getValue());
    return propElem;
  }

  private Element makeService( InvService service) {
    Element serviceElem = new Element("service", defNS);
    serviceElem.setAttribute("name", service.getName());
    serviceElem.setAttribute("serviceType", service.getServiceType().toString());
    serviceElem.setAttribute("base", service.getBase());
    if ((service.getSuffix() != null) && (service.getSuffix().length() > 0))
      serviceElem.setAttribute("suffix", service.getSuffix());

    // services
    Iterator services = service.getServices().iterator();
    while ( services.hasNext()) {
      InvService nested = (InvService) services.next();
      serviceElem.addContent( makeService( nested));
    }

    // properties
    Iterator props = service.getProperties().iterator();
    while ( props.hasNext()) {
      InvProperty p = (InvProperty) props.next();
      serviceElem.addContent( makeProperty( p));
    }

    return serviceElem;
  }

 /* private org.w3c.dom.Element toDOM( Element elem) {
    try {
      if (domOut == null) domOut = new DOMOutputter();
      return domOut.output(elem);
    } catch (JDOMException e) {
      System.out.println("InvCatalogFactory6.readMetadata.toDom error " + e);
      return null;
    }
  }

  public Element toJDOM( org.w3c.dom.Element domElement) {
    return builder.build(domElement);
  }  */

}