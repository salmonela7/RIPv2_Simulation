import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

class RoutingTable {
	private Node myNode;

	private List<String> destinationIPAddresses = new ArrayList<String>();
	private List<String> destinationHostname = new ArrayList<String>();
	private List<String> subnetMasks = new ArrayList<String>();
	private List<String> nextHopIPAddresses = new ArrayList<String>();
	private List<Integer> distanceMetrics = new ArrayList<Integer>();
	private List<Node> neighborNodes = new ArrayList<Node>();

	private final int infinity = Integer.MAX_VALUE;

	public RoutingTable(Node node, Set<Node> networkGraph){
		this.myNode = node;
		Set<Edge> neighborsEdges = node.getEdges();

		for(Edge curEdge : neighborsEdges){
			Node x = (Node)curEdge.getX();
			Node y = (Node)curEdge.getY();
			
			Node neighbor;
			if(myNode.getAddress().equals(x.getAddress())){
				neighbor = y;
			}else{
				neighbor = x;
			}
			neighborNodes.add(neighbor);
		}

		synchronized (networkGraph) {
			for (Node n : networkGraph) {
				if (this.myNode.isOperable() && n.isOperable()) {
					String curAddress = n.getAddress();

					if (!curAddress.equals(myNode.getAddress()) && !isANeighbour(n)) {
						destinationIPAddresses.add(curAddress);
						destinationHostname.add(n.getHostname());
						subnetMasks.add(n.getSubnetMask());
						distanceMetrics.add(infinity);
						nextHopIPAddresses.add("0.0.0.0");
					} else if (isANeighbour(n)) {
						int weight = 0;

						for (Edge curEdge : neighborsEdges) {
							Node x = (Node) curEdge.getX();
							Node y = (Node) curEdge.getY();

							if (x.equals(n) || y.equals(n)) {
								weight = curEdge.getWeight();
							}
						}

						destinationIPAddresses.add(curAddress);
						destinationHostname.add(n.getHostname());
						subnetMasks.add(n.getSubnetMask());
						distanceMetrics.add(weight);
						nextHopIPAddresses.add(n.getAddress());
					}
				}
			}
		}
	}

	public boolean checkIfRouteWasUpdatedToInfinity(String address) {
		int index = destinationIPAddresses.indexOf(address);
		if (index != -1 && distanceMetrics.get(index).equals(Integer.MAX_VALUE) && nextHopIPAddresses.get(index).equals("0.0.0.0")){
			return true;
		}
		return false;
	}

	public int getShortestMetricToNode(Node source, String destAddr, Node previous, int metric, ArrayList<String> visitedPlaces){
		visitedPlaces.add(source.getHostname());
		CopyOnWriteArrayList<Node> sourceNeighbors = source.getNeighbors();

		if (source.getAddress().equals(destAddr)) return 0;

		ArrayList<Integer> metrics = new ArrayList<Integer>();

		for (int i = 0; i < sourceNeighbors.size(); i++) {
			if (sourceNeighbors.get(i).getAddress().equals(destAddr) && sourceNeighbors.get(i).isOperable()) {
				metric++;
				return metric;
			}
		}

		String prevAddr = "";
		if (previous != null) prevAddr = previous.getAddress();

		for (int i = 0; i < sourceNeighbors.size(); i++){
			if(!visitedPlaces.contains(sourceNeighbors.get(i).getHostname()) && sourceNeighbors.get(i).isOperable()){
				int met = metric;
				metrics.add(getShortestMetricToNode(sourceNeighbors.get(i), destAddr, source, ++metric, visitedPlaces));
			}
		}
		int minVal;
		if (metrics.size() > 0) {
			minVal = Collections.min(metrics);
		}
		else {
			minVal = Integer.MAX_VALUE;
		}
		return minVal;
	}

	private boolean isANeighbour(Node n){
		for (int i = 0; i < neighborNodes.size(); i++){
			if (neighborNodes.get(i).getAddress().equals(n.getAddress())) return true;
		}
		return false;
	}

	public void updateMetric(String address, int newMetric){
		int index = destinationIPAddresses.indexOf(address);
		if (index != -1) distanceMetrics.set(index, newMetric);
	}

	public void updateNextHop(String address, String newHop){
		int index = destinationIPAddresses.indexOf(address);
		if (index != -1) nextHopIPAddresses.set(index, newHop);
	}

	 public void removeFromTableByNextHopAddress(String address){
		 int i = 0;
		 for(String nextHop : nextHopIPAddresses){
		 	if(nextHop.equals(address)){
				nextHopIPAddresses.set(i, "0.0.0.0");
				distanceMetrics.set(i, Integer.MAX_VALUE);
			}
		 	i++;
		 }
	 }

	public String getDestination(String targetDestination){
		for(String curDestination : destinationIPAddresses){
			if(curDestination.equals(targetDestination)){
				return curDestination;
			}
		}
		
		return null;
	}

	public String getNextHop(String targetDestination){
		int destinationIndex = destinationIPAddresses.indexOf(targetDestination);
		
		if(destinationIndex != -1){
			return nextHopIPAddresses.get(destinationIndex);
		}else{
			return null;
		}
	}

	public int getDistanceMetric(String targetDestination){
		int destinationIndex = destinationIPAddresses.indexOf(targetDestination);
		
		if(destinationIndex != -1 && isDestinationOperable(targetDestination)){
			return distanceMetrics.get(destinationIndex);
		}else{
			return infinity;
		}
	}

	public boolean isDestinationOperable(String targetDestination) {
		Set<Node> nodes = NetworkGraph.getNetworkGraph();
		synchronized (nodes){
			for(Node node : nodes){
				if(node.getAddress().equals(targetDestination) && node.isOperable()) return true;
			}
		}
		return false;
	}

	public List<String> getDestinations() {
		return destinationIPAddresses;
	}

	public synchronized void printTable(){
		synchronized(System.out) {
			if (myNode.isOperable()) {
				System.out.println("\n|RT hostname|" + "\t" + "|Dest. IP address|"
						+ "\t" + "|Dest. subnet mask|" + "\t" + "|Next hop IP address|" +
						"\t" + "|metric|");

				for (int i = 0; i < destinationIPAddresses.size(); i++) {
					String currentDestination = destinationIPAddresses.get(i);
					String currentHostname = destinationHostname.get(i);

					System.out.print("\t" + myNode.getHostname() + "\t\t\t       " + currentHostname +
							"\t" + "            255.255.255.0" + "\t    " + getNextHop(destinationIPAddresses.get(i)) + "\t\t");

					if (nextHopIPAddresses.get(i).equals("0.0.0.0")) {
						System.out.print("\t\t");
					}

					if (getDistanceMetric(currentDestination) == infinity) {
						System.out.print("infinity");
					} else {
						System.out.print("\t\t" + getDistanceMetric(currentDestination));
					}

					System.out.println();
				}
			}
			else System.out.println("\n" + myNode.getHostname() + " IS OFFLINE");
		}
	}
}
