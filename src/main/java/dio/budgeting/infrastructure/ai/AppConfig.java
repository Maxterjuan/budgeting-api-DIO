package dio.budgeting.infrastructure.ai;

import java.time.Clock;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class AppConfig {

    /** Relógio injetável: facilita testes e deixa "hoje" em um único lugar. */
    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
