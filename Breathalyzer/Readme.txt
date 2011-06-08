At it's heart, this is a simple job.  Basically, for each letter in an input word, try making the allowed changes (inserting, deleting and replacing a letter).  This gives you a new candidate word, which we can check for in our dictionary.  If it is not in the dictionary, we can recursively try our moves on each letter in this candidate.  This allows us to enumerate all possible words with 1 change, then 2 changes and so on.  Here is a diagram showing the an abbreviated search in tree form.  The starting input is "csn".


Figure 1 : Brute Force Search

However, this brute force search rapidly becomes infeasible.  However, we will be spending lots of time traversing branches of the search space that can never yield any answers.  The capitalized start of each word is common to all nodes in the tree rooted at that node.  If there are no words in the dictionary that start with a given prefix (ZB for example), that whole tree is wasted effort.

We could implement the prefix lookup as a binary chop on the dictionary.  However, even with this culling, a lot of candidates are generated.  Thus, even Log(N) lookup is slow.  Instead, I represent the dictionary as a tree.  Each arc represents a letter, and each node represents the letter sequence consisting of the arcs from the root.  Sequences that are valid words are marked, thus, as we traverse the tree we know in constant time whether we have reached a valid word.


Figure 2 : Tree storing cap, cat, cape and cut

As we search, we have a set of states in a priority queue.  As each is popped, it is extended (by performing the allowed edits) into a set of child states.  Each of these is then added to the queue.

A state consists of : 
	done : the amount of the input we have consumed
	nd : the node we have reached in the tree
	dist : how many edits we have performed so far.  This how the priority queue is sorted.

Our three modifications are then represented by generating children states:
Insertion : Move to each child node on the tree, without consuming any more input.  This effectively inserts the letters for the arcs to children into the input word.
Deletion : Consume another input letter, but remain on the same node in the tree.  This deletes the next letter in the input word.
Replacement : Consume another input letter, but move to children nodes through arcs != the next letter.  This replaces the input letter with the arc letter.

For example, say we are matching the input "can" (which is not in our example dictionary).  We pop of a state (nd = "c", done = 1, dist = 0).  When processed this yields the children:
(No Edit) : 	(nd = "ca", done = 2, dist = 0)

(Insertion) : 	(nd = "ca", done = 1, dist = 1)
				(nd = "cu", done = 1, dist = 1)
(Deletion) : 	(nd = "c", done = 2, dist = 1)
(Replacement) : (nd = "cu", done = 2, dist = 1)

There can only ever be 1 child state via the "No Edit" move, which will have the same cost as the parent.

All edited children states have distance 1 more than the parent.  These children can then in turn be processed until we reach a child that has consumed all input (done == input.length) and is on a node that represents a valid word.

We can really see here how we are limiting the size of the tree.  The Insertion move only tried "a" and "u", not the 24 other letters that would be done in the brute force mode.  Similarly, replacement only tries replacing "a" with "u" (the same 24 other letters are not tried).  Thus, we end up with 5 child state, instead of 53.  
	