/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.cdm;

import dap4.core.data.DataCursor;
import dap4.core.dmr.DapDimension;
import dap4.core.dmr.DapType;
import dap4.core.dmr.TypeSort;
import dap4.core.util.DapException;
import dap4.core.util.Index;
import dap4.core.util.Slice;
import ucar.ma2.ForbiddenConversionException;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.CDMNode;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

import java.util.ArrayList;
import java.util.List;

/**
 * CDM related Constants and utilities
 * common to client and server code
 */

abstract public class CDMUtil
{

    static final String hexchars = "0123456789abcdef";

    /**
     * Convert a list of ucar.ma2.Range to a list of Slice
     * More or less the inverst of create CDMRanges
     *
     * @param rangelist the set of ucar.ma2.Range
     * @result the equivalent list of Slice
     */
    static public List<Slice>
    createSlices(List<Range> rangelist)
            throws dap4.core.util.DapException
    {
        List<Slice> slices = new ArrayList<Slice>(rangelist.size());
        for(int i = 0; i < rangelist.size(); i++) {
            Range r = rangelist.get(i);
            // r does not store last
            int stride = r.stride();
            int first = r.first();
            int n = r.length();
            int stop = first + (n * stride);
            Slice cer = new Slice(first, stop - 1, stride);
            slices.add(cer);
        }
        return slices;
    }

    /**
     * Test a List<Range> against a List<DapDimension>
     * to see if the range list represents the whole
     * set of dimensions within the specified indices.
     *
     * @param rangelist the set of ucar.ma2.Range
     * @param dimset    the set of DapDimensions
     * @param start     start looking here
     * @param stop      stop looking here
     * @result true if rangelist is whole; false otherwise.
     */

    static public boolean
    isWhole(List<Range> rangelist, List<DapDimension> dimset, int start, int stop)
            throws dap4.core.util.DapException
    {
        int rsize = (rangelist == null ? 0 : rangelist.size());
        if(rsize != dimset.size())
            throw new dap4.core.util.DapException("range/dimset rank mismatch");
        if(rsize == 0)
            return true;
        if(start < 0 || stop < start || stop > rsize)
            throw new dap4.core.util.DapException("Invalid start/stop indices");

        for(int i = start; i < stop; i++) {
            Range r = rangelist.get(i);
            DapDimension d = dimset.get(i);
            if(r.stride() != 1 || r.first() != 0 || r.length() != d.getSize())
                return false;
        }
        return true;
    }

    /**
     * Test a List<Range> against a List<Slice>
     * to see if the range list is whole
     * wrt the slices
     *
     * @param rangelist the set of ucar.ma2.Range
     * @param slices    the set of slices
     * @result true if rangelist is whole wrt slices; false otherwise.
     */
    static public boolean
    isWhole(List<Range> rangelist, List<Slice> slices)
            throws dap4.core.util.DapException
    {
        if(rangelist.size() != slices.size())
            return false;
        for(int i = 0; i < rangelist.size(); i++) {
            Range r = rangelist.get(i);
            Slice slice = slices.get(i);
            if(r.stride() != 1 || r.first() != 0 || r.length() != slice.getCount())
                return false;
        }
        return true;
    }

    /**
     * Test a List<Range> against the CDM variable's dimensions
     * to see if the range list is whole
     * wrt the dimensions
     *
     * @param rangelist the set of ucar.ma2.Range
     * @param var       the cdm var
     * @result true if rangelist is whole wrt slices; false otherwise.
     */
    static public boolean
    isWhole(List<Range> rangelist, Variable var)
            throws dap4.core.util.DapException
    {
        List<Dimension> dimset = var.getDimensions();
        if(rangelist.size() != dimset.size())
            return false;
        for(int i = 0; i < rangelist.size(); i++) {
            Range r = rangelist.get(i);
            Dimension dim = dimset.get(i);
            if(r.stride() != 1 || r.first() != 0 || r.length() != dim.getLength())
                return false;
        }
        return true;
    }

    static public List<ucar.ma2.Range>
    createCDMRanges(List<Slice> slices)
            throws DapException
    {
        List<ucar.ma2.Range> cdmranges = new ArrayList<Range>();
        for(int i = 0; i < slices.size(); i++) {
            Slice r = slices.get(i);
            try {
                ucar.ma2.Range cmdr;
                cmdr = new ucar.ma2.Range((int) r.getFirst(),
                        (int) r.getLast(),
                        (int) r.getStride());
                cdmranges.add(cmdr);
            } catch (InvalidRangeException ire) {
                throw new DapException(ire);
            }
        }
        return cdmranges;
    }

    /**
     * NetcdfDataset can end up wrapping a variable
     * in multiple wrapping classes (e.g. VariableDS).
     * Goal of this procedure is to get down to the
     * lowest level Variable instance
     *
     * @param var possibly wrapped variable
     * @return the lowest level Variable instance
     */
    static public Variable unwrap(Variable var)
    {
/*        for(;;) {
            if(var instanceof VariableDS) {
                VariableDS vds = (VariableDS) var;
                var = vds.getOriginalVariable();
                if(var == null) {
                    var = vds;
                    break;
                }
            } else if(var instanceof StructureDS) {
                StructureDS sds = (StructureDS) var;
                var = sds.getOriginalVariable();
                if(var == null) {
                    var = sds;
                    break;
                }
            } else
                break;
        }
        return var;
        */
        return (Variable) CDMNode.unwrap(var);
    }

    /**
     * NetcdfDataset can wrap a NetcdfFile.
     * Goal of this procedure is to get down to the
     * lowest level NetcdfFile instance.
     *
     * @param file NetcdfFile or NetcdfDataset
     * @return the lowest level NetcdfFile instance
     */
    static public NetcdfFile unwrapfile(NetcdfFile file)
    {
        for(; ; ) {
            if(file instanceof NetcdfDataset) {
                NetcdfDataset ds = (NetcdfDataset) file;
                file = ds.getReferencedFile();
                if(file == null) break;
            } else break;
        }
        return file;
    }

    static public boolean
    hasVLEN(List<Range> ranges)
    {
        if(ranges == null || ranges.size() == 0) return false;
        return ranges.get(ranges.size() - 1) == Range.VLEN;
    }

    /**
     * Test if any dimension is variable length
     */
    static public boolean
    hasVLEN(Variable v)
    {
        return containsVLEN(v.getDimensions());
    }

    /**
     * Test if any dimension is variable length
     */
    static public boolean
    containsVLEN(List<Dimension> dimset)
    {
        if(dimset == null) return false;
        for(Dimension dim : dimset) {
            if(dim.isVariableLength())
                return true;
        }
        return false;
    }

    /**
     * Compute the shape inferred from a set of slices.
     * 'Effective' means that any trailing vlen will be
     * ignored.
     *
     * @param dimset from which to generate shape
     * @return
     */
    static public int[]
    computeEffectiveShape(List<DapDimension> dimset)
    {
        if(dimset == null || dimset.size() == 0)
            return new int[0];
        int effectiverank = dimset.size();
        int[] shape = new int[effectiverank];
        for(int i = 0; i < effectiverank; i++) {
            shape[i] = (int) dimset.get(i).getSize();
        }
        return shape;
    }

    /*
    static public int
    computeVariableSize(View view, DapVariable var, boolean scalar)
    {
	DapType dt = var.getBaseType();
        ViewVariable annotation = view.getAnnotation(var);
        int dimproduct = (scalar ? 1 : computeDimProduct(annotation.getSlices()));
        int elementsize = 0;
        switch (dt.getTypeSort()) {
	default: // atomic variable
            // This does not work for String or Opaque.
            elementsize =  CDMUtil.daptypeSize(dt.getTypeSort());
            break;
        case STRUCTURE:
        case SEQUENCE:
            for(DapVariable field : ((DapStructure) dt).getFields()) {
                elementsize += computeVariableSize(dataset, field, false);
            }
            break;

        default:
            break;
        }
        return dimproduct * elementsize;
    }
    */

    /**
     * Extract, as an object, value from a (presumably)
     * atomic typed array of values; dataset position
     * is presumed correct.
     *
     * @param atomtype type of object to extract ; must not be Enum
     * @param dataset  Data containing the objects
     * @param index    Which element of dataset to read
     * @return resulting value as an Object; value does not necessarily conform
     * to Convert.ValueClass.
     */

    /**
     * Extract, as a long, value from a (presumably)
     * atomic typed array of values; dataset position
     * is presumed correct.
     *
     * @param atomtype type of object to extract
     * @param dataset  Data containing the objects
     * @param index    Which element of dataset to read
     * @return resulting value as a long
     * @throws ForbiddenConversionException if cannot convert to long
     */

    static public long
    extractLongValue(TypeSort atomtype, DataCursor dataset, Index index)
            throws DapException
    {
        Object result;
        result = dataset.read(index);
        long lvalue = CDMTypeFcns.extract(atomtype, result);
        return lvalue;
    }

    /**
     * Extract, as a double, value from a (presumably)
     * atomic typed array of values; dataset position
     * is presumed correct.
     *
     * @param atomtype type of object to extract
     * @param dataset  Data containing the objects
     * @param index    Which element of dataset to read
     * @return resulting value as a double
     * @throws ForbiddenConversionException if cannot convert to double
     */

    static public double
    extractDoubleValue(TypeSort atomtype, DataCursor dataset, Index index)
            throws DapException
    {
        Object result;
        result = dataset.read(index);
        double dvalue = 0.0;
        if(atomtype.isIntegerType() || atomtype.isEnumType()) {
            long lvalue = extractLongValue(atomtype, dataset, index);
            dvalue = (double) lvalue;
        } else if(atomtype == TypeSort.Float32) {
            dvalue = (double) ((Float) result).floatValue();
        } else if(atomtype == TypeSort.Float64) {
            dvalue = ((Double) result).doubleValue();
        } else
            throw new ForbiddenConversionException();
        return dvalue;
    }

    /**
     * Extract, as an object, n consecutive values
     * of an atomic typed array of values
     *
     * @param dataset  Data containing the objects
     * @param index    Starting element to read
     * @param count    Number of elements to read
     * @return resulting array of values as an object
     */

    /*
    static public Object
    extractVector(DataAtomic dataset, long index, long count, long offset)
        throws DapException
    {
        Object vector = createVector(dataset.getType().getPrimitiveType(),count);
        try {
            dataset.read(index, count, vector, offset);
        } catch (IOException ioe) {
            throw new DapException(ioe);
        }
        return vector;
    }
    */

    /**
     * Convert an array of one type of values to another type
     *
     * @param dsttype target type
     * @param srctype source type
     * @param src     array of values to convert
     * @return resulting array of converted values as an object
     */

    static public Object
    convertVector(DapType dsttype, DapType srctype, Object src)
    {
        int i;

        TypeSort srcatomtype = srctype.getAtomicType();
        TypeSort dstatomtype = dsttype.getAtomicType();

        if(srcatomtype == dstatomtype) {
            return src;
        }
        if(srcatomtype.isIntegerType()
                && TypeSort.getSignedVersion(srcatomtype) == TypeSort.getSignedVersion(dstatomtype))
            return src;

        Object result = CDMTypeFcns.convert(dstatomtype, srcatomtype, src);
        if(result == null)
            throw new ForbiddenConversionException();
        return result;
    }

    /**
     * Given an arbitrary Array (including ArrayStructure), produce
     * a new Array that represents the slice defined by the
     * section.  For now, we create a simple array of the relevant
     * type and fill it by extracting the values specified by the
     * section.
     * <p>
     * param array   the array from which the section is extracted
     * param section determines what to extract
     * throws DapException
     * returns the slice array
     */
    /*static public ucar.ma2.Array
    arraySlice(ucar.ma2.Array array, Section section)
        throws DapException
    {
        // Case it out.
        if(!dapvar.getBaseType().isStructType()) { // =>Atomic type
            if(dapvar.isTopLevel()) {
                // Simplest case: use createview, but watch out for final VLEN
                List<Range> ranges = section.getRanges();
                try {
                    if(CDMUtil.hasVLEN(ranges))
                        return array.section(ranges.subList(0, ranges.size() - 2));
                    else
                        return array.section(ranges);
                } catch (InvalidRangeException ire) {
                    throw new DapException(ire);
                }
            } else
                throw new UnsupportedOperationException(); // same as other cdm
        } else { // struct type
            assert (array instanceof CDMArrayStructure);
            CDMArrayStructure struct = (CDMArrayStructure) array;
            if(dapvar.isTopLevel()) {
                // Build a new ArrayStructure containing
                // the relevant instances.
                int[] shape = section.getShape();
                StructureMembers sm = new StructureMembers(struct.getStructureMembers());
                ArrayStructureMA slice = new ArrayStructureMA(sm, shape);
                CDMOdometer odom = new CDMOdometer(dapvar.getDimensions(), section.getRanges());
                // Compute the number of structuredata instances we need
                long totalsize = section.computeSize();
                List<StructureMembers.Member> mlist = sm.getMembers();
                StructureData[] newdata = new StructureData[(int) totalsize];
                for(int i = 0;odom.hasNext();odom.next(), i++) {
                    long recno = odom.index();
                    StructureDataW clone = new StructureDataW(sm);
                    newdata[i] = clone;
                    StructureData record = struct.getStructureData((int) recno);
                    for(int j = 0;j < mlist.size();j++) {
                        StructureMembers.Member m = mlist.get(j);
                        clone.setMemberData(m, record.getArray(m));
                    }
                }
                return slice;
            } else
                throw new UnsupportedOperationException(); // same as other cdm
        }
    }*/
    static public String
    getChecksumString(byte[] checksum)
    {
        StringBuilder buf = new StringBuilder();
        for(int i = 0; i < checksum.length; i++) {
            byte b = checksum[i];
            buf.append(hexchars.charAt(b >> 4));
            buf.append(hexchars.charAt(b & 0xF));
        }
        return buf.toString();
    }

    /**
     * Convert a Section + variable to a constraint
     * <p>
     * <p>
     * static public View
     * sectionToView(CDMDSP dsp, Variable v, Section section)
     * throws DapException
     * {
     * if(section == null || section.getRank() == 0)
     * return null;
     * // Get the corresponding DapNode
     * DapVariable dv = (DapVariable) dsp.getNode().get(v);
     * if(dv == null)
     * throw new DapException("Variable has no corresponding dap node: " + v.getFullName());
     * // Get the structure path wrt DapDataset for dv
     * // and use path plus the Section to construct a constraint
     * List<DapVariable> structpath = DapUtil.getStructurePath(dv);
     * List<Range> ranges = section.getRanges();
     * View view = new View(dmr);
     * int next = 0;
     * for(int i = 0;i < structpath.size();i++) {
     * dv = structpath.get(i);
     * int rank = dv.getRank();
     * ViewVariable vv = new ViewVariable(dv);
     * List<Slice> slices = new ArrayList<Slice>(rank);
     * for(int j = 0;j < rank;j++, next++) {
     * if(next >= ranges.size())
     * throw new DapException("Range::Rank mismatch");
     * Range range = ranges.get(next);
     * Slice slice = new Slice(range.first(), range.last(), range.stride()).validate();
     * slices.add(slice);
     * }
     * vv.setSlices(slices);
     * view.put(dv, vv);
     * }
     * view.validate(View.EXPAND);
     * return view;
     * }
     */


    static public List<Range>
    dimsetToRanges(List<DapDimension> dimset)
            throws dap4.core.util.DapException
    {
        if(dimset == null)
            return null;
        List<Range> ranges = new ArrayList<>();
        for(int i = 0; i < dimset.size(); i++) {
            DapDimension dim = dimset.get(i);
            try {
                Range r = new Range(dim.getShortName(), 0, (int) dim.getSize() - 1, 1);
                ranges.add(r);
            } catch (InvalidRangeException ire) {
                throw new dap4.core.util.DapException(ire);
            }
        }
        return ranges;
    }

    static public List<Slice>
    shapeToSlices(int[] shape)
            throws dap4.core.util.DapException
    {
        if(shape == null)
            return null;
        List<Slice> slices = new ArrayList<>(shape.length);
        for(int i = 0; i < shape.length; i++) {
            Slice sl = new Slice(0, shape[i] - 1, 1);
            slices.add(sl);
        }
        return slices;
    }

    static public dap4.core.util.Index
    cdmIndexToIndex(ucar.ma2.Index cdmidx)
    {
        int rank = cdmidx.getRank();
        int[] shape = cdmidx.getShape();
        long[] indices = new long[shape.length];
        for(int i = 0; i < rank; i++) {
            indices[i] = shape[i];
        }
        dap4.core.util.Index dapidx = new dap4.core.util.Index(indices, indices);
        return dapidx;
    }

    static public ucar.ma2.Index
    indexToCcMIndex(dap4.core.util.Index d4)
    {
        int rank = d4.getRank();
        int[] shape = new int[rank];
        int[] indices = new int[rank];
        for(int i = 0; i < rank; i++) {
            indices[i] = (int) d4.get(i);
            shape[i] = (int) d4.getSize(i);
        }
        ucar.ma2.Index cdm = ucar.ma2.Index.factory(shape);
        cdm.set(indices);
        return cdm;
    }
}
