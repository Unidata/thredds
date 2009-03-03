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
package thredds.server.wms.responses;

import ucar.nc2.dt.GridDataset;
import ucar.unidata.geoloc.LatLonPoint;
import uk.ac.rdg.resc.ncwms.controller.GetFeatureInfoRequest;
import uk.ac.rdg.resc.ncwms.controller.GetFeatureInfoDataRequest;
import uk.ac.rdg.resc.ncwms.controller.RequestParams;
import uk.ac.rdg.resc.ncwms.datareader.HorizontalGrid;
import uk.ac.rdg.resc.ncwms.metadata.VectorLayer;
import uk.ac.rdg.resc.ncwms.metadata.LayerImpl;
import uk.ac.rdg.resc.ncwms.exceptions.WmsException;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidFormatException;
import uk.ac.rdg.resc.ncwms.usagelog.UsageLogEntry;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.awt.geom.Ellipse2D;

import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.Millisecond;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.springframework.web.servlet.ModelAndView;
import org.joda.time.DateTime;
import thredds.server.wms.util.LayerOps;

/**
 * Created by IntelliJ IDEA.
 * User: pmak
 * Date: May 12, 2008
 * Time: 2:11:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class GetFeatureInfo extends LayerBasedResponse
{
    private static org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger( GetFeatureInfo.class );

    private GetFeatureInfoRequest infoRequest;
    private GetFeatureInfoDataRequest dataRequest;
    
    public GetFeatureInfo(RequestParams params, GridDataset _dataset, UsageLogEntry usageLogEntry) throws Exception
    {
        super(params, _dataset, usageLogEntry);
        infoRequest = new GetFeatureInfoRequest(params);
        dataRequest =  infoRequest.getDataRequest();
    }

    public ModelAndView processRequest(HttpServletResponse res, HttpServletRequest req) throws Exception
    {
        // Check the feature count
        if (dataRequest.getFeatureCount() != 1)
        {
            throw new WmsException("Can only provide feature info for one layer at a time");
        }

        // Check the output format
        if (!infoRequest.getOutputFormat().equals(GetCapabilities.FEATURE_INFO_XML_FORMAT) &&
            !infoRequest.getOutputFormat().equals(GetCapabilities.FEATURE_INFO_PNG_FORMAT))
        {
            throw new InvalidFormatException("The output format " +
                infoRequest.getOutputFormat() + " is not valid for GetFeatureInfo");
        }


        String layerName = dataRequest.getLayers()[0];
        LayerImpl layer = layers.get(layerName);

        // Get the grid onto which the data is being projected
        HorizontalGrid grid = new HorizontalGrid(dataRequest);
        // Get the x and y values of the point of interest
        double x = grid.getXAxisValues()[dataRequest.getPixelColumn()];
        double y = grid.getYAxisValues()[dataRequest.getPixelRow()];
        LatLonPoint latLon = grid.transformToLatLon(x, y);
        //usageLogEntry.setFeatureInfoLocation(latLon.getLongitude(), latLon.getLatitude());

        // Get the index along the z axis
        int zIndex = LayerOps.getZIndex(dataRequest.getElevationString(), layer); // -1 if no z axis present

        // Get the information about the requested timesteps
        List<Integer> tIndices = LayerOps.getTIndices(dataRequest.getTimeString(), layer);
        //usageLogEntry.setNumTimeSteps(tIndices.size());

        // Now read the data, mapping date-times to data values
        // The map is sorted in order of ascending time
        SortedMap<DateTime, Float> featureData = new TreeMap<DateTime, Float>();
        for (int tIndex : tIndices)
        {
            DateTime date = tIndex < 0 ? null : layer.getTimesteps().get(tIndex).getDateTime();

            // Create a trivial Grid for reading a single point of data.
            // We use the same coordinate reference system as the original request
            HorizontalGrid singlePointGrid = new HorizontalGrid(dataRequest.getCrsCode(),
                1, 1, new double[]{x, y, x, y});

            float val;
            // We don't use the tile cache for getFeatureInfo
            if (layer instanceof VectorLayer)
            {
                VectorLayer vecLayer = (VectorLayer)layer;
                float xval = LayerOps.readDataArray(dataset, vecLayer.getEastwardComponent(), tIndex,
                    zIndex, singlePointGrid, usageLogEntry, reader)[0];
                float yval = LayerOps.readDataArray(dataset, vecLayer.getNorthwardComponent(), tIndex,
                    zIndex, singlePointGrid, usageLogEntry, reader)[0];
                val = (float)Math.sqrt(xval * xval + yval * yval);
            }
            else
            {
                val = LayerOps.readDataArray(dataset, layer, tIndex, zIndex, singlePointGrid,
                    usageLogEntry, reader)[0];
            }
            featureData.put(date, Float.isNaN(val) ? null : val);
        }

        if (infoRequest.getOutputFormat().equals(GetCapabilities.FEATURE_INFO_XML_FORMAT))
        {
            Map<String, Object> models = new HashMap<String, Object>();
            models.put("longitude", latLon.getLongitude());
            models.put("latitude", latLon.getLatitude());
            models.put("data", featureData);

            return new ModelAndView("showFeatureInfo_xml", models);
        }
        else
        {
            // Must be PNG format: prepare and output the JFreeChart
            // TODO: this is nasty: we're mixing presentation code in the controller
            TimeSeries ts = new TimeSeries("Data", Millisecond.class);
            for (DateTime dateTime : featureData.keySet())
            {
                ts.add(new Millisecond(dateTime.toDate()), featureData.get(dateTime));
            }
            TimeSeriesCollection xydataset = new TimeSeriesCollection();
            xydataset.addSeries(ts);

            // Create a chart with no legend, tooltips or URLs
            String title = "Lon: " + latLon.getLongitude() + ", Lat: " + latLon.getLatitude();
            String yLabel = layer.getTitle() + " (" + layer.getUnits() + ")";
            JFreeChart chart = ChartFactory.createTimeSeriesChart(title,
                "Date / time", yLabel, xydataset, false, false, false);

            // Lines added by Dave Crossman: allows a single point to be plotted
            // on a line chart
            XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
            renderer.setSeriesShape(0, new Ellipse2D.Double(-1.0, -1.0, 2.0, 2.0));
            renderer.setSeriesShapesVisible(0, true);
            chart.getXYPlot().setRenderer(renderer);

            res.setContentType("image/png");
            ChartUtilities.writeChartAsPNG(res.getOutputStream(), chart, 400, 300);
            return null;
        }
        
    }

    /*

    private void printFeatureXML(double lat, double lon, Map<Date, Float> featureData, HttpServletResponse res)
    {
        res.setHeader("Cache-Control","no-cache"); //HTTP 1.1
        res.setHeader("Pragma","no-cache"); //HTTP 1.0
        res.setDateHeader ("Expires", 0); //prevents caching at the proxy server

        String doc = "<FeatureInfoResponse>\n" +
                "    <longitude>" + lon + "</longitude>\n" +
                "    <latitude>" + lat + "</latitude>\n";

                for(Date d: featureData.keySet())
                {
                    doc += "<FeatureInfo>";
                    if(d != null)
                    {
                        doc += "<time>" + WmsUtils.dateToISO8601(d) + "</time>";
                    }

                    if(featureData.get(d) == null)
                    {
                        doc += "<value>none</value>";
                    }
                    else
                    {
                        doc += "<value>" + featureData.get(d) + "<value>";
                    }
                    doc += "<FeatureInfo>";
                }

                doc += "</FeatureInfoResponse>";

        PrintWriter writer = null;
        try
        {
            writer = res.getWriter();
            writer.write(doc);
            writer.close();
        }
        catch(IOException ioe)
        {
            log.error("Error encountered while writing response for GetFeatureInfo (XML).");
        }
        finally
        {
            if(writer != null)
                writer.close();
        }
    }*/
}
