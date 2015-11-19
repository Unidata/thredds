/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.server.ncss.view.dsg.station;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import org.springframework.http.HttpHeaders;
import thredds.server.ncss.exception.NcssException;
import thredds.util.ContentType;
import thredds.util.TdsPathUtils;
import ucar.ma2.Array;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.CDM;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.point.StationPointFeature;
import ucar.nc2.ft2.coverage.SubsetParams;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.unidata.geoloc.Station;
import ucar.unidata.util.Format;

/**
 * Created by cwardgar on 2014-05-24.
 */
public class StationSubsetWriterCSV extends AbstractStationSubsetWriter {
    private final PrintWriter writer;

    public StationSubsetWriterCSV(FeatureDatasetPoint fdPoint, SubsetParams ncssParams, OutputStream out)
            throws NcssException, IOException {
        super(fdPoint, ncssParams);
      this.writer = new PrintWriter(new OutputStreamWriter(out, CDM.utf8Charset));
    }

    @Override
    public HttpHeaders getHttpHeaders(String datasetPath, boolean isStream) {
        HttpHeaders httpHeaders = new HttpHeaders();

        if (!isStream) {
            httpHeaders.set("Content-Location", datasetPath);
            String fileName = TdsPathUtils.getFileNameForResponse(datasetPath, ".csv");
            httpHeaders.set("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            httpHeaders.add(ContentType.HEADER, ContentType.csv.getContentHeader());
        } else {
            // The problem is that the browser won't display text/csv inline.
            httpHeaders.add(ContentType.HEADER, ContentType.text.getContentHeader());
        }

        return httpHeaders;
    }

    @Override
    protected void writeHeader(StationPointFeature stationPointFeat) throws IOException {
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
    protected void writeStationPointFeature(StationPointFeature stationPointFeat) throws IOException {
        Station station = stationPointFeat.getStation();

        writer.print(CalendarDateFormatter.toDateTimeStringISO(stationPointFeat.getObservationTimeAsCalendarDate()));
        writer.print(',');
        writer.print(station.getName());
        writer.print(',');
        writer.print(Format.dfrac(station.getLatitude(), 3));
        writer.print(',');
        writer.print(Format.dfrac(station.getLongitude(), 3));

        for (VariableSimpleIF wantedVar : wantedVariables) {
            writer.print(',');
            Array dataArray = stationPointFeat.getDataAll().getArray(wantedVar.getShortName());
            writer.print(dataArray.toString().trim());
        }
        writer.println();
    }

    @Override
    protected void writeFooter() throws IOException {
        writer.flush();
    }
}
