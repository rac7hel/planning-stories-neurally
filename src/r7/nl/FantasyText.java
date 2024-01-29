package r7.nl;

import java.util.ArrayList;

import edu.uky.cs.nil.sabre.Fluent;
import edu.uky.cs.nil.sabre.comp.CompiledAction;
import edu.uky.cs.nil.sabre.logic.Expression;
import edu.uky.cs.nil.sabre.logic.Parameter;
import edu.uky.cs.nil.sabre.logic.True;

public class FantasyText extends DomainText {

	public FantasyText(Expression initial, int goal) {
		super(initial, goal);
		agents.put("Talia","Talia");
		agents.put("Rory","Rory");
		agents.put("Vince","Vince");
		agents.put("Gargax","Gargax");
		places.put("Village","The village");
		places.put("Cave","The cave");
		others.put("Money","The money");
		others.put("Treasure","The treasure");
	}
		
	@Override
	public String fluent(Fluent fluent, Expression value) {
		String str = believes(fluent, value);
		ArrayList<String> args = new ArrayList<>();
		for(Parameter arg : fluent.signature.arguments)
			args.add(arg.toString());
		String arg0 = args.get(0);
		switch(fluent.signature.name) {
		case "alive":
			if(value.equals(True.TRUE))
				str += arg0 + " is alive";
			else str += arg0 + " is not alive";
			break;
		case "loves":
			if(value.equals(True.TRUE))
				str += arg0 + " loves " + args.get(1);
			else str += arg0 + " does not love " + args.get(1);
			break;
		case "relationship":
			if(value.equals("proposed"))
				str += arg0 + " has proposed to " + args.get(1);
			else if(value.equals("accepted"))
				str += arg0 + " and " + args.get(1) + " are engaged";
			else
				str += arg0 + " and " + args.get(1) + " are married";
			break;
		case "happiness":
			str += arg0 + "'s happiness is " + value;
			break;
		case "wealth":
			str += arg0 + "'s wealth is " + value;
			break;
		case "hunger":
			str += arg0 + "'s huner is " + value; 
			break;
		case "at":
			str += standardLocation(arg0, value.toString());
			break;
		case "has":
			str += arg0 + " has " + value + " " + args.get(1) + "s";
			break;
		default:
			str += fluent + " = " + value;
		}
		return clean(str) + ". ";
	}
	
	@Override
	public String action(CompiledAction action) {
		String name = action.signature.name;
		ArrayList<String> args = new ArrayList<String>();
		for(Parameter arg : action.signature.arguments)
			args.add(arg.toString());
		String arg0 = args.get(0);
		String str = "";
		switch(name) {
		case "propose":
			str += arg0 + " proposes to " + args.get(1);
			break;
		case "accept":
			str += arg0 + " accepts " + args.get(1) + "'s proposal";
			break;
		case "marry":
			str += arg0 + " and " + args.get(1) + " get married";
			break;
		case "travel":
			str += arg0 + " travels from " + args.get(1) + " to " + args.get(2);
			break;
		case "pickup":
			str += arg0 + " picks up " + args.get(1);
			break;
		case "take":
			str += arg0 + " takes " + args.get(1) + " from " + args.get(2);
			break;
		case "get_hungry":
			str += arg0 + " gets hungry";
			break;
		case "eat":
			str += arg0 + " eats " + args.get(1);
			break;
		default:
			throw new RuntimeException(NO_ACTION + action);
		}
		return clean(str) + ".";
	}
	
	@Override
	public String characterGoals() {
		return "Talia wants to be happy and wealthy. " + 
				"Rory wants to be happy. " + 
				"Vince wants to be happy and wealthy. " + 
				"Gargax wants to be wealthy and does not want to be hungry. ";
	}
	
	@Override
	public String actionTypes() {
		return "Characters can propose, accept, marry, travel, pick up, take, get hungry, and eat. ";
	}

	@Override
	public String goal() {
		String text = "The story must end with ";
		switch(goal) {
		case 3:
			return text + "Talia being married and having both the treasure and the money. ";
		case 2:
			return text + "Talia either having both the treasure and the money, or being married and having one of these. ";
		case 1:
			return text + "Talia either being married or having the treasure or the money. ";
		default:
			return "";
		}
	}

}
