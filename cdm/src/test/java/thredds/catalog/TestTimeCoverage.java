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
package thredds.catalog;

import junit.framework.TestCase;

import ucar.nc2.units.*;

import java.util.Date;

/**
 * @author john
 */
public class TestTimeCoverage  extends TestCase {
  private static boolean showValidation = false;

  public TestTimeCoverage( String name) {
    super(name);
  }

  String urlString = "TestTimeCoverage.xml";

  public void testXTC() throws Exception {
    InvCatalogImpl cat = TestCatalogAll.open(urlString, true);

    StringBuilder buff = new StringBuilder();
    boolean isValid = cat.check(buff, false);
    System.out.println("catalog <" + cat.getName() + "> " + (isValid ? "is" : "is not") + " valid");
    System.out.println(" validation output=\n" + buff);

    InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory( false);
    catFactory.writeXML( cat, System.out);

    InvDataset ds = cat.findDatasetByID("test1");
    DateRange tc = ds.getTimeCoverage();
    assert null != tc;
    System.out.println(" tc = "+tc);
    assert tc.getEnd().isPresent();
    assert tc.getResolution() == null;
    assert tc.getDuration().equals( new TimeDuration("14 days") );

    ds = cat.findDatasetByID("test2");
    tc = ds.getTimeCoverage();
    assert null != tc;
    System.out.println(" tc = "+tc);
    Date got = tc.getStart().getDate();
    Date want = new DateFormatter().getISODate("1999-11-16T12:00:00");
    assert got.equals( want);
    assert tc.getResolution() == null;
    TimeDuration gott = tc.getDuration();
    TimeDuration wantt = new TimeDuration("P3M");
    assert gott.equals( wantt);

    ds = cat.findDatasetByID("test3");
    tc = ds.getTimeCoverage();
    assert null != tc;
    System.out.println(" tc = "+tc);
    assert tc.getResolution() == null;
    assert tc.getDuration().equals( new TimeDuration("2 days") );

    ds = cat.findDatasetByID("test4");
    tc = ds.getTimeCoverage();
    assert null != tc;
    System.out.println(" tc = "+tc);
    TimeDuration r = tc.getResolution();
    assert r != null;
    TimeDuration r2 = new TimeDuration("3 hour");
    assert r.equals( r2 );
    TimeDuration d = tc.getDuration();
    TimeUnit tu = d.getTimeUnit();
    assert tu.getUnitString().equals("days") : tu.getUnitString(); // LOOK should be 3 hours, or hours or ??
 }

}