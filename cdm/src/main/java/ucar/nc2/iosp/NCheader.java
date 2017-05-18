/*
 * Copyright 1998-2017 University Corporation for Atmospheric Research/Unidata
 *  See the LICENSE file for more information.
 */

package ucar.nc2.iosp;

import java.io.IOException;
import java.util.Arrays;

/**
 * Common header for netcdf3, netcdf4, hdf5, hdf4.
 *
 * @author dmh
 */

public class NCheader
{
    //////////////////////////////////////////////////
    // Constants

    // Constants for check_file_type
    static private final int MAGIC_NUMBER_LEN = 8;
    static private final long MAXHEADERPOS = 50000; // header's gotta be within this range

    static public final byte[] H5HEAD = {(byte)0x89, 'H', 'D', 'F', '\r', '\n', (byte) 0x1a, '\n'};
    static public final byte[] H4HEAD = {(byte)0x0e, (byte)0x03, (byte)0x13, (byte)0x01};
    static public final byte[] CDF1HEAD = {(byte)'C', (byte)'D', (byte)'F', (byte)0x01};
    static public final byte[] CDF2HEAD = {(byte)'C', (byte)'D', (byte)'F', (byte)0x02};
    static public final byte[] CDF5HEAD = {(byte)'C', (byte)'D', (byte)'F', (byte)0x05};

    // These should match the constants in netcdf-c/include/netcdf.h
    static public final int NC_FORMAT_NETCDF3 = (1);
    static public final int NC_FORMAT_64BIT_OFFSET = (2);
    static public final int NC_FORMAT_NETCDF4 = (3);
    static public final int NC_FORMAT_NETCDF4_CLASSIC = (4);
    static public final int NC_FORMAT_64BIT_DATA = (5);

    // Extensions
    static public final int NC_FORMAT_HDF4 = (0x7005);

    // Aliases
    static public final int NC_FORMAT_CLASSIC = (NC_FORMAT_NETCDF3);
    static public final int NC_FORMAT_64BIT = (NC_FORMAT_64BIT_OFFSET);
    static public final int NC_FORMAT_CDF5 = (NC_FORMAT_64BIT_DATA);
    static public final int NC_FORMAT_HDF5 = (NC_FORMAT_NETCDF4);

    //////////////////////////////////////////////////
    // Static Methods

    /**
     * Figure out what kind of netcdf-related file we have.
     * Constraint: leave raf read pointer to point just after
     * the magic number.
     *
     * @param raf to test type
     * @return
     * @throws IOException
     */
    static public int
    checkFileType(ucar.unidata.io.RandomAccessFile raf)
            throws IOException
    {
        int format = 0;

        byte[] magic = new byte[MAGIC_NUMBER_LEN];

        // If this is not an HDF5 file, then the magic number is at
        // position 0; If it is an HDF5/4 file, then we need to search
        // forward for it.

        // Look for the relevant leading tag
        raf.seek(0);
        if(raf.readBytes(magic, 0, MAGIC_NUMBER_LEN) < MAGIC_NUMBER_LEN)
            return 0; // unknown
        // Some version of CDF
        int hdrlen = 0;
        byte[] byte4 = Arrays.copyOfRange(magic,0,CDF1HEAD.length);
        hdrlen = CDF1HEAD.length;
        format = 0;
        if(Arrays.equals(CDF1HEAD,byte4))
            format = NC_FORMAT_CLASSIC;
        else if(Arrays.equals(CDF2HEAD,byte4))
            format = NC_FORMAT_64BIT_OFFSET;
        else if(Arrays.equals(CDF1HEAD,byte4))
            format = NC_FORMAT_CDF5;
        if(format != 0) {
            raf.seek(hdrlen);
            return format;
        }
        // For HDF5/4, we need to search forward
        hdrlen = 0;
        format = 0;
        long filePos = 0;
        long size = raf.length();
        while((filePos < size - 8) && (filePos < MAXHEADERPOS)) {
            boolean match;
            raf.seek(filePos);
            if(raf.readBytes(magic, 0, MAGIC_NUMBER_LEN) < MAGIC_NUMBER_LEN)
                return 0; // unknown
            // Test for HDF4
            byte4 = Arrays.copyOfRange(magic,0,H4HEAD.length);
            if(Arrays.equals(H4HEAD,byte4)) {
                format = NC_FORMAT_HDF4;
                hdrlen = H4HEAD.length;
                break;
            }
            // Test for HDF5
            if(Arrays.equals(H5HEAD,magic)) {
                format = NC_FORMAT_HDF5;
                hdrlen = H5HEAD.length;
                break;
            }
            filePos = (filePos == 0) ? 512 : 2 * filePos;
        }
        if(format != 0)
            raf.seek(filePos+hdrlen);
        return format;
    }
}
