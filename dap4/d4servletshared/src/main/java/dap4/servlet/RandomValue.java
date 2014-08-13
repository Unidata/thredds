/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/


package dap4.servlet;

import dap4.core.dmr.*;
import dap4.core.util.DapException;
import dap4.dap4shared.Dap4Util;

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

    protected Random random = new Random(SEED);

    //////////////////////////////////////////////////
    // Constructor(s)

    public RandomValue()
    {
    }

    //////////////////////////////////////////////////
    // Accessors
    
    //////////////////////////////////////////////////
    // IValue Interface

    public ValueSource source()
    {
        return ValueSource.RANDOM;
    }

    public Object
    nextValue(DapType basetype)
        throws DapException
    {
	Object value = null;
        AtomicType atomtype = basetype.getAtomicType();
        if(atomtype.isIntegerType())
            value = nextInteger(basetype);
        else if(atomtype.isFloatType())
            value = nextFloat(basetype);
        else switch (atomtype) {
        case Int8:
        case UInt8:
        case Int16:
        case UInt16:
        case Int32:
        case UInt32:
        case Int64:
        case UInt64:
            value = nextInteger(basetype);
	    break;
        case Float32:
        case Float64:
            value = nextFloat(basetype);
	    break;

        case Char:
            value = nextString(random, 1, 32, 127).charAt(0);
	    break;
        case String:
            value = nextString(random, MAXSTRINGSIZE, 32, 127);
	    break;
        case URL:
            value = nextURL();
	    break;
        case Opaque:
            int length = 2 + (random.nextInt(MAXOPAQUESIZE) * 2);
            byte[] bytes = new byte[length];
            random.nextBytes(bytes);
            value = ByteBuffer.wrap(bytes);// order is irrelevant
	    break;
        case Enum:
            //Coverity[FB.BC_UNCONFIRMED_CAST]
            value = nextEnum(((DapEnum) basetype));
	    break;
        default:
            throw new DapException("Unexpected type: " + basetype);
        }
	if(DEBUG) {
        System.err.printf("RandomValue.nextValue: (%s) %s",
            atomtype.toString(), value.toString());
        if(atomtype.isIntegerType()) {
          Number nn = (Number)value;
          System.err.printf(" 0x%x\n",nn.longValue());
        } else
            System.err.println();
    }
	return value;
    }

    // return an integer type value (including long, but not floats)
    public Object
    nextInteger(DapType basetype)
        throws DapException
    {
        AtomicType atomtype = basetype.getAtomicType();
        if(!atomtype.isIntegerType())
            throw new DapException("Unexpected type: " + basetype);
        boolean unsigned = atomtype.isUnsigned();
        switch (atomtype) {
        case Int8:
            return Byte.valueOf((byte) (random.nextInt(1 << 8) - (1 << 7)));
        case UInt8:
            return Byte.valueOf((byte) (random.nextInt(1 << 8) & 0xFF));
        case Int16:
            return Short.valueOf((short) (random.nextInt(1 << 16) - (1 << 15)));
        case UInt16:
            return Short.valueOf((short) (random.nextInt(1 << 16)));
        case Int32:
            return Integer.valueOf(random.nextInt());
        case UInt32:
            long l = random.nextLong();
            l = l & 0xFFFFFFFF;
            return Integer.valueOf((int) l);
        case Int64:
            return Long.valueOf(random.nextLong());
        case UInt64:
            return new BigInteger(64, random);
        }
        throw new DapException("Unexpected type: " + basetype);
    }

    // return an integer type value (including long, but not floats)
    public Object
    nextFloat(DapType basetype)
        throws DapException
    {
        AtomicType atomtype = basetype.getAtomicType();
        switch (atomtype) {
        case Float32:
            return random.nextFloat();
        case Float64:
            return random.nextDouble();
        default:
            break;
        }
        throw new DapException("Unexpected type: " + basetype);
    }

    Object
    nextEnum(DapEnum en)
    {
        long l;
        AtomicType basetype = en.getBaseType().getAtomicType();

        // Collect the enum const values as BigIntegers
        List<String> ecnames = en.getNames();
        BigInteger[] econsts = new BigInteger[ecnames.size()];
        for(int i = 0;i < econsts.length;i++) {
            l = en.lookup(ecnames.get(i));
            econsts[i] = BigInteger.valueOf(l);
            if(basetype == AtomicType.UInt64)
                econsts[i] = econsts[i].and(MASK);
        }

        int index = random.nextInt(econsts.length);
        l = econsts[index].longValue();
        Object val = null;
        switch (basetype) {
        case Int8:
            val = new Byte((byte) l);
            break;
        case UInt8:
            val = new Byte((byte) (l & 0xFFL));
            break;
        case Int16:
            val = new Short((short) l);
            break;
        case UInt16:
            val = new Short((short) (l & 0xFFFFL));
            break;
        case Int32:
            val = new Integer((int) l);
            break;
        case UInt32:
            val = new Integer((int) (l & 0xFFFFFFFFL));
            break;
        case Int64:
            val = new Long(l);
            break;
        case UInt64:
            val = new Long(l);
            break;
        }
        return val;
    }

    String
    nextString(Random random, int maxlength, int min, int max)
    {
        int length = random.nextInt(maxlength) + 1;
        StringBuilder buf = new StringBuilder();
        if(asciionly && max > 127) max = 127;
	    int range = (max+1) - min; // min..max+1 -> 0..(max+1)-min
        for(int i = 0;i < length;i++) {
            int c = random.nextInt(range); // 0..(max+1)-min
	        c = c + min; // 0..(max+1)-min -> min..max+1
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

    /**
     * Return an integer in range 1..max inclusive.
     *
     * @param max
     * @return random integer in range
     * @throws DapException
     */
    public int
    nextCount(int max)
        throws DapException
    {
        int min = 1;
        if(max < min || min < 1)
            throw new DapException("bad range");
        int range = (max + 1) - min;  // min..max+1 -> 0..(max+1)-min
        int n = random.nextInt(range);   //  0..(max+1)-min
        n = n + min;   // min..(max+1)
	if(DEBUG)
	    System.err.println("RandomValue.nextCount: "+n);
        return n;
    }

}
