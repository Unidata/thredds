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

package thredds.servlet;

import java.util.*;

/**
 * Not currently used.
 */
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