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
 * Create a 3D height(z,y,x) array using the CF formula for
 * "ocean sigma vertical coordinate".
 *
 * @author caron
 * @see <a href="http://cf-pcmdi.llnl.gov/">http://cf-pcmdi.llnl.gov/</a>
 */

public class OceanSigma extends VerticalTransformImpl {

  /**
   * The eta variable name identifier
   */
  public static final String ETA = "Eta_variableName";

  /**
   * The "s" variable name identifier
   */
  public static final String SIGMA = "Sigma_variableName";

  /**
   * The "depth" variable name identifier
   */
  public static final String DEPTH = "Depth_variableName";

  /**
   * The eta, s and depth variables
   */
  private Variable etaVar, sVar, depthVar;

  /**
   * Create a new vertical transform for Ocean S coordinates
   *
   * @param ds      dataset
   * @param timeDim time dimension
   * @param params  list of transformation Parameters
   */
  public OceanSigma(NetcdfFile ds, Dimension timeDim, List<Parameter> params) {

    super(timeDim);
    String etaName = getParameterStringValue(params, ETA);
    String sName = getParameterStringValue(params, SIGMA);
    String depthName = getParameterStringValue(params, DEPTH);

    etaVar = ds.findVariable(etaName);
    sVar = ds.findVariable(sName);
    depthVar = ds.findVariable(depthName);

    units = ds.findAttValueIgnoreCase(depthVar, CDM.UNITS, "none");
  }

  /**
   * Get the 3D vertical coordinate array for this time step.
   *
   * @param timeIndex the time index. Ignored if !isTimeDependent().
   * @return vertical coordinate array
   * @throws IOException           problem reading data
   * @throws InvalidRangeException _more_
   */
  public ArrayDouble.D3 getCoordinateArray(int timeIndex)
      throws IOException, InvalidRangeException {
    Array eta = readArray(etaVar, timeIndex);
    Array sigma = readArray(sVar, timeIndex);
    Array depth = readArray(depthVar, timeIndex);

    int nz = (int) sigma.getSize();
    Index sIndex = sigma.getIndex();

    int[] shape2D = eta.getShape();
    int ny = shape2D[0];
    int nx = shape2D[1];
    Index etaIndex = eta.getIndex();
    Index depthIndex = depth.getIndex();

    ArrayDouble.D3 height = new ArrayDouble.D3(nz, ny, nx);

    for (int z = 0; z < nz; z++) {
      double sigmaVal = sigma.getDouble(sIndex.set(z));
      for (int y = 0; y < ny; y++) {
        for (int x = 0; x < nx; x++) {
          double etaVal = eta.getDouble(etaIndex.set(y, x));
          double depthVal = depth.getDouble(depthIndex.set(y, x));

          height.set(z, y, x,
              etaVal + sigmaVal * (depthVal + etaVal));
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

	  Array eta = readArray(etaVar, timeIndex);
	  Array sigma = readArray(sVar, timeIndex);
	  Array depth = readArray(depthVar, timeIndex);

	  int nz = (int) sigma.getSize();
	  Index sIndex = sigma.getIndex();

	  Index etaIndex = eta.getIndex();
	  Index depthIndex = depth.getIndex();

	  ArrayDouble.D1 height = new ArrayDouble.D1(nz);

	  for (int z = 0; z < nz; z++) {
		  double sigmaVal = sigma.getDouble(sIndex.set(z));

		  double etaVal = eta.getDouble(etaIndex.set(yIndex, xIndex));
		  double depthVal = depth.getDouble(depthIndex.set(yIndex, xIndex));

		  height.set(z, etaVal + sigmaVal * (depthVal + etaVal));
	  }

	  return height;
  }  
  
}

