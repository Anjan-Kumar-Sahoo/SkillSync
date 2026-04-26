package com.skillsync.skill.repository;

import com.skillsync.skill.entity.Category;
import com.skillsync.skill.entity.Skill;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:skilldb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.sql.init.mode=always"
})
class SkillRepositoryTest {

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    @DisplayName("save, findById and deleteById for Skill")
    void shouldSaveFindAndDeleteSkill() {
        Skill skill = Skill.builder()
                .name("Repo Java")
                .category("Programming")
                .description("Repository flow")
                .isActive(true)
                .build();

        Skill saved = skillRepository.save(skill);

        assertNotNull(saved.getId());
        Skill loaded = skillRepository.findById(saved.getId()).orElseThrow();
        assertEquals("Repo Java", loaded.getName());

        skillRepository.deleteById(saved.getId());
        assertTrue(skillRepository.findById(saved.getId()).isEmpty());
    }

    @Test
    @DisplayName("searchByName matches case-insensitively")
    void searchByName_shouldMatchIgnoringCase() {
        skillRepository.save(Skill.builder()
                .name("Spring Boot")
                .category("Backend")
                .description("framework")
                .isActive(true)
                .build());
        skillRepository.save(Skill.builder()
                .name("JavaScript")
                .category("Frontend")
                .description("language")
                .isActive(true)
                .build());

        List<Skill> results = skillRepository.searchByName("SPRING");

        assertEquals(1, results.size());
        assertEquals("Spring Boot", results.get(0).getName());
    }

    @Test
    @DisplayName("findByIsActiveTrue returns only active skills")
    void findByIsActiveTrue_shouldFilterInactive() {
        skillRepository.save(Skill.builder()
                .name("Active Skill")
                .category("A")
                .description("active")
                .isActive(true)
                .build());
        skillRepository.save(Skill.builder()
                .name("Inactive Skill")
                .category("A")
                .description("inactive")
                .isActive(false)
                .build());

        Page<Skill> page = skillRepository.findByIsActiveTrue(PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals("Active Skill", page.getContent().get(0).getName());
    }

    @Test
    @DisplayName("existsByName checks duplicates")
    void existsByName_shouldReturnTrueWhenPresent() {
        skillRepository.save(Skill.builder()
                .name("Duplicate Name")
                .category("A")
                .description("x")
                .isActive(true)
                .build());

        assertTrue(skillRepository.existsByName("Duplicate Name"));
        assertFalse(skillRepository.existsByName("Missing Name"));
    }

    @Test
    @DisplayName("save, findById and delete for Category repository")
    void categoryRepository_shouldSupportCrud() {
        Category category = Category.builder().name("Programming").build();

        Category saved = categoryRepository.save(category);
        assertNotNull(saved.getId());

        Category loaded = categoryRepository.findById(saved.getId()).orElseThrow();
        assertEquals("Programming", loaded.getName());

        categoryRepository.delete(saved);
        assertTrue(categoryRepository.findById(saved.getId()).isEmpty());
    }
}
