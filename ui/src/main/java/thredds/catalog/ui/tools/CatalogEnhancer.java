// $Id: CatalogEnhancer.java 50 2006-07-12 16:30:06Z caron $
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
import ucar.nc2.constants.FeatureType;
import ucar.nc2.units.DateType;
import thredds.catalog.dl.*;
import thredds.catalog.ui.*;
import thredds.datatype.prefs.*;
import thredds.ui.*;

import ucar.util.prefs.*;
import ucar.util.prefs.ui.*;
//import ucar.util.prefs.ui.*;

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
 * @version $Id: CatalogEnhancer.java 50 2006-07-12 16:30:06Z caron $
 */

public class CatalogEnhancer extends JPanel {
  static private final String SOURCE_WINDOW_SIZE = "SourceWindowSize";
  static private final String STATUS_WINDOW_SIZE = "StatusWindowSize";

  private PreferencesExt prefs;
  private Component myParent;

  // ui
  private CatalogChooser catalogChooser;
  private CatalogTreeView tree;
  private BeanTableSorted dsTable, daTable;
  private JTabbedPane tabbedPane;
  private PrefPanel.Dialog datasetEditorDialog;
  private JSplitPane splitV;
  private ArrayList tables = new ArrayList();
  private IndependentWindow sourceWindow = null, statusWindow = null;
  private TextGetPutPane sourcePane;
  private TextHistoryPane statusPane;

  // data
  private ArrayList daList = new ArrayList();
  private ArrayList dsList = new ArrayList();
  private InvCatalogImpl currentCatalog = null;
  private DatasetBean currentBean = null;

  private DIFWriter difWriter = new DIFWriter();
  private ADNWriter adnWriter = new ADNWriter();

  private boolean debugEvents = false, debugBeans = false;

  public CatalogEnhancer(PreferencesExt prefs, Component parent) {
    this.prefs = prefs;
    this.myParent = parent;

    // create the catalog chooser
    PreferencesExt node = (prefs == null) ? null : (PreferencesExt) prefs.node("catChooser");
    catalogChooser = new CatalogChooser(node, true, false, true);
    catalogChooser.setCatrefEvents( true);
    catalogChooser.addPropertyChangeListener(  new java.beans.PropertyChangeListener() {
      public void propertyChange(java.beans.PropertyChangeEvent e) {
        if (debugEvents)
          System.out.println("CatalogEnhancer chooser propertyChange= " +e.getPropertyName());
        if (e.getPropertyName().equals("Catalog")) {
          InvCatalog cat = catalogChooser.getCurrentCatalog();
          String orgURLs = cat.findProperty("CatalogGenConfigOrigURL");
          if (orgURLs != null) {
            try {
              URI orgURL = cat.resolveUri(orgURLs);
              orgURLs = orgURL.toString();
              if (orgURLs.equals( cat.getUriString())) return;

              int val = JOptionPane.showConfirmDialog(myParent,
                "This catalog was created by the Catalog Generator program\n" +
                 "If you want to change it you should change the catgen config file\n" +
                 "Do you want to edit the catgen config file instead?",
                 "WARNING - generated file; will be overrwritten",
                 JOptionPane.YES_NO_OPTION);
              if (val == JOptionPane.YES_OPTION) {
                catalogChooser.setCatalog( orgURLs);
              }
              
            } catch (java.net.URISyntaxException se) {
              return;
            }
          }
        }
      }
    });

    // catch tree events to synch with dataset table
    tree = catalogChooser.getTreeView();
    tree.setOpenCatalogReferences( false);
    tree.addPropertyChangeListener(  new java.beans.PropertyChangeListener() {
      public void propertyChange( java.beans.PropertyChangeEvent e) {
        if (debugEvents) System.out.println("CatalogEnhancer tree propertyChange= "+e.getPropertyName());
         // see if a new catalog is set
        if (e.getPropertyName().equals("Catalog")) {
          daList = new ArrayList();
          dsList = new ArrayList();
          dsTable.setBeans( dsList);
          daTable.setBeans( daList);

        } else if (e.getPropertyName().equals("TreeNode")) {
          InvDatasetImpl ds = (InvDatasetImpl) e.getNewValue();
          addDataset(ds);
          addDatasetAccess(ds);

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

    // access bean table
    daTable = new BeanTableSorted(AccessBean.class, (PreferencesExt) prefs.node("dsBeans"), false);

    /// put tables in tabbed pane
    tabbedPane = new JTabbedPane(JTabbedPane.TOP);
    tabbedPane.addTab("Datasets", dsTable);
    tabbedPane.addTab("Access", daTable);

    // layout
    splitV = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, catalogChooser, tabbedPane);
    splitV.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout( new BorderLayout());
    add( splitV, BorderLayout.CENTER);

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

            // save all the table state LOOK - make persistent !!
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

     // window to show source
    sourcePane = new TextGetPutPane( (PreferencesExt) prefs.node("getputPane"));
    sourceWindow = new IndependentWindow( "Source", BAMutil.getImage( "thredds"), sourcePane);
    sourceWindow.setBounds((Rectangle)prefs.getBean(SOURCE_WINDOW_SIZE, new Rectangle(50, 50, 725, 450)));

    // add a show source button to catalog chooser
    JButton catSource = new JButton("Save");
    catSource.addActionListener( new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        InvCatalogImpl cat = (InvCatalogImpl) catalogChooser.getCurrentCatalog();
        if (cat == null) return;
        String catURL = catalogChooser.getCurrentURL();
        try {
          sourcePane.setCatalog(cat.getBaseURI().toString(), cat);
        } catch (IOException ioe) {
          ioe.printStackTrace();
        }

        sourcePane.gotoTop();
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
        pm.start( CatalogEnhancer.this, "Checking URLs", task.getTaskLength());
      }
    });
  }

  /* private void setCatalog(InvCatalogImpl catalog) {
    this.currentCatalog = catalog;
    dsTable.setBeans( catBean.getDatasetBeans());
  } */

  // this transforms the catalog to a local one, modifying the catalogRefs
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
  }

  public void save() {
    prefs.putInt("splitPos", splitV.getDividerLocation());
    catalogChooser.save();
    dsTable.saveState(false);
    daTable.saveState(false);
  }

  //////////////////////////////////////////////////////////////////////////////
  // dataset editor
  private PrefPanel.Dialog makeDatasetEditor(InvDatasetImpl ds) {
    PreferencesExt prefNode = (PreferencesExt) prefs.node("datasetEditor");
    PersistentBean persBean = new PersistentBean( ds);

    PrefPanel.Dialog d = new PrefPanel.Dialog( null, true, "Edit Catalog Dataset", prefNode, persBean);
    PrefPanel pp = d.getPrefPanel();
    int row = 0;
    pp.addHeading("Basic", row++);
    pp.addTextField("name", "Name", "", 0, row++, "8,1");
    pp.addTextField("ID", "ID", "", 0, row, null);
    pp.addTextField("localMetadata.authority", "Authority", "", 2, row++, null);

    pp.addEnumComboField("localMetadata.dataFormatType", "Data format", DataFormatType.getAllTypes(),
        true, 0, row, null);

    pp.addEnumComboField("localMetadata.dataType", "Data type", Arrays.asList(FeatureType.values()),
        true, 2, row, null);

    pp.addEnumComboField("collectionType", "Collection type", CollectionType.getAllTypes(),
        true, 4, row++, null);

    pp.addHeading("GeoSpatial / Temporal Coverage", row++);
    pp.addCheckBoxField("localMetadata.geospatialCoverage.global", "Global", false, 0, row++);
    // pp.addComponent(new JButton("Read Dataset"), 1, row++, "left, center");

    /* JPanel geoPanel = new JPanel();
    Field.CheckBox global = new Field.CheckBox( "localMetadata.geospatialCoverage.global", "Global", false, persBean);
    geoPanel.add( new JLabel("Global: "));
    geoPanel.add( global.getEditComponent());
    geoPanel.add( new JButton("Read Dataset"));
    pp.addComponent(geoPanel, 0, row++, "left, center"); */


    pp.addDoubleField("localMetadata.geospatialCoverage.latStart", "Starting Latitude", 0.0, 0, row, null);
    pp.addDoubleField("localMetadata.geospatialCoverage.latExtent", "Size", 0.0, 2, row, null);
    pp.addDoubleField("localMetadata.geospatialCoverage.latResolution", "Resolution", 0.0, 4, row, null);
    pp.addTextField("localMetadata.geospatialCoverage.latUnits", "Units", "", 6, row, null);
    pp.addDoubleField("localMetadata.geospatialCoverage.lonStart", "Starting Longitude", 0.0, 0, row+1, null);
    pp.addDoubleField("localMetadata.geospatialCoverage.lonExtent", "Size", 0.0, 2, row+1, null);
    pp.addDoubleField("localMetadata.geospatialCoverage.lonResolution", "Resolution", 0.0, 4, row+1, null);
    pp.addTextField("localMetadata.geospatialCoverage.lonUnits", "Units", "", 6, row+1, null);
    pp.addDoubleField("localMetadata.geospatialCoverage.heightStart", "Starting Height", 0.0, 0, row+2, null);
    pp.addDoubleField("localMetadata.geospatialCoverage.heightExtent", "Size", 0.0, 2, row+2, null);
    pp.addDoubleField("localMetadata.geospatialCoverage.heightResolution", "Resolution", 0.0, 4, row+2, null);
    pp.addTextField("localMetadata.geospatialCoverage.heightUnits", "Units", "", 6, row+2, null);
    pp.addTextField("localMetadata.geospatialCoverage.ZPositive", "Z is Positive", "up", 6, row+3, null);
    row += 4;

    //pp.addSpace(row++, null);
    pp.addField( new DateField("localMetadata.timeCoverage.start", "Start Date", null, persBean), 0, row, null);
    pp.addField( new DateField("localMetadata.timeCoverage.end", "End Date", null, persBean), 2, row, null);
    pp.addField( new DurationField("localMetadata.timeCoverage.duration", "Duration", null, persBean), 4, row, null);
    pp.addField( new DurationField("localMetadata.timeCoverage.resolution", "Resolution", null, persBean), 6, row++, null);
    //pp.addDateField("localMetadata.timeCoverage.end.date", "End Date", null, 2, row++, null);

    pp.addHeading("Digital Library Info", row++);
    pp.addCheckBoxField("harvest", "Harvest", false, 0, row++);

    pp.addTextAreaField("localMetadata.summary", "Summary", null, 7, 0, row, "3,1");
    pp.addTextAreaField("localMetadata.rights", "Rights", null, 7, 4, row++, "3,1");

    pp.addTextAreaField("localMetadata.history", "History", null, 7, 0, row, "3,1");
    pp.addTextAreaField("localMetadata.processing", "Process", null, 7, 4, row++, "3,1");

    pp.addEmptyRow(row++, 10);

    JTabbedPane tabPane = new JTabbedPane();
    pp.addComponent( tabPane, 0, row++, "8,1");

    tables = new ArrayList(); // LOOK

    Field.BeanTable creators = new Field.BeanTable("localMetadata.creators", "Creators", null, ThreddsMetadata.Source.class,
        (PreferencesExt) prefs.node("creators"), persBean);
    tabPane.addTab( "Creators", creators.getEditComponent());
    tables.add( creators.getEditComponent());

    Field.BeanTable publishers = new Field.BeanTable("localMetadata.publishers", "Publishers", null, ThreddsMetadata.Source.class,
        (PreferencesExt) prefs.node("publishers"), persBean);
    tabPane.addTab( "Publishers", publishers.getEditComponent());
    tables.add( publishers.getEditComponent());

    Field.BeanTable projects = new Field.BeanTable("localMetadata.projects", "Projects", null, ThreddsMetadata.Vocab.class,
        (PreferencesExt) prefs.node("projects") , persBean);
    tabPane.addTab( "Projects", projects.getEditComponent());
    tables.add( projects.getEditComponent());

    Field.BeanTable keywords = new Field.BeanTable("localMetadata.keywords", "Keywords", null, ThreddsMetadata.Vocab.class,
        (PreferencesExt) prefs.node("keywords"), persBean);
    tabPane.addTab( "Keywords", keywords.getEditComponent());
    tables.add( keywords.getEditComponent());

    Field.BeanTable dates = new Field.BeanTable("localMetadata.dates", "Dates", null, DateType.class,
        (PreferencesExt) prefs.node("dates"), persBean);
    tabPane.addTab( "Dates", dates.getEditComponent());
    tables.add( dates.getEditComponent());

    Field.BeanTable contributors = new Field.BeanTable("localMetadata.contributors", "Contributors", null, ThreddsMetadata.Contributor.class,
        (PreferencesExt) prefs.node("contributors"), persBean);
    tabPane.addTab( "Contributors", contributors.getEditComponent());
    tables.add( contributors.getEditComponent());

    /* Field.BeanTable properties = new Field.BeanTable("properties", "Properties", null, InvProperty.class,
        prefNode, persBean);
    tabPane.addTab( "Properties", properties.getEditComponent());

    /* Field.BeanTable documentation = new Field.BeanTable("documentation", "Documentation", null, InvDocumentation.class,
        prefNode, persBean);
    tabPane.addTab( "Documentation", documentation.getEditComponent()); */

    //pp.addTextAreaField("rights", "Rights", null, 4, 2, row, "2,4");
    //row += 4;

    //pp.addTextField("end", "end", "", 0, row++, null);


    /*

    InvDocumentation abstractDoc = null;
    InvDocumentation rights = null;
    java.util.List docs = ds.getDocumentation();
    for (int i=0; i<docs.size(); i++) {
      InvDocumentation doc = (InvDocumentation) docs.get(i);
      String type = doc.getType();
      if ((type != null) && type.equalsIgnoreCase("abstract")) abstractDoc = doc;
      if ((type != null) && type.equalsIgnoreCase("rights")) rights = doc;
    }

    pp.addTextAreaField("abstract", "abstract", (abstractDoc == null) ? "" : abstractDoc.getInlineContent(), 4);
    pp.addTextAreaField("rights", "rights", (rights == null) ? "" : rights.getInlineContent(), 4);

    pp.newColumn();

    java.util.List list = ds.getKeywords();
    for (int i=0; i<list.size(); i++) {
      ThreddsMetadata.Vocab t = (ThreddsMetadata.Vocab) list.get(i);
      pp.addTextField("keyword"+i, "keyword", t.getText());
    } */

    d.finish();
    return d;
  }

  //////////////////////////////////////////////////////////////////////////////
  // check URLs
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

    /** list of CatalogBean.DatasetAccessBean */
    public ArrayList getDatasetAccessBeans() { return daList; }

    /** list of CatalogBean.DatasetBean */
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
      private FeatureType dataType;
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
        setSQ(null != ds.findProperty("StandardQuantities")); */
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

      /** Get URLok */
      public String getUrlOk() { return URLok; }
      /** Set URLok */
      public void setUrlOk( String URLok) { this.URLok = URLok; }
    }

}


/* Change History:
   $Log: CatalogEnhancer.java,v $
   Revision 1.6  2006/01/20 20:49:05  caron
   disambiguate DataType

   Revision 1.5  2005/06/23 19:18:50  caron
   no message

   Revision 1.4  2005/04/29 14:55:56  edavis
   Fixes for change in InvCatalogFactory.writeXML( cat, filename) method
   signature. And start on allowing wildcard characters in pathname given
   to DirectoryScanner.

   Revision 1.3  2005/04/28 23:15:11  caron
   catChooser writes catalog to directory

   Revision 1.2  2004/11/16 23:35:37  caron
   no message

   Revision 1.1  2004/11/04 20:16:42  caron
   no message

   Revision 1.5  2004/09/30 00:33:36  caron
   *** empty log message ***

   Revision 1.4  2004/09/24 03:26:30  caron
   merge nj22

   Revision 1.3  2004/06/12 02:01:11  caron
   dqc 0.3

   Revision 1.1  2004/05/11 23:30:32  caron
   release 2.0a

   Revision 1.5  2004/03/05 23:35:48  caron
   rel 1.3.1 javadoc

   Revision 1.3  2004/02/20 00:49:53  caron
   1.3 changes

 */