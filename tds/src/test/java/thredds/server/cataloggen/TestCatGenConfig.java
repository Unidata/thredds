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
package thredds.server.cataloggen;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;

/**
 *
 */
public class TestCatGenConfig extends TestCase
{

  private String configPath = "src/test/data/thredds/cataloggen/servlet";

  private String testCGSC_readOldEmpty_FileName = "testCatGenServletConfig.readOldEmpty.xml";
  private String testCGSC_readNewEmpty_FileName = "testCatGenServletConfig.readNewEmpty.xml";
  private String testCGSC_readOneItem_FileName = "testCatGenServletConfig.readOneItem.xml";

  private String taskOneName = "Task1";
//  private String taskOneConfigDocName = "testCatGenServletConfig.exampleTaskConfig1.xml";
//  private String taskOneResultsFileName = "testCatGenServletConfig.resultCatalog1.xml";
//  private int taskOneDelayInMinutes = 1;
//  private int taskOnePeriodInMinutes = 0;
//
//  private String taskTwoName = "Task2";
//  private String taskTwoConfigDocName = "testCatGenServletConfig.exampleTaskConfig2.xml";
//  private String taskTwoResultsFileName = "testCatGenServletConfig.resultCatalog2.xml";
//  private int taskTwoDelayInMinutes = 2;
//  private int taskTwoPeriodInMinutes = 60;

  private CatGenConfigParser catGenConfigParser;
  private CatGenConfig catGenConfig;

  public TestCatGenConfig( String name)
  {
    super( name);
  }

  /** Test reading an empty config file.
   *  Succeeds if not null, has same file name as given, and contains no tasks.
   */
  public void testReadOldEmpty()
  {
    this.catGenConfigParser = new CatGenConfigParser();
    File configFile = new File( this.configPath, testCGSC_readOldEmpty_FileName );
    try
    {
      this.catGenConfig = catGenConfigParser.parseXML( configFile );
    }
    catch ( IOException e )
    {
      fail( "IOException while parsing a CatGenConfig [" + configFile + "]: " + e.getMessage());
      return;
    }

    assertTrue( "Failed to create a CatGenConfig.",
                this.catGenConfig != null);
    assertTrue( "Empty CatGenConfig doc ["+configFile+"] resulted in non-empty list.",
                this.catGenConfig.getTaskInfoList().isEmpty());
  }

  public void testReadNewEmpty()
  {
    this.catGenConfigParser = new CatGenConfigParser();
    File configFile = new File( this.configPath, testCGSC_readNewEmpty_FileName );
    try
    {
      this.catGenConfig = catGenConfigParser.parseXML( configFile );
    }
    catch ( IOException e )
    {
      fail( "IOException while parsing a CatGenConfig [" + configFile + "]: " + e.getMessage());
      return;
    }

    assertTrue( "Failed to create a CatGenConfig.",
                this.catGenConfig != null );
    assertTrue( "Empty CatGenConfig doc [" + configFile + "] resulted in non-empty list.",
                this.catGenConfig.getTaskInfoList().isEmpty() );
  }

  /** Test reading a config file with one item.
   *  Succeeds if not null, has same file name as given, and contains one tasks.
   */
  public void testReadOneItem()
  {
    this.catGenConfigParser = new CatGenConfigParser();
    File configFile = new File( this.configPath, testCGSC_readOneItem_FileName );
    try
    {
      this.catGenConfig = catGenConfigParser.parseXML( configFile );
    }
    catch ( IOException e )
    {
      fail( "IOException while parsing a CatGenConfig [" + configFile + "]: " + e.getMessage() );
      return;
    }

    assertTrue( "Failed to create a CatGenConfig.",
                this.catGenConfig != null );
    assertTrue( "Non-empty CatGenConfig doc [" + configFile + "] resulted in empty list.",
                ! this.catGenConfig.getTaskInfoList().isEmpty() );

    assertTrue( "One item CatGenConfig doc [" + configFile + "] resulted in more than one item.",
                this.catGenConfig.getTaskInfoList().size() == 1);

    CatGenTaskConfig task = this.catGenConfig.getTaskInfoList().get( 0 );


    assertTrue( "Config doc's task name [" + task.getName() + "] not as expected [" + taskOneName + "].",
                task.getName().equals( taskOneName));
  }
}
