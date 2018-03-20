/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dt.point;

import ucar.nc2.dt.*;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.Variable;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.CancelTask;
import ucar.ma2.StructureData;
import ucar.ma2.DataType;

import java.util.List;
import java.util.Date;
import java.io.IOException;
import java.text.ParseException;

/**
 * Superclass for implementations of PointObsDataset.
 *
 *
 * @deprecated use ucar.nc2.ft.point
 * @author caron
 */

abstract public class PointObsDatasetImpl extends TypedDatasetImpl implements PointObsDataset {

  /**
   * Get conversion factor for this unit into meters.
   * @param unitsString unit you want to convert
   * @return conversion factor : value in meters = factor * (value in units)
   * @throws Exception if not valid unit, or not convertible to meters
   */
  protected static double getMetersConversionFactor( String unitsString) throws Exception {
    SimpleUnit unit = SimpleUnit.factoryWithExceptions(unitsString);
    return unit.convertTo(1.0, SimpleUnit.meterUnit);
  }

  protected DateUnit timeUnit;

  public PointObsDatasetImpl() {
    super();
  }

  public PointObsDatasetImpl(String title, String description, String location) {
    super(title, description, location);
  }

  public PointObsDatasetImpl(NetcdfDataset ncfile) {
    super( ncfile);
  }

  protected abstract void setTimeUnits(); // reminder for subclasses to set this

  /////////////////////////////////

  public String getDetailInfo() {
    StringBuffer sbuff = new StringBuffer();
    sbuff.append("PointObsDataset\n");
    sbuff.append("  adapter   = ").append(getClass().getName()).append("\n");
    sbuff.append("  timeUnit  = ").append(getTimeUnits()).append("\n");
    sbuff.append("  dataClass = ").append(getDataClass()).append("\n");
    sbuff.append("  dataCount = ").append(getDataCount()).append("\n");
    sbuff.append(super.getDetailInfo());

    return sbuff.toString();
  }

  public FeatureType getScientificDataType() {
    return FeatureType.POINT;
  }

  public Class getDataClass() {
    return PointObsDatatype.class;
  }

  public DateUnit getTimeUnits() {
    return timeUnit;
  }

  public List getData() throws IOException {
    return getData( (CancelTask) null);
  }

  public List getData( ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException {
    return getData( boundingBox, null);
  }

  public List getData( ucar.unidata.geoloc.LatLonRect boundingBox, Date start, Date end) throws IOException {
    return getData( boundingBox, start, end, null);
  }

  protected DateFormatter formatter;
  protected double getTime(Variable timeVar, StructureData sdata) throws ParseException {
    if (timeVar == null) return 0.0;

    if ((timeVar.getDataType() == DataType.CHAR) || (timeVar.getDataType() == DataType.STRING)) {
      String time = sdata.getScalarString(timeVar.getShortName());
      if (null == formatter) formatter = new DateFormatter();
      Date date = formatter.getISODate(time);
      return date.getTime() / 1000.0;
    } else {
      return sdata.convertScalarFloat(timeVar.getShortName());
    }
  }
  
}

