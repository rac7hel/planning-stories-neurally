package r7.domaintext;

import java.util.ArrayList;

import edu.uky.cs.nil.sabre.Fluent;
import edu.uky.cs.nil.sabre.comp.CompiledAction;
import edu.uky.cs.nil.sabre.logic.Expression;
import edu.uky.cs.nil.sabre.logic.Parameter;
import edu.uky.cs.nil.sabre.logic.True;

public class TemplateText extends DomainText {

	public TemplateText(Expression initial, int goal) {
		super(initial, goal);
		this.handwrittenInitial = "TODO: Write initial state";
		// Add entities to these lists (with long spelling / Capitalized if they might start a sentence)		
		agents.put("Hank","Hank"); // Add all the agents		
		places.put("Ranch","The ranch"); // Add all the places
		// If there are any containers, add them to this.containers		
		others.put("Snakebite", "snakebite"); // Add all the items and other entities here
	}
	
	/** Main Functions **/
	
	@Override
	// Add a case for every property. Capitalize as if they're the start of the sentence
	public String fluent(Fluent fluent, Expression value) {
		String str = believes(fluent, value);
		ArrayList<String> args = new ArrayList<>();
		for(Parameter arg : fluent.signature.arguments)
			args.add(arg.toString());
		String arg0 = args.get(0);
		switch(fluent.signature.name) {
		case "at": // Handle location first. Most domains do it like this
			str += standardLocation(arg0, value.toString()); 
			break;
		case "alive": // Handle booleans like this
			if(value.equals(True.TRUE))
				str += arg0 + " is alive";
			else str += arg0 + " is not alive";
			break; 
		default:
			str += fluent + " = " + value;
		}
		// clean() replaces " The " with " the "
		// If you added more possible starting words, replace them with lowercase here
		return clean(str) + ". ";
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
		default:
			throw new RuntimeException(NO_ACTION + action);
		}
		return clean(str) + ".";
	}

	/** Simple Functions **/
	
	@Override
	public String characterGoals() { // Describe all the character goals
		return "Everyone wants people they love to be healthy and free. "
				+ "Everyone wants to have the items they own. "
				+ "The sheriff wants stolen items to be returned to their owners. ";
	}
	
	@Override
	public String actionTypes() { // List the actions by name only
		return "Characters can get bitten by a snake, die, travel, give, tie up, untie, take, and heal. ";
	}

	@Override
	public String goal() { // Describe each different goal / problem configuration
		String text = "The story must end with ";
		switch(goal) {
		case 1: 
			return text + "Timmy dead and Hank tied up at the jailhouse. ";
		default:
			return "";
		}
	}

}
