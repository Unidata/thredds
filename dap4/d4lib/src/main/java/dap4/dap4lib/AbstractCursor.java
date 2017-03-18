/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.dap4lib;

import dap4.core.data.DSP;
import dap4.core.data.DataCursor;
import dap4.core.dmr.DapNode;
import dap4.core.dmr.DapStructure;
import dap4.core.dmr.DapType;
import dap4.core.dmr.DapVariable;
import dap4.core.util.DapException;
import dap4.core.util.DapSort;
import dap4.core.util.Index;
import dap4.core.util.Slice;

import java.util.List;

abstract public class AbstractCursor implements DataCursor
{
    //////////////////////////////////////////////////
    // Constants

    //////////////////////////////////////////////////
    // Instance Variables

    protected Scheme scheme;

    protected DSP dsp;
    protected DapNode template;

    protected Index arrayindex = null;
    protected long recordindex = -1; // scheme == record

    protected AbstractCursor container = null;

    protected long recordcount = -1;

    //////////////////////////////////////////////////
    // Constructor(s)

    public AbstractCursor(Scheme scheme, DSP dsp, DapNode template, AbstractCursor container)
    {
        setScheme(scheme);
        setDSP(dsp);
        setTemplate(template);
        setContainer(container);
    }

    public AbstractCursor(AbstractCursor orig)
    {
        this(orig.getScheme(), orig.getDSP(), orig.getTemplate(), (AbstractCursor) orig.getContainer());
    }


    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append(getScheme().toString());
        if(getScheme() == Scheme.STRUCTARRAY || getScheme() == Scheme.SEQARRAY)
            buf.append("[]");
        buf.append(":");
        buf.append(getTemplate().toString());
        if(this.arrayindex != null) {
            buf.append("::");
            buf.append(this.arrayindex.toString());
        }
        if(this.recordindex >= 0) {
            buf.append("*");
            buf.append(this.recordindex);
        }
        return buf.toString();
    }

    //////////////////////////////////////////////////
    // Unimplemented DataCursor API methods

    @Override
    abstract public Object read(List<Slice> slices) throws DapException;

    @Override
    abstract public Object read(Index index) throws DapException;

    @Override
    abstract public AbstractCursor readRecord(long i) throws DapException;

    @Override
    abstract public AbstractCursor readField(int fieldindex) throws DapException;

    //////////////////////////////////////////////////
    // Selected DataCursor API overrides

    @Override
    public int fieldIndex(String name)
            throws DapException
    {
        DapStructure ds;
        if(getTemplate().getSort().isCompound())
            ds = (DapStructure) getTemplate();
        else if(getTemplate().getSort().isVar()
                && (((DapVariable) getTemplate()).getBaseType().getSort().isCompound()))
            ds = (DapStructure) ((DapVariable) getTemplate()).getBaseType();
        else
            throw new DapException("Attempt to get field name on non-compound object");
        int i = ds.indexByName(name);
        if(i < 0)
            throw new DapException("Unknown field name: " + name);
        return i;
    }

    @Override
    public Scheme getScheme()
    {
        return this.scheme;
    }

    @Override
    public DSP
    getDSP()
    {
        return this.dsp;
    }

    @Override
    public DapNode
    getTemplate()
    {
        return this.template;
    }

    @Override
    public Index getIndex()
            throws DapException
    {
        if(this.scheme != Scheme.STRUCTURE && this.scheme != Scheme.SEQUENCE)
            throw new DapException("Not a Sequence|Structure instance");
        return this.arrayindex;
    }

    @Override
    public long getRecordIndex()
            throws DapException
    {
        if(this.scheme != Scheme.RECORD)
            throw new DapException("Not a Record instance");
        return this.recordindex;
    }

    @Override
    public long
    getRecordCount()
            throws DapException
    {
        if(this.scheme != Scheme.SEQUENCE)
            throw new DapException("Not a SEQUENCE instance");
        return this.recordcount;
    }


    @Override
    public AbstractCursor
    getContainer()
    {
        return this.container;
    }

    @Override
    public boolean
    isScalar()
    {
        if(getTemplate().getSort().isVar()) {
            return ((DapVariable) getTemplate()).getRank() == 0;
        } else
            return false;
    }

    public boolean isField()
    {
        return getTemplate().getContainer() != null;
    }

    public boolean isAtomic()
    {
        boolean is = this.scheme == Scheme.ATOMIC;
        assert !is || getTemplate().getSort() == DapSort.ATOMICTYPE
                || (getTemplate().getSort() == DapSort.VARIABLE
                && ((DapVariable) getTemplate()).getBaseType().getTypeSort().isAtomic());
        return is;
    }

    public boolean isCompound()
    {
        boolean is = (this.scheme == Scheme.SEQUENCE || this.scheme == Scheme.STRUCTURE);
        assert !is
                || getTemplate().getSort() == DapSort.SEQUENCE
                || getTemplate().getSort() == DapSort.STRUCTURE
                || (getTemplate().getSort() == DapSort.VARIABLE
                && ((DapVariable) getTemplate()).getBaseType().getTypeSort().isCompoundType());
        return is;
    }

    public boolean isCompoundArray()
    {
        boolean is = (this.scheme == Scheme.SEQARRAY || this.scheme == Scheme.STRUCTARRAY);
        assert !is
                || getTemplate().getSort() == DapSort.SEQUENCE
                || getTemplate().getSort() == DapSort.STRUCTURE
                || (getTemplate().getSort() == DapSort.VARIABLE
                && ((DapVariable) getTemplate()).getBaseType().getTypeSort().isCompoundType());
        return is;
    }

    //////////////////////////////////////////////////
    // AbstractCursor extensions

    public AbstractCursor
    setIndex(Index index)
    {
        this.arrayindex = index;
        return this;
    }

    public AbstractCursor
    setRecordIndex(long index)
    {
        this.recordindex = index;
        return this;
    }

    public AbstractCursor
    setRecordCount(long count)
    {
        this.recordcount = count;
        return this;
    }

    public AbstractCursor
    setContainer(AbstractCursor container)
    {
        this.container = container;
        return this;
    }

    public AbstractCursor
    setScheme(Scheme scheme)
    {
        this.scheme = scheme;
        return this;
    }


    public AbstractCursor
    setDSP(DSP dsp)
    {
        this.dsp = dsp;
        return this;
    }

    public AbstractCursor
    setTemplate(DapNode template)
    {
        this.template = template;
        return this;
    }


    //////////////////////////////////////////////////
    // Utilities

    static public Scheme
    schemeFor(DapVariable field)
    {
        DapType ftype = field.getBaseType();
        Scheme scheme = null;
        boolean isscalar = field.getRank() == 0;
        if(ftype.getTypeSort().isAtomic())
            scheme = Scheme.ATOMIC;
        else {
            if(ftype.getTypeSort().isStructType()) scheme = Scheme.STRUCTARRAY;
            else if(ftype.getTypeSort().isSeqType()) scheme = Scheme.SEQARRAY;
        }
        return scheme;
    }
}
