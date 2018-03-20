/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc.vertical;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.unidata.util.Parameter;

import java.io.IOException;
import java.util.List;

/**
 * Create a 3D height(z,y,x) array using the CF formula for
 * "ocean s vertical coordinate".
 * <p/>
 * Modified April 20, 2009 by sachin
 *
 * @author caron
 * @author skbhate@gmail.com
 * @see "https://www.myroms.org/wiki/index.php/Vertical_S-coordinate#Metadata_Considerations"
 */

public class OceanS extends VerticalTransformImpl {

  /**
   * The eta variable name identifier
   */
  public static final String ETA = "Eta_variableName";

  /**
   * The "s" variable name identifier
   */
  public static final String S = "S_variableName";

  /**
   * The "depth" variable name identifier
   */
  public static final String DEPTH = "Depth_variableName";

  /**
   * The "depth c" variable name identifier
   */
  public static final String DEPTH_C = "Depth_c_variableName";

  /**
   * The "a" variable name
   */
  public static final String A = "A_variableName";

  /**
   * The "b" variable name
   */
  public static final String B = "B_variableName";

  /**
   * the values of depth_c, a, and b
   */
  private double depth_c;

  /**
   * The eta, s and depth variables
   */
  private Variable etaVar, sVar, depthVar, aVar, bVar, depthCVar;

  /**
   * the c array
   */
  private Array c = null;

  /**
   * Create a new vertical transform for Ocean S coordinates
   *
   * @param ds      dataset
   * @param timeDim time dimension
   * @param params  list of transformation Parameters
   */
  public OceanS(NetcdfFile ds, Dimension timeDim, List<Parameter> params) {

    super(timeDim);
    String etaName = getParameterStringValue(params, ETA);
    String sName = getParameterStringValue(params, S);
    String depthName = getParameterStringValue(params, DEPTH);
    String aName = getParameterStringValue(params, A);
    String bName = getParameterStringValue(params, B);
    String depthCName = getParameterStringValue(params, DEPTH_C);

    etaVar = ds.findVariable(etaName);
    sVar = ds.findVariable(sName);
    depthVar = ds.findVariable(depthName);
    aVar = ds.findVariable(aName);
    bVar = ds.findVariable(bName);
    depthCVar = ds.findVariable(depthCName);

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
  public ArrayDouble.D3 getCoordinateArray(int timeIndex)  throws IOException, InvalidRangeException {
    Array etaArray = readArray(etaVar, timeIndex);
    Array sArray = readArray(sVar, timeIndex);
    Array depthArray = readArray(depthVar, timeIndex);

    if (null == c) {
      double a = aVar.readScalarDouble();
      double b = bVar.readScalarDouble();
      depth_c = depthCVar.readScalarDouble();
      c = makeC(sArray, a, b);
    }

    return makeHeight(etaArray, sArray, depthArray, c, depth_c);
  }

/** Add new method for retrieving 1D Z values for specified indices for lat/lon. */
  /* -Sachin */
  /**
   * Get the 1D vertical coordinate array for this time step and
   * the specified X,Y index for Lat-Lon point.
   *
   * @param timeIndex the time index. Ignored if !isTimeDependent().
   * @param xIndex    the x index
   * @param yIndex    the y index
   * @return vertical coordinate array
   * @throws IOException           problem reading data
   * @throws InvalidRangeException _more_
   */
  public ArrayDouble.D1 getCoordinateArray1D(int timeIndex, int xIndex, int yIndex)
      throws IOException, InvalidRangeException {
    Array etaArray = readArray(etaVar, timeIndex);
    Array sArray = readArray(sVar, timeIndex);
    Array depthArray = readArray(depthVar, timeIndex);

    if (null == c) {
      double a = aVar.readScalarDouble();
      double b = bVar.readScalarDouble();
      depth_c = depthCVar.readScalarDouble();
      c = makeC(sArray, a, b);
    }


    return makeHeight1D(etaArray, sArray, depthArray, c, depth_c, xIndex, yIndex);
  }

  // C(z) = (1-b)*sinh(a*s(z))/sinh(a) + b*(tanh(a*(s(z)+0.5))/(2*tanh(0.5*a))-0.5)

  /**
   * Make the C array
   *
   * @param s s Array
   * @param a "a" value
   * @param b "b" value
   * @return the C array
   */
  private Array makeC(Array s, double a, double b) {
    int nz = (int) s.getSize();
    Index sIndex = s.getIndex();
    if (a == 0) return s;  // per R. Signell, USGS

    ArrayDouble.D1 c = new ArrayDouble.D1(nz);

    double fac1 = 1.0 - b;
    double denom1 = 1.0 / Math.sinh(a);
    double denom2 = 1.0 / (2.0 * Math.tanh(0.5 * a));

    for (int i = 0; i < nz; i++) {
      double sz = s.getDouble(sIndex.set(i));
      double term1 = fac1 * Math.sinh(a * sz) * denom1;
      double term2 = b * (Math.tanh(a * (sz + 0.5))
          * denom2 - 0.5);
      c.set(i, term1 + term2);
    }

    return c;
  }


  /**
   * Make height from the given data. <br>
   * old equationn:  height(x,y,z) = eta(x,y)*(1+s(z)) + depth_c*s(z) + (depth(x,y)-depth_c)*C(z)
   * <p/>
   * <p/>
   * /* -sachin 03/23/09
   * The new corrected equation according to Hernan Arango (Rutgers)
   * height(x,y,z) =  S(x,y,z) + eta(x,y) * (1 + S(x,y,z) / depth(x,y) )
   * <p/>
   * where,
   * S(x,y,z) = depth_c*s(z) + (depth(x,y)-depth_c)*C(z)
   * /
   *
   * @param eta     eta Array
   * @param s       s Array
   * @param depth   depth Array
   * @param c       c Array
   * @param depth_c value of depth_c
   * @return hieght data
   */
  private ArrayDouble.D3 makeHeight(Array eta, Array s, Array depth, Array c, double depth_c) {
    int nz = (int) s.getSize();
    Index sIndex = s.getIndex();
    Index cIndex = c.getIndex();

    int[] shape2D = eta.getShape();
    int ny = shape2D[0];
    int nx = shape2D[1];
    Index etaIndex = eta.getIndex();
    Index depthIndex = depth.getIndex();

    ArrayDouble.D3 height = new ArrayDouble.D3(nz, ny, nx);

    for (int z = 0; z < nz; z++) {
      double sz = s.getDouble(sIndex.set(z));
      double cz = c.getDouble(cIndex.set(z));

      double term1 = depth_c * sz;

      for (int y = 0; y < ny; y++) {
        for (int x = 0; x < nx; x++) {
          //-sachin 03/23/09  modifications according to corrected equation.
          double fac1 = depth.getDouble(depthIndex.set(y, x));
          double term2 = (fac1 - depth_c) * cz;

          double Sterm = term1 + term2;

          double term3 = eta.getDouble(etaIndex.set(y, x));
          double term4 = 1 + Sterm / fac1;
          double hterm = Sterm + term3 * term4;

          height.set(z, y, x, hterm);

        }
      }
    }

    return height;
  }


  // Modify method 'makeHeight' as  new method for getting vertical coordinate array for single point.
  //- sachin
  private ArrayDouble.D1 makeHeight1D(Array eta, Array s, Array depth,
                                      Array c, double depth_c, int x_index, int y_index) {
    int nz = (int) s.getSize();
    Index sIndex = s.getIndex();
    Index cIndex = c.getIndex();


    Index etaIndex = eta.getIndex();
    Index depthIndex = depth.getIndex();

    ArrayDouble.D1 height = new ArrayDouble.D1(nz);

    for (int z = 0; z < nz; z++) {
      double sz = s.getDouble(sIndex.set(z));
      double cz = c.getDouble(cIndex.set(z));

      double term1 = depth_c * sz;
      //-sachin 03/06/09  modifications according to corrected equation.

      double fac1 = depth.getDouble(depthIndex.set(y_index, x_index));
      double term2 = (fac1 - depth_c) * cz;


      double Sterm = term1 + term2;

      double term3 = eta.getDouble(etaIndex.set(y_index, x_index));
      double term4 = 1 + Sterm / fac1;
      double hterm = Sterm + term3 * term4;

      height.set(z, hterm);
    }

    return height;
  }
}

