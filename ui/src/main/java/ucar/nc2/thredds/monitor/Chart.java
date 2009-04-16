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

package ucar.nc2.thredds.monitor;

import java.util.*;
import java.text.*;
import java.io.*;

// Import the Swing classes
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

// Import the JFreeChart classes
import org.jfree.chart.*;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.data.*;
import org.jfree.data.general.*;
import org.jfree.data.time.*;
import org.jfree.ui.*;
import org.jfree.util.UnitType;

/**
 * Class Description.
 *
 * @author caron
 * @since Apr 9, 2009
 */
public class Chart extends JPanel {

  // Holds the data
  private TimeSeriesCollection dataset = new TimeSeriesCollection();
  private TimeSeriesCollection datasetOpenClose = new TimeSeriesCollection();
  private TimeSeriesCollection datasetHighLow = new TimeSeriesCollection();

  // Create a chart
  private JFreeChart chart;

  // Create a panels that can show our chart
  private ChartPanel panel;

  private String stockSymbol;

  public Chart(String title, String timeAxis, String valueAxis, TimeSeries data) {
    try {
      // Build the datasets
      dataset.addSeries(data);

      // Create the chart
      JFreeChart chart = ChartFactory.createTimeSeriesChart(
              title, timeAxis, valueAxis,
              dataset, true, true, false);

      // Setup the appearance of the chart
      chart.setBackgroundPaint(Color.white);
      XYPlot plot = chart.getXYPlot();
      plot.setBackgroundPaint(Color.lightGray);
      plot.setDomainGridlinePaint(Color.white);
      plot.setRangeGridlinePaint(Color.white);
      plot.setAxisOffset(new RectangleInsets(UnitType.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
      plot.setDomainCrosshairVisible(true);
      plot.setRangeCrosshairVisible(true);

      // Tell the chart how we would like dates to read
      DateAxis axis = (DateAxis) plot.getDomainAxis();
      axis.setDateFormatOverride(new SimpleDateFormat("EEE HH"));

      this.add(new ChartPanel(chart));
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }


  public Chart(String filename) {
    try {
      // Get Stock Symbol
      this.stockSymbol = filename.substring(0, filename.indexOf('.'));

      // Create time series
      TimeSeries open = new TimeSeries("Open Price", Day.class);
      TimeSeries close = new TimeSeries("Close Price", Day.class);
      TimeSeries high = new TimeSeries("High", Day.class);
      TimeSeries low = new TimeSeries("Low", Day.class);
      TimeSeries volume = new TimeSeries("Volume", Day.class);

      BufferedReader br = new BufferedReader(new FileReader(filename));
      String key = br.readLine();
      String line = br.readLine();
      while (line != null && !line.startsWith("<!--")) {
        StringTokenizer st = new StringTokenizer(line, ",", false);
        Day day = getDay(st.nextToken());
        double openValue = Double.parseDouble(st.nextToken());
        double highValue = Double.parseDouble(st.nextToken());
        double lowValue = Double.parseDouble(st.nextToken());
        double closeValue = Double.parseDouble(st.nextToken());
        long volumeValue = Long.parseLong(st.nextToken());

        // Add this value to our series'
        open.add(day, openValue);
        close.add(day, closeValue);
        high.add(day, highValue);
        low.add(day, lowValue);

        // Read the next day
        line = br.readLine();
      }

      // Build the datasets
      dataset.addSeries(open);
      dataset.addSeries(close);
      dataset.addSeries(low);
      dataset.addSeries(high);
      datasetOpenClose.addSeries(open);
      datasetOpenClose.addSeries(close);
      datasetHighLow.addSeries(high);
      datasetHighLow.addSeries(low);

      JFreeChart summaryChart = buildChart(dataset, "Summary", true);
      JFreeChart openCloseChart = buildChart(datasetOpenClose, "Open/Close Data", false);
      JFreeChart highLowChart = buildChart(datasetHighLow, "High/Low Data", true);
      JFreeChart highLowDifChart = buildDifferenceChart(datasetHighLow, "High/Low Difference Chart");

      // Create this panel
      this.setLayout(new GridLayout(2, 2));
      this.add(new ChartPanel(summaryChart));
      this.add(new ChartPanel(openCloseChart));
      this.add(new ChartPanel(highLowChart));
      this.add(new ChartPanel(highLowDifChart));
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private JFreeChart buildChart(TimeSeriesCollection dataset, String title, boolean endPoints) {
    // Create the chart
    JFreeChart chart = ChartFactory.createTimeSeriesChart(
            title,
            "Date", "Price",
            dataset,
            true,
            true,
            false
    );

    // Display each series in the chart with its point shape in the legend
    LegendTitle sl = chart.getLegend();
    //sl.setDisplaySeriesShapes(true);

    // Setup the appearance of the chart
    chart.setBackgroundPaint(Color.white);
    XYPlot plot = chart.getXYPlot();
    plot.setBackgroundPaint(Color.lightGray);
    plot.setDomainGridlinePaint(Color.white);
    plot.setRangeGridlinePaint(Color.white);
    plot.setAxisOffset(new RectangleInsets(UnitType.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
    plot.setDomainCrosshairVisible(true);
    plot.setRangeCrosshairVisible(true);

    // Display data points or just the lines?
    if (endPoints) {
      XYItemRenderer renderer = plot.getRenderer();
      if (renderer instanceof StandardXYItemRenderer) {
        StandardXYItemRenderer rr = (StandardXYItemRenderer) renderer;
        //rr.setPlotShapes(true);
        rr.setShapesFilled(true);
        rr.setItemLabelsVisible(true);
      }
    }

    // Tell the chart how we would like dates to read
    DateAxis axis = (DateAxis) plot.getDomainAxis();
    axis.setDateFormatOverride(new SimpleDateFormat("MMM-yyyy"));
    return chart;
  }

  private JFreeChart buildDifferenceChart(TimeSeriesCollection dataset, String title) {
    // Creat the chart
    JFreeChart chart = ChartFactory.createTimeSeriesChart(
            title,
            "Date", "Price",
            dataset,
            true, // legend
            true, // tool tips
            false // URLs
    );
    chart.setBackgroundPaint(Color.white);

    XYPlot plot = chart.getXYPlot();
    plot.setRenderer(new XYDifferenceRenderer(new Color(112, 128, 222), new Color(112, 128, 222), false));
    plot.setBackgroundPaint(Color.white);
    plot.setDomainGridlinePaint(Color.white);
    plot.setRangeGridlinePaint(Color.white);
    plot.setAxisOffset(new RectangleInsets(UnitType.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));

    ValueAxis domainAxis = new DateAxis("Date");
    domainAxis.setLowerMargin(0.0);
    domainAxis.setUpperMargin(0.0);
    plot.setDomainAxis(domainAxis);
    plot.setForegroundAlpha(0.5f);

    return chart;
  }

  protected Day getDay(String date) {
    try {
      String[] st = date.split("-");
      int year = Integer.parseInt(st[0]);
      int month = Integer.parseInt(st[1]);
      int day = Integer.parseInt(st[2]);

      // Build a new Day
      return new Day(day, month, year);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public String getSymbol() {
    return this.stockSymbol;
  }

  public static void main(String[] args) {
    Chart shc = new Chart("C:/TEMP/table.csv");

    JFrame frame = new JFrame("Stock History Chart for " + shc.getSymbol());
    frame.getContentPane().add(shc, BorderLayout.CENTER);
    frame.setSize(640, 480);
    frame.setVisible(true);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  }
}
