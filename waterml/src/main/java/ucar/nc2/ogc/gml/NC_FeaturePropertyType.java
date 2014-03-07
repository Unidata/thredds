package ucar.nc2.ogc.gml;

import net.opengis.gml.v_3_2_1.FeaturePropertyType;
import net.opengis.waterml.v_2_0_1.MonitoringPointType;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ogc.Factories;
import ucar.nc2.ogc.waterml.NC_MonitoringPointType;

import javax.xml.bind.JAXBElement;

/**
 * Created by cwardgar on 2014/02/26.
 */
public abstract class NC_FeaturePropertyType {
    // om:OM_Observation/om:featureOfInterest
    public static FeaturePropertyType createFeatureOfInterest(StationTimeSeriesFeature stationFeat) {
        FeaturePropertyType featOfInterest = Factories.GML.createFeaturePropertyType();

        // sam:SF_SamplingFeatureType
        MonitoringPointType monitoringPointType = NC_MonitoringPointType.createMonitoringPointType(stationFeat);
        JAXBElement<MonitoringPointType> monitoringPointElem =
                Factories.WATERML.createMonitoringPoint(monitoringPointType);
        featOfInterest.setAbstractFeature(monitoringPointElem);

        return featOfInterest;
    }

    private NC_FeaturePropertyType() { }
}
