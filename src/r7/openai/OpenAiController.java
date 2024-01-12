package r7.openai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
public class OpenAiController
{

 @Autowired
 private final OpenAi openAi;

 public OpenAiController(OpenAi openAi)
 {
  this.openAi = openAi;
 }
 
 @PostMapping("/completeText")
 public Mono<String> completeChat(@RequestBody CompleteChatRequest request) {
	 return openAi.completeChat(new String[] {request.getPrompt()});
 }

}
