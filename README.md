# planning-stories-neurally

Integrates [GPT](https://openai.com/blog/openai-api) prompting into the [Sabre Narrative Planner](https://github.com/sgware/sabre) for research and experimentation involving LLM-guided search methodologies. 

## Purpose

Assign action costs based on what GPT says should happen next in the story. 

## Installation

To run, you must have a valid [OpenAI API key](https://platform.openai.com/api-keys) stored in an environment variable called OPENAI_KEY. 

Requires Java. Uses Sabre v0.7.

## Usage

Run Sabre with `-m llm` for the LLM-UCS search method. To run the example problem:

	java -jar lib/sabre.jar -p problems/gramma.txt -atl 5 -ctl 4 -el 1 -g 1 -m llm

This searches the `gramma` problem with LLM-UCS, limited to 5 nodes by default. (Configurations in `LLMSearch.java`)

Writes two output files: 
	- `out/search.txt` lists each node in the order visited 
	- `out/transcript.txt` shows the full conversation with GPT and resulting action costs at each step.

