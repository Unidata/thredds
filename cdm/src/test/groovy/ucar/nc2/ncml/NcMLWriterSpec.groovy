package ucar.nc2.ncml

import com.google.common.base.Predicate
import com.google.common.base.Predicates
import org.jdom2.Element
import org.jdom2.Namespace
import org.jdom2.output.Format
import spock.lang.Shared
import spock.lang.Specification
import ucar.ma2.Array
import ucar.ma2.DataType
import ucar.nc2.*
import ucar.nc2.dataset.NetcdfDataset

/**
 * @author cwardgar
 * @since 2015/08/05
 */
class NcMLWriterSpec extends Specification {
    @Shared
    NetcdfFile ncFile

    /* Programmatically creates a NetcdfFile with the following (pseudo) CDL:
netcdf {
  types:
    short enum dessertType { 'pie' = 18, 'donut' = 268, 'cake' = 3284};
  dimensions:
    time = UNLIMITED;   // (3 currently)
  variables:
    enum dessertType dessert(time=3);
      :zero = ; // long
    short time(time=3);
    char charVar(5);
    String stringVar(4);
  group: recordsGroup {
    variables:
      Structure {
        int recordsVar(3);
      } recordsStruct(*);
    // group attributes:
    :stooges = "Moe Howard", "Larry Fine", "Curly Howard";
  }
  // global attributes:
  :primes = 2U, 3U, 5U, 7U, 11U; // int
}
data:
dessert =
  {18, 268, 3284}
time =
  {4, 5, 6}
charVar = "abcde"
stringVar = "Frodo Baggins", "Samwise Gamgee", "Meriadoc Brandybuck", "Peregrin Took"
recordsGroup/recordsStruct = UNREADABLE
     */
    def setupSpec() {
        setup: "NetcdfFile's 0-arg constructor is protected, so must use NetcdfFileSubclass"
        ncFile = new NetcdfFileSubclass()

        and: "create shared, unlimited Dimension"
        Dimension timeDim = new Dimension("time", 3, true, true, false)
        ncFile.addDimension(null, timeDim)

        and: "create EnumTypedef and add it to root group"
        EnumTypedef dessertType = new EnumTypedef("dessertType", [18: 'pie', 268: 'donut', 3284: 'cake'], DataType.ENUM2)
        ncFile.getRootGroup().addEnumeration(dessertType)

        and: "create Variable of type dessertType and add it"
        Variable dessert = new Variable(ncFile, null, null, "dessert", DataType.ENUM2, "time")
        dessert.enumTypedef = dessertType
        dessert.addAttribute(new Attribute("zero", DataType.ULONG))  // unsigned, zero-length, LONG attribute
        short[] dessertStorage = [18, 268, 3284] as short[]
        dessert.setCachedData(Array.factory(DataType.SHORT, [3] as int[], dessertStorage), true)  // Irregularly-spaced values
        ncFile.addVariable(null, dessert)

        and: "create 'time' coordinate Variable"
        Variable time = new Variable(ncFile, null, null, "time", DataType.SHORT, "time")
        short[] timeStorage = [4, 5, 6] as short[]
        time.setCachedData(Array.factory(DataType.SHORT, [3] as int[], timeStorage), false)
        ncFile.addVariable(null, time)

        and: "create char-valued Variable with anonymous Dimension"
        Variable charVar = new Variable(ncFile, null, null, "charVar", DataType.CHAR, "5")
        char[] charStorage = ['a', 'b', 'c', 'd', 'e'] as char[]
        charVar.setCachedData(Array.factory(DataType.CHAR, [5] as int[], charStorage), true)
        ncFile.addVariable(null, charVar)

        and: "create string-valued Variable"
        Variable stringVar = new Variable(ncFile, null, null, "stringVar", DataType.STRING, "4")
        String[] stringStorage = ['Frodo Baggins', 'Samwise Gamgee', 'Meriadoc Brandybuck', 'Peregrin Took'] as String[]
        stringVar.setCachedData(Array.factory(DataType.STRING, [4] as int[], stringStorage), true)
        ncFile.addVariable(null, stringVar)

        and: "create Group for records"
        Group recordsGroup = new Group(ncFile, null, "recordsGroup")
        ncFile.addGroup(null, recordsGroup)

        and: "create unreadable Structure with variable-length dimension and add it to recordsGroup"
        // recordsStruct will be unreadable because we don't cache any data for it. In fact, it's not even possible
        // to cache data for Structures because ArrayStructure.copy() is unsupported, and caching needs that.
        // Besides, there's no sensible way to represent a n>1-dimensional Structure's values in NcML anyway.
        Structure recordsStruct = new Structure(ncFile, null, null, "recordsStruct")
        Dimension numRecords = new Dimension("numRecords", -1, false, false, true)  // Variable-length dim
        recordsStruct.setDimensions([numRecords])
        recordsGroup.addVariable(recordsStruct)

        and: "create record Variable and add it to the records Structure"
        Variable recordsVar = new Variable(ncFile, recordsGroup, recordsStruct, "recordsVar", DataType.INT, "3")
        recordsStruct.addMemberVariable(recordsVar)

        and: "create group attribute containing multiple string values"
        Attribute stoogesAttrib = new Attribute("stooges", ['Moe Howard', 'Larry Fine', 'Curly Howard'])
        recordsGroup.addAttribute(stoogesAttrib)

        and: "create global attribute with multiple unsigned integer values"
        Attribute primesAttrib = new Attribute("primes", [2, 3, 5, 7, 11], true)
        ncFile.addAttribute(null, primesAttrib)

        and: "finish"
        ncFile.finish()
    }

    @Shared String expectedNcmlResult = '''\
<?xml version="1.0" encoding="UTF-8"?>
<netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2">
  <explicit />
  <enumTypedef name="dessertType" type="enum2">
    <enum key="18">pie</enum>
    <enum key="268">donut</enum>
    <enum key="3284">cake</enum>
  </enumTypedef>
  <dimension name="time" length="3" isUnlimited="true" />
  <variable name="dessert" shape="time" type="enum2" typedef="dessertType">
    <attribute name="zero" type="ulong" />
    <values>18.0 268.0 3284.0</values>
  </variable>
  <variable name="time" shape="time" type="short">
    <values start="4.0" increment="1.0" npts="3" />
  </variable>
  <variable name="charVar" shape="5" type="char">
    <values>abcde</values>
  </variable>
  <variable name="stringVar" shape="4" type="String">
    <values separator="|">Frodo Baggins|Samwise Gamgee|Meriadoc Brandybuck|Peregrin Took</values>
  </variable>
  <group name="recordsGroup">
    <variable name="recordsStruct" shape="*" type="Structure">
      <variable name="recordsVar" shape="3" type="int" />
    </variable>
    <attribute name="stooges" value="Moe Howard|Larry Fine|Curly Howard" separator="|" />
  </group>
  <attribute name="primes" type="uint" value="2 3 5 7 11" />
</netcdf>
'''

    NcMLWriter ncmlWriter
    def setup() {
        ncmlWriter = new NcMLWriter();
    }

    def "set NetcdfFile properties and exercise namespace and xmlFormat getters/setters"() {
        setup:
        NetcdfFile emptyNcFile = new NetcdfFileSubclass()
        emptyNcFile.setLocation("file:SOME_FILE");
        emptyNcFile.setId("SOME_ID")
        emptyNcFile.setTitle("NcMLWriter Test")
        emptyNcFile.finish()

        when: "set NcMLWriter namespace"
        // Causes:
        //     <netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2" />
        // to become:
        //     <netcdf />
        ncmlWriter.namespace = Namespace.NO_NAMESPACE   // Exercise setter.

        then: "getter returns instance just set"
        ncmlWriter.namespace == Namespace.NO_NAMESPACE  // Exercise getter.

        when: "set NcMLWriter XMLFormat"
        // Omits the '<?xml version="1.0" encoding="UTF-8"?>' declaration.
        Format xmlFormat = Format.rawFormat.setOmitDeclaration(true)
        ncmlWriter.xmlFormat = xmlFormat   // Exercise setter.

        then: "getter returns instance just set"
        ncmlWriter.xmlFormat == xmlFormat  // Exercise getter.

        expect:
        Element netcdfElem = ncmlWriter.makeNetcdfElement(emptyNcFile, null)
        ncmlWriter.writeToString(netcdfElem) ==
                '<netcdf location="file:SOME_FILE" id="SOME_ID" title="NcMLWriter Test" />\r\n'
    }

    def "makeDimensionElement() throws exception for private Dimension"() {
        when:
        ncmlWriter.makeDimensionElement(new Dimension("private", 8, false))

        then:
        IllegalArgumentException e = thrown()
        e.message == "Cannot create private dimension: in NcML, <dimension> elements are always shared."
    }

    def "'time' is a coordinate variable"() {
        expect:
        NcMLWriter.writeCoordinateVariablesPredicate.apply(ncFile.findVariable("time"))
    }

    def "'charVar', 'stringVar', and 'dessert' are metadata variables"() {
        expect:
        ['charVar', 'stringVar', 'dessert'].every {
            NcMLWriter.writeMetadataVariablesPredicate.apply(ncFile.findVariable(it))
        }
    }

    def "'recordsGroup/recordsStruct' can be selected with WriteVariablesWithNamesPredicate"() {
        setup:
        Predicate<Variable> writeVarsPred =
                new NcMLWriter.WriteVariablesWithNamesPredicate(['recordsGroup/recordsStruct'])

        expect:
        writeVarsPred.apply(ncFile.findVariable('recordsGroup/recordsStruct'))
    }

    def "write to String using compound writeVariablesPredicate"() {
        setup: "configure NcMLWriter"
        Predicate<Variable> compoundPred = Predicates.or(
                NcMLWriter.writeCoordinateVariablesPredicate,   // "time"
                NcMLWriter.writeMetadataVariablesPredicate,     // "charVar", "stringVar", "dessert"
                new NcMLWriter.WriteVariablesWithNamesPredicate(['recordsGroup/recordsStruct']));

        when: "set NcMLWriter writeVariablesPredicate"
        ncmlWriter.writeVariablesPredicate = compoundPred   // Exercise setter.

        then: "getter returns instance just set"
        ncmlWriter.writeVariablesPredicate == compoundPred  // Exercise getter.

        expect: "compoundPred applies to every Variable in ncFile"
        ncFile.variables.every { ncmlWriter.writeVariablesPredicate.apply(it) }

        and: "generated NcML string will match expectedNcmlResult"
        Element netcdfElem = ncmlWriter.makeExplicitNetcdfElement(ncFile, null)
        println ncmlWriter.writeToString(netcdfElem)
        println "\n\n" + expectedNcmlResult
        ncmlWriter.writeToString(netcdfElem) == expectedNcmlResult
    }

    // TODO: This is an integration test and probably runs much slower than the other tests in this class.
    // How to categorize it and only execute it in certain environments?
    def "round-trip: write to File and read back in, using NcMLReader"() {
        setup: "create temporary file to write NcML to"
        File outFile = File.createTempFile("NcMLWriterSpec", ".ncml")

        and: "configure NcMLWriter"
        // Don't try to write values 'recordsGroup/recordsStruct' this time; that already failed in previous method.
        // Also, the NetcdfDataset that NcMLReader returns will try to generate missing values, which we don't want.
        ncmlWriter.writeVariablesPredicate = Predicates.or(
                NcMLWriter.writeCoordinateVariablesPredicate,   // "time"
                NcMLWriter.writeMetadataVariablesPredicate)     // "charVar", "stringVar", "dessert"

        when: "write NcML to file"
        Element netcdfElem = ncmlWriter.makeExplicitNetcdfElement(ncFile, null)
        ncmlWriter.writeToFile(netcdfElem, outFile)

        then: "file's content matches expectedNcmlResult"
        outFile.text == expectedNcmlResult

        when: "read in NcML file and create a NetcdfDataset"
        NetcdfDataset readerDataset = NcMLReader.readNcML(outFile.toURI().toURL().toString(), null)

        and: "get the NcML representation of the dataset"
        readerDataset.setLocation(null)  // Leaving this non-null would screw up our comparison.
        Element readerNetcdfElem = ncmlWriter.makeExplicitNetcdfElement(readerDataset, null)

        then: "it matches expectedNcmlResult"
        ncmlWriter.writeToString(readerNetcdfElem) == expectedNcmlResult

        cleanup:
        readerDataset?.close()
        outFile?.delete()
    }
}
