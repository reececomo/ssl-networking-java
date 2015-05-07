package lib;

/**
 *  A standard message class that auto-sets the message on
 *  instantiation, as well as providing a getter for the "flag"
 *  property.
 */
 
public class Message {
	
    private String flag; // flag cannot be changed
    public String content; // contents can be referenced Message.content
    
  public Message(String rawMessage){
      if(rawMessage.length() <= 4)
          this.flag = "NONE";
      else {
          this.flag = rawMessage.substring(0,4);
          this.content = rawMessage.substring(4);
      }
  }
  
  public Message(String flag, String message) {
	  this(flag+message);
  }
 
  public String getFlag() {
      return this.flag;
  }
  
  public String asData() {
	  return this.flag + ":" + this.content;
  }
  
}
