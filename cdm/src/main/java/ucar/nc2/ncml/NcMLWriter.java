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
import ucar.nc2.util.NetworkUtils;
import ucar.nc2.dataset.*;
import ucar.nc2.dataset.conv._Coordinate;
import ucar.unidata.util.StringUtil;

import thredds.catalog.XMLEntityResolver;
import org.jdom.*;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

import java.io.*;
import java.util.*;

/**
 * Helper class to write NcML.
 *
 * @author caron
 * @see ucar.nc2.NetcdfFile
 */

public class NcMLWriter {
  protected static final Namespace ncNS = Namespace.getNamespace(XMLEntityResolver.NJ22_NAMESPACE);
  protected static final Namespace xsiNS = Namespace.getNamespace("xsi", XMLEntityResolver.W3C_XML_NAMESPACE);

  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NcMLWriter.class);

  private NetcdfDataset ncd;
  private XMLOutputter fmt;
  private Variable aggCoord;

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

    // Output the document, use standard formatter
    //fmt = new XMLOutputter("  ", true);
    //fmt.setLineSeparator("\n");
    fmt = new XMLOutputter(Format.getPrettyFormat());
    fmt.output(makeDocument(location), os);
  }

  public void writeXMLAgg(NetcdfDataset ncd, OutputStream os, String location) throws IOException {
    this.ncd = ncd;

    // Output the document, use standard formatter
    //fmt = new XMLOutputter("  ", true);
    //fmt.setLineSeparator("\n");
    fmt = new XMLOutputter(Format.getPrettyFormat());
    fmt.output(makeDocument(location), os);
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
      rootElem.setAttribute("location", NetworkUtils.canonicalizeWrite(location));
    }

    if (null != ncd.getId())
      rootElem.setAttribute("id", ncd.getId());

    if (null != ncd.getTitle())
      rootElem.setAttribute("title", ncd.getTitle());

    if (ncd.isEnhanced())
      rootElem.setAttribute("enhance", "true");

    Aggregation agg = ncd.getAggregation();
    if (agg != null) {
      String aggDimensionName = agg.getDimensionName();
      aggCoord = ncd.findVariable(aggDimensionName);
      //System.out.println("isMetadata="+aggCoord.isMetadata());
    }

    Group rootGroup = ncd.getRootGroup();

    if (ncd.getCoordSysWereAdded()) {
      String conv = ncd.findAttValueIgnoreCase(null, "Conventions", null);
      if (conv == null)
        rootGroup.addAttribute(new Attribute("Conventions", _Coordinate.Convention));
      else
        rootGroup.addAttribute(new Attribute("Conventions", conv + ", " + _Coordinate.Convention));

    }

    writeGroup(rootElem, rootGroup);

    if (agg != null) {
      rootElem.addContent(writeAggregation(agg));
    }

    return doc;
  }

  private Element writeAggregation(Aggregation agg) throws IOException {
    Element aggElem = new Element("aggregation", ncNS);
    Aggregation.Type type = agg.getType();
    aggElem.setAttribute("type", type.toString());

    String dimName = agg.getDimensionName();
    if (dimName != null)
      aggElem.setAttribute("dimName", dimName);

    List<String> varList = agg.getVariables();
    for (String s : varList) {
      Element e = new Element("variableAgg", ncNS);
      e.setAttribute("name", s);
      aggElem.addContent(e);
    }

    List<Aggregation.Dataset> dsList = agg.getNestedDatasets();
    for (Aggregation.Dataset ds : dsList) {
      Element e = new Element("netcdf", ncNS);
      e.setAttribute("location", ds.getLocation());
      //if (type == Aggregation.Type.JOIN_EXISTING)
      e.setAttribute("ncoords", Integer.toString(ds.getNcoords(null)));
      if (null != ds.getCoordValueString())
        e.setAttribute("coordValue", ds.getCoordValueString());
      aggElem.addContent(e);
    }

    if (agg.getType() == Aggregation.Type.UNION) {
      List<NetcdfDataset> unionList = ((AggregationUnion) agg).getUnionDatasets();
      for (NetcdfDataset ds : unionList) {
        Element e = new Element("netcdf", ncNS);
        e.setAttribute("location", ds.getLocation());
        aggElem.addContent(e);
      }
    }

    return aggElem;
  }

  public static Element writeAttribute(ucar.nc2.Attribute att, String elementName, Namespace ns) {
    Element attElem = new Element(elementName, ns);
    attElem.setAttribute("name", att.getName());

    DataType dt = att.getDataType();
    if ((dt != null) && (dt != DataType.STRING))
      attElem.setAttribute("type", dt.toString());

    if (att.isString()) {
      String value = att.getStringValue();
      /* String err = org.jdom.Verifier.checkCharacterData(value);
      if (err != null) {
        value = "NcMLWriter invalid attribute value, err= " + err;
        System.out.println(value);
      } */
      attElem.setAttribute("value", value);
    } else {

      StringBuffer buff = new StringBuffer();
      for (int i = 0; i < att.getLength(); i++) {
        Number val = att.getNumericValue(i);
        if (i > 0) buff.append(" ");
        buff.append(val.toString());
      }
      attElem.setAttribute("value", buff.toString());
    }
    return attElem;
  }

  /* private Element makeCoordSys( CoordinateSystem cs) {
    Element csElem = new Element("coordinateSystem", ncNS);
    csElem.setAttribute("name", cs.getName());

    /* ReferenceSystem rs = cs.getReferenceSystem();
    if (rs != null) {
      if (commonRS.contains( rs)) {
        Element rsElem = new Element("referenceSystemRef", ncNS);
        rsElem.setAttribute("ref", rs.getId());
        csElem.addContent( rsElem);
      } else {
        csElem.addContent( makeReferenceSys(rs, commonHRS));
      }
    }

    ArrayList axes = cs.getCoordinateAxes();
    for (int i=0; i<axes.size(); i++) {
      Element axisElem = new Element("coordinateAxisRef", ncNS);
      axisElem.setAttribute("ref", ((VariableEnhanced) axes.get(i)).getName());
      csElem.addContent( axisElem);
    }

    ArrayList transforms = cs.getCoordinateTransforms();
    if (transforms != null)
    for (int i=0; i<transforms.size(); i++) {
      CoordinateTransform ct = (CoordinateTransform) transforms.get(i);
      if (ct == null) continue;
      Element tElem = new Element("coordinateTransformRef", ncNS);
      tElem.setAttribute("ref", ct.getName());
      csElem.addContent( tElem);
    }
    return csElem;
  }

  private Element makeCoordTransform( CoordinateTransform coordTransform) {
    Element elem = new Element("coordinateTransform", ncNS);
    elem.setAttribute("name", coordTransform.getName());
    elem.setAttribute("authority", coordTransform.getAuthority());
    //if (coordTransform.getReferenceSystem() != null)
    //  elem.setAttribute("referenceCoordinateSystem", coordTransform.getReferenceSystem().getName());
    if (coordTransform.getTransformType() != null)
      elem.setAttribute("transformType", coordTransform.getTransformType().toString());

    ArrayList params = coordTransform.getParameters();
    for (int i=0; i<params.size(); i++) {
      ucar.unidata.util.Parameter p = (ucar.unidata.util.Parameter) params.get(i);
      elem.addContent( makeParameter(p, "parameter"));
    }
    return elem;
  }  */

  // shared dimensions

  public static Element writeDimension(Dimension dim, Namespace ns) {
    Element dimElem = new Element("dimension", ns);
    dimElem.setAttribute("name", dim.getName());
    dimElem.setAttribute("length", Integer.toString(dim.getLength()));
    if (dim.isUnlimited())
      dimElem.setAttribute("isUnlimited", "true");
    if (dim.isVariableLength())
      dimElem.setAttribute("isVariableLength", "true");
    return dimElem;
  }

  private Element writeGroup(Element elem, Group group) {

    // dimensions
    for (Dimension dim : group.getDimensions()) {
      elem.addContent(writeDimension(dim, ncNS));
    }

    // attributes
    for (Attribute att : group.getAttributes()) {
      elem.addContent(writeAttribute(att, "attribute", ncNS));
    }

    /* if (addCoords) {
         // coordinate axes
      Iterator vars = group.getVariables().iterator();
      while ( vars.hasNext()) {
        VariableEnhanced var = (VariableEnhanced) vars.next();
        if (var instanceof CoordinateAxis)
          elem.addContent( makeCoordinateAxis( (CoordinateAxis) var));
      }
    } */

    // regular variables
    for (Variable var : group.getVariables()) {
      try {
        elem.addContent(writeVariable((VariableEnhanced) var));
      } catch (ClassCastException e) {
        log.error("var not instanceof VariableEnhanced = " + var.getName(), e);
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

  /* private Element makeParameter( ucar.unidata.util.Parameter p, String elementName) {
    Element attElem = new Element(elementName, ncNS);
    attElem.setAttribute("name", p.getName());
    attElem.setAttribute("type", p.isString() ? "String" : "double");

    if (p.isString()) {
      String value = p.getStringValue();
      String err = org.jdom.Verifier.checkCharacterData(value);
      if (err != null) {
        value = "NcMLWriter invalid attribute value, err= "+err;
        System.out.println(value);
      }
      attElem.setAttribute("value", value);
    } else {

      StringBuffer buff = new StringBuffer();
      for (int i=0; i<p.getLength(); i++) {
        double val = p.getNumericValue(i);
        if (i > 0) buff.append( " ");
        buff.append( val);
      }
      attElem.setAttribute("value", buff.toString());
    }
    return attElem;
  } */

  /* private Element makeReferenceSys( ReferenceSystem referenceSystem) {
   Element elem = new Element("referenceCoordinateSystem", ncNS);
   elem.setAttribute("name", referenceSystem.getName());
   elem.setAttribute("authority", referenceSystem.getAuthority());
   if (referenceSystem.getReferenceType() != null)
     elem.setAttribute("type", referenceSystem.getReferenceType().toString());

   ArrayList params = referenceSystem.getParameters();
   for (int i=0; i<params.size(); i++) {
     ucar.nc2.Attribute att = (ucar.nc2.Attribute) params.get(i);
     elem.addContent( makeAttribute(att, "parameter"));
   }
   return elem;
 } */


  private Element writeVariable(VariableEnhanced var) {
    boolean isStructure = var instanceof Structure;

    Element varElem = new Element("variable", ncNS);
    varElem.setAttribute("name", var.getShortName());

    StringBuffer buff = new StringBuffer();
    List dims = var.getDimensions();
    for (int i = 0; i < dims.size(); i++) {
      Dimension dim = (Dimension) dims.get(i);
      if (i > 0) buff.append(" ");
      if (dim.isShared())
        buff.append(dim.getName());
      else
        buff.append(dim.getLength());
    }
    if (buff.length() > 0)
      varElem.setAttribute("shape", buff.toString());

    DataType dt = var.getDataType();
    if (dt != null)
      varElem.setAttribute("type", dt.toString());

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

  /* private Element makeCoordinateAxis( CoordinateAxis var) {
   var.addAttribute( new Attribute("units", var.getUnitsString()));
   if (var.getAxisType() != null)
     var.addAttribute( new Attribute(_Coordinate.AxisType", var.getAxisType().toString()));
   if (var.getPositive() != null)
     var.addAttribute( new Attribute(_Coordinate.ZisPositive", var.getPositive()));

   return makeVariable(var);
 } */

  public static Element writeValues(VariableEnhanced v, Namespace ns, boolean allowRegular) {
    Element elem = new Element("values", ns);

    StringBuffer buff = new StringBuffer();
    Array a;
    try {
      a = v.read();
    } catch (IOException ioe) {
      return elem;
    }

    if (v.getDataType() == DataType.CHAR) {
      char[] data = (char[]) a.getStorage();
      elem.setText(new String(data));

    } else if (v.getDataType() == DataType.STRING) { // use seperate elements??
      IndexIterator iter = a.getIndexIterator();
      int count = 0;
      while (iter.hasNext()) {
        String s = (String) iter.getObjectNext();
        if (count++ > 0) buff.append(" ");
        buff.append("\"").append(s).append("\"");
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
          elem.setAttribute("npts", Long.toString(v.getSize()));
          return elem;
        }
      }

      // not regular
      boolean isRealType = (v.getDataType() == DataType.DOUBLE) || (v.getDataType() == DataType.FLOAT);
      IndexIterator iter = a.getIndexIterator();
      buff.append(isRealType ? iter.getDoubleNext() : iter.getIntNext());
      while (iter.hasNext()) {
        buff.append(" ");
        buff.append(isRealType ? iter.getDoubleNext() : iter.getIntNext());
      }
      elem.setText(buff.toString());

    } // not string

    return elem;
  }

  public static void main(String arg[]) {
    //String urls = "C:/data/conventions/coards/cldc.mean.nc";
    String test = "C:/data/atd/rgg.20020411.000000.lel.ll.nc";
    String urls = (arg.length == 0) ? test : arg[0];

    try {
      NetcdfDataset df = NetcdfDataset.openDataset(urls);
      NcMLWriter writer = new NcMLWriter();
      System.out.println("NetcdfDataset = " + urls + "\n" + df);
      System.out.println("-----------");
      writer.writeXML(df, System.out, null);
    } catch (Exception ioe) {
      System.out.println("error = " + urls);
      ioe.printStackTrace();
    }
  }

}