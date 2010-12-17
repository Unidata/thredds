/*
 * CVS identifier:
 *
 * $Id: ImgWriterArray.java,v 1.0 2004/11/02 14:10:46 rkambic Exp $
 *
 * Class:                   ImgWriterArray
 *
 * Description:             Image Writer for Array format
 *
 *
 *
 * */
package ucar.jpeg.jj2000.j2k.image.output;

import ucar.jpeg.jj2000.j2k.image.*;
import ucar.jpeg.jj2000.j2k.util.*;

import java.io.*;

/**
 * This class extends the ImgWriter abstract class for writing Array .  
 *
 * <u>Data:</u> The image binary values appear one after the other (in raster
 * order) immediately after the last header character ('\n') and are
 * byte-aligned (they are packed into 1,2 or 4 bytes per sample, depending
 * upon the bit-depth value).
 * </p>
 *
 * <p> If the data is unsigned, level shifting is applied adding 2^(bit depth
 * - 1)</p>
 *
 * <p><u>NOTE</u>: This class is not thread safe, for reasons of internal
 * buffering.</p>
 *
 * @see ImgWriter
 *
 * @see BlkImgDataSrc
 * */
public class ImgWriterArray extends ImgWriter {

    /** Whether the data must be signed when writing or not. In the latter
     * case inverse level shifting must be applied */
    boolean isSigned;

    /** The bit-depth of the input file (must be between 1 and 31)*/
    private int bitDepth;

    /** A DataBlk, just used to avoid allocating a new one each time it is
        needed */
    private DataBlkInt db = new DataBlkInt();

    /** The number of fractional bits in the source data */
    //private int fb;

    /** The index of the component from where to get the data */
    private int c;

    /** The pack length of one sample (in bytes, according to the output
        bit-depth */
    private int packBytes;

    /**
     * Creates a new writer to the specified Array object, to write data from
     * the specified component.
     *
     * <p>The size of the image that is written to the file is the size of the
     * component from which to get the data, specified by b, not the size of
     * the source image (they differ if there is some sub-sampling).</p>
     *
     *
     * @param imgSrc The source from where to get the image data to write.
     *
     * @param c The index of the component from where to get the data.
     *
     * @param isSigned Whether the datas are signed or not (needed only when
     * writing header).
     *
     * @see DataBlk
     * */
    public ImgWriterArray(BlkImgDataSrc imgSrc, 
			int c, boolean isSigned) throws IOException {
        //Initialize
        this.c = c;
        this.isSigned = isSigned;
        //System.out.println("sign = " + isSigned );
        src = imgSrc;
        w = src.getImgWidth();
        h = src.getImgHeight();
        //System.out.println(" constructor iwa w=" + w +" h=" +h ) ;

        bitDepth = src.getNomRangeBits(this.c);
        if((bitDepth<=0)||(bitDepth>31)) {
            throw new IOException("Array supports only bit-depth between "+
                                  "1 and 31");
	}
        if(bitDepth<=8) {
            packBytes = 1;
        } else if(bitDepth<=16) {
            packBytes = 2;
        } else { // <= 31
            packBytes = 4;
	}

    } // end ImgWriterArray

    /**
     * Closes the underlying file or netwrok connection to where the data is
     * written. Any call to other methods of the class become illegal after a
     * call to this one.
     *
     * @exception IOException If an I/O error occurs.
     * */
    public void close() throws IOException {
    }

    /**
     * Writes the data of the specified area to the file, coordinates are
     * relative to the current tile of the source. Before writing, the
     * coefficients are limited to the nominal range and packed into 1,2 or 4
     * bytes (according to the bit-depth).
     *
     * <p>If the data is unisigned, level shifting is applied adding 2^(bit
     * depth - 1)</p>
     *
     * <p>This method may not be called concurrently from different
     * threads.</p> 
     *
     * <p>If the data returned from the BlkImgDataSrc source is progressive,
     * then it is requested over and over until it is not progressive
     * anymore.</p>
     *
     * @param ulx The horizontal coordinate of the upper-left corner of the
     * area to write, relative to the current tile.
     *
     * @param uly The vertical coordinate of the upper-left corner of the area
     * to write, relative to the current tile.
     *
     * @param width The width of the area to write.
     *
     * @param height The height of the area to write.
     *
     * @exception IOException If an I/O error occurs.
     * */
    public void write(int ulx, int uly, int w, int h) throws IOException {

        //System.out.println( " ulx=" + ulx +" uly=" + uly +" w=" + w +" h=" + h);
        // Initialize db
        db.ulx = ulx;
        db.uly = uly;
        db.w = w;
        db.h = h;
        if(db.data!=null && db.data.length<w*h) {
            // A new one will be allocated by getInternCompData()
            db.data = null;
        }
        // Request the data and make sure it is not
        // progressive
        do {
            db = (DataBlkInt) src.getInternCompData(db,c);
	    //System.out.println( "Progressive Comp c =" + c );
        } while (db.progressive);

	//System.out.println( "Comp c =" + c );
        //System.out.println( "db.data.length = " + db.data.length );

    } // end int ulx, int uly, int w, int h
    
    public void writeAll() throws IOException {
        // Find the list of tile to decode.
        Coord nT = src.getNumTiles(null);
        //System.out.println( "nTiles = " + nT );

        // Loop on vertical tiles
        for(int y=0; y<nT.y; y++){
            // Loop on horizontal tiles
            for(int x=0; x<nT.x; x++){
                //System.out.println( "setTiles(x,y) = " + x + ", " + y );
                src.setTile(x,y);
                write( 0, 0, src.getImgWidth(), src.getImgHeight() );
            } // End loop on horizontal tiles
        } // End loop on vertical tiles
    }

    /**
     * Writes the source's current tile to the output. The requests of data
     * issued to the source BlkImgDataSrc object are done by strips, in order
     * to reduce memory usage.
     *
     * <p>If the data returned from the BlkImgDataSrc source is progressive,
     * then it is requested over and over until it is not progressive
     * anymore.</p>
     *
     * @exception IOException If an I/O error occurs.
     *
     * @see DataBlk
     * */
    public void write() throws IOException {
        int i;
        int tIdx = src.getTileIdx();
        int tw = src.getTileCompWidth(tIdx,c);  // Tile width
        int th = src.getTileCompHeight(tIdx,c);  // Tile height
        // Write in strips
        for(i=0; i<th ; i+=DEF_STRIP_HEIGHT) {
            write(0,i,tw,(th-i<DEF_STRIP_HEIGHT) ? th-i : DEF_STRIP_HEIGHT);
        }
    }
    
    /** The pack length of one sample (in bytes, according to the output
        bit-depth */
    public int getPackBytes() {
       return packBytes;
    }

    /**
     * the jpeg data decoded into a array
     *
     * @return a byte[]
     * */
    public int[] getGdata() {
       //return gdata;
       return db.data;
    }

    public void flush() {
    }

    /**
     * Returns a string of information about the object, more than 1 line
     * long. The information string includes information from the underlying
     * RandomAccessFile (its toString() method is called in turn).
     *
     * @return A string of information about the object.
     * */
    public String toString() {
        return "ImgWriterArray: WxH = " + w + "x" + h + ", Component = "+
            c + ", Bit-depth = "+bitDepth + ", signed = "+isSigned + 
            "\nUnderlying RandomAccessFile:\n" + db.data.toString();
    }
} // end ImgWriterArray
