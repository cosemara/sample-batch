package com.example.sample_batch.adapter;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class KakaoTalkMessageResponse {
//    @JsonProperty("successful_receiver_uuids")
//    private List<String> successfulReceiverUuids;

    @JsonProperty("result_code")
    private Integer resultCode;

}