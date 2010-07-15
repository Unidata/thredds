// $Id: TDServerConfigurator.java 50 2006-07-12 16:30:06Z caron $
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

import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.*;
import ucar.unidata.util.StringUtil;
import ucar.nc2.util.net.HttpClientManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;

import thredds.catalog.ui.CatalogTreeView;
import thredds.catalog.*;
import thredds.catalog.crawl.CatalogExtractor;
import thredds.ui.*;
import thredds.ui.PopupMenu;
import ucar.nc2.util.IO;
//import thredds.util.net.HttpSession;

/**
 * Experimental widget for creating Thredds Data server configuration Catalogs.
 *
 * @author John Caron
 * @version $Id: TDServerConfigurator.java 50 2006-07-12 16:30:06Z caron $
 */

public class TDServerConfigurator extends JPanel {
  static private final String SOURCE_WINDOW_SIZE = "SourceWindowSize";
  static private final String EXTRACT_WINDOW_SIZE = "ExtractWindowSize";
  static private final String SAVE_WINDOW_SIZE = "SaveWindowSize";
  static private final String SAVE_HTML_SIZE = "SaveHtmlWindowSize";

  static private final String SPLIT_POS = "SplitPos";

  private PreferencesExt prefs;
  private Component myParent;

  // ui
  private ComboBox catalogCB;
  private CatalogTreeView catTree;
  private DatasetEditor datasetEditor;
  private JSplitPane splitH;

  private IndependentWindow sourceWindow, extractWindow, saveWindow, htmlWindow;
  private TextHistoryPane sourcePane, extractPane;
  private TextGetPutPane savePane;
  private HtmlBrowser htmlViewer;

  // data
  private CatalogExtractor extractor = new CatalogExtractor( false);
  private String catalogPath;

  private boolean debugEvents = false, debugSave = false;

  public TDServerConfigurator(PreferencesExt prefs, Component parent) {
    this.prefs = prefs;
    this.myParent = parent;

    catalogCB = new ComboBox((prefs == null) ? null : (PreferencesExt) prefs.node("catalogCB"));

    // top panel buttons
    JButton connectButton = new JButton("Connect");
    connectButton.setToolTipText("Read the Catalog");
    connectButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        catalogPath = (String) catalogCB.getSelectedItem();
        catTree.setCatalog(catalogPath); // send Catalog event from catTree
      }
    });

    JButton catSource = new JButton("Source");
    catSource.addActionListener( new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        // String catalogPath = (String) catalogCB.getSelectedItem();
        InvCatalogImpl cat = (InvCatalogImpl) catTree.getCatalog();
        ByteArrayOutputStream os = new ByteArrayOutputStream(60*1000);
        try {
          cat.writeXML (os, true);
        } catch (IOException e1) {
          e1.printStackTrace();
        }
        sourcePane.setText(os.toString());
        sourceWindow.show();

        if (debugSave) {
          try {
            IO.writeToFile(os.toString(), "C:/dev/netcdf-java-2.2/test/serverConfig/start.xml");
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    });

    JButton saveSource = new JButton("Save");
    saveSource.addActionListener( new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        if (!datasetEditor.accept()) return;

        InvCatalogImpl cat = (InvCatalogImpl) catTree.getCatalog();
        if (cat == null) return;

        try {
          savePane.setCatalog( catalogPath, cat);
        } catch (IOException e) {
          e.printStackTrace();
        }
        saveWindow.show();

        if (debugSave) {
          try {
            ByteArrayOutputStream os = new ByteArrayOutputStream(60*1000);
            cat.writeXML (os, true);
            IO.writeToFile(os.toString(), "C:/dev/netcdf-java-2.2/test/serverConfig/end.xml");
          } catch (IOException e1) {
            e1.printStackTrace();
          }
        }
      }
    });

    AbstractButton infoButton = BAMutil.makeButtcon("Information", "Extract Info", false);
    infoButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        InvDataset ds = datasetEditor.getExtractedDataset();
        if (ds == null) return;

        ByteArrayOutputStream os = new ByteArrayOutputStream(60*1000);
        PrintStream out = new PrintStream(os);
        extractor.extractTypedDatasetInfo(out, ds);
        extractPane.setText(os.toString());
        extractWindow.show();
      }
    });

    JPanel topButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
    topButtons.add(connectButton);
    topButtons.add(catSource);
    topButtons.add(saveSource);
    topButtons.add(infoButton);

    JPanel topPanel = new JPanel(new BorderLayout());
    topPanel.add(new JLabel("Catalog URL"), BorderLayout.WEST);
    topPanel.add(catalogCB, BorderLayout.CENTER);
    topPanel.add(topButtons, BorderLayout.EAST);

    // the catalog tree
    catTree = new CatalogTreeView();
    catTree.setOpenCatalogReferences( false);
    catTree.addPropertyChangeListener(  new java.beans.PropertyChangeListener() {
      public void propertyChange( java.beans.PropertyChangeEvent e) {
        if (debugEvents) System.out.println("ServerConfigurator tree propertyChange= "+e.getPropertyName());
        if (e.getPropertyName().equals("Catalog")) {
          // persist it
          InvCatalogImpl cat = catTree.getCatalog();
          setCatalogURL(); // munge BaseURL
          catalogCB.addItem( e.getNewValue());
          InvDatasetImpl top = (InvDatasetImpl) cat.getDataset();
          if (top != null)
            catTree.setSelectedDataset( top);

        } /* else if (e.getPropertyName().equals("TreeNode")) {
          InvDatasetImpl ds = (InvDatasetImpl) e.getNewValue();
          System.out.println("TreeNode dataset="+ds);

        } else */
        if (e.getPropertyName().equals("Selection")) {
          InvDatasetImpl ds = (InvDatasetImpl) e.getNewValue();
          if (!datasetEditor.setDataset(ds))  {
            JOptionPane.showMessageDialog(TDServerConfigurator.this, "The current dataset has editing errors - please fix those first");
            catTree.setSelectedDataset( datasetEditor.getDataset());
          }
        }
      }
    });

    PopupMenu csPopup = new PopupMenu(catTree.getJTree(), "Options");
    csPopup.addAction("Move To", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        InvDataset ds = catTree.getSelectedDataset();
        if (ds == null) return;
        if (ds instanceof InvDatasetScan) return;
        if (!(ds instanceof InvCatalogRef)) return;

        InvCatalogRef catref = (InvCatalogRef) ds;
        URI uri = catref.getURI();
        String path = uri.toString();

        int pos = path.indexOf("/thredds/");
        if (pos < 0) return;
        catalogPath =  path.substring(0,pos+9) + "content/" + path.substring(pos+9);

        catalogCB.addItem(catalogPath);
        catTree.setCatalog(catalogPath); // will send "Catalog" property change event if ok
      }
    });
    csPopup.addAction("Expand", new AbstractAction() {
      public void actionPerformed(ActionEvent evt) {
        InvDataset ds = catTree.getSelectedDataset();
        if (ds == null) return;
        if (!(ds instanceof InvDatasetScan)) return;

        expand( (InvDatasetScan) ds);
        catTree.redisplay();
      }
    });
    csPopup.addAction("Show", new AbstractAction() {
      public void actionPerformed(ActionEvent evt) {
        InvDatasetImpl ds = (InvDatasetImpl) catTree.getSelectedDataset();
        if (ds == null) return;

        StringBuilder sbuff = new StringBuilder( 20000);
        InvDatasetImpl.writeHtmlDescription(sbuff, ds, true, false, false, false, true);
        htmlViewer.setContent( ds.getName(), sbuff.toString());
        htmlWindow.show();
      }
    });
    csPopup.addAction("Delete", new AbstractAction() {
       public void actionPerformed(ActionEvent evt) {
         InvDatasetImpl ds = (InvDatasetImpl) catTree.getSelectedDataset();
         if (ds == null) return;

         int confirm = JOptionPane.showConfirmDialog(catTree, "Do you want to delete "+ds.getName(), "Deleting dataset",
                 JOptionPane.YES_NO_OPTION);
         if (confirm != JOptionPane.YES_OPTION) return;

        InvDatasetImpl parent = (InvDatasetImpl) ds.getParentReal();
        if (parent == null) {
          InvCatalogImpl cat = (InvCatalogImpl) ds.getParentCatalog();
          if (cat != null) cat.removeDataset( ds);
        } else {
          parent.removeDataset( ds);
          parent.finish();
        }
        catTree.redisplay();
      }
    });

    datasetEditor = new DatasetEditor();

    //configPane = new TextGetPutPane( (PreferencesExt) prefs.node("configPane"));

    sourcePane = new TextHistoryPane( false);
    sourceWindow = new IndependentWindow( "Catalog Source", BAMutil.getImage( "thredds"), sourcePane);
    sourceWindow.setBounds((Rectangle)prefs.getBean(SOURCE_WINDOW_SIZE, new Rectangle(50, 50, 725, 450)));

    extractPane = new TextHistoryPane( false);
    extractWindow = new IndependentWindow( "Extract from dataset", BAMutil.getImage( "thredds"), extractPane);
    extractWindow.setBounds((Rectangle)prefs.getBean(EXTRACT_WINDOW_SIZE, new Rectangle(50, 150, 725, 450)));

    savePane = new TextGetPutPane( prefs);
    saveWindow = new IndependentWindow( "Save Changes", BAMutil.getImage( "thredds"), savePane);
    saveWindow.setBounds((Rectangle)prefs.getBean(SAVE_WINDOW_SIZE, new Rectangle(50, 150, 725, 450)));

    htmlViewer = new HtmlBrowser();
    htmlWindow = new IndependentWindow( "Dataset", BAMutil.getImage( "thredds"), htmlViewer);
    htmlWindow.setBounds((Rectangle)prefs.getBean(SAVE_HTML_SIZE, new Rectangle(12, 25, 725, 900)));

    JButton rereadButt = new JButton("Server Reinit");
    rereadButt.addActionListener( new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        String catalogPath = (String) catalogCB.getSelectedItem();
        int pos = catalogPath.indexOf("/thredds/");
        String serverURL = catalogPath.substring(0,pos+9);
        try {
          String result = HttpClientManager.getContent( serverURL+"debug?catalogs/reinit");
          savePane.setText(result);
        } catch (Exception e) {
          savePane.setText( e.getMessage());
        }

      }
    });
    savePane.addButton( rereadButt);
    savePane.addPutActionListener( new ActionListener() {
      // reread the catalog
      public void actionPerformed(ActionEvent e) {
        catTree.setCatalog(catalogPath); // reread the catalog
      }
    });

    // layout
    setLayout( new BorderLayout());
    splitH = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, catTree, datasetEditor);
    splitH.setDividerLocation(prefs.getInt(SPLIT_POS, 500));

    //splitH = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, catTree, split2);
    //splitH.setDividerLocation(prefs.getInt("splitPos", 500));

    add( topPanel, BorderLayout.NORTH);
    add( splitH, BorderLayout.CENTER);

    JPanel bottomPanel = new JPanel();
    add( bottomPanel, BorderLayout.SOUTH);
  }

  public void save() {
    catalogCB.save();
    prefs.putInt(SPLIT_POS, splitH.getDividerLocation());
    prefs.putBeanObject(EXTRACT_WINDOW_SIZE, extractWindow.getBounds());
    prefs.putBeanObject(SOURCE_WINDOW_SIZE, sourceWindow.getBounds());
    prefs.putBeanObject(SAVE_WINDOW_SIZE, saveWindow.getBounds());
    prefs.putBeanObject(SAVE_HTML_SIZE, htmlWindow.getBounds());

    savePane.save();
  }

  private void expand (InvDatasetScan dscan) {
    InvCatalogImpl cat = (InvCatalogImpl) dscan.getParentCatalog();
    InvDatasetImpl replace = new InvDatasetImpl( dscan);

    java.util.List datasets = dscan.getDatasets();
    for (int i = 0; i < datasets.size(); i++) {
      InvDatasetImpl d = (InvDatasetImpl) datasets.get(i);
      if (d instanceof InvCatalogRef) {
        String ext = d.getName();
        String id = (dscan.getID() == null) ? ext : dscan.getID() + "/"+ ext;
        InvDatasetScan replaceNested = new InvDatasetScan(replace, ext, dscan.getPath() + "/" + ext,
                dscan.getScanLocation() + ext +"/", id, dscan);
        replace.addDataset(replaceNested);
      } else {
        d.setParent( replace);
        replace.addDataset(d);
      }
    }

    // now replace
    InvDatasetImpl parent = (InvDatasetImpl) dscan.getParentReal();
    if (parent == null) {
      cat.replaceDataset( dscan, replace);
    } else
      parent.replaceDataset( dscan, replace);

    replace.finish();
  }

  private void setCatalogURL() {
    InvCatalogImpl cat = (InvCatalogImpl) catTree.getCatalog();
    URI baseURI = cat.getBaseURI();
    String uri = baseURI.toString();
    String uriNew = StringUtil.remove(uri, "content/");
    try {
      cat.setBaseURI( new URI(uriNew));
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
  }

}

/**
 * public InvDatasetScan( InvDatasetImpl parent, String name, String path, String scanDir, String filter,
                         boolean addDatasetSize, String addLatest, boolean sortOrderIncreasing,
                         String datasetNameMatchPattern, String startTimeSubstitutionPattern, String duration )
 */

  //////////////////////////////////////////////////////////////////////////////

    /* dataset bean table
    dsTable = new BeanTableSorted(DatasetBean.class, (PreferencesExt) prefs.node("dsBeans"), false);
    dsTable.addListSelectionListener( new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        DatasetBean bean = (DatasetBean) dsTable.getSelectedBean();
        InvDatasetImpl selectedDataset = (InvDatasetImpl) bean.dataset();
        catalogChooser.setSelectedDataset( selectedDataset);
      }
    });

    // access bean table
    daTable = new BeanTableSorted(AccessBean.class, (PreferencesExt) prefs.node("dsBeans"), false);

    /// put tables in tabbed pane
    tabbedPane = new JTabbedPane(JTabbedPane.TOP);
    tabbedPane.addTab("Datasets", dsTable);
    tabbedPane.addTab("Access", daTable);

    // layout

    add( splitH, BorderLayout.CENTER);

    JPanel buttPanel = new JPanel();
    add( buttPanel, BorderLayout.SOUTH);

    // Editor
    JButton edit = new JButton("Edit");
    buttPanel.add(edit);
    edit.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        currentBean = (DatasetBean) dsTable.getSelectedBean();
        if (currentBean == null) {
          javax.swing.JOptionPane.showMessageDialog(myParent,
              "You must first connect to a catalog and select a dataset");
          return;
        }
        datasetEditorDialog = makeDatasetEditor(currentBean.dataset());
        datasetEditorDialog.getPrefPanel().addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            currentBean.dataset().finish();
            currentBean.synch();
            dsTable.setSelectedBean(currentBean);

            // save all the table state
            for (Iterator iter = tables.iterator(); iter.hasNext(); ) {
              BeanTable table = (BeanTable) iter.next();
              table.saveState(false);
            }
          }
        });
       datasetEditorDialog.show();
     }
    });

     // window to show DL status
    statusPane = new TextHistoryPane( false);
    statusWindow = new IndependentWindow( "Digital Library Status", BAMutil.getImage( "thredds"), statusPane);
    statusWindow.setBounds((Rectangle)prefs.getBean(STATUS_WINDOW_SIZE, new Rectangle(50, 50, 725, 450)));

    JButton status = new JButton("Status DL");
    buttPanel.add(status);
    status.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        DatasetBean b = (DatasetBean) dsTable.getSelectedBean();
        statusPane.setText( b.status());
        statusWindow.show();
      }
    });

    // add a show source button to catalog chooser
    JButton catSource = new JButton("Save");
    catSource.addActionListener( new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        InvCatalogImpl cat = (InvCatalogImpl) catalogChooser.getCurrentCatalog();
        if (cat == null) return;
        String catURL = catalogChooser.getCurrentURL();
        try {
          configPane.setCatalog(cat.getBaseURI().toString(), cat);
        } catch (IOException ioe) {
          ioe.printStackTrace();
        }

        configPane.gotoTop();
        sourceWindow.show();
      }
    });
    buttPanel.add(catSource);

    JButton openAllButton = new JButton("Open All");
    buttPanel.add(openAllButton);
    openAllButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        tree.openAll( false);
      }
    });

    JButton checkButton = new JButton("Check access URLs");
    buttPanel.add(checkButton);
    checkButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        tabbedPane.setSelectedComponent(daTable);

        // do in background task
        CheckURLsTask task = new CheckURLsTask();
        thredds.ui.ProgressMonitor pm = new thredds.ui.ProgressMonitor(task, 100, 100);
        pm.addActionListener( new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            daTable.revalidate();
            daTable.repaint();
          }
        });
        pm.start( TDServerConfigurator.this, "Checking URLs", task.getTaskLength());
      }
    }); */

  /* private void setCatalog(InvCatalogImpl catalog) {
    this.currentCatalog = catalog;
    dsTable.setBeans( catBean.getDatasetBeans());
  } */

  /* this transforms the catalog to a local one, modifying the catalogRefs
  private void writeCatalog(InvCatalogImpl catalog, String filename, InvCatalogFactory catFactory) {
    collapseCatalogRefs( catalog.getDataset());

    try {
      catFactory.writeXML( catalog, filename);
    } catch (IOException e) {
      JOptionPane.showMessageDialog(myParent, "Catalog failed to write to file= " + filename+
          "\n"+e.getMessage());
    }
    JOptionPane.showMessageDialog( myParent, "Catalog written to file= " + filename );
  }

  private void collapseCatalogRefs(InvDataset ds) {

    ArrayList dlist = (ArrayList) ds.getDatasets();
    for (int i=0; i<dlist.size(); i++) {
      InvDataset dds = (InvDataset) dlist.get(i);

      if (dds instanceof InvCatalogRef) {
        InvCatalogRef catRef = (InvCatalogRef) dds;
        if (!catRef.isRead()) continue;

        // collapse: should change names here
        dlist.remove(dds);
        dlist.add(i, catRef.getProxyDataset());
        System.out.println(" collapsed "+catRef.getName());
      }

      collapseCatalogRefs( dds);
    }
  } */

  /* check URLs
private boolean debugCheckUrl = false;

private class CheckURLsTask extends ProgressMonitorTask {
 int taskLength = 0;
 int count = 0;

 public void run() {
   Iterator iter = getDatasetAccessBeans().iterator();
   while (iter.hasNext()) {
     AccessBean bean = (AccessBean) iter.next();
     String urlOK = bean.getUrlOk();
     if (urlOK.length() > 0) continue; // already been checked
     if (cancel) break;

     InvAccess access = bean.access();
     if (debugCheckUrl) System.out.print("Try to open "+access.getStandardUrlName());
     String status = checkURL( makeURL(access));
     if (debugCheckUrl) System.out.println(" "+status);
     count++;

     bean.setUrlOk( status);
   }
  success = !cancel && !isError();
  done = true;    // do last!
 }

 public String getNote() { return count +" URLs out of "+taskLength; }
 public int getProgress() { return count; }

 public int getTaskLength() {
   taskLength = 0;
   Iterator iter = getDatasetAccessBeans().iterator();
   while (iter.hasNext()) {
     AccessBean bean = (AccessBean) iter.next();
     if (bean.getUrlOk().length() > 0) continue; // already been checked
     taskLength += bean.dataset().getAccess().size();
   }
   return taskLength;
 }
}

private String checkURL( String urlString) {
 URL url = null;
 try {
   url = new URL( urlString);
 } catch ( MalformedURLException e) {
   System.out.println(" BAD url "+urlString+" = "+e.getMessage());
   return "BAD";
 }

 try {
   InputStream is = url.openStream();
   is.close();
 } catch (IOException ioe) {
   System.out.println(" BAD connection "+urlString+" = "+ioe.getMessage());
   return "MISS";
 }
 return "OK";
}

private String makeURL( InvAccess access) {
 String urlString = access.getStandardUrlName();
 if (access.getService().getServiceType() == ServiceType.DODS)
   urlString = urlString + ".dds";
 return urlString;
}

 public void clear() {
   daList = new ArrayList();
   dsList = new ArrayList();
 }

 /** list of CatalogBean.DatasetAccessBean
 public ArrayList getDatasetAccessBeans() { return daList; }

 /** list of CatalogBean.DatasetBean
 public ArrayList getDatasetBeans() { return dsList; }

 private void addDatasets(InvDatasetImpl ds) {
   addDataset( ds);

   // skip unread catalogRef
   if (ds instanceof InvCatalogRef) {
     InvCatalogRef catRef = (InvCatalogRef) ds;
     if (!catRef.isRead()) return;
   }

   if (ds.hasAccess()) {
     Iterator iter = ds.getAccess().iterator();
     while (iter.hasNext())
       daList.add( new AccessBean( (InvAccess) iter.next()));
   }

   // recurse
   java.util.List dlist = ds.getDatasets();
   for (int i=0; i<dlist.size(); i++) {
     InvDatasetImpl dds = (InvDatasetImpl) dlist.get(i);
     addDatasets( dds);
   }
 }

 public void addDataset(InvDatasetImpl ds) {
   DatasetBean bean = new DatasetBean( ds);
   dsTable.addBean( bean);
 }

 public void addDatasetAccess(InvDatasetImpl ds) {
   Iterator iter = ds.getAccess().iterator();
   while (iter.hasNext()) {
     AccessBean beana = new AccessBean( (InvAccess) iter.next());
     daTable.addBean( beana);
   }
 }

 public DatasetBean findDatasetBean( InvDataset ds) {
   Iterator iter = dsList.iterator();
   while (iter.hasNext()) {
     DatasetBean item = (DatasetBean) iter.next();
     if (item.dataset() == ds) return item;
   }
   return null;
 }
 public AccessBean findAccessBean( InvDataset ds) {
   Iterator iter = daList.iterator();
   while (iter.hasNext()) {
     AccessBean item = (AccessBean) iter.next();
     if (item.dataset() == ds) return item;
   }
   return null;
 }


 public AccessBean findOrAddBean( InvAccess access) {
   Iterator iter = daList.iterator();
   while (iter.hasNext()) {
     AccessBean item = (AccessBean) iter.next();
     if (item.access == access) return item;
   }
   AccessBean newBean = new AccessBean( access);
   daList.add( newBean);

   return newBean;
 }

 public class DatasetBean {
   private InvDatasetImpl ds;
   private boolean adn, dif, summary, rights;
   private String difMessages;

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
 }

 public class AccessBean {
   // static public String editableProperties() { return "title include logging freq"; }

   private thredds.catalog.InvAccess access;
   private String name, url, URLok = "";
   private DataType dataType;
   private DataFormatType dataFormatType;
   private ServiceType serviceType;
   private int ngrids, readTime;
   private boolean hasBoundingBox, hasTimeRange, hasStandardQuantities;

   // no-arg constructor
   public AccessBean() {}

   // create from an access
   public AccessBean( InvAccess access) {
     this.access = access;
     InvDataset ds = access.getDataset();
     setName( ds.getName());
     setServiceType( access.getService().getServiceType());
     setUrl( access.getStandardUrlName());

     dataFormatType = access.getDataFormatType();

     // if (debug) System.out.println(" DatasetAccessBean added= "+getUrl());

     /* String n = ds.findProperty("ngrids");
     if (n != null)
       setNgrids( Integer.parseInt(n));

     String t = ds.findProperty("readTime");
     if (t != null)
       setReadTime( Integer.parseInt(t));

     setBB(null != ds.findProperty("BoundingBox"));
     setTR(null != ds.findProperty("DateMin") || null != ds.findProperty("DateMax"));
     setSQ(null != ds.findProperty("StandardQuantities"));
   }

   public InvDataset dataset() { return access.getDataset(); }
   public InvAccess access() { return access; }

   public String getName() { return name; }
   public void setName(String name) { this.name = name; }

   public String getServiceType() { return serviceType.toString(); }
   public void setServiceType(ServiceType serviceType) { this.serviceType = serviceType; }

   public String getFormat() { return (dataFormatType == null) ? "" : dataFormatType.toString(); }

   public String getUrl() { return url; }
   public void setUrl(String url) { this.url = url; }

   /** Get URLok
   public String getUrlOk() { return URLok; }
   /** Set URLok
   public void setUrlOk( String URLok) { this.URLok = URLok; }
 } */