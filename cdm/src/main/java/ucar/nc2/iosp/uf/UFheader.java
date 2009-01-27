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
package ucar.nc2.iosp.uf;

import ucar.unidata.io.RandomAccessFile;
import ucar.nc2.iosp.cinrad.Cinrad2Record;

import java.nio.ByteBuffer;
import java.io.IOException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Sep 24, 2008
 * Time: 3:53:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class UFheader {
    ucar.unidata.io.RandomAccessFile raf;
    ucar.nc2.NetcdfFile ncfile;
    static final boolean littleEndianData = true;
    String dataFormat = "UNIVERSALFORMAT";  // temp setting
    Ray firstRay = null;


    HashMap variableGroup;
    private int max_radials = 0;
    private int min_radials = Integer.MAX_VALUE;


    public boolean isValidFile(ucar.unidata.io.RandomAccessFile raf){
        try
        {
            raf.seek(0);
            raf.order(RandomAccessFile.BIG_ENDIAN);
            byte [] b6 = new byte[6];
            byte [] b4 = new byte[4];

            raf.read(b6, 0, 6);
            String ufStr = new String(b6, 4, 2);
            if(!ufStr.equals("UF"))
                return false;
            //if ufStr is UF, then a further checking apply
            raf.seek(0);
            raf.read(b4, 0, 4);
            int rsize = bytesToInt(b4, false);

            byte [] buffer = new byte[rsize];
            long offset = raf.getFilePointer();
            raf.read(buffer, 0, rsize);
            raf.read(b4, 0, 4);

            int endPoint = bytesToInt(b4, false);
            if(endPoint != rsize) {
                return false;
            }

            ByteBuffer bos = ByteBuffer.wrap(buffer);
            firstRay = new Ray(bos, rsize, offset);
                       
        }
        catch ( IOException e )
        {
            return( false );
        }
        return true;
    }

    void read(ucar.unidata.io.RandomAccessFile raf, ucar.nc2.NetcdfFile ncfile ) throws IOException {
        this.raf = raf;
        this.ncfile = ncfile;
        variableGroup = new HashMap();


        raf.seek(0);
        raf.order(RandomAccessFile.BIG_ENDIAN);
        int rayNumber = 0;
         /* get bad data flag from universal header */
        while (!raf.isAtEndOfFile()) {
            byte [] b4 = new byte[4];
            raf.read(b4, 0, 4);

            int rsize = bytesToInt(b4, false);

            byte [] buffer = new byte[rsize];

            long offset = raf.getFilePointer();

            raf.read(buffer, 0, rsize);
            raf.read(b4, 0, 4);

            int endPoint = bytesToInt(b4, false);

             if(endPoint != rsize || rsize == 0) {
           //     System.out.println("Herr " +velocityList.size());
                continue;
            }

            ByteBuffer bos = ByteBuffer.wrap(buffer);

            Ray r = new Ray(bos, rsize, offset);
            if(firstRay == null) firstRay = r;
            rayNumber ++;

            HashMap rayMap = r.field_header_map;
            Set kSet = rayMap.keySet();
            for(Iterator it = kSet.iterator(); it.hasNext();) {
                String ab = (String)it.next();
                ArrayList group = (ArrayList) variableGroup.get(ab);
                if (null == group) {
                    group = new ArrayList();
                    variableGroup.put(ab, group);
                }
                group.add(r);
            }

          //  System.out.println("Ray Number = " + rayNumber);

        }
        Set vSet = variableGroup.keySet();
        for(Iterator it = vSet.iterator(); it.hasNext();) {
            String key = (String)it.next();
            ArrayList group = (ArrayList) variableGroup.get(key);
            ArrayList sgroup = sortScans(key, group);
            //variableGroup.remove(key);
            variableGroup.put(key, sgroup);
        }

        //System.out.println("Herr " +velocityList.size());
        //return;
    }

    private ArrayList sortScans(String name, List rays) {

        // now group by elevation_num
        HashMap groupHash = new HashMap(600);
        for (int i = 0; i < rays.size(); i++) {
          Ray r = (Ray) rays.get(i);
          Integer groupNo = new Integer(r.uf_header2.sweepNumber); //.elevation);

          ArrayList group = (ArrayList) groupHash.get(groupNo);
          if (null == group) {
            group = new ArrayList();
            groupHash.put(groupNo, group);
          }

          group.add(r);
        }
        
        // sort the groups by elevation_num
        ArrayList groups = new ArrayList(groupHash.values());
        Collections.sort(groups, new GroupComparator());

        for (int i = 0; i < groups.size(); i++) {
            ArrayList group = (ArrayList) groups.get(i);

            max_radials = Math.max(max_radials, group.size());
            min_radials = Math.min(min_radials, group.size());
        }

        return groups;
    }

    public float getMeanElevation(String key, int eNum){
        ArrayList gp = (ArrayList)getGroup(key);
        float meanEle = getMeanElevation(gp);
        return meanEle;
    }

    public float getMeanElevation(ArrayList<Ray> gList){
        float sum = 0;
        int size = 0;

        Iterator it = gList.iterator();
        while(it.hasNext()){
            Ray r = (Ray)it.next();
            sum += r.getElevation();
            size++;
        }

        return sum/size;
    }

    public List getGroup(String key){
        return (ArrayList)variableGroup.get(key);
    }

    public int getMaxRadials() {
        return max_radials;
    }

    public String getDataFormat() {
        return dataFormat;
    }

    public Date getStartDate() {
        return firstRay.getDate();
    }

    public Date getEndDate() {
        return firstRay.getDate();
    }

    public float getHorizontalBeamWidth(String ab){
        return firstRay.getHorizontalBeamWidth(ab);
    }

    public String getStationId(){
        return firstRay.uf_header2.siteName;
    }

    public Short getSweepMode(){
        return firstRay.uf_header2.sweepMode;
    }

    public float getStationLatitude(){
        return firstRay.getLatitude();
    }

    public float getStationLongitude(){
        return firstRay.getLongtitude();
    }

    public float getStationElevation() {
        return firstRay.getElevation();
    }

    
    public short getMissingData() {
        return firstRay.getMissingData();
    }
    
    private class GroupComparator implements Comparator {

        public int compare(Object o1, Object o2) {
          List group1 = (List) o1;
          List group2 = (List) o2;
          Ray ray1 = (Ray) group1.get(0);
          Ray ray2 = (Ray) group2.get(0);

          //if (record1.elevation_num != record2.elevation_num)
          return (ray1.uf_header2.elevation - ray2.uf_header2.elevation < 13 ? 0 : 1);
          //return record1.cut - record2.cut;
        }
    }


    protected short getShort(byte[] bytes, int offset) {
        int ndx0 = offset + (littleEndianData ? 1 : 0);
        int ndx1 = offset + (littleEndianData ? 0 : 1);
        // careful that we only allow sign extension on the highest order byte
        return (short)(bytes[ndx0] << 8 | (bytes[ndx1] & 0xff));
    }


    public static int bytesToShort(byte a, byte b, boolean swapBytes) {
        // again, high order bit is expressed left into 32-bit form
        if (swapBytes) {
            return (a & 0xff) + ((int)b << 8);
        } else {
            return ((int)a << 8) + (b & 0xff);
        }
    }
    public static int bytesToInt(byte [] bytes, boolean swapBytes) {
        byte a = bytes[0];
        byte b = bytes[1];
        byte c = bytes[2];
        byte d = bytes[3];
        if (swapBytes) {
            return ((a & 0xff) ) +
                ((b & 0xff) << 8 ) +
                ((c & 0xff) << 16 ) +
                ((d & 0xff) << 24);
        } else {
            return ((a & 0xff) << 24 ) +
                ((b & 0xff) << 16 ) +
                ((c & 0xff) << 8 ) +
                ((d & 0xff) );
        }
    }

    
}
