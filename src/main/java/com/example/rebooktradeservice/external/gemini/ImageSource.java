package com.example.rebooktradeservice.external.gemini;

import lombok.AllArgsConstructor;
import lombok.Getter;


public record ImageSource(byte[] bytes, String mimeType) {

    public static ImageSource of(byte[] bytes, String mimeType) {
        return new ImageSource(bytes, mimeType);
    }
}
