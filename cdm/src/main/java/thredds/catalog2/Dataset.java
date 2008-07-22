package thredds.catalog2;

import thredds.catalog.ServiceType;

import java.util.List;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface Dataset extends DatasetNode
{
  public String getAlias();

  public boolean isAccessible();
  public List<Access> getAccesses();

}
