package com.jgy36.PoliticalApp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBadgeDto {
    private Long userId;
    private List<String> badges;
}
