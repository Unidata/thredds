package ucar.unidata.geoloc;

import junit.framework.*;
import ucar.unidata.geoloc.vertical.*;

/**
 * TestSuite that runs all the sample tests
 *
 *
 * @author John Caron
 * @version $Id: TestGeoloc.java 51 2006-07-12 17:13:13Z caron $
 */
public class TestGeoloc {

  public static junit.framework.Test suite () {
    TestSuite suite= new TestSuite();
    suite.addTest(new TestSuite(TestBasic.class));
    suite.addTest(new TestSuite(TestProjections.class));
    suite.addTest(new TestSuite(TestLatLonProjection.class)); // */

    suite.addTest(new TestSuite(TestVertical.class));
    //suite.addTest(new TestSuite(TestTiming.class));
    return suite;
  }
}