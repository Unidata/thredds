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

package ucar.nc2.ft.point.standard.plug;

import ucar.nc2.constants.FeatureType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.StructureDS;
import ucar.nc2.ft.point.standard.TableConfig;
import ucar.nc2.ft.point.standard.Table;
import ucar.nc2.ft.point.standard.TableConfigurerImpl;
import ucar.nc2.ft.point.standard.PointConfigXML;
import ucar.nc2.*;
import ucar.nc2.units.SimpleUnit;
import ucar.ma2.StructureDataScalar;
import ucar.ma2.DataType;

import java.util.*;
import java.io.IOException;

/**
 * Class Description.
 *
 * @author caron
 * @since Jan 26, 2009
 */
public class Cosmic extends TableConfigurerImpl {
  public boolean isMine(FeatureType wantFeatureType, NetcdfDataset ds) {
    String center = ds.findAttValueIgnoreCase(null, "center", null);
    return center != null && center.equals("UCAR/CDAAC");
  }

  public TableConfig getConfig(FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) throws IOException {
    PointConfigXML reader = new PointConfigXML();
    //return reader.readConfigXMLfromResource("resources/nj22/pointConfig/Cosmic.xml", wantFeatureType, ds, errlog);
    return reader.readConfigXML("C:\\dev\\tds\\thredds\\cdm\\src\\main\\resources\\resources\\nj22\\pointConfig\\Cosmic1.xml", wantFeatureType, ds, errlog);
  }

 /*

  private static String dimName = "MSL_alt";
  private static String dimVarName = "MSL_alt";
  private static String latVarName = "Lat";
  private static String lonVarName = "Lon";
  private static String elevVarName = "MSL_alt";

  private static String trajId = "trajectory data";
    // :title = "WPDN data : selected by ob time : time range from 1207951200 to 1207954800";

  public TableConfig getConfigOld(FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) {

     TableConfig traj = new TableConfig(Table.Type.Top, "traj");
     traj.featureType = FeatureType.TRAJECTORY;

     //StructureDataScalar sdata = new StructureDataScalar("profile");
     //sdata.addMember("lat", "Latitude (avg)", "degrees_north", ds.readAttributeDouble(null, "lat", Double.NaN));
     //sdata.addMember("lon", "Longitude (avg)", "degrees_east", ds.readAttributeDouble(null, "lon", Double.NaN));
     //Date time = makeTime(ds);
     //sdata.addMember("time", "Time (avg)", "seconds since 1970-01-01 00:00", time.getTime()/1000);

     //traj.sdata = sdata;
     //traj.lat = "lat";
     //traj.lon = "lon";
     //traj.time = "time";

     TableConfig obs = new TableConfig(Table.Type.Structure, "point");
     obs.structureType = TableConfig.StructureType.PsuedoStructure;
     obs.dimName = "MSL_alt";
     obs.lat = "Lat";
     obs.lon = "Lon";
     obs.time = "time";
     obs.elev = "MSL_alt";

     traj.addChild(obs);

     return traj;  // */


    /* TableConfig profile = new TableConfig(Table.Type.Singleton, "profile");
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

     return profile;  // */


    /* TableConfig obsTable = new TableConfig(Table.Type.Structure, "obsRecord");
    Structure obsStruct = buildStructure( ds, "obsRecord" );
    obsTable.featureType = FeatureType.TRAJECTORY;
    obsTable.structName = obsStruct.getName();
    obsTable.nestedTableName = obsStruct.getShortName();
    obsTable.lat = Evaluator.getVariableWithAttribute(obsStruct, _Coordinate.AxisType, AxisType.Lat.toString());
    obsTable.lon = Evaluator.getVariableWithAttribute(obsStruct, _Coordinate.AxisType, AxisType.Lon.toString());
    obsTable.elev = Evaluator.getVariableWithAttribute(obsStruct, _Coordinate.AxisType, AxisType.Height.toString());
    obsTable.time = Evaluator.getVariableWithAttribute(obsStruct, _Coordinate.AxisType, AxisType.Time.toString());

    return obsTable;    //
  }

    private  Structure buildStructure( NetcdfDataset ncd, String structureId )
    {
        // Check that only one dimension and that it is named "time".
        Structure trajStructure = new Structure(ncd, null, null, structureId);
        List list = ncd.getRootGroup().getDimensions();
        if ( list.size() != 1) return null;
        Dimension d = (Dimension) list.get(0);
        if ( ! d.getName().equals( dimName)) return null;


        trajStructure.setDimensions("");

        // Check that have variable time(time) with units that are udunits time
        Variable dimVar = ncd.getRootGroup().findVariable( dimVarName);
        if ( dimVar == null) return null;
        list = dimVar.getDimensions();
        if ( list.size() != 1) return null;
        d = (Dimension) list.get(0);
        if ( ! d.getName().equals( dimName)) return null;
        String units = dimVar.findAttribute( "units").getStringValue();
        if ( ! SimpleUnit.isCompatible( units, "km")) return null;

        trajStructure.addMemberVariable( dimVar);

        // Check for variable latitude(time) with units of "deg".
        Variable latVar = ncd.getRootGroup().findVariable( latVarName);
        if ( latVar == null ) return null;
        list = latVar.getDimensions();
        if ( list.size() != 1) return null;
        d = (Dimension) list.get(0);
        if ( ! d.getName().equals( dimName)) return null;
        units = latVar.findAttribute( "units").getStringValue();
        if ( ! SimpleUnit.isCompatible( units, "deg")) return null;

        trajStructure.addMemberVariable( latVar);

        // Check for variable longitude(time) with units of "deg".
        Variable lonVar = ncd.getRootGroup().findVariable( lonVarName);
        if ( lonVar == null ) return null;
        list = lonVar.getDimensions();
        if ( list.size() != 1) return null;
        d = (Dimension) list.get(0);
        if ( ! d.getName().equals( dimName)) return null;
        units = lonVar.findAttribute( "units").getStringValue();
        if ( ! SimpleUnit.isCompatible( units, "deg")) return null;

        trajStructure.addMemberVariable( lonVar);

        // Check for variable altitude(time) with units of "m".
        Variable elevVar = ncd.getRootGroup().findVariable( elevVarName);
        if ( elevVar == null) return null;
        list = elevVar.getDimensions();
        if ( list.size() != 1) return null;
        d = (Dimension) list.get(0);
        if ( ! d.getName().equals( dimName)) return null;
        units = elevVar.findAttribute( "units").getStringValue();
        if ( ! SimpleUnit.isCompatible( units, "km")) return null;

        trajStructure.addMemberVariable( elevVar);

        Variable timeVar = new Variable(ncd, null, null, "time");
        timeVar.setDataType(DataType.DOUBLE);
        timeVar.setDimensions(list);

        Date time = makeTime(ncd);
        ncd.setValues(timeVar, d.getLength(), time.getTime(), 0);
        timeVar.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));
        timeVar.addAttribute( new Attribute("units", "milliseconds since 1970-01-01 00:00 UTC"));
//        ncd.addVariable(null, timeVar);

        trajStructure.addMemberVariable( timeVar);

        for (Iterator it =
                ncd.getVariables().iterator();
                it.hasNext(); ) {
            Variable curVar = (Variable) it.next();
            if ((curVar.getRank() > 0) && !curVar.equals(dimVar)
                    && !curVar.equals(latVar)
                    && !curVar.equals(lonVar)
                    && !curVar.equals(elevVar)
                    && ((trajStructure == null)
                        ? true
                        : !curVar.equals(trajStructure))) {

                trajStructure.addMemberVariable(curVar);

            }
        }
        ncd.addVariable(null, new StructureDS(null, trajStructure)) ;
        return trajStructure;
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
  }  */
}
