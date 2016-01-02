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

package ucar.nc2.dataset;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.dt.grid.GridCoordSys;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.SimpleUnit;
import ucar.unidata.util.Parameter;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Formatter;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Helper class for obtaining information about a NetcdfDataset.
 * Creates a "netcdfDatasetInfo" XML document, used by the TDS "Common Data Model Coordinate System Validation".
 *
 * @author john caron
 */
public class NetcdfDatasetInfo implements Closeable {
  private NetcdfDataset ds;
  private CoordSysBuilderIF builder;

  public NetcdfDatasetInfo( String location) throws IOException {
    ds = NetcdfDataset.openDataset(location, false, null);
    builder = ds.enhance();
  }

  public NetcdfDatasetInfo( NetcdfDataset ds) throws IOException {
    File loc = new File(ds.getLocation());
    if (loc.exists()) {
      this.ds = NetcdfDataset.openDataset(ds.getLocation(), false, null); // fresh enhancement
      builder = this.ds.enhance();
    } else {  // Aggregation, fc may not exist
      this.ds = ds;  // LOOK what can we do thats better ?
      builder = ds.enhance();
    }
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
   * @return String containing netcdfDatasetInfo XML
   */
  public String writeXML( )  {
    XMLOutputter fmt = new XMLOutputter( Format.getPrettyFormat());
    return fmt.outputString( makeDocument());
  }

  public GridCoordSys getGridCoordSys(VariableEnhanced ve) {
    List<CoordinateSystem> csList = ve.getCoordinateSystems();
    for (CoordinateSystem cs : csList) {
      if (GridCoordSys.isGridCoordSys(null, cs, ve)) {
        return new GridCoordSys(cs, null);
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

      axisElem.setAttribute("name", axis.getFullName());
      axisElem.setAttribute("decl", getDecl(axis));
      if (axis.getAxisType() != null)
        axisElem.setAttribute("type", axis.getAxisType().toString());
      if (axis.getUnitsString() != null) {
        axisElem.setAttribute(CDM.UNITS, axis.getUnitsString());
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
      if (GridCoordSys.isGridCoordSys(null, cs, null)) {
        GridCoordSys gcs = new GridCoordSys(cs, null);
        csElem = new Element("gridCoordSystem");
        csElem.setAttribute("name", cs.getName());
        csElem.setAttribute("horizX", gcs.getXHorizAxis().getFullName());
        csElem.setAttribute("horizY", gcs.getYHorizAxis().getFullName());
        if (gcs.hasVerticalAxis())
          csElem.setAttribute("vertical", gcs.getVerticalAxis().getFullName());
        if (gcs.hasTimeAxis())
          csElem.setAttribute("time", cs.getTaxis().getFullName());
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

        gridElem.setAttribute("name", ve.getFullName());
        gridElem.setAttribute("decl", getDecl(ve));
        if (ve.getUnitsString() != null) {
          gridElem.setAttribute(CDM.UNITS, ve.getUnitsString());
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

        elem.setAttribute("name", ve.getFullName());
        elem.setAttribute("decl", getDecl(ve));
        if (ve.getUnitsString() != null) {
          elem.setAttribute(CDM.UNITS, ve.getUnitsString());
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

    SimpleUnit su = SimpleUnit.factory(unit);
    if (null == su) return "false";
    return su.getCanonicalString();
  }

  /** debug */
  public static void main(String args[]) throws IOException {
    String url = "C:/data/badmodels/RUC_CONUS_80km_20051211_1900.nc";

    try (NetcdfDatasetInfo info = new NetcdfDatasetInfo(url)) {
      String infoString = info.writeXML();
      System.out.println(infoString);
    }
  }

}
