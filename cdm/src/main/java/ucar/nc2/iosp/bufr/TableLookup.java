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
package ucar.nc2.iosp.bufr;

import ucar.nc2.iosp.bufr.tables.*;

import java.util.*;
import java.io.IOException;

/**
 * Encapsolates lookup into the BUFR Tables.
 *
 * @author caron
 * modified by rkambic
 * @since Jul 14, 2008
 */
public final class TableLookup {

  public enum Mode {wmoOnly,  // wmo entries only found from wmo table
    wmoLocal,                 // if wmo entries not found in  wmo table, look in local table
    localOverride};            // look in local first, then wmo

  private static TableADataCategory tablelookup;
  private static TableADataCategory wmoTableA;
  private static TableB wmoTableB;
  private static TableD wmoTableD;
  private static String wmoTableName;
  static {
    try {
      tablelookup = BufrTables.getTableA( "tablelookup.txt", false);
      // get wmo tables
      wmoTableName = tablelookup.getDataCategory( (short) 0);
      wmoTableA = BufrTables.getTableA(wmoTableName + "-A", true);
      wmoTableB = BufrTables.getTableB(wmoTableName + "-B", true);
      wmoTableD = BufrTables.getTableD(wmoTableName + "-D", true);
    } catch( IOException ioe ) {
    }
  }

  private final String localTableName;
  //private final TableA localTableA;
  private final TableB localTableB;
  private final TableD localTableD;

  public Mode mode = Mode.wmoOnly;
  //public Mode mode = Mode.wmoLocal;
  //public Mode mode = Mode.localOverride;
  static private final boolean showErrors = false;

  public TableLookup(BufrIndicatorSection is, BufrIdentificationSection ids) throws IOException {

    // check tablelookup for special local table
    // create key from category and possilbly center id
    localTableName = tablelookup.getDataCategory( makeLookupKey( ids.getCategory(), ids.getCenter_id() ));
    //String ltn = tablelookup.getDataCategory( makeLookupKey( ids.getCategory(), ids.getCenter_id() ));
    //localTableName = ltn;
    //if( ltn == null ) {
    if( localTableName == null ) {
      //this.localTableA = wmoTableA();
      this.localTableB = wmoTableB;
      this.localTableD = wmoTableD;
      return;
      /*
      //int local_table_version = ids.getLocalTableVersion();
      int master_table = ids.getMasterTableId();
      int master_table_version = ids.getMasterTableVersion();
      //int edition = is.getBufrEdition();

      //ie local table name = "B3M-000-009-ABD.diff";
      String mt;
      // implemented cuz data providers don't follow table editions correctly
      String tblEdition; // = Integer.toString(edition);
      if (master_table_version < 3) {
          tblEdition = "2";
      } else if (master_table_version < 13) {
          tblEdition = "3";
      } else {
          tblEdition = "4";
      }
      //mt = "B" + Integer.toString(edition) + "M-";
      mt = "B" + tblEdition + "M-";
      if (master_table == 0) {
          mt = mt + "000";
      } else {
          mt = mt + Integer.toString(master_table);
      }
      String version = Integer.toString(master_table_version);
      if (version.length() == 1) {
          ltn = mt + "-00" + version;
      } else if (version.length() == 2) {
          ltn = mt + "-0" + version;
      } else {
          ltn = mt + "-" + version;
      }
      localTableName = ltn +"-ABD.diff";
    } else {
      localTableName = ltn;
    }
    */
    } else if (localTableName.contains("-ABD")) {
      //localTableA = BufrTables.getTableA(localTableName.replace("-ABD", "-A"), false);
      localTableB = BufrTables.getTableB(localTableName.replace("-ABD", "-B"), false);
      localTableD = BufrTables.getTableD(localTableName.replace("-ABD", "-D"), false);
      return;

      // check if localTableName(Mnemonic) table has already been processed
    } else if (BufrTables.tablesB.containsKey(localTableName)) {
      //localTableA = BufrTables.getTableA(localTableName, false);
      localTableB = BufrTables.getTableB(localTableName, false);
      localTableD = BufrTables.getTableD(localTableName, false);
      return;
    }
    // NWS Mnemonic table
    ucar.nc2.iosp.bufr.tables.BufrReadMnemonic brm = new ucar.nc2.iosp.bufr.tables.BufrReadMnemonic();
    brm.readMnemonic(localTableName);
    //this.localTableA = brm.getTableA();
    this.localTableB = brm.getTableB();
    this.localTableD = brm.getTableD();
  }

  TableLookup( String table ) throws IOException {
    
    localTableName = table;
    if(localTableName == null ) {
      this.localTableB = wmoTableB;
      this.localTableD = wmoTableD;
      return;
    } else if(localTableName.contains( "-ABD")) {
      //localTableA = BufrTables.getTableA(localTableName.replace( "-ABD", "-A"), false);
      localTableB = BufrTables.getTableB(localTableName.replace( "-ABD", "-B"), false);
      localTableD = BufrTables.getTableD(localTableName.replace( "-ABD", "-D"), false);
      return;

    // check if localTableName(Mnemonic) table has already been processed
    } else if (BufrTables.tablesB.containsKey( localTableName )) {
       //localTableA = BufrTables.getTableA( localTableName, false );
       localTableB = BufrTables.getTableB (localTableName, false );
       localTableD = BufrTables.getTableD( localTableName, false );
       return;
    }
    // NWS Mnemonic table
    BufrReadMnemonic brm = new BufrReadMnemonic();
    brm.readMnemonic(localTableName );
    //this.localTableA = brm.getTableA();
    this.localTableB = brm.getTableB();
    this.localTableD = brm.getTableD();
  }

  public short makeLookupKey( int cat, int center_id ) {

    if(( center_id == 7) || (center_id == 8)  || (center_id == 9) ) {
      if( cat < 240 || cat > 254)
        return (short) center_id;
      return (short) (center_id * 1000 + cat );
    }
    return (short) center_id;
  }

  public final String getWmoTableName() {
    return wmoTableName;
  }

  public final String getLocalTableName() {
    return localTableName;
  }

  public TableBdescriptor getDescriptorTableB(String fxy) {
    return getDescriptorTableB( BufrDataDescriptionSection.getDesc(fxy));
  }

  public TableBdescriptor getDescriptorTableB( short id ) {
    TableBdescriptor b;
    int x = (id & 0x3F00) >> 8;
    int y = id & 0xFF;
    boolean WMOrange = ( x < 48 && y < 192 ? true : false );
    if ( WMOrange && mode.equals( Mode.wmoOnly )) {
      b =  wmoTableB.getDescriptor( id );
    } else if ( WMOrange && mode.equals( Mode.wmoLocal )) {
       b = wmoTableB.getDescriptor( id );
       if (b == null)
         b = localTableB.getDescriptor( id );
    } else {  // X > 47 or Y > 191 or local Table override
      b = localTableB.getDescriptor( id );
      if (b == null)
        b =  wmoTableB.getDescriptor( id );
    }
    if (b == null && showErrors)
      System.out.println("Cant find Table B descriptor ="+BufrDataDescriptionSection.getDescName(id)+" in tables= "+localTableName +","+ wmoTableName);
    return b;
  }

  public List<Short> getDescriptorsTableD(short id) {
    TableDdescriptor d = getDescriptorTableD( id );
    if( d != null )
      return d.getIdList();
    return null;
  }

  public List<String> getDescriptorsTableD(String fxy) {
    short id = BufrDataDescriptionSection.getDesc(fxy);
    TableDdescriptor d = getDescriptorTableD( id );
    if( d != null )
      return d.getDescList();
    return null;
  }

  public TableDdescriptor getDescriptorTableD(String fxy) {
    short id = BufrDataDescriptionSection.getDesc(fxy);
    return getDescriptorTableD( id );
  }

  public TableDdescriptor getDescriptorTableD(short id ) {
    TableDdescriptor d;
    if( showErrors)
      System.out.println("id = "+ id +" fxy ="+ BufrDataDescriptionSection.getDescName(id));
    int x = (id & 0x3F00) >> 8;
    int y = id & 0xFF;
    boolean WMOrange = ( x < 48 && y < 192 ? true : false );
    if ( WMOrange && mode.equals( Mode.wmoOnly )) {
       d = wmoTableD.getDescriptor( id );
    } else if ( WMOrange && mode.equals( Mode.wmoLocal )) {
       d = wmoTableD.getDescriptor( id );
       if (d == null)
         d = localTableD.getDescriptor( id );
    } else {  // X > 47 or Y > 191 or local Table override
      d = localTableD.getDescriptor( id );
      if (d == null)
        d =  wmoTableD.getDescriptor( id );
    }
    if (d == null && showErrors)
      System.out.println("Cant find Table D descriptor ="+BufrDataDescriptionSection.getDescName(id)+"in tables= "+localTableName +","+ wmoTableName);
    return d;
  }

  public String getNameFromTableA(int id) {
//    String result = wmoTableA.getDataCategory( (short) id);
//    return result;
    return wmoTableA.getDataCategory( (short) id);
  }

  public String getCategory(int cat) {
    String result = wmoTableA.getMap().get( (short) cat );
    //if (result == null)  // try localTableA
    //  result =  localTableA.get(Integer.toString( cat ));
    //return result;
    return wmoTableA.getDataCategory( (short) cat );
  }
  public void setMode(Mode mode) {
    this.mode = mode;
  }

  public static Map<Short, String> getTablelookup() {
      return tablelookup.getMap();
  }

  public static TableADataCategory getWmoTableA() {
      return wmoTableA;
  }

  public static TableB getWmoTableB() {
      return wmoTableB;
  }

  public static TableD getWmoTableD() {
      return wmoTableD;
  }

  public TableB getLocalTableB() {
      return localTableB;
  }

  public TableD getLocalTableD() {
      return localTableD;
  }

  public Mode getMode() {
      return mode;
  }
////////////////////////////////////////////////////////

  static private final String[] tableCdesc = new String[38];

  static {
    tableCdesc[1] = "change data width";
    tableCdesc[2] = "change scale";
    tableCdesc[3] = "change reference value";
    tableCdesc[4] = "add associated field";
    tableCdesc[5] = "signify character";
    tableCdesc[6] = "signify data width for next descriptor";
    tableCdesc[21] = "data not present";
    tableCdesc[22] = "quality information follows";
    tableCdesc[23] = "substituted values operator";
    tableCdesc[24] = "first order statistics";
    tableCdesc[25] = "difference statistics";
    tableCdesc[32] = "replaced/retained values";
    tableCdesc[35] = "cancel backward data reference";
    tableCdesc[36] = "define data present bit-map";
    tableCdesc[37] = "use/cancel data present bit-map";
  }

  static public String getTableCOperatorName(int index) {
    if ((index < 0 ) || (index >= tableCdesc.length)) return null;
    return tableCdesc[index];
  }

   /**
   * Name of sub category.
   *
   * @param cat
   * @param subCat
   * @return sub category Name
   */
  static public String getSubCategory(int cat, int subCat) {
    switch (cat) {
      case 0: // 0 Surface - land
        switch (subCat) {
          case 1:
            return "Synoptic manual and automatic";
          case 7:
            return "Aviation METAR";
          case 11:
            return "SHEF";
          case 12:
            return "Aviation SCD";
          case 20:
            return "MESONET Denver";
          case 21:
            return "MESONET RAWS";
          case 22:
            return "MESONET MesoWest ";
          case 23:
            return "MESONET APRS Weather";
          case 24:
            return "MESONET Kansas DOT";
          case 25:
            return "MESONET Florida";
          case 30:
            return "MESONET Other";

          default:
            return "Unknown";
        }

      case 1:
        switch (subCat) {
          case 1:
            return "Ship manual and automatic";
          case 2:
            return "Drifting Buoy";
          case 3:
            return "Moored Buoy";
          case 4:
            return "Land based C-MAN station";
          case 5:
            return "Tide gage";
          case 6:
            return "Sea level pressure bogus";
          case 7:
            return "Coast guard";
          case 8:
            return "Moisture bogus";
          case 9:
            return "SSMIr";
          default:
            return "Unknown";
        }

      case 2:
        switch (subCat) {
          case 1:
            return "Radiosonde fixed land";
          case 2:
            return "Radiosonde mobile land";
          case 3:
            return "Radiosonde ship";
          case 4:
            return "Dropwinsonde";
          case 5:
            return "Pibal";
          case 7:
            return "Wind Profiler NOAA";
          case 8:
            return "NEXRAD winds";
          case 9:
            return "Wind profiler PILOT";

          default:
            return "Unknown";
        }

      case 3:
        switch (subCat) {
          case 1:
            return "Geostationary";
          case 2:
            return "Polar orbitin";
          case 3:
            return "Sun synchronous";

          default:
            return "Unknown";
        }

      case 4:
        switch (subCat) {
          case 1:
            return "NCEP Re-Analysis Project";
          case 2:
            return "NCEP Ensemble Products";
          case 3:
            return "NCEP Central Operations";
          case 4:
            return "Environmental Modeling Center";
          case 5:
            return "Hydrometeorological Prediction Center";
          case 6:
            return "Marine Prediction Center";
          case 7:
            return "Climate Prediction Center";
          case 8:
            return "Aviation Weather Center";
          case 9:
            return "Storm Prediction Center";
          case 10:
            return "Tropical Prediction Center";
          case 11:
            return "NWS Techniques Development Laboratory";
          case 12:
            return "NESDIS Office of Research and Applications";
          case 13:
            return "FAA";
          case 14:
            return "NWS Meteorological Development Laboratory";
          case 15:
            return "The North American Regional Reanalysis (NARR) Project";
          default:
            return "Unknown";
        }

      case 5:
        switch (subCat) {
          case 1:
            return "NCEP Re-Analysis Project";
          case 2:
            return "NCEP Ensemble Products";
          case 3:
            return "NCEP Central Operations";
          case 4:
            return "Environmental Modeling Center";
          case 5:
            return "Hydrometeorological Prediction Center";
          case 6:
            return "Marine Prediction Center";
          case 7:
            return "Climate Prediction Center";
          case 8:
            return "Aviation Weather Center";
          case 9:
            return "Storm Prediction Center";
          case 10:
            return "Tropical Prediction Center";
          case 11:
            return "NWS Techniques Development Laboratory";
          case 12:
            return "NESDIS Office of Research and Applications";
          case 13:
            return "FAA";
          case 14:
            return "NWS Meteorological Development Laboratory";
          case 15:
            return "The North American Regional Reanalysis (NARR) Project";
          default:
            return "Unknown";
        }
      case 12:

        switch (subCat) {

          case 1:
            return "NCEP Re-Analysis Project";
          case 2:
            return "NCEP Ensemble Products";
          case 3:
            return "NCEP Central Operations";
          case 4:
            return "Environmental Modeling Center";
          case 5:
            return "Hydrometeorological Prediction Center";
          case 6:
            return "Marine Prediction Center";
          case 7:
            return "Climate Prediction Center";
          case 8:
            return "Aviation Weather Center";
          case 9:
            return "Storm Prediction Center";
          case 10:
            return "Tropical Prediction Center";
          case 11:
            return "NWS Techniques Development Laboratory";
          case 12:
            return "NESDIS Office of Research and Applications";
          case 13:
            return "FAA";
          case 14:
            return "NWS Meteorological Development Laboratory";
          case 15:
            return "The North American Regional Reanalysis (NARR) Project";
          default:
            return "Unknown";
        }
      case 31:

        switch (subCat) {

          case 1:
            return "NCEP Re-Analysis Project";
          case 2:
            return "NCEP Ensemble Products";
          case 3:
            return "NCEP Central Operations";
          case 4:
            return "Environmental Modeling Center";
          case 5:
            return "Hydrometeorological Prediction Center";
          case 6:
            return "Marine Prediction Center";
          case 7:
            return "Climate Prediction Center";
          case 8:
            return "Aviation Weather Center";
          case 9:
            return "Storm Prediction Center";
          case 10:
            return "Tropical Prediction Center";
          case 11:
            return "NWS Techniques Development Laboratory";
          case 12:
            return "NESDIS Office of Research and Applications";
          case 13:
            return "FAA";
          case 14:
            return "NWS Meteorological Development Laboratory";
          case 15:
            return "The North American Regional Reanalysis (NARR) Project";
          default:
            return "Unknown";
        }
      default:
        return "Unknown";
    }
  }
    public static void main(String args[]) throws IOException {

    // Function References
    String tableName;
    if (args.length == 1) {
      tableName = args[0];
    } else {
      tableName = "B4L-046-013-ABD.diff";
      tableName = "bufrtab.ETACLS1";
    }

    TableLookup tlu = new TableLookup(  tableName );

    //short test = tlu.makeLookupKey( 243, 46 );
    //String ltn = tablelookup.getDataCategory( test);

    // only can be element descriptors in tableB
    //
    TableBdescriptor b;
    Vector<Short> v = new Vector( tlu.localTableB.getMap().keySet());
    Collections.sort( v );
    System.out.println( "WMO table "+  tlu.getWmoTableName() );
    System.out.println( "Local table "+ tlu.getLocalTableName().replace( "-ABD", "-B"));
    System.out.println();
    System.out.println("Table B Descriptors:");
    System.out.println();
    for ( Short id : v ) {
        b = tlu.getDescriptorTableB( id );
        if ( b == null && tlu.mode.equals( Mode.wmoOnly )) {
            System.out.println("fxy ="+ BufrDataDescriptionSection.getDescName(id) +" not in wmoTable "+ tlu.getWmoTableName() );
            continue;
        }
        int x = (id & 0x3F00) >> 8;
        int y = id & 0xFF;
        boolean WMOrange = ( x > 47 || y > 191 ? false : true );
        if ( WMOrange && ! b.isWMO() )
           System.out.print("***Warning*** fxy in wmoRange found in local table "+ tlu.getLocalTableName() +"   ");
        System.out.println("fxy ="+ b.getFxy() +" name ="+ b.getName() +" isWMO ="+ b.isWMO());
    }
    // sequences can be other sequences as well as element descriptors
    System.out.println();

    List<String> al;
    TableDdescriptor d;
    v = new Vector( tlu.localTableD.getMap().keySet());
    Collections.sort( v );
    System.out.println("Table D Descriptors:\n");
    for ( Short id : v ) {
        d = tlu.getDescriptorTableD( id );
        if ( d == null && tlu.mode.equals( Mode.wmoOnly )) {
            System.out.println("fxy ="+ BufrDataDescriptionSection.getDescName(id) +" not in wmoTable "+ tlu.getWmoTableName() );
            continue;
        }
        int x = (id & 0x3F00) >> 8;
        int y = id & 0xFF;
        boolean WMOrange = ( x > 47 || y > 191 ? false : true );
        if ( WMOrange && ! d.isWMO() )
           System.out.print("***Warning*** fxy in wmoRange found in local table "+ tlu.getLocalTableName() +"   ");
        al = d.getDescList();
        System.out.println("fxy "+ BufrDataDescriptionSection.getDescName(id) +" isWMO ="+ d.isWMO());
        System.out.println("  list =" + al);
    }
  }

}
