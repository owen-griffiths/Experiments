import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.BitSet;

class Node {
	private char m_letter;
	private ArrayList<Node> m_children;
	private String m_word;
	
	private long m_doneConsumedCounts;
	
	
	private Node(char letter) {
		m_letter = letter;
		m_children = new ArrayList<Node>();
	}
	
	private void RecurseAdd(String word, int countDone)	{
		if (countDone == word.length())
		{
			m_word = word;
		}
		else
		{
			char nextLetter = word.charAt(countDone);

			Node child = null;
			for (Node cursor : m_children)
				if (cursor.m_letter == nextLetter)
					child = cursor;

			if (child == null)
			{
				// Didn't find a match in my existing children
				// Need to add a new one
				child = new Node(nextLetter);
				m_children.add(child);
			}

			child.RecurseAdd(word, countDone + 1);
		}
	}
	
	public void Clear()
	{
		m_doneConsumedCounts = 0;
	}
	
	public boolean IsFirstHit(int consumedCount, ArrayList<Node> visited)
	{
		assert(consumedCount < 63);
		
		long mask = 1 << consumedCount;
		if ((m_doneConsumedCounts & mask) != 0)
			return false;
		
		if (m_doneConsumedCounts == 0)
			visited.add(this);
		
		m_doneConsumedCounts = m_doneConsumedCounts | mask;
		
		return true;
	}
	
	public static Node MakeEmptyTree() {
		Node result = new Node(' ');
		return result;
	}
	
	public void Add(String word) {
		RecurseAdd(word, 0);
	}
	
	public char Letter() {
		return m_letter;
	}
	
	public int CountWords()	{
		int result = 0;
		if (HasWord())
			result++;
		
		for (Node c : m_children)
			result += c.CountWords();
		
		return result;
	}
	
	public int CountNodes() {
		int result = 1;
		
		for (Node c : m_children)
			result += c.CountWords();
		
		return result;
	}

	public ArrayList<Node> Children() {
		return m_children;
	}

	public boolean HasWord() {
		return m_word != null;
	}

	public String Word() {
		return m_word;
	}

	public boolean IsFirstVisit() {
		return m_doneConsumedCounts == 0;
	}

	public Node ChildFor(char nextLetter) {
		for (Node c : m_children)
			if (c.m_letter == nextLetter)
				return c;
		return null;
	}
}

class State implements Comparable<State>
{
	private String m_word;
	private int m_countConsumed;
	private int m_cost;
	private Node m_node;
	
	// Make a seed
	public State(String word, Node validWords)
	{
		m_word = word;
		m_countConsumed = 0;
		m_cost = 0;
		m_node = validWords;
	}
	
	// Make a child state
	public State(State parent, int newCost, Node newNode, int newConsumed)
	{
		m_word = parent.m_word;
		m_cost = newCost;
		m_node = newNode;
		m_countConsumed = newConsumed;
	}
	
	public static void Enqueue(ArrayList<State>[] queue, State s)
	{
		queue[s.Cost()].add(s);
	}
	
	private static void CheckCandidate(State parent, int cost, Node n, int countConsumed, ArrayList<State> nextCosts, ArrayList<Node> visited)
	{
		if (n.IsFirstHit(countConsumed, visited))
		{
			State newState = new State(parent, cost, n, countConsumed);
			nextCosts.add(newState);
		}
	}
	
	public void Process(ArrayList<State> nextCosts, ArrayList<Node> visited)
	{
		// Consume the next character
		if (m_countConsumed < m_word.length())
		{
			char nextLetter = m_word.charAt(m_countConsumed);
			
			// Replace the current letter with another
			for (Node c : m_node.Children())
			{
				if (c.Letter() != nextLetter)
					CheckCandidate(this, m_cost + 1, c, m_countConsumed + 1, nextCosts, visited);
			}
			
			// Delete the next character, so stay at the same node with increased cost
			CheckCandidate(this, m_cost + 1, m_node, m_countConsumed + 1, nextCosts, visited);
		}

		// Insert extra character
		for (Node c : m_node.Children())
		{
			CheckCandidate(this, m_cost + 1, c, m_countConsumed, nextCosts, visited);
		}
	}

	public boolean HasReachedValidWord() {
		return m_node.HasWord() && (m_countConsumed == m_word.length());
	}

	public int Cost() {
		return m_cost;
	}

	@Override
	public int compareTo(State o) {
		if (m_cost < o.m_cost)
			return -1;
		else if (m_cost == o.m_cost)
			return 0;
		return 1;
	}

	public String ReachedWord() {
		return m_node.Word();
	}

	public Node ReachedNode() {
		return m_node;
	}

	public int ConsumedCount() {
		return m_countConsumed;
	}
}


public class WordsMain {
	private static PrintStream s_log;
	
	private static Node LoadValidWords(String wordFile) throws IOException
	{
		long start = System.currentTimeMillis();
		Node result = Node.MakeEmptyTree();
		
		BufferedReader rd = new BufferedReader(new FileReader(wordFile));
		
		for (;;)
		{
			String line = rd.readLine();
			if (line == null)
				break;

			line = line.trim();

			if (line.length() > 0)
			{
				result.Add(line);
			}
		}
		
		rd.close();
		
		s_log.printf("Loaded %1$d words.  %2$d Tree Nodes\n", result.CountWords(), result.CountNodes());
		s_log.printf("Took %1$d [ms]\n\n", System.currentTimeMillis() - start);
		return result;
	}
	
	private static int FindDistance(String sentance, Node validWords)
	{
		int result = 0;
		for (String s : sentance.split(" "))
		{
			String upperS = s.toUpperCase();
			int wordDist = FindWordDistance(upperS, validWords);
			
			result = Math.max(result, wordDist);
		}		
		
		return result;
	}
	
	// Returns is a solution has been found
	private static boolean MakeFreeMoves(State seed, ArrayList<State> result, String inputWord, int cost, ArrayList<Node> visited)
	{
		Node currentNode = seed.ReachedNode();
		result.add(seed);
	
		int wordLength = inputWord.length();
		for (int i = seed.ConsumedCount(); i < wordLength; i++)
		{
			char nextLetter = inputWord.charAt(i);
			
			Node nextNode = currentNode.ChildFor(nextLetter);
			
			if ((nextNode == null) || !nextNode.IsFirstHit(i+1, visited))
				return false;
			
			currentNode = nextNode;
			seed = new State(seed, cost, currentNode, i + 1);
			result.add(seed);
		}
		
		return seed.HasReachedValidWord();
	}

	private static ArrayList<State> ProcessQueue(ArrayList<State> isoCosts, ArrayList<Node> visited, String word)
	{
		ArrayList<State> nextCosts = new ArrayList<State>();
		ArrayList<State> buffer = new ArrayList<State>();
		
		for (State top : isoCosts)
		{
			top.Process(buffer, visited);
			for (State s: buffer)
			{
				if (MakeFreeMoves(s, nextCosts, word, s.Cost(), visited))
				{
					s_log.printf("\tTook %1$d Moves to get %2$s to %3$s\n", s.Cost(), word, s.ReachedWord());
					return null;
				}
			}
			
			nextCosts.addAll(buffer);
			buffer.clear();
		}
		
		s_log.printf("\t\t%1$d States In; %2$d States out.  %3$d Visited\n", isoCosts.size(), nextCosts.size(), visited.size());
		return nextCosts;
	}
	
	private static int FindWordDistance(String word, Node validWords)
	{
		ArrayList<State> isoCosts = new ArrayList<State>();
		State seed = new State(word, validWords);
		ArrayList<Node> visited = new ArrayList<Node>();
		if (MakeFreeMoves(seed, isoCosts, word, 0, visited))
		{
			s_log.printf("\tInput word %1$s is already valid\n", word);
			isoCosts = null;
		}

		int cost = 0;
		while (isoCosts != null)
		{
			cost++;
			isoCosts = ProcessQueue(isoCosts, visited, word);
		}
		
		for (Node n : visited)
			n.Clear();

		return cost;
	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String inputFile = args[0];
		String wordFile = "/var/tmp/twl06.txt";
		
		if (args.length > 1)
			wordFile = args[1];
		
		try
		{
			s_log  = new PrintStream("log.txt");
			
			Node validWords = LoadValidWords(wordFile);
			
			BufferedReader rd = new BufferedReader(new FileReader(inputFile));
			
			for (;;)
			{
				String line = rd.readLine();
				if (line == null)
					break;
				
				line = line.trim();
				if (line.length() > 0)
				{
					long start = System.currentTimeMillis();
					int distance = FindDistance(line, validWords);
					long takenMs = System.currentTimeMillis() - start;
					s_log.printf("Distance for '%1$s' = %2$d.  Took %3$d [ms]\n\n", line, distance, takenMs);
					System.out.println(distance);
				}
			}			
		}
		catch (IOException e)
		{
			System.out.println(e.getMessage());
		}

		
	}

}
