package thredds.server.ncss.view.dsg.station;

import org.springframework.http.HttpHeaders;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ft.point.StationPointFeature;

import java.io.OutputStream;
import java.util.List;

/**
 * Created by cwardgar on 2014/05/29.
 */
public class StationSubsetWriterNetcdf extends AbstractStationSubsetWriter {
    private final OutputStream out;
    private final NetcdfFileWriter.Version version;

    public StationSubsetWriterNetcdf(OutputStream out, NetcdfFileWriter.Version version) {
        this.out = out;
        this.version = version;
    }

    @Override
    public HttpHeaders getHttpHeaders(String datasetPath, boolean isStream) {
        return null;
    }

    @Override
    public void writeHeader(List<VariableSimpleIF> wantedVariables) throws Exception {

    }

    @Override
    public void writePoint(StationPointFeature stationPointFeat, List<VariableSimpleIF> wantedVariables) throws Exception {

    }

    @Override
    public void writeFooter() throws Exception {

    }
}
