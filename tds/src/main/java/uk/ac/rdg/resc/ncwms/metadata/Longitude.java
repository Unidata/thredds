/*
 * Copyright (c) 2004 The University of Reading
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Reading, nor the names of the
 *    authors or contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package uk.ac.rdg.resc.ncwms.metadata;

/**
 * Describes a point in longitude space
 * @author  jdb
 */
public class Longitude
{
    
    private double value; // The value of this point in degrees East of Greenwich
    
    public final static boolean ROUND_UP = true;
    public final static boolean ROUND_DOWN = false;
    
    /** Creates a new instance of Longitude */
    public Longitude(double value)
    {
        this.value = constrain360(value);
    }
    
    /**
     * @return the value of this point in degrees East of Greenwich.  Will be
     * between 0 and 360 degrees
     */
    public double getValue()
    {
        return this.value;
    }
    
    /**
     * @return String representation of this point
     */
    public String toString()
    {
        return "" + this.value;
    }
    
    /**
     * Gets the clockwise distance between this point and another one
     * @param endpoint The end point
     * @return The clockwise distance between the two points.  This will always
     *         be between 0 and 360 degrees
     */
    public double getClockwiseDistanceTo(Longitude endpoint)
    {
        double distance = endpoint.getValue() - this.getValue();
        if (distance < 0.0)
        {
            distance += 360.0;
        }
        return distance;
    }
    
    /**
     * Gets the clockwise distance between this point and another one
     * @param endpoint The end point
     * @return The clockwise distance between the two points.  This will always
     *         be between 0 and 360 degrees
     */
    public double getClockwiseDistanceTo(double endpoint)
    {
        return this.getClockwiseDistanceTo(new Longitude(endpoint));
    }
    
    /**
     * Tests to see if this Longitude value is between two others
     * @param v1 The first bracketing longitude value
     * @param v2 The second bracketing longitude value
     * @return true if this Longitude value is between the bracketing values, or
     * is equal to one of them
     */
    public boolean isBetween(double v1, double v2)
    {
        Longitude lon1 = new Longitude(v1);
        Longitude lon2 = new Longitude(v2);
        return lon1.getClockwiseDistanceTo(this) <= lon1.getClockwiseDistanceTo(lon2);
    }
    
    public boolean equals(Object obj)
    {
        if (obj instanceof Longitude)
        {
            return this.equals((Longitude)obj);
        }
        return false;
    }
    
    /**
     * Test to see if this Longitude is equal to another to within a fractional
     * tolerance of 1e-6 (i.e. 0.0001%)
     * @param other The other Longitude value
     * @return true if the two Longitudes are equal
     */
    public boolean equals(Longitude other)
    {
        double distance = this.getClockwiseDistanceTo(other);
        // Make sure the apparent distance is around 0 if the values are close
        distance = Longitude.constrain180(distance);
        // Check for equality, avoiding divide-by-zero error
        if (this.getValue() < 1.0e-6)
        {
            return (Math.abs(distance) < 1.0e-6);
        }
        double fracDiff = distance / this.getValue();
        return Math.abs(fracDiff) < 1.0e-6;
    }
    
    /**
     * Test to see if this Longitude is equal to another to within a fractional
     * tolerance of 1e-6 (i.e. 0.0001%)
     * @param other The other longitude value
     * @return true if the two Longitudes are equal
     */
    public boolean equals(double other)
    {
        return this.equals(new Longitude(other));
    }
    
    /**
     * Constrains the value to be between 0 and 360 degrees 
     */
    public static double constrain360(double input)
    {
        if (input >= 0.0)
        {
            return input % 360.0;
        }
        else
        {
            // input is negative
            double temp = input % 360.0;
            return temp + 360.0;
        }
    }
    
    /**
     * Constrains the value to be between in the range -180 &lt; val &lt;= 180
     */
    public static double constrain180(double input)
    {
        double val360 = constrain360(input);
        if (val360 <= 180)
        {
            return val360;
        }
        else
        {
            return val360 - 360.0f;
        }
    }
    
    public static void main(String[] args)
    {
        Longitude long1 = new Longitude(245.67);
        Longitude long2 = new Longitude(389.02);
        Longitude long3 = new Longitude(578.32);
        Longitude long4 = new Longitude(-156.23);
        Longitude long5 = new Longitude(-1050.0);
        Longitude long6 = new Longitude(29.02);
        Longitude long7 = new Longitude(0);
        System.out.println("Long1 (should be 245.67): " + long1);
        System.out.println("Long2 (should be 29.02): " + long2);
        System.out.println("Long3 (should be 218.32): " + long3);
        System.out.println("Long4 (should be 203.77): " + long4);
        System.out.println("Long5 (should be 30.00): " + long5);
        System.out.println("");
        System.out.println("Distance from 245.67 to 29.02 (should be 143.35): "
            + long1.getClockwiseDistanceTo(long2));        
        System.out.println("Distance from 29.02 to 245.67 (should be 216.65): "
            + long2.getClockwiseDistanceTo(long1));
        System.out.println("Test of equality (29.02 = 389.02?): " + long2.equals(long6));
        System.out.println("Test of equality (0 = 360?): " + long7.equals(360.0));
        System.out.println("Test of equality (0 = 359?): " + long7.equals(359.98999));
    }
    
}
