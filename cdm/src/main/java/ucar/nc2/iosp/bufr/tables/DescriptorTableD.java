/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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
package ucar.nc2.iosp.bufr.tables;

import ucar.nc2.iosp.bufr.BufrDataDescriptionSection;

import java.util.Formatter;
import java.util.List;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: rkambic
 * Date: Sep 26, 2008
 * Time: 9:22:39 AM
 * To change this template use File | Settings | File Templates.
 */
public class DescriptorTableD implements TableDdescriptor {

  private String name;
  private String fxy;
  private short id;
  private List<String>elements;
  private List<Short>ids;
  private boolean WMO;

    public DescriptorTableD(String name, String fxy, List<String> elements, boolean WMO) {
        this.name = name;
        this.fxy = fxy;
        this.elements = elements;
        this.WMO = WMO;
        id = BufrDataDescriptionSection.getDesc( fxy );
        ids = new ArrayList( elements.size() );
        for( String elem : elements)
          ids.add( BufrDataDescriptionSection.getDesc( elem ) );
    }

    public String getName() {
        return name;
    }

    public String getFxy() {
        return fxy;
    }

    public short getId() {
        return id;
    }

    public List<String> getElements() {
        return elements;
    }

    public List<String> getDescList() {
        return elements;
    }

    public List<Short> getIds() {
        return ids;
    }

    public List<Short> getIdList() {
        return ids;
    }

    public boolean isWMO() {
        return WMO;
    }

    public void show( Formatter out){};

    //List<TableBdescriptor> getDescList(){ return null;}
}
