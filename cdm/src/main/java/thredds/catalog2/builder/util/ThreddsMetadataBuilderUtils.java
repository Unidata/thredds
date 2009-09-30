package thredds.catalog2.builder.util;

import thredds.catalog2.builder.ThreddsMetadataBuilder;
import thredds.catalog2.builder.ThreddsBuilderFactory;
import thredds.catalog.DataFormatType;
import ucar.nc2.constants.FeatureType;

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
    if ( source.getAvailableDatePointBuilder() != null )
    {
        ThreddsMetadataBuilder.DatePointBuilder dpb = source.getAvailableDatePointBuilder();
        recipient.setAvailableDatePointBuilder( dpb.getDate(), dpb.getDateFormat() );
    }
    if ( source.getCreatedDatePointBuilder() != null )
    {
        ThreddsMetadataBuilder.DatePointBuilder dpb = source.getCreatedDatePointBuilder();
        recipient.setCreatedDatePointBuilder( dpb.getDate(), dpb.getDateFormat() );
    }
    if ( source.getIssuedDatePointBuilder() != null )
    {
        ThreddsMetadataBuilder.DatePointBuilder dpb = source.getIssuedDatePointBuilder();
        recipient.setIssuedDatePointBuilder( dpb.getDate(), dpb.getDateFormat() );
    }
    if ( source.getMetadataCreatedDatePointBuilder() != null )
    {
        ThreddsMetadataBuilder.DatePointBuilder dpb = source.getMetadataCreatedDatePointBuilder();
        recipient.setMetadataCreatedDatePointBuilder( dpb.getDate(), dpb.getDateFormat() );
    }
    if ( source.getMetadataModifiedDatePointBuilder() != null )
    {
        ThreddsMetadataBuilder.DatePointBuilder dpb = source.getMetadataModifiedDatePointBuilder();
        recipient.setMetadataModifiedDatePointBuilder( dpb.getDate(), dpb.getDateFormat() );
    }
    if ( source.getModifiedDatePointBuilder() != null )
    {
        ThreddsMetadataBuilder.DatePointBuilder dpb = source.getModifiedDatePointBuilder();
        recipient.setModifiedDatePointBuilder( dpb.getDate(), dpb.getDateFormat() );
    }
    if ( source.getValidDatePointBuilder() != null )
    {
        ThreddsMetadataBuilder.DatePointBuilder dpb = source.getValidDatePointBuilder();
        recipient.setValidDatePointBuilder( dpb.getDate(), dpb.getDateFormat() );
    }

    ThreddsMetadataBuilder.GeospatialCoverageBuilder geoCovBuilder = source.getGeospatialCoverageBuilder();
    if ( geoCovBuilder != null )
      if ( geoCovBuilder.getCRS() != null )
        recipient.setNewGeospatialCoverageBuilder( geoCovBuilder.getCRS() );

    if ( source.getTemporalCoverageBuilder() != null )
    {
        ThreddsMetadataBuilder.DateRangeBuilder drb = source.getTemporalCoverageBuilder();
        recipient.setTemporalCoverageBuilder( drb.getStartDate(), drb.getStartDateFormat(),
                                              drb.getEndDate(), drb.getEndDateFormat(),
                                              drb.getDuration(), drb.getResolution() );
    }

    // Add all Builder content.
    addCopiesOfContributorBuilders( source, recipient );
    addCopiesOfCreatorBuilders( source, recipient );
    addCopiesOfDocumentationBuilders( source, recipient );
    addCopiesOfKeyphraseBuilders( source, recipient );
    addCopiesOfProjectNameBuilders( source, recipient );
    addCopiesOfPublisherBuilders( source, recipient );

    addCopiesOfVariableGroupBuilders( source, recipient );
                       
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

    addCopiesOfProjectNameBuilders( first, mergedResults );
    addCopiesOfProjectNameBuilders( second, mergedResults );

    addCopiesOfPublisherBuilders( first, mergedResults );
    addCopiesOfPublisherBuilders( second, mergedResults );

    addCopiesOfVariableGroupBuilders( first, mergedResults );
    addCopiesOfVariableGroupBuilders( second, mergedResults );
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
    for ( ThreddsMetadataBuilder.KeyphraseBuilder curKeyphrase : source.getKeyphraseBuilders() )
      result.addKeyphrase( curKeyphrase.getAuthority(), curKeyphrase.getPhrase() );
  }

  private static void addCopiesOfProjectNameBuilders( ThreddsMetadataBuilder source, ThreddsMetadataBuilder result )
  {
    for ( ThreddsMetadataBuilder.ProjectNameBuilder curProjName : source.getProjectNameBuilders() )
      result.addProjectName( curProjName.getNamingAuthority(), curProjName.getName() );
  }

  private static void addCopiesOfVariableGroupBuilders( ThreddsMetadataBuilder source, ThreddsMetadataBuilder result )
  {
    for ( ThreddsMetadataBuilder.VariableGroupBuilder curSourceVarGroupBuilder : source.getVariableGroupBuilders() )
    {
      ThreddsMetadataBuilder.VariableGroupBuilder curResultVarGroupBuilder = result.addVariableGroupBuilder();

      if ( curSourceVarGroupBuilder.getVocabularyAuthorityId() != null )
        curResultVarGroupBuilder.setVocabularyAuthorityId( curSourceVarGroupBuilder.getVocabularyAuthorityId() );
      if ( curSourceVarGroupBuilder.getVocabularyAuthorityUrl() != null )
        curResultVarGroupBuilder.setVocabularyAuthorityUrl( curSourceVarGroupBuilder.getVocabularyAuthorityUrl() );

      if ( curSourceVarGroupBuilder.getVariableMapUrl() != null )
        curResultVarGroupBuilder.setVariableMapUrl( curSourceVarGroupBuilder.getVariableMapUrl() );

      if ( curSourceVarGroupBuilder.getVariableBuilders() != null )
        for ( ThreddsMetadataBuilder.VariableBuilder curSourceVarBuilder : curSourceVarGroupBuilder.getVariableBuilders() )
          curResultVarGroupBuilder.addVariableBuilder( curSourceVarBuilder.getName(),
                                                       curSourceVarBuilder.getDescription(),
                                                       curSourceVarBuilder.getUnits(),
                                                       curSourceVarBuilder.getVocabularyId(),
                                                       curSourceVarBuilder.getVocabularyName());
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
    ThreddsMetadataBuilder.DateRangeBuilder temporalCov
            = second.getTemporalCoverageBuilder() != null
              ? second.getTemporalCoverageBuilder() : first.getTemporalCoverageBuilder();
    if ( temporalCov != null )
      mergedThreddsMetadata.setTemporalCoverageBuilder( temporalCov.getStartDate(), temporalCov.getStartDateFormat(),
                                                        temporalCov.getEndDate(), temporalCov.getEndDateFormat(),
                                                        temporalCov.getDuration(), temporalCov.getResolution() );
  }

  private static void mergeOverwriteDateValid( ThreddsMetadataBuilder first,
                                               ThreddsMetadataBuilder second,
                                               ThreddsMetadataBuilder mergedThreddsMetadata )
  {
    ThreddsMetadataBuilder.DatePointBuilder date
            = second.getValidDatePointBuilder() != null
              ? second.getValidDatePointBuilder() : first.getValidDatePointBuilder();
    if ( date != null )
      mergedThreddsMetadata.setValidDatePointBuilder( date.getDate(), date.getDateFormat() );
  }

  private static void mergeOverwriteDateModified( ThreddsMetadataBuilder first,
                                                  ThreddsMetadataBuilder second,
                                                  ThreddsMetadataBuilder mergedThreddsMetadata )
  {
      ThreddsMetadataBuilder.DatePointBuilder date
              = second.getModifiedDatePointBuilder() != null
                ? second.getModifiedDatePointBuilder() : first.getModifiedDatePointBuilder();
      if ( date != null )
          mergedThreddsMetadata.setModifiedDatePointBuilder( date.getDate(), date.getDateFormat() );
  }

  private static void mergeOverwriteDateMetadataModified( ThreddsMetadataBuilder first,
                                                          ThreddsMetadataBuilder second,
                                                          ThreddsMetadataBuilder mergedThreddsMetadata )
  {
    ThreddsMetadataBuilder.DatePointBuilder date
            = second.getMetadataModifiedDatePointBuilder() != null
              ? second.getMetadataModifiedDatePointBuilder() : first.getMetadataModifiedDatePointBuilder();
    if ( date != null )
      mergedThreddsMetadata.setMetadataModifiedDatePointBuilder( date.getDate(), date.getDateFormat() );
  }

  private static void mergeOverwriteDateMetadataCreated( ThreddsMetadataBuilder first,
                                                         ThreddsMetadataBuilder second,
                                                         ThreddsMetadataBuilder mergedThreddsMetadata )
  {
    ThreddsMetadataBuilder.DatePointBuilder date
            = second.getMetadataCreatedDatePointBuilder() != null
              ? second.getMetadataCreatedDatePointBuilder() : first.getMetadataCreatedDatePointBuilder();
    if ( date != null )
      mergedThreddsMetadata.setMetadataCreatedDatePointBuilder( date.getDate(), date.getDateFormat() );
  }

  private static void mergeOverwriteDateIssued( ThreddsMetadataBuilder first,
                                                ThreddsMetadataBuilder second,
                                                ThreddsMetadataBuilder mergedThreddsMetadata )
  {
    ThreddsMetadataBuilder.DatePointBuilder date
            = second.getIssuedDatePointBuilder() != null
              ? second.getIssuedDatePointBuilder() : first.getIssuedDatePointBuilder();
    if ( date != null )
      mergedThreddsMetadata.setIssuedDatePointBuilder( date.getDate(), date.getDateFormat() );
  }

  private static void mergeOverwriteDateCreated( ThreddsMetadataBuilder first,
                                                 ThreddsMetadataBuilder second,
                                                 ThreddsMetadataBuilder mergedThreddsMetadata )
  {
    ThreddsMetadataBuilder.DatePointBuilder date
            = second.getCreatedDatePointBuilder() != null
              ? second.getCreatedDatePointBuilder() : first.getCreatedDatePointBuilder();
    if ( date != null )
      mergedThreddsMetadata.setCreatedDatePointBuilder( date.getDate(), date.getDateFormat() );
  }

  private static void mergeOverwriteDateAvailable( ThreddsMetadataBuilder first,
                                                   ThreddsMetadataBuilder second,
                                                   ThreddsMetadataBuilder mergedThreddsMetadata )
  {
    ThreddsMetadataBuilder.DatePointBuilder date
            = second.getAvailableDatePointBuilder() != null
              ? second.getAvailableDatePointBuilder() : first.getAvailableDatePointBuilder();
    if ( date != null )
      mergedThreddsMetadata.setAvailableDatePointBuilder( date.getDate(), date.getDateFormat() );
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
    if ( source.getNamingAuthority() != null )
      recipient.setNamingAuthority( source.getNamingAuthority() );
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
