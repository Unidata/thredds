// $Id: DbaseData.java 50 2006-07-12 16:30:06Z caron $
/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
/**
 *  Class to contain a single field of data from a dbase file
 * @author Russ Rew
 * @version $Id: DbaseData.java 50 2006-07-12 16:30:06Z caron $
 */
package thredds.viewer.gis.shapefile;

import java.io.DataInputStream;
import java.text.*;


public class DbaseData {
    DbaseFieldDesc desc;
    int nrec;	/* number of records */

/**
   Character type data (String[]).
*/
    public final static int TYPE_CHAR = 0;
/**
   Data is an array of doubles (double[]).
*/
    public final static int TYPE_NUMERIC = 1;
/**
   Data is an array of booleans (boolean[]).
*/
    public final static int TYPE_BOOLEAN = 2;

    /* the various possible types */
    String[] character;
    double[] numeric;
    boolean[] logical;
    byte[] field;
    int type;


    DbaseData( DbaseFieldDesc desc, int nrec){
        this.desc = desc;
        this.nrec = nrec;
        field = new byte[desc.FieldLength];
        switch(desc.Type){
        case 'C':
        case 'D':
            character = new String[nrec];
            type = TYPE_CHAR;
            break;
        case 'N':
        case 'F':
            numeric = new double[nrec];
            type=TYPE_NUMERIC;
            break;
        case 'L':
            logical = new boolean[nrec];
            type=TYPE_BOOLEAN;
            break;
        }


    }

/**
   Method to return the type of data for the field
   @return One of TYPE_CHAR, TYPE_BOOLEAN, or TYPE_NUMERIC
*/
    public int getType(){
        return type;
    }

/**
   Method to read an entry from the data stream.  The stream is assumed to be
   in the right spot for reading.  This method should be called from something
   controlling the reading of the entire file.
   @see DbaseFile
*/
    int readRowN(DataInputStream ds, int n ){
        if (n > nrec) return -1;
        /* the assumption here is that the DataInputStream (ds)
         * is already pointing at the right spot!
         */
        try {
            ds.readFully(field,0, desc.FieldLength);
        }
        catch (java.io.IOException e){
            return -1;
        }
        switch(desc.Type){
        case 'C':
        case 'D':
            character[n] = new String(field);
            break;
        case 'N':
            numeric[n] = Double.valueOf(new String(field)).doubleValue();
            break;
        case 'F':	/* binary floating point */
            if(desc.FieldLength == 4){
                numeric[n] = (double)Swap.swapFloat(field,0);
            }else{
                numeric[n] = Swap.swapDouble(field,0);
            }
            break;
        case 'L':
            switch(field[0]){
            case 't':
            case 'T':
            case 'Y':
            case 'y':
                logical[n] = true;
                break;
            default:
                logical[n] = false;
                break;
            }
        default:
            return -1;
        }

        return 0;
    }

/**
   Method to retrieve the double array for this field
   @return An array of doubles with the data
*/
    public double[] getDoubles(){
        return numeric;
    }

/**
   Method to retrieve a double for this field
   @param i index of desired double, assumes 0 < i < getNumRec()
   @return A double with the data
*/
    public double getDouble(int i){
        return numeric[i];
    }

/**
   Method to retrieve a booleans array for this field
   @return An array of boolean values
*/
    public boolean[] getBooleans(){
        return logical;
    }

/**
   Method to retrieve a boolean for this field
   @param i index of desired boolean, assumes 0 < i < getNumRec()
   @return A boolean with the data
*/
    public boolean getBoolean(int i){
        return logical[i];
    }

/**
   Method to retrieve an array of Strings for this field
   @return An array of Strings
*/
    public String[] getStrings(){
        return character;
    }

/**
   Method to retrieve a String for this field
   @param i index of desired String, assumes 0 < i < getNumRec()
   @return A String with the data
*/
    public String getString(int i){
        return character[i];
    }

/**
   Method to retrieve data for this field
   @param i index of desired String, assumes 0 < i < getNumRec()
   @return either a Double, Boolean, or String with the data
*/
    public Object getData(int i){
        switch (type) {
        case TYPE_CHAR:
            return character[i];
        case TYPE_NUMERIC:
            return new Double(numeric[i]);
        case TYPE_BOOLEAN:
            return new Boolean(logical[i]);
        }
        return null;
    }

/**
   @return The number of records in the field.
*/
    public int getNumRec(){
        return nrec;
    }
}

/* Change History:
   $Log: DbaseData.java,v $
   Revision 1.2  2004/09/24 03:26:37  caron
   merge nj22

   Revision 1.1  2002/12/13 00:55:08  caron
   pass 2

   Revision 1.1.1.1  2002/02/26 17:24:50  caron
   import sources

   Revision 1.4  2000/08/18 04:15:26  russ
   Licensed under GNU LGPL.

   Revision 1.3  1999/06/03 01:43:55  caron
   remove the damn controlMs

   Revision 1.2  1999/06/03 01:26:22  caron
   another reorg

   Revision 1.1.1.1  1999/05/21 17:33:42  caron
   startAgain

# Revision 1.2  1998/12/14  17:11:03  russ
# Add comment for accumulating change histories.
#
*/
