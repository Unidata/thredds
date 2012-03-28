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
// $Id: TestCrawlableDatasetImpl.java 61 2006-07-12 21:36:00Z edavis $
package thredds.crawlabledataset;

import junit.framework.TestCase;

import java.util.*;
import java.io.IOException;

import thredds.cataloggen.SimpleCatalogBuilder;

/**
 * _more_
 *
 * @author edavis
 * @since Feb 10, 2006 8:13:59 PM
 */
public class TestCrawlableDatasetImpl extends TestCase
{

  public TestCrawlableDatasetImpl( String name){ super( name); }
  protected void setup() {}

  public void testSlashRoot()
  {
    doRootPathTesting( "/" );
  }

  public void testFooRoot()
  {
    doRootPathTesting( "foo/");
  }

  public void testFooBarRoot()
  {
    doRootPathTesting( "foo/bar/");
  }

  public void testSlashFooRoot()
  {
    doRootPathTesting( "/foo/");
  }

  public void testSlashFooBarRoot()
  {
    doRootPathTesting( "/foo/bar/");
  }

  public void doRootPathTesting( String rootPath)
  {
    System.out.println( "Test with root = \"" + rootPath + "\"" );
    setupTestCollection( rootPath);
    CrawlableDataset top = getInstance( rootPath);
    showCrDsAndDescendants( top );

    System.out.println( "Generate top catalog" );
    SimpleCatalogBuilder builder = new SimpleCatalogBuilder( "", top, "myservice", "OPENDAP", "/thredds/dodsC/");

    String catAsString;
    try
    {
      catAsString = builder.generateCatalogAsString( top);
    }
    catch ( IOException e )
    {
      System.out.println( "ERROR generating catalog: " + e.getMessage() );
      return;
    }
    System.out.println( catAsString );

    CrawlableDataset nc = getInstance( rootPath + "nc" );
    System.out.println( "Generate nc catalog" );
    builder = new SimpleCatalogBuilder( "", top, "myservice", "OPENDAP", "/thredds/dodsC/" );

    try
    {
      catAsString = builder.generateCatalogAsString( nc );
    }
    catch ( IOException e )
    {
      System.out.println( "ERROR generating catalog: " + e.getMessage() );
      return;
    }
    System.out.println( catAsString );

    CrawlableDataset ncTest = getInstance( rootPath + "nc/Test" );
    System.out.println( "Generate nc/Test catalog" );
    builder = new SimpleCatalogBuilder( "", top, "myservice", "OPENDAP", "/thredds/dodsC/" );

    try
    {
      catAsString = builder.generateCatalogAsString( ncTest );
    }
    catch ( IOException e )
    {
      System.out.println( "ERROR generating catalog: " + e.getMessage() );
      return;
    }
    System.out.println( catAsString );
  }

  private void showCrDsAndDescendants( CrawlableDataset crDs)
  {
    System.out.println( "path <" + crDs.getPath() + ">  name <" + crDs.getName() + ">");
    List childList;
    try
    {
      childList = crDs.listDatasets( );
    }
    catch ( IOException e )
    {
      System.out.println( "ERROR listing children: " + e.getMessage() );
      return;
    }
    for ( Iterator it = childList.iterator(); it.hasNext(); )
    {
      showCrDsAndDescendants( (CrawlableDataset) it.next() );
    }
  }

  private static java.util.HashMap hash = new java.util.HashMap( 20 );

  public static CrawlableDataset getInstance( String path )
  {
    return (CrawlableDataset) hash.get( path);
  }

  public static void setupTestCollection( String root)
  {
    if ( ! hash.isEmpty()) hash.clear();

    MyCrDs crDs1 = new MyCrDs( root + "", true, null, null);
    crDs1.addChild( new MyCrDs( root + "jojo.dat", false, crDs1, null));
    MyCrDs crDs2 = new MyCrDs( root + "nc", true, crDs1, null);
    crDs1.addChild( crDs2);
    crDs2.addChild( new MyCrDs( root + "nc/123.nc", false, crDs2, null));
    crDs2.addChild( new MyCrDs( root + "nc/fnoc1.nc", false, crDs2, null));
    crDs2.addChild( new MyCrDs( root + "nc/test1.nc", false, crDs2, null));
    MyCrDs crDs6 = new MyCrDs( root + "nc/Test", true, crDs2, null);
    crDs2.addChild( crDs6);
    crDs6.addChild( new MyCrDs( root + "nc/Test/test.nc", false, crDs6, null));
    crDs6.addChild( new MyCrDs( root + "nc/Test/testfile.nc", false, crDs6, null));

    MyCrDs crDs9 = new MyCrDs( root + "ascii", true, crDs1, null);
    crDs1.addChild( crDs9 );
    crDs9.addChild( new MyCrDs( root + "ascii/abc.txt", false, crDs9, null));
    crDs9.addChild( new MyCrDs( root + "ascii/123.txt", false, crDs9, null));
    MyCrDs crDs12 = new MyCrDs( root + "ascii/more", true, crDs9, null);
    crDs9.addChild( crDs12);
    crDs12.addChild( new MyCrDs( root + "ascii/more/junk.txt", false, crDs12, null));
    crDs12.addChild( new MyCrDs( root + "ascii/more/mojunk.txt", false, crDs12, null));

    /*
          "",
          "nc",
          "nc/123.nc",
          "nc/fnoc1.nc",
          "nc/test1.nc",
          "nc/Test",
          "nc/Test/test.nc",
          "nc/Test/testfile.nc",
          "ascii",
          "ascii/abc.txt",
          "ascii/123.txt",
          "ascii/more",
          "ascii/more/junk.txt",
          "ascii/more/mojunk.txt"

    */
  }

  /**
   *
   */
  private static class MyCrDs implements CrawlableDataset
  {
    private String path;
    private String name;
    private boolean isCollection;
    private MyCrDs parent;
    private List childList;

    private MyCrDs( String path, boolean isCollection, MyCrDs parent, List childList )
    {
      if ( ! path.equals( "/") && path.endsWith( "/"))
        this.path = path.substring( 0, path.length() - 1);
      else
        this.path = path;
      if ( this.path.equals( "/" ) )
        this.name = "";
      else
      {
        this.name = path.substring( path.lastIndexOf( "/" ) + 1 );
      }
      this.isCollection = isCollection;
      this.parent = parent;
      this.childList = childList != null ? childList : new ArrayList();

      hash.put( path, this );
    }

    private void addChild( MyCrDs child )
    {
      this.childList.add( child );
    }

    public Object getConfigObject()
    {
      return null;
    }

    public String getPath()
    {
      return path;
    }

    public String getName()
    {
      return name;
    }

    public CrawlableDataset getParentDataset()
    {
      return parent;
    }

    public boolean exists()
    {
      return true;
    }

    public boolean isCollection()
    {
      return isCollection;
    }

    public CrawlableDataset getDescendant( String relativePath )
    {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List listDatasets() throws IOException
    {
      return new ArrayList(childList);
    }

    public List listDatasets( CrawlableDatasetFilter filter ) throws IOException
    {
      List list = this.listDatasets();
      if ( filter == null ) return list;
      List retList = new ArrayList();
      for ( Iterator it = list.iterator(); it.hasNext(); )
      {
        CrawlableDataset curDs = (CrawlableDataset) it.next();
        if ( filter.accept( curDs ) )
        {
          retList.add( curDs );
        }
      }
      return ( retList );
    }

    public long length()
    {
      return -1;
    }

    public Date lastModified() // or long milliseconds?
    {
      return null;
    }
  }
}
/*
 * $Log: TestCrawlableDatasetImpl.java,v $
 * Revision 1.1  2006/03/15 21:50:05  edavis
 * Add a new test.
 *
 */