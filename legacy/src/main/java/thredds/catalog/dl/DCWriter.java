/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: DCWriter.java 48 2006-07-12 16:15:40Z caron $

package thredds.catalog.dl;

import thredds.catalog.*;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateType;


import org.jdom2.*;
import org.jdom2.output.*;

import java.io.*;
import java.util.*;

public class DCWriter {
  private static final Namespace defNS = Namespace.getNamespace("http://purl.org/dc/elements/1.1/");
  private static final String schemaLocation = defNS.getURI() + " http://www.unidata.ucar.edu/schemas/other/dc/dc.xsd";
  private static final String threddsServerURL = "http://localhost:8080/thredds/subset.html";

  private InvCatalog cat;
  public DCWriter( InvCatalog cat) {
    this.cat = cat;
  }

  public void writeItems( String fileDir) {
    File dir = new File(fileDir);
    if (!dir.exists()) {
      boolean ret = dir.mkdirs();
      assert ret;
    }

    for (InvDataset dataset :  cat.getDatasets()) {
      doDataset(dataset, fileDir);
    }

  }

  private void doDataset( InvDataset ds, String fileDir) {
    if (ds.isHarvest() && (ds.getID() != null)) {
      String fileOutName = fileDir+"/"+ds.getID()+".dc.xml";
      try ( OutputStream out = new BufferedOutputStream(new FileOutputStream(fileOutName))) {
        writeOneItem(ds, System.out);
        writeOneItem(ds, out);
        return;
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
    }

    for (InvDataset nested :  ds.getDatasets()) {
      doDataset( nested, fileDir);
    }
  }

  private void writeOneItem( InvDataset ds, OutputStream out) throws IOException {
    Element rootElem = new Element("dc", defNS);
    Document doc = new Document(rootElem);
    writeDataset( ds, rootElem);
    rootElem.addNamespaceDeclaration(XMLEntityResolver.xsiNS);
    // rootElem.setAttribute("schemaLocation", schemaLocation, XMLEntityResolver.xsiNS);
    rootElem.setAttribute("schemaLocation", defNS.getURI()+" "+schemaLocation, XMLEntityResolver.xsiNS);

    // Output the document, use standard formatter
    //XMLOutputter fmt = new XMLOutputter("  ", true);
    XMLOutputter fmt = new XMLOutputter( Format.getPrettyFormat());
    fmt.output( doc, out);
  }

  /*
        <xs:element ref="dc:title"/>
        <xs:element ref="dc:creator"/>
        <xs:element ref="dc:subject"/>
        <xs:element ref="dc:description"/>
        <xs:element ref="dc:publisher"/>
        <xs:element ref="dc:contributor"/>
        <xs:element ref="dc:date"/>
        <xs:element ref="dc:type"/>
        <xs:element ref="dc:format"/>
        <xs:element ref="dc:identifier"/>
        <xs:element ref="dc:source"/>
        <xs:element ref="dc:language"/>
        <xs:element ref="dc:relation"/>
        <xs:element ref="dc:coverage"/>
        <xs:element ref="dc:rights"/>
        <!-- controlled vocabulary from dcmitype -->
        <xs:element ref="dcmitype:dcmitype"/>
  */
  public void writeDataset(InvDataset ds, Element rootElem) {

    rootElem.addContent( new Element("title", defNS).addContent(ds.getName()));

    rootElem.addContent( new Element("Entry_ID", defNS).addContent(ds.getUniqueID()));

    // keywords
    List<ThreddsMetadata.Vocab> list = ds.getKeywords();
    if (list.size() > 0) {
      for (ThreddsMetadata.Vocab k : list) {
        rootElem.addContent( new Element("Keyword", defNS).addContent( k.getText()));
      }
    }

    //temporal
    CalendarDateRange tm = ds.getCalendarDateCoverage();
    Element tmElem = new Element("Temporal_Coverage", defNS);
    rootElem.addContent( tmElem);

    tmElem.addContent( new Element("Start_Date", defNS).addContent( tm.getStart().toString()));
    tmElem.addContent( new Element("End_Date", defNS).addContent( tm.getEnd().toString()));

    //geospatial
    ThreddsMetadata.GeospatialCoverage geo = ds.getGeospatialCoverage();
    Element geoElem = new Element("Spatial_Coverage", defNS);
    rootElem.addContent( geoElem);

    geoElem.addContent( new Element("Southernmost_Latitude", defNS).addContent( Double.toString(geo.getLatSouth())));
    geoElem.addContent( new Element("Northernmost_Latitude", defNS).addContent(Double.toString(geo.getLatNorth())));
    geoElem.addContent( new Element("Westernmost_Latitude", defNS).addContent(Double.toString(geo.getLonWest())));
    geoElem.addContent( new Element("Easternmost_Latitude", defNS).addContent(Double.toString(geo.getLonEast())));

    rootElem.addContent( new Element("Use_Constraints", defNS).addContent(ds.getDocumentation("rights")));

    // data center
    List<ThreddsMetadata.Source> slist = ds.getPublishers();
    if (list.size() > 0) {
      for ( ThreddsMetadata.Source p : slist) {
        Element dataCenter = new Element("Data_Center", defNS);
        rootElem.addContent( dataCenter);
        writePublisher(p, dataCenter);
      }
    }

    rootElem.addContent( new Element("Summary", defNS).addContent(ds.getDocumentation("summary")));

    Element primaryURLelem = new Element("Related_URL", defNS);
    rootElem.addContent( primaryURLelem);

    String primaryURL = threddsServerURL +
        "?catalog="+((InvCatalogImpl)ds.getParentCatalog()).getBaseURI().toString() +
        "&dataset="+ds.getID();
    primaryURLelem.addContent( new Element("URL_Content_Type", defNS).addContent("THREDDS access page"));
    primaryURLelem.addContent( new Element("URL", defNS).addContent(primaryURL));

    DateType today = new DateType(false, new Date());
    rootElem.addContent(new Element("DIF_Creation_Date", defNS).addContent(today.toDateTimeStringISO()));

  }

  protected void writePublisher(ThreddsMetadata.Source p, Element dataCenter) {
    Element name = new Element("Data_Center_Name", defNS);
    dataCenter.addContent( name);
    name.addContent( new Element("Short_Name", defNS).addContent(p.getName()));
    //name.addContent( new Element("Long_Name", defNS).addContent(p.getLongName()));

    if ((p.getUrl() != null) && p.getUrl().length() > 0)
      dataCenter.addContent( new Element("Data_Center_URL", defNS).addContent(p.getUrl()));

    Element person = new Element("Personnel", defNS);
    dataCenter.addContent( person);
    person.addContent( new Element("Role", defNS).addContent("DATA CENTER CONTACT"));
    person.addContent( new Element("Email", defNS).addContent(p.getEmail()));
  }

  private void writeVariable( Element param, ThreddsMetadata.Variable v) {
    String vname = v.getVocabularyName();
    StringTokenizer stoker = new StringTokenizer(vname,">");
    if (stoker.hasMoreTokens())
      param.addContent( new Element("Category", defNS).addContent(stoker.nextToken().trim()));
    if (stoker.hasMoreTokens())
      param.addContent( new Element("Topic", defNS).addContent(stoker.nextToken().trim()));
    if (stoker.hasMoreTokens())
      param.addContent( new Element("Term", defNS).addContent(stoker.nextToken().trim()));
    if (stoker.hasMoreTokens())
      param.addContent( new Element("Variable", defNS).addContent(stoker.nextToken().trim()));
    if (stoker.hasMoreTokens())
      param.addContent( new Element("Detailed_Variable", defNS).addContent(stoker.nextToken().trim()));
  }

  // test
  private static void doOne( InvCatalogFactory fac, String url) {
    System.out.println("***read "+url);
    try {
        InvCatalogImpl cat = fac.readXML(url);
        StringBuilder buff = new StringBuilder();
        boolean isValid = cat.check( buff, false);
        System.out.println("catalog <" + cat.getName()+ "> "+ (isValid ? "is" : "is not") + " valid");
        System.out.println(" validation output=\n" + buff);
        // System.out.println(" catalog=\n" + fac.writeXML(cat));

        DCWriter w = new DCWriter( cat);
        w.writeItems( "C:/temp/dif");
      } catch (Exception e) {
        e.printStackTrace();
      }

  }

   /** testing */
  public static void main (String[] args) throws Exception {
    InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory(true);

    doOne(catFactory, "file:///C:/dev/thredds/catalog/test/data/TestHarvest.xml");
  }

}