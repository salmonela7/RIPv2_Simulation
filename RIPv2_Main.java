import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RIPv2_Main {
	public static boolean automaticRouterFailure = false;
	private static NetworkGraph x;

	public static void main(String[] args) {
		clearLogfile("log.txt");
		List<Thread> routerThreads = new ArrayList<Thread>();
		automaticRouterFailure = false;
		
		x = new NetworkGraph();
		
	    try{
			Thread.sleep(1000);
			
			for(Node node : NetworkGraph.getNetworkGraph()){
				Thread curNode = new Thread(node);
				routerThreads.add(curNode);
			}
			
			for(Thread curThread : routerThreads){
				curThread.start();
			}

			mainMenu();
			
			for(Thread curThread : routerThreads){
				curThread.join();
			}
			
		}catch (InterruptedException e){
			e.printStackTrace();
		}
	}

	private static void clearLogfile(String fileName){
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(fileName);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		writer.print("");
		writer.close();
	}

	private static void failRouter(){
		String rt = "";
		BufferedReader reader =
				new BufferedReader(new InputStreamReader(System.in));
		try {
			System.out.println("Type in the hostname of router: ");
			rt = reader.readLine();

		} catch (IOException e) {
			e.printStackTrace();
		}

		Set<Node> nodes = x.getNetworkGraph();
		synchronized (nodes){
			for(Node node : nodes){
				if (node.getClass().getName().equals("Router") && node.getHostname().equals(rt)){
					((Router)node).setOperable(false);
				}
			}
		}
	}

	private static void mainMenu() {
		String choice = "";
		synchronized (System.out) {
			System.out.println("Main menu: ");
			System.out.println("1) Send message");
			System.out.println("2) Print routing tables");
			System.out.println("3) Fail a router");
			System.out.println("4) Check if is operable");
			System.out.println("5) Print initial routing tables");
			System.out.print("What is your choice: ");
		}

		BufferedReader reader =
				new BufferedReader(new InputStreamReader(System.in));
		try {
			choice = reader.readLine();

		} catch (IOException e) {
			e.printStackTrace();
		}
		if (choice.equals("1")) {
			handleMessages();
		}
		else if (choice.equals("2")){
			printRountingTables();
		}
		else if (choice.equals("3")){
			failRouter();
		}
		else if (choice.equals("4")){
			checkIfOperable();
		}
		else if (choice.equals("5")){
			printInitialRountingTables();
		}
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		mainMenu();
	}

	private static void checkIfOperable(){
		String router = "";
		BufferedReader reader =
				new BufferedReader(new InputStreamReader(System.in));
		try {
			System.out.println("Type in router hostname: ");
			router = reader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(NetworkGraph.getNodeByHostname(router).isOperable()){
			System.out.println("TRUE");
		}
		else {
			System.out.println("FALSE");
		}

	}

	private static void handleMessages() {
		String sender = "";
		String messageBody = "";
		String address = "";

		BufferedReader reader =
				new BufferedReader(new InputStreamReader(System.in));
		try {
			System.out.println("Type in the sender address: ");
			sender = reader.readLine();
			System.out.println("Type in the receiver address: ");
			address = reader.readLine();
			System.out.println("Type in the message body: ");
			messageBody = reader.readLine();

		} catch (IOException e) {
			e.printStackTrace();
		}

		Message message = new Message(messageBody, address);
		PC senderNode = (PC)x.getNodeByAddress(sender);
		if (senderNode == null) {
			System.out.println("Node with sender address does not exist...");
		}
		else senderNode.sendMessageToRouter(message);
	}

	private static void printRountingTables() {
		for(Node node : x.getNetworkGraph()){
			if (node.getClass().getName().equals("Router")) {
				
				((Router)node).getRoutingTable().printTable();
			}
		}
	}

	private static void printInitialRountingTables() {
		for(Node node : x.getNetworkGraph()){
			if (node.getClass().getName().equals("Router")) {
				((Router)node).getInitialRoutingTable().printTable();
			}
		}
	}
}
