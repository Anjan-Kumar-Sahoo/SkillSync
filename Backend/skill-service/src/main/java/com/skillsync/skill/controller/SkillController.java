package com.skillsync.skill.controller;

import com.skillsync.skill.dto.*;
import com.skillsync.skill.service.command.SkillCommandService;
import com.skillsync.skill.service.query.SkillQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillCommandService skillCommandService;
    private final SkillQueryService skillQueryService;

    // ─── QUERIES ───

    @GetMapping
    public ResponseEntity<Page<SkillResponse>> getAllSkills(Pageable pageable) {
        return ResponseEntity.ok(skillQueryService.getAllSkills(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SkillResponse> getSkillById(@PathVariable Long id) {
        return ResponseEntity.ok(skillQueryService.getSkillById(id));
    }

    @GetMapping("/batch")
    public ResponseEntity<List<SkillResponse>> getSkillsByIds(@RequestParam List<Long> ids) {
        return ResponseEntity.ok(skillQueryService.getSkillsByIds(ids));
    }

    // ─── COMMANDS ───

    @PostMapping
    public ResponseEntity<SkillResponse> createSkill(@Valid @RequestBody CreateSkillRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(skillCommandService.createSkill(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SkillResponse> updateSkill(@PathVariable Long id,
            @Valid @RequestBody CreateSkillRequest request) {
        return ResponseEntity.ok(skillCommandService.updateSkill(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateSkill(@PathVariable Long id) {
        skillCommandService.deactivateSkill(id);
        return ResponseEntity.ok().build();
    }
}
