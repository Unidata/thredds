/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.catalog.dl;

import thredds.catalog.*;
import thredds.catalog.crawl.CatalogCrawler;
import ucar.nc2.constants.CDM;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;

import org.jdom2.*;
import org.jdom2.output.*;

import java.io.*;
import java.util.*;
import java.net.URI;

import ucar.nc2.units.TimeDuration;
import ucar.unidata.geoloc.*;
import ucar.unidata.util.StringUtil2;

public class DIFWriter {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DIFWriter.class);

  static private final Namespace defNS = Namespace.getNamespace("http://gcmd.gsfc.nasa.gov/Aboutus/xml/dif/");
  static private String schemaLocation ="http://gcmd.nasa.gov/Aboutus/xml/dif/dif.xsd";

  private String fileDir;
  private StringBuilder messBuffer;
  private boolean debug = false;

  public DIFWriter() {
  }

  /**
   * Write all harvestable datasets to DIF records that have at least the minimum metadata.
   * Call isDatasetUseable() to find out.
   *
   * @param cat harvest the datasets starting from here
   * @param fileDir write records to this directory. The dataset id is used as the filename, appending "dif.xml"
   * @param mess status messages are appended here
   */
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

  /**
   * Write a DIF record for a specific dataset
   * @param ds use this dataset
   */
  public void doOneDataset( InvDataset ds) {
    if (debug) System.out.println("doDataset "+ds.getName());

    if (isDatasetUseable( ds, messBuffer)) {
      String id = StringUtil2.replace(ds.getID(), "/", "-");
      String fileOutName = fileDir+"/"+id+".dif.xml";
      try {
        OutputStream out = new BufferedOutputStream(new FileOutputStream( fileOutName));
        // writeOneEntry(ds, System.out, mess);
        writeOneEntry(ds, out, messBuffer);
        out.close();
        messBuffer.append(" OK on Write\n");
      } catch (IOException ioe) {
        messBuffer.append("DIFWriter failed on write " + ioe.getMessage() + "\n");
        log.error("DIFWriter failed on write "+ioe.getMessage(), ioe);
      }
    }
  }

   /**
   * Write a DIF record for a specific dataset
   * @param ds use this dataset
   * @param fileDir write records to this directory. The dataset id is used as the filename, appending "dif.xml"
   * @param mess status messages are appended here
   */
  public void doOneDataset( InvDataset ds, String fileDir, StringBuilder mess) {
    if (debug) System.out.println("doDataset "+ds.getName());

    if (isDatasetUseable( ds, mess)) {
      String id = StringUtil2.replace(ds.getID(), "/", "-");
      String fileOutName = fileDir+"/"+id+".dif.xml";
      try {
        OutputStream out = new BufferedOutputStream(new FileOutputStream( fileOutName));
        // writeOneEntry(ds, System.out, mess);
        writeOneEntry(ds, out, mess);
        out.close();
        mess.append(" OK on Write\n");
      } catch (IOException ioe) {
        mess.append("DIFWriter failed on write " + ioe.getMessage() + "\n");
        log.error("DIFWriter failed on write "+ioe.getMessage(), ioe);
      }
    }
  }

  /**
   * See if a dataset is harvestable to a DIF record.
   *
   * @param ds check this dataset.
   * @param sbuff  put status messages here.
   * @return true if a DIF record can be written
   */
  public boolean isDatasetUseable(InvDataset ds, StringBuilder sbuff) {
    boolean ok = true;
    sbuff.append("Dataset " + ds.getName() + " id = " + ds.getID() + ": ");

    if (!ds.isHarvest()) {
      ok = false;
      sbuff.append("Dataset " + ds.getName() + " id = " + ds.getID() + " has harvest = false\n");
    }

    if (ds.getName() == null) {
      ok = false;
      sbuff.append(" missing Name field\n");
    }

    if (ds.getUniqueID() == null) {
      ok = false;
      sbuff.append(" missing ID field\n");
    }

    ThreddsMetadata.Variables vs = ds.getVariables("DIF");
    if ((vs == null) || (vs.getVariableList().size() == 0))
      vs = ds.getVariables("GRIB-1");
    if ((vs == null) || (vs.getVariableList().size() == 0))
      vs = ds.getVariables("GRIB-2");
    if ((vs == null) || (vs.getVariableList().size() == 0)) {
      ok = false;
      sbuff.append(" missing Variables with DIF or GRIB compatible vocabulary\n");
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

    sbuff.append(" useable= "+ok+"\n");
    return ok;
  }


  private void writeOneEntry( InvDataset ds, OutputStream out, StringBuilder mess) throws IOException {
    Element rootElem = new Element("DIF", defNS);
    Document doc = new Document(rootElem);
    writeDataset( ds, rootElem, mess);
    rootElem.addNamespaceDeclaration(defNS);
    rootElem.addNamespaceDeclaration(XMLEntityResolver.xsiNS);
    rootElem.setAttribute("schemaLocation", defNS.getURI()+" "+schemaLocation, XMLEntityResolver.xsiNS);

    // Output the document, use standard formatter
    XMLOutputter fmt = new XMLOutputter( Format.getPrettyFormat());
    fmt.output( doc, out);
  }

  private Iterator translateGribVocabulary(ThreddsMetadata.Variables vs, boolean isGrib1, StringBuilder mess) {
    if (vs == null)
      return null;

    VocabTranslator vt;
    try {
      vt = isGrib1 ? (VocabTranslator) Grib1toDIF.getInstance() : (VocabTranslator) Grib2toDIF.getInstance();
    } catch (IOException e) {
      log.error("DIFWriter failed opening GribtoDIF VocabTranslator ", e);
      return null;
    }

    // hash on DIF names to eliminate duplicates
    Map<String, ThreddsMetadata.Variable> hash = new HashMap<String, ThreddsMetadata.Variable>(100);
    for (ThreddsMetadata.Variable v : vs.getVariableList()) {
      String fromVocabId = v.getVocabularyId();
      if (fromVocabId == null) {
        mess.append("** no id for " + v.getName() + "\n");
        continue;
      }

      String toVocabName = vt.translate(fromVocabId);
      if (toVocabName == null) {
        mess.append("** no translation for " + fromVocabId + " == " + v.getVocabularyName() + "\n");
        continue;
      }

      // do we already have it ?
      if (hash.get(toVocabName) == null) {
        ThreddsMetadata.Variable transV = new ThreddsMetadata.Variable(v.getName(), v.getDescription(), toVocabName,
                v.getUnits(), fromVocabId);
        hash.put(toVocabName, transV);
      }
    }
    return hash.values().iterator();
  }


  private void writeDataset(InvDataset ds, Element rootElem, StringBuilder mess) {
    String entryId = StringUtil2.allow(ds.getUniqueID(), "_-.", '-');
    rootElem.addContent( new Element("Entry_ID", defNS).addContent(entryId));
    rootElem.addContent( new Element("Entry_Title", defNS).addContent(ds.getFullName()));

    // parameters : look for DIF
    ThreddsMetadata.Variables vs = ds.getVariables("DIF");
    boolean hasVocab = (vs != null) && (vs.getVariableList().size() != 0);
    if (hasVocab) {
      for (ThreddsMetadata.Variable v : vs.getVariableList()) {
        writeVariable( rootElem, v);
      }
    } else {
      // look for GRIB-1
      vs = ds.getVariables("GRIB-1");
      if ((vs != null) && (vs.getVariableList().size() != 0)) {
        Iterator iter = translateGribVocabulary(vs, true, mess);
        while (iter.hasNext()) {
          ThreddsMetadata.Variable v = (ThreddsMetadata.Variable) iter.next();
          writeVariable( rootElem, v);
        }
      } else {
        // look for GRIB-2
        vs = ds.getVariables("GRIB-2");
        if ((vs != null) && (vs.getVariableList().size() != 0)) {
          Iterator iter = translateGribVocabulary(vs, false, mess);
          while ((iter != null) && iter.hasNext()) {
            ThreddsMetadata.Variable v = (ThreddsMetadata.Variable) iter.next();
            writeVariable( rootElem, v);
          }
        }
      }
    }

    // keywords
    List<ThreddsMetadata.Vocab> list = ds.getKeywords();
    if (list.size() > 0) {
      for (int i=0; i<list.size(); i++) {
        ThreddsMetadata.Vocab k = list.get(i);
        rootElem.addContent( new Element("Keyword", defNS).addContent( k.getText()));
      }
    }

     //temporal
    DateRange tm = ds.getTimeCoverage();
    if (tm != null) {
      DateType end = tm.getEnd();
      if (end.isPresent()) {
        TimeDuration duration = tm.getDuration();
        double ndays = duration.getValue("days");
        if (ndays > 0) {
          String reletiveTime = "RELATIVE_START_DATE: "+((int)-ndays);
          rootElem.addContent( new Element("Keyword", defNS).addContent(reletiveTime));
        }
      }
    }

    // LOOK KLUDGE - these need to be added to the catalog !!  see http://gcmd.nasa.gov/Resources/valids/sources.html
    Element platform = new Element("Source_Name", defNS);
    rootElem.addContent(platform);
    platform.addContent( new Element("Short_Name", defNS).addContent("MODELS"));

    if (tm != null) {
      Element tmElem = new Element("Temporal_Coverage", defNS);
      rootElem.addContent(tmElem);

      tmElem.addContent(new Element("Start_Date",
                        defNS).addContent(tm.getStart().toString()));
      tmElem.addContent(new Element("Stop_Date",
                        defNS).addContent(tm.getEnd().toString()));
    }

    //geospatial
    ThreddsMetadata.GeospatialCoverage geo = ds.getGeospatialCoverage();
    if (geo != null) {
      Element geoElem = new Element("Spatial_Coverage", defNS);
      rootElem.addContent(geoElem);

      double eastNormal = LatLonPointImpl.lonNormal(geo.getLonEast());
      double westNormal = LatLonPointImpl.lonNormal(geo.getLonWest());

      geoElem.addContent(new Element("Southernmost_Latitude",
                         defNS).addContent(Double.toString(geo.getLatSouth())));
      geoElem.addContent(new Element("Northernmost_Latitude",
                         defNS).addContent(Double.toString(geo.getLatNorth())));
      geoElem.addContent(new Element("Westernmost_Longitude",
                         defNS).addContent(Double.toString(westNormal)));
      geoElem.addContent(new Element("Easternmost_Longitude",
                         defNS).addContent(Double.toString(eastNormal)));
    }

    /* LOOK
    "<Data_Resolution>\n" +
            "    <Latitude_Resolution>12 Km</Latitude_Resolution>\n" +
            "    <Longitude_Resolution>12 Km</Longitude_Resolution>\n" +
            "    <Horizontal_Resolution_Range>10 km - &lt; 50 km or approximately .09 degree - &lt; .5 degree</Horizontal_Resolution_Range>\n" +
            "    <Temporal_Resolution>6 Hours</Temporal_Resolution>\n" +
            "    <Temporal_Resolution_Range>Hourly - &lt; Daily</Temporal_Resolution_Range>\n" +
            "  </Data_Resolution> "
    */


    String rights = ds.getDocumentation("rights");
    if (rights != null)
      rootElem.addContent( new Element("Use_Constraints", defNS).addContent(rights));

    // data center
    List<ThreddsMetadata.Source> plist = ds.getPublishers();
    if (plist.size() > 0) {
      for (ThreddsMetadata.Source p : plist) {
        if (p.getNameVocab().getVocabulary().equalsIgnoreCase("DIF")) {
          Element dataCenter = new Element("Data_Center", defNS);
          rootElem.addContent(dataCenter);
          writeDataCenter(p, dataCenter);
          break;
        }
      }
    }

    String summary = ds.getDocumentation("summary");
    if (summary != null) {
      String summaryLines = StringUtil2.breakTextAtWords(summary, "\n", 80);
      rootElem.addContent( new Element("Summary", defNS).addContent(summaryLines));
    }

    URI uri;
    String href;
    if (ds instanceof InvCatalogRef) {      // LOOK !!
      InvCatalogRef catref = (InvCatalogRef) ds;
      uri = catref.getURI();
      href = uri.toString();
      int pos = href.lastIndexOf('.');
      href = href.substring(0,pos)+".html";

    } else {
      InvCatalogImpl cat = (InvCatalogImpl) ds.getParentCatalog();
      uri = cat.getBaseURI();
      String catURL = uri.toString();
      int pos = catURL.lastIndexOf('.');
      href = catURL.substring(0,pos)+".html";
      if (ds.hasAccess())
        href = href+"?dataset="+ds.getID();
    }

    rootElem.addContent( makeRelatedURL("GET DATA", "THREDDS CATALOG", uri.toString()));
    rootElem.addContent( makeRelatedURL("GET DATA", "THREDDS DIRECTORY", href));

    InvAccess access;
    if (null != (access = ds.getAccess(ServiceType.OPENDAP))) {
      rootElem.addContent( makeRelatedURL("GET DATA", "OPENDAP DATA", access.getStandardUrlName()));
    }
    if (null != (access = ds.getAccess(ServiceType.DAP4))) {
      rootElem.addContent( makeRelatedURL("GET DATA", "DAP4 DATA", access.getStandardUrlName()));
    }

    rootElem.addContent(new Element("Metadata_Name", defNS).addContent("CEOS IDN DIF"));
    rootElem.addContent(new Element("Metadata_Version", defNS).addContent("9.4"));
    DateType today = new DateType(false, new Date());
    rootElem.addContent(new Element("DIF_Creation_Date", defNS).addContent(today.toDateTimeStringISO()));
  }

  private Element makeRelatedURL(String type, String subtype, String url) {
    Element elem = new Element("Related_URL", defNS);
    Element uctElem = new Element("URL_Content_Type", defNS);
    elem.addContent(uctElem);
    uctElem.addContent( new Element("Type", defNS).addContent(type));
    uctElem.addContent( new Element("Subtype", defNS).addContent(subtype));
    elem.addContent( new Element("URL", defNS).addContent(url));
    return elem;
  }

  private void writeDataCenter(ThreddsMetadata.Source p, Element dataCenter) {
    Element name = new Element("Data_Center_Name", defNS);
    dataCenter.addContent( name);
    // dorky
    StringTokenizer stoker = new StringTokenizer(p.getName(), ">");
    int n = stoker.countTokens();
    if (n == 2) {
      name.addContent( new Element("Short_Name", defNS).addContent(stoker.nextToken().trim()));
      name.addContent( new Element("Long_Name", defNS).addContent(stoker.nextToken().trim()));
    } else {
      name.addContent( new Element("Short_Name", defNS).addContent(p.getName()));
    }

    if ((p.getUrl() != null) && p.getUrl().length() > 0)
      dataCenter.addContent( new Element("Data_Center_URL", defNS).addContent(p.getUrl()));

    Element person = new Element("Personnel", defNS);
    dataCenter.addContent( person);
    person.addContent( new Element("Role", defNS).addContent("DATA CENTER CONTACT"));
    person.addContent( new Element("Last_Name", defNS).addContent("Any"));
    person.addContent( new Element("Email", defNS).addContent(p.getEmail()));
  }

  private void writeVariable( Element rootElem, ThreddsMetadata.Variable v) {
    String vname = v.getVocabularyName();
    StringTokenizer stoker = new StringTokenizer(vname,">");
    int n = stoker.countTokens();
    if (n < 3) return; // gottta have the first 3

    Element param = new Element("Parameters", defNS);
    rootElem.addContent(param);

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
  private static void doCatalog( InvCatalogFactory fac, String url) {
    System.out.println("***read "+url);
    try {
      InvCatalogImpl cat = fac.readXML(url);
      StringBuilder buff = new StringBuilder();
      boolean isValid = cat.check( buff, false);
      System.out.println("catalog <" + cat.getName()+ "> "+ (isValid ? "is" : "is not") + " valid");
      System.out.println(" validation output=\n" + buff);
      System.out.println(" catalog=\n" + fac.writeXML(cat));

      DIFWriter w = new DIFWriter();
      StringBuilder sbuff = new StringBuilder();
      w.writeDatasetEntries( cat, "C:/temp/dif2", sbuff);
      System.out.println(" messages=\n"+sbuff);
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

   /** testing */
  public static void main (String[] args) throws Exception {
    InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory(true);

    doCatalog(catFactory, "http://thredds.ucar.edu:9080/thredds/idd/models.xml");
  }

}