package thredds.cataloggen.servlet;

import junit.framework.*;

import java.io.File;

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

  private CatGenTimerTask taskTwo = null;
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

    taskTwo = new CatGenTimerTask( taskTwoName, taskTwoConfigDocName,
                                   taskTwoResultsFileName,
                                   taskTwoPeriodInMinutes, taskTwoDelayInMinutes );
    taskTwo.init( new File( configPath ), new File( configPath ) );
  }

//  protected void tearDown()
//  {
//  }

  public void testValid()
  {
    // Test task one.
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
    boolean valid = taskOne.isValid( msg );
    assertTrue( "Task <" + taskOne.getName() + " - " + taskOne.getConfigDocName() + "> is not valid: " + msg,
                valid );

    // Test task two.
    assertTrue( "Task name <" + taskTwo.getName() + "> doesn't match expected <" + taskTwoName + ">.",
                taskTwo.getName().equals( taskTwoName));
    assertTrue( "Task config doc name <" + taskTwo.getConfigDocName() + "> doesn't match expected <" + taskTwoConfigDocName + ">.",
                taskTwo.getConfigDocName().equals( taskTwoConfigDocName ) );
    assertTrue( "Task result doc name <" + taskTwo.getResultFileName() + "> doesn't match expected <" + taskTwoResultsFileName + ">.",
                taskTwo.getResultFileName().equals( taskTwoResultsFileName ) );
    assertTrue( "Task period in minutes <" + taskTwo.getPeriodInMinutes() + "> doesn't match expected <" + taskTwoPeriodInMinutes + ">.",
                taskTwo.getPeriodInMinutes() == taskTwoPeriodInMinutes );
    assertTrue( "Task delay in minutes <" + taskTwo.getDelayInMinutes() + "> doesn't match expected <" + taskTwoDelayInMinutes + ">.",
                taskTwo.getDelayInMinutes() == taskTwoDelayInMinutes );

    log.debug( "testValid(): taskTwo - name=" + taskTwo.getName() +
                  "; config doc name=" + taskTwo.getConfigDocName() + "" +
                  "; result doc name=" + taskTwo.getResultFileName() + "" +
                  "; period in minutes=" + taskTwo.getPeriodInMinutes() + "" +
                  "; delay in minutes=" + taskTwo.getDelayInMinutes() + ".");

    msg = new StringBuffer();
    valid = taskTwo.isValid( msg );
    assertTrue( "Task <" + taskTwo.getName() + " - " + taskTwo.getConfigDocName() + "> is not valid: " + msg,
                valid );
  }

}
