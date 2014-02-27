package ucar.nc2.ogc.gml;

import net.opengis.gml.v_3_2_1.FeaturePropertyType;
import net.opengis.waterml.v_2_0_1.MonitoringPointType;
import net.opengis.waterml.v_2_0_1.ObjectFactory;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ogc.waterml.NC_MonitoringPointType;

import javax.xml.bind.JAXBElement;

/**
 * In OGC 12-031r2, used at:
 *     om:OM_Observation/om:featureOfInterest
 *
 * Created by cwardgar on 2014/02/26.
 */
public class NC_FeaturePropertyType extends FeaturePropertyType {
    public NC_FeaturePropertyType(StationTimeSeriesFeature stationFeat) {
        // om:OM_Observation/om:featureOfInterest/sam:SF_SamplingFeatureType
        MonitoringPointType monitoringPointType = new NC_MonitoringPointType(stationFeat);
        JAXBElement<MonitoringPointType> monitoringPointElem =
                new ObjectFactory().createMonitoringPoint(monitoringPointType);
        setAbstractFeature(monitoringPointElem);
    }
}
