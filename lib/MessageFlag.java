package lib;
/**
 *  A list of Message flags shared by classes
 *  
 *  @author Alexander Popoff-Asotoff, Reece Notargiacomo, Jesse Fletcher, Caleb Fetzer
 */

public class MessageFlag {
	public static final String BANK_DEP = "DEP";	// bank deposit (analyst)
	public static final String BANK_WIT = "WIT";	// bank withdrawl (collector)
	public static final String C_INIT = "INIC";	// collector init with dir
	public static final String A_INIT = "INIA";	// analyst init with dir
	public static final String EXAM_REQ = "DOIT";	// analysis request
	public static final String PUB_KEY = "PUBK";
	public static final String ERROR = "Error";
	public static final String NONE = "NONE";
}
