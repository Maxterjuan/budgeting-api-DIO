package dio.budgeting.infrastructure.ai;

import java.time.Clock;
import java.time.LocalDate;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/** Orquestra o fluxo de IA: áudio -> texto -> (modelo + tools) -> texto -> áudio. */
@Service
public class AiAssistantService {

    private final TranscriptionModel transcriptionModel;
    private final ChatClient chatClient;
    private final TextToSpeechModel textToSpeechModel;
    private final BudgetingTools tools;
    private final Clock clock;

    public AiAssistantService(TranscriptionModel transcriptionModel,
                              ChatClient chatClient,
                              TextToSpeechModel textToSpeechModel,
                              BudgetingTools tools,
                              Clock clock) {
        this.transcriptionModel = transcriptionModel;
        this.chatClient = chatClient;
        this.textToSpeechModel = textToSpeechModel;
        this.tools = tools;
        this.clock = clock;
    }

    public String transcribe(Resource audio) {
        return transcriptionModel.transcribe(audio);
    }

    public String answer(String userText) {
        return chatClient.prompt()
                .system(systemPrompt())
                .user(userText)
                .tools(tools)
                .call()
                .content();
    }

    public byte[] speak(String text) {
        return textToSpeechModel.call(text);
    }

    private String systemPrompt() {
        return "You are a personal budgeting assistant. The user speaks Brazilian Portuguese, "
                + "but you must ALWAYS reply in English. "
                + "Today is " + LocalDate.now(clock) + ". "
                + "Use the tools to record income and expenses and to query transactions and summaries. "
                + "Never invent values: if the amount or the type is missing, ask. "
                + "Reply in one or two short, natural sentences, because the answer will be read aloud. "
                + "Write amounts in plain words, for example '30 reais' instead of 'R$ 30,00'.";
    }
}