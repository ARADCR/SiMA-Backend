package com.sima.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Servicio central de comunicación con el LLM (DeepSeek vía adapter OpenAI).
 * Todas las features de IA (chatbot, recomendaciones, análisis, etc.) consumen este servicio
 * en lugar de hablar directamente con Spring AI.
 */
@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);
    private static final int TIMEOUT_SECONDS = 30;
    private static final String FALLBACK_MESSAGE =
            "No se pudo obtener respuesta del asistente de IA en este momento. Intentá nuevamente en unos minutos.";

    private final ChatModel chatModel;

    public AiService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String chat(String systemPrompt, String userMessage) {
        return chat(systemPrompt, userMessage, null);
    }

    /**
     * Igual que {@link #chat(String, String)} pero permite overridear el límite de tokens de salida
     * configurado por defecto. Útil para respuestas estructuradas (JSON) que no entran en ~500 tokens.
     */
    public String chat(String systemPrompt, String userMessage, Integer maxTokens) {
        Prompt prompt = maxTokens == null
                ? new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage(userMessage)))
                : new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage(userMessage)),
                        OpenAiChatOptions.builder().maxTokens(maxTokens).build());

        CompletableFuture<ChatResponse> future = CompletableFuture.supplyAsync(() -> chatModel.call(prompt));

        try {
            ChatResponse response = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            String content = response.getResult().getOutput().getText();

            var usage = response.getMetadata().getUsage();
            if (usage != null) {
                log.info("AiService: tokens consumidos - prompt={}, completion={}",
                        usage.getPromptTokens(), usage.getCompletionTokens());
            }

            return content;
        } catch (TimeoutException ex) {
            log.error("AiService: timeout tras {}s esperando respuesta del LLM", TIMEOUT_SECONDS, ex);
            return FALLBACK_MESSAGE;
        } catch (ExecutionException ex) {
            log.error("AiService: error al comunicarse con el proveedor de IA", ex.getCause());
            return FALLBACK_MESSAGE;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("AiService: request interrumpido", ex);
            return FALLBACK_MESSAGE;
        } catch (Exception ex) {
            log.error("AiService: error inesperado", ex);
            return FALLBACK_MESSAGE;
        }
    }

    public boolean isFallback(String response) {
        return FALLBACK_MESSAGE.equals(response);
    }
}
