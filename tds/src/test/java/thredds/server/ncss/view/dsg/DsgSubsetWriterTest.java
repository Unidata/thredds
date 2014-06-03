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
    private static NcssParamsBean ncssParamsAll;
    private static NcssParamsBean ncssParamsSubset;
    private static DiskCache2 diskCache;

    // @BeforeClass  <-- Can't use: JUnit won't invoke this method before getTestParameters().
    public static void setupClass() throws URISyntaxException {
        ncssParamsAll = new NcssParamsBean();
        ncssParamsAll.setVar(Arrays.asList("pr", "tas"));

        ncssParamsSubset = new NcssParamsBean();
        ncssParamsSubset.setVar(Arrays.asList("tas"));
        ncssParamsSubset.setTime_start("1970-01-05T00:00:00Z");
        ncssParamsSubset.setTime_end("1970-02-05T00:00:00Z");
        ncssParamsSubset.setStns(Arrays.asList("AAA", "CCC"));

        diskCache = DiskCache2.getDefault();
    }

    @Parameterized.Parameters(name = "{4}")  // Name the tests after the expectedResultResource.
    public static List<Object[]> getTestParameters() throws URISyntaxException {
        // Normally, we'd annotate setupClass() with @BeforeClass and let JUnit call it. Unfortunately, that won't
        // work as expected when @Parameters gets involved.
        // See http://feraldeveloper.blogspot.co.uk/2013/12/beforeclass-and-parametrized-junit-tests.html
        // I like this workaround better than <clinit> because subclasses can potentially override setupClass().
        setupClass();

        return Arrays.asList(new Object[][] {
                { FeatureType.POINT, "pointMultiVar.ncml",
                        SupportedFormat.CSV_FILE, ncssParamsAll, "pointCsvAll.csv" },

                { FeatureType.STATION, "multiStationMultiVar.ncml",
                        SupportedFormat.CSV_FILE, ncssParamsAll, "stationCsvAll.csv" },
                { FeatureType.STATION, "multiStationMultiVar.ncml",
                        SupportedFormat.CSV_FILE, ncssParamsSubset, "stationCsvSubset.csv" },
                { FeatureType.STATION, "multiStationMultiVar.ncml",
                        SupportedFormat.XML_FILE, ncssParamsAll, "stationXmlAll.xml" },
                { FeatureType.STATION, "multiStationMultiVar.ncml",
                        SupportedFormat.XML_FILE, ncssParamsSubset, "stationXmlSubset.xml" }
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
