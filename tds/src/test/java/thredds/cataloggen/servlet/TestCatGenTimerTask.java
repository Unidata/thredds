// $Id$

package thredds.cataloggen.servlet;

import junit.framework.*;

import java.io.File;
import java.io.IOException;

/**
 *
 *
 *
 *
 */
public class TestCatGenTimerTask extends TestCase
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( TestCatGenTimerTask.class );

  // @todo Modify CatGenTimerTask so that these tests can be run on resources rather than files.
  private String configPath = "src/test/data/thredds/cataloggen/config";

  private CatGenTimerTask taskOne = null;
  private String taskOneName = "Task1";
  private String taskOneConfigDocName = "test1CatGenConfig0.6.xml";
  private String taskOneResultsFileName = "myCatalogExample1.xml";
  private int taskOneDelayInMinutes = 1;
  private int taskOnePeriodInMinutes = 30;

  private String taskTwoName = "Task2";
  private String taskTwoConfigDocName = "test1CatGenConfig1.0.xml";
  private String taskTwoResultsFileName = "myCatalogExample2.xml";
  private int taskTwoDelayInMinutes = 2;
  private int taskTwoPeriodInMinutes = 60;


  public TestCatGenTimerTask( String name)
  {
    super( name);
  }

  protected void setUp()
  {
    taskOne = new CatGenTimerTask( taskOneName, taskOneConfigDocName,
                                   taskOneResultsFileName,
                                   taskOnePeriodInMinutes, taskOneDelayInMinutes );
    taskOne.init( new File( configPath ), new File( configPath ) );

  }

//  protected void tearDown()
//  {
//  }

  public void testValid()
  {
    assertTrue( "Task name <" + taskOne.getName() + "> doesn't match expected <" + taskOneName + ">.",
                taskOne.getName().equals( taskOneName));
    assertTrue( "Task config doc name <" + taskOne.getConfigDocName() + "> doesn't match expected <" + taskOneConfigDocName + ">.",
                taskOne.getConfigDocName().equals( taskOneConfigDocName ) );
    assertTrue( "Task result doc name <" + taskOne.getResultFileName() + "> doesn't match expected <" + taskOneResultsFileName + ">.",
                taskOne.getResultFileName().equals( taskOneResultsFileName ) );
    assertTrue( "Task period in minutes <" + taskOne.getPeriodInMinutes() + "> doesn't match expected <" + taskOnePeriodInMinutes + ">.",
                taskOne.getPeriodInMinutes() == taskOnePeriodInMinutes );
    assertTrue( "Task delay in minutes <" + taskOne.getDelayInMinutes() + "> doesn't match expected <" + taskOneDelayInMinutes + ">.",
                taskOne.getDelayInMinutes() == taskOneDelayInMinutes );

    log.debug( "testValid(): taskOne - name=" + taskOne.getName() +
                  "; config doc name=" + taskOne.getConfigDocName() + "" +
                  "; result doc name=" + taskOne.getResultFileName() + "" +
                  "; period in minutes=" + taskOne.getPeriodInMinutes() + "" +
                  "; delay in minutes=" + taskOne.getDelayInMinutes() + ".");
    StringBuffer msg = new StringBuffer();
    boolean testIsValid = taskOne.isValid( msg);
    assertTrue( "Task <" + taskOne.getName() + " - " + taskOne.getConfigDocName() + "> is not valid: " + msg,
                testIsValid );


    taskOne.setName( taskTwoName);
    assertTrue( "Setting new task name <" + taskTwoName + "> failed.",
                taskOne.getName( ).equals( taskTwoName));
    taskOne.setConfigDocName( taskTwoConfigDocName);
    assertTrue( "Setting new task config doc name <" + taskTwoConfigDocName + "> failed.",
                taskOne.getConfigDocName().equals( taskTwoConfigDocName ) );
    taskOne.setResultFileName( taskTwoResultsFileName);
    assertTrue( "Setting new task results file name <" + taskTwoResultsFileName + "> failed.",
                taskOne.getResultFileName().equals( taskTwoResultsFileName ) );
    taskOne.setPeriodInMinutes( taskTwoPeriodInMinutes);
    assertTrue( "Setting new task period in minutes <" + taskTwoPeriodInMinutes + "> failed.",
                taskOne.getPeriodInMinutes() == taskTwoPeriodInMinutes );
    taskOne.setDelayInMinutes( taskTwoDelayInMinutes );
    assertTrue( "Setting new task delay in minutes <" + taskTwoDelayInMinutes + "> failed.",
                taskOne.getDelayInMinutes() == taskTwoDelayInMinutes );

    testIsValid = taskOne.isValid( msg );
    assertTrue( "Task <" + taskOne.getName() + " - " + taskOne.getConfigDocName() + "> is not valid: " + msg,
                testIsValid );
  }

}
/*
 * $Log: TestCatGenTimerTask.java,v $
 * Revision 1.5  2006/03/30 21:50:15  edavis
 * Minor fixes to get tests running.
 *
 * Revision 1.4  2006/01/20 20:42:06  caron
 * convert logging
 * use nj22 libs
 *
 * Revision 1.3  2005/07/14 20:01:26  edavis
 * Make ID generation mandatory for datasetScan generated catalogs.
 * Also, remove log4j from some tests.
 *
 * Revision 1.2  2005/04/27 23:05:41  edavis
 * Move sorting capabilities into new DatasetSorter class.
 * Fix a bunch of tests and such.
 *
 * Revision 1.1  2005/03/30 05:41:19  edavis
 * Simplify build process: 1) combine all build scripts into one,
 * thredds/build.xml; 2) combine contents of all resources/ directories into
 * one, thredds/resources; 3) move all test source code and test data into
 * thredds/test/src and thredds/test/data; and 3) move all schemas (.xsd and .dtd)
 * into thredds/resources/resources/thredds/schemas.
 *
 * Revision 1.6  2004/08/23 16:45:17  edavis
 * Update DqcServlet to work with DQC spec v0.3 and InvCatalog v1.0. Folded DqcServlet into the THREDDS server framework/build/distribution. Updated documentation (DqcServlet and THREDDS server).
 *
 * Revision 1.5  2004/06/03 20:40:59  edavis
 * Modified tests to handle changes to code.
 *
 * Revision 1.4  2004/05/11 16:29:08  edavis
 * Updated to work with new thredds.catalog 0.6 stuff and the THREDDS
 * servlet framework.
 *
 * Revision 1.3  2003/08/20 17:32:58  edavis
 * Minor name change.
 *
 * Revision 1.2  2003/05/01 23:45:41  edavis
 * Minor fix.
 *
 * Revision 1.1  2003/03/04 23:11:32  edavis
 * Added for 0.7 release (addition of CatGenServlet). Not fully implemented.
 *
 *
 */
