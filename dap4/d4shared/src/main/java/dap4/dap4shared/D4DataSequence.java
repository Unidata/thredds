/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.dap4shared;

import dap4.core.data.*;
import dap4.core.dmr.DapSequence;

import java.util.ArrayList;
import java.util.List;

public class D4DataSequence extends D4DataVariable implements DataSequence
{
    //////////////////////////////////////////////////
    // Type Decls

    /*
    static class Field
    {
        DapVariable field;
        SequenceMembers.Member member;
        int index;

        public Field(DapVariable field, int index, SequenceMembers.Member member)
        {
            this.field = field;
            this.member = member;
            this.index = index;
        }
    }
    */

    //////////////////////////////////////////////////
    // Instance variables

    protected D4DataCompoundArray parent = null;
    //Coverity[FB.URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD]
    protected int index = 0;
    List<D4DataRecord> records = new ArrayList<D4DataRecord>();

    //////////////////////////////////////////////////
    // Constructor(s)

    /**
     * @param dsp   The containing DSP
     * @param dap   The template for this sequence
     * @param cdv   the parent compound array
     * @param index within the parent compound array
     * @return A D4DataSequence for the records for this sequence.
     * @throws DataException
     */
    public D4DataSequence(D4DSP dsp, DapSequence dap, D4DataCompoundArray cdv, int index)
            throws DataException
    {
        super(dsp, dap);
        this.dsp = dsp;
        this.parent = cdv;
        this.index = index;
    }

    //////////////////////////////////////////////////
    // Accessors

    public void addRecord(D4DataRecord record)
    {
        records.add(record);
    }

    //////////////////////////////////////////////////
    // DataSequence Interface

    @Override
    public long
    getRecordCount()
    {
        return records.size();
    }

    @Override
    public DataRecord readRecord(long recordno)
            throws DataException
    {
        if (recordno < 0 || recordno >= records.size())
            throw new DataException("Illegal record index: " + recordno);
        return records.get((int) recordno);
    }
}
