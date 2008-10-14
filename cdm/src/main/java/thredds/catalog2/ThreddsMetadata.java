package thredds.catalog2;

import thredds.catalog.MetadataType;

import java.net.URI;
import java.util.List;

import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface ThreddsMetadata
{
  public List<Documentation> getDocumentation();

  public List<Keyword> getKeywords();
  public List<Contributor> getCreator();
  public List<Contributor> getContributor();
  public List<Contributor> getPublisher();

  public String getProject();
  public DateType getDateCreated();
  public DateType getDateModified();
  public DateType getDateIssued();

  public DateRange getDateValid();
  public DateRange getDateAvailable();

  public DateType getDateMetadataCreated();
  public DateType getDateMetadataModified();

  public thredds.catalog.ThreddsMetadata.GeospatialCoverage getGeospatialCoverage();
            // ToDo Or change GeospatialCoverage to CRS plus range for each dimension???
  public DateRange getTemporalCoverage();
  public List<Variable> getVariables();

  public long getDataSize();
  public String getDataFormat();
  public String getDataType();
  public String getCollectionType(); // ?????

  public interface Documentation
  {
    public boolean isContainedContent();

    public String getDocType(); //"summary" ("abstract"?), "history", "processing_level",
                                //  "funding", "rights"
    public String getContent();

    public String getTitle();
    public URI getExternalReference();
  }

  public interface Keyword
  {
    public String getAuthority();
    public String getKeyword();
  }

  public interface Contributor
  {
    public String getAuthority();
    public String getName();
    public String getEmail();
    public URI getWebPage();
  }

  public interface Variable
  {
    public String getAuthority();
    public String getId();
    public String getTitle();
    public String getDescription();
    public String getUnits();
  }
}