package thredds.catalog2.xml.names;

import thredds.catalog2.xml.names.CatalogNamespace;
import thredds.catalog2.ThreddsMetadata;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

/**
 * XML qualified names for the elements and attributes defined by
 * the "threddsMetadataGroup" group in the THREDDS InvCatalog
 * specification (version 1.0.2).
 *
 * @author edavis
 * @since 4.0
 */
public class ThreddsMetadataElementNames
{
  private ThreddsMetadataElementNames() {}

  /**
   * Placeholder only. DO NOT USE. 
   */
  public static final QName ThreddsMetadataElement
          = new QName( XMLConstants.NULL_NS_URI,
                       "_proxy_" );

  public static final QName ServiceNameElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "serviceName" );
  public static final QName AuthorityElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "authority" );
  
  public static final QName DocumentationElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "documentation" );
  public static final QName DocumentationElement_Type
          = new QName( XMLConstants.NULL_NS_URI,
                       "type" );
  public static final String DocumentationElement_Type_Funding = "funding";
  public static final String DocumentationElement_Type_History = "history";
  public static final String DocumentationElement_Type_ProcessingLevel = "processing_level";
  public static final String DocumentationElement_Type_Rights = "rights";
  public static final String DocumentationElement_Type_Summary = "summary";

  public static final QName DocumentationElement_XlinkTitle
          = new QName( CatalogNamespace.XLINK.getNamespaceUri(),
                       "title" );
  public static final QName DocumentationElement_XlinkHref
          = new QName( CatalogNamespace.XLINK.getNamespaceUri(),
                       "href" );
  public static final QName DocumentationElement_XlinkType
          = new QName( CatalogNamespace.XLINK.getNamespaceUri(),
                       "type" );


  public static final QName KeywordElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "keyword" );
  public static final QName ProjectElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "project" );
  public static final QName ControlledVocabType_Authority
          = new QName( XMLConstants.NULL_NS_URI,
                       "vocabulary" );

  public static final QName DateElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "date" );
    public static final QName DateElement_Type
            = new QName( XMLConstants.NULL_NS_URI,
                         "type" );
    public static final String DateElement_Type_Created = ThreddsMetadata.DatePointType.Created.toString(); // "created";
    public static final String DateElement_Type_Modified = ThreddsMetadata.DatePointType.Modified.toString(); // "modified";
    public static final String DateElement_Type_Valid = ThreddsMetadata.DatePointType.Valid.toString(); // "valid";
    public static final String DateElement_Type_Issued = ThreddsMetadata.DatePointType.Issued.toString(); // "issued";
    public static final String DateElement_Type_Available = ThreddsMetadata.DatePointType.Available.toString(); // "available";
    public static final String DateElement_Type_MetadataCreated = ThreddsMetadata.DatePointType.MetadataCreated.toString(); // "metadataCreated";
    public static final String DateElement_Type_MetadataModified = ThreddsMetadata.DatePointType.MetadataModified.toString(); // "metadataModified";
     
    public static final QName DateElement_Format
            = new QName( XMLConstants.NULL_NS_URI,
                         "format" );

  public static final QName CreatorElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "creator" );
  public static final QName CreatorElement_NameElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "name" );
  public static final QName CreatorElement_NameElement_NamingAuthority
          = new QName( XMLConstants.NULL_NS_URI,
                       "vocabulary" );
  public static final QName CreatorElement_ContactElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "contact" );
  public static final QName CreatorElement_ContactElement_Email
          = new QName( XMLConstants.NULL_NS_URI,
                       "email" );
  public static final QName CreatorElement_ContactElement_Url
          = new QName( XMLConstants.NULL_NS_URI,
                       "url" );

  public static final QName ContributorElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "contributor" );
  public static final QName ContributorElement_Role
          = new QName( XMLConstants.NULL_NS_URI,
                       "role" );

  public static final QName PublisherElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "publisher" );
  public static final QName PublisherElement_NameElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "name" );
  public static final QName PublisherElement_NameElement_NamingAuthority
          = new QName( XMLConstants.NULL_NS_URI,
                       "vocabulary" );
  public static final QName PublisherElement_ContactElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "contact" );
  public static final QName PublisherElement_ContactElement_Email
          = new QName( XMLConstants.NULL_NS_URI,
                       "email" );
  public static final QName PublisherElement_ContactElement_Url
          = new QName( XMLConstants.NULL_NS_URI,
                       "url" );

  public static final QName GeospatialCoverageElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "geospatialCoverage" );
  public static final QName GeospatialCoverageElement_NameElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "name" );
  public static final QName GeospatialCoverageElement_NorthsouthElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "northsouth" );
  public static final QName GeospatialCoverageElement_EastwestElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "eastwest" );
  public static final QName GeospatialCoverageElement_UpdownElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "updown" );
  public static final QName GeospatialCoverageElement_Zpositive
          = new QName( XMLConstants.NULL_NS_URI,
                       "zpositive" );

  public static final QName SpatialRangeType_Start
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "start" );
  public static final QName SpatialRangeType_Size
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "size" );
  public static final QName SpatialRangeType_Resolution
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "resolution" );
  public static final QName SpatialRangeType_Units
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "units" );


  public static final QName TimeCoverageElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "timeCoverage" );   
  public static final QName DateRangeType_StartElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "start" );
  public static final QName DateRangeType_EndElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "end" );
  public static final QName DateRangeType_DurationElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "duration" );
  public static final QName DateRangeType_ResolutionElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "resolution" );
  public static final QName DateType_Format
          = new QName( XMLConstants.NULL_NS_URI,
                       "format" );
  public static final QName DateType_Type
          = new QName( XMLConstants.NULL_NS_URI,
                       "type" );

  public static final QName VariablesElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "variables" );
  public static final QName VariablesElement_vocabAuthorityId
          = new QName( XMLConstants.NULL_NS_URI,
                       "vocabulary" );
  public static final QName VariablesElement_vocabAuthorityUrl
          = new QName( CatalogNamespace.XLINK.getNamespaceUri(),
                       "href" );
  public static final QName VariablesElement_VariableElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "variable" );
  public static final QName VariablesElement_VariableElement_name
          = new QName( XMLConstants.NULL_NS_URI,
                       "name" );
  public static final QName VariablesElement_VariableElement_units
          = new QName( XMLConstants.NULL_NS_URI,
                       "units" );
  public static final QName VariablesElement_VariableElement_vocabularyId
          = new QName( XMLConstants.NULL_NS_URI,
                       "vocabulary_id" );
  public static final QName VariablesElement_VariableElement_vocabularyName
          = new QName( XMLConstants.NULL_NS_URI,
                       "vocabulary_name" );
  public static final QName VariablesElement_VariableMapElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "variableMap" );
  public static final QName VariablesElement_VariableMapElement_XlinkHref
          = new QName( CatalogNamespace.XLINK.getNamespaceUri(),
                       "href" );

  public static final QName DataSizeElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "dataSize" );
  // ToDo "dataSize" attributes

  public static final QName DataFormatElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "dataFormat" );
  // ToDo "dataFormat" attributes

  public static final QName DataTypeElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "dataType" );
  // ToDo "dataType" attributes
}