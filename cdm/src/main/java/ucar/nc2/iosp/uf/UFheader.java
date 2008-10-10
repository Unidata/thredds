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
    static final boolean littleEndianData = false;
    String dataFormat = "ARCHIVE2";  // temp setting
    boolean hasVelocity = false;
    boolean hasSpectrum = false;
    boolean hasZdr  = false;
    boolean hasCorrecteddBZ  = false;
    boolean hasTotaldBZ  = false;
    boolean hasRhoHV  = false;
    boolean hasPhiDP  = false;
    boolean hasKdp  = false;
    boolean hasLdrH  = false;
    boolean hasLdrV  = false;

    Ray firstRay = null;

    List<Ray> velocityGroup = new ArrayList<Ray>();
    List<Ray> spectrumGroup = new ArrayList<Ray>();
    List<Ray> zdrGroup = new ArrayList<Ray>();
    List<Ray> correcteddBZGroup = new ArrayList<Ray>();
    List<Ray> totaldBZGroup = new ArrayList<Ray>();
    List<Ray> rhoHVGroup = new ArrayList<Ray>();
    List<Ray> phiDPGroup = new ArrayList<Ray>();
    List<Ray> kdpGroup = new ArrayList<Ray>();
    List<Ray> ldrHGroup = new ArrayList<Ray>();
    List<Ray> ldrVGroup = new ArrayList<Ray>();

    private int max_radials = 0;
    private int min_radials = Integer.MAX_VALUE;


    public boolean isValidFile(ucar.unidata.io.RandomAccessFile raf){
        try
        {
            raf.seek(0);
            raf.order(RandomAccessFile.BIG_ENDIAN);
            byte [] b4 = new byte[4];
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

        List<Ray> velocityList = new ArrayList<Ray>();
        List<Ray> spectrumList = new ArrayList<Ray>();
        List<Ray> zdrList = new ArrayList<Ray>();
        List<Ray> correcteddBZList = new ArrayList<Ray>();
        List<Ray> totaldBZList = new ArrayList<Ray>();
        List<Ray> rhoHVList = new ArrayList<Ray>();
        List<Ray> phiDPList = new ArrayList<Ray>();
        List<Ray> kdpList = new ArrayList<Ray>();
        List<Ray> ldrHList = new ArrayList<Ray>();
        List<Ray> ldrVList = new ArrayList<Ray>();

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
            if(r.hasVR)velocityList.add(r);
            if(r.hasSW)spectrumList.add(r);
            if(r.hasDR)zdrList.add(r);
            if(r.hasCZ)correcteddBZList.add(r);
            if(r.hasDZ)totaldBZList.add(r);
            if(r.hasRH)rhoHVList.add(r);
            if(r.hasPH)phiDPList.add(r);
            if(r.hasKD)kdpList.add(r);
            if(r.hasLH)ldrHList.add(r);
            if(r.hasLV)ldrVList.add(r);
            rayNumber ++;

          //  System.out.println("Ray Number = " + rayNumber);

        }

        if(velocityList.size() > 0) {
            hasVelocity = true;
            velocityGroup = sortScans("velocity", velocityList);
        }
        if(spectrumList.size() > 0) {
            hasSpectrum = true;
            spectrumGroup = sortScans("spectrum", spectrumList);
        }
        if(zdrList.size()> 0) {
            hasZdr  = true;
            zdrGroup = sortScans("zdr", zdrList);
        }
        if(correcteddBZList.size() > 0) {
            hasCorrecteddBZ  = true;
            correcteddBZGroup = sortScans("correctedDBZ", correcteddBZList);
        }
        if(totaldBZList.size() > 0) {
            hasTotaldBZ  = true;
            totaldBZGroup  = sortScans("totalDBZ", totaldBZList);
        }
        if(rhoHVList.size() > 0) {
            hasRhoHV  = true;
            rhoHVGroup  = sortScans("rhoHV", rhoHVList);
        }
        if(phiDPList.size() > 0) {
            hasPhiDP  = true;
            phiDPGroup = sortScans("phiDP", phiDPList);
        }
        if(kdpList.size() > 0) {
            hasKdp  = true;
            kdpGroup = sortScans("kdp", kdpList);
        }
        if(ldrHList.size() > 0) {
            hasLdrH  = true;
            ldrHGroup = sortScans("ldrH", ldrHList);
        }
        if(ldrVList.size() > 0) {
            hasLdrV  = true;
            ldrVGroup = sortScans("ldrV", ldrVList);
        }

        //System.out.println("Herr " +velocityList.size());
        //return;
    }

    private ArrayList sortScans(String name, List rays) {

        // now group by elevation_num
        HashMap groupHash = new HashMap(600);
        for (int i = 0; i < rays.size(); i++) {
          Ray r = (Ray) rays.get(i);
          Integer groupNo = new Integer(r.uf_header2.elevation);

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

    public float getHorizontalBeamWidth(){
        return firstRay.getHorizontalBeamWidth();
    }
    public String getStationId(){
        return firstRay.uf_header2.siteName;
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
