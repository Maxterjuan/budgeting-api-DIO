package dio.budgeting.infrastructure.ai;

import java.io.IOException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ai")
public class VoiceAssistantController {

    public record TextRequest(String text) {}

    public record TextResponse(String transcription, String answer) {}

    private final AiAssistantService assistant;

    public VoiceAssistantController(AiAssistantService assistant) {
        this.assistant = assistant;
    }

    /** Fluxo completo: recebe um áudio e devolve a resposta em áudio (WAV). */
    @PostMapping(value = "/voice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "audio/wav")
    public ResponseEntity<byte[]> voice(@RequestParam("audio") MultipartFile audio) throws IOException {
        if (audio.isEmpty()) {
            throw new IllegalArgumentException("O arquivo de áudio está vazio.");
        }
        String transcription = assistant.transcribe(asResource(audio));
        if (transcription == null || transcription.isBlank()) {
            throw new IllegalArgumentException("Não consegui entender o áudio. Tente novamente.");
        }
        String answer = assistant.answer(transcription);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/wav"))
                .body(assistant.speak(answer));
    }

    /** Versão só texto do mesmo fluxo: útil para testar as tools sem gravar áudio. */
    @PostMapping("/text")
    public TextResponse text(@RequestBody TextRequest request) {
        if (request.text() == null || request.text().isBlank()) {
            throw new IllegalArgumentException("O campo 'text' é obrigatório.");
        }
        return new TextResponse(request.text(), assistant.answer(request.text()));
    }

    /** A OpenAI usa a extensão do nome do arquivo para identificar o formato do áudio. */
    private Resource asResource(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "audio.mp3";
        return new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }
}
