import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class PC extends Node implements Runnable{

    private String hostname;
    private String ipAddress;
    private final String subnetMask = "255.255.255.0";
    private Set<Edge> edges = new HashSet<Edge>();
    private boolean isOperable = true;
    private Message message;
    private CopyOnWriteArrayList<Node> neighbors;


    public PC(String ipAddress, String hostname) {
        this.ipAddress = ipAddress;
        this.hostname = hostname;
        this.neighbors = new CopyOnWriteArrayList<Node>();
    }

    public void collectNeighbours(){
        this.neighbors.clear();
        Set<Edge> edges = getEdges();
        for (Edge edge : edges){
            if (!((Node)edge.getX()).getAddress().equals(ipAddress)) this.neighbors.add((Node)edge.getX());
            if (!((Node)edge.getY()).getAddress().equals(ipAddress)) this.neighbors.add((Node)edge.getY());
        }
    }
    
    public Message getMessage() { return this.message; }

    public CopyOnWriteArrayList<Node> getNeighbors(){ return this.neighbors; }

    public String getHostname() { return this.hostname; }

    public String getAddress(){
        return this.ipAddress;
    }

    public String getSubnetMask(){
        return subnetMask;
    }

    public Set<Edge> getEdges(){
        return edges;
    }

    public void addEdge(Edge edge){
        edges.add(edge);
    }

    public boolean isOperable(){
        return isOperable;
    }

    public void sendMessageToRouter(Message message){
        Set<Edge> edges = getEdges();
        for(Edge edge : edges){
            if (edge.getX().getClass().getName().equals("Router")) {
                System.out.println();
                System.out.println("SENDING FROM " + getHostname() + " TO ROUTER(" + ((Router)edge.getX()).getHostname() + ")");
                sendMessageDirectly((Router)edge.getX(), message);
                this.message = null;
                return;
            }
            else if (edge.getY().getClass().getName().equals("Router")) {
                System.out.println();
                System.out.println("SENDING FROM " + getHostname() + " TO ROUTER(" + ((Router)edge.getY()).getHostname() + ")");
                sendMessageDirectly((Router)edge.getY(), message);
                this.message = null;
                return;
            }
        }
        System.out.println("Computer is not connected to any router...");
    }

    public void receiveMessage(Message message){
        this.message = message;
        System.out.println();
        debug("MESSAGE RECEIVED: " + message.getMessageBody());
        if (message.getMessageDestination().equals(ipAddress)) {
            debug("MESSAGE ARRIVED TO DESTINATION!");
            this.message = null;
        }
        System.out.println();
    }

    private void sendMessageDirectly(Router destination, Message message){
        destination.receiveMessage(message);
        destination.sendMessageToDestination(message);
        this.message = null;
    }

    @Override
    public void run() {
        while(true){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
