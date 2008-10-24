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

/**
 * A class that represents a BUFR descriptor from Table B. A desciptor is used
 * to decode a unit of raw data into a numeric or String value.
 */

public class DescriptorTableB implements TableBdescriptor {

   /**
    * FXY key of descriptor.
    */
   private final String fxy;

   /**
    * FXY key of descriptor.
    */
   private short id;

   /**
    * Scale of descriptor.
    */
   private int scale;

   /**
    * Reference Value of descriptor.
    */
   private int refVal;

   /**
    * Width in bit on read of descriptor.
    */
   private int width;

   /**
    * Units of descriptor.
    */
   private String units;

   /**
    * Name of descriptor.
    */
   private final String name;

   /**
    * description of descriptor.
    */
   private final String description;

   /**
    * data type of descriptor (String|float).
    */
   private boolean numeric = true;

   /**
    * is this decriptor from the WMO table.
    */
   private boolean WMO;
    /**
     * Constructor for reading in Descriptor from NCEP tableB.
     * @param fxy
     * @param scale
     * @param refVal
     * @param width
     * @param uStr
     * @param desc
     */
   public DescriptorTableB(String fxy, String scale, String refVal,
      String width, String uStr, String desc, boolean WMO ) {
      this.fxy = fxy;
      id = BufrDataDescriptionSection.getDesc( fxy );
      //System.out.println( fxy );
      this.scale = Integer.parseInt( scale.trim() );
      this.refVal = Integer.parseInt( refVal.trim() );
      this.width = Integer.parseInt( width.trim() );
      this.units = uStr.trim();

      if( units.equals( "CCITT_IA5" ) || units.equals( "CCITT IA5" ) ) { // String var
          numeric = false;
          // this.units = "";
      } /* else if( uStr.equals( "Numeric" ) ) {
          this.units = "";
      } else if( uStr.equals( "Code_table" ) || uStr.equals( "Flag_table" ) ) {
          this.units = "enum";
      } else {
          this.units = uStr;
      } */
      desc = desc.replaceFirst( "\\w+\\s+TABLE B ENTRY( - )?", "" );
      desc = desc.trim();
      this.description = desc;
      desc = desc.replaceAll( "\\s*\\(.*\\)", "" );
      desc = desc.replaceAll( "\\*", "" );
      desc = desc.replaceAll( "\\s+", "_" );
      desc = desc.replaceAll( "\\/", "_or_" );
      desc = desc.replaceFirst( "1-", "One-" );
      desc = desc.replaceFirst( "2-", "Two-" );
      this.name = desc;
      //System.out.println( "name =" + desc );
      this.WMO = WMO;
   }
    /**
     * Constructor for reading in Descriptor from the data.
     * @param f
     * @param x
     * @param y
     * @param scale
     * @param refVal
     * @param width
     * @param uStr
     * @param desc
     */
   public DescriptorTableB(String f, String x, String y, String scale, String refVal,
      String width, String uStr, String desc ) {
      // trim leading 0's from x and y
      if( x.length() == 2 && x.startsWith( "0" ) ) {
         x = x.substring( 1 );
      }
      if( y.length() == 3 && y.startsWith( "00" ) ) {
         y = y.substring( 2 );
      } else if( y.length() == 3 && y.startsWith( "0" ) ) {
         y = y.substring( 1 );
      }
      fxy = f +"-"+ x +"-"+ y;
      id = BufrDataDescriptionSection.getDesc( fxy );
      this.scale = Integer.parseInt( scale.trim() );
      this.refVal = Integer.parseInt( refVal.trim() );
      this.width = Integer.parseInt( width.trim() );
      this.units = uStr.trim();

      if( units.equals( "CCITT_IA5" ) || units.equals( "CCITT IA5" ) ) { // String var
        numeric = false;
      }

      /* uStr = uStr.trim();
      if( uStr.equals( "CCITT_IA5" ) ) { // String var
         numeric = false;
         this.units = "";
      } else { // try to make udunits type units
         if( uStr.equals( "Numeric" ) || uStr.equals( "Code_Table" ) ||
             uStr.equals( "Flag_Table" ) ) {
            this.units = "";
         } else {
            this.units = uStr;
         }
      } */

      desc = desc.replaceFirst( "\\w+\\s+TABLE B ENTRY( - )?", "" );
      desc = desc.trim();
      this.description = desc;
      desc = desc.replaceAll( "\\s*\\(.*\\)", "" );
      desc = desc.replaceAll( "\\*", "" );
      desc = desc.replaceAll( "\\s+", "_" );
      desc = desc.replaceAll( "\\/", "_or_" );
      desc = desc.replaceFirst( "1-", "One-" );
      desc = desc.replaceFirst( "2-", "Two-" );
      this.name = desc;
      //System.out.println( "name =" + desc );
      WMO = false;
   }

    /**
     * Constructor for reading in Descriptor from the data.
     * @param f
     * @param x
     * @param y
     * @param scaleSign
     * @param scale
     * @param refSign
     * @param refVal
     * @param width
     * @param uStr
     * @param desc
     */
   public DescriptorTableB(String f, String x, String y, String scaleSign,
      String scale, String refSign, String refVal, String width, 
      String uStr, String desc ) {

      // trim leading 0's from x and y
      if( x.length() == 2 && x.startsWith( "0" ) ) {
         x = x.substring( 1 );
      }
      if( y.length() == 3 && y.startsWith( "00" ) ) {
         y = y.substring( 2 );
      } else if( y.length() == 3 && y.startsWith( "0" ) ) {
         y = y.substring( 1 );
      }
      fxy = f +"-"+ x +"-"+ y;
      id = BufrDataDescriptionSection.getDesc( fxy );
      this.scale = Integer.parseInt( scale.trim() );
      if( scaleSign.startsWith( "-" ) )
         this.scale *= -1;
      this.refVal = Integer.parseInt( refVal.trim() );
      if( refSign.startsWith( "-" ) )
         this.refVal *= -1;
      this.width = Integer.parseInt( width.trim() );
      this.units = uStr.trim();

      if( units.equals( "CCITT_IA5" ) || units.equals( "CCITT IA5" ) ) { // String var
          numeric = false;
      }
      /* uStr = uStr.trim();
      // String var
      if( uStr.equals( "CCITT_IA5" ) ||  uStr.equals( "CCITT IA5" ) ) { 
         numeric = false;
         this.units = "";
      } else { // try to make udunits type units
         if( uStr.equals( "Numeric" ) || uStr.equals( "Code_Table" ) ||
             uStr.equals( "Flag_Table" ) ) {
            this.units = "";
         } else {
            this.units = uStr;
         }
      } */

      desc = desc.replaceFirst( "\\w+\\s+TABLE B ENTRY( - )?", "" );
      desc = desc.toLowerCase();
      desc = desc.trim();
      this.description = desc;
      desc = desc.replaceAll( "\\s*\\(.*\\)", "" );
      desc = desc.replaceAll( "\\*", "" );
      desc = desc.replaceAll( "\\s+", "_" );
      desc = desc.replaceAll( "\\/", "_or_" );
      desc = desc.replaceFirst( "1-", "One-" );
      desc = desc.replaceFirst( "2-", "Two-" );
      this.name = desc;
      //System.out.println( "name =" + desc );
      WMO = false;
   }
    /**
     * FXY key of descriptor.
     *
     * @return fxy
     */
   public final String getKey() {
      return fxy;
   }

   public final String getFxy() {
      return fxy;
   }

   public short getId() {
        return id;
   }

    /**
     * scale of descriptor.
     * @return scale
     */
   public final int getScale() {
      return scale;
   }

    /**
     * refVal of descriptor.
     * @return refVal
     */
   public final int getRefVal() {
      return refVal;
   }

    /**
     * width of descriptor.
     * @return width
     */
   public final int getWidth() {
      return width;
   }

    /**
     * width of descriptor.
     * @param width
     */
   public final void setWidth( int width ) {
      this.width = width;
   }

    /**
     * units of descriptor.
     * @return units
     */
   public final String getUnits() {
      return units;
   }

    /**
     * description of descriptor.
     *
     * @return description
     */
   public final String getDescription() {
      return description;
   }

    /**
     * short name of descriptor.
     *
     * @return name
     */
   public final String getName() {
      return name;
   }

    /**
     * is data type of descriptor a numeric.
     *
     * @return true or false
     */
   public final boolean isNumeric() {
      return numeric;
   }

    public boolean isWMO() {
        return WMO;
    }

    /**
     * sets units of descriptor.
     * @param units of descriptor
     */
   public final void setUnits( String units ) {
      this.units = units ;
   }


  public String toString() {
    StringBuffer sbuff = new StringBuffer();
    sbuff.append(name).append(" units=").append(units);
    if (numeric) {
      sbuff.append(" scale=").append(scale).append(" refVal=").append(refVal);
      sbuff.append(" nbits=").append(width);
    } else {
      sbuff.append(" nchars=").append(width/8);      
    }
    //sbuff.append(" isWMO=").append(WMO);
    return sbuff.toString();
  }

  public void show( Formatter out) {
    out.format( "%s units=%s ", name, units );
    if (numeric) {
      out.format( "scale=%d refVal=%d nbits=%d\n", scale, refVal, width );
    } else {
      out.format( "nchars=%d\n", width/8 );
    }
  }
}
