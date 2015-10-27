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
package timing;

import  java.util.*;

 /**
 * Calculates the mean and stddev of a series of numbers.  Not the
 * most efficient algorithm, but fast enough.
 *
 * @author Heinz Kabutz
 */ 
 public class  Average {
   /** The set of values stored as doubles.  Autoboxed. */ 

   private ArrayList values =  new  ArrayList();

   /**
   * Add a new value to the series.  Changes the values returned by
   * mean() and stddev().
   * @param value the new value to add to the series.
   */ 
   public void  add( double  value) {
    values.add( new Double(value) );
  }

   /**
   * Calculate and return the mean of the series of numbers.
   * Throws an exception if this is called before the add() method.
   * @return the mean of all the numbers added to the series.
   * @throws IllegalStateException if no values have been added yet.
   * Otherwise we could cause a NullPointerException.
   */ 

   public double  mean() {
     int  elements = values.size();
     if  (elements == 0)  throw new  IllegalStateException("No values");
     double  sum = 0;
     for (int i = 0; i < values.size(); i++) {
       Double valo =  (Double) values.get(i);
       sum += valo.doubleValue();
     }
     return  sum / elements;
  }

   /**
   * Calculate and return the standard deviation of the series of
   * numbers.  See Stats 101 for more information...
   * Throws an exception if this is called before the add() method.
   * @return the standard deviation of numbers added to the series.
   * @throws IllegalStateException if no values have been added yet.
   * Otherwise we could cause a NullPointerException.
   */ 

   public double  stddev() {
     double  mean = mean();
     double  stddevtotal = 0;
     for (int i = 0; i < values.size(); i++) {
       Double valo =  (Double) values.get(i);
       double  dev = valo.doubleValue() - mean;
       stddevtotal += dev * dev;
     }

     return  Math.sqrt(stddevtotal / values.size());
  }

   public String toString() {
     return " avg= "+mean() + " stdev= "+stddev()+ " count= "+values.size();
   }
}