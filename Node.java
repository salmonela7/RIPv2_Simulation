import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Node implements Runnable{
	private String hostname;
	private String ipAddress;
	private final String subnetMask = "255.255.255.0";
	private Set<Edge> edges = new HashSet<Edge>();
	private boolean isOperable = true;
	private Message message;
	private CopyOnWriteArrayList<Node> neighbors;

	private Random random = new Random();

	public Node() {

	}

	public void collectNeighbours(){
		this.neighbors = new CopyOnWriteArrayList<Node>();
		Set<Edge> edges = getEdges();
		synchronized (edges) {
			for (Edge edge : edges) {
				if (!((Node) edge.getX()).getAddress().equals(this.ipAddress)) {
					synchronized (this.neighbors) {
						this.neighbors.add((Node)edge.getX());
					}
				}
				if (!((Node) edge.getY()).getAddress().equals(this.ipAddress)) {
					synchronized (this.neighbors) {
						this.neighbors.add((Node)edge.getY());
					}
				}
			}
		}
	}

	public Message getMessage() { return this.message; }

	public CopyOnWriteArrayList<Node> getNeighbors(){ return this.neighbors; }

	public String getHostname(){ return this.hostname; }

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

	public void receiveMessage(Message message){
		this.message = message;
		debug("MESSAGE RECEIVE: " + message.getMessageBody());
	}

	public void debug(String debugText){
		System.out.println("(" + this.getClass().getName() + ": " + getAddress() + "): " + debugText);
	}

	@Override
	public void run() {

	}
}
