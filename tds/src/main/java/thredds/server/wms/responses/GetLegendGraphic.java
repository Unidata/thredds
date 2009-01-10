package thredds.server.wms.responses;

import uk.ac.rdg.resc.ncwms.controller.RequestParams;
import uk.ac.rdg.resc.ncwms.controller.GetMapStyleRequest;
import uk.ac.rdg.resc.ncwms.controller.ColorScaleRange;
import uk.ac.rdg.resc.ncwms.styles.ColorPalette;
import uk.ac.rdg.resc.ncwms.metadata.Layer;
import uk.ac.rdg.resc.ncwms.usagelog.UsageLogEntry;
import uk.ac.rdg.resc.ncwms.exceptions.WmsException;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import ucar.nc2.dt.GridDataset;
import org.springframework.web.servlet.ModelAndView;

/**
 * Created by IntelliJ IDEA.
 * User: pmak
 * Date: May 9, 2008
 * Time: 2:53:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class GetLegendGraphic extends LayerBasedResponse
{
    private static org.slf4j.Logger log =
              org.slf4j.LoggerFactory.getLogger( GetLegendGraphic.class );
    

    public GetLegendGraphic(RequestParams _params, GridDataset _dataset, UsageLogEntry usageLogEntry) throws Exception
    {
        super(_params, _dataset, usageLogEntry);
    }

    public ModelAndView processRequest(HttpServletResponse res, HttpServletRequest req) throws Exception
    {
        BufferedImage legend;

        // Find the requested colour palette, or use the default if not set
        ColorPalette palette = ColorPalette.get(params.getString("palette"));
        // numColourBands defaults to 254 (the maximum) if not set
        int numColourBands = GetMapStyleRequest.getNumColourBands(params);

        // Find out if we just want the colour bar with no supporting text
        String colorBarOnly = params.getString("colorbaronly", "false");
        if (colorBarOnly.equalsIgnoreCase("true"))
        {
            // We're only creating the colour bar so we need to know a width
            // and height
            int width = params.getPositiveInt("width", 50);
            int height = params.getPositiveInt("height", 200);
            legend = palette.createColorBar(width, height, numColourBands);
        }
        else
        {
            // We're creating a legend with supporting text so we need to know
            // the colour scale range and the layer in question
            String layerName = params.getMandatoryString("layer");
            Layer layer = layers.get(layerName);          
            ColorScaleRange colorScaleRange = GetMapStyleRequest.getColorScaleRange(params);

            boolean logarithmic = GetMapStyleRequest.isLogScale(params);
            float scaleMin;
            float scaleMax;

            // Get the legend graphic and write it to the client
            // The scale range will be [0,0] if the client has not specified one
            // explicitly.  In this case createLegend() will use the layer's
            // default scale range.
            if (colorScaleRange.isDefault())
            {
                float[] scaleRange = layer.getScaleRange();
                scaleMin = scaleRange[0];
                scaleMax = scaleRange[1];
            }
            else if (colorScaleRange.isAuto())
            {
                throw new WmsException("Cannot automatically create a colour scale "
                    + "for a legend graphic.  Use COLORSCALERANGE=default or specify "
                    + "the scale extremes explicitly.");
            }
            else
            {
                scaleMin = colorScaleRange.getScaleMin();
                scaleMax = colorScaleRange.getScaleMax();
            }
            // Get the legend graphic and write it to the client
            // The scale range will be [0,0] if the client has not specified one
            // explicitly.  In this case createLegend() will use the layer's
            // default scale range.
            legend = palette.createLegend(numColourBands, layer,
                logarithmic, scaleMin, scaleMax);
        }
        res.setContentType("image/png");
        ImageIO.write(legend, "png", res.getOutputStream());
        return null;
    }
}
