/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/
package dap4.dap4lib;

import dap4.core.ce.CEConstraint;
import dap4.core.data.DSP;
import dap4.core.data.DataCursor;
import dap4.core.dmr.*;
import dap4.core.util.*;

import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.List;

/**
 * DAP DSP Printer.
 * Given a constraint and a DSP print the constrained
 * subset of the data in text form.
 */

public class DSPPrinter
{
    //////////////////////////////////////////////////
    // Printer Control flags

    public enum Flags
    {
        CONTROLCHAR;
    }

    //////////////////////////////////////////////////
    // Constants

    static protected final int COLUMNS = 8;

    static protected final char LPAREN = '(';
    static protected final char RPAREN = ')';
    static protected final char LBRACE = '{';
    static protected final char RBRACE = '}';
    static protected final char LBRACKET = '[';
    static protected final char RBRACKET = ']';

    // Could use enumset, but it is so ugly,
    // so use good old OR'able flags
    static protected final int NILFLAGS = 0;
    static protected final int PERLINE = 1; // print xml attributes 1 per line
    static protected final int NONAME = 2; // do not print name xml attribute
    static protected final int NONNIL = 4; // print empty xml attributes

    //////////////////////////////////////////////////
    // Instance variables

    protected PrintWriter writer = null;
    protected IndentWriter printer = null;

    protected DSP dsp = null;
    protected CEConstraint ce = null;

    protected EnumSet<Flags> flags = EnumSet.noneOf(Flags.class);

    //////////////////////////////////////////////////
    //Constructor(s)

    protected DSPPrinter()
    {
    }

    public DSPPrinter(DSP dsp, Writer writer)
    {
        this(dsp, null, writer);
    }

    public DSPPrinter(DSP dsp, CEConstraint ce, Writer writer)
    {
        this.dsp = dsp;
        this.ce = ce;
        this.writer = new PrintWriter(writer);
        this.printer = new IndentWriter(this.writer);
    }

    //////////////////////////////////////////////////
    // External API

    public DSPPrinter flag(Flags flag)
    {
        this.flags.add(flag);
        return this;
    }

    public DSPPrinter flush()
    {
        this.printer.flush();
        return this;
    }

    public DSPPrinter close()
    {
        this.flush();
        return this;
    }

    /**
     * Print data from a DSP
     * - optionally constrained
     *
     * @throws DapException
     */

    public DSPPrinter
    print()
            throws DapException
    {
        DapDataset dmr = this.dsp.getDMR();
        if(this.ce == null)
            this.ce = CEConstraint.getUniversal(dmr);
        this.printer.setIndent(0);
        List<DapVariable> topvars = dmr.getTopVariables();
        for(int i = 0; i < topvars.size(); i++) {
            DapVariable top = topvars.get(i);
            List<Slice> slices = this.ce.getConstrainedSlices(top);
            if(this.ce.references(top)) {
                DataCursor data = dsp.getVariableData(top);
                printVariable(data,slices);
            }
        }
        printer.eol();
        return this;
    }

    //////////////////////////////////////////////////

    /**
     * Print an arbitrary DataVariable using a constraint.
     * <p>
     * Handling newlines is a bit tricky
     * so the rule is that the
     * last newline is elided and left
     * for the caller to print.
     * Exceptions: ?
     *
     * @param data - the cursor to print
     * @throws DapException Note that the PrintWriter and CEConstraint are global.
     */

    protected void
    printVariable(DataCursor data, List<Slice> slices)
            throws DapException
    {
        DapVariable dapv = (DapVariable) data.getTemplate();
        if(data.isScalar()) {
            assert slices == Slice.SCALARSLICES;
            printScalar(data);
        } else {// not scalar
            printArray(data, slices);
        }
    }

    protected void
    printScalar(DataCursor data)
            throws DapException
    {
        DapVariable dapv = (DapVariable) data.getTemplate();
        printer.marginPrint(dapv.getFQN() + " = ");
        switch (data.getScheme()) {
        case ATOMIC:
            printAtomicInstance(data, Index.SCALAR);
            break;
        case STRUCTARRAY:
        case SEQARRAY:  // remember that scalars are treated as 1-element arrays
            DataCursor[] scalar = (DataCursor[]) data.read(Index.SCALAR);
            assert scalar.length == 1;
            data = scalar[0]; // fall thru
        case STRUCTURE:
        case SEQUENCE:
        case RECORD:
            printer.marginPrint("{");
            printer.eol();
            printer.indent();
            printCompoundInstance(data);
            printer.outdent();
            printer.marginPrint("}");
            printer.eol();
            break;
        default:
            throw new DapException("Unexpected data cursor type: " + data.getScheme());
        }
    }

    protected void
    printArray(DataCursor data, List<Slice> slices)
            throws DapException
    {
        DapVariable dapv = (DapVariable) data.getTemplate();
        Odometer odom = Odometer.factory(slices);
        switch (data.getScheme()) {
        case ATOMIC:
            if(DapUtil.isContiguous(slices))
                printAtomicVector(data, slices, odom);
            else {
                while(odom.hasNext()) {
                    Index pos = odom.next();
                    String s = indicesToString(pos);
                    printer.marginPrint(dapv.getFQN() + s + " = ");
                    printAtomicInstance(data, pos);
                }
            }
            break;
        case STRUCTARRAY:
        case SEQARRAY:
            DapStructure ds = (DapStructure) ((DapVariable) data.getTemplate()).getBaseType();
            DataCursor[] instances = (DataCursor[]) data.read(slices);
            while(odom.hasNext()) {
                Index pos = odom.next();
                String s = indicesToString(pos);
                printer.marginPrint(ds.getFQN() + s + " = {");
                printer.eol();
                printer.indent();
                printCompoundInstance(instances[(int)pos.index()]);
                printer.outdent();
                printer.marginPrint("}");
                printer.eol();
            }
            break;
        default:
            throw new DapException("Unexpected data cursor type: " + data.getScheme());
        }
    }

    protected void
    printAtomicVector(DataCursor data, List<Slice> slices, Odometer odom)
            throws DapException
    {
        assert data.getScheme() == DataCursor.Scheme.ATOMIC;
        Object values = data.read(slices);
        DapVariable atom = (DapVariable) data.getTemplate();
        String name = atom.getFQN();
        if(Array.getLength(values) == 0) {// zero length case
            printer.marginPrint(name + " = <empty>");
            printer.eol();
        } else for(int i = 0; odom.hasNext(); i++) {
            Index index = odom.next();
            String prefix = (odom.rank() == 0 ? name : name + indicesToString(index));
            printer.marginPrint(prefix + " = ");
            printer.print(valueString(values, i, atom.getBaseType()));
            printer.eol();
        }
    }

    protected void
    printAtomicInstance(DataCursor datav, Index pos)
            throws DapException
    {
        assert datav.getScheme() == DataCursor.Scheme.ATOMIC;
        Object value = datav.read(pos);
        DapVariable av = (DapVariable) datav.getTemplate();
        printer.print(valueString(value, 0, av.getBaseType()));
        printer.eol();
    }

    /**
     * Print a single structure or sequence or record instance
     *
     * @param datav
     * @throws DapException
     */
    protected void
    printCompoundInstance(DataCursor datav)
            throws DapException
    {
        //Index index = datav.getIndex();
        DapStructure dstruct = (DapStructure) ((DapVariable) datav.getTemplate()).getBaseType();
        switch (datav.getScheme()) {
        case STRUCTURE:
        case RECORD:
            List<DapVariable> dfields = dstruct.getFields();
            for(int f = 0; f < dfields.size(); f++) {
                DapVariable field = dfields.get(f);
                List<Slice> fieldslices = this.ce.getConstrainedSlices(field);
                DataCursor fdata = datav.readField(f);
                printVariable(fdata,fieldslices);
            }
            break;

        case SEQUENCE:
            DapSequence dseq = (DapSequence)dstruct;
            long count = datav.getRecordCount();
            for(long r = 0; r < count; r++) {
                DataCursor dr = datav.readRecord(r);
                printer.marginPrint("[");
                printer.eol();
                printer.indent();
                printCompoundInstance(dr);
                printer.outdent();
                printer.marginPrint("]");
            }
            break;
        default:
            throw new DapException("Unexpected data cursor scheme:" + datav.getScheme());
        }
    }

    protected String
    indicesToString(Index indices)
            throws DapException
    {
        StringBuilder buf = new StringBuilder();
        if(indices != null && indices.getRank() > 0) {
            for(int i = 0; i < indices.getRank(); i++) {
                buf.append(i == 0 ? LBRACKET : ",");
                buf.append(String.format("%d", indices.get(i)));
            }
            buf.append(RBRACKET);
        }
        return buf.toString();
    }

    protected String
    valueString(Object vector, long pos, DapType basetype)
            throws DapException
    {
        if(vector == null) return "null";
        TypeSort atype = basetype.getTypeSort();
        boolean unsigned = atype.isUnsigned();
        int ipos = (int) pos;
        switch (atype) {
        case Int8:
        case UInt8:
            long lvalue = ((byte[]) vector)[ipos];
            if(unsigned) lvalue &= 0xFFL;
            return String.format("%d", lvalue);
        case Int16:
        case UInt16:
            lvalue = ((short[]) vector)[ipos];
            if(unsigned) lvalue &= 0xFFFFL;
            return String.format("%d", lvalue);
        case Int32:
        case UInt32:
            lvalue = ((int[]) vector)[ipos];
            if(unsigned) lvalue &= 0xFFFFFFFFL;
            return String.format("%d", lvalue);
        case Int64:
        case UInt64:
            lvalue = ((long[]) vector)[ipos];
            if(unsigned) {
                BigInteger b = BigInteger.valueOf(lvalue);
                b = b.and(DapUtil.BIG_UMASK64);
                return b.toString();
            } else
                return String.format("%d", lvalue);
        case Float32:
            return String.format("%f", ((float[]) vector)[ipos]);
        case Float64:
            return String.format("%f", ((double[]) vector)[ipos]);
        case Char:
            return String.format("'%c'", ((char[]) vector)[ipos]);
        case String:
        case URL:
            String s = (((String[]) vector)[ipos]);
            if(flags.contains(Flags.CONTROLCHAR)) {
                s = s.replace("\r", "\\r");
                s = s.replace("\n", "\\n");
                s = s.replace("\t", "\\t");
            }
            return "\"" + s + "\"";
        case Opaque:
            ByteBuffer opaque = ((ByteBuffer[]) vector)[ipos];
            StringBuilder buf = new StringBuilder();
            buf.append("0x");
            for(int i = 0; i < opaque.limit(); i++) {
                byte b = opaque.get(i);
                char c = hexchar((b >> 4) & 0xF);
                buf.append(c);
                c = hexchar((b) & 0xF);
                buf.append(c);
            }
            return buf.toString();
        case Enum:
            DapEnumeration de = (DapEnumeration) basetype;
            Object newvec = CoreTypeFcns.createVector(de.getBaseType().getTypeSort(),1);
            Object v = java.lang.reflect.Array.get(vector, ipos);
            java.lang.reflect.Array.set(newvec,0,v);
            long[] lv  = (long[]) Convert.convert(DapType.INT64, de, newvec);
            DapEnumConst dec = de.lookup(lv[0]);
            return dec.getShortName();
        default:
            break;
        }
        throw new DapException("Unknown type: " + basetype);
    }


    //////////////////////////////////////////////////
    // Misc. Utilities

    static protected char
    hexchar(int i)
    {
        return "0123456789ABCDEF".charAt((i & 0xF));
    }


    static protected String
    getPrintValue(Object value)
    {
        if(value instanceof String) {
            return Escape.entityEscape((String) value,null);
        } else
            return value.toString();
    }

}

