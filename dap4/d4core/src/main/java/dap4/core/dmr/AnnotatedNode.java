/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.dmr;

public interface AnnotatedNode
{
    public void annotate(Integer key, Object value);

    public Object annotation(Integer key);

    public void clearAnnotations();
}
