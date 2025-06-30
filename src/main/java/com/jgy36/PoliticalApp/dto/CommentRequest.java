package com.jgy36.PoliticalApp.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CommentRequest {
    private String content; // ✅ The text of the comment

    public CommentRequest(String content) {
        this.content = content;
    }
}
