/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import java.awt.*;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import javax.swing.*;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.general.Dataset;
import org.jfree.data.general.Series;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.AbstractIntervalXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
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

    private static final org.slf4j.Logger log
                = org.slf4j.LoggerFactory.getLogger (MethodHandles.lookup ( ).lookupClass ( ));

  final JFreeChart chart;

  public VariablePlot(PreferencesExt prefs) {
    // this.prefs = prefs;

    setLayout(new BorderLayout());
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

    chart = createChart();

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
      legend.setPosition(RectangleEdge.BOTTOM);
    }
    add(chartPanel);
    
  }

  private JFreeChart createChart() {
    return ChartFactory.createTimeSeriesChart(
            "Variable Plot",
            "X",
            "Value",
            null,
            true,
            true,
            false
    );
  }

  NetcdfFile file;

  public void setDataset(NetcdfFile ncFile) {
	  log.info("setDataset");
	  file = ncFile;	  
  }
  
  public void autoScale() {
	chart.getXYPlot().getRangeAxis().setAutoRange(true);
	chart.getXYPlot().getDomainAxis().setAutoRange(true);
  }

  public void clear() {
	  XYPlot p = chart.getXYPlot();

	  for(int i = 0;i<p.getDatasetCount();i++)
	  {
		  Dataset dataset = p.getDataset(i);		  

		  log.info("clear dataset " + i + " dataset " + dataset);
		  
		  if (dataset instanceof TimeSeriesCollection)		  
			  ((TimeSeriesCollection)dataset).removeAllSeries();
		  if (dataset instanceof XYSeriesCollection)		  
			  ((XYSeriesCollection)dataset).removeAllSeries();
		  
		  p.setDataset(i, null);
		  if (i > 0)
			  p.setRangeAxis(i, null);
	  }
  }

  // assume first dimension is TIME series
  public void setVariable(Variable v) throws IOException {
    log.info("variable " + v.getShortName());

    AbstractIntervalXYDataset dataset;

    Dimension dim = v.getDimension(0);
    String dimName = dim.getShortName();

    Attribute title = file.findGlobalAttribute("title");
    if (title != null)
      chart.setTitle(title.getStringValue());
    
    Variable varXdim = file.findVariable(null, dimName);
    boolean hasXdim = false;
    if (varXdim != null)
    	hasXdim = true;
    
    boolean xIsTime = false;

    XYPlot p = chart.getXYPlot();
    if (hasXdim)
    {
	    Attribute xUnit = varXdim.findAttribute("units");
	    Attribute xAxis = varXdim.findAttribute("axis");
	    if (xUnit != null)
	    	if (xUnit.getStringValue().contains("since"))
	    		xIsTime = true;
	    if (xAxis != null)
	    	if (xAxis.getStringValue().equals("T"))
	    		xIsTime = true;
	    if (xUnit != null)
	    	p.getDomainAxis().setLabel(xUnit.getStringValue());
	    else
	    	p.getDomainAxis().setLabel(dimName);

        if (xAxis != null) {
          log.info("X axis type " + xAxis.getDataType() + " value " + xAxis.toString() + " is Time "
              + xIsTime);
        }
    }

    int ax = 0;
    log.info("dataset count " + p.getDatasetCount());
    if (p.getDatasetCount() >= 1)
    {
    	if (p.getDataset(p.getDatasetCount()-1) != null)
    	{
		    log.info("number in dataset " + p.getDataset(0));
		    ax = p.getDatasetCount();
		    if (ax > 0)
		    {
		    	p.setRangeAxis(ax, new NumberAxis());
		    }
    	}
    }
    log.info("axis number " + ax);
    
    final XYItemRenderer renderer = p.getRenderer();
    if (xIsTime)
    {
        final StandardXYToolTipGenerator g = new StandardXYToolTipGenerator(StandardXYToolTipGenerator.DEFAULT_TOOL_TIP_FORMAT, new SimpleDateFormat("d-MMM-yyyy"), new DecimalFormat("0.00"));
        renderer.setBaseToolTipGenerator(g);
    	
    	dataset = new TimeSeriesCollection();
    }
    else
    {
        final StandardXYToolTipGenerator g = new StandardXYToolTipGenerator(StandardXYToolTipGenerator.DEFAULT_TOOL_TIP_FORMAT, new DecimalFormat("0.00"), new DecimalFormat("0.00"));
        renderer.setBaseToolTipGenerator(g);

        dataset = new XYSeriesCollection();
        p.setDomainAxis(new NumberAxis()); // change to NumberAxis from DateAxis which is what is created
    }
    p.getRangeAxis(ax).setAutoRange(true);
    
    Attribute vUnit = v.findAttribute("units");
    Attribute vfill = v.findAttribute("_FillValue");
    double dfill = Double.NaN;
    if (vfill != null)
      dfill = vfill.getNumericValue().doubleValue();

    if (vUnit != null)
    	p.getRangeAxis(ax).setLabel(vUnit.getStringValue());

    NetcdfDataset fds = new NetcdfDataset(file);

    CoordinateAxis1DTime tm;
    List<CalendarDate> dates = null;
    Array varXarray = null;
    
    if (hasXdim)
    {
    	varXarray = varXdim.read();
	    if (xIsTime)
	    {
		    tm = CoordinateAxis1DTime.factory(fds, new VariableDS(null, varXdim, true), null);
		    dates = tm.getCalendarDates();
	    }
    }
    
    Array a = v.read();

    Index idx = a.getIndex();
    idx.setCurrentCounter(0);

    int d2 = 1;
    int rank = idx.getRank();
    for (int k = 1; k < rank; k++) {
      d2 *= idx.getShape(k);
    }
    log.info("variable : " + v.getShortName() + " : dims " + v.getDimensionsString() + " rank " + rank + " d2 " + d2);

    double max = -1000;
    double min = 1000;

    for (int j = 0; j < d2; j++) {
      if (rank > 1)
        idx.set1(j); // this wont work for 3rd dimension > 1
      
      String name = v.getShortName();
      if (d2 > 1)
        name += "-" + j;

      Series s1;
      if (xIsTime)
    	  s1 = new TimeSeries(name);
      else
    	  s1 = new XYSeries(name);
      
      for (int i = 0; i < idx.getShape(0); i++) {
        idx.set0(i);
        float f = a.getFloat(idx);
        if (f != dfill) {
        	if (!Float.isNaN(f))
        	{
	        	max = Math.max(max,  f);
	        	min = Math.min(min,  f);
        	}
        	if (xIsTime)
        	{
        		Date ts = new Date(dates.get(i).getMillis());
        		((TimeSeries)s1).addOrUpdate(new Second(ts), f);
        	}
        	else if (hasXdim)
        	{
        		((XYSeries)s1).addOrUpdate(varXarray.getDouble(i), f);        		
        	}
        	else
        	{
        		((XYSeries)s1).addOrUpdate(i, f);        		        		
        	}
        }
      }
	  if (dataset instanceof TimeSeriesCollection)		  
		  ((TimeSeriesCollection)dataset).addSeries((TimeSeries)s1);
	  if (dataset instanceof XYSeriesCollection)		  
		  ((XYSeriesCollection)dataset).addSeries((XYSeries)s1);
    }
    final XYLineAndShapeRenderer renderer1 = new XYLineAndShapeRenderer(true, false); 
    p.setRenderer(ax, renderer1);

    log.info("dataset " + ax + " max " + max + " min " + min + " : " + dataset);
    p.setDataset(ax, dataset);
    p.mapDatasetToRangeAxis(ax, ax);
    p.getRangeAxis(ax).setLowerBound(min);
    p.getRangeAxis(ax).setUpperBound(max);

  }
}
