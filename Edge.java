public class Edge {
	private final Object x;
	private final Object y;
	private int weight;

	public Edge(Node x, Node y, int weight){
		this.x = x;
		this.y = y;
		this.weight = weight;
	}

	public Object getX(){
		return x;
	}

	public Object getY(){
		return y;
	}

	public int getWeight(){
		return weight;
	}
}
