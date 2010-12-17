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



package ucar.unidata.geoloc;


//import net.jcip.annotations.Immutable;

import java.util.Formatter;


/**
 * Defines the shape of the earth ellipsoid.
 * @author Russ Rew
 */
public class Earth {

    /** _more_ */
    private static final double earthRadius = 6371229.;  // canonical radius of the spherical earth


    /**
     * eccentricity
     */
    private double eccentricity;

    /**
     * eccentricity squared
     */
    private double eccentricitySquared;

    /**
     * equatorial radius (semimajor axis)
     */
    private double equatorRadius;

    /**
     * polar radius (semiminor axis)
     */
    private double poleRadius;

    /**
     * flattening
     */
    private double flattening;

    /** _more_          */
    private String name = "earth";


    /**
     * Spherical earth with canonical radius.
     */
    public Earth() {
        this(earthRadius);
    }


    /**
     * Create a spherical earth.
     * @param radius radius of spherical earth.
     */
    public Earth(double radius) {
        this.equatorRadius       = radius;
        this.poleRadius          = radius;
        this.flattening          = 0.0;
        this.eccentricity        = 1.0;
        this.eccentricitySquared = 1.0;
    }

    /**
     * Create an ellipsoidal earth.
     * The reciprocalFlattening is used if not zero, else the poleRadius is used.
     *
     * @param equatorRadius equatorial radius (semimajor axis) in meters, must be specified
     * @param poleRadius  polar radius (semiminor axis) in meters
     * @param reciprocalFlattening inverse flattening = 1/flattening = a / (a-b)
     */
    public Earth(double equatorRadius, double poleRadius,
                 double reciprocalFlattening) {

        this(equatorRadius, poleRadius, reciprocalFlattening, "earth");
    }



    /**
     * _more_
     *
     * @param equatorRadius _more_
     * @param poleRadius _more_
     * @param reciprocalFlattening _more_
     * @param name _more_
     */
    public Earth(double equatorRadius, double poleRadius,
                 double reciprocalFlattening, String name) {
        this.equatorRadius = equatorRadius;
        this.poleRadius    = poleRadius;
        this.name          = name;
        if (reciprocalFlattening != 0) {
            flattening          = 1.0 / reciprocalFlattening;
            eccentricitySquared = 2 * flattening - flattening * flattening;
            poleRadius = equatorRadius * Math.sqrt(1.0 - eccentricitySquared);
        } else {
            eccentricitySquared = 1.0
                                  - (poleRadius * poleRadius)
                                    / (equatorRadius * equatorRadius);
            flattening = 1.0 - poleRadius / equatorRadius;
        }
        eccentricity = Math.sqrt(eccentricitySquared);
    }


    /**
     * Get radius of spherical earth, in meters
     *
     * @return radius of spherical earth in meters
     */
    public static double getRadius() {
        return earthRadius;
    }



    /**
     * Specify earth with equatorial and polar radius.
     *
     * @param a semimajor (equatorial) radius, in meters.
     * @param b semiminor (polar) radius, in meters.
     *
     * Earth(double a, double b) {
     * this.equatorRadius = a;
     * this.poleRadius = b;
     * eccentricitySquared = 1.0 - (b * b) / (a * a);
     * flattening = 1.0 - b / a;
     * }
     *
     *
     * Specify earth with semimajor radius a, and flattening f.
     * b = a(1-flattening)
     *
     * @param a    semimajor (equatorial) radius, in meters.
     * @param f    flattening.
     * @param fake fake
     *
     * Earth(double a, double f, boolean fake) {
     * this.equatorRadius = a;
     * this.flattening = flattening;
     * poleRadius = a * (1.0 - flattening);
     * eccentricitySquared = 1.0 - (poleRadius * poleRadius) / (a * a);
     * }
     *
     * @return _more_
     */





    /**
     * Get the equatorial radius (semimajor axis) of the earth, in meters.
     *
     * @return equatorial radius (semimajor axis) in meters
     */
    public double getMajor() {
        return equatorRadius;
    }

    /**
     * Get the polar radius (semiminor axis) of the earth, in meters.
     *
     * @return polar radius (semiminor axis) in meters
     */
    public double getMinor() {
        return poleRadius;
    }



    /**
     *  Set the Name property.
     *
     *  @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }

    /**
     *  Get the Name property.
     *
     *  @return The Name
     */
    public String getName() {
        return name;
    }

    /**
     *  Set the Eccentricity property.
     *
     *  @param value The new value for Eccentricity
     */
    public void setEccentricity(double value) {
        eccentricity = value;
    }

    /**
     *  Get the Eccentricity property.
     *
     *  @return The Eccentricity
     */
    public double getEccentricity() {
        return eccentricity;
    }

    /**
     *  Set the EccentricitySquared property.
     *
     *  @param value The new value for EccentricitySquared
     */
    public void setEccentricitySquared(double value) {
        eccentricitySquared = value;
    }

    /**
     *  Get the EccentricitySquared property.
     *
     *  @return The EccentricitySquared
     */
    public double getEccentricitySquared() {
        return eccentricitySquared;
    }

    /**
     *  Set the EquatorRadius property.
     *
     *  @param value The new value for EquatorRadius
     */
    public void setEquatorRadius(double value) {
        equatorRadius = value;
    }

    /**
     *  Get the EquatorRadius property.
     *
     *  @return The EquatorRadius
     */
    public double getEquatorRadius() {
        return equatorRadius;
    }


    /**
     *  Set the PoleRadius property.
     *
     *  @param value The new value for PoleRadius
     */
    public void setPoleRadius(double value) {
        poleRadius = value;
    }

    /**
     *  Get the PoleRadius property.
     *
     *  @return The PoleRadius
     */
    public double getPoleRadius() {
        return poleRadius;
    }

    /**
     *  Set the Flattening property.
     *
     *  @param value The new value for Flattening
     */
    public void setFlattening(double value) {
        flattening = value;
    }

    /**
     *  Get the Flattening property.
     *
     *  @return The Flattening
     */
    public double getFlattening() {
        return flattening;
    }



    /**
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ( !(o instanceof Earth)) {
            return false;
        }

        Earth oo = (Earth) o;
        return (this.getMajor() == oo.getMajor())
               && (this.getMinor() == oo.getMinor());
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        Formatter ff = new Formatter();
        ff.format("equatorRadius=%f inverseFlattening=%f", equatorRadius,
                  (1.0 / flattening));
        return ff.toString();
    }

}

