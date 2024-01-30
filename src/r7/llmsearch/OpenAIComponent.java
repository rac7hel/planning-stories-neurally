package r7.llmsearch;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class OpenAIComponent {

	private static final String URL = "https://api.openai.com/";	
    private static final String[] ENDPOINTS = new String[] { /** 01/18/2024 **/
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
	private static final String[] CHAT_MODELS = new String[]{ 
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
    private static final String[] SYSTEM_ROLES = new String[] {
    		"You are a helpful assistant.",
    		"You are outlining a branching story.",
    		"You are helping the user outline a branching story." // ...?
    };
    private static final String API_KEY = System.getenv("OPENAI_KEY");
    private static final WebClient webClient = WebClient.builder().baseUrl(URL)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + API_KEY).build();

    private String chatModel = CHAT_MODELS[0];
    private String systemRole = SYSTEM_ROLES[2];    

    public void setChatModel(String model) {
    	this.chatModel = model;
    }
    
    public void setSystemRole(String role) {
    	this.systemRole = role;
    }

    public OpenAIComponent() {
    	System.out.println((API_KEY != null ? "Found API key" : "Could not find API key."));    	
    }
    
    public Mono<String> completeChat(String[] messages, int maxTokens, float temperature) {
    	StringBuilder jsonRequest = new StringBuilder("{\"model\":\"").append(chatModel)
    			.append("\",\"messages\":[{\"role\": \"system\", \"content\": \"")
    			.append(systemRole).append("\"},");
        
    	for (String mmessage : messages) {
            jsonRequest.append("{\"role\": \"user\", \"content\": \"")
                       .append(mmessage).append("\"},");
    	}

    	jsonRequest.deleteCharAt(jsonRequest.length() - 1)
        	.append("],\"max_tokens\":").append(maxTokens)
        	.append(",\"temperature\":").append(temperature).append("}");

        return webClient.post().uri(URL + ENDPOINTS[4])
        		.body(BodyInserters.fromValue(jsonRequest.toString()))
                .retrieve().bodyToMono(String.class);
    }

    public Mono<String> getTextEmbedding(String prompt) {
    	StringBuilder jsonRequest = new StringBuilder("{\"model\":\"").append(EMBEDDING_MODEL)
    			.append("\",\"input\":\"").append(prompt).append("\"}");

        return webClient.post()
                .uri(URL  + ENDPOINTS[6])
                .body(BodyInserters.fromValue(jsonRequest.toString()))
                .retrieve()
                .bodyToMono(String.class);
    }
    
}
