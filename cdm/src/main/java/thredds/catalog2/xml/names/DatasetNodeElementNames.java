package thredds.catalog2.xml.names;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

/**
 * XML qualified names for the elements and attributes defined
 * in common by the "dataset" and "catalogRef elements in the
 * THREDDS InvCatalog specification (version 1.0.2).
 *
 * @author edavis
 * @since 4.0
 */
public class DatasetNodeElementNames
{
  private DatasetNodeElementNames(){}

  public static final QName DatasetNodeElement_Id
          = new QName( XMLConstants.NULL_NS_URI,
                       "ID" );

  /**
   * @deprecated Use metadata/authority element instead.
   */
  public static final QName DatasetNodeElement_Authority
          = new QName( XMLConstants.NULL_NS_URI,
                       "authority" );
  public static final QName DatasetNodeElement_CollectionType
          = new QName( XMLConstants.NULL_NS_URI,
                       "collectionType" );
  public static final QName DatasetNodeElement_Harvest
          = new QName( XMLConstants.NULL_NS_URI,
                       "harvest" );

}