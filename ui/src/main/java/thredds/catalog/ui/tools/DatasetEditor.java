// $Id: DatasetEditor.java 50 2006-07-12 16:30:06Z caron $
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

import ucar.util.prefs.ui.*;
import ucar.nc2.thredds.MetadataExtractor;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import thredds.catalog.*;
import thredds.datatype.prefs.DateField;
import thredds.datatype.prefs.DurationField;
import thredds.ui.RangeDateSelector;
import thredds.ui.IndependentWindow;
import thredds.ui.BAMutil;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.io.IOException;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * GUI Editor for  Thredds catalogs.
 * @author john
 */
public class DatasetEditor extends JPanel {
  private static final String NAME = "name";
  private static final String ID = "ID";
  private static final String AUTHORITY = "authority";
  private static final String SERVICE_NAME = "serviceName";

  private static final String FORMAT_TYPE = "dataFormatType";
  private static final String DATA_TYPE = "dataType";
  private static final String COLLECTION_TYPE = "collectionType";

  private static final String GC_TYPE = "gcType";
  private static final String TC_TYPE = "tcType";

  private static final String LAT_START = "latStart";
  private static final String LAT_EXTENT = "latExtent";
  private static final String LAT_RESOLUTION = "latResolution";
  private static final String LAT_UNITS = "latUnits";

  private static final String LON_START = "lonStart";
  private static final String LON_EXTENT = "lonExtent";
  private static final String LON_RESOLUTION = "lonResolution";
  private static final String LON_UNITS = "lonUnits";

  private static final String HEIGHT_START = "heightStart";
  private static final String HEIGHT_EXTENT = "heightExtent";
  private static final String HEIGHT_RESOLUTION = "heightResolution";
  private static final String HEIGHT_UNITS = "heightUnits";
  private static final String ZPOSITIVE_UP = "ZPositiveUp";

  private static final String HARVEST = "harvest";
  private static final String SUMMARY = "summary";
  private static final String RIGHTS = "rights";
  private static final String HISTORY = "history";
  private static final String PROCESSING = "processing";

  private static final String VARIABLES = "variables";
  private static final String CREATORS = "creators";
  private static final String PUBLISHERS = "publishers";
  private static final String PROJECTS = "projects";
  private static final String KEYWORDS = "keywords";
  private static final String DATES = "dates";
  private static final String CONTRIBUTORS = "contributors";
  private static final String DOCUMENTATION = "documentationLinks";

  private static final ArrayList inherit_types = new ArrayList();
  private static final String LOCAL = "Local";
  private static final String INHERITABLE = "Local, Inheritable";
  private static final String INHERITED = "Inherited";

  static {
    inherit_types.add(LOCAL);
    inherit_types.add(INHERITABLE);
    inherit_types.add(INHERITED);
  }

  //////////////////////////////////////////////////////

  private Field gc_type, tc_type;
  private Field.BeanTable variablesFld, creatorsFld, publishersFld, projectsFld, keywordsFld, datesFld, contributorsFld, docsFld;
  private ArrayList tables = new ArrayList();

  private RangeDateSelector dateRangeSelector;
  private JButton extractGCButton, extractVButton;

  private PrefPanel metadataPP, dscanPP;
  private IndependentWindow dscanWindow;
  private JButton exampleButton;

  private InvDatasetImpl dataset, leafDataset;

  public DatasetEditor()  {
    JTabbedPane tabs = new  JTabbedPane();

    metadataPP = new PrefPanel("Edit Catalog Dataset", null, null);
    tabs.add("metadata", metadataPP);

    int row = 0;
    metadataPP.addHeading("Basic", row++);

    // row 0
    metadataPP.addTextField(NAME, "Name", "", 0, row++, "8,1");
    // row 1
    metadataPP.addTextField(ID, "ID", "", 0, row, null);
    addPopups( metadataPP.addTextField(AUTHORITY, "Authority", "", 2, row, null));
    addPopups( metadataPP.addTextField(SERVICE_NAME, "Service", "", 4, row, null));

    row++;
     // row 2
    addPopups( metadataPP.addEnumComboField(FORMAT_TYPE, "Data format", DataFormatType.getAllTypes(), true, 0, row, null));
    addPopups( metadataPP.addEnumComboField(DATA_TYPE, "Data type", Arrays.asList(FeatureType.values()), true, 2, row, null));
    metadataPP.addEnumComboField(COLLECTION_TYPE, "Collection type", CollectionType.getAllTypes(), true, 4, row++, null);

////////////
    metadataPP.addHeading("GeoSpatial Coverage", row++);

    //addCheckBoxField("localMetadata.geospatialCoverage.global", "Global", false, 0, row);
    gc_type = metadataPP.addEnumComboField(GC_TYPE, "type", inherit_types, false, 0, row, null);
    gc_type.addPropertyChangeListener( new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        String mode = (String) gc_type.getValue();
        setGCmode( getMode(mode));
      }
    });

    extractGCButton = makeButton("Extract Geospatial");
    extractGCButton.addActionListener( new ActionListener() {
      public void actionPerformed(ActionEvent e) { extractGeospatial(); }
    });
    metadataPP.addComponent(extractGCButton, 2, row, "left, center");

    metadataPP.addCheckBoxField(ZPOSITIVE_UP, "Z positive up", false, 4, row);
    row++;

    /* JPanel geoPanel = new JPanel();
Field.CheckBox global = new Field.CheckBox( "localMetadata.geospatialCoverage.global", "Global", false, persBean);
geoPanel.add( new JLabel("Global: "));
geoPanel.add( global.getEditComponent());
geoPanel.add( new JButton("Read Dataset"));
pp.addComponent(geoPanel, 0, row++, "left, center"); */

    // 4 columns in 3 rows
    metadataPP.addDoubleField(LAT_START, "Starting Latitude", 0.0, 5, 0, row, null);
    metadataPP.addDoubleField(LAT_EXTENT, "Size", 0.0, 5, 2, row, null);
    metadataPP.addDoubleField(LAT_RESOLUTION, "Resolution", 0.0, 5, 4, row, null);
    metadataPP.addTextField(LAT_UNITS, "Units", "", 6, row, null);
    metadataPP.addDoubleField(LON_START, "Starting Longitude", 0.0, 5, 0, row+1, null);
    metadataPP.addDoubleField(LON_EXTENT, "Size", 0.0, 5, 2, row+1, null);
    metadataPP.addDoubleField(LON_RESOLUTION, "Resolution", 0.0, 5, 4, row+1, null);
    metadataPP.addTextField(LON_UNITS, "Units", "", 6, row+1, null);
    metadataPP.addDoubleField(HEIGHT_START, "Starting Height", 0.0, 5, 0, row+2, null);
    metadataPP.addDoubleField(HEIGHT_EXTENT, "Size", 0.0, 5, 2, row+2, null);
    metadataPP.addDoubleField(HEIGHT_RESOLUTION, "Resolution", 0.0, 5, 4, row+2, null);
    metadataPP.addTextField(HEIGHT_UNITS, "Units", "", 6, row+2, null);
    //addTextField("localMetadata.geospatialCoverage.ZPositive", "Z is Positive", "up", 6, row+3, null);
    row += 3;

  //////
    metadataPP.addHeading("Temporal Coverage", row++);
    tc_type = metadataPP.addEnumComboField(TC_TYPE, "type", inherit_types, false, 0, row++, null);
    tc_type.addPropertyChangeListener( new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        String mode = (String) tc_type.getValue();
        setTCmode( getMode(mode));
      }
    });

    DateRange range = null;
    try {
      range = new DateRange();
    } catch (Exception e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
    dateRangeSelector = new RangeDateSelector("", range,  false, false, null, false, false);
    DateField minDateField = dateRangeSelector.getMinDateField();
    DateField maxDateField = dateRangeSelector.getMaxDateField();
    DurationField durationField = dateRangeSelector.getDurationField();
    DurationField resolutionField = dateRangeSelector.getResolutionField();

    metadataPP.addField( minDateField, 0, row, null);
    metadataPP.addField( maxDateField, 2, row, null);
    metadataPP.addField( durationField, 4, row, null);
    metadataPP.addField( resolutionField, 6, row++, null);

  ////
    metadataPP.addHeading("Digital Library Info", row++);
    metadataPP.addCheckBoxField(HARVEST, "Harvest", false, 0, row++);

    addPopups( metadataPP.addTextAreaField(SUMMARY, "Summary", null, 7, 0, row, "3,1"));
    addPopups( metadataPP.addTextAreaField(RIGHTS, "Rights", null,   2, 0, row+1, "3,1"));

    addPopups( metadataPP.addTextAreaField(HISTORY, "History", null,    7, 4, row, "3,1"));
    addPopups( metadataPP.addTextAreaField(PROCESSING, "Process", null, 2, 4, row+1, "3,1"));
    row += 2;

    metadataPP.addEmptyRow(row++, 10);

    JTabbedPane tabPane = new JTabbedPane();
    metadataPP.addComponent( tabPane, 0, row++, "8,1");

    tables = new ArrayList();

    tabPane.addTab( "Variables", makeVariablesPanel());
    tables.add( variablesFld);

    creatorsFld = new Field.BeanTable(CREATORS, "Creators", null, ThreddsMetadata.Source.class,
        null, null);
    tabPane.addTab( "Creators", creatorsFld.getEditComponent());
    tables.add( creatorsFld);

    publishersFld = new Field.BeanTable(PUBLISHERS, "Publishers", null, ThreddsMetadata.Source.class,
        null, null);
    tabPane.addTab( "Publishers", publishersFld.getEditComponent());
    tables.add( publishersFld);

    projectsFld = new Field.BeanTable(PROJECTS, "Projects", null, ThreddsMetadata.Vocab.class,
        null, null);
    tabPane.addTab( "Projects", projectsFld.getEditComponent());
    tables.add( projectsFld);

    keywordsFld = new Field.BeanTable(KEYWORDS, "Keywords", null, ThreddsMetadata.Vocab.class,
        null, null);
    tabPane.addTab( "Keywords", keywordsFld.getEditComponent());
    tables.add( keywordsFld);

    datesFld = new Field.BeanTable(DATES, "Dates", null, DateType.class,
        null, null);
    tabPane.addTab( "Dates", datesFld.getEditComponent());
    tables.add( datesFld);

    contributorsFld = new Field.BeanTable(CONTRIBUTORS, "Contributors", null, ThreddsMetadata.Contributor.class,
        null, null);
    tabPane.addTab( "Contributors", contributorsFld.getEditComponent());
    tables.add( contributorsFld);

    docsFld = new Field.BeanTable(DOCUMENTATION, "Documentation", null, InvDocumentation.class,
        null, null);
    tabPane.addTab( "Documentation", docsFld.getEditComponent());
    tables.add( docsFld);

    for (int i = 0; i < tables.size(); i++)
      addPopups( (Field.BeanTable) tables.get(i));

    metadataPP.finish(false);

    makeDscanPanel();
    tabs.add("datasetScan", dscanPP);

    setLayout(new BorderLayout());
    add( tabs, BorderLayout.CENTER);
  }

  private static final String DSCAN_PATH = "path";
  private static final String DSCAN_DIR = "scanDir";
  // private static final String DSCAN_FILTER = "filter";
  private static final String DSCAN_ADDSIZE = "addDatasetSize";
  // private static final String DSCAN_ADDLATEST = "addLatest";
  //private static final String DSCAN_TC_MATCH = "datasetNameMatchPattern";
  //private static final String DSCAN_TC_SUBS = "startTimeSubstitutionPattern";
  //private static final String DSCAN_TC_DURATOPN = "duration";

  private void makeDscanPanel() {
    dscanPP = new PrefPanel("dscan", null);
    int row = 0;
    dscanPP.addCheckBoxField(DSCAN_ADDSIZE, "Add File Size", false, 0, row++);
    //dscanPP.addCheckBoxField(DSCAN_ADDLATEST, "Add Latest", false, 0, row++);
    dscanPP.addTextField(DSCAN_PATH, "Path", "", 0, row++, null);
    dscanPP.addTextField(DSCAN_DIR, "Directory Location", "", 0, row++, null);
    //dscanPP.addTextField(DSCAN_FILTER, "Filter", "", 0, row++, null);

    dscanPP.addEmptyRow(row++, 2);
    dscanPP.addHeading("Time Coverage", row++);

    exampleButton = makeButton("Example filename");
    exampleButton.addActionListener( new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        InvDataset leaf = findLeafDataset( dataset);
        exampleButton.setText( (leaf == null) ? "ERR" : leaf.getName());
      }
    });
    dscanPP.addComponent(exampleButton, 0, row++, null);

    //dscanPP.addTextField(DSCAN_TC_MATCH, "Dataset Name Match", "", 0, row++, null);
    //dscanPP.addTextField(DSCAN_TC_SUBS, "Substituton Pattern", "$1-$2-$3T$4:00:00", 0, row++, null);
    //dscanPP.addTextField(DSCAN_TC_DURATOPN, "Duration", "", 0, row++, null);

    dscanPP.finish(false);

    dscanWindow = new IndependentWindow( "DatasetScan options", BAMutil.getImage( "thredds"), dscanPP);
    dscanWindow.setBounds(new Rectangle(150, 50, 700, 300));
  }

  private JPanel makeVariablesPanel() {
    variablesFld = new Field.BeanTable(VARIABLES, "Variables", null, ThreddsMetadata.Variable.class, null, null);

    extractVButton = makeButton("Extract Variables");
    extractVButton.addActionListener( new ActionListener() {
      public void actionPerformed(ActionEvent e) { extractVariables(); }
    });

    JPanel buttPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
    buttPanel.add(extractVButton);

    JPanel vPanel = new JPanel( new BorderLayout());
    vPanel.add(buttPanel, BorderLayout.NORTH);
    vPanel.add(variablesFld.getEditComponent(), BorderLayout.CENTER);
    return vPanel;
  }


  private Color actionColor = new Color(142, 0, 100);
  private JButton makeButton(String name) {
    JButton b = new JButton(name);
    b.setForeground( actionColor);
    return b;
  }

  private void addPopups( Field fld) {
    fld.addPopupMenuAction( "local", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        setMode( (Field) e.getSource(), 0);
      }
    });
    fld.addPopupMenuAction( "inheritable", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        setMode( (Field) e.getSource(), 1);
      }
    });
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private Color normalColor = Color.black;
  private Color inheritedColor = Color.blue;
  private Color inheritableColor = Color.red;

  public InvDatasetImpl getDataset() { return dataset; }
  public boolean setDataset( InvDatasetImpl ds) {
    if (!accept())
      return false;

    this.dataset = ds;
    this.leafDataset = null;
    exampleButton.setText("Example Dataset");

    //////////
    PersistentBean persBean = new PersistentBean( ds);

    setEditValue( NAME, persBean, 0);
    setEditValue( ID, persBean, 0);

    setEditValueWithInheritence(AUTHORITY, persBean);
    setEditValueWithInheritence(SERVICE_NAME, persBean);

    setEditValueWithInheritence(FORMAT_TYPE, persBean);
    setEditValueWithInheritence(DATA_TYPE, persBean);

    setEditValue( COLLECTION_TYPE, persBean, 0);
    setEditValue( HARVEST, persBean, 0);

    // gotta find which GeospatialCoverage to use.
    int mode = 0;
    ThreddsMetadata.GeospatialCoverage gc = ds.getLocalMetadata().getGeospatialCoverage();
    if ((gc == null) || gc.isEmpty()) {
      gc = ds.getLocalMetadataInheritable().getGeospatialCoverage();
      mode = 1;
    }
    if ((gc == null) || gc.isEmpty()) {
      gc = ds.getGeospatialCoverage();
      mode = 2; // inherited
    }
    metadataPP.setFieldValue( GC_TYPE, inherit_types.get(mode));
    setGC( gc, mode);

    // gotta find which TimeCoverage to use.
    mode = 0;
    DateRange tc = ds.getLocalMetadata().getTimeCoverage();
    if (tc == null) {
      tc = ds.getLocalMetadataInheritable().getTimeCoverage();
      mode = 1;
    }
    if (tc == null) {
      tc = ds.getTimeCoverage();
      mode = 2; // inherited
    }
    metadataPP.setFieldValue( TC_TYPE, inherit_types.get(mode));
    if (tc != null) dateRangeSelector.setDateRange( tc);
    setTCmode( mode);

    setEditValueWithInheritence( SUMMARY, persBean);
    setEditValueWithInheritence( RIGHTS, persBean);
    setEditValueWithInheritence( HISTORY, persBean);
    setEditValueWithInheritence( PROCESSING, persBean);

    setVariables( variablesFld);
    setBeanList( creatorsFld, CREATORS, persBean);
    setBeanList( publishersFld, PUBLISHERS, persBean);
    setBeanList( projectsFld, PROJECTS, persBean);
    setBeanList( keywordsFld, KEYWORDS, persBean);
    setBeanList( datesFld, DATES, persBean);
    setBeanList( contributorsFld, CONTRIBUTORS, persBean);
    setBeanList( docsFld, DOCUMENTATION, persBean);

    if (ds instanceof InvDatasetScan) {
      dscanPP.setEnabled(true);
      setEditValue( dscanPP, DSCAN_PATH, persBean, 0);
      setEditValue( dscanPP, DSCAN_DIR, persBean, 0);
      //setEditValue( dscanPP, DSCAN_FILTER, persBean, 0);
      setEditValue( dscanPP, DSCAN_ADDSIZE, persBean, 0);
      //setEditValue( dscanPP, DSCAN_ADDLATEST, persBean, 0);
      //setEditValue( dscanPP, DSCAN_TC_MATCH, persBean, 0);
      //setEditValue( dscanPP, DSCAN_TC_SUBS, persBean, 0);
      //setEditValue( dscanPP, DSCAN_TC_DURATOPN, persBean, 0);
    } else
      dscanPP.setEnabled(false);

    return true;
  }

  private void setGC(ThreddsMetadata.GeospatialCoverage gc, int mode) {
    PersistentBean gcBean = (gc == null) ? null : new PersistentBean( gc);

    setEditValue(LAT_START, gcBean, mode);
    setEditValue(LAT_EXTENT, gcBean, mode);
    setEditValue(LAT_RESOLUTION, gcBean, mode);
    setEditValue(LAT_UNITS, gcBean, mode);

    setEditValue(LON_START, gcBean, mode);
    setEditValue(LON_EXTENT, gcBean, mode);
    setEditValue(LON_RESOLUTION, gcBean, mode);
    setEditValue(LON_UNITS, gcBean, mode);

    setEditValue(HEIGHT_START, gcBean, mode);
    setEditValue(HEIGHT_EXTENT, gcBean, mode);
    setEditValue(HEIGHT_RESOLUTION, gcBean, mode);
    setEditValue(HEIGHT_UNITS, gcBean, mode);

    setEditValue(ZPOSITIVE_UP, gcBean, mode);

    extractGCButton.setEnabled(mode != 2);
  }

  private void setGCmode(int mode) {
    setMode( metadataPP.getField(LAT_START),  mode);
    setMode( metadataPP.getField(LAT_EXTENT),  mode);
    setMode( metadataPP.getField(LAT_RESOLUTION),  mode);
    setMode( metadataPP.getField(LAT_UNITS),  mode);
    setMode( metadataPP.getField(LON_START),  mode);
    setMode( metadataPP.getField(LON_EXTENT),  mode);
    setMode( metadataPP.getField(LON_RESOLUTION),  mode);
    setMode( metadataPP.getField(LON_UNITS),  mode);
    setMode( metadataPP.getField(HEIGHT_START),  mode);
    setMode( metadataPP.getField(HEIGHT_EXTENT),  mode);
    setMode( metadataPP.getField(HEIGHT_RESOLUTION),  mode);
    setMode( metadataPP.getField(HEIGHT_UNITS),  mode);
    setMode( metadataPP.getField(ZPOSITIVE_UP),  mode);

    extractGCButton.setEnabled(mode != 2);
  }

  private void setTCmode(int mode) {
    setMode( metadataPP.getField(RangeDateSelector.TIME_START),  mode);
    setMode( metadataPP.getField(RangeDateSelector.TIME_END),  mode);
    setMode( metadataPP.getField(RangeDateSelector.TIME_DURATION),  mode);
    setMode( metadataPP.getField(RangeDateSelector.TIME_RESOLUTION),  mode);
  }

  private void setVariables(Field.BeanTable beanTable) {
    List variableLists = dataset.getLocalMetadata().getVariables();
    if ((variableLists != null) && (variableLists.size() > 0)) {
     ThreddsMetadata.Variables vars = (ThreddsMetadata.Variables) variableLists.get(0);
     beanTable.setValue( vars.getVariableList());
     setMode(beanTable, 0);
     return;
    }

    variableLists = dataset.getLocalMetadataInheritable().getVariables();
    if ((variableLists != null) && (variableLists.size() > 0)) {
     ThreddsMetadata.Variables vars = (ThreddsMetadata.Variables) variableLists.get(0);
     beanTable.setValue( vars.getVariableList());
     setMode(beanTable, 1);
     return;
    }

    variableLists = dataset.getVariables();
    if ((variableLists != null) && (variableLists.size() > 0)) {
      ThreddsMetadata.Variables vars = (ThreddsMetadata.Variables) variableLists.get(0);
      beanTable.setValue( vars.getVariableList());
      setMode(beanTable, (vars == null || vars.getVariableList().size() == 0) ? 1 : 2);
      return;
    }

    // clear out the table
    beanTable.setValue( new ArrayList());
  }

  private void setEditValue(String name, PersistentBean bean, int mode) {
    setEditValue( metadataPP, name, bean, mode);
  }

  private void setEditValue(PrefPanel pp, String name, PersistentBean bean, int mode) {
    Field fld = pp.getField(name);
    if (bean == null)
      fld.setValue( null);
    else {
      Object value = bean.getObject( name);
      fld.setValue( value);
    }
    setMode(fld, mode);
  }

  private void setEditValueWithInheritence(String name, PersistentBean bean) {
    Field fld = metadataPP.getField(name);
    if (bean == null) {
      fld.setValue( null);
      return;
    }

    Object value = bean.getObject( "localMetadata."+name); // local, non inheritable
    if (value != null) {
      fld.setValue( value);
      setMode(fld, 0);
    } else {
      value = bean.getObject( "localMetadataInheritable."+name); // local, inheritable
      if (value != null) {
        fld.setValue( value);
        setMode( fld, 1);

      } else {
        value = bean.getObject( name); // inherited
        fld.setValue( value);
        setMode( fld, (value == null) ? 1 : 2);
      }
    }
  }

  private void setBeanList(Field.BeanTable beanTable, String name, PersistentBean bean) {
    List value = (List) bean.getObject( "localMetadata."+name); // local, non inheritable
    if ((value != null) && (value.size() > 0)) {
      beanTable.setValue( value);
      setMode(beanTable, 0);
    } else {
      value = (List) bean.getObject( "localMetadataInheritable."+name); // local, inheritable
      if ((value != null) && (value.size() > 0)) {
        beanTable.setValue( value);
        setMode(beanTable, 1);
      } else {
        value = (List) bean.getObject( name); // inherited
        beanTable.setValue( value);
        setMode(beanTable, (value == null || value.size() == 0) ? 1 : 2);
      }
    }
  }

  private int getMode(String want_mode) {
    for (int i = 0; i < inherit_types.size(); i++) {
      String mode = (String) inherit_types.get(i);
      if (mode.equals(want_mode)) return i;
    }
    return -1;
  }

  private void setMode(Field fld, int mode) {
    if (mode == 1) setColor( fld, inheritableColor);
    else if (mode == 2) setColor( fld, inheritedColor);
    else setColor( fld, normalColor);

    fld.setEnabled( mode != 2);
  }

  private void setColor(Field fld, Color color) {
    fld.getDeepEditComponent().setForeground(color);
  }

  private boolean isInheritable( Field fld) {
    return fld.getDeepEditComponent().getForeground().equals(inheritableColor);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////

  public boolean accept() {
    if (dataset == null) return true;
    boolean ok = metadataPP.accept();
    boolean ok2 = dscanPP.accept();
    if (ok && ok2) store2Dataset();
    return (ok && ok2);
  }

  public void store2Dataset() {
    PersistentBean persBean = new PersistentBean( dataset); // reset the BeanMaps

    setStoreValue( NAME, persBean, false);
    setStoreValue( ID, persBean, false);

    setStoreValue(AUTHORITY, persBean, true);
    setStoreValue(SERVICE_NAME, persBean, true);

    setStoreValue(FORMAT_TYPE, persBean, true);
    setStoreValue(DATA_TYPE, persBean, true);

    setStoreValue(COLLECTION_TYPE, persBean, false);
    setStoreValue(HARVEST, persBean, false);

    String gcType = (String) metadataPP.getFieldValue( GC_TYPE);
    if (!gcType.equals(INHERITED)) {
      ThreddsMetadata.GeospatialCoverage gc = new ThreddsMetadata.GeospatialCoverage();
      storeGC( new PersistentBean(gc));
      if (gcType.equals(LOCAL)) {
        dataset.getLocalMetadata().setGeospatialCoverage( gc);
        dataset.getLocalMetadataInheritable().setGeospatialCoverage( null);

      } else {
        dataset.getLocalMetadata().setGeospatialCoverage( null);
        dataset.getLocalMetadataInheritable().setGeospatialCoverage( gc);
      }
    }

    String tcType = (String) metadataPP.getFieldValue( TC_TYPE);
    if (!tcType.equals(INHERITED)) {
      DateRange dateRange = dateRangeSelector.getDateRange();
      if (tcType.equals(LOCAL)) {
        dataset.getLocalMetadata().setTimeCoverage( dateRange);
        dataset.getLocalMetadataInheritable().setTimeCoverage( null);

      } else {
        dataset.getLocalMetadata().setTimeCoverage( null);
        dataset.getLocalMetadataInheritable().setTimeCoverage( dateRange);
      }
    }

    setStoreValue( SUMMARY, persBean, true);
    setStoreValue( RIGHTS, persBean, true);
    setStoreValue( HISTORY, persBean, true);
    setStoreValue( PROCESSING, persBean, true);

    //storeBeanList( variablesFld, VARIABLES, persBean);
    storeBeanList( creatorsFld, CREATORS, persBean);
    storeBeanList( publishersFld, PUBLISHERS, persBean);
    storeBeanList( projectsFld, PROJECTS, persBean);
    storeBeanList( keywordsFld, KEYWORDS, persBean);
    storeBeanList( datesFld, DATES, persBean);
    storeBeanList( contributorsFld, CONTRIBUTORS, persBean);
    storeBeanList( docsFld, DOCUMENTATION, persBean);

    if (dataset instanceof InvDatasetScan) {
      setStoreValue( dscanPP, DSCAN_PATH, persBean, false);
      setStoreValue( dscanPP, DSCAN_DIR, persBean, false);
      //setStoreValue( dscanPP, DSCAN_FILTER, persBean, false);
      setStoreValue( dscanPP, DSCAN_ADDSIZE, persBean, false);
      //setStoreValue( dscanPP, DSCAN_ADDLATEST, persBean, false);
      //setStoreValue( dscanPP, DSCAN_TC_MATCH, persBean, false);
      //setStoreValue( dscanPP, DSCAN_TC_SUBS, persBean, false);
      //setStoreValue( dscanPP, DSCAN_TC_DURATOPN, persBean, false);
    }

    dataset.finish();
  }

  private void storeGC( PersistentBean gcBean) {
    setStoreValue(LAT_START, gcBean, false);
    setStoreValue(LAT_EXTENT, gcBean, false);
    setStoreValue(LAT_RESOLUTION, gcBean, false);
    setStoreValue(LAT_UNITS, gcBean, false);

    setStoreValue(LON_START, gcBean, false);
    setStoreValue(LON_EXTENT, gcBean, false);
    setStoreValue(LON_RESOLUTION, gcBean, false);
    setStoreValue(LON_UNITS, gcBean, false);

    setStoreValue(HEIGHT_START, gcBean, false);
    setStoreValue(HEIGHT_EXTENT, gcBean, false);
    setStoreValue(HEIGHT_RESOLUTION, gcBean, false);
    setStoreValue(HEIGHT_UNITS, gcBean, false);

    setStoreValue(ZPOSITIVE_UP, gcBean, false);
  }

  private void setStoreValue(String name, PersistentBean bean, boolean inheritable) {
    setStoreValue(metadataPP, name, bean, inheritable);
  }

  private void setStoreValue(PrefPanel pp, String name, PersistentBean bean, boolean inheritable) {
    if (bean == null) return;

    Field fld = pp.getField(name);
    Object newValue = fld.getValue();

    //if (newValue == null) return; // LOOK remove from store ??

    // if it matches whats already stored (inherited or not), dont need to store it
    Object oldValue = bean.getObject( name);
    if (newValue == oldValue) return;
    if ((newValue != null) && newValue.equals(oldValue))
      return;

    // otherwise store it
    if (!inheritable)
      bean.putObject( name, newValue);
    else if (isInheritable(fld))
      bean.putObject( "localMetadataInheritable."+name, newValue);
    else
      bean.putObject( "localMetadata."+name, newValue);
  }

  private void storeBeanList(Field.BeanTable beanTable, String name, PersistentBean bean) {
    if (bean == null) return;

    List newValue = (List) beanTable.getValue();

    // if it matches whats already stored (inherited or not), dont need to store it
    List oldValue = (List) bean.getObject( name);
    if (newValue.equals(oldValue))
      return;

    // otherwise store it
    if (isInheritable(beanTable))
      bean.putObject( "localMetadataInheritable."+name, newValue);
    else
      bean.putObject( "localMetadata."+name, newValue);
  }

  ////////////////////////////////////////////

  public InvDatasetImpl getExtractedDataset() {
    if (leafDataset == null)
      leafDataset = findLeafDataset( dataset);
    return leafDataset;
  }

  private void extractGeospatial(){
    if (leafDataset == null)
      leafDataset = findLeafDataset( dataset);

    if (leafDataset != null) {
      ThreddsMetadata.GeospatialCoverage gc = null;
      try {
        gc = MetadataExtractor.extractGeospatial( leafDataset);
      } catch (IOException e) {
        return;
      }
      if (gc != null)
        setGC( gc, 1);
    }
  }

  private void extractVariables() {
    if (leafDataset == null)
      leafDataset = findLeafDataset(dataset);
    if (leafDataset == null) return;

    ThreddsMetadata.Variables vars = null;
    try {
      vars = MetadataExtractor.extractVariables(leafDataset);
    } catch (IOException e) {
      return;
    }

    if (vars != null) {
      ThreddsMetadata tm = dataset.getLocalMetadataInheritable();
      List varsList = tm.getVariables();
      boolean replaced = false;
      for (int i = 0; i < varsList.size(); i++) {
        ThreddsMetadata.Variables vs = (ThreddsMetadata.Variables) varsList.get(i);
        if (vs.getVocabulary().equals(vars.getVocabulary())) {
          varsList.set(i, vars); // replace
          replaced = true;
          break;
        }
      }
      if (!replaced)
        tm.addVariables(vars);

      variablesFld.setValue(vars.getVariableList());
      setMode(variablesFld, 1);
      return;
    }
  }

  private InvDatasetImpl findLeafDataset(InvDatasetImpl ds) {
    leafDataset = getLeafDataset(ds);
    return leafDataset;
  }

  private InvDatasetImpl getLeafDataset(InvDatasetImpl ds) {
    if (ds == null) return null;
    System.out.println("getLeafDataset "+ds.getFullName());
    List nestedList = ds.getDatasets();
    for (int i = 0; i < nestedList.size(); i++) {
      InvDatasetImpl nestedDS = (InvDatasetImpl) nestedList.get(i);

      if (nestedDS.hasAccess() && !nestedDS.hasNestedDatasets()) {
        InvDatasetImpl result = chooseLeafDataset(nestedList); // this assumes that they are uniform !!
        return (result.hasAccess()) ? result : nestedDS;
      }

      // depth first
      InvDatasetImpl leaf = getLeafDataset( nestedDS);
      if (leaf != null)
        return leaf;
    }

    return null;
  }

  private  InvDatasetImpl chooseLeafDataset(List nestedList) {
    // try random one
    int size = nestedList.size();
    return (InvDatasetImpl) nestedList.get(size/2);
  }

    ////////////////////////////////////////////////////////////////////
  /**
   * test
   */
  private static InvCatalogImpl cat;
  public static void main(String args[]) {
    JFrame frame;
    final DatasetEditor editor = new DatasetEditor();

    frame = new JFrame("Test DatasetEditor");
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        System.exit(0);
      }
    });

    JButton save = new JButton("Accept");
    save.addActionListener( new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        editor.accept();
        editor.store2Dataset();
        try {
          cat.writeXML (System.out, true);
        } catch (IOException e1) {
          e1.printStackTrace();
        }
      }
    }
    );
    JPanel main  = new JPanel( new BorderLayout());
    main.add(editor, BorderLayout.CENTER);
    main.add(save, BorderLayout.NORTH);

    frame.getContentPane().add(main);
    frame.pack();
    frame.setLocation(150, 10);
    frame.setVisible(true);

    // LOOK-NOSAVE
    java.net.Authenticator.setDefault(new thredds.ui.UrlAuthenticatorDialog(frame));

    // String url = "http://uni10.unidata.ucar.edu:8088/thredds/content/idd/models.xml";
    String url = "http://motherlode.ucar.edu:8088/thredds/content/idd/models.xml";
    InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory(true);
    cat = catFactory.readXML(url);
    InvDatasetImpl ds = (InvDatasetImpl) cat.findDatasetByID("NCEP/NAM/V");

    editor.setDataset(ds);
  }

}
