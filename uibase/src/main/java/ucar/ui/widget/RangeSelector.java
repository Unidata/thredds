/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.widget;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import ucar.ui.event.ActionSourceListener;
import ucar.ui.event.ActionValueEvent;
import ucar.ui.event.ActionValueListener;
import ucar.ui.prefs.Field;
import ucar.ui.prefs.PrefPanel;

/**
 * Widget to select a point or a range from a double range.
 */
public class RangeSelector extends JPanel {
  private static final int SLIDER_RESOLUTION = 1000;

  private String tit, helpMessage;

  private double minLimit, maxLimit, resolution;
  private double minSelect, maxSelect;
  private int nfracDig;

  private JSlider minSlider, maxSlider;
  private PrefPanel pp;
  private Field.Double minField, maxField;
  private Scale scale;
  private HelpWindow helpWindow;
  private JButton helpButton;

    // event management
  private ActionSourceListener actionSource;
  private String actionName = "rangeSelection";
  private boolean eventOK = true;
  private int incrY = 1;

    // state
  private int currentIdx = -1;

  private static boolean debugEvent = false, debugSliderSize = false;

  public RangeSelector(String title, String min, String max, String resolutionS, String units,
      boolean acceptButton, String help, boolean pointOnly) {

    this.tit = title;
    this.minLimit = Double.parseDouble(min);
    this.maxLimit = Double.parseDouble(max);
    this.helpMessage = help;

    // figure out num fractions based on strings
    nfracDig = Math.max( calcFracDigits(min), calcFracDigits(max) );

    if (resolutionS != null) {
      this.resolution = Double.parseDouble(resolutionS);
      nfracDig = Math.max( nfracDig, calcFracDigits(resolutionS) );
    }

    this.minSelect = minLimit;
    this.maxSelect = maxLimit;
    this.scale = new Scale( minLimit, maxLimit, resolution);

    // UI
    JPanel sliderPanel = new JPanel();

     // optional top panel
    JPanel topPanel = null;
    if ((title != null) || (help != null)) {
      topPanel = new JPanel(new BorderLayout());
      JPanel butts = new JPanel();

      if (title != null)
        topPanel.add(BorderLayout.WEST, new JLabel("  "+title+":"));

      if (help != null) {
        helpButton = new JButton("help");
        helpButton.addActionListener(e -> {
            if (helpWindow == null)
              helpWindow = new HelpWindow(null, "Help on "+tit, helpMessage);
            helpWindow.show(helpButton);
        });
        butts.add(helpButton);
      }

      if (acceptButton) {
        JButton okButton = new JButton("accept");
        okButton.addActionListener(e -> {
            pp.accept();
            sendEvent();
        });

        butts.add(okButton);
        acceptButton = false; // dont need it in prefpanel
      }

      topPanel.add(BorderLayout.EAST, butts);
    }

    // the sliders
    sliderPanel.setLayout( new BoxLayout(sliderPanel, BoxLayout.Y_AXIS));
    sliderPanel.setBorder(new LineBorder(Color.black, 1, true));

    minSlider = new JSlider(JSlider.HORIZONTAL, 0, SLIDER_RESOLUTION, 0);
    maxSlider = new JSlider(JSlider.HORIZONTAL, 0, SLIDER_RESOLUTION, SLIDER_RESOLUTION);

    //minSlider.setPaintLabels( true);
    //int minorTicks = SLIDER_RESOLUTION / 32;
    //minSlider.setMajorTickSpacing(minorTicks * 4);
    //minSlider.setMinorTickSpacing(minorTicks);
    //minSlider.setPaintTicks( true);

    Border b = BorderFactory.createEmptyBorder(0,15,0,15);
    minSlider.setBorder(b);
    maxSlider.setBorder(b);

    JPanel labelPanel = new JPanel( new BorderLayout());
    labelPanel.add( new JLabel("    "+scale.getMinLabel()), BorderLayout.WEST);
    labelPanel.add( new JLabel(scale.getMaxLabel()+"    "), BorderLayout.EAST);

    // the fields use a PrefPanel
    pp = new PrefPanel( null, null);
    int col = 0;
    //if (tit != null) {
    //  pp.addComponent(new JLabel(tit), col, 0, null);
    //  col+=2;
    //}
    if (pointOnly) {
      minField = pp.addDoubleField("min", "value", minSelect, nfracDig, col, 0, null);
      col += 2;
    } else {
      minField = pp.addDoubleField("min", "min", minSelect, nfracDig, col, 0, null);
      col += 2;
      maxField = pp.addDoubleField("max", "max", maxSelect, nfracDig, col, 0, null);
      col += 2;
    }
    pp.addComponent(new JLabel(units), col, 0, null);
    pp.finish(acceptButton, BorderLayout.EAST);

    // overall layout
    if (topPanel != null)
     sliderPanel.add( topPanel);
    sliderPanel.add( pp);
    if (!pointOnly) sliderPanel.add( maxSlider);
    sliderPanel.add( minSlider);
    sliderPanel.add( labelPanel);

    setLayout(new BorderLayout()); // allow width expansion
    add( sliderPanel, BorderLayout.NORTH);

    /// event management

    // listen for changes from user manupulation
    maxSlider.addChangeListener(e -> {
        if (!eventOK) { return; }

        int pos = maxSlider.getValue();
        double val = scale.slider2world(pos);
        maxSelect = val;

        // change value of the field
        eventOK = false;
        maxField.setDouble( maxSelect);
        eventOK = true;

        if (val < minSelect) minSlider.setValue(pos); // drag min along
    });

    minSlider.addChangeListener(e -> {
        if (!eventOK) { return; }

        int pos = minSlider.getValue();
        double val = scale.slider2world(pos);
        minSelect = val;

        // change value of the field
        eventOK = false;
        minField.setDouble( minSelect);
        eventOK = true;

        if ((val > maxSelect) && (maxSlider != null))
         maxSlider.setValue(pos); // drag max along
    });

    minField.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange( PropertyChangeEvent e) {
        if (debugEvent) System.out.println("minField event= "+e.getNewValue()+" "+e.getNewValue().getClass().getName());
        if (!eventOK) return;

        double val = minField.getDouble();
        if ((val >= minLimit) && (val <= maxLimit)) {
          eventOK = false;
          minSlider.setValue( scale.world2slider( val));
          eventOK = true;
        } else
          minField.setDouble( minSelect);
      }
    });

    if (maxField != null) {
      maxField.addPropertyChangeListener(new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent e) {
          if (debugEvent) System.out.println("maxField event= "+e.getNewValue());
          if (!eventOK) return;

          double val = maxField.getDouble();
          if ((val >= minLimit) && (val <= maxLimit)) {
            eventOK = false;
            maxSlider.setValue( scale.world2slider( val));
            eventOK = true;
          } else
            maxField.setDouble( maxSelect);
        }
      });
    }

      // listen for outside changes
    actionSource = new ActionSourceListener(actionName) {
      public void actionPerformed( ActionValueEvent e) {
        if (debugEvent) System.out.println(" actionSource event "+e);
        //?? setSelectedByName( e.getValue().toString());
      }
    };

        // catch resize events on the slider
    /* minSlider.addComponentListener( new ComponentAdapter() {
      public void componentResized( ComponentEvent e) {
        setLabels();
      }
    }); */
  }

  public double getMinSelected() { return minSelect; }
  public double getMaxSelected() { return maxSelect; }

  public String getMinSelectedString() { return format( minSelect); }
  public String getMaxSelectedString() { return format( maxSelect); }

  public void sendEvent() {
        // gotta do this after the dust settles
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        System.out.println("event min= "+minSelect+" max = "+maxSelect);
        actionSource.fireActionValueEvent(actionName, this);
      } // run
    }); // invokeLater */
  }

    /** add ActionValueListener listener */
  public void addActionValueListener( ActionValueListener l) { actionSource.addActionValueListener(l); }

    /** remove ActionValueListener listener */
  public void removeActionValueListener( ActionValueListener l) { actionSource.removeActionValueListener(l); }

  /* private void setLabels() {
    if (debugSliderSize) {
      Rectangle bounds = minSlider.getBounds();
      System.out.println(" minSlider Bounds= " + bounds);
      System.out.println(" minSlider Bounds= " + maxSlider.getBounds());
      maxSlider.setBounds(bounds);
      maxSlider.revalidate();
      System.out.println(" maxSlider Bounds= " + maxSlider.getBounds());

      double h = (bounds.getHeight() > 0) ? bounds.getHeight() : 100.0;
    //double wh = (max - min) > 0.0 ? (max - min) : 1.0;
    //scale = 100.0 / wh;
    //double slider2pixel = h/100.0;

      Font font = minSlider.getFont();
      FontMetrics fontMetrics = Toolkit.getDefaultToolkit().getFontMetrics(font);
      if (fontMetrics != null)
        incrY = fontMetrics.getAscent(); // + fontMetrics.getDescent();
      if (debugEvent)
        System.out.println(" scale= " + scale + " incrY = " + incrY);
    }

    java.util.Hashtable labelTable = new java.util.Hashtable();

    labelTable.put( new Integer( 0), new JLabel(scale.getMinLabel()) );
    labelTable.put( new Integer( SLIDER_RESOLUTION), new JLabel(scale.getMaxLabel()) );
    minSlider.setLabelTable( labelTable );
    maxSlider.setLabelTable( labelTable );
  } */

  /*public void setLevels( ucar.nc2.dataset.grid.GridCoordSys gcs, int current) {
    this.zAxis = gcs.getVerticalAxis();
    if (zAxis == null) {
      slider.setEnabled( false);
      return;
    }
      // set up the slider and conversion
    slider.setEnabled( true);
    slider.setInverted( !gcs.isZPositive());
    slider.setToolTipText( zAxis.getUnitString() );
    setSelectedIndex(current);

    int n = (int) zAxis.getSize();
    min = Math.min(zAxis.getCoordEdge(0), zAxis.getCoordEdge(n));
    max = Math.max(zAxis.getCoordEdge(0), zAxis.getCoordEdge(n));

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
  } */

  private int calcFracDigits( String d) {
    int pos = d.indexOf(".");
    if (pos < 0) return 0;
    return d.length() - pos - 1;
  }

  private String format(double val) {
    return String.format("%." + nfracDig + "f", val);
  }

  private class Scale {
    private double min, max, resolution, scale;

    Scale( double min, double max, double resolution) {
      this.min = min;
      this.max= max;
      scale = SLIDER_RESOLUTION / (max - min);
      this.resolution = (resolution > 0) ? resolution : 1.0 / scale;
    }

    private int world2slider(double val) {
      return ( (int) (scale * (val - min)));
    }

    private double slider2world(int pval) {
      // we want this to reflect the resolution
      double val = pval / scale;
      double floor = Math.floor(val / resolution);
      double incr = floor * resolution;
      return incr + min;
    }

    private String getMinLabel() { return format(min); }
    private String getMaxLabel() { return format(max); }

  }

  public static void main(String[] args) {

    JFrame frame = new JFrame("Test Range Selector");
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {System.exit(0);}
    });

    RangeSelector rs1 = new RangeSelector(null, ".07", ".09", ".0025", "dongles", true, null, false);
    RangeSelector rs2 = new RangeSelector("Time:", "100.0", "1000.0", null, "megaPancakes", true,
      "I need somebody, help, not just anybody HELP!", false);
    RangeSelector rs3 = new RangeSelector("Longitude:", "0", "1000", "10", "megaPancakes", false,
      null, false);
    RangeSelector rs4 = new RangeSelector("Flatulence:", "200", "1000", "17", "megaFauna", false,
      null, true);

    JPanel main = new JPanel( new FlowLayout());

    frame.getContentPane().add(main);
    //main.setPreferredSize(new Dimension(400, 200));
    //main.add( new JSlider(), BorderLayout.NORTH);
    main.add( rs1, BorderLayout.NORTH);
    main.add( rs2, BorderLayout.CENTER);
    main.add( rs3, BorderLayout.SOUTH);
    main.add( rs4, BorderLayout.SOUTH);
    //main.add( ri);

    frame.pack();
    frame.setLocation(400, 300);
    frame.setVisible(true);
  }

}
