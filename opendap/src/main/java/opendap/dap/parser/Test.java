
import opendap.dap.*;
import opendap.dap.parser.*;
import java.io.InputStream;

class Test
{
    public static void main(String[] argv)
	throws Exception
    {
	if(argv[0].equals("dap")) {
            DapParser parser = new DapParser(System.in);
//	    parser.setDebugLevel(1);
	    parser.parse();
	    if(parser.getDDSroot() != null)
	        parser.getDDSroot().print(System.out);
	    if(parser.getDASroot() != null)
	        parser.getDASroot().print(System.out);
	} else {
            DAP2Exception de = new DAP2Exception();
            ErrorParser parser = new ErrorParser(System.in);
//	    parser.setDebugLevel(1);
	    parser.ErrorObject(de);
	    System.out.printf("Error {\n    code = %d;\n    message = \"%s\";\n    program type = %d;\n    program = \"%s\";\n};\n",
		de.getErrorCode(),
		de.getErrorMessage(),
		de.getProgramType(),
		de.getProgramSource());
	}
    }
}
