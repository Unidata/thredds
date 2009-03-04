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
