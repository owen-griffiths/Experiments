package MyTrees;

/**
 *
 * @author Owen.Griffiths
 */
// Nodes are immutable
class Node<T extends Comparable<T>> {
    public static int height(Node n) {
        if (n == null) {
            return 0;
        } else {
            return n.m_height;
        }
    }
    
    @Override
    public String toString() {
        return nodeToString(this);
    }
    
    public static String nodeToString(Node n) {
        if (n == null) {
            return " X ";
        } else {
            return n.m_key.toString() + " [" + nodeToString(n.m_left) + "] [" + nodeToString(n.m_right) + "]";
        }
    }
    
    public static int getRebalanceCount() {
        return s_rebalanceCount;
    }
    
    public Node remove(T elem) {
        int comp = m_key.compareTo(elem);

        Node unbalanced;
        
        if (comp < 0) {
            unbalanced = new Node<>(m_key, m_left, m_right.remove(elem));
        } else if (comp > 0) {
            unbalanced = new Node<>(m_key, m_left.remove(elem), m_right);
        } else {
            unbalanced = removeRoot();
        }
        
        if (unbalanced != null) {
            Node balanced = unbalanced.rebalance();
            return balanced;
        } else {
            return null;
        }
    }
    
    private T findMax() {
        if (m_right == null) { 
            return m_key;
        }
        
        return (T) m_right.findMax();
    }
    
    private Node removeRoot() {
        // If < 2 children, easy to delete - just promote the single child
        if ((m_left == null) || (m_right == null)) {
            return removeSimple();
        }
        
        // Otherwise, temporarily remove the left's max (this is guarenteed not to have 2 children)
        // Then, can just overwrite this key with left's max
        T toPromote = (T) m_left.findMax();
        Node newLeft = m_left.remove(toPromote);
        
        Node result = new Node(toPromote, newLeft, m_right);
        return result;
    }
    
    // Removes this from the tree, assuming it has 0 or 1 children
    private Node removeSimple() {
        assert (m_left == null) || (m_right == null);
        
        if (m_left != null) {
            // right must be null, so the new tree is just the left tree
            return m_left;
        } else {
            // either right != null, and the new tree is just the right tree
            // or no children, so result is empty tree
            // either way, just just return m_right
            return m_right;
        }
    }
    
    private enum Path { LeftLeft, LeftRight, RightRight, RightLeft };
    
    private Path getLongPath() {
        if (height(m_left) > height(m_right)) {
            // first step to the left

            if (height(m_left.m_left) >= height(m_left.m_right)) {
                // Give priority to LeftLeft - only want the double rotation if m_left.m_right is 
                //  taller
                return Path.LeftLeft;
            } else {
                return Path.LeftRight;
            }
        } else {
            // first step to the right

            if (height(m_right.m_left) > height(m_right.m_right)) {
                return Path.RightLeft;
            } else {
                // Give priority to RightRight - only want the double rotation if m_right.m_left is 
                //  taller
                return Path.RightRight;
            }
        }
    }
    
    private Node<T> rebalance() {
        if (!needsRebalancing()) {
            return this;
        }
        
        s_rebalanceCount++;
        Node<T> result = null;
        
        switch (getLongPath()) {
            case RightRight: 
                result = rotateLeft();
                break;
            case LeftLeft:
                result = rotateRight();
                break;
            case RightLeft:
                result = rotateRightLeft();
                break;
            case LeftRight:
                result = rotateLeftRight();
                break;
        }
        
        // Only non empty trees can need rebalancing, so result can never be null
        assert result != null;
       
        return result;
    }
    
    private Node<T> rotateLeftRight() {
        Node<T> intermediate = new Node(m_key, m_left.rotateLeft(), m_right);
        return intermediate.rotateRight();
    }
    
    private Node<T> rotateRightLeft() {
        Node<T> intermediate = new Node(m_key, m_left, m_right.rotateRight());
        return intermediate.rotateLeft();
    }
    
    private Node<T> rotateRight() {
        Node<T> newThis = new Node<>(m_key, m_left.m_right, m_right);
        Node<T> newRoot = new Node<T>(m_left.m_key, m_left.m_left, newThis);
        
        return newRoot;
    }
    
    private Node<T> rotateLeft() {
        Node<T> newThis = new Node<>(m_key, m_left, m_right.m_left);
        Node<T> newRoot = new Node<T>(m_right.m_key, newThis, m_right.m_right);
        
        return newRoot;
    }
    
    private boolean needsRebalancing() {
        int leftHeight = height(m_left);
        int rightHeight = height(m_right);
        
        int diff = Math.abs(leftHeight - rightHeight);

        assert(diff <= 2);
        return diff > 1;
    }
    

    // PRE  : !Node.contains(root, elem)
    // POST : Node.contains(root, elem)
    public static <T extends Comparable<T>> Node<T> add(Node<T> root, T elem) {
        if (root == null) {
            return new Node<>(elem);
        }
        
        int comp = root.m_key.compareTo(elem);
        assert comp != 0;

        Node intermediate;
        
        if (comp < 0) {
            intermediate = new Node<>(root.m_key, root.m_left, Node.add(root.m_right, elem));
        } else { // can't be equal, so root must be greater than elem
            intermediate = new Node<>(root.m_key, Node.add(root.m_left, elem), root.m_right);
        }
        
        Node balanced = intermediate.rebalance();
        
        return balanced;
    }
    
    public static <T extends Comparable<T>> int getCount(Node<T> root) {
        if (root == null) { 
            return 0;
        }
        
        return 1 + getCount(root.m_left) + getCount(root.m_right);
    }
    
    public static <T extends Comparable<T>> boolean contains(Node<T> root, T elem) {
        if (root == null) {
            return false;
        }
        
        int comp = root.m_key.compareTo(elem);
        
        if (comp < 0) {
            return contains(root.m_right, elem);
        } else if (comp > 0) {
            return contains(root.m_left, elem);
        } else {
            return true;
        }
    }
    
    public boolean isValid() {
        int heightDiff = Math.abs(height(m_left) - height(m_right));
        if (heightDiff > 1) {
            return false;
        }
        if ((m_left != null) && !m_left.isValid()) {
            return false;
        }
        
        return (m_right == null) || m_right.isValid();
    }

    // Makes a tree with 1 elem, i.e. key
    private Node(T key) {
        m_key = key;
        m_height = 1;
    }

    // Make a tree with key at root, and left and right rub trees
    // PRE  : (left == null) || (left.getMax() < key)
    //        (right == null) || (right.getMin() > key)
    // This doesn't need to be a balanced AVL tree, as unbalanced intermediate nodes 
    //  are created before being fixed.
    private Node(T key, Node left, Node right) {
        m_key = key;
        m_left = left;
        m_right = right;
        m_height = 1 + Math.max(height(left), height(right));
    }

    private T m_key;
    
    private int m_height;
    
    private Node m_left;
    private Node m_right;
    
    private static int s_rebalanceCount;
}

public class AvlTree<T extends Comparable<T>> {
    public AvlTree() {
        m_root = null;
    }
    
    public AvlTree add(T elem) {
        if (!contains(elem)) {
            return new AvlTree(Node.add(m_root, elem));
        }
        return this;
    }
    
    public AvlTree remove(T elem) {
        if (contains(elem)) {
            return new AvlTree(m_root.remove(elem));
        }
        return this;
    }
    
    public boolean contains(T elem) {
        return Node.contains(m_root, elem);
    }
    
    public int getHeight() {
        return Node.height(m_root);
    }
    
    public int getCount() { 
        return Node.getCount(m_root);
    }
    
    public boolean isValid() {
        return (m_root == null) || m_root.isValid();
    }
    
    @Override
    public String toString() {
        return Node.nodeToString(m_root);
    }
    
    public static int getRebalanceCount() {
        return Node.getRebalanceCount();
    }

    private AvlTree(Node root) {
        m_root = root;
    }
    
    private Node m_root = null;
}
