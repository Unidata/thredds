package thredds.server.ncss.view.dsg;

import org.springframework.http.HttpHeaders;
import thredds.server.ncss.util.NcssRequestUtils;
import thredds.util.ContentType;
import ucar.ma2.Array;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ft.point.StationPointFeature;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.unidata.geoloc.Station;
import ucar.unidata.util.Format;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;

/**
 * Created by cwardgar on 2014-05-24.
 */
public class StationSubsetWriterCSV extends AbstractStationSubsetWriter {
    private final PrintWriter writer;
    private final boolean isStream;

    public StationSubsetWriterCSV(OutputStream out, boolean isStream) {
        this.writer = new PrintWriter(out);
        this.isStream = isStream;
    }

    @Override
    public HttpHeaders getHttpHeaders(String datasetPath) {
        HttpHeaders httpHeaders = new HttpHeaders();

        if (!isStream) {
            httpHeaders.set("Content-Location", datasetPath);
            httpHeaders.set("Content-Disposition",
                    "attachment; filename=\"" + NcssRequestUtils.nameFromPathInfo(datasetPath) + ".csv\"");
            httpHeaders.add(ContentType.HEADER, ContentType.csv.getContentHeader());
        } else {
            httpHeaders.add(ContentType.HEADER, ContentType.text.getContentHeader());
        }

        return httpHeaders;
    }

    @Override
    public void writeHeader(List<VariableSimpleIF> wantedVariables) throws IOException {
        writer.print("time,station,latitude[unit=\"degrees_north\"],longitude[unit=\"degrees_east\"]");
        for (VariableSimpleIF wantedVar : wantedVariables) {
            writer.print(",");
            writer.print(wantedVar.getShortName());
            if (wantedVar.getUnitsString() != null)
                writer.print("[unit=\"" + wantedVar.getUnitsString() + "\"]");
        }
        writer.println();
    }

    @Override
    public void writePoint(StationPointFeature stationPointFeat, List<VariableSimpleIF> wantedVariables)
            throws IOException {
        Station station = stationPointFeat.getStation();

        writer.print(CalendarDateFormatter.toDateTimeString(stationPointFeat.getObservationTimeAsCalendarDate()));
        writer.print(',');
        writer.print(station.getName());
        writer.print(',');
        writer.print(Format.dfrac(station.getLatitude(), 3));
        writer.print(',');
        writer.print(Format.dfrac(station.getLongitude(), 3));

        for (VariableSimpleIF wantedVar : wantedVariables) {
            writer.print(',');
            Array dataArray = stationPointFeat.getData().getArray(wantedVar.getShortName());
            writer.print(dataArray.toString());
        }
        writer.println();
    }

    @Override
    public void writeFooter() throws IOException {
        writer.flush();
    }
}
