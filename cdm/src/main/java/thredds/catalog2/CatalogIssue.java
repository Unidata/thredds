package thredds.catalog2;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface CatalogIssue
{
  public enum Severity { FATAL, ERROR, WARNING } ;

  public Severity getSeverity();
  public String getMessage();
  public CatalogObject getCatalogObject();
  public Exception getCause();
}
