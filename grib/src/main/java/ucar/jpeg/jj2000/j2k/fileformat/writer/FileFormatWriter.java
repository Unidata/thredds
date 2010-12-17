/*
 * cvs identifier:
 *
 * $Id: FileFormatWriter.java,v 1.13 2001/02/16 11:53:54 qtxjoas Exp $
 * 
 * Class:                   FileFormatWriter
 *
 * Description:             Writes the file format
 *
 *
 *
 * COPYRIGHT:
 * 
 * This software module was originally developed by Rapha?l Grosbois and
 * Diego Santa Cruz (Swiss Federal Institute of Technology-EPFL); Joel
 * Askel?f (Ericsson Radio Systems AB); and Bertrand Berthelot, David
 * Bouchard, F?lix Henry, Gerard Mozelle and Patrice Onno (Canon Research
 * Centre France S.A) in the course of development of the JPEG2000
 * standard as specified by ISO/IEC 15444 (JPEG 2000 Standard). This
 * software module is an implementation of a part of the JPEG 2000
 * Standard. Swiss Federal Institute of Technology-EPFL, Ericsson Radio
 * Systems AB and Canon Research Centre France S.A (collectively JJ2000
 * Partners) agree not to assert against ISO/IEC and users of the JPEG
 * 2000 Standard (Users) any of their rights under the copyright, not
 * including other intellectual property rights, for this software module
 * with respect to the usage by ISO/IEC and Users of this software module
 * or modifications thereof for use in hardware or software products
 * claiming conformance to the JPEG 2000 Standard. Those intending to use
 * this software module in hardware or software products are advised that
 * their use may infringe existing patents. The original developers of
 * this software module, JJ2000 Partners and ISO/IEC assume no liability
 * for use of this software module or modifications thereof. No license
 * or right to this software module is granted for non JPEG 2000 Standard
 * conforming products. JJ2000 Partners have full right to use this
 * software module for his/her own purpose, assign or donate this
 * software module to any third party and to inhibit third parties from
 * using this software module for non JPEG 2000 Standard conforming
 * products. This copyright notice must be included in all copies or
 * derivative works of this software module.
 * 
 * Copyright (c) 1999/2000 JJ2000 Partners.
 *  */
package ucar.jpeg.jj2000.j2k.fileformat.writer;

import ucar.jpeg.jj2000.j2k.codestream.*;
import ucar.jpeg.jj2000.j2k.fileformat.*;
import ucar.jpeg.jj2000.j2k.io.*;

import java.io.*;

/**
 * This class writes the file format wrapper that may or may not exist around
 * a valid JPEG 2000 codestream. This class writes the simple possible legal
 * fileformat
 *
 * @see jj2000.j2k.fileformat.reader.FileFormatReader
 * */
public class FileFormatWriter implements FileFormatBoxes{
    
    /** The file from which to read the codestream and write file*/
    private BEBufferedRandomAccessFile fi;

    /** The name of the file from which to read the codestream and to write
     * the JP2 file*/
    private String filename;

    /** Image height */
    private int height;

    /** Image width */
    private int width;

    /** Number of components */
    private int nc;

    /** Bits per component */
    private int bpc[];

    /** Flag indicating whether number of bits per component varies */
    private boolean bpcVaries;

    /** Length of codestream */
    private int clength;

    /** Length of Colour Specification Box */
    private static final int CSB_LENGTH = 15;

    /** Length of File Type Box */
    private static final int FTB_LENGTH = 20;

    /** Length of Image Header Box */
    private static final int IHB_LENGTH = 22;

    /** base length of Bits Per Component box */
    private static final int BPC_LENGTH = 8;

    

    /** 
     * The constructor of the FileFormatWriter. It receives all the
     * information necessary about a codestream to generate a legal JP2 file
     *
     * @param filename The name of the file that is to be made a JP2 file
     *
     * @param height The height of the image
     *
     * @param width The width of the image
     *
     * @param nc The number of components
     *
     * @param bpc The number of bits per component
     * 
     * @param clength Length of codestream 
     * */
    public FileFormatWriter(String filename, int height, int width, int nc, 
                            int[] bpc, int clength){ 
        this.height = height;
        this.width = width;
        this.nc = nc;
        this.bpc = bpc;
        this.filename=filename;
        this.clength = clength;

        bpcVaries=false;
        int fixbpc = bpc[0];
        for(int i=nc-1; i>0 ; i--){
            if(bpc[i]!=fixbpc)
                bpcVaries = true;
        }
          
    }



    /** 
     * This method reads the codestream and writes the file format wrapper and
     * the codestream to the same file
     *
     * @return The number of bytes increases because of the file format
     *
     * @exception java.io.IOException If an I/O error ocurred.
     * */
    public int writeFileFormat() throws IOException {
        byte[] codestream;

        try{
            // Read and buffer the codestream
            fi = new BEBufferedRandomAccessFile(filename,"rw+");
            codestream= new byte[clength];
            fi.readFully(codestream, 0, clength);

            // Write the JP2_SINATURE_BOX
            fi.seek(0);
            fi.writeInt(0x0000000c);
            fi.writeInt(JP2_SIGNATURE_BOX);
            fi.writeInt(0x0d0a870a);
            
            // Write File Type box
            writeFileTypeBox();
            
            // Write JP2 Header box
            writeJP2HeaderBox();

            // Write the Codestream box 
            writeContiguousCodeStreamBox(codestream);

            fi.close();

        }
        catch(Exception e){
            throw new Error("Error while writing JP2 file format");
        }
        if(bpcVaries)
            return 12+FTB_LENGTH+8+IHB_LENGTH+CSB_LENGTH+BPC_LENGTH+nc+8;
        else
            return 12+FTB_LENGTH+8+IHB_LENGTH+CSB_LENGTH+8;

    }

    /** 
     * This method writes the File Type box
     *
     * @exception java.io.IOException If an I/O error ocurred.
     * */
    public void writeFileTypeBox()throws IOException {
        // Write box length (LBox)
        // LBox(4) + TBox (4) + BR(4) + MinV(4) + CL(4) = 20
        fi.writeInt(FTB_LENGTH);

        // Write File Type box (TBox)
        fi.writeInt(FILE_TYPE_BOX);

        // Write File Type data (DBox)
        // Write Brand box (BR)
        fi.writeInt(FT_BR);

        // Write Minor Version
        fi.writeInt(0);
        
        // Write Compatibility list
        fi.writeInt(FT_BR);
        
    }

    /** 
     * This method writes the JP2Header box
     *
     * @exception java.io.IOException If an I/O error ocurred.
     * */
    public void writeJP2HeaderBox()throws IOException {

        // Write box length (LBox)
        // if the number of bits per components varies, a bpcc box is written
        if(bpcVaries)
            fi.writeInt(8+IHB_LENGTH+CSB_LENGTH+BPC_LENGTH+nc);
        else
            fi.writeInt(8+IHB_LENGTH+CSB_LENGTH);

        // Write a JP2Header (TBox)
        fi.writeInt(JP2_HEADER_BOX);

        // Write image header box 
        writeImageHeaderBox();

        // Write Colour Bpecification Box
        writeColourSpecificationBox();

        // if the number of bits per components varies write bpcc box
        if(bpcVaries)
            writeBitsPerComponentBox();
    }

    /** 
     * This method writes the Bits Per Component box
     *
     * @exception java.io.IOException If an I/O error ocurred.
     *
     */
    public void writeBitsPerComponentBox()throws IOException {

        // Write box length (LBox)
        fi.writeInt(BPC_LENGTH+nc);

        // Write a Bits Per Component box (TBox)
        fi.writeInt(BITS_PER_COMPONENT_BOX);

        // Write bpc fields
        for(int i=0;i<nc;i++){
          fi.writeByte(bpc[i]-1);
        }
    }

    /** 
     * This method writes the Colour Specification box
     *
     * @exception java.io.IOException If an I/O error ocurred.
     *
     */
    public void writeColourSpecificationBox()throws IOException {

        // Write box length (LBox)
        fi.writeInt(CSB_LENGTH);

        // Write a Bits Per Component box (TBox)
        fi.writeInt(COLOUR_SPECIFICATION_BOX);

        // Write METH field
        fi.writeByte(CSB_METH);

        // Write PREC field
        fi.writeByte(CSB_PREC);

        // Write APPROX field
        fi.writeByte(CSB_APPROX);

        // Write EnumCS field
        if(nc>1)
            fi.writeInt(CSB_ENUM_SRGB);
        else
            fi.writeInt(CSB_ENUM_GREY);       
    }

    /** 
     * This method writes the Image Header box
     *
     * @exception java.io.IOException If an I/O error ocurred.
     * */
    public void writeImageHeaderBox()throws IOException {

        // Write box length
        fi.writeInt(IHB_LENGTH);

        // Write ihdr box name
        fi.writeInt(IMAGE_HEADER_BOX);

        // Write HEIGHT field
        fi.writeInt(height);

        // Write WIDTH field
        fi.writeInt(width);

        // Write NC field
        fi.writeShort(nc);

        // Write BPC field
        // if the number of bits per component varies write 0xff else write
        // number of bits per components
        if(bpcVaries)
            fi.writeByte(0xff);
        else
            fi.writeByte(bpc[0]-1);

        // Write C field
        fi.writeByte(IMB_C);

        // Write UnkC field
        fi.writeByte(IMB_UnkC);

        // Write IPR field
        fi.writeByte(IMB_IPR);

    }

    /** 
     * This method writes the Contiguous codestream box
     *
     * @param cs The contiguous codestream
     *
     * @exception java.io.IOException If an I/O error ocurred.
     * */
    public void writeContiguousCodeStreamBox(byte[] cs)throws IOException {

        // Write box length (LBox)
        // This value is set to 0 since in this implementation, this box is
        // always last
        fi.writeInt(clength+8);

        // Write contiguous codestream box name (TBox)
        fi.writeInt(CONTIGUOUS_CODESTREAM_BOX);
                
        // Write codestream
        for(int i=0; i<clength ;i++)
            fi.writeByte(cs[i]);
    }          
} 
