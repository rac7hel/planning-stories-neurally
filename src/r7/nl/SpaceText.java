package r7.nl;

import java.util.ArrayList;

import edu.uky.cs.nil.sabre.Fluent;
import edu.uky.cs.nil.sabre.comp.CompiledAction;
import edu.uky.cs.nil.sabre.logic.Expression;
import edu.uky.cs.nil.sabre.logic.Parameter;
import edu.uky.cs.nil.sabre.logic.True;

public class SpaceText extends DomainText {

	public SpaceText(Expression initial, int goal) {
		super(initial, goal);
		agents.put("Zoe","Zoe");
		agents.put("Lizard","The lizard");
		places.put("Cave","The cave");
		places.put("Surface","The surface");
		places.put("Ship","The ship");
		others.put("Safe", "safe");
		others.put("Dangerous", "dangerous");
		others.put("Uninhabitable", "uninhabitable");
		others.put("Healthy", "healthy");
		others.put("Stunned", "stunned");
		others.put("Dead", "dead");
	}
		
	@Override
	public String fluent(Fluent fluent, Expression value) {
		String str = believes(fluent, value);
		ArrayList<String> args = new ArrayList<>();
		for(Parameter arg : fluent.signature.arguments)
			args.add(arg.toString());
		String arg0 = args.get(0);
		switch(fluent.signature.name) {
		case "path":
			if(value.equals(True.TRUE))
				str += "There is a path from " + arg0 + " to " + args.get(1);
			else str += "There is not a path from " + arg0 + " to " + args.get(1);
			break;
		case "status":
			str += arg0 + " is " + value;
			break;
		case "captain":
			if(value.equals(True.TRUE))
				str += arg0 + " is the captain of " + args.get(1);
			else str += arg0 + " is not the captain of " + args.get(1);
			break;
		case "guardian":
			if(value.equals(True.TRUE))
				str += arg0 + " is the guardian of " + args.get(1);
			else str += arg0 + " is not the guardian of " + args.get(1);
			break;
		case "at":
			str += arg0 + " is at " + value;
			break;
		case "safe":
			if(value.equals(True.TRUE))
				str += arg0 + " is safe";
			else str += arg0 + " is not safe";
			break;
		case "fighting":
			if(value.equals(True.TRUE))
				str += arg0 + " and " + args.get(1) + " are fighting";
			else str += arg0 + " and " + args.get(1) + " are not fighting";
			break;
		case "relationship":
			if(value.equals(1)) // probs dont work
				str += arg0 + " and " + args.get(1) + " are friends";
			else if(value.equals(-1))
				str += arg0 + " and " + args.get(1) + " are enemies";
			else 
				str += arg0 + " and " + args.get(1) + " have no relatinship";
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
		case "teleport_from_ship":
			str += arg0 + " teleports from " + args.get(1) + " to " + args.get(2);
			break;
		case "teleport_to_ship":
			str += arg0 + " teleports to " + args.get(2) + " from " + args.get(1);
			break;
		case "walk":
			str += arg0 + " walks from " + args.get(1) + " to " + args.get(2);
			break;
		case "attack":
			str += arg0 + " attacks " + args.get(1);
			break;
		case "stun":
			str += arg0 + " stuns " + args.get(1);
			break;
		case "kill":
			str += arg0 + " kills " + args.get(1);
			break;
		case "break_free":
			str += arg0 + " breaks free";
			break;
		case "make_peace":
			str += arg0 + " makes peace with " + args.get(1);
			break;
		case "begin_erupt":
			str += arg0 + " begins to erupt";
			break;
		case "erupt":
			str += arg0 + " erupts";
			break;
		default:
			throw new RuntimeException(NO_ACTION + action);
		}
		return clean(str) + ".";
	}
	
	@Override
	public String characterGoals() {
		return "Zoe and the lizard each want to have friends and be healthy and safe. ";
	}
	
	@Override
	public String actionTypes() {
		return "Characters can teleport to and from the ship, walk, attack, stun, kill, break free, and make peace. "
				+ "Landforms can begin to erupt and erupt. ";
	}

	@Override
	public String goal() {
		String text = "The story must end with ";
		switch(goal) {
		case 4: 
			return text + "Zoe and the lizard being friends and either one of them being dead or the surface being uninhabitable. ";
		case 1:
			return text + "either the surface being uninhabitable, Zoe and the lizard being friends, or one of them being dead. ";
		default: 
			return "";
		}
	}

}
