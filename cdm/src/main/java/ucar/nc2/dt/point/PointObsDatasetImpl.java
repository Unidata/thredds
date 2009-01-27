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
      Date date = formatter.isoDateTimeFormat(time);
      return date.getTime() / 1000.0;
    } else {
      return sdata.convertScalarFloat(timeVar.getShortName());
    }
  }
  
}

