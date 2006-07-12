// $Id:NcMLGWriter.java 63 2006-07-12 21:50:51Z edavis $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

import ucar.nc2.*;
import ucar.nc2.Attribute;
import ucar.nc2.dataset.*;
import ucar.ma2.*;

import org.jdom.*;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

import java.io.*;
import java.util.*;

import thredds.catalog.XMLEntityResolver;

/**
 * Helper class to write NcML-G.
 *
 * @see ucar.nc2.NetcdfFile
 * @author caron
 * @version $Revision:63 $ $Date:2006-07-12 21:50:51Z $
 */

public class NcMLGWriter {
  protected static final Namespace ncNS = Namespace.getNamespace("http://www.ucar.edu/schemas/netcdf");
  protected static final String schemaLocation = "http://www.unidata.ucar.edu/schemas/netcdf-cs.xsd";

  /**
   * Write a NetcdfDataset as an NcML-G document to the specified stream.
   *
   * @param ncd write this dataset; should have opened with "add coordinates".
   * @param os write to this OutputStream
   * @param showCoords show 1D coordinate values
   * @param uri use this uri, if null use getLocation()
   * @throws IOException
   */
  public void writeXML(NetcdfDataset ncd, OutputStream os, boolean showCoords, String uri) throws IOException {

    // Output the document, use standard formatter
    //XMLOutputter fmt = new XMLOutputter("  ", true);
    //fmt.setLineSeparator("\n");
    XMLOutputter fmt = new XMLOutputter( Format.getPrettyFormat());
    fmt.output(makeDocument(ncd, showCoords, uri), os);
  }

  Document makeDocument(NetcdfDataset ncd, boolean showCoords, String uri) {
    Element rootElem = new Element("netcdf", ncNS);
    Document doc = new Document(rootElem);

      // namespaces
    rootElem.addNamespaceDeclaration(ncNS);
    rootElem.addNamespaceDeclaration(XMLEntityResolver.xsiNS);
    rootElem.setAttribute("schemaLocation", ncNS.getURI()+" "+schemaLocation, XMLEntityResolver.xsiNS);

    if (null != ncd.getId())
      rootElem.setAttribute("id", ncd.getId());

    if (null != uri)
      rootElem.setAttribute("uri", uri);
    else
      rootElem.setAttribute("uri", ncd.getLocation());

    if (ncd.getCoordSysWereAdded()) {
      String conv = ncd.findAttValueIgnoreCase(null, "Conventions", null);
      if (conv == null)
        ncd.addAttribute( null, new Attribute("Conventions", "_Coordinates"));
      else
        ncd.addAttribute( null, new Attribute("Conventions", conv + ", _Coordinates"));
    }

        // dimensions
    Iterator dims = ncd.getDimensions().iterator();
    while ( dims.hasNext()) {
      Dimension dim = (Dimension) dims.next();
      rootElem.addContent( makeDim( dim));
    }

        // attributes
    Iterator atts = ncd.getGlobalAttributes().iterator();
    while ( atts.hasNext()) {
      ucar.nc2.Attribute att = (ucar.nc2.Attribute) atts.next();
      rootElem.addContent( makeAttribute( att, "attribute"));
    }

        // coordinate axes
    Iterator vars = ncd.getVariables().iterator();
    while ( vars.hasNext()) {
      VariableDS var = (VariableDS) vars.next();
      if (var instanceof CoordinateAxis)
        rootElem.addContent( makeCoordinateAxis( (CoordinateAxis)var, showCoords));
    }
         // regular variables
    vars = ncd.getVariables().iterator();
    while ( vars.hasNext()) {
      VariableDS var = (VariableDS) vars.next();
      if (!(var instanceof CoordinateAxis))
        rootElem.addContent( makeVariable( var));
    }

    ///////////////////////////////////////////////////////////
    // reference systems

      // find common coordinate transforms

      /* ReferenceSystem rs = cs.getReferenceSystem();
      int idx = commonRS.indexOf(rs);
      if (idx >= 0) {
        cs.setReferenceSystem( (ReferenceSystem) commonRS.get(idx)); // point to common one
        continue;
      }
      for (int j=i+1; j<csys.size(); j++) {
        CoordinateSystem cs2 = (CoordinateSystem) csys.get(j);
        ReferenceSystem rs2 = cs2.getReferenceSystem();
        if (rs.equals(rs2)) { // used more than once
          commonRS.add( rs);
          break;
        }
      }
    }

      /* find common reference systems
    ArrayList commonRS = new ArrayList();
    List csys = ncd.getCoordinateSystems();
    for (int i=0; i<csys.size(); i++) {
      CoordinateSystem cs = (CoordinateSystem) csys.get(i);
      ReferenceSystem rs = cs.getReferenceSystem();
      int idx = commonRS.indexOf(rs);
      if (idx >= 0) {
        cs.setReferenceSystem( (ReferenceSystem) commonRS.get(idx)); // point to common one
        continue;
      }
      for (int j=i+1; j<csys.size(); j++) {
        CoordinateSystem cs2 = (CoordinateSystem) csys.get(j);
        ReferenceSystem rs2 = cs2.getReferenceSystem();
        if (rs.equals(rs2)) { // used more than once
          commonRS.add( rs);
          break;
        }
      }
    }

      // find common horiz reference systems
    ArrayList commonHRS = new ArrayList();
    csys = ncd.getCoordinateSystems();
    for (int i=0; i<csys.size(); i++) {
      CoordinateSystem cs = (CoordinateSystem) csys.get(i);
      ReferenceSystem rs = cs.getReferenceSystem();
      ReferenceSystem.Horiz hrs = rs.getHoriz();
      int idx = commonHRS.indexOf(hrs);
      if (idx >= 0) {
        rs.setHoriz( (ReferenceSystem.Horiz) commonHRS.get(idx)); // point to common one
        continue;
      }
      for (int j=i+1; j<csys.size(); j++) {
        CoordinateSystem cs2 = (CoordinateSystem) csys.get(j);
        ReferenceSystem rs2 = cs2.getReferenceSystem();
        ReferenceSystem.Horiz hrs2 = rs2.getHoriz();
        if (hrs.equals(hrs2)) { // used more than once
          commonHRS.add( hrs);
          hrs.setId( hrs.getProjectionName()); //  wrong
          break;
        }
      }
    } */

    // coordinate systems
    List csys2 = ncd.getCoordinateSystems();
    for (int i=0; i<csys2.size(); i++) {
      rootElem.addContent( makeCoordSys( (CoordinateSystem) csys2.get(i)));
    }

      // look for coordinate transforms, ref systems
    ArrayList coordTrans = new ArrayList();
    // ArrayList refSys = new ArrayList();
    List csys = ncd.getCoordinateSystems();
    for (int i=0; i<csys.size(); i++) {
      CoordinateSystem cs = (CoordinateSystem) csys.get(i);
      List ctList = cs.getCoordinateTransforms();
      if (ctList != null) {
        for (int j=0; j<ctList.size(); j++) {
          CoordinateTransform ct = (CoordinateTransform) ctList.get(j);
          if (!coordTrans.contains( ct))
            coordTrans.add(ct);
          /* ReferenceSystem rs = ct.getReferenceSystem();
          if ((rs != null) & !refSys.contains( rs))
            refSys.add(rs); */
        }
      }
    }

      // coordinate transforms
    for (int i=0; i<coordTrans.size(); i++)
      rootElem.addContent( makeCoordTransform( (CoordinateTransform) coordTrans.get(i)));

       /* reference systems
    for (int i=0; i<refSys.size(); i++)
      rootElem.addContent( makeReferenceSys( (ReferenceSystem) refSys.get(i))); */

    /* do common reference systems
    for (int i=0; i<commonRS.size(); i++) {
      ReferenceSystem rs = (ReferenceSystem) commonRS.get(i);
      rootElem.addContent( makeReferenceSys( rs, commonHRS));
    } */

    return doc;
  }

  private Element makeAttribute( ucar.nc2.Attribute att, String elementName) {
    Element attElem = new Element(elementName, ncNS);
    attElem.setAttribute("name", att.getName());

    DataType dt = att.getDataType();
    if (dt != null)
      attElem.setAttribute("type", dt.toString());

    if (att.isString()) {
      String value = att.getStringValue();
      String err = org.jdom.Verifier.checkCharacterData(value);
      if (err != null) {
        value = "NcMLWriter invalid attribute value, err= "+err;
        System.out.println(value);
      }
      attElem.setAttribute("value", value);
    } else {

      StringBuffer buff = new StringBuffer();
      for (int i=0; i<att.getLength(); i++) {
        Number val = att.getNumericValue(i);
        if (i > 0) buff.append( " ");
        buff.append( val.toString());
      }
      attElem.setAttribute("value", buff.toString());
    }
    return attElem;
  }

  private Element makeAttribute( ucar.unidata.util.Parameter att, String elementName) {
    Element attElem = new Element(elementName, ncNS);
    attElem.setAttribute("name", att.getName());

    if (att.isString()) {
      String value = att.getStringValue();
      String err = org.jdom.Verifier.checkCharacterData(value);
      if (err != null) {
        value = "NcMLWriter invalid attribute value, err= "+err;
        System.out.println(value);
      }
      attElem.setAttribute("value", value);
    } else {
      attElem.setAttribute("type", "double");

      StringBuffer buff = new StringBuffer();
      for (int i=0; i<att.getLength(); i++) {
        double val = att.getNumericValue(i);
        if (i > 0) buff.append( " ");
        buff.append( Double.toString( val));
      }
      attElem.setAttribute("value", buff.toString());
    }
    return attElem;
  }

  private Element makeCoordinateAxis( CoordinateAxis var, boolean showCoords) {

    Element varElem = new Element("coordinateAxis", ncNS);
    varElem.setAttribute("name", var.getName());

    StringBuffer buff = new StringBuffer();
    List dims = var.getDimensions();
    for (int i=0; i<dims.size(); i++) {
      Dimension dim = (Dimension) dims.get(i);
      if (i > 0) buff.append( " ");
      buff.append( dim.getName());
    }
    if (buff.length() > 0)
      varElem.setAttribute("shape", buff.toString());

    DataType dt = var.getDataType();
    varElem.setAttribute("type", dt.toString());

        // attributes
    Iterator atts = var.getAttributes().iterator();
    while ( atts.hasNext()) {
      ucar.nc2.Attribute att = (ucar.nc2.Attribute) atts.next();
      varElem.addContent( makeAttribute( att, "attribute"));
    }

    if (var.isMetadata() || (showCoords && var.getRank() <= 1))
      varElem.addContent( makeValues( var));

    // coordinate axis
    varElem.setAttribute("units", var.getUnitsString());
    if (var.getAxisType() != null)
      varElem.setAttribute("axisType", var.getAxisType().toString());

    String positive = var.getPositive();
    if (positive != null)
      varElem.setAttribute("positive", positive);

    String boundaryRef = var.getBoundaryRef();
    if (boundaryRef != null)
      varElem.setAttribute("boundaryRef", boundaryRef);

    // coordinate systems
    List csys = var.getCoordinateSystems();
    if (csys.size() > 0) {
      buff.setLength(0);
      for (int i=0; i<csys.size(); i++) {
        CoordinateSystem cs = (CoordinateSystem) csys.get(i);
        if (i>0) buff.append(" ");
        buff.append( cs.getName());
      }
      varElem.setAttribute("coordinateSystems", buff.toString());
    }

    return varElem;
  }

  private Element makeCoordSys( CoordinateSystem cs) {
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
    } */

    List axes = cs.getCoordinateAxes();
    for (int i=0; i<axes.size(); i++) {
      Element axisElem = new Element("coordinateAxisRef", ncNS);
      axisElem.setAttribute("ref", ((VariableDS)axes.get(i)).getName());
      csElem.addContent( axisElem);
    }

    List transforms = cs.getCoordinateTransforms();
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
      ucar.unidata.util.Parameter att = (ucar.unidata.util.Parameter) params.get(i);
      elem.addContent( makeAttribute(att, "parameter"));
    }
    return elem;
  }

  private Element makeDim( Dimension dim) {
    Element dimElem = new Element("dimension", ncNS);
    dimElem.setAttribute("name", dim.getName());
    dimElem.setAttribute("length", Integer.toString(dim.getLength()));
    if (dim.isUnlimited())
      dimElem.setAttribute("isUnlimited", "true");
    return dimElem;
  }

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

  private Element makeVariable( VariableDS var) {

    Element varElem = new Element("variable", ncNS);

    varElem.setAttribute("name", var.getName());

    StringBuffer buff = new StringBuffer();
    List dims = var.getDimensions();
    for (int i=0; i<dims.size(); i++) {
      Dimension dim = (Dimension) dims.get(i);
      if (i > 0) buff.append( " ");
      buff.append( dim.getName());
    }
    if (buff.length() > 0)
      varElem.setAttribute("shape", buff.toString());

    DataType dt = var.getDataType();
    if (dt != null)
      varElem.setAttribute("type", dt.toString());

        // attributes
    Iterator atts = var.getAttributes().iterator();
    while ( atts.hasNext()) {
      ucar.nc2.Attribute att = (ucar.nc2.Attribute) atts.next();
      varElem.addContent( makeAttribute( att, "attribute"));
    }

    if (var.isMetadata())
      varElem.addContent( makeValues( var));

    // coordinate systems
    List csys = var.getCoordinateSystems();
    if (csys.size() > 0) {
      buff.setLength(0);
      for (int i=0; i<csys.size(); i++) {
        CoordinateSystem cs = (CoordinateSystem) csys.get(i);
        if (i>0) buff.append(" ");
        buff.append( cs.getName());
      }
      varElem.setAttribute("coordinateSystems", buff.toString());
    }

    return varElem;
  }

  private Element makeValues( VariableDS v) {
    Element elem = new Element("values", ncNS);

    StringBuffer buff = new StringBuffer();
    Array a = null;
    try {
      a = v.read();
    } catch (IOException ioe) {
      return elem;
    }

    if (a instanceof ArrayChar) {
       //strings
      ArrayChar dataC = (ArrayChar) a;
      for (int i=0; i<dataC.getShape()[0]; i++) {
        if (i > 0) buff.append(" ");
        buff.append( "\""+dataC.getString(i)+"\"");
      }
      elem.setText(buff.toString());
    } else {
       // numbers
      if (v instanceof CoordinateAxis1D && ((CoordinateAxis1D)v).isRegular()) {
        CoordinateAxis1D axis = (CoordinateAxis1D) v;
        elem.setAttribute("start", Double.toString(axis.getStart()));
        elem.setAttribute("increment", Double.toString(axis.getIncrement()));
        elem.setAttribute("npts", Long.toString(v.getSize()));

      } else {
         // numbers
        boolean isDouble = (v.getDataType() == DataType.DOUBLE);
        boolean isFloat = (v.getDataType() == DataType.FLOAT);
        IndexIterator iter = a.getIndexIterator();
        while (iter.hasNext()) {
          if (isDouble)
            buff.append(iter.getDoubleNext());
          else if (isFloat)
            buff.append(iter.getFloatNext());
          else
            buff.append(iter.getIntNext());
          buff.append(" ");
        }
        elem.setText(buff.toString());
      } // not regular

    } // not string

    return elem;
  }

  public static void main( String arg[]){
    //String urls = "C:/data/conventions/coards/cldc.mean.nc";
    String urls = "C:/data/galeon/RUC.nc";
    try {
      NetcdfDataset df = NetcdfDataset.openDataset(urls);
      NcMLGWriter ncFactory = new NcMLGWriter();
      System.out.println("NetcdfDataset = "+urls+"\n"+df);
      System.out.println("-----------");
      ncFactory.writeXML( df, System.out, true, null);
    } catch (Exception ioe) {
      System.out.println("error = "+urls);
      ioe.printStackTrace();
    }
  }

}