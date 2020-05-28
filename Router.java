import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Router extends Node implements Runnable{
    private String hostname;
    private String ipAddress;
    private String subnetMask;
    private Set<Edge> edges = new HashSet<Edge>();
    private boolean isOperable = true;
    private RoutingTable table;
    private RoutingTable initialRoutingTable;
    private Message message;
    private CopyOnWriteArrayList<Node> neighbors;
    private int currentTime = 0;
    private Timer timer = new Timer();
    private Random random = new Random();

    public Router(String ipAddress, String hostname, String subnetMask) {
        this.ipAddress = ipAddress;
        this.hostname = hostname;
        this.subnetMask = subnetMask;
        this.neighbors = new CopyOnWriteArrayList<Node>();
    }

    public void collectNeighbours(){
            this.neighbors = new CopyOnWriteArrayList<Node>();
            Set<Edge> edges = getEdges();
            synchronized (edges) {
                for (Edge edge : edges) {
                    if (!((Node) edge.getX()).getAddress().equals(this.ipAddress) && !this.neighbors.contains(((Node) edge.getX()))) {
                        synchronized (this.neighbors) {
                            this.neighbors.add((Node)edge.getX());
                        }
                    }
                    else if (!((Node) edge.getY()).getAddress().equals(this.ipAddress) && !this.neighbors.contains(((Node) edge.getY()))) {
                        synchronized (this.neighbors) {
                            this.neighbors.add((Node)edge.getY());
                        }
                    }
                }
            }
    }

    public void removeNeighborByAddress(String address){
        this.neighbors.remove(NetworkGraph.getNodeByAddress(address));
    }

    public Message getMessage() { return this.message; }

    public CopyOnWriteArrayList<Node> getNeighbors(){ return this.neighbors; }

    public String getHostname(){ return this.hostname; }

    public RoutingTable getRoutingTable() { return this.table; }

    public RoutingTable getInitialRoutingTable() { return this.initialRoutingTable; }

    public String getAddress(){
        return ipAddress;
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

    public void setOperable(boolean operable) { isOperable = operable; }

    public boolean isOperable(){
        return isOperable;
    }

    private boolean isDestinationReachable(String destAddress){
        int length = table.getShortestMetricToNode(this, destAddress, this, 0, new ArrayList<String>());
        if(length < Integer.MAX_VALUE) return true;
        else return false;
    }

    private void receiveRoutingTable(Node neighbor, RoutingTable neighborTable){
        try{
            if(isOperable()) {
                String neighborAddress = neighbor.getAddress();

                List<String> neighborDestinations = neighborTable.getDestinations();
                List<String> nodeDestinations = table.getDestinations();

                for (String neighborDestination : neighborDestinations) {
                    for (String currentDestination : nodeDestinations) {
                        if (currentDestination.equals(neighborDestination)) {

                            int currentMetric = table.getDistanceMetric(currentDestination);
                            int neighborMetric = neighborTable.getDistanceMetric(neighborDestination);

                            String currentNextHop = table.getNextHop(currentDestination);
                            String neighborNextHop = neighborTable.getNextHop(neighborDestination);

                            if (neighborMetric < currentMetric && isDestinationReachable(currentDestination)) {
                                int newMetric;
                                int curNodeToNeighborDistance = table.getDistanceMetric(neighborAddress);

                                if (curNodeToNeighborDistance == Integer.MAX_VALUE) {
                                    newMetric = neighborMetric;
                                } else {
                                    newMetric = neighborMetric + curNodeToNeighborDistance;
                                }

                                if (!isDestinationReachable(table.getDestination(neighborAddress))) {
                                    table.updateMetric(currentDestination, Integer.MAX_VALUE);
                                    table.updateNextHop(currentDestination, "0.0.0.0");
                                }
                                else {
                                    table.updateMetric(currentDestination, newMetric);
                                    table.updateNextHop(currentDestination, table.getDestination(neighborAddress));
                                }
                            }
                            else if (!isDestinationReachable(currentNextHop) || !isDestinationReachable(currentDestination)) {
                                table.updateMetric(currentDestination, Integer.MAX_VALUE);
                                table.updateNextHop(currentDestination, "0.0.0.0");
                            }
                        }
                    }
                }
            }
        }catch(NullPointerException e){
        }
    }

    private boolean knowThatNodeIsOffline(String nodeAddress){
        return table.checkIfRouteWasUpdatedToInfinity(nodeAddress);
    }

    private void routerFailed(String failedAddress){

        if(!table.checkIfRouteWasUpdatedToInfinity(failedAddress)) {

            table.updateMetric(failedAddress, Integer.MAX_VALUE);
            table.updateNextHop(failedAddress, "0.0.0.0");
            table.removeFromTableByNextHopAddress(failedAddress);

            CopyOnWriteArrayList<Node> neighbrs = getNeighbors();
            ArrayList<String> neighborsToDelete = new ArrayList<String>();
            synchronized (neighbrs) {
                for (Node node : neighbrs) {
                    if (node.getAddress().equals(failedAddress)) {
                        neighborsToDelete.add(node.getAddress());;
                    }
                    else if(node.getClass().getName().equals("Router") && !((Router) node).knowThatNodeIsOffline(failedAddress)){
                        ((Router) node).routerFailed(failedAddress);
                    }
                }

                for(String str : neighborsToDelete){
                    removeNeighborByAddress(str);
                }
            }
        }
    }

    private void broadcastRoutingTable(RoutingTable nodeTable){
        List<Node> allNeighbs = getNeighbors();
        synchronized (allNeighbs) {
            for (int i = 0; i < allNeighbs.size(); i++) {
                Node neighbor = allNeighbs.get(i);
                if (neighbor != null && neighbor.getClass().getName().equals("Router") && neighbor.isOperable()) {
                    ((Router) neighbor).receiveRoutingTable(this, nodeTable);
                }
            }
        }
    }

    private void tryToFail(){
        if(random.nextInt(100) <= 1){
            isOperable = false;
        }
    }

    public void sendMessageToDestination(Message message) {
        List<Node> allNeighbors = getNeighbors();
        synchronized (allNeighbors) {
            for (Node neighbor : allNeighbors) {
                if (neighbor.getAddress().equals(message.getMessageDestination())) {
                    sendMessageToPC(neighbor, message);
                    return;
                }
            }
        }
        if (this.message != null && message.getMessageDestination().equals(getAddress())) {
            System.out.println("MESSAGE ARRIVED TO DESTINATION");
            this.message = null;
            return;
        }
        else {
            String nextHop = table.getNextHop(message.getMessageDestination());
            Node nextHopNode = NetworkGraph.getNodeByAddress(nextHop);
            if(nextHopNode != null){
            System.out.println();
            System.out.println("SENDING TO: " + nextHopNode.getHostname());
            nextHopNode.receiveMessage(message);
            this.message = null;
            }
            else {
            System.out.println();
            System.out.println("NO NEXT HOP ADDRESS TO DESTINATION FOUND");
            System.out.println("DISCARDING MESSAGE");
            System.out.println();
            this.message = null;
            return;
            }
        }
    }

    public void receiveMessage(Message message){
        this.message = message;
        System.out.println();
        debug("MESSAGE RECEIVED: " + message.getMessageBody());
    }

    public void sendMessageToPC(Node pc, Message message){
        pc.receiveMessage(message);
        this.message = null;
    }

    class LogIntoFileTask extends TimerTask{
        String filename = "";
        LogIntoFileTask(String filename){
            this.filename = filename;
        }
        public void run(){
            FileWriter fw = null;

            SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
            Date date = new Date(System.currentTimeMillis());

            try {
                fw = new FileWriter(filename, true);
                fw.write("[" + formatter.format(date) + "] " + hostname + " (" + ipAddress + "): " + "\n");
                fw.write("Status: ");
                if (isOperable()) fw.write("ONLINE");
                else fw.write("OFFLINE");
                fw.write("\n");
                fw.write("Seconds online: " + currentTime + "\n");
                fw.write("Currently posessing a message: ");
                Message message = getMessage();
                if(message == null) fw.write("NO\n");
                else fw.write("YES: " + message.getMessageBody() + "(DEST_ADDR: " + message.getMessageDestination() + ")" + "\n");
                fw.write("Neighbors: \n");
                CopyOnWriteArrayList<Node> allNeighbors = getNeighbors();
                synchronized (allNeighbors){
                    for(Node node : allNeighbors){
                        fw.write(node.getHostname() + " (" + node.getAddress() + ") " + "\n");
                    }
                }
                fw.write("\n");
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class BroadcastRoutingTableTask extends TimerTask{
        RoutingTable routingTable;

        BroadcastRoutingTableTask(RoutingTable routingTable){
            this.routingTable = routingTable;
        }
        public void run(){
            if(isOperable()) broadcastRoutingTable(routingTable);
        }
    }

    private void printNeighborsList(){
        synchronized (neighbors){
            synchronized (System.out) {
                System.out.print("(" + neighbors.size() + ")" + hostname + ": ");
                for (Node neig : neighbors) {
                    System.out.print(neig.getHostname() + ", ");
                }
                System.out.println();
            }
        }
    }

    @Override
    public void run() {
        Set<Node> network = NetworkGraph.getNetworkGraph();
        currentTime = 0;

        table = new RoutingTable(this, network);
        initialRoutingTable = new RoutingTable(this, network);;

        timer.scheduleAtFixedRate(new LogIntoFileTask("log.txt"), 0, 1000);
        timer.scheduleAtFixedRate(new BroadcastRoutingTableTask(table), 0, 1000);

        while(true){
            try {
                if (isOperable()) {
                    synchronized (neighbors) {
                        Iterator<Node> iterator = neighbors.iterator();
                        while(iterator.hasNext()) {
                            Node node = iterator.next();
                            if (!node.isOperable()) {
                                routerFailed(node.getAddress());
                            }
                        }
                    }

                    if (this.message != null) {
                        sendMessageToDestination(this.message);
                    }
                    currentTime++;

                    if (RIPv2_Main.automaticRouterFailure) {
                        tryToFail();
                    }
                }
                else {
                    synchronized (System.out) {
                        System.out.println("\n" + ipAddress + " has failed!");
                        break;
                    }
                }
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
