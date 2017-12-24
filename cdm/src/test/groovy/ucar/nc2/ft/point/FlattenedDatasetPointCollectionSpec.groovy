package ucar.nc2.ft.point

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Specification
import ucar.nc2.constants.FeatureType
import ucar.nc2.ft.*
import ucar.nc2.time.CalendarDateRange
import ucar.nc2.time.CalendarDateUnit
import ucar.unidata.geoloc.EarthLocationImpl
import ucar.unidata.geoloc.LatLonPointImpl
import ucar.unidata.geoloc.LatLonRect

/**
 * @author cwardgar
 * @since 2015/06/26
 */
class FlattenedDatasetPointCollectionSpec extends Specification {
    private static final Logger logger = LoggerFactory.getLogger(FlattenedDatasetPointCollectionSpec)
    
    // FDP used in all feature methods. Its getPointFeatureCollectionList() method will be stubbed to return
    // different collections per test.
    def fdPoint = Mock(FeatureDatasetPoint)

    PointFeature pf1, pf2, pf3, pf4, pf5, pf6, pf7, pf8, pf9

    def setup() {   // run before every feature method
        setup: "create point features"
        CalendarDateUnit dateUnit = CalendarDateUnit.of(null, "days since 1970-01-01 00:00:00")
        DsgFeatureCollection dummyDsg = new SimplePointFeatureCollection("dummy", dateUnit, "m")

        pf1 = makePointFeat(dummyDsg, -75, -70,  630,   23,  dateUnit)
        pf2 = makePointFeat(dummyDsg, -60, -40,  94,    51,  dateUnit)
        pf3 = makePointFeat(dummyDsg, -45, -10,  1760,  88,  dateUnit)
        pf4 = makePointFeat(dummyDsg, -85,  20,  18940, 120, dateUnit)
        pf5 = makePointFeat(dummyDsg,  0,   50,  26600, 150, dateUnit)
        pf6 = makePointFeat(dummyDsg,  85,  80,  52800, 180, dateUnit)
        pf7 = makePointFeat(dummyDsg,  15,  110, 1894,  200, dateUnit)
        pf8 = makePointFeat(dummyDsg,  30,  140, 266,   300, dateUnit)
        pf9 = makePointFeat(dummyDsg,  45,  170, 5280,  400, dateUnit)
    }

    private static PointFeature makePointFeat(
            DsgFeatureCollection dsg, double lat, double lon, double alt, double time, CalendarDateUnit dateUnit) {
        def earthLoc = new EarthLocationImpl(lat, lon, alt)

        // Pass null StructureData; we only care about the metadata for these tests.
        return new SimplePointFeature(dsg, earthLoc, time, time, dateUnit, null)
    }

    def "handles empty FeatureDatasetPoint"() {
        setup: "fdPoint returns an empty list of PointFeatureCollections"
        fdPoint.getPointFeatureCollectionList() >> []

        when: "we construct a FlattenedDatasetPointCollection using our exmpty FeatureDatasetPoint"
        def flattenedDatasetCol = new FlattenedDatasetPointCollection(fdPoint);

        then: "the default unitsString and altUnits are used"
        flattenedDatasetCol.timeUnit.udUnit == CalendarDateUnit.unixDateUnit.udUnit
        flattenedDatasetCol.altUnits == null

        when: "get empty collection's iterator"
        def flattenedDatasetIter = flattenedDatasetCol.getPointFeatureIterator()

        then: "iterator is empty"
        !flattenedDatasetIter.hasNext()
        flattenedDatasetIter.next() == null
    }

    def "metadata of aggregate collection is taken from the first collection"() {
        setup: "create CalendarDateUnits"
        CalendarDateUnit calDateUnitAlpha = CalendarDateUnit.of(null, "d since 1970-01-01 00:00:00")
        CalendarDateUnit calDateUnitBeta  = CalendarDateUnit.of(null, "day since 1970-01-01 00:00:00")
        CalendarDateUnit dateUnitGamma    = CalendarDateUnit.of(null, "days since 1970-01-01 00:00:00")

        and: "create PointFeatureCollections"
        PointFeatureCollection pointFeatColAlpha = new SimplePointFeatureCollection("Alpha", calDateUnitAlpha, "yard");
        PointFeatureCollection pointFeatColBeta  = new SimplePointFeatureCollection("Beta", calDateUnitBeta, "mm")
        PointFeatureCollection pointFeatColGamma = new SimplePointFeatureCollection("Gamma", dateUnitGamma, "feet")

        and: "fdPoint returns our 3 feature collections"
        fdPoint.getPointFeatureCollectionList() >> [pointFeatColAlpha, pointFeatColBeta, pointFeatColGamma]

        when: "we flatten our dataset containing 3 collections into one collection"
        def flattenedDatasetCol = new FlattenedDatasetPointCollection(fdPoint);

        then: "flattenedDatasetCol metadata objects are same as pointFeatColAlpha's"
        flattenedDatasetCol.timeUnit.is pointFeatColAlpha.timeUnit
        flattenedDatasetCol.altUnits.is pointFeatColAlpha.altUnits
    }

    def "all kinds of empty"() {
        setup: "create an empty instance of each of the DsgFeatureCollection types"
        PointFeatureCollection emptyC   = new SimplePointFeatureCollection("emptyC", null, "m")
        PointFeatureCC         emptyCC  = new SimplePointFeatureCC("emptyCC", null, "y", FeatureType.POINT)
        PointFeatureCCC        emptyCCC = new SimplePointFeatureCCC("emptyCCC", null, "in", FeatureType.POINT)

        and: "create a non-empty PointFeatureCC that contains an empty PointFeatureCollection"
        PointFeatureCC nonEmptyCC = new SimplePointFeatureCC("nonEmptyCC", null, "y", FeatureType.POINT)
        nonEmptyCC.add(emptyC)

        and: "create a non-empty PointFeatureCCC that contains both an empty and non-empty PointFeatureCC"
        PointFeatureCCC nonEmptyCCC = new SimplePointFeatureCCC("nonEmptyCCC", null, "in", FeatureType.POINT)
        nonEmptyCCC.add(emptyCC)
        nonEmptyCCC.add(nonEmptyCC)

        and: "create a mock FeatureDatasetPoint that returns each of our DsgFeatureCollection instances"
        fdPoint.getPointFeatureCollectionList() >> [
                emptyC, emptyCC, emptyCCC, nonEmptyCC, nonEmptyCCC
        ]

        and: "create flattened collection from our mocked dataset"
        FlattenedDatasetPointCollection flattenedDatasetCol = new FlattenedDatasetPointCollection(fdPoint)

        expect: "collection contains no PointFeatures"
        flattenedDatasetCol.asList().isEmpty()
    }

    def "multiple DsgFeatureCollection types in one FeatureDataset"() {
        setup: "create PointFeatureCollections"
        PointFeatureCollection pfc1 = new SimplePointFeatureCollection("pfc1", null, "m")
        pfc1.add(pf1);

        PointFeatureCollection pfc2 = new SimplePointFeatureCollection("pfc2", null, "m")
        pfc2.add(pf2)
        pfc2.add(pf3)

        PointFeatureCollection pfc3 = new SimplePointFeatureCollection("pfc3", null, "m")

        PointFeatureCollection pfc4 = new SimplePointFeatureCollection("pfc4", null, "m")
        pfc4.add(pf4)

        PointFeatureCollection pfc5 = new SimplePointFeatureCollection("pfc5", null, "m")
        pfc5.add(pf5)
        pfc5.add(pf6)
        pfc5.add(pf7)

        PointFeatureCollection pfc6 = new SimplePointFeatureCollection("pfc6", null, "m")
        pfc6.add(pf8)

        PointFeatureCollection pfc7 = new SimplePointFeatureCollection("pfc7", null, "m")
        pfc7.add(pf9)

        and: "create PointFeatureCCs"
        PointFeatureCC pfcc1 = new SimplePointFeatureCC("pfcc1", null, "m", FeatureType.POINT)
        pfcc1.add(pfc1)
        pfcc1.add(pfc2)

        PointFeatureCC pfcc2 = new SimplePointFeatureCC("pfcc2", null, "m", FeatureType.POINT)
        pfcc2.add(pfc3)
        pfcc2.add(pfc4)

        PointFeatureCC pfcc3 = new SimplePointFeatureCC("pfcc3", null, "m", FeatureType.POINT)
        pfcc3.add(pfc6)
        pfcc3.add(pfc7)

        and: "create PointFeatureCCC"
        CalendarDateUnit dateUnit = CalendarDateUnit.of(null, "d since 1970-01-01 00:00:00")
        PointFeatureCCC pfccc = new SimplePointFeatureCCC("pfccc", dateUnit, "m", FeatureType.POINT)
        pfccc.add(pfcc1)
        pfccc.add(pfcc2)

        and: "mock FeatureDatasetPoint to return 1 of each DsgFeatureCollection instance, then flatten it"
        fdPoint.getPointFeatureCollectionList() >> [pfccc, pfc5, pfcc3]
        FlattenedDatasetPointCollection flattenedDatasetCol = new FlattenedDatasetPointCollection(fdPoint)


        expect: "before iterating over the collection, bounds are null"
        flattenedDatasetCol.boundingBox == null
        flattenedDatasetCol.calendarDateRange == null

        when: "get the iterator and enable bounds calculation"
        PointIteratorAbstract flattenedPointIter = flattenedDatasetCol.getPointFeatureIterator() as PointIteratorAbstract
        flattenedPointIter.calculateBounds = flattenedDatasetCol.info

        and: "iterate over the collection"
        def actualPointFeats = []
        flattenedPointIter.withCloseable {
            for (PointFeature pointFeat : flattenedPointIter) {
                actualPointFeats << pointFeat
            }
        }

        then: "the 9 PointFeatures are returned in order"
        actualPointFeats == [pf1, pf2, pf3, pf4, pf5, pf6, pf7, pf8, pf9]

        and: "the bounds include all 9 PointFeatures"
        flattenedDatasetCol.size() == 9
        flattenedDatasetCol.boundingBox == new LatLonRect(new LatLonPointImpl(-85, -70), new LatLonPointImpl(85, 170))

        and:
        def calDateUnit = flattenedDatasetCol.timeUnit
        flattenedDatasetCol.calendarDateRange == CalendarDateRange.of(
                calDateUnit.makeCalendarDate(23), calDateUnit.makeCalendarDate(400))
    }
}
