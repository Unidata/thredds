package ucar.nc2.ogc.gml;

import net.opengis.gml.v_3_2_1.FeaturePropertyType;
import net.opengis.waterml.v_2_0_1.MonitoringPointType;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ogc.waterml.NC_MonitoringPointType;

import javax.xml.bind.JAXBElement;

/**
 * In OGC 12-031r2, used at:
 *     om:OM_Observation/om:featureOfInterest
 *
 * Created by cwardgar on 2014/02/26.
 */
public abstract class NC_FeaturePropertyType {
    private final static net.opengis.gml.v_3_2_1.ObjectFactory gmlObjectFactory =
            new net.opengis.gml.v_3_2_1.ObjectFactory();
    private final static net.opengis.waterml.v_2_0_1.ObjectFactory watermlObjectFactory =
            new net.opengis.waterml.v_2_0_1.ObjectFactory();

    public static FeaturePropertyType createFeatureOfInterest(StationTimeSeriesFeature stationFeat) {
        FeaturePropertyType featOfInterest = gmlObjectFactory.createFeaturePropertyType();

        // om:OM_Observation/om:featureOfInterest/sam:SF_SamplingFeatureType
        MonitoringPointType monitoringPointType = NC_MonitoringPointType.createMonitoringPointType(stationFeat);
        JAXBElement<MonitoringPointType> monitoringPointElem =
                watermlObjectFactory.createMonitoringPoint(monitoringPointType);
        featOfInterest.setAbstractFeature(monitoringPointElem);

        return featOfInterest;
    }

    private NC_FeaturePropertyType() { }
}
