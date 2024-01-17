package r7.nl;
import java.util.LinkedHashMap;

import edu.uky.cs.nil.sabre.Fluent;
import edu.uky.cs.nil.sabre.HeadPlan;
import edu.uky.cs.nil.sabre.Problem;
import edu.uky.cs.nil.sabre.comp.CompiledAction;
import edu.uky.cs.nil.sabre.comp.CompiledProblem;
import edu.uky.cs.nil.sabre.logic.Effect;
import edu.uky.cs.nil.sabre.logic.Expression;
import edu.uky.cs.nil.sabre.logic.Unknown;

public abstract class DomainText {
	
	public final static boolean GRAMMA_SCRAMBLED = false;

	public final LinkedHashMap<String, String> agents = new LinkedHashMap<>();
	public final LinkedHashMap<String, String> places = new LinkedHashMap<>();
	public final LinkedHashMap<String, String> containers = new LinkedHashMap<>();
	public final LinkedHashMap<String, String> others = new LinkedHashMap<>();
	
	public String handwrittenInitial;
	
	public static final String NO_ACTION = "Action not implemented: ";
	public static final String NO_FLUENT = "Fluent not implemented: ";
	public static final String NO_METHOD = "Not implemented.";

	public final Expression initial;
	public final int goal;

	public abstract String characterGoals();
	public abstract String actionTypes();
	public abstract String goal();
	public abstract String action(CompiledAction action);
	public abstract String fluent(Fluent fluent, Expression value);

	public DomainText(Expression initial, int goal) {
		this.initial = initial;
		this.goal = goal;
	}

	public String believes(Fluent fluent, Expression value) {
		String str = "";
		for(int i=0; i<fluent.characters.size(); i++) {
			if(!value.equals(Unknown.UNKNOWN) || (i+1 < fluent.characters.size()))
				str += fluent.characters.get(i) + " believes that ";
			else
				str += fluent.characters.get(i) + " does not know ";
		}
		return str;
	}
	
	public String initialState() {
		return state(this.initial);
	}
	
	public String plan(HeadPlan<CompiledAction> plan) {
		String str = "";
		for(CompiledAction action : plan)
			str += action(action) + " ";
		return str;
	}
	
	public String state(Expression expression) {
		String str = "";
		for(Effect effect : expression.toEffect().arguments)
			str += fluent(effect.fluent, effect.value);
		return str;
	}

	public String standardLocation(String arg0, String value){
		if(places.containsKey(value))
			return arg0 + " is at " + value;
		else if(containers.containsKey(value))
			return arg0 + " is in " + value;
		else if(value.equals("?"))
			return "where " + arg0 + " is";
		return value + " has " + arg0;
	}
	
	public static DomainText get(Problem problem, int goal) {
		switch(problem.name) {
		case "aladdin":
			return new AladdinText(problem.initial, goal);
		case "basketball":
			return new BasketballText(problem.initial, goal);
		case "bribery":
			return new BriberyText(problem.initial, goal);
		case "deerhunter":
			return new DeerhunterText(problem.initial, goal);
		case "fantasy":
			return new FantasyText(problem.initial, goal);
		case "gramma":
			if(GRAMMA_SCRAMBLED)
				return new GrammaTextWonky(problem.initial, goal);
			else
				return new GrammaText(problem.initial, goal); 			
		case "hospital":
			return new HospitalText(problem.initial, goal);
		case "jailbreak":
			return new JailbreakText(problem.initial, goal);
		case "raiders":
			return new RaidersText(problem.initial, goal);
		case "secretagent":
			return new SecretagentText(problem.initial, goal);
		case "snakebite":
			return new SnakebiteText(problem.initial, goal);
		case "space":
			return new SpaceText(problem.initial, goal);
		case "treasure":
			return new TreasureText(problem.initial, goal);
		default:
			return null;
		}
	}
	
	public String clean(String str) {
		for(String agent : agents.keySet())
			str = str.replaceAll(agent, agents.get(agent));
		for(String place : places.keySet())
			str = str.replaceAll(place, places.get(place));
		for(String other : others.keySet())
			str = str.replaceAll(other, others.get(other));
		return str.replaceAll(" The ", " the ");
	}
	
	@Override
	public String toString() {
		return initialState() 
				+ characterGoals()
				+ actionTypes()
				+ goal();
	}

}
