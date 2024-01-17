package r7.nl;

import java.util.ArrayList;

import edu.uky.cs.nil.sabre.Fluent;
import edu.uky.cs.nil.sabre.comp.CompiledAction;
import edu.uky.cs.nil.sabre.logic.Expression;
import edu.uky.cs.nil.sabre.logic.Parameter;

public class TreasureText extends DomainText {

	public TreasureText(Expression initial, int goal) {
		super(initial, goal);
		this.handwrittenInitial = "TODO: Write initial state";
		agents.put("Hawkins","Jim Hawkins");
		agents.put("Silver","Long John Silver");
		places.put("Port","Port Royal");
		places.put("Island","The island");
		others.put("Treasure", "The treasure");
		others.put("Buried","buried");
	}
	
	/** Main Functions **/
	
	@Override
	// Add a case for every property
	// Capitalize as if they're the the start of the sentence
	public String fluent(Fluent fluent, Expression value) {
		String str = believes(fluent, value);
		ArrayList<String> args = new ArrayList<>();
		for(Parameter arg : fluent.signature.arguments)
			args.add(arg.toString());
		String arg0 = args.get(0);
		switch(fluent.signature.name) {
		case "at": 
			if(places.containsKey(value.toString()))
				str += arg0 + " is at " + value;
			else if(agents.containsKey(value.toString()))
				str += value + " has " + arg0;
			else if(value.toString().equals("?"))
				str += "where " + arg0 + " is";
			else 
				str += arg0 + " is " + value;
			break;
		default:
			str += fluent + " = " + value;
		}
		return clean(str) + ". ";
	}
	
	@Override
	// Add a case for every action
	public String action(CompiledAction action) {
		String name = action.signature.name;
		ArrayList<String> args = new ArrayList<String>();
		for(Parameter arg : action.signature.arguments)
			args.add(arg.toString());
		String str = "";
		switch(name) {
		case "rumor":
			str += "Hawkins spreads a rumor that Treasure is buried on Island";
			break;
		case "sail":
			str += "Hawkins and Silver sail from Port to Island";
			break;
		case "dig":
			str += "Hawkins digs up Treasure";
			break;
		case "take":
			str += args.get(0) + " takes Treasure";
			break;
		default:
			throw new RuntimeException(NO_ACTION + action);
		}
		return clean(str) + ".";
	}

	/** Simple Functions **/
	
	@Override
	public String characterGoals() {
		return "Jim Hawkins and Long John Silver each want to have the treasure for themselves. ";
	}
	
	@Override
	public String actionTypes() {
		return "Characters can spread a rumor, sail, dig, and take. ";
	}

	@Override
	public String goal() {
		String text = "The story must end with ";
		switch(goal) {
		case 1: 
			return text + "Jim Hawkins having the treasure. ";
		default:
			return "";
		}
	}

}
