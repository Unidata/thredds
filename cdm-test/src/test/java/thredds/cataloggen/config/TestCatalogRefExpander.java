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
// $Id: TestCatalogRefExpander.java 61 2006-07-12 21:36:00Z edavis $
package thredds.cataloggen.config;

import junit.framework.TestCase;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A description
 * <p/>
 * User: edavis
 * Date: Dec 9, 2004
 * Time: 4:17:29 PM
 */
public class TestCatalogRefExpander extends TestCase
{
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TestCatalogRefExpander.class);

  public TestCatalogRefExpander( String name )
  {
    super( name );
  }

  protected void setUp()
  {
  }

  /**
   * Test ...
   */
  public void testMatchAndSubstitutions()
  {
    log.debug( "testMatchAndSubstitutions(): starting." );
//    private String name;
//    private String directoryMatchPattern; // whenCreateCatalogRef
//    private String catalogTitleSubstitutionPattern;
//    private String catalogFilenameSubstitutionPattern;
//    private boolean expand = true;
//    private boolean flattenCatalog = false;

    //Pattern p = Pattern.compile("/(eta_[0-9]*)/$"); // doesn't match
    Pattern p = Pattern.compile(".*/(eta_[0-9]*)/$"); // matches
    Matcher m = p.matcher("fred/the/eta_211/");
    //boolean b = m.matches();
    if ( m.matches())
    {
      System.out.println( "Matches" );
      System.out.println( "numGroups : " + m.groupCount());
      for ( int i = 0; i <= m.groupCount(); i++)
      {
        System.out.println( "Group[" + i + "]: " + m.group( i) );
      }
    }
    else
      System.out.println( "Doesn't match" );

//    me = new CatalogRefExpander( "test catRefExpander", "/(eta_[0-9][0-9][0-9])/$",
//                                 "The $1 directory", "$1/catalog.xml", false, false);
//    assertTrue( me != null );
//
//    // Test a directory.
//    InvDataset ds1 = new InvDatasetImpl( null, "data/eta_211/");
//
//    assertTrue( "The dataset " + ds1.getName() + " did not match CatRefExpander <" + me.getLabel() + "--" + me.getDirectoryMatchPattern() + ">.",
//                me.makeCatalogRef( ds1));
//    System.out.println( "Dataset   : " + ds1.getName() );
//    System.out.println( "  Title   : " + me.catalogRefTitle() );
//    System.out.println( "  Filename: " + me.catalogRefFilename() );
//
//    // Test another directory.
//    InvDataset ds2 = new InvDatasetImpl( null, "data/eta_222/");
//
//    assertTrue( "The dataset " + ds2.getName() + " did not match CatRefExpander <" + me.getLabel() + "--" + me.getDirectoryMatchPattern() + ">.",
//                me.makeCatalogRef( ds2));
//    System.out.println( "Dataset   : " + ds2.getName() );
//    System.out.println( "  Title   : " + me.catalogRefTitle() );
//    System.out.println( "  Filename: " + me.catalogRefFilename() );
//
//    // Test a non-matching directory.
//    InvDataset ds3 = new InvDatasetImpl( null, "data/eta_22a/");
//
//    assertTrue( "The dataset " + ds3.getName() + " did not match CatRefExpander <" + me.getLabel() + "--" + me.getDirectoryMatchPattern() + ">.",
//                me.makeCatalogRef( ds3));
//    System.out.println( "Dataset   : " + ds3.getName() );
//    System.out.println( "  Title   : " + me.catalogRefTitle() );
//    System.out.println( "  Filename: " + me.catalogRefFilename() );
  }
}

/*
 * $Log: TestCatalogRefExpander.java,v $
 * Revision 1.4  2006/01/20 02:08:25  caron
 * switch to using slf4j for logging facade
 *
 * Revision 1.3  2005/11/18 23:51:05  edavis
 * More work on CrawlableDataset refactor of CatGen.
 *
 * Revision 1.2  2005/07/14 20:01:26  edavis
 * Make ID generation mandatory for datasetScan generated catalogs.
 * Also, remove log4j from some tests.
 *
 * Revision 1.1  2005/03/30 05:41:18  edavis
 * Simplify build process: 1) combine all build scripts into one,
 * thredds/build.xml; 2) combine contents of all resources/ directories into
 * one, thredds/resources; 3) move all test source code and test data into
 * thredds/test/src and thredds/test/data; and 3) move all schemas (.xsd and .dtd)
 * into thredds/resources/resources/thredds/schemas.
 *
 * Revision 1.1  2004/12/14 22:47:22  edavis
 * Add simple interface to thredds.cataloggen and continue adding catalogRef capabilities.
 *
 */