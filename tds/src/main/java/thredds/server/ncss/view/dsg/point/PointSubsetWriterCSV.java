/*
 * (c) 1998-2016 University Corporation for Atmospheric Research/Unidata
 */

package thredds.server.ncss.view.dsg.point;

import org.springframework.http.HttpHeaders;

import thredds.server.ncss.exception.NcssException;
import thredds.server.ncss.params.NcssParamsBean;
import thredds.server.ncss.util.NcssRequestUtils;
import thredds.util.ContentType;
import ucar.ma2.Array;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.CDM;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.unidata.geoloc.EarthLocation;
import ucar.unidata.util.Format;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * Created by cwardgar on 2014/06/02.
 */
public class PointSubsetWriterCSV extends AbstractPointSubsetWriter {
    final protected PrintWriter writer;

    public PointSubsetWriterCSV(FeatureDatasetPoint fdPoint, NcssParamsBean ncssParams, OutputStream out)
            throws NcssException, IOException {
        super(fdPoint, ncssParams);
        this.writer = new PrintWriter(new OutputStreamWriter(out, CDM.utf8Charset));
    }

    @Override
    public HttpHeaders getHttpHeaders(String datasetPath, boolean isStream) {
        HttpHeaders httpHeaders = new HttpHeaders();

        if (!isStream) {
            httpHeaders.set("Content-Location", datasetPath);
            String fileName = NcssRequestUtils.getFileNameForResponse(datasetPath, ".csv");
            httpHeaders.set("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            httpHeaders.add(ContentType.HEADER, ContentType.csv.getContentHeader());
        } else {
            // The problem is that the browser won't display text/csv inline.
            httpHeaders.add(ContentType.HEADER, ContentType.text.getContentHeader());
        }

        return httpHeaders;
    }

    @Override
    public void writeHeader(PointFeature pf) throws Exception {
        writer.print("time,latitude[unit=\"degrees_north\"],longitude[unit=\"degrees_east\"]");
        for (VariableSimpleIF wantedVar : wantedVariables) {
            writer.print(",");
            writer.print(wantedVar.getShortName());
            if (wantedVar.getUnitsString() != null)
                writer.print("[unit=\"" + wantedVar.getUnitsString() + "\"]");
        }
        writer.println();
    }

    @Override
    public void writePoint(PointFeature pointFeat) throws Exception {
        EarthLocation loc = pointFeat.getLocation();

        writer.print(CalendarDateFormatter.toDateTimeStringISO(pointFeat.getObservationTimeAsCalendarDate()));
        writer.print(',');
        writer.print(Format.dfrac(loc.getLatitude(), 3));
        writer.print(',');
        writer.print(Format.dfrac(loc.getLongitude(), 3));

        for (VariableSimpleIF wantedVar : wantedVariables) {
            writer.print(',');
            Array dataArray = pointFeat.getData().getArray(wantedVar.getShortName());
            writer.print(dataArray.toString().trim());
        }
        writer.println();
    }

    @Override
    public void writeFooter() throws Exception {
        writer.flush();
    }
}
