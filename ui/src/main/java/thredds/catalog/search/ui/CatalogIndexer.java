// $Id: CatalogIndexer.java 50 2006-07-12 16:30:06Z caron $
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
import thredds.catalog.dl.*;
import thredds.catalog.ui.*;
import thredds.ui.*;

import ucar.util.prefs.*;
import ucar.util.prefs.ui.BeanTableSorted;

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
 * @version $Id: CatalogIndexer.java 50 2006-07-12 16:30:06Z caron $
 */

public class CatalogIndexer extends JPanel {
  static private final String SOURCE_WINDOW_SIZE = "SourceWindowSize";
  static private final String STATUS_WINDOW_SIZE = "StatusWindowSize";

  private PreferencesExt prefs;
  private Component myParent;

  // ui
  private CatalogChooser catalogChooser;
  private CatalogTreeView tree;
  private BeanTableSorted dsTable;
  private JSplitPane splitV;
  private ArrayList tables = new ArrayList();
  private IndependentWindow indexWindow = null;
  private TextHistoryPane indexMessages;

  // data
  private InvCatalogImpl currentCatalog = null;
  private DatasetBean currentBean = null;
  private ArrayList beans = new ArrayList(); // dataset beans
  private ArrayList datasets = new ArrayList(); // InvDataset

  // indexer
  private thredds.catalog.search.Indexer indexer = new thredds.catalog.search.Indexer();

  private DIFWriter difWriter = new DIFWriter();
  private ADNWriter adnWriter = new ADNWriter();

  private boolean debugEvents = false;

  public CatalogIndexer(PreferencesExt prefs, Component parent) {
    this.prefs = prefs;
    this.myParent = parent;

    // create the catalog chooser
    PreferencesExt node = (prefs == null) ? null : (PreferencesExt) prefs.node("catIndexer");
    catalogChooser = new CatalogChooser(node, true, false, true);

    tree = catalogChooser.getTreeView();
    tree.addPropertyChangeListener(  new java.beans.PropertyChangeListener() {
      public void propertyChange( java.beans.PropertyChangeEvent e) {
        if (debugEvents) System.out.println("CatalogEditor tree propertyChange= "+e.getPropertyName()+
                           " "+e.getNewValue()+" ("+e.getNewValue().getClass().getName()+")");
         // see if a new catalog is set
        if (e.getPropertyName().equals("Catalog")) {
          datasets = new ArrayList();
          beans = new ArrayList();
          dsTable.setBeans( beans);

        } else if (e.getPropertyName().equals("TreeNode")) {
          InvDatasetImpl ds = (InvDatasetImpl) e.getNewValue();
          addDataset(ds);

        } else if (e.getPropertyName().equals("Selection")) {
          InvDatasetImpl ds = (InvDatasetImpl) e.getNewValue();
          DatasetBean dsBean = findDatasetBean( ds);
          dsTable.setSelectedBean( dsBean);
        }
      }
    });

    // dataset bean table
    dsTable = new BeanTableSorted(DatasetBean.class, (PreferencesExt) prefs.node("dsBeans"), false);
    dsTable.addListSelectionListener( new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        DatasetBean bean = (DatasetBean) dsTable.getSelectedBean();
        InvDatasetImpl selectedDataset = (InvDatasetImpl) bean.dataset();
        catalogChooser.setSelectedDataset( selectedDataset);
      }
    });

    splitV = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, catalogChooser, dsTable);
    splitV.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout( new BorderLayout());
    add( splitV, BorderLayout.CENTER);

    JPanel buttPanel = new JPanel();
    add( buttPanel, BorderLayout.SOUTH);

    JButton openAllButton = new JButton("Open All");
    buttPanel.add(openAllButton);
    openAllButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        tree.openAll( true);
      }
    });

    JButton status = new JButton("Status");
    buttPanel.add(status);
    status.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        indexWindow.show();
      }
    });

     // window to show index results
    indexMessages = new TextHistoryPane( false);
    indexWindow = new IndependentWindow( "Indexing", BAMutil.getImage( "thredds"), indexMessages);
    indexWindow.setBounds((Rectangle)prefs.getBean(SOURCE_WINDOW_SIZE, new Rectangle(50, 50, 850, 450)));

    // add a show source button to catalog chooser
    JButton catSource = new JButton("Index");
    catSource.addActionListener( new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        indexMessages.clear();
        StringBuffer messBuff = new StringBuffer();
        indexer.index(messBuff, getDatasetsToIndex());
        indexMessages.setText( messBuff.toString());
        indexMessages.gotoTop();
        indexWindow.show();
      }
    });
    buttPanel.add(catSource);
  }

  /* private void setCatalog(InvCatalogImpl catalog) {
    this.currentCatalog = catalog;
    addDatasets( catalog.getDataset());
    dsTable.setBeans( beans);
  } */

  public void save() {
    // prefs.put(FILECHOOSER_DEFAULTDIR, fileChooser.getCurrentDirectory());
    prefs.putInt("splitPos", splitV.getDividerLocation());
    catalogChooser.save();
    dsTable.saveState(false);
  }

  /** list of DatasetBean
  public ArrayList getDatasetBeans() { return beans; } */

  /* private void addDatasets(InvDataset ds) {
    beans.add( new DatasetBean( (InvDatasetImpl) ds));

    // skip unread catalogRef
    if (ds instanceof InvCatalogRef) {
      InvCatalogRef catRef = (InvCatalogRef) ds;
      if (!catRef.isRead()) return;
    }

    // recurse
    java.util.List dlist = ds.getDatasets();
    for (int i=0; i<dlist.size(); i++) {
      InvDataset dds = (InvDataset) dlist.get(i);
      addDatasets( dds);
    }
  } */

  public void addDataset(InvDatasetImpl ds) {
    if (datasets.contains( ds))
      return;
    datasets.add( ds);
    DatasetBean bean = new DatasetBean( ds);
    dsTable.addBean( bean);
  }

  public DatasetBean findDatasetBean( InvDataset ds) {
    Iterator iter = beans.iterator();
    while (iter.hasNext()) {
      DatasetBean item = (DatasetBean) iter.next();
      if (item.dataset().equals(ds)) return item;
    }
    return null;
  }

  public ArrayList getDatasetsToIndex() {
    ArrayList sublist = new ArrayList();
    Iterator iter = datasets.iterator();
    while (iter.hasNext()) {
      InvDataset item = (InvDataset) iter.next();
      if (item.isHarvest())
        sublist.add( item);
    }
    return sublist;
  }

  public class DatasetBean {
    private InvDatasetImpl ds;
    private boolean adn, dif, summary, rights;
    private String difMessages;
    int keywords;

    // no-arg constructor
    public DatasetBean() {}

    // create from a dataset
    public DatasetBean( InvDatasetImpl ds) {
      this.ds = ds;
      synch();
   }

    public void synch() {
      String s = ds.getDocumentation("summary");
      summary = (s != null);
      s = ds.getDocumentation("rights");
      rights = (s != null);

      StringBuffer sbuff = new StringBuffer();
      sbuff.append("DIF:\n");
      dif = difWriter.isDatasetUseable( ds, sbuff);
      sbuff.append("\nADN:\n");
      adn = adnWriter.isDatasetUseable( ds, sbuff);
      difMessages = sbuff.toString();

      ds.getKeywords();
      keywords = ds.getKeywords().size();
    }

    public InvDatasetImpl dataset() { return ds; }
    public String status() { return difMessages; }

    //public String getDataType() { return ds.getDataType() == null ? "" : ds.getDataType().toString(); }
    //public String getCollection() { return ds.getCollectionType() == null ? "" : ds.getCollectionType().toString(); }
    public String getId() { return ds.getID() == null ? "" : ds.getID(); }
    public String getName() { return ds.getName(); }

    public boolean isAdn() { return adn; }
    public boolean isDif() { return dif; }
    public boolean isHarvest() { return ds.isHarvest(); }
    public boolean isGeo() {
      ThreddsMetadata.GeospatialCoverage geo = ds.getGeospatialCoverage();
      return (geo != null) && geo.isValid();
    }
    public boolean isTime() { return ds.getTimeCoverage() != null; }
    public boolean isVars() { return ds.getVariables().size() > 0; }
    public boolean isPublish() { return (ds.getPublishers().size() > 0); }
    public boolean isRights() { return rights; }
    public boolean isSummary() { return summary; }

    /** Get keywords */
    public int getKeywords() { return keywords; }
    /** Set keywords */
    public void setKeywords( int keywords) { this.keywords = keywords; }
  }

}


/* Change History:
   $Log: CatalogIndexer.java,v $
   Revision 1.3  2004/09/30 00:33:36  caron
   *** empty log message ***

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