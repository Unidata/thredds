/*
 * CVS identifier:
 *
 * $Id: CodestreamWriter.java,v 1.11 2001/07/24 17:03:30 grosbois Exp $
 *
 * Class:                   CodestreamWriter
 *
 * Description:             Interface for writing bit streams
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
 * */
package ucar.jpeg.jj2000.j2k.codestream.writer;

import java.io.*;

/**
 * This is the abstract class for writing to a codestream. A codestream
 * corresponds to headers (main and tile-parts) and packets. Each packet has a
 * head and a body. The codestream always has a maximum number of bytes that
 * can be written to it. After that many number of bytes no more data is
 * written to the codestream but the number of bytes is counted so that the
 * value returned by getMaxAvailableBytes() is negative. If the number of
 * bytes is unlimited a ridicoulosly large value, such as Integer.MAX_VALUE,
 * is equivalent.
 *
 * <p>Data writting to the codestream can be simulated. In this case, no byto
 * is effectively written to the codestream but the resulting number of bytes
 * is calculated and returned (although it is not accounted in the bit
 * stream). This can be used in rate control loops.</p>
 *
 * <p>Implementing classes should write the header of the bit stream before
 * writing any packets. The bit stream header can be written with the help of
 * the HeaderEncoder class.</p>
 *
 * @see HeaderEncoder
 * */
public abstract class CodestreamWriter {

    /** The number of bytes already written to the bit stream */
    protected int ndata=0;

    /** The maximum number of bytes that can be written to the bit stream */
    protected int maxBytes;

    /**
     * Allocates this object and initializes the maximum number of bytes.
     *
     * @param mb The maximum number of bytes that can be written to the
     * codestream.
     * */
    protected CodestreamWriter(int mb) {
        maxBytes = mb;
    }

    /**
     * Returns the number of bytes remaining available in the codestream. This
     * is the maximum allowed number of bytes minus the number of bytes that
     * have already been written to the bit stream. If more bytes have been
     * written to the bit stream than the maximum number of allowed bytes,
     * then a negative value is returned.
     *
     * @return The number of bytes remaining available in the bit stream.
     * */
    public abstract int getMaxAvailableBytes();

    /**
     * Returns the current length of the entire codestream.
     *
     * @return the current length of the codestream
     * */
    public abstract int getLength();

    /**
     * Writes a packet head into the codestream and returns the number of
     * bytes used by this header. If in simulation mode then no data is
     * effectively written to the codestream but the number of bytes is
     * calculated. This can be used for iterative rate allocation.
     *
     * <p>If the number of bytes that has to be written to the codestream is
     * more than the space left (as returned by getMaxAvailableBytes()), only
     * the data that does not exceed the allowed length is effectively written
     * and the rest is discarded. However the value returned by the method is
     * the total length of the packet, as if all of it was written to the bit
     * stream.</p>
     *
     * <p>If the codestream header has not been commited yet and if 'sim' is
     * false, then the bit stream header is automatically commited (see
     * commitBitstreamHeader() method) before writting the packet.
     *
     * @param head The packet head data.
     *
     * @param hlen The number of bytes in the packet head.
     *
     * @param sim Simulation mode flag. If true nothing is written to the bit
     * stream, but the number of bytes that would be written is returned.
     *
     * @param sop Start of packet header marker flag. This flag indicates
     * whether or not SOP markers should be written. If true, SOP markers
     * should be written, if false, they should not.
     *
     * @param eph End of Packet Header marker flag. This flag indicates
     * whether or not EPH markers should be written. If true, EPH markers
     * should be written, if false, they should not.
     *
     * @return The number of bytes spent by the packet head.
     *
     * @exception IOException If an I/O error occurs while writing to the
     * output stream.
     *
     * @see #commitBitstreamHeader
     * */ 
    public abstract int writePacketHead(byte head[],int hlen,boolean sim, 
					boolean sop, boolean eph)
        throws IOException;

    /**
     * Writes a packet body to the codestream and returns the number of bytes
     * used by this body. If in simulation mode then no data is written to the
     * bit stream but the number of bytes is calculated. This can be used for
     * iterative rate allocation.
     *
     * <p>If the number of bytes that has to be written to the codestream is
     * more than the space left (as returned by getMaxAvailableBytes()), only
     * the data that does not exceed the allowed length is effectively written
     * and the rest is discarded. However the value returned by the method is
     * the total length of the packet, as if all of it was written to the bit
     * stream.</p>
     *
     * @param body The packet body data.
     *
     * @param blen The number of bytes in the packet body.
     *
     * @param sim Simulation mode flag. If true nothing is written to the bit
     * stream, but the number of bytes that would be written is returned.
     *
     * @param roiInPkt Whether or not there is ROI information in this packet
     *
     * @param roiLen Number of byte to read in packet body to get all the ROI
     * information 
     *
     * @return The number of bytes spent by the packet body.
     *
     * @exception IOException If an I/O error occurs while writing to the
     * output stream.
     *
     * @see #commitBitstreamHeader
     * */ 
    public abstract int writePacketBody(byte body[],int blen,boolean sim,
                                        boolean roiInPkt, int roiLen) 
        throws IOException;


    /**
     * Closes the underlying resource (file, stream, network connection,
     * etc.). After a CodestreamWriter is closed no more data can be written
     * to it.
     *
     * @exception IOException If an I/O error occurs while closing the
     * resource.
     * */
    public abstract void close() throws IOException;
    
    /**
     * Writes the header data to the bit stream, if it has not been already
     * done. In some implementations this method can be called only once, and
     * an IllegalArgumentException is thrown if called more than once.
     *
     * @exception IOException If an I/O error occurs while writing the data.
     *
     * @exception IllegalArgumentException If this method has already been
     * called.
     * */
    public abstract void commitBitstreamHeader(HeaderEncoder he)
        throws IOException;

    /** 
     * Gives the offset of the end of last packet containing ROI information 
     *
     * @return End of last ROI packet 
     * */
    public abstract int getOffLastROIPkt();
}
