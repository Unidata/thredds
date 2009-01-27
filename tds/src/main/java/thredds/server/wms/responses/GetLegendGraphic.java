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
