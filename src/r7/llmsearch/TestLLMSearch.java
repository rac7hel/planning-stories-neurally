package r7.llmsearch;


import edu.uky.cs.nil.sabre.*;
import edu.uky.cs.nil.sabre.Number;

import java.io.File;
import java.util.concurrent.TimeUnit;

import edu.uky.cs.nil.sabre.comp.*;
import edu.uky.cs.nil.sabre.io.*;
import edu.uky.cs.nil.sabre.prog.*;
import edu.uky.cs.nil.sabre.search.*;
import edu.uky.cs.nil.sabre.util.*;
import edu.uky.cs.nil.sabre.util.Worker.Status;
import edu.uky.cs.nil.sabre.ptree.ProgressionTreeSpace;

import r7.nl.DomainText;

public class TestLLMSearch {
	
	/**
	 * Note: the difference between the current version of "gramma" and the one we have prior results from
	 * is that in the current version, (all?) actions are only observed by alive characters
	 **/

	public static final long MAX_NODES_TO_VISIT = Planner.UNLIMITED_NODES;
	public static final long MAX_NODES_TO_GENERATE = Planner.UNLIMITED_NODES;
	public static final long MAX_MILLISECONDS = Planner.UNLIMITED_TIME;

	public static void run(Session session) throws Exception{
		Problem problem = session.getCompiledProblem();
		DomainText text = DomainText.get(problem, session.getGoal().value.intValue());
		int run = 1;
		System.out.println(text.toString());
		Result<CompiledAction> result = Worker.get(status -> {
			ProgressionPlanner planner = new ProgressionPlanner();
			CompiledProblem compiled = planner.compile(problem, status);
			ProgressionTreeSpace space = new ProgressionTreeSpace(compiled, new Status());
			System.out.println(compiled);
			LLMSearch search = new LLMSearch(
					compiled,
					MAX_NODES_TO_VISIT,
					MAX_NODES_TO_GENERATE,
					MAX_MILLISECONDS,
					session.getAuthorTemporalLimit(),
					session.getCharacterTemporalLimit(),
					session.getEpistemicLimit(),
					space,
					problem.name//,
					//session.getGoal(),
					//text
				);
			search.setRun(run);
			search.setStart(compiled.start);
			search.setGoal(session.getGoal());
			System.out.println(search);
			return search.get(status);
		}, 30, TimeUnit.SECONDS);
		System.out.println(result);
		if(result.solution != null)
			System.out.println(result.solution);
	}

	public static void main(String[] args) throws Exception {
		/*Parser parser = new DefaultParser();
		Problem problem = parser.parse(new File(URL), Problem.class);

		double goalValue = GOAL.value;
		DomainText text = DomainText.get(problem, (int)(goalValue));
		int run = 1;
		System.out.println(text.toString());
		Result<CompiledAction> result = Worker.get(status -> {
			ProgressionPlanner planner = new ProgressionPlanner();
			CompiledProblem compiled = planner.compile(problem, status);
			ProgressionTreeSpace space = new ProgressionTreeSpace(compiled, new Status());
			System.out.println(compiled);
			LLMSearch search = new LLMSearch(
					compiled,
					MAX_NODES_TO_VISIT,
					MAX_NODES_TO_GENERATE,
					MAX_MILLISECONDS,
					AUTHOR_TEMPORAL_LIMIT,
					AGENT_TEMPORAL_LIMIT,
					EPISTEMIC_LIMIT,
					space,
					DOMAIN,
					GOAL,
					text
				);
			search.setRun(run);
			search.setStart(compiled.start);
			search.setGoal(GOAL);
			System.out.println(search);
			return search.get(status);
		}, 30, TimeUnit.SECONDS);
		System.out.println(result);
		if(result.solution != null)
			System.out.println(result.solution);
		*/
	}
}