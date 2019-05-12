/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.grid;

import ucar.ui.event.ActionSourceListener;
import ucar.nc2.dataset.*;

import ucar.ui.event.ActionValueEvent;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Slider for Vertical scale
 *
 * @author caron
 */

public class VertScaleSlider extends JPanel {
  private JSlider slider;

    // event management
  private ActionSourceListener actionSource;
  private String actionName = "level";
  private boolean eventOK = true;
  private int incrY = 1;

    // state
  private int currentIdx = -1;
  private double min, max, scale = 1.0;
  private CoordinateAxis1D zAxis;

  private static boolean debugEvent = false, debugLevels = false;

  public VertScaleSlider() {

    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    slider = new JSlider(JSlider.VERTICAL, 0, 100, 0);
    //slider.setPaintTrack(false);
    //s.setMajorTickSpacing(20);
    //s.setMinorTickSpacing(5);
    slider.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));

      // listen for changes from user manupulation
    slider.addChangeListener(e -> {
        if (eventOK && (zAxis != null) && !slider.getValueIsAdjusting()) {
          int pos = slider.getValue();
          int idx = slider2index(pos);
          if (idx == currentIdx) return;
          currentIdx = idx;

              // gotta do this after the dust settles
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              setSelectedIndex( currentIdx);
              actionSource.fireActionValueEvent(actionName, zAxis.getCoordName(currentIdx));
            } // run
          }); // invokeLater

        } // eventPOk
    }); //add ChangeListener

      // listen for outside changes
    actionSource = new ActionSourceListener(actionName) {
      public void actionPerformed( ActionValueEvent e) {
        if (debugEvent) System.out.println(" actionSource event "+e);
        setSelectedByName( e.getValue().toString());
      }
    };

        // catch resize events on the slider
    slider.addComponentListener( new ComponentAdapter() {
      public void componentResized( ComponentEvent e) {
        setLabels();
      }
    });

    //this.add(Box.createRigidArea(10));
    this.add(slider);
    //this.add(Box.createRigidArea(10));
  }

      /** better way to do event management */
  public ActionSourceListener getActionSourceListener() { return actionSource; }

  public void setLevels( ucar.nc2.dt.GridCoordSystem gcs, int current) {
    this.zAxis = gcs.getVerticalAxis();
    if (zAxis == null) {
      slider.setEnabled( false);
      return;
    }
      // set up the slider and conversion
    slider.setEnabled( true);
    slider.setInverted( !gcs.isZPositive());
    slider.setToolTipText( zAxis.getUnitsString() );
    setSelectedIndex(current);

    int n = (int) zAxis.getSize();
    if (zAxis.isContiguous()) {
      min = Math.min(zAxis.getCoordEdge(0), zAxis.getCoordEdge(n));
      max = Math.max(zAxis.getCoordEdge(0), zAxis.getCoordEdge(n));
    } else {
      min = zAxis.getMinValue();
      max = zAxis.getMaxValue();     
    }

    setLabels();
    slider.setPaintLabels( true );
  }

  private void setLabels() {
    Rectangle bounds = slider.getBounds();
    if (debugEvent) System.out.println(" setLevels Bounds= "+bounds);

    double h = (bounds.getHeight() > 0) ? bounds.getHeight() : 100.0;
    double wh = (max - min) > 0.0 ? (max - min) : 1.0;
    scale = 100.0 / wh;
    double slider2pixel = h/100.0;

    Font font = slider.getFont();
    FontMetrics fontMetrics = Toolkit.getDefaultToolkit().getFontMetrics( font);
    if (fontMetrics != null)
      incrY = fontMetrics.getAscent(); // + fontMetrics.getDescent();
    if (debugEvent) System.out.println(" scale= "+scale+ " incrY = "+incrY);

    java.util.Hashtable labelTable = new java.util.Hashtable();

    if (zAxis == null) return;
    int n = (int) zAxis.getSize();
    int last = world2slider(zAxis.getCoordValue(n-1));  // always
    labelTable.put(last, new JLabel(zAxis.getCoordName(n-1)) ); // always
    int next = world2slider(zAxis.getCoordValue(0));
    labelTable.put(next, new JLabel(zAxis.getCoordName(0)) ); // always
    if (debugLevels) System.out.println("level = "+slider2pixel*next+" "+ zAxis.getCoordName(0)+" added ");

    for (int i=1; i< n-1; i++) {
      int ival = world2slider(zAxis.getCoordValue(i));
      if (debugLevels) System.out.println("level = "+slider2pixel*ival+" "+ zAxis.getCoordName(i));
      if ((slider2pixel*Math.abs(ival-last) > incrY) &&
          (slider2pixel*Math.abs(ival-next) > incrY)) {
        labelTable.put(ival, new JLabel(zAxis.getCoordName(i)) );
        next = ival;
        if (debugLevels) System.out.println("  added ");
      }
    }
    if (debugLevels) System.out.println("level = "+slider2pixel*last+" "+ zAxis.getCoordName(n-1)+" added ");

    slider.setLabelTable( labelTable );
  }

  private int world2slider( double val) {
    return ((int) (scale * (val - min)));
  }

  private double slider2world( int pval) {
    return pval/scale + min;
  }

  private int pos = -1; // optimization
  private int slider2index( int pval) {
    pos = zAxis.findCoordElement(slider2world(pval), pos);
    return pos;
  }

  private void setSelectedByName( String name) {
    if (zAxis == null) return;
    for (int i=0; i< zAxis.getSize(); i++)
      if (name.equals(zAxis.getCoordName(i))) {
        setSelectedIndex(i);
        return;
      }
    System.out.println("ERROR VertScaleSlider cant find = "+name);
  }

      // set current value - no event
  private void setSelectedIndex( int idx) {
    if (zAxis == null)
      return;
    eventOK = false;
    currentIdx = idx;
    slider.setValue( world2slider(zAxis.getCoordValue(currentIdx)));
    eventOK = true;
  }

}
