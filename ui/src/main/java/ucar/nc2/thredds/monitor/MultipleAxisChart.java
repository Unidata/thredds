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

import java.awt.*;

import javax.swing.*;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.time.Minute;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

/**
 * This demo shows a time series chart that has multiple range axes.
 */
public class MultipleAxisChart extends JPanel {

  JFreeChart chart;

  /**
   * A demonstration application showing how to create a time series chart
   * with multiple axes.
   *
   * @param title the frame title.
   */
  public MultipleAxisChart(String title1, String xaxis1, String yaxis1, TimeSeries series1) {

    TimeSeriesCollection dataset1 = new TimeSeriesCollection();
    dataset1.addSeries(series1);

    chart = ChartFactory.createTimeSeriesChart(title1, xaxis1, yaxis1, dataset1,
            true,
            true,
            false
    );

    // chart.addSubtitle(new TextTitle("Four datasets and four range axes."));
    XYPlot plot = (XYPlot) chart.getPlot();
    plot.setOrientation(PlotOrientation.VERTICAL);
    plot.getRangeAxis().setFixedDimension(15.0);

    this.setLayout(new BorderLayout());
    this.add(new ChartPanel(chart), BorderLayout.CENTER);
  }

  private int axisNum = 1;
  public void addSeries(String yaxisName, TimeSeries series) {
    NumberAxis axis2 = new NumberAxis(yaxisName);
    axis2.setFixedDimension(10.0);
    axis2.setAutoRangeIncludesZero(false);

    XYPlot plot = (XYPlot) chart.getPlot();
    plot.setRangeAxis(axisNum, axis2);
    plot.setRangeAxisLocation(1, AxisLocation.BOTTOM_OR_LEFT);

    TimeSeriesCollection dataset = new TimeSeriesCollection();
    dataset.addSeries(series);

    plot.setDataset(axisNum, dataset);
    plot.mapDatasetToRangeAxis(axisNum, axisNum);
    XYItemRenderer renderer2 = new StandardXYItemRenderer();
    plot.setRenderer(axisNum, renderer2);

    axisNum++;
  }

  private static final Color[] colors = new Color[] { Color.black, Color.red, Color.blue, Color.green};

  public void finish(java.awt.Dimension preferredSize) {
    ChartUtilities.applyCurrentTheme(chart);

    XYPlot plot = (XYPlot) chart.getPlot();
    for (int i=0; i<axisNum; i++) {
      XYItemRenderer renderer = plot.getRenderer(i);
      renderer.setSeriesPaint(0, colors[i]);

      ValueAxis axis = plot.getRangeAxis(i);
      axis.setLabelPaint(colors[i]);
      axis.setTickLabelPaint(colors[i]);
    }
    
    ChartPanel chartPanel = new ChartPanel(chart);
    chartPanel.setPreferredSize(preferredSize);
    chartPanel.setDomainZoomable(true);
    chartPanel.setRangeZoomable(true);
  }

  /**
   * Creates the demo chart.
   *
   * @return The chart.
   *
  private JFreeChart createChart() {

    XYDataset dataset1 = createDataset("Series 1", 100.0, new Minute(), 200);

    JFreeChart chart = ChartFactory.createTimeSeriesChart(
            "Multiple Axis Demo 1",
            "Time of Day",
            "Primary Range Axis",
            dataset1,
            true,
            true,
            false
    );

    chart.addSubtitle(new TextTitle("Four datasets and four range axes."));
    XYPlot plot = (XYPlot) chart.getPlot();
    plot.setOrientation(PlotOrientation.VERTICAL);

    plot.getRangeAxis().setFixedDimension(15.0);

    // AXIS 2
    NumberAxis axis2 = new NumberAxis("Range Axis 2");
    axis2.setFixedDimension(10.0);
    axis2.setAutoRangeIncludesZero(false);
    plot.setRangeAxis(1, axis2);
    plot.setRangeAxisLocation(1, AxisLocation.BOTTOM_OR_LEFT);

    XYDataset dataset2 = createDataset("Series 2", 1000.0, new Minute(),
            170);
    plot.setDataset(1, dataset2);
    plot.mapDatasetToRangeAxis(1, 1);
    XYItemRenderer renderer2 = new StandardXYItemRenderer();
    plot.setRenderer(1, renderer2);

    // AXIS 3
    NumberAxis axis3 = new NumberAxis("Range Axis 3");
    plot.setRangeAxis(2, axis3);

    XYDataset dataset3 = createDataset("Series 3", 10000.0, new Minute(),
            170);
    plot.setDataset(2, dataset3);
    plot.mapDatasetToRangeAxis(2, 2);
    XYItemRenderer renderer3 = new StandardXYItemRenderer();
    plot.setRenderer(2, renderer3);

    // AXIS 4
    NumberAxis axis4 = new NumberAxis("Range Axis 4");
    plot.setRangeAxis(3, axis4);

    XYDataset dataset4 = createDataset("Series 4", 25.0, new Minute(), 200);
    plot.setDataset(3, dataset4);
    plot.mapDatasetToRangeAxis(3, 3);

    XYItemRenderer renderer4 = new StandardXYItemRenderer();
    plot.setRenderer(3, renderer4);

    ChartUtilities.applyCurrentTheme(chart);

    // change the series and axis colours after the theme has
    // been applied...
    plot.getRenderer().setSeriesPaint(0, Color.black);
    renderer2.setSeriesPaint(0, Color.red);
    axis2.setLabelPaint(Color.red);
    axis2.setTickLabelPaint(Color.red);
    renderer3.setSeriesPaint(0, Color.blue);
    axis3.setLabelPaint(Color.blue);
    axis3.setTickLabelPaint(Color.blue);
    renderer4.setSeriesPaint(0, Color.green);
    axis4.setLabelPaint(Color.green);
    axis4.setTickLabelPaint(Color.green);

    return chart;
  }

  /**
   * Creates a sample dataset.
   *
   * @param name  the dataset name.
   * @param base  the starting value.
   * @param start the starting period.
   * @param count the number of values to generate.
   * @return The dataset.
   */
  private static TimeSeries createDataset(String name, double base,
          RegularTimePeriod start, int count) {

    TimeSeries series = new TimeSeries(name, start.getClass());
    RegularTimePeriod period = start;
    double value = base;
    for (int i = 0; i < count; i++) {
      series.add(period, value);
      period = period.next();
      value = value * (1 + (Math.random() - 0.495) / 10.0);
    }
    return series;

  }

  /**
   * Starting point for the demonstration application.
   *
   * @param args ignored.
   */
  public static void main(String[] args) {

    TimeSeries dataset1 = createDataset("Series 1", 100.0, new Minute(), 200);

    MultipleAxisChart demo = new MultipleAxisChart(
            "Multiple Axis Demo 1",
            "Time of Day",
            "Primary Range Axis",
            dataset1);

    /* AXIS 2
    NumberAxis axis2 = new NumberAxis("Range Axis 2");
    axis2.setFixedDimension(10.0);
    axis2.setAutoRangeIncludesZero(false);
    plot.setRangeAxis(1, axis2);
    plot.setRangeAxisLocation(1, AxisLocation.BOTTOM_OR_LEFT); /

    plot.setDataset(1, dataset2);
    plot.mapDatasetToRangeAxis(1, 1);
    XYItemRenderer renderer2 = new StandardXYItemRenderer();
    plot.setRenderer(1, renderer2); */

    TimeSeries dataset2 = createDataset("Series 2", 1000.0, new Minute(), 170);
    demo.addSeries("Range Axis 2", dataset2);

    /*     // AXIS 3
    NumberAxis axis3 = new NumberAxis("Range Axis 3");
    plot.setRangeAxis(2, axis3);

    XYDataset dataset3 = createDataset("Series 3", 10000.0, new Minute(), 170);
    plot.setDataset(2, dataset3);
    plot.mapDatasetToRangeAxis(2, 2);
    XYItemRenderer renderer3 = new StandardXYItemRenderer();
    plot.setRenderer(2, renderer3);
    */
    TimeSeries dataset3 = createDataset("Series 3", 10000.0, new Minute(), 170);
    demo.addSeries("Range Axis 3", dataset3);

    /* AXIS 4
    NumberAxis axis4 = new NumberAxis("Range Axis 4");
    plot.setRangeAxis(3, axis4);

    XYDataset dataset4 = createDataset("Series 4", 25.0, new Minute(), 200);
    plot.setDataset(3, dataset4);
    plot.mapDatasetToRangeAxis(3, 3);

    XYItemRenderer renderer4 = new StandardXYItemRenderer();
    plot.setRenderer(3, renderer4); */
    TimeSeries dataset4 = createDataset("Series 4", 25.0, new Minute(), 200);
    demo.addSeries("Range Axis 4", dataset4);


    demo.finish(new java.awt.Dimension(600, 270));
    
    JFrame frame = new JFrame("Demovabulous ");
    frame.getContentPane().add(demo, BorderLayout.CENTER);
    frame.setSize(640, 480);
    frame.setVisible(true);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  }

}

