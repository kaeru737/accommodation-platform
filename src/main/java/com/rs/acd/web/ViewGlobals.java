package com.rs.acd.web;

import org.springframework.stereotype.Component;

@Component("viewGlobals")
public class ViewGlobals {

    /**
     * 목록 카드에서만 짧게 보여줄 주소를 만든다.
     * 기본: [도/광역시] + [시/군/구] (+ 필요 시 구까지 1단계 더)
     * - 서울/부산/대구/인천/광주/대전/울산 은 "특별시/광역시" 접미사를 제거해 짧게 표시
     * - 제주/세종도 축약
     * - title로 전체 주소를 주면 툴팁으로 전체 확인 가능
     */
    public String region2(String full) {
        if (full == null) return "";
        String addr = full.trim().replaceAll("\\s+", " ");
        if (addr.isEmpty()) return "";

        String[] t = addr.split(" ");
        if (t.length == 1) return addr;

        // 1) 광역시/특별시 축약
        String first = t[0];
        first = first.replace("특별자치도", "")
                     .replace("특별자치시", "")
                     .replace("특별시", "")
                     .replace("광역시", "")
                     .trim();

        // 제주/세종 축약 보정
        if (first.startsWith("제주")) first = "제주";
        if (first.startsWith("세종")) first = "세종";

        // 서울/부산 등 광역시는 보통 [도시 + 구]까지만
        boolean isMetro = isMetroCity(t[0]);

        if (isMetro) {
            // ex) "서울 강남구"
            if (t.length >= 2) return first + " " + t[1];
            return first;
        }

        // 일반 도/도시의 경우: [도 + 시/군] (+ 가능하면 구까지 1개 더)
        if (t.length >= 3 && endsWithAdministrative(t[2])) {
            // ex) "충청북도 청주시 상당구"
            return first + " " + t[1] + " " + t[2];
        }
        // ex) "경기도 성남시"
        return first + " " + t[1];
    }

    private boolean isMetroCity(String s) {
        if (s == null) return false;
        return s.contains("서울") || s.contains("부산") || s.contains("대구")
            || s.contains("인천") || s.contains("광주") || s.contains("대전")
            || s.contains("울산") || s.contains("세종");
    }

    private boolean endsWithAdministrative(String token) {
        if (token == null) return false;
        // 구/군/시/읍/면 중 하나로 끝나면 행정단위로 판단
        return token.endsWith("구") || token.endsWith("군") || token.endsWith("시")
            || token.endsWith("읍") || token.endsWith("면");
    }
}
