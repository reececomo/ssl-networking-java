package lib;

/**
 *  A standard message format that auto-sets the message on
 *  instantiation, as well as providing a getter for the "flag"
 *  and analyst data properties.
 *  
 *  @author Reece Notargiacomo, Alexander Popoff-Asotoff
 */
 
public class Message {
	
	public enum MessageFlag { NONE, INITIATE_ANALYST, EXAM_REQ, WITHDRAW, DEPOSIT, CONFIRM_DEPOSIT, CANCEL_DEPOSIT,
							KEYPAIR, VALID_KEYPAIR, PUB_KEY, ERROR, WARNING, VALIDATE_WITH_BANK };
	public MessageFlag flag; 			// flag cannot be changed
	public String data; 			// contents can be referenced Message.content
    
  public Message(String rawMessage){
	  try {
		  String[] parts = rawMessage.split(":");
		  this.flag = MessageFlag.valueOf(parts[0].toUpperCase());
		  if (parts.length > 1)
			  this.data = parts[1];
	      
	  } catch (Exception e) {
	  	if(rawMessage!=null){
		  this.data = rawMessage;
		  this.flag = MessageFlag.NONE;
		}else {
			data = null;
			flag = MessageFlag.ERROR;
		}
	  }
  }
  
  public Message(String flag, String message) {
	  this(flag+":"+message);
  }
 
  public String getMessageFlag() {
	  String flag;
  
	  try {
		  flag = this.flag.toString();
	  } catch (Exception err) {
		  flag = "";
	  }
	  
	  return flag;
  }
  
  public String raw() {
	  return this.getMessageFlag() + ":" + this.data;
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
