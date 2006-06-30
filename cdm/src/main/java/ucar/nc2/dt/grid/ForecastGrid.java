package ucar.nc2.dt.grid;

import ucar.nc2.Dimension;
import ucar.ma2.Array;
import ucar.ma2.MAMath;

import java.util.List;
import java.util.ArrayList;

/**
 * Experimental interface for "ForecastModelRun" grids.
 * These have two time dimensions, ForecastTime and RunTime.
 */
public interface ForecastGrid extends ucar.nc2.VariableSimpleIF {

  public int getRank();
  public Dimension getDimension(int dimIndex);
  public List getDimensions();

  public ucar.nc2.dt.grid.GridCoordSys getCoordinateSystem();
  public ucar.unidata.geoloc.ProjectionImpl getProjection();

  public Dimension getTimeDimension();
  public int getTimeDimensionIndex();

  public Dimension getXDimension();
  public int getXDimensionIndex();

  public Dimension getYDimension();
  public int getYDimensionIndex();

  public Dimension getZDimension();
  public int getZDimensionIndex();

  public ArrayList getLevels();
  public ArrayList getTimes();

  public Array getDataSlice(int t, int z, int y, int x) throws java.io.IOException;
  public Array readVolumeData(int t) throws java.io.IOException;
  public Array readYXData(int y, int x) throws java.io.IOException;
  public Array readZYData(int z, int y) throws java.io.IOException;

  public boolean hasMissingData();
  public boolean isMissingData(double val);
  public float[] setMissingToNaN(float[] data);
  public MAMath.MinMax getMinMaxSkipMissingData(Array data);

}
