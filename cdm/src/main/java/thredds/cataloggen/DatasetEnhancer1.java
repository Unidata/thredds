// $Id$
package thredds.cataloggen;

import thredds.catalog.InvCatalogRef;
import thredds.catalog.InvDataset;
import thredds.catalog.InvDatasetImpl;
import thredds.catalog.InvDatasetScan;
import thredds.datatype.DateRange;
import thredds.datatype.DateType;
import thredds.datatype.TimeDuration;

import java.text.ParseException;

/**
 * A description
 *
 * @author edavis
 * @since Apr 20, 2005 17:02:08 PM
 */
public class DatasetEnhancer1
{
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DatasetEnhancer1.class);

  private DatasetMetadataAdder mdataAdder;
  private boolean applyToLeafNodesOnly;

  /** Constructor */
  private DatasetEnhancer1( DatasetMetadataAdder metadataAdder, boolean applyToLeafNodesOnly )
  {
    if ( metadataAdder == null ) throw new IllegalArgumentException( "MetadataAdder must not be null.");
    this.mdataAdder = metadataAdder;
    this.applyToLeafNodesOnly = applyToLeafNodesOnly;
  }

  public static DatasetEnhancer1 createDatasetEnhancer( DatasetMetadataAdder metadataAdder, boolean applyToLeafNodesOnly )
  {
    return( new DatasetEnhancer1( metadataAdder, applyToLeafNodesOnly) );
  }

  public static DatasetEnhancer1 createAddTimeCoverageEnhancer( String dsNameMatchPattern, String startTimeSubstitutionPattern, String duration)
  {
    return( new DatasetEnhancer1( new AddTimeCoverageModels( dsNameMatchPattern, startTimeSubstitutionPattern, duration), true ) );
  }

  public static DatasetEnhancer1 createAddIdEnhancer( String baseID )
  {
    return( new DatasetEnhancer1( new AddId( baseID ), false ) );
  }

  public void addMetadata( InvDataset dataset )
  {
    boolean doEnhance;
    boolean doEnhanceChildren;

    // For catalogRef and datasetScan, don't enhance children and enhance self if appropriate.
    if ( dataset.getClass().equals( InvCatalogRef.class )
         || dataset.getClass().equals( InvDatasetScan.class ) )
    {
      doEnhanceChildren = false;
      doEnhance = ! this.applyToLeafNodesOnly;
    }
    // For collection datasets, enhance children and enhance self if appropriate.
    else if ( dataset.hasNestedDatasets() )
    {
      doEnhanceChildren = true;
      doEnhance = ! this.applyToLeafNodesOnly;
    }
    // For atomic datasets, no children to enhance and enhance self.
    else
    {
      doEnhanceChildren = false;
      doEnhance = true;
    }

    // Enhance this dataset.
    if ( doEnhance )
    {
      if ( ! this.mdataAdder.addMetadata( dataset ) )
      {
        logger.debug( "addMetadata(): failed to enhance dataset <{}>.", dataset.getName());
      }
    }

    // Enhance children datasets.
    if ( doEnhanceChildren )
    {
      for ( java.util.Iterator dsIter = dataset.getDatasets().iterator(); dsIter.hasNext(); )
      {
        this.addMetadata( (InvDataset) dsIter.next() );
      }
    }
  }

//  public void addMetadataToNestedCollection( InvDataset collectionDataset)
//  {
//    // Skip catalogRef, datasetScan, and non-collection datasets.
//    if ( collectionDataset.getClass().equals( InvCatalogRef.class )
//         || collectionDataset.getClass().equals( InvDatasetScan.class )
//         || ! collectionDataset.hasNestedDatasets() )
//    {
//      return;
//    }
//
//    // Add metadata to grand children datasets.
//    for ( java.util.Iterator dsIter = collectionDataset.getDatasets().iterator(); dsIter.hasNext(); )
//    {
//      InvDataset curDs = (InvDataset) dsIter.next();
//      // Do not dereference catalogRef or datasetScan.
//      if ( !curDs.getClass().equals( InvCatalogRef.class )
//           && !curDs.getClass().equals( InvDatasetScan.class )
//           && curDs.hasNestedDatasets() )
//      {
//        this.addMetadataToNestedCollection( curDs );
//      }
//    }
//
//    // Add metadata child datasets.
//    log.debug( "addMetadataToNestedCollection(): enhance the datasets contained by dataset (" + collectionDataset.getName() + ")" );
//    java.util.List children = collectionDataset.getDatasets();
//    int numEnhanced = this.addMetadata( children );
//    log.debug( "addMetadataToNestedCollection(): for \"" + collectionDataset.getFullName() + "\", added metadata to " + numEnhanced + " of " + children.size() + "children datasets.");
//
//    return;
//  }
//
//  public int addMetadata( List datasets )
//  {
//    int numAdded = 0;
//    for ( java.util.Iterator dsIter = datasets.iterator(); dsIter.hasNext(); )
//    {
//      InvDataset curDs = (InvDataset) dsIter.next();
//      if ( this.mdataAdder.addMetadata( curDs) )
//        numAdded++;
//    }
//
//    return( numAdded);
//  }

  public interface DatasetMetadataAdder
  {
    /**
     * Attempt to add metadata to the given dataset and return true if successful.
     * Return false if this DatasetMetadataAdder does not apply to the given
     * dataset or if otherwise unsuccessful.
     *
     * @param dataset the dataset to enhance with metadata.
     * @return True if metadata added, otherwise false.
     */
    public boolean addMetadata( InvDataset dataset);
  }

  protected static class AddTimeCoverageModels implements DatasetMetadataAdder
  {
    private String substitutionPattern;
    private String duration;

    private java.util.regex.Pattern pattern;

    public AddTimeCoverageModels( String matchPattern, String substitutionPattern, String duration)
    {
      this.substitutionPattern = substitutionPattern;
      this.duration = duration;

      this.pattern = java.util.regex.Pattern.compile( matchPattern );
    }

    public boolean addMetadata( InvDataset dataset)
    {
      // <dataset name="2005061512_NAM.wmo" urlPath="2005061512_NAM.wmo"/>
      java.util.regex.Matcher matcher = this.pattern.matcher( dataset.getName() );
      if ( ! matcher.find())
        return( false); // Pattern not found.
      StringBuffer startTime = new StringBuffer();
      matcher.appendReplacement( startTime, this.substitutionPattern );
      startTime.delete( 0, matcher.start() );

      try
      {
        ((InvDatasetImpl) dataset).setTimeCoverage(
                new DateRange( new DateType( startTime.toString(), null, null), null,
                               new TimeDuration( this.duration ), null ) );
      }
      catch ( ParseException e )
      {
        logger.debug( "Start time <" + startTime.toString() + "> or duration <" + this.duration + "> not parsable: " + e.getMessage());
        return( false);
      }
      ( (InvDatasetImpl) dataset ).finish();

      return ( true );
    }
  }

  protected static class AddId implements DatasetMetadataAdder
  {
    private String baseId;

    public AddId( String baseId )
    {
      if ( baseId == null ) throw new IllegalArgumentException( "Base Id must not be null.");
      this.baseId = baseId;
    }

    public boolean addMetadata( InvDataset dataset )
    {
      InvDataset parentDs = dataset.getParent();
      String curId = ( parentDs == null) ? this.baseId : parentDs.getID();
      if ( curId == null) curId = this.baseId;
      if ( dataset.getName() != null && ! dataset.getName().equals( "") )
        curId += "/" + dataset.getName();

      ( (InvDatasetImpl) dataset).setID( curId );

      return ( true );
    }
  }
}
