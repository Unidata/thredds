/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: ADNWriter.java 48 2006-07-12 16:15:40Z caron $

package thredds.catalog.dl;

import thredds.catalog.*;
import thredds.catalog.crawl.CatalogCrawler;
import ucar.nc2.constants.CDM;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import ucar.nc2.units.TimeDuration;

import org.jdom2.*;
import org.jdom2.output.*;

import java.io.*;
import java.util.*;
import java.net.URI;

import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.util.StringUtil2;

public class ADNWriter {
  private static final Namespace defNS = Namespace.getNamespace("http://adn.dlese.org");
  private static final String schemaLocation = "http://www.dlese.org/Metadata/adn-item/0.6.50/record.xsd";
  // private static final String schemaLocationLocal = defNS.getURI() + " file://c:/dev/metadata/adn/0.6.50/record.xsd";

  private String fileDir;
  private StringBuilder messBuffer;

  public ADNWriter( ) {
  }

  public void writeDatasetEntries( InvCatalogImpl cat, String fileDir, StringBuilder mess) {
    this.fileDir = fileDir;
    this.messBuffer = mess;

    File dir = new File(fileDir);
    if (!dir.exists()) {
      boolean ret = dir.mkdirs();
      assert ret;
    }

    CatalogCrawler.Listener listener = new CatalogCrawler.Listener() {
      public void getDataset(InvDataset ds, Object context) {
        doOneDataset(ds);
      }
      public boolean getCatalogRef(InvCatalogRef dd, Object context) { return true; }
      
    };

    ByteArrayOutputStream bis = new ByteArrayOutputStream();
    PrintWriter pw = new PrintWriter( new OutputStreamWriter(bis, CDM.utf8Charset));
    CatalogCrawler crawler = new CatalogCrawler( CatalogCrawler.USE_ALL, true, listener);
    crawler.crawl(cat, null, pw, null);
    mess.append("\n*********************\n");
    mess.append(new String(bis.toByteArray(), CDM.utf8Charset));
  }

  private void doOneDataset( InvDataset ds) {
    if (!ds.isHarvest()) {
      messBuffer.append( " Dataset "+ ds.getName()+ " id = "+ds.getID()+" has harvest = false\n");

    } else if (isDatasetUseable( ds, messBuffer)) {
      String id = StringUtil2.replace(ds.getID(), "/", "-");
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

  public boolean isDatasetUseable(InvDataset ds, StringBuilder sbuff) {
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
    String id = StringUtil2.allow(ds.getUniqueID(), ".", '-');
    entry.setAttribute("entry", id);
    entry.addContent("THREDDS");

    DateType today = new DateType(false, new Date());
    Element dateInfo = new Element("dateInfo", defNS);
    metaElem.addContent( dateInfo);
    dateInfo.setAttribute("created", today.toDateTimeStringISO());
    dateInfo.setAttribute("accessioned", today.toDateTimeStringISO());

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
    int pos = name.indexOf('/');
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

    Element time = null;
    DateType startDate = dateRange.getStart();
    DateType endDate = dateRange.getEnd();
    if (endDate.isPresent()) {

      String units = "Days ago";
      TimeDuration duration = dateRange.getDuration();
      double value = duration.getValue("days");
      if (value > 0) {
        time = new Element("timeRelative", defNS);
        Element begin = new Element("begin", defNS);
        begin.setAttribute(CDM.UNITS, units);
        begin.addContent(Double.toString(value));
        time.addContent( begin);

        Element end = new Element("end", defNS);
        end.setAttribute(CDM.UNITS, units);
        end.addContent("0");
        time.addContent( end);
      }

    } else {
      // LOOK not tested
      time = new Element("timeAD", defNS);
      Element begin = new Element("begin", defNS);
      begin.setAttribute("date", startDate.toString());
      time.addContent( begin);

      Element end = new Element("end", defNS);
      end.setAttribute("date", endDate.toString());
      time.addContent( end);
    }

    if (time != null)
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
        StringBuilder sbuff = new StringBuilder();
        w.writeDatasetEntries( cat, "C:/temp/adn3", sbuff);
        System.out.println(" messages=\n"+sbuff);

      } catch (Exception e) {
        e.printStackTrace();
      }

  }

   /** testing */
  public static void main (String[] args) throws Exception {
    InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory(true);
    doOne(catFactory, "http://thredds.ucar.edu/thredds/idd/models.xml");
  }

}
