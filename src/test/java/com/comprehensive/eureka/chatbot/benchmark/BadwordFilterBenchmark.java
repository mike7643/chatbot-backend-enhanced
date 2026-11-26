package com.comprehensive.eureka.chatbot.benchmark;

import com.comprehensive.eureka.chatbot.badword.redis.service.AllowWordRedisService;
import com.comprehensive.eureka.chatbot.badword.redis.service.ForbiddenWordRedisService;
import com.comprehensive.eureka.chatbot.badword.service.BanWordValidatorProvider;
import com.comprehensive.eureka.chatbot.langchain.service.impl.BanWordValidator;
import com.comprehensive.eureka.chatbot.langchain.service.impl.ContentNormalizer;
import com.comprehensive.eureka.chatbot.support.RedisContainerSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 금칙어 필터 성능 측정. 기본 테스트에서는 제외되며 ./gradlew benchmarkTest 로 실행한다.
 * 결과는 build/reports/benchmark/badword-filter.md 에 기록된다.
 */
@Tag("benchmark")
class BadwordFilterBenchmark extends RedisContainerSupport {

    private static final int FORBIDDEN_WORDS = 1_000;
    private static final int ALLOW_WORDS = 100;
    private static final int WARMUP = 500;
    private static final int ITERATIONS = 3_000;
    private static final String NORMAL_MESSAGE =
            "요즘 데이터를 많이 쓰는데 영상 시청이 많고 테더링도 자주 해서 무제한 요금제 중에 가격이 괜찮은 걸 추천해 주세요";

    private static final List<String> report = new ArrayList<>();
    private static StringRedisTemplate redisTemplate;
    private static ForbiddenWordRedisService forbiddenWordRedisService;
    private static AllowWordRedisService allowWordRedisService;
    private static Set<String> forbiddenWords;
    private static String badMessage;

    @BeforeAll
    static void setUp() {
        redisTemplate = newRedisTemplate();
        forbiddenWordRedisService = new ForbiddenWordRedisService(redisTemplate);
        allowWordRedisService = new AllowWordRedisService(redisTemplate);
        Random random = new Random(42);
        forbiddenWords = randomWords(random, FORBIDDEN_WORDS);
        Set<String> allowWords = randomWords(random, ALLOW_WORDS);
        redisTemplate.delete(List.of("forbidden:words", "allow:words", "badword:version"));
        redisTemplate.opsForSet().add("forbidden:words", forbiddenWords.toArray(String[]::new));
        redisTemplate.opsForSet().add("allow:words", allowWords.toArray(String[]::new));
        redisTemplate.opsForValue().set("badword:version", "1");
        badMessage = NORMAL_MESSAGE + " " + forbiddenWords.iterator().next();
        report.add("# 금칙어 필터 성능 측정");
        report.add("");
        report.add("- 금칙어 " + FORBIDDEN_WORDS + "개, 허용어 " + ALLOW_WORDS + "개, Redis 7 (Testcontainers, 로컬)");
        report.add("- 워밍업 " + WARMUP + "회 후 " + ITERATIONS + "회 측정, 단위 μs");
        report.add("");
    }

    @Test
    void 메시지_1건당_필터링_비용_캐시_적용_전후() throws IOException {
        BanWordValidatorProvider provider = new BanWordValidatorProvider(
                forbiddenWordRedisService, allowWordRedisService, redisTemplate);

        // 기존: 메시지마다 Set 전체 조회 + 필터 생성. 금칙어가 있으면 기록 전송 시 한 번 더 반복.
        Supplier<Object> beforeNormal = () -> rebuild().checkBanWord(NORMAL_MESSAGE);
        Supplier<Object> beforeBad = () -> rebuild().checkBanWord(badMessage) && !rebuild().findBanWords(badMessage).isEmpty();
        // 변경: 버전 키 1회 조회 + 캐시된 필터 사용
        Supplier<Object> afterNormal = () -> provider.get().checkBanWord(NORMAL_MESSAGE);
        Supplier<Object> afterBad = () -> provider.get().checkBanWord(badMessage) && !provider.get().findBanWords(badMessage).isEmpty();

        report.add("## 메시지 1건당 필터링 비용");
        report.add("");
        report.add("| 시나리오 | 평균 | p50 | p95 | p99 |");
        report.add("| --- | ---: | ---: | ---: | ---: |");
        report.add(row("일반 메시지, 캐시 적용 전", measure(beforeNormal)));
        report.add(row("일반 메시지, 캐시 적용 후", measure(afterNormal)));
        report.add(row("금칙어 메시지, 캐시 적용 전", measure(beforeBad)));
        report.add(row("금칙어 메시지, 캐시 적용 후", measure(afterBad)));
        report.add("");
        write();
    }

    @Test
    void 금칙어_수에_따른_검사_비용_단순_탐색_대비_AhoCorasick() throws IOException {
        report.add("## 금칙어 수에 따른 검사 비용 (메시지 " + NORMAL_MESSAGE.length() + "자)");
        report.add("");
        report.add("| 금칙어 수 | 단순 탐색 (단어마다 contains) 평균 | Aho-Corasick 평균 |");
        report.add("| ---: | ---: | ---: |");
        for (int size : new int[]{100, 1_000, 10_000}) {
            Set<String> words = randomWords(new Random(size), size);
            BanWordValidator validator = new BanWordValidator(words, Set.of());
            String normalized = ContentNormalizer.normalize(NORMAL_MESSAGE);
            Supplier<Object> naive = () -> words.stream().anyMatch(normalized::contains);
            Supplier<Object> trie = () -> validator.checkBanWord(NORMAL_MESSAGE);
            report.add(String.format("| %,d | %.1f | %.1f |", size, measure(naive)[0], measure(trie)[0]));
        }
        report.add("");
        write();
    }

    private static BanWordValidator rebuild() {
        return new BanWordValidator(forbiddenWordRedisService.getAllForbiddenWords(), allowWordRedisService.getAllAllowWords());
    }

    /** @return [평균, p50, p95, p99] μs */
    private static double[] measure(Supplier<Object> task) {
        Object sink = null;
        for (int i = 0; i < WARMUP; i++) sink = task.get();
        long[] nanos = new long[ITERATIONS];
        for (int i = 0; i < ITERATIONS; i++) {
            long start = System.nanoTime();
            sink = task.get();
            nanos[i] = System.nanoTime() - start;
        }
        if (sink == null) throw new IllegalStateException();
        Arrays.sort(nanos);
        double avg = Arrays.stream(nanos).average().orElse(0) / 1_000.0;
        return new double[]{avg, pct(nanos, 50), pct(nanos, 95), pct(nanos, 99)};
    }

    private static double pct(long[] sorted, int p) {
        return sorted[Math.min(sorted.length - 1, (int) Math.ceil(p / 100.0 * sorted.length) - 1)] / 1_000.0;
    }

    private static String row(String name, double[] v) {
        return String.format("| %s | %.1f | %.1f | %.1f | %.1f |", name, v[0], v[1], v[2], v[3]);
    }

    private static Set<String> randomWords(Random random, int count) {
        Set<String> words = new LinkedHashSet<>();
        while (words.size() < count) {
            int length = 2 + random.nextInt(3);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < length; i++) sb.append((char) ('가' + random.nextInt(11_172)));
            words.add(sb.toString());
        }
        return words;
    }

    private static void write() throws IOException {
        Path out = Path.of("build/reports/benchmark/badword-filter.md");
        Files.createDirectories(out.getParent());
        Files.write(out, report);
    }
}
