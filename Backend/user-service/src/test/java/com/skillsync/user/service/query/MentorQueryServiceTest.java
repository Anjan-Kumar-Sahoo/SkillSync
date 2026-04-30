package com.skillsync.user.service.query;

import com.skillsync.cache.CacheService;
import com.skillsync.user.dto.*;
import com.skillsync.user.entity.MentorProfile;
import com.skillsync.user.entity.MentorSkill;
import com.skillsync.user.enums.MentorStatus;
import com.skillsync.user.feign.AuthServiceClient;
import com.skillsync.user.feign.SessionServiceClient;
import com.skillsync.user.feign.SkillServiceClient;
import com.skillsync.user.mapper.MentorMapper;
import com.skillsync.user.repository.MentorProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MentorQueryService Unit Tests")
class MentorQueryServiceTest {

    @Mock private MentorProfileRepository mentorProfileRepository;
    @Mock private CacheService cacheService;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private SessionServiceClient sessionServiceClient;
    @Mock private SkillServiceClient skillServiceClient;

    @InjectMocks private MentorQueryService mentorQueryService;

    private MentorProfile profile;
    private static final Long MENTOR_ID = 1L;
    private static final Long USER_ID = 100L;

    @BeforeEach
    void setUp() {
        profile = MentorProfile.builder()
                .id(MENTOR_ID)
                .userId(USER_ID)
                .bio("Expert mentor")
                .experienceYears(5)
                .hourlyRate(new BigDecimal("50.00"))
                .avgRating(4.0)
                .totalReviews(5)
                .totalSessions(10)
                .status(MentorStatus.APPROVED)
                .skills(new ArrayList<>())
                .build();
        ReflectionTestUtils.setField(mentorQueryService, "mentorTtl", 600L);
    }

    @Nested
    @DisplayName("getMentorById() & getMentorByUserId() tests")
    class GetMentorTests {
        
        @Test
        @DisplayName("getMentorById: Cache Miss -> Loads and Enriches")
        void getMentorById_CacheMiss() {
            // Arrange
            when(cacheService.getOrLoad(anyString(), eq(MentorProfileResponse.class), any(Duration.class), any()))
                    .thenAnswer(inv -> ((Supplier<MentorProfileResponse>) inv.getArgument(3)).get());
            when(mentorProfileRepository.findById(MENTOR_ID)).thenReturn(Optional.of(profile));
            mockEnrichmentSuccess();

            // Act
            MentorProfileResponse response = mentorQueryService.getMentorById(MENTOR_ID);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(MENTOR_ID);
            verify(mentorProfileRepository).findById(MENTOR_ID);
            verify(authServiceClient).getUserById(USER_ID);
        }

        @Test
        @DisplayName("getMentorById: Cache Hit -> Returns directly")
        void getMentorById_CacheHit() {
            // Arrange
            MentorProfileResponse cachedResponse = mock(MentorProfileResponse.class);
            when(cacheService.getOrLoad(anyString(), eq(MentorProfileResponse.class), any(Duration.class), any()))
                    .thenReturn(cachedResponse);

            // Act
            MentorProfileResponse response = mentorQueryService.getMentorById(MENTOR_ID);

            // Assert
            assertThat(response).isEqualTo(cachedResponse);
            verifyNoInteractions(mentorProfileRepository);
        }

        @Test
        @DisplayName("getMentorByUserId: Success path")
        void getMentorByUserId_Success() {
            when(cacheService.getOrLoad(anyString(), eq(MentorProfileResponse.class), any(Duration.class), any()))
                    .thenAnswer(inv -> ((Supplier<MentorProfileResponse>) inv.getArgument(3)).get());
            when(mentorProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
            mockEnrichmentSuccess();

            MentorProfileResponse response = mentorQueryService.getMentorByUserId(USER_ID);

            assertThat(response.userId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("Not Found: Returns null")
        void getMentor_NotFound() {
            when(cacheService.getOrLoad(anyString(), eq(MentorProfileResponse.class), any(Duration.class), any()))
                    .thenAnswer(inv -> ((Supplier<MentorProfileResponse>) inv.getArgument(3)).get());
            when(mentorProfileRepository.findById(anyLong())).thenReturn(Optional.empty());

            assertThat(mentorQueryService.getMentorById(999L)).isNull();
        }
    }

    @Nested
    @DisplayName("searchMentors() & Filtering tests")
    class SearchTests {
        
        @Test
        @DisplayName("No filters: Returns all approved mentors")
        void search_NoFilters() {
            Pageable pageable = PageRequest.of(0, 10);
            when(mentorProfileRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(profile)));
            mockEnrichmentSuccess();

            Page<MentorProfileResponse> result = mentorQueryService.searchMentors(pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(mentorProfileRepository).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        @DisplayName("Full filters: Validates skill resolution and spec call")
        void search_AllFilters() {
            Pageable pageable = PageRequest.of(0, 10);
            when(skillServiceClient.searchSkills("Java")).thenReturn(List.of(new SkillSummary(10L, "Java", null)));
            when(mentorProfileRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(profile)));
            mockEnrichmentSuccess();

            mentorQueryService.searchMentors("Java", 4.5, BigDecimal.TEN, new BigDecimal("100.00"), "Search term", pageable);

            verify(skillServiceClient).searchSkills("Java");
            verify(mentorProfileRepository).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        @DisplayName("Skill filter: Resolution returns empty -> returns empty page immediately")
        void search_SkillEmptyResolution() {
            Pageable pageable = PageRequest.of(0, 10);
            when(skillServiceClient.searchSkills("Unknown")).thenReturn(List.of());

            Page<MentorProfileResponse> result = mentorQueryService.searchMentors("Unknown", null, null, null, null, pageable);

            assertThat(result.getContent()).isEmpty();
            verifyNoInteractions(mentorProfileRepository);
        }

        @Test
        @DisplayName("Skill filter: Resolution fails -> returns empty page")
        void search_SkillResolutionFails() {
            Pageable pageable = PageRequest.of(0, 10);
            when(skillServiceClient.searchSkills(anyString())).thenThrow(new RuntimeException("Service Down"));

            Page<MentorProfileResponse> result = mentorQueryService.searchMentors("Java", null, null, null, null, pageable);

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("Blank strings: Handles blank skill/search as null")
        void search_BlankStrings() {
            Pageable pageable = PageRequest.of(0, 10);
            when(mentorProfileRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(profile)));
            mockEnrichmentSuccess();

            mentorQueryService.searchMentors(" ", null, null, null, " ", pageable);

            verify(skillServiceClient, never()).searchSkills(anyString());
        }
    }

    @Nested
    @DisplayName("enrichProfile() tests (indirectly via getMentorByUserId)")
    class EnrichmentTests {

        @BeforeEach
        void setupCache() {
            // Force cache miss to trigger enrichment logic
            when(cacheService.getOrLoad(anyString(), eq(MentorProfileResponse.class), any(Duration.class), any()))
                    .thenAnswer(inv -> ((Supplier<MentorProfileResponse>) inv.getArgument(3)).get());
            when(mentorProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
        }

        @Test
        @DisplayName("Success: Enriches user, skills, and metrics")
        void enrich_AllSuccess() {
            profile.getSkills().add(MentorSkill.builder().skillId(10L).mentor(profile).build());
            when(authServiceClient.getUserById(USER_ID)).thenReturn(Map.of("firstName", "AuthName"));
            when(skillServiceClient.getSkillsByIds(anyList())).thenReturn(List.of(new SkillSummary(10L, "Java", "Backend")));
            when(sessionServiceClient.getMentorMetrics(USER_ID)).thenReturn(Map.of("averageRating", 4.8));

            MentorProfileResponse response = mentorQueryService.getMentorByUserId(USER_ID);

            assertThat(response.firstName()).isEqualTo("AuthName");
            assertThat(response.skills().get(0).name()).isEqualTo("Java");
            assertThat(response.avgRating()).isEqualTo(4.8);
        }

        @Test
        @DisplayName("Failure: Auth Service Exception")
        void enrich_AuthFailure() {
            when(authServiceClient.getUserById(USER_ID)).thenThrow(new RuntimeException("Auth fail"));
            when(sessionServiceClient.getMentorMetrics(USER_ID)).thenReturn(Map.of());

            MentorProfileResponse response = mentorQueryService.getMentorByUserId(USER_ID);

            assertThat(response).isNotNull();
            verify(authServiceClient).getUserById(USER_ID);
        }

        @Test
        @DisplayName("Failure: Skill Enrichment Exception")
        void enrich_SkillFailure() {
            profile.getSkills().add(MentorSkill.builder().skillId(10L).mentor(profile).build());
            when(authServiceClient.getUserById(anyLong())).thenReturn(Map.of());
            when(skillServiceClient.getSkillsByIds(anyList())).thenThrow(new RuntimeException("Skill fail"));
            when(sessionServiceClient.getMentorMetrics(anyLong())).thenReturn(Map.of());

            MentorProfileResponse response = mentorQueryService.getMentorByUserId(USER_ID);

            assertThat(response.skills()).hasSize(1);
            verify(skillServiceClient).getSkillsByIds(anyList());
        }

        @Test
        @DisplayName("Failure: Session Metrics Exception")
        void enrich_MetricsFailure() {
            when(authServiceClient.getUserById(anyLong())).thenReturn(Map.of());
            when(sessionServiceClient.getMentorMetrics(USER_ID)).thenThrow(new RuntimeException("Session fail"));
            
            mentorQueryService.getMentorByUserId(USER_ID);

            verify(sessionServiceClient).getMentorMetrics(USER_ID);
        }

        @Test
        @DisplayName("Metrics: Handles non-Number types and nulls")
        void enrich_MetricHandling() {
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("averageRating", "Not a number");
            metrics.put("totalReviews", null);
            metrics.put("completedSessions", 50);

            when(authServiceClient.getUserById(anyLong())).thenReturn(Map.of());
            when(sessionServiceClient.getMentorMetrics(USER_ID)).thenReturn(metrics);

            MentorProfileResponse response = mentorQueryService.getMentorByUserId(USER_ID);

            assertThat(response.avgRating()).isEqualTo(4.0);
            assertThat(response.totalSessions()).isEqualTo(50);
        }

        @Test
        @DisplayName("Skills: Skips enrichment if skills list is null or empty")
        void enrich_SkipSkills() {
            when(authServiceClient.getUserById(anyLong())).thenReturn(Map.of());
            when(sessionServiceClient.getMentorMetrics(anyLong())).thenReturn(Map.of());

            profile.setSkills(null);
            mentorQueryService.getMentorByUserId(USER_ID);
            verify(skillServiceClient, never()).getSkillsByIds(anyList());

            profile.setSkills(new ArrayList<>());
            mentorQueryService.getMentorByUserId(USER_ID);
            verify(skillServiceClient, never()).getSkillsByIds(anyList());
        }
    }

    @Nested
    @DisplayName("Pagination logic tests")
    class PaginationTests {
        @Test
        @DisplayName("First Page: Success")
        void getPending_FirstPage() {
            Pageable pageable = PageRequest.of(0, 1);
            when(mentorProfileRepository.findByStatus(eq(MentorStatus.PENDING), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(profile), pageable, 2));
            mockEnrichmentSuccess();

            Page<MentorProfileResponse> result = mentorQueryService.getPendingApplications(pageable);

            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.hasNext()).isTrue();
        }

        @Test
        @DisplayName("Empty Results: Success")
        void getPending_Empty() {
            Pageable pageable = PageRequest.of(0, 10);
            when(mentorProfileRepository.findByStatus(any(), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            Page<MentorProfileResponse> result = mentorQueryService.getPendingApplications(pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

    private void mockEnrichmentSuccess() {
        lenient().when(authServiceClient.getUserById(anyLong())).thenReturn(new HashMap<>());
        lenient().when(sessionServiceClient.getMentorMetrics(anyLong())).thenReturn(new HashMap<>());
        lenient().when(skillServiceClient.getSkillsByIds(anyList())).thenReturn(new ArrayList<>());
    }
}
