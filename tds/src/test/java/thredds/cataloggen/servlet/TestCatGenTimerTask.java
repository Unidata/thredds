package thredds.cataloggen.servlet;

import junit.framework.*;

import java.io.File;

/**
 *
 */
public class TestCatGenTimerTask extends TestCase
{
//  private static org.slf4j.Logger log =
//          org.slf4j.LoggerFactory.getLogger( TestCatGenTimerTask.class );

  // @todo Modify CatGenTimerTask so that these tests can be run on resources rather than files.
  private String configPath = "src/test/data/thredds/cataloggen/servlet";

  private String taskName = "Task1";
  private String taskConfigDocName = "testCatGenTimerTask.exampleCatGenConfig.xml";
  private String taskResultsFileName = "testCatGenTimerTask.nonexistentResultCatalog.xml";
  private int taskDelayInMinutes = 1;
  private int taskPeriodInMinutes = 30;

  private int taskPeriodInMinutesZero = 0;

  public TestCatGenTimerTask( String name)
  {
    super( name);
  }

//  protected void setUp() { }
//  protected void tearDown() {}

  public void testValid()
  {
    // Setup task.
    CatGenTimerTask task =
            new CatGenTimerTask( taskName, taskConfigDocName,
                                 taskResultsFileName,
                                 taskPeriodInMinutes, taskDelayInMinutes );
    task.init( new File( configPath ), new File( configPath ) );

    // Test task.
    assertTrue( "Task name <" + task.getName() + "> doesn't match expected <" + taskName + ">.",
                task.getName().equals( taskName ));
    assertTrue( "Task config doc name <" + task.getConfigDocName() + "> doesn't match expected <" + taskConfigDocName + ">.",
                task.getConfigDocName().equals( taskConfigDocName ) );
    assertTrue( "Task result doc name <" + task.getResultFileName() + "> doesn't match expected <" + taskResultsFileName + ">.",
                task.getResultFileName().equals( taskResultsFileName ) );
    assertTrue( "Task period in minutes <" + task.getPeriodInMinutes() + "> doesn't match expected <" + taskPeriodInMinutes + ">.",
                task.getPeriodInMinutes() == taskPeriodInMinutes );
    assertTrue( "Task delay in minutes <" + task.getDelayInMinutes() + "> doesn't match expected <" + taskDelayInMinutes + ">.",
                task.getDelayInMinutes() == taskDelayInMinutes );

    StringBuilder msg = new StringBuilder();
    boolean valid = task.isValid( msg );
    assertTrue( "Task <" + task.getName() + " - " + task.getConfigDocName() + "> is not valid: " + msg,
                valid );

    // Setup task with period set to zero.
    task = new CatGenTimerTask( taskName, taskConfigDocName,
                                taskResultsFileName,
                                taskPeriodInMinutesZero, taskDelayInMinutes );
    task.init( new File( configPath ), new File( configPath ) );

    // Test task.
    assertTrue( "Task name <" + task.getName() + "> doesn't match expected <" + taskName + ">.",
                task.getName().equals( taskName ) );
    assertTrue( "Task config doc name <" + task.getConfigDocName() + "> doesn't match expected <" + taskConfigDocName + ">.",
                task.getConfigDocName().equals( taskConfigDocName ) );
    assertTrue( "Task result doc name <" + task.getResultFileName() + "> doesn't match expected <" + taskResultsFileName + ">.",
                task.getResultFileName().equals( taskResultsFileName ) );
    assertTrue( "Task period in minutes <" + task.getPeriodInMinutes() + "> doesn't match expected <" + taskPeriodInMinutesZero + ">.",
                task.getPeriodInMinutes() == taskPeriodInMinutesZero );
    assertTrue( "Task delay in minutes <" + task.getDelayInMinutes() + "> doesn't match expected <" + taskDelayInMinutes + ">.",
                task.getDelayInMinutes() == taskDelayInMinutes );

    msg = new StringBuilder();
    valid = task.isValid( msg );
    assertTrue( "Task <" + task.getName() + " - " + task.getConfigDocName() + "> is not valid: " + msg,
                valid );
  }

}
