package ucar.nc2.iosp.bufr;

import ucar.bufr.Index;
import ucar.bufr.BufrDataExtractor;
import ucar.bufr.BufrData;
//import ucar.bufr.BufrIndexExtender;
import ucar.ma2.*;

import ucar.nc2.*;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.DiskCache;

import ucar.unidata.io.RandomAccessFile;

import java.io.*;
import java.util.*;
import java.net.URL;

/**
 * BufrIosp
 */
public class BufrIosp extends AbstractIOServiceProvider {

  protected NetcdfFile ncfile;
  protected RandomAccessFile raf;
  protected StringBuffer parseInfo = new StringBuffer();

  // keep this info to reopen index when extending or syncing
  private Index saveIndex = null;    // the Bufr record index
  private File saveIndexFile = null; // the index file
  private String saveLocation;
  private BufrDataExtractor dataReader;

  // debugging
  static boolean debugOpen = false, debugMissing = false, debugMissingDetails = false, debugProj = false, debugTiming = false, debugVert = false;

  static public boolean forceNewIndex = false; // force that a new index file is written
  static public boolean useMaximalCoordSys = false;
  static public boolean extendIndex = false; // check if index needs to be extended

  static public void useMaximalCoordSys(boolean b) { useMaximalCoordSys = b; }
  static public void setExtendIndex(boolean b) { extendIndex = b; }

  static public void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag) {
    debugOpen = debugFlag.isSet("Bufr/open");
    debugMissing = debugFlag.isSet("Bufr/missing");
    debugMissingDetails = debugFlag.isSet("Bufr/missingDetails");
    debugProj = debugFlag.isSet("Bufr/projection");
    debugVert = debugFlag.isSet("Bufr/vertical");
    debugTiming = debugFlag.isSet("Bufr/timing");
  }

  public boolean isValidFile( ucar.unidata.io.RandomAccessFile raf )
  {
     try {
        raf.order( RandomAccessFile.BIG_ENDIAN );
        raf.seek( 0 );
        // Create BufrInput instance
        ucar.bufr.BufrInput bi = new ucar.bufr.BufrInput( raf );
        return bi.isValidFile();
     } catch (IOException ex) {
         ex.printStackTrace();
         return false;
     }
  }

  protected void open(Index index, CancelTask cancelTask) throws IOException {

    long startTime = System.currentTimeMillis();

    // make it into netcdf objects
    Index2NC delegate = new Index2NC();
    delegate.open( index, ncfile, cancelTask);

    ncfile.finish();

    HashMap atts = (HashMap)index.getGlobalAttributes();
    //System.out.println( "table ="+ (String)atts.get( "table" ) );
    dataReader = new BufrDataExtractor( raf, (String)atts.get( "table" ) );

    if (debugTiming) {
      long took = System.currentTimeMillis() - startTime;
      System.out.println(" open "+ncfile.getLocation()+" took="+took+" msec ");
    }
  }

  public void open(RandomAccessFile raf, NetcdfFile ncfile,
    CancelTask cancelTask) throws IOException {
    this.raf = raf;
    this.ncfile = ncfile;

    Index index = getIndex(raf.getLocation(), cancelTask);
    open(index, cancelTask);
  }

  /**
   * Open the index file. If not exists, create it.
   * When writing use DiskCache, to make sure location is writeable.
   *
   * @param location location of the file. The index file has ".bfx" appended.
   * @param cancelTask user may cancel
   * @return ucar.bufr.Index
   * @throws IOException
   */
  protected Index getIndex(String location, CancelTask cancelTask)
    throws IOException {
    // get an Index
    saveLocation = location;
    String indexLocation = location + ".bfx";

    if (indexLocation.startsWith("http:")) { // LOOK direct access through http
      InputStream ios = indexExistsAsURL(indexLocation);
      if (ios != null) {
        saveIndex = new Index();
        saveIndex.open(indexLocation, ios);
        if (debugOpen) System.out.println("  opened HTTP index = "+indexLocation);
        return saveIndex;

      } else { // otherwise write it to / get it from the cache
        saveIndexFile = DiskCache.getCacheFile(indexLocation);
        if (debugOpen) System.out.println("  HTTP index = "+saveIndexFile.getPath());
      }

    } else {
      // get the index file, look in cache if need be
      saveIndexFile = DiskCache.getFileStandardPolicy( indexLocation );
    }

    // if exist already, read it
    if (!forceNewIndex && saveIndexFile.exists()) {
      saveIndex = new Index();
      boolean ok = saveIndex.open(saveIndexFile.getPath());

      if (ok) {
        if (debugOpen) System.out.println("  opened index = "+saveIndexFile.getPath());

        // deal with possiblity that the bufr file has grown, and the index should be extended.
        // only do this if index extension is allowed.
        if (extendIndex) {
          HashMap attrHash = saveIndex.getGlobalAttributes();
          String lengthS = (String) attrHash.get( "length" );
          long length = (lengthS == null) ? 0 : Long.parseLong( lengthS );
          if( length < raf.length() ) {
             if (debugOpen) System.out.println("  calling extendIndex" );
             saveIndex = extendIndex( raf, saveIndexFile, saveIndex );
           }
        }

      } else {  // rewrite if fail to open
        saveIndex = writeIndex( saveIndexFile, raf);
        if (debugOpen) System.out.println("  rewrite index = "+saveIndexFile.getPath());
      }

    } else {
      // doesnt exist (or is being forced), create it and write it
      saveIndex = writeIndex( saveIndexFile, raf);
      if (debugOpen) System.out.println("  write index = "+saveIndexFile.getPath());
    }
    return saveIndex;
  }

  private Index writeIndex(File indexFile, RandomAccessFile raf)
    throws IOException {
    Index index = null;
    raf.seek(0);

    ucar.bufr.BufrIndexer indexer = new ucar.bufr.BufrIndexer();
    PrintStream ps = new PrintStream(
      new BufferedOutputStream(new FileOutputStream( indexFile)));
    index = indexer.writeFileIndex(raf, ps, true);

    return index;
  }

  public boolean sync() throws IOException {
    HashMap attrHash = saveIndex.getGlobalAttributes();
    String lengthS = (String) attrHash.get( "length" );
    long length = (lengthS == null) ? 0 : Long.parseLong( lengthS );

    if( length < raf.length() ) {

      // case 1 is where we look to see if the index has grown. This can be turned off if needed to deal with multithreading
      // conflicts (eg TDS). We should get File locking working to deal with this instead.
      if (extendIndex && (saveIndexFile != null)) {
        if (debugOpen) System.out.println("calling IndexExtender" );
        saveIndex = extendIndex( raf, saveIndexFile, saveIndex );

      // case 2 just reopen the index again
      } else {
        if (debugOpen) System.out.println("sync reopen Index" );
        saveIndex = getIndex(saveLocation, null);
      }

      // reconstruct the ncfile objects
      ncfile.empty();
      open(saveIndex, null);
      return true;
    }

    return false;
  }

  /*
   * takes a bufr data file, a .bfx index file and a index, reads the current
   * .bfx index, reads the data file starting at old eof for new
   * data, updates the *.bfx file and the index
   *
   */
  private Index extendIndex(RandomAccessFile raf, File indexFile, Index index)
    throws IOException {

  /*
    BufrIndexExtender indexExt = new BufrIndexExtender();
    index = indexExt.extendIndex(raf, indexFile, index);

    return index;
   */
    return null;
  }

  // if exists, return input stream, otherwise null
  private InputStream indexExistsAsURL(String indexLocation) {
    try {
      URL url = new URL(indexLocation);
      return url.openStream();
    }  catch (IOException e) {
      return null;
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////
  public Array readData(Variable v2, Section section)
    throws IOException, InvalidRangeException {
    long start = System.currentTimeMillis();

    // check if v2 is a Structure
    //System.out.println( "v2 ="+ v2.getName() );
    if( v2 instanceof Structure ) {
       //System.out.println( "v2 is a Structure" );
       // get the index data
       HashMap atts = saveIndex.getGlobalAttributes();
       ArrayList locations = saveIndex.getLocations();
       //System.out.println( "locations ="+ locations );
       HashMap observations = saveIndex.getObservations();
       //ArrayList times = saveIndex.getObsTimes();
       //HashMap timeLocations = saveIndex.getObsLocations();
       //ArrayList parameters = saveIndex.getParameters();

       int[] shape = section.getShape();
       Range range = section.getRange(0);
       //System.out.println( "range.first ="+ range.first() +" range.last ="+ range.last() );
       //System.out.println( "shape[0]  ="+ shape[0] );

       // allocate ArrayStructureMA for outer structure
       StructureMembers members = ((Structure)v2).makeStructureMembers();
       ArrayStructureMA ama = new ArrayStructureMA(members, shape);
       ArrayList vars = (ArrayList)((Structure)v2).getVariables();
       //System.out.println( "vars ="+ vars );
       HashMap dataHash = new HashMap();
       HashMap iiHash = new HashMap();

       // inner structure variables
       ArraySequence ias = null;
       StructureMembers imembers = null;
       ArrayList ivars = null;
       HashMap sdataHash = null;
       HashMap siiHash = null;
       for( int k = 0; k < vars.size(); k++ ) {
          Variable v = (Variable) vars.get( k );
          // inner structure
          if( v instanceof Structure ) {
             imembers = ((Structure)v).makeStructureMembers();
             ias = new ArraySequence( imembers, shape[ 0 ] );
             ivars = (ArrayList)((Structure)v).getVariables();
             sdataHash = new HashMap();
             siiHash = new HashMap();
             int currOb = -1;
             int idx = 0;
             for( int i = 0; i < locations.size(); i++ ) {
                if( currOb > range.last() ) { // done
                   break;
                }
                String loc = (String)locations.get( i );
                //System.out.println( "loc =" + loc );
                ArrayList obs = (ArrayList) observations.get( loc );
                ucar.bufr.Index.BufrObs bo;
                for( int j = 0; j < obs.size(); j++ ) {
                   currOb++;
                   if( currOb < range.first() || currOb > range.last() ) {
                      continue;
                   }
                   bo = (ucar.bufr.Index.BufrObs) obs.get( j );
                   //System.out.println( currOb +" "+ bo.name +" "+ bo.dim );
                   //for (int seq=0; seq < outerLength; seq++)
                   //   aseq.setSequenceLength(seq, seqLength);
                   ias.setSequenceLength( idx++, bo.dim );
                }
             }
             ias.finish();

/*           sudo code
             for (int j=0; j<members.size(); j++) {
                Member m = members.get(j);
                Array data = (Array) m.getDataObject();
                Iterator iter = data.getIterator();
                Object data = extract(m); // really a float[], long[], String[]
                aseq.setDataArray(seq, m, data);
             }
*/
             ArrayList im = (ArrayList)imembers.getMembers();
             for( int j = 0; j < im.size(); j++ ) {
                StructureMembers.Member m = (StructureMembers.Member)im.get( j );
                Array data = (Array) m.getDataArray();
                IndexIterator ii = data.getIndexIterator();
                //System.out.println( "IndexIterator ii ="+ ii );
                sdataHash.put( m.getName(), data );
                siiHash.put( m.getName(), ii );
                //System.out.println( "m.getName() ="+ m.getName() ); 
                //ii = (IndexIterator) siiHash.get( m.getName() );
                //System.out.println( "IndexIterator ii ="+ ii );
             }    
          } // end inner Structure

          // outer structure variables
          Array data = null;
          //Array data = Array.factory(v.getDataType().getPrimitiveClassType(), Range.getShape( section ));
          // there should be a better way of allocating storage
          int[] vshape = v.getShape();
          if( vshape.length != 0 && vshape[0] != -1 ) {
             //System.out.println( "v.sshape ="+ vshape[0] +" rank ="+ vshape.length );
             vshape[0] *= shape[0];
             data = Array.factory(v.getDataType().getPrimitiveClassType(), vshape );
          } else {
             data = Array.factory(v.getDataType().getPrimitiveClassType(), shape );
          }
          //System.out.println( "data.getSize ="+ data.getSize() +" rank ="+ data.getRank() );
          IndexIterator ii = data.getIndexIterator();
          dataHash.put( v.getShortName(), data );
          iiHash.put( v.getShortName(), ii );
       }
       // compressed data is organized by field, not sequential obs
       if( ((String)atts.get( "compressdata")).equals( "true") ) {
          System.out.println( "compressed data");
          readDataCompressed( (String)atts.get( "table"), locations, range, 
             observations, vars, iiHash, dataHash, members );
          return ama;
       }

       // start of data reads for sequential obs
       BufrDataExtractor bde = 
          new BufrDataExtractor( raf, (String)atts.get( "table") );
       int currOb = -1; // current ob being processed
       Variable v = null;
       for( int i = 0; i < locations.size(); i++ ) {
          if( currOb > range.last() ) { // done
             break;
          }
          String loc = (String)locations.get( i );
          //System.out.println( "loc =" + loc );
          ArrayList obs = (ArrayList) observations.get( loc );
          ucar.bufr.Index.BufrObs bo;
          for( int j = 0; j < obs.size(); j++ ) {
             currOb++;
             if( currOb < range.first() || currOb > range.last() ) {
                continue;
             }
             bo = (ucar.bufr.Index.BufrObs) obs.get( j );
             //System.out.println( bo.name +" "+ bo.DDSoffset +" "+ bo.obsOffset +" "+ bo.bitPos +" "+ bo.bitBuf );
             if( bde.getData( bo.DDSoffset, bo.obsOffset, bo.bitPos, bo.bitBuf ) ) {
                HashMap bufrdatas = (HashMap) bde.getBufrDatas();
                String bKey = null;
                BufrData bd;
                IndexIterator ii;
                for( int k = 0; k < vars.size(); k++ ) {
                   v = (Variable) vars.get( k );
                   // inner structure
                   if( v instanceof Structure ) {
                      //System.out.println( "v is a Structure name ="+ v.getShortName() );
                      for( int m = 0; m < ivars.size(); m++ ) {
                         v = (Variable) ivars.get( m );
                         Attribute a = v.findAttribute( "Bufr_key");
                         if( a == null )
                            continue;
                         bKey = a.getStringValue();
                         bd = (BufrData) bufrdatas.get( bKey );
                         if ( bd.getName().equals( "time_nominal" ) || bd.getName().equals( "time_observation" )) {
                           ii = (IndexIterator)siiHash.get( v.getShortName() );
                           ii.setLongNext( (bd.getLongData())[0] );
                        } else if( bd.isNumeric() ) {
                           //System.out.println( "v.getShortName() ="+ v.getShortName() );
                           ii = (IndexIterator)siiHash.get( v.getShortName() );
                           //System.out.println( "IndexIterator ii ="+ ii );
                           float[] bufrdata = bd.getFloatData();
                           for( int n = 0; n < bufrdata.length; n++ ) {
                              ii.setFloatNext( bufrdata[n] );
                              //System.out.print( bufrdata[n] + ", " );
                           }
                           //System.out.println();

                       } else { // String data
                         ii = (IndexIterator)siiHash.get( v.getShortName() );
                         ii.setObjectNext( (bd.getStringData())[0] );
                      }
                    }
                    continue;
///////////////////// end inner structure

                   } else if( v.getShortName().equals( "parent_index")) {
                      ii = (IndexIterator)iiHash.get( v.getShortName() );
                      ii.setIntNext( i );
                      continue;
                   } else {
                      Attribute a = v.findAttribute( "Bufr_key");
                      if( a == null )
                         continue;
                      bKey = a.getStringValue();
                   }
                   bd = (BufrData) bufrdatas.get( bKey );
                   if ( bd.getName().equals( "time_nominal" ) || bd.getName().equals( "time_observation" )) {
                      ii = (IndexIterator)iiHash.get( v.getShortName() );
                      ii.setLongNext( (bd.getLongData())[0] );
                   } else if( bd.isNumeric() ) {
                      ii = (IndexIterator)iiHash.get( v.getShortName() );
                      float[] bufrdata = bd.getFloatData();
                      for( int n = 0; n < bufrdata.length; n++ ) {
                         ii.setFloatNext( bufrdata[n] );
                      }

                    } else { // String data
                      ii = (IndexIterator)iiHash.get( v.getShortName() );
                      ii.setObjectNext( (bd.getStringData())[0] );
                   }
                }
             } else {
                System.out.println( "getData failed for loc ="+ loc );
             }
          }
       } // end looping over locations

       // enter data into members
       for( int k = 0; k < vars.size(); k++ ) {
           v = (Variable) vars.get( k );
           Array data = (Array)dataHash.get( v.getShortName() );
           StructureMembers.Member m = members.findMember( v.getShortName());
           if( v instanceof Structure ) {
              m.setDataArray( ias );
              // dump ias as a check
              //m = (StructureMembers.Member) imembers.findMember( "Hgt_above_station" );
              // data = (Array) m.getDataObject();
              //IndexIterator ii = data.getIndexIterator();
              //System.out.println( "Hgt_above_station" );
              //for( ; ii.hasNext(); ) {
              //   System.out.print( ii.getFloatNext()  +", " );
              //}
           } else {
              m.setDataArray( data );
           }
       }
       return ama;
    }
    // non-structure variable read
    //System.out.println( "non-structure variable read name ="+ v2.getName() );
    return readDataVariable( v2.getName(),  v2.getDataType(), section.getRanges() );
  }

  public void readDataCompressed( String table, ArrayList locations,
  Range range, HashMap observations, ArrayList vars, HashMap iiHash, 
  HashMap dataHash, StructureMembers members )
    throws IOException, InvalidRangeException {
       // start of data reads
       BufrDataExtractor bde = new BufrDataExtractor( raf, table );
       int currOb = -1; // current ob being processed
       Variable v = null;
       int numObs = -1; // number of obs in this data block
       for( int i = 0; i < locations.size(); i++ ) {
          if( currOb > range.last() ) { // done
             break;
          }
          String loc = (String)locations.get( i );
          //System.out.println( "loc =" + loc );
          ArrayList obs = (ArrayList) observations.get( loc );
          ucar.bufr.Index.BufrObs bo;
          int end = -1;
          for( int j = 0; j < obs.size(); j++ ) {
             if( currOb > range.last() ) { // done
                break;
             }
             bo = (ucar.bufr.Index.BufrObs) obs.get( j );
             //System.out.println( bo.name +" "+ bo.DDSoffset +" "+ bo.obsOffset +" "+ bo.bitPos +" "+ bo.bitBuf );
             // for compressed data bo.bitBuf contains number of obs in data
             if( (currOb + bo.bitBuf) > range.last() ) {
                 end = range.last() - currOb;
                 //System.out.println( "end ="+ end );
             } else {
                 end = -1;
             }
             currOb += bo.bitBuf; // bo.bitBuf contains number of obs in data
             //System.out.println( "currOb ="+ currOb );
             if( currOb < range.first() ) {
                continue;
             }
             numObs = bo.bitBuf;
             // compressed data packs a number of Obs into one data block
             // bo.bitBuf is always 0 for compressed data
             if( bde.getData( bo.DDSoffset, bo.obsOffset, bo.bitPos, 0 ) ) {
                HashMap bufrdatas = (HashMap) bde.getBufrDatas();
                String bKey = null;
                BufrData bd;
                IndexIterator ii;
                for( int k = 0; k < vars.size(); k++ ) {
                   v = (Variable) vars.get( k );
                   if( v.getShortName().equals( "parent_index")) {
                      ii = (IndexIterator)iiHash.get( v.getShortName() );
                      ii.setIntNext( i );
                      continue;
                   } else {
                      Attribute a = v.findAttribute( "Bufr_key");
                      if( a == null )
                         continue;
                      bKey = a.getStringValue();
                   }
                   bd = (BufrData) bufrdatas.get( bKey );
                   if ( bd.getName().equals( "time_nominal" ) || bd.getName().equals( "time_observation" )) {
                      ii = (IndexIterator)iiHash.get( v.getShortName() );
                      long[] bufrdata = bd.getLongData();
                      for( int n = 0; n < bufrdata.length && n != end; n++ ) {
                         ii.setLongNext( bufrdata[n] );
                         //System.out.println( "time_observation ="+ bufrdata[n] );
                      }
                   } else if( bd.isNumeric() ) {
                      ii = (IndexIterator)iiHash.get( v.getShortName() );
                      float[] bufrdata = bd.getFloatData();
                      //System.out.println( "bufrdata.length ="+ bufrdata.length
                      //+" = "+ numObs );
                      for( int m = 0; m < numObs && m != end; m++ ) {
                         for( int n = m; n < bufrdata.length; n += numObs ) {
                            ii.setFloatNext( bufrdata[n] );
                         }
                      }
                    } else { // String data
                      ii = (IndexIterator)iiHash.get( v.getShortName() );
                      String[] bufrdata = bd.getStringData();
                      for( int n = 0; n < bufrdata.length && n != end; n++ ) {
                         ii.setObjectNext( bufrdata[n] );
                         //System.out.println( "String ="+ bufrdata[n] );
                      }
                   }
                }
             } else {
                System.out.println( "getData failed for loc ="+ loc );
             }
          }
       }
       // enter data in members
       for( int k = 0; k < vars.size(); k++ ) {
           v = (Variable) vars.get( k );
           Array data = (Array)dataHash.get( v.getShortName() );
           StructureMembers.Member m = members.findMember( v.getShortName());
           m.setDataArray( data );
       }
  } // end readDataCompressed

  private Array readDataVariable(String name,  DataType datatype,
      java.util.List section) throws IOException, InvalidRangeException  {

    //System.out.println( "readDataVariable name ="+ name );
    ucar.bufr.Index.parameter p = null;
    int[] shape = Range.getShape( section );
    ArrayList locations = saveIndex.getLocations();
    HashMap observations = saveIndex.getObservations();
    ArrayList times = saveIndex.getObsTimes();
    //HashMap timeLocations = saveIndex.getObsLocations();
    //ArrayList parameters = saveIndex.getParameters();

    if( name.equals( "number_stations" ) || name.equals( "number_trajectories" )) {
       int[] number = new int[ 1 ];
       number[ 0 ] = locations.size();
       return Array.factory( datatype.getPrimitiveClassType(), shape, number );
    }

    if( name.equals( "station_id" ) || name.equals( "trajectory_id" ) ) {
       String[] ids = new String[ locations.size() ];
       for( int i = 0; i < locations.size(); i++ ) {
          ids[ i ] = (String)locations.get( i );
       }
       return Array.factory( datatype.getPrimitiveClassType(), shape, ids );
    }

    if( name.equals( "firstChild" ) ) {
       int pos = 0;
       int[] fc = new int[ locations.size() ];
       fc[ 0 ] = 0;
       for( int i = 0; i < locations.size(); i++ ) {
          String loc = (String)locations.get( i );
          if( i == 0 ) {
             fc[ i ] = 0;
             pos = ((ArrayList)observations.get( loc )).size();
          } else {
             fc[ i ] = pos;
             pos += ((ArrayList)observations.get( loc )).size();
          }
       }
       return Array.factory( datatype.getPrimitiveClassType(), shape, fc );
    }

    if( name.equals( "numChildren" ) ) {
       int[] nc = new int[ locations.size() ];
       for( int i = 0; i < locations.size(); i++ ) {
          String loc = (String)locations.get( i );
          nc[ i ] = ((ArrayList)observations.get( loc )).size();
       }
       return Array.factory( datatype.getPrimitiveClassType(), shape, nc );
    }

    if( name.equals( "latitude" ) ) {
       HashMap coordinates = saveIndex.getCoordinates();
       ucar.bufr.Index.coordinate coord;
       String stn;
       float[] lat = new float[ locations.size() ];
       for( int i = 0; i < locations.size(); i++ ) {
          stn = (String)locations.get( i );
          coord = (ucar.bufr.Index.coordinate) coordinates.get( stn );
          lat[ i ] = coord.latitude;
       }
       return Array.factory( datatype.getPrimitiveClassType(), shape, lat );
    }

    if( name.equals( "longitude" )) {
       HashMap coordinates = saveIndex.getCoordinates();
       ucar.bufr.Index.coordinate coord;
       String stn;
       float[] lon = new float[ locations.size() ];
       for( int i = 0; i < locations.size(); i++ ) {
          stn = (String)locations.get( i );
          coord = (ucar.bufr.Index.coordinate) coordinates.get( stn );
          lon[ i ] = coord.longitude;
       }
       return Array.factory( datatype.getPrimitiveClassType(), shape, lon );
    }

    if( name.equals( "altitude" )) {
       HashMap coordinates = saveIndex.getCoordinates();
       ucar.bufr.Index.coordinate coord;
       String stn;
       int[] alt = new int[ locations.size() ];
       for( int i = 0; i < locations.size(); i++ ) {
          stn = (String)locations.get( i );
          coord = (ucar.bufr.Index.coordinate) coordinates.get( stn );
          alt[ i ] = coord.altitude;
       }
       return Array.factory( datatype.getPrimitiveClassType(), shape, alt );
    }
    System.out.println( "Don't know anything about "+ name );
    return null; // should never get here
  }

  private Array readDataVariableRecord(Variable v2, List section)
    throws IOException, InvalidRangeException {

       //System.out.println( "v2 is a variable in a Structure" );
       // get the index data
       HashMap atts = saveIndex.getGlobalAttributes();
       ArrayList locations = saveIndex.getLocations();
       //System.out.println( "locations ="+ locations );
       HashMap observations = saveIndex.getObservations();
       //ArrayList times = saveIndex.getObsTimes();
       //HashMap timeLocations = saveIndex.getObsLocations();
       //ArrayList parameters = saveIndex.getParameters();

       int[] shape = Range.getShape( section );
       Range range = (Range) section.get(0);
       //System.out.println( "range.first ="+ range.first() +" range.last ="+ range.last() );
       //System.out.println( "shape[0]  ="+ shape[0] );

       Array data = null;
       IndexIterator ii = null;

       // inner structure variables if needed
       ArraySequence ias = null;
       StructureMembers imembers = null;
       StructureMembers.Member member = null;

       // inner structure
          if( v2.getName().startsWith( "record.level" )) {
             member = imembers.addMember( v2.getShortName(), v2.getDescription(),
                v2.getUnitsString(), v2.getDataType(), v2.getShape());
             imembers = new StructureMembers( "level" );
             imembers.addMember( member );
             ias = new ArraySequence( imembers, shape[ 0 ] );
             int currOb = -1;
             int idx = 0;
             for( int i = 0; i < locations.size(); i++ ) {
                if( currOb > range.last() ) { // done
                   break;
                }
                String loc = (String)locations.get( i );
                //System.out.println( "loc =" + loc );
                ArrayList obs = (ArrayList) observations.get( loc );
                ucar.bufr.Index.BufrObs bo;
                for( int j = 0; j < obs.size(); j++ ) {
                   currOb++;
                   if( currOb < range.first() || currOb > range.last() ) {
                      continue;
                   }
                   bo = (ucar.bufr.Index.BufrObs) obs.get( j );
                   //System.out.println( currOb +" "+ bo.name +" "+ bo.dim );
                   //for (int seq=0; seq < outerLength; seq++)
                   //   aseq.setSequenceLength(seq, seqLength);
                   ias.setSequenceLength( idx++, bo.dim );
                }
             }
             ias.finish();
                data = (Array) member.getDataArray();
                //ii = data.getIndexIterator();
                //System.out.println( "IndexIterator ii ="+ ii );
          } else { // end inner Structure

          // outer structure variables
          //data = Array.factory(v.getDataType().getPrimitiveClassType(), Range.getShape( section ));
          // there should be a better way of allocating storage
          int[] vshape = v2.getShape();
          if( vshape.length != 0 && vshape[0] != -1 ) {
             //System.out.println( "v.sshape ="+ vshape[0] +" rank ="+ vshape.length );
             vshape[0] *= shape[0];
             data = Array.factory(v2.getDataType().getPrimitiveClassType(), vshape );
          } else {
             data = Array.factory(v2.getDataType().getPrimitiveClassType(), shape );
             //System.out.println( "data.getSize ="+ data.getSize() +" rank ="+ data.getRank() );
          }
          }
          //System.out.println( "data.getSize ="+ data.getSize() +" rank ="+ data.getRank() );

          ii = data.getIndexIterator();

       // compressed data is organized by field, not sequential obs
       if( ((String)atts.get( "compressdata")).equals( "true") ) {
          //System.out.println( "compressed data");
          readDataVariableRecordCompressed( (String)atts.get( "table"), locations, range,
             observations, v2, ii, data );
          return data;
       }

       // start of data reads for sequential obs
       BufrDataExtractor bde = 
          new BufrDataExtractor( raf, (String)atts.get( "table") );
       int currOb = -1; // current ob being processed
       for( int i = 0; i < locations.size(); i++ ) {
          if( currOb > range.last() ) { // done
             break;
          }
          String loc = (String)locations.get( i );
          //System.out.println( "loc =" + loc );
          ArrayList obs = (ArrayList) observations.get( loc );
          ucar.bufr.Index.BufrObs bo;
          for( int j = 0; j < obs.size(); j++ ) {
             currOb++;
             if( currOb < range.first() || currOb > range.last() ) {
                continue;
             }
             bo = (ucar.bufr.Index.BufrObs) obs.get( j );
             //System.out.println( bo.name +" "+ bo.DDSoffset +" "+ bo.obsOffset +" "+ bo.bitPos +" "+ bo.bitBuf );
             if( bde.getData( bo.DDSoffset, bo.obsOffset, bo.bitPos, bo.bitBuf ) ) {
                HashMap bufrdatas = (HashMap) bde.getBufrDatas();
                String bKey = null;
                BufrData bd;
                   // inner structure
                   if( v2.getName().startsWith( "record.level" )) {
                      //System.out.println( "v2 is a Structure name ="+ v2.getShortName() );
                         Attribute a = v2.findAttribute( "Bufr_key");
                         if( a == null )
                            continue;
                         bKey = a.getStringValue();
                         bd = (BufrData) bufrdatas.get( bKey );
                         if ( bd.getName().equals( "time_nominal" ) || bd.getName().equals( "time_observation" )) {
                           ii.setLongNext( (bd.getLongData())[0] );
                        } else if( bd.isNumeric() ) {
                           //System.out.println( "v2.getShortName() ="+ v2.getShortName() );
                           //System.out.println( "IndexIterator ii ="+ ii );
                           float[] bufrdata = bd.getFloatData();
                           for( int n = 0; n < bufrdata.length; n++ ) {
                              ii.setFloatNext( bufrdata[n] );
                              //System.out.print( bufrdata[n] + ", " );
                           }
                           //System.out.println();

                       } else { // String data
                         ii.setObjectNext( (bd.getStringData())[0] );
                      }
                    continue;
///////////////////// end inner structure

                   } else if( v2.getShortName().equals( "parent_index")) {
                      ii.setIntNext( i );
                      continue;
                   } else {
                      Attribute a = v2.findAttribute( "Bufr_key");
                      if( a == null )
                         continue;
                      bKey = a.getStringValue();
                   }
                   //System.out.println( "bKey ="+ bKey );
                   bd = (BufrData) bufrdatas.get( bKey );
                   //System.out.println( "bd.getName() ="+ bd.getName() );
                   if ( bd.getName().equals( "time_nominal" ) || bd.getName().equals( "time_observation" )) {
                      ii.setLongNext( (bd.getLongData())[0] );
                   } else if( bd.isNumeric() ) {
                      float[] bufrdata = bd.getFloatData();
                      for( int n = 0; n < bufrdata.length; n++ ) {
                         ii.setFloatNext( bufrdata[n] );
                      }
                   } else { // String data
                      ii.setObjectNext( (bd.getStringData())[0] );
                   }
             } else {
                System.out.println( "getData failed for loc ="+ loc );
             }
          }
       } // end looping over locations

              /*
              System.out.println( "data is returned" );
              ii = data.getIndexIterator();
              System.out.println( v2.getName() );
              for( ; ii.hasNext(); ) {
                 System.out.print( ii.getLongNext()  +", " );
              }
              System.out.println();
              */
              return data;
  } // end readDataVariableRecord

  public void readDataVariableRecordCompressed( String table, ArrayList locations,
    Range range, HashMap observations, Variable v2, IndexIterator ii,
    Array data )
    throws IOException, InvalidRangeException {
       // start of data reads
       BufrDataExtractor bde = new BufrDataExtractor( raf, table );
       int currOb = -1; // current ob being processed
       int numObs = -1; // number of obs in this data block
       for( int i = 0; i < locations.size(); i++ ) {
          if( currOb > range.last() ) { // done
             break;
          }
          String loc = (String)locations.get( i );
          //System.out.println( "loc =" + loc );
          ArrayList obs = (ArrayList) observations.get( loc );
          ucar.bufr.Index.BufrObs bo;
          int end = -1;
          for( int j = 0; j < obs.size(); j++ ) {
             if( currOb > range.last() ) { // done
                break;
             }
             bo = (ucar.bufr.Index.BufrObs) obs.get( j );
             //System.out.println( bo.name +" "+ bo.DDSoffset +" "+ bo.obsOffset +" "+ bo.bitPos +" "+ bo.bitBuf );
             // for compressed data bo.bitBuf contains number of obs in data
             if( (currOb + bo.bitBuf) > range.last() ) {
                 end = range.last() - currOb;
                 //System.out.println( "end ="+ end );
             } else {
                 end = -1;
             }
             currOb += bo.bitBuf; // bo.bitBuf contains number of obs in data
             //System.out.println( "currOb ="+ currOb );
             if( currOb < range.first() ) {
                continue;
             }
             numObs = bo.bitBuf;
             // compressed data packs a number of Obs into one data block
             // bo.bitBuf is always 0 for compressed data
             if( bde.getData( bo.DDSoffset, bo.obsOffset, bo.bitPos, 0 ) ) {
                HashMap bufrdatas = (HashMap) bde.getBufrDatas();
                String bKey = null;
                BufrData bd;
                   if( v2.getShortName().equals( "parent_index")) {
                      ii.setIntNext( i );
                      continue;
                   } else {
                      Attribute a = v2.findAttribute( "Bufr_key");
                      if( a == null )
                         continue;
                      bKey = a.getStringValue();
                   }
                   bd = (BufrData) bufrdatas.get( bKey );
                   if ( bd.getName().equals( "time_nominal" ) || bd.getName().equals( "time_observation" )) {
                      long[] bufrdata = bd.getLongData();
                      for( int n = 0; n < bufrdata.length && n != end; n++ ) {
                         ii.setLongNext( bufrdata[n] );
                         //System.out.println( "time_observation ="+ bufrdata[n] );
                      }
                   } else if( bd.isNumeric() ) {
                      float[] bufrdata = bd.getFloatData();
                      //System.out.println( "bufrdata.length ="+ bufrdata.length
                      //+" = "+ numObs );
                      for( int m = 0; m < numObs && m != end; m++ ) {
                         for( int n = m; n < bufrdata.length; n += numObs ) {
                            ii.setFloatNext( bufrdata[n] );
                         }
                      }
                    } else { // String data
                      String[] bufrdata = bd.getStringData();
                      for( int n = 0; n < bufrdata.length && n != end; n++ ) {
                         ii.setObjectNext( bufrdata[n] );
                         //System.out.println( "String ="+ bufrdata[n] );
                      }
                   }
             } else {
                System.out.println( "getData failed for loc ="+ loc );
             }
          }
       }
  } // end readDataVariableRecordCompressed

  public Array readNestedData(Variable v2, Section section)
    throws IOException, InvalidRangeException {
    //System.out.println( "readNestedData variable ="+ v2.getName() );
    //throw new UnsupportedOperationException("Bufr IOSP does not support nested variables");
    if( v2.getName().startsWith( "record." ) ) {
       return readDataVariableRecord( v2, section.getRanges() );
    } else {
       return readDataVariable( v2.getName(),  v2.getDataType(), section.getRanges() );
    }
  }

  protected void _readData( long DDSoffset, long obsOffset, int bitPos, int bitBuf) throws IOException {
      dataReader.getData( DDSoffset, obsOffset, bitPos, bitBuf );
  }

  public void close() throws IOException {
    raf.close();
  }

  public String getDetailInfo() {
    return parseInfo.toString();
  }

  /** main. */
  public static void main(String args[]) throws Exception, IOException,
    InstantiationException, IllegalAccessException {

    //String fileIn = "/home/rkambic/code/bufr/data/2005122912.bufr";
    String fileIn = "/home/rkambic/code/bufr/data/PROFILER_3.bufr";
    //String fileIn = "/home/rkambic/code/bufr/data/PROFILER_1.bufr";
    //String fileIn = "/home/rkambic/code/bufr/data/PROFILER_.bufr";
    //String fileIn = "R:/testdata/point/bufr/PROFILER_3.bufr";
    //String fileIn = "/home/rkambic/code/bufr/data/ruc2.t22z.class1.bufr.tm00";
    ucar.nc2.NetcdfFile.registerIOProvider( ucar.nc2.iosp.bufr.BufrIosp.class);
    //ucar.nc2.NetcdfFile ncf = ucar.nc2.NetcdfFile.open(fileIn);
    ucar.nc2.NetcdfFile ncf = ucar.nc2.NetcdfFile.open( args[ 0 ] );

    System.out.println();
    System.out.println( ncf.toString() );

    ucar.nc2.Variable v;

    v = ncf.findVariable("trajectory_id");
    if( v != null ) {
       Array data = v.read();
       NCdump.printArray(data, v.getName(), System.out, null);
    }
    v = ncf.findVariable("station_id");
    if( v != null ) {
       Array data = v.read();
       NCdump.printArray(data, v.getName(), System.out, null);
    }
    v = ncf.findVariable("firstChild");
    if( v != null ) {
       Array data = v.read();
       NCdump.printArray(data, v.getName(), System.out, null);
    }
    v = ncf.findVariable("numChildren");
    if( v != null ) {
       Array data = v.read();
       NCdump.printArray(data, v.getName(), System.out, null);
    }
    System.out.println();

    v = ncf.findVariable("record");
    //ucar.nc2.Variable v = ncf.findVariable("Latitude");
    //ucar.nc2.Variable v = ncf.findVariable("time");
    //System.out.println();
    //System.out.println( v.toString());

    if( v instanceof Structure ) {
       Array data = v.read();
       NCdump.printArray(data, "record", System.out, null);
    } else {
       Array data = v.read();
       int[] length = data.getShape();
       System.out.println();
       System.out.println( "v2 length =" + length[ 0 ] );

       IndexIterator ii = data.getIndexIterator();
       for( ; ii.hasNext(); ) {
          System.out.println( ii.getFloatNext() );
       }
    }
    ncf.close();
  }
} // end BufrIosp
