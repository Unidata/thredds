package ucar.nc2.ft.point.remote

import org.apache.commons.io.FilenameUtils
import spock.lang.Specification
import spock.lang.Unroll
import ucar.nc2.constants.FeatureType
import ucar.nc2.ft.FeatureDatasetFactoryManager
import ucar.nc2.ft.FeatureDatasetPoint
import ucar.nc2.ft.PointFeatureCollection
import ucar.nc2.ft.point.FlattenedDatasetPointCollection
import ucar.nc2.ft.point.PointTestUtil
import ucar.unidata.util.test.TestDir

/**
 * @author cwardgar
 * @since 2015/09/21
 */
class PointStreamSpec extends Specification {
    public static final String cfDocDsgExamplesDir = TestDir.cdmLocalTestDataDir + "cfDocDsgExamples/";
    public static final String pointDir = TestDir.cdmLocalTestDataDir + "point/";

    @Unroll  // Method will have its iterations reported independently.
    def "round trip['#location']"() {
        setup:
        File outFile = File.createTempFile(FilenameUtils.getBaseName(location) + "_", ".bin")
        FeatureDatasetPoint fdPoint =
                (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(FeatureType.ANY_POINT, location, null)

        when:
        PointFeatureCollection origPointCol = new FlattenedDatasetPointCollection(fdPoint);
        PointStream.write(origPointCol, outFile);
        PointFeatureCollection roundTrippedPointCol = new PointCollectionStreamLocal(outFile);

        then:
        PointTestUtil.equals(origPointCol, roundTrippedPointCol)

        cleanup:
        fdPoint.close()
        outFile.delete()

        where:
        location << [
                cfDocDsgExamplesDir + "H.1.1.ncml",
                pointDir + "point.ncml",
                pointDir + "pointMissing.ncml",
                pointDir + "pointUnlimited.nc"
        ]
    }
}
