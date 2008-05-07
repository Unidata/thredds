/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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
package ucar.nc2.ncml;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.Attribute;
import ucar.nc2.dataset.*;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.IO;
import ucar.nc2.util.URLnaming;
import ucar.unidata.util.StringUtil;

import thredds.catalog.XMLEntityResolver;
import org.jdom.*;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Read NcML and create NetcdfDataset.
 *
 * @author caron
 * @see "http://www.unidata.ucar.edu/software/netcdf/ncml/"
 */

public class NcMLReader {
  static public final Namespace ncNS = Namespace.getNamespace("nc", XMLEntityResolver.NJ22_NAMESPACE);
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NcMLReader.class);

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

  private static boolean validate = false;

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
    InputStream is = cl.getResourceAsStream(ncmlResourceLocation);
    if (is == null)
      throw new FileNotFoundException(ncmlResourceLocation);

    if (debugXML) {
      System.out.println(" NetcdfDataset URL = <" + ncmlResourceLocation + ">");
      InputStream is2 = cl.getResourceAsStream(ncmlResourceLocation);
      System.out.println(" contents=\n" + IO.readContents(is2));
    }

    org.jdom.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder(validate);
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


  /**
   * Use NCML to modify the dataset, getting NcML from a URL
   *
   * @param ncDataset    modify this dataset
   * @param ncmlLocation URL location of NcML
   * @param cancelTask   allow user to cancel task; may be null
   * @throws IOException on read error
   */
  static public void wrapNcML(NetcdfDataset ncDataset, String ncmlLocation, CancelTask cancelTask) throws IOException {
    org.jdom.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder(validate);
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

    if (referencedDatasetUri == null) {
      // the ncml probably refers to another dataset, but doesnt have to
      referencedDatasetUri = netcdfElem.getAttributeValue("location");
      if (referencedDatasetUri == null)
        referencedDatasetUri = netcdfElem.getAttributeValue("uri");
    }

    NcMLReader reader = new NcMLReader();
    NetcdfDataset ncd = reader.readNcML(ncmlLocation, referencedDatasetUri, netcdfElem, cancelTask);
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

    org.jdom.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder(validate);
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
   * @param r        the Reader containing the NcML document
   * @param cancelTask allow user to cancel the task; may be null
   * @return the resulting NetcdfDataset
   * @throws IOException on read error, or bad referencedDatasetUri URI
   */
  static public NetcdfDataset readNcML(Reader r, CancelTask cancelTask) throws IOException {

    org.jdom.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder(validate);
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
    NetcdfDataset ncd = readNcML(null, netcdfElem, cancelTask);
    if (debugOpen) System.out.println("***NcMLReader.readNcML (stream) result= \n" + ncd);
    return ncd;
  }

  /**
   * Read NcML from a JDOM Document, and construct a NetcdfDataset.
   *
   * @param ncmlLocation the location of the NcML, or may be just a unique name for caching purposes.
   * @param netcdfElem   the JDOM Document's root (netcdf) element
   * @param cancelTask   allow user to cancel the task; may be null
   * @return the resulting NetcdfDataset
   * @throws IOException on read error, or bad referencedDatasetUri URI
   */
  static public NetcdfDataset readNcML(String ncmlLocation, Element netcdfElem, CancelTask cancelTask) throws IOException {

    // the ncml probably refers to another dataset, but doesnt have to
    String referencedDatasetUri = netcdfElem.getAttributeValue("location");
    if (referencedDatasetUri == null)
      referencedDatasetUri = netcdfElem.getAttributeValue("uri");

    NcMLReader reader = new NcMLReader();
    return reader.readNcML(ncmlLocation, referencedDatasetUri, netcdfElem, cancelTask);
  }

  //////////////////////////////////////////////////////////////////////////////////////
  //private boolean enhance = false;
  private boolean explicit = false;
  private boolean hasFatalError = false;

  /**
   * This sets up the target dataset and the referenced dataset.
   *
   * @param ncmlLocation         the URL location string of the NcML document, or may be just a unique name for caching purposes.
   * @param referencedDatasetUri refers to this dataset (may be null)
   * @param netcdfElem           JDOM netcdf element
   * @param cancelTask           allow user to cancel the task; may be null
   * @return NetcdfDataset the constructed dataset
   * @throws IOException on read error, or bad referencedDatasetUri URI
   */
  private NetcdfDataset readNcML(String ncmlLocation, String referencedDatasetUri,
                                 Element netcdfElem, CancelTask cancelTask) throws IOException {

    // augment URI.resolve(), by also dealing with base file: URIs
    referencedDatasetUri = URLnaming.resolve(ncmlLocation, referencedDatasetUri);

    // common error causing infinite regression
    if ((referencedDatasetUri != null) && referencedDatasetUri.equals(ncmlLocation))
      throw new IllegalArgumentException("NcML location attribute refers to the NcML document itself" + referencedDatasetUri);

    // they can specify the iosp to use - but must be file based
    String iospS = netcdfElem.getAttributeValue("iosp");
    String iospParam = netcdfElem.getAttributeValue("iospParam");
    String bufferSizeS = netcdfElem.getAttributeValue("buffer_size");
    int buffer_size = -1;
    if (bufferSizeS != null)
      buffer_size = Integer.parseInt(bufferSizeS);

    // open the referenced dataset - do NOT use acquire, and dont enhance
    NetcdfDataset refds = null;
    if (referencedDatasetUri != null) {
      if (iospS != null) {
        NetcdfFile ncfile;
        try {
          ncfile = new NcMLNetcdfFile(iospS, iospParam, referencedDatasetUri, buffer_size, cancelTask);
        } catch (Exception e) {
          throw new IOException(e.getMessage());
        }
        refds = new NetcdfDataset(ncfile, false);
      } else {
        refds = NetcdfDataset.openDataset(referencedDatasetUri, false, buffer_size, cancelTask, iospParam);
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

  // need access to protected constructor
  private static class NcMLNetcdfFile extends NetcdfFile {
    NcMLNetcdfFile(String iospClassName, String iospParam, String location, int buffer_size, ucar.nc2.util.CancelTask cancelTask)
        throws IOException, IllegalAccessException, ClassNotFoundException, InstantiationException {

      super(iospClassName, iospParam, location, buffer_size, cancelTask);
    }
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
   * @param refds        the referenced datset; may equal newds, never null
   * @param netcdfElem   JDOM netcdf element
   * @param cancelTask   allow user to cancel the task; may be null
   * @throws IOException on read error
   */
  public void readNetcdf(String ncmlLocation, NetcdfDataset targetDS, NetcdfDataset refds, Element netcdfElem, CancelTask cancelTask) throws IOException {

    if (debugOpen)
      System.out.println("NcMLReader.readNetcdf ncml= " + ncmlLocation + " referencedDatasetUri= " + refds.getLocation());

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
    if (hasFatalError)
      throw new IllegalArgumentException("NcML had fatal errors - see logs");

    // transfer from groups to global conatiners
    targetDS.finish();

    // enhance means do scale/offset and/or add CoordSystems
    String enhanceS = netcdfElem.getAttributeValue("enhance");
    if (enhanceS != null) {
      NetcdfDataset.EnhanceMode mode = NetcdfDataset.EnhanceMode.None;
      if (enhanceS.equalsIgnoreCase("true"))
        mode = NetcdfDataset.EnhanceMode.All;
      else if (enhanceS.equalsIgnoreCase("All"))
        mode = NetcdfDataset.EnhanceMode.All;
      else if (enhanceS.equalsIgnoreCase("ScaleMissing"))
        mode = NetcdfDataset.EnhanceMode.ScaleMissing;
      else if (enhanceS.equalsIgnoreCase("CoordSystems"))
        mode = NetcdfDataset.EnhanceMode.CoordSystems;

      targetDS.enhance(mode);
    }

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
      log.warn("NcML Attribute name is required (" + attElem + ")");
      hasFatalError = true;
      return;
    }
    String nameInFile = attElem.getAttributeValue("orgName");
    boolean newName = (nameInFile != null) && !nameInFile.equals(name);
    if (nameInFile == null)
      nameInFile = name;
    else if (null == findAttribute(refParent, nameInFile)) { // has to exists
      log.warn("NcML attribute orgName '" + nameInFile + "' doesnt exist. att=" + name + " in=" + parent);
      hasFatalError = true;
      return;
    }

    // see if its new
    ucar.nc2.Attribute att = findAttribute(refParent, nameInFile);
    if (att == null) { // new
      if (debugConstruct) System.out.println(" add new att = " + name);
      try {
        ucar.ma2.Array values = readAttributeValues(attElem);
        addAttribute(parent, new ucar.nc2.Attribute(name, values));
      } catch (RuntimeException e) {
        log.warn("NcML new Attribute Exception: " + e.getMessage() + " att=" + name + " in=" + parent);
        hasFatalError = true;
      }

    } else { // already exists

      if (debugConstruct) System.out.println(" modify existing att = " + name);
      boolean hasValue = attElem.getAttribute("value") != null;
      if (hasValue) {
        try {
          ucar.ma2.Array values = readAttributeValues(attElem);
          addAttribute(parent, new ucar.nc2.Attribute(name, values));
        } catch (RuntimeException e) {
          log.warn("NcML existing Attribute Exception: " + e.getMessage() + " att=" + name + " in=" + parent);
          hasFatalError = true;
          return;
        }
      } else { // use the old values
        addAttribute(parent, new ucar.nc2.Attribute(name, att.getValues()));
      }

      // remove the old one ??
      if (newName && !explicit) {
        removeAttribute(parent, att);
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
  private ucar.ma2.Array readAttributeValues(Element s) throws IllegalArgumentException {
    String valString = s.getAttributeValue("value");
    if (valString == null) throw new IllegalArgumentException("No value specified");
    valString = StringUtil.unquoteXmlAttribute(valString);

    String type = s.getAttributeValue("type");
    DataType dtype = (type == null) ? DataType.STRING : DataType.getType(type);
    if (dtype == DataType.CHAR) dtype = DataType.STRING;

    String sep = s.getAttributeValue("separator");
    if ((sep == null) && (dtype == DataType.STRING)) {
      List<String> list = new ArrayList<String>();
      list.add(valString);
      return NetcdfDataset.makeArray(dtype, list);
    }

    if (sep == null) sep = " "; // default whitespace separated

    List<String> stringValues = new ArrayList<String>();
    StringTokenizer tokn = new StringTokenizer(valString, sep);
    while (tokn.hasMoreTokens())
      stringValues.add(tokn.nextToken());

    return NetcdfDataset.makeArray(dtype, stringValues);
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
      log.info("NcML Dimension name is required (" + dimElem + ")");
      hasFatalError = true;
      return;
    }

    String nameInFile = dimElem.getAttributeValue("orgName");
    if (nameInFile == null) nameInFile = name;

    // see if it already exists
    Dimension dim = refg.findDimension(nameInFile);
    if (dim == null) { // nope - create it
      String lengthS = dimElem.getAttributeValue("length");
      String isUnlimitedS = dimElem.getAttributeValue("isUnlimited");
      String isSharedS = dimElem.getAttributeValue("isShared");
      String isUnknownS = dimElem.getAttributeValue("isVariableLength");

      boolean isUnlimited = (isUnlimitedS != null) && isUnlimitedS.equalsIgnoreCase("true");
      boolean isUnknown = (isUnknownS != null) && isUnknownS.equalsIgnoreCase("true");
      boolean isShared = true;
      if ((isSharedS != null) && isSharedS.equalsIgnoreCase("false"))
        isShared = false;

      int len = Integer.parseInt(lengthS);
      if ((isUnknownS != null) && isUnknownS.equalsIgnoreCase("false"))
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
   * Read the NcML group element, and nested elements.
   *
   * @param newds     new dataset
   * @param refds     referenced dataset
   * @param parent    Group
   * @param refParent parent Group in referenced dataset
   * @param groupElem ncml group element
   */
  private void readGroup(NetcdfDataset newds, NetcdfDataset refds, Group parent, Group refParent, Element groupElem) throws IOException {

    Group g, refg;
    if (parent == null) { // this is the <netcdf> element
      g = newds.getRootGroup();
      refg = refds.getRootGroup();
      if (debugConstruct) System.out.println(" root group ");

    } else {

      String name = groupElem.getAttributeValue("name");
      if (name == null) {
        log.info("NcML Group name is required (" + groupElem + ")");
        hasFatalError = true;
        return;
      }

      String nameInFile = groupElem.getAttributeValue("orgName");
      if (nameInFile == null) nameInFile = name;

      // see if it exists in referenced dataset
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
      if (debugConstruct) System.out.println(" add group = " + g.getName());
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
    Element unitElem = varElem.getChild("units", ncNS);
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
   */
  private void readVariable(NetcdfDataset ds, Group g, Group refg, Element varElem) throws IOException {
    String name = varElem.getAttributeValue("name");
    if (name == null) {
      log.info("NcML Variable name is required (" + varElem + ")");
      hasFatalError = true;
      return;
    }

    String nameInFile = varElem.getAttributeValue("orgName");
    if (nameInFile == null) nameInFile = name;

    // see if it already exists
    Variable refv = refg.findVariable(nameInFile);
    if (refv == null) { // new
      if (debugConstruct) System.out.println(" add new var = " + name);
      g.addVariable(readVariableNew(ds, g, null, varElem));
      return;
    }

    // exists already
    DataType dtype = null;
    String typeS = varElem.getAttributeValue("type");
    if (typeS != null)
      dtype = DataType.getType(typeS);
    else
      dtype = refv.getDataType();

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
      if (shape != null)
        v.setDimensions(shape); // LOOK check conformable
      if (debugConstruct) System.out.println(" modify existing var = " + nameInFile);

    } else { //explicit
      if (refv instanceof Structure) {
        v = new StructureDS(g, (Structure) refv); // true
        v.setName(name);
        if (shape != null) // LOOK check conformable
          v.setDimensions(shape);
        //StructureDS vs = new StructureDS(ds, g, null, name, shape, null, null);
        //vs.setIOVar(refv);
      } else {
        v = new VariableDS(g, refv, false);
        v.setName(name);
        v.setDataType(dtype);
        if (shape != null) // LOOK check conformable
          v.setDimensions(shape);
        //VariableDS vs = new VariableDS(ds, g, null, name, dtype, shape, null, null);
        //vs.setIOVar(refv);
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

    /* now that we have attributes finalized, redo the enhance
    if (enhance && (v instanceof VariableDS))
      ((VariableDS) v).enhance();  */

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
      log.info("NcML Variable name is required (" + varElem + ")");
      hasFatalError = true;
      return null;
    }

    String type = varElem.getAttributeValue("type");
    String shape = varElem.getAttributeValue("shape");

    DataType dtype = DataType.getType(type);

    Variable v;

    if (dtype == DataType.STRUCTURE) {
      StructureDS s = new StructureDS(ds, g, parentS, name, shape, null, null);
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
      log.info("NcML Variable name is required (" + varElem + ")");
      hasFatalError = true;
      return;
    }

    String nameInFile = varElem.getAttributeValue("orgName");
    if (nameInFile == null) nameInFile = name;

    // see if it already exists
    Variable refv = refStruct.findVariable(nameInFile);
    if (refv == null) { // new
      if (debugConstruct) System.out.println(" add new var = " + name);
      Variable nested = readVariableNew(ds, parentS.getParentGroup(), parentS, varElem);
      parentS.addMemberVariable(nested);
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
      if (valueElem != null)
        readValues(ds, v, varElem, valueElem);
    }

    /* now that we have attributes finalized, redo the enhance
    if (enhance && (v instanceof VariableDS))
      ((VariableDS) v).enhance(); */
  }

  private void readValues(NetcdfDataset ds, Variable v, Element varElem, Element valuesElem) {
    List<String> valList = new ArrayList<String>();

    // should really be a seperate element
    String startS = valuesElem.getAttributeValue("start");
    String incrS = valuesElem.getAttributeValue("increment");
    String nptsS = valuesElem.getAttributeValue("npts");
    int npts = (nptsS == null) ? (int) v.getSize() : Integer.parseInt(nptsS);

    // either start, increment are specified
    if ((startS != null) && (incrS != null)) {
      double start = Double.parseDouble(startS);
      double incr = Double.parseDouble(incrS);
      ds.setValues(v, npts, start, incr);
      return;
    }

    String values = varElem.getChildText("values", ncNS);
    String sep = valuesElem.getAttributeValue("separator");
    if (sep == null) sep = " ";

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
      // or a list of values
      StringTokenizer tokn = new StringTokenizer(values, sep);
      while (tokn.hasMoreTokens())
        valList.add(tokn.nextToken());
      ds.setValues(v, valList);
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////

  private Aggregation readAgg(Element aggElem, String ncmlLocation, NetcdfDataset newds, CancelTask cancelTask) throws IOException {
    String dimName = aggElem.getAttributeValue("dimName");
    String type = aggElem.getAttributeValue("type");
    String recheck = aggElem.getAttributeValue("recheckEvery");

    Aggregation agg;
    if (type.equals("joinExisting")) {
      agg = new AggregationExisting(newds, dimName, recheck);

    } else if (type.equals("joinNew")) {
      agg = new AggregationNew(newds, dimName, recheck);

    } else if (type.equals("tiled")) {
      agg = new AggregationTiled(newds, dimName, recheck);

    } else if (type.equals("union")) {
      agg = new AggregationUnion(newds, dimName, recheck);

    } else if (type.equals("forecastModelRunCollection")) {
      AggregationFmrc aggc = new AggregationFmrc(newds, dimName, recheck);
      agg = aggc;

      String fmrcDefinition = aggElem.getAttributeValue("fmrcDefinition");
      if (fmrcDefinition != null)
        aggc.setInventoryDefinition(fmrcDefinition);

    } else if (type.equals("forecastModelRunSingleCollection")) {
      AggregationFmrcSingle aggh = new AggregationFmrcSingle(newds, dimName, recheck);
      agg = aggh;

      String fmrcDefinition = aggElem.getAttributeValue("fmrcDefinition");
      if (fmrcDefinition != null)
        aggh.setInventoryDefinition(fmrcDefinition);

      // nested scan2 elements
      java.util.List<Element> scan2List = aggElem.getChildren("scanFmrc", ncNS);
      for (Element scanElem : scan2List) {
        String dirLocation = scanElem.getAttributeValue("location");
        String suffix = scanElem.getAttributeValue("suffix");
        String regexpPatternString = scanElem.getAttributeValue("regExp");
        String runMatcher = scanElem.getAttributeValue("runDateMatcher");
        String forecastMatcher = scanElem.getAttributeValue("forecastDateMatcher");
        String offsetMatcher = scanElem.getAttributeValue("forecastOffsetMatcher");
        String subdirs = scanElem.getAttributeValue("subdirs");
        String olderS = scanElem.getAttributeValue("olderThan");

        aggh.addDirectoryScanFmrc(dirLocation, suffix, regexpPatternString, subdirs, olderS, runMatcher, forecastMatcher, offsetMatcher);

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

      // look for variable to cache
      list = aggElem.getChildren("cacheVariable", ncNS);
      for (Element gattElem : list) {
        String varName = gattElem.getAttributeValue("name");
        aggo.addCacheVariable(varName);
      }
    }

    // nested netcdf elements
    java.util.List<Element> ncList = aggElem.getChildren("netcdf", ncNS);
    for (Element netcdfElemNested : ncList) {
      String location = netcdfElemNested.getAttributeValue("location");
      if (location == null)
        location = netcdfElemNested.getAttributeValue("uri");

      String ncoords = netcdfElemNested.getAttributeValue("ncoords");
      String coordValueS = netcdfElemNested.getAttributeValue("coordValue");
      String sectionSpec = netcdfElemNested.getAttributeValue("section");

      NcmlElementReader reader = new NcmlElementReader(ncmlLocation, location, netcdfElemNested);
      String cacheName = ncmlLocation + "#" + Integer.toString(netcdfElemNested.hashCode());
      agg.addExplicitDataset(cacheName, location, ncoords, coordValueS, sectionSpec, reader, cancelTask);

      if ((cancelTask != null) && cancelTask.isCancel())
        return null;
      if (debugAggDetail) System.out.println(" debugAgg: nested dataset = " + location);
    }

    // nested scan elements
    java.util.List<Element> dirList = aggElem.getChildren("scan", ncNS);
    for (Element scanElem : dirList) {
      String dirLocation = scanElem.getAttributeValue("location");
      String suffix = scanElem.getAttributeValue("suffix");
      String regexpPatternString = scanElem.getAttributeValue("regExp");
      String dateFormatMark = scanElem.getAttributeValue("dateFormatMark");
      String enhance = scanElem.getAttributeValue("enhance");
      String subdirs = scanElem.getAttributeValue("subdirs");
      String olderS = scanElem.getAttributeValue("olderThan");

      Element cdElement = scanElem.getChild("crawlableDatasetImpl", ncNS);  // ok if null
      agg.addCrawlableDatasetScan(cdElement, dirLocation, suffix, regexpPatternString, dateFormatMark, enhance, subdirs, olderS);

      if ((cancelTask != null) && cancelTask.isCancel())
        return null;
      if (debugAggDetail) System.out.println(" debugAgg: nested dirLocation = " + dirLocation);
    }

    return agg;
  }

  private class NcmlElementReader implements NetcdfFileFactory {
    private Element netcdfElem;
    private String ncmlLocation, location;

    NcmlElementReader(String ncmlLocation, String location, Element netcdfElem) {
      this.ncmlLocation = ncmlLocation;
      this.location = location;
      this.netcdfElem = netcdfElem;
    }

    public NetcdfFile open(String cacheName, int buffer_size, CancelTask cancelTask, Object spiObject) throws IOException {
      if (debugAggDetail) System.out.println(" NcmlElementReader open nested dataset " + cacheName);
      return readNcML(ncmlLocation, location, netcdfElem, cancelTask);
    }
  }

  /* protected Variable readCoordVariable( NetcdfDataset ds, Element varElem) {
    String name = varElem.getAttributeValue("name");
    String type = varElem.getAttributeValue("type");
    String shape;
    if (type.equals("string"))
      shape = name+" "+name+"_len";
    else
      shape = name;

    VariableDS v = new VariableDS( ds, name, type, shape, null);

    // look for attributes
    java.util.List attList = varElem.getChildren("attribute", ncNS);
    for (int j=0; j< attList.size(); j++) {
      Attribute a = readAtt((Element) attList.get(j));
      v.addAttribute( a);
    }

    // look for values
    String values = varElem.getAttributeValue("value");
    if (values == null)
      values = varElem.getChildText("values", ncNS);
    if (values != null) {
      ArrayList valList = new ArrayList();
      String sep = varElem.getAttributeValue("separator");
      if (sep == null) sep = " ";
      StringTokenizer tokn = new StringTokenizer(values, sep);
      while (tokn.hasMoreTokens())
        valList.add(tokn.nextToken());
      v.setValues( valList);
    }

    return v;
  } */

  /* protected void readMetadata( NetcdfDataset ds, Element readElem, CancelTask cancelTask) throws IOException {
    //String name = readElem.getAttributeValue("name"); // not implemented yet
    //String type = readElem.getAttributeValue("type"); // not implemented yet

    // read dataset
    NetcdfDataset referencedDataset = ds.openReferencedDataset(cancelTask);
    transferDataset( referencedDataset, ds);
    if (debugCmd) System.out.println("CMD readMetadata "+referencedDataset.getLocation());

    /* remove elements just read
    java.util.List removeList = readElem.getChildren("remove", ncNS);
    for (int j=0; j< removeList.size(); j++) {
      Element e = (Element) removeList.get(j);
      cmdRemove(ds, e.getAttributeValue("type"), e.getAttributeValue("name"));
    }

    // look for renames
    java.util.List renameList = readElem.getChildren("rename", ncNS);
    for (int j=0; j< renameList.size(); j++) {
      Element e = (Element) renameList.get(j);
      cmdRename(ds, e.getAttributeValue("type"), e.getAttributeValue("nameInFile"),
        e.getAttributeValue("rename"));
    }

    java.util.List varCmdList = readElem.getChildren("readVariable", ncNS);
    for (int j=0; j< varCmdList.size(); j++) {
      Element e = (Element) varCmdList.get(j);
      readReadVariable( ds, e);
    }

  } */

  /* protected void readReadVariable( NetcdfDataset ds, Element readVarElem) {
      String varName = readVarElem.getAttributeValue("name");

      String rename = readVarElem.getAttributeValue("rename");
      if (rename != null) {
        cmdRename( ds, "variable", varName, rename);
        varName = rename;
      }

      VariableDS v = (VariableDS) ds.findVariable(varName);
      if (v == null) {
        System.out.println("NetcdfDataset (readReadVariable) cant find variable= "+varName);
        return;
      }

      java.util.List removeList = readVarElem.getChildren("removeAttribute", ncNS);
      for (int j=0; j< removeList.size(); j++) {
        Element e = (Element) removeList.get(j);
        cmdRemoveVarAtt(ds, v, e.getAttributeValue("name"));
      }

      java.util.List renameList = readVarElem.getChildren("renameAttribute", ncNS);
      for (int j=0; j< renameList.size(); j++) {
        Element e = (Element) renameList.get(j);
        cmdRenameVarAtt(ds, v, e.getAttributeValue("nameInFile"), e.getAttributeValue("rename"));
      }

      java.util.List addList = readVarElem.getChildren("attribute", ncNS);
      for (int j=0; j< addList.size(); j++) {
        Element e = (Element) addList.get(j);
        cmdAddVarAtt(ds, v, e);
      }
    } */

  /////////////////////////////////////////////
  // command procesing

  private void cmdRemove(Group g, String type, String name) {
    if (type.equals("dimension")) {
      Dimension dim = g.findDimension(name);
      if (dim != null) {
        g.remove(dim);
        if (debugCmd) System.out.println("CMD remove " + type + " " + name);
      } else
        System.out.println("CMD remove " + type + " CANT find " + name);

    } else if (type.equals("variable")) {
      Variable v = g.findVariable(name);
      if (v != null) {
        g.remove(v);
        if (debugCmd) System.out.println("CMD remove " + type + " " + name);
      } else
        System.out.println("CMD remove " + type + " CANT find " + name);

    } else if (type.equals("attribute")) {
      ucar.nc2.Attribute a = g.findAttribute(name);
      if (a != null) {
        g.remove(a);
        if (debugCmd) System.out.println("CMD remove " + type + " " + name);
      } else
        System.out.println("CMD remove " + type + " CANT find " + name);
    }
  }

  private void cmdRemove(Variable v, String type, String name) {
    if (type.equals("attribute")) {
      ucar.nc2.Attribute a = v.findAttribute(name);
      if (a != null) {
        v.remove(a);
        if (debugCmd) System.out.println("CMD remove " + type + " " + name);
      } else
        System.out.println("CMD remove " + type + " CANT find " + name);

    } else if (type.equals("variable") && v instanceof Structure) {
      Structure s = (Structure) v;
      Variable nested = s.findVariable(name);
      if (nested != null) {
        s.removeMemberVariable(nested);
        if (debugCmd) System.out.println("CMD remove " + type + " " + name);
      } else
        System.out.println("CMD remove " + type + " CANT find " + name);

    }
  }

  /* protected void cmdRename(NetcdfDataset ds, String type, String name, String rename) {
    if (type.equals("variable")) {
      VariableDS v = (VariableDS) ds.findVariable(name);
      if (v == null) {
        System.out.println("NetcdfDataset cmdRename cant find variable= "+name);
        return;
      }
      v.rename( rename);
      v.setAlias( name);
      if (debugCmd) System.out.println("CMD rename var "+name+" to "+rename);

    } else if (type.equals("attribute")) {
      ucar.nc2.Attribute oldAtt = ds.findGlobalAttribute( name);
      if (null == oldAtt) {
        System.out.println("NetcdfDataset cmdRename cant find global attribute "+name);
        return;
      }

      oldAtt.rename( rename);
      if (debugCmd) System.out.println("CMD rename att "+name+" renamed to "+rename);

    } else if (type.equals("dimension")) {
      ucar.nc2.Dimension d = ds.findDimension( name);
      if (null == d) {
        System.out.println("NetcdfDataset cmdRename cant find dimension "+name);
        return;
      }
      d.rename( rename);
      if (debugCmd) System.out.println("CMD rename dim "+name+" renamed to "+rename);
    }
  }

  protected void cmdAddVarAtt(NetcdfDataset ds, VariableDS v, Element e) {
    ucar.nc2.Attribute newAtt = readAtt(e);
    ucar.nc2.Attribute oldAtt = v.findAttribute( newAtt.getName());
    if (null != oldAtt)
      v.removeAttribute( oldAtt);
    ds.addVariableAttribute( v, newAtt);
    if (debugCmd) System.out.println("CMD variable added att "+newAtt);
  }

  protected void cmdRemoveVarAtt(NetcdfDataset ds, VariableDS v, String attName) {
    ucar.nc2.Attribute oldAtt = v.findAttribute( attName);
    if (null != oldAtt) {
      v.removeAttribute( oldAtt);
      if (debugCmd) System.out.println("CMD variable removed att "+attName);
    } else
      System.out.println("NetcdfDataset cant find attribute "+attName+" variable "+v.getName());
  }

  protected void cmdRenameVarAtt(NetcdfDataset ds, Variable v, String attName, String newName) {
    ucar.nc2.Attribute oldAtt = v.findAttribute( attName);
    if (null != oldAtt) {
      oldAtt.rename( newName);
      if (debugCmd) System.out.println("CMD variable att "+attName+" renamed to "+newName);
    } else
      System.out.println("NetcdfDataset cmdRenameVarAtt cant find attribute "+attName+
        " variable "+v.getName());
  } */

  /**
   * Read an NcML file and write an equivilent NetcdfFile to a physical file, using Netcdf-3 file format.
   *
   * @param ncmlLocation read this NcML file
   * @param fileOutName  write to this local file
   * @throws IOException on write error
   * @see ucar.nc2.FileWriter#writeToFile
   */
  public static void writeNcMLToFile(String ncmlLocation, String fileOutName) throws IOException {
    NetcdfDataset ncd = (NetcdfDataset) NetcdfDataset.acquireFile(ncmlLocation, null);
    //int dataMode = (ncd.getReferencedFile() != null) ? 1 : 2;
    NetcdfFile ncdnew = ucar.nc2.FileWriter.writeToFile(ncd, fileOutName);
    ncd.close();
    ncdnew.close();
  }

  /**
   * Read an NcML and write an equivilent NetcdfFile to a physical file, using Netcdf-3 file format.
   * The NcML may have a referenced dataset in the location URL, in which case the underlying data
   * (modified by the NcML is written to the new file. If the NcML does not have a referenced dataset,
   * then the new file is filled with fill values, like ncgen.
   *
   * @param ncml        read NcML from this input stream
   * @param fileOutName write to this local file
   * @throws IOException on error
   * @see ucar.nc2.FileWriter#writeToFile
   */
  public static void writeNcMLToFile(InputStream ncml, String fileOutName) throws IOException {
    NetcdfDataset ncd = NcMLReader.readNcML(ncml, null);
    NetcdfFile ncdnew = ucar.nc2.FileWriter.writeToFile(ncd, fileOutName, true);
    ncd.close();
    ncdnew.close();
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