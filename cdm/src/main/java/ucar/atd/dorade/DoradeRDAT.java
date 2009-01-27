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
package ucar.atd.dorade;

/**
 * <p>Title: DoradeRDAT</p>
 * <p>Description: DORADE data block</p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: University Corporation for Atmospheric Research</p>
 * @author Chris Burghart
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */
/* $Id:DoradeRDAT.java 51 2006-07-12 17:13:13Z caron $ */

import java.io.RandomAccessFile;

class DoradeRDAT extends DoradeDescriptor {

    private String paramName;
    private byte[] paramData;

    public DoradeRDAT(RandomAccessFile file, boolean littleEndianData)
            throws DescriptorException {
        byte[] data = readDescriptor(file, littleEndianData, "RDAT");

        //
        // unpack
        //
        paramName = new String(data, 8, 8).trim();
        paramData = new byte[data.length - 16];
        System.arraycopy(data, 16, paramData, 0, data.length - 16);

        //
        // debugging output
        //
        if (verbose)
            System.out.println(this);
    }

    public String toString() {
        String s = "RDAT\n";
        s += "  param name: " + paramName + "\n";
        s += "  data length: " + (int)(paramData.length);
        return s;
    }

    public String getParamName() {
        return paramName;
    }

    public byte[] getRawData() {
        return paramData;
    }

    public static DoradeRDAT getNextOf(DoradePARM parm, RandomAccessFile file,
                                       boolean littleEndianData)
            throws DescriptorException, java.io.IOException {
        while (true) {
            long pos = findNextWithName("RDAT", file, littleEndianData);
            if (peekParamName(file).equals(parm.getName()))
                return new DoradeRDAT(file, littleEndianData);
            else
                skipDescriptor(file, littleEndianData);
        }
    }

    private static String peekParamName(RandomAccessFile file)
            throws DescriptorException {
        try {
            long filepos = file.getFilePointer();
            file.skipBytes(8);
            byte[] nameBytes = new byte[8];
            if (file.read(nameBytes) == -1)
                throw new DescriptorException("unexpected EOF");
            file.seek(filepos);
            return new String(nameBytes).trim();
        } catch (Exception ex) {
            throw new DescriptorException(ex);
        }
    }

}