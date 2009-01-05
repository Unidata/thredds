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

package ucar.nc2.dataset;

import ucar.nc2.dt.grid.GridCoordSys;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.TimeUnit;
import ucar.nc2.Variable;
import ucar.unidata.util.Parameter;

import org.jdom.*;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

import java.util.List;
import java.util.StringTokenizer;
import java.util.Formatter;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Helper class for obtaining information about a NetcdfDataset.
 * Creates a "netcdfDatasetInfo" XML document, used by the TDS "Common Data Model Coordinate System Validation".
 *
 * @author john caron
 */
public class NetcdfDatasetInfo {
  private NetcdfDataset ds;
  private CoordSysBuilderIF builder;

  public NetcdfDatasetInfo( String location) throws IOException {
    ds = NetcdfDataset.openDataset(location, false, null);
    builder = CoordSysBuilder.addCoordinateSystems(ds, null);
    //info.setCoordSysBuilderName(conventionName);
    //info.addParseInfo(parseInfo.toString());
    //info.addUserAdvice(userAdvice.toString());
  }

  public void close() throws IOException {
    if (ds != null) ds.close();
  }

  /**
   * Detailed information when the coordinate systems were parsed
   * @return String containing parsing info
   */
  public String getParseInfo( ) {
    return (builder == null) ? "" : builder.getParseInfo();
  }
  /**
   * Specific advice to a user about problems with the coordinate information in the file.
   * @return String containing advice to a user about problems with the coordinate information in the file.
   */
  public String getUserAdvice( ) {
    return (builder == null) ? "" : builder.getUserAdvice();
  }

  /**
   * Get the name of the CoordSysBuilder that parses this file.
   * @return the name of the CoordSysBuilder that parses this file.
   */
  public String getConventionUsed( ) {
    return (builder == null) ? "None" : builder.getConventionUsed();
  }

  /** Write the information as an XML document
   * @return String contining netcdfDatasetInfo XML
   */
  public String writeXML( )  {
    XMLOutputter fmt = new XMLOutputter( Format.getPrettyFormat());
    return fmt.outputString( makeDocument());
  }

  public GridCoordSys getGridCoordSys(VariableEnhanced ve) {
    List<CoordinateSystem> csList = ve.getCoordinateSystems();
    for (CoordinateSystem cs : csList) {
      if (GridCoordSys.isGridCoordSys(null, cs)) {
        GridCoordSys gcs = new GridCoordSys(cs, null);
        if (gcs.isComplete(ve))
          return gcs;
      }
    }
    return null;
  }

  public void writeXML(OutputStream os) throws IOException {
    XMLOutputter fmt = new XMLOutputter( Format.getPrettyFormat());
    fmt.output( makeDocument(), os);
  }

  /** Create an XML document from this info
   * @return netcdfDatasetInfo XML document
   */
  public Document makeDocument() {
    Element rootElem = new Element("netcdfDatasetInfo");
    Document doc = new Document(rootElem);
    rootElem.setAttribute("location", ds.getLocation());
    rootElem.addContent( new Element("convention").setAttribute("name", getConventionUsed()));

    int nDataVariables = 0;
    int nOtherVariables = 0;

    List<CoordinateAxis> axes = ds.getCoordinateAxes();
    int nCoordAxes = axes.size();
    for (CoordinateAxis axis : axes) {
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
          axisElem.setAttribute("regular", ucar.unidata.util.Format.d(axis1D.getIncrement(), 5));
      }
    }

    List<CoordinateSystem> csList = ds.getCoordinateSystems();
    for (CoordinateSystem cs : csList) {
      Element csElem;
      if (GridCoordSys.isGridCoordSys(null, cs)) {
        GridCoordSys gcs = new GridCoordSys(cs, null);
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

      List<CoordinateTransform> coordTransforms = cs.getCoordinateTransforms();
      for (CoordinateTransform ct : coordTransforms) {
        Element ctElem = new Element("coordTransform");
        csElem.addContent(ctElem);
        ctElem.setAttribute("name", ct.getName());
        ctElem.setAttribute("type", ct.getTransformType().toString());
      }

      rootElem.addContent(csElem);
    }

    List<CoordinateTransform> coordTransforms = ds.getCoordinateTransforms();
    for (CoordinateTransform ct : coordTransforms) {
      Element ctElem = new Element("coordTransform");
      rootElem.addContent(ctElem);

      ctElem.setAttribute("name", ct.getName());
      ctElem.setAttribute("type", ct.getTransformType().toString());
      List<Parameter> params = ct.getParameters();
      for (Parameter pp : params) {
        Element ppElem = new Element("param");
        ctElem.addContent(ppElem);
        ppElem.setAttribute("name", pp.getName());
        ppElem.setAttribute("value", pp.getStringValue());
      }
    }

    for (Variable var : ds.getVariables()) {
      VariableEnhanced ve = (VariableEnhanced) var;
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

    for (Variable var : ds.getVariables()) {
      VariableEnhanced ve =  (VariableEnhanced) var;
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

    String userAdvice = getUserAdvice();
    if (userAdvice.length() > 0) {
      StringTokenizer toker = new StringTokenizer(userAdvice, "\n");
      while (toker.hasMoreTokens())
        rootElem.addContent( new Element("userAdvice").addContent( toker.nextToken()));
    }

    return doc;
  }

  private String getDecl( VariableEnhanced ve) {
    Formatter sb = new Formatter();
    sb.format("%s ", ve.getDataType().toString());
    ve.getNameAndDimensions(sb, true, true);
    return sb.toString();
  }

  private String getCoordSys( VariableEnhanced ve) {
    List csList = ve.getCoordinateSystems();
    if (csList.size() == 1) {
      CoordinateSystem cs = (CoordinateSystem) csList.get(0);
      return cs.getName();
    } else if (csList.size() > 1) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < csList.size(); i++) {
        CoordinateSystem cs = (CoordinateSystem) csList.get(i);
        if (i > 0) sb.append(";");
        sb.append(cs.getName());
      }
      return sb.toString();
    }
    return " ";
  }

 private String isUdunits(String unit) {
    try {
      new DateUnit(unit);
      return "date";
    } catch (Exception e) {
      // ok
    }

    try {
      new TimeUnit(unit);
      return "time";
    } catch (Exception e) {
      // ok
    }

    SimpleUnit su = SimpleUnit.factory(unit);
    if (null == su) return "false";
    return su.getCanonicalString();
  }

  /** debug */
  public static void main(String args[]) throws IOException {
    String url = "C:/data/badmodels/RUC_CONUS_80km_20051211_1900.nc";

    NetcdfDatasetInfo info = new NetcdfDatasetInfo(url);
    String infoString = info.writeXML();
    System.out.println(infoString);
  }

}
