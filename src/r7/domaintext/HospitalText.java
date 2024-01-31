package r7.domaintext;

import java.util.ArrayList;

import edu.uky.cs.nil.sabre.Fluent;
import edu.uky.cs.nil.sabre.comp.CompiledAction;
import edu.uky.cs.nil.sabre.logic.Expression;
import edu.uky.cs.nil.sabre.logic.Parameter;
import edu.uky.cs.nil.sabre.logic.True;
import edu.uky.cs.nil.sabre.logic.Unknown;

public class HospitalText extends DomainText {

	public HospitalText(Expression initial, int goal) {
		super(initial, goal);
		agents.put("Hathaway","Dr. Hathaway");
		agents.put("Jones","Jones");
		agents.put("Ross","Ross");
		agents.put("Young","Young");
		places.put("Admissions","Admissions");
		places.put("PatientRoomA","Patient Room A");
		places.put("PatientRoomB","Patient Room B");
		places.put("PatientRoomC","Patient Room C");
		others.put("Healthy", "No symptoms");
		others.put("SymptomA", "Symptom A");
		others.put("SymptomB", "Symptom B");
		others.put("TreatmentA", "Treatment A");
		others.put("TreatmentB", "Treatment B");
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
		case "alive":
			if(value.equals(True.TRUE))
				str += arg0 + " is alive";
			else str += arg0 + " is not alive";
			break;
		case "treats":
			str += value + " treats " + arg0;
			break;
		case "symptom":
			str += arg0 + " is experiencing " + value;
			break;
		case "assigned":
			if(value.equals(Unknown.UNKNOWN))
				str += arg0 + " has not been assigned";
			else
				str += arg0 + " is assigned to " + value;
			break;
		case "workload":
			str += arg0 + "'s workload is " + value;
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
		case "admit":
			str += arg0 + " admits " + args.get(1) + " to " + args.get(2);
			break;
		case "walk":
			str += arg0 + " walks from " + args.get(1) + " to " + args.get(2);
			break;
		case "assess":
			str += arg0 + " assesses " + args.get(1) + " and observes " + args.get(2);
			break;
		case "treat":
			str += arg0 + " treats " + args.get(1) + " using " + args.get(2);
			break;
		default:
			throw new RuntimeException(NO_ACTION + action);
		}
		return clean(str) + ".";
	}
	
	@Override
	public String characterGoals() {
		return "Patients Jones, Ross, and Young each want to be healthy. "
				+ "Dr. Hathaway wants all patients to be healthy. ";
	}
	
	@Override
	public String actionTypes() {
		return "Characters can walk from room to room, and doctors can admit, assess, and treat patients. ";
	}

	@Override
	public String goal() {
		String text = "The story must end with ";
		switch(goal) {
		case 2: 
			return text + "one patient having recovered from their symptom and another patient having died from an incorrect assessment. ";
		case 1: 
			return text + "any patient either recovering from their symptom or dying as a result of an incorrect assessment. ";
		default:
			return "";
		}
	}

}
