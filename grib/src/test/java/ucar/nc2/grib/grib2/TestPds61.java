/*
 * (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 */
package ucar.nc2.grib.grib2;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import ucar.nc2.time.CalendarDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test data provided by github issue https://github.com/Unidata/thredds/issues/834
 * Only the first record was used to create a .gbx9 file.
 *
 * Full output from ecCodes v2.6.0 grib_dump at the end of the file, with data values
 * removed (as required by ECMWF data use policy for redistribution without the need
 * for netCDF-java / TDS users to register with them). Note also because of the data
 * use policy, we are only testing the gbx9 file (which is the GRIB record, minus the
 * data block)
 */
public class TestPds61 {

    private Grib2Pds pds;

    @Before
    public void openTestFile() throws IOException {
        String testfile = "../grib/src/test/data/index/example_pds_61.grib2.gbx9";

        Grib2Index gi = new Grib2Index();
        boolean success = gi.readIndex(testfile, -1);
        assertTrue(success);
        List<Grib2Record> records = gi.getRecords();
        Grib2Record record = records.get(0);
        pds = record.getPDS();
    }

    @Test
    public void checkPdsBasic() {
        assertEquals(pds.getRawLength(), 68);
        assertEquals(pds.getTemplateNumber(), 61);
    }

    @Test
    public void checkModelVersionDate() {
        assertEquals(pds.calcTime(38),
                CalendarDate.parseISOformat("proleptic_gregorian", "2011-03-01T00:00:00"));
    }

    @Test
    public void checkEndOfOverallIntervalDate() {
        assertEquals(pds.calcTime(45),
                CalendarDate.parseISOformat("proleptic_gregorian", "2010-12-29T06:00:00"));
    }

    @Test
    public void checkTypeOfGeneratingProcess() {
        assertEquals(pds.getGenProcessType(), 4);
    }

    @Test
    public void checkNumberOfTimeRanges() {
        assertEquals(pds.getOctet(52), 1);
    }

}

/*
grib_dump -O example_pds_61.grib2
        ***** FILE: example_pds_61.grib2
        #==============   MESSAGE 1 ( length=807 )                 ==============
        1-4       identifier = GRIB
        5-6       reserved = MISSING
        7         discipline = 0 [Meteorological products (grib2/tables/4/0.0.table) ]
        8         editionNumber = 2
        9-16      totalLength = 807
        ======================   SECTION_1 ( length=21, padding=0 )    ======================
        1-4       section1Length = 21
        5         numberOfSection = 1
        6-7       centre = 7 [US National Weather Service - NCEP  (WMC)  (common/c-11.table) ]
        8-9       subCentre = 0
        10        tablesVersion = 4 [Version implemented on 7 November 2007 (grib2/tables/1.0.table) ]
        11        localTablesVersion = 0 [Local tables not used  (grib2/tables/4/1.1.table) ]
        12        significanceOfReferenceTime = 1 [Start of forecast (grib2/tables/4/1.2.table) ]
        13-14     year = 2010
        15        month = 12
        16        day = 29
        17        hour = 0
        18        minute = 0
        19        second = 0
        20        productionStatusOfProcessedData = 6 [Unknown code table entry (grib2/tables/4/1.3.table) ]
        21        typeOfProcessedData = 3 [Control forecast products (grib2/tables/4/1.4.table) ]
        ======================   SECTION_3 ( length=72, padding=0 )    ======================
        1-4       section3Length = 72
        5         numberOfSection = 3
        6         sourceOfGridDefinition = 0 [Specified in Code table 3.1 (grib2/tables/4/3.0.table) ]
        7-10      numberOfDataPoints = 396
        11        numberOfOctectsForNumberOfPoints = 0
        12        interpretationOfNumberOfPoints = 0 [There is no appended list (grib2/tables/4/3.11.table) ]
        13-14     gridDefinitionTemplateNumber = 0 [Latitude/longitude  (Also called equidistant cylindrical, or Plate Carree)  (grib2/tables/4/3.1.table) ]
        15        shapeOfTheEarth = 6 [Earth assumed spherical with radius of 6,371,229.0 m (grib2/tables/4/3.2.table) ]
        16        scaleFactorOfRadiusOfSphericalEarth = MISSING
        17-20     scaledValueOfRadiusOfSphericalEarth = MISSING
        21        scaleFactorOfEarthMajorAxis = MISSING
        22-25     scaledValueOfEarthMajorAxis = MISSING
        26        scaleFactorOfEarthMinorAxis = MISSING
        27-30     scaledValueOfEarthMinorAxis = MISSING
        31-34     Ni = 22
        35-38     Nj = 18
        39-42     basicAngleOfTheInitialProductionDomain = 0
        43-46     subdivisionsOfBasicAngle = MISSING
        47-50     latitudeOfFirstGridPoint = -15000000
        51-54     longitudeOfFirstGridPoint = 289500000
        55        resolutionAndComponentFlags = 48 [00110000]
        56-59     latitudeOfLastGridPoint = -40500000
        60-63     longitudeOfLastGridPoint = 321000000
        64-67     iDirectionIncrement = 1500000
        68-71     jDirectionIncrement = 1500000
        72        scanningMode = 0 [00000000]
        ======================   SECTION_4 ( length=68, padding=0 )    ======================
        1-4       section4Length = 68
        5         numberOfSection = 4
        6-7       NV = 0
        8-9       productDefinitionTemplateNumber = 61 [Unknown code table entry (grib2/tables/4/4.0.table) ]
        10        parameterCategory = 1 [Moisture (grib2/tables/4/4.1.0.table) ]
        11        parameterNumber = 52 [Total precipitation rate  (kg m-2 s-1)  (grib2/tables/4/4.2.0.1.table) ]
        12        typeOfGeneratingProcess = 4 [Ensemble forecast (grib2/tables/4/4.3.table) ]
        13        backgroundProcess = 255
        14        generatingProcessIdentifier = 82
        15-16     hoursAfterDataCutoff = 0
        17        minutesAfterDataCutoff = MISSING
        18        indicatorOfUnitOfTimeRange = 1 [Hour (grib2/tables/4/4.4.table) ]
        19-22     forecastTime = 0
        23        typeOfFirstFixedSurface = 1 [Ground or water surface  (grib2/tables/4/4.5.table) ]
        24        scaleFactorOfFirstFixedSurface = MISSING
        25-28     scaledValueOfFirstFixedSurface = MISSING
        29        typeOfSecondFixedSurface = 255 [Missing (grib2/tables/4/4.5.table) ]
        30        scaleFactorOfSecondFixedSurface = MISSING
        31-34     scaledValueOfSecondFixedSurface = MISSING
        35        typeOfEnsembleForecast = 255 [Missing (grib2/tables/4/4.6.table) ]
        36        perturbationNumber = 0
        37        numberOfForecastsInEnsemble = 4
        38-39     YearOfModelVersion = 2011
        40        MonthOfModelVersion = 3
        41        DayOfModelVersion = 1
        42        HourOfModelVersion = 0
        43        MinuteOfModelVersion = 0
        44        SecondOfModelVersion = 0
        45-46     yearOfEndOfOverallTimeInterval = 2010
        47        monthOfEndOfOverallTimeInterval = 12
        48        dayOfEndOfOverallTimeInterval = 29
        49        hourOfEndOfOverallTimeInterval = 6
        50        minuteOfEndOfOverallTimeInterval = 0
        51        secondOfEndOfOverallTimeInterval = 0
        52        numberOfTimeRange = 1
        53-56     numberOfMissingInStatisticalProcess = 0
        57        typeOfStatisticalProcessing = 1 [Accumulation (grib2/tables/4/4.10.table) ]
        58        typeOfTimeIncrement = 2 [Successive times processed have same start time of forecast, forecast time is incremented (grib2/tables/4/4.11.table) ]
        59        indicatorOfUnitForTimeRange = 1 [Hour (grib2/tables/4/4.4.table) ]
        60-63     lengthOfTimeRange = 6
        64        indicatorOfUnitForTimeIncrement = 255 [Missing (grib2/tables/4/4.4.table) ]
        65-68     timeIncrement = 0
        ======================   SECTION_5 ( length=21, padding=0 )    ======================
        1-4       section5Length = 21
        5         numberOfSection = 5
        6-9       numberOfValues = 396
        10-11     dataRepresentationTemplateNumber = 0 [Grid point data - simple packing (grib2/tables/4/5.0.table) ]
        12-15     referenceValue = 0
        16-17     binaryScaleFactor = -7
        18-19     decimalScaleFactor = 0
        20        bitsPerValue = 12
        21        typeOfOriginalFieldValues = 0 [Floating point (grib2/tables/4/5.1.table) ]
        ======================   SECTION_6 ( length=6, padding=0 )     ======================
        1-4       section6Length = 6
        5         numberOfSection = 6
        6         bitMapIndicator = 255 [A bit map does not apply to this product (grib2/tables/4/6.0.table) ]
        ======================   SECTION_7 ( length=599, padding=0 )   ======================
        1-4       section7Length = 599
        5         numberOfSection = 7
        6-599     codedValues = (396,594) {
        <data block removed>
        } # data_g2simple_packing codedValues
        ======================   SECTION_8 ( length=4, padding=0 )     ======================
        1-4       7777 = 7777
*/
