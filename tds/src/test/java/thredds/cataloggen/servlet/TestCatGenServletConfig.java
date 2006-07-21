package thredds.cataloggen.servlet;

import junit.framework.TestCase;

import java.util.Iterator;
import java.io.File;
import java.io.IOException;

// @todo Straighten out test file names.
// @todo Change testReadEmpty() to testReadOldEmpty()
// @todo Add testReadNewEmpty()
/**
 *
 */
public class TestCatGenServletConfig extends TestCase
{
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( TestCatGenServletConfig.class );

  private String configPath = "src/test/data/thredds/cataloggen/servlet";

  private String testCGSC_readOldEmpty_FileName = "testCatGenServletConfig.readOldEmpty.xml";
  private String testCGSC_readNewEmpty_FileName = "testCatGenServletConfig.readNewEmpty.xml";
  private String testCGSC_readOneItem_FileName = "testCatGenServletConfig.readOneItem.xml";

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

  public TestCatGenServletConfig( String name)
  {
    super( name);
  }

  protected void setUp()
  {
    taskOne = new CatGenTimerTask( taskOneName, taskOneConfigDocName,
                                   taskOneResultsFileName,
                                   taskOnePeriodInMinutes, taskOneDelayInMinutes);

    taskTwo = new CatGenTimerTask( taskTwoName, taskTwoConfigDocName,
                                   taskTwoResultsFileName,
                                   taskTwoPeriodInMinutes, taskTwoDelayInMinutes);

    return;
  }

//  protected void tearDown()
//  {
//  }

  /** Test reading an empty config file.
   *  Succeeds if not null, has same file name as given, and contains no tasks.
   */
  public void testReadOldEmpty()
  {
    try
    {
      me = new CatGenServletConfig( new File( configPath), new File( configPath), testCGSC_readOldEmpty_FileName );
    }
    catch ( IOException e )
    {
      String tmpMsg = "Exception thrown while creating a CatGenServletConfig " +
                      "<resultPath=" + this.configPath + " - configPath=" + this.configPath +
                      " - filename=" + this.testCGSC_readOldEmpty_FileName + ">:" + e.getMessage();
      log.info( "testReadOldEmpty(): " + tmpMsg );
      assertTrue( tmpMsg, false );
    }
    assertTrue( "Failed to create a CatGenServletConfig.",
                me != null);
    assertTrue( "Config doc name <" + me.getServletConfigDocName() + "> not as expected <" + testCGSC_readOldEmpty_FileName + ">.",
                me.getServletConfigDocName().equals( testCGSC_readOldEmpty_FileName ));
    assertTrue( "Empty config doc resulted in non-empty list.",
                ! me.getUnmodTaskIterator().hasNext());
  }

  public void testReadNewEmpty()
  {
    try
    {
      me = new CatGenServletConfig( new File( configPath ), new File( configPath ), testCGSC_readNewEmpty_FileName );
    }
    catch ( IOException e )
    {
      String tmpMsg = "Exception thrown while creating a CatGenServletConfig " +
                      "<resultPath=" + this.configPath + " - configPath=" + this.configPath +
                      " - filename=" + this.testCGSC_readOldEmpty_FileName + ">:" + e.getMessage();
      log.info( "testReadNewEmpty(): " + tmpMsg );
      assertTrue( tmpMsg, false );
    }
    assertTrue( "Failed to create a CatGenServletConfig.",
                me != null );
    assertTrue( "Config doc name <" + me.getServletConfigDocName() + "> not as expected <" + testCGSC_readNewEmpty_FileName + ">.",
                me.getServletConfigDocName().equals( testCGSC_readNewEmpty_FileName ) );
    assertTrue( "Empty config doc resulted in non-empty list.",
                ! me.getUnmodTaskIterator().hasNext() );
  }

  /** Test reading a config file with one item.
   *  Succeeds if not null, has same file name as given, and contains one tasks.
   */
  public void testReadOneItem()
  {
    try
    {
      me = new CatGenServletConfig( new File( configPath), new File( configPath), testCGSC_readOneItem_FileName );
    }
    catch ( IOException e )
    {
      String tmpMsg = "Exception thrown while creating a CatGenServletConfig " +
                      "<resultPath=" + this.configPath + " - configPath=" + this.configPath +
                      " - filename=" + this.testCGSC_readOneItem_FileName + ">:" + e.getMessage();
      log.info( "testReadOneItem(): " + tmpMsg );
      assertTrue( tmpMsg, false );
    }
    assertTrue( "Config doc name <" + me.getServletConfigDocName() + "> not as expected <" + testCGSC_readOneItem_FileName + ">.",
                me.getServletConfigDocName().equals( testCGSC_readOneItem_FileName ));
    Iterator it = me.getUnmodTaskIterator();
    assertTrue( "Config doc <" + me.getServletConfigDocName() + "> has no tasks.",
                it.hasNext());
    CatGenTimerTask aTask = (CatGenTimerTask) it.next();
    assertTrue( "Config doc's task name <" + aTask.getName() + "> not as expected <" + taskOneName + ">.",
                aTask.getName().equals( taskOneName));
    assertTrue( "Single item config doc <" + me.getServletConfigDocName() + "> has extra item(s).",
                ! it.hasNext());
  }

  /** Test adding and then removing an item to a config file.
   *  Succeeds if task is not in config before add and is after.
   */
  public void testAddAndRemoveItem()
  {
    try
    {
      me = new CatGenServletConfig( new File( configPath), new File( configPath), testCGSC_readOneItem_FileName );
    }
    catch ( IOException e )
    {
      String tmpMsg = "Exception thrown while creating a CatGenServletConfig " +
                      "<resultPath=" + this.configPath + " - configPath=" + this.configPath +
                      " - filename=" + this.testCGSC_readOneItem_FileName + ">:" + e.getMessage();
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
      me = new CatGenServletConfig( new File( configPath), new File( configPath), testCGSC_readOneItem_FileName );
    }
    catch ( IOException e )
    {
      String tmpMsg = "Exception thrown while creating a CatGenServletConfig " +
                      "<resultPath=" + this.configPath + " - configPath=" + this.configPath +
                      " - filename=" + this.testCGSC_readOneItem_FileName + ">:" + e.getMessage();
      log.info( "testAddItem(): " + tmpMsg );
      assertTrue( tmpMsg, false );
    }
    assertTrue( "Name of found task does not match search name<" + taskOne.getName() + ">.",
                me.findTask( taskOne.getName()).getName().equals( taskOne.getName()));
    try
    {
      assertFalse( "Adding duplicate task <" + taskOne.getName() + "> succeeded.",
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
