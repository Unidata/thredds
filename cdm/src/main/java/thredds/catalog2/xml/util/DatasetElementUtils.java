package thredds.catalog2.xml.util;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class DatasetElementUtils
{
  private DatasetElementUtils() {}
  
  public static final String ELEMENT_NAME = "dataset";
  public static final String NAME_ATTRIBUTE_NAME = "name";
  public static final String ID_ATTRIBUTE_NAME = "ID";
  public static final String ALIAS_ATTRIBUTE_NAME = "alias";
  
  /**
   * @deprecated Use metadata/authority element instead.
   */
  public static final String AUTHORITY_ATTRIBUTE_NAME = "authority";
  public static final String COLLECTION_TYPE_ATTRIBUTE_NAME = "collectionType";

  /**
   * @deprecated Use metadata/dataType element instead.
   */
  public static final String DATA_TYPE_ATTRIBUTE_NAME = "dataType";
  public static final String HARVEST_ATTRIBUTE_NAME = "harvest";
  public static final String RESOURCE_CONTROL_ATTRIBUTE_NAME = "resourceControl";

  /**
   * @deprecated Use metadata/serviceName element instead.
   */
  public static final String SERVICE_NAME_ATTRIBUTE_NAME = "serviceName";
  public static final String URL_PATH_ATTRIBUTE_NAME = "urlPath";

}
