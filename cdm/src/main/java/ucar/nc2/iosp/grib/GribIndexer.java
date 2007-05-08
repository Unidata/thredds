/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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

package ucar.nc2.iosp.grib;

import ucar.grib.*;
import ucar.grib.grib1.*;
import ucar.grib.grib2.*;
import ucar.unidata.io.RandomAccessFile;
import ucar.nc2.dt.fmrc.ForecastModelRunInventory;


import ucar.grib.grib2.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;

// Name:  	GribIndexer.pl
// 
// Author: 	Robb Kambic
// Date  : 	Mar 22, 2007
// 
// Purpose: 	walks directory sturcture making Grib Indexes as needed.
//    
// Description:  
//

public final class GribIndexer {

   /* 
    * dirs to inspect
    *
    */
    //private ArrayList<String> dirs = new ArrayList();
    private ArrayList dirs = new ArrayList();


   /* 
    * reads in the configuration file 
    *
    */
    private boolean readConf( String conf ) throws IOException {

        InputStream ios = new FileInputStream( conf );
        BufferedReader dataIS =
            new BufferedReader(new InputStreamReader(ios));

        while (true) {
            String line = dataIS.readLine();
            if (line == null) {
                break;
            }
            if (line.startsWith("#")) {
                continue;
            }
            dirs.add( line );
            //System.out.println( line );
       }
       ios.close() ;
       return true;
   }


   /* 
    * clears all IndexLock in the directories
    *
    */
   private void clearLocks() {

       for( int i = 0; i < dirs.size(); i++ ) {
           String dir = (String) dirs.get( i );
           File f = new File( dir +"/IndexLock" );
           if( f.exists() ) {
               f.delete();
               System.out.println( "Cleared lock "+ dir +"/IndexLock" );
           } else {
               System.out.println( "In directory "+ dir );
           }
       }
   }

   /* 
    * walks the directory trees setting IndexLocks
    *
    */
   private void indexer() throws IOException {

       System.out.println( "Start "+ Calendar.getInstance().getTime().toString() );
       for( int i = 0; i < dirs.size(); i++ ) {
           String dir = (String) dirs.get( i );
           File d = new File( dir );
           if( ! d.exists() ) {
               System.out.println( "Dir "+ dir +" doesn't exists" );
               continue;
           }
           File dl = new File( dir +"/IndexLock" );
           if( dl.exists() ) {
               System.out.println( "Exiting "+ dir +" another Indexer working here" );
               continue;
           }
           //System.out.println( "In directory "+ dir );
           dl.createNewFile(); // create a lock while indexing dir
          
           checkDirs( d );

           dl.delete();  // delete lock when done
       }
       System.out.println( "End "+ Calendar.getInstance().getTime().toString() );
   }

   /*
    * checkDirs is a recursive routine used to walk the directory tree in a
    * depth first search checking the index of GRIB files . 
    */
   private void checkDirs( File dir ) throws IOException {

        if (dir.isDirectory()) {
            System.out.println( "In directory "+ dir.getParent() +"/"+ dir.getName() );
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                if( children[ i ].equals( "IndexLock" ) )
                    continue;
                //System.out.println( "children i ="+ children[ i ]);
                File child = new File(dir, children[i]);
                //System.out.println( "child ="+ child.getName() );
                if (child.isDirectory()) {
                    checkDirs( child );
                // skip index *gbx and inventory *xml files
                } else if( children[ i ].endsWith( "gbx" ) ||
                    children[ i ].endsWith( "xml" )) {
                    continue;
                } else {
                    checkIndex( dir, child );
                }
            }
        } else {
        }

   }

   /* 
    * checks the status of index files
    *
    */
   private void checkIndex( File dir, File grib ) 
       throws IOException {

        String[] args = new String[ 2 ];
        File gbx = new File( dir, grib.getName() +".gbx" );
        //System.out.println( "index ="+ gbx.getName() );

        args[ 0 ] = grib.getParent() +"/"+ grib.getName();
        args[ 1 ] = grib.getParent() +"/"+ gbx.getName();
        //System.out.println( "args ="+ args[ 0] +" "+ args[ 1 ] );
        if( gbx.exists() ) {
            // skip files older than 3 hours
            if( (System.currentTimeMillis() - grib.lastModified() ) > 10800000 )
                return;
            // skip grib files that have the same size as in the Gbx
            if( grib.length() == lengthGbx( args[ 1 ] ) )
               return;
            // skip indexes that have a length of 0, most likely there is a index problem
            if( gbx.length() == 0 ) {
               System.out.println( "ERROR "+ args[ 1 ] +" has length zero" );
               return;
            }
        }

        if( grib.getName().endsWith( "grib1" )) {
            grib1check( grib, gbx, args );
        } else if( grib.getName().endsWith( "grib2" )) {
            grib2check( grib, gbx, args );
        } else { // else check file for Grib version
            RandomAccessFile raf = new RandomAccessFile( args[0], "r");
            //System.out.println("Grib "+ args[ 0 ] );
            int result = GribChecker.getEdition(raf);
            if (result == 2) {
                //System.out.println("Valid Grib Edition 2 File");
                grib2check( grib, gbx, args );
            } else if (result == 1) {
                //System.out.println("Valid Grib Edition 1 File");
                grib1check( grib, gbx, args );
            } else {
                System.out.println("Not a Grib File "+ args[ 0 ] );
            }
            raf.close();
        }

   }

   /* 
    * indexes or extends Grib1 files plus creates inventories files
    *
    */
   private void grib1check( File grib, File gbx, String[] args ) {
       // args 0  grib name, args 1 grib index name

       try {
           if( gbx.exists() ) {
                System.out.println( "IndexExtending "+ grib.getName() +" "+ 
                    Calendar.getInstance().getTime().toString() );
                Grib1IndexExtender.main( args ); 
                //ForecastModelRunInventory.main( args );
                ForecastModelRunInventory.open(null, args[ 0 ], ForecastModelRunInventory.OPEN_FORCE_NEW, true);
           } else {  // create index
                System.out.println( "Indexing "+ grib.getName() +" "+ 
                    Calendar.getInstance().getTime().toString() );
                Grib1Indexer.main( args ); 
                //ForecastModelRunInventory.main( args );
                ForecastModelRunInventory.open(null, args[ 0 ], ForecastModelRunInventory.OPEN_FORCE_NEW, true);

           }
       } catch( Exception e ) {
           e.printStackTrace();
           System.out.println( "Caught Exception doing index or inventory" );
           return;
       }

   }

   /* 
    * indexes or extends Grib2 files plus creates inventories files
    *
    */
   private void grib2check( File grib, File gbx, String[] args ) { 

       try {
           if( gbx.exists() ) {
                System.out.println( "IndexExtending "+ grib.getName() +" "+ 
                    Calendar.getInstance().getTime().toString() );
                Grib2IndexExtender.main( args ); 
                //ForecastModelRunInventory.main( args );
                ForecastModelRunInventory.open(null, args[ 0 ], ForecastModelRunInventory.OPEN_FORCE_NEW, true);
           } else {  // create index
                System.out.println( "Indexing "+ grib.getName() +" "+ 
                    Calendar.getInstance().getTime().toString() );
                Grib2Indexer.main( args ); 
                //ForecastModelRunInventory.main( args );
                ForecastModelRunInventory.open(null, args[ 0 ], ForecastModelRunInventory.OPEN_FORCE_NEW, true);
           }
       } catch( Exception e ) {
           e.printStackTrace();
           System.out.println( "Caught Exception doing index or inventory" );
           return;
       }

   }

   /* 
    * reads index to extract the length of the Grib when the index was created
    *
    */
    private long lengthGbx( String gbx ) throws IOException {

        InputStream ios = new FileInputStream( gbx );
        BufferedReader dataIS =
            new BufferedReader(new InputStreamReader(ios));

        long length = -1;
        while (true) {
            String line = dataIS.readLine();
            if (line == null) {
                break;
            }
            if (line.startsWith("---")) {
                break;
            }
            //System.out.println( line );
            if( line.startsWith( "length" )) {
                String len = line.substring(line.indexOf(" = ") + 3);
                length = Long.parseLong( len );

                break;
            }
       }
       ios.close() ;
       //System.out.println( length );
       return length;

   }

    /**
     * main.
     * @param args can be clear and the GribIndexer.conf file 
     * @throws IOException
     */
    // process command line switches
    static public void main(String[] args) throws IOException {
        GribIndexer gi = new GribIndexer();

        boolean clear = false;
        String conf = null;
        for( int i = 0; i < args.length; i++ ) {
            if( args[ i ].equals( "clear" ) ) {
                clear = true;
                System.out.println( "Clearing Index locks" );
                continue;
            }
            // else conf file
            File f = new File( args[ i ] );
            if( ! f.exists() ) {
                System.out.println( "Conf file "+ args[ i ] +" doesn't exist: " );
                return;
            }
            // read in conf file
            gi.readConf( args[ i ] );
        }
        if( clear ) {
            gi.clearLocks();
            return;
        }
        // Grib Index files in dirs
        gi.indexer();

    }
}
