/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.dap4shared;

import dap4.core.data.*;
import dap4.core.dmr.DapSequence;

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

    protected int index = 0;

    protected D4DSP dsp = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public D4DataSequence(D4DSP dsp, DapSequence dap, D4DataCompoundArray cdv, int index)
        throws DataException
    {
        super(dsp, dap);
        this.dsp = dsp;
        this.parent = cdv;
        this.index = index;
     }

    //////////////////////////////////////////////////
    // DataSequence Interface

    @Override
    public long
    getRecordCount()
    {
        return 0;
    }

    @Override
    public DataRecord readRecord(long recordno)
        throws DataException
    {
        return null;
    }
}
