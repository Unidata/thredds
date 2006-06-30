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

package dods.util; // JC-CHANGED

import java.io.*;
import java.util.*;

import dods.util.iniFile;


public class test_iniFile {


    //************************************************************************
    /**
    */
    public static void main(String args[]){
        boolean dbgFlag = true;
                iniFile inf = null;

        switch(args.length){
            case 1:
                inf = new iniFile(null,args[0],dbgFlag);
                break;

            case 2:
                inf = new iniFile(args[0], args[1],dbgFlag);
                break;

            case 3:
                if(args[2].equalsIgnoreCase("false"))
                    dbgFlag = false;
                inf = new iniFile(args[0], args[1],dbgFlag);
                break;

            default:
                System.err.println("Usage: test_iniFile [path] filename.ini [false]");
                System.exit(1);
        }


        inf.printProps(System.out);


    }
    //************************************************************************


}


