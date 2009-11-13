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

package ucar.nc2.iosp.misc;


import ucar.ma2.*;

import ucar.nc2.*;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CF;
import ucar.nc2.constants._Coordinate;

import ucar.nc2.iosp.AbstractIOServiceProvider;

import java.io.IOException;


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
        v.addAttribute(new Attribute("long_name", longName));
        if (cfName != null) {
            v.addAttribute(new Attribute(CF.STANDARD_NAME, cfName));
        }
        if (units != null) {
            v.addAttribute(new Attribute(CF.UNITS, units));
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
        ncfile.addAttribute(null,
                            new Attribute(CF.featureTypeAtt,
                                          CF.FeatureType.point.toString()));
        ncfile.addAttribute(
            null,
            new Attribute("history", "Read directly by Netcdf Java IOSP"));
    }

    /**
     * Make the sequence that holds the data.
     * @param the netcdf file to add to.
     * @return the Sequence
     */
    //protected abstract Sequence makeSequence(NetcdfFile ncfile);
}

