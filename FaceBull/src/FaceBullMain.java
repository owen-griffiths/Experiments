import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Scanner;

class Util {
	public static int CompareInt(int l, int r) {
		if (l < r)
			return -1;
		if (l == r)
			return 0;
		return 1;
	}
}
class Machine implements Comparable<Machine>
{
	public Machine(String name, Compound input, Compound output, int price) {
		m_name = name;
		m_input = input;
		m_output = output;
		m_price = price;
	
		String resultAsStr = m_name.substring(1);
		m_number = Integer.parseInt(resultAsStr);
	}
	
	public String toString() {
		return String.format("%S (%s - %s) : $%,d", m_name, m_input.Name(), m_output.Name(), m_price);
	}
	
	public Compound Output() { return m_output; }

	public Compound Input() { return m_input; }
	
	public int Price() { return m_price; }
	
	public int Number() {
		return m_number;
	}
	
	public String Name() { return m_name; }
	
	public boolean[][] RemainingConnectivity()
	{
		return m_remainingConnectivity;
	}

	public void SetRemainingConnectivity(int[][] connectivity)
	{
		m_remainingConnectivity = new boolean[connectivity.length][];
		for (int i = 0; i < connectivity.length; i++)
		{
			m_remainingConnectivity[i] = new boolean[connectivity.length];
			
			for (int j = 0; j < connectivity.length; j++)
			{
				m_remainingConnectivity[i][j] = (connectivity[i][j] != FaceBullMain.NOT_CONNECTED); 
			}
		}
	}
	
	private String m_name;
	private Compound m_input;
	private Compound m_output;
	private int m_price;
	private int m_number;
	private boolean[][] m_remainingConnectivity;	// Floyd Warshall connectivity of all machines left after this one

	public int compareTo(Machine o) {
		return Util.CompareInt(m_number, o.m_number);
	}
}


class Compound {
	public Compound(String name, int index) {
		m_name = name;
		m_index = index;
		m_mask = 1L << m_index;
		m_consumers = new Machine[0];
		m_producers = new Machine[0];
		
		m_minProducerCost = Integer.MAX_VALUE;
		m_minConsumerCost = Integer.MAX_VALUE;
	}
	
	public String Name() { return m_name; }

	public void AddConsumer(Machine m) {
		assert m.Input() == this;
		m_consumers = Append(m_consumers, m);
		
		m_minConsumerCost = Math.min(m_minConsumerCost, m.Price());
	}

	public void AddProducer(Machine m) {
		assert m.Output() == this;
		m_producers = Append(m_producers, m);
		
		m_minProducerCost = Math.min(m_minProducerCost, m.Price());
	}
	
	public int Index() { return m_index; }
	
	public Machine[] Consumers() { return m_consumers; }
	
	public Machine[] Producers() { return m_producers; }
	
	private Machine[] Append(Machine[] src, Machine newEntry) {
		Machine[] result = Arrays.copyOf(src, src.length + 1);
		result[src.length] = newEntry;
		return result;
	}
	
	private String m_name;
	private int m_index;
	
	private Machine[] m_consumers;
	private Machine[] m_producers;
	
	private int m_minProducerCost;
	private int m_minConsumerCost;
	
	private long m_mask;
	
	public long Mask() { return m_mask;	}

	public int MinProducerCost() {
		return m_minProducerCost;
	}

	public long ConsumersMask() {
		long result = 0;
		for (Machine m : m_producers)
			result |= m.Input().Mask();
		
		return result;
	}

	public int MinConsumerCost() { return m_minConsumerCost; }
}

class Result {
	public Result(Machine[] machines) {
		m_machines = machines;
		Arrays.sort(m_machines);
		
		for (Machine m : machines) 
			m_totalPrice += m.Price();
	}
	
	public int TotalPrice() { return m_totalPrice; }
	
	public String toString() {
		String result = "";
		for (Machine m : m_machines) { 
			result += m.Number();
			result += " ";
		}
		return result;
	}
	
	private int m_totalPrice;
	private Machine[] m_machines;
	public Machine[] RequiredMachines() { return m_machines; }
}


public class FaceBullMain {

	public FaceBullMain(String inputFile) throws IOException {
		Scanner rd = new Scanner(new FileInputStream(inputFile));

		HashMap<String, Compound> nameToCompound = new HashMap<String, Compound>();
		m_machines = new HashMap<String, Machine>(); 

		for (;;)
		{
			try
			{
				String machineId = rd.next();
				String fromName = rd.next();
				String toName = rd.next();
				int price = rd.nextInt();
				
				Compound from = FindCompound(fromName, nameToCompound);
				Compound to = FindCompound(toName, nameToCompound);
				
				Machine m = new Machine(machineId, from, to, price);
				from.AddConsumer(m);
				to.AddProducer(m);
				
				m_machines.put(machineId, m);				
			}
			catch (Exception e) 
			{
				break;
			}
		}

		m_machinesByProducer = new Machine[m_machines.size()];
		int countMachines = 0;
		
		m_compounds = new Compound[nameToCompound.size()];
		for (Compound c : nameToCompound.values()) {
			m_compounds[c.Index()] = c;
			for (Machine m : c.Producers()) {
				m_machinesByProducer[countMachines] = m;
				countMachines++;
			}
		}
		
		SetUpRemainingMachineConnectivity();
		
		assert countMachines == m_machinesByProducer.length;
	}
	
	private void SetUpRemainingMachineConnectivity()
	{
		ArrayList<Machine> remainingMachines = new ArrayList<Machine>();
		for (Machine m : m_machinesByProducer)
			remainingMachines.add(m);
		
		while (remainingMachines.size() > 0)
		{
			Machine head = remainingMachines.remove(0);
			
			int[][] connectivity = FloydWarshall(remainingMachines.toArray(new Machine[0]));
			head.SetRemainingConnectivity(connectivity);
		}
	}
	
	private static boolean IsConnected(int[][] minCostMatrix) {
		for (int[] row : minCostMatrix)
			for (int cell : row)
				if (cell == Integer.MAX_VALUE)
					return false;
		
		return true;
	}
	
	
	private Result BreathFirst() {
		PriorityQueue<State> q = new PriorityQueue<State>();
		q.add(new State());
		Runtime runtime = Runtime.getRuntime();
		
		s_log.printf("Mem memory = %,d [MB]\n", runtime.maxMemory() / 1024 / 1024);
		
		int popCount = 0;
		
		for (;;) {
			State top = q.poll();
			if (top == null)
				break;
			
			popCount++;
			
//			if ((popCount % 100) == 0) {
//				long usedMem = runtime.totalMemory() - runtime.freeMemory();
//				usedMem /= 1024 * 1024;
//				s_log.printf("PopCount %,d : Top Cost = %,d; Penalty = %,d; \nQueue Size = %,d; #Invalid = %,d; Mem %,d[MB]\n\n", popCount, 
//						top.Cost() + top.Penalty(), top.Penalty(), 
//						q.size(), m_countInvalidStates, usedMem);
//				s_log.flush();
//			}
			
			if (q.size() > 1000 * 1000) {
				s_log.println("\n\nQueue Too Large****\n");
				
				while (true)
				{
					for (int i = 1; i < 1000; i++)
						q.poll();
					
					State t = q.poll();
					if (t == null)
						return null;
					
					s_log.println(t.TotalCost());
				}
			}
			
			if (top.IsFullyConnected())
				return new Result(top.MachinesUsed());
			
			top.AddChildren(q);
		}

		assert false;
		return null;
	}
	
	public void Run() {
		s_log.printf("%d machines and %d compounds\n", m_machines.size(), m_compounds.length);
		long start = System.currentTimeMillis(); 
			
		for (Compound c : m_compounds)
		{
			m_fullMask = m_fullMask | c.Mask();
			m_sumMinProducerCost += c.MinProducerCost();
			m_sumMinConsumerCost += c.MinConsumerCost();
		}
		
//		new State().TryPath("M6 M15 M37 M47 M67 M79 M91 M96 M108 M123 M134 M147 M164 M169 M193 M197 M216 M219 M234 M255 ");
		
		Result result = BreathFirst();
		
		System.out.println(result.TotalPrice());
		System.out.println(result);
		
		s_log.printf("Took %,d[ms]\n\n", System.currentTimeMillis() - start);

		int[][] connectivity = FloydWarshall(result.RequiredMachines());
		
		for (int[] row : connectivity) {
			s_log.println(Arrays.toString(row));
		}
		
		assert IsConnected(connectivity);
	}
	

	public static final int NOT_CONNECTED = Integer.MAX_VALUE / 2;
	
	public int[][] FloydWarshall(Machine[] machines) {

		int n = m_compounds.length;
		
		int[][] result = new int[n][];
		for (int i = 0; i < n; i++) {
			result[i] = new int[n];
			Arrays.fill(result[i], NOT_CONNECTED);
			result[i][i] = 0;
		}
		
		for (Machine m : machines) {
			result[m.Input().Index()][m.Output().Index()] = m.Price();
		}

		for (int k = 0; k < n; k++)
			for (int i = 0; i < n; i++)
				for (int j = 0; j < n; j++) {
					int bestByK = result[i][k] + result[k][j];
					result[i][j] = Math.min(result[i][j], bestByK);
				}
		
		return result;
	}
	
	private Compound FindCompound(String name, HashMap<String, Compound> nameToCompound) {
		Compound result = nameToCompound.get(name);
		if (result == null) {
			result = new Compound(name, nameToCompound.size());
			nameToCompound.put(name, result);
		}
		return result;
	}
	
	private Compound[] m_compounds;
	private HashMap<String, Machine> m_machines;
	
	private Machine[] m_machinesByProducer;
	
	private long m_fullMask;
	
	private int m_sumMinConsumerCost;
	private int m_sumMinProducerCost;
	
	private int m_countInvalidStates;
	
	private static PrintWriter s_log;
	
	
	class State implements Comparable<State> {
		public void TryPath(String path) {
			State s = new State();
			System.out.printf("Seed state %s\n\n", s.toString());
			
			for (Machine m : m_machinesByProducer)
			{
				boolean isInPath = path.contains(m.Name() + " ");
				
				if (isInPath)
				{
					if (!IsNewConnection(s.m_connectivity, m))
					{
						System.out.printf("%s unexpectedly not new connection", m.toString());
						return;
					}
					
					s = new State(s, UpdateConnectivity(s.m_connectivity, m), m);
					System.out.printf("Added %s\n", m.toString());
					System.out.printf("New State %s\n\n", s.toString());
				}
				else
				{
					if (!IsRedundant(m, s.m_connectivity, m.RemainingConnectivity()))
					{
						System.out.printf("%s unexpectedly not redundant", m.toString());
						
						int[][] usedConn = FloydWarshall(s.MachinesUsed());
						System.out.println("Used Machines Conn");
						for (Compound c : m_compounds)
						{
							System.out.print(c.Name() + " -> ");
							for (Compound o : m_compounds)
								if (usedConn[c.Index()][o.Index()] != NOT_CONNECTED)
									System.out.print(o.Name() + " ");
							
							System.out.println();
						}
						
						
						System.out.println("Remaining Machines Conn");
						for (Compound c : m_compounds)
						{
							System.out.print(c.Name() + " -> ");
							for (Compound o : m_compounds)
								if (m.RemainingConnectivity()[c.Index()][o.Index()])
									System.out.print(o.Name() + " ");
							
							System.out.println();
						}
						
						
						return;
					}
					
					s = new State(s, s.m_connectivity, null);

					System.out.printf("Skipped %s\n", m.toString());
					System.out.printf("New State %s\n\n", s.toString());
				}
			}
		}

		public State() {
			m_connectivity = new long[m_compounds.length];
			// Even with no machines we can get from each compound to itself
			for (Compound c : m_compounds)
				m_connectivity[c.Index()] = c.Mask();
			
			m_rawCost = 0;
			m_penalizedCost = 0;
			m_consumerPenalty = m_sumMinConsumerCost;
			m_producerPenalty = m_sumMinProducerCost;
			
			m_iNextMachine = 0;	
		}

		// newMachine can be null
		public State(State parent, long[] connectivity, Machine newMachine) {
			m_connectivity = connectivity;
			m_parent = parent;
			
			m_rawCost = parent.m_rawCost;
			m_consumed = parent.m_consumed;
			m_produced = parent.m_produced;
			
			m_consumerPenalty = parent.m_consumerPenalty;
			m_producerPenalty = parent.m_producerPenalty;
			
			m_machineAdded = newMachine;

			if (newMachine != null) {
				m_rawCost += newMachine.Price();
				m_consumed |= newMachine.Input().Mask();
				if (m_consumed != parent.m_consumed)
					m_consumerPenalty -= newMachine.Input().MinConsumerCost();
				
				m_produced |= newMachine.Output().Mask();
				if (m_produced != parent.m_produced)
					m_producerPenalty -= newMachine.Output().MinProducerCost();
			}
			
			m_iNextMachine = parent.m_iNextMachine + 1;
			m_penalizedCost = m_rawCost + Math.max(m_consumerPenalty, m_producerPenalty);
		}
		
		private long[] m_connectivity;
		private long m_consumed;
		private long m_produced;
		private int m_rawCost;
		private int m_penalizedCost;
		private int m_iNextMachine;
		private int m_consumerPenalty;
		private int m_producerPenalty;
		private Machine m_machineAdded;
		
		private State m_parent;

		public int compareTo(State o) {
			return Util.CompareInt(m_penalizedCost, o.m_penalizedCost);
		}
		
		public String toString() {
			String result = String.format("RawCost : $%,d; Consumer Penalty : $%,d; Producer Penalty : $%,d; Penalized Cost : $%,d", m_rawCost, m_consumerPenalty, m_producerPenalty, m_penalizedCost);
			if (IsFullyConnected())
				return "SOLUTION " + result;
			
			return result;
		}
		
		public void AddChildren(PriorityQueue<State> queue) {
			if (m_iNextMachine >= m_machinesByProducer.length) {
				return;
			}
			
			Machine next = m_machinesByProducer[m_iNextMachine];
			State child;
			
			// See if we can skip this machine
			if (IsRedundant(next, m_connectivity, next.RemainingConnectivity()))
			{			
				child = new State(this, m_connectivity, null);
				queue.add(child);
			}
			else {
				m_countInvalidStates++;
			}
			
			// Create a child state with this machine
			// This cannot become invalid, as we are not skipping anything
			if (IsNewConnection(m_connectivity, next)) {
				long[] newCon = UpdateConnectivity(m_connectivity, next);
				child = new State(this, newCon, next);
				queue.add(child);
			}
		}

		private boolean IsRedundant(Machine m, long[] usedConnectivity, boolean[][] remainingConnectivity) {
			boolean[] hasReached = new boolean[m_compounds.length];
			long[] totalCon = usedConnectivity.clone();
			for (Compound f : m_compounds)
				for (Compound t : m_compounds)
					if (remainingConnectivity[f.Index()][t.Index()])
						totalCon[f.Index()] |= t.Mask();
			
			return RecurseCanReach(hasReached, m.Input(), m.Output(), totalCon);
		}

		private boolean RecurseCanReach(boolean[] hasReached, Compound reached,
				Compound target, long[] totalCon) {
			
			if (reached == target)
				return true;
			
			if (hasReached[reached.Index()])
				return false;
			
			hasReached[reached.Index()] = true;
			
			for (Compound c : m_compounds)
				if ((totalCon[reached.Index()] & c.Mask()) != 0)
					if (RecurseCanReach(hasReached, c, target, totalCon))
						return true;
			
			return false;
		}

		private int TotalCost() { return m_penalizedCost; }

		private boolean IsNewConnection(long[] connectivity, Machine p) {
			long existingOutputsMask = connectivity[p.Input().Index()];
			long thisOutputMask = existingOutputsMask & p.Output().Mask(); 
			return thisOutputMask == 0;
		}

		private long[] UpdateConnectivity(long[] connectivity, Machine m) {
			long[] result = connectivity.clone();
			
			long possibleOutputs = connectivity[m.Output().Index()];
			
			for (int iCompound = 0; iCompound < m_compounds.length; iCompound++) {
				if ((connectivity[iCompound] & m.Input().Mask()) != 0) {
					// Can go from c to m.Input
					// Thus, with m can go from c to anywhere m.Output can go
					result[iCompound] = result[iCompound] | possibleOutputs;
				}
			}
			
			return result;
		}

		public boolean IsFullyConnected() {
			for (long l : m_connectivity)
				if (l != m_fullMask)
					return false;
			return true;
		}

		public Machine[] MachinesUsed() {
			ArrayList<Machine> allUsed = new ArrayList<Machine>();
			
			for (State cursor = this; cursor != null; cursor = cursor.m_parent) {
				Machine m = cursor.MachineAdded();
				if (m != null)
					allUsed.add(m);
			}
			
			return allUsed.toArray(new Machine[allUsed.size()]);
		}

		private Machine MachineAdded() { return m_machineAdded; }

		public int  Cost() { return m_rawCost; }

		public int Penalty() { return m_penalizedCost - m_rawCost; }
	}

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		assert args.length == 1;
		
		try
		{
			s_log = new PrintWriter("Log.txt");
			
			FaceBullMain inst = new FaceBullMain(args[0]);
			
			inst.Run();
			
			s_log.close();
		}
		catch (Exception e)
		{
			s_log.close();
			throw new RuntimeException(e);
		}
	}
}
