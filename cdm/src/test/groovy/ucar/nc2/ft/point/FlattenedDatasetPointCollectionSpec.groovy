//package ucar.nc2.ft.point
//
//import spock.lang.Specification
//import ucar.nc2.ft.*
//import ucar.nc2.time.CalendarDateRange
//import ucar.nc2.units.DateUnit
//import ucar.unidata.geoloc.EarthLocationImpl
//import ucar.unidata.geoloc.LatLonPointImpl
//import ucar.unidata.geoloc.LatLonRect
//
///**
// * @author cwardgar
// * @since 2015/06/26
// */
//class FlattenedDatasetPointCollectionSpec extends Specification {
//    // FDP used in all feature methods. Its getPointFeatureCollectionList() method will be stubbed to return
//    // different collections per test.
//    def fdPoint = Mock(FeatureDatasetPoint)
//
//    // PFCs used in many of the feature methods.
//    PointFeatureCollection pointFeatColAlpha, pointFeatColBeta, pointFeatColGamma
//
//    def setup() {   // run before every feature method
//        setup: "create feature collections"
//
//        and: "create alpha"
//        DateUnit dateUnitAlpha = new DateUnit("d since 1970-01-01 00:00:00")
//        pointFeatColAlpha = new SimplePointFeatureCollection("Alpha", dateUnitAlpha, "yard");
//        pointFeatColAlpha.add makePointFeat(-75, -70, 630,  23, dateUnitAlpha)
//        pointFeatColAlpha.add makePointFeat(-60, -40, 94,   51, dateUnitAlpha)
//        pointFeatColAlpha.add makePointFeat(-45, -10, 1760, 88, dateUnitAlpha)
//
//        and: "test alpha"
//        pointFeatColAlpha.calcBounds()
//        assert pointFeatColAlpha.getBoundingBox() ==
//                new LatLonRect(new LatLonPointImpl(-75, -70), new LatLonPointImpl(-45, -10))
//        assert pointFeatColAlpha.getCalendarDateRange() ==
//                CalendarDateRange.of(dateUnitAlpha.makeCalendarDate(23), dateUnitAlpha.makeCalendarDate(88))
//
//        and: "create beta"
//        DateUnit dateUnitBeta = new DateUnit("day since 1970-01-01 00:00:00")
//        pointFeatColBeta = new SimplePointFeatureCollection("Beta", dateUnitBeta, "millimeter")
//        pointFeatColBeta.add makePointFeat(-85, 20, 18940, 120, dateUnitBeta)
//        pointFeatColBeta.add makePointFeat(0,   50, 26600, 150, dateUnitBeta)
//        pointFeatColBeta.add makePointFeat(85,  80, 52800, 180, dateUnitBeta)
//
//        and: "test beta"
//        pointFeatColBeta.calcBounds()
//        assert pointFeatColBeta.getBoundingBox() ==
//                new LatLonRect(new LatLonPointImpl(-85, 20), new LatLonPointImpl(85, 80))
//        assert pointFeatColBeta.getCalendarDateRange() ==
//                CalendarDateRange.of(dateUnitBeta.makeCalendarDate(120), dateUnitBeta.makeCalendarDate(180))
//
//        and: "create gamma"
//        DateUnit dateUnitGamma = new DateUnit("days since 1970-01-01 00:00:00")
//        pointFeatColGamma = new SimplePointFeatureCollection("Gamma", dateUnitGamma, "feet")
//        pointFeatColGamma.add makePointFeat(15, 110, 1894, 200, dateUnitGamma)
//        pointFeatColGamma.add makePointFeat(30, 140, 266,  300, dateUnitGamma)
//        pointFeatColGamma.add makePointFeat(45, 170, 5280, 400, dateUnitGamma)
//
//        and: "test gamma"
//        pointFeatColGamma.calcBounds()
//        assert pointFeatColGamma.getBoundingBox() ==
//                new LatLonRect(new LatLonPointImpl(15, 110), new LatLonPointImpl(45, 170))
//        assert pointFeatColGamma.getCalendarDateRange() ==
//                CalendarDateRange.of(dateUnitGamma.makeCalendarDate(200), dateUnitGamma.makeCalendarDate(400))
//    }
//
//    private static PointFeature makePointFeat(double lat, double lon, double alt, double time, DateUnit dateUnit) {
//        def earthLoc = new EarthLocationImpl(lat, lon, alt)
//
//        // Pass null StructureData; we only care about the metadata for these tests.
//        return new SimplePointFeature(earthLoc, time, time, dateUnit, null)
//    }
//
//
//    def "handles empty FeatureDatasetPoint"() {
//        setup: "fdPoint returns an empty list of PointFeatureCollections"
//        fdPoint.getPointFeatureCollectionList() >> []
//
//        when: "we construct a FlattenedDatasetPointCollection using our exmpty FeatureDatasetPoint"
//        def flattenedDatasetCol = new FlattenedDatasetPointCollection(fdPoint);
//
//        then: "the default unitsString and altUnits are used"
//        flattenedDatasetCol.timeUnit.unitsString == DateUnit.unixDateUnit.unitsString
//        flattenedDatasetCol.altUnits == null
//
//        when: "get empty collection's iterator"
//        def flattenedDatasetIter = flattenedDatasetCol.getPointFeatureIterator(-1)
//
//        then: "iterator is empty"
//        !flattenedDatasetIter.hasNext()
//        flattenedDatasetIter.next() == null
//    }
//
//    def "metadata of aggregate collection is taken from the first collection"() {
//        setup: "fdPoint returns our 3 feature collections"
//        fdPoint.getPointFeatureCollectionList() >> [pointFeatColAlpha, pointFeatColBeta, pointFeatColGamma]
//
//        when: "we flatten our dataset containing 3 collections into one collection"
//        def flattenedDatasetCol = new FlattenedDatasetPointCollection(fdPoint);
//
//        then: "flattenedDatasetCol metadata objects are same as pointFeatColAlpha's"
//        flattenedDatasetCol.getTimeUnit().is pointFeatColAlpha.getTimeUnit()
//        flattenedDatasetCol.getAltUnits().is pointFeatColAlpha.getAltUnits()
//        flattenedDatasetCol.getExtraVariables().is pointFeatColAlpha.getExtraVariables()
//    }
//
//    def "flattens dataset containing 3 feature collections"() {
//        setup: "fdPoint returns our 3 feature collections"
//        fdPoint.getPointFeatureCollectionList() >> [pointFeatColAlpha, pointFeatColBeta, pointFeatColGamma]
//
//        when: "we flatten our dataset containing 3 collections into one collection and calc bounds"
//        def flattenedDatasetCol = new FlattenedDatasetPointCollection(fdPoint);
//        flattenedDatasetCol.calcBounds()
//
//        then: "the bounds include all 3 collections"
//        flattenedDatasetCol.size() == 9
//        flattenedDatasetCol.getBoundingBox() ==
//                new LatLonRect(new LatLonPointImpl(-85, -70), new LatLonPointImpl(85, 170))
//
//        def dateUnit = flattenedDatasetCol.getTimeUnit()
//        flattenedDatasetCol.getCalendarDateRange() ==
//                CalendarDateRange.of(dateUnit.makeCalendarDate(23), dateUnit.makeCalendarDate(400))
//    }
//
//    def "iterator is consistent with collection"() {
//        setup: "get iterator over flattened dataset"
//        fdPoint.getPointFeatureCollectionList() >> [pointFeatColAlpha, pointFeatColBeta, pointFeatColGamma]
//        def flattenedDatasetCol = new FlattenedDatasetPointCollection(fdPoint)
//        def flattenedDatasetIter = flattenedDatasetCol.getPointFeatureIterator(-1)
//
//        when: "turn on bounds calculation and iterator over all elements"
//        flattenedDatasetIter.setCalculateBounds(flattenedDatasetCol)  // Turn on bounds calculation.
//        while (flattenedDatasetIter.hasNext()) {
//            flattenedDatasetIter.next()
//        }
//
//        then: "iterator's and collection's bounds are equal"
//        flattenedDatasetIter.getBoundingBox() == flattenedDatasetCol.getBoundingBox()
//        flattenedDatasetIter.getDateRange() == flattenedDatasetCol.getDateRange()
//        flattenedDatasetIter.getCalendarDateRange() == flattenedDatasetCol.getCalendarDateRange()
//        flattenedDatasetIter.getCount() == flattenedDatasetCol.size()
//    }
//
//    def "handles NestedPointFeatureCollection"() {
//        setup: "fdPoint returns a NestedPointFeatureCollection, which flattenes to pointFeatColAlpha"
//        def npfc1 = Mock(NestedPointFeatureCollection) {
//            flatten(_, _) >> pointFeatColAlpha
//        }
//        fdPoint.getPointFeatureCollectionList() >> [npfc1]
//
//        when: "we flatten the dataset and calculate bounds"
//        PointFeatureCollection flattenedDatasetCol = new FlattenedDatasetPointCollection(fdPoint);
//        flattenedDatasetCol.calcBounds()
//
//        then: "flattenedDatasetCol's bounding box equals pointFeatColAlpha's bounding box"
//        flattenedDatasetCol.getBoundingBox() == pointFeatColAlpha.getBoundingBox()
//    }
//
//    def "throws exception for unexpected FeatureCollection subtype"() {
//        setup: "create a FeatureCollection that is not a PointFeatureCollection or NestedPointFeatureCollection"
//        def featCol = Mock(FeatureCollection)
//        fdPoint.getPointFeatureCollectionList() >> [featCol]
//
//        when: "we flatten the dataset"
//        new FlattenedDatasetPointCollection(fdPoint);
//
//        then: "an exception is thrown"
//        thrown(IllegalArgumentException)
//    }
//}
