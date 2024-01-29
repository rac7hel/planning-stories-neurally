package r7.nl;

import edu.uky.cs.nil.sabre.Fluent;
import edu.uky.cs.nil.sabre.comp.CompiledAction;
import edu.uky.cs.nil.sabre.logic.Expression;
import edu.uky.cs.nil.sabre.logic.Parameter;
import edu.uky.cs.nil.sabre.logic.True;
import edu.uky.cs.nil.sabre.logic.Unknown;
import edu.uky.cs.nil.sabre.util.ImmutableArray;

public class RaidersText extends DomainText {
		
	public RaidersText(Expression initial, int goal) {
		super(initial, goal);
		this.handwrittenInitial = "Indiana Jones is in the USA. " + 
				"The Nazis are at Tanis. " + 
				"The U.S. Army is in the USA. " + 
				"The Ark is buried at Tanis. " + 
				"The Nazis are armed. " + 
				"The U.S. Army is armed. " + 
				"The Nazis believe opening the Ark will make them immortal, but really it will kill them. " + 
				"Only Indiana Jones knows where the Ark is buried. " + 
				"The U.S. Army wants to have the Ark. " + 
				"Indiana Jones wants the U.S. Army to have the Ark. " + 
				"The Nazis want to be immortal. ";
		agents.put("Jones","Indiana Jones"); 
		agents.put("USArmy","The U.S. Army");
		agents.put("Nazis","The Nazis");
		places.put("Tanis","Tanis"); 
		places.put("USA","The USA");
		others.put("Ark", "The Ark");
	}

	@Override
	public String characterGoals() {
		return "Indiana Jones wants the U.S. Army to have the Ark. "
				+ "The U.S. Army wants to have the Ark. "
				+ "The Nazis want to be immortal. ";
	}

	@Override
	public String actionTypes() {
		return "Characters can travel, give, take, dig, and open. ";
	}
	
	@Override
	public String goal() {
		return "The story must end with "
				+ "the Nazis being dead and the U.S. Army having the Ark. ";
	}
			
	@Override
	public String fluent(Fluent fluent, Expression value) {
		String str = believes(fluent, value);
		String arg0 = fluent.signature.arguments.get(0).toString();
		switch(fluent.signature.name) {
		case "armed":
			if(value.equals(True.TRUE))
				str += arg0 + " is armed";
			else 
				str += arg0 + " is unarmed";
			break;
		case "dangerous":
			if(value.equals(True.TRUE))
				str += arg0 + " is dangerous";
			else
				str += arg0 + " is not dangerous";
			break;
		case "at":
			if(value.equals(Unknown.UNKNOWN)) {
				str += "where " + arg0 + " is";
			} else if(!isLocation(value.toString())) {
				str += value + " has " + arg0;
			} else {
				str += arg0 + " is at " + value;
			}
			break;
		case "status":
			if(value.toString().equals("Alive"))
				str += arg0 + " is alive";
			else if(value.toString().equals("Immortal"))
				str += arg0 + " is immortal";
			else
				str += arg0 + " is dead";
			break;
		default:
			str += fluent + " = " + value;
		}
		return str.replaceAll("USArmy", "The U.S. Army")
				.replaceAll("at USA", "in the USA")
				.replaceAll("Nazis", "The Nazis")
				.replaceAll("Jones", "Indiana Jones")
				.replaceAll("Ark", "The Ark")
				.replaceAll(" The ", " the ") 
				.replaceAll("Nazis is", "Nazis are")
				.replaceAll("Nazis has", "Nazis have") 
				.replaceAll("Nazis believes", "Nazis believe") 
				.replaceAll("Nazis does", "Nazis do") + ". ";
	}
	
	private boolean isLocation(String str) {
		return str.equals("Tanis") || str.equals("USA");
	}
	
	@Override
	public String action(CompiledAction action) {
		String name = action.signature.name;
		ImmutableArray<Parameter> args = action.signature.arguments;
		String arg0 = args.get(0).toString();
		String str = "";
		switch(arg0) {
		case "Nazis":
			switch(name) {
			case "travel":
				str = "Nazis travel from " + args.get(1) + " to " + args.get(2) + ".";
				break;
			case "dig":
				str = "Nazis dig up the " + args.get(1) + ".";
				break;
			case "give":
				str = "Nazis give the " + args.get(1) + " to " + args.get(2) + ".";
				break;
			case "take":
				str = "Nazis take the " + args.get(1) + " from " + args.get(2) + ".";
				break;
			case "open":
				str = "Nazis open the Ark and die."; // example of action descriptions mattering
				break;
			default:
				throw new RuntimeException(NO_ACTION + action);
			}
			break;
		default:
			switch(name) {
			case "travel":
				str = arg0 + " travels from " + args.get(1) + " to " + args.get(2) + ".";
				break;
			case "dig":
				str = arg0 + " digs up the " + args.get(1) + ".";
				break;
			case "give":
				str = arg0 + " gives the " + args.get(1) + " to " + args.get(2) + ".";
				break;
			case "take":
				str = arg0 + " takes the " + args.get(1) + " from " + args.get(2) + ".";
				break;
			case "open":
				if(arg0.equals("Jones"))
					str = arg0 + " opens the Ark and dies.";
				else
					str = arg0 + " open the Ark and die."; 
				break;
			default:
				throw new RuntimeException(NO_ACTION + action);
			}
		}
		return str.replaceAll("USArmy", "The U.S. Army")
				.replaceAll("Nazis", "The Nazis")
				.replaceAll(" The ", " the ");
	}
}
