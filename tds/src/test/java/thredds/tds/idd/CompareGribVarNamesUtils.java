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

import ucar.nc2.dt.GridDataset;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.units.DateFormatter;

import java.util.Date;
import java.util.List;
import java.util.Formatter;
import java.io.IOException;
import java.text.ParseException;

import thredds.catalog.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;

/**
 * Utility class for comparing FMRC RUN datasets with FMRC Raw File datasets with matching run time.
 *
 * @author edavis
 * @since 4.0
 */
public class CompareGribVarNamesUtils
{
    private CompareGribVarNamesUtils() {}

    private final static String FMRC_PREFIX = "catalog/fmrc/";
    private final static String FMRC_RUN_CATALOG_SUFFIX = "/runs/catalog.xml";
    private final static String FMRC_RAW_FILE_CATALOG_SUFFIX = "/files/catalog.xml";

    /**
     * For the given model ID, get the dataset at the given index in the
     * "FMRC Run" catalog and compare it to the dataset with matching
     * run time in the "FMRC Raw File" catalog.
     *
     * @param tdsUrl the URL of the target TDS.
     * @param modelId the ID of the target FMRC model to compare.
     * @param index   the index of the target dataset in the "FMRC Run" catalog.
     */
    public static void assertEqualityOfFmrcRunDsAndMatchingFmrcRawFileDsVariableNames(String tdsUrl, String modelId, int index )
    {
        // 1) Get the dataset at <index> in "FMRC Run" catalog.
        GridDataset fmrcRunGridDs = assertFmrcRunDatasetIsAccessible( tdsUrl, modelId, index );

        // 2) Extract the date of FMRC run from the dataset URL.
        Date runDate = parseTimestampInFmrcRunDatasetUrl( fmrcRunGridDs );

        // 3) Locate the dataset with matching run date in IDV datasetScan.
        GridDataset scanGridDs = assertFmrcRawFileDatasetForMatchingTimeIsAccessible( tdsUrl, modelId, runDate );

        // 4) Compare the matching datasets.
        StringBuilder diffLog = new StringBuilder();
        boolean isEqual = GridDatasetTestUtils.equalityOfGridDatasetsByGridNameAndShape( scanGridDs, fmrcRunGridDs, diffLog );

        assertTrue( diffLog.toString(), isEqual );
    }

    /**
     * Return the dataset at the given index location in the "FMRC Run" catalog
     * for the model identified by the modelId String.
     *
     * @param tdsUrl the URL of the target TDS.
     * @param modelId the model ID for the target model FMRC.
     * @param index   the target index location in the "FMRC Run" catalog.
     * @return the target "FMRC Run" dataset.
     */
    private static GridDataset assertFmrcRunDatasetIsAccessible( String tdsUrl, String modelId, int index )
    {
        // Construct URL for the given model's FMRC Run catalog.
        String fmrcRunCatUrl = tdsUrl + FMRC_PREFIX + modelId + FMRC_RUN_CATALOG_SUFFIX;

        // Read the "FMRC Run" catalog
        InvCatalogImpl cat = InvCatalogFactory.getDefaultFactory( false ).readXML( fmrcRunCatUrl );
        assertFalse( "\"FMRC Run\" catalog [" + fmrcRunCatUrl + "] had a fatal error= "  + " " +cat.getLog(),
                     cat.hasFatalError() );

        // Make sure the "FMRC Run" catalog has a top dataset and get its children.
        List<InvDataset> datasets = cat.getDatasets();
        assertEquals( "\"FMRC Run\" catalog [" + fmrcRunCatUrl + "] does not have a top dataset.",
                      datasets.size(), 1 );
        datasets = datasets.get( 0 ).getDatasets();

        // Make sure the "FMRC Run" collection has enough children to satisfy request.
        assertFalse( "\"FMRC Run\" top dataset [" + fmrcRunCatUrl + "] - requested child index [" + index + "] out of range [0-" + ( datasets.size() - 1 ) + "].",
                     datasets.size() < index + 1 );

        // Get the requested child dataset and make sure it exists, has no children, and is OPeNDAP accessible.
        InvDataset requestedChildDs = datasets.get( index );
        assertNotNull( "\"FMRC Run\" child [" + index + "] dataset [" + fmrcRunCatUrl + "] is null.",
                       requestedChildDs );
        assertFalse( "\"FMRC Run\" child [" + index + "] dataset [" + requestedChildDs.getFullName() + "] has nested datasets.",
                     requestedChildDs.hasNestedDatasets() );
        InvAccess access = requestedChildDs.getAccess( ServiceType.OPENDAP );
        assertNotNull( "\"FMRC Run\" child [" + index + "] dataset [" + requestedChildDs.getFullName() + "] not OPeNDAP accessible.",
                       access );

        System.out.println( "FMRC Run child(" + index + ") dataset              : "
                            + access.getStandardUrlName() );

        // Open the dataset as a gridded dataset.
        Formatter errlog = new Formatter();
        FeatureDataset dataset = null;
        try
        {
            dataset = FeatureDatasetFactoryManager.open( FeatureType.GRID, access.getStandardUrlName(), null, errlog );
        }
        catch ( IOException e )
        {
            fail( "Dataset [" + requestedChildDs.getFullName() + "] failed to open:"
                  + "\n*****" + e.getMessage()
                  + "\n*****" + errlog.toString() );
        }
        assertNotNull( "Dataset [" + requestedChildDs.getFullName() + "] failed to open: " + errlog.toString(),
                       dataset );
        assertTrue( "Dataset [" + requestedChildDs.getFullName() + "] not a gridded dataset: " + errlog.toString(),
                    dataset instanceof GridDataset );

        return (GridDataset) dataset;
    }

    /**
     * Return the date indicated by the timestamp in the URL of the given
     * "FMRC Run" dataset.
     * <p/>
     * <p>The dataset URL has the following form:</p>
     * <pre>".../&lt;model&gt;_RUN_&lt;yyyy-MM-dd'T'HH:mm:ss&gt;Z"</pre>
     *
     * @param fmrcRunGridDs the "FMRC Run" dataset.
     * @return the run date/time of the given "FMRC Run" dataset.
     */
    private static Date parseTimestampInFmrcRunDatasetUrl( GridDataset fmrcRunGridDs )
    {
        // Parse the timestamp from the URL of a "FMRC Run" dataset
        // (".../<model>_RUN_<yyyy-MM-dd'T'HH:mm:ss>Z").
        String url = fmrcRunGridDs.getLocationURI();
        if ( !url.endsWith( "Z" ) )
            fail( "No \"Z\" at end of FMRC Run dataset URL [" + url + "]." );
        int start = url.lastIndexOf( "_RUN_" ) + 5;

        DateFormatter dateFormatter = new DateFormatter();
        Date runDate = null;
        try
        {
            runDate = dateFormatter.isoDateTimeFormat( url.substring( start, url.length() - 1 ) );
        }
        catch ( ParseException e )
        {
            fail( "\"FMRC Run\" dataset URL [" + url + "] did not contain timestamp in expected format: " + e.getMessage() );
        }
        return runDate;
    }

    /**
     * Return the dataset from the "FMRC Raw File" catalog for the given model ID
     * with a run time/date matching the given run date.
     *
     * @param tdsUrl the URL of the target TDS.
     * @param modelId the ID of the target FMRC model.
     * @param runDate the Date of the target run time/date.
     * @return the desired grid dataset.
     */
    private static GridDataset assertFmrcRawFileDatasetForMatchingTimeIsAccessible( String tdsUrl, String modelId, Date runDate )
    {
        // Construct URL for the given model's "Scan" catalog.
        String scanCatalogUrl = tdsUrl + FMRC_PREFIX + modelId + FMRC_RAW_FILE_CATALOG_SUFFIX;

        // Read the "Scan" catalog.
        InvCatalogImpl cat = InvCatalogFactory.getDefaultFactory( false ).readXML( scanCatalogUrl );
        assertFalse( "\"FMRC Raw File\" catalog [" + scanCatalogUrl + "] had a fatal error.", cat.hasFatalError() );

        // Make sure the "Scan" catalog contains one top dataset and that it has children.
        List<InvDataset> datasets = cat.getDatasets();
        assertEquals( "\"FMRC Raw File\" catalog [" + scanCatalogUrl + "] contains more than one dataset.",
                      datasets.size(), 1 );
        InvDataset topDs = datasets.get( 0 );
        assertTrue( "\"FMRC Raw File\" top dataset [" + topDs.getFullName() + "] does not have child datasets.",
                    topDs.hasNestedDatasets() );

        // Determine timestamp String to look for in dataset URL
        // (".../<model>_<yyyyMMdd_HHmm>.grib[12]").
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat( "yyyyMMdd_HHmm" );
        sdf.setTimeZone( java.util.TimeZone.getTimeZone( "GMT" ) );
        String timeStamp = sdf.format( runDate );

        InvDataset ds = null;
        String dsUrl = null;
        // Find the desired dataset.
        for ( InvDataset curDs : topDs.getDatasets() )
        {
            // Get OPeNDAP URL for the current dataset.
            InvAccess access = curDs.getAccess( ServiceType.OPENDAP );
            if ( access == null )
                continue;
            String curDsUrl = access.getStandardUrlName();

            // Does URL have correct format and contain desired timestamp string.
            if ( !curDsUrl.matches( ".*\\.grib[12]" ) )
                continue;
            curDsUrl = curDsUrl.substring( 0, curDsUrl.lastIndexOf( "." ) );
            if ( !curDsUrl.endsWith( timeStamp ) )
                continue;

            // Found the desired dataset.
            ds = curDs;
            dsUrl = access.getStandardUrlName();
            break;
        }

        System.out.println( "\"FMRC Raw File\" matching [" + timeStamp + "] dataset: " + dsUrl );

        // Open the dataset as a gridded dataset.
        Formatter errlog = new Formatter();
        FeatureDataset gridDs = null;
        try
        {
            gridDs = FeatureDatasetFactoryManager.open( FeatureType.GRID, dsUrl, null, errlog );
        }
        catch ( IOException e )
        {
            fail( "Matching dataset [" + ds.getFullName() + "][" + dsUrl + "] failed to open:"
                  + "\n*****" + e.getMessage()
                  + "\n*****" + errlog.toString() );
        }
        assertNotNull( "Matching dataset [" + ds.getFullName() + "][" + dsUrl + "] failed to open: " + errlog.toString(),
                       gridDs );
        assertTrue( "Matching dataset [" + ds.getFullName() + "][" + dsUrl + "] not a gridded dataset: " + errlog.toString(),
                    gridDs instanceof GridDataset );

        return (GridDataset) gridDs;

    }

}
