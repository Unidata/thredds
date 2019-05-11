package ucar.nc2.jni.netcdf

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Specification
import spock.lang.Unroll
import ucar.ma2.Array
import ucar.ma2.ArrayChar
import ucar.ma2.DataType
import ucar.nc2.Attribute
import ucar.nc2.Dimension
import ucar.nc2.NetcdfFile
import ucar.nc2.NetcdfFileWriter
import ucar.nc2.Variable

/**
 * Tests miscellaneous aspects of Nc4Iosp.
 *
 * @author cwardgar
 * @since 2017-03-27
 */
class Nc4IospMiscSpec extends Specification {
    private static final Logger logger = LoggerFactory.getLogger(Nc4IospMiscSpec)
    
    @Rule TemporaryFolder tempFolder = new TemporaryFolder()
    
    /*
     * Demonstrates bug from http://www.unidata.ucar.edu/mailing_lists/archives/netcdf-java/2017/msg00012.html
     * Prior to fix, this test would fail for 'u_short', 'u_int', and 'u_long' variables with
     * "Unknown userType == 8", "Unknown userType == 9", and "Unknown userType == 11" errors respectively.
     */
    @Unroll  // Report iterations of method independently.
    def "Nc4Iosp.readDataSection() can read '#varName' variables"() {
        setup: "locate test file"
        File file = new File(this.class.getResource("unsigned.nc4").toURI())
        assert file.exists()
        
        and: "open it as a NetcdfFile using Nc4Iosp"
        NetcdfFile ncFile = NetcdfFile.open(file.absolutePath, Nc4Iosp.class.canonicalName, -1, null, null)
        
        and: "grab the Nc4Iosp instance within so that we can test Nc4Iosp.readDataSection()"
        Nc4Iosp nc4Iosp = ncFile.iosp as Nc4Iosp
        
        when: "read all of var's data using readDataSection()"
        Variable var = ncFile.findVariable(varName)
        Nc4Iosp.Vinfo vinfo = var.SPobject as Nc4Iosp.Vinfo
        Array array = nc4Iosp.readDataSection(vinfo.g4.grpid, vinfo.varid, vinfo.typeid, var.shapeAsSection);
        
        then: "actual data equals expected data"
        array.storage == expectedData
    
        cleanup: "close NetcdfFile"
        ncFile?.close()
        
        where: "data are too big for their type. Overflow expected because Java doesn't support unsigned types"
        varName << [ "u_byte", "u_short", "u_int", "u_long" ]
        expectedData << [
                [(1  << 7),  (1  << 7)  + 1, (1  << 7)  + 2] as byte[],  // Will overflow to [-128, -127, -126]
                [(1  << 15), (1  << 15) + 1, (1  << 15) + 2] as short[],
                [(1  << 31), (1  << 31) + 1, (1  << 31) + 2] as int[],
                [(1L << 63), (1L << 63) + 1, (1L << 63) + 2] as long[]
        ];
    }
    
    /*
     * Demonstrates bug from
     * https://andy.unidata.ucar.edu/esupport/staff/index.php?_m=tickets&_a=viewticket&ticketid=28098
     * Prior to fix, primary2Dim and primary3Dim were not being identified as unlimited.
     */
    def "Nc4Iosp supports multiple groups, each containing an unlimited dimension"() {
        setup: "locate test file"
        File file = new File(this.class.getResource("DBP-690959.nc4").toURI())
        assert file.exists()
    
        and: "open it as a NetcdfFile using Nc4Iosp"
        NetcdfFile ncFile = NetcdfFile.open(file.absolutePath, Nc4Iosp.class.canonicalName, -1, null, null)
    
        and: "find unlimited dimensions"
        Dimension primary1Dim = ncFile.findDimension("/group1/primary")
        Dimension primary2Dim = ncFile.findDimension("/group2/primary")
        Dimension primary3Dim = ncFile.findDimension("/group3/primary")
        
        expect: "all dimensions are unlimited"
        primary1Dim.isUnlimited()
        primary2Dim.isUnlimited()
        primary3Dim.isUnlimited()
    
        cleanup: "close NetcdfFile"
        ncFile?.close()
    }

    def "create NetCDF-4 file with unlimited dimension"() {
        setup: "create temp file that will be deleted after test by TemporaryFolder @Rule"
        File tempFile = new File(tempFolder.root, "Nc4IospMiscSpec.nc4")

        and: "open a NetcdfFileWriter that will write NetCDF-4 to tempFile"
        NetcdfFileWriter ncWriter = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4, tempFile.absolutePath)

        and: "add an unlimited dimension and create the file on disk"
        Dimension dimBefore = ncWriter.addDimension(null, "dim", 3, true, false)
        ncWriter.create()

        and: "close the file for writing and reopen it for reading"
        ncWriter.close()
        NetcdfFile ncFile = NetcdfFile.open(tempFile.absolutePath)

        expect: "the dimension is the same after the write/read round-trip"
        Dimension dimAfter = ncFile.findDimension(dimBefore.fullName)
        // Failed prior to fix, because dimAfter was not unlimited.
        dimBefore.equals dimAfter

        cleanup: "close writer and reader"
        ncWriter?.close()  // Under normal circumstances, this will already be closed. Luckily method is idempotent.
        ncFile?.close()
    }

    def "create NetCDF-4 file null valued attributes"() {
        setup: "create temp file that will be deleted after test by TemporaryFolder @Rule"
        File tempFile = new File(tempFolder.root, "Nc4IospMiscSpec.nc4")

        and: "open a NetcdfFileWriter that will write NetCDF-4 to tempFile"
        NetcdfFileWriter ncWriter = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4, tempFile.absolutePath)

        and: "add a numerical valued attribute with a null value"
        Attribute attrNum = new Attribute("nullvalnum", DataType.INT)
        Attribute attrNumBefore = ncWriter.addGlobalAttribute(attrNum)

        and: "add a string valued attribute with a null value"
        Attribute attrStr = new Attribute("nullvalstr", DataType.STRING)
        Attribute attrStrBefore = ncWriter.addGlobalAttribute(attrStr)

        and: "add a character valued attribute with a null value"
        Attribute attrChar = new Attribute("nullvalchar", DataType.CHAR)
        Attribute attrCharBefore = ncWriter.addGlobalAttribute(attrChar)

        and: "add a character valued attribute with a specific null char value"
        Attribute attrNullChar = new Attribute("nullcharvalchar", DataType.CHAR)
        Array attrNullCharValue = ArrayChar.makeFromString("\0", 1);
        attrNullChar.setValues(attrNullCharValue)
        Attribute attrNullCharBefore = ncWriter.addGlobalAttribute(attrNullChar)
        ncWriter.create()

        and: "close the file for writing and reopen it for reading"
        ncWriter.close()
        NetcdfFile ncFile = NetcdfFile.open(tempFile.absolutePath)

        expect: "the value of the attributes are null"
        Attribute attrNumAfter = ncFile.findGlobalAttribute(attrNumBefore.fullName)
        attrNumBefore.getValues().equals attrNumAfter.getValues()
        attrNumBefore.getValues() == null

        Attribute attrStrAfter = ncFile.findGlobalAttribute(attrStrBefore.fullName)
        attrStrBefore.getValues().equals attrStrAfter.getValues()
        attrStrBefore.getValues() == null

        Attribute attrCharAfter = ncFile.findGlobalAttribute(attrCharBefore.fullName)
        attrCharBefore.getValues().equals attrCharAfter.getValues()
        attrCharBefore.getValues() == null

        Attribute attrNullCharAfter = ncFile.findGlobalAttribute(attrNullCharBefore.fullName)
        attrNullCharBefore.getValues().getSize() == attrNullCharAfter.getValues().getSize()
        attrNullCharBefore.getValues().getSize() == 1
        attrNullCharBefore.getValue(0).equals(attrNullCharAfter.getValue(0))
        attrNullCharBefore.equals(attrNullCharAfter)

        cleanup: "close writer and reader"
        ncWriter?.close()  // Under normal circumstances, this will already be closed. Luckily method is idempotent.
        ncFile?.close()
    }
}
