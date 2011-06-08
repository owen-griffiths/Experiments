import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.HashMap;

class SetSet {
	public static long m_nodeCount;
	
	private class Node {
		public Node(int value, int countValues) {
			m_value = value;
			int countChildren = countValues - value - 1;
			m_children = new Node[countChildren];
			SetSet.m_nodeCount++;
		}
		
		public void Add(BitSet set, int countValues) {
			int next = NextPresent(set, m_value + 1, countValues);
			if (next == countValues)
				return;
			
			int i = next - m_value - 1;
			
			if (m_children[i] == null)
				m_children[i] = new Node(next, countValues);
			
			m_children[i].Add(set, countValues);
		}
		
		public boolean ContainsSuper(BitSet set, int countValues) {
			int next = NextPresent(set, m_value + 1, countValues);
			if (next == countValues)
				return true;
			
			int endI = next - m_value;
			for (int i = 0; i < endI; i++) {
				Node c = m_children[i];
				if ((c != null) && c.ContainsSuper(set, countValues))
					return true;
			}
			
			return false;
		}

		private int m_value;
		private Node[] m_children;
	}
	
	public SetSet(int countValues) {
		m_roots = new Node[countValues];
	}
	
	public boolean ContainsSuper(BitSet set) {
		int countValues = m_roots.length;
		
		int first = NextPresent(set, 0, countValues);
		if (first == countValues)
			return true;
		
		for (int i = 0; i <= first; i++)
			if ((m_roots[i] != null) && m_roots[i].ContainsSuper(set, countValues))
				return true;
		
		return false;
	}
	
	public void Add(BitSet set) {
		int countValues = m_roots.length;

		int first = NextPresent(set, 0, countValues);
		if (first == countValues)
			return;

		if (m_roots[first] == null)
			m_roots[first] = new Node(first, countValues);
		
		m_roots[first].Add(set, countValues);
	}
	
	public static int NextPresent(BitSet set, int start, int countValues) {
		while (start < countValues) {
			if (set.get(start))
				break;
			
			start++;
		}
		
		return start;
	}
	
	private Node[] m_roots; 
}

class Arc {
	private Location m_to;
	private double m_time;
	
	public Arc(Location to, double time) {
		m_to = to;
		m_time = time;
	}
	
	public Location To() { return m_to; }
	
	public double Length() { return m_time; }
}

class Location {
	private String m_name;
	private int m_index;
	private double m_prob;
	private ArrayList<Arc> m_arcs;
	
	public Location(String name, double prob, int index) {
		m_name = name;
		m_prob = prob;
		m_arcs = new ArrayList<Arc>();
		m_index = index;
	}
	
	public int Index() { return m_index; }
	
	public void AddArc(Arc a) {
		m_arcs.add(a);
	}
	
	public String Name() {
		return m_name;
	}
	
	public double Prob() { return m_prob; }

	public ArrayList<Arc> Arcs() {
		return m_arcs;
	}
}

class State implements Comparable<State> {
	private Location m_terminus;
	private double m_cost;
	private double m_probLeft;
	private double m_pathLength;
	private State m_parent;
	
	public State(Location seed) {
		m_cost = 0;
		m_terminus = seed;
		m_pathLength = 0;
		m_parent = null;
		m_probLeft = 1 - seed.Prob();
	}
	
	public State Extend(Arc a) {
		State result = new State(a.To());
		
		result.m_pathLength = m_pathLength + a.Length();
		result.m_cost = m_cost;
		result.m_parent = this;
		result.m_probLeft = m_probLeft;
		
		if (!HasVisited(a.To())) {
			result.m_probLeft -= a.To().Prob();
			result.m_cost += a.To().Prob() * result.m_pathLength;
		}
		
		return result;
	}
	
	public Location Terminus() { return m_terminus; }
	
	public boolean HasVisited(Location l) {
		for (State cursor = this; cursor != null; cursor = cursor.m_parent)
			if (cursor.m_terminus == l)
				return true;
		
		return false;
	}
	
	public double PenalizedCost() {
		return m_cost + m_pathLength*m_probLeft;
	}
	
	@Override
	public int compareTo(State o) {
		double myPenalizedCost = PenalizedCost();
		double otherPenalizedCost = o.PenalizedCost();
		
		if (myPenalizedCost < otherPenalizedCost)
			return -1;
		else if (myPenalizedCost > otherPenalizedCost)
			return 1;
		
		return 0;
	}
	
	public String toString() {
		return String.format("%s - Cost %.2f, Path Length %f, Prob Left %.2f, PCost %.2f", 
				m_terminus.Name(), m_cost, m_pathLength, m_probLeft, PenalizedCost());
	}

	public ArrayList<Arc> Arcs() {
		return m_terminus.Arcs();
	}

	public double Cost() {
		return m_cost;
	}

	public State Parent() {
		return m_parent;
	}

	public void DumpPath(PrintStream wr) {
		for (State cursor = this; cursor != null; cursor = cursor.m_parent)
			wr.println(cursor);
	}
}


public class FindSophieMain {
	
	public double FindMinCost(Location seedLocation, int locationCount) {
		PriorityQueue<State> q = new PriorityQueue<State>(); 

		State seedState = new State(seedLocation);
		for (Arc a : seedLocation.Arcs()) {
			State s = seedState.Extend(a);
			q.add(s);
		}
		
		SetSet[] done = new SetSet[locationCount];
		for (int i = 0; i < locationCount; i++)
			done[i] = new SetSet(locationCount);

		BitSet visited = new BitSet(locationCount);

		int popCount = 0;
		int processedCount = 0;
		double result = -1;
		
		for (;popCount < 3000 * 1000;) {
			State top = q.poll();
			
			if (top == null)
				break;
			
			ExtractVisited(top, visited);
			
			popCount++;
			
			if (popCount % 10000 == 0) {
				m_log.printf("\tPop %,d, Process %,d, Nd %,d - Top %s\n", popCount, processedCount, SetSet.m_nodeCount, top);
			}
			
			if (visited.cardinality() == (locationCount-1)) {
				top.DumpPath(m_log);
				result = top.Cost();
				break;
			}
			
			SetSet topDoneSets = done[top.Terminus().Index()];  
			if (topDoneSets.ContainsSuper(visited))
				continue;
			
			topDoneSets.Add(visited);
			processedCount++;
			
			for (Arc a : top.Arcs()) {
				State child = top.Extend(a);
				
				q.add(child);
			}
		}
		
		m_log.printf("Popped %d states\n", popCount);
		
		return result;
	}
	
	private void ExtractVisited(State top, BitSet visited) {
		visited.clear();
		Location terminus = top.Terminus();
		
		while (top.Parent() != null) {
			top = top.Parent();
			Location l = top.Terminus();
			
			if (l != terminus)
				visited.set(l.Index());
		}
	}

	public void Run(String inputFile) throws FileNotFoundException {
		Scanner rd = new Scanner(new FileInputStream(inputFile));
		int countPlaces = rd.nextInt();
		
		HashMap<String, Location> locations = new HashMap<String, Location>();
		Location seedLocation = null;
		int i;
		
		for (i = 0; i < countPlaces; i++)
		{
			String name = rd.next();
			double prob = rd.nextDouble();
			
			m_log.printf("Place : '%s' Prob %.2f\n", name, prob);
			
			Location l = new Location(name, prob, i);
			locations.put(name, l);

			if (seedLocation == null)
			{
				seedLocation = l;
				m_log.printf("Starting from %s\n", seedLocation.Name());
			}
		}
		
		int countArcs = rd.nextInt();
		for (i = 0; i < countArcs; i++)
		{
			String fromName = rd.next();
			String toName = rd.next();
			double time = rd.nextDouble();
			
			m_log.printf("%s - %s takes %f[s]\n", fromName, toName, time);
			
			Location from = locations.get(fromName);
			Location to = locations.get(toName);
			
			Arc a = new Arc(to, time);
			from.AddArc(a);
			
			a = new Arc(from, time);
			to.AddArc(a);
		}
		
		double minCost = FindMinCost(seedLocation, locations.size());
		
		System.out.printf("%.2f", minCost);
		m_log.printf("%.2f", minCost);
		
	}
	
	@SuppressWarnings("unused")
	private static void GenerateGrid() throws FileNotFoundException {
		PrintStream wr = new PrintStream("Grid.txt");
		
		int SideLength = 5;
		// Everything equal probability
		double prob = 1.0 / SideLength / SideLength;
		
		wr.printf("%d\n", SideLength * SideLength);
		
		for (int x = 1; x <= SideLength; x++) {
			for (int y = 1; y <= SideLength; y++) {
				String name = String.format("L%d%d", x, y);
				
				wr.printf("%s %f\n", name, prob);
			}
		}
		
		// Connect to near neighbours
		wr.printf("%d\n", 2 * SideLength * (SideLength-1));
		
		for (int x = 1; x <= SideLength; x++) {
			for (int y = 1; y <= SideLength; y++) {
				String fromName = String.format("L%d%d", x, y);

				if (x < SideLength) {
					// Go across to right
					String toName = String.format("L%d%d", x+1, y);
					
					wr.printf("%s %s %d\n", fromName, toName, 1);
				}
				
				if (y < SideLength) {
					// Go down
					String toName = String.format("L%d%d", x, y+1);
					
					wr.printf("%s %s %d\n", fromName, toName, 1);
				}
			}
		}
		
		
		wr.close();
	}
	
	public FindSophieMain() throws FileNotFoundException {
		m_log = new PrintStream("Log.txt");
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		assert args.length == 1;
		
		try
		{
			FindSophieMain inst = new FindSophieMain();

			inst.Run(args[0]);
			
//			GenerateGrid();
		}
		catch (Exception e)
		{
			System.out.printf("Error : %s", e.getMessage());
		}
	}
	
	private PrintStream m_log;

}
