// $Id: TestTimeCoverage.java 61 2006-07-12 21:36:00Z edavis $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package thredds.catalog;

import junit.framework.TestCase;

import ucar.nc2.units.DateRange;
import ucar.nc2.units.TimeDuration;
import ucar.nc2.units.TimeUnit;

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
    assert tc.getStart().isPresent();
    assert tc.getResolution() == null;
    assert tc.getDuration().equals( new TimeDuration("10 days") );

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

/* Change History:
   $Log: TestTimeCoverage.java,v $
   Revision 1.4  2006/06/06 16:17:08  caron
   *** empty log message ***

   Revision 1.3  2006/05/08 02:47:20  caron
   cleanup code for 1.5 compile
   modest performance improvements
   dapper reading, deal with coordinate axes as structure members
   improve DL writing
   TDS unit testing

   Revision 1.2  2006/02/14 01:01:02  caron
   SpatialCOverage may hav enull ranges.
   tweak Validator - add regular field for  axes.
   change TDS version header, must be "dods/version"

   Revision 1.1  2005/05/26 01:58:01  caron
   fix DateRange bugs

*/