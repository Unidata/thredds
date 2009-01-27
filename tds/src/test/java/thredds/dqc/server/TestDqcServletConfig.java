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
// $Id: TestDqcServletConfig.java 51 2006-07-12 17:13:13Z caron $
package thredds.dqc.server;

import junit.framework.TestCase;

import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: edavis
 * Date: Nov 23, 2003
 * Time: 8:38:21 PM
 * To change this template use Options | File Templates.
 */
public class TestDqcServletConfig extends TestCase
{
  static private org.slf4j.Logger logger =
          org.slf4j.LoggerFactory.getLogger( TestDqcServletConfig.class);

  private String configResourcePath = "/thredds/dqc/server";

  private String goodConfigResourceName = "configExampleGood.xml";
  private String badConfigResourceName = "configExampleBad.xml";
  private String emptyConfigResourceName = "configExampleEmpty.xml";
  private String nonExistConfigResourceName = "configThisFileDoesNotExist.xml";

  private String item1Name = "example1";
  private String item1Description = "An example (1) DqcServletConfigItem";
  private String item1HandlerClassName = "an.example.dqc.Handler1";
  private String item1HandlerConfigFileName = "exampleConfig1.xml";

  private String item2Name = "example2";
  private String item2Description = "An example (2) DqcServletConfigItem";
  private String item2HandlerClassName = "an.example.dqc.Handler2";
  private String item2HandlerConfigFileName = "exampleConfig2.xml";

  private DqcServletConfig me;
  private DqcServletConfig me2;

  public TestDqcServletConfig( String name)
  {
    super( name);
  }

  protected void setUp()
  {
  }

  public void testGoodRead()
  {
    logger.debug( "testGoodRead(): starting.");

    String resourceName = null;

    // Open a config doc resource.
    try
    {
      resourceName = configResourcePath + "/" + goodConfigResourceName;
      me = new DqcServletConfig( configResourcePath, goodConfigResourceName );
    }
    catch (IOException e)
    {
      // Constructor threw exception so fail.
      assertTrue( "Unexpected IOException thrown by DqcServletConfig (" + resourceName +
                  "): " + e.getMessage(), false);
    }

    // Test that item is as expected.
    DqcServletConfigItem item;
    item = me.findItem( item1Name);
    assertTrue( "Example item (" + item1Name + ") not found.",
                item != null);
    assertTrue( "Item description != \"" + item1Description + "\"",
                item.getDescription().equals( item1Description));
    assertTrue( "Item handler name != \"" + item1HandlerClassName + "\"",
                item.getHandlerClassName().equals( item1HandlerClassName));
    assertTrue( "Item handler config file name != \"" + item1HandlerConfigFileName + "\"",
                item.getHandlerConfigFileName().equals( item1HandlerConfigFileName));

    // Test that item is as expected.
    item = me.findItem( item2Name);
    assertTrue( "Example item (" + item2Name + ") not found.",
                item != null);
    assertTrue( "Item description != \"" + item2Description + "\"",
                item.getDescription().equals( item2Description));
    assertTrue( "Item handler name != \"" + item2HandlerClassName + "\"",
                item.getHandlerClassName().equals( item2HandlerClassName));
    assertTrue( "Item handler config file name != \"" + item2HandlerConfigFileName + "\"",
                item.getHandlerConfigFileName().equals( item2HandlerConfigFileName));
  }

  public void testBadRead()
  {
    logger.debug( "testBadRead(): starting.");

    // Open a resource that isn't a config resource.
    try
    {
      me = new DqcServletConfig( configResourcePath, badConfigResourceName );
    }
    // @todo I would expect an exception here but none are thrown.
    // @todo Probably something in ucar.util.prefs should throw an exception.
    catch (IOException e)
    {
      assertTrue( "Unexpected IOException thrown by DqcServletConfig " +
                  "(ucar.util.prefs probably should throw exception here): " + e.getMessage(),
                  false);
      return;
    }
    catch (RuntimeException e)
    {
      assertTrue( "Unexpected exception thrown by DqcServletConfig " +
                  "(though ucar.util.prefs probably should throw exception here): " + e.getMessage(),
                  false );

    }

    // Though not even an XMLStore document, everything fine except that config item list is empty.
    assertTrue( "List of DqcServletConfigItem entries in config doc should be empty but is not.",
                ! me.getIterator().hasNext() );
  }

  public void testEmptyRead()
  {
    logger.debug( "testEmptyRead(): starting.");

    String resourceName = null;

    // Open a config doc resource that is empty (i.e., contains no config items).
    try
    {
      resourceName = configResourcePath + "/" + emptyConfigResourceName;
      me = new DqcServletConfig( configResourcePath, emptyConfigResourceName );
    }
    catch (Exception e)
    {
      // Exception thrown, fail.
      assertTrue( "Creation of DqcServletConfig (" + resourceName +
                  ") threw an exception: " + e.getMessage() + ".", false);
      return;
    }

    // If resource has item, fail.
    assertTrue( "Empty DqcServletConfig resource (" + resourceName +
                ") isn't empty.", ! me.getIterator().hasNext() );

  }

  public void testWrite()
  {
    logger.debug( "testWrite(): starting.");

    // Prepare to write to a temporary test file.
    File f = new File("TestWriteOfDqcServletConfigFile.xml");

    // Test if file is a directory.
    assertTrue( "Temporary test file <" + f.getAbsolutePath() + "> is unexpectedly a directory.",
                !f.isDirectory() );

    // If file already exists, delete it.
    if ( f.exists() )
      if ( ! f.delete())
    {
      assertTrue( "Couldn't delete already existing temporary test file <" + f.getAbsolutePath() + ">.",
                  false);
    }

    logger.debug( "testWrite(): test writing preferences to " + f.getAbsolutePath() + ".");

    try
    {
      assertTrue( "Couldn't create new file (" + f.getAbsolutePath() + ").",
                  f.createNewFile( ));
    }
    catch (IOException e)
    {
      assertTrue( "Exception thrown on createNewFile() <" + f.getAbsolutePath() + ">:" + e.getMessage(),
                  false);
    }
    assertTrue( "File can't be written (" + f.getAbsolutePath() + ").",
                f.canWrite());

    // Open a FileOutputStream for the file.
    OutputStream os = null;
    try
    {
      os = new FileOutputStream( f);
    }
    catch (FileNotFoundException e)
    {
      assertTrue( "FileNotFoundException creating OutputStream from " + f.getAbsolutePath() + ":" + e.getMessage(),
                  false);
    }

    // Read preferences from good config file.
    String resourceName = null;
    try
    {
      resourceName = configResourcePath + "/" + goodConfigResourceName;
      me = new DqcServletConfig( configResourcePath, goodConfigResourceName );
    }
    catch (Exception e)
    {
      // Exception thrown, fail.
      assertTrue( "DqcServletConfig (" + resourceName + ") threw an exception: " + e.getMessage(),
                  false);
    }

    // Write the preferences to new file.
    try
    {
      me.writeConfig( os);
    }
    catch (java.io.IOException e)
    {
      assertTrue( "Unexpected IOException thrown on DqcServletConfig.writeConfig("
                  + f.getAbsolutePath() + "): " + e.getMessage(),
                  false);
    }

    // Close the OutputStream to the new file.
    try
    {
      os.close();
    }
    catch (IOException e)
    {
      assertTrue( "Exception while closing FileOutputStream: " + e.getMessage(),
                  false);
    }

    // Read preferences from newly written file.
    try
    {
      me2 = new DqcServletConfig( f.getAbsoluteFile().getParentFile(), f.getName() );
    }
    catch (IOException e)
    {
      // Exception thrown, fail.
      assertTrue( "Unexpected IOException thrown by DqcServletConfig(" + f.getParent() + ", " + f.getName() + "): "
                  + e.getMessage() + ".", false);
    }

    // Test that item is as expected.
    DqcServletConfigItem item;
    item = me2.findItem( item1Name);
    assertTrue( "Example item (" + item1Name + ") not found.",
                item != null);
    assertTrue( "Item description != \"" + item1Description + "\"",
                item.getDescription().equals( item1Description));
    assertTrue( "Item handler name != \"" + item1HandlerClassName + "\"",
                item.getHandlerClassName().equals( item1HandlerClassName));
    assertTrue( "Item handler config file name != \"" + item1HandlerConfigFileName + "\"",
                item.getHandlerConfigFileName().equals( item1HandlerConfigFileName));

    // Test that item is as expected.
    item = me2.findItem( item2Name);
    assertTrue( "Example item (" + item2Name + ") not found.",
                item != null);
    assertTrue( "Item description != \"" + item2Description + "\"",
                item.getDescription().equals( item2Description));
    assertTrue( "Item handler name != \"" + item2HandlerClassName + "\"",
                item.getHandlerClassName().equals( item2HandlerClassName));
    assertTrue( "Item handler config file name != \"" + item2HandlerConfigFileName + "\"",
                item.getHandlerConfigFileName().equals( item2HandlerConfigFileName));

    // Delete the test config document file.
    assertTrue( "Couldn't delete file (" + f.getAbsolutePath() + ").",
                f.delete());
  }
}
/*
 * $Log: TestDqcServletConfig.java,v $
 * Revision 1.2  2006/01/23 22:11:14  edavis
 * Switch from log4j to SLF4J logging.
 *
 * Revision 1.1  2005/03/30 05:41:20  edavis
 * Simplify build process: 1) combine all build scripts into one,
 * thredds/build.xml; 2) combine contents of all resources/ directories into
 * one, thredds/resources; 3) move all test source code and test data into
 * thredds/test/src and thredds/test/data; and 3) move all schemas (.xsd and .dtd)
 * into thredds/resources/resources/thredds/schemas.
 *
 * Revision 1.5  2004/08/24 23:46:09  edavis
 * Update for DqcServlet version 0.3.
 *
 * Revision 1.4  2004/08/23 16:45:18  edavis
 * Update DqcServlet to work with DQC spec v0.3 and InvCatalog v1.0. Folded DqcServlet into the THREDDS server framework/build/distribution. Updated documentation (DqcServlet and THREDDS server).
 *
 * Revision 1.3  2004/01/15 19:43:07  edavis
 * Some additions to the tests.
 *
 * Revision 1.2  2003/12/11 01:08:51  edavis
 * Enhance test suite.
 *
 * Revision 1.1  2003/12/04 00:43:16  edavis
 * Initial version.
 *
 *
 */