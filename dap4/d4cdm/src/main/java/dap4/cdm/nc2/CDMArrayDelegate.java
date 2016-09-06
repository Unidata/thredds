/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.cdm.nc2;

import dap4.core.dmr.*;
import dap4.core.data.DSP;
import dap4.core.data.DSP;

/**
 * Since we can't use AbstractCDMArray,
 * we define a delegator for CDMArray.
 * This class actually is never used, it is just
 * a placeholder to store the API that should be
 * included in every class implementing CMDArray.
 * If this class fails to compile then any fix
 * must be propagated to the CDMArray implementing classes.
 */

/*package*/ class CDMArrayDelegate implements CDMArray
{
    protected DSP dsp = null;
    protected DapVariable template = null;
    protected long bytesize = 0;
    protected DapType basetype = null;
    protected TypeSort primitivetype = null;

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
    public DapVariable getTemplate()
    {
        return this.template;
    }

    @Override
    public long getSizeBytes()
    {
        return this.bytesize;
    }

    @Override
    public DapType getBaseType()
    {
        return this.basetype;
    }

}
