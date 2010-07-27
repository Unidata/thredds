package opendap.dap.parser;

import opendap.dap.BaseType;
import java.util.Vector;

public class OClist extends Vector<Object>
{
    public void pop() {remove(size()-1);}
}
