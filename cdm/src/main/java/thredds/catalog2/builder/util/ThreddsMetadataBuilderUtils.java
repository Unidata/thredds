package thredds.catalog2.builder.util;

import thredds.catalog2.builder.ThreddsMetadataBuilder;
import thredds.catalog2.builder.ThreddsBuilderFactory;
import thredds.catalog.DataFormatType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;

/**
 * Utility methods for copying and merging <code>ThreddsMetadataBuilder</code>s.
 *
 * @author edavis
 * @since 4.0
 */
public class ThreddsMetadataBuilderUtils
{
  private ThreddsMetadataBuilderUtils() {}

  public static ThreddsMetadataBuilder copyIntoNewThreddsMetadataBuilder( ThreddsMetadataBuilder source,
                                                                          ThreddsBuilderFactory builderFactory )
  {
    if ( source == null )
      throw new IllegalArgumentException( "Source builder may not be null." );
    if ( builderFactory == null )
      throw new IllegalArgumentException( "Builder factory may not be null.");

    ThreddsMetadataBuilder result = builderFactory.newThreddsMetadataBuilder();
    copyThreddsMetadataBuilder( source, result );
    return result;
  }

  public static ThreddsMetadataBuilder copyThreddsMetadataBuilder( ThreddsMetadataBuilder source,
                                                                   ThreddsMetadataBuilder recipient )
  {
    if ( source == null )
      throw new IllegalArgumentException( "Source builder may not be null." );
    if ( recipient == null )
      throw new IllegalArgumentException( "Recipient builder may not be null.");

    // Set non-builder content.
    if ( source.getCollectionType() != null )
      recipient.setCollectionType( source.getCollectionType() );
    if ( source.getDataFormat() != null )
      recipient.setDataFormat( source.getDataFormat() );
    recipient.setDataSizeInBytes( source.getDataSizeInBytes());
    if ( source.getDataType() != null )
      recipient.setDataType( source.getDataType() );
    if ( source.getDateAvailable() != null )
      recipient.setDateAvailable( source.getDateAvailable() );
    if ( source.getDateCreated() != null )
      recipient.setDateCreated( source.getDateCreated() );
    if ( source.getDateIssued() != null )
      recipient.setDateIssued( source.getDateIssued() );
    if ( source.getDateMetadataCreated() != null )
      recipient.setDateMetadataCreated( source.getDateMetadataCreated() );
    if ( source.getDateMetadataModified() != null )
      recipient.setDateMetadataModified( source.getDateMetadataModified() );
    if ( source.getDateModified() != null )
      recipient.setDateModified( source.getDateModified() );
    if ( source.getDateValid() != null )
      recipient.setDateValid( source.getDateValid() );
    if ( source.getProjectTitle() != null )
      recipient.setProjectTitle( source.getProjectTitle() );

    ThreddsMetadataBuilder.GeospatialCoverageBuilder geoCovBuilder = source.getGeospatialCoverageBuilder();
    if ( geoCovBuilder != null )
      if ( geoCovBuilder.getCRS() != null )
        recipient.setNewGeospatialCoverageBuilder( geoCovBuilder.getCRS() );

    if ( source.getTemporalCoverage() != null )
      recipient.setTemporalCoverage( source.getTemporalCoverage() );

    // Add all Builder content.
    addCopiesOfContributorBuilders( source, recipient );
    addCopiesOfCreatorBuilders( source, recipient );
    addCopiesOfDocumentationBuilders( source, recipient );
    addCopiesOfKeyphraseBuilders( source, recipient );
    addCopiesOfPublisherBuilders( source, recipient );

    addCopiesOfVariableBuilders( source, recipient );
                       
    return recipient;
  }

  public static ThreddsMetadataBuilder mergeTwoThreddsMetadata( ThreddsMetadataBuilder first,
                                                                ThreddsMetadataBuilder second,
                                                                ThreddsBuilderFactory builderFactory )
  {
    ThreddsMetadataBuilder mergedResults = builderFactory.newThreddsMetadataBuilder();
    mergeTwoThreddsMetadata( first, second, mergedResults );
    return mergedResults;
  }

  public static void mergeTwoThreddsMetadata( ThreddsMetadataBuilder first,
                                              ThreddsMetadataBuilder second,
                                              ThreddsMetadataBuilder mergedResults )
  {
    mergeOverwriteCollectionType( first, second, mergedResults );
    mergeOverwriteDataFormat( first, second, mergedResults );
    mergeOverwriteDataSizeInBytes( first, second, mergedResults );
    mergeOverwriteDataType( first, second, mergedResults );
    mergeOverwriteDateAvailable( first, second, mergedResults );
    mergeOverwriteDateCreated( first, second, mergedResults );
    mergeOverwriteDateIssued( first, second, mergedResults );
    mergeOverwriteDateMetadataCreated( first, second, mergedResults );
    mergeOverwriteDateMetadataModified( first, second, mergedResults );
    mergeOverwriteDateModified( first, second, mergedResults );
    mergeOverwriteDateValid( first, second, mergedResults );
    mergeOverwriteProjectTitle( first, second, mergedResults );

    mergeOverwriteGeospatialCoverage( first, second, mergedResults );
    mergeOverwriteTemporalCoverage( first, second, mergedResults );

    // Add all Builder content.
    addCopiesOfContributorBuilders( first, mergedResults );
    addCopiesOfContributorBuilders( second, mergedResults );

    addCopiesOfCreatorBuilders( first, mergedResults );
    addCopiesOfCreatorBuilders( second, mergedResults );

    addCopiesOfDocumentationBuilders( first, mergedResults );
    addCopiesOfDocumentationBuilders( second, mergedResults );

    addCopiesOfKeyphraseBuilders( first, mergedResults );
    addCopiesOfKeyphraseBuilders( second, mergedResults );

    addCopiesOfPublisherBuilders( first, mergedResults );
    addCopiesOfPublisherBuilders( second, mergedResults );

    addCopiesOfVariableBuilders( first, mergedResults );
    addCopiesOfVariableBuilders( second, mergedResults );

  }

  private static void addCopiesOfDocumentationBuilders( ThreddsMetadataBuilder source,
                                                ThreddsMetadataBuilder result )
  {
    for ( ThreddsMetadataBuilder.DocumentationBuilder curDoc : source.getDocumentationBuilders() )
    {
      if ( curDoc.isContainedContent())
        result.addDocumentation( curDoc.getDocType(), curDoc.getContent() );
      else
        result.addDocumentation( curDoc.getDocType(), curDoc.getTitle(), curDoc.getExternalReference() );
    }
  }

  private static void addCopiesOfCreatorBuilders( ThreddsMetadataBuilder source,
                                           ThreddsMetadataBuilder result )
  {
    for ( ThreddsMetadataBuilder.ContributorBuilder curSourceCreator : source.getCreatorBuilder() )
    {
      ThreddsMetadataBuilder.ContributorBuilder curResultCreator = result.addCreator();
      copySingleContributorBuilder( curSourceCreator, curResultCreator );
    }
  }

  private static void addCopiesOfContributorBuilders( ThreddsMetadataBuilder source,
                                               ThreddsMetadataBuilder result )
  {
    for ( ThreddsMetadataBuilder.ContributorBuilder curSourceContrib : source.getContributorBuilder() )
    {
      ThreddsMetadataBuilder.ContributorBuilder curResultContrib = result.addContributor();
      copySingleContributorBuilder( curSourceContrib, curResultContrib );
    }
  }

  private static void addCopiesOfPublisherBuilders( ThreddsMetadataBuilder source,
                                             ThreddsMetadataBuilder result )
  {
    for ( ThreddsMetadataBuilder.ContributorBuilder curSourcePublisher : source.getPublisherBuilder() )
    {
      ThreddsMetadataBuilder.ContributorBuilder curResultPublisher = result.addPublisher();
      copySingleContributorBuilder( curSourcePublisher, curResultPublisher );
    }
  }

  private static void addCopiesOfKeyphraseBuilders( ThreddsMetadataBuilder source, ThreddsMetadataBuilder result )
  {
    for ( ThreddsMetadataBuilder.KeyphraseBuilder curKeyphrase : source.getKeyphraseBuilder() )
      result.addKeyphrase( curKeyphrase.getAuthority(), curKeyphrase.getPhrase() );
  }

  private static void addCopiesOfVariableBuilders( ThreddsMetadataBuilder source, ThreddsMetadataBuilder result )
  {
    for ( ThreddsMetadataBuilder.VariableBuilder curSourceVarBuilder : source.getVariableBuilders() )
    {
      ThreddsMetadataBuilder.VariableBuilder curResultVarBuilder = result.addVariableBuilder();
      if ( curResultVarBuilder.getAuthority() != null )
        curSourceVarBuilder.setAuthority( curResultVarBuilder.getAuthority() );
      if ( curResultVarBuilder.getId() != null )
        curSourceVarBuilder.setId( curResultVarBuilder.getId() );
      if ( curResultVarBuilder.getTitle() != null )
        curSourceVarBuilder.setTitle( curResultVarBuilder.getTitle() );
      if ( curResultVarBuilder.getDescription() != null )
        curSourceVarBuilder.setDescription( curResultVarBuilder.getDescription() );
      if ( curResultVarBuilder.getUnits() != null )
        curSourceVarBuilder.setUnits( curResultVarBuilder.getUnits() );
    }
  }

  private static void mergeOverwriteGeospatialCoverage( ThreddsMetadataBuilder first,
                                                        ThreddsMetadataBuilder second,
                                                        ThreddsMetadataBuilder mergedThreddsMetadata )
  {
    if ( !setGeospatialCoverateIfNotNull( second, mergedThreddsMetadata))
      setGeospatialCoverateIfNotNull( first, mergedThreddsMetadata);
  }
  
  private static void mergeOverwriteTemporalCoverage( ThreddsMetadataBuilder first,
                                                      ThreddsMetadataBuilder second,
                                                      ThreddsMetadataBuilder mergedThreddsMetadata )
  {
    DateRange temporalCov = second.getTemporalCoverage() != null
                            ? second.getTemporalCoverage() : first.getTemporalCoverage();
    if ( temporalCov != null )
      mergedThreddsMetadata.setTemporalCoverage( temporalCov );
  }

  private static void mergeOverwriteProjectTitle( ThreddsMetadataBuilder first,
                                                  ThreddsMetadataBuilder second,
                                                  ThreddsMetadataBuilder mergedThreddsMetadata )
  {
    String projectTitle = second.getProjectTitle() != null ? second.getProjectTitle() : first.getProjectTitle();
    if ( projectTitle != null )
      mergedThreddsMetadata.setProjectTitle( projectTitle );
  }

  private static void mergeOverwriteDateValid( ThreddsMetadataBuilder first,
                                               ThreddsMetadataBuilder second,
                                               ThreddsMetadataBuilder mergedThreddsMetadata )
  {
    DateRange dateValid = second.getDateValid() != null ? second.getDateValid() : first.getDateValid();
    if ( dateValid != null )
      mergedThreddsMetadata.setDateValid( dateValid );
  }

  private static void mergeOverwriteDateModified( ThreddsMetadataBuilder first,
                                                  ThreddsMetadataBuilder second,
                                                  ThreddsMetadataBuilder mergedThreddsMetadata )
  {
    DateType dateModified = second.getDateModified() != null
                            ? second.getDateModified() : first.getDateModified();
    if ( dateModified != null )
      mergedThreddsMetadata.setDateModified( dateModified );
  }

  private static void mergeOverwriteDateMetadataModified( ThreddsMetadataBuilder first,
                                                          ThreddsMetadataBuilder second,
                                                          ThreddsMetadataBuilder mergedThreddsMetadata )
  {
    DateType dateMetadataModified = second.getDateMetadataModified() != null
                                   ? second.getDateMetadataModified() : first.getDateMetadataModified();
    if ( dateMetadataModified != null )
      mergedThreddsMetadata.setDateMetadataModified( dateMetadataModified );
  }

  private static void mergeOverwriteDateMetadataCreated( ThreddsMetadataBuilder first,
                                                         ThreddsMetadataBuilder second,
                                                         ThreddsMetadataBuilder mergedThreddsMetadata )
  {
    DateType dateMetadataCreated = second.getDateMetadataCreated() != null
                                   ? second.getDateMetadataCreated() : first.getDateMetadataCreated();
    if ( dateMetadataCreated != null )
      mergedThreddsMetadata.setDateMetadataCreated( dateMetadataCreated );
  }

  private static void mergeOverwriteDateIssued( ThreddsMetadataBuilder first,
                                                ThreddsMetadataBuilder second,
                                                ThreddsMetadataBuilder mergedThreddsMetadata )
  {
    DateType dateIssued = second.getDateIssued() != null ? second.getDateIssued() : first.getDateIssued();
    if ( dateIssued != null )
      mergedThreddsMetadata.setDateIssued( dateIssued );
  }

  private static void mergeOverwriteDateCreated( ThreddsMetadataBuilder first,
                                                 ThreddsMetadataBuilder second,
                                                 ThreddsMetadataBuilder mergedThreddsMetadata )
  {
    DateType dateCreated = second.getDateCreated() != null ? second.getDateCreated() : first.getDateCreated();
    if ( dateCreated != null )
      mergedThreddsMetadata.setDateCreated( dateCreated );
  }

  private static void mergeOverwriteDateAvailable( ThreddsMetadataBuilder first,
                                                   ThreddsMetadataBuilder second,
                                                   ThreddsMetadataBuilder mergedThreddsMetadata )
  {
    DateRange dateAvailable = second.getDateAvailable() != null ? second.getDateAvailable() : first.getDateAvailable();
    if ( dateAvailable != null )
      mergedThreddsMetadata.setDateAvailable( dateAvailable );
  }

  private static void mergeOverwriteDataType( ThreddsMetadataBuilder first,
                                              ThreddsMetadataBuilder second,
                                              ThreddsMetadataBuilder mergedThreddsMetadata )
  {
    FeatureType dataType = second.getDataType() != null ? second.getDataType() : first.getDataType();
    if ( dataType != null )
      mergedThreddsMetadata.setDataType( dataType );
  }

  private static void mergeOverwriteDataSizeInBytes( ThreddsMetadataBuilder first,
                                                     ThreddsMetadataBuilder second,
                                                     ThreddsMetadataBuilder mergedThreddsMetadata )
  {
    long dataSizeInBytes = second.getDataSizeInBytes() != -1 ? second.getDataSizeInBytes() : first.getDataSizeInBytes();
    mergedThreddsMetadata.setDataSizeInBytes( dataSizeInBytes );
  }

  private static void mergeOverwriteDataFormat( ThreddsMetadataBuilder first,
                                         ThreddsMetadataBuilder second,
                                         ThreddsMetadataBuilder mergedThreddsMetadata )
  {
    DataFormatType dataFormat = second.getDataFormat() != null ? second.getDataFormat() : first.getDataFormat();
    if ( dataFormat != null )
      mergedThreddsMetadata.setDataFormat( dataFormat );
  }

  private static void mergeOverwriteCollectionType( ThreddsMetadataBuilder first,
                                             ThreddsMetadataBuilder second,
                                             ThreddsMetadataBuilder mergedThreddsMetadata )
  {
    String collectionType = second.getCollectionType() != null ? second.getCollectionType() : first.getCollectionType();
    if ( collectionType != null )
      mergedThreddsMetadata.setCollectionType( collectionType );
  }

  private static void copySingleContributorBuilder( ThreddsMetadataBuilder.ContributorBuilder source,
                                                    ThreddsMetadataBuilder.ContributorBuilder recipient )
  {
    if ( source.getAuthority() != null )
      recipient.setAuthority( source.getAuthority() );
    if ( source.getName() != null )
      recipient.setName( source.getName() );
    if ( source.getEmail() != null )
      recipient.setEmail( source.getEmail() );
    if ( source.getWebPage() != null )
      recipient.setWebPage( source.getWebPage() );
  }

  private static boolean setGeospatialCoverateIfNotNull( ThreddsMetadataBuilder source,
                                                         ThreddsMetadataBuilder recipient )
  {
    ThreddsMetadataBuilder.GeospatialCoverageBuilder geoCovBuilder = source.getGeospatialCoverageBuilder();
    if ( geoCovBuilder != null )
    {
      if ( geoCovBuilder.getCRS() != null )
      {
        recipient.setNewGeospatialCoverageBuilder( geoCovBuilder.getCRS() );
        return true;
      }
    }
    return false;
  }

}
