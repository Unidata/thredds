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
package ucar.nc2.ui.grid;

import ucar.nc2.dataset.*;

import thredds.viewer.ui.event.ActionSourceListener;
import thredds.viewer.ui.event.ActionValueEvent;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 * Slider for Vertical scale
 *
 * @author caron
 * @version $Revision: 50 $ $Date: 2006-07-12 16:30:06Z $
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
    slider.addChangeListener( new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
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
      } // stateChanged
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
    labelTable.put( new Integer( last), new JLabel(zAxis.getCoordName(n-1)) ); // always
    int next = world2slider(zAxis.getCoordValue(0));
    labelTable.put( new Integer( next), new JLabel(zAxis.getCoordName(0)) ); // always
    if (debugLevels) System.out.println("level = "+slider2pixel*next+" "+ zAxis.getCoordName(0)+" added ");

    for (int i=1; i< n-1; i++) {
      int ival = world2slider(zAxis.getCoordValue(i));
      if (debugLevels) System.out.println("level = "+slider2pixel*ival+" "+ zAxis.getCoordName(i));
      if ((slider2pixel*Math.abs(ival-last) > incrY) &&
          (slider2pixel*Math.abs(ival-next) > incrY)) {
        labelTable.put( new Integer( ival), new JLabel(zAxis.getCoordName(i)) );
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