/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/


package dap4.servlet;

import dap4.core.dmr.*;
import dap4.core.util.DapException;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;

public class RandomValue extends Value
{
    //////////////////////////////////////////////////
    // Constants

    static final long SEED = 37L; 

    //////////////////////////////////////////////////
    // Instance variables

    Random random = new Random(SEED);

    //////////////////////////////////////////////////
    // Constructor(s)

    public
    RandomValue()
    {
    }

    //////////////////////////////////////////////////
    // IValue Interface

    public ValueSource source() {return ValueSource.RANDOM;}

    public Object
    nextValue(DapType basetype)
        throws DapException
    {
        AtomicType atomtype = basetype.getAtomicType();
        long l = 0;
        if(atomtype.isIntegerType()) {
            l = (int) random.nextInt(MAXINT);
            if(atomtype.isUnsigned()) {
                BigInteger bi = BigInteger.valueOf(l);
                bi = bi.and(MASK);
                l = bi.longValue();
            }
        }

        switch (atomtype) {
        case Int8:
        case UInt8:
            return new Byte((byte) (l & 0xFF));
        case Int16:
            return new Short((short) l);
        case UInt16:
            return new Short((short) (l & 0xFFFF));
        case Int32:
            return new Integer((int) l);
        case UInt32:
            return new Integer((int) (l & 0xFFFFFFFF));
        case Int64:
        case UInt64:
            return new Long(l);

        case Float32:
            return new Float(random.nextFloat());
        case Float64:
            return new Double(random.nextDouble());

        case Char:
            return nextString(random, 1, 32, 127).charAt(0);

        case String:
            return nextString(random, MAXSTRINGSIZE, 32, 127);

        case URL:
            return nextURL();

        case Opaque:
            int length = 2 + (random.nextInt(MAXOPAQUESIZE) * 2);
            byte[] bytes = new byte[length];
            random.nextBytes(bytes);
            return ByteBuffer.wrap(bytes);

        case Enum:
            return nextEnum(((DapEnum)basetype));

        default:
            throw new DapException("Unexpected type: " + basetype);
        }
    }

    Object
    nextEnum(DapEnum en)
    {
        long l;
        AtomicType basetype = en.getBaseType().getAtomicType();

        // Collect the enum const values as BigIntegers
        List<String> ecnames = en.getNames();
        BigInteger[] econsts = new BigInteger[ecnames.size()];
        for(int i=0;i<econsts.length;i++) {
            l = en.lookup(ecnames.get(i));
            econsts[i] = BigInteger.valueOf(l);
            if(basetype == AtomicType.UInt64)
                econsts[i] = econsts[i].and(MASK);
        }

        int index = random.nextInt(econsts.length);
        l = econsts[index].longValue();
        Object val = null;
        switch (basetype) {
        case Int8: val = new Byte((byte)l); break;
        case UInt8: val = new Byte((byte)(l & 0xFFL)); break;
        case Int16: val = new Short((short)l); break;
        case UInt16: val = new Short((short)(l & 0xFFFFL)); break;
        case Int32: val = new Integer((int)l); break;
        case UInt32: val = new Integer((int)(l & 0xFFFFFFFFL)); break;
        case Int64: val = new Long(l); break;
        case UInt64: val = new Long(l); break;
        }
        return val;
    }

    String
    nextString(Random random, int maxlength, int min, int max)
    {
        int length = random.nextInt(maxlength);
        if(length == 0) length = 1;
        StringBuilder buf = new StringBuilder();
        for(int i = 0;i < length;i++) {
            int c = random.nextInt((max - min)) + min;
            buf.append((char) c);
        }
        return buf.toString();
    }

    static final String[] protocols = new String[]{"http", "https"};
    static final String legal =
        "abcdefghijklmnoqqrstuvwxyz"
            + "ABCDEFGHIJKLMNOQQRSTUVWXYZ"
            + "0123456789"
            + "_";

    String
    nextURL()
    {
        StringBuilder url = new StringBuilder();
        url.append(protocols[random.nextInt(protocols.length)]);
        url.append("://");
        for(int i = 0;i < HOSTNSEG;i++) {
            if(i > 0) url.append(".");
            for(int j = 0;j < MAXSEGSIZE;j++) {
                int c;
                do {
                    c = random.nextInt('z');
                } while(legal.indexOf(c) < 0);
                url.append((char) c);
            }
        }
        if(random.nextBoolean())
            url.append(String.format(":%d", random.nextInt(5000) + 1));
        for(int i = 0;i < PATHNSEG;i++) {
            url.append("/");
            for(int j = 0;j < MAXSEGSIZE;j++) {
                int c;
                do {
                    c = random.nextInt('z');
                } while(legal.indexOf(c) < 0);
                url.append((char) c);
            }
        }
        return url.toString();
    }


}
