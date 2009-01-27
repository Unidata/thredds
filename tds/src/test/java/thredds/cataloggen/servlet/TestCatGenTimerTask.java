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
