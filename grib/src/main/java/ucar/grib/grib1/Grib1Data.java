/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

// $Id: Grib1Data.java,v 1.11 2005/12/13 22:58:55 rkambic Exp $

package ucar.grib.grib1;

import ucar.grib.*;

import ucar.unidata.io.RandomAccessFile;

/*
 * Grib1Data.java  1.0  10/12/2004
 *
 * @author Robb Kambic
 *
 */

import java.io.IOException;

/**
 * A class used to extract data from a GRIB1 file.
 * see <a href="../../../IndexFormat.txt"> IndexFormat.txt</a>
 */

public final class Grib1Data {

  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Grib1Data.class);

  /*
  *  used to hold open file descriptor
  */
  private final RandomAccessFile raf;

  // *** constructors *******************************************************

  /**
   * Constructs a Grib2Data object from a stream.
   *
   * @param raf ucar.unidata.io.RandomAccessFile with GRIB content.
   */
  public Grib1Data(RandomAccessFile raf) {
    this.raf = raf;
  }

  /**
   * Reads the Grib data
   *
   * @param gdsOffset PDS offset into file.
   * @param dataOffset GDS offset into file.
   * @return float[]
   * @throws IOException
   */
  public final float[] getData(long gdsOffset, long dataOffset, int decimalScale, boolean bmsExists)
      throws IOException {

    //long start = System.currentTimeMillis();
    Grib1GridDefinitionSection gds = null;
    Grib1GDSVariables gdsv = null;
    boolean isThin = false;
    try {
      // check for thin grids
      if ( gdsOffset != -1 ) {
        raf.seek(gdsOffset);
        gds = new Grib1GridDefinitionSection(raf);
        gdsv = gds.getGdsVars();
        int PVorPL = gdsv.getPVorPL();
        int NV = gdsv.getNV();
        isThin = false;
        if (PVorPL != 255 && (NV == 0 || NV == 255) ) {
          isThin = true;
        }
      }
    } catch (NoValidGribException nvge) {
      log.debug("gds exception was caught");
    }
    // seek data start
    raf.seek(dataOffset);

    // Need section 3 and 4 to read/interpet the data
    Grib1BitMapSection bms = null;
    if (bmsExists) {
      // read Bit Mapped Section 3
      bms = new Grib1BitMapSection(raf);
    }

    try {
      // read Binary Data Section 4
      Grib1BinaryDataSection bds =
          new Grib1BinaryDataSection(raf, decimalScale, bms, gdsv.getScanMode(), gdsv.getNx(), gdsv.getNy() );
      if (isThin) {
        QuasiRegular qr = new QuasiRegular(bds.getValues(), gdsv.getParallels(), gdsv.getNx(), gdsv.getNy() );
        return qr.getData();
      } else {
        return bds.getValues();
      }
    } catch (NotSupportedException notSupport) {
      log.error("Grib1BinaryDataSection exception was caught");
      return null;
    }
  }  // end getData

  
  /**
   * Reads the Grib data
   *
   * @param offset       offset into file.
   * @param decimalScale
   * @param bmsExists
   * @return float[]
   * @throws NotSupportedException
   * @throws IOException
   */
  public final float[] getData(long offset, int decimalScale, boolean bmsExists)
      throws IOException {
    //long start = System.currentTimeMillis();

    // check if the offset is for the GDS or Data. The new indexer code
    // will make all the offsets GDS
    boolean isThin = false;
    Grib1GridDefinitionSection gds = null;
    Grib1GDSVariables gdsv = null;
    try {
      raf.seek(offset);
      //System.out.print( "seek took "+ (System.currentTimeMillis() - start) );
      //System.out.println( "raf.getFilePointer()="+ raf.getFilePointer() );
      // Read/Check if this is section 2 GDS
      // octets 1-3 (Length of GDS)
      int length = GribNumbers.uint3(raf);
      //System.out.println( "GDS length = " + length );
      // octets 4 NV
      int NV = raf.read();
      //System.out.println( "GDS NV = " + NV );
      // octet 5 PL is this a Quasi/Thin Grid no == 255
      int P_VorL = raf.read();
      //System.out.println( "GDS PL = " + P_VorL );

      if (length < 50) {
        raf.skipBytes(length - 5);
        // Quasi/Thin grid
      } else if (length < 1200 && !bmsExists) {
        if (P_VorL != 255) {
          raf.skipBytes(-5); //reset raf to start of GDS
          gds = new Grib1GridDefinitionSection(raf);
          gdsv = gds.getGdsVars();
          isThin = gdsv.isThin();
          //System.out.println( "GDS isThin = " + isThin );
          // sigma vertical coordinates
        } else {   // NV != 0 && NV != 255
          raf.skipBytes(length - 5);
          //System.out.println( "GDS sigma vertical coordinates" );
        }
        // non standard sigma vertical coordinates
      } else if (length == ((NV * 4) + 32)) {
        raf.skipBytes(length - 5);
        // tighter critera if bmsExist could be an error
      } else if (length < 600) {
        if (P_VorL != 255) {
          raf.skipBytes(-5); //reset raf to start of GDS
          gds = new Grib1GridDefinitionSection(raf);
          gdsv = gds.getGdsVars();
          isThin = gdsv.isThin();
          //System.out.println( "GDS isThin = " + isThin );
          // sigma vertical coordinates
        } else {   // NV != 0 && NV != 255
          raf.skipBytes(length - 5);
          //System.out.println( "GDS sigma vertical coordinates" );
        }
      } else {
        raf.seek(offset);
      }
    } catch (NoValidGribException nvge) {
      log.error("gds exception was caught");
      raf.seek(offset);
    }

    // Need section 3 and 4 to read/interpet the data, section 5
    // as a check that all data read and sections are correct

    Grib1BitMapSection bms = null;
    if (bmsExists) {
      // read Bit Mapped Section 3
      bms = new Grib1BitMapSection(raf);
    }

    try {
      // read Binary Data Section 4
      Grib1BinaryDataSection bds = new Grib1BinaryDataSection(raf, decimalScale, bms );
      if (isThin) {
        QuasiRegular qr = new QuasiRegular(bds.getValues(), (Object) gds);
        return qr.getData();
      } else {
        return bds.getValues();
      }
    } catch (NotSupportedException notSupport) {
      log.error("Grib1BinaryDataSection exception was caught");
      return null;
    }
  }  // end getData

}  // end Grib1Data
