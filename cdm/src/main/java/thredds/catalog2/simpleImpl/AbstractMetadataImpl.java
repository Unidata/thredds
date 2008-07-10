package thredds.catalog2.simpleImpl;

import thredds.catalog2.Metadata;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public abstract class AbstractMetadataImpl implements Metadata
{
  private org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( AbstractMetadataImpl.class );

  private boolean inherited;

  @Override
  public boolean isInherited()
  {
    return inherited;
  }

  public void setInherited( boolean inherited )
  {
    this.inherited = inherited;
  }
}
