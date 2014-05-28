package thredds.server.ncss.view.dsg;

import org.springframework.http.HttpHeaders;
import thredds.server.ncss.params.NcssParamsBean;
import ucar.nc2.ft.FeatureDatasetPoint;

/**
 * Created by cwardgar on 2014/05/20.
 */
public interface DsgSubsetWriter {
    void write(FeatureDatasetPoint fdPoint, NcssParamsBean ncssParams, ucar.nc2.util.DiskCache2 diskCache)
            throws Exception;

    HttpHeaders getHttpHeaders(String datasetPath);
}
