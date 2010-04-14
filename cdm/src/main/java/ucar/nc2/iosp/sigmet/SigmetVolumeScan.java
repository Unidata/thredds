/*
 * Copyright (c) 1998 - 2010. University Corporation for Atmospheric Research/Unidata
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



package ucar.nc2.iosp.sigmet;

//~--- non-JDK imports --------------------------------------------------------

import ucar.nc2.Variable;
import ucar.nc2.iosp.nexrad2.Level2Record;
import ucar.unidata.io.RandomAccessFile;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Apr 7, 2010
 * Time: 2:25:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class SigmetVolumeScan {
    String[]   data_name = {
        " ", "TotalPower", "Reflectivity", "Velocity", "Width", "DifferentialReflectivity"
    };
    private List<List<Ray>> differentialReflectivityGroups;
    private List<List<Ray>> reflectivityGroups;
    private List<List<Ray>> totalPowerGroups;
    private List<List<Ray>> velocityGroups;
    private List<List<Ray>> widthGroups;
    private List<List<Ray>> timeGroups;
    private int[] num_gates;
    public int[] base_time;
    public short[] year;
    public short[] month;
    public short[] day;
    public Ray firstRay = null;
    public Ray lastRay = null;
    public ucar.unidata.io.RandomAccessFile raf;
    public boolean hasReflectivity = false;
    public boolean hasVelocity = false;
    public boolean hasWidth = false;
    public boolean hasTotalPower = false;
    public boolean hasDifferentialReflectivity = false;
    public boolean hasTime = false;

    /**
     * Read all the values from SIGMET-IRIS file which are necessary to fill in the ncfile.
     *  @param raf      ucar.unidata.io.RandomAccessFile corresponds to SIGMET datafile.
     *  @param ncfile   an empty NetcdfFile object which will be filled.
     *  @param varList  ArrayList of Variables of ncfile
     */
    SigmetVolumeScan(ucar.unidata.io.RandomAccessFile raf, ucar.nc2.NetcdfFile ncfile, ArrayList<Variable> varList)
            throws java.io.IOException {
        final int REC_SIZE      = 6144;
        int       len           = 12288;    // ---- Read from the 3d record----------- 6144*2=12288
        short     nrec          = 0,
                  nsweep        = 1,
                  nray          = 0,
                  byteoff       = 0;
        int       nwords        = 0,
                  end_words     = 0,
                  data_read     = 0,
                  num_zero      = 0,
                  rays_count    = 0,
                  nb            = 0,
                  pos           = 0,
                  pos_ray_hdr   = 0,
                  t             = 0;
        short     a0            = 0,
                  a00           = 0,
                  dty           = 1;
        short     beg_az        = 0,
                  beg_elev      = 0,
                  end_az        = 0,
                  end_elev      = 0,
                  num_bins      = 0,
                  time_start_sw = 0;
        float     az            = 0.0f,
                  elev          = 0.0f,
                  d             = 0.0f,
                  step          = 0.0f;
   //     byte      data          = 0;
        boolean   beg_rec       = true,
                  end_rec       = true,
                  read_ray_hdr  = true,
                  begin         = true;
        int       cur_len       = len,
                  beg           = 1,
                  kk            = 0,
                  col           = 0,
                  nu            = 0,
                  bt0           = 0,
                  bt1           = 0;
        int       start_sweep   = 1,
                  end_sweep     = 1,
                  start_ray     = 1,
                  end_ray       = 1;

        // Input
        this.raf = raf;
        raf.order(RandomAccessFile.LITTLE_ENDIAN);

        int                           fileLength    = (int) raf.length();
        java.util.Map<String, Number> recHdr        = SigmetIOServiceProvider.readRecordsHdr(raf);
        int                           nparams       = ((Integer) recHdr.get("nparams")).intValue();    // System.out.println("DO: nparams="+nparams);
        short                         number_sweeps = ((Short) recHdr.get("number_sweeps")).shortValue();

        // System.out.println("DO: number_sweeps="+number_sweeps);
        short num_rays = ((Short) recHdr.get("num_rays")).shortValue();

        // System.out.println("DO: num_rays="+num_rays);
        int   range_1st   = ((Integer) recHdr.get("range_first")).intValue();
        float range_first = range_1st * 0.01f;
        int   stepp       = ((Integer) recHdr.get("range_last")).intValue();

        // System.out.println("DO: stepp="+stepp);
        float range_last = stepp * 0.01f;
        short bins       = ((Short) recHdr.get("bins")).shortValue();

        // System.out.println("DO: bins="+bins);
       // int[]   base_time    = new int[nparams * number_sweeps];

        short[] num_sweep    = new short[nparams];
        short[] num_rays_swp = new short[nparams];
        short[] indx_1ray    = new short[nparams];
        short[] num_rays_act = new short[nparams];
        short[] angl_swp     = new short[nparams];
        short[] bin_len      = new short[nparams];
        short[] data_type    = new short[nparams];
       // float[] dd           = new float[bins];
        num_gates=new int[number_sweeps];
        end_sweep = (int) number_sweeps;
        end_ray   = (int) num_rays;
        base_time=new int[nparams*number_sweeps];
        year         = new short[nparams * number_sweeps];
        month        = new short[nparams * number_sweeps];
        day          = new short[nparams * number_sweeps];
        // Array of Ray objects is 2D. Number of columns=number of rays
        // Number of raws = number of types of data if number_sweeps=1,
        // or number of raws = number_sweeps
        List<Ray> totalPower       = new ArrayList<Ray>();
        List<Ray> velocity         = new ArrayList<Ray>();
        List<Ray> reflectivity     = new ArrayList<Ray>();
        List<Ray> width            = new ArrayList<Ray>();
        List<Ray> diffReflectivity = new ArrayList<Ray>();
        List<Ray> time = new ArrayList<Ray>();
        int       irays            = (int) num_rays;
        Ray       ray              = null;
        int two = 0;
        // init array
        float[] val = new float[bins];

        while (len < fileLength) {
            int     rayoffset  = 0;
            int     rayoffset1 = 0;
            int     datalen    = 0;

            cur_len = len;

            if (nsweep == number_sweeps & rays_count == beg) {
                return;
            }

            if (beg_rec) {

                // --- <raw_prod_bhdr>  12bytes -----------
                raf.seek(cur_len);
                nrec    = raf.readShort();    // cur_len
                nsweep  = raf.readShort();    // cur_len+2
                byteoff = raf.readShort();
                len     = len + 2;            // cur_len+4
                nray    = raf.readShort();
                len     = len + 2;            // cur_len+6

                // ---- end of <raw_prod_bhdr> -------------
                cur_len = cur_len + 12;
                beg_rec = false;
            }

            if ((nsweep <= number_sweeps) & (rays_count % beg == 0)) {

                // --Read <ingest_data_hdrs> Number of them=nparams*number_sweeps -----
                // ---Len of <ingest_data_hdr>=76 bytes -----------------
                beg = 0;

                for (int i = 0; i < nparams; i++) {
                    int idh_len = cur_len + 12 + i * 76;

                    raf.seek(idh_len);

                    // Read seconds since midnight
                    base_time[nu] = raf.readInt();         // idh_len
                    raf.skipBytes(2);
                    year[nu]  = raf.readShort();           // idh_len+6
                    month[nu] = raf.readShort();           // idh_len+8
                    day[nu]   = raf.readShort();           // idh_len+10
                    nu++;
                    num_sweep[i]    = raf.readShort();     // idh_len+12
                    num_rays_swp[i] = raf.readShort();     // idh_len+14
                    indx_1ray[i]    = raf.readShort();     // idh_len+16
                    raf.skipBytes(2);
                    num_rays_act[i] = raf.readShort();
                    beg             += num_rays_act[i];    // idh_len+20
                    angl_swp[i]     = raf.readShort();     // idh_len+22
                    bin_len[i]      = raf.readShort();     // idh_len+24
                    data_type[i]    = raf.readShort();     // idh_len+26
                }

                cur_len = cur_len + nparams * 76;
            }

            len = cur_len;

            if (end_rec) {

                // --- Read compression code=2 bytes from cur_len
                raf.seek(cur_len);
                a0      = raf.readShort();
                cur_len = cur_len + 2;

                // --- Check if the code=1 ("1" means an end of a ray)
                if (a0 == (short) 1) {
                    if (cur_len % REC_SIZE == 0) {
                        beg_rec = true;
                        end_rec = true;
                        rays_count++;
                        read_ray_hdr = true;
                        pos          = 0;
                        data_read    = 0;
                        nb           = 0;
                        len          = cur_len;
                    } else {
                        end_rec = true;
                        len     = cur_len;
                        rays_count++;
                    }

                    continue;
                }

                nwords    = a0 & 0x7fff;
                end_words = nwords - 6;
                data_read = end_words * 2;
                end_rec   = false;

                if (cur_len % REC_SIZE == 0) {
                    len          = cur_len;
                    read_ray_hdr = true;
                    beg_rec      = true;

                    continue;
                }
            }

            len = cur_len;

            // ---Define output data files for each data_type (= nparams)/sweep ---------
            dty = data_type[0];

            if (nparams > 1) {
                kk  = rays_count % nparams;
                col = rays_count / nparams;
                dty = data_type[kk];
            } else if (number_sweeps > 1) {
                kk  = nsweep - 1;
                col = rays_count % irays;
            }

            String var_name = data_name[dty];

            // --- read ray_header (size=12 bytes=6 words)---------------------------------------
            if (read_ray_hdr) {
                if (pos_ray_hdr < 2) {
                    raf.seek(cur_len);
                    beg_az  = raf.readShort();
                    cur_len = cur_len + 2;
                    len     = cur_len;

                    if (cur_len % REC_SIZE == 0) {
                        pos_ray_hdr  = 2;
                        beg_rec      = true;
                        read_ray_hdr = true;

                        continue;
                    }
                }
                if (pos_ray_hdr < 4) {
                    raf.seek(cur_len);
                    beg_elev = raf.readShort();
                    cur_len  = cur_len + 2;
                    len      = cur_len;

                    if (cur_len % REC_SIZE == 0) {
                        pos_ray_hdr  = 4;
                        beg_rec      = true;
                        read_ray_hdr = true;
                        continue;
                    }
                }
                if (pos_ray_hdr < 6) {
                    raf.seek(cur_len);
                    end_az  = raf.readShort();
                    cur_len = cur_len + 2;
                    len     = cur_len;

                    if (cur_len % REC_SIZE == 0) {
                        pos_ray_hdr  = 6;
                        beg_rec      = true;
                        read_ray_hdr = true;
                        continue;
                    }
                }
                if (pos_ray_hdr < 8) {
                    raf.seek(cur_len);
                    end_elev = raf.readShort();
                    cur_len  = cur_len + 2;
                    len      = cur_len;

                    if (cur_len % REC_SIZE == 0) {
                        pos_ray_hdr  = 8;
                        beg_rec      = true;
                        read_ray_hdr = true;

                        continue;
                    }
                }

                if (pos_ray_hdr < 10) {
                    raf.seek(cur_len);
                    num_bins = raf.readShort();
                    cur_len  = cur_len + 2;
                    len      = cur_len;

                    if (num_bins % 2 != 0) {
                        num_bins = (short) (num_bins + 1);
                    }
                    num_gates[nsweep-1]=(int)num_bins;
                    if (cur_len % REC_SIZE == 0) {
                        pos_ray_hdr  = 10;
                        beg_rec      = true;
                        read_ray_hdr = true;

                        continue;
                    }
                }

                if (pos_ray_hdr < 12) {
                    raf.seek(cur_len);
                    time_start_sw = raf.readShort();
                    cur_len       = cur_len + 2;
                    len           = cur_len;
                }
            }

            // ---------- end of ray header ----------------------------------------------
            az   = SigmetIOServiceProvider.calcAz(beg_az, end_az);
            elev = SigmetIOServiceProvider.calcElev(end_elev);
            step = SigmetIOServiceProvider.calcStep(range_first, range_last, num_bins);

            if (cur_len % REC_SIZE == 0) {
                len          = cur_len;
                beg_rec      = true;
                read_ray_hdr = false;

                continue;
            }

            if (pos > 0) {
                data_read = data_read - pos;
                pos       = 0;
            }
          //  if(az > 358.5 && az < 358.8 && var_name.contains("Reflectivity")) {
          //      System.out.println(" here ");
          //  }
            if (data_read > 0) {
                raf.seek(cur_len);
                rayoffset = cur_len;
                datalen   = data_read;

                for (int i = 0; i < data_read; i++) {
                  //  data   = raf.readByte();
                  //  dd[nb] = SigmetIOServiceProvider.calcData(recHdr, dty, data);
                    cur_len++;
                    nb++;

                    if (cur_len % REC_SIZE == 0) {
                        pos          = i + 1;
                        beg_rec      = true;
                        read_ray_hdr = false;
                        len          = cur_len;
                        raf.seek(cur_len);
                        break;
                    }
                }
                raf.seek(cur_len);
                if (pos > 0) {
                    continue;
                }
            }

            if (cur_len % REC_SIZE == 0) {
                pos          = 0;
                beg_rec      = true;
                read_ray_hdr = false;
                data_read    = 0;
                len          = cur_len;

                continue;
            }

            raf.seek(cur_len);
            rayoffset1 = cur_len;

            while (nb < (int) num_bins) {
                a00     = raf.readShort();
                cur_len = cur_len + 2;

                // --- Check if the code=1 ("1" means an end of a ray)
                if (a00 == (short) 1) {
                   // for (int uk = 0; uk < (int) num_bins; uk++) {
                      //  dd[uk] = -999.99f;
                  //  }
                    ray       = new Ray(-999.99f, -999.99f, -999.99f, -999.99f, num_bins, (short) (-99), -999, 0, -999,
                                        nsweep, var_name, dty);
                    rays_count++;
                    beg_rec = false;
                    end_rec = true;

                    break;
                }

                if (a00 < 0) {    // -- This is data
                    nwords    = a00 & 0x7fff;
                    data_read = nwords * 2;

                    if (cur_len % REC_SIZE == 0) {
                        pos          = 0;
                        beg_rec      = true;
                        end_rec      = false;
                        len          = cur_len;
                        read_ray_hdr = false;

                        break;
                    }

                    raf.seek(cur_len);

                    for (int ii = 0; ii < data_read; ii++) {
                     //   data    = raf.readByte();
                    //    dd[nb]  = SigmetIOServiceProvider.calcData(recHdr, dty, data);
                        cur_len = cur_len + 1;
                        nb      = nb + 1;

                        if (cur_len % REC_SIZE == 0) {
                            pos          = ii + 1;
                            beg_rec      = true;
                            end_rec      = false;
                            len          = cur_len;
                            read_ray_hdr = false;
                            raf.seek(cur_len);
                            break;
                        }
                    }
                    raf.seek(cur_len);
                    if (pos > 0) {
                        break;
                    }
                } else if (a00 > 0 & a00 != 1) {
                    num_zero = a00 * 2;

                   // for (int k = 0; k < num_zero; k++) {
                     //   dd[nb + k] = SigmetIOServiceProvider.calcData(recHdr, dty, (byte) 0);
                   // }

                    nb = nb + num_zero;

                    if (cur_len % REC_SIZE == 0) {
                        beg_rec      = true;
                        end_rec      = false;
                        read_ray_hdr = false;
                        pos          = 0;
                        data_read    = 0;
                        len          = cur_len;

                        break;
                    }
                }
            }                     // ------ end of while for num_bins---------------------------------

            if (cur_len % REC_SIZE == 0) {
                len = cur_len;

                continue;
            }

            raf.seek(cur_len);

            if (nb == (int) num_bins) {
                a00     = raf.readShort();
                cur_len = cur_len + 2;
                end_rec = true;
                ray     = new Ray(range_first, step, az, elev, num_bins, time_start_sw, rayoffset, datalen, rayoffset1,
                                   nsweep, var_name, dty);
                rays_count++;
                two++;
                if ((nsweep == number_sweeps) & (rays_count % beg == 0)) {
                    if (var_name.trim().equalsIgnoreCase("TotalPower")) {
                        totalPower.add(ray);
                    } else if (var_name.trim().equalsIgnoreCase("Reflectivity")) {
                        reflectivity.add(ray);
                    } else if (var_name.trim().equalsIgnoreCase("Velocity")) {
                        velocity.add(ray);
                    } else if (var_name.trim().equalsIgnoreCase("Width")) {
                        width.add(ray);
                    } else if (var_name.trim().equalsIgnoreCase("DifferentialReflectivity")) {
                        diffReflectivity.add(ray);
                    } else {
                        System.out.println(" Error: Unknown Radial Variable found!!");
                    }
                    break;
                }

                if (cur_len % REC_SIZE == 0) {
                    beg_rec      = true;
                    end_rec      = true;
                    read_ray_hdr = true;
                    pos          = 0;
                    data_read    = 0;
                    nb           = 0;
                    len          = cur_len;
                    if (var_name.trim().equalsIgnoreCase("TotalPower")) {
                        totalPower.add(ray);
                    } else if (var_name.trim().equalsIgnoreCase("Reflectivity")) {
                        reflectivity.add(ray);
                    } else if (var_name.trim().equalsIgnoreCase("Velocity")) {
                        velocity.add(ray);
                    } else if (var_name.trim().equalsIgnoreCase("Width")) {
                        width.add(ray);
                    } else if (var_name.trim().equalsIgnoreCase("DifferentialReflectivity")) {
                        diffReflectivity.add(ray);
                    } else {
                        System.out.println(" Error: Unknown Radial Variable found!!");
                    }
                    continue;
                }
            }

            // "TotalPower", "Reflectivity", "Velocity", "Width", "DifferentialReflectivity"
            if(firstRay == null) firstRay = ray;

            if (var_name.trim().equalsIgnoreCase("TotalPower")) {
                totalPower.add(ray);
            } else if (var_name.trim().equalsIgnoreCase("Reflectivity")) {
                reflectivity.add(ray);
            } else if (var_name.trim().equalsIgnoreCase("Velocity")) {
                velocity.add(ray);
            } else if (var_name.trim().equalsIgnoreCase("Width")) {
                width.add(ray);
            } else if (var_name.trim().equalsIgnoreCase("DifferentialReflectivity")) {
                diffReflectivity.add(ray);
            } else {
                System.out.println(" Error: Unknown Radial Variable found!!");
            }

            pos          = 0;
            data_read    = 0;
            nb           = 0;
            read_ray_hdr = true;
            pos_ray_hdr  = 0;

            if ((nsweep <= number_sweeps) & (rays_count % beg == 0)) {
                beg_rec      = true;
                end_rec      = true;
                rays_count   = 0;
                nb           = 0;
                cur_len      = REC_SIZE * (nrec + 1);
                len          = cur_len;
                read_ray_hdr = true;
            }

            len = cur_len;
        }    // ------------end of outer while  ---------------
        lastRay = ray;
        
        if (reflectivity.size() > 0) {
            reflectivityGroups = sortScans("reflectivity", reflectivity, 1000);
            hasReflectivity = true;
        }
        if (velocity.size() > 0) {
            velocityGroups = sortScans("velocity", velocity, 1000);
            hasVelocity = true;
        }
        if (totalPower.size() > 0) {
            totalPowerGroups = sortScans("totalPower", totalPower, 1000);
            hasTotalPower = true;
        }
        if (width.size() > 0)  {
            widthGroups = sortScans("width", width, 1000);
            hasWidth = true;
        }
        if (diffReflectivity.size() > 0) {
            differentialReflectivityGroups = sortScans("diffReflectivity", diffReflectivity, 1000);
            hasDifferentialReflectivity = true;
        }
        if (time.size() > 0) {
            timeGroups = sortScans("diffReflectivity", diffReflectivity, 1000);
            hasTime = true;
        }

        // --------- fill all of values in the ncfile ------
    }    // ----------- end of doData -----------------------
    private int max_radials = 0;
    private int min_radials = Integer.MAX_VALUE;
    private boolean debugRadials = false;


    private ArrayList sortScans(String name, List<Ray> scans, int siz) {

        // now group by elevation_num
        Map<Short, List<Ray>> groupHash = new HashMap<Short, List<Ray>>(siz);

        for (Ray ray : scans) {
            List<Ray> group = groupHash.get((short)ray.nsweep);

            if (null == group) {
                group = new ArrayList<Ray>();
                groupHash.put((short) ray.nsweep, group);
            }

            group.add(ray);
        }
        Iterator itt = groupHash.keySet().iterator();
        ArrayList groups0 = new ArrayList();
        while(itt.hasNext()){
            List<Ray> group  = (List)groupHash.get(itt.next());
            Ray [] rr = new Ray[group.size()];
            group.toArray(rr);
            checkSort(rr);
        }
        // sort the groups by elevation_num
        ArrayList groups = new ArrayList(groupHash.values());

        Collections.sort(groups, new GroupComparator());

        // use the maximum radials
        for (int i = 0; i < groups.size(); i++) {
            ArrayList    group = (ArrayList) groups.get(i);
            Ray r     = (Ray) group.get(0);

            max_radials = Math.max(max_radials, group.size());
            min_radials = Math.min(min_radials, group.size());
        }

        if (debugRadials) {
            System.out.println(name + " min_radials= " + min_radials + " max_radials= " + max_radials);

            for (int i = 0; i < groups.size(); i++) {
                ArrayList    group = (ArrayList) groups.get(i);
                Ray lastr = (Ray) group.get(0);

                for (int j = 1; j < group.size(); j++) {
                    Ray r = (Ray) group.get(j);

                    if (r.getTime() < lastr.getTime()) {
                        System.out.println(" out of order " + j);
                    }

                    lastr = r;
                }
            }
        }



        return groups;
    }

    private class GroupComparator implements Comparator<List<Ray>> {
        public int compare(List<Ray> group1, List<Ray> group2) {
            Ray record1 = group1.get(0);
            Ray record2 = group2.get(0);

            // if (record1.elevation_num != record2.elevation_num)
            return record1.nsweep - record2.nsweep;

            // return record1.cut - record2.cut;
        }
    }

    public List getTotalPowerGroups() {
      return totalPowerGroups;
    }

    public List getVelocityGroups() {
      return velocityGroups;
    }

    public List getWidthGroups() {
      return widthGroups;
    }
    public List getReflectivityGroups() {
      return reflectivityGroups;
    }

    public List getDifferentialReflectivityGroups() {
      return differentialReflectivityGroups;
    }
    public int[] getNumberGates() {
        return num_gates;
    }
    public int[] getStartSweep(){
        return base_time;
    }

    /** Sort Ray objects in the same sweep according to the ascended azimuth (from 0 to 360)
     *  and time.
     * @param r  the array of Ray objects in a sweep. Its length=number_rays
     */
    void checkSort(Ray[] r) {
        int j=0, n=0, n1=0, n2=0;
        short time1=0, time2=0;
        int[] k1=new int[300];
        int[] k2=new int[300];
        //      define the groups of rays with the same "time". For ex.:
        //      group1 - ray[0]={time=1,az=344}, ray[1]={time=1,az=345}, ... ray[11]={time=1,az=359}
        //      group2 - ray[12]={time=1,az=0}, ray[13]={time=1,az=1}, ... ray[15]={time=1,az=5}
        //      k1- array of begin indx (0,12), k2- array of end indx (11,15)
        for (int i=0; i< r.length-1; i++) {
            time1=r[i].getTime();  time2=r[i+1].getTime();
            if (time1 != time2) {
                k2[j]=i;
                j=j+1; k1[j]=i+1;
            }
        }
        if (k2[j]< r.length-1) { k1[j]=k2[j-1]+1; k2[j]=r.length-1; n=j+1; }

        //      if different groups have the same value of "time" (may be 2 and more groups) -
        //      it1= indx of "k1" of 1st group, it2= indx of "k2" of last group
        int it1=0, it2=0;
        for (int ii=0; ii<j+1; ii++) {
            n1=k1[ii];
            for (int i=0; i<j+1; i++) {
                if (i != ii) {
                    n2=k1[i];
                    if (r[n1].getTime() == r[n2].getTime()) { it1=ii; it2=i;}
                }
            }
        }

        n1=k1[it1];  n2=k1[it2];
        int s1=k2[it1]-k1[it1]+1;  int s2=k2[it2]-k1[it2]+1;
        float[] t0=new float[s1];  float[] t00=new float[s2];
        for (int i=0; i<s1; i++) { t0[i]=r[n1+i].getAz();   }
        for (int i=0; i<s2; i++) { t00[i]=r[n2+i].getAz();  }
        float mx0=t0[0];
        for (int i=0; i<s1; i++) { if(mx0<t0[i]) mx0=t0[i]; }
        float mx00=t00[0];
        for (int i=0; i<s2; i++) { if(mx00<t00[i]) mx00=t00[i]; }
        if ((mx0>330.0f & mx00<50.0f)) {
            for (int i=0; i<s1; i++) {
                float q=r[n1+i].getAz();
                r[n1+i].setAz(q-360.0f);
            }
        }
        Arrays.sort(r, new RayComparator());
        for (int i=0; i<r.length; i++) { float a=r[i].getAz();
        if (a < 0 & a > -361.0f) { float qa=r[i].getAz(); r[i].setAz(qa+360.0f); }
        }

    }
    class RayComparator implements Comparator<Ray> {
           public int compare(Ray ray1, Ray ray2) {
               if (ray1.getTime() < ray2.getTime()) { return -1; }
               else if (ray1.getTime() == ray2.getTime()) {
                   if (ray1.getAz() < ray2.getAz()) { return -1; }
                   if (ray1.getAz() > ray2.getAz()) { return 1; }
                   if (ray1.getAz() == ray2.getAz()) { return 0; }
               } else if (ray1.getTime() > ray2.getTime()) { return 1; }
               return 0;
           }
       } // class RayComparator end ----------------------------------


}

