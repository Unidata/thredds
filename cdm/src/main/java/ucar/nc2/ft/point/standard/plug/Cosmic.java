/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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

package ucar.nc2.ft.point.standard.plug;

import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.point.standard.TableConfig;
import ucar.nc2.ft.point.standard.Table;
import ucar.nc2.ft.point.standard.TableConfigurerImpl;
import ucar.ma2.StructureDataScalar;

import java.util.*;

/**
 * Class Description.
 *
 * @author caron
 * @since Jan 26, 2009
 */
public class Cosmic extends TableConfigurerImpl {

    // :title = "WPDN data : selected by ob time : time range from 1207951200 to 1207954800";
  public boolean isMine(FeatureType wantFeatureType, NetcdfDataset ds) {
    String center = ds.findAttValueIgnoreCase(null, "center", null);
    return center != null && center.equals("UCAR/CDAAC");
  }

  public TableConfig getConfig(FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) {
    TableConfig profile = new TableConfig(Table.Type.Singleton, "profile");
    profile.featureType = FeatureType.PROFILE;

    StructureDataScalar sdata = new StructureDataScalar("profile");
    sdata.addMember("lat", "Latitude (avg)", "degrees_north", ds.readAttributeDouble(null, "lat", Double.NaN));
    sdata.addMember("lon", "Longitude (avg)", "degrees_east", ds.readAttributeDouble(null, "lon", Double.NaN));
    Date time = makeTime(ds);
    sdata.addMember("time", "Time (avg)", "seconds since 1970-01-01 00:00", time.getTime()/1000);

    profile.sdata = sdata;
    profile.lat = "lat";
    profile.lon = "lon";
    profile.time = "time";

    TableConfig obs = new TableConfig(Table.Type.Structure, "levels");
    obs.isPsuedoStructure = true;
    obs.dim = ds.findDimension("MSL_alt");
    obs.elev = "MSL_alt";

    profile.addChild(obs);

    return obs;
  }

  Date makeTime( NetcdfDataset ds) {
    int year = ds.readAttributeInteger(null, "year", 0);
    int month = ds.readAttributeInteger(null, "month", 0);
    int dayOfMonth = ds.readAttributeInteger(null, "day", 0);
    int hourOfDay = ds.readAttributeInteger(null, "hour", 0);
    int minute = ds.readAttributeInteger(null, "minute", 0);
    int second = ds.readAttributeInteger(null, "second", 0);

    Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
    cal.clear();
    cal.set(year, month, dayOfMonth, hourOfDay, minute, second);
    return cal.getTime();
  }
}
