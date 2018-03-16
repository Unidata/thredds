package ucar.nc2.ft2.coverage

import spock.lang.Specification

/**
 * Tests BoundariesAsGisText
 *
 * @author cwardgar
 * @since 2018-03-13
 */
class HorizCoordSysGisTextBoundarySpec extends Specification {
    // Builds on HorizCoordSysCrossSeamBoundarySpec."calcConnectedLatLonBoundaryPoints(2, 3) - lat/lon 1D"()
    def "getLatLonBoundaryAsWKT(2, 3) - lat/lon 1D"() {
        setup: "get the HorizCoordSys of the dataset"
        HorizCoordSys horizCoordSys = getHorizCoordSysOfDataset("crossSeamLatLon1D.ncml")
        
        and: "get actual WKT"
        String actualWKT = horizCoordSys.getLatLonBoundaryAsWKT(2, 3)
        
        and: "declare expected WKT"
        String expectedWKT = "POLYGON((" +
                                "130.000 0.000, 170.000 0.000, 210.000 0.000, " +     // Bottom edge
                                "230.000 0.000, 230.000 30.000, " +                   // Right edge
                                "230.000 50.000, 190.000 50.000, 150.000 50.000, " +  // Top edge
                                "130.000 50.000, 130.000 20.000" +                    // Left edge
                             "))"
    
        expect: "expected equals actual"
        actualWKT == expectedWKT
        println actualWKT
    }
    
    // Builds on HorizCoordSysCrossSeamBoundarySpec."calcConnectedLatLonBoundaryPoints(2, 2) - lat/lon 2D"()
    def "getLatLonBoundaryAsGeoJSON(2, 2) - lat/lon 2D"() {
        setup: "get the HorizCoordSys of the dataset"
        HorizCoordSys horizCoordSys = getHorizCoordSysOfDataset("crossSeamLatLon2D.ncml")
    
        and: "get actual GeoJSON"
        String actualGeoJSON = horizCoordSys.getLatLonBoundaryAsGeoJSON(2, 2)
    
        and: "declare expected GeoJSON"
        String expectedGeoJSON = "{ 'type': 'Polygon', 'coordinates': [ [ " +
                                    "[-169.527, 44.874], [-145.799, 58.685], " +  // Bottom edge
                                    "[-106.007, 69.750], [-162.839, 82.356], " +  // Right edge
                                    "[-252.973, 85.132], [-221.068, 66.429], " +  // Top edge
                                    "[-206.411, 48.271], [-188.523, 47.761]"   +  // Left edge
                                 " ] ] }"
    
        expect: "expected equals actual"
        actualGeoJSON == expectedGeoJSON
        println actualGeoJSON
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
}
