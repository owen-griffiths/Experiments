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
		return String.format("%s - %s : %,d", m_input.Name(), m_output.Name(), m_price);
	}
	
	public Compound Output() { return m_output; }

	public Compound Input() { return m_input; }
	
	public int Price() { return m_price; }
	
	public int Number() {
		return m_number;
	}
	
	private String m_name;
	private Compound m_input;
	private Compound m_output;
	private int m_price;
	private int m_number;

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
		
		assert countMachines == m_machinesByProducer.length;
		
		m_remainingConsumers = new long[countMachines+1];
		m_remainingProducers = new long[countMachines + 1];
		
		for (int i = countMachines-1; i >= 0; i--) {
			m_remainingConsumers[i] = m_remainingConsumers[i + 1] | m_machinesByProducer[i].Input().Mask();
			m_remainingProducers[i] = m_remainingProducers[i + 1] | m_machinesByProducer[i].Output().Mask();
		}
	}
	
	private static boolean IsConnected(int[][] minCostMatrix) {
		for (int[] row : minCostMatrix)
			for (int cell : row)
				if (cell == Integer.MAX_VALUE)
					return false;
		
		return true;
	}
	
	class State implements Comparable<State> {
		public State() {
			m_connectivity = new long[m_compounds.length];
			// Even with no machines we can get from each compound to itself
			for (Compound c : m_compounds)
				m_connectivity[c.Index()] = c.Mask();
			
			m_cost = 0;
			m_penalty = 0;
			m_iNextMachine = 0;	
		}

		// newMachine can be null
		public State(State parent, long[] connectivity, Machine newMachine) {
			m_connectivity = connectivity;
			m_parent = parent;
			
			m_cost = parent.m_cost;
			m_consumed = parent.m_consumed;
			m_produced = parent.m_produced;
			
			m_machineAdded = newMachine;

			if (newMachine != null) {
				m_cost += newMachine.Price();
				m_consumed |= newMachine.Input().Mask();
				m_produced |= newMachine.Output().Mask();
			}
			
			m_iNextMachine = parent.m_iNextMachine + 1;
			
			int consumerPenalty = 0;
			int producerPenalty = 0;
			
			for (Compound c : m_compounds) {
				if ((m_consumed & c.Mask()) == 0)
					consumerPenalty += c.MinConsumerCost();
				
				if ((m_produced & c.Mask()) == 0)
					producerPenalty += c.MinProducerCost();
			}
			
			m_penalty = Math.max(producerPenalty, consumerPenalty);
		}
		
		private long[] m_connectivity;
		private long m_consumed;
		private long m_produced;
		private int m_cost;			
		private int m_iNextMachine;
		private int m_penalty;
		private Machine m_machineAdded;
		
		private State m_parent;

		public int compareTo(State o) {
			return Util.CompareInt(m_cost + m_penalty, o.m_cost + o.m_penalty);
		}
		
		public String toString() {
			return String.format("%s; %,d; %s", m_machines.toString(), m_cost, Arrays.toString(m_connectivity));
		}
		
		public void AddChildren(PriorityQueue<State> queue) {
			if (m_iNextMachine >= m_machinesByProducer.length)
				return;
			
			Machine next = m_machinesByProducer[m_iNextMachine];
			State child;
			
			// See if we can skip this machine
			long availableProducers = m_produced | m_remainingProducers[m_iNextMachine + 1];
			long availableConsumers = m_consumed | m_remainingConsumers[m_iNextMachine + 1];
			if ((availableProducers == m_fullMask) && (availableConsumers == m_fullMask))
			{			
				child = new State(this, m_connectivity, null);
				queue.add(child);
				m_maxCostInQueue = Math.max(m_maxCostInQueue, child.TotalCost());
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
				m_maxCostInQueue = Math.max(m_maxCostInQueue, child.TotalCost());
			}
		}

		private int TotalCost() { return m_cost + m_penalty; }

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

		public int  Cost() { return m_cost; }

		public int Penalty() { return m_penalty; }
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
//				s_log.printf("PopCount %,d : Top Cost = %,d; Penalty = %,d; \nQueue Size = %,d; #Invalid = %,d; Mem %,d[MB]\n", popCount, 
//						top.Cost() + top.Penalty(), top.Penalty(), 
//						q.size(), m_countInvalidStates, usedMem);
//				s_log.printf("Max Q Cost %,d\n\n", m_maxCostInQueue);
//				s_log.flush();
//			}
			
			if (q.size() > 1000 * 1000) {
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
		
		for (Compound c : m_compounds)
			m_fullMask = m_fullMask | c.Mask();
		
		Result result = BreathFirst();
		
		System.out.println(result.TotalPrice());
		System.out.println(result);

		int[][] connectivity = FloydWarshall(result.RequiredMachines());
		
		for (int[] row : connectivity) {
			s_log.println(Arrays.toString(row));
		}
		
		assert IsConnected(connectivity);
	}
	
	public int[][] FloydWarshall(Machine[] machines) {

		int n = m_compounds.length;
		
		int[][] result = new int[n][];
		for (int i = 0; i < n; i++) {
			result[i] = new int[n];
			Arrays.fill(result[i], Integer.MAX_VALUE / 2);
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
	
	// If you are up to Machine i, records which Compounds are consumed by this and subsequent machines 
	private long[] m_remainingConsumers;
	private long[] m_remainingProducers;
	
	private int m_countInvalidStates;
	private int m_maxCostInQueue;
	
	private static PrintWriter s_log;
	
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
