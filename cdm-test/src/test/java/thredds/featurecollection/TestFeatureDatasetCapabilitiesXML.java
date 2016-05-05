/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */
package thredds.featurecollection;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.point.writer.FeatureDatasetCapabilitiesWriter;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * Test FeatureDatasetCapabilitiesXML
 *
 * @author caron
 * @since 9/23/2015.
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestFeatureDatasetCapabilitiesXML {

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[]{TestDir.cdmUnitTestDir+"ft/point/ship/nc/Surface_Buoy_20090920_0000.nc",
            "http://thredds.ucar.edu/thredds/cdmremote/idd/buoy/collection", false}
    );

    return result;
  }

  String location;
  String path;
  boolean hasAlt;

  public TestFeatureDatasetCapabilitiesXML(String location, String path, boolean hasAlt) {
    this.location = location;
    this.path = path;
    this.hasAlt = hasAlt;
  }

  @Test
  public void doOne() throws IOException, JDOMException {
    FeatureDataset fd = FeatureDatasetFactoryManager.open(FeatureType.ANY_POINT, location, null, new Formatter(System.out));
    FeatureDatasetCapabilitiesWriter capWriter = new FeatureDatasetCapabilitiesWriter((FeatureDatasetPoint) fd, path);
    String orgXml = capWriter.getCapabilities();

    File f = TestDir.getTempFile();
    FileOutputStream fos = new FileOutputStream(f);
    capWriter.getCapabilities(fos);
    fos.close();
    System.out.printf("%s written%n", f.getPath());

    // round trip
    Document doc = capWriter.readCapabilitiesDocument( new FileInputStream(f));
    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    String xml = fmt.outputString(doc);
    System.out.printf("%s%n", xml);

    Assert.assertEquals(orgXml, xml); // lame test

    String altUnits = FeatureDatasetCapabilitiesWriter.getAltUnits(doc);
    if (hasAlt)
      System.out.printf("altUnits=%s%n", altUnits);
    Assert.assertEquals(hasAlt, altUnits != null);

    CalendarDateUnit cdu = FeatureDatasetCapabilitiesWriter.getTimeUnit(doc);
    Assert.assertNotNull("cdu", cdu);
    System.out.printf("CalendarDateUnit= %s%n", cdu);
    System.out.printf("Calendar= %s%n", cdu.getCalendar());

    CalendarDateRange cd = FeatureDatasetCapabilitiesWriter.getTimeSpan(doc);
    Assert.assertNotNull("CalendarDateRange", cd);
    System.out.printf("CalendarDateRange= %s%n", cd);

    LatLonRect bbox = FeatureDatasetCapabilitiesWriter.getSpatialExtent(doc);
    Assert.assertNotNull("bbox", bbox);
    System.out.printf("LatLonRect=%s%n", bbox);
  }
}
