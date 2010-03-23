// $Id: ADNWriter.java 48 2006-07-12 16:15:40Z caron $
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
import thredds.catalog.crawl.CatalogCrawler;
import ucar.nc2.units.DateType;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.TimeDuration;


import org.jdom.*;
import org.jdom.output.*;

import java.io.*;
import java.util.*;
import java.net.URI;

import ucar.unidata.util.StringUtil;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.nc2.units.TimeUnit;
import ucar.nc2.constants.FeatureType;

public class ADNWriter {
  private static final Namespace defNS = Namespace.getNamespace("http://adn.dlese.org");
  private static final String schemaLocation = "http://www.dlese.org/Metadata/adn-item/0.6.50/record.xsd";
  // private static final String schemaLocationLocal = defNS.getURI() + " file://c:/dev/metadata/adn/0.6.50/record.xsd";

  private String fileDir;
  private StringBuffer messBuffer;

  public ADNWriter( ) {
  }

  public void writeDatasetEntries( InvCatalogImpl cat, String fileDir, StringBuffer mess) {
    this.fileDir = fileDir;
    this.messBuffer = mess;

    File dir = new File(fileDir);
    if (!dir.exists())
      dir.mkdirs();

    CatalogCrawler.Listener listener = new CatalogCrawler.Listener() {
      public void getDataset(InvDataset ds, Object context) {
        doOneDataset(ds);
      }
      public boolean getCatalogRef(InvCatalogRef dd, Object context) { return true; }
      
    };

    ByteArrayOutputStream bis = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream( bis);
    CatalogCrawler crawler = new CatalogCrawler( CatalogCrawler.USE_ALL, true, listener);
    crawler.crawl(cat, null, ps, null);
    mess.append("\n*********************\n");
    mess.append(bis.toString());
  }

  private void doOneDataset( InvDataset ds) {
    if (!ds.isHarvest()) {
      messBuffer.append( " Dataset "+ ds.getName()+ " id = "+ds.getID()+" has harvest = false\n");

    } else if (isDatasetUseable( ds, messBuffer)) {
      String id = StringUtil.replace(ds.getID(), "/","-");
      String fileOutName = fileDir+"/"+id+".adn.xml";
      try {
        OutputStream out = new BufferedOutputStream(new FileOutputStream( fileOutName));
        writeOneItem(ds, out);
        out.close();
        messBuffer.append(" OK on Write\n");

      } catch (IOException ioe) {
        messBuffer.append("FAILED on Write "+ioe.getMessage()+"\n");
        ioe.printStackTrace();
      }
    }
  }

  public boolean isDatasetUseable(InvDataset ds, StringBuffer sbuff) {
    boolean ok = true;
    sbuff.append("Dataset "+ ds.getName()+ " id = "+ds.getID()+": ");

    if (ds.getName() == null) {
      ok = false;
      sbuff.append(" missing Name field\n");
    }

    if (ds.getUniqueID() == null) {
      ok = false;
      sbuff.append(" missing ID field\n");
    }

    List list = ds.getPublishers();
    if ((list == null) || (list.size() == 0)) {
      ok = false;
      sbuff.append(" must have publisher element that defines the data center\n");
    }

    String summary = ds.getDocumentation("summary");
    if (summary == null) {
      ok = false;
      sbuff.append(" must have documentation element of type summary\n");
    }

    String rights = ds.getDocumentation("rights");
    if (rights == null) {
      ok = false;
      sbuff.append(" must have documentation element of type rights\n");
    }

    sbuff.append(" useable= "+ok+"\n");
    return ok;
  }



  private void writeOneItem( InvDataset ds, OutputStream out) throws IOException {
    Element rootElem = new Element("itemRecord", defNS);
    Document doc = new Document(rootElem);
    writeDataset( ds, rootElem);
    rootElem.addNamespaceDeclaration(XMLEntityResolver.xsiNS);
    // rootElem.setAttribute("schemaLocation", schemaLocationLocal, XMLEntityResolver.xsiNS);
    rootElem.setAttribute("schemaLocation", defNS.getURI()+" "+schemaLocation, XMLEntityResolver.xsiNS);

    // Output the document, use standard formatter
    //XMLOutputter fmt = new XMLOutputter("  ", true);
    XMLOutputter fmt = new XMLOutputter( Format.getPrettyFormat());
    fmt.output( doc, out);
  }

  public void writeDataset(InvDataset ds, Element rootElem) {
    // general
    Element generalElem = new Element("general", defNS);
    rootElem.addContent( generalElem);

    generalElem.addContent( new Element("title", defNS).addContent("Dataset "+ds.getFullName()));
    generalElem.addContent( new Element("description", defNS).addContent(ds.getDocumentation("summary")));
    generalElem.addContent( new Element("language", defNS).addContent("en"));

    Element subjects = new Element("subjects", defNS);
    generalElem.addContent( subjects);
    subjects.addContent( new Element("subject", defNS).addContent("DLESE:Atmospheric science"));

    List list = ds.getKeywords();
    if (list.size() > 0) {
      Element keywords = new Element("keywords", defNS);
      generalElem.addContent( keywords);
      for (int i=0; i<list.size(); i++) {
        ThreddsMetadata.Vocab k = (ThreddsMetadata.Vocab) list.get(i);
        keywords.addContent( new Element("keyword", defNS).addContent( k.getText()));
      }
    }

    // life cycle
    Element lifeElem = new Element("lifecycle", defNS);
    rootElem.addContent( lifeElem);

    if ((ds.getPublishers().size() > 0) || (ds.getCreators().size() > 0)){
      Element contributors = new Element("contributors", defNS);
      lifeElem.addContent( contributors);

      list = ds.getPublishers();
      for (int i=0; i<list.size(); i++) {
        ThreddsMetadata.Source p = (ThreddsMetadata.Source) list.get(i);
        if (p.getNameVocab().getVocabulary().equalsIgnoreCase("ADN")) {
          contributors.addContent( writeSource(p, "Publisher"));
          break;
        }
      }

      list = ds.getCreators();
      for (int i=0; i<list.size(); i++) {
        ThreddsMetadata.Source p = (ThreddsMetadata.Source) list.get(i);
        if (p.getNameVocab().getVocabulary().equalsIgnoreCase("ADN")) {
          contributors.addContent( writeSource(p, "Author"));
          break;
        }
      }
    }

    // metaMetadata
    Element metaElem = new Element("metaMetadata", defNS);
    rootElem.addContent( metaElem);

    Element entries = new Element("catalogEntries", defNS);
    metaElem.addContent( entries);

    Element entry = new Element("catalog", defNS);
    entries.addContent( entry);
    String id = StringUtil.allow( ds.getUniqueID(), ".", '-');
    entry.setAttribute("entry", id);
    entry.addContent("THREDDS-motherlode");

    DateType today = new DateType(false, new Date());
    Element dateInfo = new Element("dateInfo", defNS);
    metaElem.addContent( dateInfo);
    dateInfo.setAttribute("created", today.toDateString());
    dateInfo.setAttribute("accessioned", today.toDateString());

    Element status = new Element("statusOf", defNS);
    metaElem.addContent( status);
    status.setAttribute("status", "Accessioned");

    metaElem.addContent( new Element("language", defNS).addContent("en"));
    metaElem.addContent( new Element("scheme", defNS).addContent("ADN (ADEPT/DLESE/NASA Alexandria Digital Earth Prototype/Digital Library for Earth System Education/National Aeronautics and Space Administration)"));
    metaElem.addContent( new Element("copyright", defNS).addContent("Copyright (c) 2002 UCAR (University Corporation for Atmospheric Research)"));

    Element terms = new Element("termsOfUse", defNS);
    metaElem.addContent( terms);
    terms.addContent( "Terms of use consistent with DLESE (Digital Library for Earth System Education) policy");
    terms.setAttribute( "URI", "http://www.dlese.org/documents/policy/terms_use_full.html");

    // technical
    Element technical = new Element("technical", defNS);
    rootElem.addContent( technical);

    Element online = new Element("online", defNS);
    technical.addContent( online);

    Element primaryURLelem = new Element("primaryURL", defNS);
    online.addContent( primaryURLelem);

    String href;
    if (ds instanceof InvCatalogRef) {
      InvCatalogRef catref = (InvCatalogRef) ds;
      URI uri = catref.getURI();
      href = uri.toString();
      int pos = href.lastIndexOf('.');
      href = href.substring(0,pos)+".html";

    } else {
      InvCatalogImpl cat = (InvCatalogImpl) ds.getParentCatalog();
      String catURL = cat.getBaseURI().toString();
      int pos = catURL.lastIndexOf('.');
      String catURLh = catURL.substring(0,pos)+".html";
      href = catURLh+"?dataset="+ds.getID();
    }

    primaryURLelem.addContent( href);

    Element mediums = new Element("mediums", defNS);
    online.addContent( mediums);
    mediums.addContent( new Element("medium", defNS).addContent("text/html"));

    Element reqs = new Element("requirements", defNS);
    online.addContent( reqs);
    Element req = new Element("requirement", defNS);
    reqs.addContent( req);
    req.addContent( new Element("reqType", defNS).addContent("DLESE:Other:More specific technical requirements"));

    Element oreqs = new Element("otherRequirements", defNS);
    online.addContent( oreqs);
    Element oreq = new Element("otherRequirement", defNS);
    oreqs.addContent( oreq);
    oreq.addContent( new Element("otherType", defNS).addContent("Requires data viewer tool. See the documentation page in the resource."));

    // educational
    Element educational = new Element("educational", defNS);
    rootElem.addContent( educational);

    Element audiences = new Element("audiences", defNS);
    educational.addContent( audiences);

    Element audience = new Element("audience", defNS);
    audiences.addContent( audience);
    audience.addContent( new Element("gradeRange", defNS).addContent("DLESE:Graduate or professional"));

    Element resourceTypes = new Element("resourceTypes", defNS);
    educational.addContent( resourceTypes);
    String resourceType = "DLESE:Data:In situ dataset"; // default
    if (ds.getDataType() == FeatureType.GRID)
      resourceType = "DLESE:Data:Modeled dataset"; // take a guess
    else if (ds.getDataType() == FeatureType.IMAGE)
      resourceType = "DLESE:Data:Remotely sensed dataset";
    resourceTypes.addContent( new Element("resourceType", defNS).addContent(resourceType));

    // rights
    Element rights = new Element("rights", defNS);
    rootElem.addContent( rights);

    rights.addContent( new Element("cost", defNS).addContent("DLESE:No"));
    rights.addContent( new Element("description", defNS).addContent(ds.getDocumentation("rights")));

    // geospatial
    ThreddsMetadata.GeospatialCoverage gc = ds.getGeospatialCoverage();
    if (null != gc)
      rootElem.addContent( writeGeospatialCoverage( gc));

    // temporal
    DateRange dateRange = ds.getTimeCoverage();
    if (null != dateRange)
      rootElem.addContent( writeTemporalCoverage(dateRange));
  }

  // check its an acceptable form of email
  protected boolean emailOK(ThreddsMetadata.Source p) {
    String email = p.getEmail();
    return email.indexOf('@') >= 0; // should really do a regexp
  }


  protected Element writeSource(ThreddsMetadata.Source p, String role) {
    Element contributor = new Element("contributor", defNS);
    contributor.setAttribute("role", role);

    Element organization = new Element("organization", defNS);
    contributor.addContent( organization);

    String name = p.getNameVocab().getText();
    int pos = name.indexOf("/");
    if (pos > 0) {
      organization.addContent( new Element("instName", defNS).addContent(name.substring(0,pos)));
      organization.addContent( new Element("instDept", defNS).addContent(name.substring(pos+1)));
    } else
      organization.addContent( new Element("instName", defNS).addContent(name));

    if ((p.getUrl() != null) && p.getUrl().length() > 0)
      organization.addContent( new Element("instUrl", defNS).addContent(p.getUrl()));

    if (emailOK(p))
      organization.addContent( new Element("instEmail", defNS).addContent(p.getEmail()));

    return contributor;
  }

  protected Element writeGeospatialCoverage(ThreddsMetadata.GeospatialCoverage gc) {
    Element geos = new Element("geospatialCoverages", defNS);

    Element geo = new Element("geospatialCoverage", defNS);
    geos.addContent( geo);

    Element body = new Element("body", defNS);
    geo.addContent( body);
    body.addContent( new Element("planet", defNS).addContent("Earth"));

    geo.addContent( new Element("geodeticDatumGlobalOrHorz", defNS).addContent("DLESE:WGS84"));
    geo.addContent( new Element("projection", defNS).setAttribute("type", "DLESE:Unknown"));
    geo.addContent( new Element("coordinateSystem", defNS).setAttribute("type", "DLESE:Geographic latitude and longitude"));

    Element bb = new Element("boundBox", defNS);
    geo.addContent( bb);

    double west = LatLonPointImpl.lonNormal(gc.getLonWest());
    double east = LatLonPointImpl.lonNormal(gc.getLonEast());
    bb.addContent( new Element("westCoord", defNS).setText( ucar.unidata.util.Format.dfrac( west, 2)));
    bb.addContent( new Element("eastCoord", defNS).setText( ucar.unidata.util.Format.dfrac( east, 2)));
    bb.addContent( new Element("northCoord", defNS).setText( ucar.unidata.util.Format.dfrac( gc.getLatNorth(), 2)));
    bb.addContent( new Element("southCoord", defNS).setText( ucar.unidata.util.Format.dfrac( gc.getLatSouth(), 2)));

    bb.addContent( new Element("bbSrcName", defNS).setText( "Calculated from dataset coordinate system by CDM/THREDDS"));
    return geos;
  }

  protected Element writeTemporalCoverage(DateRange dateRange) {
    Element tc = new Element("temporalCoverages", defNS);
    Element tp = new Element("timeAndPeriod", defNS);
    Element ti = new Element("timeInfo", defNS);
    tc.addContent(tp.addContent(ti));

    Element time;
    DateType startDate = dateRange.getStart();
    DateType endDate = dateRange.getEnd();
    if (endDate.isPresent()) {

      String units = "Days ago";
      TimeDuration duration = dateRange.getDuration();
      double value = -0.0;

      try {
        TimeUnit tdayUnit = new TimeUnit("days");
        value = duration.getValue(tdayUnit);

      } catch (Exception e) {
        e.printStackTrace();
      }

      time = new Element("timeRelative", defNS);
      Element begin = new Element("begin", defNS);
      begin.setAttribute("units", units);
      begin.addContent(Double.toString(value));
      time.addContent( begin);

      Element end = new Element("end", defNS);
      end.setAttribute("units", units);
      end.addContent("0");
      time.addContent( end);

    } else {
      // LOOK not tested
      time = new Element("timeAD", defNS);
      Element begin = new Element("begin", defNS);
      begin.setAttribute("date", startDate.toDateString());
      time.addContent( begin);

      Element end = new Element("end", defNS);
      end.setAttribute("date", endDate.toDateString());
      time.addContent( end);
    }

    ti.addContent(time);
    return tc;
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

        ADNWriter w = new ADNWriter();
        StringBuffer sbuff = new StringBuffer();
        w.writeDatasetEntries( cat, "C:/temp/adn3", sbuff);
        System.out.println(" messages=\n"+sbuff);

      } catch (Exception e) {
        e.printStackTrace();
      }

  }

   /** testing */
  public static void main (String[] args) throws Exception {
    InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory(true);

    // doOne(catFactory, "file:///C:/dev/thredds/catalog/test/data/TestHarvest.xml");
    doOne(catFactory, "http://motherlode.ucar.edu:9080/thredds/idd/models.xml");
    //doOne(catFactory, "http://motherlode.ucar.edu:8088/thredds/configCatGenModels.xml");
  }

}
