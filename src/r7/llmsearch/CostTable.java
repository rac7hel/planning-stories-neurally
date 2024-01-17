package r7.llmsearch;

import java.util.HashMap;

import edu.uky.cs.nil.sabre.prog.ProgressionCost;
import edu.uky.cs.nil.sabre.prog.ProgressionNode;

public class CostTable implements ProgressionCost {

	private final HashMap<Long, Double> table = new HashMap<>();
	
	@Override
	public <N> double evaluate(ProgressionNode<N> node) {
		if(node.getNode() == node.getRoot())
			return 0;
		return getCost((long)node.getNode());
	}
	
	public <N> double getCost(long node) {
		Double cost = table.get(node);
		if(cost == null)
			throw new RuntimeException("No cost has been set for node " + node + ".");
		return cost;
	}
	
	public void setCost(long node, double cost) {
		table.put(node, cost);
	}

}