package thredds.server.ncss.view.dsg.point;

import thredds.server.ncss.exception.NcssException;
import thredds.server.ncss.params.NcssParamsBean;
import thredds.server.ncss.view.dsg.AbstractDsgSubsetWriter;
import ucar.nc2.ft.*;

import java.io.IOException;
import java.util.List;

/**
 * Created by cwardgar on 2014/06/02.
 */
public abstract class AbstractPointSubsetWriter extends AbstractDsgSubsetWriter {
    protected final PointFeatureCollection pointFeatureCollection;

    public AbstractPointSubsetWriter(FeatureDatasetPoint fdPoint, NcssParamsBean ncssParams)
            throws NcssException, IOException {
        super(fdPoint, ncssParams);

        List<FeatureCollection> featColList = fdPoint.getPointFeatureCollectionList();
        assert featColList.size() == 1 : "Is there ever a case when this is NOT 1?";
        assert featColList.get(0) instanceof PointFeatureCollection :
                "This class only deals with PointFeatureCollections.";

        this.pointFeatureCollection = (PointFeatureCollection) featColList.get(0);
    }

    public abstract void writeHeader() throws Exception;

    public abstract void writePoint(PointFeature pointFeat) throws Exception;

    public abstract void writeFooter() throws Exception;

    @Override
    public void write() throws Exception {
        writeHeader();

        // Perform spatial and temporal subset.
        PointFeatureCollection subsettedPointFeatColl =
                pointFeatureCollection.subset(ncssParams.getBoundingBox(), wantedRange);

        subsettedPointFeatColl.resetIteration();
        try {
            while (subsettedPointFeatColl.hasNext()) {
                PointFeature pointFeat = subsettedPointFeatColl.next();
                writePoint(pointFeat);
            }
        } finally {
            subsettedPointFeatColl.finish();
        }

        writeFooter();
    }
}
