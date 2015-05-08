package thredds.server.ncss.view.dsg;

import org.springframework.http.HttpHeaders;
import thredds.server.ncss.format.SupportedFormat;
import thredds.server.ncss.params.NcssParamsBean;
import ucar.nc2.ft.FeatureDataset;

import javax.servlet.http.HttpServletResponse;

/**
 * Created by cwardgar on 2014/05/20.
 */
public interface DsgSubsetWriter {
    void write() throws Exception;

    void respond(HttpServletResponse res, FeatureDataset ft, String requestPathInfo, NcssParamsBean queryParams,
             SupportedFormat format) throws Exception;

   	HttpHeaders getResponseHeaders(FeatureDataset fd, SupportedFormat format, String datasetPath);

    HttpHeaders getHttpHeaders(String datasetPath, boolean isStream);
}
