package thredds.server.ncss.view.dsg;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import thredds.server.ncss.format.SupportedFormat;
import thredds.server.ncss.params.NcssParamsBean;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.util.DiskCache2;

import java.io.*;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;

/**
 * Created by cwardgar on 2014/05/27.
 */
@RunWith(Parameterized.class)
public class DsgSubsetWriterTest {
    private static DiskCache2 diskCache;

    private static NcssParamsBean ncssParamsAll;
    private static NcssParamsBean ncssParamsPoint;
    private static NcssParamsBean ncssParamsStation;

    // @BeforeClass  <-- Can't use: JUnit won't invoke this method before getTestParameters().
    public static void setupClass() throws URISyntaxException {
        diskCache = DiskCache2.getDefault();

        ncssParamsAll = new NcssParamsBean();
        ncssParamsAll.setVar(Arrays.asList("pr", "tas"));

        ncssParamsPoint = new NcssParamsBean();
        ncssParamsPoint.setVar(Arrays.asList("pr"));
        ncssParamsPoint.setTime("1970-01-01 02:00:00Z");
        ncssParamsPoint.setNorth(53.0);   // Full extension == 68.0
        ncssParamsPoint.setSouth(40.0);   // Full extension == 40.0
        ncssParamsPoint.setWest(-100.0);  // Full extension == -100.0
        ncssParamsPoint.setEast(-58.0);   // Full extension == -58.0

        ncssParamsStation = new NcssParamsBean();
        ncssParamsStation.setVar(Arrays.asList("tas"));
        ncssParamsStation.setTime_start("1970-01-05T00:00:00Z");
        ncssParamsStation.setTime_end("1970-02-05T00:00:00Z");
        ncssParamsStation.setStns(Arrays.asList("AAA", "CCC"));
    }

    @Parameterized.Parameters(name = "{4}")  // Name the tests after the expectedResultResource.
    public static List<Object[]> getTestParameters() throws URISyntaxException {
        // Normally, we'd annotate setupClass() with @BeforeClass and let JUnit call it. Unfortunately, that won't
        // work as expected when @Parameters gets involved.
        // See http://feraldeveloper.blogspot.co.uk/2013/12/beforeclass-and-parametrized-junit-tests.html
        // I like this workaround better than <clinit> because subclasses can potentially override setupClass().
        setupClass();

        return Arrays.asList(new Object[][] {
            { FeatureType.POINT,   "point.ncml",   SupportedFormat.CSV_FILE, ncssParamsAll,     "pointAll.csv"      },
            { FeatureType.POINT,   "point.ncml",   SupportedFormat.CSV_FILE, ncssParamsPoint,   "pointSubset.csv"   },
            { FeatureType.POINT,   "point.ncml",   SupportedFormat.XML_FILE, ncssParamsAll,     "pointAll.xml"      },
            { FeatureType.POINT,   "point.ncml",   SupportedFormat.XML_FILE, ncssParamsPoint,   "pointSubset.xml"   },

            { FeatureType.STATION, "station.ncml", SupportedFormat.CSV_FILE, ncssParamsAll,     "stationAll.csv"    },
            { FeatureType.STATION, "station.ncml", SupportedFormat.CSV_FILE, ncssParamsStation, "stationSubset.csv" },
            { FeatureType.STATION, "station.ncml", SupportedFormat.XML_FILE, ncssParamsAll,     "stationAll.xml"    },
            { FeatureType.STATION, "station.ncml", SupportedFormat.XML_FILE, ncssParamsStation, "stationSubset.xml" }
        });
    }

    private final FeatureType wantedType;
    private final File datasetFile;
    private final SupportedFormat format;
    private final NcssParamsBean ncssParams;
    private final String expectedResultResource;

    public DsgSubsetWriterTest(FeatureType wantedType, String datasetResource,
            SupportedFormat format, NcssParamsBean ncssParams, String expectedResultResource) throws URISyntaxException {
        this.wantedType = wantedType;
        this.datasetFile = new File(DsgSubsetWriterTest.class.getResource(datasetResource).toURI());
        this.format = format;
        this.ncssParams = ncssParams;
        this.expectedResultResource = expectedResultResource;
    }

    @Test
    public void testWrite() throws Exception {
        FeatureDatasetPoint fdPoint = openPointDataset(wantedType, datasetFile);
        InputStream expectedInputStream = getClass().getResourceAsStream(expectedResultResource);

        try {
            ByteArrayOutputStream actualOutputStream = new ByteArrayOutputStream();
            DsgSubsetWriter subsetWriter = DsgSubsetWriterFactory.newInstance(
                    fdPoint, ncssParams, diskCache, actualOutputStream, wantedType, format);
            subsetWriter.write();

//            DsgSubsetWriter subsetWriterConsole = DsgSubsetWriterFactory.newInstance(
//                    fdPoint, ncssParams, diskCache, System.out, wantedType, format);
//            subsetWriterConsole.write();

            ByteArrayInputStream actualInputStream = new ByteArrayInputStream(actualOutputStream.toByteArray());

            if (format.isBinary()) {
                // Perform binary comparison.
                Assert.assertTrue(IOUtils.contentEquals(expectedInputStream, actualInputStream));
            } else {
                // Perform text comparison.
                Reader expectedReader = new InputStreamReader(expectedInputStream, "UTF-8");
                Reader actualReader   = new InputStreamReader(actualInputStream,   "UTF-8");
                Assert.assertTrue(IOUtils.contentEqualsIgnoreEOL(expectedReader, actualReader));
            }
        } finally {
            expectedInputStream.close();
            fdPoint.close();
        }
    }

    public static FeatureDatasetPoint openPointDataset(FeatureType wantedType, File datasetFile) throws IOException {
        Formatter errlog = new Formatter();
        FeatureDataset fDset = FeatureDatasetFactoryManager.open(
                wantedType, datasetFile.getAbsolutePath(), null, errlog);

        assert fDset != null : "No factory found: " + errlog.toString();
        return (FeatureDatasetPoint) fDset;
    }
}
