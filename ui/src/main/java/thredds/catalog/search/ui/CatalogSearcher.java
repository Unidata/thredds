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

package thredds.catalog.search.ui;

import thredds.catalog.*;
import thredds.ui.*;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;

import ucar.util.prefs.*;
import ucar.util.prefs.ui.*;

import java.io.*;
import java.net.*;
import java.util.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 * Experimental widget for extracting and modifying catalogs. Do not use yet.
 *
 * @author John Caron
 * @version $Id: CatalogSearcher.java 50 2006-07-12 16:30:06Z caron $
 */

public class CatalogSearcher extends JPanel {
  static private final String SOURCE_WINDOW_SIZE = "SourceWindowSize";
  static private final String STATUS_WINDOW_SIZE = "StatusWindowSize";

  private PreferencesExt prefs;
  private Component myParent;

  // ui
  private PrefPanel queryPanel;
  private JSplitPane splitV;
  private TextHistoryPane resultTA;

  private boolean debugEvents = false;

  public CatalogSearcher(PreferencesExt prefs, Component parent) {
    this.prefs = prefs;
    this.myParent = parent;

    queryPanel = makeQueryInputPanel();
    resultTA = new TextHistoryPane(false);

    splitV = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, queryPanel, resultTA);
    splitV.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout( new BorderLayout());
    add( splitV, BorderLayout.CENTER);

    JPanel buttPanel = new JPanel();
    add( buttPanel, BorderLayout.SOUTH);

    queryPanel.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        makeQuery();
      }
    });

  }

  public void save() {
    // prefs.put(FILECHOOSER_DEFAULTDIR, fileChooser.getCurrentDirectory());
    prefs.putInt("splitPos", splitV.getDividerLocation());
  }

  private void makeQuery() {
    resultTA.clear();
    ucar.util.prefs.ui.Field.Text f = (ucar.util.prefs.ui.Field.Text) queryPanel.getField("name");
    String nameValue = f.getText();
    resultTA.appendLine("Look for docs with name containing "+nameValue);


    try {
      Searcher searcher = new IndexSearcher("index");
      Analyzer analyzer = new StandardAnalyzer();

      TermQuery query = new TermQuery( new Term("name", nameValue));
      Hits hits = searcher.search(query);
      resultTA.appendLine(" "+hits.length() + " total matching documents");

      for (int i=0; i<hits.length(); i++) {
        Document doc = hits.doc(i);
        showDoc(doc);
      }
      searcher.close();

    } catch (Exception e) {
     resultTA.appendLine("Lucene ERROR = "+ e.getMessage());
    }

  }

  private void showDoc( Document doc) {

    resultTA.appendLine("Document:");
    Enumeration e = doc.fields();
    while (e.hasMoreElements()) {
      org.apache.lucene.document.Field f = (org.apache.lucene.document.Field)e.nextElement();
      resultTA.appendLine("   "+f.name()+" = "+f.stringValue());
    }
    resultTA.appendLine("");
  }



  //////////////////////////////////////////////////////////////////////////////
  // dataset editor
  private PrefPanel makeQueryInputPanel() {
    PreferencesExt prefNode = (PreferencesExt) prefs.node("queryInput");

    PrefPanel pp = new PrefPanel( "Query", prefNode);
    int row = 0;
    pp.addHeading("Search on", row++);
    pp.addTextField("name", "name", "", 0, row++, null);
    pp.addTextField("keyword", "keyword", "", 0, row++, null);
    pp.addTextField("summary", "summary", "", 0, row++, null);

    pp.finish();
    return pp;
  }

}


/* Change History:
   $Log: CatalogSearcher.java,v $
   Revision 1.3  2004/10/15 19:16:07  caron
   enum now keyword in 1.5
   SelectDateRange send ISO date string

   Revision 1.2  2004/09/24 03:26:30  caron
   merge nj22

   Revision 1.1  2004/06/12 02:01:10  caron
   dqc 0.3

   Revision 1.1  2004/05/11 23:30:32  caron
   release 2.0a

   Revision 1.5  2004/03/05 23:35:48  caron
   rel 1.3.1 javadoc

   Revision 1.3  2004/02/20 00:49:53  caron
   1.3 changes

 */