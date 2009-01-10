package thredds.server.wms.responses;

import uk.ac.rdg.resc.ncwms.controller.RequestParams;
import uk.ac.rdg.resc.ncwms.metadata.LayerImpl;
import uk.ac.rdg.resc.ncwms.datareader.DataReader;
import uk.ac.rdg.resc.ncwms.usagelog.UsageLogEntry;
import ucar.nc2.dt.GridDataset;

import java.util.Map;

import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * Created by IntelliJ IDEA.
 * User: pmak
 * Date: Aug 11, 2008
 * Time: 11:39:48 PM
 * To change this template use File | Settings | File Templates.
 */
abstract public class FileBasedResponse
{
    private static org.slf4j.Logger log =
              org.slf4j.LoggerFactory.getLogger( FileBasedResponse.class );

    protected RequestParams params;
    protected GridDataset dataset;
    protected Map<String, LayerImpl> layers;
    protected LayerImpl layer;
    protected DataReader reader;
    protected UsageLogEntry usageLogEntry;

    public FileBasedResponse(RequestParams _params, GridDataset _dataset, UsageLogEntry _usageLogEntry) throws Exception
    {
       params = _params;
       dataset = _dataset;
       usageLogEntry = _usageLogEntry;
       reader = DataReader.getDataReader("uk.ac.rdg.resc.ncwms.datareader.DefaultDataReader",
               dataset.getLocationURI());

       layers = reader.getAllLayers(dataset);
    }

    abstract public ModelAndView processRequest(HttpServletResponse res, HttpServletRequest req) throws Exception;
    
}
