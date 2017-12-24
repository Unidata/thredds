package ucar.nc2.ft.point

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Specification
import ucar.ma2.Array
import ucar.ma2.ArrayObject
import ucar.ma2.StructureData
import ucar.nc2.ft.FeatureDatasetPoint
import ucar.nc2.ft.PointFeature
import ucar.nc2.ft.PointFeatureCollection
import ucar.nc2.ft.PointFeatureIterator
import ucar.nc2.time.CalendarDate
import ucar.nc2.time.CalendarDateRange
import ucar.nc2.time.CalendarDateUnit
import ucar.unidata.geoloc.LatLonPointImpl
import ucar.unidata.geoloc.LatLonRect

/**
 * @author cwardgar
 * @since 2015/09/21
 */
class PointIteratorFilteredSpec extends Specification {
    private static final Logger logger = LoggerFactory.getLogger(PointIteratorFilteredSpec)
    
    def "space and time filter"() {
        setup: "feature dataset"
        FeatureDatasetPoint fdPoint = PointTestUtil.openPointDataset("pointsToFilter.ncml")

        and: "bouding box"
        double latMin = +10.0
        double latMax = +50.0
        double lonMin = -60.0
        double lonMax = +10.0
        LatLonRect filter_bb = new LatLonRect(
                new LatLonPointImpl(latMin, lonMin), new LatLonPointImpl(latMax, lonMax))

        and: "time range"
        CalendarDateUnit calDateUnit = CalendarDateUnit.of("standard", "days since 1970-01-01 00:00:00")
        CalendarDate start = calDateUnit.makeCalendarDate(20)
        CalendarDate end = calDateUnit.makeCalendarDate(130)
        CalendarDateRange filter_date = CalendarDateRange.of(start, end)

        and: "filtered point iterator"
        PointFeatureCollection flattenedDatasetCol = new FlattenedDatasetPointCollection(fdPoint)
        PointFeatureIterator pointIterOrig = flattenedDatasetCol.getPointFeatureIterator()
        PointFeatureIterator pointIterFiltered = new PointIteratorFiltered(pointIterOrig, filter_bb, filter_date)

        expect:
        getIdsOfPoints(pointIterFiltered) == ['BBB', 'EEE']

        when: "we call next() when there are no more elements"
        pointIterFiltered.next()
        then: "an exception is thrown"
        NoSuchElementException e = thrown()
        e.message == 'This iterator has no more elements.'

        cleanup:
        pointIterFiltered?.close()
        fdPoint?.close()
    }

    def getIdsOfPoints(PointFeatureIterator iter) {
        def ids = []
        while (iter.hasNext()) {
            iter.hasNext();  // Test idempotency. This call should have no effect.
            ids << getIdOfPoint(iter.next())
        }
        return ids
    }

    private static String getIdOfPoint(PointFeature pointFeat) throws IOException {
        StructureData data = pointFeat.getFeatureData()
        Array memberArray = data.getArray("id");
        assert memberArray instanceof ArrayObject.D0

        ArrayObject.D0 memberArrayObject = memberArray as ArrayObject.D0
        return memberArrayObject.get() as String
    }
}
