package r7.llmsearch;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class OpenAI {

	private static final String URL = "https://api.openai.com/";	

	/** from OpenAI, 01/18/2024 **/
    private static final String[] ENDPOINTS = new String[] { 
			"v1/assistants", 
			"v1/audio/transcriptions", 
			"v1/audio/translations", 
			"v1/audio/speech", 
			"v1/chat/completions", 
			"v1/completions", 
			"v1/embeddings", 
			"v1/fine_tuning/jobs", 
			"v1/moderations", 
			"v1/images/generations"
	}; 	

	private static final String EMBEDDING_MODEL = "text-embedding-ada-002";

	private static final String[] BASE_MODELS = new String[] {
			"davinci-002",
			"babbage-002"
	};

    protected static final String[] CHAT_MODELS = new String[]{ 
    		"gpt-3.5-turbo-1106", // New Updated GPT 3.5 Turbo.
    		"gpt-3.5-turbo", // Currently points to gpt-3.5-turbo-0613.	
    		"gpt-3.5-turbo-instruct", 
    		"gpt-4-1106-preview", 
    		"gpt-4-vision-preview",
    		"gpt-4", 
    		"gpt-4-32k", 
    		"gpt-4-0613", 
    		"gpt-4-32k-0613"
    }; 

    /** This is new, haven't tested these **/
    protected static final String[] SYSTEM_ROLES = new String[] {
    		"You are a helpful assistant.",
    		"You are outlining a branching story.",
    		"You are helping the user outline a branching story." // ...?
    };

    private final String apiKey = System.getenv("OPENAI_KEY");
    
    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.openai.com")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .build();

    private String chatModel = CHAT_MODELS[0];
    private String systemRole = SYSTEM_ROLES[2];    

    public OpenAI() {
    	System.out.println((apiKey != null ? "Found API key" : "Could not find API key."));    	
    }
    
    public void setChatModel(String model) {
    	this.chatModel = model;
    }
    
    public void setSystemRole(String role) {
    	this.systemRole = role;
    }
    
    public Mono<String> completeChat(String[] messages, int maxTokens, float temperature) {
    	String requestJson = "{\"model\":\"" + chatModel + 
        		"\",\"messages\":[" + 
        		"{\"role\": \"system\", \"content\": \"" + systemRole + "\"},";
        for (String m : messages)
            requestJson += "{\"role\": \"user\", \"content\": \"" + m + "\"},";
        requestJson = requestJson.substring(0, requestJson.length() - 1) + "]" +
        		",\"max_tokens\":" + maxTokens +
        		",\"temperature\":" + temperature + "}";
        return webClient.post()
                .uri(URL + ENDPOINTS[4])
                .body(BodyInserters.fromValue(requestJson))
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> getTextEmbedding(String prompt) {
        String requestJson = "{\"model\":\"" + EMBEDDING_MODEL + "\",\"input\":\"" + prompt + "\"}";
        return webClient.post()
                .uri(URL  + ENDPOINTS[6])
                .body(BodyInserters.fromValue(requestJson))
                .retrieve()
                .bodyToMono(String.class);
    }
}
