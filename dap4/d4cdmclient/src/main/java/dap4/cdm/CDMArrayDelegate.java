/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.cdm;

import dap4.cdmshared.CDMUtil;
import dap4.core.dmr.*;
import dap4.core.util.DapException;
import dap4.dap4shared.DSP;
import dap4.dap4shared.D4DSP;
import ucar.ma2.Range;

import java.util.List;

/**
 * Since we can't use AbstractCDMArray,
 * we define a delegator for CDMArray.
 * This class actually is never used, it is just
 * a placeholder to store the API that should be
 * included in every class implementing CMDArray.
 * If this class fails to compile then any fix
 * must be propagated to the CDMArray implementing classes.
 */

class CDMArrayDelegate implements CDMArray
{
    protected CDMDataset root = null;
    protected D4DSP dsp = null;
    protected DapVariable template = null;
    protected long bytesize = 0;
    protected DapType basetype = null;
    protected AtomicType primitivetype = null;

    /* The implementing class will need to
       initialize the fields.
        this.template = template;
        this.bytesize = 0;
        this.root = root;
        this.dsp = dsp;
        this.bytesize = size;
        this.basetype = this.template.getBaseType();
	this.primitivetype = this.basetype.getPrimitiveType();
    */

    @Override
    public DSP getDSP()
    {
        return this.dsp;
    }

    @Override
    public CDMDataset getRoot()
    {
        return this.root;
    }

    @Override
    public DapVariable getTemplate()
    {
        return this.template;
    }

    @Override
    public long getByteSize()
    {
        return this.bytesize;
    }

    @Override
    public DapType getBaseType()
    {
        return this.basetype;
    }

    @Override
    public AtomicType getPrimitiveType()
    {
        return this.primitivetype;
    }

}
