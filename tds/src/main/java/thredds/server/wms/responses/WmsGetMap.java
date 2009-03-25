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

import uk.ac.rdg.resc.ncwms.controller.GetMapRequest;
import uk.ac.rdg.resc.ncwms.controller.GetMapDataRequest;
import uk.ac.rdg.resc.ncwms.controller.RequestParams;
import uk.ac.rdg.resc.ncwms.graphics.ImageFormat;
import uk.ac.rdg.resc.ncwms.graphics.KmzFormat;
import uk.ac.rdg.resc.ncwms.exceptions.WmsException;
import uk.ac.rdg.resc.ncwms.datareader.HorizontalGrid;
import uk.ac.rdg.resc.ncwms.metadata.LayerImpl;
import uk.ac.rdg.resc.ncwms.metadata.Layer;
import uk.ac.rdg.resc.ncwms.styles.ImageProducer;
import uk.ac.rdg.resc.ncwms.utils.WmsUtils;
import uk.ac.rdg.resc.ncwms.usagelog.UsageLogEntry;
import ucar.nc2.dt.GridDataset;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.awt.image.BufferedImage;

import thredds.server.wms.util.LayerOps;
import org.springframework.web.servlet.ModelAndView;

/**
 * Created by IntelliJ IDEA.
 * User: pmak
 * Date: May 8, 2008
 * Time: 9:17:31 AM
 * To change this template use File | Settings | File Templates.
 */
public class WmsGetMap extends LayerBasedResponse
{
    private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( WmsGetMap.class );

    protected GetMapRequest getMapRequest;
    
    public WmsGetMap(RequestParams params, GridDataset _dataset, UsageLogEntry usageLogEntry) throws Exception
    {
        super(params, _dataset, usageLogEntry);

        getMapRequest = new GetMapRequest(params);
    }

    public ModelAndView processRequest(HttpServletResponse res, HttpServletRequest req) throws Exception
    {
        // Get the ImageFormat object corresponding with the requested MIME type
        String mimeType = getMapRequest.getStyleRequest().getImageFormat();

        // This throws an InvalidFormatException if the MIME type is not supported
        ImageFormat imageFormat = ImageFormat.get(mimeType);

        GetMapDataRequest dr = getMapRequest.getDataRequest();
        String[] layers = dr.getLayers();

        if (layers.length > GetCapabilities.LAYER_LIMIT)
        {
            throw new WmsException("You may only request a maximum of " +
                GetCapabilities.LAYER_LIMIT + " layer(s) simultaneously from this server");
        }

        // Get the grid onto which the data will be projected
        HorizontalGrid grid = new HorizontalGrid(dr);


        // Create an object that will turn data into BufferedImages
        ImageProducer imageProducer = new ImageProducer(getMapRequest, layer);
        // Need to make sure that the images will be compatible with the
        // requested image format
        if (imageProducer.isTransparent() && !imageFormat.supportsFullyTransparentPixels())
        {
            throw new WmsException("The image format " + mimeType +
                " does not support fully-transparent pixels");
        }
        if (imageProducer.getOpacity() < 100 && !imageFormat.supportsPartiallyTransparentPixels())
        {
            throw new WmsException("The image format " + mimeType +
                " does not support partially-transparent pixels");
        }

        String zValue = dr.getElevationString();
        int zIndex = LayerOps.getZIndex(zValue, layer); // -1 if no z axis present

        // Cycle through all the provided timesteps, extracting data for each step
        List<String> tValues = new ArrayList<String>();
        String timeString = getMapRequest.getDataRequest().getTimeString();
        List<Integer> tIndices = LayerOps.getTIndices(timeString, layer);
        if (tIndices.size() > 1 && !imageFormat.supportsMultipleFrames())
        {
            throw new WmsException("The image format " + mimeType +
                " does not support multiple frames");
        }

        log.debug("Number of time steps set: " + tIndices.size());

        long beforeExtractData = System.currentTimeMillis();
        for (int tIndex : tIndices)
        {
            // tIndex == -1 if there is no t axis present
            List<float[]> picData = LayerOps.readData(dataset, layer, tIndex, zIndex, grid,
                /*this.tileCache, */ usageLogEntry, reader);
            // Only add a label if this is part of an animation

            for(int i = 0; i < picData.size(); i++)
            {
                log.info(picData.get(i).toString());         
            }

            String tValue = "";
            if (layer.isTaxisPresent() && tIndices.size() > 1)
            {
                tValue = WmsUtils.dateTimeToISO8601(layer.getTimesteps().get(tIndex).getDateTime());
            }
            tValues.add(tValue);
            imageProducer.addFrame(picData, tValue); // the tValue is the label for the image
        }
        
        long timeToExtractData = System.currentTimeMillis() - beforeExtractData;

        log.debug("time taken to extract data: " + timeToExtractData);

        // We only create a legend object if the image format requires it
        BufferedImage legend = imageFormat.requiresLegend() ? imageProducer.getLegend() : null;

        log.debug("create a legend...: " + imageFormat.requiresLegend());

        // Write the image to the client
        res.setStatus(HttpServletResponse.SC_OK);
        res.setContentType(mimeType);
        // If this is a KMZ file give it a sensible filename
        if (imageFormat instanceof KmzFormat)
        {
            res.setHeader("Content-Disposition", "inline; filename=\"" +
                layer.getDataset().getTitle() + "_" + layer.getId() + ".kmz\"");
        }

        log.debug("Set mime type: " + mimeType + " and about to write image");

        log.debug("Image product: " + imageProducer + " layer: " + layer + " tValues: " + tValues + " zValues: " + zValue
        + " gridBbox: " + grid.getBbox() + " legend: " + legend);

        try
        {
            // Send the images to the picMaker and write to the output
            imageFormat.writeImage(imageProducer.getRenderedFrames(),
                res.getOutputStream(), layer, tValues, zValue,
                grid.getBbox(), legend);
        }
        catch(NullPointerException e)
        {
            e.printStackTrace();
        }
        log.debug("completed image writing");

        return null;
    }

    

}
