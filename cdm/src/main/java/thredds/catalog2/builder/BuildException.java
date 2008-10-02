package thredds.catalog2.builder;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class BuildException extends Exception
{
  private final ThreddsBuilder causeBuilder;

  public BuildException( ThreddsBuilder causeBuilder )
  {
    super();
    this.causeBuilder = causeBuilder;
  }

  public BuildException( ThreddsBuilder causeBuilder, String message)
  {
    super( message);
    this.causeBuilder = causeBuilder;
  }

  public BuildException( ThreddsBuilder causeBuilder, String message, Throwable cause)
  {
    super( message, cause);
    this.causeBuilder = causeBuilder;
  }

  public BuildException( ThreddsBuilder causeBuilder, Throwable cause)
  {
    super( cause);
    this.causeBuilder = causeBuilder;
  }
}
