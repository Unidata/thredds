/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.ft.point.standard;

import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.ArrayList;
import java.util.Formatter;

import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.util.IO;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.Structure;
import ucar.ma2.DataType;

/**
 * Helper class to convert a  TableConfig to and from XML
 *
 * @author caron
 * @since Aug 18, 2009
 */
public class PointConfigXML {

  public static void writeConfigXML(FeatureDatasetPoint pfd, java.util.Formatter f) {
    if (!(pfd instanceof PointDatasetStandardFactory.PointDatasetStandard)) {
      f.format("%s not instance of PointDatasetStandard%n", pfd.getLocation());
      return;
    }
    PointDatasetStandardFactory.PointDatasetStandard spfd = (PointDatasetStandardFactory.PointDatasetStandard) pfd;
    TableAnalyzer analyser = spfd.getTableAnalyzer();
    TableConfig config = analyser.getTableConfig();
    TableConfigurer tc = analyser.getTableConfigurer();
    if (tc == null) {
      f.format("%s has no TableConfig%n", pfd.getLocation());
      return;
    }

    PointConfigXML writer = new PointConfigXML();
    try {
      writer.writeConfigXML(config, tc.getClass().getName(), f);
    } catch (IOException e) {
      f.format("%s error writing=%s%n", pfd.getLocation(), e.getMessage());
      return;
    }
  }

  ////////////////////////////////////////////////////////////////////
  private TableConfig tc;
  private String tableConfigurerClass;

  public void writeConfigXML(TableConfig tc, String tableConfigurerClass, java.util.Formatter sf) throws IOException {
    this.tc = tc;
    this.tableConfigurerClass = tableConfigurerClass;

    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    sf.format("%s", fmt.outputString(makeDocument()));
  }

  /**
   * Create an XML document from this info
   *
   * @return netcdfDatasetInfo XML document
   */
  public Document makeDocument() {
    Element rootElem = new Element("pointConfig");
    Document doc = new Document(rootElem);
    if (tableConfigurerClass != null)
      rootElem.addContent( new Element("tableConfigurer").setAttribute("class", tableConfigurerClass));
    if (tc.featureType != null)
      rootElem.setAttribute("featureType", tc.featureType.toString());

    rootElem.addContent(writeTable(tc));

    return doc;
  }

  private Element writeTable(TableConfig config) {
    Element tableElem = new Element("table");

    //if (config.name != null)
    //  tableElem.setAttribute("name", config.name);
    if (config.type != null)
      tableElem.setAttribute("type", config.type.toString());

    switch (config.type) {
      case ArrayStructure:
        tableElem.setAttribute("dimension", config.dimName);
        break;

      case Construct:
        break;

      case Contiguous:
        if (config.start != null)
          tableElem.setAttribute("start", config.start);
        tableElem.setAttribute("numRecords", config.numRecords);
        break;

      case LinkedList:
        tableElem.setAttribute("start", config.start);
        tableElem.setAttribute("next", config.next);
        break;

      case MultidimInner:
        tableElem.setAttribute("structName", config.structName);
      case MultidimInnerPsuedo:
        tableElem.setAttribute("dim0", config.outerName);
        tableElem.setAttribute("dim1", config.innerName);
        tableElem.setAttribute("subtype", config.structureType.toString());
        break;

      case MultidimInner3D:
        break;

      case MultidimInnerPsuedo3D:
        break;

      case MultidimStructure:
        tableElem.setAttribute("structName", config.structName);
        break;

      case NestedStructure:
        tableElem.setAttribute("structName", config.structName);
        break;

      case ParentId:
      case ParentIndex:
        tableElem.setAttribute("parentIndex", config.parentIndex);
        break;

      case Singleton:
        break;

      case Structure:
        tableElem.setAttribute("subtype", config.structureType.toString());
        switch (config.structureType) {
          case Structure:
            tableElem.setAttribute("structName", config.structName);
            break;
          case PsuedoStructure:
            tableElem.setAttribute("dim", config.dimName);
            break;
          case PsuedoStructure2D:
            tableElem.setAttribute("dim0", config.dimName);
            tableElem.setAttribute("dim1", config.outerName);
            break;
        }
        break;

      case Top:
        tableElem.setAttribute("structName", config.structName);
        break;

    }

    List<String> varNames = (config.vars == null) ? new ArrayList<String>() : new ArrayList<String>(config.vars);

    // add coordinates
    for (Table.CoordName coord : Table.CoordName.values()) {
      addCoord(tableElem, config, coord, varNames);
    }

    // add variables
    if (config.vars != null) {
      for (String col : varNames)
        tableElem.addContent(new Element("variable").addContent(col));
    }

    // add joins
    if (config.extraJoin != null) {
      for (Join j : config.extraJoin) {
        if (j instanceof JoinArray)
          tableElem.addContent(writeJoinArray((JoinArray) j));
        else if (j instanceof JoinMuiltdimStructure)
          tableElem.addContent(writeJoinMuiltdimStructure((JoinMuiltdimStructure) j));
        else if (j instanceof JoinParentIndex)
          tableElem.addContent(writeJoinParentIndex((JoinParentIndex) j));
      }
    }

    if (config.children != null) {
      for (TableConfig child : config.children)
        tableElem.addContent(writeTable(child));
    }
    return tableElem;
  }

  private void addCoord(Element tableElem, TableConfig table, Table.CoordName type, List<String> varNames) {
    String name = table.findCoordinateVariableName(type);
    if (name != null) {
      Element elem = new Element("coordinate").setAttribute("type", type.name());
      elem.addContent(name);
      tableElem.addContent(elem);
      varNames.remove(name);
    }
  }

  private Element writeJoinArray(JoinArray join) {
    Element joinElem = new Element("join");
    joinElem.setAttribute("class", join.getClass().toString());
    if (join.type != null)
      joinElem.setAttribute("type", join.type.toString());
    if (join.v != null)
      joinElem.addContent(new Element("variable").addContent(join.v.getName()));
    joinElem.addContent(new Element("param").addContent(Integer.toString(join.param)));
    return joinElem;
  }

  private JoinArray readJoinArray(NetcdfDataset ds, Element joinElement) {
    JoinArray.Type type = JoinArray.Type.valueOf(joinElement.getAttributeValue("type"));
    Element paramElem = joinElement.getChild("param");
    String paramS = paramElem.getText();
    Integer param = Integer.parseInt(paramS);

    Element varElem = joinElement.getChild("variable");
    String varName = varElem.getText();
    VariableDS v = (VariableDS) ds.findVariable(varName);
    return new JoinArray(v, type, param);
  }

  private Element writeJoinMuiltdimStructure(JoinMuiltdimStructure join) {
    Element joinElem = new Element("join");
    joinElem.setAttribute("class", join.getClass().toString());
    if (join.parentStructure != null)
      joinElem.addContent(new Element("parentStructure").addContent(join.parentStructure.getName()));
    joinElem.addContent(new Element("dimLength").setAttribute("value", Integer.toString(join.dimLength)));
    return joinElem;
  }

  private Element writeJoinParentIndex(JoinParentIndex join) {
    Element joinElem = new Element("join");
    joinElem.setAttribute("class", join.getClass().toString());
    if (join.parentStructure != null)
      joinElem.addContent(new Element("parentStructure").addContent(join.parentStructure.getName()));
    if (join.parentIndex != null)
      joinElem.addContent(new Element("parentIndex").addContent(join.parentIndex));
    return joinElem;
  }

  ///////////////////////////////////////////////////////////////////////////
  private static boolean debugXML = false;
  private static boolean debugURL = false;
  private static boolean showParsedXML = false;

  public TableConfig readConfigXMLfromResource(String resourceLocation, FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) throws IOException {
    ClassLoader cl = this.getClass().getClassLoader();
    InputStream is = cl.getResourceAsStream(resourceLocation);
    if (is == null)
      throw new FileNotFoundException(resourceLocation);

    if (debugXML) {
      System.out.println(" PointConfig URL = <" + resourceLocation + ">");
      InputStream is2 = cl.getResourceAsStream(resourceLocation);
      System.out.println(" contents=\n" + IO.readContents(is2));
    }

    org.jdom.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder(false);
      if (debugURL) System.out.println(" PointConfig URL = <" + resourceLocation + ">");
      doc = builder.build(is);
    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }
    if (debugXML) System.out.println(" SAXBuilder done");

    if (showParsedXML) {
      XMLOutputter xmlOut = new XMLOutputter();
      System.out.println("*** PointConfig/showParsedXML = \n" + xmlOut.outputString(doc) + "\n*******");
    }

    Element configElem = doc.getRootElement();
    String featureType = configElem.getAttributeValue("featureType");
    Element tableElem = configElem.getChild("table");
    TableConfig tc = parseTableConfig(ds, tableElem, null);
    tc.featureType = FeatureType.valueOf(featureType);

    return tc;
  }

  public TableConfig readConfigXML(String fileLocation, FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) throws IOException {

    org.jdom.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder(false);
      if (debugURL) System.out.println(" PointConfig URL = <" + fileLocation + ">");
      doc = builder.build(fileLocation);
    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }
    if (debugXML) System.out.println(" SAXBuilder done");

    if (showParsedXML) {
      XMLOutputter xmlOut = new XMLOutputter();
      System.out.println("*** PointConfig/showParsedXML = \n" + xmlOut.outputString(doc) + "\n*******");
    }

    Element configElem = doc.getRootElement();
    String featureType = configElem.getAttributeValue("featureType");
    Element tableElem = configElem.getChild("table");
    TableConfig tc = parseTableConfig(ds, tableElem, null);
    tc.featureType = FeatureType.valueOf(featureType);

    return tc;
  }

  private TableConfig parseTableConfig(NetcdfDataset ds, Element tableElem, TableConfig parent) {
    String typeS = tableElem.getAttributeValue("type");
    Table.Type ttype = Table.Type.valueOf(typeS);

    String name = tableElem.getAttributeValue("name");
    TableConfig tc = new TableConfig(ttype, name);

    switch (ttype) {
      case ArrayStructure:
        tc.dimName = tableElem.getAttributeValue("dimension");
        break;

      case Construct:
        break;

      case Contiguous:
        tc.numRecords = tableElem.getAttributeValue("numRecords");
        tc.start = tableElem.getAttributeValue("start");
        break;

      case LinkedList:
        tc.start = tableElem.getAttributeValue("start");
        tc.next = tableElem.getAttributeValue("next");
        break;

      case MultidimInner:
        tc.structName = tableElem.getAttributeValue("structName");
      case MultidimInnerPsuedo:
        tc.outerName = tableElem.getAttributeValue("dim0");
        tc.dimName = tc.outerName;
        tc.innerName = tableElem.getAttributeValue("dim1");
        tc.structureType = TableConfig.StructureType.valueOf(tableElem.getAttributeValue("subtype"));
        makeMultidimInner(ds, parent, tc);
        break;

      case MultidimInner3D:
        break;

      case MultidimInnerPsuedo3D:
        break;

      case MultidimStructure:
        tc.structName = tableElem.getAttributeValue("structName");
        break;

      case NestedStructure:
        tc.structName = tableElem.getAttributeValue("structName");
        break;

      case ParentId:
      case ParentIndex:
        tc.parentIndex = tableElem.getAttributeValue("parentIndex");
        break;

      case Singleton:
        break;

      case Structure:
        tc.structureType = TableConfig.StructureType.valueOf(tableElem.getAttributeValue("subtype"));
        switch (tc.structureType) {
          case Structure:
            tc.structName = tableElem.getAttributeValue("structName");
            break;
          case PsuedoStructure:
            tc.dimName = tableElem.getAttributeValue("dim");
            break;
          case PsuedoStructure2D:
            tc.dimName = tableElem.getAttributeValue("dim0");
            tc.outerName = tableElem.getAttributeValue("dim1");
            break;
        }
        break;

      case Top:
        tc.structName = tableElem.getAttributeValue("structName");
        break;

    }

    List<Element> coordList = (List<Element>) tableElem.getChildren("coordinate");
    for (Element coordElem : coordList) {
      String coordNameType = coordElem.getAttributeValue("type");
      Table.CoordName coordName = Table.CoordName.valueOf(coordNameType);
      tc.setCoordinateVariableName(coordName, coordElem.getText());
    }

    List<Element> joinList = (List<Element>) tableElem.getChildren("join");
    for (Element joinElem : joinList) {
      tc.addJoin( readJoinArray(ds, joinElem));
    }

    List<Element> nestedTableList = (List<Element>) tableElem.getChildren("table");
    for (Element nestedTable : nestedTableList)
      tc.addChild( parseTableConfig(ds, nestedTable, tc));

    return tc;
  }

  private void makeMultidimInner(NetcdfDataset ds, TableConfig parentTable, TableConfig childTable) {
    Dimension parentDim = ds.findDimension(parentTable.dimName);
    Dimension childDim = ds.findDimension(childTable.innerName);

    // divide up the variables between the parent and the child
    List<String> obsVars = null;
    List<Variable> vars = ds.getVariables();
    List<String> parentVars = new ArrayList<String>(vars.size());
    obsVars = new ArrayList<String>(vars.size());
    for (Variable orgV : vars) {
      if (orgV instanceof Structure) continue;

      Dimension dim0 = orgV.getDimension(0);
      if ((dim0 != null) && dim0.equals(parentDim)) {
        if ((orgV.getRank() == 1) || ((orgV.getRank() == 2) && orgV.getDataType() == DataType.CHAR)) {
          parentVars.add(orgV.getShortName());
        } else {
          Dimension dim1 = orgV.getDimension(1);
          if ((dim1 != null) && dim1.equals(childDim))
            obsVars.add(orgV.getShortName());
        }
      }
    }
    parentTable.vars = parentVars;
    childTable.vars = obsVars;
  }

  /* the inner table of Structure(outer, middle, inner)
  private TableConfig makeMultidimInner3D(NetcdfDataset ds, TableConfig outerTable, TableConfig middleTable, Dimension innerDim, Formatter errlog) throws IOException {
    Dimension outerDim = ds.findDimension(outerTable.dimName);
    Dimension middleDim = ds.findDimension(middleTable.innerName);

    Table.Type obsTableType = (outerTable.structureType == TableConfig.StructureType.PsuedoStructure) ? Table.Type.MultidimInnerPsuedo3D : Table.Type.MultidimInner3D;
    TableConfig obsTable = new TableConfig(obsTableType, innerDim.getName());
    obsTable.structureType = TableConfig.StructureType.PsuedoStructure2D;
    obsTable.dimName = outerTable.dimName;
    obsTable.outerName = middleTable.innerName;
    obsTable.innerName = innerDim.getName();
    obsTable.structName = innerDim.getName();

    obsTable.lat = matchAxisTypeAndDimension(ds, AxisType.Lat, outerDim, middleDim, innerDim);
    obsTable.lon = matchAxisTypeAndDimension(ds, AxisType.Lon, outerDim, middleDim, innerDim);
    obsTable.elev = matchAxisTypeAndDimension(ds, AxisType.Height, outerDim, middleDim, innerDim);
    obsTable.time = matchAxisTypeAndDimension(ds, AxisType.Time, outerDim, middleDim, innerDim);

    // divide up the variables between the 3 tables
    List<Variable> vars = ds.getVariables();
    List<String> outerVars = new ArrayList<String>(vars.size());
    List<String> middleVars = new ArrayList<String>(vars.size());
    List<String> innerVars = new ArrayList<String>(vars.size());
    for (Variable orgV : vars) {
      if (orgV instanceof Structure) continue;

      if ((orgV.getRank() == 1) || ((orgV.getRank() == 2) && orgV.getDataType() == DataType.CHAR)) {
        if (outerDim.equals(orgV.getDimension(0)))
          outerVars.add(orgV.getShortName());

      } else if (orgV.getRank() == 2) {
        if (outerDim.equals(orgV.getDimension(0)) && middleDim.equals(orgV.getDimension(1)))
          middleVars.add(orgV.getShortName());

      } else if (orgV.getRank() == 3) {
        if (outerDim.equals(orgV.getDimension(0)) && middleDim.equals(orgV.getDimension(1)) && innerDim.equals(orgV.getDimension(2)))
          innerVars.add(orgV.getShortName());
      }
    }
    outerTable.vars = outerVars;
    middleTable.vars = middleVars;
    obsTable.vars = innerVars;

    return obsTable;
  } */

}
