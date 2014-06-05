package thredds.server.ncss.view.dsg.station;

import net.opengis.waterml.x20.CollectionDocument;
import org.springframework.http.HttpHeaders;
import thredds.server.ncss.exception.NcssException;
import thredds.server.ncss.params.NcssParamsBean;
import thredds.server.ncss.util.NcssRequestUtils;
import thredds.util.ContentType;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.point.StationPointFeature;
import ucar.nc2.ogc.MarshallingUtil;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by cwardgar on 2014/06/04.
 */
public class StationSubsetWriterWaterML extends AbstractStationSubsetWriter {
    private final OutputStream out;
    private final CollectionDocument collectionDoc;

    public StationSubsetWriterWaterML(FeatureDatasetPoint fdPoint, NcssParamsBean ncssParams, OutputStream out)
            throws XMLStreamException, NcssException, IOException {
        super(fdPoint, ncssParams);

        this.out = out;
        this.collectionDoc = CollectionDocument.Factory.newInstance();
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

    }


    @Override
    public void writeHeader() throws Exception {

    }

    @Override
    public void writePoint(StationPointFeature stationPointFeat) throws Exception {

    }

    @Override
    public void writeFooter() throws Exception {
        MarshallingUtil.writeObject(collectionDoc, out, true);
        out.flush();
    }
}
