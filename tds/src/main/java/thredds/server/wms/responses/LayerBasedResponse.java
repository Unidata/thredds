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

        layerName = params.getMandatoryString("layers");
        log.debug("layers: " + layerName);
        layer = layers.get(layerName);

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
