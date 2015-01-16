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
  public boolean isValid();

  public int getNumFatalIssues();
  public int getNumErrorIssues();
  public int getNumWarningIssues();
  public String getIssuesMessage();
  public String getFatalIssuesMessage();
  public String getErrorIssuesMessage();
  public String getWarningIssuesMessage();

  public String toString();
}
