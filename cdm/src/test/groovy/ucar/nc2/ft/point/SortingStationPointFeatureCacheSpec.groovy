package ucar.nc2.ft.point

import spock.lang.Specification
import ucar.ma2.DataType
import ucar.ma2.StructureDataScalar
import ucar.nc2.units.DateUnit

/**
 * @author cwardgar
 * @since 2015/09/21
 */
class SortingStationPointFeatureCacheSpec extends Specification {
    def "reverse order - points from memory"() {
        setup:
        StructureDataScalar stationData = new StructureDataScalar("StationFeature")
        stationData.addMemberString("name", null, null, "Foo", 3)
        stationData.addMemberString("desc", null, null, "Bar", 3)
        stationData.addMemberString("wmoId", null, null, "123", 3)
        stationData.addMember("lat", null, "degrees_north", DataType.DOUBLE, 30)
        stationData.addMember("lon", null, "degrees_east", DataType.DOUBLE, 60)
        stationData.addMember("alt", null, "meters", DataType.DOUBLE, 5000)

        and:
        StationFeature stationFeat = new StationFeatureImpl("Foo", "Bar", "123", 30, 60, 5000, 4, stationData);
        DateUnit timeUnit = new DateUnit("days since 1970-01-01");

        and:
        List<StationPointFeature> spfList = []
        spfList << makeStationPointFeature(stationFeat, timeUnit, 10, 10, 103)
        spfList << makeStationPointFeature(stationFeat, timeUnit, 20, 20, 96)
        spfList << makeStationPointFeature(stationFeat, timeUnit, 30, 30, 118)
        spfList << makeStationPointFeature(stationFeat, timeUnit, 40, 40, 110)

        and:
        Comparator<StationPointFeature> revObsTimeComp = new Comparator<StationPointFeature>() {
            @Override
            public int compare(StationPointFeature left, StationPointFeature right) {
                return -Double.compare(left.getObservationTime(), right.getObservationTime());
            }
        };
        SortingStationPointFeatureCache cache = new SortingStationPointFeatureCache(revObsTimeComp);

        when:
        spfList.each { cache.add(it) }

        then:
        PointTestUtil.equals(new PointIteratorAdapter(spfList.reverse().iterator()), cache.getPointFeatureIterator())
    }

    StationPointFeature makeStationPointFeature(
            StationFeature stationFeat, DateUnit timeUnit, double obsTime, double nomTime, double tasmax) {
        StructureDataScalar featureData = new StructureDataScalar("StationPointFeature");
        featureData.addMember("obsTime", "Observation time", timeUnit.getUnitsString(), DataType.DOUBLE, obsTime);
        featureData.addMember("nomTime", "Nominal time", timeUnit.getUnitsString(), DataType.DOUBLE, nomTime);
        featureData.addMember("tasmax", "Max temperature", "Celsius", DataType.DOUBLE, tasmax);

        return new SimpleStationPointFeature(stationFeat, obsTime, nomTime, timeUnit, featureData);
    }
}
