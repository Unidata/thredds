// $Id: DbaseFieldDesc.java,v 1.2 2004/09/24 03:26:37 caron Exp $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package thredds.viewer.gis.shapefile;

import java.io.DataInputStream;

/**
 * A dBase field descriptor object.  Nothing public here, this is all just for
 * use by DbaseFile and DbaseData.
 * @author Kirk Water
 * @version $Id: DbaseFieldDesc.java,v 1.2 2004/09/24 03:26:37 caron Exp $
 */
class DbaseFieldDesc {
    String Name;
    byte Type;
    int FieldLength;
    int DecimalCount;
    int SetFlags;
    int WorkAreaID;
    byte MDXflag;
    byte[] Header;

    DbaseFieldDesc(DataInputStream in, byte version){
        if((version & 0x03) == 3){
            read_dbase3(in);
        }else{
            read_dbase4(in);
        }
    }

    DbaseFieldDesc(String Name, byte Type, int FieldLength, int DecimalCount,
                   int SetFlags, int WorkAreaID, byte MDXflag){
        this.Name = Name;
        this.Type = Type;
        this.FieldLength = FieldLength;
        this.DecimalCount = DecimalCount;
        this.SetFlags = SetFlags;
        this.WorkAreaID = WorkAreaID;
        this.MDXflag = MDXflag;
        Header = new byte[32];
        for(int i = 0; i < 32; i++){
            Header[i] = (byte)' ';
        }
        // put the name into the header
        byte[] headerName = Name.getBytes();
        for (int i = 0; i < headerName.length; i++) {
            Header[i] = headerName[i];
        }

        Header[11] = Type;
        Header[16] = (byte)FieldLength;
        Header[17] = (byte)DecimalCount;
        Header[23] = (byte)SetFlags;
        Header[20] = (byte)WorkAreaID;
        Header[31] = MDXflag;
    }

    private int read_dbase3(DataInputStream in){
        double ver = 1.02;
        try{
            String os = System.getProperty("java.version");
            ver = Double.valueOf(os).doubleValue();
        }
        catch(NumberFormatException e){
            ver = 1.02;
        }
        Header = new byte[32];
        try{
            in.readFully(Header,0,32);
        }
        catch(java.io.IOException e){
            return -1;
        }
        /*
          catch(java.io.EOFException e){
          return -1;
          }
        */

        /* requires 1.1 compiler or higher */
        Name = new String(Header,0,11);

        Name = Name.trim();
        Type = Header[11];
        FieldLength = (int)Header[16];
        if (FieldLength < 0) FieldLength += 256;
        DecimalCount = (int)Header[17];
        if (DecimalCount < 0) DecimalCount += 256;
        SetFlags = (int)Header[23];
        WorkAreaID = (int)Header[20];
        return 0;
    }

    /* works for dbase 5.0 DOS and Windows too */
    private int read_dbase4(DataInputStream in){
        if(read_dbase3(in) != 0) return -1;
        MDXflag = Header[31];
        SetFlags = 0;
        return 0;
    }

    public String toString(){
        return(Name);
    }
}

/* Change History:
   $Log: DbaseFieldDesc.java,v $
   Revision 1.2  2004/09/24 03:26:37  caron
   merge nj22

   Revision 1.1  2002/12/13 00:55:08  caron
   pass 2

   Revision 1.1.1.1  2002/02/26 17:24:50  caron
   import sources

   Revision 1.4  2000/08/18 04:15:27  russ
   Licensed under GNU LGPL.

   Revision 1.3  1999/06/03 01:43:55  caron
   remove the damn controlMs

   Revision 1.2  1999/06/03 01:26:23  caron
   another reorg

   Revision 1.1.1.1  1999/05/21 17:33:42  caron
   startAgain

# Revision 1.2  1998/12/14  17:11:04  russ
# Add comment for accumulating change histories.
#
*/




