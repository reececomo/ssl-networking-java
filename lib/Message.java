/**
 *  A standard message class that auto-sets the message on
 *  instantiation, as well as providing a getter for the "flag"
 *  property.
 */
 
public class Message {
    private String flag; // flag cannot be changed
    public String content; // contents can be referenced Message.content

  public Message(String rawMessage) {
      if(rawMessage.length() <= 4)
          throw new Exception ("Invalid string length!");
      else {
          this.flag = rawMessage.subString(0,4);
          this.content = rawMessage.subString(4);
      }
  }
  
  public String getFlag() {
      return this.flag;
  }
  
}
