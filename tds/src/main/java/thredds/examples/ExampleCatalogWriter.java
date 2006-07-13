// $Id: ExampleCatalogWriter.java 51 2006-07-12 17:13:13Z caron $
package thredds.examples;

import thredds.catalog.*;

import java.io.IOException;

/**
 * An example of how to write a simple THREDDS InvCatalog document.
 */
public class ExampleCatalogWriter
{
  public static void main( String[] args )
  {
    InvCatalogFactory catFactory = new InvCatalogFactory( "default", true );

    InvCatalogImpl catalog = new InvCatalogImpl( "My Catalog", "1.0", null);

    InvService myService = new InvService( "myServer", ServiceType.DODS.toString(), "http://my.server/cgi-bin/nph-dods", null, null);
    catalog.addService( myService);

    InvDatasetImpl curDs = null;

    for ( int i = 0; i < 5; i++ )
    {
      curDs = new InvDatasetImpl( null, "dataset " + i, null, "myServer", "data/datafile"+i+".nc");
      catalog.addDataset( curDs);
    }

    InvDatasetImpl sDs = new InvDatasetImpl( null, "My Special Datasets");
    catalog.addDataset( sDs);

    for ( int i = 0; i < 3; i++ )
    {
      curDs = new InvDatasetImpl( null, "Special Dataset " + i, null, "myServer", "data/sdatafile" + i + ".nc" );
      sDs.addDataset( curDs );
    }

    // Tie up any loose ends in catalog with finish().
    ((InvCatalogImpl) catalog).finish();

    // Write the catalog to standard output.
    try
    {
      catFactory.writeXML( (InvCatalogImpl) catalog, System.out );
    }
    catch ( IOException e )
    {
      System.err.println( "IOException while writing catalog: " + e.getMessage() );
    }
  }
}

/*
 * $Log: ExampleCatalogWriter.java,v $
 * Revision 1.1  2004/08/23 16:50:34  edavis
 * Update DqcServlet to work with DQC spec v0.3 and InvCatalog v1.0. Folded DqcServlet into the THREDDS server framework/build/distribution. Updated documentation (DqcServlet and THREDDS server).
 *
 */