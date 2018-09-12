/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.ncml;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.Attribute;
import ucar.nc2.dataset.*;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.IO;
import ucar.nc2.util.URLnaming;

import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import ucar.unidata.util.StringUtil2;

import java.io.*;
import java.net.*;
import java.util.*;

import static ucar.unidata.util.StringUtil2.getTokens;

/**
 * Read NcML and create NetcdfDataset.
 *
 * @author caron
 * @see <a href="http://www.unidata.ucar.edu/software/netcdf/ncml/">http://www.unidata.ucar.edu/software/netcdf/ncml/</a>
 */

public class NcMLReader {

  static private final Namespace ncNSHttp = thredds.client.catalog.Catalog.ncmlNS;
  static private final Namespace ncNSHttps = thredds.client.catalog.Catalog.ncmlNSHttps;
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NcMLReader.class);

  private Namespace ncNS;

  private static boolean debugURL = false, debugXML = false, showParsedXML = false;
  private static boolean debugOpen = false, debugConstruct = false, debugCmd = false;
  private static boolean debugAggDetail = false;

  static public void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag) {
    debugURL = debugFlag.isSet("NcML/debugURL");
    debugXML = debugFlag.isSet("NcML/debugXML");
    showParsedXML = debugFlag.isSet("NcML/showParsedXML");
    debugCmd = debugFlag.isSet("NcML/debugCmd");
    debugOpen = debugFlag.isSet("NcML/debugOpen");
    debugConstruct = debugFlag.isSet("NcML/debugConstruct");
    debugAggDetail = debugFlag.isSet("NcML/debugAggDetail");
  }

   // private static boolean validate = false;

  /**
   * Use NCML to modify a dataset, getting the NcML document as a resource stream.
   * Uses ClassLoader.getResourceAsStream(ncmlResourceLocation), so the NcML can be inside of a jar file, for example.
   *
   * @param ncDataset            modify this dataset
   * @param ncmlResourceLocation resource location of NcML
   * @param cancelTask           allow user to cancel task; may be null
   * @throws IOException on read error
   */
  static public void wrapNcMLresource(NetcdfDataset ncDataset, String ncmlResourceLocation, CancelTask cancelTask) throws IOException {
    ClassLoader cl = ncDataset.getClass().getClassLoader();
    try (InputStream is = cl.getResourceAsStream(ncmlResourceLocation)) {
      if (is == null)
        throw new FileNotFoundException(ncmlResourceLocation);

      if (debugXML) {
        System.out.println(" NetcdfDataset URL = <" + ncmlResourceLocation + ">");
        try (InputStream is2 = cl.getResourceAsStream(ncmlResourceLocation)) {
          System.out.println(" contents=\n" + IO.readContents(is2));
        }
      }

      org.jdom2.Document doc;
      try {
        SAXBuilder builder = new SAXBuilder();
        if (debugURL) System.out.println(" NetcdfDataset URL = <" + ncmlResourceLocation + ">");
        doc = builder.build(is);
      } catch (JDOMException e) {
        throw new IOException(e.getMessage());
      }
      if (debugXML) System.out.println(" SAXBuilder done");

      if (showParsedXML) {
        XMLOutputter xmlOut = new XMLOutputter();
        System.out.println("*** NetcdfDataset/showParsedXML = \n" + xmlOut.outputString(doc) + "\n*******");
      }

      Element netcdfElem = doc.getRootElement();

      NcMLReader reader = new NcMLReader();
      reader.readNetcdf(ncDataset.getLocation(), ncDataset, ncDataset, netcdfElem, cancelTask);
      if (debugOpen) System.out.println("***NcMLReader.wrapNcML result= \n" + ncDataset);
    }
  }


  /**
   * Use NCML to modify the dataset, getting NcML from a URL
   *
   * @param ncDataset    modify this dataset
   * @param ncmlLocation URL location of NcML
   * @param cancelTask   allow user to cancel task; may be null
   * @throws IOException on read error
   */
  static public void wrapNcML(NetcdfDataset ncDataset, String ncmlLocation, CancelTask cancelTask) throws IOException {
    org.jdom2.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      if (debugURL) System.out.println(" NetcdfDataset URL = <" + ncmlLocation + ">");
      doc = builder.build(ncmlLocation);
    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }
    if (debugXML) System.out.println(" SAXBuilder done");

    if (showParsedXML) {
      XMLOutputter xmlOut = new XMLOutputter();
      System.out.println("*** NetcdfDataset/showParsedXML = \n" + xmlOut.outputString(doc) + "\n*******");
    }

    Element netcdfElem = doc.getRootElement();

    NcMLReader reader = new NcMLReader();
    reader.readNetcdf(ncmlLocation, ncDataset, ncDataset, netcdfElem, cancelTask);
    if (debugOpen) System.out.println("***NcMLReader.wrapNcML result= \n" + ncDataset);
  }

  /**
   * Use NCML to modify the referenced dataset, create a new dataset with the merged info
   * Used to wrap each dataset of an aggregation before its aggregated
   *
   * @param ref        referenced dataset
   * @param parentElem parent element - usually the aggregation element of the ncml
   * @return new dataset with the merged info
   * @throws IOException on read error
   */
  static public NetcdfDataset mergeNcML(NetcdfFile ref, Element parentElem) throws IOException {
    NetcdfDataset targetDS = new NetcdfDataset(ref, null); // no enhance

    NcMLReader reader = new NcMLReader();
    reader.readGroup(targetDS, targetDS, null, null, parentElem);
    targetDS.finish();

    return targetDS;
  }

  /**
   * Use NCML to directly modify the dataset
   *
   * @param targetDS   referenced dataset
   * @param parentElem parent element - usually the aggregation element of the ncml
   * @return new dataset with the merged info
   * @throws IOException on read error
   */
  static public NetcdfDataset mergeNcMLdirect(NetcdfDataset targetDS, Element parentElem) throws IOException {

    NcMLReader reader = new NcMLReader();
    reader.readGroup(targetDS, targetDS, null, null, parentElem);
    targetDS.finish();

    return targetDS;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Read an NcML file from a URL location, and construct a NetcdfDataset.
   *
   * @param ncmlLocation the URL location string of the NcML document
   * @param cancelTask   allow user to cancel the task; may be null
   * @return the resulting NetcdfDataset
   * @throws IOException on read error, or bad referencedDatasetUri URI
   */
  static public NetcdfDataset readNcML(String ncmlLocation, CancelTask cancelTask) throws IOException {
    return readNcML(ncmlLocation, (String) null, cancelTask);
  }

  /**
   * Read an NcML file from a URL location, and construct a NetcdfDataset.
   *
   * @param ncmlLocation         the URL location string of the NcML document
   * @param referencedDatasetUri if null (usual case) get this from NcML, otherwise use URI as the location of the referenced dataset.
   * @param cancelTask           allow user to cancel the task; may be null
   * @return the resulting NetcdfDataset
   * @throws IOException on read error, or bad referencedDatasetUri URI
   */
  static public NetcdfDataset readNcML(String ncmlLocation, String referencedDatasetUri, CancelTask cancelTask) throws IOException {
    URL url = new URL(ncmlLocation);

    if (debugURL) {
      System.out.println(" NcMLReader open " + ncmlLocation);
      System.out.println("   URL = " + url.toString());
      System.out.println("   external form = " + url.toExternalForm());
      System.out.println("   protocol = " + url.getProtocol());
      System.out.println("   host = " + url.getHost());
      System.out.println("   path = " + url.getPath());
      System.out.println("  file = " + url.getFile());
    }

    org.jdom2.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
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

    if (referencedDatasetUri == null) {
      // the ncml probably refers to another dataset, but doesnt have to
      referencedDatasetUri = netcdfElem.getAttributeValue("location");
      if (referencedDatasetUri == null)
        referencedDatasetUri = netcdfElem.getAttributeValue("url");
    }

    NcMLReader reader = new NcMLReader();
    NetcdfDataset ncd = reader._readNcML(ncmlLocation, referencedDatasetUri, netcdfElem, cancelTask);
    if (debugOpen) System.out.println("***NcMLReader.readNcML result= \n" + ncd);
    return ncd;
  }

  /**
   * Read NcML doc from an InputStream, and construct a NetcdfDataset.
   *
   * @param ins        the InputStream containing the NcML document
   * @param cancelTask allow user to cancel the task; may be null
   * @return the resulting NetcdfDataset
   * @throws IOException on read error, or bad referencedDatasetUri URI
   */
  static public NetcdfDataset readNcML(InputStream ins, CancelTask cancelTask) throws IOException {

    org.jdom2.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      doc = builder.build(ins);
    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }
    if (debugXML) System.out.println(" SAXBuilder done");

    if (showParsedXML) {
      XMLOutputter xmlOut = new XMLOutputter();
      System.out.println("*** NetcdfDataset/showParsedXML = \n" + xmlOut.outputString(doc) + "\n*******");
    }

    Element netcdfElem = doc.getRootElement();
    NetcdfDataset ncd = readNcML(null, netcdfElem, cancelTask);
    if (debugOpen) System.out.println("***NcMLReader.readNcML (stream) result= \n" + ncd);
    return ncd;
  }

  /**
   * Read NcML doc from a Reader, and construct a NetcdfDataset.
   *
   * @param r          the Reader containing the NcML document
   * @param cancelTask allow user to cancel the task; may be null
   * @return the resulting NetcdfDataset
   * @throws IOException on read error, or bad referencedDatasetUri URI
   */
  static public NetcdfDataset readNcML(Reader r, CancelTask cancelTask) throws IOException {
    return readNcML(r, "NcMLReader", cancelTask);
  }

  /**
   * Read NcML doc from a Reader, and construct a NetcdfDataset.
   * eg: NcMLReader.readNcML(new StringReader(ncml), location, null);
   *
   * @param r            the Reader containing the NcML document
   * @param ncmlLocation the URL location string of the NcML document, used to resolve reletive path of the referenced dataset,
   *                     or may be just a unique name for caching purposes.
   * @param cancelTask   allow user to cancel the task; may be null
   * @return the resulting NetcdfDataset
   * @throws IOException on read error, or bad referencedDatasetUri URI
   */
  static public NetcdfDataset readNcML(Reader r, String ncmlLocation, CancelTask cancelTask) throws IOException {

    org.jdom2.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      doc = builder.build(r);
    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }
    if (debugXML) System.out.println(" SAXBuilder done");

    if (showParsedXML) {
      XMLOutputter xmlOut = new XMLOutputter();
      System.out.println("*** NetcdfDataset/showParsedXML = \n" + xmlOut.outputString(doc) + "\n*******");
    }

    Element netcdfElem = doc.getRootElement();
    NetcdfDataset ncd = readNcML(ncmlLocation, netcdfElem, cancelTask);
    if (debugOpen) System.out.println("***NcMLReader.readNcML (stream) result= \n" + ncd);
    return ncd;
  }

  /**
   * Read NcML from a JDOM Document, and construct a NetcdfDataset.
   *
   * @param ncmlLocation the URL location string of the NcML document, used to resolve reletive path of the referenced dataset,
   *                     or may be just a unique name for caching purposes.
   * @param netcdfElem   the JDOM Document's root (netcdf) element
   * @param cancelTask   allow user to cancel the task; may be null
   * @return the resulting NetcdfDataset
   * @throws IOException on read error, or bad referencedDatasetUri URI
   */
  static public NetcdfDataset readNcML(String ncmlLocation, Element netcdfElem, CancelTask cancelTask) throws IOException {
    // the ncml probably refers to another dataset, but doesnt have to
    String referencedDatasetUri = netcdfElem.getAttributeValue("location");
    if (referencedDatasetUri == null)
      referencedDatasetUri = netcdfElem.getAttributeValue("url");

    NcMLReader reader = new NcMLReader();
    return reader._readNcML(ncmlLocation, referencedDatasetUri, netcdfElem, cancelTask);
  }

  /**
   * Read NcML from a JDOM Document, and pass in the name of the dataset. Used to augment datasetScan with NcML
   *
   * @param ncmlLocation the URL location string of the NcML document, used as a unique name for caching purposes.
   * @param netcdfElem   the JDOM Document's root (netcdf) element
   * @param referencedDatasetUri the URL location string of the underlying dataset, which overrides anything in netcdfElem.
   *                             prepend with "file:" to eliminate reletive resolving against ncmlLocation
   * @param cancelTask   allow user to cancel the task; may be null
   * @return the resulting NetcdfDataset
   * @throws IOException on read error, or bad referencedDatasetUri URI
   */
  static public NetcdfDataset readNcML(String ncmlLocation, Element netcdfElem, String referencedDatasetUri, CancelTask cancelTask) throws IOException {
    NcMLReader reader = new NcMLReader();
    return reader._readNcML(ncmlLocation, referencedDatasetUri, netcdfElem, cancelTask);
  }

  //////////////////////////////////////////////////////////////////////////////////////
  private String location;
  private boolean explicit = false;
  private Formatter errlog = new Formatter();

  /**
   * This sets up the target dataset and the referenced dataset.
   * only place that iospParam is processed, so everything must go through here
   *
   * @param ncmlLocation         the URL location string of the NcML document, used to resolve reletive path of the referenced dataset,
   *                             or may be just a unique name for caching purposes.
   * @param referencedDatasetUri refers to this dataset (may be null)
   * @param netcdfElem           JDOM netcdf element
   * @param cancelTask           allow user to cancel the task; may be null
   * @return NetcdfDataset the constructed dataset
   * @throws IOException on read error, or bad referencedDatasetUri URI
   */
  private NetcdfDataset _readNcML(String ncmlLocation, String referencedDatasetUri, Element netcdfElem, CancelTask cancelTask) throws IOException {

    // get ncml namespace and set namespace variable
    this.ncNS = ncNSHttp;
    if (netcdfElem.getNamespaceURI().startsWith("https")) {
      this.ncNS = ncNSHttps;
    }

    // augment URI.resolve(), by also dealing with base file: URIs
    referencedDatasetUri = URLnaming.resolve(ncmlLocation, referencedDatasetUri);

    // common error causing infinite regression
    if ((referencedDatasetUri != null) && referencedDatasetUri.equals(ncmlLocation))
      throw new IllegalArgumentException("NcML location attribute refers to the NcML document itself" + referencedDatasetUri);

    // they can specify the iosp to use - but must be file based
    String iospS = netcdfElem.getAttributeValue("iosp");
    Object iospParam = netcdfElem.getAttributeValue("iospParam");
    if (iospParam == null) {
      // can pass iosp a JDOM tree
      iospParam = netcdfElem.getChild("iospParam", ncNS); // LOOK namespace ??
    }

    String bufferSizeS = netcdfElem.getAttributeValue("buffer_size");
    int buffer_size = -1;
    if (bufferSizeS != null)
      buffer_size = Integer.parseInt(bufferSizeS);

    // open the referenced dataset - do NOT use acquire, and dont enhance
    // LOOK : shouldnt enhance be controlled by enhance attribute on the netcdf element ?
    NetcdfDataset refds = null;
    if (referencedDatasetUri != null) {
      if (iospS != null) {
        NetcdfFile ncfile;
        try {
          ncfile = new NetcdfFileSubclass(iospS, iospParam, referencedDatasetUri, buffer_size, cancelTask);
        } catch (Exception e) {
          throw new IOException(e);
        }
        refds = new NetcdfDataset(ncfile, false);
      } else {
        //  String location, boolean enhance,              int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {
        // (String location, EnumSet<Enhance> enhanceMode, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {

        refds = NetcdfDataset.openDataset(referencedDatasetUri, null, buffer_size, cancelTask, iospParam);
        // refds.setEnhanceProcessed(false); // hasnt had enhance applied to it yet - wait till ncml mods have been applied
      }
    }

    // explicit means all of the metadata is specified in the XML, and the referenced dataset is used only for data access
    Element elemE = netcdfElem.getChild("explicit", ncNS);
    explicit = (elemE != null);

    // general idea is that we just modify the referenced dataset
    // the exception is when explicit is specified, then we keep them seperate.
    //                    refds != null               refds == null
    //  explicit            refds!=new                  new (ref=new)
    //  readMetadata        modify (new=ref)            new (ref=new)
    //
    NetcdfDataset targetDS;
    if (explicit || (refds == null)) {
      targetDS = new NetcdfDataset();
      if (refds == null)
        refds = targetDS;
      else
        targetDS.setReferencedFile(refds); // gotta set so it gets closed !!

    } else { // modify the referenced dataset directly
      targetDS = refds;
    }

    // continue processing here
    readNetcdf(ncmlLocation, targetDS, refds, netcdfElem, cancelTask);

    return targetDS;
  }

  ///////// Heres where the parsing work starts

  /**
   * parse a netcdf JDOM Element, and add contents to the targetDS NetcdfDataset.
   * <p/>
   * This is a bit tricky, because it handles several cases
   * When targetDS == refds, we are just modifying targetDS.
   * When targetDS != refds, we keep them seperate, and copy from refds to newds.
   * <p/>
   * The user may be defining new elements or modifying old ones. The only way to tell is by seeing
   * if the elements already exist.
   *
   * @param ncmlLocation NcML URL location, or may be just a unique name for caching purposes.
   * @param targetDS     add the info to this one, never null
   * @param refds        the referenced dataset; may equal newds, never null
   * @param netcdfElem   JDOM netcdf element
   * @param cancelTask   allow user to cancel the task; may be null
   * @throws IOException on read error
   */
  private void readNetcdf(String ncmlLocation, NetcdfDataset targetDS, NetcdfFile refds, Element netcdfElem, CancelTask cancelTask) throws IOException {
    this.location = ncmlLocation; // log messages need this

    if (debugOpen)
      System.out.println("NcMLReader.readNetcdf ncml= " + ncmlLocation + " referencedDatasetUri= " + refds.getLocation());

    // detect incorrect namespace
    Namespace use = netcdfElem.getNamespace();
    if (!use.equals(ncNS) && !use.equals(ncNSHttps)) {
      throw new IllegalArgumentException("Incorrect namespace specified in NcML= " + use.getURI() + "\n   must be=" + ncNS.getURI());
    }

    if (ncmlLocation != null) targetDS.setLocation(ncmlLocation);
    targetDS.setId(netcdfElem.getAttributeValue("id"));
    targetDS.setTitle(netcdfElem.getAttributeValue("title"));

    // aggregation first
    Element aggElem = netcdfElem.getChild("aggregation", ncNS);
    if (aggElem != null) {
      Aggregation agg = readAgg(aggElem, ncmlLocation, targetDS, cancelTask);
      targetDS.setAggregation(agg);
      agg.finish(cancelTask);
    }

    // the root group
    readGroup(targetDS, refds, null, null, netcdfElem);
    String errors = errlog.toString();
    if (errors.length() > 0)
      throw new IllegalArgumentException("NcML had fatal errors:" + errors);

    // transfer from groups to global containers
    targetDS.finish();

    // enhance means do scale/offset and/or add CoordSystems
    Set<NetcdfDataset.Enhance> mode = NetcdfDataset.parseEnhanceMode(netcdfElem.getAttributeValue("enhance"));
    //if (mode == null)
    //  mode = NetcdfDataset.getEnhanceDefault();
    targetDS.enhance(mode);

    // optionally add record structure to netcdf-3
    String addRecords = netcdfElem.getAttributeValue("addRecords");
    if ((addRecords != null) && addRecords.equalsIgnoreCase("true"))
      targetDS.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

  }

  ////////////////////////////////////////////////////////////////////////

  /**
   * Read an NcML attribute element.
   *
   * @param parent    Group or Variable
   * @param refParent Group or Variable in reference dataset
   * @param attElem   ncml attribute element
   */
  private void readAtt(Object parent, Object refParent, Element attElem) {
    String name = attElem.getAttributeValue("name");
    if (name == null) {
      errlog.format("NcML Attribute name is required (%s)%n", attElem);
      return;
    }
    String nameInFile = attElem.getAttributeValue("orgName");
    boolean newName = (nameInFile != null) && !nameInFile.equals(name);
    if (nameInFile == null)
      nameInFile = name;
    else if (null == findAttribute(refParent, nameInFile)) { // has to exists
      errlog.format("NcML attribute orgName '%s' doesnt exist. att=%s in=%s%n", nameInFile, name, parent);
      return;
    }

    // see if its new
    ucar.nc2.Attribute oldatt = findAttribute(refParent, nameInFile);
    if (oldatt == null) { // new
      if (debugConstruct) System.out.println(" add new att = " + name);
      try {
        ucar.ma2.Array values = readAttributeValues(attElem);
        addAttribute(parent, new ucar.nc2.Attribute(name, values));
      } catch (RuntimeException e) {
        errlog.format("NcML new Attribute Exception: %s att=%s in=%s%n", e.getMessage(), name, parent);
      }

    } else { // already exists

      if (debugConstruct) System.out.println(" modify existing att = " + name);
      boolean hasValue = attElem.getAttribute("value") != null;
      if (hasValue) {  // has a new value
        try {
          ucar.ma2.Array values = readAttributeValues(attElem);
          addAttribute(parent, new ucar.nc2.Attribute(name, values));
        } catch (RuntimeException e) {
          errlog.format("NcML existing Attribute Exception: %s att=%s in=%s%n", e.getMessage(), name, parent);
          return;
        }

      } else { // use the old values
        Array oldval = oldatt.getValues();
        if (oldval != null)
          addAttribute(parent, new ucar.nc2.Attribute(name, oldatt.getValues()));
        else {  // weird corner case of attribute with no value - must use the type
          String unS = attElem.getAttributeValue("isUnsigned");
          boolean isUnsigned =  unS != null && unS.equalsIgnoreCase("true");
          String typeS = attElem.getAttributeValue("type");
          DataType type = typeS == null ? DataType.STRING : DataType.getType(typeS);
          addAttribute(parent, new ucar.nc2.Attribute(name, type, isUnsigned));
        }
      }

      // remove the old one ??
      if (newName && !explicit) {
        removeAttribute(parent, oldatt);
        if (debugConstruct) System.out.println(" remove old att = " + nameInFile);
      }

    }
  }

  /**
   * Parse the values element
   *
   * @param s JDOM element to parse
   * @return Array with parsed values
   * @throws IllegalArgumentException if string values not parsable to specified data type
   */
  public static ucar.ma2.Array readAttributeValues(Element s) throws IllegalArgumentException {
    String valString = s.getAttributeValue("value");
    if (valString != null)
      valString = StringUtil2.unquoteXmlAttribute(valString);

    // can also be element text
    if (valString == null) {
      valString = s.getTextNormalize();
    }

    // no value specified  hmm technically this is not ilegal !!
    if (valString == null)
      throw new IllegalArgumentException("No value specified");

    String type = s.getAttributeValue("type");
    DataType dtype = (type == null) ? DataType.STRING : DataType.getType(type);
    if (dtype == DataType.CHAR) dtype = DataType.STRING;

    String sep = s.getAttributeValue("separator");
    if ((sep == null) && (dtype == DataType.STRING)) {
      List<String> list = new ArrayList<>();
      list.add(valString);
      return Array.makeArray(dtype, list);
    }

    if (sep == null) sep = " "; // default whitespace separated

    List<String> stringValues = new ArrayList<>();
    StringTokenizer tokn = new StringTokenizer(valString, sep);
    while (tokn.hasMoreTokens())
      stringValues.add(tokn.nextToken());

    return Array.makeArray(dtype, stringValues);
  }

  private ucar.nc2.Attribute findAttribute(Object parent, String name) {
    if (parent == null)
      return null;
    if (parent instanceof Group)
      return ((Group) parent).findAttribute(name);
    else if (parent instanceof Variable)
      return ((Variable) parent).findAttribute(name);
    return null;
  }

  private void addAttribute(Object parent, ucar.nc2.Attribute att) {
    if (parent instanceof Group)
      ((Group) parent).addAttribute(att);
    else if (parent instanceof Variable)
      ((Variable) parent).addAttribute(att);
  }

  private void removeAttribute(Object parent, Attribute att) {
    if (parent instanceof Group)
      ((Group) parent).remove(att);
    else if (parent instanceof Variable)
      ((Variable) parent).remove(att);
  }

  /**
   * Read an NcML dimension element.
   *
   * @param g       put dimension into this group
   * @param refg    parent Group in referenced dataset
   * @param dimElem ncml dimension element
   */
  private void readDim(Group g, Group refg, Element dimElem) {
    String name = dimElem.getAttributeValue("name");
    if (name == null) {
      errlog.format("NcML Dimension name is required (%s)%n", dimElem);
      return;
    }

    String nameInFile = dimElem.getAttributeValue("orgName");
    if (nameInFile == null) nameInFile = name;

    // see if it already exists
    Dimension dim = (refg == null) ? null : refg.findDimension(nameInFile);
    if (dim == null) { // nope - create it
      String lengthS = dimElem.getAttributeValue("length");
      if (lengthS == null) {
        errlog.format("NcML Dimension length is required (%s)%n", dimElem);
        return;
      }

      String isUnlimitedS = dimElem.getAttributeValue("isUnlimited");
      String isSharedS = dimElem.getAttributeValue("isShared");
      String isUnknownS = dimElem.getAttributeValue("isVariableLength");

      boolean isUnlimited = (isUnlimitedS != null) && isUnlimitedS.equalsIgnoreCase("true");
      boolean isUnknown = (isUnknownS != null) && isUnknownS.equalsIgnoreCase("true");
      boolean isShared = true;
      if ((isSharedS != null) && isSharedS.equalsIgnoreCase("false"))
        isShared = false;

      int len = Integer.parseInt(lengthS);
      if (isUnknown)
        len = Dimension.VLEN.getLength();

      if (debugConstruct) System.out.println(" add new dim = " + name);
      g.addDimension(new Dimension(name, len, isShared, isUnlimited, isUnknown));

    } else { // yes - modify it
      dim.setName(name);

      String lengthS = dimElem.getAttributeValue("length");
      String isUnlimitedS = dimElem.getAttributeValue("isUnlimited");
      String isSharedS = dimElem.getAttributeValue("isShared");
      String isUnknownS = dimElem.getAttributeValue("isVariableLength");

      if (isUnlimitedS != null)
        dim.setUnlimited(isUnlimitedS.equalsIgnoreCase("true"));

      if (isSharedS != null)
        dim.setShared(!isSharedS.equalsIgnoreCase("false"));

      if (isUnknownS != null)
        dim.setVariableLength(isUnknownS.equalsIgnoreCase("true"));

      if ((lengthS != null) && !dim.isVariableLength()) {
        int len = Integer.parseInt(lengthS);
        dim.setLength(len);
      }

      if (debugConstruct) System.out.println(" modify existing dim = " + name);

      if (g != refg) // explicit, copy to new
        g.addDimension(dim);
    }
  }

  /**
   * Read an NcML enumTypedef element.
   *
   * @param g       put enumTypedef into this group
   * @param refg    parent Group in referenced dataset
   * @param etdElem ncml enumTypedef element
   */
  private void readEnumTypedef(Group g, Group refg, Element etdElem) {
    String name = etdElem.getAttributeValue("name");
    if (name == null) {
      errlog.format("NcML enumTypedef name is required (%s)%n", etdElem);
      return;
    }
    String typeS = etdElem.getAttributeValue("type");
    DataType baseType = (typeS == null) ? DataType.ENUM1 : DataType.getType(typeS);

    Map<Integer, String> map = new HashMap<>(100);
    for (Element e : etdElem.getChildren("enum", ncNS)) {
      String key = e.getAttributeValue("key");
      String value = e.getTextNormalize();
      if (key == null)
        errlog.format("NcML enumTypedef enum key attribute is required (%s)%n", e);
      if (value == null)
        errlog.format("NcML enumTypedef enum value is required (%s)%n", e);
      try {
        int keyi = Integer.parseInt(key);
        map.put(keyi, value);
      } catch (Exception e2) {
        errlog.format("NcML enumTypedef enum key attribute not an integer (%s)%n", e);
      }
    }

    EnumTypedef td = new EnumTypedef(name, map, baseType);
    g.addEnumeration(td);

  }

  /**
   * Read the NcML group element, and nested elements.
   *
   * @param newds     new dataset
   * @param refds     referenced dataset
   * @param parent    Group
   * @param refParent parent Group in referenced dataset
   * @param groupElem ncml group element
   */
  private void readGroup(NetcdfDataset newds, NetcdfFile refds, Group parent, Group refParent, Element groupElem) throws IOException {

    Group g, refg = null;
    if (parent == null) { // this is the <netcdf> element
      g = newds.getRootGroup();
      refg = refds.getRootGroup();
      if (debugConstruct) System.out.println(" root group ");

    } else {

      String name = groupElem.getAttributeValue("name");
      if (name == null) {
        errlog.format("NcML Group name is required (%s)%n", groupElem);
        return;
      }

      String nameInFile = groupElem.getAttributeValue("orgName");
      if (nameInFile == null) nameInFile = name;

      // see if it exists in referenced dataset
      if (refParent != null)
        refg = refParent.findGroup(nameInFile);
      if (refg == null) { // new
        g = new Group(newds, parent, name);
        parent.addGroup(g);
        if (debugConstruct) System.out.println(" add new group = " + name);

      } else {

        if (parent != refParent) { // explicit
          g = new Group(newds, parent, name);
          parent.addGroup(g);
          if (debugConstruct) System.out.println(" transfer existing group = " + name);

        } else { // modify
          g = refg;
          if (!nameInFile.equals(name))
            g.setName(name);

          if (debugConstruct) System.out.println(" modify existing group = " + name);
        }
      }
    }

    // look for attributes
    java.util.List<Element> attList = groupElem.getChildren("attribute", ncNS);
    for (Element attElem : attList) {
      readAtt(g, refg, attElem);
    }

    // look for enumTypedef
    java.util.List<Element> etdList = groupElem.getChildren("enumTypedef", ncNS);
    for (Element elem : etdList) {
      readEnumTypedef(g, refg, elem);
    }

    // look for dimensions
    java.util.List<Element> dimList = groupElem.getChildren("dimension", ncNS);
    for (Element dimElem : dimList) {
      readDim(g, refg, dimElem);
    }

    // look for variables
    java.util.List<Element> varList = groupElem.getChildren("variable", ncNS);
    for (Element varElem : varList) {
      readVariable(newds, g, refg, varElem);
    }

    // process remove command
    java.util.List<Element> removeList = groupElem.getChildren("remove", ncNS);
    for (Element e : removeList) {
      cmdRemove(g, e.getAttributeValue("type"), e.getAttributeValue("name"));
    }

    // look for nested groups
    java.util.List<Element> groupList = groupElem.getChildren("group", ncNS);
    for (Element gElem : groupList) {
      readGroup(newds, refds, g, refg, gElem);
      if (debugConstruct) System.out.println(" add group = " + g.getFullName());
    }
  }

  /* private boolean debugView = false, debugConvert = false;
  protected VariableDS readVariable2( NetcdfDataset ds, Element varElem) {
    VariableDS v = readVariable( ds, varElem);

    // look for logical views
    java.util.List viewList = varElem.getChildren("logicalView", ncNS);
    for (int j=0; j< viewList.size(); j++) {
      Element viewElem = (Element) viewList.get(j);
      String value = viewElem.getAttributeValue("section");
      if (value != null) {
        v.setLogicalView("section", value);
        if (debugView) System.out.println("set view = "+value);
      }
    }

    // look for unit conversion
    Element unitElem = varElem.getChild(CDM.UNITS, ncNS);
    if (unitElem != null) {
      String value = unitElem.getAttributeValue("convertTo");
      if (value != null) {
        v.setConvertUnit(value);
        if (debugConvert) System.out.println("setConvertUnit on "+v.getName()+" to <" + value+">");
      }
    }

    return v;
     } */

  /**
   * Read the NcML variable element, and nested elements.
   *
   * @param ds      target dataset
   * @param g       parent Group
   * @param refg    referenced dataset parent Group - may be same (modify) or different (explicit)
   * @param varElem ncml variable element
   * @throws java.io.IOException on read error
   */
  private void readVariable(NetcdfDataset ds, Group g, Group refg, Element varElem) throws IOException {
    String name = varElem.getAttributeValue("name");
    if (name == null) {
      errlog.format("NcML Variable name is required (%s)%n", varElem);
      return;
    }

    String nameInFile = varElem.getAttributeValue("orgName");
    if (nameInFile == null) nameInFile = name;

    // see if it already exists
    Variable refv = (refg == null) ? null : refg.findVariable(nameInFile);
    if (refv == null) { // new
      if (debugConstruct) System.out.println(" add new var = " + name);
      g.addVariable(readVariableNew(ds, g, null, varElem));
      return;
    }

    // exists already
    DataType dtype;
    String typeS = varElem.getAttributeValue("type");
    if (typeS != null)
      dtype = DataType.getType(typeS);
    else
      dtype = refv.getDataType();

    EnumTypedef typedef = null;
    if (dtype.isEnum()) {
      String typedefS = varElem.getAttributeValue("typedef");
      if (typedefS != null)
        typedef = g.findEnumeration(typedefS);
    }

    String shape = varElem.getAttributeValue("shape");

    Variable v;
    if (refg == g) { // modify
      v = refv;
      v.setName(name);
      /* if (dtype != v.getDataType() && v.hasCachedData()) {
        Array data = v.read();
        Array newData = Array.factory(dtype, v.getShape());
        MAMath.copy(newData, data);
        v.setCachedData(newData, false);
      } */
      v.setDataType(dtype);
      if (typedef != null)
        v.setEnumTypedef(typedef);

      if (shape != null)
        v.setDimensions(shape); // LOOK check conformable
      if (debugConstruct) System.out.println(" modify existing var = " + nameInFile);

    } else { //explicit - create new
      if (refv instanceof Structure) {
        v = new StructureDS(ds, g, null, name, (Structure) refv);
        v.setDimensions(shape);
       } else {
        v = new VariableDS(g, null, name, refv);
        v.setDataType(dtype);
        v.setDimensions(shape);
      }
      if (debugConstruct) System.out.println(" modify explicit var = " + nameInFile);
      g.addVariable(v);
    }

    java.util.List<Element> attList = varElem.getChildren("attribute", ncNS);
    for (Element attElem : attList) {
      readAtt(v, refv, attElem);
    }

    // process remove command
    java.util.List<Element> removeList = varElem.getChildren("remove", ncNS);
    for (Element remElem : removeList) {
      cmdRemove(v, remElem.getAttributeValue("type"), remElem.getAttributeValue("name"));
    }

    if (v.getDataType() == DataType.STRUCTURE) {
      // deal with nested variables
      StructureDS s = (StructureDS) v;
      StructureDS refS = (StructureDS) refv;
      java.util.List<Element> varList = varElem.getChildren("variable", ncNS);
      for (Element vElem : varList) {
        readVariableNested(ds, s, refS, vElem);
      }

    } else {

      // deal with values
      Element valueElem = varElem.getChild("values", ncNS);
      if (valueElem != null) {
        readValues(ds, v, varElem, valueElem);

      } else {
        // see if  we need to munge existing data. use case : aggregation
        if (v.hasCachedData()) {
          Array data;
          try {
            data = v.read();
          } catch (IOException e) {
            throw new IllegalStateException(e.getMessage());
          }
          if (data.getClass() != v.getDataType().getPrimitiveClassType()) {
            Array newData = Array.factory(v.getDataType(), v.getShape());
            MAMath.copy(newData, data);
            v.setCachedData(newData, false);
          }
        }
      }
    }

    // look for logical views
    processLogicalViews(varElem, v, g);
  }

  private void processLogicalViews(Element varElem, Variable v, Group g) {

    Element viewElem = varElem.getChild("logicalSection", ncNS);
    if (null != viewElem) {
      String sectionSpec = viewElem.getAttributeValue("section");
      if (sectionSpec != null) {
        try {
          Section s = new Section(sectionSpec); // parse spec
          Section viewSection = Section.fill(s, v.getShape());
          // check that its a subset
          if (!v.getShapeAsSection().contains(viewSection)) {
            errlog.format("Invalid logicalSection on variable=%s section =(%s) original=(%s) %n", v.getFullName(), sectionSpec, v.getShapeAsSection());
            return;
          }
          Variable view = v.section(viewSection);
          g.removeVariable(v.getShortName());
          g.addVariable(view);

        } catch (InvalidRangeException e) {
          errlog.format("Invalid logicalSection on variable=%s section=(%s) error=%s %n", v.getFullName(), sectionSpec, e.getMessage());
          return;
        }
      }
    }

    viewElem = varElem.getChild("logicalSlice", ncNS);
    if (null != viewElem) {
      String dimName = viewElem.getAttributeValue("dimName");
      if (null == dimName) {
        errlog.format("NcML logicalSlice: dimName is required, variable=%s %n", v.getFullName());
        return;
      }
      int dim = v.findDimensionIndex(dimName);
      if (dim < 0) {
        errlog.format("NcML logicalSlice: cant find dimension %s in variable=%s %n", dimName, v.getFullName());
        return;
      }

      String indexS = viewElem.getAttributeValue("index");
      int index;
      if (null == indexS) {
        errlog.format("NcML logicalSlice: index is required, variable=%s %n", v.getFullName());
        return;
      }
      try {
        index = Integer.parseInt(indexS);
      } catch (NumberFormatException e) {
        errlog.format("NcML logicalSlice: index=%s must be integer, variable=%s %n", indexS, v.getFullName());
        return;
      }

      try {
        Variable view = v.slice(dim, index);
        g.removeVariable(v.getShortName());
        g.addVariable(view);

      } catch (InvalidRangeException e) {
        errlog.format("Invalid logicalSlice (%d,%d) on variable=%s error=%s %n", dim, index, v.getFullName(), e.getMessage());
      }
    }

    viewElem = varElem.getChild("logicalReduce", ncNS);
    if (null != viewElem) {
      String dimName = viewElem.getAttributeValue("dimNames");
      if (null == dimName) {
        errlog.format("NcML logicalReduce: dimNames is required, variable=%s %n", v.getFullName());
        return;
      }
      String[] dims = StringUtil2.splitString(dimName);
      List<Dimension> dimList = new ArrayList<>();
      for (String s : dims) {
        int idx = v.findDimensionIndex(s);
        if (idx < 0) {
          errlog.format("NcML logicalReduce: cant find dimension %s in variable=%s %n", dimName, v.getFullName());
          return;
        }
        dimList.add(v.getDimension(idx));
      }

      try {
        Variable view = v.reduce(dimList);
        g.removeVariable(v.getShortName());
        g.addVariable(view);

      } catch (InvalidRangeException e) {
        errlog.format("Failed logicalReduce (%s) on variable=%s error=%s %n", dimName, v.getFullName(), e.getMessage());
      }

    }

  }

  /**
   * Read a NcML variable element, and nested elements, when it creates a new Variable.
   *
   * @param ds      target dataset
   * @param g       parent Group
   * @param parentS parent Structure
   * @param varElem ncml variable element
   * @return return new Variable
   */
  private Variable readVariableNew(NetcdfDataset ds, Group g, Structure parentS, Element varElem) {
    String name = varElem.getAttributeValue("name");
    if (name == null) {
      errlog.format("NcML Variable name is required (%s)%n", varElem);
      return null;
    }

    String type = varElem.getAttributeValue("type");
    if (type == null)
      throw new IllegalArgumentException("New variable (" + name + ") must have datatype attribute");
    DataType dtype = DataType.getType(type);

    EnumTypedef typedef = null;
    if (dtype.isEnum()) {
      String typedefS = varElem.getAttributeValue("typedef");
      if (typedefS != null)
        typedef = g.findEnumeration(typedefS);
    }

    String shape = varElem.getAttributeValue("shape");
    if (shape == null)
      shape = ""; // deprecated, prefer explicit ""

    Variable v;

    if (dtype == DataType.STRUCTURE) {
      StructureDS s = new StructureDS(ds, g, parentS, name, shape, null, null);
      v = s;
      // look for nested variables
      java.util.List<Element> varList = varElem.getChildren("variable", ncNS);
      for (Element vElem : varList) {
        readVariableNested(ds, s, s, vElem);
      }

    } else if (dtype == DataType.SEQUENCE) {
        Sequence org = new Sequence(ds, g, parentS, name);
        SequenceDS s = new SequenceDS(g, org); // barf
        v = s;
        // look for nested variables
        java.util.List<Element> varList = varElem.getChildren("variable", ncNS);
        for (Element vElem : varList) {
          readVariableNested(ds, s, s, vElem);
        }

    } else {
      v = new VariableDS(ds, g, parentS, name, dtype, shape, null, null);

      // deal with values
      Element valueElem = varElem.getChild("values", ncNS);
      if (valueElem != null)
        readValues(ds, v, varElem, valueElem);
      // otherwise has fill values.
    }

    // look for attributes
    java.util.List<Element> attList = varElem.getChildren("attribute", ncNS);
    for (Element attElem : attList)
      readAtt(v, null, attElem);

    if (typedef != null)
      v.setEnumTypedef(typedef);

    /* now that we have attributes finalized, redo the enhance
    if (enhance && (v instanceof VariableDS))
      ((VariableDS) v).enhance(); */

    return v;
  }

  /**
   * Read the NcML variable element, and nested elements.
   *
   * @param ds        target dataset
   * @param parentS   parent Structure
   * @param refStruct reference dataset structure
   * @param varElem   ncml variable element
   */
  private void readVariableNested(NetcdfDataset ds, Structure parentS, Structure refStruct, Element varElem) {
    String name = varElem.getAttributeValue("name");
    if (name == null) {
      errlog.format("NcML Variable name is required (%s)%n", varElem);
      return;
    }

    String nameInFile = varElem.getAttributeValue("orgName");
    if (nameInFile == null) nameInFile = name;

    // see if it already exists
    Variable refv = refStruct.findVariable(nameInFile);
    if (refv == null) { // new
      if (debugConstruct) System.out.println(" add new var = " + name);
      Variable nested = readVariableNew(ds, parentS.getParentGroup(), parentS, varElem);
      if (nested != null) parentS.addMemberVariable(nested);
      return;
    }

    Variable v;
    if (parentS == refStruct) { // modify
      v = refv;
      v.setName(name);

    } else { //explicit
      if (refv instanceof Structure) {
        v = new StructureDS(parentS.getParentGroup(), (Structure) refv); // true
        v.setName(name);
        v.setParentStructure(parentS);
      } else {
        v = new VariableDS(parentS.getParentGroup(), refv, false);
        v.setName(name);
        v.setParentStructure(parentS);
      }

      /* if (refv instanceof Structure) {
        v = new StructureDS(ds, parentS.getParentGroup(), parentS, name, refv.getDimensionsString(), null, null);
      } else {
        v = new VariableDS(ds, parentS.getParentGroup(), parentS, name, refv.getDataType(), refv.getDimensionsString(), null, null);
      }
      v.setIOVar(refv);  */
      parentS.addMemberVariable(v);
    }

    if (debugConstruct) System.out.println(" modify existing var = " + nameInFile);

    String typeS = varElem.getAttributeValue("type");
    if (typeS != null) {
      DataType dtype = DataType.getType(typeS);
      v.setDataType(dtype);
    }

    String shape = varElem.getAttributeValue("shape");
    if (shape != null) {
      v.setDimensions(shape);
    }

    java.util.List<Element> attList = varElem.getChildren("attribute", ncNS);
    for (Element attElem : attList) {
      readAtt(v, refv, attElem);
    }

    // process remove command
    java.util.List<Element> removeList = varElem.getChildren("remove", ncNS);
    for (Element remElem : removeList) {
      cmdRemove(v, remElem.getAttributeValue("type"), remElem.getAttributeValue("name"));
    }

    if ((v.getDataType() == DataType.STRUCTURE) || (v.getDataType() == DataType.SEQUENCE)) {
      // deal with nested variables
      StructureDS s = (StructureDS) v;
      StructureDS refS = (StructureDS) refv;
      java.util.List<Element> varList = varElem.getChildren("variable", ncNS);
      for (Element vElem : varList) {
        readVariableNested(ds, s, refS, vElem);
      }

    } else {

      // deal with values
      Element valueElem = varElem.getChild("values", ncNS);
      if (valueElem != null)
        readValues(ds, v, varElem, valueElem);
    }

    /* now that we have attributes finalized, redo the enhance
    if (enhance && (v instanceof VariableDS))
      ((VariableDS) v).enhance(); */
  }

  private void readValues(NetcdfDataset ds, Variable v, Element varElem, Element valuesElem) {
    try {

      // check if values are specified by attribute
      String fromAttribute = valuesElem.getAttributeValue("fromAttribute");
      if (fromAttribute != null) {
        Attribute att;
        int pos = fromAttribute.indexOf('@'); // varName@attName
        if (pos > 0) {
          String varName = fromAttribute.substring(0, pos);
          String attName = fromAttribute.substring(pos + 1);
          Variable vFrom = ds.getRootGroup().findVariable(varName); // LOOK groups
          if (vFrom == null) {
            errlog.format("Cant find variable %s %n", fromAttribute);
            return;
          }
          att = vFrom.findAttribute(attName);

        } else {  // attName or @attName
          String attName = (pos == 0) ? fromAttribute.substring(1) : fromAttribute;
          att = ds.getRootGroup().findAttribute(attName);
        }
        if (att == null) {
          errlog.format("Cant find attribute %s %n", fromAttribute);
          return;
        }
        Array data = att.getValues();
        v.setCachedData(data, true);
        return;
      }

      // check if values are specified by start / increment
      String startS = valuesElem.getAttributeValue("start");
      String incrS = valuesElem.getAttributeValue("increment");
      String nptsS = valuesElem.getAttributeValue("npts");
      int npts = (nptsS == null) ? (int) v.getSize() : Integer.parseInt(nptsS);

      // either start, increment are specified
      if ((startS != null) && (incrS != null)) {
        double start = Double.parseDouble(startS);
        double incr = Double.parseDouble(incrS);
        v.setValues(npts, start, incr);
        return;
      }

      // otherwise values are listed in text
      String values = varElem.getChildText("values", ncNS);
      String sep = valuesElem.getAttributeValue("separator");

      if (v.getDataType() == DataType.CHAR) {
        int nhave = values.length();
        int nwant = (int) v.getSize();
        char[] data = new char[nwant];
        int min = Math.min(nhave, nwant);
        for (int i = 0; i < min; i++) {
          data[i] = values.charAt(i);
        }
        Array dataArray = Array.factory(DataType.CHAR.getPrimitiveClassType(), v.getShape(), data);
        v.setCachedData(dataArray, true);

      } else {
        List<String> valList = getTokens(values, sep);
        v.setValues(valList);
      }

    } catch (Throwable t) {
      throw new RuntimeException("NCML Reading on " + v.getFullName(), t);
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////

  private Aggregation readAgg(Element aggElem, String ncmlLocation, NetcdfDataset newds, CancelTask cancelTask) throws IOException {
    String dimName = aggElem.getAttributeValue("dimName");
    String type = aggElem.getAttributeValue("type");
    String recheck = aggElem.getAttributeValue("recheckEvery");

    Aggregation agg;
    if (type.equalsIgnoreCase("joinExisting")) {
      agg = new AggregationExisting(newds, dimName, recheck);

    } else if (type.equalsIgnoreCase("joinNew")) {
      agg = new AggregationNew(newds, dimName, recheck);

    } else if (type.equalsIgnoreCase("tiled")) {
      agg = new AggregationTiled(newds, dimName, recheck);

    } else if (type.equalsIgnoreCase("union")) {
      agg = new AggregationUnion(newds, dimName, recheck);

    } else if (type.equalsIgnoreCase("forecastModelRunCollection") || type.equalsIgnoreCase("forecastModelRunSingleCollection")) {
      AggregationFmrc aggc = new AggregationFmrc(newds, dimName, recheck);
      agg = aggc;

      // nested scanFmrc elements
      java.util.List<Element> scan2List = aggElem.getChildren("scanFmrc", ncNS);
      for (Element scanElem : scan2List) {
        String dirLocation = scanElem.getAttributeValue("location");
        String regexpPatternString = scanElem.getAttributeValue("regExp");
        String suffix = scanElem.getAttributeValue("suffix");
        String subdirs = scanElem.getAttributeValue("subdirs");
        String olderS = scanElem.getAttributeValue("olderThan");

        String runMatcher = scanElem.getAttributeValue("runDateMatcher");
        String forecastMatcher = scanElem.getAttributeValue("forecastDateMatcher");
        String offsetMatcher = scanElem.getAttributeValue("forecastOffsetMatcher");

        // possible relative location
        dirLocation = URLnaming.resolve(ncmlLocation, dirLocation);

        aggc.addDirectoryScanFmrc(dirLocation, suffix, regexpPatternString, subdirs, olderS, runMatcher, forecastMatcher, offsetMatcher);

        if ((cancelTask != null) && cancelTask.isCancel())
          return null;
        if (debugAggDetail) System.out.println(" debugAgg: nested dirLocation = " + dirLocation);
      }

    } else {
      throw new IllegalArgumentException("Unknown aggregation type=" + type);
    }

    if (agg instanceof AggregationOuterDimension) {
      AggregationOuterDimension aggo = (AggregationOuterDimension) agg;

      String timeUnitsChange = aggElem.getAttributeValue("timeUnitsChange");
      if (timeUnitsChange != null)
        aggo.setTimeUnitsChange(timeUnitsChange.equalsIgnoreCase("true"));

      // look for variables that need to be aggregated (aggNew)
      java.util.List<Element> list = aggElem.getChildren("variableAgg", ncNS);
      for (Element vaggElem : list) {
        String varName = vaggElem.getAttributeValue("name");
        aggo.addVariable(varName);
      }

      // look for attributes to promote to variables
      list = aggElem.getChildren("promoteGlobalAttribute", ncNS);
      for (Element gattElem : list) {
        String varName = gattElem.getAttributeValue("name");
        String orgName = gattElem.getAttributeValue("orgName");
        aggo.addVariableFromGlobalAttribute(varName, orgName);
      }

      // look for attributes to promote to variables
      list = aggElem.getChildren("promoteGlobalAttributeCompose", ncNS);
      for (Element gattElem : list) {
        String varName = gattElem.getAttributeValue("name");
        String format = gattElem.getAttributeValue("format");
        String orgName = gattElem.getAttributeValue("orgName");
        aggo.addVariableFromGlobalAttributeCompose(varName, format, orgName);
      }

      // look for variable to cache
      list = aggElem.getChildren("cacheVariable", ncNS);
      for (Element gattElem : list) {
        String varName = gattElem.getAttributeValue("name");
        aggo.addCacheVariable(varName, null);
      }
    }

    // nested netcdf elements
    java.util.List<Element> ncList = aggElem.getChildren("netcdf", ncNS);
    for (Element netcdfElemNested : ncList) {
      String location = netcdfElemNested.getAttributeValue("location");
      if (location == null)
        location = netcdfElemNested.getAttributeValue("url");

      String id = netcdfElemNested.getAttributeValue("id");
      String ncoords = netcdfElemNested.getAttributeValue("ncoords");
      String coordValueS = netcdfElemNested.getAttributeValue("coordValue");
      String sectionSpec = netcdfElemNested.getAttributeValue("section");

      // must always open through a NcML reader, in case the netcdf element modifies the dataset
      NcmlElementReader reader = new NcmlElementReader(ncmlLocation, location, netcdfElemNested);
      String cacheName = (location != null) ? location : ncmlLocation;
      cacheName += "#" + Integer.toString(netcdfElemNested.hashCode()); // need a unique name, in case file has been modified by ncml

      String realLocation = URLnaming.resolveFile(ncmlLocation, location);
      agg.addExplicitDataset(cacheName, realLocation, id, ncoords, coordValueS, sectionSpec, reader);

      if ((cancelTask != null) && cancelTask.isCancel())
        return null;
      if (debugAggDetail) System.out.println(" debugAgg: nested dataset = " + location);
    }

    // nested scan elements
    java.util.List<Element> dirList = aggElem.getChildren("scan", ncNS);
    for (Element scanElem : dirList) {
      String dirLocation = scanElem.getAttributeValue("location");
      String regexpPatternString = scanElem.getAttributeValue("regExp");
      String suffix = scanElem.getAttributeValue("suffix");
      String subdirs = scanElem.getAttributeValue("subdirs");
      String olderS = scanElem.getAttributeValue("olderThan");

      String dateFormatMark = scanElem.getAttributeValue("dateFormatMark");
      Set<NetcdfDataset.Enhance> enhanceMode = NetcdfDataset.parseEnhanceMode(scanElem.getAttributeValue("enhance"));

      // possible relative location
      dirLocation = URLnaming.resolve(ncmlLocation, dirLocation);

      // can embed a full-blown crawlableDatasetImpl element
      Element cdElement = scanElem.getChild("crawlableDatasetImpl", ncNS);  // ok if null
      agg.addDatasetScan(cdElement, dirLocation, suffix, regexpPatternString, dateFormatMark, enhanceMode, subdirs, olderS);

      if ((cancelTask != null) && cancelTask.isCancel())
        return null;
      if (debugAggDetail) System.out.println(" debugAgg: nested dirLocation = " + dirLocation);
    }

    // experimental
    Element collElem = aggElem.getChild("collection", ncNS);
    if (collElem != null)
      agg.addCollection(collElem.getAttributeValue("spec"), collElem.getAttributeValue("olderThan"));

    /* <!-- experimental - modify each dataset in aggregation  -->
        <xsd:choice minOccurs="0" maxOccurs="unbounded">
          <xsd:element ref="group"/>
          <xsd:element ref="dimension"/>
          <xsd:element ref="variable"/>
          <xsd:element ref="attribute"/>
          <xsd:element ref="remove"/>
        </xsd:choice> */
    boolean needMerge = aggElem.getChildren("attribute", ncNS).size() > 0;
    if (!needMerge) needMerge = aggElem.getChildren("variable", ncNS).size() > 0;
    if (!needMerge) needMerge = aggElem.getChildren("dimension", ncNS).size() > 0;
    if (!needMerge) needMerge = aggElem.getChildren("group", ncNS).size() > 0;
    if (!needMerge) needMerge = aggElem.getChildren("remove", ncNS).size() > 0;
    if (needMerge)
      agg.setModifications(aggElem);

    return agg;
  }

  private class NcmlElementReader implements ucar.nc2.util.cache.FileFactory {
    private Element netcdfElem;
    private String ncmlLocation, location;

    NcmlElementReader(String ncmlLocation, String location, Element netcdfElem) {
      this.ncmlLocation = ncmlLocation;
      this.location = location;
      this.netcdfElem = netcdfElem;
    }

    public NetcdfFile open(String cacheName, int buffer_size, CancelTask cancelTask, Object spiObject) throws IOException {
      if (debugAggDetail) System.out.println(" NcmlElementReader open nested dataset " + cacheName);
      NetcdfFile result = _readNcML(ncmlLocation, location, netcdfElem, cancelTask);
      result.setLocation(ncmlLocation + "#" + location);
      return result;
    }
  }

  /////////////////////////////////////////////
  // command procesing

  private void cmdRemove(Group g, String type, String name) {
    boolean err = false;
    switch (type) {
      case "dimension":
        Dimension dim = g.findDimension(name);
        if (dim != null) {
          g.remove(dim);
          if (debugCmd) System.out.println("CMD remove " + type + " " + name);
        } else
          err = true;

        break;
      case "variable":
        Variable v = g.findVariable(name);
        if (v != null) {
          g.remove(v);
          if (debugCmd) System.out.println("CMD remove " + type + " " + name);
        } else
          err = true;

        break;
      case "attribute":
        Attribute a = g.findAttribute(name);
        if (a != null) {
          g.remove(a);
          if (debugCmd) System.out.println("CMD remove " + type + " " + name);
        } else
          err = true;
        break;
    }

    if (err) {
      Formatter f = new Formatter();
      f.format("CMD remove %s CANT find %s location %s%n", type, name, location);
      log.info(f.toString());
    }
  }

  private void cmdRemove(Variable v, String type, String name) {
    boolean err = false;

    if (type.equals("attribute")) {
      ucar.nc2.Attribute a = v.findAttribute(name);
      if (a != null) {
        v.remove(a);
        if (debugCmd) System.out.println("CMD remove " + type + " " + name);
      } else
        err = true;

    } else if (type.equals("variable") && v instanceof Structure) {
      Structure s = (Structure) v;
      Variable nested = s.findVariable(name);
      if (nested != null) {
        s.removeMemberVariable(nested);
        if (debugCmd) System.out.println("CMD remove " + type + " " + name);
      } else
        err = true;

    }

    if (err) {
      Formatter f = new Formatter();
      f.format("CMD remove %s CANT find %s location %s%n", type, name, location);
      log.info(f.toString());
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////

  /**
   * Read an NcML file and write an equivalent NetcdfFile to a physical file, using Netcdf-3 file format.
   *
   * @param ncmlLocation read this NcML file
   * @param fileOutName  write to this local file
   * @throws IOException on write error
   * @see ucar.nc2.FileWriter2
   */
  public static void writeNcMLToFile(String ncmlLocation, String fileOutName) throws IOException {
    NetcdfFile ncd = NetcdfDataset.acquireFile(ncmlLocation, null);

    FileWriter2 writer = new FileWriter2(ncd, fileOutName, NetcdfFileWriter.Version.netcdf3, null);
    NetcdfFile result = writer.write();
    result.close();
    ncd.close();
  }

  /**
   * Read an NcML and write an equivalent NetcdfFile to a physical file, using Netcdf-3 file format.
   * The NcML may have a referenced dataset in the location URL, in which case the underlying data
   * (modified by the NcML) is written to the new file. If the NcML does not have a referenced dataset,
   * then the new file is filled with fill values, like ncgen.
   *
   * @param ncml        read NcML from this input stream
   * @param fileOutName write to this local file
   * @throws IOException on error
   * @see ucar.nc2.FileWriter2
   */
  public static void writeNcMLToFile(InputStream ncml, String fileOutName) throws IOException {
    writeNcMLToFile(ncml, fileOutName, NetcdfFileWriter.Version.netcdf3, null);
  }

  /**
   * Read an NcML and write an equivilent NetcdfFile to a physical file, using Netcdf-3 file format.
   * The NcML may have a referenced dataset in the location URL, in which case the underlying data
   * (modified by the NcML) is written to the new file. If the NcML does not have a referenced dataset,
   * then the new file is filled with fill values, like ncgen.
   *
   * @param ncml        read NcML from this input stream
   * @param fileOutName write to this local file
   * @param version     kind of netcdf file
   * @param chunker     optional chunking (netcdf4 only)
   * @throws IOException
   */
  public static void writeNcMLToFile(InputStream ncml, String fileOutName, NetcdfFileWriter.Version version, Nc4Chunking chunker) throws IOException {
    NetcdfDataset ncd = NcMLReader.readNcML(ncml, null);
    FileWriter2 writer = new FileWriter2(ncd, fileOutName, version, chunker);
    NetcdfFile result = writer.write();
    result.close();
    ncd.close();
  }

  public static void main(String arg[]) {
    String ncmlFile = "C:/data/AStest/oots/test.ncml";
    String ncmlFileOut = "C:/TEMP/testNcmlOut.nc";
    try {
      //NetcdfDataset ncd = NcMLReader.readNcML (ncmlFile, null);
      //ncd.writeNcMLG(System.out, true, null);
      //System.out.println("NcML = "+ncmlFile);
      InputStream in = new FileInputStream(ncmlFile);
      writeNcMLToFile(in, ncmlFileOut);

    } catch (Exception ioe) {
      System.out.println("error = " + ncmlFile);
      ioe.printStackTrace();
    }
  }

}
