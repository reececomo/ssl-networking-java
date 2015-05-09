package lib;

/**
 *  A standard message class that auto-sets the message on
 *  instantiation, as well as providing a getter for the "flag"
 *  and analyst data properties.
 *  
 *  @author Alexander Popoff-Asotoff, Reece Notargiacomo, Jesse Fletcher, Caleb Fetzer
 */
 
public class Message {
	
	private enum Flag { NONE, INIC, INIA, DOIT, WIT, DEP };
	private Flag flag; 			// flag cannot be changed
	public String data; 			// contents can be referenced Message.content
    
  public Message(String rawMessage){
	  try {
	      this.flag = Flag.valueOf(rawMessage.split(":")[0]);
	      this.data = rawMessage.split(":")[1];
	      
	  } catch (Exception e) {
		  
		  System.err.println("Invalid message format: " + e);
	  }
  }
  
  public Message(String flag, String message) {
	  this(flag+":"+message);
  }
 
  public String getFlag() {
      return this.flag.toString();
  }
  
  public String raw() {
	  return this.getFlag() + ":" + this.data;
  }
  
  // For messages in the standard form:
  // "INIT_FLAG:DATA;DATA;DATA"
  public String[] getData() {
	  try {
		String[] array = this.data.split(";");
		if(array.length == 3)
			return array;
	  } catch (NullPointerException err) {
		  System.out.println("Not analyst data");
	  }
	  return null;
  }
  
}
