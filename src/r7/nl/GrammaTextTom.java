package r7.nl;

import edu.uky.cs.nil.sabre.Fluent;
import edu.uky.cs.nil.sabre.comp.CompiledAction;
import edu.uky.cs.nil.sabre.logic.Expression;
import edu.uky.cs.nil.sabre.logic.Parameter;
import edu.uky.cs.nil.sabre.logic.True;
import edu.uky.cs.nil.sabre.logic.Unknown;
import edu.uky.cs.nil.sabre.util.ImmutableArray;

public class GrammaTextTom extends DomainText {
	
	/** Warning: This is out-dated since I started working on GrammaTextHero **/

	public GrammaTextTom(Expression initial, int goal) {
		super(initial, goal);
	}
	
	@Override
	public String characterGoals() {
		 return "Tom wants to bring the medicine to the Cottage. " 
				 + "The Bandit wants to have all valuable items. " 
				 + "The Merchant wants coins and is not willing to commit crimes. "
				 + "The Guard wants to attack criminals. ";
	}
	
	@Override
	public String actionTypes() {
		return "Characters can "
				+ "walk along paths, "
				+ "buy items from the Merchant, "
				+ "loot items from dead people, "
				+ "attack each other, "
				+ "rob each other, "
				+ "report the Bandit's location to the Guard, "
				+ "and take items from the chest at the Camp. ";
	}
	
	@Override
	public String goal() {
		return "The story must end with "
				+ "either Tom being attacked "
				+ "or Tom bringing the medicine to the Cottage. ";
	}

	@Override
	public String fluent(Fluent fluent, Expression value) {
		String str = believes(fluent, value);
		String arg0 = fluent.signature.arguments.get(0).toString();
		switch(fluent.signature.name) {
		case "alive":
			if(value.equals(True.TRUE))
				str += arg0 + " is alive";
			else 
				str += arg0 + " is not alive";
			break;
		case "criminal":
			if(value.equals(True.TRUE))
				str += arg0 + " is a criminal";
			else
				str += arg0 + " is not a criminal";
			break;
		case "location":
			if(isItem(arg0)) {
				if(value.equals(Unknown.UNKNOWN))
					str += "where " + theItem(arg0) + " is";
				else if(value.toString().equals("Chest"))
					str += theItem(arg0) + " is in the chest";
				else
					str += value + " has " + theItem(arg0);
			} else {
				if(value.equals(Unknown.UNKNOWN))
					str += "where " + arg0 + " is";
				else
					str += arg0 + " is at the " + value;
			}
			break;
		case "path":
			if(value.equals(True.TRUE))
				str += "There is a path from the " + arg0 + " to the " + fluent.signature.arguments.get(1);
			break;
		default:
			str += fluent + " = " + value;
		}
		return str.replaceAll("Merchant", "The Merchant")
				.replaceAll("Guard", "The Guard")
				.replaceAll("Bandit", "The Bandit")
				.replaceAll(" The ", " the ")+ ". ";
	}
	
	private boolean isItem(String str) {
		if(str.contains("Coin"))
			return true;
		if(str.contains("Sword"))
			return true;
		return str.equals("Medicine");
	}
	
	private String theItem(String item) {
		if(item.contains("Coin")) {
			if(item.contains("Bandit"))
				return "Bandit's coin";
			else return "Tom's coin";
		} else if(item.contains("Sword")) {
			if(item.contains("Bandit"))
				return "Bandit's sword";
			else if(item.contains("Guard"))
				return "Guard's sword";
			else return "Merchant's sword";
		} else 
			return "the medicine";
	}
	
	@Override
	public String action(CompiledAction action) {
		String name = action.signature.name;
		ImmutableArray<Parameter> args = action.signature.arguments;
		switch(name) {
		case "attack":
			return args.get(0) + " attacks " + args.get(1) + ".";
		case "buy":
			return args.get(0) + " buys " + theItem(args.get(1).toString()) + " from Merchant" + ".";
		case "loot":
			return args.get(0) + " loots " + theItem(args.get(1).toString()) + " from " + args.get(2) + ".";
		case "report":
			return args.get(0) + " reports seeing Bandit at " + args.get(1) + " to Guard" + ".";
		case "rob":
			return args.get(0) + " robs " + theItem(args.get(1).toString()) + " from " + args.get(2) + ".";
		case "take":
			return args.get(0) + " takes " + theItem(args.get(1).toString()) + " from the chest" + ".";
		case "walk":
			return args.get(0) + " walks from " + args.get(1) + " to " + args.get(2) + ".";
		default:
			throw new RuntimeException(NO_ACTION + action);
		}
	}

}
