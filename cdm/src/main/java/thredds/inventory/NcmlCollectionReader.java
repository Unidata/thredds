/*
 * Copyright (c) 1998-2017 University Corporation for Atmospheric Research/Unidata
 * See LICENSE.txt for license information.
 */

package thredds.inventory;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;

import thredds.client.catalog.Catalog;
import ucar.nc2.util.AliasTranslator;
import ucar.nc2.util.URLnaming;
import ucar.unidata.util.StringUtil2;

import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * Convert NcML into a CollectionManager.
 * Must have aggregation and scan/scanFmrc element.
 * Tracks inner and outer ncml modifications.
 *
 * @author caron
 * @since Feb 24, 2010
 */
public class NcmlCollectionReader {
  // static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NcmlCollectionReader.class);

  private static final boolean debugURL = false, debugXML = false, showParsedXML = false;
  //private static final boolean validate = false;

  static private final Namespace ncNSHttp = thredds.client.catalog.Catalog.ncmlNS;
  static private final Namespace ncNSHttps = thredds.client.catalog.Catalog.ncmlNSHttps;
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NcmlCollectionReader.class);

  private Namespace ncmlNS;
  
  /**
   * Read an NcML file from a String, and construct a NcmlCollectionReader from its scan or scanFmrc element.
   *
   * @param ncmlString the NcML to construct the reader from
   * @param errlog put error messages here
   * @return the resulting NetcdfDataset
   * @throws IOException on read error, or bad referencedDatasetUri URI
   */
  static public NcmlCollectionReader readNcML(String ncmlString, Formatter errlog) throws IOException {
      StringReader reader = new StringReader(ncmlString);
      
      org.jdom2.Document doc;
      try {
        SAXBuilder builder = new SAXBuilder();
        if (debugURL) System.out.println(" NetcdfDataset NcML String = <" + ncmlString + ">");
        doc = builder.build(new StringReader(ncmlString));
      } catch (JDOMException e) {
        throw new IOException(e.getMessage());
      }
      if (debugXML) System.out.println(" SAXBuilder done");

      return readXML(doc, errlog, null);
  }

  /**
   * Read an NcML file from a URL location, and construct a NcmlCollectionReader from its scan or scanFmrc element.
   *
   * @param ncmlLocation the URL location string of the NcML document
   * @param errlog put error messages here
   * @return the resulting NetcdfDataset
   * @throws IOException on read error, or bad referencedDatasetUri URI
   */
  static public NcmlCollectionReader open(String ncmlLocation, Formatter errlog) throws IOException {
    if (!ncmlLocation.startsWith("http:") && !ncmlLocation.startsWith("file:"))
      ncmlLocation = "file:" + ncmlLocation;
    
    URL url = new URL(ncmlLocation);

    org.jdom2.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      if (debugURL) System.out.println(" NetcdfDataset URL = <" + url + ">");
      doc = builder.build(url);
    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }
    if (debugXML) System.out.println(" SAXBuilder done");

    return readXML(doc, errlog, ncmlLocation);
  }
  
  static private NcmlCollectionReader readXML(org.jdom2.Document doc, Formatter errlog, String ncmlLocation) {
    if (showParsedXML) {
      XMLOutputter xmlOut = new XMLOutputter();
      System.out.println("*** NetcdfDataset/showParsedXML = \n" + xmlOut.outputString(doc) + "\n*******");
    }

    Element netcdfElem = doc.getRootElement();
    Namespace myNS = netcdfElem.getNamespace(); // detect incorrect namespace

    if (!myNS.equals(Catalog.ncmlNS) && !myNS.equals(Catalog.ncmlNSHttps)) {
      errlog.format("Incorrect namespace specified in NcML= %s must be %s%n or %s%n", myNS.getURI(),
              Catalog.ncmlNS.getURI(), Catalog.ncmlNSHttps.getURI());
      return null;
    }

    Element aggElem = netcdfElem.getChild("aggregation", myNS);
    if (aggElem == null) {
      errlog.format("NcML must have aggregation element");
      return null;
    }

    String type = aggElem.getAttributeValue("type");
    if (!type.equals("forecastModelRunCollection") && !type.equals("forecastModelRunSingleCollection") &&
        !type.equals("fmrc")) {
       errlog.format("NcML aggregation must be of type fmrc");
      return null;
    }
    
    return new NcmlCollectionReader(ncmlLocation, netcdfElem);
  }

  //////////////////////////////////////////////////////////////////
  private MFileCollectionManager datasetManager;
  private boolean hasInner, hasOuter;
  private Element netcdfElem, aggElem;

  NcmlCollectionReader(String ncmlLocation, Element netcdfElem) {

    if (netcdfElem.equals(Catalog.ncmlNSHttps)) {
      this.ncmlNS = Catalog.ncmlNSHttps;
    } else {
      this.ncmlNS = Catalog.ncmlNS;
    }
    Element aggElem = netcdfElem.getChild("aggregation", ncmlNS);
    String recheck = aggElem.getAttributeValue("recheckEvery");

    // get the aggregation/scan element
    Element scanElem = aggElem.getChild("scan", ncmlNS);
    if (scanElem == null) scanElem = aggElem.getChild("scanFmrc", ncmlNS);

    if (scanElem == null) {
      // no directory scan going on here - look for explicitly named datasets
      Map<String, String> realLocationRunTimeMap = new HashMap<>();
      List<String> realLocationList = new ArrayList<>();
      java.util.List<Element> ncList = aggElem.getChildren("netcdf", ncmlNS);
      for (Element netcdfElemNested : ncList) {
        String location = netcdfElemNested.getAttributeValue("location");
        if (location == null)
          location = netcdfElemNested.getAttributeValue("url");
        if (location != null)
          location = AliasTranslator.translateAlias(location);

        String runTime = netcdfElemNested.getAttributeValue("coordValue");
        if (runTime == null) {
          Formatter f = new Formatter();
          f.format("runtime must be explicitly defined for each netcdf element using the attribute coordValue");
          log.error(f.toString());
        }

        String realLocation = URLnaming.resolveFile(ncmlLocation, location);
        // for realLocation on windows, need to standardize path we put into the map
        // for example, C:\data\file.nc will become C:/data/file.nc
        // Hacky hacky hacky hack

        realLocation = StringUtil2.replace(realLocation, '\\', "/");
        realLocationRunTimeMap.put(realLocation, runTime);
      }
      datasetManager = MFileCollectionManager.openWithRecheck(ncmlLocation, recheck);
      datasetManager.setFilesAndRunDate(realLocationRunTimeMap);

    } else {
      String dirLocation = scanElem.getAttributeValue("location");
      dirLocation = URLnaming.resolve(ncmlLocation, dirLocation); // possible relative location

      String regexpPatternString = scanElem.getAttributeValue("regExp");
      String suffix = scanElem.getAttributeValue("suffix");
      String subdirs = scanElem.getAttributeValue("subdirs");
      String olderThan = scanElem.getAttributeValue("olderThan");

      datasetManager = MFileCollectionManager.openWithRecheck(ncmlLocation, recheck);
      datasetManager.addDirectoryScan(dirLocation, suffix, regexpPatternString, subdirs, olderThan, null);

      String dateFormatMark = scanElem.getAttributeValue("dateFormatMark");
      DateExtractor dateExtractor = null;
      if (dateFormatMark != null)
        dateExtractor = new DateExtractorFromName(dateFormatMark, true);
      else {
        String runDateMatcher = scanElem.getAttributeValue("runDateMatcher");
        if (runDateMatcher != null)
          dateExtractor = new DateExtractorFromName(runDateMatcher, false);
      }
      datasetManager.setDateExtractor(dateExtractor);

    }

    hasOuter = hasMods(netcdfElem);
    hasInner = hasMods(aggElem);

    if (hasOuter)
      this.netcdfElem = netcdfElem;
    if (hasInner)
      this.aggElem = aggElem;
  }

  private boolean hasMods(Element elem) {
    if (elem.getChildren("attribute", ncmlNS).size() > 0) return true;
    if (elem.getChildren("variable", ncmlNS).size() > 0) return true;
    if (elem.getChildren("dimension", ncmlNS).size() > 0) return true;
    if (elem.getChildren("group", ncmlNS).size() > 0) return true;
    if (elem.getChildren("remove",ncmlNS).size() > 0) return true;
    return false;
  }

  public Element getNcmlOuter() {
    return netcdfElem;
  }

  public Element getNcmlInner() {
    return aggElem;
  }

  public CollectionManager getCollectionManager() {
    return datasetManager;
  }
}
