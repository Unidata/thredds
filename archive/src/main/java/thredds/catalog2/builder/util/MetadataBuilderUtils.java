package thredds.catalog2.builder.util;

import thredds.catalog2.builder.MetadataBuilder;
import thredds.catalog2.builder.ThreddsBuilderFactory;

/**
 * Utility methods for copying <code>MetadataBuilder</code>s.
 *
 * @author edavis
 * @since 4.0
 */
public class MetadataBuilderUtils
{
  private MetadataBuilderUtils() {}

  public static MetadataBuilder copyIntoNewMetadataBuilder( MetadataBuilder source,
                                                            ThreddsBuilderFactory builderFactory )
  {
    if ( source == null )
      throw new IllegalArgumentException( "Source builder may not be null." );
    if ( builderFactory == null )
      throw new IllegalArgumentException( "Builder factory may not be null.");

    MetadataBuilder result = builderFactory.newMetadataBuilder();
    copyMetadataBuilder( source, result );
    return result;
  }

  public static MetadataBuilder copyMetadataBuilder( MetadataBuilder source,
                                                     MetadataBuilder recipient )
  {
    if ( source == null )
      throw new IllegalArgumentException( "Source builder may not be null.");
    if ( recipient == null )
      throw new IllegalArgumentException( "Recipient builder may not be null.");

    recipient.setContainedContent( source.isContainedContent() );
    if ( source.isContainedContent())
      recipient.setContent( source.getContent() );
    else
    {
      recipient.setTitle( source.getTitle() );
      recipient.setExternalReference( source.getExternalReference() );
    }

    return recipient;
  }
}