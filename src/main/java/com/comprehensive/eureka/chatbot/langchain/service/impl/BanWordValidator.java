package com.comprehensive.eureka.chatbot.langchain.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BanWordValidator {

    private final Trie trie;
    private final Map<String, String> keywordTypes;

    public BanWordValidator(Set<String> banWords, Set<String> allowWords) {
        this.keywordTypes = new HashMap<>();
        Trie.TrieBuilder builder = Trie.builder().ignoreCase();

        banWords.forEach(word -> {
            builder.addKeyword(word);
            keywordTypes.put(word, "banWord");
        });

        allowWords.forEach(word -> {
            builder.addKeyword(word);
            keywordTypes.put(word, "allowWord");
        });

        this.trie = builder.build();
    }

    public List<String> findBanWords(String content) {
        String normalizedContent = ContentNormalizer.normalize(content);
        Collection<Emit> emits = trie.parseText(normalizedContent);

        return filterAllowWords(content, emits);
    }

    public boolean checkBanWord(String content) {
        String normalizedContent = ContentNormalizer.normalize(content);
        Collection<Emit> emits = trie.parseText(normalizedContent);

        return !filterAllowWords(content, emits).isEmpty();
    }

    public List<String> filterAllowWords(String originalContent, Collection<Emit> emits) {
        List<String> finalBanWords = new ArrayList<>();
        if (emits == null || emits.isEmpty()) {
            return finalBanWords;
        }

        // PriorityQueue를 사용하여 Emit 객체를 직접 정렬합니다.
        Queue<Emit> queue = new PriorityQueue<>((emit1, emit2) -> {

            // 1. 시작 위치가 다르면, 시작 위치가 빠른 순서로 정렬
            if (emit1.getStart() != emit2.getStart()) {
                return Integer.compare(emit1.getStart(), emit2.getStart());
            }

            // 2. 시작 위치가 같으면, 허용어가 금칙어보다 먼저 오도록 정렬 (우선순위 부여)
            //    "allowWord"는 0, "banWord"는 1로 간주하여 오름차순 정렬
            int type1Priority = "banWord".equals(keywordTypes.get(emit1.getKeyword())) ? 1 : 0;
            int type2Priority = "banWord".equals(keywordTypes.get(emit2.getKeyword())) ? 1 : 0;
            return Integer.compare(type1Priority, type2Priority);
        });

        queue.addAll(emits);

        int allowWordLastPosition = -1;

        while (!queue.isEmpty()) {
            Emit currentEmit = queue.poll();
            log.debug("{}", currentEmit);

            String word = currentEmit.getKeyword();
            String type = keywordTypes.get(word);
            boolean isBanWord = "banWord".equals(type);

            if (isBanWord) {
                // 이 금칙어가 이전에 발견된 허용어의 범위 밖에 있을 때만 최종 목록에 추가
                if (allowWordLastPosition < currentEmit.getStart()) {
                    finalBanWords.add(word);
                }
            } else { // 허용어인 경우
                // 허용어의 끝 위치를 기록하여 이 범위 내의 금칙어를 '마스킹'할 준비를 함
                if (originalContent.contains(word)) {
                    allowWordLastPosition = Math.max(allowWordLastPosition, currentEmit.getEnd());
                }
            }
        }
        return finalBanWords;
    }
}