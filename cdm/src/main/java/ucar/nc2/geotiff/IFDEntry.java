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
package ucar.nc2.geotiff;

import ucar.nc2.constants.CDM;

import java.io.*;
import java.util.*;

/**
 *
 * @author caron
 */
class IFDEntry implements Comparable {
  protected Tag tag;
  protected FieldType type;
  protected int count;
  protected int[] value;
  protected double[] valueD;
  protected String valueS;

  protected List<GeoKey> geokeys = null;

  IFDEntry( Tag tag, FieldType type) {
    this.tag = tag;
    this.type = type;
    this.count = 1;
  }

  IFDEntry( Tag tag, FieldType type, int count) {
    this.tag = tag;
    this.type = type;
    this.count = count;
  }

  public IFDEntry setValue(int v) {
    this.value = new int[1];
    this.value[0] = v;
    return this;
  }

  public IFDEntry setValue(int n, int d) {
    this.value = new int[2];
    this.value[0] = n;
    this.value[1] = d;
    return this;
  }

  public IFDEntry setValue(int n, int d, int f) {
    this.value = new int[3];
    this.value[0] = n;
    this.value[1] = d;
    this.value[2] = f;
    return this;
  }
  public IFDEntry setValue( int[] v) {
    this.count = v.length;
    value = v.clone();
    return this;
  }

  public IFDEntry setValue( double v) {
    this.count = 1;
    valueD = new double[1];
    valueD[0] = v;
    return this;
  }

  public IFDEntry setValue( double[] v) {
    this.count = v.length;
    valueD = v.clone();
    return this;
  }

  public IFDEntry setValue( String v) {
    this.count = v.length();
    valueS = v;
    return this;
  }

  public void addGeoKey( GeoKey geokey) {
    if (geokeys == null)
      geokeys = new ArrayList<>();
    geokeys.add( geokey);
  }

  public int compareTo( Object o) {
    return tag.compareTo( ((IFDEntry)o).tag);
  }


  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("IFDEntry{");
    sb.append("tag=").append(tag);
    sb.append(", type=").append(type);
    sb.append(", count=").append(count);

    if ((type == FieldType.DOUBLE) || (type == FieldType.FLOAT))
      sb.append(", valueD=").append(Arrays.toString(valueD));

    else if (type == FieldType.ASCII)
      sb.append(", valueS='").append(valueS).append('\'');

    else if (type == FieldType.RATIONAL) {
      for (int i = 0; i < value.length; i += 2) {
        if (i > 0) sb.append(", ");
        sb.append(value[i]).append("/").append(value[i + 1]);
      }
    }
    else
      sb.append(", value=").append(Arrays.toString(value));

    sb.append(", geokeys=").append(geokeys);
    sb.append('}');
    return sb.toString();
  }

}

