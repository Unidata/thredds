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

import java.io.IOException;

/**
 * Describe
 *
 * @author caron
 * @since Aug 18, 2009
 */
public class TableConfigXML {
  private TableConfig tc;
  private String analyserClass;

  TableConfigXML(TableConfig tc, String analyserClass) {
    this.tc = tc;
    this.analyserClass = analyserClass;
  }

  public void writeConfigXML(java.util.Formatter sf) throws IOException {
    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    sf.format("%s", fmt.outputString(makeDocument()));
  }

  /**
   * Create an XML document from this info
   *
   * @return netcdfDatasetInfo XML document
   */
  public Document makeDocument() {
    Element rootElem = new Element("tableConfig");
    Document doc = new Document(rootElem);
    if (analyserClass != null)
      rootElem.addContent( new Element("analyser").setAttribute("class", analyserClass));
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
        //tableElem.addContent(new Element("as").setAttribute("name", config.as));
        tableElem.addContent(new Element("dimension").addContent(config.dim.getName()));
        break;
      case Construct:
        tableElem.addContent(new Element("structure").addContent(config.structName));
        break;
      case Contiguous:
        if (config.start != null)
          tableElem.addContent(new Element("start").addContent(config.start));
        tableElem.addContent(new Element("numRecords").addContent(config.numRecords));
        break;
      case LinkedList:
        tableElem.addContent(new Element("start").addContent(config.start));
        tableElem.addContent(new Element("next").addContent(config.next));
        break;
      case MultidimInner:
      case MultidimInnerPsuedo:
        tableElem.addContent(new Element("outerDimension").addContent(config.outer.getName()));
        tableElem.addContent(new Element("innerDimension").addContent(config.inner.getName()));
        break;
      case NestedStructure:
        tableElem.addContent(new Element("structure").addContent(config.structName));
        break;
      case ParentId:
      case ParentIndex:
        tableElem.addContent(new Element("parentIndex").addContent(config.parentIndex));
        break;
      case Singleton:
        break;
      case MultidimStructure:
      case Structure:
        tableElem.setAttribute("structureType", config.structureType.toString());
        tableElem.addContent(new Element("dimension").addContent(config.dim.getName()));
        break;
      case Top:
        tableElem.addContent(new Element("structure").addContent(config.structName));
        break;
    }

    addCoordinates(tableElem, config);
    if (config.vars != null) {
      for (String col : config.vars)
        tableElem.addContent(new Element("variable").addContent(col));
    }

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

  private void addCoordinates(Element tableElem, TableConfig table) {
    addCoord(tableElem, table.lat, "lat");
    addCoord(tableElem, table.lon, "lon");
    addCoord(tableElem, table.elev, "elev");
    addCoord(tableElem, table.time, "time");
    addCoord(tableElem, table.timeNominal, "timeNominal");
    addCoord(tableElem, table.stnId, "stnId");
    addCoord(tableElem, table.stnDesc, "stnDesc");
    addCoord(tableElem, table.stnNpts, "stnNpts");
    addCoord(tableElem, table.stnWmoId, "stnWmoId");
    addCoord(tableElem, table.stnAlt, "stnAlt");
    addCoord(tableElem, table.limit, "limit");
  }

  private void addCoord(Element tableElem, String name, String type) {
    if (name != null) {
      Element elem = new Element("coordinate").setAttribute("type", type);
      elem.addContent(name);
      tableElem.addContent(elem);
    }
  }

  private Element writeJoinArray(JoinArray join) {
    Element joinElem = new Element("extraJoin");
    joinElem.setAttribute("class", join.getClass().toString());
    if (join.type != null)
      joinElem.setAttribute("type", join.type.toString());
    if (join.v != null)
      joinElem.addContent(new Element("variable").addContent(join.v.getName()));
    joinElem.addContent(new Element("param").setAttribute("value", Integer.toString(join.param)));
    return joinElem;
  }

  private Element writeJoinMuiltdimStructure(JoinMuiltdimStructure join) {
    Element joinElem = new Element("extraJoin");
    joinElem.setAttribute("class", join.getClass().toString());
    if (join.parentStructure != null)
      joinElem.addContent(new Element("parentStructure").addContent(join.parentStructure.getName()));
    joinElem.addContent(new Element("dimLength").setAttribute("value", Integer.toString(join.dimLength)));
    return joinElem;
  }

  private Element writeJoinParentIndex(JoinParentIndex join) {
    Element joinElem = new Element("extraJoin");
    joinElem.setAttribute("class", join.getClass().toString());
    if (join.parentStructure != null)
      joinElem.addContent(new Element("parentStructure").addContent(join.parentStructure.getName()));
    if (join.parentIndex != null)
      joinElem.addContent(new Element("parentIndex").addContent(join.parentIndex));
    return joinElem;
  }
}
