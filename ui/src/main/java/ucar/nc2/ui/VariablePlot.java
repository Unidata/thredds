/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Rectangle;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;

import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.time.CalendarDate;
import ucar.util.prefs.PreferencesExt;

/**
 * Plot with JFreeChart
 *
 * @author https://github.com/petejan
 */
public class VariablePlot extends JPanel {
  final private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(this.getClass());

  final TimeSeriesCollection dataset;
  final JFreeChart chart;

  public VariablePlot(PreferencesExt prefs) {
    // this.prefs = prefs;

    setLayout(new BorderLayout());

    dataset = createDataset();
    chart = createChart(dataset);

    final ChartPanel chartPanel = new ChartPanel(chart);
    chartPanel.setPreferredSize(new java.awt.Dimension(800, 600));
    chartPanel.setMouseZoomable(true, false);

    if (prefs != null)
      chartPanel.setBounds((Rectangle) prefs.getBean("PlotWindowBounds", new Rectangle(300, 300, 600, 600)));
    else
      chartPanel.setBounds(new Rectangle(300, 300, 600, 600));


    XYPlot plot = chart.getXYPlot();

    plot.setBackgroundPaint(Color.white);
    plot.setDomainGridlinePaint(Color.GRAY);
    plot.setDomainGridlinesVisible(true);
    plot.setRangeGridlinePaint(Color.GRAY);
    plot.setAxisOffset(RectangleInsets.ZERO_INSETS);
    LegendTitle legend = chart.getLegend();
    if (legend != null) {
      legend.setPosition(RectangleEdge.RIGHT);
    }
    add(chartPanel);
  }

  private JFreeChart createChart(final TimeSeriesCollection dataset) {
    final JFreeChart chart = ChartFactory.createTimeSeriesChart(
            "Variable Plot",
            "Date",
            "Value",
            dataset,
            true,
            true,
            false
    );
    final XYItemRenderer renderer = chart.getXYPlot().getRenderer();
    final StandardXYToolTipGenerator g = new StandardXYToolTipGenerator(
            StandardXYToolTipGenerator.DEFAULT_TOOL_TIP_FORMAT,
            new SimpleDateFormat("d-MMM-yyyy"), new DecimalFormat("0.00")
    );
    renderer.setBaseToolTipGenerator(g);
    return chart;
  }

  private TimeSeriesCollection createDataset() {
    return new TimeSeriesCollection();
  }

  NetcdfFile file;

  public void setDataset(NetcdfFile ncFile) {
    file = ncFile;
  }

  public void clear() {
    dataset.removeAllSeries();
  }

  // assume first dimension is TIME series
  public void setVariable(Variable v) throws IOException {
    log.info("variable " + v.getShortName());

    Dimension dim = v.getDimension(0);
    String dimName = dim.getShortName();

    Attribute title = file.findGlobalAttribute("title");
    if (title != null)
      chart.setTitle(title.getStringValue());

    Variable varT = file.findVariable(null, dimName);
    if (varT == null) return;

    Attribute tunit = varT.findAttribute("units");
    Attribute vunit = v.findAttribute("units");
    Attribute vfill = v.findAttribute("_FillValue");
    double dfill = Double.NaN;
    if (vfill != null)
      dfill = vfill.getNumericValue().doubleValue();

    chart.getXYPlot().getRangeAxis().setLabel(vunit.getStringValue());

    log.info("Time type " + tunit.getDataType() + " value " + tunit.toString());

    NetcdfDataset fds = new NetcdfDataset(file);

    CoordinateAxis1DTime tm = CoordinateAxis1DTime.factory(fds, new VariableDS(null, varT, true), null);
    List<CalendarDate> dates = tm.getCalendarDates();

    Array a = v.read();

    Index idx = a.getIndex();
    idx.setCurrentCounter(0);

    int d2 = 1;
    int rank = idx.getRank();
    for (int k = 1; k < rank; k++) {
      d2 *= idx.getShape(k);
    }
    log.info(v.getShortName() + " DIMS " + v.getDimensionsString() + " RANK " + rank + " D2 " + d2);

    for (int j = 0; j < d2; j++) {
      if (rank > 1)
        idx.set1(j); // this wont work for 3rd dimension > 1
      String name = v.getShortName();
      if (d2 > 1)
        name += "-" + j;

      TimeSeries s1 = new TimeSeries(name);

      for (int i = 0; i < idx.getShape(0); i++) {
        idx.set0(i);
        float f = a.getFloat(idx);
        if (f != dfill) {
          Date ts = new Date(dates.get(i).getMillis());
          s1.addOrUpdate(new Second(ts), f);
        }
      }

      dataset.addSeries(s1);
    }
  }
}
