/**
 * 
 */
package ucar.nc2.iosp.fysat;

/**
 * @author Hurricane
 *
 */
public class AwxFileGridProductExtendedPart {

	char[] sat2004FileName = new char[64]; 
	char[] version = new char[8]; 
	char[] manufacturers = new char[8];
	char[] satelliteName = new char[8]; 
	char[] instrument = new char[8]; 
	char[] processProgram =new char[8]; 
	char[] reserved1 = new char[8];
	char[] copyright = new char[8];
	char[] fillLength = new char[8];
	char[] fillData = new char[234];
	
	/**
	 * 
	 */
	public AwxFileGridProductExtendedPart() {
		// TODO Auto-generated constructor stub
	}

}
