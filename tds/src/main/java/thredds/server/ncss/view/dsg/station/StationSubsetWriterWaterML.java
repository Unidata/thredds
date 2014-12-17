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
import ucar.nc2.ft.point.StationPointFeature;
import ucar.nc2.ogc.MarshallingUtil;
import ucar.nc2.ogc.om.NcOMObservationPropertyType;
import ucar.nc2.ogc.waterml.NcDocumentMetadataPropertyType;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by cwardgar on 2014/06/04.
 */
public class StationSubsetWriterWaterML extends AbstractStationSubsetWriter {
    private final OutputStream out;
    private final CollectionDocument collectionDoc;
    private final CollectionType collection;

    public StationSubsetWriterWaterML(FeatureDatasetPoint fdPoint, NcssParamsBean ncssParams, OutputStream out)
            throws XMLStreamException, NcssException, IOException {
        super(fdPoint, ncssParams);

        this.out = out;
        this.collectionDoc = CollectionDocument.Factory.newInstance();
        this.collection = collectionDoc.addNewCollection();
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
    protected void writeHeader(StationPointFeature stationPointFeat) throws Exception {
        MarshallingUtil.resetIds();

        // @gml:id
        String id = MarshallingUtil.createIdForType(CollectionType.class);
        collection.setId(id);

        // wml2:metadata
        NcDocumentMetadataPropertyType.initMetadata(collection.addNewMetadata());
    }

    @Override
    protected void writeStationTimeSeriesFeature(StationTimeSeriesFeature stationFeat) throws Exception {
        if (!headerDone) {
            writeHeader(null);
            headerDone = true;
        }

        for (VariableSimpleIF wantedVar : wantedVariables) {
            // wml2:observationMember
            NcOMObservationPropertyType.initObservationMember(
                    collection.addNewObservationMember(), stationFeat, wantedVar);
        }
    }

    @Override
    protected void writeStationPointFeature(StationPointFeature stationPointFeat) throws Exception {
        throw new UnsupportedOperationException("Method not used in " + getClass());
    }

    @Override
    protected void writeFooter() throws Exception {
        MarshallingUtil.writeObject(collectionDoc, out, true);
        out.flush();
    }
}
