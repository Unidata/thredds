package thredds.catalog2;

import java.util.List;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface CatalogIssues
{
  public List<CatalogIssue> getIssues();

  public boolean isValid();
  public boolean isEmpty();
  public int size();

  public String toString();
}
