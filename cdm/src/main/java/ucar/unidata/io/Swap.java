/*
 * $Id: Swap.java,v 1.6 2005/05/13 18:31:26 jeffmc Exp $
 * 
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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

package ucar.unidata.io;



/**
 * The Swap class provides static methods for swapping the bytes of chars,
 * shorts, ints, longs, floats, and doubles.
 *
 * @author  Kirk Waters
 * @author  Russ Rew, 1998, added documentation
 * @version $Id: Swap.java,v 1.6 2005/05/13 18:31:26 jeffmc Exp $
 */

public class Swap {

    /**
     * Returns the short resulting from swapping 2 bytes at a specified
     * offset in a byte array.
     *
     * @param  b       the byte array
     * @param  offset  the offset of the first byte
     * @return         the short represented by the bytes
     *                 <code>b[offset+1], b[offset]</code>
     */
    static public short swapShort(byte[] b, int offset) {
        // 2 bytes
        int low  = b[offset] & 0xff;
        int high = b[offset + 1] & 0xff;
        return (short) (high << 8 | low);
    }

    /**
     * Returns the int resulting from reversing 4 bytes at a specified
     * offset in a byte array.
     *
     * @param  b       the byte array
     * @param  offset  the offset of the first byte
     * @return         the int represented by the bytes
     *                 <code>b[offset+3], b[offset+2], ..., b[offset]</code>
     */
    static public int swapInt(byte[] b, int offset) {
        // 4 bytes
        int accum = 0;
        for (int shiftBy = 0, i = offset; shiftBy < 32; shiftBy += 8, i++) {
            accum |= (b[i] & 0xff) << shiftBy;
        }
        return accum;
    }

    /**
     * Returns the long resulting from reversing 8 bytes at a specified
     * offset in a byte array.
     *
     * @param  b       the byte array
     * @param  offset  the offset of the first byte
     * @return         the long represented by the bytes
     *                 <code>b[offset+7], b[offset+6], ..., b[offset]</code>
     */
    static public long swapLong(byte[] b, int offset) {
        // 8 bytes
        long accum = 0;
        long shiftedval;
        for (int shiftBy = 0, i = offset; shiftBy < 64; shiftBy += 8, i++) {
            shiftedval = ((long) (b[i] & 0xff)) << shiftBy;
            accum      |= shiftedval;
        }
        return accum;
    }

    /**
     * Returns the float resulting from reversing 4 bytes at a specified
     * offset in a byte array.
     *
     * @param  b       the byte array
     * @param  offset  the offset of the first byte
     * @return         the float represented by the bytes
     *                 <code>b[offset+3], b[offset+2], ..., b[offset]</code>
     */
    static public float swapFloat(byte[] b, int offset) {
        int accum = 0;
        for (int shiftBy = 0, i = offset; shiftBy < 32; shiftBy += 8, i++) {
            accum |= (b[i] & 0xff) << shiftBy;
        }
        return Float.intBitsToFloat(accum);
    }

    /**
     * Returns the double resulting from reversing 8 bytes at a specified
     * offset in a byte array.
     *
     * @param  b       the byte array
     * @param  offset  the offset of the first byte
     * @return         the double represented by the bytes
     *                 <code>b[offset+7], b[offset+6], ..., b[offset]</code>
     */
    static public double swapDouble(byte[] b, int offset) {
        long accum = 0;
        long shiftedval;
        for (int shiftBy = 0, i = offset; shiftBy < 64; shiftBy += 8, i++) {
            shiftedval = ((long) (b[i] & 0xff)) << shiftBy;
            accum      |= shiftedval;
        }
        return Double.longBitsToDouble(accum);
    }

    /**
     * Returns the char resulting from swapping 2 bytes at a specified
     * offset in a byte array.
     *
     * @param  b       the byte array
     * @param  offset  the offset of the first byte
     * @return         the char represented by the bytes
     *                 <code>b[offset+1], b[offset]</code>
     */
    static public char swapChar(byte[] b, int offset) {
        // 2 bytes
        int low  = b[offset] & 0xff;
        int high = b[offset + 1] & 0xff;
        return (char) (high << 8 | low);
    }

    /**
     * Returns the short resulting from swapping 2 bytes of a specified
     * short.
     *
     * @param  s       input value for which byte reversal is desired
     * @return         the value represented by the bytes of <code>s</code>
     *                 reversed
     */
    static public short swapShort(short s) {
        return (swapShort(shortToBytes(s), 0));
    }

    /**
     * Returns the int resulting from reversing 4 bytes of a specified
     * int.
     *
     * @param  v       input value for which byte reversal is desired
     * @return         the value represented by the bytes of <code>v</code>
     *                 reversed
     */
    static public int swapInt(int v) {
        return (swapInt(intToBytes(v), 0));
    }

    /**
     * Returns the long resulting from reversing 8 bytes of a specified
     * long.
     *
     * @param  l       input value for which byte reversal is desired
     * @return         the value represented by the bytes of <code>l</code>
     *                 reversed
     */
    static public long swapLong(long l) {
        return (swapLong(longToBytes(l), 0));
    }

    /**
     * Returns the float resulting from reversing 4 bytes of a specified
     * float.
     *
     * @param  v       input value for which byte reversal is desired
     * @return         the value represented by the bytes of <code>v</code>
     *                 reversed
     */
    static public float swapFloat(float v) {
        int l = swapInt(Float.floatToIntBits(v));
        return (Float.intBitsToFloat(l));
    }

    /**
     * Returns the double resulting from reversing 8 bytes of a specified
     * double.
     *
     * @param  v       input value for which byte reversal is desired
     * @return         the value represented by the bytes of <code>v</code>
     *                 reversed
     */
    static public double swapDouble(double v) {
        long l = swapLong(Double.doubleToLongBits(v));
        return (Double.longBitsToDouble(l));
    }

    /**
     * Convert a short to an array of 2 bytes.
     *
     * @param  v       input value
     * @return         the corresponding array of bytes
     */
    static public byte[] shortToBytes(short v) {
        byte[] b       = new byte[2];
        int    allbits = 255;
        for (int i = 0; i < 2; i++) {
            b[1 - i] = (byte) ((v & (allbits << i * 8)) >> i * 8);
        }
        return b;
    }

    /**
     * Convert an int to an array of 4 bytes.
     *
     * @param  v       input value
     * @return         the corresponding array of bytes
     */
    static public byte[] intToBytes(int v) {
        byte[] b       = new byte[4];
        int    allbits = 255;
        for (int i = 0; i < 4; i++) {
            b[3 - i] = (byte) ((v & (allbits << i * 8)) >> i * 8);
        }
        return b;
    }

    /**
     * Convert a long to an array of 8 bytes.
     *
     * @param  v       input value
     * @return         the corresponding array of bytes
     */
    static public byte[] longToBytes(long v) {
        byte[] b       = new byte[8];
        long   allbits = 255;
        for (int i = 0; i < 8; i++) {
            b[7 - i] = (byte) ((v & (allbits << i * 8)) >> i * 8);
        }
        return b;
    }
}

/* Change History:
   $Log: Swap.java,v $
   Revision 1.6  2005/05/13 18:31:26  jeffmc
   Clean up the odd copyright symbols

   Revision 1.5  2004/01/29 17:37:02  jeffmc
   A big sweeping checkin after a big sweeping reformatting
   using the new jindent.

   jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
   templates there is a '_more_' to remind us to fill these in.

   Revision 1.4  2000/08/18 04:15:43  russ
   Licensed under GNU LGPL.

   Revision 1.3  1999/06/03 01:44:02  caron
   remove the damn controlMs

   Revision 1.2  1999/06/03 01:26:29  caron
   another reorg

   Revision 1.1.1.1  1999/05/21 17:33:47  caron
   startAgain

# Revision 1.2  1998/12/14  17:11:32  russ
# Add comment for accumulating change histories.
#
*/





