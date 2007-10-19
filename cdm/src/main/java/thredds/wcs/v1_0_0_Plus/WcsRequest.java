package thredds.wcs.v1_0_0_Plus;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;

/**
 * Represent the incoming WCS 1.0.0+ request.
 *
 * @author edavis
 * @since 4.0
 */
public abstract class WcsRequest
{
  // General request info
  private Operation operation;
  private String version;

  // Dataset
  private String datasetPath;
  private GridDataset dataset;
  private List<String> availableCoverageNames;

  public enum Operation
  {
    GetCapabilities, DescribeCoverage, GetCoverage
  }

  public enum RequestEncoding
  {
    GET_KVP, POST_XML, POST_SOAP
  }

  public enum Format
  {
    NONE, GeoTIFF, GeoTIFF_Float, NetCDF3
  }

  WcsRequest( Operation operation, String version, String datasetPath, GridDataset dataset )
  {
    this.operation = operation;
    this.version = version;
    this.datasetPath = datasetPath;
    this.dataset = dataset;
    this.availableCoverageNames = new ArrayList<String>();
    for ( GridDataset.Gridset curGridset : this.dataset.getGridsets() )
    {
      for ( GridDatatype curGridDatatype : curGridset.getGrids())
      {
        this.availableCoverageNames.add( curGridDatatype.getName());
      }
    }

    if ( operation == null )
      throw new IllegalArgumentException( "Non-null operation required." );
    if ( this.datasetPath == null )
      throw new IllegalArgumentException( "Non-null dataset path required." );
    if ( this.dataset == null )
      throw new IllegalArgumentException( "Non-null dataset required." );
  }

  public Operation getOperation() { return operation; }
  public String getVersion() { return version; }
  public String getDatasetPath() { return datasetPath; }
  public GridDataset getDataset() { return dataset; }

  public List<String> getAvailableCoverageNames()
  {
    return Collections.unmodifiableList( availableCoverageNames);
  }
}
