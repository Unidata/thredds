// $Id: TestDqcServletConfigItem.java 51 2006-07-12 17:13:13Z caron $
package thredds.dqc.server;

import junit.framework.*;

import java.io.File;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: edavis
 * Date: Nov 23, 2003
 * Time: 8:38:21 PM
 * To change this template use Options | File Templates.
 */
public class TestDqcServletConfigItem extends TestCase
{
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger( TestDqcServletConfigItem.class);

  private String name1 = "name 1";
  private String description1 = "description 1";
  private String handlerClassName1 = "handler class name 1";
  private String handlerConfigFileName1 = "handler config file name 1";

  private String name2 = "name 2";
  private String description2 = "description 2";
  private String handlerClassName2 = "handler class name 2";
  private String handlerConfigFileName2 = "handler config file name 2";

  private DqcServletConfigItem me;
  private DqcServletConfigItem me2;

  public TestDqcServletConfigItem( String name)
  {
    super( name);
  }

  /** Test reading an empty config file.
   *  Succeeds if not null, has same file name as given, and contains no tasks.
   */
  public void testCtorGetSet()
  {
    logger.debug( "testCtorGetSet(): starting.");
    me = new DqcServletConfigItem( name1, description1, handlerClassName1,
                                   handlerConfigFileName1 );
    me2 = new DqcServletConfigItem( name2, description2, handlerClassName2,
                                    handlerConfigFileName2 );
    assertTrue( "DqcServletConfigItem() for " + name1 + " returned null.",
                me != null );
    assertTrue( "DqcServletConfigItem() for " + name2 + " returned null.",
                me2 != null );
    assertTrue( "Unexpected equality between " + me.toString() + " and " + me2.toString(),
                ! me.equals( me2 ) );


    assertTrue( "DqcServletConfigItem name <" + me.getName() + "> does not equal expected name <" + name1 + ">.",
                me.getName().equals( name1 ) );
    me.setName( name2);
    assertTrue( "Newly set DqcServletConfigItem name <" + me.getName() + "> does not equal expected name <" + name2 + ">.",
                me.getName().equals( name2));
    assertTrue( "Unexpected equality between " + me.toString() + " and " + me2.toString(),
                !me.equals( me2 ) );

    assertTrue( "DqcServletConfigItem description <" + me.getDescription() + "> does not equal expected description <" + description1 + ">.",
                me.getDescription().equals( description1));
    me.setDescription( description2);
    assertTrue( "Newly set DqcServletConfigItem description <" + me.getDescription() + "> does not equal expected description <" + description2 + ">.",
                me.getDescription().equals( description2));
    assertTrue( "Unexpected equality between " + me.toString() + " and " + me2.toString(),
                !me.equals( me2 ) );

    assertTrue( "DqcServletConfigItem handler class name <" + me.getHandlerClassName() + "> does not equal expected handler class name <" + handlerClassName1 + ">.",
                me.getHandlerClassName().equals( handlerClassName1));
    me.setHandlerClassName( handlerClassName2);
    assertTrue( "Newly set DqcServletConfigItem handler class name <" + me.getHandlerClassName() + "> does not equal expected handler class name <" + handlerClassName2 + ">.",
                me.getHandlerClassName().equals( handlerClassName2));
    assertTrue( "Unexpected equality between " + me.toString() + " and " + me2.toString(),
                !me.equals( me2 ) );

    assertTrue( "DqcServletConfigItem handler config filename <" + me.getHandlerConfigFileName() + "> does not equal expected handler config filename <" + handlerConfigFileName1 + ">.",
                me.getHandlerConfigFileName().equals( handlerConfigFileName1));
    me.setHandlerConfigFileName( handlerConfigFileName2);
    assertTrue( "Newly set DqcServletConfigItem handler config filename <" + me.getHandlerConfigFileName() + "> does not equal expected handler config filename <" + handlerConfigFileName2 + ">.",
                me.getHandlerConfigFileName().equals( handlerConfigFileName2));

    assertTrue( "Unexpected inequality between " + me.toString() + " and " + me2.toString(),
                me.equals( me2));
  }
}
/*
 * $Log: TestDqcServletConfigItem.java,v $
 * Revision 1.2  2006/01/23 22:11:15  edavis
 * Switch from log4j to SLF4J logging.
 *
 * Revision 1.1  2005/03/30 05:41:21  edavis
 * Simplify build process: 1) combine all build scripts into one,
 * thredds/build.xml; 2) combine contents of all resources/ directories into
 * one, thredds/resources; 3) move all test source code and test data into
 * thredds/test/src and thredds/test/data; and 3) move all schemas (.xsd and .dtd)
 * into thredds/resources/resources/thredds/schemas.
 *
 * Revision 1.4  2004/08/24 23:46:09  edavis
 * Update for DqcServlet version 0.3.
 *
 * Revision 1.3  2004/08/23 16:45:18  edavis
 * Update DqcServlet to work with DQC spec v0.3 and InvCatalog v1.0. Folded DqcServlet into the THREDDS server framework/build/distribution. Updated documentation (DqcServlet and THREDDS server).
 *
 * Revision 1.2  2004/01/15 19:43:08  edavis
 * Some additions to the tests.
 *
 */