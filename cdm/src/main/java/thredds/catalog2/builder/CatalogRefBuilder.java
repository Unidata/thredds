package thredds.catalog2.builder;

import thredds.catalog2.CatalogRef;

import java.net.URI;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface CatalogRefBuilder extends CatalogRef
{
  public void setId( String id );
  public void setTitle( String title );
  public void setReference( URI reference );

  public MetadataBuilder addMetadata();

  public void finish();
}
