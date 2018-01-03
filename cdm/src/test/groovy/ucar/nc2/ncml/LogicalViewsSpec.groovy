package ucar.nc2.ncml

import spock.lang.Shared
import spock.lang.Specification
import ucar.nc2.Variable
import ucar.nc2.dataset.NetcdfDataset

/**
 * Tests the logical view NcML elements: logicalSection, logicalSlice, and logicalReduce.
 *
 * @author cwardgar
 * @since 2018-01-03
 */
class LogicalViewsSpec extends Specification {
    @Shared
    NetcdfDataset ncDataset
    
    def setupSpec() {
        File testFile = new File(getClass().getResource("afterLogicalViews.ncml").toURI())
        ncDataset = NetcdfDataset.openDataset(testFile.absolutePath)
    }
    
    def cleanupSpec() {
        ncDataset.close()
    }
    
    def "logicalSection"() {
        setup: "get logicalSection of the original 'section' variable"
        Variable sectionVar = ncDataset.findVariable("section")

        expect: "new shape is the same as the section's shape (':,0:2:2,1:2')"
        sectionVar.getShape() as List == [2, 2, 2]  // Shape was originally [ 2, 3, 4 ].
        
        and: "data only includes values that are part of the section"
        sectionVar.read().storage as List == [2, 3, 10, 11, 14, 15, 22, 23]
    }
    
    def "logicalSlice"() {
        setup: "get logicalSlice of the original 'slice' variable"
        Variable sliceVar = ncDataset.findVariable("slice")
    
        expect: "new shape excludes the dimension we sliced out"
        sliceVar.getShape() as List == [3, 4]  // Shape was originally [ 2, 3, 4 ].
    
        and: "data only includes values that are part of the slice"
        sliceVar.read().storage as List == [13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24]
    }
    
    def "logicalReduce"() {
        setup: "get logicalReduce of the original 'reduce' variable"
        Variable reduceVar = ncDataset.findVariable("reduce")
    
        expect: "new shape excludes the dimensions of length==1"
        reduceVar.getShape() as List == [2, 3]  // Shape was originally [ 2, 1, 3, 1 ].
    
        and: "data includes all of the values from the original variable"
        reduceVar.read().storage as List == [1, 2, 3, 4, 5, 6]
    }
}
