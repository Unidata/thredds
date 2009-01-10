package thredds.server.wms.responses;

import uk.ac.rdg.resc.ncwms.controller.RequestParams;
import uk.ac.rdg.resc.ncwms.usagelog.UsageLogEntry;
import uk.ac.rdg.resc.ncwms.graphics.ImageFormat;
import uk.ac.rdg.resc.ncwms.datareader.HorizontalGrid;
import uk.ac.rdg.resc.ncwms.datareader.DataReader;
import uk.ac.rdg.resc.ncwms.styles.ColorPalette;
import uk.ac.rdg.resc.ncwms.config.Config;
import uk.ac.rdg.resc.ncwms.metadata.LayerImpl;
import ucar.nc2.dt.GridDataset;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.HashMap;

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

    public GetCapabilities(RequestParams _params, GridDataset _dataset, UsageLogEntry _usageLogEntry) throws Exception
    {
        super(_params, _dataset, _usageLogEntry);
    }

    public void setConfig(Config _config)
    {
        config = _config;
    }

    public ModelAndView processRequest(HttpServletResponse res, HttpServletRequest req) throws Exception
    {
        String jspPage = "";
        Map<String, Object> model = new HashMap<String, Object>();

        if(params.getWmsVersion().equals("1.1.1"))
        {
            jspPage = "capabilities_xml_1_1_1";
        }
        else if(params.getWmsVersion().equals("1.3.0"))
        {
            jspPage = "capabilities_xml";
        }

        model.put("config", config);
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
