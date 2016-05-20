/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
// $Id: TestDmspIosp.java 51 2006-07-12 17:13:13Z caron $
package ucar.nc2.iosp.dmsp;

import junit.framework.TestCase;
import org.junit.experimental.categories.Category;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.*;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TestDmspIosp extends TestCase {
  private String testFilePath = TestDir.cdmUnitTestDir + "formats/dmsp";
  private String testDataFileName = "F14200307192230.n.OIS";

  private String testDataFileFileIdAttValue = "/dmsp/moby-1-3/subscriptions/IBAMA/1353226646955.tmp";
  private String testDataFileDatasetIdAttValue = "DMSP F14 OLS LS & TS";
  private int testDataFileNumBytesPerRecordAttValue = 3040;
  private int testDataFileNumHeaderRecordsAttValue = 1;
  private int testDataFileNumDataRecordsAttValue = 691;
  private int testDataFileNumSamplesPerBandDimAttValue = 1465;
  private String testDataFileSuborbitHistoryAttValue = "F14200307192230.OIS (1,691)";
  private String testDataFileProcessingSystemAttValue = "v2.1b";
  private String testDataFileProcessingDateAttValue = "2003-07-19T19:33:23.000Z";
  //"Sat Jul 19 19:33:23 2003";
  private String testDataFileSpacecraftIdAttValue = "F14";
  private String testDataFileNoradIdAttValue = "24753";
  private double testDataFileAscendingNodeAttValue = 320.55;
  private double testDataFileNodeHeadingAttValue = 8.64;

  private String numDataRecordsDimName = "numScans";
  private String numSamplesPerBandDimName = "numSamplesPerScan";

  private String fileIdAttName = "fileId";
  private String datasetIdAttName = "datasetId";
  private String suborbitHistoryAttName = "suborbitHistory";
  private String processingSystemAttName = "processingSystem";
  private String processingDateAttName = "processingDate";
  private String spacecraftIdAttName = "spacecraftId";
  private String noradIdAttName = "noradId";

  private String ascendingNodeAttName = "ascendingNode";
  private String nodeHeadingAttName = "nodeHeading";

  private DMSPHeader meHeader = null;
  private ucar.unidata.io.RandomAccessFile meRaf = null;
  private NetcdfFile meNcf = null;

  public TestDmspIosp(String name) {
    super(name);
  }

  public void testDateFormatHandler() {
    String isoDateFormatString = "yyyy-MM-dd";
    String isoTimeFormatString = "HH:mm:ss.SSSz";
    String isoDateTimeFormatString = "yyyy-MM-dd\'T\'HH:mm:ss.SSSz";
    String altDateTimeFormatString = "EEE MMM dd HH:mm:ss yyyy";

    int targetDate1Year = 2003;
    int targetDate1Month = 6; // July
    int targetDate1Day = 19;
    int targetDate1Hour = 19;
    int targetDate1Minute = 33;
    int targetDate1Second = 23;
    int targetDate1Milisecond = 0;

    String targetDate1ISODateString = "2003-07-19";
    String targetDate1ISODateTimeString = "2003-07-19T19:33:23.000GMT";
    String targetDate1AltDateTimeString = "Sat Jul 19 19:33:23 2003";

    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.US);
    calendar.set(targetDate1Year, targetDate1Month, targetDate1Day,
            targetDate1Hour, targetDate1Minute, targetDate1Second);
    calendar.set(Calendar.MILLISECOND, targetDate1Milisecond);
    Date targetDate1 = calendar.getTime();

    Date testDate = null;
    String testDateTimeString = null;


    // Test that alternate date/time format string is as expected.
    assertTrue("Alternate date/time format string <" + DMSPHeader.DateFormatHandler.ALT_DATE_TIME.getDateTimeFormatString() + "> not as expected <" + altDateTimeFormatString + ">.",
            DMSPHeader.DateFormatHandler.ALT_DATE_TIME.getDateTimeFormatString().equals(altDateTimeFormatString));

    // Test that alternate date/time format string handler parses date/time string properly.
    try {
      testDate = DMSPHeader.DateFormatHandler.ALT_DATE_TIME.getDateFromDateTimeString(targetDate1AltDateTimeString);
    } catch (ParseException e) {
      assertTrue("Unexpected ParseException while parsing date/time string <" + targetDate1AltDateTimeString + ">: " + e.getMessage(),
              false);
    }
    assertTrue("Alternate date/time <" + testDate + "> not as expected <" + targetDate1.toString() + ">.",
            testDate.equals(targetDate1));

    // Test that alternate date/time format string handler formats date/time properly.
    testDateTimeString = DMSPHeader.DateFormatHandler.ALT_DATE_TIME.getDateTimeStringFromDate(targetDate1);
    assertTrue("Date/time string <" + testDateTimeString + "> not as expected <" + targetDate1AltDateTimeString + ">.",
            testDateTimeString.equals(targetDate1AltDateTimeString));
  }

  /**
   * Test ...
   */
  @Category(NeedsCdmUnitTest.class)
  public void testDimAndAtt() throws IOException {
    setupReadDmspAsNetcdf(this.testFilePath, this.testDataFileName);
    assertTrue("Created NetcdfFile is null.", meNcf != null);

//    // Test some header information not available from NetcdfFile.
//    assertTrue( "Number of bytes per records <" + meHeader.getRecordSizeInBytes() + "> not as expected <" + this.testDataFileNumBytesPerRecordAttValue + ">.",
//                meHeader.getRecordSizeInBytes() == this.testDataFileNumBytesPerRecordAttValue );
//    assertTrue( "Number of header records <" + meHeader.getNumHeaderRecords() + "> not as expected <" + this.testDataFileNumHeaderRecordsAttValue + ">.",
//                meHeader.getNumHeaderRecords() == this.testDataFileNumHeaderRecordsAttValue );


    // Test for the dimensions of the NetcdfFile.
    Dimension curDim = meNcf.getRootGroup().findDimension(this.numDataRecordsDimName);
    assertTrue("Number of data records <" + curDim.getLength() + "> not as expected <" + this.testDataFileNumDataRecordsAttValue + ">.",
            curDim.getLength() == this.testDataFileNumDataRecordsAttValue);

    curDim = meNcf.getRootGroup().findDimension(this.numSamplesPerBandDimName);
    assertTrue("Number of bytes per records <" + curDim.getLength() + "> not as expected <" + this.testDataFileNumSamplesPerBandDimAttValue + ">.",
            curDim.getLength() == this.testDataFileNumSamplesPerBandDimAttValue);

    // Test for the attributes of the NetcdfFile.
    Attribute curAtt = meNcf.getRootGroup().findAttribute(this.fileIdAttName);
    assertTrue("FileId attribute <" + curAtt.getStringValue() + "> not as expected <" + this.testDataFileFileIdAttValue + ">.",
            curAtt.getStringValue().equals(this.testDataFileFileIdAttValue));

    curAtt = meNcf.getRootGroup().findAttribute(this.datasetIdAttName);
    assertTrue("DatasetId attribute <" + curAtt.getStringValue() + "> not as expected <" + this.testDataFileDatasetIdAttValue + ">.",
            curAtt.getStringValue().equals(this.testDataFileDatasetIdAttValue));


    curAtt = meNcf.getRootGroup().findAttribute(this.suborbitHistoryAttName);
    assertTrue("SuborbitHistory attribute <" + curAtt.getStringValue() + "> not as expected <" + this.testDataFileSuborbitHistoryAttValue + ">.",
            curAtt.getStringValue().equals(this.testDataFileSuborbitHistoryAttValue));

    curAtt = meNcf.getRootGroup().findAttribute(this.processingSystemAttName);
    assertTrue("ProcessingSystem attribute <" + curAtt.getStringValue() + "> not as expected <" + this.testDataFileProcessingSystemAttValue + ">.",
            curAtt.getStringValue().equals(this.testDataFileProcessingSystemAttValue));

    curAtt = meNcf.getRootGroup().findAttribute(this.processingDateAttName);
    assertTrue("ProcessingDate attribute <" + curAtt.getStringValue() + "> not as expected <" + this.testDataFileProcessingDateAttValue + ">.",
            curAtt.getStringValue().equals(this.testDataFileProcessingDateAttValue));

    curAtt = meNcf.getRootGroup().findAttribute(this.spacecraftIdAttName);
    assertTrue("SpacecraftId attribute <" + curAtt.getStringValue() + "> not as expected <" + this.testDataFileSpacecraftIdAttValue + ">.",
            curAtt.getStringValue().equals(this.testDataFileSpacecraftIdAttValue));

    curAtt = meNcf.getRootGroup().findAttribute(this.noradIdAttName);
    assertTrue("ProcessingDate attribute <" + curAtt.getStringValue() + "> not as expected <" + this.testDataFileNoradIdAttValue + ">.",
            curAtt.getStringValue().equals(this.testDataFileNoradIdAttValue));

    curAtt = meNcf.getRootGroup().findAttribute(this.ascendingNodeAttName);
    assertTrue("AscendingNode attribute <" + curAtt.getNumericValue().doubleValue() + "> not as expected <" + this.testDataFileAscendingNodeAttValue + ">.",
            curAtt.getNumericValue().doubleValue() == testDataFileAscendingNodeAttValue);

    curAtt = meNcf.getRootGroup().findAttribute(this.nodeHeadingAttName);
    assertTrue("NodeHeading attribute <" + curAtt.getNumericValue().doubleValue() + "> not as expected <" + this.testDataFileNodeHeadingAttValue + ">.",
            curAtt.getNumericValue().doubleValue() == testDataFileNodeHeadingAttValue);

    meNcf.close();

  }

  @Category(NeedsCdmUnitTest.class)
  public void testReadEpoch() throws IOException {
    setupReadDmspAsNetcdf(this.testFilePath, this.testDataFileName);

    // Test reading year.
    ucar.ma2.Array year = null;
    try {
      year = meNcf.findVariable("year").read();
    } catch (IOException e) {
      assertTrue("Unexpected IOException reading \"year\" variable: " + e.getMessage(),
              false);
    }
    int val = 0;
    IndexIterator iter = year.getIndexIterator();
    while (iter.hasNext()) {
      val = iter.getIntNext();
      assertTrue("Value of variable \"year\" <" + val + "> not expected <2003>.",
              val == 2003);
    }

    // Test reading dayOfYear.
    ucar.ma2.Array dayOfYear = null;
    try {
      dayOfYear = meNcf.findVariable("dayOfYear").read();
    } catch (IOException e) {
      assertTrue("Unexpected IOException reading \"dayOfYear\" variable: " + e.getMessage(),
              false);
    }
    val = 0;
    iter = dayOfYear.getIndexIterator();
    while (iter.hasNext()) {
      val = iter.getIntNext();
      assertTrue("Value of variable \"dayOfYear\" <" + val + "> not expected <200>.",
              val == 200);
    }

    // Test reading dayOfYear.
    ucar.ma2.Array secondsOfDay = null;
    try {
      secondsOfDay = meNcf.findVariable("secondsOfDay").read();
    } catch (IOException e) {
      assertTrue("Unexpected IOException reading \"secondsOfDay\" variable: " + e.getMessage(),
              false);
    }
    double prevVal = 0;
    double curVal = 0;
    double timeInterval = 0;
    double timeIntervalGuess = 0.42;
    double delta = 0.01;

    iter = secondsOfDay.getIndexIterator();
    if (iter.hasNext()) {
      prevVal = iter.getDoubleNext();
      int timeStep = 1;
      while (iter.hasNext()) {
        curVal = iter.getDoubleNext();
        timeInterval = curVal - prevVal;
        StringBuffer tmpMsg = new StringBuffer("Variable \"secondsOfDay\": [")
                .append(timeStep).append("]=<").append(curVal).append(">, [")
                .append((timeStep - 1)).append("]=<").append(prevVal).append("> difference <")
                .append(timeInterval).append("> not within delta <").append(delta)
                .append("> of expected <").append(timeIntervalGuess).append(">.");
        assertTrue(tmpMsg.toString(),
                timeInterval >= timeIntervalGuess - delta && timeInterval <= timeIntervalGuess + delta);
        prevVal = curVal;
        timeStep++;
      }
    }

    meNcf.close();
  }

  @Category(NeedsCdmUnitTest.class)
  public void testLatLonCalcAndCache() throws IOException {
    setupReadDmspAsNetcdf(this.testFilePath, this.testDataFileName);

    Variable latVar = meNcf.findVariable("latitude");

    ucar.ma2.Array latitude = null;
    try {
      latitude = latVar.read();
    } catch (IOException e) {
      assertTrue("Unexpected IOException reading \"latitude\" variable: " + e.getMessage(),
              false);
    }

    // Test that difference between neighboring pixels in first scan is small
    IndexIterator iter = latitude.getIndexIterator();
    float curVal = 0.0F;
    float diff = 0.0F;
    float biggestDiff = 0.0F;
    float smallestDiff = 0.0F;
    float prevVal = iter.getFloatNext();
    int cnt = 1;
    while (iter.hasNext() && cnt < 1465) {
      curVal = iter.getFloatNext();
      diff = curVal - prevVal;
      if (cnt == 1) {
        biggestDiff = diff;
        smallestDiff = diff;
      } else {
        if (diff > biggestDiff) biggestDiff = diff;
        if (diff < smallestDiff) smallestDiff = diff;
      }
      //System.out.println( curVal + "   :   " + diff);
      prevVal = curVal;
      cnt++;
    }
    //System.out.println( "\nBiggest Diff=" + biggestDiff);
    //System.out.println( "Smallest Diff=" + smallestDiff);
    assertTrue("Biggest difference in latitude between neighboring pixels of the first scan <" + biggestDiff + "> bigger than expected <0.004>.",
            biggestDiff < 0.004);
    assertTrue("Smallest difference in latitude between neighboring pixels of the first scan <" + smallestDiff + "> smaller than expected <0.002>.",
            smallestDiff > 0.002);

    // Now test that the user isn't getting the cached data.
    float lat1 = latitude.getFloat(latitude.getIndex().set(0, 0));
    latitude.setFloat(latitude.getIndex().set(0, 0), lat1 + 100.0F);

    ucar.ma2.Array latPointAfterModify = null;

    try {
      latPointAfterModify = latVar.read("0:0:1,0:0:1");
    } catch (InvalidRangeException e) {
      assertTrue("Unexpected InvalidRangeException reading \"latitude\" variable: " + e.getMessage(),
              false);
    } catch (IOException e) {
      assertTrue("Unexpected IOException reading \"latitude\" variable: " + e.getMessage(),
              false);
    }

    float lat1After = latPointAfterModify.getFloat(latPointAfterModify.getIndex().set(0, 0));
    assertTrue("Value of lat[0,0] <" + lat1 + "> changed <" + lat1After + ">",
            lat1 == lat1After);

    meNcf.close();
  }

  // From: John Caron
  // Date: 9 May 2006
  // I just tracked down an insidious bug where Array.section() was used
  // instead of Array.sectionNoReduce(). The difference is that section()
  // will eliminate any dimension of length =1, thus reducing the rank.
  //
  // I.e., if a user requests one point in a particular dimension, that
  // dimension will be removed.
  @Category(NeedsCdmUnitTest.class)
  public void testSectionVsSectionNoReduce() throws IOException {
    setupReadDmspAsNetcdf(this.testFilePath, this.testDataFileName);

    Variable yearVar = meNcf.findVariable("year");
    assertTrue("Year variable not of rank one.",
            yearVar.getRank() == 1);

    Variable visVar = meNcf.findVariable("visibleImagery");
    assertTrue("Visible imagery variable not of rank two.",
            visVar.getRank() == 2);

    Variable irVar = meNcf.findVariable("infraredImagery");
    assertTrue("Infrared imagery variable not of rank two.",
            irVar.getRank() == 2);

    long visSize = visVar.getSize();
    long irSize = irVar.getSize();
    assertTrue("Visible and infrared imagery variables are different sizes (" + visSize + " vs " + irSize + ".",
            visSize == irSize);

    // Read in year variable with section not for single point.
    ucar.ma2.Array yearArray = null;
    try {
      yearArray = yearVar.read("0:1:1");
    } catch (InvalidRangeException e) {
      assertTrue("Unexpected InvalidRangeException reading \"year\" variable: " + e.getMessage(),
              false);
    } catch (IOException e) {
      assertTrue("Unexpected IOException reading \"year\" variable: " + e.getMessage(),
              false);
    }

    int yearRank = yearArray.getRank();
    assertTrue("Year array not rank 1 <" + yearRank + ">",
            yearRank == 1);

    // Read in visible imagery with only one point selected in one dimension.
    ucar.ma2.Array visArray = null;
    try {
      visArray = visVar.read("0:0:1,0:1:1");
    } catch (InvalidRangeException e) {
      assertTrue("Unexpected InvalidRangeException reading \"visibleImagery\" variable: " + e.getMessage(),
              false);
    } catch (IOException e) {
      assertTrue("Unexpected IOException reading \"visibleImagery\" variable: " + e.getMessage(),
              false);
    }

    int visRank = visArray.getRank();
    assertTrue("Latitude array rank not 2 <" + visRank + ">.",
            visRank == 2);


    meNcf.close();
  }

  private void setupReadDmspAsNetcdf(String testFilePath, String testDataFileName) {
    // Register the DMSP IOServiceProvider.
    try {
      NetcdfFile.registerIOProvider(DMSPiosp.class);
    } catch (IllegalAccessException e) {
      assertTrue("Unexpected IllegalAccessException registering DMSPiosp: " + e.getMessage(),
              false);
    } catch (InstantiationException e) {
      assertTrue("Unexpected InstantiationException registering DMSPiosp: " + e.getMessage(),
              false);
    }

    // Make sure test DMSP file exists and such.
    File testFile = new File(testFilePath, testDataFileName);
    assertTrue("Test file <" + testFile.getAbsolutePath() + "> does not exist.",
            testFile.exists());
    assertTrue("Test file <" + testFile.getAbsolutePath() + "> cannot be read.",
            testFile.canRead());
    assertTrue("Test file <" + testFile.getAbsolutePath() + "> is a directory.",
            !testFile.isDirectory());

    // Open test DMSP file as NetCDF file.
    try {
      meNcf = NetcdfFile.open(testFilePath + "/" + testDataFileName);
    } catch (IOException e) {
      assertTrue("Unexpected IOException opening DMSP file <" + testFile.getAbsolutePath() + ">: " + e.getMessage(),
              false);
    }
  }

}

/*
 * $Log: TestDmspIosp.java,v $
 * Revision 1.5  2006/05/31 19:04:41  edavis
 * Fix bug with use of section() instead of sectionNoReduce().
 * Also, fix so that copy of cached data is returned to user instead of backing store.
 *
 * Revision 1.4  2005/07/25 00:07:13  caron
 * cache debugging
 *
 * Revision 1.3  2004/10/14 00:13:01  edavis
 * Comment out some print statements.
 *
 * Revision 1.2  2004/10/13 15:30:35  edavis
 * Some clean up and add test for latitude/longitude calculation.
 *
 * Revision 1.1  2004/10/06 21:47:06  edavis
 * Initial tests for ucar.nc2.iosp.dmsp classes.
 *
 */
