package thredds.server.ncss.view.dsg;

import org.springframework.http.HttpHeaders;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ft.PointFeature;

import java.io.IOException;
import java.util.List;

/**
 * Created by cwardgar on 2014-05-24.
 */
public class StationSubsetWriterCSV extends AbstractStationSubsetWriter {
    @Override
    public HttpHeaders getHttpHeaders(String datasetPath) {
        return null;
    }

    @Override
    public void writeHeader() throws IOException {

    }

    @Override
    public void writePoint(PointFeature pf, List<VariableSimpleIF> wantedVariables) throws IOException {

    }

    @Override
    public void writeFooter() throws IOException {

    }
}
