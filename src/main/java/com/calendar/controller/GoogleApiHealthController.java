package com.calendar.controller;

import com.calendar.service.GoogleApiHealthCheck;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Google API健康检查控制器
 */
@RestController
@RequestMapping("/api/health")
@Tag(name = "健康检查", description = "API健康状态检查")
@RequiredArgsConstructor
public class GoogleApiHealthController {

    private final GoogleApiHealthCheck apiHealthCheck;

    @GetMapping("/google")
    @Operation(summary = "检查Google API连通性")
    public ResponseEntity<String> checkGoogleApiHealth() {
        boolean isAccessible = apiHealthCheck.checkGoogleApiAccess();
        if (isAccessible) {
            return ResponseEntity.ok("Google API 可以访问");
        } else {
            return ResponseEntity.status(503).body("Google API 无法访问");
        }
    }
}