package thredds.server.ncss.view.dsg;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.server.ncss.format.SupportedFormat;
import thredds.server.ncss.params.NcssParamsBean;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.iosp.netcdf4.Nc4;
import ucar.nc2.jni.netcdf.Nc4Iosp;
import ucar.nc2.ogc.MarshallingUtil;
import ucar.nc2.util.CompareNetcdf2;
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
    private static final Logger logger = LoggerFactory.getLogger(DsgSubsetWriterTest.class);

    private static final boolean isClibraryPresent;
    static {
        isClibraryPresent = Nc4Iosp.isClibraryPresent();
        if (!isClibraryPresent) {
            logger.error("Could not load the NetCDF-4 C library. Tests that require it will be skipped.");
        }
    }

    private static DiskCache2 diskCache;

    private static NcssParamsBean ncssParamsAll;
    private static NcssParamsBean ncssParamsPoint;
    private static NcssParamsBean ncssParamsStation1;
    private static NcssParamsBean ncssParamsStation2;

    // @BeforeClass  <-- Can't use: JUnit won't invoke this method before getTestParameters().
    public static void setupClass() throws URISyntaxException {
        // The WaterML marshaller usually initializes wml2:generationDate and om:resultTime to "now". This is a problem,
        // because those values will always differ from the fixed values we have in our expectedResultResource files.
        // So, to facilitate testing, we're going to fix the values that the marshaller emits.
        MarshallingUtil.fixedGenerationDate = new DateTime(1970, 1, 1, 0, 0, DateTimeZone.UTC);
        MarshallingUtil.fixedResultTime     = new DateTime(1970, 1, 1, 0, 0, DateTimeZone.UTC);

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

        ncssParamsStation1 = new NcssParamsBean();
        ncssParamsStation1.setVar(Arrays.asList("tas"));
        ncssParamsStation1.setTime_start("1970-01-05T00:00:00Z");
        ncssParamsStation1.setTime_end("1970-02-05T00:00:00Z");
        ncssParamsStation1.setStns(Arrays.asList("AAA", "CCC"));

        ncssParamsStation2 = new NcssParamsBean();
        ncssParamsStation2.setVar(Arrays.asList("pr", "tas"));
        ncssParamsStation2.setTime("1970-01-21 01:00:00Z");  // The nearest will be "1970-01-21 00:00:00Z"
    }

    @Parameterized.Parameters(name = "{0}/{1}/{3}")
    public static List<Object[]> getTestParameters() throws URISyntaxException {
        // Normally, we'd annotate setupClass() with @BeforeClass and let JUnit call it. Unfortunately, that won't
        // work as expected when @Parameters gets involved.
        // See http://feraldeveloper.blogspot.co.uk/2013/12/beforeclass-and-parametrized-junit-tests.html
        // I like this workaround better than <clinit> because subclasses can potentially override setupClass().
        setupClass();

        return Arrays.asList(new Object[][] {
                // Point
                { FeatureType.POINT,   SupportedFormat.CSV_FILE, ncssParamsAll,      "outputAll.csv"      },
                { FeatureType.POINT,   SupportedFormat.CSV_FILE, ncssParamsPoint,    "outputSubset.csv"   },

                { FeatureType.POINT,   SupportedFormat.GEOCSV_FILE, ncssParamsAll,      "outputAll.csv"      },
                { FeatureType.POINT,   SupportedFormat.GEOCSV_FILE, ncssParamsPoint,    "outputSubset.csv"   },

                { FeatureType.POINT,   SupportedFormat.XML_FILE, ncssParamsAll,      "outputAll.xml"      },
                { FeatureType.POINT,   SupportedFormat.XML_FILE, ncssParamsPoint,    "outputSubset.xml"   },

                { FeatureType.POINT,   SupportedFormat.NETCDF3,  ncssParamsAll,      "outputAll.ncml"     },
                { FeatureType.POINT,   SupportedFormat.NETCDF3,  ncssParamsPoint,    "outputSubset.ncml"  },

                { FeatureType.POINT,   SupportedFormat.NETCDF4,  ncssParamsAll,      "outputAll.ncml"     },
                { FeatureType.POINT,   SupportedFormat.NETCDF4,  ncssParamsPoint,    "outputSubset.ncml"  },

                // Station
                { FeatureType.STATION, SupportedFormat.CSV_FILE, ncssParamsAll,      "outputAll.csv"      },
                { FeatureType.STATION, SupportedFormat.CSV_FILE, ncssParamsStation1, "outputSubset1.csv"  },
                { FeatureType.STATION, SupportedFormat.CSV_FILE, ncssParamsStation2, "outputSubset2.csv"  },

                { FeatureType.STATION, SupportedFormat.GEOCSV_FILE, ncssParamsAll,      "outputAll.csv"      },
                { FeatureType.STATION, SupportedFormat.GEOCSV_FILE, ncssParamsStation1, "outputSubset1.csv"  },
                { FeatureType.STATION, SupportedFormat.GEOCSV_FILE, ncssParamsStation2, "outputSubset2.csv"  },

                { FeatureType.STATION, SupportedFormat.XML_FILE, ncssParamsAll,      "outputAll.xml"      },
                { FeatureType.STATION, SupportedFormat.XML_FILE, ncssParamsStation1, "outputSubset1.xml"  },
                { FeatureType.STATION, SupportedFormat.XML_FILE, ncssParamsStation2, "outputSubset2.xml"  },

                { FeatureType.STATION, SupportedFormat.WATERML2, ncssParamsAll,      "outputAll.xml"      },
                { FeatureType.STATION, SupportedFormat.WATERML2, ncssParamsStation1, "outputSubset1.xml"  },
                { FeatureType.STATION, SupportedFormat.WATERML2, ncssParamsStation2, "outputSubset2.xml"  },

                { FeatureType.STATION, SupportedFormat.NETCDF3,  ncssParamsAll,      "outputAll.ncml"     },
                { FeatureType.STATION, SupportedFormat.NETCDF3,  ncssParamsStation1, "outputSubset1.ncml" },
                { FeatureType.STATION, SupportedFormat.NETCDF3,  ncssParamsStation2, "outputSubset2.ncml" },

                { FeatureType.STATION, SupportedFormat.NETCDF4,  ncssParamsAll,      "outputAll.ncml"     },
                { FeatureType.STATION, SupportedFormat.NETCDF4,  ncssParamsStation1, "outputSubset1.ncml" },
                { FeatureType.STATION, SupportedFormat.NETCDF4,  ncssParamsStation2, "outputSubset2.ncml" },
        });
    }

    private final FeatureType wantedType;
    private final String datasetResource;
    private final SupportedFormat format;
    private final NcssParamsBean ncssParams;
    private final String expectedResultResource;

    public DsgSubsetWriterTest(FeatureType wantedType, SupportedFormat format, NcssParamsBean ncssParams,
            String expectedResultResource) throws URISyntaxException {
        this.wantedType = wantedType;
        this.datasetResource = wantedType.name().toLowerCase() + "/input.ncml";
        this.format = format;
        this.ncssParams = ncssParams;
        this.expectedResultResource =
                wantedType.name().toLowerCase() + "/" + format.name().toLowerCase() + "/" + expectedResultResource;
    }

    @Test
    public void testWrite() throws Exception {
        if ((format == SupportedFormat.NETCDF4/* || format == SupportedFormat.NETCDF4EXT*/) && !isClibraryPresent) {
            return;  // Skip NetCDF 4 test.
        }

        File datasetFile = new File(getClass().getResource(datasetResource).toURI());
        File expectedResultFile = new File(getClass().getResource(expectedResultResource).toURI());

        String extension;
        switch (format) {
            case CSV_FILE: extension = "csv"; break;
            case XML_FILE: // fall through
            case GEOCSV_FILE: extension = "geocsv"; break;
            case WATERML2: extension = "xml"; break;
            case NETCDF3:  extension = "nc";  break;
            case NETCDF4:  extension = "nc4"; break;
            default: throw new AssertionError("Unknown format: " + format);
        }

        String basename = FilenameUtils.getBaseName(expectedResultResource);
        File actualResultFile = File.createTempFile(basename + "_", "." + extension);

        try {
            try (   FeatureDatasetPoint fdPoint = openPointDataset(wantedType, datasetFile);
                    OutputStream outFileStream = new BufferedOutputStream(new FileOutputStream(actualResultFile))) {
                DsgSubsetWriter subsetWriterFile = DsgSubsetWriterFactory.newInstance(
                        fdPoint, ncssParams, diskCache, outFileStream, format);
                subsetWriterFile.write();
            }

            // outFileStream must be closed before we compare actualResultFile.
            // That happens at the end of the try block above.
            if (format == SupportedFormat.NETCDF3 || format == SupportedFormat.NETCDF4) {
                Assert.assertTrue(compareNetCDF(expectedResultFile, actualResultFile));
            } else {
                Assert.assertTrue(compareText(expectedResultFile, actualResultFile));
            }
        } finally {
            if (!actualResultFile.delete()) {
                logger.warn("Failed to delete " + actualResultFile);
            }
        }
    }

    public static boolean compareNetCDF(File expectedResultFile, File actualResultFile) throws IOException {
        try (   NetcdfFile expectedNcFile = NetcdfDataset.openDataset(expectedResultFile.getAbsolutePath());
                NetcdfFile actualNcFile   = NetcdfDataset.openDataset(actualResultFile.getAbsolutePath())) {
            Formatter formatter = new Formatter();
            boolean contentsAreEqual = new CompareNetcdf2(formatter).compare(
                    expectedNcFile, actualNcFile, new NcssNetcdfObjFilter(), false, false, true);

            if (!contentsAreEqual) {
                System.err.println(formatter.toString());
            }

            return contentsAreEqual;
        }
    }

    private static class NcssNetcdfObjFilter implements CompareNetcdf2.ObjFilter {
        @Override
        public boolean attCheckOk(Variable v, Attribute att) {
            return !att.getShortName().equals(CDM.TITLE) &&  // Ignore the "title" attribute.
                   !att.getShortName().equals(CDM.NCPROPERTIES);
        }

        @Override
        public boolean varDataTypeCheckOk(Variable v) {
            return true;  // Check all variables.
        }
    }

    public static boolean compareText(File expectedResultFile, File actualResultFile) throws IOException {
        try (   BufferedReader actualReader   = new BufferedReader(new FileReader(actualResultFile));
                BufferedReader expectedReader = new BufferedReader(new FileReader(expectedResultFile))) {
            return IOUtils.contentEqualsIgnoreEOL(expectedReader, actualReader);
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
