package thredds.catalog2.simpleImpl;

import thredds.catalog2.ThreddsMetadata;
import thredds.catalog2.builder.ThreddsMetadataBuilder;
import thredds.catalog2.builder.BuilderFinishIssue;
import thredds.catalog2.builder.BuilderException;

import java.util.List;

import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class ThreddsMetadataImpl
        implements ThreddsMetadata, ThreddsMetadataBuilder
{
  public ThreddsMetadataImpl()
  {
  }

  public List<Documentation> getDocumentation()
  {
    return null;
  }

  public List<Keyword> getKeywords()
  {
    return null;
  }

  public List<Contributor> getCreator()
  {
    return null;
  }

  public List<Contributor> getContributor()
  {
    return null;
  }

  public List<Contributor> getPublisher()
  {
    return null;
  }

  public String getProject()
  {
    return null;
  }

  public DateType getDateCreated()
  {
    return null;
  }

  public DateType getDateModified()
  {
    return null;
  }

  public DateType getDateIssued()
  {
    return null;
  }

  public DateRange getDateValid()
  {
    return null;
  }

  public DateRange getDateAvailable()
  {
    return null;
  }

  public DateType getDateMetadataCreated()
  {
    return null;
  }

  public DateType getDateMetadataModified()
  {
    return null;
  }

  public thredds.catalog.ThreddsMetadata.GeospatialCoverage getGeospatialCoverage()
  {
    return null;
  }// ToDo Or change GeospatialCoverage to CRS plus range for each dimension???
public DateRange getTemporalCoverage()
{
  return null;
}

  public List<Variable> getVariables()
  {
    return null;
  }

  public long getDataSize()
  {
    return 0;
  }

  public String getDataFormat()
  {
    return null;
  }

  public String getDataType()
  {
    return null;
  }

  public String getCollectionType() // ?????
  {
    return null;
  }

  public boolean isBuildable( List<BuilderFinishIssue> issues )
  {
    return false;
  }

  public Object build() throws BuilderException
  {
    return null;
  }
}
