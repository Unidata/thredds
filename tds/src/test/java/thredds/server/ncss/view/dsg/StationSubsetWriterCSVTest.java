package thredds.server.ncss.view.dsg;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import thredds.server.ncss.params.NcssParamsBean;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.util.DiskCache2;

import java.io.*;
import java.util.Arrays;
import java.util.Formatter;

/**
 * Created by cwardgar on 2014/05/27.
 */
public class StationSubsetWriterCSVTest {
    @Test
    public void testWriteAll() throws Exception {
        File testFile = new File(getClass().getResource("multiStationMultiVar.ncml").toURI());
        FeatureDatasetPoint fdPoint = openPointDataset(testFile);

        NcssParamsBean ncssParams = createParams();

        InputStream expectedInputStream = getClass().getResourceAsStream("stationCsvAll.txt");
        try {
            ByteArrayOutputStream actualOutputStream = new ByteArrayOutputStream();
            DsgSubsetWriter subsetWriter = new StationSubsetWriterCSV(actualOutputStream, false);
            subsetWriter.write(fdPoint, ncssParams, DiskCache2.getDefault());

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
                FeatureType.ANY_POINT, datasetFile.getAbsolutePath(), null, errlog);

        assert fDset != null : "No factory found: " + errlog.toString();
        return (FeatureDatasetPoint) fDset;
    }

    public NcssParamsBean createParams() {
        NcssParamsBean ncssParams = new NcssParamsBean();

        ncssParams.setAccept("csv");
        ncssParams.setVar(Arrays.asList("pr", "tas"));
        ncssParams.setTime_start("1970-01-01T00:00:00Z");
        ncssParams.setTime_end("1970-02-10T00:00:00Z");
        ncssParams.setNorth(44.0);
        ncssParams.setSouth(40.0);
        ncssParams.setEast(-94.0);
        ncssParams.setWest(-100.0);

        return ncssParams;
    }
}
