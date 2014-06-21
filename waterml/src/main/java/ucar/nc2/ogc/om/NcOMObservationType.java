package ucar.nc2.ogc.om;

import net.opengis.om.x20.OMObservationType;
import net.opengis.waterml.x20.MeasurementTimeseriesDocument;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ogc.MarshallingUtil;
import ucar.nc2.ogc.gml.NcFeaturePropertyType;
import ucar.nc2.ogc.gml.NcReferenceType;
import ucar.nc2.ogc.gml.NcTimeInstantPropertyType;
import ucar.nc2.ogc.waterml.NcMeasurementTimeseriesType;

import java.io.IOException;

/**
 * Created by cwardgar on 2014/02/26.
 */
public abstract class NcOMObservationType {
    // wml2:Collection/wml2:observationMember/om:OM_Observation
    public static OMObservationType initOmObservation(OMObservationType omObservation,
            StationTimeSeriesFeature stationFeat, VariableSimpleIF dataVar) throws IOException {
        // @gml:id
        String id = MarshallingUtil.createIdForType(OMObservationType.class);
        omObservation.setId(id);

        // om:phenomenonTime
        NcTimeObjectPropertyType.initPhenomenonTime(omObservation.addNewPhenomenonTime(), stationFeat);

        // om:resultTime
        NcTimeInstantPropertyType.initResultTime(omObservation.addNewResultTime());

        // om:observedProperty
        NcReferenceType.initObservedProperty(omObservation.addNewObservedProperty(), dataVar);

        // om:procedure
        NcOMProcessPropertyType.initProcedure(omObservation.addNewProcedure());

        // om:featureOfInterest
        NcFeaturePropertyType.initFeatureOfInterest(omObservation.addNewFeatureOfInterest(), stationFeat);

        // om:result
        MeasurementTimeseriesDocument measurementTimeseriesDoc = MeasurementTimeseriesDocument.Factory.newInstance();
        NcMeasurementTimeseriesType.initMeasurementTimeseries(
                measurementTimeseriesDoc.addNewMeasurementTimeseries(), stationFeat, dataVar);
        omObservation.setResult(measurementTimeseriesDoc);

        return omObservation;
    }

    private NcOMObservationType() { }
}
