// $Id: Stat.java,v 1.4 2004/09/24 03:26:36 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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
package thredds.util;

import java.io.PrintStream;
import java.util.ArrayList;

public class Stat {
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



}

/**
 * $Log: Stat.java,v $
 * Revision 1.4  2004/09/24 03:26:36  caron
 * merge nj22
 *
 * Revision 1.3  2003/01/23 21:36:10  john
 * stats
 *
 * Revision 1.2  2003/01/13 19:47:37  john
 * io,stat
 *
 * Revision 1.1  2002/12/19 23:02:19  caron
 * latest adde mods
 *
 */
