// $Id$
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

package thredds.catalog.query;

/**
 * Implementation of a DQC select range.
 *
 * @author john caron
 * @version $Revision$ $Date$
 */

public class SelectRange extends Selector {
  private String min, max, units, resolution, selectType;
  private boolean modulo;

   /**
    * Construct from fields in XML catalog.
    * @see Selector
    */
  public SelectRange( String min, String max, String units, String modulo,
                      String resolution, String selectType) {
    this.min = min;
    this.max = max;
    this.units = units;
    this.modulo = (modulo != null) && modulo.equals("true");
    this.resolution = resolution;
    this.selectType = selectType;
  }

  public String getMin() { return min; }
  public String getMax() { return max; }
  public String getUnits() { return units; }
  public boolean isModulo() { return modulo; }
  public String getResolution() { return resolution; }
  public String getSelectType() { return selectType; }

   public boolean equals(Object o) {
     if (this == o) return true;
     if (!(o instanceof SelectRange)) return false;
     return o.hashCode() == this.hashCode();
  }

  /** Override Object.hashCode() to implement equals. */
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      if (getTitle() != null)
        result = 37*result + getTitle().hashCode();
      if (getId() != null)
        result = 37*result + getId().hashCode();
      if (getTemplate() != null)
        result = 37*result + getTemplate().hashCode();
      if (isRequired()) result++;
      if (isMultiple()) result++;

      if (getMin() != null)
        result = 37*result + getMin().hashCode();
      if (getMax() != null)
        result = 37*result + getMax().hashCode();
      if (getUnits() != null)
        result = 37*result + getUnits().hashCode();
      if (getResolution() != null)
        result = 37*result + getResolution().hashCode();
      if (getSelectType() != null)
        result = 37*result + getSelectType().hashCode();
      if (isModulo()) result++;

      hashCode = result;
    }
    return hashCode;
  }
  private volatile int hashCode = 0; // Bloch, item 8

}

/* Change History:
   $Log: SelectRange.java,v $
   Revision 1.5  2004/09/24 03:26:29  caron
   merge nj22

   Revision 1.4  2004/06/19 00:45:43  caron
   redo nested select list

   Revision 1.3  2004/06/12 02:01:10  caron
   dqc 0.3

   Revision 1.2  2004/05/21 05:57:32  caron
   release 2.0b

   Revision 1.1  2004/05/11 23:30:30  caron
   release 2.0a
 */