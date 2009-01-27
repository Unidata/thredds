// $Id: GridDatasetInfo.java,v 1.3 2006/06/06 16:07:12 caron Exp $
/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
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

package ucar.nc2.dataset.grid;

import ucar.nc2.dataset.*;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.TimeUnit;
import ucar.unidata.util.Parameter;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.Document;
import org.jdom.Element;

import java.util.List;
import java.util.StringTokenizer;
import java.io.OutputStream;
import java.io.IOException;
import java.io.FileOutputStream;

/**
 *  @deprecated (use ucar.nc2.dt.grid.GridDatasetInfo)
 */
public class GridDatasetInfo {
  private GridDataset gds;
  private String path;
  private StringBuffer parseInfo = new StringBuffer();
  private StringBuffer userAdvice = new StringBuffer();

  public GridDatasetInfo( GridDataset gds, String path) {
    this.gds = gds;
    this.path = path;
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

  /** Write the information as an XML document */
  public String writeXML( )  {
    XMLOutputter fmt = new XMLOutputter( Format.getPrettyFormat());
    return fmt.outputString( makeDocument());
  }

  public void writeXML(OutputStream os) throws IOException {
    XMLOutputter fmt = new XMLOutputter( Format.getPrettyFormat());
    fmt.output( makeDocument(), os);
  }

  /** Create an XML document from this info */
  public Document makeDocument() {
    Element rootElem = new Element("gridDatasetInfo");
    Document doc = new Document(rootElem);
    rootElem.setAttribute("location", gds.getName());
    rootElem.setAttribute("path", path);

    int nDataVariables = 0;
    int nOtherVariables = 0;

    List grids = gds.getGrids();
    for (int i = 0; i < grids.size(); i++) {
      GeoGrid grid =  (GeoGrid) grids.get(i);
      nDataVariables++;
      Element gridElem = new Element("grid");
      rootElem.addContent(gridElem);
      gridElem.setAttribute("name", grid.getName());
    }

    return doc;
  }

  /* private String getDecl( VariableEnhanced ve) {
    StringBuffer sb = new StringBuffer();
    sb.append(ve.getDataType().toString());
    sb.append(" ");
    ve.getNameAndDimensions(sb, true, true);
    return sb.toString();
  } */

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

  /** debug */
  public static void main(String args[]) throws IOException {
    String url = "C:/data/grib/ruc/c20s/RUC2_CONUS_20km_surface_20060327_0900.grib1";

    GridDataset ncd = GridDataset.open(url);
    GridDatasetInfo info = new GridDatasetInfo( ncd, "path");
    FileOutputStream fos2 = new FileOutputStream("C:/TEMP/gridInfo.xml");
    info.writeXML(fos2);
    fos2.close();

    String infoString = info.writeXML();    
    System.out.println(infoString);
  }

}

