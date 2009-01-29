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
package thredds.catalog.ui.query;

import thredds.catalog.*;
import thredds.catalog.query.*;
import thredds.catalog.ui.*;

import thredds.ui.RangeSelector;
import thredds.ui.RangeDateSelector;

import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.Station;
import ucar.unidata.util.Format;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.ComboBox;
import ucar.util.prefs.ui.Debug;
import ucar.nc2.ui.point.StationRegionDateChooser;
import ucar.nc2.units.DateRange;
import ucar.nc2.util.IO;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Component;
import java.awt.event.*;
import java.beans.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 * Choose datasets from a Dataset Query Capabilities (DQC) XML document.
 * The user interface is built dynamically based on what is in the DQC.
 * <p/>
 * When the user selects dataset, a PropertyChangeEvent is thrown,
 * see addPropertyChangeListener for details.
 * <p/>
 * Example:
 * <pre>
 * node = (prefs == null) ? null : (PreferencesExt) prefs.node("dqc");
 * queryChooser = new QueryChooser(node);
 * queryChooser.addPropertyChangeListener( new PropertyChangeListener() {
 * public void propertyChange( PropertyChangeEvent e) {
 * if (e.getPropertyName().equals("Dataset")) doSomething();
 * }
 * });
 * queryChooser.setDataset( dqc_dataset);
 * </pre>
 *
 * @author John Caron
 * @version $Id: QueryChooser.java 50 2006-07-12 16:30:06Z caron $
 */

public class QueryChooser extends JPanel {
  private static final String HDIVIDER = "HSplit_Divider";
  private ucar.util.prefs.PreferencesExt prefs;

  private DqcFactory dqcFactory = new DqcFactory(true);

  // chooser widgets, need object scope
  private StationRegionDateChooser mapChooser = null;

  // ui components
  private JPanel innerPanel;
  private JPanel buttPanel;
  private JLabel status;
  private CatalogChooser cc;
  private JSplitPane split;
  private ComboBox comboBox = null;

  // layout
  private ArrayList mapSelectors, smallSelectors, bigSelectors;

  // event management
  private EventListenerList listenerList = new EventListenerList();
//  private boolean eventsOK = true;

  // state
  private QueryCapability qc;
  private ArrayList choosers = new ArrayList();
  private boolean returnsCatalog = true;
  private ArrayList extraButtons = new ArrayList();

  private boolean debugLayout = false, debugNested = false;

  /**
   * Constructor.
   *
   * @param prefs     store persistent info
   * @param showCombo add combobox, keep list of DQC URLs
   */
  public QueryChooser(ucar.util.prefs.PreferencesExt prefs, boolean showCombo) {
    this.prefs = prefs;

    innerPanel = new JPanel();
    innerPanel.setLayout(new BorderLayout());
    setLayout(new BorderLayout());
    add(innerPanel, BorderLayout.CENTER);

    if (showCombo) {
      // combo box holds the catalogs
      comboBox = new ComboBox(prefs);

      // top panel
      JButton connectButton = new JButton("Connect");
      connectButton.setToolTipText("connect to this DQC");

      JPanel topPanel = new JPanel(new BorderLayout());
      topPanel.add(new JLabel("DQC URL:"), BorderLayout.WEST);
      topPanel.add(comboBox, BorderLayout.CENTER);
      topPanel.add(connectButton, BorderLayout.EAST);

      //status label
      //decoratedStatus = new JLabel("Not connected");

      // put it all together
      add(topPanel, BorderLayout.NORTH);
      //decorated.add(decoratedStatus, BorderLayout.SOUTH);

      // button listeners
      connectButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
          String name = (String) comboBox.getSelectedItem();
          //decoratedStatus.setText("Connecting to "+name);
          try {
            setDqcUrl(name);
            addToCB(name);
          } catch (Exception e) {
            //decoratedStatus.setText("Connection failed to " + name);
          } finally {
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
          }
        }
      });
    }

    // make catalog chooser ahead of time and reuse.
    PreferencesExt node = (prefs == null) ? null : (PreferencesExt) prefs.node("cc");
    cc = new CatalogChooser(node, false, true, false);
    cc.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
      public void propertyChange(java.beans.PropertyChangeEvent e) {
        if (e.getPropertyName().equals("Dataset") || e.getPropertyName().equals("File"))
          firePropertyChangeEvent(e);
      }
    });

  }

  /**
   * Set the DQC from a dataset with access type ServiceType.QC.
   *
   * @param ds: InvDataset with an access of ServiceType.QC
   * @return true if successful
   */
  public boolean setDataset(InvDataset ds) {
    InvAccess qcAccess = ds.getAccess(ServiceType.QC);
    if (null == qcAccess)
      throw new IllegalArgumentException("QueryChooser :" + ds.getName() + " not a QC");

    // this.dataset = ds;
    String urlString = qcAccess.getStandardUrlName();
    try {
      setDqcUrl(urlString);
      return true;
    } catch (java.net.MalformedURLException e) {
      javax.swing.JOptionPane.showMessageDialog(this, "Malformed URL= " + urlString);

    } catch (java.io.IOException ex) {
      javax.swing.JOptionPane.showMessageDialog(this, "Error opening DQC URL= " + urlString);
    }

    return false;
  }

  /**
   * Set the DQC from a URL string pointing to a DQC.
   *
   * @param urlString: url of the qc document
   * @throws java.net.MalformedURLException : if urlString is malformed.
   * @throws java.io.IOException            : error reading qc
   */
  public void setDqcUrl(String urlString) throws java.net.MalformedURLException, java.io.IOException {
    qc = dqcFactory.readXML(urlString);
    if (qc.hasFatalError()) {
      javax.swing.JOptionPane.showMessageDialog(this, "Fatal dqc errors= \n" + qc.getErrorMessages());
      return;
    }

    try {
      choosers = new ArrayList();
      buildUI();
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }

    addToCB(urlString);
  }

  private void buildUI() {
    // selectors
    mapSelectors = new ArrayList();
    smallSelectors = new ArrayList();
    bigSelectors = new ArrayList();
    mapChooser = null;

    ArrayList selectors = qc.getAllUniqueSelectors();
    for (int i = 0; i < selectors.size(); i++)
      addSelector((Selector) selectors.get(i));

    // action buttons
    buttPanel = new JPanel();
    JButton queryButton = new JButton("QueryAvailable");
    buttPanel.add(queryButton);
    queryButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        makeQuery();
      }
    });

    // extra buttons
    for (int i = 0; i < extraButtons.size(); i++)
      buttPanel.add((AbstractButton) extraButtons.get(i));

    // standard layout
    JPanel dqcPanel = new JPanel(new BorderLayout());
    status = new JLabel("");

    JPanel botPanel = new JPanel(new BorderLayout());
    botPanel.add(buttPanel, BorderLayout.NORTH);
    botPanel.add(status, BorderLayout.SOUTH);

    dqcPanel.add(layoutSelectors(), BorderLayout.CENTER);
    dqcPanel.add(botPanel, BorderLayout.SOUTH);

    // if returns a catalog, add CatalogChooser widget
    returnsCatalog = true;

    //setBorder(new javax.swing.border.EtchedBorder());
    innerPanel.removeAll();

    if (returnsCatalog) {
      split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, dqcPanel, cc);
      innerPanel.add(split, BorderLayout.CENTER);
      if (prefs != null)
        split.setDividerLocation(prefs.getInt(HDIVIDER, 800));

    } else {
      innerPanel.add(dqcPanel, BorderLayout.CENTER);
    }

    revalidate();
  }

  private JComponent layoutSelectors() {

    int nbig = bigSelectors.size();
    int nsmall = smallSelectors.size();
    int nmaps = mapSelectors.size();
    int ncols = nmaps + nbig;
    if (nsmall > 0) ncols++;

    if ((nbig == 0) && (nmaps == 0)) {
      Box hbox = new Box(BoxLayout.X_AXIS);
      addAll(hbox, smallSelectors);
      return hbox;
    }

    if ((ncols > 3) && (nmaps > 1)) {
      JTabbedPane tabs = new JTabbedPane();
      for (int i = 0; i < mapSelectors.size(); i++) {
        Component elem = (Component) mapSelectors.get(i);
        tabs.addTab("comp" + i, elem);
      }
      mapSelectors.clear();
      bigSelectors.add(tabs);
    }

    return standardLayout();
  }

  private JComponent standardLayout() {
    if (debugLayout) System.out.println(" dqc standardLayout; smallSelectors = " + smallSelectors.size());
    Box hbox = new Box(BoxLayout.X_AXIS);

    if (smallSelectors.size() == 1) {
      addAll(hbox, smallSelectors);

    } else if (smallSelectors.size() > 1) {

      Box vbox = new Box(BoxLayout.Y_AXIS);
      addAll(vbox, smallSelectors);
      vbox.add(Box.createGlue());
      hbox.add(vbox);
    }

    addAll(hbox, bigSelectors);
    addAll(hbox, mapSelectors);
    return hbox;
  }

  private void addAll(Container c, ArrayList list) {
    for (int i = 0; i < list.size(); i++) {
      c.add((JComponent) list.get(i));
      if (debugLayout) System.out.println(" layout added "+list.get(i));
    }
  }

  // wrap each selecctor in a chooser, that tracks what choices are makes in the UI widget.
  private void addSelector(Selector s) {

    if (s instanceof SelectList) {
      SelectList selectList = (SelectList) s;
      ChooserList clist = new ChooserList(selectList, selectList.getChoices());
      choosers.add(clist);

      // Set the preferred row count. This affects the preferredSize
      // of the JList when it's in a scrollpane.
      //jlist.setVisibleRowCount(Math.min(selectList.getSize(), 10));

      JScrollPane scrollPane = new JScrollPane(clist.jlist);
      JPanel panel = new JPanel(new BorderLayout());
      panel.add(BorderLayout.NORTH, new JLabel(selectList.getTitle()));
      panel.add(BorderLayout.CENTER, scrollPane);
      panel.setBorder(new javax.swing.border.EtchedBorder());

      // add it to the selectorPanel or vselector
      if ((selectList.getSize() < 5) && !selectList.hasNestedSelectors()) {
        clist.jlist.setVisibleRowCount(selectList.getSize());
        smallSelectors.add(panel);
      } else
        bigSelectors.add(panel);

      // possible nested selecctors
      if (selectList.hasNestedSelectors()) {
        if (debugNested) System.out.println(" QueryChooser selectList.hasNestedSelectors = " + selectList.getId());

        addSelector(selectList.getFirstNestedSelector());
      }

      //////////////////////////////////////////////////////////////////////////
    } else if (s instanceof SelectService) {
      SelectService selectService = (SelectService) s;
      ChooserList clist = new ChooserList(selectService, selectService.getChoices());
      choosers.add(clist);

      if (selectService.getChoices().size() == 1)
        return; // dont need a visible widget

      // Set the preferred row count. This affects the preferredSize
      // of the JList when it's in a scrollpane.
      clist.jlist.setVisibleRowCount(Math.min(selectService.getChoices().size(), 5));

      // layout
      JScrollPane scrollPane = new JScrollPane(clist.jlist);
      JPanel panel = new JPanel(new BorderLayout());
      panel.add(BorderLayout.NORTH, new JLabel(selectService.getTitle()));
      panel.add(BorderLayout.CENTER, scrollPane);
      panel.setBorder(new javax.swing.border.EtchedBorder());

      // add it to the vertical selectorPanel
      smallSelectors.add(panel);

      //////////////////////////////////////////////////////////////////////////
    } else if (s instanceof SelectStation) {
      SelectStation selectStation = (SelectStation) s;
      boolean need2add = makeMapChooser();

      // wrap stations to implement StationIF
      // LOOK : can we use proxy?
      List stations = selectStation.getStations();
      ArrayList wrappedStations = new ArrayList(stations.size());
      for (int i = 0; i < stations.size(); i++) {
        wrappedStations.add(new DqcStation((thredds.catalog.query.Station) stations.get(i)));
      }
      mapChooser.setStations(wrappedStations);
      choosers.add(new ChooserStation(selectStation, mapChooser));

      // layout
      if (need2add) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(BorderLayout.NORTH, new JLabel(s.getTitle() + ":"));
        panel.add(BorderLayout.CENTER, mapChooser);
        panel.setBorder(new javax.swing.border.EtchedBorder());
        mapSelectors.add(panel);
      }

      //////////////////////////////////////////////////////////////////////////
    } else if (s instanceof SelectGeoRegion) {
      SelectGeoRegion geoRegion = (SelectGeoRegion) s;
      boolean need2add = makeMapChooser();

      Location loc = geoRegion.getLowerLeft();
      LatLonPoint left = new LatLonPointImpl(loc.getLatitude(), loc.getLongitude());
      loc = geoRegion.getUpperRight();
      LatLonPoint right = new LatLonPointImpl(loc.getLatitude(), loc.getLongitude());
      LatLonRect bounds = new LatLonRect(left, right);
      mapChooser.setGeoBounds(bounds);

      double centerLon = bounds.getCenterLon();
      double width = bounds.getWidth();
      double centerLat = (right.getLatitude() + left.getLatitude()) / 2;
      double height = right.getLatitude() - left.getLatitude();
      right = new LatLonPointImpl(centerLat + height / 4, centerLon + width / 4);
      left = new LatLonPointImpl(centerLat - height / 4, centerLon - width / 4);
      LatLonRect selected = new LatLonRect(left, right);
      mapChooser.setGeoSelection(selected);

      choosers.add(new ChooserGeo(geoRegion, mapChooser));

      if (need2add) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(BorderLayout.NORTH, new JLabel(s.getTitle() + ":"));
        panel.add(BorderLayout.CENTER, mapChooser);
        panel.setBorder(new javax.swing.border.EtchedBorder());
        mapSelectors.add(panel);
      }

      //////////////////////////////////////////////////////////////////////////
    } else if (s instanceof SelectRange) {
      SelectRange sr = (SelectRange) s;
      InvDocumentation desc = sr.getDescription();
      String help = (desc == null) ? null : desc.getInlineContent();
      RangeSelector range = new RangeSelector(sr.getTitle(), sr.getMin(), sr.getMax(),
          sr.getResolution(), sr.getUnits(), true, help, sr.getSelectType().equals("point"));

      choosers.add(new ChooserRange(sr, range));
      smallSelectors.add(range);

      //////////////////////////////////////////////////////////////////////////
    } else if (s instanceof SelectRangeDate) {
      SelectRangeDate srd = (SelectRangeDate) s;
      InvDocumentation desc = srd.getDescription();
      String help = (desc == null) ? null : desc.getInlineContent();

      try {
        RangeDateSelector range = new RangeDateSelector(srd.getTitle(),
            srd.getStart(), srd.getEnd(),
            srd.getDuration(), srd.getResolution(), true, false, help,
            srd.getSelectType().equals("point"));

        choosers.add(new ChooserRangeDate(srd, range));
        smallSelectors.add(range);

      } catch (Exception e) {
        System.out.println("Error on SelectRangeDate = " + srd.getTitle());
        e.printStackTrace();
      }


    }

    /* depth first traversal
    ArrayList selectors = s.getDependentSelectors();
    for (int i=0; i< selectors.size(); i++) {
      Object o = selectors.get(i);
      addSelector( (Selector) o, true);
    } */
  }

  /* private void doAccept() {
    Object[] selected = resultList.getSelectedValues();
    if (selected.length == 0) {
      resultStatus.setText("  You must select from Available Datasets");
      return;
    }

    if (debug) {
      for (int i=0; i<selected.length; i++) {
        System.out.print("  result = "+selected[i]);
        System.out.println("  class = "+selected[i].getClass().getName());

        try {
          InvDataset dataset = (InvDataset) selected[i];
          InvAccess access = dataset.getAccess(ServiceType.ADDE);
          if (access != null) {
            System.out.println("  urlPath = "+access.getStandardUrlName());
            resultStatus.setText("  urlPath = "+access.getStandardUrlName());
          }
        } catch (Exception e) { e.printStackTrace(); }
      }
    }
    firePropertyChangeEvent( "Datasets", selected);
  } */

  // return true if newly made
  private boolean makeMapChooser() {
    if (null != mapChooser) return false;
    mapChooser = new StationRegionDateChooser(true, true, false);
    return true;
  }

  private void showStatus() {
    /* StringBuffer sbuff = new StringBuffer();
    sbuff.setLength(0);
    sbuff.append(qc.getName()+": ");
    for (int i=0; i<choosers.size(); i++) {
      Chooser c = (Chooser) choosers.get(i);
      //if (c.currentChoice != null) {
      //  sbuff.append(c.label+"="+c.currentChoice.getName()+"; ");
      //}
    }
    status.setText(sbuff.toString()); */
  }

  private void makeQuery() {

    // look through all the choosers, make sure all required ones are done
    Chooser need = null;
    for (int i = 0; i < choosers.size(); i++) {
      Chooser c = (Chooser) choosers.get(i);
      if (!c.hasChoice() && c.isRequired()) {
        need = c;
        break;
      }
    }
    if (null != need) {
      javax.swing.JOptionPane.showMessageDialog(this, "You must make selection from " + need.getName());
      return;
    } else {
      status.setText(" ");
    }

    // construct the query
    String queryString = qc.getQuery().getUriResolved().toString();
    StringBuffer queryb = new StringBuffer();
    queryb.append(queryString);
    for (int i = 0; i < choosers.size(); i++) {
      Chooser c = (Chooser) choosers.get(i);
      if (!c.hasChoice()) continue;

      Selector s = c.getSelector();
      s.appendQuery(queryb, c.getChoices());
    }

    queryString = queryb.toString();
    status.setText(queryString);
    if (Debug.isSet("dqc/showQuery")) {
      System.out.println("dqc/showQuery= " + queryString);
    }

    // fetch the catalog
    InvCatalogFactory factory = InvCatalogFactory.getDefaultFactory(true); // use default factory
    InvCatalog catalog = factory.readXML(queryString);
    StringBuilder buff = new StringBuilder();
    if (!catalog.check(buff)) {
      javax.swing.JOptionPane.showMessageDialog(this, "Invalid catalog " + buff.toString());
      System.out.println("Invalid catalog " + buff.toString());
      return;
    }
    if (Debug.isSet("dqc/showQueryResult")) {
      System.out.println("dqc/showQueryResult catalog check msgs= " + buff.toString());
      System.out.println("  query result =\n" + IO.readURLcontents(queryString));
    }
    cc.setCatalog((InvCatalogImpl) catalog);
  }

  /**
   * Add a PropertyChangeEvent Listener. Throws a PropertyChangeEvent:
   * <ul>
   * <li>  propertyName = "Dataset", getNewValue() = an InvDataset chosen.
   * </ul>
   */
  public void addPropertyChangeListener(PropertyChangeListener l) {
    listenerList.add(PropertyChangeListener.class, l);
  }

  /**
   * Remove a PropertyChangeEvent Listener.
   */
  public void removePropertyChangeListener(PropertyChangeListener l) {
    listenerList.remove(PropertyChangeListener.class, l);
  }

/*  private void firePropertyChangeEvent(String name, Object newValue) {
    firePropertyChangeEvent (new PropertyChangeEvent(this, name, null, newValue));
  } */

  private void firePropertyChangeEvent(PropertyChangeEvent event) {
    Object[] listeners = listenerList.getListenerList();
    // Process the listeners last to first
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == PropertyChangeListener.class) {
        ((PropertyChangeListener) listeners[i + 1]).propertyChange(event);
      }
    }
  }


  // add name to list if not already there
  private void addToCB(String name) {
    if (comboBox == null) return;
    comboBox.addItem(name);
  }

  /**
   * save persistent state
   */
  public void save() {
    if (comboBox != null) comboBox.save();
    if (prefs != null) {
      if (split != null) prefs.putInt(HDIVIDER, split.getDividerLocation());
    }
    if (cc != null) cc.save();
  }

  /**
   * Get the current DQC URL from the comboBox.
   */
  public String getCurrentURL() {
    return (comboBox == null) ? null : (String) comboBox.getSelectedItem();
  }

  /**
   * Get the component CatalogChooser
   */
  public CatalogChooser getCatalogChooser() { return cc; }

  /**
   * Add a button to the lower row of buttons.
   */
  public void addButton(AbstractButton b) {
    extraButtons.add(b);
  }

  private class ListModel extends AbstractListModel {
    private ArrayList list;

    ListModel(ArrayList list) { this.list = list; }

    public int getSize() { return list.size(); }

    public Object getElementAt(int index) { return list.get(index); }
  }

  // the Choosers wrap a Selector, and manage a UI widget.
  private abstract class Chooser {
    Selector selector;

    Chooser(Selector s) {
      this.selector = s;
    }

    String getName() { return selector.getTitle(); }

    String getId() { return selector.getId(); }

    Selector getSelector() { return selector; }

    boolean isRequired() { return selector.isRequired(); }

    abstract boolean hasChoice();

    abstract ArrayList getChoices();
  }

  // choose from list of Choice elements
  private class ChooserList extends Chooser {
    JList jlist;
    boolean loaded = false;

    ChooserList(Selector sel, ArrayList choices) {
      super(sel);

      this.jlist = new JList(new ListModel(choices));
      if (sel.isMultiple())
        jlist.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
      else
        jlist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

      // listen for new selection
      jlist.addListSelectionListener(new ListSelectionListener() {
        public void valueChanged(ListSelectionEvent e) {
          if (e.getValueIsAdjusting()) return;

          if (null == jlist.getSelectedValue()) return;
          Choice currentChoice = (thredds.catalog.query.Choice) jlist.getSelectedValue();
          if (null == currentChoice) return;

          // deal with nested lists
          if ((currentChoice instanceof ListChoice) &&
              ((ListChoice) currentChoice).hasNestedSelectors()) {

            ListChoice listChoice = (ListChoice) currentChoice;
            ArrayList selectors = listChoice.getNestedSelectors();
            for (int i = 0; i < selectors.size(); i++) {
              SelectList sl = (SelectList) selectors.get(i);
              loadNested(sl);
            }
          }
        }
      });
    }

    private void setList(ArrayList choices) {
      jlist.setModel(new ListModel(choices));
    }

    private void loadNested(SelectList nestedList) {
      if (nestedList == null) return;

      // find the corresponding chooser
      ChooserList nestedChooser = (ChooserList) findChooser(nestedList.getId());
      if (nestedChooser == null) return;

      ArrayList choices = nestedList.getChoices();
      nestedChooser.setList(choices);
      nestedChooser.jlist.setSelectedIndex(0);
      nestedChooser.jlist.repaint();
    }

    Chooser findChooser(String id) {
      for (int i = 0; i < choosers.size(); i++) {
        Chooser c = (Chooser) choosers.get(i);
        if (id.equals(c.getId()))
          return c;
      }
      return null;
    }

    /* void setSelectList(SelectList lsel) {
      loaded = true;
      if (currentChoice != null) {
        SelectList currentSelector = (SelectList) currentChoice.getParentSelector();
        if (lsel == currentSelector)
          return;
      }

      jlist.setModel( new ListModel( lsel.getChoices()));
      jlist.setEnabled(true);
      currentChoice = (Choice) lsel.getChoices().get(0);
    }

       // recursively mark parent choosers as loaded
    void markLoaded() {
      loaded = true;
      if (currentChoice == null) return;
      SelectList currentSelector = (SelectList) currentChoice.getParentSelector();
      if (currentSelector == null) return;
      Selector parentSelector = currentSelector.getParentSelector();
      if (parentSelector != null) {
        Chooser c = findChooser( parentSelector.getId());
        if (c != null)
          c.markLoaded();
      }
    }

       // recursively load nested selectors
    private void loadNested(Choice choice) {
      ArrayList selectors = choice.getSelectors();
      for (int i=0; i< selectors.size(); i++) {
        Object o = selectors.get(i);
        if (debug) System.out.println(" has nested = " + o.getClass().getName());
        if (o instanceof SelectList) {
          SelectList nested = (SelectList) o;
          Chooser c = findChooser( nested.getId());
          if (debug) System.out.println(" found chooser for "+nested.getId());
          c.setSelectList( nested);
          loadNested(c.currentChoice);
            //?? c.jlist.setSelectedIndex(0);
          //c.jcomp.repaint();
        }
      }
    }

       // now load any non-nested selectors
    private void loadNonNested(Chooser chooser) {
      if (debug) System.out.println(" need to load "+chooser.id);
      GraphNode grandparent = currentChoice.getParent().getParent();
      SelectList want = search(grandparent, chooser.id);

      if (want != null) {
        if (debug) System.out.println(" found non-nested chooser for "+chooser.id);
        chooser.setSelectList( want);
      }
    }

    // perform GraphNode search
    private SelectList search(GraphNode node, String id) {
      if (node == null) return null;

      ArrayList kids = node.getChildren();
      for (int i=0; i<kids.size(); i++) {
        Object k = kids.get(i);
        if (k instanceof SelectList) {
          if (id.equals(((SelectList) k).getId()))
            return (SelectList) k;
        }
      }

      return search( node.getParent(), id);
    } */

    boolean hasChoice() { return !jlist.isSelectionEmpty(); }

    ArrayList getChoices() {
      ArrayList choices = new ArrayList();
      Object[] values = jlist.getSelectedValues();
      for (int i = 0; i < values.length; i++) {
        Choice c = (Choice) values[i];
        choices.add("{value}");
        choices.add(c.getValue());
      }
      return choices;
    }

  } // ChooserList

  private class ChooserStation extends Chooser {
    StationRegionDateChooser mapChooser;
    DqcStation currentChoice;

    ChooserStation(SelectStation sel, StationRegionDateChooser mapChooser) {
      super(sel);
      this.mapChooser = mapChooser;

      mapChooser.addPropertyChangeListener(new PropertyChangeListener() {
        public void propertyChange(java.beans.PropertyChangeEvent e) {
          if (e.getPropertyName().equals("Station")) {
            currentChoice = (DqcStation) e.getNewValue();
            showStatus();
          }
        }
      });
    }

    boolean hasChoice() { return currentChoice != null; }

    ArrayList getChoices() {
      ArrayList choices = new ArrayList();
      if (currentChoice != null) {
        choices.add("{value}");
        choices.add(currentChoice.s.getValue());
      }
      return choices;
    }
  }

  private class ChooserGeo extends Chooser {
    StationRegionDateChooser geoChooser;

    ChooserGeo(SelectGeoRegion sel, StationRegionDateChooser geoChooser) {
      super(sel);
      this.geoChooser = geoChooser;
    }

    boolean hasChoice() { return geoChooser.getGeoSelection() != null; }

    ArrayList getChoices() {
      LatLonRect llbb = geoChooser.getGeoSelectionLL();
      ArrayList choices = new ArrayList();
      if (llbb != null) {
        LatLonPoint llpoint = llbb.getLowerLeftPoint();

        choices.add("{minLat}");
        choices.add(Format.d(llpoint.getLatitude(), 5));
        choices.add("{minLon}");
        choices.add(Format.d(llpoint.getLongitude(), 5));

        LatLonPoint urPoint = llbb.getUpperRightPoint();
        choices.add("{maxLat}");
        choices.add(Format.d(urPoint.getLatitude(), 5));
        choices.add("{maxLon}");
        choices.add(Format.d(urPoint.getLongitude(), 5));
      }
      return choices;
    }
  }

  private class ChooserRange extends Chooser {
    RangeSelector range;
    boolean isPoint;

    ChooserRange(SelectRange sel, RangeSelector range) {
      super(sel);
      this.range = range;
      isPoint = "point".equals(sel.getSelectType());
    }

    boolean hasChoice() { return true; }

    ArrayList getChoices() {
      ArrayList choices = new ArrayList();
      if (isPoint) {
        choices.add("{point}");
        choices.add(range.getMinSelectedString());
      } else {
        choices.add("{min}");
        choices.add(range.getMinSelectedString());
        choices.add("{max}");
        choices.add(range.getMaxSelectedString());
      }
      return choices;
    }
  }

  private class ChooserRangeDate extends Chooser {
    RangeDateSelector rds;
    boolean isPoint;

    ChooserRangeDate(SelectRangeDate sel, RangeDateSelector range) {
      super(sel);
      this.rds = range;
      isPoint = "point".equals(sel.getSelectType());
    }

    boolean hasChoice() { return rds.isEnabled(); }

    ArrayList getChoices() {
      DateRange selected = rds.getDateRange();
      if (selected == null) selected = rds.getDateRange(); // LOOK force acceptence, should bail out
      ArrayList choices = new ArrayList();
      if (isPoint) {
        choices.add("{point}");
        choices.add(selected.getStart().toString());
      } else {
        choices.add("{start}");
        choices.add(selected.getStart().toDateTimeStringISO());
        choices.add("{end}");
        choices.add(selected.getEnd().toDateTimeStringISO());
        choices.add("{duration}");
        choices.add(selected.getDuration().toString());
      }
      return choices;
    }
  }

  private class DqcStation implements Station {
    thredds.catalog.query.Station s;

    DqcStation(thredds.catalog.query.Station s) {
      this.s = s;
    }

    public String getID() {
      return s.getValue();
    }

    public String getName() {
      return s.getValue();
    }

    public String getDescription() {
      thredds.catalog.InvDocumentation doc = s.getDescription();
      return (doc == null) ? s.getName() : doc.getInlineContent();
    }

    public String getWmoId() {
      return null;
    }

    public double getLatitude() {
      return s.getLocation().getLatitude();
    }

    public double getLongitude() {
      return s.getLocation().getLongitude();
    }

    public double getAltitude() {
      return s.getLocation().getElevation();
    }

    public LatLonPoint getLatLon() {
      return new LatLonPointImpl( getLatitude(), getLongitude());
    }

    public boolean isMissing() {
      return Double.isNaN(getLatitude()) || Double.isNaN(getLongitude());
    }

    public int compareTo(Station so) {
      return getName().compareTo(so.getName());
    }
  }

  /////////////////////////////////////////////////////////////////////////////
  // convenience routine for dialogs

  /**
   * Convenience routine for making the QC into a dialog.
   * Do not call before calling setDataset() or setQC().
   * Use dialog.show() and dialog.setVisible(true) to popup and hide.
   *
   * @param parent: parent of the dialog, usually a JFrame
   * @param title:  Dialog title; if null, use QC name
   * @param modal:  is modal
   * @return the JDialog
   */
  public JDialog makeDialog(RootPaneContainer parent, String title, boolean modal) {
    return new MyDialog(parent, title == null ? qc.getName() : title, modal);
  }

  private class MyDialog extends JDialog {

    private MyDialog(RootPaneContainer parent, String title, boolean modal) {
      super(parent instanceof java.awt.Frame ? (java.awt.Frame) parent : null, title, modal);

      // L&F may change
      UIManager.addPropertyChangeListener(new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent e) {
          if (e.getPropertyName().equals("lookAndFeel"))
            SwingUtilities.updateComponentTreeUI(QueryChooser.MyDialog.this);
        }
      });

      // add a dismiss button
      JButton dismissButton = new JButton("Dismiss");
      buttPanel.add(dismissButton, null);

      dismissButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          setVisible(false);
        }
      });

      // add it to contentPane
      java.awt.Container cp = getContentPane();
      cp.setLayout(new BorderLayout());
      cp.add(QueryChooser.this, BorderLayout.CENTER);
      pack();
    }
  }

  ////////////////////////////////////////////////////////////////////
  /**
   * test
   */
  public static void main(String args[]) {
    JFrame frame;
    QueryChooser qcs;

    frame = new JFrame("Test QueryChooser");
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        System.exit(0);
      }
    });


    qcs = new QueryChooser(new PreferencesExt(null, ""), true);

    // listen for selection
    qcs.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(java.beans.PropertyChangeEvent e) {
        System.out.println("qcs.PropertyChange = " + e.getPropertyName() + " " + e.getNewValue().getClass().getName());
      }
    });

    try {
      qcs.setDqcUrl("file:///C:/dev/thredds/catalog/test/data/dqc/exampleDqc.xml");
      // qcs.setDqcUrl("file:///C:/dev/thredds/catalog/test/data/dqc/dqc.JplQuikScat.xml");
    } catch (Exception e) {
      try {
        JOptionPane.showMessageDialog(null, null, e.getMessage(),
            JOptionPane.ERROR_MESSAGE);
      } catch (java.awt.HeadlessException ee) {}
    }

    frame.getContentPane().add(qcs);
    frame.pack();
    frame.setLocation(300, 300);
    frame.setVisible(true);
  }

}

/* Change History:
   $Log: QueryChooser.java,v $
   Revision 1.20  2005/08/22 01:12:24  caron
   DatasetEditor

   Revision 1.19  2005/06/23 19:18:50  caron
   no message

   Revision 1.18  2005/06/11 19:03:55  caron
   no message

   Revision 1.17  2005/05/12 14:29:54  caron
   more station refactoring
   intelliJ CVS wierdness

   Revision 1.16  2005/05/04 17:56:27  caron
   use nj22.09

   Revision 1.15  2005/05/04 17:16:23  caron
   replace ThreddsMetadata.TimeCoverage object with DateRange

   Revision 1.14  2005/05/04 02:16:12  caron
   mv CatalogEnhancer.java, consolidate QueryChooser widgets

   Revision 1.13  2004/12/14 15:41:01  caron
   *** empty log message ***

   Revision 1.12  2004/11/16 23:35:36  caron
   no message

   Revision 1.11  2004/11/07 03:00:47  caron
   *** empty log message ***

   Revision 1.10  2004/11/07 02:55:10  caron
   no message

   Revision 1.9  2004/10/15 19:16:07  caron
   enum now keyword in 1.5
   SelectDateRange send ISO date string

   Revision 1.8  2004/09/24 03:26:31  caron
   merge nj22

   Revision 1.7  2004/06/19 00:45:43  caron
   redo nested select list

   Revision 1.6  2004/06/18 21:54:27  caron
   update dqc 0.3

   Revision 1.5  2004/06/12 04:12:43  caron
   *** empty log message ***

   Revision 1.4  2004/06/12 02:01:11  caron
   dqc 0.3

   Revision 1.3  2004/06/09 00:27:29  caron
   version 2.0a release; cleanup javadoc

   Revision 1.2  2004/05/21 05:57:33  caron
   release 2.0b

*/