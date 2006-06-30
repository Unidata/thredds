// $Id: IFDEntry.java,v 1.1 2004/10/19 20:38:53 yuanho Exp $
/*
 * Copyright 1997-2000 Unidata Program Center/University Corporation for
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
package ucar.nc2.geotiff;

import java.util.*;


class IFDEntry implements Comparable {
  protected Tag tag;
  protected FieldType type;
  protected int count;
  protected int[] value;
  protected double[] valueD;
  protected String valueS;

  protected ArrayList geokeys = null;

  public IFDEntry( Tag tag, FieldType type) {
    this.tag = tag;
    this.type = type;
    this.count = 1;
  }

  public IFDEntry( Tag tag, FieldType type, int count) {
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

  public IFDEntry setValue( int[] v) {
    this.count = v.length;
    value = (int[]) v.clone();
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
    valueD = (double[]) v.clone();
    return this;
  }

  public IFDEntry setValue( String v) {
    this.count = v.length();
    valueS = v;
    return this;
  }

  public void addGeoKey( GeoKey geokey) {
    if (geokeys == null)
      geokeys = new ArrayList();
    geokeys.add( geokey);
  }

  public int compareTo( Object o) {
    return tag.compareTo( ((IFDEntry)o).tag);
  }

  public String toString() {
    StringBuffer sbuf  = new StringBuffer();
    sbuf.append(" tag = "+tag);
    sbuf.append(" type = "+type);
    sbuf.append(" count = "+count);
    sbuf.append(" values = ");

    if (type == FieldType.ASCII) {
      sbuf.append(valueS);

    } else if (type == FieldType.RATIONAL) {
      for (int i=0; i<2; i+=2) {
        if (i > 1) sbuf.append(", ");
        sbuf.append(value[i]+"/"+value[i+1]);
      }

    } else if ((type == FieldType.DOUBLE) || (type == FieldType.FLOAT)) {
       for (int i=0; i<count; i++)
        sbuf.append(valueD[i]+" ");

    } else {
       int n= Math.min(count, 30);
       for (int i=0; i<n; i++)
        sbuf.append(value[i]+" ");
    }

    if (geokeys != null) {
      sbuf.append("\n");
      for (int i=0; i<geokeys.size(); i++) {
        GeoKey elem = (GeoKey) geokeys.get(i);
        sbuf.append("        "+elem+"\n");
      }
    }

    return sbuf.toString();
  }

}

/* Change History:
   $Log: IFDEntry.java,v $
   Revision 1.1  2004/10/19 20:38:53  yuanho
   geotiff checkin

   Revision 1.3  2003/09/19 00:03:29  caron
   clean up geotiff javadoc for release

   Revision 1.2  2003/07/12 23:08:55  caron
   add cvs headers, trailers

*/

