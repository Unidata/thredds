/////////////////////////////////////////////////////////////////////////////
// Copyright (c) 1999, COAS, Oregon State University  
// ALL RIGHTS RESERVED.   U.S. Government Sponsorship acknowledged. 
//
// Please read the full copyright notice in the file COPYRIGHT
// in this directory.
//
// Author: Nathan Potter (ndp@oce.orst.edu)
//
//                        College of Oceanic and Atmospheric Scieneces
//                        Oregon State University
//                        104 Ocean. Admin. Bldg.
//                        Corvallis, OR 97331-5503
//         
/////////////////////////////////////////////////////////////////////////////


 
package dods.servers.ascii;
import java.io.DataInputStream;
import java.io.PrintWriter;
import dods.dap.*;

/**
 */
public class asciiList extends DList implements toASCII {
  
    private static boolean _Debug = false;

     /** Constructs a new <code>asciiList</code>. */
    public asciiList() {
        this(null);
    }

    /**
    * Constructs a new <code>asciiList</code> with name <code>n</code>.
    * @param n the name of the variable.
    */
    public asciiList(String n) {
        super(n);
    }


    /**
    * Returns a string representation of the variables value. This
    * is really foreshadowing functionality for Server types, but
    * as it may come in useful for clients it is added here. Simple 
    * types (example: DFloat32) will return a single value. DConstuctor
    * and DVector types will be flattened. DStrings and DURL's will 
    * have double quotes around them.
    *
    * @param addName is a flag indicating if the variable name should
    * appear at the begining of the returned string.
    */
    public void toASCII(PrintWriter pw,
                        boolean addName,
		        String rootName,
		        boolean newLine){
       
        if(_Debug) System.out.println("asciiList.toASCII("+addName+",'"+rootName+"')  getName(): "+getName());

        rootName = toASCIIAddRootName(pw,addName, rootName);

	if(newLine)
            pw.println("");

		   
        PrimitiveVector pv = getPrimitiveVector();
	
	for(int i=0; i<getLength() ;i++){

            if(pv instanceof BaseTypePrimitiveVector){

                BaseType bt = ((BaseTypePrimitiveVector)pv).getValue(i);

                if(i>0) {
	            if(bt instanceof DString)
	                pw.print(", ");
	            else
	                pw.println("");
                }
                ((toASCII)bt).toASCII(pw,false,null,false);


            }
            else {
	    
                if(i>0)
                    pw.print(", ");
                pv.printSingleVal(pw, i);	    
            }
        }

              
        if(newLine)
            pw.println("");
    }


    public String toASCIIAddRootName(PrintWriter pw, boolean addName, String rootName){
    
        if(addName){
            rootName = toASCIIFlatName(rootName);
            pw.print(rootName);
        }	
	return(rootName);

    }


    public String toASCIIFlatName(String rootName){
    
        String s = "";
        if(rootName != null){
            s = rootName +  "." + getName();
        }
        else {
            s = getName();
        }

	String s2 = "";
        PrimitiveVector pv = getPrimitiveVector();

        if(pv instanceof BaseTypePrimitiveVector){

            BaseType bt = ((BaseTypePrimitiveVector)pv).getValue(0);
            
	    if(_Debug) System.out.println("List["+0+"]: name: "+bt.getName()+"  typeName: "+bt.getTypeName());
	    
            if(bt instanceof DString){
	    
	        //System.out.println("That's a DString!");
	    
	        s2 = s;
            }
            else if(bt instanceof DArray){
	    
	        s2 = ((toASCII)bt).toASCIIFlatName(null);
            }
            else {
	        s2 = ((toASCII)bt).toASCIIFlatName(s);
            }

        }
	else {
	    s2 = s;
	}


        
        if(_Debug) System.out.println("asciiList.toASCIIFlatName().rootName: "+s2);


	return(s2);
    }





  
}
