package com.boylu.controller.perf;

import com.boylu.common.RedisConstants;
import com.boylu.common.Result;
import com.boylu.utils.RedisUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/perf")
@RequiredArgsConstructor
@Api(tags = "门户-性能监控")
@Slf4j
public class PerfMonitorController {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RedisUtil redisUtil;

    @PostMapping("/report")
    @ApiOperation(value = "上报性能指标")
    public Result<Boolean> report(@RequestBody PerfReportRequest request) {
        if (request == null || StringUtils.isBlank(request.getEventType())) {
            return Result.success(true);
        }
        String day = LocalDate.now().format(DAY_FORMAT);
        if ("article_detail".equalsIgnoreCase(request.getEventType())) {
            incrAndExpire(RedisConstants.PERF_ARTICLE_LOAD_TOTAL + day, 2);
            if (request.getDurationMs() != null && request.getDurationMs() > 3000) {
                incrAndExpire(RedisConstants.PERF_ARTICLE_LOAD_OVER_3S + day, 2);
            }
        } else if ("upload".equalsIgnoreCase(request.getEventType())) {
            incrAndExpire(RedisConstants.PERF_UPLOAD_TOTAL + day, 2);
            if (!Boolean.TRUE.equals(request.getSuccess())) {
                incrAndExpire(RedisConstants.PERF_UPLOAD_FAILED + day, 2);
            }
        }
        evaluateAndWarn(day);
        return Result.success(true);
    }

    @GetMapping("/alert-status")
    @ApiOperation(value = "性能告警状态")
    public Result<Map<String, Object>> alertStatus() {
        String day = LocalDate.now().format(DAY_FORMAT);
        long articleTotal = readLong(RedisConstants.PERF_ARTICLE_LOAD_TOTAL + day);
        long articleOver3s = readLong(RedisConstants.PERF_ARTICLE_LOAD_OVER_3S + day);
        long uploadTotal = readLong(RedisConstants.PERF_UPLOAD_TOTAL + day);
        long uploadFailed = readLong(RedisConstants.PERF_UPLOAD_FAILED + day);

        double articleOver3sRate = articleTotal == 0 ? 0D : (articleOver3s * 1.0D / articleTotal);
        double uploadFailedRate = uploadTotal == 0 ? 0D : (uploadFailed * 1.0D / uploadTotal);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("articleTotal", articleTotal);
        data.put("articleOver3s", articleOver3s);
        data.put("articleOver3sRate", articleOver3sRate);
        data.put("uploadTotal", uploadTotal);
        data.put("uploadFailed", uploadFailed);
        data.put("uploadFailedRate", uploadFailedRate);
        data.put("articleAlert", articleOver3sRate > 0.10D);
        data.put("uploadAlert", uploadFailedRate > 0.05D);
        return Result.success(data);
    }

    private void evaluateAndWarn(String day) {
        long articleTotal = readLong(RedisConstants.PERF_ARTICLE_LOAD_TOTAL + day);
        long articleOver3s = readLong(RedisConstants.PERF_ARTICLE_LOAD_OVER_3S + day);
        long uploadTotal = readLong(RedisConstants.PERF_UPLOAD_TOTAL + day);
        long uploadFailed = readLong(RedisConstants.PERF_UPLOAD_FAILED + day);

        if (articleTotal >= 30) {
            double over3sRate = articleOver3s * 1.0D / articleTotal;
            if (over3sRate > 0.10D) {
                log.warn("Performance alert: article detail load >3s rate too high, day={}, total={}, over3s={}, rate={}",
                        day, articleTotal, articleOver3s, over3sRate);
            }
        }
        if (uploadTotal >= 20) {
            double failedRate = uploadFailed * 1.0D / uploadTotal;
            if (failedRate > 0.05D) {
                log.warn("Performance alert: upload failed rate too high, day={}, total={}, failed={}, rate={}",
                        day, uploadTotal, uploadFailed, failedRate);
            }
        }
    }

    private void incrAndExpire(String key, int days) {
        redisUtil.increment(key, 1);
        redisUtil.expire(key, Math.max(1, days), TimeUnit.DAYS);
    }

    private long readLong(String key) {
        Object value = redisUtil.get(key);
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ex) {
            return 0L;
        }
    }

    @Data
    public static class PerfReportRequest {
        private String eventType;
        private Long durationMs;
        private Boolean success;
    }
}
