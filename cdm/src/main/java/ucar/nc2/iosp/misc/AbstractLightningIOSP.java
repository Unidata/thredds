/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.misc;


import ucar.ma2.*;

import ucar.nc2.*;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.constants._Coordinate;

import ucar.nc2.iosp.AbstractIOServiceProvider;

/**
 * AbstractIOSP for lighting data.  Parameters for each stroke are held in
 * a structure.
 *
 * @see "http://www.unidata.ucar.edu/data/lightning.html"
 * @author dmurray
 * @since Nov 12, 2009
 */
public abstract class AbstractLightningIOSP extends AbstractIOServiceProvider {

    /** The time variable name */
    public static final String TIME = "time";

    /** The latitude variable name */
    public static final String LAT = "lat";

    /** The longitude variable name */
    public static final String LON = "lon";

    /** The stroke signal strength (amplitude) variable name */
    public static final String SIGNAL = "sgnl";

    /** The number of strokes variable name */
    public static final String MULTIPLICITY = "mult";

    /** The error ellipse major axis variable name */
    public static final String MAJOR_AXIS = "majorAxis";

    /** The error ellipse minor axis variable name */
    public static final String MINOR_AXIS = "minorAxis";

    /** The error ellipse major axis orientation variable name */
    public static final String ELLIPSE_ANGLE = "ellipseAngle";

    /** The error ellipse major axis orientation variable name */
    public static final String ECCENTRICITY = "eccent";

    /** The record dimension name */
    public static final String RECORD = "record";

    /** time unit */
    public final static String secondsSince1970 = 
         "seconds since 1970-01-01 00:00:00";

    /**
     * Make lightning variables
     * @param  ncfile      the netCDF file
     * @param  group       the group (may be null)
     * @param  seq         the sequence to add to
     * @param  name        variable name
     * @param  dataType    the data type
     * @param  dims        dimenstion names
     * @param  longName    the long_name attribute value (a description)
     * @param  cfName      the CF standard_name attribute value (may be null)
     * @param  units       the units attribute value (if null, not added)
     * @param  type        coordinate axis type units (if null, not added)
     *
     * @return the variable
     */
    protected Variable makeLightningVariable(NetcdfFile ncfile, Group group,
                                             Structure seq, String name,
                                             DataType dataType, String dims,
                                             String longName, String cfName,
                                             String units, AxisType type) {
        Variable v = new Variable(ncfile, group, seq, name);
        v.setDataType(dataType);
        v.setDimensions(dims);
        v.addAttribute(new Attribute(CDM.LONG_NAME, longName));
        if (cfName != null) {
            v.addAttribute(new Attribute(CF.STANDARD_NAME, cfName));
        }
        if (units != null) {
            v.addAttribute(new Attribute(CDM.UNITS, units));
        }
        if (type != null) {
            v.addAttribute(new Attribute(_Coordinate.AxisType,
                                         type.toString()));
        }
        return v;
    }

    /**
     * Add the global attributes.  Specific implementations should call super
     * and then add their own.
     * @param ncfile  the file to add to
     */
    protected void addLightningGlobalAttributes(NetcdfFile ncfile) {
        ncfile.addAttribute(null, new Attribute(CF.FEATURE_TYPE, CF.FeatureType.point.toString()));
        ncfile.addAttribute( null, new Attribute(CDM.HISTORY, "Read directly by Netcdf Java IOSP"));
    }

    /**
     * Make the sequence that holds the data.
     * @param the netcdf file to add to.
     * @return the Sequence
     */
    //protected abstract Sequence makeSequence(NetcdfFile ncfile);
}

