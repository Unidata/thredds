// $Id: TestCatGenServletConfig.java 62 2006-07-12 21:41:46Z edavis $

package thredds.cataloggen.servlet;

import junit.framework.TestCase;

import java.util.Iterator;
import java.io.File;
import java.io.IOException;

/**
 *
 */
public class TestCatGenServletConfig extends TestCase
{
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( TestCatGenServletConfig.class );

  // @todo Modify CatGenServletConfig so that these tests can be run on resources rather than files.
  private String configPath = "src/test/classes/thredds/cataloggen/servlet";

  private String configEmptyFileName = "configEmpty.xml";
  private String configOneItemFileName = "configOneItem.xml";

  private CatGenTimerTask taskOne = null;
  private String taskOneName = "Task1";
  private String taskOneConfigDocName = "configCatGenExample1.xml";
  private String taskOneResultsFileName = "myCatalogExample1.xml";
  private int taskOneDelayInMinutes = 1;
  private int taskOnePeriodInMinutes = 0;

  private CatGenTimerTask taskTwo = null;
  private String taskTwoName = "Task2";
  private String taskTwoConfigDocName = "configCatGenExample2.xml";
  private String taskTwoResultsFileName = "myCatalogExample2.xml";
  private int taskTwoDelayInMinutes = 2;
  private int taskTwoPeriodInMinutes = 60;

  private CatGenServletConfig me = null;
  private CatGenTimerTask aTask = null;

  public TestCatGenServletConfig( String name)
  {
    super( name);
  }

  protected void setUp()
  {
    taskOne = new CatGenTimerTask( taskOneName, taskOneConfigDocName,
                                   taskOneResultsFileName,
                                   taskOnePeriodInMinutes, taskOneDelayInMinutes);
    taskOne.init( new File( configPath), new File( configPath));

    taskTwo = new CatGenTimerTask( taskTwoName, taskTwoConfigDocName,
                                   taskTwoResultsFileName,
                                   taskTwoPeriodInMinutes, taskTwoDelayInMinutes);
    taskTwo.init( new File( configPath), new File( configPath));

    return;
  }

//  protected void tearDown()
//  {
//  }

  /** Test reading an empty config file.
   *  Succeeds if not null, has same file name as given, and contains no tasks.
   */
  public void testReadEmpty()
  {
    try
    {
      me = new CatGenServletConfig( new File( configPath), new File( configPath), configEmptyFileName);
    }
    catch ( IOException e )
    {
      String tmpMsg = "Exception thrown while creating a CatGenServletConfig " +
                      "<resultPath=" + this.configPath + " - configPath=" + this.configPath +
                      " - filename=" + this.configEmptyFileName + ">:" + e.getMessage();
      log.info( "testReadEmpty(): " + tmpMsg );
      assertTrue( tmpMsg, false );
    }
    assertTrue( me != null);
    assertTrue( me.getServletConfigDocName().equals( configEmptyFileName));
    assertTrue( ! me.getTaskIterator().hasNext());
  }

  /** Test reading a config file with one item.
   *  Succeeds if not null, has same file name as given, and contains one tasks.
   */
  public void testReadOneItem()
  {
    try
    {
      me = new CatGenServletConfig( new File( configPath), new File( configPath), configOneItemFileName);
    }
    catch ( IOException e )
    {
      String tmpMsg = "Exception thrown while creating a CatGenServletConfig " +
                      "<resultPath=" + this.configPath + " - configPath=" + this.configPath +
                      " - filename=" + this.configOneItemFileName + ">:" + e.getMessage();
      log.info( "testReadOneItem(): " + tmpMsg );
      assertTrue( tmpMsg, false );
    }
    assertTrue( "Config doc name <" + me.getServletConfigDocName() + "> does not match expected <" + configOneItemFileName + ">.",
                me.getServletConfigDocName().equals( configOneItemFileName));
    Iterator it = me.getTaskIterator();
    assertTrue( "Config doc <" + me.getServletConfigDocName() + "> has no tasks.",
                it.hasNext());
    aTask = (CatGenTimerTask) it.next();
    assertTrue( "The name of the task listed in config doc <" + aTask.getName() + "> does not match expected <" + taskOneName + ">.",
                aTask.getName().equals( taskOneName));
    assertTrue( "The config doc <" + me.getServletConfigDocName() + "> contains a second task.",
                ! it.hasNext());
  }

  /** Test adding an item to a config file.
   *  Succeeds if task is not in config before add and is after.
   */
  public void testAddItem()
  {
    try
    {
      me = new CatGenServletConfig( new File( configPath), new File( configPath), configOneItemFileName);
    }
    catch ( IOException e )
    {
      String tmpMsg = "Exception thrown while creating a CatGenServletConfig " +
                      "<resultPath=" + this.configPath + " - configPath=" + this.configPath +
                      " - filename=" + this.configOneItemFileName + ">:" + e.getMessage();
      log.info( "testAddItem(): " + tmpMsg );
      assertTrue( tmpMsg, false );
    }
    assertTrue( "CatGenServletConfig() returned a null (is this even possible?).",
                me != null);
    assertTrue( "Task <" + taskTwo.getName() + "> was found.",
                me.findTask( taskTwo.getName()) == null);
    try
    {
      assertTrue( "Adding task <" + taskTwo.getName() + "> failed.",
                  me.addTask( taskTwo));
    }
    catch ( IOException e )
    {
      String tmpMsg = "Exception thrown while adding a task <" + taskTwo.getName() + "> to a CatGenServletConfig.";
      log.info( "testAddItem(): " + tmpMsg );
      assertTrue( tmpMsg, false );
    }
    assertTrue( "Didn't find added task <" + taskTwo.getName() + ">.",
                me.findTask( taskTwo.getName()).equals( taskTwo));

    // Remove the added task.
    try
    {
      assertTrue( "Removing task <" + taskTwo.getName() + "> failed.",
                  me.removeTask( taskTwo) );
    }
    catch ( IOException e )
    {
      String tmpMsg = "Exception thrown while removing a task <" + taskTwo.getName() + "> from a CatGenServletConfig.";
      log.info( "testAddItem(): " + tmpMsg );
      assertTrue( tmpMsg, false );
    }
    assertTrue( "Found removed task <" + taskTwo.getName() + ">.",
                me.findTask( taskTwo.getName()) == null );
  }

  /** Test adding a duplicate item to a config file.
   *  Succeeds if task is in config before and is after and addTask() is false.
   */
  public void testAddDuplicateItem()
  {
    try
    {
      me = new CatGenServletConfig( new File( configPath), new File( configPath), configOneItemFileName);
    }
    catch ( IOException e )
    {
      String tmpMsg = "Exception thrown while creating a CatGenServletConfig " +
                      "<resultPath=" + this.configPath + " - configPath=" + this.configPath +
                      " - filename=" + this.configOneItemFileName + ">:" + e.getMessage();
      log.info( "testAddItem(): " + tmpMsg );
      assertTrue( tmpMsg, false );
    }
    assertTrue( "Name of found task does not match search name<" + taskOne.getName() + ">.",
                me.findTask( taskOne.getName()).getName().equals( taskOne.getName()));
    try
    {
      assertFalse( "Adding duplicate task <" + taskOne.getName() + "> failed.",
                   me.addTask( taskOne));
    }
    catch ( IOException e )
    {
      String tmpMsg = "Exception thrown while adding a task <" + taskOne.getName() + "> to a CatGenServletConfig.";
      log.info( "testAddItem(): " + tmpMsg );
      assertTrue( tmpMsg, false );
    }
    assertTrue( "Name of found duplicate task does not equal search name <" + taskOne.getName() + ">.",
                me.findTask( taskOne.getName()).getName().equals( taskOne.getName()));
  }

}
/*
 * $Log: TestCatGenServletConfig.java,v $
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
 * Revision 1.2  2005/06/07 22:50:25  edavis
 * Fixed catalogRef links so relative to catalog instead of to service.
 * Fixed all tests in TestAllCatalogGen (including changing directory
 * filters because catalogRef names no longer contain slashes ("/").
 *
 * Revision 1.1  2005/03/30 05:41:19  edavis
 * Simplify build process: 1) combine all build scripts into one,
 * thredds/build.xml; 2) combine contents of all resources/ directories into
 * one, thredds/resources; 3) move all test source code and test data into
 * thredds/test/src and thredds/test/data; and 3) move all schemas (.xsd and .dtd)
 * into thredds/resources/resources/thredds/schemas.
 *
 * Revision 1.1  2004/05/11 16:29:08  edavis
 * Updated to work with new thredds.catalog 0.6 stuff and the THREDDS
 * servlet framework.
 *
 * Revision 1.3  2003/08/29 21:38:23  edavis
 * The following changes where made:
 *
 *  1) Added more extensive logging (changed from thredds.util.Log and
 * thredds.util.Debug to using Log4j).
 *
 * 2) Improved existing error handling and added additional error
 * handling where problems could fall through the cracks. Added some
 * catching and throwing of exceptions but also, for problems that aren't
 * fatal, added the inclusion in the resulting catalog of datasets with
 * the error message as its name.
 *
 * 3) Change how the CatGenTimerTask constructor is given the path to the
 * config files and the path to the resulting files so that resulting
 * catalogs are placed in the servlet directory space. Also, add ability
 * for servlet to serve the resulting catalogs.
 *
 * 4) Switch from using java.lang.String to using java.io.File for
 * handling file location information so that path seperators will be
 * correctly handled. Also, switch to java.net.URI rather than
 * java.io.File or java.lang.String where necessary to handle proper
 * URI/URL character encoding.
 *
 * 5) Add handling of requests when no path ("") is given, when the root
 * path ("/") is given, and when the admin path ("/admin") is given.
 *
 * 6) Fix the PUTting of catalogGenConfig files.
 *
 * 7) Start adding GDS DatasetSource capabilities.
 *
 * Revision 1.2  2003/08/20 17:32:26  edavis
 * Fix to work in new test environment.
 *
 * Revision 1.1  2003/05/01 23:50:46  edavis
 * Added TestCatGenServletConfig.java for testing the reading of config files and
 * adding items to the config files.  Added data files (config files)
 * for the TestCatGenServletConfig tests.
 *
 *
 */