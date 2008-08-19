package thredds.catalog2;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class BasicAccessUriBuilderResolver
        implements AccessUriBuilderResolver
{
  //public void setAccessUriBuilders( List<AccessUriBuilder> accessUriBuilders );

  public AccessUriBuilder resolveAccessUriBuilder( Dataset dataset, Access access )
  {
    return new BasicAccessUriBuilder();
  }
}
