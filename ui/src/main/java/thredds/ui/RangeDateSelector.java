// $Id: RangeDateSelector.java 50 2006-07-12 16:30:06Z caron $
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

package thredds.ui;

import thredds.datatype.prefs.*;
import thredds.viewer.ui.event.*;

import ucar.util.prefs.ui.*;
import ucar.nc2.units.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.beans.*;
import java.util.Date;

/**
 * Widget to select a point or a range from a date range.
 */

public class RangeDateSelector extends JPanel implements FieldValidator {
  public static final String TIME_START = "start";
  public static final String TIME_END = "end";
  public static final String TIME_DURATION = "duration";
  public static final String TIME_RESOLUTION = "resolution";

  static private final int SLIDER_RESOLUTION = 1000;

  private String title, helpMessage;
  private boolean acceptButton, enableButton, isPointOnly, useLimits;

  private DateType minLimit, maxLimit; // min and max allowed values
  private DateRange dateRange; // data model
  private Scale scale;

  // various widgets to manipulate the model
  private JSlider minSlider, maxSlider;
  private DateField minField, maxField;
  private DurationField durationField, resolutionField;

  // ui helpers
  private PrefPanel pp;
  private JLabel minLabel, maxLabel;
  private JButton helpButton;
  private JToggleButton disableButton;
  private HelpWindow helpWindow;

    // event management
  private ActionSourceListener actionSource;
  private String actionName = "rangeDateSelection";
  private boolean eventOK = true;

  private static boolean debugEvent = false, debugEvent2 = false;

  /**
   * Constructor using info from thredds DQC.
   *
   * @param title widget title displayed to user, may be null
   * @param start starting date as a string
   * @param end ending date as a string
   * @param durationS duration as a String
   * @param minInterval minimum useful interval as a String, may be null.
   * @param acceptButton add an acceptButton
   * @param help optional help text
   * @param pointOnly if user can only select one point, otherwise can select a range of dates.
   * @throws java.text.ParseException
   */
  public RangeDateSelector(String title, String start, String end, String durationS, String minInterval,
                           boolean enableButton, boolean acceptButton, String help, boolean pointOnly) throws Exception {

    this( title,
          new DateRange( (start == null) ? null : new DateType(start, null, null),
                         (end == null) ? null : new DateType(end, null, null),
                         (durationS == null) ? null : new TimeDuration(durationS),
                         (minInterval == null) ? null : new TimeDuration(minInterval)),
          enableButton, acceptButton, help, pointOnly, true);
  }

  /**
   * Constructor.
   * @param title widget title displayed to user, may be null
   * @param range range that the user can select from
   * @param acceptButton add an accept Button
   * @param enableButton add an enable Button
   * @param help optional help text
   * @param pointOnly if user can only select one point, otherwise can select a range of dates.
   */
  public RangeDateSelector(String title, DateRange range,  boolean enableButton, boolean acceptButton, String help,
                           boolean pointOnly, boolean useLimits) {
    this.title = title;
    this.dateRange = range;
    this.acceptButton = acceptButton;
    this.enableButton = enableButton;
    this.helpMessage = help;
    this.isPointOnly = pointOnly;
    this.useLimits = useLimits;

    init();
  }

  private void init() {

    // UI
    // optional top panel
    JPanel topPanel = null;
    topPanel = new JPanel(new BorderLayout());
    JPanel butts = new JPanel();

    if (title != null)
      topPanel.add(BorderLayout.WEST, new JLabel("  " + title + ":"));

    if (helpMessage != null) {
      helpButton = new JButton("help");
      helpButton.addActionListener(new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          if (helpWindow == null)
            helpWindow = new HelpWindow(null, "Help on " + title, helpMessage);
          helpWindow.show(helpButton);
        }
      });
      butts.add(helpButton);
    }

    if (acceptButton) {
      JButton okButton = new JButton("accept");
      okButton.addActionListener(new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          pp.accept();
          sendEvent();
        }
      });

      butts.add(okButton);
      acceptButton = false; // dont need it in prefpanel
    }

    if (enableButton) {
      disableButton = new JToggleButton("disable", false);
      disableButton.addActionListener(new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          boolean b = !disableButton.getModel().isSelected();
          minField.setEnabled(b);
          maxField.setEnabled(b);
          durationField.setEnabled(b);
          minSlider.setEnabled(b);
          maxSlider.setEnabled(b);
        }
      });

      butts.add(disableButton);
    }

    topPanel.add(BorderLayout.EAST, butts);

    // the sliders
    JPanel sliderPanel = new JPanel();
    sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.Y_AXIS));
    sliderPanel.setBorder(new LineBorder(Color.black, 1, true));

    minSlider = new JSlider(JSlider.HORIZONTAL, 0, SLIDER_RESOLUTION, 0);
    maxSlider = new JSlider(JSlider.HORIZONTAL, 0, SLIDER_RESOLUTION, SLIDER_RESOLUTION);

    Border b = BorderFactory.createEmptyBorder(0, 15, 0, 15);
    minSlider.setBorder(b);
    maxSlider.setBorder(b);

    // set this so we can call setDateRange();
    minLabel = new JLabel();
    maxLabel = new JLabel();
    minField = new DateField(TIME_START, isPointOnly ? "value" : "start", dateRange.getStart(), null);
    maxField = new DateField(TIME_END, "end", dateRange.getEnd(), null);
    durationField = new DurationField(TIME_DURATION, "duration", dateRange.getDuration(), null);
    resolutionField = new DurationField(TIME_RESOLUTION, "resolution", dateRange.getResolution(), null);

    minField.addValidator(this);
    maxField.addValidator(this);
    durationField.addValidator(this);
    setDateRange(dateRange);

    JPanel labelPanel = new JPanel(new BorderLayout());
    labelPanel.add(minLabel, BorderLayout.WEST);
    labelPanel.add(maxLabel, BorderLayout.EAST);

    // the fields use a PrefPanel
    pp = new PrefPanel(null, null);
    int row = 0;
    //if (tit != null) {
    //  pp.addComponent(new JLabel(tit), col, 0, null);
    //  col+=2;
    //}
    if (isPointOnly) {
      pp.addField(minField, 0, row++, null);
    } else {
      pp.addField(minField, 0, row++, null);
      pp.addField(maxField, 0, row++, null);
      pp.addField(durationField, 0, row++, null);
      pp.addField(resolutionField, 0, row++, null);
    }
    pp.finish(acceptButton, BorderLayout.EAST);

    setLayout(new BorderLayout()); // allow width expansion

    // overall layout
    if (topPanel != null)
      sliderPanel.add(topPanel);
    sliderPanel.add(pp);

    if (useLimits) {
      if (!isPointOnly) sliderPanel.add(maxSlider);
      sliderPanel.add(minSlider);
      sliderPanel.add(labelPanel);
    }

    add(sliderPanel, BorderLayout.NORTH);

    /// event management

    // listen for changes from user manupulation
    maxSlider.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        if (debugEvent2) System.out.println("maxSlider event= " + maxSlider.getValue());
        if (!eventOK) return;

        int pos = maxSlider.getValue();
        dateRange.setEnd(scale.slider2world(pos));
        synchUI(false);

        if (dateRange.isPoint())
          minSlider.setValue(pos); // drag min along */
      }
    });

    minSlider.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        if (debugEvent2) System.out.println("minSlider event= " + minSlider.getValue());
        if (!eventOK) return;

        int pos = minSlider.getValue();
        dateRange.setStart(scale.slider2world(pos));
        synchUI(false);

        if (dateRange.isPoint() && !isPointOnly)
          maxSlider.setValue(pos); // drag max along
      }
    });

    minField.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent e) {
        if (debugEvent) System.out.println("minField event= " + e.getNewValue() + " " + e.getNewValue().getClass().getName());
        if (!eventOK) return;

        DateType val = (DateType) minField.getValue();
        dateRange.setStart(val);
        synchUI(true);
      }
    });

    if (maxField != null) {
      maxField.addPropertyChangeListener(new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent e) {
          if (debugEvent) System.out.println("maxField event= " + e.getNewValue());
          if (!eventOK) return;

          DateType val = (DateType) maxField.getValue();
          dateRange.setEnd(val);
          synchUI(true);
        }
      });
    }

    if (durationField != null) {
      durationField.addPropertyChangeListener(new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent e) {
          if (debugEvent) System.out.println("durationField event= " + e.getNewValue());
          if (!eventOK) return;

          TimeDuration val = durationField.getTimeDuration();
          dateRange.setDuration( val);
          synchUI(true);
        }
      });
    }

    if (resolutionField != null) {
      resolutionField.addPropertyChangeListener(new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent e) {
          if (debugEvent) System.out.println("resolutionField event= " + e.getNewValue());
          if (!eventOK) return;

          TimeDuration val = resolutionField.getTimeDuration();
          dateRange.setResolution( val);
          //synchUI(true);
        }
      });
    }

    // listen for outside changes
    actionSource = new ActionSourceListener(actionName) {
      public void actionPerformed(ActionValueEvent e) {
        if (debugEvent) System.out.println(" actionSource event " + e);
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

  private DateFormatter formatter = new DateFormatter();

  public boolean validate( Field fld, Object editValue, StringBuffer errMessages) {
    if (!useLimits) return true;

    DateType checkVal = null;

    if (fld == durationField) {
      TimeDuration duration = (TimeDuration) editValue;
      if (dateRange.getEnd().isPresent())
        checkVal = dateRange.getEnd().subtract( duration);
      else
        checkVal = dateRange.getStart().add( duration);
    } else
      checkVal =  (DateType) editValue; // otherwise its one of the dates

    // have to be inside the limits
    Date d = checkVal.getDate();
    if (d.after(maxLimit.getDate()) || d.before(minLimit.getDate())) {
      errMessages.append( "Date ");
      errMessages.append(  formatter.toDateTimeString(d));
      errMessages.append(" must be between ");
      errMessages.append( minLimit.getText());
      errMessages.append( " and ");
      errMessages.append( maxLimit.getText());
      return false;
    }

    return true;
  }

  // set values on the UI
  private void synchUI(boolean slidersOK) {
    eventOK = false;
    if (slidersOK) minSlider.setValue(scale.world2slider(dateRange.getStart()));
    minField.setValue(dateRange.getStart());

    if (maxField != null) {
      if (slidersOK) maxSlider.setValue(scale.world2slider(dateRange.getEnd()));
      maxField.setValue(dateRange.getEnd());
    }

    if (durationField != null)
      durationField.setValue(dateRange.getDuration());

    eventOK = true;
  }

  public void setDateRange(DateRange dateRange) {
    this.dateRange = dateRange;

    this.minLimit = new DateType( dateRange.getStart());
    this.maxLimit = new DateType( dateRange.getEnd());
    this.scale = new Scale( dateRange);

    minLabel.setText(" "+minLimit.getText()+" ");
    maxLabel.setText(" "+maxLimit.getText()+" ");

    if (isPointOnly) {
      minField.setValue( dateRange.getStart());
    } else {
      minField.setValue( dateRange.getStart());
      maxField.setValue( dateRange.getEnd());
      durationField.setValue( dateRange.getDuration());
      resolutionField.setValue( dateRange.getResolution());
    }
  }

  public DateField getMinDateField() { return minField; }
  public DateField getMaxDateField() { return maxField; }
  public DurationField getDurationField() { return durationField; }
  public DurationField getResolutionField() { return resolutionField; }

  public boolean isEnabled() {
    return (null == disableButton) || !disableButton.getModel().isSelected();
  }

  public DateRange getDateRange() {
    if (!pp.accept())
      return null;
    return dateRange;
  }

  public void sendEvent() {
        // gotta do this after the dust settles
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        System.out.println("event range= "+dateRange);
        actionSource.fireActionValueEvent(actionName, this);
      } // run
    }); // invokeLater */
  }

    /** add ActionValueListener listener */
  public void addActionValueListener( ActionValueListener l) { actionSource.addActionValueListener(l); }

    /** remove ActionValueListener listener */
  public void removeActionValueListener( ActionValueListener l) { actionSource.removeActionValueListener(l); }

  private class Scale {
    private double min; // secs
    private double scale;  // pixels / secs

    Scale( DateRange dateRange) {
      this.min = .001 * dateRange.getStart().getDate().getTime();
      //this.max = .001 * dateRange.getEnd().getDate().getTime();
      //scale = SLIDER_RESOLUTION / (this.max - this.min);

      scale = SLIDER_RESOLUTION / dateRange.getDuration().getValueInSeconds();
      // System.out.println("slider scale= "+scale);
    }

    private int world2slider(DateType val) {
      double msecs = .001 * val.getDate().getTime() - min;
      return (int) (scale * msecs);
    }

    private DateType slider2world(int pval) {
      double val = pval / scale; // secs
      //double floor = Math.floor(val / resolution);
      //double incr = floor * resolution;
      double msecs = 1000 * (min + val);
      return new DateType(false, new java.util.Date( (long) msecs));
    }
  }

  public static void main(String args[]) throws Exception {

    JFrame frame = new JFrame("Test Date Range Selector");
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {System.exit(0);}
    });


    RangeDateSelector rs1 = new RangeDateSelector("Date Range", "1990-01-01T01:00:00", "1990-01-02T02:00:00",
       null, "15 minute", true, true, "i think im fallin", false);
    RangeDateSelector rs2 = new RangeDateSelector("Date", "1990-01-01", "1991-01-01", null, "1 day", false, true,
       "i think im fallin\n in love with youuuu ", false);
    RangeDateSelector rs3 = new RangeDateSelector("Date", "1990-01-01", "1991-01-01", null, "10 days", true, true,
       null, false);
    RangeDateSelector rs4 = new RangeDateSelector("Date", "1990-01-01", "1991-01-01", null, "10 days", false, false,
       null, true);
    RangeDateSelector rs5 = new RangeDateSelector("Date", null, "present", "10 days", "1 day", true, false,
       null, false);


    // simulate what we do in PointObsViewer
    DateRange range = null;
    try {
      range = new DateRange(); // phony
    } catch (Exception e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
    RangeDateSelector rs6 = new RangeDateSelector(null, range, false, true, null, false, false);
    DateUnit start = new DateUnit("0 secs since 2005-05-02 23:00:00");
    DateUnit end = new DateUnit("0 secs since 2005-05-02 23:59:59");
    rs6.setDateRange( new DateRange( start.getDate(), end.getDate()));

    Box main = new Box( BoxLayout.Y_AXIS);

    frame.getContentPane().add(main);
    //main.setPreferredSize(new Dimension(400, 200));
    //main.add( new JSlider(), BorderLayout.NORTH);
    main.add( rs1);
    /*main.add( rs2);
    main.add( rs3);
    main.add( rs4);
    main.add( rs5); */
    main.add( rs6);

    frame.pack();
    frame.setLocation(400, 300);
    frame.setVisible(true);
  }

}