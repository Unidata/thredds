/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

package thredds.tds.idd;

import ucar.nc2.thredds.ThreddsDataFactory;
import ucar.nc2.dataset.NetcdfDataset;

import java.util.*;
import java.io.*;

import thredds.catalog.crawl.CatalogCrawler;
import thredds.catalog.InvDataset;
import thredds.catalog.InvCatalogRef;
import thredds.catalog.InvAccess;

import static org.junit.Assert.*;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogDatasetTestUtils
{
    private CatalogDatasetTestUtils() {}

    private static ThreddsDataFactory threddsDataFactory = new ThreddsDataFactory();


    public static List<String> getRandomAccessibleDatasetUrlFromEachCollectionInCatalogListTree(
            List<String> catalogUrls, StringBuilder log, boolean verbose )
    {
        List<String> datasetUrls = new ArrayList<String>();
        for ( String currentCatalogUrl : catalogUrls )
        {
            List<String> datasetList = CatalogDatasetTestUtils
                    .getRandomAccessibleDatasetUrlFromEachCollectionInCatalogTree( currentCatalogUrl, log, verbose );
            datasetUrls.addAll( datasetList );
        }

        return datasetUrls;
    }

    public static List<String> getRandomAccessibleDatasetUrlFromEachCollectionInCatalogTree(
            String catalogUrl, StringBuilder log, boolean verbose )
    {
        if ( verbose ) System.out.println( "Crawling catalog: " + catalogUrl );

        ByteArrayOutputStream statusMsg = new ByteArrayOutputStream();
        PrintStream psStatusMsg = new PrintStream( statusMsg );

        final List<String> accessibleDatasetUrls = new ArrayList<String>();

        CatalogCrawler.Listener listener = new CatalogCrawler.Listener()
        {
            public void getDataset( InvDataset ds, Object context ) {
                InvAccess bestAccess = threddsDataFactory.chooseDatasetAccess( ds.getAccess() );
                accessibleDatasetUrls.add( bestAccess.getStandardUrlName() );
            }

            public boolean getCatalogRef( InvCatalogRef dd, Object context ) {
                return true;
            }
        };
        CatalogCrawler crawler = new CatalogCrawler( CatalogCrawler.USE_RANDOM_DIRECT_NOT_FIRST_OR_LAST, false, listener );

        int numDs = crawler.crawl( catalogUrl, null, psStatusMsg, null );
        psStatusMsg.close();

        StringBuilder message = new StringBuilder()
                .append( "Crawled catalog [" ).append( catalogUrl )
                .append( "] and found " ).append( numDs ).append( " random accessible dataset URLs." );

        if ( verbose ) System.out.println( message );

        log.append( log.length() > 0 ? "\n\n" : "" )
                .append( message).append( "\n")
                .append( statusMsg.toString() );

        return accessibleDatasetUrls;
    }

    public static void assertDatasetIsAccessible( String datasetUrl )
    {
        StringBuilder message = new StringBuilder();
        NetcdfDataset ncDataset = openDataset( datasetUrl, message);

        assertNotNull( "Failed to open dataset: " + message.toString(), ncDataset);
    }

    private static NetcdfDataset openDataset( String datasetUrl, StringBuilder message )
    {
        try
        {
          // ToDo - Handle URLs for "resolver" datasets.
            return NetcdfDataset.openDataset( datasetUrl, false, null );
        }
        catch ( IOException e )
        {
            if ( message != null )
                message.append( "I/O error opening dataset [").append( datasetUrl).append( "]: ")
                        .append( getStackTraceAsString( e ));
            return null;
        }
        catch ( Exception e )
        {
            if ( message != null )
                message.append( "Exception opening dataset [" ).append( datasetUrl ).append( "]:\n" )
                        .append( getStackTraceAsString( e ));
            return null;
        }
    }

    private static String getStackTraceAsString( Exception e )
    {
        ByteArrayOutputStream stackTrace = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter( stackTrace );
        e.printStackTrace( pw );
        pw.close();
        return stackTrace.toString();
    }
}
