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
package ucar.nc2.ui.point;

import thredds.viewer.ui.*;
import thredds.viewer.ui.Renderer;
import thredds.viewer.ui.geoloc.*;
import thredds.viewer.ui.event.*;
import thredds.ui.BAMutil;
import thredds.ui.PopupManager;
import thredds.ui.RangeDateSelector;
import thredds.ui.IndependentDialog;
import ucar.nc2.units.DateRange;

import ucar.unidata.geoloc.*;
import ucar.util.prefs.ui.*;
import ucar.unidata.geoloc.Station;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.EventListenerList;


/**
 * A Swing widget for THREDDS clients to choose a station and/or a region from navigatable map.
 * <p>
 * Typically a user listens for property change events:
 *  <pre>
 *   stationRegionDateChooser.addPropertyChangeListener( new PropertyChangeListener() {
        public void propertyChange(java.beans.PropertyChangeEvent e) {
          if (e.getPropertyName().equals("Station")) {
            selectedStation = (Station) e.getNewValue();
            ...
          }
          else if (e.getPropertyName().equals("GeoRegion")) {
            geoRegion = (ProjectionRect) e.getNewValue();
            ...
          }
        }
      });
   </pre>
 *
 * @author caron
 * @version $Revision: 50 $ $Date: 2006-07-12 16:30:06Z $
 */

/* implementation note:
 * do we wnat to remove actionSource ? we have setSelectedStation instead.
 */
public class StationRegionDateChooser extends thredds.viewer.ui.geoloc.NPController {
  private boolean regionSelect = true, stationSelect = true, dateSelect = true;

  // station
  private StationRenderer stnRender = null;
  private Station selectedStation;

  // region
  private ProjectionRect geoBounds;
  private ProjectionRect geoSelection;
  private boolean geoSelectionMode = false;
  private Color outlineColor = Color.black;
  private int nfracDig = 3;

  // date
  private RangeDateSelector dateSelector;
  private IndependentDialog  dateWindow;
  private AbstractAction dateAction;

  // prefs
  private PrefPanel minmaxPP;
  private Field.Double minLonField, maxLonField, minLatField, maxLatField;

  // events
  private EventListenerList listenerList = new EventListenerList();
  private ActionSourceListener actionSource;

  // local caches
  private PopupManager popupInfo = new PopupManager("Station Info");
  private StringBuffer sbuff = new StringBuffer();

  // debugging
  private boolean debugEvent = false;

  /**
   * Default Contructor, allow both region and station selection.
   */
  public StationRegionDateChooser() {
    this(true, true, true);
  }

  /**
   * Constructor
   * @param regionSelect allow selecting a region
   * @param stationSelect allow selecting a station
   * @param dateSelect allow selecting a date range
   */
  public StationRegionDateChooser(boolean stationSelect, boolean regionSelect, boolean dateSelect) {
    super();

    this.regionSelect = regionSelect;
    this.stationSelect = stationSelect;
    this.dateSelect = dateSelect;

    np.setGeoSelectionMode( regionSelect && geoSelectionMode);
    // setGeoBounds( np.getMapArea());

    if (stationSelect) {
      stnRender = new StationRenderer();
      addRenderer( stnRender);

      // get Pick events from the navigated panel: mouse click
      np.addPickEventListener( new PickEventListener() {
        public void actionPerformed(PickEvent e) {
          selectedStation = stnRender.pick(e.getLocation());
          if (selectedStation != null) {
            redraw();
            firePropertyChangeEvent(selectedStation, "Station");
            actionSource.fireActionValueEvent( ActionSourceListener.SELECTED, selectedStation);
          }
        }
      });


      // get mouse motion events
      np.addMouseMotionListener( new MouseMotionAdapter() {
        public void mouseMoved(MouseEvent e) {
          Point p = e.getPoint();
          StationRenderer.StationUI sui = stnRender.isOnStation(p);

          if (sui != null) {
            ucar.unidata.geoloc.Station s = sui.getStation();
            sbuff.setLength(0);
            sbuff.append(s.getName());
            sbuff.append(" ");
            sbuff.append("\n");
            if (null != s.getDescription())
              sbuff.append(s.getDescription()).append("\n");
            sbuff.append( LatLonPointImpl.latToString(s.getLatitude(), 4));
            sbuff.append(" ");
            sbuff.append( LatLonPointImpl.lonToString(s.getLongitude(), 4));
            sbuff.append(" ");
            double alt = s.getAltitude();
            if (!Double.isNaN(alt)) {
              sbuff.append( ucar.unidata.util.Format.d(alt,0));
              sbuff.append(" m");
            }

            popupInfo.show(sbuff.toString(), p, StationRegionDateChooser.this, s);
          }
          else
            popupInfo.hide();
        }
      });

      // get mouse exit events
      np.addMouseListener( new MouseAdapter() {
        public void mouseExited(MouseEvent e) {
          popupInfo.hide();
        }
      });

        // station was selected
      actionSource = new ActionSourceListener("station") {
        public void actionPerformed( ActionValueEvent e) {
          if (debugEvent) System.out.println(" StationdatasetChooser: actionSource event "+e);
          selectedStation = (ucar.unidata.geoloc.Station) e.getValue();
          redraw();
        }
      };
    }

    if (regionSelect) {
      double defArea = 1.0/8; // default area is 1/4 total
      LatLonRect llbb = np.getProjectionImpl().getDefaultMapAreaLL();
      LatLonPointImpl left = llbb.getLowerLeftPoint();
      LatLonPointImpl right = llbb.getUpperRightPoint();
      double centerLon = llbb.getCenterLon();
      double width = llbb.getWidth();
      double centerLat = (right.getLatitude() + left.getLatitude())/2;
      double height = right.getLatitude() - left.getLatitude();
      right = new LatLonPointImpl( centerLat + height*defArea, centerLon + width*defArea);
      left = new LatLonPointImpl( centerLat - height*defArea, centerLon - width*defArea);
      LatLonRect selected =  new LatLonRect( left, right);
      setGeoSelection( selected);

      // get GeoSelectionEvents from the navigated panel
      np.addGeoSelectionListener( new GeoSelectionListener() {
        public void actionPerformed(GeoSelectionEvent e) {
          setGeoSelection( e.getProjectionRect());
          if (debugEvent) System.out.println("GeoSelectionEvent="+geoSelection);
          firePropertyChangeEvent(geoSelection, "GeoRegion");
          redraw();
        }
      });
    }

    if (dateSelect) {
      // date selection
      DateRange range = null;
      try {
        range = new DateRange(); // phony
      } catch (Exception e) {
        e.printStackTrace();
      }
      dateSelector = new RangeDateSelector(null, range, true, false, null, false, true);
      dateWindow = new IndependentDialog(null, false, "Date Selection", dateSelector);
      dateAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          dateWindow.setVisible(true);
        }
      };
      BAMutil.setActionProperties( dateAction, "selectDate", "select date range", false, 'D', -1);
    }

    makeMyUI();
  }

  protected void makeUI() { } // override superclass
  private void makeMyUI() {

    AbstractAction incrFontAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        stnRender.incrFontSize();
        redraw();
      }
    };
    BAMutil.setActionProperties( incrFontAction, "FontIncr", "increase font size", false, 'I', -1);

    AbstractAction decrFontAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        stnRender.decrFontSize();
        redraw();
      }
    };
    BAMutil.setActionProperties( decrFontAction, "FontDecr", "decrease font size", false, 'D', -1);

    JCheckBox declutCB = new JCheckBox("Declutter", true);
    declutCB.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        setDeclutter(((JCheckBox) e.getSource()).isSelected());
      }
    });

    AbstractAction bbAction = new AbstractAction() {
       public void actionPerformed(ActionEvent e) {
         geoSelectionMode = !geoSelectionMode;
         np.setGeoSelectionMode( geoSelectionMode);
         redraw();
       }
     };
     BAMutil.setActionProperties( bbAction, "geoselect", "select geo region", true, 'B', -1);
     bbAction.putValue(BAMutil.STATE, geoSelectionMode ? Boolean.TRUE : Boolean.FALSE );

     // the fields use a PrefPanel
    if (regionSelect) {
      minmaxPP = new PrefPanel( null, null);
      minLonField = minmaxPP.addDoubleField("minLon", "minLon", geoSelection.getMinX(), nfracDig, 0, 0, null);
      maxLonField = minmaxPP.addDoubleField("maxLon", "maxLon", geoSelection.getMaxX(), nfracDig, 2, 0, null);
      minLatField = minmaxPP.addDoubleField("minLat", "minLat", geoSelection.getMinY(), nfracDig, 4, 0, null);
      maxLatField = minmaxPP.addDoubleField("maxLat", "maxLat", geoSelection.getMaxY(), nfracDig, 6, 0, null);

      minmaxPP.finish(true, BorderLayout.EAST);
      minmaxPP.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          // "Apply" was called
          double minLon = minLonField.getDouble();
          double minLat = minLatField.getDouble();
          double maxLon = maxLonField.getDouble();
          double maxLat = maxLatField.getDouble();
          LatLonRect llbb = new LatLonRect( new LatLonPointImpl(minLat, minLon),
                                           new LatLonPointImpl(maxLat, maxLon));
          setGeoSelection( llbb);
          redraw();
        }
      });



    }

    // assemble
    setLayout(new BorderLayout());

    if (stationSelect) {
      BAMutil.addActionToContainer( toolPanel, incrFontAction);
      BAMutil.addActionToContainer( toolPanel, decrFontAction);
      toolPanel.add(declutCB);
    }

    if (regionSelect) BAMutil.addActionToContainer( toolPanel, bbAction);
    if (dateSelect) BAMutil.addActionToContainer( toolPanel, dateAction);

    JPanel upperPanel = new JPanel(new BorderLayout());
    if (regionSelect) upperPanel.add(minmaxPP, BorderLayout.NORTH);
    upperPanel.add(toolPanel, BorderLayout.SOUTH);

    JPanel statusPanel = new JPanel(new BorderLayout());
    statusPanel.setBorder(new EtchedBorder());
    JLabel positionLabel = new JLabel("position");
    statusPanel.add(positionLabel, BorderLayout.CENTER);

    np.setPositionLabel(positionLabel);
    add(upperPanel, BorderLayout.NORTH);
    add(np, BorderLayout.CENTER);
    add(statusPanel, BorderLayout.SOUTH);
  }

  /**
   * Add a PropertyChangeListener. Throws a PropertyChangeEvent:
   * <ul>
   *   <li>propertyName = "Station", getNewValue() = Station
   *   <li>propertyName =  "GeoRegion", getNewValue() = ProjectionRect
   * </ul>
   */
  public void addPropertyChangeListener( PropertyChangeListener l) {
    listenerList.add(PropertyChangeListener.class, l);
  }

  /**
   * Remove a PropertyChangeEvent Listener.
   */
  public void removePropertyChangeListener( PropertyChangeListener l) {
    listenerList.remove(PropertyChangeListener.class, l);
  }

  /**
   * Add an action to the toolbar.
   * @param act add this action
   */
  public void addToolbarAction(AbstractAction act) {
    BAMutil.addActionToContainer( toolPanel, act);
  }

  // Notify all listeners that have registered interest for
  // notification on this event type.  The event instance
  // is lazily created using the parameters passed into
  // the fire method.
  // see addPropertyChangeListener for list of valid arguments
  private void firePropertyChangeEvent(Object newValue, String propertyName) {
    PropertyChangeEvent event = null;
    Object[] listeners = listenerList.getListenerList();
    // Process the listeners last to first
    for (int i = listeners.length-2; i>=0; i-=2) {
      if (listeners[i] == PropertyChangeListener.class) {
        if (event == null) // Lazily create the event:
          event = new PropertyChangeEvent(this, propertyName, null, newValue);
        // send event
        ((PropertyChangeListener)listeners[i+1]).propertyChange(event);
      }
    }
  }

  public void addActionValueListener( ActionValueListener l) { actionSource.addActionValueListener(l); }
  public void removeActionValueListener( ActionValueListener l) { actionSource.removeActionValueListener(l); }

    // better way to do event management
  public ActionSourceListener getActionSourceListener() { return actionSource; }

  public void setMapArea(ProjectionRect ma) {
    np.getProjectionImpl().setDefaultMapArea( ma);
    //np.setMapArea(ma);
  }

  /**
   * Set the list of Stations.
   * @param stns list of Station
   */
  public void setStations(java.util.List stns) {
    stnRender.setStations( stns);
    redraw(true);
  }

  /**
   * Looks for the station with givemn id. If found, makes it current. Redraws.
   * @param id must match stationIF.getID().
   */
  public void setSelectedStation( String id) {
    stnRender.setSelectedStation( id);
    selectedStation = stnRender.getSelectedStation();
    np.setLatLonCenterMapArea( selectedStation.getLatitude(), selectedStation.getLongitude());
    redraw();
  }

  /**
   * Get currently selected station, or null if none.
   * @return selected station
   */
  public ucar.unidata.geoloc.Station getSelectedStation( ) { return selectedStation; }


  /**
   * Access to the navigated panel.
   * @return  navigated panel object
   */
  public NavigatedPanel getNavigatedPanel() { return np; }

  /**
   *  Change the state of decluttering
   * @param declut if true, declutter
   */
  public void setDeclutter(boolean declut) {
    stnRender.setDeclutter(declut);
    redraw();
  }

  /**
   * Get the state of the declutter flag.
   * @return the state of the declutter flag.
   */
  public boolean getDeclutter() { return stnRender.getDeclutter(); }


  /**
   *  Redraw the graphics on the screen.
   */
  protected void redraw() {
    long tstart = System.currentTimeMillis();

    java.awt.Graphics2D gNP = np.getBufferedImageGraphics();
    if (gNP == null) // panel not drawn on screen yet
        return;

      // clear it
    gNP.setBackground(np.getBackgroundColor());
    java.awt.Rectangle r = gNP.getClipBounds();
    gNP.clearRect(r.x, r.y, r.width, r.height);

    if (regionSelect && geoSelectionMode) {
      if (geoSelection != null) drawBB( gNP, geoSelection, Color.cyan);
      if (geoBounds != null) drawBB( gNP, geoBounds, null);
      // System.out.println("GeoRegionChooser.redraw geoBounds= "+geoBounds);

      if (geoSelection != null) {
        // gNP.setColor( Color.orange);
        Navigation navigate = np.getNavigation();
        double handleSize = RubberbandRectangleHandles.handleSizePixels / navigate.getPixPerWorld();
        RubberbandRectangleHandles.drawHandledRect( gNP, geoSelection, handleSize);
        if (debug) System.out.println("GeoRegionChooser.drawHandledRect="+handleSize+" = "+geoSelection);
      }
    }

    for (int i = 0; i < renderers.size(); i++) {
      Renderer rend = (Renderer) renderers.get(i);
      rend.draw(gNP, atI);
    }
    gNP.dispose();

    if (debug) {
      long tend = System.currentTimeMillis();
      System.out.println("StationRegionDateChooser draw time = "+ (tend - tstart)/1000.0+ " secs");
    }

    // copy buffer to the screen
    np.repaint();
  }

   private void drawBB(java.awt.Graphics2D g, ProjectionRect bb, Color fillColor) {
    if ( null != fillColor) {
      g.setColor(fillColor);
      g.fill(bb);
    }
    g.setColor(outlineColor);
    g.draw(bb);
  }
  private boolean debug = false;

  public void setGeoBounds( LatLonRect llbb) {
    np.setMapArea( llbb);
    geoBounds = np.getProjectionImpl().latLonToProjBB( llbb);
    np.getProjectionImpl().setDefaultMapArea( geoBounds);
    setGeoSelection( geoBounds);
  }
  public void setGeoBounds( ProjectionRect bb) {
    geoBounds = new ProjectionRect( bb);
    np.setMapArea( bb);
    np.getProjectionImpl().setDefaultMapArea( geoBounds);
  }
  public void setGeoSelection( LatLonRect llbb) {
    np.setGeoSelection( llbb);
    setGeoSelection( np.getGeoSelection());
  }
  public void setGeoSelection( ProjectionRect bb) {
    geoSelection = bb;
    if (minLonField != null) {
      minLonField.setDouble(geoSelection.getMinX());
      minLatField.setDouble(geoSelection.getMinY());
      maxLonField.setDouble(geoSelection.getMaxX());
      maxLatField.setDouble(geoSelection.getMaxY());
    }
    np.setGeoSelection( geoSelection);
  }

  public LatLonRect getGeoSelectionLL() { return np.getGeoSelectionLL(); }
  public ProjectionRect getGeoSelection() { return np.getGeoSelection(); }
  public boolean getGeoSelectionMode() { return geoSelectionMode; }

  public DateRange getDateRange() {
    if (!dateSelect || !dateWindow.isShowing() || !dateSelector.isEnabled())
      return null;
    return dateSelector.getDateRange();
  }
                                                                                                          
  public void setDateRange( DateRange range) {
    dateSelector.setDateRange( range);
  }


  /** Wrap this in a JDialog component.
   *
   * @param parent      JFrame (application) or JApplet (applet) or null
   * @param title       dialog window title
   * @param modal     is modal
   * @return the JDialog widget
   */
  public JDialog makeDialog( RootPaneContainer parent, String title, boolean modal) {
    return new Dialog( parent, title, modal);
  }

  private class Dialog extends JDialog {

    private Dialog(RootPaneContainer parent, String title, boolean modal) {
      super(parent instanceof Frame ? (Frame) parent : null, title, modal);

      // L&F may change
      UIManager.addPropertyChangeListener( new PropertyChangeListener() {
        public void propertyChange( PropertyChangeEvent e) {
          if (e.getPropertyName().equals("lookAndFeel"))
            SwingUtilities.updateComponentTreeUI( StationRegionDateChooser.Dialog.this);
        }
      });

      // add a dismiss button
      JPanel buttPanel = new JPanel();
      JButton dismissButton = new JButton("Dismiss");
      dismissButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          setVisible(false);
        }
      });
      buttPanel.add(dismissButton, null);

     // add it to contentPane
      Container cp = getContentPane();
      cp.setLayout(new BorderLayout());
      cp.add( StationRegionDateChooser.this, BorderLayout.CENTER);
      cp.add( buttPanel, BorderLayout.SOUTH);
      pack();
    }
  }

  public static void main(String[] args) {
    StationRegionDateChooser slm = new StationRegionDateChooser();
    slm.setBounds( new Rectangle( 10, 10, 400, 200));

    JFrame frame = new JFrame("StationRegionChooser Test");
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        System.exit(0);
      }
    });
    frame.getContentPane().add(slm);
    frame.pack();
    frame.setVisible(true);
  }
}
