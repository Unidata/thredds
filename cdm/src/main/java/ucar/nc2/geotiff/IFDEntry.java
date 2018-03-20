/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    IFDEntry ifdEntry = (IFDEntry) o;

    if (count != ifdEntry.count) return false;
    if (tag != null ? !tag.equals(ifdEntry.tag) : ifdEntry.tag != null) return false;
    if (type != null ? !type.equals(ifdEntry.type) : ifdEntry.type != null) return false;
    if (!Arrays.equals(value, ifdEntry.value)) return false;
    if (!Arrays.equals(valueD, ifdEntry.valueD)) return false;
    if (valueS != null ? !valueS.equals(ifdEntry.valueS) : ifdEntry.valueS != null) return false;
    return !(geokeys != null ? !geokeys.equals(ifdEntry.geokeys) : ifdEntry.geokeys != null);

  }

  @Override
  public int hashCode() {
    int result = tag != null ? tag.hashCode() : 0;
    result = 31 * result + (type != null ? type.hashCode() : 0);
    result = 31 * result + count;
    result = 31 * result + (value != null ? Arrays.hashCode(value) : 0);
    result = 31 * result + (valueD != null ? Arrays.hashCode(valueD) : 0);
    result = 31 * result + (valueS != null ? valueS.hashCode() : 0);
    result = 31 * result + (geokeys != null ? geokeys.hashCode() : 0);
    return result;
  }
}

