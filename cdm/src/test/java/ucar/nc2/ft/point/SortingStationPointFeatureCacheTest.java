package ucar.nc2.ft.point;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import com.google.common.collect.Ordering;
import org.junit.Assert;
import org.junit.Test;
import ucar.ma2.DataType;
import ucar.ma2.StructureDataScalar;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.NoFactoryFoundException;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.time.CalendarDateUnit;

public class SortingStationPointFeatureCacheTest {
    @Test
    public void test1() throws Exception {
        StructureDataScalar stationData = new StructureDataScalar("StationFeature");  // leave it empty.
        stationData.addMemberString("name", null, null, "Foo", 3);
        stationData.addMemberString("desc", null, null, "Bar", 3);
        stationData.addMemberString("wmoId", null, null, "123", 3);
        stationData.addMember("lat", null, "degrees_north", DataType.DOUBLE, 30);
        stationData.addMember("lon", null, "degrees_east", DataType.DOUBLE, 60);
        stationData.addMember("alt", null, "meters", DataType.DOUBLE, 5000);

        StationFeature stationFeat = new StationFeatureImpl("Foo", "Bar", "123", 30, 60, 5000, 4, stationData);

        CalendarDateUnit timeUnit = CalendarDateUnit.of(null, "days since 1970-01-01");

        List<StationPointFeature> spfList = new ArrayList<>();
        spfList.add(makeStationPointFeature(stationFeat, timeUnit, 10, 10, 103));
        spfList.add(makeStationPointFeature(stationFeat, timeUnit, 20, 20, 96));
        spfList.add(makeStationPointFeature(stationFeat, timeUnit, 30, 30, 118));
        spfList.add(makeStationPointFeature(stationFeat, timeUnit, 40, 40, 110));

        Comparator<StationPointFeature> revObsTimeComp = new Comparator<StationPointFeature>() {
            @Override
            public int compare(StationPointFeature left, StationPointFeature right) {
                return -Double.compare(left.getObservationTime(), right.getObservationTime());
            }
        };
        
        SortingStationPointFeatureCache cache = new SortingStationPointFeatureCache(revObsTimeComp);

        for (StationPointFeature stationPointFeat : spfList) {
            cache.add(stationPointFeat);
        }
        
        Collections.reverse(spfList);
        Assert.assertTrue(
                PointTestUtil.equals(new PointIteratorAdapter(spfList.iterator()), cache.getPointFeatureIterator()));
    }

    private static StationPointFeature makeStationPointFeature(
            StationFeature stationFeat, CalendarDateUnit timeUnit, double obsTime, double nomTime, double tasmax) {
        StructureDataScalar featureData = new StructureDataScalar("StationPointFeature");
        featureData.addMember("obsTime", "Observation time", timeUnit.getUdUnit(), DataType.DOUBLE, obsTime);
        featureData.addMember("nomTime", "Nominal time", timeUnit.getUdUnit(), DataType.DOUBLE, nomTime);
        featureData.addMember("tasmax", "Max temperature", "Celsius", DataType.DOUBLE, tasmax);

        return new SimpleStationPointFeature(stationFeat, obsTime, nomTime, timeUnit, featureData);
    }
    
    @Test
    public void test2() throws IOException, NoFactoryFoundException, URISyntaxException {
        File testFile = new File(getClass().getResource("orthogonal.ncml").toURI());
        
        Comparator<StationPointFeature> reverseStationNameComparator =
                Ordering.from(SortingStationPointFeatureCache.stationNameComparator).reverse();
        SortingStationPointFeatureCache cache = new SortingStationPointFeatureCache(reverseStationNameComparator);
        cache.addAll(testFile);
        
        List<String> expectedStationNames = Arrays.asList("CCC", "BBB", "AAA");
        List<String> actualStationNames = new LinkedList<>();

        try (PointFeatureIterator iter = cache.getPointFeatureIterator()) {
            while (iter.hasNext()) {
                StationPointFeature stationPointFeat = (StationPointFeature) iter.next();
                actualStationNames.add(stationPointFeat.getStation().getName());
            }
        }
        
        Assert.assertEquals(expectedStationNames, actualStationNames);
    }
    
    @Test
    public void test3() throws URISyntaxException, NoFactoryFoundException, IOException {
        // Sort in reverse order of station name length.
        Comparator<StationPointFeature> longestStationNameFirst = new Comparator<StationPointFeature>() {
            @Override
            public int compare(StationPointFeature o1, StationPointFeature o2) {
                return -Integer.compare(o1.getStation().getName().length(), o2.getStation().getName().length());
            }
        };
        SortingStationPointFeatureCache cache = new SortingStationPointFeatureCache(longestStationNameFirst);
        
        try (FeatureDatasetPoint fdInput = PointTestUtil.openPointDataset("cacheTestInput1.ncml");
                FeatureDatasetPoint fdExpected = PointTestUtil.openPointDataset("cacheTestExpected1.ncml")) {
            cache.addAll(fdInput);

//            PointFeatureIterator pointIterExpected =
//                    new FlattenedDatasetPointCollection(fdExpected).getPointFeatureIterator(-1);
//            PointFeatureIterator pointIterActual = cache.getPointFeatureIterator();
//            Assert.assertTrue(PointTestUtil.equals(pointIterExpected, pointIterActual));
            Assert.fail("Need to fix FlattenedDatastPointCollection");
        }
    }
}
