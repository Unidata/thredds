// $Id:PointObsDatasetImpl.java 51 2006-07-12 17:13:13Z caron $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.dt.point;

import ucar.nc2.dt.*;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
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
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */

abstract public class PointObsDatasetImpl extends TypedDatasetImpl implements PointObsDataset {
  protected static SimpleUnit meterUnit = SimpleUnit.factory("m");

  /**
   * Get conversion factor for this unit into meters.
   * @param unitsString unit you want to convert
   * @return conversion factor : value in meters = factor * (value in units)
   * @throws Exception if not valid unit, or not convertible to meters
   */
  protected static double getMetersConversionFactor( String unitsString) throws Exception {
    SimpleUnit unit = SimpleUnit.factoryWithExceptions(unitsString);
    return unit.convertTo(1.0, meterUnit);
  }

  protected DateUnit timeUnit;

  public PointObsDatasetImpl() {
    super();
  }

  public PointObsDatasetImpl(String title, String description, String location) {
    super(title, description, location);
  }

  public PointObsDatasetImpl(NetcdfFile ncfile) {
    super( ncfile);
  }

  protected abstract void setTimeUnits(); // reminder for subclasses to set this

  /////////////////////////////////

  public String getDetailInfo() {
    StringBuffer sbuff = new StringBuffer();
    sbuff.append("PointObsDataset\n");
    sbuff.append("  adapter   = "+getClass().getName()+"\n");
    sbuff.append("  timeUnit  = "+getTimeUnits()+"\n");
    sbuff.append("  dataClass = "+getDataClass()+"\n");
    sbuff.append("  dataCount = "+getDataCount()+"\n");
    sbuff.append(super.getDetailInfo());

    return sbuff.toString();
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
      return sdata.getScalarFloat(timeVar.getShortName());
    }
  }
  
}

