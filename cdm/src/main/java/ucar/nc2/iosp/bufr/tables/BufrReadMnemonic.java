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

/*
 * BufrRead mnemonic.java  1.0  05/09/2008
 * @author  Robb Kambic
 * @version 1.0
 */

import ucar.nc2.iosp.bufr.*;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.lang.Integer;
import java.net.URL;

/**
 * A class that reads a  mnemonic table. It doesn't include X < 48 and Y < 192 type of
 * descriptors because they are already stored in the latest WMO tables.
 */

final public class BufrReadMnemonic {
    
  //| HEADR    | 362001 | TABLE D ENTRY - PROFILE COORDINATES                      |                      |
  private static final Pattern  fields3 =
        Pattern.compile("^\\|\\s+(.*)\\s+\\|\\s+(.*)\\s+\\|\\s+(.*)\\s*\\|");
  private static final Pattern  fields2 =
        Pattern.compile("^\\|\\s+(.*)\\s+\\|\\s+(.*)\\s+\\|");
  private static final Pattern  fields5 =
        Pattern.compile("^\\|\\s+(.*)\\s+\\|\\s+(.*)\\s+\\|\\s+(.*)\\s+\\|\\s+(.*)\\s+\\|\\s+(.*)\\s+\\|");
  /**
   * Pattern to get 3 integers from beginning of line.
   */
  private static final Pattern ints0123 = Pattern.compile("^(0|1|2|3)" );
  private static final Pattern ints6 = Pattern.compile("^\\d{6}" );
  private static final int XlocalCutoff = 48;
  private static final int YlocalCutoff = 192;
     /**
      * debug.
      */
  private static final boolean debugTable = false;

  private static final String RESOURCE_PATH = "resources/bufr/tables/";
  /**
   * tables containing data set descriptors from static tables for NCEP.
   */
  //private TableA tableA;
  private TableB tableB;
  private TableD tableD;


  // *** constructors *******************************************************

  /**
   * Constructs a BufrRead mnemonic object. Use this constructor
   *
   * @throws java.io.IOException
   */
  public BufrReadMnemonic() throws IOException {
  }
  /**
   * Used to read  mnemonic tables.
   *
   * @return boolean
   * @throws IOException
   */
  public synchronized boolean readMnemonic( String mnemonic ) {
    try {

      // read this mnemonic table from NWS
      HashMap<String,String> number = new HashMap<String,String>();
      HashMap<String,String> desc = new HashMap<String,String>();
      HashMap<String,String> mnseq = new HashMap<String,String>();
      Map<Short, TableBdescriptor> descriptors = new HashMap<Short, TableBdescriptor>();
      Map<Short, TableDdescriptor> sequences = new HashMap<Short, TableDdescriptor>();

      InputStream ios = open(  mnemonic );
      if( ios == null )
        return false;
      BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios));

      // read  mnemonic table
      Matcher m;
      // read header info and disregard
      while (true) {
        String line = dataIS.readLine();
        //if (line == null) break;
        if( line.contains( "MNEMONIC" )) break;
      }
      // read mnemonic, number, and description
      //| HEADR    | 362001 | TABLE D ENTRY - PROFILE COORDINATES                      |
      while (true) {
        String line = dataIS.readLine();
        //if (line == null) break;
        if( line.contains( "MNEMONIC" )) break;
        if( line.contains( "----" )) continue;
        if( line.startsWith( "*" )) continue;
        if( line.startsWith( "|       " )) continue;
        m = fields3.matcher(line);
        if (m.find()) {
          if (m.group(2).startsWith("3")) {
              number.put( m.group(1).trim(), m.group(2) );
              desc.put( m.group(1).trim(), m.group(3).replace( "TABLE D ENTRY - ", "").trim() );
          } else if (m.group(2).startsWith("0")) {
             number.put( m.group(1).trim(), m.group(2) );
             desc.put( m.group(1).trim(), m.group(3).replace( "TABLE B ENTRY - ", "").trim() );
          } else if (m.group(2).startsWith("A")) {
             number.put( m.group(1).trim(), m.group(2) );
             desc.put( m.group(1).trim(), m.group(3).replace( "TABLE A ENTRY - ", "").trim() );
          }
        } else if( debugTable ) {
            System.out.println( "bad mnemonic, number, and description: "+ line);
        }
      }
      // read in sequences using mnemonics
      //| ETACLS1  | HEADR {PROFILE} SURF FLUX HYDR D10M {SLYR} XTRA                   |
      while (true) {
        String line = dataIS.readLine();
        //if (line == null) break;
        if( line.contains( "MNEMONIC" )) break;
        if( line.contains( "----" )) continue;
        if( line.startsWith( "|       " )) continue;
        if( line.startsWith( "*" )) continue;
        m = fields2.matcher(line);
        if (m.find()) {
          if( mnseq.containsKey(m.group(1).trim() )) {
                 String value = mnseq.get( m.group(1).trim() );
                 value = value +" "+ m.group(2);
                 mnseq.put(m.group(1).trim(), value );
          } else {
                 mnseq.put(m.group(1).trim(), m.group(2) );
          }
        } else if( debugTable ) {
            System.out.println( "bad sequence mnemonic: "+ line);
        }
      }
      // create sequences, replacing mnemonics with numbers
      DescriptorTableD d;
      Iterator it = mnseq.keySet().iterator();
      while( it.hasNext() ) {
          String key = (String) it.next();
          String seq = mnseq.get( key );
          seq = seq.replaceAll( "\\<", "1-1-0 0-31-0 ");
          seq = seq.replaceAll( "\\>", "");
          seq = seq.replaceAll( "\\{", "1-1-0 0-31-1 ");
          seq = seq.replaceAll( "\\}", "");
          seq = seq.replaceAll( "\\(", "1-1-0 0-31-2 ");
          seq = seq.replaceAll( "\\)", "");

          StringTokenizer stoke = new StringTokenizer(seq, " ");
          List list = new ArrayList<String>();
          while( stoke.hasMoreTokens()  ) {
            String mn = stoke.nextToken();
            if( mn.charAt( 1 ) == '-' ) {
              list.add( mn );
              continue;
            }
            // element descriptor needs hypens  
            m = ints6.matcher( mn );
            if( m.find() ) {
              String F = mn.substring(0,1);
              String X = removeLeading0( mn.substring(1,3) );
              String Y = removeLeading0( mn.substring(3) );
              list.add(F + "-" + X + "-" + Y);
              continue;  
            }
            if( mn.startsWith( "\"")) {
              int idx = mn.lastIndexOf( '"');
              String count = mn.substring( idx +1 );
              list.add( "1-1-" +count );
              mn = mn.substring( 1, idx );

            }
            if( mn.startsWith( ".")) {
              String des = mn.substring( mn.length() -4 );
              mn = mn.replace( des, "....");

            }
            String fxy = number.get( mn );
            String F = fxy.substring(0,1);
            String X = removeLeading0( fxy.substring(1,3) );
            String Y = removeLeading0( fxy.substring(3) );
            list.add(F + "-" + X + "-" + Y);
          }
          String fxy = number.get( key );
          String F = fxy.substring(0,1);
          if( F.equals( "A"))
              F = "3";
          String X = removeLeading0( fxy.substring(1,3) );
          String Y = removeLeading0( fxy.substring(3) );
          // these are in latest tables
          if( XlocalCutoff > Integer.parseInt(X) && YlocalCutoff > Integer.parseInt(Y) )
              continue;
          key = F + "-" + X + "-" + Y;

          d = new DescriptorTableD("", key, list, false);
          short id = BufrDataDescriptionSection.getDesc(key);
          sequences.put( Short.valueOf( id ), d);
      }
      // add some static repetition sequences

      List list = new ArrayList<String>();
      // 16 bit delayed repetition
      list.add( "1-1-0" );
      list.add(  "0-31-2" );
      d = new DescriptorTableD("", "3-60-1", list, false);
      //tableD.put( "3-60-1", d);
      short id = BufrDataDescriptionSection.getDesc("3-60-1");
      sequences.put( Short.valueOf( id ), d);

      list = new ArrayList<String>();
      // 8 bit delayed repetition
      list.add( "1-1-0" );
      list.add(  "0-31-1" );
      d = new DescriptorTableD("", "3-60-2", list, false);
      //tableD.put( "3-60-2", d);
      id = BufrDataDescriptionSection.getDesc("3-60-2");
      sequences.put( Short.valueOf( id ), d);

      list = new ArrayList<String>();
      // 8 bit delayed repetition
      list.add( "1-1-0" );
      list.add(  "0-31-1" );
      d = new DescriptorTableD("", "3-60-3", list, false);
      //tableD.put( "3-60-3", d);
      id = BufrDataDescriptionSection.getDesc("3-60-3");
      sequences.put( Short.valueOf( id ), d);

      list = new ArrayList<String>();
      // 1 bit delayed repetition
      list.add( "1-1-0" );
      list.add(  "0-31-0" );
      d = new DescriptorTableD("", "3-60-4", list, false);
      //tableD.put( "3-60-4", d);
      id = BufrDataDescriptionSection.getDesc("3-60-4");
      sequences.put( Short.valueOf( id ), d);

      tableD = new BufrTableD( mnemonic, null, sequences );

      // add in element descriptors
      //  MNEMONIC | SCAL | REFERENCE   | BIT | UNITS
      //| FTIM     |    0 |           0 |  24 | SECONDS                  |-------------|
      while (true) {
        String line = dataIS.readLine();
         if (line == null) break;
        if( line.contains( "MNEMONIC" )) break;
        if( line.startsWith( "|       " )) continue;
        if( line.startsWith( "*" )) continue;
        m = fields5.matcher(line);
        if (m.find()) {
          if (m.group(1).equals( "" ) ) {
              continue;
          } else if( number.containsKey( m.group(1).trim() )) { // add descriptor to tableB
             String fxy = number.get( m.group(1).trim() );
             String F = fxy.substring(0,1);
             String X = fxy.substring(1,3);
             String Y = fxy.substring(3);
             String name =  desc.get( m.group(1).trim() );
             // scale = m.group(2)
             // reference = m.group(3)
             // width = m.group(4)
             // units = m.group(5)
             // these are in latest tables so skip
             if( XlocalCutoff > Integer.parseInt(X) && YlocalCutoff > Integer.parseInt(Y) )
               continue;
             DescriptorTableB des = new DescriptorTableB(F, X, Y, m.group(2), m.group(3),
                   m.group(4), m.group(5), name);
             //tableB.put(des.getKey(), des);
             short bid = BufrDataDescriptionSection.getDesc(des.getKey());
             descriptors.put(Short.valueOf( bid ), des);
          } else if( debugTable ) {
            System.out.println( "bad element descriptors: "+ line);      
          }
        }
      }
      // default for NCEP
      // 0; 63; 0; 0; 0; 16; Numeric; Byte count
      DescriptorTableB des = new DescriptorTableB( "0", "63", "0", "0", "0", "16", "Numeric", "Byte count");
      short bid = BufrDataDescriptionSection.getDesc("0-63-0");
      descriptors.put(Short.valueOf( bid ), des);
      tableB = new BufrTableB( mnemonic, null, descriptors );

      // add new tables to master MAP of all tables
      //BufrTables.tablesA.put( mnemonic, tableA );
      BufrTables.tablesB.put( mnemonic, tableB );
      BufrTables.tablesD.put( mnemonic, tableD );

      dataIS.close();
    } catch ( Exception ioe ) {
      return false;
    }
    return true;
  }
  /**
   * Used to open BUFR tables.
   *
   * @param location URL or local filename of BUFR table file
   * @return InputStream
   * @throws IOException
   */
  private InputStream open(String location) throws IOException {
    InputStream ios = null;

    // Try class loader to get resource
    ClassLoader cl = BufrReadMnemonic.class.getClassLoader();
    String tmp = RESOURCE_PATH + location;
    ios = cl.getResourceAsStream(tmp);
    if (ios != null) return ios;

    if (location.startsWith("http:")) {
      URL url = new URL(location);
      ios = url.openStream();
    } else {
      ios = new FileInputStream(location);
    }
    return ios;
  }
  private String removeLeading0( String number ) {
    if (number.length() == 2 && number.startsWith("0")) {
        number = number.substring(1);
    } else if (number.length() == 3 && number.startsWith("00")) {
        number = number.substring(2);
    } else if (number.length() == 3 && number.startsWith("0")) {
        number = number.substring(1);
    }
    return number;
  } // end
   
  // getters and setters
                                               
  /**
   *   tableA from the mnemonic.
   *
   * @return tableA
   */
//  public final TableA getTableA() {
//    return tableA;
//  }

  /**
   *  tableB from mnemonic.
   *
   * @return tableB
   */
  public final TableB getTableB() {
    return tableB;
  }

  /**
   *  tableD from mnemonic.
   *
   * @return tableD
   */
  public final TableD getTableD() {
    return tableD;
  }

    public static void main(String args[]) throws IOException {

    BufrReadMnemonic brm = new BufrReadMnemonic();

    // Function References
    String[] tables = null;
    if (args.length == 1) {
      tables = new String[ 1 ];
      tables[ 0 ] = args[0];
    } else {
      //tables = {"bufrtab.000","bufrtab.001","bufrtab.002","bufrtab.003","bufrtab.004","bufrtab.005",
      //    "bufrtab.006","bufrtab.007","bufrtab.008","bufrtab.012","bufrtab.021","bufrtab.031","bufrtab.255"};
      tables = new String[ 1 ];
      tables[0] = "bufrtab.ETACLS1";
    }

    // process all NCEP tables
    StringBuilder tNames = new StringBuilder();
    for ( String tableName : tables ) {
        tNames.append( tableName +" ");
        if( brm.readMnemonic( tableName )) { // print it out
          // tables descriptors are stored
        } else {
          System.out.println("Table process problem for "+ tableName +"\n");
        }
    }

    Vector<Short> v;
    // only can be element descriptors in tableB
    //
    TableB tableB = brm.getTableB();
    TableBdescriptor b;
    v = new Vector( tableB.getMap().keySet());
    Collections.sort( v );
    System.out.println("Elements Descriptors from table "+ tNames.toString() +"\n#");
    for ( Short id : v ) {
        b = tableB.getDescriptor(id);
        System.out.println("FXY ="+ b.getFxy() +" name ="+ b.getName() +" WMO ="+ b.isWMO() );
    }
    // sequences can be other sequences as well as element descriptors
    System.out.println();
    //*/
/*
        Given  values

key 3-52-1
list =[0-35-195, 0-35-21, 0-35-23, 0-35-22, 0-35-194]

        want format for table D
3   0   3
    0   0  10  F,  part descriptor
    0   0  11  X,  part descriptor
    0   0  12  Y,  part descriptor
   -1
*/
    TableD tableD = brm.getTableD();
    TableDdescriptor d;
    List<String> al;
    v = new Vector( tableD.getMap().keySet());
    Collections.sort( v );
    System.out.println("# Local Sequences from NCEP tables: ");
    System.out.println("# "+ tNames.toString() +"\n#\n#");    
    for ( Short id : v ) {

        String fxy = BufrDataDescriptionSection.getDescName(id);
        StringTokenizer stoke = new StringTokenizer(fxy, "-");
        String f = stoke.nextToken();
        String x = stoke.nextToken();
        if( x.length() == 1)
          x = " "+ x;
        String y = stoke.nextToken();
        if( y.length() == 2) {
          y = " "+ y;
        } else if( y.length() == 1) {
          y = "  "+ y;
        }
        d = tableD.getDescriptor( id );
        System.out.println( f +"  "+ x +" "+ y +" isWMO ="+ d.isWMO() );
        al = (ArrayList) d.getDescList();
        //System.out.println("list =" + al);
        
        for ( String element : al ) {
          stoke = new StringTokenizer(element, "-");
          f = stoke.nextToken();
          x = stoke.nextToken();
          if( x.length() == 1)
            x = " "+ x;
          y = stoke.nextToken();
          if( y.length() == 2) {
            y = " "+ y;
          } else if( y.length() == 1) {
            y = "  "+ y;
          }
          System.out.println( "    "+ f +"  "+ x +" "+ y );
        }
        System.out.println( "   -1\n#" );
    }

  }

}