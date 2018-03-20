/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc.vertical;

import java.io.IOException;
import java.util.List;

import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayDouble.D1;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.units.SimpleUnit;
import ucar.unidata.util.Parameter;


/**
 * Create a 3D height(z,y,x) array using the CF formula for
 * "atmospheric sigma vertical coordinate".
 * <p><strong>pressure(x,y,z) = ptop + sigma(z)*surfacePressure(x,y)</strong>
 *
 * @author caron
 * @see <a href="http://cf-pcmdi.llnl.gov/">http://cf-pcmdi.llnl.gov/</a>
 */

public class AtmosSigma extends VerticalTransformImpl {

  /**
   * P-naught identifier
   */
  public static final String PTOP = "PressureTop_variableName";

  /**
   * Surface pressure name identifier
   */
  public static final String PS = "SurfacePressure_variableName";

  /**
   * The "depth" variable name identifier
   */
  public static final String SIGMA = "Sigma_variableName";

  /**
   * The ps, sigma variables
   */
  private Variable psVar;

  /**
   * The sigma array, function of z
   */
  private double[] sigma;

  /**
   * Top of the model
   */
  private double ptop;

  /**
   * Create a new vertical transform for Ocean S coordinates
   *
   * @param ds      dataset
   * @param timeDim time dimension
   * @param params  list of transformation Parameters
   */
  public AtmosSigma(NetcdfFile ds, Dimension timeDim, List<Parameter> params) {
    super(timeDim);

    String psName = getParameterStringValue(params, PS);
    psVar = ds.findVariable(psName);

    String ptopName = getParameterStringValue(params, PTOP);
    Variable ptopVar = ds.findVariable(ptopName);
    try {
      this.ptop = ptopVar.readScalarDouble();
    } catch (IOException e) {
      throw new IllegalArgumentException("AtmosSigma failed to read "
          + ptopVar + " err= " + e.getMessage());
    }

    String sigmaName = getParameterStringValue(params, SIGMA);
    Variable sigmaVar = ds.findVariable(sigmaName);

    try {
      Array data = sigmaVar.read();
      sigma = (double[]) data.get1DJavaArray(double.class);
    } catch (IOException e) {
      throw new IllegalArgumentException("AtmosSigma failed to read "
          + sigmaName + " err= " + e.getMessage());
    }

    units = ds.findAttValueIgnoreCase(psVar, CDM.UNITS, "none");
    
    String ptopUnitStr = ds.findAttValueIgnoreCase(ptopVar, CDM.UNITS, "none");
    if (!units.equalsIgnoreCase(ptopUnitStr)) {
      // Convert ptopVar to units of psVar
      SimpleUnit psUnit = SimpleUnit.factory(units);
      SimpleUnit ptopUnit = SimpleUnit.factory(ptopUnitStr);
      double factor = ptopUnit.convertTo(1.0, psUnit);
      this.ptop = this.ptop * factor;
    }    
    
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
    Array ps = readArray(psVar, timeIndex);
    Index psIndex = ps.getIndex();

    int nz = sigma.length;
    int[] shape2D = ps.getShape();
    int ny = shape2D[0];
    int nx = shape2D[1];

    ArrayDouble.D3 result = new ArrayDouble.D3(nz, ny, nx);

    for (int y = 0; y < ny; y++) {
      for (int x = 0; x < nx; x++) {
        double psVal = ps.getDouble(psIndex.set(y, x));
        for (int z = 0; z < nz; z++) {
          result.set(z, y, x, ptop + sigma[z] * (psVal - ptop));
        }
      }
    }

    return result;
  }
  
  /**
   * Get the 1D vertical coordinate array for this time step and point
   * 
   * (needds test!!!)
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
	  	  		  		  		 
	    Array ps = readArray(psVar, timeIndex); 
	    Index psIndex = ps.getIndex();	  
	    int nz = sigma.length;  
	    ArrayDouble.D1 result = new ArrayDouble.D1(nz);
	    
        double psVal = ps.getDouble(psIndex.set(yIndex, xIndex));
        for (int z = 0; z < nz; z++) {
          result.set(z,  ptop + sigma[z] * (psVal - ptop));
        }	    
	    
	    return result;
	    
  }  
  
}

