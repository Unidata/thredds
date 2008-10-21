package thredds.catalog2;

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

  public List<Keyphrase> getKeyphrases();
  public List<Contributor> getCreator();
  public List<Contributor> getContributor();
  public List<Contributor> getPublisher();

  public String getProjectTitle();
  public DateType getDateCreated();
  public DateType getDateModified();
  public DateType getDateIssued();

  public DateRange getDateValid();
  public DateRange getDateAvailable();

  public DateType getDateMetadataCreated();
  public DateType getDateMetadataModified();

  public GeospatialCoverage getGeospatialCoverage();
  public DateRange getTemporalCoverage();
  public List<Variable> getVariables();

  public long getDataSizeInBytes();
  public String getDataFormat();
  public String getDataType();
  public String getCollectionType(); // ?????

  public interface Documentation
  {
    public boolean isContainedContent();

    public String getDocType(); //"summary" ("abstract"?), "history", "processing_level", "funding", "rights"

    public String getContent();

    public String getTitle();
    public URI getExternalReference();
  }

  public interface Keyphrase
  {
    public String getAuthority();
    public String getPhrase();
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

  public interface GeospatialCoverage
  {
    public URI getCRS();
    public boolean isGlobal();
    public boolean isZPositiveUp();   // Is this needed since have CRS?
    public List<GeospatialRange> getExtent(); 
  }

  public interface GeospatialRange
  {
    public boolean isHorizontal();
    public double getStart();
    public double getSize();
    public double getResolution();
    public String getUnits();
  }
}