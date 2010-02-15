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


package ucar.unidata.geoloc.ogc;


import ucar.nc2.units.SimpleUnit;

import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.projection.*;

import java.text.ParseException;


/**
 * This class parses OGC WKT Spatial Reference Text.
 * @author Barrodale Computing Services, Ltd. (Eric Davies)
 * @author Unidata Java Development Team
 */
public class WKTParser {

    /*
     * geogcs info
     */

    /** geo coord sys name */
    private String geogcsName;

    /** datum name */
    private String datumName;

    /** spheriod name */
    private String spheroidName;

    /** major axis, inverse minor axis */
    private double majorAxis, inverseMinor;

    /** primeMeridian name */
    private String primeMeridianName;

    /** primeMeridian name */
    private double primeMeridianValue;

    /** geographic unit name */
    private String geogUnitName;

    /** geographic unit value */
    private double geogUnitValue;

    /** is this a projection */
    private boolean isAProjection;

    /** projection name */
    private String projName;

    /** projection type */
    private String projectionType;

    /** projection parameters */
    private java.util.HashMap parameters = new java.util.HashMap();

    /** projection unit name */
    private String projUnitName;

    /** projection unit value */
    private double projUnitValue;

    /** parse position */
    private int position = 0;

    /** the reader */
    java.io.StringReader reader;

    /**
     * Creates a new instance of WKTParser. If the constructor
     * succeeds, the spatial reference text was successfully parsed.
     * @param srtext The spatial reference text to be parsed.
     *               Geocentric coordinate text is not currently supported.
     * @throws ParseException A ParseException is thrown
     *                   if the spatial reference text could not be parsed.
     */
    public WKTParser(String srtext) throws ParseException {
        reader = new java.io.StringReader(srtext);

        if (srtext.startsWith("PROJCS")) {
            isAProjection = true;
            parseProjcs();
        } else {
            isAProjection = false;
            parseGeogcs();
        }
    }

    /**
     * Peek ahead
     *
     * @return the char
     *
     * @throws ParseException problem parsing
     */
    private char peek() throws ParseException {
        try {
            reader.mark(10);
            int aChar = reader.read();
            reader.reset();
            if (aChar < 0) {
                return (char) 0;
            } else {
                return (char) aChar;
            }
        } catch (java.io.IOException e1) {
            throw new ParseException("Strange io error " + e1, position);
        }
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws ParseException _more_
     */
    private char getChar() throws ParseException {
        try {
            int val = reader.read();
            position++;
            if (val < 0) {
                throw new ParseException("unexpected eof of srtext",
                                         position);
            }
            return (char) val;
        } catch (java.io.IOException e1) {
            throw new ParseException(e1.toString(), position);
        }

    }

    /**
     * _more_
     *
     * @param literal _more_
     *
     * @throws ParseException _more_
     */
    private void eatLiteral(String literal) throws ParseException {
        int n = literal.length();
        for (int i = 0; i < n; i++) {
            char v = getChar();
            if (v != literal.charAt(i)) {
                throw new ParseException("bad srtext", position);
            }
        }
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws ParseException _more_
     */
    private double eatReal() throws ParseException {
        StringBuffer b = new StringBuffer();
        for (;;) {
            char t = peek();
            if (Character.isDigit(t) || (t == 'e') || (t == 'E')
                    || (t == '.') || (t == '-') || (t == '+')) {
                b.append(getChar());
            } else {
                break;
            }
        }
        try {
            return Double.parseDouble(b.toString());
        } catch (NumberFormatException e1) {
            throw new ParseException("bad number" + e1, position);
        }
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws ParseException _more_
     */
    private String eatString() throws ParseException {
        StringBuffer b = new StringBuffer();
        if (getChar() != '"') {
            throw new ParseException("expected string", position);
        }
        for (;;) {
            char t = getChar();
            if (t == '"') {
                break;
            }
            b.append(t);
        }
        return b.toString();
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws ParseException _more_
     */
    private String eatTerm() throws ParseException {
        StringBuffer b = new StringBuffer();
        for (;;) {
            char val = peek();
            if ( !Character.isJavaIdentifierPart(val)) {
                break;
            }
            b.append(getChar());
        }
        return b.toString();
    }

    /**
     * _more_
     *
     * @throws ParseException _more_
     */
    private void eatComma() throws ParseException {
        if (getChar() != ',') {
            throw new ParseException("expected comma", position);
        }
    }

    /**
     * _more_
     *
     * @throws ParseException _more_
     */
    private void eatOpenBrace() throws ParseException {
        if (getChar() != '[') {
            throw new ParseException("expected [", position);
        }
    }

    /**
     * _more_
     *
     * @throws ParseException _more_
     */
    private void eatCloseBrace() throws ParseException {
        if (getChar() != ']') {
            throw new ParseException("expected ]", position);
        }
    }

    /**
     * _more_
     *
     * @throws ParseException _more_
     */
    private void parseProjcs() throws ParseException {
        eatLiteral("PROJCS[");
        projName = eatString();
        eatComma();
        parseGeogcs();
        for (;;) {
            char next = getChar();
            if (next == ']') {
                break;
            } else if (next != ',') {
                throw new ParseException("expected , or ]", position);
            } else {
                String term = eatTerm();
                if ("PARAMETER".equals(term)) {
                    eatParameter();
                } else if ("UNIT".equals(term)) {
                    eatProjcsUnit();
                } else if ("PROJECTION".equals(term)) {
                    eatProjectionType();
                }
            }
        }
    }


    /**
     * _more_
     *
     * @throws ParseException _more_
     */
    private void eatParameter() throws ParseException {
        eatOpenBrace();
        String parameterName = eatString();
        eatComma();
        Double value = new Double(eatReal());
        eatCloseBrace();
        parameters.put(parameterName.toLowerCase(), value);
    }


    /**
     * _more_
     *
     * @throws ParseException _more_
     */
    private void eatProjcsUnit() throws ParseException {
        eatOpenBrace();
        projUnitName = eatString();
        eatComma();
        projUnitValue = eatReal();
        eatCloseBrace();
    }


    /**
     * _more_
     *
     * @throws ParseException _more_
     */
    private void eatProjectionType() throws ParseException {
        eatOpenBrace();
        projectionType = eatString();
        eatCloseBrace();
    }

    /**
     * _more_
     *
     * @throws ParseException _more_
     */
    private void parseGeogcs() throws ParseException {
        eatLiteral("GEOGCS[");
        geogcsName = eatString();
        for (;;) {
            char t = getChar();
            if (t == ']') {
                break;
            } else if (t != ',') {
                throw new ParseException("expected , or ]", position);
            } else {
                String term = eatTerm();
                if ("DATUM".equals(term)) {
                    eatDatum();
                } else if ("PRIMEM".equals(term)) {
                    eatPrimem();
                } else if ("UNIT".equals(term)) {
                    eatUnit();
                }
            }
        }
    }


    /**
     * _more_
     *
     * @throws ParseException _more_
     */
    private void eatDatum() throws ParseException {
        eatOpenBrace();
        datumName = eatString();
        eatComma();
        eatSpheroid();
        eatCloseBrace();
    }


    /**
     * _more_
     *
     * @throws ParseException _more_
     */
    private void eatPrimem() throws ParseException {
        eatOpenBrace();
        primeMeridianName = eatString();
        eatComma();
        primeMeridianValue = eatReal();
        eatCloseBrace();
    }

    /**
     * _more_
     *
     * @throws ParseException _more_
     */
    private void eatSpheroid() throws ParseException {
        eatLiteral("SPHEROID");
        eatOpenBrace();
        spheroidName = eatString();
        eatComma();
        majorAxis = eatReal();
        eatComma();
        inverseMinor = eatReal();
        eatCloseBrace();
    }

    /**
     * _more_
     *
     * @throws ParseException _more_
     */
    private void eatUnit() throws ParseException {
        eatOpenBrace();
        geogUnitName = eatString();
        eatComma();
        geogUnitValue = eatReal();
        eatCloseBrace();
    }


    /**
     * Get the name of the geographic coordinate system.
     * @return the name.
     */
    public String getGeogcsName() {
        return geogcsName;
    }

    /**
     * Get the datum name. Note that the datum name itself implies information
     * not found in the spheroid.
     * @return The name of the datum.
     */
    public String getDatumName() {
        return datumName;
    }

    /**
     * Get the name of the spheroid.
     * @return The name of the spheroid.
     */
    public String getSpheroidName() {
        return spheroidName;
    }

    /**
     * Get the major axis of the spheroid.
     * @return The major axis of the spheroid, in meters.
     */
    public double getMajorAxis() {
        return majorAxis;
    }


    /**
     * Get the inverse flattening.
     * @return The inverse flattening. Note that this is unitless.
     */
    public double getInverseFlattening() {
        return inverseMinor;
    }


    /**
     * Get the name of the prime meridian.
     * @return the name of the prime meridian. Usually "Greenwich".
     */
    public String getPrimeMeridianName() {
        return primeMeridianName;
    }


    /**
     * Return the value of prime meridian.
     * @return The longitude of the prime meridian, usually 0.
     */
    public double getPrimeMeridianValue() {
        return primeMeridianValue;
    }


    /**
     * Get the name of the unit that the prime meridian is expressed in.
     * @return Tje name of the unit that the prime meridian is expressed in. Usually "Degree".
     */
    public String getGeogUnitName() {
        return geogUnitName;
    }


    /**
     * Get the size of the unit that the prime meridian is expressed in.
     * @return The conversion from the prime meridian units to radians.
     */
    public double getGeogUnitValue() {
        return geogUnitValue;
    }


    /**
     * Inquire if a particular projection parameter is present.
     * @param name The name of the parameter. Case is ignored.
     * @return True if the parameter is present.
     */
    public boolean hasParameter(String name) {
        return (parameters.get(name.toLowerCase()) != null);
    }


    /**
     * Get the value of the projection parameter. An IllegalArgument exception
     * is thrown if the parameter is not found.
     * @param name The name of the parameter. Case is ignored.
     * @return The value of the parameter, as a double.
     */
    public double getParameter(String name) {
        Double val = (Double) parameters.get(name.toLowerCase());
        if (val == null) {
            throw new IllegalArgumentException("no parameter called " + name);
        }
        return val.doubleValue();
    }

    /**
     * Determine if the spatial reference text defines a planar projection,
     * as opposed to a Geographic coordinate system.
     * @return True if the spatial reference text defines a planar projection system.
     */
    public boolean isPlanarProjection() {
        return isAProjection;
    }

    /**
     * Get the name of the projection.
     * @return The name of the projection.
     */
    public String getProjName() {
        return projName;
    }

    /**
     * Get the name of the type of projection.
     * @return Returns the name of the type of the projection. For example,Transverse_Mercator.
     * Returns null for geographic coordinate systems.
     */
    public String getProjectionType() {
        return projectionType;
    }


    /**
     * Get the name of the projection unit.
     * @return Get the name of the projection unit. Usually "Meter".
     */
    public String getProjUnitName() {
        return projUnitName;
    }

    /**
     * Get the projection unit value.
     * @return The size of the projection unit, in meters.
     */
    public double getProjUnitValue() {
        return projUnitValue;
    }


    /**
     * Convert OGC spatial reference WKT to a ProjectionImpl.
     * An IllegalArgumentException may be thrown if a parameter is missing.
     * @param srp The parsed OGC WKT spatial reference text.
     * @throws java.text.ParseException If the OGIS spatial reference text was not parseable.
     * @return The ProjectionImpl class.
     */
    public static ProjectionImpl convertWKTToProjection(WKTParser srp) {
            if ( !srp.isPlanarProjection()) {
                return new ucar.unidata.geoloc.projection.LatLonProjection();
            } else {
                String         projectionType = srp.getProjectionType();
                double         falseEasting   = 0;
                double         falseNorthing  = 0;
                ProjectionImpl proj           = null;
                if (srp.hasParameter("False_Easting")) {
                    falseEasting = srp.getParameter("False_Easting");
                }
                if (srp.hasParameter("False_Northing")) {
                    falseNorthing = srp.getParameter("False_Northing");
                }
                if ((falseEasting != 0.0) || (falseNorthing != 0.0)) {
                    double scalef = 1.0;
                    if (srp.getProjUnitName() != null) {
                        try {
                            SimpleUnit unit =
                                SimpleUnit.factoryWithExceptions(
                                    srp.getProjUnitName());
                            scalef = unit.convertTo(srp.getProjUnitValue(),
                                    SimpleUnit.kmUnit);
                        } catch (Exception e) {
                            System.out.println(srp.getProjUnitValue() + " "
                                    + srp.getProjUnitName()
                                    + " not convertible to km");
                        }
                    }
                    falseEasting  *= scalef;
                    falseNorthing *= scalef;
                }

                if ("Transverse_Mercator".equals(projectionType)) {
                    double lat0       =
                        srp.getParameter("Latitude_Of_Origin");
                    double scale      = srp.getParameter("Scale_Factor");
                    double tangentLon = srp.getParameter("Central_Meridian");
                    proj = new TransverseMercator(lat0, tangentLon, scale,
                            falseEasting, falseNorthing);
                } else if ("Lambert_Conformal_Conic".equals(projectionType)) {
                    double lon0 = srp.getParameter("Central_Meridian");
                    double par1 = srp.getParameter("Standard_Parallel_1");
                    double par2 = par1;
                    if (srp.hasParameter("Standard_Parallel_2")) {
                        par2 = srp.getParameter("Standard_Parallel_2");
                    }
                    double lat0 = srp.getParameter("Latitude_Of_Origin");
                    return new LambertConformal(lat0, lon0, par1, par2,
                            falseEasting, falseNorthing);
                } else if ("Albers".equals(projectionType)) {
                    double lon0 = srp.getParameter("Central_Meridian");
                    double par1 = srp.getParameter("Standard_Parallel_1");
                    double par2 = par1;
                    if (srp.hasParameter("Standard_Parallel_2")) {
                        par2 = srp.getParameter("Standard_Parallel_2");
                    }
                    double lat0 = srp.getParameter("Latitude_Of_Origin");
                    return new AlbersEqualArea(lat0, lon0, par1, par2,
                            falseEasting, falseNorthing);
                } else if ("Stereographic".equals(projectionType)) {
                    double lont  = srp.getParameter("Central_Meridian");
                    double scale = srp.getParameter("Scale_Factor");
                    double latt  = srp.getParameter("Latitude_Of_Origin");
                    return new Stereographic(latt, lont, scale, falseEasting,
                                             falseNorthing);
                } else if ("Mercator".equals(projectionType)) {
                    double lat0 = srp.getParameter("Latitude_Of_Origin");
                    double lon0 = srp.getParameter("Central_Meridian");
                    proj = new Mercator(lon0, lat0, falseEasting,
                                        falseNorthing);
                } else if ("Universal_Transverse_Mercator".equals(
                        projectionType)) {
                    //throw new java.text.ParseException(
                    //    "UTM adapter not implemented yet", 0);
                }
                return proj;
            }
            
    }
}
