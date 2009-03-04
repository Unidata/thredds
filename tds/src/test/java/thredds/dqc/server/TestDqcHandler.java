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
package thredds.dqc.server;

import junit.framework.*;

import java.io.IOException;

/**
 * A description
 *
 * User: edavis
 * Date: Dec 24, 2003
 * Time: 2:11:51 PM
 */
public class TestDqcHandler extends TestCase
{
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger( TestDqcHandler.class);

  private String configResourcePath = "/thredds/dqc/server";
  private String goodLatestModelConfigResourceName = "configLatestModelExampleGood.xml";

  private DqcServletConfigItem handlerInfo = null;
  private String handlerName = null;
  private String handlerDescription = null;
  private String handlerClassName = null;
  private String handlerConfigFileName = null;


  private DqcHandler me;

  public TestDqcHandler( String name )
  {
    super( name );
  }

  /** Test the DqcHandler factory() method with a class that doesn't exist. */
  public void testFactoryNonExistClass()
  {
    String tmpMsg = null;

    logger.debug( "testFactoryNonExistClass(): starting.");

    handlerName = "NonExistClass";
    handlerDescription = "This class doesn't exist.";
    handlerClassName = "this.class.does.not.Exist";
    handlerConfigFileName = goodLatestModelConfigResourceName;

    handlerInfo = new DqcServletConfigItem( handlerName, handlerDescription,
                                            handlerClassName, handlerConfigFileName );

    // Try creating a DqcHandler class with a class that doesn't exist.
    try
    {
      me = DqcHandler.factory( handlerInfo, configResourcePath );
    }
    catch ( DqcHandlerInstantiationException e )
    {
      logger.debug( "testFactoryNonExistClass(): Expected DqcHandlerInstantiationException thrown by DqcHandler.factory() when given a nonexistent class <" + handlerClassName + ">.", e );
      return;
    }
    catch ( IOException e )
    {
      tmpMsg = "Unexpected IOException from DqcHandler.factory() when given a nonexistent class <" + handlerClassName + ">: " + e.getMessage();
      logger.debug( "testFactoryNonExistClass(): " + tmpMsg, e );
      assertTrue( tmpMsg, false );
    }
    tmpMsg = "Expected DqcHandlerInstantiationException not thrown by DqcHandler.factory() when given a nonexistent class <" + handlerClassName + ">.";
    logger.debug( "testFactoryNonExistClass(): " + tmpMsg );
    assertTrue( tmpMsg, false );
  }

  /** Test the DqcHandler factory() method with a class that is not a DqcHandler. */
  public void testFactoryNonHandlerClass()
  {
    String tmpMsg = null;

    logger.debug( "testFactoryNonHandlerClass(): starting.");

    handlerName = "NonDqcHandlerClass";
    handlerDescription = "This class is not a DqcHandler.";
    handlerClassName = "thredds.dqc.server.DqcServletConfigItem";
    handlerConfigFileName = goodLatestModelConfigResourceName;

    handlerInfo = new DqcServletConfigItem( handlerName, handlerDescription,
                                            handlerClassName, handlerConfigFileName );

    // Try creating a DqcHandler class with a class that is not a DqcHandler class.
    try
    {
      me = DqcHandler.factory( handlerInfo, this.configResourcePath );
    }
    catch ( ClassCastException e )
    {
      logger.debug( "testFactoryNonHandlerClass(): Expected ClassCastException thrown by DqcHandler.factory() when given a non-DqcHandler class <" + handlerClassName + ">.", e );
      return;
    }
    catch ( DqcHandlerInstantiationException e )
    {
      tmpMsg = "Unexpected DqcHandlerInstantiationException from DqcHandler.factory() when given a non-DqcHandler class <" + handlerClassName + ">: " + e.getMessage();
      logger.debug( "testFactoryNonHandlerClass(): " + tmpMsg, e );
      assertTrue( tmpMsg, false );
    }
    catch ( IOException e )
    {
      tmpMsg = "Unexpected IOException from DqcHandler.factory() when given a non-DqcHandler class <" + handlerClassName + ">: " + e.getMessage();
      logger.debug( "testFactoryNonHandlerClass(): " + tmpMsg, e );
      assertTrue( tmpMsg, false );
    }
    tmpMsg = "Expected ClassCastException not thrown by DqcHandler.factory() when given a non-DqcHandler class <" + handlerClassName + ">.";
    logger.debug( "testFactoryNonHandlerClass(): " + tmpMsg );
    assertTrue( tmpMsg, false );
  }

  /** Test the DqcHandler factory() method with a LatestModel DqcHandler. */
  public void testFactoryGood()
  {
    String tmpMsg = null;

    logger.debug( "testFactoryGood(): starting." );

    handlerName = "GoodDqcHandlerClass";
    handlerDescription = "This class is a DqcHandler.";
    handlerClassName = "thredds.dqc.server.LatestModel";
    handlerConfigFileName = goodLatestModelConfigResourceName;

    handlerInfo = new DqcServletConfigItem( handlerName, handlerDescription,
                                            handlerClassName, handlerConfigFileName );

    // Create a LatestModel DQCHandler.
    try
    {
      me = DqcHandler.factory( handlerInfo, this.configResourcePath );
    }
    catch ( DqcHandlerInstantiationException e )
    {
      tmpMsg = "Unexpected DqcHandlerInstantiationException from DqcHandler.factory() when given a DqcHandler class <" + handlerClassName + ">: " + e.getMessage();
      logger.debug( "testFactoryGood(): " + tmpMsg, e );
      assertTrue( tmpMsg, false );
    }
    catch ( IOException e )
    {
      tmpMsg = "Unexpected IOException from DqcHandler.factory() when given a DqcHandler class <" + handlerClassName + ">: " + e.getMessage();
      logger.debug( "testFactoryGood(): " + tmpMsg, e );
      assertTrue( tmpMsg, false );
    }
    assertTrue( "testFactoryGood(): DqcHandler.factory() returned a null.", me != null );
  }

}
