package thredds.catalog2.xml.names;

import thredds.catalog2.xml.names.CatalogNamespace;

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

  public static final QName KeywordElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "keyword" );
  // ToDo "keyword" attributes

  public static final QName DateElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "date" );
    public static final QName DateElement_Type
            = new QName( XMLConstants.NULL_NS_URI,
                         "type" );
    public static final String DateElement_Type_Created = "created";
    public static final String DateElement_Type_Modified = "modified";
    public static final String DateElement_Type_Valid = "valid";
    public static final String DateElement_Type_Issued = "issued";
    public static final String DateElement_Type_Available = "available";
    public static final String DateElement_Type_MetadataCreated = "metadataCreated";
    public static final String DateElement_Type_MetadataModified = "metadataModified";
     
    public static final QName DateElement_Format
            = new QName( XMLConstants.NULL_NS_URI,
                         "format" );

  public static final QName CreatorElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "creator" );
  // ToDo "creator" attributes

  public static final QName ContributorElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "contributor" );
  // ToDo "contributor" attributes

  public static final QName PublisherElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "publisher" );
  // ToDo "publisher" attributes

  public static final QName ProjectElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "project" );
  // ToDo "project" attributes

  public static final QName GeospatialCoverageElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "geospatialCoverage" );
  // ToDo "geospatialCoverage" attributes

  public static final QName TimeCoverageElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "timeCoverage" );
  // ToDo "timeCoverage" attributes

  public static final QName VariablesElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "variables" );
  // ToDo "variables" attributes

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