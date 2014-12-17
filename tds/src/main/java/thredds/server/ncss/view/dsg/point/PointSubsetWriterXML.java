package thredds.server.ncss.view.dsg.point;

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
import ucar.nc2.ft.PointFeature;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.unidata.geoloc.EarthLocation;
import ucar.unidata.util.Format;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by cwardgar on 2014/06/03.
 */
public class PointSubsetWriterXML extends AbstractPointSubsetWriter {
    private final XMLStreamWriter staxWriter;

    public PointSubsetWriterXML(FeatureDatasetPoint fdPoint, NcssParamsBean ncssParams, OutputStream out)
            throws XMLStreamException, NcssException, IOException {
        super(fdPoint, ncssParams);

        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        staxWriter = factory.createXMLStreamWriter(out, "UTF-8");
    }

    @Override
    public HttpHeaders getHttpHeaders(String datasetPath, boolean isStream) {
        HttpHeaders httpHeaders = new HttpHeaders();

        if (!isStream) {
            httpHeaders.set("Content-Location", datasetPath);
            String fileName = NcssRequestUtils.getFileNameForResponse(datasetPath, ".xml");
            httpHeaders.set("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        }

        httpHeaders.set(ContentType.HEADER, ContentType.xml.getContentHeader());
        return httpHeaders;
    }

    @Override
    public void writeHeader(PointFeature pf) throws XMLStreamException {
        staxWriter.writeStartDocument("UTF-8", "1.0");
        staxWriter.writeCharacters("\n");
        staxWriter.writeStartElement("pointFeatureCollection");
    }

    @Override
    public void writePoint(PointFeature pointFeat) throws Exception {
        EarthLocation loc = pointFeat.getLocation();

        staxWriter.writeCharacters("\n    ");
        staxWriter.writeStartElement("pointFeature");
        staxWriter.writeAttribute("date",
                CalendarDateFormatter.toDateTimeString(pointFeat.getObservationTimeAsCalendarDate()));

        staxWriter.writeCharacters("\n        ");
        staxWriter.writeEmptyElement("location");
        staxWriter.writeAttribute("latitude", Format.dfrac(loc.getLatitude(), 3));
        staxWriter.writeAttribute("longitude", Format.dfrac(loc.getLongitude(), 3));
        if (!Double.isNaN(loc.getAltitude()))
            staxWriter.writeAttribute("altitude", Format.dfrac(loc.getAltitude(), 0));

        for (VariableSimpleIF wantedVar : wantedVariables) {
            staxWriter.writeCharacters("\n        ");
            staxWriter.writeStartElement("data");
            staxWriter.writeAttribute("name", wantedVar.getShortName());
            if (wantedVar.getUnitsString() != null)
                staxWriter.writeAttribute(CDM.UNITS, wantedVar.getUnitsString());

            Array dataArray = pointFeat.getData().getArray(wantedVar.getShortName());
            String ss = dataArray.toString();
            Class elemType = dataArray.getElementType();
            if ((elemType == String.class) || (elemType == char.class) || (elemType == StructureData.class))
                ss = ucar.nc2.util.xml.Parse.cleanCharacterData(ss); // make sure no bad chars
            staxWriter.writeCharacters(ss.trim());
            staxWriter.writeEndElement();
        }

        staxWriter.writeCharacters("\n    ");
        staxWriter.writeEndElement();
    }

    @Override
    public void writeFooter() throws XMLStreamException {
        staxWriter.writeCharacters("\n");
        staxWriter.writeEndElement();
        staxWriter.writeCharacters("\n");
        staxWriter.writeEndDocument();

        staxWriter.close();  // This should flush the writer. The underlying output stream remains open.
    }
}
