/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package MyTrees;

import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import sun.invoke.empty.Empty;

/**
 *
 * @author Owen.Griffiths
 */
public class AvlTreeTest {
    
    public AvlTreeTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        m_tree = new AvlTree<Integer>();
        m_ref = new TreeSet<>();
    }
    
    @After
    public void tearDown() {
    }
    
    @Test
    public void testLeftRotation() {
        int startRebalCount = AvlTree.getRebalanceCount();
        
        add(10);
        add(20);
        add(30);

        assertTreeMatches();
        assertEquals(2, m_tree.getHeight());
        assertEquals(1, AvlTree.getRebalanceCount() - startRebalCount);
    }
    
    @Test
    public void testRightRotation() {
        int startRebalCount = AvlTree.getRebalanceCount();
        
        add(30);
        add(20);
        add(10);

        assertTreeMatches();
        assertEquals(2, m_tree.getHeight());
        assertEquals(1, AvlTree.getRebalanceCount() - startRebalCount);
    }
    
    @Test
    public void testLeftRightRotation() {
        int startRebalCount = AvlTree.getRebalanceCount();
        
        add(30);
        add(10);
        add(20);

        assertTreeMatches();
        assertEquals(2, m_tree.getHeight());
        assertEquals(1, AvlTree.getRebalanceCount() - startRebalCount);
    }

    @Test
    public void testRightLeftRotation() {
        int startRebalCount = AvlTree.getRebalanceCount();
        
        add(10);
        add(30);
        add(20);

        assertTreeMatches();
        assertEquals(2, m_tree.getHeight());
        assertEquals(1, AvlTree.getRebalanceCount() - startRebalCount);
    }
    
    @Test
    public void deleteLeaf() {
        // Make tree:
        //      20
        //   10     30
        //              40

        add(20);
        add(10);
        add(30);
        add(40);
        
        // 10 is leaf, but will require rebalance
        int startRebal = AvlTree.getRebalanceCount();
        
        delete(10);
        
        assertTreeMatches();
        assertEquals(2, m_tree.getHeight());
        assertEquals(1, AvlTree.getRebalanceCount() - startRebal);
        
        delete(20);
        delete(40);
        delete(30);
        
        assertEquals(1, AvlTree.getRebalanceCount() - startRebal);
        assertTreeMatches();
        assertEquals(0, m_tree.getHeight());
    }
    
    @Test
    public void testParent() {
        add(50);
        add(10);
        add(100);
        add(5);
        add(80);
        add(120);
        add(90);

        assertEquals(4, m_tree.getHeight());
        
        delete(100);
        
        assertEquals(3, m_tree.getHeight());
        assertTreeMatches();
    }
    
    @Test
    public void testRemoveSmallLeft() {
        add(50);
        
        add(40);
        add(100);
        
        add(30);
        add(80);
        add(120);
        
        add(70);
        add(140);

        assertEquals(8, m_tree.getCount());
        
        delete(30);
        
        assertEquals(7, m_tree.getCount());
        assertTrue(m_tree.isValid());
        
    }
    
    @Test
    public void testRemoveSmallRight() {
        add(47);
        
        add(42);
        add(51);
        
        add(41);
        add(45);
        add(52);
        
        add(40);
        add(46);
        
        assertEquals(4, m_tree.getHeight());
        
        delete(51);
        
        assertTrue(m_tree.isValid());
    }

    private void checkPersistentRemove(int x) {
        AvlTree<Integer> next = m_tree.remove(new Integer(x));
        
        assertTreeMatches();
        
        m_tree = next;
        m_ref.remove(new Integer(x));
        
        assertTreeMatches();
    }

    private void checkPersistentAdd(int x) {
        AvlTree<Integer> next = m_tree.add(new Integer(x));
        
        assertTreeMatches();
        
        m_tree = next;
        m_ref.add(new Integer(x));
        
        assertTreeMatches();
    }
    
    @Test
    public void testPersistent() {
        for (int i = 10; i < 100; i+=5) {
            add(i);
        }

        checkPersistentRemove(50);
        checkPersistentAdd(75);
        checkPersistentAdd(55);
        checkPersistentRemove(40);
        checkPersistentRemove(10);

        checkPersistentRemove(55);
        checkPersistentRemove(100);
        checkPersistentAdd(77);
        checkPersistentAdd(23);
        checkPersistentRemove(75);
        checkPersistentAdd(68);
        checkPersistentAdd(73);
        checkPersistentRemove(80);
    }
    
    @Test
    public void testRandomRemove() {
        System.out.println("\nRandomRemove");

        final int numRemoves = 10000;
        final int addressSpace = 5000;
        
        for (int i = 0; i < addressSpace; i++) {
            add(i);
        }
        
        Random rand = new Random(506575);

        int totalRebals = -AvlTree.getRebalanceCount();
        int maxRebals = 0;
        
        for (int i = 0; i < numRemoves; i++) {
            int countRebals = -AvlTree.getRebalanceCount();
            delete(rand.nextInt(addressSpace));
            assertTrue(m_tree.isValid());
            countRebals += AvlTree.getRebalanceCount();
            
            maxRebals = Math.max(maxRebals, countRebals);

            assertTreeMatches();
        }
        
        totalRebals += AvlTree.getRebalanceCount();
        int countDeletes = addressSpace - m_tree.getCount();
        
        System.out.printf("%d rebalances in removing %d elements\n", totalRebals, countDeletes);
        System.out.printf("Max Rebals for single delete = %d\n", maxRebals);
        System.out.println();
    }
    
    /**
     * Test of add method, of class AvlTree.
     */
    @Test
    public void testAddRebalances() {
        final int randSeed = 1324;
        final int addCount = 100 * 1000;
        final int addressSpace = 50 * 1000;
        
        System.out.println("randomAdd");
        
        long myStart = System.nanoTime();
        java.util.Random rand = new java.util.Random(randSeed);
        
        for (int i = 0; i < addCount; i++) {
            Integer nextElem = rand.nextInt(addressSpace);
            int rebalCount = -AvlTree.getRebalanceCount();
            m_tree = m_tree.add(nextElem);
            rebalCount += AvlTree.getRebalanceCount();
            assertTrue(rebalCount <= 1);
        }
        long myTaken = System.nanoTime() - myStart;
        
        rand = new java.util.Random(randSeed);
        
        long refStart = System.nanoTime();
        for (int i = 0; i < addCount; i++) {
            Integer nextElem = rand.nextInt(addressSpace);
            m_ref.add(nextElem);
        }
        long refTaken = System.nanoTime() - refStart;

        assertTreeMatches();
        
        System.out.printf("Tree count %d\nRef count %d\nHeight %d\n", 
                m_tree.getCount(), 
                m_ref.size(),
                m_tree.getHeight());
        System.out.printf("Mine took: %,d[ns]\n", myTaken);
        System.out.printf(" Ref took: %,d[ns]\n", refTaken);
        System.out.printf("Avg rebalances = %f / insert\n", (double) AvlTree.getRebalanceCount() / m_tree.getCount());
        System.out.printf("#Rebalances    = %,d\n", AvlTree.getRebalanceCount());
    }

    private void add(int x) {
        m_tree = m_tree.add(new Integer(x));
        m_ref.add(new Integer(x));
    }
    
    private void delete(int x) {
        m_tree = m_tree.remove(new Integer(x));
        m_ref.remove(new Integer(x));
    }
    
    private void assertTreeMatches() {
        assertEquals(m_ref.size(), m_tree.getCount());
        for (Integer i : m_ref) {
            assertTrue(m_tree.contains(i));
        }
        double heightLimit = 1.44 * Math.ceil(Math.log(m_tree.getCount() + 1) / Math.log(2));
        assertTrue(m_tree.getHeight() <= heightLimit);
    }
    private AvlTree<Integer> m_tree;
    private SortedSet<Integer> m_ref;
}
