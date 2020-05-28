public class Message {
    private String body;

    private String destination;

    public String getMessageBody(){ return this.body; }

    public String getMessageDestination(){ return this.destination; }

    public Message(String body, String dest){
        this.body = body;
        this.destination = dest;
    }

}

