/*
 * CVS identifier:
 *
 * $Id: jj2kGrib2.java,v 
 *
 * Class:                   jj2kGrib2
 *
 * Description:             .
 *
 *
 * */

package ucar.jpeg;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

import ucar.jpeg.jj2000.j2k.decoder.*;
import ucar.jpeg.jj2000.j2k.entropy.decoder.*;
import ucar.jpeg.jj2000.j2k.entropy.decoder.ByteInputBuffer;

//import ucar.jpeg.unidata.io.*;
/**
 * This class is a wrapper for the CmdLnDecoder class in the
 * jj2000.j2k.decoder package. It avoids having to list the whole package
 * hierarchy in the java virtual machine command line
 * (i.e. jj2000.j2k.decoder.Decoder).
 * */
public class jj2kGrib2 {

    /**
     * The starting point of the program. It forwards the call to the
     * CmdLnDecoder class.
     *
     * @param argv The command line arguments.
     * */
    public static void main(String argv[]) throws IOException
    {
        if (argv.length != 0) {
            System.err.println("jj2kGrib2: jj2kGrib2EG 2000 Decoder\n");
            System.err.println("    use jj2kGrib2 -u to get help\n");
            System.exit(1);
        }

        //CmdLnDecoder.main(argv);
       long start = System.currentTimeMillis();

       RandomAccessFile raf = new  RandomAccessFile(
          "/home/rkambic/jpeg2000/test/eta.j2k", "r" );

       //while( raf.getFilePointer() < raf.length() )
       //{
	  //byte buf[] = new byte[ bb.limit() ];
	  //bb.get( buf, 0, bb.limit() );
//          ByteInputBuffer bib = new ByteInputBuffer( buf );
//		( buf, 0, bb.limit() );

//	  int noc = 10;
//	  int is[] = new int[ 10 ];
//	  MQDecoder mqd = new MQDecoder( bib, noc, is );
//	  int ctxts = mqd.getNumCtxts();
//	  System.out.println( "NumCtxts=" + ctxts );

          System.out.println( "raf.getFilePointer()=" + raf.getFilePointer() );
          System.out.println( "raf.length()=" + raf.length() );
      //} // end while true
      System.out.println("GribFile: processed in " +
         (System.currentTimeMillis()- start) + " milliseconds");

    } // end main
}
