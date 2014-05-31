package thredds.server.ncss.view.dsg;

import org.springframework.http.HttpHeaders;

/**
 * Created by cwardgar on 2014/05/20.
 */
public interface DsgSubsetWriter {
    void write() throws Exception;

    HttpHeaders getHttpHeaders(String datasetPath, boolean isStream);
}
