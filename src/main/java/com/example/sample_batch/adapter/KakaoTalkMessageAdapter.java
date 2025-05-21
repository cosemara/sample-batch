package com.example.sample_batch.adapter;

import com.example.sample_batch.config.KakaoTalkMessageConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
public class KakaoTalkMessageAdapter {
    private final WebClient webClient;

    public KakaoTalkMessageAdapter(KakaoTalkMessageConfig config) {
        webClient = WebClient.builder()
                             .baseUrl(config.getHost())
                             .defaultHeaders(h -> {
                                 h.setBearerAuth(config.getToken());
                                 h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                             }).build();

    }

    public boolean sendKakaoTalkMessage(final String uuid, final String text) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            // template_object는 KakaoTalkMessageRequest의 templateObject를 JSON 문자열로 변환
            // KakaoTalkMessageRequest 생성자 로직을 그대로 사용
            KakaoTalkMessageRequest.TemplateObject.Link link = new KakaoTalkMessageRequest.TemplateObject.Link();
            KakaoTalkMessageRequest.TemplateObject templateObject = new KakaoTalkMessageRequest.TemplateObject();
            templateObject.setObjectType("text");
            templateObject.setText(text);
            templateObject.setLink(link);

            String templateObjectJson = objectMapper.writeValueAsString(templateObject);

            // BodyInserters.fromFormData를 사용하여 폼 데이터 구성
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("template_object", templateObjectJson);

            KakaoTalkMessageResponse response = webClient.post().uri("/v2/api/talk/memo/default/send")
                                                         .body(BodyInserters.fromValue(formData))
                                                         .retrieve()
                                                         .bodyToMono(KakaoTalkMessageResponse.class)
                                                         .block();

            return response != null && response.getResultCode() == 0;

        } catch (Exception e) {
            log.error("Exception:", e);
            return false;
        }
    }

}