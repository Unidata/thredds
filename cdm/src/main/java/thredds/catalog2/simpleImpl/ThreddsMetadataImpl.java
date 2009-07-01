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
package thredds.catalog2.simpleImpl;

import thredds.catalog2.ThreddsMetadata;
import thredds.catalog2.builder.ThreddsMetadataBuilder;
import thredds.catalog2.builder.BuilderIssue;
import thredds.catalog2.builder.BuilderException;
import thredds.catalog.DataFormatType;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.net.URI;
import java.net.URISyntaxException;

import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import ucar.nc2.constants.FeatureType;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class ThreddsMetadataImpl
        implements ThreddsMetadata, ThreddsMetadataBuilder
{
  private boolean isBuilt;

  private List<DocumentationImpl> docs;
  private List<KeyphraseImpl> keyphrases;
  private List<ContributorImpl> creators;
  private List<ContributorImpl> contributors;
  private List<ContributorImpl> publishers;

  private String projectTitle;
  private DateType dateCreated;
  private DateType dateModified;
  private DateType dateIssued;
  private DateRange dateValid;
  private DateRange dateAvailable;
  private DateType dateMetadataCreated;
  private DateType dateMetadataModified;

  private GeospatialCoverageImpl geospatialCoverage;
  private DateRange temporalCoverage;

  private List<VariableImpl> variables;
  private long dataSizeInBytes;
  private DataFormatType dataFormat;
  private FeatureType dataType;
  private String collectionType;

  public ThreddsMetadataImpl()
  {
    this.isBuilt = false;
    this.dataSizeInBytes = -1;
  }

  public boolean isEmpty()
  {
    if ( this.docs != null && ! this.docs.isEmpty() )
      return false;
    if ( this.keyphrases != null && ! this.keyphrases.isEmpty() )
      return false;
    if ( this.creators != null && ! this.creators.isEmpty() )
      return false;
    if ( this.contributors != null && ! this.contributors.isEmpty() )
      return false;
    if ( this.publishers != null && ! this.publishers.isEmpty() )
      return false;

    if ( this.projectTitle != null || this.dateCreated != null || this.dateModified != null
         || this.dateIssued != null || this.dateValid != null || this.dateAvailable != null
         || this.dateMetadataCreated != null || this.dateMetadataModified != null
         || this.geospatialCoverage != null || this.temporalCoverage != null )
      return false;

    if ( this.variables != null && ! this.variables.isEmpty() )
      return false;

    if ( this.dataSizeInBytes != -1 || this.dataFormat != null || this.dataType != null || this.collectionType != null )
      return false;

    return true;
  }

  public DocumentationBuilder addDocumentation( String docType, String title, URI externalReference )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built.");
    if ( title == null ) throw new IllegalArgumentException( "Title may not be null.");
    if ( externalReference == null ) throw new IllegalArgumentException( "External reference may not be null.");
    if ( this.docs == null )
      this.docs = new ArrayList<DocumentationImpl>();
    DocumentationImpl doc = new DocumentationImpl( docType, title, externalReference );
    this.docs.add( doc );
    return doc;
  }

  public DocumentationBuilder addDocumentation( String docType, String content )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    if ( content == null ) throw new IllegalArgumentException( "Content may not be null." );
    if ( this.docs == null )
      this.docs = new ArrayList<DocumentationImpl>();
    DocumentationImpl doc = new DocumentationImpl( docType, content );
    this.docs.add( doc );
    return doc;
  }

  public boolean removeDocumentation( DocumentationBuilder docBuilder )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    if ( docBuilder == null )
      return false;
    if ( this.docs == null )
      return false;
    return this.docs.remove( (DocumentationImpl) docBuilder );
  }

  public List<DocumentationBuilder> getDocumentationBuilders()
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    if ( this.docs == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<DocumentationBuilder>( this.docs) );
  }

  public List<Documentation> getDocumentation()
  {
    if ( ! this.isBuilt )
      throw new IllegalStateException( "Sorry, I've escaped from my Builder before being built." );
    if ( this.docs == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<Documentation>( this.docs ) );
  }

  public KeyphraseBuilder addKeyphrase( String authority, String phrase )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    if ( phrase == null )
      throw new IllegalArgumentException( "Phrase may not be null.");
    if ( this.keyphrases == null )
      this.keyphrases = new ArrayList<KeyphraseImpl>();
    KeyphraseImpl keyphrase = new KeyphraseImpl();
    keyphrase.setAuthority( authority );
    keyphrase.setPhrase( phrase );
    this.keyphrases.add( keyphrase );
    return keyphrase;
  }

  public boolean removeKeyphrase( KeyphraseBuilder keyphraseBuilder )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    if ( keyphraseBuilder == null )
      return false;
    if ( this.keyphrases == null )
      return false;
    return this.keyphrases.remove( (KeyphraseImpl) keyphraseBuilder );
  }

  public List<KeyphraseBuilder> getKeyphraseBuilder()
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    if ( this.keyphrases == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<KeyphraseBuilder>( this.keyphrases ) );
  }

  public List<Keyphrase> getKeyphrases()
  {
    if ( ! this.isBuilt )
      throw new IllegalStateException( "Sorry, I've escaped from my Builder before being built." );
    if ( this.keyphrases == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<Keyphrase>( this.keyphrases ) );
  }

  public ContributorBuilder addCreator()
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    if ( this.creators == null )
      this.creators = new ArrayList<ContributorImpl>();
    ContributorImpl contributor = new ContributorImpl();
    this.creators.add( contributor );
    return contributor;
  }

  public boolean removeCreator( ContributorBuilder creatorBuilder )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    if ( creatorBuilder == null )
      return false;
    if ( this.creators == null )
      return false;
    return this.creators.remove( (ContributorImpl) creatorBuilder );
  }

  public List<ContributorBuilder> getCreatorBuilder()
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    if ( this.creators == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<ContributorBuilder>( this.creators ) );
  }

  public List<Contributor> getCreator()
  {
    if ( ! this.isBuilt )
      throw new IllegalStateException( "Sorry, I've escaped from my Builder before being built." );
    if ( this.creators == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<Contributor>( this.creators ) );
  }

  public ContributorBuilder addContributor()
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    if ( this.contributors == null )
      this.contributors = new ArrayList<ContributorImpl>();
    ContributorImpl contributor = new ContributorImpl();
    this.contributors.add( contributor );
    return contributor;
  }

  public boolean removeContributor( ContributorBuilder contributorBuilder )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    if ( contributorBuilder == null )
      return false;
    if ( this.contributors == null )
      return false;
    return this.contributors.remove( (ContributorImpl) contributorBuilder );
  }

  public List<ContributorBuilder> getContributorBuilder()
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    if ( this.contributors == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<ContributorBuilder>( this.contributors ) );
  }

  public List<Contributor> getContributor()
  {
    if ( ! this.isBuilt )
      throw new IllegalStateException( "Sorry, I've escaped from my Builder before being built." );
    if ( this.contributors == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<Contributor>( this.contributors ) );
  }

  public ContributorBuilder addPublisher()
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    if ( this.publishers == null )
      this.publishers = new ArrayList<ContributorImpl>();
    ContributorImpl contributor = new ContributorImpl();
    this.publishers.add( contributor );
    return contributor;
  }

  public boolean removePublisher( ContributorBuilder publisherBuilder )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    if ( publisherBuilder == null )
      return false;
    if ( this.publishers == null )
      return false;
    return this.publishers.remove( (ContributorImpl) publisherBuilder );
  }

  public List<ContributorBuilder> getPublisherBuilder()
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    if ( this.publishers == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<ContributorBuilder>( this.publishers ) );
  }

  public List<Contributor> getPublisher()
  {
    if ( ! this.isBuilt )
      throw new IllegalStateException( "Sorry, I've escaped from my Builder before being built." );
    if ( this.publishers == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<Contributor>( this.publishers ) );
  }

  public void setProjectTitle( String projectTitle )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    this.projectTitle = projectTitle;
  }

  public String getProjectTitle()
  {
    return this.projectTitle;
  }

  public void setDateCreated( DateType dateCreated )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    this.dateCreated = dateCreated;
  }

  public DateType getDateCreated()
  {
    return this.dateCreated;
  }

  public void setDateModified( DateType dateModified )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    this.dateModified = dateModified;
  }

  public DateType getDateModified()
  {
    return this.dateModified;
  }

  public void setDateIssued( DateType dateIssued )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    this.dateIssued = dateIssued;
  }

  public DateType getDateIssued()
  {
    return this.dateIssued;
  }

  public void setDateValid( DateRange dateValid )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    this.dateValid = dateValid;
  }

  public DateRange getDateValid()
  {
    return this.dateValid;
  }

  public void setDateAvailable( DateRange dateAvailable )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    this.dateAvailable = dateAvailable;
  }

  public DateRange getDateAvailable()
  {
    return this.dateAvailable;
  }

  public void setDateMetadataCreated( DateType dateMetadataCreated )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    this.dateMetadataCreated = dateMetadataCreated;
  }

  public DateType getDateMetadataCreated()
  {
    return this.dateMetadataCreated;
  }

  public void setDateMetadataModified( DateType dateMetadataModified )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    this.dateMetadataModified = dateMetadataModified;
  }

  public DateType getDateMetadataModified()
  {
    return this.dateMetadataModified;
  }

  public GeospatialCoverageBuilder setNewGeospatialCoverageBuilder( URI crsUri )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    GeospatialCoverageImpl gci = new GeospatialCoverageImpl();
    gci.setCRS( crsUri );
    this.geospatialCoverage = gci;
    return null;
  }

  public void removeGeospatialCoverageBuilder()
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    this.geospatialCoverage = null;
  }

  public GeospatialCoverageBuilder getGeospatialCoverageBuilder()
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    return this.geospatialCoverage;
  }

  public GeospatialCoverage getGeospatialCoverage()
  {
    if ( ! this.isBuilt )
      throw new IllegalStateException( "Sorry, I've escaped from my Builder before being built." );
    return this.geospatialCoverage;
  }

  public void setTemporalCoverage( DateRange temporalCoverage )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    this.temporalCoverage = temporalCoverage;
  }

  public DateRange getTemporalCoverage()
  {
    return this.temporalCoverage;
  }

  public VariableBuilder addVariableBuilder()
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    if ( this.variables == null )
      this.variables = new ArrayList<VariableImpl>();
    VariableImpl var = new VariableImpl();
    this.variables.add( var);
    return var;
  }

  public boolean removeVariableBuilder( VariableBuilder variableBuilder )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    if ( variableBuilder == null )
      return false;
    if ( this.variables == null )
      return false;
    return this.variables.remove( (VariableImpl) variableBuilder );
  }

  public List<VariableBuilder> getVariableBuilders()
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    if ( this.variables == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<VariableBuilder>( this.variables ) );
  }

  public List<Variable> getVariables()
  {
    if ( ! this.isBuilt )
      throw new IllegalStateException( "Sorry, I've escaped from my Builder before being built." );
    if ( this.variables == null )
      return Collections.emptyList();
    return Collections.unmodifiableList( new ArrayList<Variable>( this.variables ) );
  }

  public void setDataSizeInBytes( long dataSizeInBytes )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    this.dataSizeInBytes = dataSizeInBytes;
  }

  public long getDataSizeInBytes()
  {
    return this.dataSizeInBytes;
  }

  public void setDataFormat( DataFormatType dataFormat )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    this.dataFormat = dataFormat;
  }
  public void setDataFormat( String dataFormat )
  {
    this.setDataFormat( DataFormatType.getType( dataFormat));
  }

  public DataFormatType getDataFormat()
  {
    return this.dataFormat;
  }

  public void setDataType( FeatureType dataType )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    this.dataType = dataType;
  }

  public FeatureType getDataType()
  {
    return this.dataType;
  }

  public void setCollectionType( String collectionType )
  {
    if ( this.isBuilt )
      throw new IllegalStateException( "This Builder has been built." );
    this.collectionType = collectionType;
  }

  public String getCollectionType() // ?????
  {
    return this.collectionType;
  }

  public boolean isBuilt()
  {
    return this.isBuilt;
  }

  public boolean isBuildable( List<BuilderIssue> issues )
  {
    if ( this.isBuilt )
      return true;

    List<BuilderIssue> localIssues = new ArrayList<BuilderIssue>();

    // Check subordinates.
    if ( this.docs != null )
      for ( DocumentationImpl doc : this.docs )
        doc.isBuildable( localIssues );
    // ToDo keywords
    // ToDo ...


    if ( localIssues.isEmpty() )
      return true;

    issues.addAll( localIssues );
    return false;
  }

  public ThreddsMetadata build() throws BuilderException
  {
    if ( this.isBuilt )
      return this;

    List<BuilderIssue> issues = new ArrayList<BuilderIssue>();
    if ( ! isBuildable( issues ) )
      throw new BuilderException( issues );

    // Check subordinates.
    if ( this.docs != null )
      for ( DocumentationImpl doc : this.docs )
        doc.build();
    // ToDo keywords
    // ToDo ...

    this.isBuilt = true;
    return this;
  }

  public static class DocumentationImpl
          implements Documentation, DocumentationBuilder
  {
    private final boolean isContainedContent;

    public final String docType;
    public final String title;
    public final URI externalReference;
    public final String content;

    public DocumentationImpl( String docType, String title, URI externalReference )
    {
      if ( title == null ) throw new IllegalArgumentException( "Title may not be null.");
      if ( externalReference == null ) throw new IllegalArgumentException( "External reference may not be null.");
      this.isContainedContent = false;
      this.docType = docType;
      this.title = title;
      this.externalReference = externalReference;
      this.content = null;
    }

    public DocumentationImpl( String docType, String content )
    {
      if ( content == null ) throw new IllegalArgumentException( "Content may not be null." );
      this.isContainedContent = true;
      this.docType = docType;
      this.title = null;
      this.externalReference = null;
      this.content = content;
    }

    public boolean isContainedContent()
    {
      return this.isContainedContent;
    }

    public String getDocType()
    {
      return this.docType;
    }

    public String getContent()
    {
      if ( ! this.isContainedContent )
        throw new IllegalStateException( "No contained content, use externally reference to access documentation content." );
      return this.content;
    }

    public String getTitle()
    {
      if ( this.isContainedContent )
        throw new IllegalStateException( "Documentation with contained content has no title." );
      return this.title;
    }

    public URI getExternalReference()
    {
      if ( this.isContainedContent )
        throw new IllegalStateException( "Documentation with contained content has no external reference.");
      return this.externalReference;
    }

    public boolean isBuilt()
    {
      return true;
    }

    public boolean isBuildable( List<BuilderIssue> issues )
    {
      return true;
    }

    public Documentation build() throws BuilderException
    {
      return this;
    }
  }

  public static class KeyphraseImpl
          implements Keyphrase, KeyphraseBuilder
  {
    private boolean isBuilt;
    private String authority;
    private String phrase;

    public void setAuthority( String authority )
    {
    }

    public String getAuthority()
    {
      return null;
    }

    public void setPhrase( String phrase )
    {
    }

    public String getPhrase()
    {
      return null;
    }

    public boolean isBuilt()
    {
      return this.isBuilt;
    }

    public boolean isBuildable( List<BuilderIssue> issues )
    {
      return false;
    }

    public Keyphrase build() throws BuilderException
    {
      return null;
    }
  }

  public static class ContributorImpl
          implements Contributor, ContributorBuilder
  {
    private boolean isBuilt;

    private String authority;
    private String name;
    private String email;
    private URI webPage;

    public String getAuthority()
    {
      return this.authority;
    }

    public void setAuthority( String authority )
    {
      if ( this.isBuilt )
        throw new IllegalStateException( "This Builder has been built." );
      this.authority = authority;
    }

    public String getName()
    {
      return this.name;
    }

    public void setName( String name )
    {
      if ( this.isBuilt )
        throw new IllegalStateException( "This Builder has been built." );
      if ( name == null )
        throw new IllegalArgumentException( "Name may not be null.");
      this.name = name;
    }

    public String getEmail()
    {
      return this.email;
    }

    public void setEmail( String email )
    {
      if ( this.isBuilt )
        throw new IllegalStateException( "This Builder has been built." );
      this.email = email;
    }

    public URI getWebPage()
    {
      return this.webPage;
    }

    public void setWebPage( URI webPage )
    {
      if ( this.isBuilt )
        throw new IllegalStateException( "This Builder has been built." );
      this.webPage = webPage;
    }

    public boolean isBuilt()
    {
      return this.isBuilt;
    }

    public boolean isBuildable( List<BuilderIssue> issues )
    {
      return true;
    }

    public Contributor build() throws BuilderException
    {
      this.isBuilt = true;
      return this;
    }
  }

  public static class VariableImpl
          implements Variable, VariableBuilder
  {
    private boolean isBuilt;

    private String authority = "";
    private String id;
    private String title = "";
    private String description = "";
    private String units = "";

    public VariableImpl() {}

    public String getAuthority()
    {
      return this.authority;
    }

    public void setAuthority( String authority )
    {
      if ( this.isBuilt )
        throw new IllegalStateException( "This Builder has been built." );
      this.authority = authority == null ? "" : authority;
    }

    public String getId()
    {
      return this.id;
    }

    public void setId( String id )
    {
      if ( this.isBuilt )
        throw new IllegalStateException( "This Builder has been built." );
      if ( id == null )
        throw new IllegalArgumentException( "Id may not be null.");
      this.id = id;
    }

    public String getTitle()
    {
      return this.title;
    }

    public void setTitle( String title )
    {
      if ( this.isBuilt )
        throw new IllegalStateException( "This Builder has been built." );
      this.title = title == null ? "" : title;
    }

    public String getDescription()
    {
      return this.description;
    }

    public void setDescription( String description )
    {
      if ( this.isBuilt )
        throw new IllegalStateException( "This Builder has been built." );
      this.description = description == null ? "" : description;
    }

    public String getUnits()
    {
      return this.units;
    }

    public void setUnits( String units )
    {
      if ( this.isBuilt )
        throw new IllegalStateException( "This Builder has been built." );
      this.units = units == null ? "" : units;
    }

    public boolean isBuilt()
    {
      return this.isBuilt;
    }

    public boolean isBuildable( List<BuilderIssue> issues )
    {
      return true;
    }

    public Variable build() throws BuilderException
    {
      this.isBuilt = true;
      return this;
    }
  }

  public static class GeospatialCoverageImpl
          implements GeospatialCoverage,
                     GeospatialCoverageBuilder
  {
    private boolean isBuilt;

    private URI defaultCrsUri;

    private URI crsUri;
    private boolean is3D;
    private boolean isZPositiveUp;

    private boolean isGlobal;
    private List<GeospatialRangeImpl> extent;

    GeospatialCoverageImpl()
    {
      this.isBuilt = false;
      String defaultCrsUriString = "urn:x-mycrs:2D-WGS84-ellipsoid";
      try
      { this.defaultCrsUri = new URI( defaultCrsUriString ); }
      catch ( URISyntaxException e )
      { throw new IllegalStateException( "Bad URI syntax for default CRS URI ["+defaultCrsUriString+"]: " + e.getMessage()); }
      this.crsUri = this.defaultCrsUri;
    }

    public void setCRS( URI crsUri )
    {
      if ( this.isBuilt)
        throw new IllegalStateException( "This Builder has been built.");
      if ( crsUri == null )
        this.crsUri = this.defaultCrsUri;
      this.crsUri = crsUri;
    }

    public URI getCRS()
    {
      return this.crsUri;
    }

    public void setGlobal( boolean isGlobal )
    {
      if ( this.isBuilt )
        throw new IllegalStateException( "This Builder has been built." );
      this.isGlobal = isGlobal;
    }

    public boolean isGlobal()
    {
      return this.isGlobal;
    }

    public void setZPositiveUp( boolean isZPositiveUp )
    {
      if ( this.isBuilt )
        throw new IllegalStateException( "This Builder has been built." );
      this.isZPositiveUp = isZPositiveUp;
    }

    public boolean isZPositiveUp()   // Is this needed since have CRS?
    {
      return this.isZPositiveUp;
    }

    public GeospatialRangeBuilder addExtentBuilder()
    {
      if ( this.isBuilt )
        throw new IllegalStateException( "This Builder has been built." );
      if ( this.extent == null )
        this.extent = new ArrayList<GeospatialRangeImpl>();
      GeospatialRangeImpl gri = new GeospatialRangeImpl();
      this.extent.add( gri );
      return gri;
    }

    public boolean removeExtentBuilder( GeospatialRangeBuilder geospatialRangeBuilder )
    {
      if ( this.isBuilt )
        throw new IllegalStateException( "This Builder has been built." );
      if ( geospatialRangeBuilder == null )
        return true;
      if ( this.extent == null )
        return false;
      return this.extent.remove( (GeospatialRangeImpl) geospatialRangeBuilder );
    }

    public List<GeospatialRangeBuilder> getExtentBuilders()
    {
      if ( this.isBuilt )
        throw new IllegalStateException( "This Builder has been built." );
      if ( this.extent == null )
        return Collections.emptyList();
      return Collections.unmodifiableList( new ArrayList<GeospatialRangeBuilder>( this.extent) );
    }

    public List<GeospatialRange> getExtent()
    {
      if ( ! this.isBuilt )
        throw new IllegalStateException( "Sorry, I've escaped from my Builder before being built." );
      if ( this.extent == null )
        return Collections.emptyList();
      return Collections.unmodifiableList( new ArrayList<GeospatialRange>( this.extent ) );
    }

    public boolean isBuilt()
    {
      return this.isBuilt;
    }

    public boolean isBuildable( List<BuilderIssue> issues )
    {
      return false;
    }

    public GeospatialCoverage build() throws BuilderException
    {
      return this;
    }
  }

  public static class GeospatialRangeImpl
          implements GeospatialRange,
                     GeospatialRangeBuilder
  {
    private boolean isBuilt;

    private boolean isHorizontal;
    private double start;
    private double size;
    private double resolution;
    private String units;

    public GeospatialRangeImpl()
    {
      this.isBuilt = false;
      
      this.isHorizontal = false;
      this.start = 0.0;
      this.size = 0.0;
      this.resolution = 0.0;
      this.units = "";
    }

    public void setHorizontal( boolean isHorizontal )
    {
      if ( this.isBuilt ) throw new IllegalStateException( "This Builder has been built.");
      this.isHorizontal = isHorizontal;
    }

    public boolean isHorizontal()
    {
      return this.isHorizontal;
    }

    public void setStart( double start )
    {
      if ( this.isBuilt ) throw new IllegalStateException( "This Builder has been built." );
      this.start = start;
    }

    public double getStart()
    {
      return this.start;
    }

    public void setSize( double size )
    {
      if ( this.isBuilt ) throw new IllegalStateException( "This Builder has been built." );
      this.size = size;
    }

    public double getSize()
    {
      return this.size;
    }

    public void setResolution( double resolution )
    {
      if ( this.isBuilt ) throw new IllegalStateException( "This Builder has been built." );
      this.resolution = resolution;
    }

    public double getResolution()
    {
      return this.resolution;
    }

    public void setUnits( String units )
    {
      if ( this.isBuilt ) throw new IllegalStateException( "This Builder has been built." );
      this.units = units == null ? "" : units;
    }

    public String getUnits()
    {
      return this.units;
    }

    public boolean isBuilt()
    {
      return this.isBuilt;
    }

    public boolean isBuildable( List<BuilderIssue> issues )
    {
      return true;
    }

    public GeospatialRange build() throws BuilderException
    {
      this.isBuilt = true;
      return this;
    }
  }
}
