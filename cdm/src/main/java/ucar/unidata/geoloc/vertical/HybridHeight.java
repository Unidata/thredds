/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc.vertical;

import ucar.ma2.*;
import ucar.ma2.ArrayDouble.D1;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.unidata.util.Parameter;

import java.io.IOException;
import java.util.List;


/**
 * Create a 3D height(z,y,x) array using the netCDF CF convention formula for
 * "Atmospheric Hybrid Height".
 * <p><strong>height(x,y,z) = a(z) + b(z)*orog(x,y)</strong>
 *
 * @author murray
 * @see <a href="http://cf-pcmdi.llnl.gov/">http://cf-pcmdi.llnl.gov/</a>
 */
public class HybridHeight extends VerticalTransformImpl {

  /**
   * Surface pressure name identifier
   */
  public static final String OROG = "Orography_variableName";

  /**
   * The "a" variable name identifier
   */
  public static final String A = "A_variableName";

  /**
   * The "b" variable name identifier
   */
  public static final String B = "B_variableName";

  /**
   * ps, a, and b variables
   */
  private Variable aVar, bVar, orogVar;

  /**
   * a and b Arrays
   */
  private Array aArray = null,
      bArray = null;

  /**
   * Construct a coordinate transform for hybrid height
   *
   * @param ds      netCDF dataset
   * @param timeDim time dimension
   * @param params  list of transformation Parameters
   */
  public HybridHeight(NetcdfFile ds, Dimension timeDim, List<Parameter> params) {

    super(timeDim);
    String aName = getParameterStringValue(params, A);
    String bName = getParameterStringValue(params, B);
    String orogName = getParameterStringValue(params, OROG);

    aVar = ds.findVariable(aName);
    bVar = ds.findVariable(bName);
    orogVar = ds.findVariable(orogName);
    units = ds.findAttValueIgnoreCase(orogVar, CDM.UNITS, "none");

  }

  /**
   * Get the 3D vertical coordinate array for this time step.
   *
   * @param timeIndex the time index. Ignored if !isTimeDependent().
   * @return vertical coordinate array
   * @throws IOException           problem reading data
   * @throws InvalidRangeException not a valid time range
   */
  public ArrayDouble.D3 getCoordinateArray(int timeIndex)
      throws IOException, InvalidRangeException {
    Array orogArray = readArray(orogVar, timeIndex);

    if (null == aArray) {
      aArray = aVar.read();
      bArray = bVar.read();
    }

    int nz = (int) aArray.getSize();
    Index aIndex = aArray.getIndex();
    Index bIndex = bArray.getIndex();

    int[] shape2D = orogArray.getShape();
    int ny = shape2D[0];
    int nx = shape2D[1];
    Index orogIndex = orogArray.getIndex();

    ArrayDouble.D3 height = new ArrayDouble.D3(nz, ny, nx);

    for (int z = 0; z < nz; z++) {
      double az = aArray.getDouble(aIndex.set(z));
      double bz = bArray.getDouble(bIndex.set(z));

      for (int y = 0; y < ny; y++) {
        for (int x = 0; x < nx; x++) {
          double orog = orogArray.getDouble(orogIndex.set(y, x));
          height.set(z, y, x, az + bz * orog);
        }
      }
    }

    return height;
  }
  
  /**
   * Get the 1D vertical coordinate array for this time step and point
   * 
   * @param timeIndex the time index. Ignored if !isTimeDependent().
   * @param xIndex    the x index
   * @param yIndex    the y index
   * @return vertical coordinate array
   * @throws java.io.IOException problem reading data
   * @throws ucar.ma2.InvalidRangeException _more_ 
   */  
  public D1 getCoordinateArray1D(int timeIndex, int xIndex, int yIndex)
  		throws IOException, InvalidRangeException {
	  
	  
	  Array orogArray = readArray(orogVar, timeIndex);
	  if (null == aArray) {
		  aArray = aVar.read();
		  bArray = bVar.read();
	  }

	  int nz = (int) aArray.getSize();
	  Index aIndex = aArray.getIndex();
	  Index bIndex = bArray.getIndex();	  

	  Index orogIndex = orogArray.getIndex();
	  ArrayDouble.D1 height = new ArrayDouble.D1(nz);

	  for (int z = 0; z < nz; z++) {
		  double az = aArray.getDouble(aIndex.set(z));
		  double bz = bArray.getDouble(bIndex.set(z));
		  
          double orog = orogArray.getDouble(orogIndex.set(yIndex, xIndex));
          height.set(z,  az + bz * orog);
		  
	  }
	  
	  return height;
  }  

}

