# planning-stories-neurally

 Experiment: using GPT as a heuristic to speed up narrative planning with [Sabre](https://github.com/sgware/sabre)

Explain what LLM-UCS is.

Sabre treats story generation as a planning problem: Start with a logical model of the world and events that can change it, and generate stories using a forward-chaining search process starting from a given initial state. Add actions onto the ends of plans until certain criteria are met. The plan must 1) achieve a certain goal condition in the final state, and 2) be consistent with a model of character behavior using the character goals defined in the domain. 

In order to achieve its believability model Sabre tracks nested character beliefs. However its theory of mind is expensive and causes a huge explosion of the state space. Search heuristics like additive, Glaive, etc. are not effective in these spaces because they do not account for Sabre's model of belief. 

Could large language models help us with this problem? Explain the argument for why they should be helpful. Their latent space contains general knowledge that mirrors the domain author's. 

What we need is a good heuristic to estimate which actions are worth exploring, or which ones are likely to lead to a solution. 

Explain the process.

Explain other stuff, like cliffnotes of the paper. Maybe answer anticipated criticism like, What this is and what it is not. This is an exploration, not a solution. Whatevs.


## Installation

- Get your own OpenAI API key and save it to an environment variable called OPENAI_KEY 

## Usage

Run Sabre with `-m llm` to use the LLM-UCS search method. E.g.
	java -jar sabre.jar -p macguffin.txt
	java -jar lib/sabre.jar -p ../sabre-benchmarks/problems/gramma.txt -atl 5 -ctl 4 -el 1 -g 1 -m llm

This compiles the problem in the specified domain file, `gramma.txt`. For more example domain files, see /sabre-benchmarks. 



## License


## Credits


## Citation

- ArXiv preprint