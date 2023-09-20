package com.example.querydsl.dto;

import lombok.Data;

// 검색 조건 넘겨받을 때 사용하는 클래스
@Data
public class MemberSearchCondition {
    private String username;
    private String teamName;
    // greater or equal 이상
    private Integer ageGoe;
    // less or equal 이하
    private Integer ageLoe;
}