package r7.nl;

import java.util.ArrayList;

import edu.uky.cs.nil.sabre.Fluent;
import edu.uky.cs.nil.sabre.comp.CompiledAction;
import edu.uky.cs.nil.sabre.logic.Expression;
import edu.uky.cs.nil.sabre.logic.Parameter;
import edu.uky.cs.nil.sabre.logic.True;

public class SnakebiteText extends DomainText {

	public SnakebiteText(Expression initial, int goal) {
		super(initial, goal);
		agents.put("Hank","Hank");
		agents.put("Timmy","Timmy");
		agents.put("Will", "Sheriff Will");
		agents.put("Carl", "Carl");
		places.put("Ranch","The ranch");
		places.put("Saloon", "The saloon");
		places.put("Jailhouse", "The jailhouse");
		places.put("GeneralStore", "The general store");
		others.put("Antivenom","The antivenom");
		others.put("Snakebite", "snakebite");
	}
		
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
		case "loves":
			if(value.equals(True.TRUE))
				str += arg0 + " loves " + args.get(1);
			else str += arg0 + " does not love " + args.get(1);
			break;
		case "sheriff":
			if(value.equals(True.TRUE))
				str += arg0 + " is the sheriff";
			else str += arg0 + " is not the sheriff";
			break;
		case "alive":
			if(value.equals(True.TRUE))
				str += arg0 + " is alive";
			else str += arg0 + " is not alive";
			break;
		case "sick":
			if(value.equals(True.TRUE))
				str += arg0 + " is sick from " + args.get(1);
			else str += arg0 + " is not sick from " + args.get(1);
			break;
		case "free":
			if(value.equals(True.TRUE))
				str += arg0 + " is free";
			else str += arg0 + " is not free";
			break;
		case "relationship":
			str += arg0 + "'s relationship with " + args.get(1) + " is " + value;
			break;
		case "cures":
			if(value.equals(True.TRUE))
				str += arg0 + " cures a " + args.get(1);
			else str += arg0 + " does not cure a " + args.get(1);
			break;
		case "owner":
			str += value + " owns " + arg0;
			break;
		case "possession": // these bad
			str += arg0 + " possesses " + value + args.get(1) + "s";
			break;
		case "stolen":
			str += arg0 + " has stolen " + value + args.get(1) + "s";
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
		case "snakebite":
			str += arg0 + " gets bitten by a snake";
			break;
		case "die":
			str += arg0 + " dies from the " + args.get(1);
			break;
		case "travel":
			str += arg0 + " travels to " + args.get(1);
			break;
		case "give":
			str += arg0 + " gives " + args.get(1) + " to " + args.get(2);
			break;
		case "tie_up":
			str += arg0 + " ties up " + args.get(1) + " at " + args.get(2);
			break;
		case "untie":
			str += arg0 + " unties " + args.get(1);
			break;
		case "force_travel":
			str += arg0 + " takes " + args.get(1) + " to " + args.get(2);
			break;
		case "take":
			str += arg0 + " takes " + args.get(1) + " from " + args.get(2);
			break;
		case "heal":
			str += arg0 + " heals " + args.get(1) + "'s " + args.get(2) + " with " + args.get(3);
			break;
		default:
			throw new RuntimeException(NO_ACTION + action);
		}
		return clean(str) + ".";
	}
	
	@Override
	public String characterGoals() {
		return "Everyone wants people they love to be healthy and free. "
				+ "Everyone wants to have the items they own. "
				+ "The sheriff wants stolen items to be returned to their owners. ";
	}
	
	@Override
	public String actionTypes() {
		return "Characters can get bitten by a snake, die, travel, give, tie up, untie, take, and heal. ";
	}

	@Override
	public String goal() {
		return "The story must end with "
				+ "Timmy dead and Hank tied up at the jailhouse. ";
	}

}
