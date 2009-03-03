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
import uk.ac.rdg.resc.ncwms.usagelog.UsageLogEntry;
import uk.ac.rdg.resc.ncwms.graphics.ImageFormat;
import uk.ac.rdg.resc.ncwms.datareader.HorizontalGrid;
import uk.ac.rdg.resc.ncwms.datareader.DataReader;
import uk.ac.rdg.resc.ncwms.styles.ColorPalette;
import uk.ac.rdg.resc.ncwms.config.Config;
import uk.ac.rdg.resc.ncwms.metadata.LayerImpl;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidUpdateSequence;
import uk.ac.rdg.resc.ncwms.exceptions.CurrentUpdateSequence;
import uk.ac.rdg.resc.ncwms.utils.WmsUtils;
import ucar.nc2.dt.GridDataset;
import org.springframework.web.servlet.ModelAndView;
import org.joda.time.DateTime;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.HashMap;
import java.io.File;

import thredds.servlet.DataRootHandler;

/**
 * Created by IntelliJ IDEA.
 * User: pmak
 * Date: Aug 8, 2008
 * Time: 3:43:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class GetCapabilities extends FileBasedResponse
{
     private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( GetCapabilities.class );

    protected Config config;
    protected static final String FEATURE_INFO_XML_FORMAT = "text/xml";
    protected static final String FEATURE_INFO_PNG_FORMAT = "image/png";
    public static final int LAYER_LIMIT = 1;
    protected DateTime lastUpdate;

    public GetCapabilities(RequestParams _params, GridDataset _dataset, UsageLogEntry _usageLogEntry) throws Exception
    {
        super(_params, _dataset, _usageLogEntry);
    }

    public void setConfig(Config _config)
    {
        config = _config;
    }

    public void setStartupDate(long startupDate)
    {
        lastUpdate = new DateTime(startupDate);
    }

    public ModelAndView processRequest(HttpServletResponse res, HttpServletRequest req) throws Exception
    {
        String jspPage = "";
        Map<String, Object> model = new HashMap<String, Object>();

        String datasetPath = req.getPathInfo();
        File file = DataRootHandler.getInstance().getCrawlableDatasetAsFile(datasetPath);
        if ((file != null) && file.exists())
            lastUpdate = new DateTime(file.lastModified());

        //else
        //    use last startupDate's value


        if(params.getWmsVersion().equals("1.1.1"))
        {
            jspPage = "capabilities_xml_1_1_1";
        }
        else if(params.getWmsVersion().equals("1.3.0"))
        {
            jspPage = "capabilities_xml";
        }

        //check updateSequence
        String updateSeqStr = params.getString("updatesequence");
        if (updateSeqStr != null)
        {
            DateTime updateSequence;
            try
            {
                updateSequence = WmsUtils.iso8601ToDateTime(updateSeqStr);
            }
            catch (IllegalArgumentException iae)
            {
                throw new InvalidUpdateSequence(updateSeqStr +
                    " is not a valid ISO date-time");
            }
            // We use isEqual(), which compares dates based on millisecond values
            // only, because we know that the calendar system will be
            // the same in each case (ISO).  Comparisons using equals() may return false
            // because updateSequence is read using UTC, whereas lastUpdate is
            // created in the server's time zone, meaning that the Chronologies
            // are different.
            if (updateSequence.isEqual(lastUpdate))
            {
                throw new CurrentUpdateSequence(updateSeqStr);
            }
            else if (updateSequence.isAfter(lastUpdate))
            {
                throw new InvalidUpdateSequence(updateSeqStr +
                    " is later than the current server updatesequence value");
            }
        }


        model.put("config", config);
        model.put("lastUpdate", lastUpdate);
        model.put("layerLimit", LAYER_LIMIT);
        model.put("wmsBaseUrl", req.getRequestURL().toString());
        model.put("supportedImageFormats", ImageFormat.getSupportedMimeTypes());
        model.put("supportedCrsCodes", /*supportedCrsCodes); //*/HorizontalGrid.SUPPORTED_CRS_CODES);
        model.put("featureInfoFormats", new String[]{FEATURE_INFO_PNG_FORMAT,
                    FEATURE_INFO_XML_FORMAT});
        model.put("legendWidth", ColorPalette.LEGEND_WIDTH);
        model.put("dataset", dataset);
        model.put("layers", layers.values());
        
        log.info("Got layers: " + layers.size() + layers.keySet());

        model.put("legendHeight", ColorPalette.LEGEND_HEIGHT);
        model.put("paletteNames", ColorPalette.getAvailablePaletteNames());

        return new ModelAndView(jspPage, model);
    }
}
