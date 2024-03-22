# planning-stories-neurally

Integrates [GPT](https://openai.com/blog/openai-api) prompting into the [Sabre Narrative Planner](https://github.com/sgware/sabre) for research and experimentation involving LLM-guided search methodologies for planning-based story generation. 

## Purpose

Assign action costs based on what GPT says should happen next in the story. 

![LLM-guided search process](/images/diagram.png)

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

## Paper

[View preprint](https://www.techrxiv.org/doi/full/10.36227/techrxiv.171085113.35202301/v1)

	Rachelyn Farrell, Stephen G Ware. Planning Stories Neurally. TechRxiv. March 19, 2024. DOI: 10.36227/techrxiv.171085113.35202301/v1

## Results 

Search results for the [Sabre benchmark problems](https://github.com/sgware/sabre-benchmarks) are discussed in the paper, and complete transcripts from these searches are included [here](results/).
