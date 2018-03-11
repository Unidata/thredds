package ucar.nc2.ft2.coverage

import spock.lang.Specification
import ucar.unidata.geoloc.LatLonPointImpl
import ucar.unidata.geoloc.LatLonPointNoNormalize
import ucar.unidata.geoloc.LatLonRect
import ucar.unidata.geoloc.ProjectionPoint
import ucar.unidata.geoloc.ProjectionPointImpl
import ucar.unidata.geoloc.ProjectionRect

/**
 * Asserts that HorizCoordSys calculates correct boundaries for coverages that cross the international date line.
 * Tests for projection, latLon1D, and latLon2D CRSs are included. The projection and latLon2D datasets are based
 * on a PolarStereographic_Projection.
 *
 * @author cwardgar
 * @since 2018-02-26
 */
class HorizCoordSysCrossSeamBoundarySpec extends Specification {
    def "calcProjectionBoundaryPoints()"() {
        setup: "get the HorizCoordSys of the dataset"
        HorizCoordSys horizCoordSys = getHorizCoordSysOfDataset("crossSeamProjection.ncml")
    
        and: "get actual points"
        List<ProjectionPoint> actualPoints = horizCoordSys.calcProjectionBoundaryPoints()
        
        and: "declare expected points"
        List<ProjectionPoint> expectedPoints = convertCoordsToPoints(false, [
                [-2450, -4500], [-2450, -3500], [-2450, -2500], [-2450, -1500],  // Bottom edge
                [-2450, -500],  [-1550, -500],  [-650, -500],   [250, -500],     // Right edge
                [1150, -500],   [1150, -1500],  [1150, -2500],  [1150, -3500],   // Top edge
                [1150, -4500],  [250, -4500],   [-650, -4500],  [-1550, -4500],  // Left edge
        ])
        
        expect: "expected equals actual"
        expectedPoints == actualPoints
    }
    
    def "calcProjectionBoundaryPoints(2, 2)"() {
        setup: "get the HorizCoordSys of the dataset"
        HorizCoordSys horizCoordSys = getHorizCoordSysOfDataset("crossSeamProjection.ncml")
    
        and: "get actual points"
        // Results in strideY == 2 and strideX == 2.
        List<ProjectionPoint> actualPoints = horizCoordSys.calcProjectionBoundaryPoints(2, 2)
    
        and: "declare expected points"
        List<ProjectionPoint> expectedPoints = convertCoordsToPoints(false, [
                [-2450, -4500], [-2450, -2500],  // Bottom edge
                [-2450, -500],  [-650, -500],    // Right edge
                [1150, -500],   [1150, -2500],   // Top edge
                [1150, -4500],  [-650, -4500],   // Left edge
        ])
    
        expect: "expected equals actual"
        expectedPoints == actualPoints
    }
    
    def "calcConnectedLatLonBoundaryPoints() - lat/lon 1D"() {
        setup: "get the HorizCoordSys of the dataset"
        HorizCoordSys horizCoordSys = getHorizCoordSysOfDataset("crossSeamLatLon1D.ncml")
        
        and: "get actual points"
        List<LatLonPointNoNormalize> actualPoints = horizCoordSys.calcConnectedLatLonBoundaryPoints()
        
        and: "declare expected points"
        List<LatLonPointNoNormalize> expectedPoints = convertCoordsToPoints(true, [
                [0, 130],  [0, 150],  [0, 170],  [0, 190],  [0, 210],   // Bottom edge
                [0, 230],  [10, 230], [20, 230], [30, 230], [40, 230],  // Right edge
                [50, 230], [50, 210], [50, 190], [50, 170], [50, 150],  // Top edge
                [50, 130], [40, 130], [30, 130], [20, 130], [10, 130]   // Left edge
        ])
        
        expect: "expected equals actual"
        expectedPoints == actualPoints
    }
    
    def "calcConnectedLatLonBoundaryPoints(2, 3) - lat/lon 1D"() {
        setup: "get the HorizCoordSys of the dataset"
        HorizCoordSys horizCoordSys = getHorizCoordSysOfDataset("crossSeamLatLon1D.ncml")
        
        and: "get actual points"
        // Results in strideY == 3 and strideX == 2.
        List<LatLonPointNoNormalize> actualPoints = horizCoordSys.calcConnectedLatLonBoundaryPoints(2, 3)
        
        and: "declare expected points"
        List<LatLonPointNoNormalize> expectedPoints = convertCoordsToPoints(true, [
                [0, 130],  [0, 170],  [0, 210],   // Bottom edge
                [0, 230],  [30, 230],             // Right edge
                [50, 230], [50, 190], [50, 150],  // Top edge
                [50, 130], [20, 130],             // Left edge
        ])
        
        expect: "expected equals actual"
        expectedPoints == actualPoints
    }
    
    def "calcConnectedLatLonBoundaryPoints() - projection"() {
        setup: "get the HorizCoordSys of the dataset"
        HorizCoordSys horizCoordSys = getHorizCoordSysOfDataset("crossSeamProjection.ncml")
        
        and: "get actual points"
        List<LatLonPointNoNormalize> actualPoints = horizCoordSys.calcConnectedLatLonBoundaryPoints()
        
        and: "declare expected points"
        List<LatLonPointNoNormalize> expectedPoints = convertCoordsToPoints(true, [
                [43.3711, -166.4342], [50.4680, -160.0080], [57.1887, -150.5787], [62.8319, -136.4768],  // Bottom edge
                [66.2450, -116.5346], [74.3993, -122.8787], [82.1083, -142.5686], [84.6159, -221.5651],  // Right edge
                [77.9578, -261.5014], [71.9333, -232.4762], [63.9355, -219.7024], [55.5660, -213.1890],  // Top edge
                [47.3219, -209.3354], [48.4777, -198.1798], [48.1430, -186.7808], [46.3647, -175.9940]   // Left edge
        ])
        
        expect: "same number of actualPoints as expectedPoints"
        actualPoints.size() == expectedPoints.size()
        
        and: "corresponding points are nearly equal"
        for (int i = 0; i < actualPoints.size(); ++i) {
            assert actualPoints[i].nearlyEquals(expectedPoints[i], 1e-5)
        }
    }
    
    def "calcConnectedLatLonBoundaryPoints() - lat/lon 2D"() {
        setup: "get the HorizCoordSys of the dataset"
        HorizCoordSys horizCoordSys = getHorizCoordSysOfDataset("crossSeamLatLon2D.ncml")
    
        and: "get actual points"
        List<LatLonPointNoNormalize> actualPoints = horizCoordSys.calcConnectedLatLonBoundaryPoints()
        println actualPoints
    
        and: "declare expected points"
        List<LatLonPointNoNormalize> expectedPoints = convertCoordsToPoints(true, [
                // Verified by visually inspecting the coverage drawing in ToolsUI.
                // Note how these boundary points differ from the ones we calculated in the test above, even though
                // "crossSeamProjection.ncml" and "crossSeamLatLon2D.ncml" represent the same grid. That's because the
                // edges in "calcConnectedLatLonBoundaryPoints() - projection" were calculated in projection coordinates
                // and THEN converted to lat/lon. THESE edges, on the other hand, were calculated from 2D lat/lon
                // midpoints generated from the projection. Taking that path, there's an unavoidable loss of precision.
                [44.8740, -169.5274], [51.7795, -157.6634], [58.6851, -145.7993], [64.2176, -125.9033],  // Bottom edge
                [69.7501, -106.0074], [76.0530, -134.4232], [82.3559, -162.8391], [83.7438, -207.9060],  // Right edge
                [85.1317, -252.9728], [75.7804, -237.0202], [66.4291, -221.0677], [57.3500, -213.7392],  // Top edge
                [48.2709, -206.4107], [48.0159, -197.4671], [47.7609, -188.5235], [46.3175, -179.0254],  // Left edge
        ])
    
        expect: "same number of actualPoints as expectedPoints"
        actualPoints.size() == expectedPoints.size()
    
        and: "corresponding points are nearly equal"
        for (int i = 0; i < actualPoints.size(); ++i) {
            assert actualPoints[i].nearlyEquals(expectedPoints[i], 1e-5)
        }
    }
    
    def "calcConnectedLatLonBoundaryPoints(2, 2) - lat/lon 2D"() {
        setup: "get the HorizCoordSys of the dataset"
        HorizCoordSys horizCoordSys = getHorizCoordSysOfDataset("crossSeamLatLon2D.ncml")
        
        and: "get actual points"
        // Results in strideY == 2 and strideX == 2.
        List<LatLonPointNoNormalize> actualPoints = horizCoordSys.calcConnectedLatLonBoundaryPoints(2, 2)
        
        and: "declare expected points"
        List<LatLonPointNoNormalize> expectedPoints = convertCoordsToPoints(true, [
                [44.8740, -169.5274], [58.6851, -145.7993],  // Bottom edge
                [69.7501, -106.0074], [82.3559, -162.8391],  // Right edge
                [85.1317, -252.9728], [66.4291, -221.0677],  // Top edge
                [48.2709, -206.4107], [47.7609, -188.5235],  // Left edge
        ])
        
        expect: "same number of actualPoints as expectedPoints"
        actualPoints.size() == expectedPoints.size()
        
        and: "corresponding points are nearly equal"
        for (int i = 0; i < actualPoints.size(); ++i) {
            assert actualPoints[i].nearlyEquals(expectedPoints[i], 1e-5)
        }
    }
    
    
    def "calcProjectionBoundingBox"() {
        setup: "get the HorizCoordSys of the dataset"
        HorizCoordSys horizCoordSys = getHorizCoordSysOfDataset("crossSeamProjection.ncml")
        
        and: "get actual bounding box"
        ProjectionRect actualBB = horizCoordSys.calcProjectionBoundingBox()
        
        and: "declare expected bounding box"
        ProjectionRect expectedBB = new ProjectionRect(
                new ProjectionPointImpl(-4500, -2450), new ProjectionPointImpl(-500, 1150))
    
        expect: "expected equals actual"
        expectedBB == actualBB
    }
    
    def "calcLatLonBoundingBox - 1DLatLon"() {
        setup: "get the HorizCoordSys of the dataset"
        HorizCoordSys horizCoordSys = getHorizCoordSysOfDataset("crossSeamLatLon1D.ncml")
        
        and: "get actual bounding box"
        LatLonRect actualBB = horizCoordSys.calcLatLonBoundingBox()
        
        and: "declare expected bounding box"
        // Derived by manually finding the minimum and maximum lat & lon values of the expected points in the
        // "calcConnectedLatLonBoundaryPoints() - lat/lon 1D" test.
        LatLonRect expectedBB = new LatLonRect(new LatLonPointImpl(0, 130), new LatLonPointImpl(50, 230))
        
        expect: "expected equals actual"
        expectedBB == actualBB
    }
    
    def "calcLatLonBoundingBox - projection"() {
        setup: "get the HorizCoordSys of the dataset"
        HorizCoordSys horizCoordSys = getHorizCoordSysOfDataset("crossSeamProjection.ncml")
    
        and: "get actual bounding box"
        LatLonRect actualBB = horizCoordSys.calcLatLonBoundingBox()
    
        and: "declare expected bounding box"
        // Derived by manually finding the minimum and maximum lat & lon values of the expected points in the
        // "calcConnectedLatLonBoundaryPoints() - projection" test.
        LatLonRect expectedBB = new LatLonRect(
                new LatLonPointImpl(43.3711, -261.5014), new LatLonPointImpl(84.6159, -116.5346))
    
        expect: "expected equals actual"
        expectedBB.nearlyEquals(actualBB)
    }
    
    def "calcLatLonBoundingBox - 2DLatLon"() {
        setup: "get the HorizCoordSys of the dataset"
        HorizCoordSys horizCoordSys = getHorizCoordSysOfDataset("crossSeamLatLon2D.ncml")
        
        and: "get actual bounding box"
        LatLonRect actualBB = horizCoordSys.calcLatLonBoundingBox()
        
        and: "declare expected bounding box"
        // Derived by manually finding the minimum and maximum lat & lon values of the expected points in the
        // "calcConnectedLatLonBoundaryPoints() - lat/lon 2D" test.
        LatLonRect expectedBB = new LatLonRect(
                new LatLonPointImpl(44.8740, -252.9728), new LatLonPointImpl(85.1317, -106.0074))
        
        expect: "expected equals actual"
        expectedBB.nearlyEquals(actualBB)
    }
    
    
    private HorizCoordSys getHorizCoordSysOfDataset(String resourceName) {
        File file = new File(getClass().getResource(resourceName).toURI())
    
        CoverageDatasetFactory.open(file.absolutePath).withCloseable { FeatureDatasetCoverage featDsetCov ->
            // Assert that featDsetCov was opened without failure and it contains 1 CoverageCollection.
            assert featDsetCov != null
            assert featDsetCov.getCoverageCollections().size() == 1
    
            // Return HorizCoordSys from single CoverageCollection.
            CoverageCollection covColl = featDsetCov.getCoverageCollections().get(0)
            return covColl.getHorizCoordSys()
        }
    }
    
    // Each List<Double> contains coords in "y x" order.
    private def convertCoordsToPoints(boolean coordsAreLatLons, List<List<Double>> coords) {
        def points = []
        coords.each { List<Double> coord ->
            if (coordsAreLatLons) {
                points << new LatLonPointNoNormalize(coord.get(0), coord.get(1))
            } else {
                points << new ProjectionPointImpl(coord.get(1), coord.get(0))
            }
        }
        return points
    }
}
