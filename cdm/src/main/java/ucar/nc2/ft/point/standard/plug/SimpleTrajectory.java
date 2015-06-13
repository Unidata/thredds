/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.ft.point.standard.plug;

import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.point.standard.PointConfigXML;
import ucar.nc2.ft.point.standard.TableConfig;
import ucar.nc2.ft.point.standard.TableConfigurerImpl;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.SimpleUnit;

import java.io.IOException;
import java.util.Date;
import java.util.Formatter;
import java.util.List;

/**
 * SimpleTrajectory netcdf files
 *   One trajectory, one dimension (time), per file.
 *   The EOL trajectory datasets follow this simple layout.
 *
 * @author sarms
 * @since June 4, 2015
 */

public class SimpleTrajectory extends TableConfigurerImpl {

    private static String timeDimName = "time";
    private static String timeVarName = "time";
    private static String latVarName = "latitude";
    private static String lonVarName = "longitude";
    private static String elevVarName = "altitude";

    public boolean isMine(FeatureType wantFeatureType, NetcdfDataset ds) {


        List list = ds.getRootGroup().getDimensions();

        // should have only one dimension
        if ( list.size() != 1) {return false;}

        // dimension name should be "time"
        Dimension d = (Dimension) list.get(0);
        if ( ! d.getShortName().equals( timeDimName)) {return false;}

        // Check that have variable time(time) with units that are udunits time
        Variable var = ds.getRootGroup().findVariable( timeVarName);
        if ( var == null) {return false;}
        list = var.getDimensions();
        if ( list.size() != 1) {return false;}
        d = (Dimension) list.get(0);
        if ( ! d.getShortName().equals( timeDimName)) {return false;}
        String units = var.findAttribute( "units").getStringValue();
        Date date = DateUnit.getStandardDate("0 " + units);
        if ( date == null)  {return false;}

        // Check for variable latitude(time) with units of "deg".
        var = ds.getRootGroup().findVariable( latVarName);
        if ( var == null ) {return false;}
        list = var.getDimensions();
        if ( list.size() != 1) {return false;}
        d = (Dimension) list.get(0);
        if ( ! d.getShortName().equals( timeDimName)) {return false;}
        units = var.findAttribute( "units").getStringValue();
        if ( ! SimpleUnit.isCompatible(units, "degrees_north")) {return false;}

        // Check for variable longitude(time) with units of "deg".
        var = ds.getRootGroup().findVariable( lonVarName);
        if ( var == null ) {return false;}
        list = var.getDimensions();
        if ( list.size() != 1) {return false;}
        d = (Dimension) list.get(0);
        if ( ! d.getShortName().equals( timeDimName)) {return false;}
        units = var.findAttribute( "units").getStringValue();
        if ( ! SimpleUnit.isCompatible( units, "degrees_east")) {return false;}


        // Check for variable altitude(time) with units of "m".
        var = ds.getRootGroup().findVariable( elevVarName);
        if ( var == null) {return false;}
        list = var.getDimensions();
        if ( list.size() != 1) {return false;}
        d = (Dimension) list.get(0);
        if ( ! d.getShortName().equals( timeDimName)) {return false;}
        units = var.findAttribute( "units").getStringValue();
        if ( ! SimpleUnit.isCompatible( units, "meters")) {return false;}

        return true;

    }

    public TableConfig getConfig(FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) throws IOException {
        PointConfigXML reader = new PointConfigXML();
        return reader.readConfigXMLfromResource("resources/nj22/pointConfig/SimpleTrajectory.xml", wantFeatureType, ds, errlog);
    }
}
