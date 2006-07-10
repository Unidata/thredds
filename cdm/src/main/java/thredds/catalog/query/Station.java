// $Id: Station.java,v 1.5 2004/09/24 03:26:30 caron Exp $
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
 * Implementation of a DQC  station element. This extends Choice with a location.
 *
 * @author john caron
 * @version $Revision: 1.5 $ $Date: 2004/09/24 03:26:30 $
 */

public class Station extends ListChoice {
  private Location location;

   /**
    * Construct from fields in XML catalog.
    * @see Choice
    */
  public Station( Selector parent, String name, String value, String description) {
    super( parent, name, value, description);
  }

  public void setLocation(Location location) { this.location = location; }
  public Location getLocation() { return location; }

   public boolean equals(Object o) {
     if (this == o) return true;
     if (!(o instanceof Station )) return false;
     return o.hashCode() == this.hashCode();
  }

  /** Override Object.hashCode() to implement equals. */
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      result = 37*result + getName().hashCode();
      result = 37*result + getValue().hashCode();
      if (getTemplate() != null)
        result = 37*result + getTemplate().hashCode();
      if (getDescription() != null)
        result = 37*result + getDescription().hashCode();

      result = 37*result + getLocation().hashCode();
      hashCode = result;
    }
    return hashCode;
  }
  private volatile int hashCode = 0; // Bloch, item 8

}

/* Change History:
   $Log: Station.java,v $
   Revision 1.5  2004/09/24 03:26:30  caron
   merge nj22

   Revision 1.4  2004/06/18 21:54:26  caron
   update dqc 0.3

   Revision 1.3  2004/05/11 23:30:31  caron
   release 2.0a

   Revision 1.2  2004/02/20 00:49:52  caron
   1.3 changes

 */