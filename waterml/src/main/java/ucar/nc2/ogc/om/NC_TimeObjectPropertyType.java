package ucar.nc2.ogc.om;

import net.opengis.gml.v_3_2_1.TimePeriodType;
import net.opengis.om.v_2_0_0.TimeObjectPropertyType;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ogc.Factories;
import ucar.nc2.ogc.gml.NC_TimePeriodType;

import javax.xml.bind.JAXBElement;

/**
 * Created by cwardgar on 3/6/14.
 */
public abstract class NC_TimeObjectPropertyType {
    // om:OM_Observation/om:phenomenonTime
    public static TimeObjectPropertyType createPhenomenonTime(StationTimeSeriesFeature stationFeat) {
        TimeObjectPropertyType phenomenonTime = Factories.OM.createTimeObjectPropertyType();

        // gml:TimePeriod
        TimePeriodType timePeriod = NC_TimePeriodType.createTimePeriod(stationFeat);
        JAXBElement<TimePeriodType> timePeriodElem = Factories.GML.createTimePeriod(timePeriod);
        phenomenonTime.setAbstractTimeObject(timePeriodElem);

        return phenomenonTime;
    }

    private NC_TimeObjectPropertyType() { }
}
