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

import uk.ac.rdg.resc.ncwms.controller.RequestParams;
import uk.ac.rdg.resc.ncwms.metadata.LayerImpl;
import uk.ac.rdg.resc.ncwms.metadata.Layer;
import uk.ac.rdg.resc.ncwms.datareader.DataReader;
import uk.ac.rdg.resc.ncwms.datareader.HorizontalGrid;
import uk.ac.rdg.resc.ncwms.usagelog.UsageLogEntry;
import uk.ac.rdg.resc.ncwms.exceptions.WmsException;
import ucar.nc2.dt.GridDataset;

import java.util.Map;
import java.io.IOException;

import thredds.server.wms.util.LayerOps;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.ModelAndView;

/**
 * Created by IntelliJ IDEA.
 * User: pmak
 * Date: Jul 2, 2008
 * Time: 4:58:43 PM
 * To change this template use File | Settings | File Templates.
 */
abstract public class LayerBasedResponse extends FileBasedResponse
{
    private static org.slf4j.Logger log =
              org.slf4j.LoggerFactory.getLogger( LayerBasedResponse.class );

    protected String layerName;

    public LayerBasedResponse(RequestParams _params, GridDataset _dataset, UsageLogEntry _usageLogEntry) throws Exception
    {
        super(_params, _dataset, _usageLogEntry);

        String[] layerNames = params.getMandatoryString("layers").split(",");

        if (layerNames.length > GetCapabilities.LAYER_LIMIT)
        {
            throw new WmsException("You may only request a maximum of " +
                GetCapabilities.LAYER_LIMIT + " layer(s) simultaneously from this server");
        }

        //Hmmm... this kinds of restrict it to 1 layer at a time.
        //Given that this is a setting in GetCapabilities.LAYER_LIMIT, it's probably
        //not soooo bad.  But beware for future versions....
        // From ncWMS: TODO: support more than one layer (superimposition, difference, mask)
        layer = layers.get(layerNames[0]);
        if ( layer == null )
          throw new WmsException( "Layer [" + layerNames[0] + "] not recognized.");

        log.debug("layers: " + layerNames[0]);

        HorizontalGrid grid = new HorizontalGrid("CRS:84", 100, 100, layer.getBbox());

        int tIndex = layer.isTaxisPresent() ? 0 : -1;

        int zIndex = layer.isZaxisPresent() ? 0 : -1;

        float[] minMax = LayerOps.findMinMax(dataset, layer, tIndex, zIndex,
                grid, usageLogEntry, reader);

        if (Float.isNaN(minMax[0]) || Float.isNaN(minMax[1]))
        {
            // Just guess at a scale
            layer.setScaleMin(-50.0f);
            layer.setScaleMax(50.0f);
        }
        else
        {
            // Set the scale range of the layer, factoring in a 10% expansion
            // to deal with the fact that the sample data we read might
            // not be representative
            float diff = minMax[1] - minMax[0];

            log.debug(layerName + " real min: " + minMax[0] + " setMin: " + (minMax[0] - 0.05f * diff));
            log.debug(layerName + " real max: " + minMax[1] + " setMax: " + (minMax[1] + 0.05f * diff));
            
            layer.setScaleMin(minMax[0] - 0.05f * diff);
            layer.setScaleMax(minMax[1] + 0.05f * diff);
        }
    }
}
