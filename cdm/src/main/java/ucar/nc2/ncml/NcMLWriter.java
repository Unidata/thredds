/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.ncml;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.Attribute;
import ucar.nc2.util.URLnaming;
import ucar.nc2.dataset.*;

import org.jdom2.*;
import org.jdom2.output.XMLOutputter;
import org.jdom2.output.Format;
import ucar.nc2.util.xml.Parse;

import java.io.*;
import java.util.*;

/**
 * Helper class to write NcML.
 *
 * @author caron
 * @see ucar.nc2.NetcdfFile
 * @see <a href="http://www.unidata.ucar.edu/software/netcdf/ncml/">http://www.unidata.ucar.edu/software/netcdf/ncml/</a>
 */

public class NcMLWriter {
  static private final Namespace ncNS = thredds.client.catalog.Catalog.ncmlNS;
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NcMLWriter.class);

  private NetcdfDataset ncd;
  private XMLOutputter fmt;
  private Variable aggCoord;

  /**
   * Write NcML from specified NetcdfFile to a String.
   *
   * @param ncfile      NcML for this NetcdfFile
   * @return the NcML in a String
   * @throws IOException on io error
   */
  public String writeXML(NetcdfFile ncfile) throws IOException {
    if (ncfile instanceof NetcdfDataset)
      ncd = (NetcdfDataset) ncfile;
    else
      ncd = new NetcdfDataset(ncfile, false);

    // Output the document, use standard formatter
    //fmt = new XMLOutputter("  ", true);
    //fmt.setLineSeparator("\n");
    fmt = new XMLOutputter(Format.getPrettyFormat());
    return fmt.outputString(makeDocument( null));
  }


  /**
   * Write a NetcdfFile as an XML document to the specified file.
   *
   * @param ncfile      NcML for this NetcdfFile
   * @param filenameOut write NcML to this location
   * @throws IOException on io error
   */
  public void writeXML(NetcdfFile ncfile, String filenameOut) throws IOException {
    OutputStream out = new BufferedOutputStream(new FileOutputStream(filenameOut, false));
    writeXML(ncfile, out, null);
    out.close();
  }

  /**
   * Write a NetcdfFile as an XML document to the specified stream.
   *
   * @param ncfile   NcML for this NetcdfFile
   * @param os       write to this OutputStream
   * @param location normally null, meaning use ncd.getLocation(); otherwise put this into the NcML location
   * @throws IOException on io error
   */
  public void writeXML(NetcdfFile ncfile, OutputStream os, String location) throws IOException {

    if (ncfile instanceof NetcdfDataset)
      ncd = (NetcdfDataset) ncfile;
    else
      ncd = new NetcdfDataset(ncfile, false);

    fmt = new XMLOutputter(Format.getPrettyFormat());
    fmt.output(makeDocument(location), os);
  }

  public void writeXMLexplicit(NetcdfFile ncfile, OutputStream os, String location) throws IOException {
    if (ncfile instanceof NetcdfDataset)
      ncd = (NetcdfDataset) ncfile;
    else
      ncd = new NetcdfDataset(ncfile, false);

    fmt = new XMLOutputter(Format.getPrettyFormat());
    Document doc = makeDocument(location);
    Element root = doc.getRootElement();
    root.addContent( new Element("explicit", ncNS));
    fmt.output(doc, os);
  }


  private Document makeDocument(String location) throws IOException {
    Element rootElem = new Element("netcdf", ncNS);
    Document doc = new Document(rootElem);

    // namespaces
    rootElem.addNamespaceDeclaration(ncNS);
    /* rootElem.addNamespaceDeclaration(xsiNS);

    if (ncfile instanceof NetcdfDataset)
      rootElem.setAttribute("schemaLocation",
        "http://www.ucar.edu/schemas/netcdf-2.2 http://www.unidata.ucar.edu/schemas/netcdfCS-2.2.xsd", xsiNS);
    else
      rootElem.setAttribute("schemaLocation",
        "http://www.ucar.edu/schemas/netcdf-2.2 http://www.unidata.ucar.edu/schemas/netcdf-2.2.xsd", xsiNS);
    */

    if (null == location)
      location = ncd.getLocation();

    if (null != location) {
      rootElem.setAttribute("location", URLnaming.canonicalizeWrite(location));
    }

    if (null != ncd.getId())
      rootElem.setAttribute("id", ncd.getId());

    if (null != ncd.getTitle())
      rootElem.setAttribute("title", ncd.getTitle());

    //if (ncd.getEnhanceMode() != NetcdfDataset.EnhanceMode.None)
    //  rootElem.setAttribute("enhance", ncd.getEnhanceMode().toString());

    Aggregation agg = ncd.getAggregation();
    if (agg != null) {
      String aggDimensionName = agg.getDimensionName();
      aggCoord = ncd.findVariable(aggDimensionName);
      //System.out.println("isMetadata="+aggCoord.isMetadata());
    }

    Group rootGroup = ncd.getRootGroup();

    /* if (ncd.getCoordSysWereAdded()) {
      String conv = ncd.findAttValueIgnoreCase(null, "Conventions", null);
      if (conv == null)
        rootGroup.addAttribute(new Attribute("Conventions", _Coordinate.Convention));
      else
        rootGroup.addAttribute(new Attribute("Conventions", conv + ", " + _Coordinate.Convention));
    } */

    writeGroup(rootElem, rootGroup);

    //if (agg != null) { LOOK ncml3
    //  rootElem.addContent(writeAggregation(agg));
    //}

    return doc;
  }

  public static Element writeAttribute(ucar.nc2.Attribute att, String elementName, Namespace ns) {
    Element attElem = new Element(elementName, ns);
    attElem.setAttribute("name", att.getShortName());

    DataType dt = att.getDataType();
    if ((dt != null) && (dt != DataType.STRING))
      attElem.setAttribute("type", dt.toString());

    if (att.getLength() == 0) {
      if (att.isUnsigned())
        attElem.setAttribute("isUnsigned", "true");
      return attElem;
    }

    if (att.isString()) {
      StringBuilder buff = new StringBuilder();
      for (int i = 0; i < att.getLength(); i++) {
        String sval = att.getStringValue(i);
        if (i > 0) buff.append(",");
        buff.append(sval);
      }
      attElem.setAttribute("value", Parse.cleanCharacterData(buff.toString()));
      if (att.getLength() > 1)
        attElem.setAttribute("separator", ",");

    } else {
      StringBuilder buff = new StringBuilder();
      for (int i = 0; i < att.getLength(); i++) {
        Number val = att.getNumericValue(i);
        if (i > 0) buff.append(" ");
        buff.append(val.toString());
      }
      attElem.setAttribute("value", buff.toString());

      if (att.isUnsigned())
        attElem.setAttribute("isUnsigned", "true");
    }
    return attElem;
  }

  // shared dimensions
  public static Element writeDimension(Dimension dim, Namespace ns) {
    Element dimElem = new Element("dimension", ns);
    dimElem.setAttribute("name", dim.getShortName());
    if (dim.isVariableLength())
      dimElem.setAttribute("length", "*");
    else
      dimElem.setAttribute("length", Integer.toString(dim.getLength()));

    if (dim.isUnlimited())
      dimElem.setAttribute("isUnlimited", "true");
    if (dim.isVariableLength())
      dimElem.setAttribute("isVariableLength", "true");

    return dimElem;
  }

     // enum Typedef
  public static Element writeEnumTypedef(EnumTypedef etd, Namespace ns) {
    Element typeElem = new Element("enumTypedef", ns);
    typeElem.setAttribute("name", etd.getShortName());
    typeElem.setAttribute("type", etd.getBaseType().toString());
    Map<Integer, String> map = etd.getMap();
    for (Map.Entry<Integer, String> entry : map.entrySet()) {
      typeElem.addContent(new Element("enum", ns)
              .setAttribute("key", Integer.toString(entry.getKey()))
              .addContent(entry.getValue()));
    }

    return typeElem;
  }

  private Element writeGroup(Element elem, Group group) {

    // enumTypeDef
    for (EnumTypedef etd : group.getEnumTypedefs()) {
      elem.addContent(writeEnumTypedef(etd, ncNS));
    }

    // dimensions
    for (Dimension dim : group.getDimensions()) {
      elem.addContent(writeDimension(dim, ncNS));
    }

    // attributes
    for (Attribute att : group.getAttributes()) {
      elem.addContent(writeAttribute(att, "attribute", ncNS));
    }

    // regular variables
    for (Variable var : group.getVariables()) {
      try {
        elem.addContent(writeVariable((VariableEnhanced) var));
      } catch (ClassCastException e) {
        log.error("var not instanceof VariableEnhanced = " + var.getFullName(), e);
      }
    }

    // nested groups
    for (Group g : group.getGroups()) {
      Element groupElem = new Element("group", ncNS);
      groupElem.setAttribute("name", g.getShortName());
      elem.addContent(writeGroup(groupElem, g));
    }

    return elem;
  }

  private Element writeVariable(VariableEnhanced var) {
    boolean isStructure = var instanceof Structure;

    Element varElem = new Element("variable", ncNS);
    varElem.setAttribute("name", var.getShortName());

    StringBuilder buff = new StringBuilder();
    List dims = var.getDimensions();
    for (int i = 0; i < dims.size(); i++) {
      Dimension dim = (Dimension) dims.get(i);
      if (i > 0) buff.append(" ");
      if (dim.isShared())
        buff.append(dim.getShortName());
      else if (dim.isVariableLength())
        buff.append("*");
      else
        buff.append(dim.getLength());
    }
    //if (buff.length() > 0)
    varElem.setAttribute("shape", buff.toString());

    DataType dt = var.getDataType();
    if (dt != null) {
      varElem.setAttribute("type", dt.toString());
      if (dt.isEnum())
        varElem.setAttribute("typedef", var.getEnumTypedef().getShortName());
    }

    // attributes
    for (Attribute att : var.getAttributes()) {
      varElem.addContent(writeAttribute(att, "attribute", ncNS));
    }

    if (var.isMetadata() || (var == aggCoord))
      varElem.addContent(writeValues(var, ncNS, true));

    if (isStructure) {
      Structure s = (Structure) var;
      for (Variable variable : s.getVariables()) {
        VariableEnhanced nestedV = (VariableEnhanced) variable;
        varElem.addContent(writeVariable(nestedV));
      }
    }

    return varElem;
  }

  public static Element writeValues(VariableEnhanced v, Namespace ns, boolean allowRegular) {
    Array a;
    try {
      a = v.read();
    } catch (IOException ioe) {
      return new Element("values", ns);
    }
    return writeValues(a, ns, allowRegular);
  }

  public static Element writeValues(Array a, Namespace ns, boolean allowRegular) {
    Element elem = new Element("values", ns);

    DataType dtype = a.getDataType();
    if (dtype == DataType.CHAR) {
      char[] data = (char[]) a.getStorage();
      elem.setText(new String(data));

    } else if (dtype == DataType.STRING) { // use seperate elements??
      IndexIterator iter = a.getIndexIterator();
      int count = 0;
      Formatter buff = new Formatter();
      while (iter.hasNext()) {
        String s = (String) iter.getObjectNext();
        if (count++ > 0) buff.format(" ");
        buff.format("\"%s\"", s);
      }
      elem.setText(buff.toString());

    } else {

      //check to see if regular
      if (allowRegular && (a.getRank() == 1) && (a.getSize() > 2)) {
        Index ima = a.getIndex();
        double start = a.getDouble(ima.set(0));
        double incr = a.getDouble(ima.set(1)) - start;
        boolean isRegular = true;
        for (int i = 2; i < a.getSize(); i++) {
          double v1 = a.getDouble(ima.set(i));
          double v0 = a.getDouble(ima.set(i - 1));
          if (!ucar.nc2.util.Misc.closeEnough(v1 - v0, incr))
            isRegular = false;
        }

        if (isRegular) {
          elem.setAttribute("start", Double.toString(start));
          elem.setAttribute("increment", Double.toString(incr));
          elem.setAttribute("npts", Long.toString(a.getSize()));
          return elem;
        }
      }

      // not regular
      boolean isRealType = (dtype == DataType.DOUBLE) || (dtype == DataType.FLOAT);
      IndexIterator iter = a.getIndexIterator();
      Formatter buff = new Formatter();
      buff.format("%s", isRealType ? iter.getDoubleNext() : iter.getIntNext());
      while (iter.hasNext()) {
        buff.format(" ");
        buff.format("%s", isRealType ? iter.getDoubleNext() : iter.getIntNext());
      }
      elem.setText(buff.toString());

    } // not string

    return elem;
  }

}