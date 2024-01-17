package r7.nl;

import edu.uky.cs.nil.sabre.Fluent;
import edu.uky.cs.nil.sabre.comp.CompiledAction;
import edu.uky.cs.nil.sabre.logic.Expression;
import edu.uky.cs.nil.sabre.logic.Parameter;
import edu.uky.cs.nil.sabre.logic.True;
import edu.uky.cs.nil.sabre.logic.Unknown;
import edu.uky.cs.nil.sabre.util.ImmutableArray;

public class GrammaText extends DomainText {

	public GrammaText(Expression initial, int goal) {
		super(initial, goal);
		this.handwrittenInitial = "The hero is at his cottage with his grandmother and wants to bring her a special medicine. "
				+ "A merchant works at the market selling items for one coin each. "
				+ "Currently the merchant's only items for sale are a sword and the medicine that the hero wants. "
				+ "There is a bandit at a nearby camp. "
				+ "A crossroads connects the cottage, the market, and the camp. "
				+ "There is a guard at the market who wants to attack criminals. "
				+ "The hero and the guard do not know where the bandit is. "
				+ "The hero has one coin, and there is one coin in the bandit's chest. "
				+ "The bandit is a criminal who wants to collect valuable items in the chest. "
				+ "The merchant wants coins but is not willing to commit crimes. ";
		agents.put("Merchant","The merchant");
		agents.put("Guard","The guard");
		agents.put("Bandit", "The bandit");
		agents.put("Tom", "The hero");
		places.put("Cottage","The cottage");
		places.put("Crossroads", "The crossroads");
		places.put("Market","The market");
		places.put("Camp","The camp");
		others.put("MerchantSword","The merchant's sword");
		others.put("GuardSword","The guard's sword");
		others.put("BanditSword","The bandit's sword");
		others.put("TomCoin","The hero's coin");
		others.put("BanditCoin","The bandit's coin");
		others.put("Medicine","The medicine");
	}
	
	@Override
	public String characterGoals() {
		 return "The hero wants to bring the medicine to the cottage. " 
				 + "The bandit wants to collect valuable items in the chest. " 
				 + "The merchant wants coins and is not willing to commit crimes. "
				 + "The guard wants to attack criminals. ";
	}
	
	@Override
	public String actionTypes() {
		return "Characters can walk, buy, loot, attack, rob, report, and take. ";
	}
	
	@Override
	public String goal() {
		switch(goal) {
		case 2:
			return "The story must end with the hero having the medicine at the cottage. ";
		case 1:
			return "The story must end with the hero either being attacked or having the medicine at the cottage. ";
		default:
			return "";
		}
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
				str += arg0 + " is dead";
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
					str += arg0 + " is at " + value;
			}
			break;
		case "path":
			if(value.equals(True.TRUE))
				str += "There is a path between " + arg0 + " and " + fluent.signature.arguments.get(1);
			break;
		default:
			str += fluent + " = " + value;
		}
		return clean(str) + ". ";
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
	
	private String reflexivePronoun(String agent) {
		switch(agent) {
		case "Merchant":
			return "herself";
		default:
			return "himself";
		}
	}
	
	@Override
	public String action(CompiledAction action) {
		String name = action.signature.name;
		ImmutableArray<Parameter> args = action.signature.arguments;
		String str = "";
		switch(name) {
		case "attack":
			if(args.get(0).equals(args.get(1)))
				str = args.get(0) + " attacks " + reflexivePronoun(args.get(1).toString());
			else
				str = args.get(0) + " attacks " + args.get(1);
			break;
		case "buy":
			str = args.get(0) + " buys " + theItem(args.get(1).toString()) + " from Merchant";
			break;
		case "loot":
			str = args.get(0) + " loots " + theItem(args.get(1).toString()) + " from " + args.get(2);
			break;
		case "report":
			if(args.get(0).toString().equals("Guard"))
				str = args.get(0) + " reports seeing Bandit at " + args.get(1) + " to himself";
			else if(args.get(0).toString().equals("Bandit"))
				str = args.get(0) + " reports seeing himself at " + args.get(1) + " to Guard";
			else
				str = args.get(0) + " reports seeing Bandit at " + args.get(1) + " to Guard";
			break;
		case "rob":
			str = args.get(0) + " robs " + theItem(args.get(1).toString()) + " from " + args.get(2);
			break;
		case "take":
			str = args.get(0) + " takes " + theItem(args.get(1).toString()) + " from the chest";
			break;
		case "walk":
			str = args.get(0) + " walks from " + args.get(1) + " to " + args.get(2);
			break;
		default:
			throw new RuntimeException(NO_ACTION + action);
		}
		return clean(str) + ".";
	}

}
