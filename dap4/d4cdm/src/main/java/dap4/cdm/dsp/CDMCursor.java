/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.cdm.dsp;

import dap4.cdm.CDMTypeFcns;
import dap4.core.data.DataCursor;
import dap4.core.dmr.*;
import dap4.core.util.*;
import dap4.core.util.Index;
import org.apache.http.impl.cookie.DateParseException;
import ucar.ma2.*;

import java.util.List;

public class CDMCursor implements DataCursor
{

    //////////////////////////////////////////////////
    // Instance variables

    protected Scheme scheme;

    protected CDMDSP dsp = null;
    protected DapNode template = null;

    protected ucar.ma2.Array array = null;
    protected ucar.ma2.StructureData structdata = null; // scheme == STRUCTURE

    // Computed instance variables
    DapSort sort;

    //////////////////////////////////////////////////
    // Constructor(s)

    public CDMCursor(Scheme scheme, DapNode template, CDMDSP dsp)
            throws DapException
    {
        this.scheme = scheme;
        this.template = template;
        this.dsp = dsp;
        this.sort = template.getSort();
    }

    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append(getScheme().toString());
        if(getScheme() == Scheme.STRUCTARRAY || getScheme() == Scheme.SEQARRAY)
            buf.append("[]");
        buf.append(":");
        buf.append(getTemplate().getFQN());
        return buf.toString();
    }

    //////////////////////////////////////////////////
    // DataCursor Interface

    @Override
    public Scheme getScheme()
    {
        return this.scheme;
    }

    @Override
    public CDMDSP getDSP()
    {
        return this.dsp;
    }

    @Override
    public DapNode getTemplate()
    {
        return this.template;
    }

    public Object
    read(List<Slice> slices)
            throws DapException
    {
        try {
            DataCursor[] instances = null;
            switch (this.scheme) {
            case ATOMIC:
                return readAtomic(slices);
            case STRUCTARRAY:
                Odometer odom = Odometer.factory(slices);
                instances = new DataCursor[(int) odom.totalSize()];
                for(int i = 0; odom.hasNext(); i++) {
                    instances[i] = readStructure(odom.next());
                }
                return instances;
            case SEQARRAY:
                odom = Odometer.factory(slices);
                instances = new DataCursor[(int) odom.totalSize()];
                for(int i = 0; odom.hasNext(); i++) {
                    instances[i] = readSequence(odom.next());
                }
                return instances;
            case STRUCTURE:
                assert slices.size() == 0;
                instances = new DataCursor[1];
                instances[0] = readStructure(Index.SCALAR);
                return instances;
            case SEQUENCE:
                assert slices.size() == 0;
                instances = new DataCursor[1];
                instances[0] = readSequence(Index.SCALAR);
                return instances;
            default:
                throw new DapException("Unexpected data cursor type: " + this.scheme);
            }
        } catch (DapException e) {
            throw new DapException(e);
        }
    }

    public Object
    read(Index index)
            throws DapException
    {
        switch (this.scheme) {
        case ATOMIC:
            return readAtomic(DapUtil.indexToSlices(index, (DapAtomicVariable) getTemplate()));
        case STRUCTARRAY:
            return readStructure(index);
        case SEQARRAY:
            return readSequence(index);
        case STRUCTURE:
            assert index.getRank() == 0;
            return readStructure(index);
        case SEQUENCE:
            assert index.getRank() == 0;
            return readSequence(index);
        default:
            break;
        }
        throw new DapException("Unexpected cursor scheme: " + this.scheme);
    }

    protected Object
    readAtomic(List<Slice> slices)
            throws DapException
    {
        if(slices == null)
            throw new DapException("DataCursor.read: null set of slices");
        assert (this.scheme == scheme.ATOMIC);
        DapAtomicVariable atomvar = (DapAtomicVariable) getTemplate();
        DapType basetype = atomvar.getBaseType();
        int rank = atomvar.getRank();
        assert slices != null && slices.size() == rank;

        // If content.getDataType returns object, then we
        // really do not know its true datatype. So, as a rule,
        // we will rely on this.basetype.
        DataType datatype = CDMTypeFcns.daptype2cdmtype(basetype);
        if(datatype == null)
            throw new dap4.core.util.DapException("Unknown basetype: " + basetype);

        Object content = this.array.get1DJavaArray(datatype); // not very efficient; should do conversion
        try {
            Odometer odom = Odometer.factory(slices, ((DapVariable) this.getTemplate()).getDimensions());
            Object data = CDMTypeFcns.createVector(basetype, odom.totalSize());
            for(int dstoffset = 0; odom.hasNext(); dstoffset++) {
                Index index = odom.next();
                long srcoffset = index.index();
                CDMTypeFcns.vectorcopy(basetype, content, data, srcoffset, dstoffset);
            }
            return data;
        } catch (dap4.core.util.DapException de) {
            throw new dap4.core.util.DapException(de);
        }
    }

    protected CDMCursor
    readStructure(Index index)
            throws DapException
    {
        assert (index != null);
        DapStructure template = (DapStructure) getTemplate();
        long pos = index.index();
        if(pos < 0 || pos > template.getCount())
            throw new IndexOutOfBoundsException("read: " + index);
        ArrayStructure sarray = (ArrayStructure) this.array;
        CDMCursor instance;
        StructureData data;
        if(template.getRank() == 0) {
            assert (this.scheme == scheme.STRUCTURE);
            data = sarray.getStructureData(0);
            instance = this.setStructureData(data);
        } else {
            assert (this.scheme == scheme.STRUCTARRAY);
            data = sarray.getStructureData((int) pos);
            instance = new CDMCursor(Scheme.STRUCTURE, template, this.dsp)
                    .setStructureData(data);
        }
        return instance;
    }

    public CDMCursor
    getField(int findex)
            throws DapException
    {
        assert (this.scheme == scheme.RECORD || this.scheme == scheme.STRUCTURE);
        if(this.structdata == null)
            read(Index.SCALAR); // pre-read to get structuredata
        assert this.structdata != null;
        StructureMembers.Member member = this.structdata.getMembers().get(findex);
        if(member == null)
            throw new DapException("getField: field index out of bounds: " + findex);
        DapVariable field = ((DapStructure) getTemplate()).getField(findex);
        Array array = this.structdata.getArray(member);
        CDMCursor instance = null;
        switch (field.getSort()) {
        case ATOMICVARIABLE:
            instance = new CDMCursor(Scheme.ATOMIC, field, dsp).setArray(array);
            break;
        case SEQUENCE:
            throw new UnsupportedOperationException();
        case STRUCTURE:
            Scheme scheme = (field.getRank() == 0 ? Scheme.STRUCTURE : Scheme.STRUCTARRAY);
            instance = new CDMCursor(scheme, field, dsp).setArray(array);
            break;
        default:
            throw new DapException("Unexpected field type: "+field);
        }
        return instance;
    }

    public CDMCursor
    readSequence(Index index)
            throws DapException
    {
        assert (this.scheme == scheme.SEQARRAY);
        throw new UnsupportedOperationException();
    }

    public long getRecordCount()
    {
        assert (this.scheme == scheme.SEQUENCE);
        throw new UnsupportedOperationException("Not a Sequence");
    }

    public DataCursor
    getRecord(long i)
    {
        assert (this.scheme == scheme.SEQUENCE);
        throw new UnsupportedOperationException("Not a Sequence");
    }

    //////////////////////////////////////////////////
    // CDMCursor Extensions

    public CDMCursor setArray(ucar.ma2.Array a)
    {
        this.array = a;
        return this;
    }

    public CDMCursor setStructureData(ucar.ma2.StructureData sd)
    {
        this.structdata = sd;
        return this;
    }

}
