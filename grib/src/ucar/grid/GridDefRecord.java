/*
 * $Id: IDV-Style.xjs,v 1.3 2007/02/16 19:18:30 dmurray Exp $
 *
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


package ucar.grid;


import ucar.grib.GribGDSVariablesIF;
import ucar.grib.GribNumbers;

import java.util.HashMap;
import java.util.Map;


/**
 * Class to represent the grid definition (projection) information
 * purpose is to convert from String representation to native value.
 */
public abstract class GridDefRecord {

  public static final boolean debug = false;

  /**
   * UNDEFINED int value
   */
  public static final int UNDEFINED = -9999;

  // enumerations for common variables. These enumerations values are consistent
  // with the binary and the older text indexes. If they are changed then
  // many iosp grid classes will fail.

  /**
   * GDS key
   */
  public static final String GDS_KEY = "GDSkey";

  /**
   * Wind flag
   * @deprecated  use Grib2Tables.VectorComponentFlag
   */
  public static final String WIND_FLAG = "Winds";

  /**
   * number of points in X direction (columns)
   */
  public static final String NX = "Nx";

  /**
   * number of points in Y direction (rows)
   */
  public static final String NY = "Ny";

  /**
   * distance in X direction
   */
  public static final String DX = "Dx";

  /**
   * distance in Y direction
   */
  public static final String DY = "Dy";

  /**
   * resolution
   */
  public static final String RESOLUTION = "ResCompFlag";

  /**
   * resolution
   */
  public static final String VECTOR_COMPONENT_FLAG = "VectorComponentFlag";

  /**
   * first lat
   */
  public static final String LATIN = "Latin";

  /**
   * first lat
   */
  public static final String LATIN1 = "Latin1";

  /**
   * second lat
   */
  public static final String LATIN2 = "Latin2";

  /**
   * La1
   */
  public static final String LA1 = "La1";

  /**
   * Lo1
   */
  public static final String LO1 = "Lo1";

  /**
   * La2
   */
  public static final String LA2 = "La2";

  /**
   * Lo2
   */
  public static final String LO2 = "Lo2";

  /**
   * LoD
   */
  public static final String LAD = "LaD";

  /**
   * LoV
   */
  public static final String LOV = "LoV";

  /**
   * Lap
   */
  public static final String LAP = "Lap";

  /**
   * Lop
   */
  public static final String LOP = "Lop";

  /**
   * pLat
   */
  public static final String PLAT = "pLat";

  /**
   * pLon
   */
  public static final String PLON = "pLon";

  /**
   * SpLat
   */
  public static final String SPLAT = "SpLat";

  /**
   * SpLon
   */
  public static final String SPLON = "SpLon";

  /**
   * RotationAngle
   */
  public static final String ROTATIONANGLE = "RotationAngle";

  /**
   * StretchingFactor
   */
  public static final String STRETCHINGFACTOR = "StretchingFactor";

  /**
   * Angle
   */
  public static final String ANGLE = "Angle";

  /**
   * BasicAngle
   */
  public static final String BASICANGLE = "BasicAngle";

  /**
   * Xp
   */
  public static final String XP = "Xp";

  /**
   * Yp
   */
  public static final String YP = "Yp";


  /**
   * Xo
   */
  public static final String XO = "Xo";

  /**
   * Yo
   */
  public static final String YO = "Yo";

  /**
   * Np
   */
  public static final String NP = "Np";

  /**
   * Nr
   */
  public static final String NR = "Nr";

  /**
   * J
   */
  public static final String J = "J";

  /**
   * K
   */
  public static final String K = "K";

  /**
   * M
   */
  public static final String M = "M";

  /**
   * NumberParallels
   */
  public static final String NUMBERPARALLELS = "NumberParallels";

  /**
   * MethodNorm
   */
  public static final String METHODNORM = "MethodNorm";

  /**
   * ModeOrder
   */
  public static final String MODEORDER = "ModeOrder";
  /**
   * PROJ
   */
  public static final String PROJ = "ProjFlag";

  /**
   * North pole PROJ
   */
  public static final String NPPROJ = "NpProj";

  /**
   * grid type
   */
  public static final String GRID_TYPE = "grid_type";

  /**
   * grid name
   */
  public static final String GRID_NAME = "grid_name";

  /**
   * GRID_SHAPE_CODE
   */
  public static final String GRID_SHAPE_CODE = "grid_shape_code";

  /**
   * GRID_SHAPE
   */
  public static final String GRID_SHAPE = "grid_shape";

  /**
   * Radius of spherical earth
   */
  public static final String RADIUS_SPHERICAL_EARTH =
      "grid_radius_spherical_earth";

  /**
   * major axis of earth
   */
  public static final String MAJOR_AXIS_EARTH = "grid_major_axis_earth";

  /**
   * minor axis of earth
   */
  public static final String MINOR_AXIS_EARTH = "grid_minor_axis_earth";

  /**
   * Quasi
   */
  public static final String QUASI = "Quasi";

  /**
   * grid spacing units (DX, DY)
   */
  public static final String GRID_UNITS = "grid_units";

  /**
   * Scanning mode for the data
   */
  public static final String SCANNING_MODE = "scanning_mode";

  /////////////////////////////////////////////////////////////

  /**
   * An Object that returns GDS variables from a byte[]
   */
  private final GribGDSVariablesIF gdsv;

  /**
   * A Map to hold grid parameter definitions of String
   */
  private final Map<String, String> paramStr = new HashMap<String, String>();

  /**
   * A Map to hold grid parameter definitions of Integer
   */
  private final Map<String, Integer> paramInt = new HashMap<String, Integer>();

  /**
   * A Map to hold grid parameter definitions of Double
   */
  private final Map<String, Double> paramDbl = new HashMap<String, Double>();

  /**
   * String of all parameters with their values
   */
  private String paramsValues;
  

  /*
  * These and other projection type int/double vars are store in the
  * approprite MAPs  paramInt and paramDble for quick access without
  * conversion except for the first request. Use routines:
  * getInt( String ) and getDouble( String )
  *
  * public String gdsKey, winds;
  * public int grid_type, nx, ny, resolution;
  * public double dx, dy;
  * public double latin1, latin2, La1, Lo1, LaD, LoV;
  * public int grid_shape_code;
  * public double radius_spherical_earth, major_axis_earth, minor_axis_earth;
  */

  /**
   * constructors.
   */
  public GridDefRecord() {
    paramsValues = "";
    gdsv = null;
  }

  public GridDefRecord( GribGDSVariablesIF gdsv ) {
    paramsValues = "";
    this.gdsv = gdsv;
  }

  public GridDefRecord(String paramsValues) {
    this.paramsValues = paramsValues;
    String[] split = paramsValues.split("\\t");
    for (int i = 0; i < split.length; i += 2) {
      paramStr.put(split[i], split[i + 1]);
    }
    gdsv = null;
  }

  /**
   * adds a param and value.
   *
   * @param key   name of the param
   * @param value of the param
   */
  public final void addParam(String key, String value) {
    //System.out.println(" adding " + key + " = " + value);
    paramStr.put(key.trim(), value);
    paramsValues = paramsValues +"\t"+ key +"\t"+ value;
  }

  /**
   * adds a param and value.
   *
   * @param key   name of the param
   * @param value of the param
   */
  public final void addParam(String key, int value) {
    //System.out.println(" adding " + key + " = " + value);
    paramInt.put(key, new Integer(value));
    paramStr.put(key, Integer.toString(value));
  }

  /**
   * adds a param and value.
   *
   * @param key   name of the param
   * @param value of the param
   */
  public final void addParam(String key, Integer value) {
    //System.out.println(" adding " + key + " = " + value);
    paramInt.put(key, value);
  }

  /**
   * adds a param and value.
   *
   * @param key   name of the param
   * @param value of the param
   */
  public final void addParam(String key, float value) {
    //System.out.println(" adding " + key + " = " + value);
    paramDbl.put(key, new Double (value));
    paramStr.put(key, Float.toString(value));
  }

  /**
   * adds a param and value.
   *
   * @param key   name of the param
   * @param value of the param
   */
  public final void addParam(String key, double value) {
    //System.out.println(" adding " + key + " = " + value);
    paramDbl.put(key, new Double (value));
    paramStr.put(key, Double.toString(value));
  }

  /**
   * adds a param and value.
   *
   * @param key   name of the param
   * @param value of the param
   */
  public final void addParam(String key, Double value) {
    //System.out.println(" adding " + key + " = " + value);
    paramDbl.put(key, value);
  }

  /**
   * gets a param and value.
   *
   * @param key name of the param
   * @return the value or null
   */
  public final String getParam(String key) {
    String value = paramStr.get(key);
    if(value == null) { // check the dbl and int tables
        Double result = paramDbl.get(key);
        if (result != null) {
            value = result.toString();
        } else {
            Integer intResult = paramInt.get(key);
            if (intResult != null) {
                value = result.toString();
            }
        }
        // save it back off to the string table for next time
        if (value != null) {
            paramStr.put(key, value);
        }
    }
    if (debug && value == null) {
        System.out.println( key +" value not found");
    }
    return value;
  }

  /*
  * Only converts the String param to a int once, then stores it as a
  * Integer in paramInt Map for future accesses
  * @deprecated use getInt()
  */
  public final int getParamInt(String key) {
    return getInt(key);
  }

  public final int getInt(String key) {
    Integer result = paramInt.get(key);
    if (result != null) {
      return result.intValue();
    } else {
      String value = paramStr.get(key);
      if (value != null) {
        try {
          result = new Integer(value.trim());
        } catch (NumberFormatException e) {
          // number might be written as int like 65.0
          double dvalue = getDouble( key );
          if ( ! Double.isNaN(dvalue )) {
            result = new Integer( (int) dvalue );
            paramInt.put(key, result);
            return result.intValue();
          }
          e.printStackTrace();
          if( debug )
            System.out.println( key +" cannot be convert to Int");
          return UNDEFINED;
        }
        paramInt.put(key, result);
        return result.intValue();
      } else {
        if( debug )
          System.out.println( key +" value not found");
        return UNDEFINED;
      }
    }
  }

  /*
  * Only converts the String param to a double once, then stores it as a
  * Double in paramDbl Map for future accesses
  * @deprecated use getDouble()
  */
  public final double getParamDouble(String key) {
    return getDouble(key);
  }

  public final double getDouble(String key) {
    Double result = paramDbl.get(key);
    if (result != null) {
      return result.doubleValue();
    } else {
      String value = paramStr.get(key);
      if (value != null) {
        try {
          result = new Double(value.trim());
        } catch (NumberFormatException e) {
          e.printStackTrace();
          if( debug )
            System.out.println( key +" cannot be convert to Double");
          return Double.NaN;
        }
        paramDbl.put(key, result);
        return result.doubleValue();
      } else {
        if( debug )
          System.out.println( key +" value not found");
        return Double.NaN;
      }
    }
  }

  /**
   * get the hcs as a String of params values
   * @return the hcs as a String of params values
   */
  public String getParamsValues() {
    return paramsValues;
  }

  /**
   * Get a short name for this GDSKey for the netCDF group.
   * Subclasses should implement as a short description
   *
   * @return short name
   */
  public abstract String getGroupName();

  /**
   * get the keySet
   *
   * @return the set of keys
   */
  public final java.util.Set<String> getKeys() {
    return paramStr.keySet();
  }

  /**
   * returns the value of the param.
   *
   * @param name param name
   * @return value, or NaN if value doest exist
   * @deprecated use getParamDouble(String key)
   */
  public final double readDouble(String name) {
    return getParamDouble(name);
  }

  /*
   * make available all GDS variables
   * @return GribGDSVariablesIF
   */
  public GribGDSVariablesIF getGdsv() {
    return gdsv;
  }

  /**
   * Compare GridDefRecords, the numerics will use closeEnough so values that
   * differ in 3 or 4th decimal places will return equal. This is being coded
   * because the NDFD model dx differ in the 3 decimal place otherwise equal.
   */
  public static boolean compare( GridDefRecord local, GridDefRecord other ) {
    java.util.Set<String> keys = local.getKeys();
    java.util.Set<String> okeys = other.getKeys();
    if( keys.size() != okeys.size() )
      return false;

    for( String key : keys ) {
      if( key.equals(WIND_FLAG) || key.equals(RESOLUTION) || key.equals(VECTOR_COMPONENT_FLAG)
        || key.equals(GDS_KEY))
        continue;

      String val = local.getParam( key );
      String oval = other.getParam( key );   //

      if( val.matches( "^[0-9]+\\.[0-9]*")) {
        //double
        double d = local.getDouble( key );
        double od = other.getDouble( key );
        if( ! GribNumbers.closeEnough(d, od ) )
          return false;
      } else if( val.matches( "^[0-9]+")) {
        // int
        if( !val.equals( oval ))
          return false;
      } else {
        // String
        if( !val.equals( oval ))
          return false;
      }
    }
    return true;
  }


}

