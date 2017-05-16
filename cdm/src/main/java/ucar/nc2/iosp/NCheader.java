/*
 * Copyright 1998-2017 University Corporation for Atmospheric Research/Unidata
 *  See the LICENSE file for more information.
 */

package ucar.nc2.iosp;

import java.io.IOException;

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

    static private final byte[] H5HEAD = {(byte) 0x89, 'H', 'D', 'F', '\r', '\n', (byte) 0x1a, '\n'};

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

    static public int
    checkFileType(ucar.unidata.io.RandomAccessFile raf)
            throws IOException
    {
        byte[] magic = new byte[MAGIC_NUMBER_LEN];

        // If this is not an HDF5 file, then the magic number is at
        // position 0; If it is an HDF5/4 file, then we need to search
        // forward for it.

        // Look for the relevant leading tag
        raf.seek(0);
        if(raf.readBytes(magic, 0, MAGIC_NUMBER_LEN) < MAGIC_NUMBER_LEN)
            return 0; // unknown
        // Some version of CDF
        if(magic[0] == (byte) 'C'
                && magic[1] == (byte) 'D'
                && magic[2] == (byte) 'F') {
            if(magic[3] == 0x01) return NC_FORMAT_CLASSIC;
            if(magic[3] == 0x02) return NC_FORMAT_64BIT_OFFSET;
            if(magic[3] == 0x05) return NC_FORMAT_CDF5;
            return 0; // unknown
        }
        // For HDF5/4, we need to search forward
        long filePos = 0;
        long size = raf.length();
        while((filePos < size - 8) && (filePos < MAXHEADERPOS)) {
            boolean match;
            raf.seek(filePos);
            if(raf.readBytes(magic, 0, MAGIC_NUMBER_LEN) < MAGIC_NUMBER_LEN)
                return 0; // unknown
            // Test for HDF5; Need to use full set of Header bytes
            match = true;
            for(int i = 0; i < H5HEAD.length; i++) {
                if(magic[i] != H5HEAD[i]) {
                    match = false;
                    break;
                }
            }
            if(match)
                return NC_FORMAT_HDF5;
            // Try HDF4
            if(magic[0] == (byte) 0x0e
                    && magic[1] == (byte) 0x03
                    && magic[2] == (byte) 0x13
                    && magic[3] == (byte) 0x01) {
                return NC_FORMAT_HDF4;
            }
            filePos = (filePos == 0) ? 512 : 2 * filePos;
        }
        return 0;
    }
}
