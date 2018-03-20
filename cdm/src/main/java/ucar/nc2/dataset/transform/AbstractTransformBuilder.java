/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset.transform;

import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.*;
import ucar.unidata.geoloc.Earth;
import ucar.unidata.util.Parameter;

import java.util.StringTokenizer;
import java.util.List;
import java.util.Formatter;

/**
 * Abstract superclass for implementations of HorizTransformBuilderIF and VertTransformBuilderIF
 *
 * @author caron
 */
public abstract class AbstractTransformBuilder {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AbstractTransformBuilder.class);

   /*
   from CF: false_easting(false_northing):
    The value added to all abscissa(ordinate) values in the rectangular coordinates for a map projection.
    This value frequently is assigned to eliminate negative numbers.
    Expressed in the unit of the coordinate variable identified by the standard name projection_x_coordinate (projection_y_coordinate).
   */
  static public double getFalseEastingScaleFactor(NetcdfDataset ds, AttributeContainer ctv) {
    String units = getGeoCoordinateUnits(ds, ctv);
    return getFalseEastingScaleFactor(units);
  }

  static public String getGeoCoordinateUnits(NetcdfDataset ds, AttributeContainer ctv) {
    String units = ctv.findAttValueIgnoreCase(CDM.UNITS, null);
    if (units == null) {
      List<CoordinateAxis> axes = ds.getCoordinateAxes();
      for (CoordinateAxis axis : axes) {
        if (axis.getAxisType() == AxisType.GeoX) { // kludge - what if there's multiple ones?
          Variable v = axis.getOriginalVariable(); // LOOK why original variable ?
          units = (v == null) ? axis.getUnitsString() : v.getUnitsString();
          break;
        }
      }
    }
    return units;
  }

  static public double getFalseEastingScaleFactor(String geoCoordinateUnits) {
    if (geoCoordinateUnits != null) {
      try {
        SimpleUnit unit = SimpleUnit.factoryWithExceptions(geoCoordinateUnits);
        return unit.convertTo(1.0, SimpleUnit.kmUnit);
      } catch (Exception e) {
        log.warn(geoCoordinateUnits + " not convertible to km");
      }
    }
    return 1.0;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////
  protected Formatter errBuffer = null;
  protected double lat0, lon0, false_easting, false_northing, earth_radius;
  protected Earth earth;

  public void setErrorBuffer(Formatter errBuffer) {
    this.errBuffer = errBuffer;
  }

  public abstract String getTransformName();

  protected void readStandardParams(AttributeContainer ctv, String units) {
    lon0 = readAttributeDouble(ctv, CF.LONGITUDE_OF_CENTRAL_MERIDIAN, Double.NaN);
    if (Double.isNaN(lon0))
      lon0 = readAttributeDouble(ctv, CF.LONGITUDE_OF_PROJECTION_ORIGIN, Double.NaN);
    lat0 = readAttributeDouble(ctv, CF.LATITUDE_OF_PROJECTION_ORIGIN, Double.NaN);
    false_easting = readAttributeDouble(ctv, CF.FALSE_EASTING, 0.0);
    false_northing = readAttributeDouble(ctv, CF.FALSE_NORTHING, 0.0);

    if ((false_easting != 0.0) || (false_northing != 0.0)) {
      double scalef = getFalseEastingScaleFactor(units);
      false_easting *= scalef;
      false_northing *= scalef;
    }

    double semi_major_axis = readAttributeDouble(ctv, CF.SEMI_MAJOR_AXIS, Double.NaN);
    double semi_minor_axis = readAttributeDouble(ctv, CF.SEMI_MINOR_AXIS, Double.NaN);
    double inverse_flattening = readAttributeDouble(ctv, CF.INVERSE_FLATTENING, 0.0);

    earth_radius = getEarthRadiusInKm(ctv);
    // check for ellipsoidal earth
    if (!Double.isNaN(semi_major_axis) && (!Double.isNaN(semi_minor_axis) || inverse_flattening != 0.0)) {
      earth = new Earth(semi_major_axis, semi_minor_axis, inverse_flattening);
    }
  }

  /**
   * Read a variable attribute as a double.
   *
   * @param v        the variable
   * @param attname  name of variable
   * @param defValue default value if attribute is not found
   * @return attribute value as a double, else NaN if not found
   */
  protected double readAttributeDouble(AttributeContainer v, String attname, double defValue) {
    Attribute att = v.findAttributeIgnoreCase(attname);
    if (att == null) return defValue;
    if (att.isString())
      return Double.parseDouble(att.getStringValue());
    else
      return att.getNumericValue().doubleValue();
  }

  /**
   * Read an attribute as double[2]. If only one value, make second same as first.
   *
   * @param att the attribute. May be numeric or String.
   * @return attribute value as a double[2]
   */
  protected double[] readAttributeDouble2(Attribute att) {
    if (att == null) return null;

    double[] val = new double[2];
    if (att.isString()) {
      StringTokenizer stoke = new StringTokenizer(att.getStringValue());
      val[0] = Double.parseDouble(stoke.nextToken());
      val[1] = stoke.hasMoreTokens() ? Double.parseDouble(stoke.nextToken()) : val[0];
    } else {
      val[0] = att.getNumericValue().doubleValue();
      val[1] = (att.getLength() > 1) ? att.getNumericValue(1).doubleValue() : val[0];
    }
    return val;
  }

  /**
   * Add a Parameter to a CoordinateTransform.
   * Make sure that the variable exists. If readData is true, read the data and use it as the value of the
   * parameter, otherwise use the variable name as the value of the parameter.
   *
   * @param rs             the CoordinateTransform
   * @param paramName      the parameter name
   * @param ds             dataset
   * @param varNameEscaped escaped variable name
   * @return true if success, false is failed
   */
  protected boolean addParameter(CoordinateTransform rs, String paramName, NetcdfFile ds, String varNameEscaped) {
    if (null == (ds.findVariable(varNameEscaped))) {
      if (null != errBuffer)
        errBuffer.format("CoordTransBuilder %s: no Variable named %s%n", getTransformName(), varNameEscaped);
      return false;
    }

    rs.addParameter(new Parameter(paramName, varNameEscaped));
    return true;
  }

  protected String getFormula(AttributeContainer ctv) {
    String formula = ctv.findAttValueIgnoreCase("formula_terms", null);
    if (null == formula) {
      if (null != errBuffer)
        errBuffer.format("CoordTransBuilder %s: needs attribute 'formula_terms' on Variable %s%n", getTransformName(), ctv.getName());
      return null;
    }
    return formula;
  }

  public String[] parseFormula(String formula_terms, String termString) {
    String[] formulaTerms = formula_terms.split("[\\s:]+");  // split on 1 or more whitespace or ':'
    String[] terms = termString.split("[\\s]+");             // split on 1 or more whitespace 
    String[] values = new String[terms.length];

    for (int i = 0; i < terms.length; i++) {
      for (int j = 0; j < formulaTerms.length; j += 2) {  // look at every other formula term
        if (terms[i].equals(formulaTerms[j])) {     // if it matches
          values[i] = formulaTerms[j + 1];            // next term is the value
          break;
        }
      }
    }

    boolean ok = true;
    for (int i = 0; i < values.length; i++) {
      if (values[i] == null) {
        if (null != errBuffer)
          errBuffer.format("Missing term=%s in the formula '%s' for the vertical transform= %s%n", terms[i], formula_terms, getTransformName());
        ok = false;
      }
    }

    return ok ? values : null;
  }

  /**
   * Get the earth radius in km from the attribute "earth_radius".
   * Normally this is in meters, convert to km if its > 10,000.
   * Use Earth.getRadius() as default.
   *
   * @param ctv coord transform variable
   * @return earth radius in km
   */
  protected double getEarthRadiusInKm(AttributeContainer ctv) {
    double earth_radius = readAttributeDouble(ctv, CF.EARTH_RADIUS, Earth.getRadius());
    if (earth_radius > 10000.0) earth_radius *= .001;
    return earth_radius;
  }


  //////////////////////////////////////////
  /*
    //////////////////////////////////////////
  static public double getFalseEastingScaleFactor(NetcdfDataset ds, Variable ctv) {
    double factor = 1.0;
    String units = ds.findAttValueIgnoreCase(ctv, CDM.UNITS, null);
    if (units == null) {
      List<CoordinateAxis> axes = ds.getCoordinateAxes();
      for (CoordinateAxis axis : axes) {
        if (axis.getAxisType() == AxisType.GeoX) { // kludge - what if there's multiple ones?
          Variable v = axis.getOriginalVariable(); // LOOK why original variable ?
          units = v.getUnitsString();
          break;
        }
      }
    }

    if (units != null) {
      try {
        SimpleUnit unit = SimpleUnit.factoryWithExceptions(units);
        factor =  unit.convertTo(1.0, SimpleUnit.kmUnit);
      } catch (Exception e) {
        log.error(units + " not convertible to km");
      }
    }

    return factor;
  }
   */



}
