// $Id: DCWriter.java 48 2006-07-12 16:15:40Z caron $
/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.catalog.dl;

import thredds.catalog.*;
import ucar.nc2.units.DateType;
import ucar.nc2.units.DateRange;


import org.jdom.*;
import org.jdom.output.*;

import java.io.*;
import java.util.*;

public class DCWriter {
  private static final Namespace defNS = Namespace.getNamespace("http://purl.org/dc/elements/1.1/");
  private static final String schemaLocation = defNS.getURI() + " http://www.unidata.ucar.edu/schemas/other/dc/dc.xsd";
  private static final String schemaLocationLocal = defNS.getURI() + " file://P:/schemas/other/dc/dc.xsd";
  private static String threddsServerURL = "http://localhost:8080/thredds/subset.html";

  private InvCatalog cat;
  public DCWriter( InvCatalog cat) {
    this.cat = cat;
  }

  public void writeItems( String fileDir) {
    File dir = new File(fileDir);
    if (!dir.exists())
      dir.mkdirs();

    List datasets = cat.getDatasets();
    for (int i=0; i<datasets.size(); i++) {
      InvDataset elem = (InvDataset) datasets.get(i);
      doDataset( elem, fileDir);
    }

  }

  private void doDataset( InvDataset ds, String fileDir) {
    if (ds.isHarvest() && (ds.getID() != null)) {
      String fileOutName = fileDir+"/"+ds.getID()+".dc.xml";
      try {
        OutputStream out = new BufferedOutputStream(new FileOutputStream(
            fileOutName));
        writeOneItem(ds, System.out);
        writeOneItem(ds, out);
        out.close();
        return;
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
    }

    List datasets = ds.getDatasets();
    for (int i=0; i<datasets.size(); i++) {
      InvDataset nested = (InvDataset) datasets.get(i);
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
    List list = ds.getKeywords();
    if (list.size() > 0) {
      for (int i=0; i<list.size(); i++) {
        ThreddsMetadata.Vocab k = (ThreddsMetadata.Vocab) list.get(i);
        rootElem.addContent( new Element("Keyword", defNS).addContent( k.getText()));
      }
    }

    //temporal
    DateRange tm = ds.getTimeCoverage();
    Element tmElem = new Element("Temporal_Coverage", defNS);
    rootElem.addContent( tmElem);

    tmElem.addContent( new Element("Start_Date", defNS).addContent( tm.getStart().toDateString()));
    tmElem.addContent( new Element("End_Date", defNS).addContent( tm.getEnd().toDateString()));

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
    list = ds.getPublishers();
    if (list.size() > 0) {
      for (int i=0; i<list.size(); i++) {
        ThreddsMetadata.Source p = (ThreddsMetadata.Source) list.get(i);
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
    rootElem.addContent(new Element("DIF_Creation_Date", defNS).addContent(today.toDateString()));

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
        InvCatalogImpl cat = (InvCatalogImpl) fac.readXML(url);
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

/* Change History:
   $Log: DCWriter.java,v $
   Revision 1.7  2006/04/20 22:13:14  caron
   improve DL record extraction
   CatalogCrawler improvements

   Revision 1.6  2006/01/04 00:16:33  caron
   jdom 1.0
   use nj22.12, visadNoDods

   Revision 1.5  2005/05/04 17:56:26  caron
   use nj22.09

   Revision 1.4  2005/05/04 17:16:23  caron
   replace ThreddsMetadata.TimeCoverage object with DateRange

   Revision 1.3  2004/09/24 03:26:28  caron
   merge nj22

   Revision 1.2  2004/06/09 00:27:26  caron
   version 2.0a release; cleanup javadoc

 */