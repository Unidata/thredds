package thredds.server.ncss.view.dsg;

import org.springframework.http.HttpHeaders;
import thredds.server.ncss.controller.NcssResponder;

/**
 * Created by cwardgar on 2014/05/20.
 */
public interface DsgSubsetWriter extends NcssResponder {
    void write() throws Exception;

    HttpHeaders getHttpHeaders(String datasetPath, boolean isStream);
}
