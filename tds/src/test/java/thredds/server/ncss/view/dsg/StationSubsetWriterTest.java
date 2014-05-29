package thredds.server.ncss.view.dsg;

import org.apache.commons.io.IOUtils;
import org.junit.*;
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
public class StationSubsetWriterTest {
    private static FeatureType wantedType;
    private static File datasetFile;
    private static NcssParamsBean ncssParamsAll;
    private static NcssParamsBean ncssParamsSubset;
    private static DiskCache2 diskCache;

    // @BeforeClass  <-- Can't use: JUnit won't invoke this method before getTestParameters().
    public static void setupClass() throws URISyntaxException {
        wantedType = FeatureType.STATION;
        datasetFile = new File(StationSubsetWriterTest.class.getResource("multiStationMultiVar.ncml").toURI());

        ncssParamsAll = new NcssParamsBean();
        ncssParamsAll.setVar(Arrays.asList("pr", "tas"));
        ncssParamsAll.setTime_start("1970-01-01T00:00:00Z");
        ncssParamsAll.setTime_end("1970-02-10T00:00:00Z");
        ncssParamsAll.setNorth(44.0);
        ncssParamsAll.setSouth(40.0);
        ncssParamsAll.setEast(-94.0);
        ncssParamsAll.setWest(-100.0);

        ncssParamsSubset = new NcssParamsBean();
        // TODO

        diskCache = DiskCache2.getDefault();
    }

    @Parameterized.Parameters(name = "{2}")  // Name the tests after the expectedResultResource.
    public static List<Object[]> getTestParameters() throws URISyntaxException {
        // Normally, we'd annotate setupClass() with @BeforeClass and let JUnit call it. Unfortunately, that won't
        // work as expected when @Parameters gets involved.
        // See http://feraldeveloper.blogspot.co.uk/2013/12/beforeclass-and-parametrized-junit-tests.html
        // I like this workaround better than <clinit> because subclasses can potentially override setupClass().
        setupClass();

        return Arrays.asList(new Object[][] {
                { SupportedFormat.CSV_FILE, ncssParamsAll, "stationCsvAll.csv" },
                { SupportedFormat.XML_FILE, ncssParamsAll, "stationXmlAll.xml" }
        });
    }

    private final SupportedFormat format;
    private final NcssParamsBean ncssParams;
    private final String expectedResultResource;

    public StationSubsetWriterTest(SupportedFormat format, NcssParamsBean ncssParams, String expectedResultResource) {
        this.format = format;
        this.ncssParams = ncssParams;
        this.expectedResultResource = expectedResultResource;
    }

    @Test
    public void testWrite() throws Exception {
        FeatureDatasetPoint fdPoint = openPointDataset(datasetFile);
        InputStream expectedInputStream = getClass().getResourceAsStream(expectedResultResource);

        try {
            ByteArrayOutputStream actualOutputStream = new ByteArrayOutputStream();
            DsgSubsetWriter subsetWriter = DsgSubsetWriterFactory.newInstance(actualOutputStream, wantedType, format);
            subsetWriter.write(fdPoint, ncssParams, diskCache);

            DsgSubsetWriter subsetWriterConsole = DsgSubsetWriterFactory.newInstance(System.out, wantedType, format);
            subsetWriterConsole.write(fdPoint, ncssParams, diskCache);

            ByteArrayInputStream actualInputStream = new ByteArrayInputStream(actualOutputStream.toByteArray());

            Assert.assertTrue(IOUtils.contentEquals(expectedInputStream, actualInputStream));
        } finally {
            expectedInputStream.close();
            fdPoint.close();
        }
    }

    public static FeatureDatasetPoint openPointDataset(File datasetFile) throws IOException {
        Formatter errlog = new Formatter();
        FeatureDataset fDset = FeatureDatasetFactoryManager.open(
                wantedType, datasetFile.getAbsolutePath(), null, errlog);

        assert fDset != null : "No factory found: " + errlog.toString();
        return (FeatureDatasetPoint) fDset;
    }
}
