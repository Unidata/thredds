/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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

package thredds.servlet;

import java.util.*;

public class Annotation {
  static private List<Annotation> annotations = new ArrayList<Annotation>();

  static {
    add(new Annotation("cat", "id", "Title", "If this worked you would see something here", "IDV65002.jnlp", "IDV"));
  }

  static void add(Annotation v) {
    annotations.add(v);
  }

  static List<Annotation> getAnnotations() {
    return annotations;
  }

  static List<Annotation> findAnnotation(String cat, String ds, String viewer) {
    List<Annotation> result = new ArrayList<Annotation>();
    for (Annotation v : annotations) {
      if (v.catalog.equals(cat) && v.datasetID.equals(ds) &&
          v.viewer.equals(viewer))
        result.add(v);
    }
    return result;
  }

  /////////////////////////////////////////////////////
  String catalog;
  String datasetID;
  String title;
  String desc;
  String jnlpFilename;
  String viewer;

  Annotation(String catalog, String datasetID, String title, String desc,
             String jnlpFilename, String viewer) {
    this.catalog = catalog;
    this.datasetID = datasetID;
    this.title = title;
    this.desc = desc;
    this.jnlpFilename = jnlpFilename;
    this.viewer = viewer;
  }

  /* public Object readMetadataContent( InvDataset dataset, org.w3c.dom.Element mdataElement) {
    // convert to JDOM document
    Document doc = builder.build(mdataElement);
    Element root = doc.getRootElement();

    ArrayList list = new ArrayList();
    List child = root.getChildren();
    for (int i = 0; i < child.size(); i++) {
      Element s = (Element) child.get(i);
      String catalog = s.getAttributeValue("catalog");
      String datasetID = s.getAttributeValue("datasetID");
      String title = s.getAttributeValue("title");
      String viewer = s.getAttributeValue("viewer");
      String jnlpFile = s.getAttributeValue("jnlpFile");
      String desc = s.getText();
      list.add( new View( catalog, datasetID, title, desc, jnlpFilename, viewer));
    }
    return list;
  }

  public void addMetadataContent( org.w3c.dom.Element mdataElement, Object contentObject) {
    List list = (List) contentObject;
  }
  public Object readMetadataContent( InvDataset dataset, org.w3c.dom.Element mdataElement) {
    // convert to JDOM document
    Document doc = builder.build(mdataElement);
    Element root = doc.getRootElement();

    ArrayList list = new ArrayList();
    List child = root.getChildren();
    for (int i = 0; i < child.size(); i++) {
      Element s = (Element) child.get(i);
      String catalog = s.getAttributeValue("catalog");
      String datasetID = s.getAttributeValue("datasetID");
      String title = s.getAttributeValue("title");
      String viewer = s.getAttributeValue("viewer");
      String jnlpFile = s.getAttributeValue("jnlpFile");
      String desc = s.getText();
      list.add( new View( catalog, datasetID, title, desc, jnlpFilename, viewer));
    }
    return list;
  } */

  void writeHtml(StringBuffer sbuff) {
    sbuff.append("   <li> <a href='views/").append(jnlpFilename).append("'>").append(title).append("</a> : ").append(desc).append("\n");
  }
}