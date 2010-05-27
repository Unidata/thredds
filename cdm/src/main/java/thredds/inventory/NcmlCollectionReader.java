/*
 * Copyright (c) 1998 - 2010. University Corporation for Atmospheric Research/Unidata
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
package thredds.inventory;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import thredds.catalog.XMLEntityResolver;
import ucar.nc2.util.URLnaming;

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
  static public final Namespace ncNS = Namespace.getNamespace("nc", XMLEntityResolver.NJ22_NAMESPACE);
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NcmlCollectionReader.class);

  private static boolean debugURL = false, debugXML = false, showParsedXML = false;
  private static final boolean validate = false;

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

    org.jdom.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder(validate);
      if (debugURL) System.out.println(" NetcdfDataset URL = <" + url + ">");
      doc = builder.build(url);
    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }
    if (debugXML) System.out.println(" SAXBuilder done");

    if (showParsedXML) {
      XMLOutputter xmlOut = new XMLOutputter();
      System.out.println("*** NetcdfDataset/showParsedXML = \n" + xmlOut.outputString(doc) + "\n*******");
    }

    Element netcdfElem = doc.getRootElement();
    Namespace use = netcdfElem.getNamespace(); // detect incorrect namespace
    if (!use.equals(ncNS)) {
      errlog.format("Incorrect namespace specified in NcML= %s must be %s%n", use.getURI(), ncNS.getURI());
      return null;
    }

    Element aggElem = netcdfElem.getChild("aggregation", ncNS);
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

    Element scanElem = aggElem.getChild("scan", ncNS);
    if (scanElem == null) scanElem = aggElem.getChild("scanFmrc", ncNS);
    if (scanElem == null) {
      errlog.format("NcML must have aggregation scan or scanFmrc element");
      return null;
    }

    return new NcmlCollectionReader(ncmlLocation, netcdfElem);
  }

  //////////////////////////////////////////////////////////////////
  private DatasetCollectionManager datasetManager;
  private boolean hasInner, hasOuter;
  private Element netcdfElem, aggElem;

  NcmlCollectionReader(String ncmlLocation, Element netcdfElem) {

    Element aggElem = netcdfElem.getChild("aggregation", ncNS);
    String recheck = aggElem.getAttributeValue("recheckEvery");

    // get the aggregation/scan element
    Element scanElem = aggElem.getChild("scan", ncNS);
    if (scanElem == null) scanElem = aggElem.getChild("scanFmrc", ncNS);

    String dirLocation = scanElem.getAttributeValue("location");
    dirLocation = URLnaming.resolve(ncmlLocation, dirLocation); // possible relative location

    String regexpPatternString = scanElem.getAttributeValue("regExp");
    String suffix = scanElem.getAttributeValue("suffix");
    String subdirs = scanElem.getAttributeValue("subdirs");
    String olderThan = scanElem.getAttributeValue("olderThan");

    datasetManager = new DatasetCollectionManager(recheck);
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

    hasOuter = hasMods(netcdfElem);
    hasInner = hasMods(aggElem);

    if (hasOuter)
      this.netcdfElem = netcdfElem;
    if (hasInner)
      this.aggElem = aggElem;
  }

  private boolean hasMods(Element elem) {
    if (elem.getChildren("attribute", ncNS).size() > 0) return true;
    if (elem.getChildren("variable", ncNS).size() > 0) return true;
    if (elem.getChildren("dimension", ncNS).size() > 0) return true;
    if (elem.getChildren("group", ncNS).size() > 0) return true;
    if (elem.getChildren("remove", ncNS).size() > 0) return true;
    return false;
  }

  public Element getNcmlOuter() {
    return netcdfElem;
  }

  public Element getNcmlInner() {
    return aggElem;
  }

  public DatasetCollectionManager getDatasetManager() {
    return datasetManager;
  }

  /* public DateExtractor getDateExtractor() {
    return dateExtractor;
  } */
}
