/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc.vertical;

import ucar.ma2.*;
import ucar.ma2.ArrayDouble.D1;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.units.SimpleUnit;
import ucar.unidata.util.Parameter;

import java.io.IOException;
import java.util.List;

/**
 * Create a 3D height(z,y,x) array using the netCDF CF convention formula for
 * "Hybrid Sigma Pressure".
 * <p><strong>pressure(x,y,z) = a(z)*p0 + b(z)*surfacePressure(x,y)</strong>
 *   or
 * <p><strong>pressure(x,y,z) = ap(z) + b(z)*surfacePressure(x,y)</strong>
 *
 * @author caron
 * @see <a href="http://cf-pcmdi.llnl.gov/">http://cf-pcmdi.llnl.gov/</a>
 */
public class HybridSigmaPressure extends VerticalTransformImpl {

  /**
   * P-naught identifier
   */
  public static final String P0 = "P0_variableName";

  /**
   * Surface pressure name identifier
   */
  public static final String PS = "SurfacePressure_variableName";

  /**
   * The "a" variable name identifier
   */
  public static final String A = "A_variableName";

  /**
   * The "a" variable name identifier
   */
  public static final String AP = "AP_variableName";

  /**
   * The "b" variable name identifier
   */
  public static final String B = "B_variableName";

  /**
   * value of p-naught
   */
  private double p0;

  /**
   * ps, a, and b variables
   */
  private Variable psVar, aVar, bVar, p0Var;

  /**
   * If has AP, this is a pressure value and has to be also in the same units as PS 
   */
  private String apUnits ="";
  
  /**
   * a and b Arrays
   */
  private Array aArray = null,
          bArray = null;

  /**
   * Construct a coordinate transform for sigma pressure
   *
   * @param ds      netCDF dataset
   * @param timeDim time dimension
   * @param params  list of transformation Parameters
   */
  public HybridSigmaPressure(NetcdfFile ds, Dimension timeDim, List<Parameter> params) {

    super(timeDim);
    String psName = getParameterStringValue(params, PS);
    String aName = getParameterStringValue(params, A);
    String bName = getParameterStringValue(params, B);
    String p0Name = getParameterStringValue(params, P0);
    String apName = getParameterStringValue(params, AP);

    
    
    if (apName != null){
       aVar = ds.findVariable(apName);       
    }else
       aVar = ds.findVariable(aName);
    
    if(aVar.findAttributeIgnoreCase(CDM.UNITS) != null){
    	apUnits = aVar.findAttributeIgnoreCase(CDM.UNITS).getStringValue();
    }
    
    psVar = ds.findVariable(psName);
    bVar = ds.findVariable(bName);
    units = ds.findAttValueIgnoreCase(psVar, CDM.UNITS, "none");
    if (p0Name != null){    	
      p0Var = ds.findVariable(p0Name);
      apUnits = units; //Won't need transformation for AP = A * P0 * 1 (in this case) 
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
    Array psArray = readArray(psVar, timeIndex);

    if (null == aArray) {
      aArray = aVar.read();
      bArray = bVar.read();
      //p0 = (p0Var == null) ? 1.0 : p0Var.readScalarDouble();
      p0 = computeP0();
    }

    int nz = (int) aArray.getSize();
    Index aIndex = aArray.getIndex();
    Index bIndex = bArray.getIndex();

    // it's possible to have rank 3 because pressure can have a level, usually 1
    // Check if rank 3 and try to reduce
    if( psArray.getRank() == 3) 
      psArray = psArray.reduce(0);

    int[] shape2D = psArray.getShape();
    int ny = shape2D[0];
    int nx = shape2D[1];

    Index psIndex = psArray.getIndex();

    ArrayDouble.D3 press = new ArrayDouble.D3(nz, ny, nx);

    double ps;
    for (int z = 0; z < nz; z++) {
      double term1 = aArray.getDouble(aIndex.set(z)) * p0;
      
      //AP might need unit conversion
      if(!apUnits.equals(units) ){
    	  term1 = convertPressureToPSUnits(apUnits, term1);
      }
      
      double bz = bArray.getDouble(bIndex.set(z));

      for (int y = 0; y < ny; y++) {
        for (int x = 0; x < nx; x++) {
          ps = psArray.getDouble(psIndex.set( y, x));
          press.set(z, y, x, term1 + bz * ps);
        }
      }
    }

    return press;
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
	  
	    Array psArray = readArray(psVar, timeIndex);

	    if (null == aArray) {
	      aArray = aVar.read();
	      bArray = bVar.read();
	      //p0 = (p0Var == null) ? 1.0 : p0Var.readScalarDouble();
	      p0 = computeP0();
	    }

	    int nz = (int) aArray.getSize();
	    Index aIndex = aArray.getIndex();
	    Index bIndex = bArray.getIndex();

	    // it's possible to have rank 3 because pressure can have a level, usually 1
	    // Check if rank 3 and try to reduce
	    if( psArray.getRank() == 3) 
	      psArray = psArray.reduce(0);

	    Index psIndex = psArray.getIndex();

	    ArrayDouble.D1 press = new ArrayDouble.D1(nz);

	    double ps;
	    for (int z = 0; z < nz; z++) {
	    	    	
	      double term1 = aArray.getDouble(aIndex.set(z)) * p0;
	      //AP might need unit conversion
	      if(!apUnits.equals(units) ){
	    	  term1 = convertPressureToPSUnits(apUnits, term1);
	      }	      
	      
	      double bz = bArray.getDouble(bIndex.set(z));

          ps = psArray.getDouble(psIndex.set( yIndex, xIndex));
          press.set(z, term1 + bz * ps);
          
	    }

	    return press;
	  
	  
  }
  
  private double computeP0() throws IOException{
	  
	  if (p0Var == null) return 1.0; //Has AP variable
	  
	  double p0 = p0Var.readScalarDouble(); 
	  
	  //Units check:
	  // P0 must have same units as PS
	  String p0UnitStr = p0Var.findAttributeIgnoreCase(CDM.UNITS).getStringValue();
    if (p0UnitStr == null) throw new IllegalStateException();
	  if (!units.equalsIgnoreCase(p0UnitStr)) {
		  p0 = convertPressureToPSUnits(p0UnitStr, p0);		  
	  }	  
	  
	  return p0;
  }
  
  private double convertPressureToPSUnits(String unit, double val){
	  SimpleUnit psUnit = SimpleUnit.factory(units);
	  SimpleUnit ptopUnit = SimpleUnit.factory(unit);
	  double factor = ptopUnit.convertTo(1.0, psUnit);
	  return val * factor;	  
	  
  }

}

