package r7.nl;

import java.util.ArrayList;

import edu.uky.cs.nil.sabre.Fluent;
import edu.uky.cs.nil.sabre.comp.CompiledAction;
import edu.uky.cs.nil.sabre.logic.Expression;
import edu.uky.cs.nil.sabre.logic.Parameter;
import edu.uky.cs.nil.sabre.logic.True;

public class JailbreakText extends DomainText {

	public JailbreakText(Expression initial, int goal) {
		super(initial, goal);
		agents.put("Ernest","Ernest");
		agents.put("Roy","Roy");
		agents.put("Bully","The bully");
		places.put("Cells","The cells");
		places.put("Laundry","The laundry room");
		places.put("Kitchen","The kitchen");
		places.put("Gym","The gym");
		places.put("Hall","The hall");
		places.put("Highway","The highway");
		others.put("Cigarettes", "The cigarettes");
		others.put("Clothes","The clothes");
		others.put("Knife","The knife");
	}
		
	@Override
	public String fluent(Fluent fluent, Expression value) {
		String str = believes(fluent, value);
		ArrayList<String> args = new ArrayList<>();
		for(Parameter arg : fluent.signature.arguments)
			args.add(arg.toString());
		switch(fluent.signature.name) {
		case "location":
			str += standardLocation(args.get(0), value.toString()).replace(" at "," in ");
			break;
		case "alive":
			if(value.equals(True.TRUE))
				str += args.get(0) + " is alive";
			else str += args.get(0) + " is not alive";
			break;
		case "time":
			if(value.toString().equals("Day1"))
				str += "It is the first day";
			else if(value.toString().equals("Day2"))
				str += "It is the second day";
			else
				str += "What day it is";
			break;
		case "threatened":
			if(value.equals(True.TRUE))
				str += args.get(0) + " is threatened";
			else
				str += args.get(0) + " is not threatened";
			break;
		case "chores":
			if(value.equals(True.TRUE))
				str += args.get(0) + " has done his chores";
			else
				str += args.get(0) + " has not done his chores";
			break;
		case "locked":
			if(value.equals(True.TRUE))
				str += args.get(0) + " is locked";
			else
				str += args.get(0) + " is not locked";
			break;
		case "disguised":
			if(value.equals(True.TRUE))
				str += args.get(0) + " is disguised";
			else
				str += args.get(0) + " is not disguised";
			break;
		default:
			str += fluent + " = " + value;
		}
		return clean(str)
				.replaceAll(" What ", " what ")
				.replaceAll(" It ", " it ")
				.replaceAll("cigarettes is", "cigarettes are")
				.replaceAll("clothes is", "clothes are") + ". ";
	}
	
	@Override
	public String action(CompiledAction action) {
		String name = action.signature.name;
		ArrayList<String> args = new ArrayList<String>();
		for(Parameter arg : action.signature.arguments)
			args.add(arg.toString());
		String str = "";
		switch(name) {
		case "confiscate":
			str += "A guard confiscates " + args.get(1) + " from " + args.get(0) + " and sends him to " + args.get(3) + " for punishment duties";
			break;
		case "chores":
			str += args.get(0) + " does his chores in " + args.get(1);
			break;
		case "disguise":
			str += args.get(0) + " puts on the clothes in " + args.get(2);
			break;
		case "escape":
			str += args.get(0) + " runs down the highway and escapes";
			break;
		case "go":
			str += args.get(0) + " goes to the hall";
			break;
		case "kill":
			str += args.get(0) + " kills " + args.get(1) + " in " + args.get(2);
			break;
		case "lock_gym":
			str += args.get(0) + " locks the door to the gym";
			break;
		case "next_day":
			str += "The prisoners go to sleep in their cells and wake up the next day";
			break;
		case "recreation":
			str += args.get(0) + " goes to the gym for recreation";
			break;
		case "revenge":
			str += args.get(0) + " kills the bully in the gym";
			break;
		case "steal":
			str += args.get(0) + " steals " + args.get(1) + " from " + args.get(2);
			if(args.get(1).toString().equals("Cigarettes"))
				str += ". This angers the bully, who threatens to kill both Roy and Ernest";
			break;
		case "thwart":
			str += args.get(0) + " is thwarted in the hall";
			break;
		case "vent":
			str += args.get(0) + " sneaks through the vent to " + args.get(1);
			break;
		default:
			throw new RuntimeException(NO_ACTION + action);
		}
		return clean(str).replaceAll(" A "," a ") + ".";
	}
	
	@Override
	public String characterGoals() {
		return "Ernest wants to have the cigarettes and not be threatened or dead. "
				+ "Roy wants to have the cigarettes and not be threatened or dead. "
				+ "The bully wants to be in the gym and to have killed anyone he has threatened. ";
	}
	
	@Override
	public String actionTypes() {
		return "Characters can steal items, have items get confiscated, do chores, put on clothes, escape, kill, lock doors, go to the hall, be thwarted, and sneak through vents. "; 
	}

	@Override
	public String goal() {
		String text = "The story must end with ";
		switch(goal) {
		case 2:
			return text + "Roy or Ernest either escaping onto the highway or killing the bully. ";
		case 1: 
			return text + "Roy or Ernest either being thwarted, escaping onto the highway, or killing the bully. ";
		default:
			return "";
		}
	}

}
