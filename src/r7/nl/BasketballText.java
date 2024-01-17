package r7.nl;

import java.util.ArrayList;

import edu.uky.cs.nil.sabre.Fluent;
import edu.uky.cs.nil.sabre.comp.CompiledAction;
import edu.uky.cs.nil.sabre.logic.Expression;
import edu.uky.cs.nil.sabre.logic.Parameter;
import edu.uky.cs.nil.sabre.logic.True;
import edu.uky.cs.nil.sabre.Number;

public class BasketballText extends DomainText {

	public BasketballText(Expression initial, int goal) {
		super(initial, goal);
		this.handwrittenInitial = "TODO: Write initial state";
		agents.put("Alice","Alice");
		agents.put("Bob","Bob");
		agents.put("Charlie","Charlie");
		agents.put("Sherlock","The detective");
		places.put("HomeB","Bob's house");
		places.put("BasketballCourt","The basketball court");
		places.put("Downtown","downtown");
		others.put("Basketball", "The basketball");
		others.put("Bat","The bat");
		others.put("Theft","theft");
		others.put("Murder","murder");
	}
	
	/** Main Functions **/
	
	@Override
	public String fluent(Fluent fluent, Expression value) {
		String str = believes(fluent, value);
		ArrayList<String> args = new ArrayList<>();
		for(Parameter arg : fluent.signature.arguments)
			args.add(arg.toString());
		String arg0 = args.get(0);
		switch(fluent.signature.name) {
		case "at":
			str += standardLocation(arg0, value.toString());
			break;
		case "has":
			str += value + " has " + arg0;
			break;
		case "alive":
			if(value.equals(True.TRUE))
				str += arg0 + " is alive";
			else str += arg0 + " is not alive";
			break;
		case "underArrest":
			if(value.equals(Number.get(1)))
				str += arg0 + " is under arrest";
			else
				str += arg0 + " is not under arrest";
			break;
		case "angry":
			if(value.equals(Number.get(1)))
				str += arg0 + " is angry";
			else
				str += arg0 + " is not angry";
			break;
		case "searched":
			if(value.equals(Number.get(1)))
				str += arg0 + " has been searched";
			else
				str += arg0 + " has not been searched";
			break;
		case "clue":
			if(value.equals(True.TRUE))
				str += args.get(1) + " is a clue that a " + arg0 + " occurred at " + args.get(2);
			else 
				str += args.get(1) + " is not a clue that a " + arg0 + " occurred at " + args.get(2);
			break;
		default:
			str += fluent + " = " + value;
		}
		return clean(str).replaceAll("at downtown", "downtown") + ". ";
	}
	
	@Override
	// Add a case for every action
	public String action(CompiledAction action) {
		String name = action.signature.name;
		ArrayList<String> args = new ArrayList<String>();
		for(Parameter arg : action.signature.arguments)
			args.add(arg.toString());
		String arg0 = args.get(0);
		String str = "";
		switch(name) {
		case "travel":
			str += arg0 + " travels from " + args.get(1) + " to " + args.get(2);
			break;
		case "arrest":
			str += arg0 + " arrests " + args.get(1) + " for " + args.get(3);
			break;
		case "steal":
			str += arg0 + " steals " + args.get(3) + " from " + args.get(1);
			break;
		case "playBasketball": 
			str += arg0 + " and " + args.get(1) + " play basketball and feel better";
			break;
		case "kill":
			str += arg0 + " kills " + args.get(1) + " with " + args.get(3);
			break;
		case "findClues":
			str += arg0 + " searches " + args.get(3) + " for clues";
			break;
		case "shareClues":
			str += arg0 + " shares clues with " + args.get(1);
			break;
		case "suspectOfCrime":
			str += arg0 + " suspects " + args.get(1) + " of " + args.get(2);
			break;
		default:
			throw new RuntimeException(NO_ACTION + action);
		}
		return clean(str) + ".";
	}

	/** Simple Functions **/
	
	@Override
	public String characterGoals() {
		return "Alice wants to be happy. "
				+ "Bob wants everyone to be happy. "
				+ "Charlie wants Alice to be dead. "
				+ "The detective wants to search for clues and arrest citizens for crimes. ";
	}
	
	@Override
	public String actionTypes() {
		return "Characters can travel, steal, play basketball, and kill. "
				+ "Police characters can arrest, find clues, share clues, and suspect citizens of crimes. ";
	}

	@Override
	public String goal() {
		String text = "The story must end with ";
		switch(goal) {
		case 2:
			return text + "a character being under arrest and a character feeling better. ";
		case 1: 
			return text + "a character either being arrested or feeling better. ";
		default:
			return "";
		}
	}

}
