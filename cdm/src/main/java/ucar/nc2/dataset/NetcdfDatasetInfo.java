// $Id: NetcdfDatasetInfo.java,v 1.6 2006/02/14 01:00:57 caron Exp $
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

package ucar.nc2.dataset;

import ucar.nc2.dataset.grid.GridCoordSys;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.TimeUnit;
import ucar.unidata.util.Parameter;

import org.jdom.*;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

import java.util.List;
import java.util.StringTokenizer;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Helper class for obtaining information about a NetcdfDataset.
 * @author john caron
 * @version $Revision: 1.3 $ $Date: 2006/02/13 19:51:26 $
 */
public class NetcdfDatasetInfo {
  private NetcdfDataset ds;
  private StringBuffer parseInfo = new StringBuffer();
  private StringBuffer userAdvice = new StringBuffer();
  private String coordSysBuilderName;

  NetcdfDatasetInfo( NetcdfDataset ds) {
    this.ds = ds;
  }

  /** Detailed information when the coordinate systems were parsed */
  public StringBuffer getParseInfo( ) {
    return parseInfo;
  }
  /** Specific advice to a user about problems with the coordinate information in the file. */
  public StringBuffer getUserAdvice( ) {
    return userAdvice;
  }

  void addParseInfo( String info) {
    parseInfo.append(info);
  }
  void addUserAdvice( String advice) {
    userAdvice.append(advice);
  }

  void setCoordSysBuilderName( String coordSysBuilderName) { this.coordSysBuilderName = coordSysBuilderName; }

  /** Get the name of the COordSysBuilder that parses this file. */
  public String getCoordSysBuilderName( ) { return coordSysBuilderName; }

  /** Write the information as an XML document */
  public String writeXML( )  {
    XMLOutputter fmt = new XMLOutputter( Format.getPrettyFormat());
    return fmt.outputString( makeDocument());
  }

  public GridCoordSys getGridCoordSys(VariableEnhanced ve) {
    List csList = ve.getCoordinateSystems();
    for (int i = 0; i < csList.size(); i++) {
      CoordinateSystem cs = (CoordinateSystem) csList.get(i);
      if (GridCoordSys.isGridCoordSys( null, cs)) {
        GridCoordSys gcs = new GridCoordSys( cs);
        if (gcs.isComplete( ve))
          return gcs;
      }
    }
    return null;
  }

  public void writeXML(OutputStream os) throws IOException {
    XMLOutputter fmt = new XMLOutputter( Format.getPrettyFormat());
    fmt.output( makeDocument(), os);
  }

  /** Create an XML document from this info */
  public Document makeDocument() {
    Element rootElem = new Element("netcdfDatasetInfo");
    Document doc = new Document(rootElem);
    rootElem.setAttribute("location", ds.getLocation());
    if (coordSysBuilderName != null)
      rootElem.addContent( new Element("convention").setAttribute("name", coordSysBuilderName));

    int nDataVariables = 0;
    int nOtherVariables = 0;

    List axes = ds.getCoordinateAxes();
    int nCoordAxes = axes.size();
    for (int i = 0; i < axes.size(); i++) {
      CoordinateAxis axis =  (CoordinateAxis) axes.get(i);
      Element axisElem = new Element("axis");
      rootElem.addContent(axisElem);

      axisElem.setAttribute("name", axis.getName());
      axisElem.setAttribute("decl", getDecl(axis));
      if (axis.getAxisType() != null)
        axisElem.setAttribute("type", axis.getAxisType().toString());
      if (axis.getUnitsString() != null) {
        axisElem.setAttribute("units", axis.getUnitsString());
        axisElem.setAttribute("udunits", isUdunits(axis.getUnitsString()));
      }
      if (axis instanceof CoordinateAxis1D) {
        CoordinateAxis1D axis1D = (CoordinateAxis1D) axis;
        if (axis1D.isRegular())
          axisElem.setAttribute("regular",  ucar.unidata.util.Format.d( axis1D.getIncrement(), 5));
      }
    }

    List csList = ds.getCoordinateSystems();
    for (int i = 0; i < csList.size(); i++) {
      CoordinateSystem cs =  (CoordinateSystem) csList.get(i);
      Element csElem;
      if (GridCoordSys.isGridCoordSys( null, cs)) {
        GridCoordSys gcs = new GridCoordSys( cs);
        csElem = new Element("gridCoordSystem");
        csElem.setAttribute("name", cs.getName());
        csElem.setAttribute("horizX", gcs.getXHorizAxis().getName());
        csElem.setAttribute("horizY", gcs.getYHorizAxis().getName());
        if (gcs.hasVerticalAxis())
           csElem.setAttribute("vertical", gcs.getVerticalAxis().getName());
        if (gcs.hasTimeAxis())
          csElem.setAttribute("time", cs.getTaxis().getName());
      } else {
        csElem = new Element("coordSystem");
        csElem.setAttribute("name", cs.getName());
      }

      List coordTransforms = cs.getCoordinateTransforms();
      for (int j = 0; j < coordTransforms.size(); j++) {
        CoordinateTransform ct =  (CoordinateTransform) coordTransforms.get(j);
        Element ctElem = new Element("coordTransform");
        csElem.addContent(ctElem);
        ctElem.setAttribute("name", ct.getName());
        ctElem.setAttribute("type", ct.getTransformType().toString());
      }

      rootElem.addContent(csElem);
    }

    List coordTransforms = ds.getCoordinateTransforms();
    for (int i = 0; i < coordTransforms.size(); i++) {
      CoordinateTransform ct =  (CoordinateTransform) coordTransforms.get(i);
      Element ctElem = new Element("coordTransform");
      rootElem.addContent(ctElem);

      ctElem.setAttribute("name", ct.getName());
      ctElem.setAttribute("type", ct.getTransformType().toString());
      List params = ct.getParameters();
      for (int j = 0; j < params.size(); j++) {
        Parameter pp = (Parameter) params.get(j);
        Element ppElem = new Element("param");
        ctElem.addContent(ppElem);
        ppElem.setAttribute("name", pp.getName());
        ppElem.setAttribute("value", pp.getStringValue());
      }
    }

    List vars = ds.getVariables();
    for (int i = 0; i < vars.size(); i++) {
      VariableEnhanced ve =  (VariableEnhanced) vars.get(i);
      if (ve instanceof CoordinateAxis) continue;
      GridCoordSys gcs = getGridCoordSys(ve);
      if (null != gcs) {
        nDataVariables++;
        Element gridElem = new Element("grid");
        rootElem.addContent(gridElem);

        gridElem.setAttribute("name", ve.getName());
        gridElem.setAttribute("decl", getDecl(ve));
        if (ve.getUnitsString() != null) {
          gridElem.setAttribute("units", ve.getUnitsString());
          gridElem.setAttribute("udunits", isUdunits(ve.getUnitsString()));
        }
        gridElem.setAttribute("coordSys", gcs.getName());
      }
    }

    vars = ds.getVariables();
    for (int i = 0; i < vars.size(); i++) {
      VariableEnhanced ve =  (VariableEnhanced) vars.get(i);
      if (ve instanceof CoordinateAxis) continue;
      GridCoordSys gcs = getGridCoordSys(ve);
      if (null == gcs) {
        nOtherVariables++;
        Element elem = new Element("variable");
        rootElem.addContent(elem);

        elem.setAttribute("name", ve.getName());
        elem.setAttribute("decl", getDecl(ve));
        if (ve.getUnitsString() != null) {
          elem.setAttribute("units", ve.getUnitsString());
          elem.setAttribute("udunits", isUdunits(ve.getUnitsString()));
        }
        elem.setAttribute("coordSys", getCoordSys(ve));
      }
    }

    if (nDataVariables > 0) {
      rootElem.addContent( new Element("userAdvice").addContent( "Dataset contains useable gridded data."));
      if (nOtherVariables > 0)
        rootElem.addContent( new Element("userAdvice").addContent( "Some variables are not gridded fields; check that is what you expect."));
    } else {
      if (nCoordAxes == 0)
         rootElem.addContent( new Element("userAdvice").addContent( "No Coordinate Axes were found."));
      else
        rootElem.addContent( new Element("userAdvice").addContent( "No gridded data variables were found."));
    }

    if (userAdvice.length() > 0) {
      StringTokenizer toker = new StringTokenizer(userAdvice.toString(), "\n");
      while (toker.hasMoreTokens())
        rootElem.addContent( new Element("userAdvice").addContent( toker.nextToken()));
    }

    return doc;
  }

  private String getDecl( VariableEnhanced ve) {
    StringBuffer sb = new StringBuffer();
    sb.append(ve.getDataType().toString());
    sb.append(" ");
    ve.getNameAndDimensions(sb, true, true);
    return sb.toString();
  }

  private String getCoordSys( VariableEnhanced ve) {
    List csList = ve.getCoordinateSystems();
    if (csList.size() == 1) {
      CoordinateSystem cs = (CoordinateSystem) csList.get(0);
      return cs.getName();
    } else if (csList.size() > 1) {
      StringBuffer sb = new StringBuffer();
      for (int i = 0; i < csList.size(); i++) {
        CoordinateSystem cs = (CoordinateSystem) csList.get(i);
        if (i > 0) sb.append(";");
        sb.append(cs.getName());
      }
      return sb.toString();
    }
    return " ";
  }

  /* public void writeXML2( java.io.OutputStream os) {
    StringBuffer sb = new StringBuffer();

    PrintStream out = new PrintStream( os);
    out.print("<?xml version='1.0' encoding='UTF-8'?>\n");
    out.print("<netcdfDataset location='"+q(ds.getLocation())+"' >\n");
    if (coordSysBuilderName != null)
      out.print("  <convention name='"+q(coordSysBuilderName)+"'/>\n");
    out.print("\n");

    List axes = ds.getCoordinateAxes();
    for (int i = 0; i < axes.size(); i++) {
      CoordinateAxis axis =  (CoordinateAxis) axes.get(i);
      sb.setLength(0);
      sb.append(axis.getDataType().toString());
      sb.append(" ");
      axis.getNameAndDimensions(sb, true, true);
      out.print("  <axis name='"+q(sb.toString())+"' ");
      if (axis.getAxisType() != null)
        out.print("type='"+axis.getAxisType()+"' ");
      if (axis.getUnitsString() != null) {
        out.print("units='"+q(axis.getUnitsString())+"' ");
        out.print("udunits='"+isUdunits(axis.getUnitsString())+"' ");
      }
      out.print("/>\n");
    }
    out.print("\n");

    int count = 0;
    List coordTransforms = ds.getCoordinateTransforms();
    for (int i = 0; i < coordTransforms.size(); i++) {
      CoordinateTransform ct =  (CoordinateTransform) coordTransforms.get(i);
      out.print("  <transform name='"+q(ct.getName())+"' ");
      out.print(" type='"+q(ct.getTransformType().toString())+"' >\n");
      List params = ct.getParameters();
      for (int j = 0; j < params.size(); j++) {
        Parameter pp = (Parameter) params.get(j);
        out.print("    <param type='"+q(pp.getName())+"' value='"+pp.getStringValue()+"' />\n");
      }
      out.print("  </transform>\n");
      count++;
    }
    if (count > 0) out.print("\n");

    count = 0;
    List vars = ds.getVariables();
    for (int i = 0; i < vars.size(); i++) {
      VariableEnhanced v =  (VariableEnhanced) vars.get(i);
      if (v instanceof CoordinateAxis) continue;
      GridCoordSys gcs = getGridCoordSys(v);
      if (null != gcs) {
        doVar(v, "grid", gcs, out);
        count++;
      }
    }
    if (count > 0) out.print("\n");

    count = 0;
    for (int i = 0; i < vars.size(); i++) {
      VariableEnhanced v =  (VariableEnhanced) vars.get(i);
      if (v instanceof CoordinateAxis) continue;
      if (null == getGridCoordSys(v)) {
        doVar(v, "variable", null, out);
        count++;
      }
    }
    if (count > 0) out.print("\n");

    if (parseInfo != null) {
      out.print("  <parseInfo>\n");
      out.print(parseInfo);
      out.print("  </parseInfo>\n");
    }

    out.print("</netcdfDataset>\n");
    out.flush();
  }


  private void doVar( VariableEnhanced ve, String elementName, CoordinateSystem cs, PrintStream out) {
    StringBuffer sb = new StringBuffer();
    sb.setLength(0);
    sb.append(ve.getDataType().toString());
    sb.append(" ");
    ve.getNameAndDimensions(sb, true, true);
    out.print("  <"+elementName+" name='"+q(sb.toString())+"' ");
    if (ve.getUnitsString() != null) {
      out.print("units='"+q(ve.getUnitsString())+"' ");
      out.print("udunits='"+isUdunits(ve.getUnitsString())+"' ");
    }
    if (cs != null)
      out.print("coordSys='"+q(cs.getName())+"' ");
    else {
      List csList = ve.getCoordinateSystems();
      if (csList.size() == 1) {
        cs = (CoordinateSystem) csList.get(0);
        out.print("coordSys='"+q(cs.getName())+"' ");
      } else if (csList.size() > 1) {
        out.print("/>\n");
      }
    }
    out.print("/>\n");
  }

  private String q(String s) {
    return StringUtil.quoteXmlAttribute(s);
  } */

  private String isUdunits(String unit) {
    SimpleUnit su = SimpleUnit.factory(unit);
    if (null == su) return "false";
    if (su instanceof DateUnit) return "date";
    if (su instanceof TimeUnit) return "time";
    return su.getUnit().getCanonicalString();
  }

  /** debug */
  public static void main(String args[]) throws IOException {
    String url = "C:/data/badmodels/RUC_CONUS_80km_20051211_1900.nc";

    NetcdfDataset ncd = NetcdfDataset.openDataset(url);
    NetcdfDatasetInfo info = ncd.getInfo();
    String infoString = info.writeXML();
    System.out.println(infoString);
  }

}
