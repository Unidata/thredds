package ucar.nc2.ogc2.om;

import net.opengis.om.x20.OMObservationPropertyType;
import net.opengis.om.x20.OMObservationType;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ft.StationTimeSeriesFeature;

import java.io.IOException;

/**
 * Created by cwardgar on 2014/03/13.
 */
public abstract class NC_OMObservationPropertyType {
    // wml2:Collection/wml2:observationMember
    public static OMObservationPropertyType createObservationMember(
            StationTimeSeriesFeature stationFeat, VariableSimpleIF dataVar)
            throws IOException {
        OMObservationPropertyType observationMember = OMObservationPropertyType.Factory.newInstance();

        // om:OM_Observation
        OMObservationType omObservation = NC_OMObservationType.createOmObservation(stationFeat, dataVar);
        observationMember.setOMObservation(omObservation);

        return observationMember;
    }

    public static OMObservationPropertyType initObservationMember(OMObservationPropertyType observationMember,
            StationTimeSeriesFeature stationFeat, VariableSimpleIF dataVar) throws IOException {
        // om:OM_Observation
        NC_OMObservationType.initOmObservation(observationMember.addNewOMObservation(), stationFeat, dataVar);

        return observationMember;
    }

    private NC_OMObservationPropertyType() { }
}
