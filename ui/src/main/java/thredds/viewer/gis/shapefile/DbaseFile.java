// $Id: DbaseFile.java 50 2006-07-12 16:30:06Z caron $
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
package thredds.viewer.gis.shapefile;

import java.net.URL;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.DataOutputStream;

/**
 * Class to read a dbase file in its entirety.
 *
 * @author  Kirk Waters, NOAA Coastal Services Center, 1997.
 * @author  Russ Rew, modified to restrict access to read-only
 * @version $Id: DbaseFile.java 50 2006-07-12 16:30:06Z caron $
 */
public class DbaseFile extends Object {

    static public int DBASEIII = 0;
    static public int DBASEIV = 1;
    static public int DBASE5DOS = 2;
    static public int DBASE5WIN = 3;

    URL url;
    byte filetype;
    byte updateYear;
    byte updateMonth;
    byte updateDay;
    int nrecords;
    int nfields;
    short nbytesheader;
    short nbytesrecord;
    DbaseFieldDesc[] FieldDesc;
    DbaseData[] data;
    byte[] Header;
    private boolean headerLoaded = false;
    private boolean dataLoaded = false;
    InputStream stream = null;
    DataInputStream ds = null;

/**
   @param url URL to the *.dbf file
*/
    public DbaseFile(URL url)
        throws java.net.MalformedURLException,java.io.IOException
        {
            this.url = url;
            stream = url.openStream();
        }

/**
   @param spec Location of the *.dbf file, as either a URL or filename
*/
    public DbaseFile(String spec) throws java.io.IOException {
        try{
            url = new URL(spec);
            stream = url.openStream();
        }
        catch(java.net.MalformedURLException e){
            stream = new FileInputStream(spec);
            if(stream == null){
                System.out.println("Got a null trying to open " + spec);
                throw new java.io.IOException("Failed to open stream");
            }
        }
    }

/**
   @param file A file object of the *.dbf file.
*/
    public DbaseFile(File file){
        try{
            stream = new FileInputStream(file);
        }catch(java.io.FileNotFoundException e){
            System.out.println("Failed to open file " + file);
            stream = null;
        }
    }

    public DbaseFile(InputStream s){
        stream = s;
    }

/**
   Load the dbase file header.
   @return 0 for success, -1 for failure
*/
    private int loadHeader(){
        if(headerLoaded) return 0;
        InputStream s = stream;
        if(s == null) return -1;
        try{
            BufferedInputStream bs = new BufferedInputStream(s);
            ds = new DataInputStream(bs);
            /* read the header as one block of bytes*/
            Header = new byte[32];
            ds.read(Header);
            //System.out.println("dbase header is " + Header);
            if(Header[0] == '<'){ //looks like html coming back to us!
                close(ds);
                return -1;
            }
            filetype=Header[0];
            updateYear=Header[1];
            updateMonth=Header[2];
            updateDay=Header[3];
            /* 4 bytes for number of records is in little endian */
            nrecords = Swap.swapInt(Header,4);
            nbytesheader = Swap.swapShort(Header,8);
            nbytesrecord = Swap.swapShort(Header,10);

            /* read in the Field Descriptors */
            /* have to figure how many there are from
             * the header size.  Should be nbytesheader/32 -1
             */
            nfields = (int)(nbytesheader/32) -1;
            if(nfields < 1){
                System.out.println("nfields = "+nfields);
                System.out.println("nbytesheader = " +
                                   nbytesheader);
                return -1;
            }
            FieldDesc = new DbaseFieldDesc[nfields];
            data = new DbaseData[nfields];
            for(int i=0; i < nfields; i++){
                FieldDesc[i] = new DbaseFieldDesc(ds, filetype);
                data[i] = new DbaseData( FieldDesc[i],
                                         nrecords);
            }

            /* read the last byte of the header (0x0d) */
            ds.readByte();

            headerLoaded = true;
        }
        catch(java.io.IOException e){
            close(s);
            return -1;
        }
        return 0;
    }

/**
   Load the dbase file data.
   @return 0 for success, -1 for failure
*/
    private int loadData(){
        if(!headerLoaded) return -1;
        if(dataLoaded) return 0;
        InputStream s = stream;
        if(s == null) return -1;
        try{
            /* read in the data */
            for(int i = 0; i < nrecords; i++){
                                /* read the data record indicator */
                byte recbyte = ds.readByte();
                if(recbyte == 0x20 ){
                    for(int j = 0; j < nfields; j++){
                        data[j].readRowN(ds,i);
                    }
                }else{
                    /* a deleted record */
                    nrecords--;
                    i--;
                }
            }
            dataLoaded = true;
        }
        catch(java.io.IOException e){
            close(s);
            return -1;
        }
        /*
          catch(java.net.UnknownServiceException e){
          return -1;
          }
        */
        finally {
            if(s != null){
                close(s);
            }
        }
        return 0;
    }

    private void close(InputStream d){
        if(d == null) return;
        try{
            d.close();
        }
        catch(java.io.IOException e){}
        catch(NullPointerException e){}
    }

/**
   Extract the data for a field by field index number.
   @param index Column number of the field to extract.
   @return A DbaseData object if the column is within bounds. Otherwise, null.
*/
    public DbaseData getField(int index){
        if(index < 0 || index >= nfields) return null;
        return data[index];
    }

/**
   Extract the data for a given field by name.
   @param Name String with the name of the field to retrieve.
   @return A DbaseData object if the name was found or null if not found
*/
    public DbaseData getField(String Name){
        for(int i = 0; i < nfields; i++){
            if(FieldDesc[i].Name.equals(Name)) return data[i];
        }
        return null;
    }

/**
   Extract the double array of data for a field by Name.
   @param Name String with the name of the field to retrieve
   @return A double[] if valid numeric field, otherwise null
*/
    public double[] getDoublesByName(String Name){
        DbaseData d;
        if((d = getField(Name)) == null) return null;
        if(d.getType() == DbaseData.TYPE_CHAR){
            String [] s = d.getStrings();
            double[] dd = new double[s.length];
            for(int i = 0; i < s.length; i++){
                dd[i] = Double.valueOf(s[i]).doubleValue();
            }
            return dd;
        }
        if(d.getType() == DbaseData.TYPE_BOOLEAN){
            boolean[] b = d.getBooleans();
            double[] dd = new double[b.length];
            for(int i = 0; i < b.length; i++){
                if(b[i]){
                    dd[i] = 1;
                }else{
                    dd[i] = 0;
                }
            }
            return dd;

        }
        return d.getDoubles();
    }

/**
   Extract the string array of data for a field by Name.
   @param Name String with the name of the field to retrieve
   @return A String[] if valid character field, otherwise null
*/
    public String[] getStringsByName(String Name){
        DbaseData d;
        if((d = getField(Name)) == null) return null;
        if(d.getType() != DbaseData.TYPE_CHAR) return null;
        return d.getStrings();
    }

/**
   Extract the boolean array of data for a field by Name.
   @param Name String with the name of the field to retrieve
   @return A boolean[] if valid character field, otherwise null
*/
    public boolean[] getBooleansByName(String Name){
        DbaseData d;
        if((d = getField(Name)) == null) return null;
        if(d.getType() != DbaseData.TYPE_BOOLEAN) return null;
        return d.getBooleans();
    }

/**
   Get the name of a field by column number.
   @param i The column number of the field name.
   @return A String with the field name or null if out of bounds
*/
    public String getFieldName(int i){
        if(i >= nfields || i < 0){
            return null;
        }
        return(FieldDesc[i].Name);
    }

/**
   Get a list of all the field names in the dbase file
   @return A String array of all the field names
*/
    public String[] getFieldNames(){
        String [] s = new String[nfields];
        for(int i = 0; i < nfields; i++){
            s[i] = new String(getFieldName(i));
        }
        return(s);
    }

/**
   Get the number of fields in the file.
   @return number of fields
*/
    public int getNumFields(){
        return (nfields);
    }

/**
    @return number of records in the dbase file
*/
    public int getNumRecords(){
        return(nrecords);
    }

/**
   @return Boolean true if the data has been loaded, otherwise false.
*/
    public boolean isLoaded(){
        return(dataLoaded);
    }

    /**
     * Test program, dumps a Dbase file to stdout.
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("filename or URL required");
            System.exit(-1);
        }
        for (int i = 0; i < args.length; i++) {
            String s = args[i];
            System.out.println("*** Dump of Dbase " + s + ":");
            try {
                DbaseFile dbf = new DbaseFile(s);
                // load() method reads all data at once
                if (dbf.loadHeader() != 0) {
                    System.out.println("Error loading header" + s);
                    System.exit(-1);
                }

                // output schema as [type0 field0, type1 field1, ...]
                String[] fieldNames = dbf.getFieldNames();
                System.out.print("[");

                int nf = dbf.getNumFields();
                DbaseData[] dbd = new DbaseData[nf];
                for(int field = 0; field < nf; field++) {
                    dbd[field] = dbf.getField(field);
                    switch(dbd[field].getType()) {
                    case DbaseData.TYPE_BOOLEAN:
                        System.out.print("boolean ");
                        break;
                    case DbaseData.TYPE_CHAR:
                        System.out.print("String ");
                        break;
                    case DbaseData.TYPE_NUMERIC:
                        System.out.print("double ");
                        break;
                    }
                    System.out.print(fieldNames[field]);
                    if (field < nf - 1)
                        System.out.print(", ");
                }
                System.out.println("]");

                if (dbf.loadData() != 0) {
                    System.out.println("Error loading data" + s);
                    System.exit(-1);
                }

                // output data
                for(int rec = 0; rec < dbf.getNumRecords(); rec++) {
                    for(int field = 0; field < nf; field++){
                        System.out.print(dbd[field].getData(rec));
                        if (field < nf - 1)
                            System.out.print(", ");
                        else
                            System.out.println();
                    }
                }
            } catch (java.io.IOException e) {
                System.out.println(e);
                System.exit(-1);
            }
        }
    }
}

/* Change History:
   $Log: DbaseFile.java,v $
   Revision 1.2  2004/09/24 03:26:38  caron
   merge nj22

   Revision 1.1  2002/12/13 00:55:08  caron
   pass 2

   Revision 1.1.1.1  2002/02/26 17:24:50  caron
   import sources

   Revision 1.4  2000/08/18 04:15:27  russ
   Licensed under GNU LGPL.

   Revision 1.3  1999/06/03 01:43:56  caron
   remove the damn controlMs

   Revision 1.2  1999/06/03 01:26:23  caron
   another reorg

   Revision 1.1.1.1  1999/05/21 17:33:42  caron
   startAgain

# Revision 1.2  1998/12/14  17:11:05  russ
# Add comment for accumulating change histories.
#
*/
