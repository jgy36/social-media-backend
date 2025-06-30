package com.jgy36.PoliticalApp.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class HashtagDTO {
    private Long id;
    private String name;
    private int useCount;
    private int postCount;
}
