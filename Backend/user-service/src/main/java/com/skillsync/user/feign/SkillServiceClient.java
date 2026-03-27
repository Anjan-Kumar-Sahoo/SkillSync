package com.skillsync.user.feign;

import com.skillsync.user.dto.SkillSummary;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "skill-service")
public interface SkillServiceClient {
    @GetMapping("/api/skills/{id}")
    SkillSummary getSkillById(@PathVariable Long id);
}
