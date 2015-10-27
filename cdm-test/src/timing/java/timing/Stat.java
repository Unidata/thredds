// $Id: Stat.java 51 2006-07-12 17:13:13Z caron $
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

import ucar.unidata.util.Format;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;

public class Stat {
  static private HashMap<String, Stat> map = new HashMap<String, Stat>();
  static public Stat factory(String name) {
    Stat s = map.get(name);
    if (s == null) {
      s = new Stat(name, false);
      map.put(name, s);
    }
    return s;
  }

  private String name;
  private int n = 0;
  private double sum = 0.0;
  private double sumsq = 0.0;
  private ArrayList samples = null;



  public Stat() { }

  /**
   * Constructor.
   * @param keepSamples whether to keep a list of samples
   */
  public Stat( String name, boolean keepSamples) {
    this.name = name;
    if (keepSamples) samples = new ArrayList();
  }

  /**
   * Constructor with initial values.
   * @param n number of samples.
   * @param avg average of samples
   * @param std std.dev of samples
   * @param keepSamples whether to keep a list of samples
   */
  public Stat( String name, int n, double avg, double std, boolean keepSamples) {
    this( name, keepSamples);
    this.n = n;
    this.sum = n * avg;
    this.sumsq = n * (std * std  + avg * avg);
  }

  /**
   * Add a sample.
   * @param s sample value
   */
  public void sample( int s) {
    sum += s;
    sumsq += s * s;
    n++;
    if (samples != null)
      samples.add( new Double( (double) s));
  }

  /**
   * Add a sample.
   * @param s sample value
   */
  public void sample( double s) {
    sum += s;
    sumsq += s * s;
    n++;
    if (samples != null)
      samples.add( new Double( s));
  }

  /**
   * @return average of samples
   */
  public double average() {
    return (n == 0) ? 0.0 : sum/n;
  }

  /**
   * @return standard deviation of samples
   */
  public double std() {
    if (n == 0) return 0.0;
    double avg = average();
    return Math.sqrt( sumsq/n - avg * avg);
  }

  /**
   * @return name.
   */
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  /**
   * @return number of samples.
   */
  public int getN() { return n; }

  /**
   * List of the samples, if you are keeping them.
   * Note length may not be n, if you used second constructor.
   * @return List of Double, or null
   */
  public ArrayList getSamples() { return samples; }
  public void setSamples(ArrayList samples) {
    this.samples = samples;
    sum = 0.0;
    sumsq = 0.0;
    n = 0;
    for (int i=0; i<samples.size(); i++) {
      double s = ((Double)samples.get(i)).doubleValue();
      sum += s;
      sumsq += s * s;
      n++;
    }
  }

  public int[] bin(int nbins, double intv, double min) {
    if (null == samples) return null;

    int[] bins = new int[ nbins+1];

    for (int i=0; i<samples.size(); i++) {
      Double valueObject = (Double) samples.get(i);
      int bin = (int) ((valueObject.doubleValue() - min) / intv);
      if (bin < 0) bins[0]++;
      else if (bin > nbins) bins[nbins]++;
      else bins[bin]++;
    }
    return bins;
  }

  public void printBins(int nbins, double intv, double min, PrintStream out) {

    int[] bins = bin(nbins, intv, min);
    for (int i=0; i<bins.length-1; i++) {
      double edge = ((i+1)*intv+min);
      if (bins[i] > 0)
        out.println("< "+edge+" == "+bins[i]);
    }
    double edge = (nbins*intv+min);
    if (bins[nbins] > 0)
      out.println("> "+edge+" == "+bins[nbins]);
  }

  ///////////////////////////////////////
  public int[] bin(double[] intv) {
    if (null == samples) return null;

    int nbins = intv.length;
    int[] bins = new int[ nbins+1];

    for (int i=0; i<samples.size(); i++) {
      Double valueObject = (Double) samples.get(i);
      int binno = findBin( intv, valueObject.doubleValue());
      bins[binno]++;
    }
    return bins;
  }

  /*
     0: val <= intv[0]
     1: intv[0] < val <= intv[1]
     i: intv[i-1] < val <= intv[i]
     nbin-1: intv[nbin-2] < val <= intv[nbin-1]
     nbin: intv[nbin-1] < val <= intv[nbin]
     nbin+1: val > intv[nbin]
   */
  private int findBin( double[] intv, double value) {
    if (value <= intv[0]) return 0;

    for (int i=1; i<intv.length; i++)
      if (value <= intv[i]) return i;

    return intv.length;
  }

  public void printBins(double[] intv, boolean showAll, PrintStream out) {
    if (null == samples) return;

    int[] bins = bin(intv);
    int nbins = bins.length;

    out.println("Stat= "+getName());
    if (showAll || bins[0] > 0)
      out.println(" <= "+intv[0]+" == "+bins[0]);

    for (int i=1; i<nbins-1; i++) {
      if (showAll || bins[i] > 0)
        out.println("("+intv[i-1]+","+intv[i]+"] == "+bins[i]);
    }

    if (showAll || bins[nbins-1] > 0)
      out.println("> "+intv[nbins-2]+" == "+bins[nbins-1]);
  }

  public String toString() {
    return name +": avg= "+ Format.d(average(), 3)+" std=" +Format.d(std(), 3);
  }

}
