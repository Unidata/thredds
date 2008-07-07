package thredds.catalog2;

import thredds.catalog.ServiceType;

import java.util.List;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface Dataset extends MetadataContainer
{
  public String getName();
  public String getId();
  public String getAlias();
  public List<Access> getAccesses();
  public Access getAccess( ServiceType type );
  public List<Dataset> getDatasets(); 
}
