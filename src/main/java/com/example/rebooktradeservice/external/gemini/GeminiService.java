package com.example.rebooktradeservice.external.gemini;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.rebook.common.core.exception.BusinessException;
import com.rebook.common.core.exception.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

  private final Client client;
  private final ObjectMapper objectMapper;
  private static final String MODEL = "gemini-2.5-flash";

  // --- [Text Only] ---
  public String callString(String prompt) {
    return executeApi(prompt);
  }

  // --- [Text + Image] ---
  public <T> T callObjectWithImages(String prompt, List<ImageSource> images, Class<T> clazz) {
    validateNotString(clazz);
    String rawResponse = executeApi(prompt, images);
    return parseJson(rawResponse, clazz);
  }

  // --- [Private] ---

  //프롬프트
  private String executeApi(String prompt) {
    try{
      GenerateContentResponse response = client.models.generateContent(
          MODEL,
          prompt,
          null
      );

      String resultText = response.text();
      valdateEmptyString(resultText == null);

      return resultText;
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
    }
  }

  // 프롬프트 + 이미지
  private String executeApi(String prompt, List<ImageSource> images) {
    List<Part> parts = new ArrayList<>();
    parts.add(Part.fromText(prompt));

    if (images != null) {
      for (ImageSource img : images) {
        parts.add(Part.fromBytes(img.bytes(), img.mimeType()));
      }
    }

    GenerateContentResponse response = client.models.generateContent(
        MODEL,
        Content.fromParts(parts.toArray(Part[]::new)),
        null
    );

    String resultText = response.text();
    if (resultText == null) throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);

    return resultText;
  }


  private <T> T parseJson(String json, Class<T> clazz) {
    String cleanedJson = json.replaceAll("```json|```", "").trim();
    try {
      return objectMapper.readValue(cleanedJson, clazz);
    } catch (JsonProcessingException e) {
      throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
    }
  }

  private void validateNotString(Class<?> clazz) {
    valdateEmptyString(clazz.equals(String.class));
  }

  private static void valdateEmptyString(boolean resultText) {
    if (resultText) {
      throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
    }
  }
}