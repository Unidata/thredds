/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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

package thredds.catalog;

/**
 * Abstract class to filter datasets.
 * @see InvCatalog#filter
 *
 * @author john caron
 */

public abstract class DatasetFilter {

  /**
   * Decision function as to whether to accept this dataset or not.
   * @param d dataset to filter
   * @return 1 if pass, -1 if fail, 0 is dont know.
   */
  abstract public int accept( InvDataset d);

  /**
   * Filter a Catalog by the access service type.
   */
  static public class ByServiceType extends DatasetFilter {
    private ServiceType type;
    public ByServiceType( ServiceType type) { this.type = type; }

    public int accept( InvDataset d) {
      // check all access for any that has this servicee
      for (InvAccess a : d.getAccess()) {
         if (a.getService().getServiceType() == type) return 1;
       }

       // cant tell about DQC and resolvers !!
       for (InvAccess a : d.getAccess()) {
         if (a.getService().getServiceType() == ServiceType.QC) return 0;
         if (a.getService().getServiceType() == ServiceType.RESOLVER) return 0;
       }

      return -1;
    }
  }

  /**
   * Filter a Catalog by the dataset data type.
   */
  static public class ByDataType extends DatasetFilter {
    private DataType type;
    public ByDataType( DataType type) { this.type = type; }

    public int accept( InvDataset d) {
      if (null == d.getDataType()) return 0;
      return (d.getDataType() == type) ? 1 : -1;
    }
  }

}