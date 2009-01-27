// $Id: CatalogSearcher.java 50 2006-07-12 16:30:06Z caron $
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

package thredds.catalog.ui.tools;

import thredds.catalog.*;
import thredds.ui.*;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.*;

import ucar.util.prefs.*;
import ucar.util.prefs.ui.*;
import ucar.nc2.constants.FeatureType;

import java.io.*;
import java.util.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * GUI interface to catalog search service.
 *
 * @author John Caron
 * @version $Id: CatalogSearcher.java 50 2006-07-12 16:30:06Z caron $
 */

public class CatalogSearcher extends JPanel {
  static private final String SOURCE_WINDOW_SIZE = "SourceWindowSize";
  static private final String STATUS_WINDOW_SIZE = "StatusWindowSize";

  private PreferencesExt prefs;

  // ui
  private PrefPanel queryPanel;
  private JSplitPane splitV;
  private HtmlBrowser htmlPanel;

  private boolean debugEvents = false;

  public CatalogSearcher(PreferencesExt prefs) {
    this.prefs = prefs;

    //create widgets
    queryPanel = makeSearchPanel();
    JButton clear = new JButton("Clear");
    queryPanel.addButton(clear);

    htmlPanel = new HtmlBrowser();

    // layout
    splitV = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, queryPanel, htmlPanel);
    splitV.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout( new BorderLayout());
    add( splitV, BorderLayout.CENTER);

    // event handling
    queryPanel.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String queryString = makeQuery().trim();
        if (queryString.length() == 0) return;
        String resultPage = doQuery( queryString);
        htmlPanel.setContent(queryString, resultPage);
      }
    });

    htmlPanel.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
      public void propertyChange(java.beans.PropertyChangeEvent e) {
        if (e.getPropertyName().equals("datasetURL")) {
          String urlString = (String) e.getNewValue();
          System.out.println("***CatalogSearcher datasetURL= " + urlString);
        }
      }
    });

   clear.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        // do something
      }
    });

  }

  public void save() {
    // prefs.put(FILECHOOSER_DEFAULTDIR, fileChooser.getCurrentDirectory());
    prefs.putInt("splitPos", splitV.getDividerLocation());
  }

  private String makeQuery() {
    StringBuffer sbuff = new StringBuffer();

    Field.Text f = (Field.Text) queryPanel.getField("lucene");
    String value = f.getText().trim();
    if (value.length() > 0)
      return value;

    addEnumQuery( "DataType", sbuff);
    addEnumQuery( "DataFormatType", sbuff);
    addEnumQuery( "ServiceType", sbuff);

    addTextQuery( "name", sbuff);
    addTextQuery( "keyword", sbuff);
    addTextQuery( "summary", sbuff);

    System.out.println("Look for docs using query = "+sbuff.toString());
    return sbuff.toString();
  }

  private void addTextQuery(String fieldName, StringBuffer result) {
    Field.Text f = (Field.Text) queryPanel.getField(fieldName);
    String value = f.getText();
    if (value == null)
      return;
    value = value.trim();
    if (value.length() == 0)
      return;

    StringTokenizer stoker = new StringTokenizer(value);
    while (stoker.hasMoreTokens()) {
      String toke = stoker.nextToken();
      if (result.length() > 0)
        result.append(" AND ");
      result.append(fieldName);
      result.append(":");
      result.append(value);
    }
  }

  private void addEnumQuery(String fieldName, StringBuffer result) {
    Field.EnumCombo f = (Field.EnumCombo) queryPanel.getField(fieldName);
    String value = f.getValue().toString();
    if (value == null)
      return;
    value = value.trim();
    if (value.length() == 0)
      return;

    StringTokenizer stoker = new StringTokenizer(value);
    while (stoker.hasMoreTokens()) {
      String toke = stoker.nextToken();
      if (result.length() > 0)
        result.append(" AND ");
      result.append(fieldName);
      result.append(":");
      result.append(value);
    }
  }

  private String doQuery( String queryString) {

    try {
      Searcher searcher = new IndexSearcher("index");
      Analyzer analyzer = new StandardAnalyzer();

      Query query = QueryParser.parse(queryString, "contents", analyzer);
      System.out.println("Searching for: " + query.toString("contents"));

      Hits hits = searcher.search(query);
      System.out.println(" "+hits.length() + " total matching documents");

      String result = makePage( queryString, hits);
      searcher.close();
      return result;

    } catch (Exception e) {
      System.out.println("Lucene ERROR = "+ e.getMessage());
      return null;
    }

  }

  private String makePage(String qs, Hits hits) throws IOException {
    StringBuffer buff = new StringBuffer(20000);

    buff.append( "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n" )
        .append( "        \"http://www.w3.org/TR/html4/loose.dtd\">\n" )
        .append( "<html>\n" );
    buff.append("<head>");
    buff.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">");
    buff.append("</head>");
    buff.append("<body>\n");

    for (int i = 0; i < hits.length(); i++) {
      Document doc = hits.doc(i);
      doOneDoc(buff, doc);
    }

    buff.append("</body></html>");

    System.out.println("html="+buff.toString());
    return buff.toString();
  }

  private String threddsServer="http://motherlode.ucar.edu:8088/thredds/subset.html?";
  private void doOneDoc( StringBuffer buff, Document doc) {

    buff.append("<h3>Dataset: "+doc.getField("name").stringValue()+"</h3><ul>\n");

    Enumeration e = doc.fields();
    while (e.hasMoreElements()) {
      org.apache.lucene.document.Field f = (org.apache.lucene.document.Field)e.nextElement();
      if (f.name().equals("name")) continue;
      if (f.name().equals("subsetURL")) continue;
      buff.append(" <li><b>"+f.name()+":</b> "+f.stringValue()+"</li>\n");
    }

    String subsetURL = doc.getField("subsetURL").stringValue();
    buff.append(" <li><a href='"+threddsServer+subsetURL+"'>"+subsetURL+"</a>\n");

    buff.append("</ul>\n");
  }



  //////////////////////////////////////////////////////////////////////////////
  // dataset editor
  private PrefPanel makeSearchPanel() {
    PreferencesExt prefNode = (PreferencesExt) prefs.node("queryInput");

    PrefPanel pp = new PrefPanel( "Query", null);
    int row = 0;
    pp.addHeading("Find Datasets that must have:", row++);

    pp.addEnumComboField("DataType", "Data type", Arrays.asList(FeatureType.values()),
        true, 0, row, null);

    pp.addEnumComboField("ServiceType", "Service type", ServiceType.getAllTypes(),
        true, 2, row, null);

    pp.addEnumComboField("DataFormatType", "Data format", DataFormatType.getAllTypes(),
        true, 4, row++, null);

    pp.addTextField("name", "name", "", 0, row++, null);
    pp.addTextField("keyword", "keyword", "", 0, row++, null);
    pp.addTextField("summary", "summary", "", 0, row++, null);

    pp.addSeparator();
    pp.addHeading("OR Enter a direct lucene query:", row++);
    pp.addTextField("lucene", "Lucene Query", "", 0, row++, null);

    pp.finish();
    return pp;
  }

}


/* Change History:
   $Log: CatalogSearcher.java,v $
   Revision 1.2  2006/01/20 20:49:06  caron
   disambiguate DataType

   Revision 1.1  2004/11/04 20:16:43  caron
   no message

   Revision 1.4  2004/10/15 19:16:07  caron
   enum now keyword in 1.5
   SelectDateRange send ISO date string

   Revision 1.3  2004/09/30 00:33:37  caron
   *** empty log message ***

   Revision 1.2  2004/09/24 03:26:31  caron
   merge nj22

   Revision 1.1  2004/06/12 02:01:11  caron
   dqc 0.3

   Revision 1.1  2004/05/11 23:30:32  caron
   release 2.0a

   Revision 1.5  2004/03/05 23:35:48  caron
   rel 1.3.1 javadoc

   Revision 1.3  2004/02/20 00:49:53  caron
   1.3 changes

 */