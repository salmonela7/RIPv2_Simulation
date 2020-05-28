import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class NetworkGraph{
	private static Set<Node> networkGraph = new HashSet<Node>();
	private Set<String> uniqueAddresses = new HashSet<String>();

	public NetworkGraph(){
			Set<Edge> initialEdges = new HashSet<Edge>();
			File configFile = new File("config.txt");
			String template = "";
			ArrayList<Router> routers = new ArrayList<Router>();
			ArrayList<PC> pcs = new ArrayList<PC>();

			try {
				Scanner reader = new Scanner(configFile);
				while(reader.hasNextLine()){
					String line = reader.nextLine();
					if (line.startsWith("[") && line.contains("devices")){
						template = "devices";
					}
					else if (line.startsWith("[") && line.contains("neighborhood")){
						template = "neighborhood";
					}
					if(!line.startsWith("[") && template.equals("devices") && !line.equals("")){
						String[] attributes = line.split(";");
						String[][] attributeAndValue = new String[4][2];
						for(int j = 0; j < attributes.length; j++){
							attributeAndValue[j] = attributes[j].split(":");
						}

						if(attributeAndValue[0][1].equals("PC")){
							PC pc = new PC(attributeAndValue[2][1], attributeAndValue[1][1]);
							pcs.add(pc);
							networkGraph.add(pc);
							uniqueAddresses.add(pc.getAddress());
						}
						else if(attributeAndValue[0][1].equals("Router")){
							Router router = new Router(attributeAndValue[2][1], attributeAndValue[1][1], attributeAndValue[3][1]);
							routers.add(router);
							networkGraph.add(router);
							uniqueAddresses.add(router.getAddress());
						}
					}
					else if (!line.startsWith("[") && template.equals("neighborhood")){
						String[] attribute = line.split(":");
						String[] neighbNodenames;
						int columnIndex = attribute[1].indexOf(",");
						if(columnIndex != -1){
							Node sourceNode = getNodeByHostname(attribute[0]);
							neighbNodenames = attribute[1].split(",");
							for(String neighbName : neighbNodenames){
								String cleanHostname = neighbName.replaceAll("[;]", "");
								Node destNode = getNodeByHostname(cleanHostname);
								Edge edge = new Edge(sourceNode, destNode, 1);
								sourceNode.addEdge(edge);
								destNode.addEdge(edge);
								initialEdges.add(edge);
							}
						}
						else {
							String cleanHostname = attribute[1].replaceAll("[;]", "");
							Node destNode = getNodeByHostname(cleanHostname);
							Node sourceNode = getNodeByHostname(attribute[0]);
							Edge edge = new Edge(sourceNode, destNode, 1);
							sourceNode.addEdge(edge);
							destNode.addEdge(edge);
							initialEdges.add(edge);
						}
					}
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

			Set<Node> allNodes = getNetworkGraph();
			synchronized (allNodes){
				for(Node node : allNodes){
					node.collectNeighbours();
				}
			}
        }

	public static Node getNodeByAddress(String address){
		for(Node node : networkGraph){
			if(node.getAddress().equals(address)){
				return node;
			}
		}
		return null;
	}

	public static Node getNodeByHostname(String hostname){
		Set<Node> allNodes = getNetworkGraph();
		synchronized (allNodes){
			for(Node node : allNodes){
				if(node.getHostname().equals(hostname)){
					return node;
				}
			}
		}
		return null;
	}

	public static Set<Node> getNetworkGraph(){
		return networkGraph;
	}
}
