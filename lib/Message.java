package lib;

/**
 *  A standard message class that auto-sets the message on
 *  instantiation, as well as providing a getter for the "flag"
 *  and analyst data properties.
 *  
 *  @author Alexander Popoff-Asotoff, Reece Notargiacomo, Jesse Fletcher, Caleb Fetzer
 */
 
public class Message {
	
	public enum Flag { NONE, INIC, INIA, DOIT, WIT, DEP, PUBK, Error, Warning };
	private Flag flag; 			// flag cannot be changed
	public String data; 			// contents can be referenced Message.content
    
  public Message(String rawMessage){
	  try {
		  String[] parts = rawMessage.split(":");
		  this.flag = Flag.valueOf(parts[0]);
		  if (parts.length > 1)
			  this.data = parts[1];
	      
	  } catch (Exception e) {
	  	if(rawMessage!=null){
		  this.data = rawMessage;
		  this.flag = Flag.NONE;
		}else {
			data = null;
			flag = Flag.Error;
		}
	  }
  }
  
  public Message(String flag, String message) {
	  this(flag+":"+message);
  }
 
  public String getFlag() {
	  String flag;
  
	  try {
		  flag = this.flag.toString();
	  } catch (Exception err) {
		  flag = "";
	  }
	  
	  return flag;
  }
  
  public Flag getFlagEnum() {
  
  	return this.flag;
  }
  
  public String raw() {
	  return this.getFlag() + ":" + this.data;
  }
  
  // For messages in the standard form:
  // "INIT_FLAG:DATA;DATA;DATA; ... ;"
  public String[] getData() {
	  try {
		String[] array = this.data.split(";");
		if(array.length > 0)
			return array;
	  } catch (NullPointerException err) {
	  }
	  return null;
  }
  
}
