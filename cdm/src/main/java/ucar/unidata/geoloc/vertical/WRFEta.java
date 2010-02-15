/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.unidata.geoloc.vertical;

import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.Index;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;

import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.NetcdfFile;

import ucar.unidata.util.Parameter;

import java.io.IOException;
import java.util.List;

/**
 * Models the vertical coordinate for the Weather Research and Forecast
 * (WRF) model's vertical Eta coordinate
 *
 * @author IDV Development Team
 */
public class WRFEta extends VerticalTransformImpl {

  public static final String BasePressureVariable = "base_presure";
  public static final String PerturbationPressureVariable = "perturbation_presure";
  public static final String BaseGeopotentialVariable = "base_geopotential";
  public static final String PerturbationGeopotentialVariable = "perturbation_geopotential";
  public static final String IsStaggeredX = "staggered_x";
  public static final String IsStaggeredY = "staggered_y";
  public static final String IsStaggeredZ = "staggered_z";

  /**
   * perturbation variable
   */
  private Variable pertVar;

  /**
   * base variable
   */
  private Variable baseVar;

  /**
   * some boolean flags
   */
  private boolean isXStag, isYStag, isZStag;

  /**
   * Construct a vertical coordinate for the Weather Research and Forecast
   * (WRF) model's vertical Eta coordinate
   *
   * @param ds      netCDF dataset
   * @param timeDim time dimension
   * @param params  list of transformation Parameters
   */
  public WRFEta(NetcdfFile ds, Dimension timeDim, List<Parameter> params) {

    super(timeDim);

    isXStag = getParameterBooleanValue(params, IsStaggeredX);
    isYStag = getParameterBooleanValue(params, IsStaggeredY);
    isZStag = getParameterBooleanValue(params, IsStaggeredZ);

    String pertVarName;
    String baseVarName;
    if (isZStag) {
      //Geopotential is provided on the staggered z grid
      //so we'll transform like grids to height levels
      pertVarName = getParameterStringValue(params, PerturbationGeopotentialVariable);
      baseVarName = getParameterStringValue(params, BaseGeopotentialVariable);
      units = "m";  //PH and PHB are in m^2/s^2, so dividing by g=9.81 m/s^2 results in meters
    } else {
      //Pressure is provided on the non-staggered z grid
      //so we'll transform like grids to pressure levels
      pertVarName = getParameterStringValue(params, PerturbationPressureVariable);
      baseVarName = getParameterStringValue(params, BasePressureVariable);
      units = "Pa";  //P and PB are in Pascals  //ADD:safe assumption? grab unit attribute?
    }

    pertVar = ds.findVariable(pertVarName);
    baseVar = ds.findVariable(baseVarName);

    if (pertVar == null) {
      throw new RuntimeException(
          "Cant find perturbation pressure variable= " + pertVarName
              + " in WRF file");
    }
    if (baseVar == null) {
      throw new RuntimeException(
          "Cant find base state pressure variable=  " + baseVarName
              + " in WRF file");
    }
  }

  /**
   * Get the 3D vertical coordinate array for this time step.
   *
   * @param timeIndex the time index. Ignored if !isTimeDependent().
   * @return vertical coordinate array
   * @throws IOException problem reading data
   */
  public ArrayDouble.D3 getCoordinateArray(int timeIndex)
      throws IOException {
    ArrayDouble.D3 array;

    Array pertArray = getTimeSlice(pertVar, timeIndex);
    Array baseArray = getTimeSlice(baseVar, timeIndex);

    //ADD: use MAMath?
    //ADD: use IndexIterator from getIndexIteratorFast?
    int[] shape = pertArray.getShape();
    //ADD: assert that rank = 3
    //ADD: assert that both arrays are same shape
    int ni = shape[0];
    int nj = shape[1];
    int nk = shape[2];

    array = new ArrayDouble.D3(ni, nj, nk);
    Index index = array.getIndex();

    for (int i = 0; i < ni; i++) {
      for (int j = 0; j < nj; j++) {
        for (int k = 0; k < nk; k++) {
          index.set(i, j, k);
          double d = pertArray.getDouble(index)
              + baseArray.getDouble(index);
          if (isZStag) {
            d = d / 9.81;  //convert geopotential to height
          }
          array.setDouble(index, d);
        }
      }
    }

    if (isXStag) {
      array = addStagger(array, 2);  //assuming x dim index is 2
    }
    if (isYStag) {
      array = addStagger(array, 1);  //assuming y dim index is 1
    }

    return array;
  }

  /**
   * Add 1 to the size of the array for the given dimension.
   * Use linear average and interpolation to fill in the values.
   *
   * @param array    use this array
   * @param dimIndex use this dimension
   * @return new array with stagger
   */
  private ArrayDouble.D3 addStagger(ArrayDouble.D3 array, int dimIndex) {

    //ADD: assert 0<=dimIndex<=2

    int[] shape = array.getShape();
    int[] newShape = new int[3];
    for (int i = 0; i < 3; i++) {
      newShape[i] = shape[i];
    }
    newShape[dimIndex]++;
    int ni = newShape[0];
    int nj = newShape[1];
    int nk = newShape[2];
    ArrayDouble.D3 newArray = new ArrayDouble.D3(ni, nj, nk);
    //Index newIndex = newArray.getIndex();

    //extract 1d array to be extended
    int n = shape[dimIndex];  //length of extracted array
    double[] d = new double[n];  //tmp array to hold extracted values
    int[] eshape = new int[3];       //shape of extracted array
    int[] neweshape = new int[3];  //shape of new array slice to write into
    for (int i = 0; i < 3; i++) {
      eshape[i] = (i == dimIndex)
          ? n
          : 1;
      neweshape[i] = (i == dimIndex)
          ? n + 1
          : 1;
    }
    int[] origin = new int[3];

    try {

      //loop through the other 2 dimensions and "extrapinterpolate" the other
      for (int i = 0; i < ((dimIndex == 0)
          ? 1
          : ni); i++) {
        for (int j = 0; j < ((dimIndex == 1)
            ? 1
            : nj); j++) {
          for (int k = 0; k < ((dimIndex == 2)
              ? 1
              : nk); k++) {
            origin[0] = i;
            origin[1] = j;
            origin[2] = k;
            IndexIterator it = array.section(origin,
                eshape).getIndexIterator();
            for (int l = 0; l < n; l++) {
              d[l] = it.getDoubleNext();  //get the original values
            }
            double[] d2 = extrapinterpolate(d);  //compute new values
            //define slice of new array to write into
            IndexIterator newit =
                newArray.section(origin,
                    neweshape).getIndexIterator();
            for (int l = 0; l < n + 1; l++) {
              newit.setDoubleNext(d2[l]);
            }
          }
        }
      }
    } catch (InvalidRangeException e) {
      //ADD: report error?
      return null;
    }

    return newArray;
  }

  /**
   * Add one element to the array by linear interpolation
   * and extrapolation at the ends.
   *
   * @param array input array
   * @return extrapolated/interpolated array
   */
  private double[] extrapinterpolate(double[] array) {
    int n = array.length;
    double[] d = new double[n + 1];

    //end points from linear extrapolation
    //equations confirmed by Christopher Lindholm
    d[0] = 1.5 * array[0] - 0.5 * array[1];
    d[n] = 1.5 * array[n - 1] - 0.5 * array[n - 2];

    //inner points from simple average
    for (int i = 1; i < n; i++) {
      d[i] = 0.5 * (array[i - 1] + array[i]);
    }

    return d;
  }

  /**
   * Extract an Array (with rank reduced by one) from the Variable
   * for the given time index.
   *
   * @param v         variable to extract from
   * @param timeIndex time index
   * @return Array of data
   * @throws IOException problem getting Array
   */
  private Array getTimeSlice(Variable v, int timeIndex) throws IOException {
    //ADD: this would make a good utility method
    //ADD: use Array.slice?
    int[] shape = v.getShape();
    int[] origin = new int[v.getRank()];

    if (getTimeDimension() != null) {
      int dimIndex = v.findDimensionIndex(getTimeDimension().getName());
      if (dimIndex >= 0) {
        shape[dimIndex] = 1;
        origin[dimIndex] = timeIndex;
      }
    }

    try {
      return v.read(origin, shape).reduce();
    } catch (InvalidRangeException e) {
      return null;
    }
  }

}

