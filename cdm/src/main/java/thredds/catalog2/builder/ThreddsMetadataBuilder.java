package thredds.catalog2.builder;

import java.net.URI;
import java.util.List;

import ucar.nc2.units.DateType;
import ucar.nc2.units.DateRange;
import ucar.nc2.constants.FeatureType;
import thredds.catalog2.ThreddsMetadata;
import thredds.catalog.DataFormatType;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface ThreddsMetadataBuilder extends ThreddsBuilder
{
  public DocumentationBuilder addDocumentation( String docType, String title, URI externalReference );
  public DocumentationBuilder addDocumentation( String docType, String content );
  public boolean removeDocumentation( DocumentationBuilder docBuilder );
  public List<DocumentationBuilder> getDocumentationBuilders();

  public KeyphraseBuilder addKeyphrase( String authority, String phrase );
  public boolean removeKeyphrase( KeyphraseBuilder keyphraseBuilder );
  public List<KeyphraseBuilder> getKeyphraseBuilder();

  public ContributorBuilder addCreator();
  public boolean removeCreator( ContributorBuilder creatorBuilder );
  public List<ContributorBuilder> getCreatorBuilder();

  public ContributorBuilder addContributor();
  public boolean removeContributor( ContributorBuilder contributorBuilder );
  public List<ContributorBuilder> getContributorBuilder();


  public ContributorBuilder addPublisher();
  public boolean removePublisher( ContributorBuilder PublisherBuilder );
  public List<ContributorBuilder> getPublisherBuilder();

  public void setProjectTitle( String projectTitle );
  public String getProjectTitle();

  public void setDateCreated( DateType dateCreated );
  public DateType getDateCreated();

  public void setDateModified( DateType dateModified );
  public DateType getDateModified();

  public void setDateIssued( DateType dateIssued );
  public DateType getDateIssued();

  public void setDateValid( DateRange dateValid );
  public DateRange getDateValid();

  public void setDateAvailable( DateRange dateAvailable );
  public DateRange getDateAvailable();

  public void setDateMetadataCreated( DateType dateMetadataCreated );
  public DateType getDateMetadataCreated();

  public void setDateMetadataModified( DateType dateMetadataModified );
  public DateType getDateMetadataModified();

  public GeospatialCoverageBuilder setNewGeospatialCoverageBuilder( URI crsUri );
  public void removeGeospatialCoverageBuilder();
  public GeospatialCoverageBuilder getGeospatialCoverageBuilder();

  public void setTemporalCoverage( DateRange temporalCoverage );
  public DateRange getTemporalCoverage();

  public VariableBuilder addVariableBuilder();
  public boolean removeVariableBuilder( VariableBuilder variableBuilder );
  public List<VariableBuilder> getVariableBuilders();

  public void setDataSizeInBytes( long dataSizeInBytes );
  public long getDataSizeInBytes();

  public void setDataFormat( DataFormatType dataFormat);
  public DataFormatType getDataFormat();

  public void setDataType( FeatureType dataType );
  public FeatureType getDataType();

  public void setCollectionType( String collectionType );
  public String getCollectionType();

  @Override
  ThreddsMetadata build() throws BuilderException;

  public interface DocumentationBuilder extends ThreddsBuilder
  {
    //public void setContainedContent( boolean containedContent );
    public boolean isContainedContent();

    //public void setDocType( String docType );
    public String getDocType();

    //public void setContent( String content );
    public String getContent();

    //public void setTitle( String title );
    public String getTitle();

    //public void setExternalReference( URI externalReference );
    public URI getExternalReference();

    public ThreddsMetadata.Documentation build() throws BuilderException;
  }

  public interface KeyphraseBuilder extends ThreddsBuilder
  {
    public void setAuthority( String authority );
    public String getAuthority();

    public void setPhrase( String phrase );
    public String getPhrase();

    public ThreddsMetadata.Keyphrase build() throws BuilderException;
  }

  public interface ContributorBuilder extends ThreddsBuilder
  {
    public String getAuthority();
    public void setAuthority( String authority );
    public String getName();
    public void setName( String name );
    public String getEmail();
    public void setEmail( String email );

    public URI getWebPage();
    public void setWebPage( URI webPage );

    public ThreddsMetadata.Contributor build() throws BuilderException;
  }

  public interface VariableBuilder extends ThreddsBuilder
  {
    public String getAuthority();
    public void setAuthority( String authority );

    public String getId();
    public void setId( String id);

    public String getTitle();
    public void setTitle( String title );

    public String getDescription();
    public void setDescription( String description );

    public String getUnits();
    public void setUnits( String units );

    public ThreddsMetadata.Variable build() throws BuilderException;
  }

  public interface GeospatialCoverageBuilder extends ThreddsBuilder
  {
    public void setCRS( URI crsUri );
    public URI getCRS();

    public void setGlobal( boolean isGlobal );
    public boolean isGlobal();

    public void setZPositiveUp( boolean isZPositiveUp );
    public boolean isZPositiveUp();

    public GeospatialRangeBuilder addExtentBuilder();
    public boolean removeExtentBuilder( GeospatialRangeBuilder geospatialRangeBuilder );
    public List<GeospatialRangeBuilder> getExtentBuilders();

    public ThreddsMetadata.GeospatialCoverage build() throws BuilderException;
  }

  public interface GeospatialRangeBuilder extends ThreddsBuilder
  {
    public void setHorizontal( boolean isHorizontal );
    public boolean isHorizontal();

    public void setStart( double start );
    public double getStart();

    public void setSize( double size );
    public double getSize();

    public void setResolution( double resolution );
    public double getResolution();

    public void setUnits( String units );
    public String getUnits();

    public ThreddsMetadata.GeospatialRange build() throws BuilderException;
  }
}
