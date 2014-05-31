package thredds.server.ncss.view.dsg.station;

import org.springframework.http.HttpHeaders;
import thredds.server.ncss.exception.NcssException;
import thredds.server.ncss.params.NcssParamsBean;
import thredds.server.ncss.util.NcssRequestUtils;
import thredds.util.ContentType;
import ucar.ma2.Array;
import ucar.ma2.StructureData;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.CDM;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.point.StationPointFeature;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.unidata.geoloc.Station;
import ucar.unidata.util.Format;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by cwardgar on 2014/05/27.
 */
public class StationSubsetWriterXML extends AbstractStationSubsetWriter {
    private final XMLStreamWriter staxWriter;

    public StationSubsetWriterXML(FeatureDatasetPoint fdPoint, NcssParamsBean ncssParams, OutputStream out)
            throws XMLStreamException, NcssException, IOException {
        super(fdPoint, ncssParams);

        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        staxWriter = factory.createXMLStreamWriter(out);
    }

    @Override
    public HttpHeaders getHttpHeaders(String datasetPath, boolean isStream) {
        HttpHeaders httpHeaders = new HttpHeaders();

        if (!isStream) {
            httpHeaders.set("Content-Location", datasetPath);
            httpHeaders.set("Content-Disposition",
                    "attachment; filename=\"" + NcssRequestUtils.nameFromPathInfo(datasetPath) + ".xml\"");
        }

        httpHeaders.set(ContentType.HEADER, ContentType.xml.getContentHeader());
        return httpHeaders;
    }

    @Override
    public void writeHeader() throws XMLStreamException {
        staxWriter.writeStartDocument("UTF-8", "1.0");
        staxWriter.writeCharacters("\n");
        staxWriter.writeStartElement("stationFeatureCollection");
        staxWriter.writeCharacters("\n ");
    }

    @Override
    public void writePoint(StationPointFeature stationPointFeat) throws XMLStreamException, IOException {
        Station station = stationPointFeat.getStation();

        staxWriter.writeStartElement("pointFeature");
        staxWriter.writeAttribute("date",
                CalendarDateFormatter.toDateTimeString(stationPointFeat.getObservationTimeAsCalendarDate()));
        staxWriter.writeCharacters("\n  ");

        staxWriter.writeStartElement("station");
        staxWriter.writeAttribute("name", station.getName());
        staxWriter.writeAttribute("latitude", Format.dfrac(station.getLatitude(), 3));
        staxWriter.writeAttribute("longitude", Format.dfrac(station.getLongitude(), 3));
        if (!Double.isNaN(station.getAltitude()))
            staxWriter.writeAttribute("altitude", Format.dfrac(station.getAltitude(), 0));
        if (station.getDescription() != null)
            staxWriter.writeCharacters(station.getDescription());
        staxWriter.writeEndElement();
        staxWriter.writeCharacters("\n ");

        for (VariableSimpleIF wantedVar : wantedVariables) {
            staxWriter.writeCharacters(" ");
            staxWriter.writeStartElement("data");
            staxWriter.writeAttribute("name", wantedVar.getShortName());
            if (wantedVar.getUnitsString() != null)
                staxWriter.writeAttribute(CDM.UNITS, wantedVar.getUnitsString());

            Array dataArray = stationPointFeat.getData().getArray(wantedVar.getShortName());
            String ss = dataArray.toString();
            Class elemType = dataArray.getElementType();
            if ((elemType == String.class) || (elemType == char.class) || (elemType == StructureData.class))
                ss = ucar.nc2.util.xml.Parse.cleanCharacterData(ss); // make sure no bad chars
            staxWriter.writeCharacters(ss);
            staxWriter.writeEndElement();
            staxWriter.writeCharacters("\n ");
        }
        staxWriter.writeEndElement();
        staxWriter.writeCharacters("\n");
    }

    @Override
    public void writeFooter() throws XMLStreamException {
        staxWriter.writeEndElement();
        staxWriter.writeCharacters("\n");
        staxWriter.writeEndDocument();

        staxWriter.close();  // This should flush the writer. The underlying output stream remains open.
    }
}
