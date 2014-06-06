package thredds.server.ncss.view.dsg.station;

import net.opengis.waterml.x20.CollectionDocument;
import net.opengis.waterml.x20.CollectionType;
import org.springframework.http.HttpHeaders;
import thredds.server.ncss.exception.NcssException;
import thredds.server.ncss.params.NcssParamsBean;
import thredds.server.ncss.util.NcssRequestUtils;
import thredds.util.ContentType;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ft.StationTimeSeriesFeatureCollection;
import ucar.nc2.ft.point.StationPointFeature;
import ucar.nc2.ogc.MarshallingUtil;
import ucar.nc2.ogc.waterml.NcDocumentMetadataPropertyType;
import ucar.nc2.units.DateType;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by cwardgar on 2014/06/04.
 */
public class StationSubsetWriterWaterML extends AbstractStationSubsetWriter {
    private final OutputStream out;
//    private final CollectionDocument collectionDoc;

    public StationSubsetWriterWaterML(FeatureDatasetPoint fdPoint, NcssParamsBean ncssParams, OutputStream out)
            throws XMLStreamException, NcssException, IOException {
        super(fdPoint, ncssParams);

        this.out = out;
//        this.collectionDoc = CollectionDocument.Factory.newInstance();
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
    public void write() throws Exception {
        MarshallingUtil.resetIds();



        CollectionDocument collectionDoc = CollectionDocument.Factory.newInstance();
        CollectionType collection = collectionDoc.addNewCollection();

        // @gml:id
        String id = MarshallingUtil.createIdForType(CollectionType.class);
        collection.setId(id);

        // wml2:metadata
        NcDocumentMetadataPropertyType.initMetadata(collection.addNewMetadata());

        // wml2:observationMember[0..*]
        // Perform spatial subset.
        StationTimeSeriesFeatureCollection subsettedStationFeatCol = stationFeatureCollection.subset(wantedStations);
        try {
            while (subsettedStationFeatCol.hasNext()) {
                StationTimeSeriesFeature stationFeat = subsettedStationFeatCol.next();

                // Perform temporal subset. We do this even when a time instant is specified, in which case wantedRange
                // represents a sanity check (i.e. "give me the feature closest to the specified time, but it must at
                // least be within an hour").
                StationTimeSeriesFeature subsettedStationFeat = stationFeat.subset(wantedRange);

                for (VariableSimpleIF wantedVar : wantedVariables) {
                    if (ncssParams.getTime() != null) {
                        DateType wantedDateType = new DateType(ncssParams.getTime(), null, null);  // Parse time string.
                        long wantedTime = wantedDateType.getCalendarDate().getMillis();
                        writePointWithClosestTime(subsettedStationFeat, wantedTime);
                    } else {
                        writeAllPoints(subsettedStationFeat);
                    }
                }

                if (ncssParams.getTime() != null) {
                    DateType wantedDateType = new DateType(ncssParams.getTime(), null, null);  // Parse time string.
                    long wantedTime = wantedDateType.getCalendarDate().getMillis();
                    writePointWithClosestTime(subsettedStationFeat, wantedTime);
                } else {
                    writeAllPoints(subsettedStationFeat);
                }

//                for (VariableSimpleIF wantedVar : wantedVariables) {
//                    stationFeat.resetIteration();
//                    try {
//                        // wml2:observationMember
//                        NcOMObservationPropertyType.initObservationMember(
//                                collection.addNewObservationMember(), stationFeat, wantedVar);
//                    } finally {
//                        stationFeat.finish();
//                    }
//                }
            }
        } finally {
            subsettedStationFeatCol.finish();
        }

        MarshallingUtil.writeObject(collectionDoc, out, true);
        out.flush();
    }


    @Override
    public void writeHeader() throws Exception {

    }

    @Override
    public void writePoint(StationPointFeature stationPointFeat) throws Exception {

    }

    @Override
    public void writeFooter() throws Exception {

    }
}
