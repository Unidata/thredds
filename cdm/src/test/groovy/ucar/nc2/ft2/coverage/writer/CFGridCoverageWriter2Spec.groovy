package ucar.nc2.ft2.coverage.writer

import org.junit.Assert
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import ucar.ma2.Array
import ucar.ma2.DataType
import ucar.ma2.MAMath
import ucar.nc2.NetcdfFile
import ucar.nc2.NetcdfFileWriter
import ucar.nc2.ft2.coverage.CoverageCollection
import ucar.nc2.ft2.coverage.CoverageDatasetFactory
import ucar.nc2.ft2.coverage.FeatureDatasetCoverage
import ucar.nc2.ft2.coverage.HorizCoordSysCrossSeamBoundarySpec

/**
 * Tests that CFGridCoverageWriter2 properly adds 2D lat/lon variables to output file when {@code addLatLon == true}.
 *
 * @author cwardgar
 * @since 2018-03-17
 */
class CFGridCoverageWriter2Spec extends Specification {
    def "calc output file sizes with and without 2D lat/lon"() {
        def numY = 4, numX = 4  // Lengths of the y and x axes in the dataset.
        
        setup: "Open test resource as FeatureDatasetCoverage"
        File testFile = new File(HorizCoordSysCrossSeamBoundarySpec.getResource("crossSeamProjection.ncml").toURI())
        FeatureDatasetCoverage featDsetCov = CoverageDatasetFactory.open(testFile.absolutePath)
        
        and: "assert that featDsetCov was opened without failure and get its 1 CoverageCollection"
        assert featDsetCov != null
        assert featDsetCov.getCoverageCollections().size() == 1
        CoverageCollection covColl = featDsetCov.getCoverageCollections().get(0)
        
        when: "calculate expected size excluding 2D lat/lon vars"
        long expectedSizeNoLatLon =
                numY * numX * DataType.FLOAT.size +  // Temperature_surface
                numY * DataType.FLOAT.size +         // y
                numX * DataType.FLOAT.size +         // x
                1 * DataType.INT.size                // PolarStereographic_Projection
        
        and: "calculate actual size excluding 2D lat/lon vars"
        long actualSizeNoLatLon = CFGridCoverageWriter2.writeOrTestSize(
                // No subset; don't addLatLon; calc file size but don't write file.
                covColl, null, null, false, true, null).get()
        
        then: "expected equals actual"
        expectedSizeNoLatLon == actualSizeNoLatLon
    
        when: "calculate expected size including 2D lat/lon vars"
        long expectedSizeWithLatLon = expectedSizeNoLatLon +
                numY * numX * DataType.DOUBLE.size +          // lat
                numY * numX * DataType.DOUBLE.size            // lon
    
        and: "calculate actual size excluding 2D lat/lon vars"
        long actualSizeWithLatLon = CFGridCoverageWriter2.writeOrTestSize(
                // No subset; do addLatLon; calc file size but don't write file.
                covColl, null, null, true, true, null).get()
    
        then: "expected equals actual"
        expectedSizeWithLatLon == actualSizeWithLatLon
        
        cleanup: "close all resources"
        featDsetCov?.close()
    }
    
    @Rule public final TemporaryFolder tempFolder = new TemporaryFolder()
    
    def "CFGridCoverageWriter2 properly adds 2D lat/lon variables"() {
        setup: "Open test resource as FeatureDatasetCoverage"
        File testFile = new File(HorizCoordSysCrossSeamBoundarySpec.getResource("crossSeamProjection.ncml").toURI())
        FeatureDatasetCoverage featDsetCov = CoverageDatasetFactory.open(testFile.absolutePath)
    
        and: "assert that featDsetCov was opened without failure and get its 1 CoverageCollection"
        assert featDsetCov != null
        assert featDsetCov.getCoverageCollections().size() == 1
        CoverageCollection covColl = featDsetCov.getCoverageCollections().get(0)
        
        and: "setup NetcdfFileWriter"
        File outputFile = tempFolder.newFile()
        NetcdfFileWriter writer = NetcdfFileWriter.createNew(outputFile.absolutePath, false)
        
        and: "write output file"
        // No subset; do addLatLon; write to outputFile.
        CFGridCoverageWriter2.writeOrTestSize(covColl, null, null, true, false, writer)
        
        and: "open output file"
        NetcdfFile ncFile = NetcdfFile.open(outputFile.absolutePath)
        
        and: "declare expected lats"
        def expectedShape = [4, 4] as int[]
        def expectedLatsList = [  // These come from crossSeamLatLon2D.ncml
                48.771275207078986, 56.257940168398875, 63.23559652027781, 68.69641273007204,
                51.52824383539942,  59.91283563136657,  68.26407960692367, 75.7452461192097,
                52.765818800755305, 61.615297053148296, 70.80822358575152, 80.19456756234185,
                52.28356434154232,  60.94659393490472,  69.78850194830888, 78.27572828144659
        ]
        Array expectedLats = Array.factory(DataType.DOUBLE, expectedShape, expectedLatsList as double[])
        
        and: "declare expected lons"
        def expectedLonsList = [  // These come from crossSeamLatLon2D.ncml
                -168.434948822922,   -161.3099324740202,  -150.0,              -131.56505117707798,
                -179.6237487511738,  -174.86369657175186, -166.1892062570269,  -147.27368900609372,
                 167.86240522611175,  168.81407483429038,  170.71059313749964,  176.3099324740202,
                 155.0737544933483,   151.8659776936037,   145.70995378081128,  130.00797980144134
        ]
        Array expectedLons = Array.factory(DataType.DOUBLE, expectedShape, expectedLonsList as double[])
        
        when: "read actual latitudes"
        Array actualLats = ncFile.findVariable("lat").read()
        
        then: "expected nearly equals actual"
        Assert.assertArrayEquals(expectedLats.shape, actualLats.shape)
        MAMath.nearlyEquals(expectedLats, actualLats)
        
        when: "read actual longitudes"
        Array actualLons = ncFile.findVariable("lon").read()
        
        then: "expected nearly equals actual"
        Assert.assertArrayEquals(expectedLons.shape, actualLons.shape)
        MAMath.nearlyEquals(expectedLons, actualLons)
        
        cleanup: "close all resources"
        ncFile?.close()
        writer?.close()
        featDsetCov?.close()
    }
}
