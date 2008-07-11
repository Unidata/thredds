package thredds.catalog2.xml;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public abstract class AbstractDatasetElement
{
  public static final String THREDDS_DATASET_ELEMENT_NAME = "dataset";
  public static final String THREDDS_DATASET_NAME_ATTRIBUTE_NAME = "name";
  public static final String THREDDS_DATASET_ID_ATTRIBUTE_NAME = "ID";
  public static final String THREDDS_DATASET_ALIAS_ATTRIBUTE_NAME = "alias";
  
  /**
   * @deprecated Use metadata/authority element instead.
   */
  public static final String THREDDS_DATASET_AUTHORITY_ATTRIBUTE_NAME = "authority";
  public static final String THREDDS_DATASET_COLLECTION_TYPE_ATTRIBUTE_NAME = "collectionType";

  /**
   * @deprecated Use metadata/dataType element instead.
   */
  public static final String THREDDS_DATASET_DATA_TYPE_ATTRIBUTE_NAME = "dataType";
  public static final String THREDDS_DATASET_HARVEST_ATTRIBUTE_NAME = "harvest";
  public static final String THREDDS_DATASET_RESOURCE_CONTROL_ATTRIBUTE_NAME = "resourceControl";

  /**
   * @deprecated Use metadata/serviceName element instead.
   */
  public static final String THREDDS_DATASET_SERVICE_NAME_ATTRIBUTE_NAME = "serviceName";
  public static final String THREDDS_DATASET_URL_PATH_ATTRIBUTE_NAME = "urlPath";

}
