package thredds.tds.idd;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.units.DateFormatter;

import java.io.IOException;
import java.util.*;
import java.text.ParseException;

import static org.junit.Assert.*;

import thredds.catalog.*;

/**
 * Compare variable names in
 *
 * @author edavis
 * @since 4.0
 */
@RunWith(Parameterized.class)
public class TestMotherlodeGribVarNames
{
  private String modelId;

  public TestMotherlodeGribVarNames( String modelId )
  {
    super();
    this.modelId = modelId;
  }

  @Parameterized.Parameters
  public static Collection<Object[]> getModelIds()
  {
    return Arrays.asList( TestIddModels.getModelIds());
  }

  private final String server = "http://motherlode.ucar.edu:8080";

  private final String fmrcPrefix = "/thredds/catalog/fmrc/";
  private final String fmrcSuffix = "/runs/catalog.xml";

  private final String scanPrefix = "/thredds/catalog/model/";
  private final String scanCatalogSuffix = "/catalog.xml";

  /**
   * For the given model ID, get the third dataset in the "FMRC Run" catalog
   * and compare it to the dataset with matching run time from the IDV
   * datasetScan catalog.
   */
  @Test
  public void compareThirdFmrcRunDsAndMatchingScanDsVariableNames()
  {
    compareFmrcRunDsAndMatchingScanDsVariableNames( this.modelId, 2);
  }

  /**
   * For the given model ID, get the second dataset in the "FMRC Run" catalog
   * and compare it to the dataset with matching run time from the IDV
   * datasetScan catalog.
   */
  @Test
  public void compareSecondFmrcRunDsAndMatchingScanDsVariableNames()
  {
    compareFmrcRunDsAndMatchingScanDsVariableNames( this.modelId, 1);
  }

  /**
   * For the given model ID, get the dataset at the given index in the
   * "FMRC Run" catalog and compare it to the dataset with matching
   * run time from the IDV datasetScan catalog.
   *
   * @param modelId the ID of the target FMRC model to compare.
   * @param index the index of the target dataset in the "FMRC Run" catalog.
   */
  private void compareFmrcRunDsAndMatchingScanDsVariableNames( String modelId, int index )
  {
    // 1) Get the dataset at <index> in "FMRC Run" catalog.
    GridDataset fmrcRunGridDs  = getFmrcRunDataset( modelId, index );

    // 2) Extract the date of FMRC run from the dataset URL.
    Date runDate = parseTimestampInFmrcRunDatasetUrl( fmrcRunGridDs );

    // 3) Locate the dataset with matching run date in IDV datasetScan.
    GridDataset scanGridDs = getScanDatasetMatchingTime( modelId, runDate);

    // 4) Compare the matching datasets.
    StringBuilder diffLog = new StringBuilder();
    boolean isEqual = GridDatasetTestUtils.equalityOfGridDatasetsByGridNameAndShape( scanGridDs, fmrcRunGridDs, diffLog );

    assertTrue( diffLog.toString(), isEqual );
  }

  /**
   * Return the dataset at the given index location in the "FMRC Run" catalog
   * for the model identified by the modelId String.
   *
   * @param modelId the model ID for the target model FMRC.
   * @param index the target index location in the "FMRC Run" catalog.
   * @return the target "FMRC Run" dataset.
   */
  private GridDataset getFmrcRunDataset( String modelId, int index )
  {
    // Construct URL for the given model's FMRC Run catalog.
    String fmrcRunCatUrl = this.server + this.fmrcPrefix + modelId + this.fmrcSuffix;

    // Read the "FMRC Run" catalog
    InvCatalogImpl cat = InvCatalogFactory.getDefaultFactory( false ).readXML( fmrcRunCatUrl );
    assertFalse( "\"FMRC Run\" catalog [" + fmrcRunCatUrl + "] had a fatal error.",
                cat.hasFatalError());

    // Make sure the "FMRC Run" catalog has a top dataset and get its children.
    List<InvDataset> datasets = cat.getDatasets();
    assertEquals( "\"FMRC Run\" catalog [" + fmrcRunCatUrl + "] does not have a top dataset.",
                  datasets.size(), 1);
    datasets = datasets.get(0).getDatasets();

    // Make sure the "FMRC Run" collection has enough children to satisfy request.
    assertFalse( "\"FMRC Run\" top dataset [" + fmrcRunCatUrl + "] - requested child index [" + index + "] out of range [0-" + (datasets.size()-1) + "].",
                  datasets.size() < index + 1 );

    // Get the requested child dataset and make sure it exists, has no children, and is OPeNDAP accessible.
    InvDataset requestedChildDs = datasets.get( index );
    assertNotNull( "\"FMRC Run\" child [" + index + "] dataset [" + fmrcRunCatUrl + "] is null.",
                   requestedChildDs);
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
      fail( "\"Latest\" dataset [" + requestedChildDs.getFullName() + "] failed to open:"
            + "\n*****" + e.getMessage()
            + "\n*****" + errlog.toString() );
    }
    assertNotNull( "\"Latest\" dataset [" + requestedChildDs.getFullName() + "] failed to open: " + errlog.toString(),
                   dataset );
    assertTrue( "\"Latest\" dataset [" + requestedChildDs.getFullName() + "] not a gridded dataset: " + errlog.toString(),
                dataset instanceof GridDataset );

    return (GridDataset) dataset;
  }

  /**
   * Return the date indicated by the timestamp in the URL of the given
   * "FMRC Run" dataset.
   *
   * <p>The dataset URL has the following form:</p>
   * <pre>".../&lt;model&gt;_RUN_&lt;yyyy-MM-dd'T'HH:mm:ss&gt;Z"</pre>
   *
   * @param fmrcRunGridDs the "FMRC Run" dataset.
   * @return the run date/time of the given "FMRC Run" dataset.
   */
  private Date parseTimestampInFmrcRunDatasetUrl( GridDataset fmrcRunGridDs )
  {
    // Parse the timestamp from the URL of a "FMRC Run" dataset
    // (".../<model>_RUN_<yyyy-MM-dd'T'HH:mm:ss>Z").
    String url = fmrcRunGridDs.getLocationURI();
    if ( ! url.endsWith( "Z" ) )
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
   * Return the dataset from the IDV model datasetScan for the given model ID
   * with a run time/date matching the given run date.
   *
   * @param modelId the ID of the target FMRC model.
   * @param runDate the Date of the target run time/date.
   * @return the desired grid dataset.
   */
  private GridDataset getScanDatasetMatchingTime( String modelId, Date runDate)
  {
    // Construct URL for the given model's "Scan" catalog.
    String scanCatalogUrl = this.server + this.scanPrefix + modelId + this.scanCatalogSuffix;

    // Read the "Scan" catalog.
    InvCatalogImpl cat = InvCatalogFactory.getDefaultFactory( false ).readXML( scanCatalogUrl );
    assertFalse( "\"Scan\" catalog [" + scanCatalogUrl + "] had a fatal error.", cat.hasFatalError() );

    // Make sure the "Scan" catalog contains one top dataset and that it has children.
    List<InvDataset> datasets = cat.getDatasets();
    assertEquals( "\"Scan\" catalog [" + scanCatalogUrl + "] contains more than one dataset.",
                  datasets.size(), 1 );
    InvDataset topDs = datasets.get( 0);
    assertTrue( "\"Scan\" top dataset [" + topDs.getFullName() + "] does not have child datasets.",
                topDs.hasNestedDatasets());

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
      if ( ! curDsUrl.matches( ".*\\.grib[12]" ))
        continue;
      curDsUrl = curDsUrl.substring( 0, curDsUrl.lastIndexOf( "." ));
      if ( ! curDsUrl.endsWith( timeStamp ))
        continue;

      // Found the desired dataset.
      ds = curDs;
      dsUrl = access.getStandardUrlName();
      break;
    }

    System.out.println( "\"Scan\" matching [" + timeStamp + "] dataset: " + dsUrl );

    // Open the dataset as a gridded dataset.
    Formatter errlog = new Formatter();
    FeatureDataset gridDs = null;
    try
    {
      gridDs = FeatureDatasetFactoryManager.open( FeatureType.GRID, dsUrl, null, errlog );
    }
    catch ( IOException e )
    {
      fail( "\"Latest\" dataset [" + ds.getFullName() + "][" + dsUrl + "] failed to open:"
            + "\n*****" + e.getMessage()
            + "\n*****" + errlog.toString() );
    }
    assertNotNull( "\"Latest\" dataset [" + ds.getFullName() + "][" + dsUrl + "] failed to open: " + errlog.toString(),
                   gridDs );
    assertTrue( "\"Latest\" dataset [" + ds.getFullName() + "][" + dsUrl + "] not a gridded dataset: " + errlog.toString(),
                gridDs instanceof GridDataset );

    return (GridDataset) gridDs;

  }
}
