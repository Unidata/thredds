package thredds.server.config;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface TdsCatConfigInitialize
{
  public void init();
  public void reinit();
  public void reinitPartial( String catalogPath );
}
