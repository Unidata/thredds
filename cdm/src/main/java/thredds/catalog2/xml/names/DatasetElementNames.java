package thredds.catalog2.xml.names;

import thredds.catalog2.xml.names.CatalogNamespace;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

/**
 * XML qualified names for the elements and attributes defined by
 * the "dataset" element in the THREDDS InvCatalog
 * specification (version 1.0.2).
 *
 * @author edavis
 * @since 4.0
 */
public class DatasetElementNames
{
  private DatasetElementNames(){}

  public static final QName DatasetElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "dataset" );
  public static final QName DatasetElement_Name
          = new QName( XMLConstants.NULL_NS_URI,
                       "name" );

  /**
   * @deprecated Use metadata/dataType element instead.
   */
  public static final QName DatasetElement_DataType
          = new QName( XMLConstants.NULL_NS_URI,
                       "dataType" );
  public static final QName DatasetElement_ResourceControl
          = new QName( XMLConstants.NULL_NS_URI,
                       "resourceControl" );

  /**
   * @deprecated Use metadata/serviceName element instead.
   */
  public static final QName DatasetElement_ServiceName
          = new QName( XMLConstants.NULL_NS_URI,
                       "serviceName" );
  public static final QName DatasetElement_UrlPath
          = new QName( XMLConstants.NULL_NS_URI,
                       "urlPath" );
}