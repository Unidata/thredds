package thredds.server.ncss.view.dsg;

import org.springframework.http.HttpHeaders;
import thredds.server.ncss.exception.NcssException;

import java.io.IOException;
import java.text.ParseException;

/**
 * Created by cwardgar on 2014/05/20.
 */
public interface DsgWriter {
    void write() throws ParseException, IOException, NcssException;

    HttpHeaders getHttpHeaders(String datasetPath);
}
