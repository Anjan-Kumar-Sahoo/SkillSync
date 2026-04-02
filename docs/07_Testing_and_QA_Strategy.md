# 07 Testing and QA Strategy



---

## Content from: doc4_testing_strategy.md

# 📄 DOCUMENT 4: TESTING STRATEGY

> [!IMPORTANT]
> **Architecture Update (March 2026):** The backend architecture has been simplified:
> - **Mentor Service + Group Service → User Service** (port 8082)
> - **Review Service → Session Service** (port 8085)
>
> **CQRS + Redis Caching (March 2026):** All business services now use the **CQRS pattern** with **Redis 7.2** distributed caching. Unit tests mock `CacheService` to verify cache hit/miss/invalidation behavior. Integration tests include Redis via Testcontainers.
>
> Testing principles remain the same, but tests for merged services now reside in their new parent service modules. See `service_architecture_summary.md` for details.

## SkillSync — Comprehensive Testing Plan

---

## 4.1 Testing Pyramid

```
                    ┌──────────┐
                    │   E2E    │    15% — Critical user journeys
                    │ Playwright│    Slow, expensive, high confidence
                    ├──────────┤
                    │          │
                ┌───┤  Integ.  ├───┐  25% — Service interactions, API contracts
                │   │  Tests   │   │  Medium speed, medium confidence
                ├───┤          ├───┤
                │   │          │   │
            ┌───┤   ├──────────┤   ├───┐
            │   │   │          │   │   │  60% — Business logic, utilities
            │   │   │   Unit   │   │   │  Fast, cheap, frequent
            │   │   │  Tests   │   │   │
            └───┴───┴──────────┴───┴───┘
```

### Strategy at a Glance

| Layer | Tool | Scope | Target Coverage |
|---|---|---|---|
| Backend Unit | JUnit 5 + Mockito | Service & utility logic | 80% line coverage |
| Backend Integration | @SpringBootTest + Testcontainers | REST APIs, DB, RabbitMQ | 70% of endpoints |
| Frontend Unit | Jest + React Testing Library | Components, hooks, utilities | 75% line coverage |
| Frontend Component | Jest + RTL | Molecule/organism rendering | All user-facing components |
| E2E | Playwright | Critical user journeys | 5 core flows |

> [!IMPORTANT]
> **Implementation Status:** 16 base unit test classes (Service & Controller layers) have been implemented across all 8 business microservices using JUnit 5 and Mockito. All service tests have been updated to test CQRS `CommandService` and `QueryService` classes separately, including cache hit/miss/invalidation verification via mocked `CacheService`.
> **Mapper Tests:** Pure function `Mapper` classes have been extracted from QueryServices and are exhaustively unit tested without requiring a Spring Context (`MapperTest.java`).

---

## 4.2 Backend Testing

### 4.2.1 Unit Testing (JUnit 5 + Mockito)

#### CQRS Service Layer Testing — Cache Interactions

With the CQRS refactoring, service tests now verify **cache behavior** in addition to business logic:

```java
// Example: SkillQueryServiceTest.java — Testing cache-aside pattern
@ExtendWith(MockitoExtension.class)
class SkillQueryServiceTest {

    @Mock private SkillRepository skillRepository;
    @Mock private CacheService cacheService;
    @InjectMocks private SkillQueryService skillQueryService;

    @Test
    @DisplayName("Should return cached skill on cache HIT")
    void getSkillById_CacheHit_ReturnsFromRedis() {
        Skill cachedSkill = new Skill();
        cachedSkill.setId(1L);
        cachedSkill.setName("Java");

        when(cacheService.get("skill:1", Skill.class)).thenReturn(cachedSkill);

        Skill result = skillQueryService.getSkillById(1L);

        assertEquals("Java", result.getName());
        verify(skillRepository, never()).findById(any());  // DB never called
        verify(cacheService).get("skill:1", Skill.class);
    }

    @Test
    @DisplayName("Should query DB and populate cache on cache MISS")
    void getSkillById_CacheMiss_QueriesDbAndCaches() {
        Skill dbSkill = new Skill();
        dbSkill.setId(1L);
        dbSkill.setName("Java");

        when(cacheService.get("skill:1", Skill.class)).thenReturn(null);
        when(skillRepository.findById(1L)).thenReturn(Optional.of(dbSkill));

        Skill result = skillQueryService.getSkillById(1L);

        assertEquals("Java", result.getName());
        verify(skillRepository).findById(1L);               // DB called
        verify(cacheService).put(eq("skill:1"), any(), eq(3600L)); // Cached with TTL
    }
}

// Example: SkillCommandServiceTest.java — Testing cache invalidation
@ExtendWith(MockitoExtension.class)
class SkillCommandServiceTest {

    @Mock private SkillRepository skillRepository;
    @Mock private CacheService cacheService;
    @InjectMocks private SkillCommandService skillCommandService;

    @Test
    @DisplayName("Should invalidate cache after skill update")
    void updateSkill_InvalidatesCache() {
        Skill existing = new Skill();
        existing.setId(1L);
        existing.setName("Java");

        when(skillRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(skillRepository.save(any())).thenReturn(existing);

        skillCommandService.updateSkill(1L, updateRequest);

        verify(cacheService).evict("skill:1");              // Single key evicted
        verify(cacheService).evictByPattern("skill:all:*");  // Pattern eviction
    }
}
```

> [!TIP]
> **Key testing pattern:** QueryService tests verify `cacheService.get()` is called first, and `repository.findById()` is only called on cache miss. CommandService tests verify `cacheService.evict()` is called after every write operation.

#### Mapper Layer Testing (Pure Functions)

Mappers are pure functions that translate Entities to DTOs. They do not depend on Spring context and should be tested using simple JUnit tests.

```java
class MapperTest {
    @Test
    @DisplayName("Should map Session to SessionResponse correctly")
    void sessionMapper_mapsCorrectly() {
        Session session = new Session();
        session.setId(1L);
        session.setStatus(SessionStatus.ACCEPTED);
        
        SessionResponse response = SessionMapper.toResponse(session, "Mentor Name", "Learner Name");
        
        assertEquals(1L, response.id());
        assertEquals("ACCEPTED", response.status());
        assertEquals("Mentor Name", response.mentorName());
    }
}
```

#### Original Service Layer Testing (pre-CQRS reference)

```java
// SessionServiceTest.java
@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock private SessionRepository sessionRepository;
    @Mock private MentorServiceClient mentorServiceClient;
    @Mock private SessionEventPublisher eventPublisher;

    @InjectMocks private SessionServiceImpl sessionService;

    // ---------- Happy Path Tests ----------

    @Test
    @DisplayName("Should create session when mentor is available and approved")
    void createSession_Success() {
        // Arrange
        Long learnerId = 1L;
        Long mentorId = 1L;
        LocalDateTime sessionDate = LocalDateTime.now().plusDays(2);

        CreateSessionRequest request = new CreateSessionRequest(
            mentorId, "Java Basics", "Learn OOP", sessionDate, 60
        );

        MentorProfileResponse mentor = new MentorProfileResponse(
            mentorId, 1L, "John", "Doe", null,
            "Expert", 5, BigDecimal.valueOf(50), 4.5, 10, 20,
            "APPROVED", List.of(), List.of()
        );

        when(mentorServiceClient.getMentorById(mentorId)).thenReturn(mentor);
        when(sessionRepository.findConflictingSessions(
            eq(mentorId), any(), any()
        )).thenReturn(Collections.emptyList());
        when(sessionRepository.save(any(Session.class))).thenAnswer(
            invocation -> {
                Session s = invocation.getArgument(0);
                s.setId(1L);
                return s;
            }
        );

        // Act
        SessionResponse result = sessionService.createSession(request, learnerId);

        // Assert
        assertNotNull(result);
        assertEquals("REQUESTED", result.status());
        assertEquals(mentorId, result.mentorId());
        assertEquals(learnerId, result.learnerId());
        
        verify(eventPublisher).publishSessionRequested(any());
        verify(sessionRepository).save(any(Session.class));
    }

    // ---------- Validation Error Tests ----------

    @Test
    @DisplayName("Should throw exception when mentor is not approved")
    void createSession_MentorNotApproved_ThrowsException() {
        Long learnerId = 1L;
        Long mentorId = 1L;

        CreateSessionRequest request = new CreateSessionRequest(
            mentorId, "Topic", "Desc",
            LocalDateTime.now().plusDays(2), 60
        );

        MentorProfileResponse mentor = new MentorProfileResponse(
            mentorId, 1L, "John", "Doe", null,
            "Bio", 5, BigDecimal.valueOf(50), 0, 0, 0,
            "PENDING", List.of(), List.of()  // NOT APPROVED
        );

        when(mentorServiceClient.getMentorById(mentorId)).thenReturn(mentor);

        // Act & Assert
        MentorNotApprovedException exception = assertThrows(
            MentorNotApprovedException.class,
            () -> sessionService.createSession(request, learnerId)
        );

        assertEquals("MENTOR_NOT_APPROVED", exception.getErrorCode());
        verify(sessionRepository, never()).save(any());
        verify(eventPublisher, never()).publishSessionRequested(any());
    }

    @Test
    @DisplayName("Should throw exception when time slot conflicts with existing session")
    void createSession_ConflictingSession_ThrowsException() {
        Long learnerId = 1L;
        Long mentorId = 1L;
        LocalDateTime sessionDate = LocalDateTime.now().plusDays(2);

        CreateSessionRequest request = new CreateSessionRequest(
            mentorId, "Topic", null, sessionDate, 60
        );

        MentorProfileResponse mentor = new MentorProfileResponse(
            mentorId, 1L, "John", "Doe", null,
            "Bio", 5, BigDecimal.valueOf(50), 4.5, 10, 20,
            "APPROVED", List.of(), List.of()
        );

        Session conflicting = new Session();
        conflicting.setId(1L);

        when(mentorServiceClient.getMentorById(mentorId)).thenReturn(mentor);
        when(sessionRepository.findConflictingSessions(
            eq(mentorId), any(), any()
        )).thenReturn(List.of(conflicting));

        assertThrows(SessionConflictException.class,
            () -> sessionService.createSession(request, learnerId)
        );
    }

    // ---------- State Transition Tests ----------

    @Test
    @DisplayName("Should accept session that is in REQUESTED state")
    void acceptSession_FromRequested_Success() {
        Long sessionId = 1L;
        Long mentorUserId = 1L;
        
        Session session = new Session();
        session.setId(sessionId);
        session.setMentorId(1L);
        session.setStatus(SessionStatus.REQUESTED);

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any())).thenReturn(session);

        sessionService.acceptSession(sessionId, mentorUserId);

        assertEquals(SessionStatus.ACCEPTED, session.getStatus());
        verify(eventPublisher).publishSessionAccepted(any());
    }

    @Test
    @DisplayName("Should not allow accepting an already completed session")
    void acceptSession_FromCompleted_ThrowsException() {
        Long sessionId = 1L;
        
        Session session = new Session();
        session.setId(sessionId);
        session.setStatus(SessionStatus.COMPLETED);

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        assertThrows(InvalidStateTransitionException.class,
            () -> sessionService.acceptSession(sessionId, 1L)
        );
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for non-existent session")
    void acceptSession_NotFound_ThrowsException() {
        Long sessionId = 1L;

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> sessionService.acceptSession(sessionId, 1L)
        );
    }

    // ---------- Edge Case Tests ----------

    @Test
    @DisplayName("Should not allow learner to book their own mentor profile")
    void createSession_SelfBooking_ThrowsException() {
        Long userId = 1L;

        MentorProfileResponse mentor = new MentorProfileResponse(
            1L, userId /* same user */, "Self", "Mentor", null,
            "Bio", 5, BigDecimal.valueOf(50), 4.5, 10, 20,
            "APPROVED", List.of(), List.of()
        );

        CreateSessionRequest request = new CreateSessionRequest(
            mentor.id(), "Topic", null,
            LocalDateTime.now().plusDays(2), 60
        );

        when(mentorServiceClient.getMentorById(mentor.id())).thenReturn(mentor);

        assertThrows(IllegalArgumentException.class,
            () -> sessionService.createSession(request, userId)
        );
    }
}
```

#### Repository Testing

```java
// SessionRepositoryTest.java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class SessionRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("skillsync_session_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private SessionRepository sessionRepository;

    @Test
    @DisplayName("Should find conflicting sessions for a mentor in the same time window")
    void findConflictingSessions_ReturnsConflicts() {
        Long mentorId = 1L;
        LocalDateTime sessionDate = LocalDateTime.of(2026, 4, 1, 10, 0);

        Session existing = new Session();
        existing.setMentorId(mentorId);
        existing.setLearnerId(1L);
        existing.setTopic("Existing Session");
        existing.setSessionDate(sessionDate);
        existing.setDurationMinutes(60);
        existing.setStatus(SessionStatus.ACCEPTED);
        sessionRepository.save(existing);

        // Query for overlapping time
        LocalDateTime newStart = sessionDate.plusMinutes(30);
        LocalDateTime newEnd = newStart.plusMinutes(60);

        List<Session> conflicts = sessionRepository.findConflictingSessions(
            mentorId, newStart, newEnd
        );

        assertFalse(conflicts.isEmpty());
        assertEquals(existing.getId(), conflicts.get(0).getId());
    }

    @Test
    @DisplayName("Should return empty list when no conflicts exist")
    void findConflictingSessions_NoConflicts_ReturnsEmpty() {
        Long mentorId = 1L;

        List<Session> conflicts = sessionRepository.findConflictingSessions(
            mentorId,
            LocalDateTime.of(2026, 4, 1, 10, 0),
            LocalDateTime.of(2026, 4, 1, 11, 0)
        );

        assertTrue(conflicts.isEmpty());
    }
}
```

#### Controller Testing

```java
// SessionControllerTest.java
@WebMvcTest(SessionController.class)
@Import(SecurityConfig.class)
class SessionControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private SessionService sessionService;
    @Autowired private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/sessions - returns 201 for valid request")
    @WithMockUser(roles = "LEARNER")
    void createSession_ValidRequest_Returns201() throws Exception {
        CreateSessionRequest request = new CreateSessionRequest(
            1L, "Java Basics", "Learn OOP",
            LocalDateTime.now().plusDays(2), 60
        );

        SessionResponse response = new SessionResponse(
            1L, request.mentorId(), 1L,
            "Mentor", "Learner", request.topic(), request.description(),
            request.sessionDate(), 60, null, "REQUESTED", null,
            LocalDateTime.now(), LocalDateTime.now()
        );

        when(sessionService.createSession(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("REQUESTED"))
            .andExpect(jsonPath("$.topic").value("Java Basics"));
    }

    @Test
    @DisplayName("POST /api/sessions - returns 400 for missing required fields")
    @WithMockUser(roles = "LEARNER")
    void createSession_InvalidRequest_Returns400() throws Exception {
        String invalidJson = """
            {
                "mentorId": null,
                "topic": "",
                "sessionDate": null,
                "durationMinutes": 5
            }
            """;

        mockMvc.perform(post("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.details").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/sessions - returns 403 for MENTOR role")
    @WithMockUser(roles = "MENTOR")
    void createSession_WrongRole_Returns403() throws Exception {
        CreateSessionRequest request = new CreateSessionRequest(
            1L, "Topic", null,
            LocalDateTime.now().plusDays(2), 60
        );

        mockMvc.perform(post("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/sessions/{id}/accept - returns 409 for invalid state transition")
    @WithMockUser(roles = "MENTOR")
    void acceptSession_InvalidTransition_Returns422() throws Exception {
        Long sessionId = 1L;

        when(sessionService.acceptSession(eq(sessionId), any()))
            .thenThrow(new InvalidStateTransitionException("COMPLETED", "ACCEPTED"));

        mockMvc.perform(put("/api/sessions/" + sessionId + "/accept"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error").value("INVALID_STATE_TRANSITION"));
    }
}
```

### 4.2.2 Integration Testing

```java
// SessionServiceIntegrationTest.java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class SessionServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("skillsync_session_test");

    @Container
    static RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:3.12-management");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQ::getAmqpPort);
    }

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private SessionRepository sessionRepository;

    @Test
    @DisplayName("Full session lifecycle: create → accept → complete")
    void sessionLifecycle_FullFlow() {
        // 1. Create session
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(generateTestJwt("ROLE_LEARNER"));

        CreateSessionRequest createReq = new CreateSessionRequest(
            1L, "Integration Test", null,
            LocalDateTime.now().plusDays(2), 60
        );

        ResponseEntity<SessionResponse> createResp = restTemplate.exchange(
            "/api/sessions", HttpMethod.POST,
            new HttpEntity<>(createReq, headers),
            SessionResponse.class
        );

        assertEquals(HttpStatus.CREATED, createResp.getStatusCode());
        Long sessionId = createResp.getBody().id();

        // 2. Accept session (as mentor)
        HttpHeaders mentorHeaders = new HttpHeaders();
        mentorHeaders.setBearerAuth(generateTestJwt("ROLE_MENTOR"));

        ResponseEntity<Void> acceptResp = restTemplate.exchange(
            "/api/sessions/" + sessionId + "/accept", HttpMethod.PUT,
            new HttpEntity<>(mentorHeaders),
            Void.class
        );

        assertEquals(HttpStatus.OK, acceptResp.getStatusCode());

        // 3. Verify state in DB
        Session session = sessionRepository.findById(sessionId).orElseThrow();
        assertEquals(SessionStatus.ACCEPTED, session.getStatus());

        // 4. Complete session
        ResponseEntity<Void> completeResp = restTemplate.exchange(
            "/api/sessions/" + sessionId + "/complete", HttpMethod.PUT,
            new HttpEntity<>(mentorHeaders),
            Void.class
        );

        assertEquals(HttpStatus.OK, completeResp.getStatusCode());

        Session completedSession = sessionRepository.findById(sessionId).orElseThrow();
        assertEquals(SessionStatus.COMPLETED, completedSession.getStatus());
    }
}
```

---

### 4.2.3 CQRS Integration Testing with Redis

Integration tests now include a Redis container alongside PostgreSQL and RabbitMQ:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class SkillServiceCQRSIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.2-alpine")
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired private SkillQueryService queryService;
    @Autowired private SkillCommandService commandService;
    @Autowired private RedisTemplate<String, Object> redisTemplate;

    @Test
    @DisplayName("Full cache lifecycle: miss → populate → hit → invalidate → miss")
    void cacheLifecycle_FullFlow() {
        // 1. Create a skill (write)
        commandService.createSkill(new CreateSkillRequest("Java", "Programming", "desc"));

        // 2. First read → cache MISS → populates cache
        Skill first = queryService.getSkillById(1L);
        assertNotNull(first);

        // 3. Verify key exists in Redis
        assertNotNull(redisTemplate.opsForValue().get("skill:1"));

        // 4. Second read → cache HIT (no DB query)
        Skill second = queryService.getSkillById(1L);
        assertEquals(first.getName(), second.getName());

        // 5. Update skill → cache INVALIDATED
        commandService.updateSkill(1L, new UpdateSkillRequest("Java SE", "Programming", "desc"));

        // 6. Verify key removed from Redis
        assertNull(redisTemplate.opsForValue().get("skill:1"));

        // 7. Next read → cache MISS again → fresh data from DB
        Skill updated = queryService.getSkillById(1L);
        assertEquals("Java SE", updated.getName());
    }
}
```

---

## 4.3 Frontend Testing

### 4.3.1 Component Testing (Jest + React Testing Library)

```tsx
// components/molecules/MentorCard/MentorCard.test.tsx
import { render, screen, fireEvent } from '@testing-library/react';
import { MentorCard } from './MentorCard';

const mockMentor = {
  id: '123',
  firstName: 'Jane',
  lastName: 'Doe',
  avatarUrl: null,
  experienceYears: 5,
  avgRating: 4.5,
  totalReviews: 20,
  hourlyRate: 50,
  skills: [
    { id: '1', name: 'React' },
    { id: '2', name: 'TypeScript' },
    { id: '3', name: 'Node.js' },
    { id: '4', name: 'GraphQL' },
    { id: '5', name: 'AWS' },
  ],
  isAvailable: true,
};

describe('MentorCard', () => {
  it('renders mentor name and experience', () => {
    render(<MentorCard mentor={mockMentor} />);
    
    expect(screen.getByText('Jane Doe')).toBeInTheDocument();
    expect(screen.getByText('5+ years experience')).toBeInTheDocument();
  });

  it('displays hourly rate', () => {
    render(<MentorCard mentor={mockMentor} />);
    
    expect(screen.getByText('$50')).toBeInTheDocument();
    expect(screen.getByText('/hour')).toBeInTheDocument();
  });

  it('shows first 4 skills with overflow count', () => {
    render(<MentorCard mentor={mockMentor} />);
    
    expect(screen.getByText('React')).toBeInTheDocument();
    expect(screen.getByText('TypeScript')).toBeInTheDocument();
    expect(screen.getByText('Node.js')).toBeInTheDocument();
    expect(screen.getByText('GraphQL')).toBeInTheDocument();
    expect(screen.getByText('+1 more')).toBeInTheDocument();
    expect(screen.queryByText('AWS')).not.toBeInTheDocument();
  });

  it('calls onBookSession when Book Session button is clicked', () => {
    const onBookSession = jest.fn();
    render(<MentorCard mentor={mockMentor} onBookSession={onBookSession} />);
    
    fireEvent.click(screen.getByRole('button', { name: /book session/i }));
    
    expect(onBookSession).toHaveBeenCalledWith('123');
  });

  it('disables Book Session button when mentor is unavailable', () => {
    const unavailableMentor = { ...mockMentor, isAvailable: false };
    render(<MentorCard mentor={unavailableMentor} />);
    
    expect(screen.getByRole('button', { name: /book session/i })).toBeDisabled();
  });

  it('shows available status badge when mentor is available', () => {
    render(<MentorCard mentor={mockMentor} />);
    
    expect(screen.getByText(/available/i)).toBeInTheDocument();
  });
});
```

### 4.3.2 Form Component Testing

```tsx
// features/auth/components/LoginForm.test.tsx
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import { LoginForm } from './LoginForm';
import authReducer from '../slices/authSlice';
import { rest } from 'msw';
import { setupServer } from 'msw/node';

// Mock API server
const server = setupServer(
  rest.post('/api/auth/login', (req, res, ctx) => {
    return res(
      ctx.json({
        accessToken: 'mock-access-token',
        refreshToken: 'mock-refresh-token',
        expiresIn: 900,
        tokenType: 'Bearer',
        user: { id: '1', email: 'test@test.com', role: 'ROLE_LEARNER' },
      })
    );
  })
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const renderWithStore = (ui: React.ReactElement) => {
  const store = configureStore({
    reducer: { auth: authReducer },
  });
  return render(<Provider store={store}>{ui}</Provider>);
};

describe('LoginForm', () => {
  it('shows validation errors for empty submission', async () => {
    renderWithStore(<LoginForm />);
    
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));
    
    await waitFor(() => {
      expect(screen.getByText(/please enter a valid email/i)).toBeInTheDocument();
      expect(screen.getByText(/password must be at least 8 characters/i)).toBeInTheDocument();
    });
  });

  it('shows validation error for invalid email format', async () => {
    renderWithStore(<LoginForm />);
    
    await userEvent.type(screen.getByLabelText(/email/i), 'not-an-email');
    await userEvent.type(screen.getByLabelText(/password/i), 'validpassword1');
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));
    
    await waitFor(() => {
      expect(screen.getByText(/please enter a valid email/i)).toBeInTheDocument();
    });
  });

  it('submits form with valid credentials', async () => {
    renderWithStore(<LoginForm />);
    
    await userEvent.type(screen.getByLabelText(/email/i), 'test@test.com');
    await userEvent.type(screen.getByLabelText(/password/i), 'Password123!');
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));
    
    await waitFor(() => {
      expect(screen.queryByText(/invalid/i)).not.toBeInTheDocument();
    });
  });

  it('shows error message for invalid credentials', async () => {
    server.use(
      rest.post('/api/auth/login', (req, res, ctx) => {
        return res(
          ctx.status(401),
          ctx.json({
            timestamp: new Date().toISOString(),
            status: 401,
            error: 'AUTHENTICATION_FAILED',
            message: 'Invalid email or password',
            path: '/api/auth/login',
          })
        );
      })
    );

    renderWithStore(<LoginForm />);
    
    await userEvent.type(screen.getByLabelText(/email/i), 'wrong@test.com');
    await userEvent.type(screen.getByLabelText(/password/i), 'WrongPassword1!');
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));
    
    await waitFor(() => {
      expect(screen.getByText(/invalid email or password/i)).toBeInTheDocument();
    });
  });

  it('shows loading state during submission', async () => {
    server.use(
      rest.post('/api/auth/login', async (req, res, ctx) => {
        await new Promise((resolve) => setTimeout(resolve, 1000));
        return res(ctx.json({ accessToken: 'token' }));
      })
    );

    renderWithStore(<LoginForm />);
    
    await userEvent.type(screen.getByLabelText(/email/i), 'test@test.com');
    await userEvent.type(screen.getByLabelText(/password/i), 'Password123!');
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));
    
    expect(screen.getByRole('button', { name: /sign in/i })).toBeDisabled();
  });
});
```

### 4.3.3 Custom Hook Testing

```tsx
// features/auth/hooks/useAuth.test.tsx
import { renderHook, act } from '@testing-library/react-hooks';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import { useAuth } from './useAuth';
import authReducer from '../slices/authSlice';

const wrapper = ({ children }: { children: React.ReactNode }) => {
  const store = configureStore({ reducer: { auth: authReducer } });
  return <Provider store={store}>{children}</Provider>;
};

describe('useAuth', () => {
  it('returns isAuthenticated as false initially', () => {
    const { result } = renderHook(() => useAuth(), { wrapper });
    
    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.user).toBeNull();
  });

  it('updates state after successful login', async () => {
    const { result } = renderHook(() => useAuth(), { wrapper });
    
    await act(async () => {
      await result.current.login({
        email: 'test@test.com',
        password: 'Password123!',
      });
    });
    
    expect(result.current.isAuthenticated).toBe(true);
    expect(result.current.user).not.toBeNull();
  });

  it('clears state after logout', async () => {
    const { result } = renderHook(() => useAuth(), { wrapper });
    
    await act(async () => {
      await result.current.login({ email: 'test@test.com', password: 'pass' });
    });

    act(() => {
      result.current.logout();
    });
    
    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.user).toBeNull();
  });
});
```

### 4.3.4 API Service Testing

```tsx
// features/mentor/services/mentorApi.test.ts
import { mentorApi } from './mentorApi';
import { rest } from 'msw';
import { setupServer } from 'msw/node';

const server = setupServer(
  rest.get('/api/mentors/search', (req, res, ctx) => {
    const skill = req.url.searchParams.get('skillId');
    return res(
      ctx.json({
        content: [
          {
            id: '1', firstName: 'Test', lastName: 'Mentor',
            avgRating: 4.5, hourlyRate: 50,
            skills: [{ id: skill, name: 'React' }],
          },
        ],
        totalElements: 1,
        totalPages: 1,
        page: 0,
        size: 10,
      })
    );
  })
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('mentorApi', () => {
  it('search returns paginated results', async () => {
    const result = await mentorApi.search({
      skillId: 'skill-1',
      page: 0,
      size: 10,
    });
    
    expect(result.content).toHaveLength(1);
    expect(result.content[0].firstName).toBe('Test');
    expect(result.totalElements).toBe(1);
  });

  it('handles 500 error gracefully', async () => {
    server.use(
      rest.get('/api/mentors/search', (req, res, ctx) => {
        return res(ctx.status(500), ctx.json({
          status: 500,
          error: 'INTERNAL_ERROR',
          message: 'Internal server error',
        }));
      })
    );

    await expect(mentorApi.search({ page: 0, size: 10 }))
      .rejects
      .toThrow();
  });
});
```

---

## 4.4 End-to-End Testing (Playwright)

### 4.4.1 Configuration

```typescript
// playwright.config.ts
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [
    ['html'],
    ['junit', { outputFile: 'test-results/e2e-results.xml' }],
  ],
  use: {
    baseURL: 'http://localhost:3000',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'on-first-retry',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'firefox', use: { ...devices['Desktop Firefox'] } },
    { name: 'mobile-chrome', use: { ...devices['Pixel 5'] } },
  ],
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:3000',
    reuseExistingServer: !process.env.CI,
  },
});
```

### 4.4.2 E2E Test: Session Booking Flow

```typescript
// e2e/session-booking.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Session Booking Flow', () => {
  
  test.beforeEach(async ({ page }) => {
    // Login as learner
    await page.goto('/login');
    await page.fill('[name="email"]', 'learner@test.com');
    await page.fill('[name="password"]', 'TestPassword123!');
    await page.click('button[type="submit"]');
    await page.waitForURL('/');
  });

  test('complete session booking workflow', async ({ page }) => {
    // Step 1: Navigate to mentor discovery
    await page.click('a[href="/mentors"]');
    await expect(page).toHaveURL('/mentors');
    await expect(page.locator('h1')).toContainText('Find a Mentor');

    // Step 2: Search for a mentor by skill
    await page.fill('[data-testid="search-input"]', 'React');
    await page.waitForTimeout(500); // Debounce
    
    // Wait for results
    await expect(page.locator('[data-testid="mentor-card"]').first()).toBeVisible();

    // Step 3: Click on a mentor to view profile
    await page.click('[data-testid="mentor-card"]:first-child');
    await expect(page.locator('[data-testid="mentor-profile"]')).toBeVisible();

    // Step 4: Click "Book Session"
    await page.click('button:has-text("Book Session")');
    await expect(page.locator('[data-testid="book-session-modal"]')).toBeVisible();

    // Step 5: Fill booking form
    await page.fill('[name="topic"]', 'React Hooks Deep Dive');
    await page.click('[data-testid="date-picker"]');
    await page.click('[data-testid="date-next-week"]'); // Select a date next week
    await page.click('[data-testid="time-slot-10-00"]'); // Select 10:00 AM

    // Step 6: Submit
    await page.click('button:has-text("Confirm Booking")');

    // Step 7: Verify success
    await expect(page.locator('[data-testid="success-toast"]'))
      .toContainText('Session requested successfully');

    // Step 8: Verify in sessions list
    await page.click('a[href="/sessions"]');
    await expect(page.locator('[data-testid="session-card"]').first())
      .toContainText('React Hooks Deep Dive');
    await expect(page.locator('[data-testid="session-status"]').first())
      .toContainText('REQUESTED');
  });

  test('shows error for conflicting time slot', async ({ page }) => {
    await page.goto('/mentors');
    await page.click('[data-testid="mentor-card"]:first-child');
    await page.click('button:has-text("Book Session")');
    
    await page.fill('[name="topic"]', 'Conflict Test');
    // Select an already-booked slot (mocked via API)
    await page.click('[data-testid="date-picker"]');
    await page.click('[data-testid="date-today"]');
    await page.click('[data-testid="time-slot-14-00"]'); // Conflicting slot
    
    await page.click('button:has-text("Confirm Booking")');
    
    await expect(page.locator('[data-testid="error-message"]'))
      .toContainText('already booked');
  });
});
```

### 4.4.3 E2E Test: Login Flow

```typescript
// e2e/login.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Login Flow', () => {
  
  test('successful login redirects to dashboard', async ({ page }) => {
    await page.goto('/login');
    
    await page.fill('[name="email"]', 'learner@test.com');
    await page.fill('[name="password"]', 'TestPassword123!');
    await page.click('button[type="submit"]');
    
    await page.waitForURL('/');
    await expect(page.locator('h1')).toContainText('Dashboard');
  });

  test('shows error for invalid credentials', async ({ page }) => {
    await page.goto('/login');
    
    await page.fill('[name="email"]', 'wrong@test.com');
    await page.fill('[name="password"]', 'WrongPassword!');
    await page.click('button[type="submit"]');
    
    await expect(page.locator('[data-testid="form-error"]'))
      .toContainText('Invalid email or password');
    await expect(page).toHaveURL('/login');
  });

  test('shows validation errors for empty form', async ({ page }) => {
    await page.goto('/login');
    await page.click('button[type="submit"]');
    
    await expect(page.locator('text=Please enter a valid email')).toBeVisible();
    await expect(page.locator('text=Password must be at least 8 characters')).toBeVisible();
  });

  test('redirects to login when accessing protected route', async ({ page }) => {
    await page.goto('/mentors');
    await expect(page).toHaveURL(/\/login/);
  });

  test('redirect to intended page after login', async ({ page }) => {
    // Try to access mentor page while logged out
    await page.goto('/mentors');
    await expect(page).toHaveURL(/\/login/);

    // Login
    await page.fill('[name="email"]', 'learner@test.com');
    await page.fill('[name="password"]', 'TestPassword123!');
    await page.click('button[type="submit"]');

    // Should redirect back to mentors page
    await page.waitForURL('/mentors');
  });
});
```

### 4.4.4 E2E Test: Mentor Approval Flow

```typescript
// e2e/mentor-approval.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Mentor Approval Flow', () => {

  test('admin can approve a pending mentor application', async ({ page }) => {
    // Login as admin
    await page.goto('/login');
    await page.fill('[name="email"]', 'admin@test.com');
    await page.fill('[name="password"]', 'AdminPassword123!');
    await page.click('button[type="submit"]');
    await page.waitForURL('/admin');

    // Navigate to mentor approvals
    await page.click('a[href="/admin/mentors"]');
    await expect(page.locator('h1')).toContainText('Mentor Approvals');

    // Find pending application
    const firstPending = page.locator('[data-testid="mentor-approval-card"]').first();
    await expect(firstPending).toBeVisible();
    await expect(firstPending.locator('[data-testid="status-badge"]'))
      .toContainText('PENDING');

    // Approve
    await firstPending.locator('button:has-text("Approve")').click();
    
    // Confirm in dialog
    await page.click('button:has-text("Confirm")');

    // Verify status updated
    await expect(page.locator('[data-testid="success-toast"]'))
      .toContainText('Mentor approved successfully');
  });
});
```

---

## 4.5 Coverage Targets

| Metric | Target | Tool |
|---|---|---|
| Backend line coverage | ≥ 80% | JaCoCo |
| Backend branch coverage | ≥ 70% | JaCoCo |
| Frontend line coverage | ≥ 75% | Jest Coverage |
| Frontend branch coverage | ≥ 65% | Jest Coverage |
| Critical path coverage | ≥ 90% | Custom report |
| E2E scenario coverage | 5 core flows | Playwright |

### Critical Flows (Must Have ≥90% Coverage)

1. **User Registration + Login** — Auth Service + Frontend auth
2. **Session Booking (w/ Payment)** — Book → Razorpay Checkout → Accept → Complete → Review
3. **Mentor Onboarding** — Apply → Pay Mentor Fee → Auto-Approve / Admin fallback
4. **Mentor Discovery** — Search with filters, pagination
5. **Review Submission** — After completed session

---

## 4.6 Test Data Management

### Test Data Factory (Backend)

```java
public class TestDataFactory {

    public static Session createSession(SessionStatus status) {
        Session session = new Session();
        session.setId(1L);
        session.setMentorId(1L);
        session.setLearnerId(1L);
        session.setTopic("Test Topic");
        session.setSessionDate(LocalDateTime.now().plusDays(2));
        session.setDurationMinutes(60);
        session.setStatus(status);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        return session;
    }

    public static MentorProfileResponse createApprovedMentor() {
        return new MentorProfileResponse(
            1L, 1L,
            "Test", "Mentor", null, "Expert developer",
            10, BigDecimal.valueOf(75), 4.8, 50, 100,
            "APPROVED",
            List.of(new SkillSummary(1L, "Java", "Programming")),
            List.of()
        );
    }

    public static String generateTestJwt(String role) {
        return Jwts.builder()
            .setSubject(1L.toString())
            .claim("role", role)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 900000))
            .signWith(SignatureAlgorithm.HS512, "test-secret-key-12345678901234567890")
            .compact();
    }
}
```

### Test Data Factory (Frontend)

```typescript
// test/factories.ts
export const createMockMentor = (overrides?: Partial<MentorProfileResponse>): MentorProfileResponse => ({
  id: 123456789L,
  userId: 123456789L,
  firstName: 'Test',
  lastName: 'Mentor',
  avatarUrl: null,
  bio: 'Expert developer',
  experienceYears: 10,
  hourlyRate: 75,
  avgRating: 4.8,
  totalReviews: 50,
  totalSessions: 100,
  status: 'APPROVED',
  skills: [{ id: 123456789L, name: 'React', category: 'Frontend' }],
  availability: [],
  ...overrides,
});

export const createMockSession = (overrides?: Partial<SessionResponse>): SessionResponse => ({
  id: 123456789L,
  mentorId: 123456789L,
  learnerId: 123456789L,
  mentorName: 'Test Mentor',
  learnerName: 'Test Learner',
  topic: 'Test Session',
  description: 'Description',
  sessionDate: new Date(Date.now() + 86400000).toISOString(),
  durationMinutes: 60,
  meetingLink: null,
  status: 'REQUESTED',
  cancelReason: null,
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
  ...overrides,
});
```

---

## 4.7 CI Test Configuration

```yaml
# .github/workflows/test.yml (excerpt)
jobs:
  backend-tests:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_DB: skillsync_test
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        ports: ['5432:5432']
      rabbitmq:
        image: rabbitmq:3.12-management
        ports: ['5672:5672']
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - run: mvn test -pl session-service -Dspring.profiles.active=test
      - run: mvn jacoco:report -pl session-service
      - name: Check coverage threshold
        run: |
          COVERAGE=$(cat session-service/target/site/jacoco/index.html | grep -oP 'Total.*?(\d+)%' | grep -oP '\d+')
          if [ "$COVERAGE" -lt 80 ]; then
            echo "Coverage $COVERAGE% is below 80% threshold"
            exit 1
          fi

  frontend-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
      - run: cd frontend && npm ci
      - run: cd frontend && npm run test -- --coverage --watchAll=false
      - run: cd frontend && npx playwright install --with-deps
      - run: cd frontend && npx playwright test
```

---

> [!TIP]
> Run `npm run test:coverage` locally before pushing to avoid CI coverage failures.
> Use `npx playwright test --ui` for interactive E2E debugging.


---

## Content from: backend_testing_guide.md

# SkillSync Backend — Complete Testing Guide

> [!IMPORTANT]
> **Architecture Update (March 2026):** The following services have been merged:
> - **Mentor Service + Group Service → User Service** (port 8082)
> - **Review Service → Session Service** (port 8085)
>
> **CQRS + Redis Caching (March 2026):** All business services now use Redis 7.2 for distributed caching. Services require a running Redis instance on port 6379. If Redis is unavailable, services fall back to direct PostgreSQL queries with zero data loss.
>
> All code is now organized in **layered packages** with CQRS sub-packages (`service.command/`, `service.query/`, `cache/`) within each service.
>
> **Active services:**
> | # | Service | Port |
> |---|---------|------|
> | 1 | Eureka Server | 8761 |
> | 2 | Config Server | 8888 |
> | 3 | API Gateway | 8080 |
> | 4 | Auth Service | 8081 |
> | 5 | User Service (includes Mentor & Group) | 8082 |
> | 6 | Payment Service | 8086 |
> | 7 | Skill Service | 8084 |
> | 8 | Session Service (includes Review) | 8085 |
> | 9 | Notification Service | 8088 |

---

## 📋 STEP 1: Prerequisites

### 1.1 Create PostgreSQL Databases & Schemas

Open **psql** or **pgAdmin** and run:

```sql
-- Create databases
CREATE DATABASE skillsync_auth;
CREATE DATABASE skillsync_user;
CREATE DATABASE skillsync_skill;
CREATE DATABASE skillsync_session;
CREATE DATABASE skillsync_notification;
CREATE DATABASE skillsync_payment;

-- Create schemas inside each database
\c skillsync_auth
CREATE SCHEMA IF NOT EXISTS auth;

\c skillsync_user
CREATE SCHEMA IF NOT EXISTS users;
CREATE SCHEMA IF NOT EXISTS mentors;
CREATE SCHEMA IF NOT EXISTS groups;

\c skillsync_skill
CREATE SCHEMA IF NOT EXISTS skills;

\c skillsync_session
CREATE SCHEMA IF NOT EXISTS sessions;
CREATE SCHEMA IF NOT EXISTS reviews;

\c skillsync_notification
CREATE SCHEMA IF NOT EXISTS notifications;

\c skillsync_payment
CREATE SCHEMA IF NOT EXISTS payments;
```

> [!NOTE]
> If running via Docker, the `init-databases.sql` file handles this automatically on first container start. You only need to run the above when testing locally without Docker.

### 1.2 Install & Start RabbitMQ

Download from https://www.rabbitmq.com/download.html and start the service.  
Management UI: http://localhost:15672 (guest/guest)

> [!NOTE]
> If you don't have RabbitMQ, services that use it (User Service for mentor events, Session Service for session/review events, Notification Service) will still start but event publishing will fail silently. Auth and Skill services work fine without it.

### 1.3 Install & Start Redis

**Option A — Docker (Recommended):**
```powershell
docker run -d --name skillsync-redis -p 6379:6379 redis:7.2-alpine --appendonly yes --maxmemory 256mb --maxmemory-policy allkeys-lru
```

**Option B — Manual install:**
Download from https://redis.io/download and start the service.

Verify: `redis-cli ping` should return `PONG`

> [!NOTE]
> If Redis is not running, all services will still start and function correctly. Read operations will fall back to PostgreSQL queries. Cache hit/miss metrics will show 0 hits. This is by design (graceful degradation).

---

## 📋 STEP 2: Start Services

### Option A — Docker (Recommended)

```powershell
cd f:\SkillSync
docker-compose up --build
```

This starts everything (Postgres, RabbitMQ, all services, API Gateway ingress). Wait for all containers to become healthy.  
Verify: **http://localhost:8761** (Eureka Dashboard — all services should be registered)

### Option B — Local (for development/debugging)

Open **separate terminals** for each service and run:

```powershell
# Terminal 1 — Eureka Server (MUST start first, wait until ready)
cd f:\SkillSync\eureka-server
mvn spring-boot:run

# Terminal 2 — Config Server
cd f:\SkillSync\config-server
mvn spring-boot:run

# Terminal 3 — API Gateway
cd f:\SkillSync\api-gateway
mvn spring-boot:run

# Terminal 4 — Auth Service
cd f:\SkillSync\auth-service
mvn spring-boot:run

# Terminal 5 — User Service (serves User + Mentor + Group APIs)
cd f:\SkillSync\user-service
mvn spring-boot:run

# Terminal 6 — Skill Service
cd f:\SkillSync\skill-service
mvn spring-boot:run

# Terminal 7 — Session Service (serves Session + Review APIs)
cd f:\SkillSync\session-service
mvn spring-boot:run

# Terminal 8 — Payment Service
cd f:\SkillSync\payment-service
mvn spring-boot:run

# Terminal 9 — Notification Service
cd f:\SkillSync\notification-service
mvn spring-boot:run
```

### Verify Eureka Dashboard
Open: **http://localhost:8761**  
You should see all services registered.

---

## 📋 STEP 3: Test APIs (via Gateway at `localhost:8080`)

> [!IMPORTANT]
> All requests go through the **API Gateway** at port **8080**.  
> Use Postman, Insomnia, or `curl`. The examples below use `curl`.

---

### 🔐 3.1 AUTH SERVICE

#### Register User 1 (Learner)
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "learner@test.com",
    "password": "password123",
    "firstName": "Anjan",
    "lastName": "Sahoo"
  }'
```

#### Verify OTP (Check the registered email inbox for the OTP)
```bash
curl -X POST http://localhost:8080/api/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{
    "email": "learner@test.com",
    "otp": "<OTP_FROM_EMAIL>" 
  }'
```
> [!IMPORTANT]
> **Email verification is mandatory.** The OTP is always sent via real email to the registered address — even during testing. Check the email inbox (including spam/junk folder) for the OTP code. Login will be **blocked** until the email is verified.
>
> If you attempt to login without verifying your email, the service will automatically re-send a new OTP and reject the login with a clear error message.

**Expected Response** (save the `accessToken`!):
```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "expiresIn": 900,
  "tokenType": "Bearer",
  "user": {
    "id": 1,
    "email": "learner@test.com",
    "role": "ROLE_LEARNER",
    "firstName": "Anjan",
    "lastName": "Sahoo"
  }
}
```

#### Register User 2 (will become Mentor)
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "mentor@test.com",
    "password": "password123",
    "firstName": "Anshul",
    "lastName": "Kumar"
  }'

# IMPORTANT: Verify OTP from email inbox before logging in!
```

#### Verify OTP for Mentor
```bash
curl -X POST http://localhost:8080/api/auth/verify-otp \\
  -H "Content-Type: application/json" \\
  -d '{
    "email": "mentor@test.com",
    "otp": "<OTP_FROM_EMAIL>"
  }'
```

#### Register Admin User
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@test.com",
    "password": "password123",
    "firstName": "Admin",
    "lastName": "User"
  }'
```

#### Verify OTP for Admin
```bash
curl -X POST http://localhost:8080/api/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@test.com",
    "otp": "<OTP_FROM_EMAIL>"
  }'
```

Then manually promote to admin (direct call to Auth Service):  
```bash
curl -X PUT "http://localhost:8080/api/auth/users/3/role?role=ROLE_ADMIN"
```

#### Login (only works AFTER email verification)
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "learner@test.com",
    "password": "password123"
  }'
```

> [!WARNING]
> Login will fail with `"Email not verified"` error if OTP verification has not been completed. The service will auto-resend a new OTP on each failed login attempt due to unverified email.

#### Refresh Token
```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "<REFRESH_TOKEN_FROM_LOGIN>"
  }'
```

#### Validate Token
```bash
curl -X GET http://localhost:8080/api/auth/validate \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

---

### 🎯 3.2 SKILL SERVICE (create skills first — other services depend on these)

#### Create Skills
```bash
curl -X POST http://localhost:8080/api/skills \
  -H "Content-Type: application/json" \
  -d '{"name": "Java", "category": "Programming", "description": "Java programming language"}'

curl -X POST http://localhost:8080/api/skills \
  -H "Content-Type: application/json" \
  -d '{"name": "Spring Boot", "category": "Framework", "description": "Spring Boot framework"}'

curl -X POST http://localhost:8080/api/skills \
  -H "Content-Type: application/json" \
  -d '{"name": "React", "category": "Frontend", "description": "React.js library"}'

curl -X POST http://localhost:8080/api/skills \
  -H "Content-Type: application/json" \
  -d '{"name": "Python", "category": "Programming", "description": "Python programming language"}'
```

#### Get All Skills
```bash
curl http://localhost:8080/api/skills
```

#### Get Skill by ID
```bash
curl http://localhost:8080/api/skills/1
```

#### Search Skills
```bash
curl "http://localhost:8080/api/skills/search?q=java"
```

#### Update Skill
```bash
curl -X PUT http://localhost:8080/api/skills/1 \
  -H "Content-Type: application/json" \
  -d '{"name": "Java SE", "category": "Programming", "description": "Java Standard Edition"}'
```

---

### 👤 3.3 USER SERVICE

> [!NOTE]
> The Gateway passes `X-User-Id` header after JWT validation. For direct testing, pass it manually.

#### Update My Profile (as Learner, userId=1)
```bash
curl -X PUT http://localhost:8080/api/users/me \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>" \
  -H "X-User-Id: 1" \
  -d '{
    "firstName": "Anjan",
    "lastName": "Sahoo",
    "bio": "Full stack developer passionate about learning",
    "phone": "9876543210",
    "location": "India"
  }'
```

#### Get My Profile
```bash
curl http://localhost:8080/api/users/me \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>" \
  -H "X-User-Id: 1"
```

#### Add Skills to Profile
```bash
curl -X POST http://localhost:8080/api/users/me/skills \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>" \
  -H "X-User-Id: 1" \
  -d '{"skillId": 1, "proficiency": "INTERMEDIATE"}'

curl -X POST http://localhost:8080/api/users/me/skills \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>" \
  -H "X-User-Id: 1" \
  -d '{"skillId": 3, "proficiency": "BEGINNER"}'
```

#### Get Profile by ID
```bash
curl http://localhost:8080/api/users/1 \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>"
```

---

### 🎓 3.4 MENTOR APIs (served by User Service on port 8082)

#### Apply to Become Mentor (as User 2)
```bash
curl -X POST http://localhost:8080/api/mentors/apply \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <MENTOR_USER_ACCESS_TOKEN>" \
  -H "X-User-Id: 2" \
  -d '{
    "bio": "Experienced Java developer with 5 years in Spring Boot microservices. Passionate about teaching and mentoring junior developers in backend development.",
    "experienceYears": 5,
    "hourlyRate": 25.00,
    "skillIds": [1, 2]
  }'
```

#### View Pending Applications (Admin)
```bash
curl http://localhost:8080/api/mentors/pending \
  -H "Authorization: Bearer <ADMIN_ACCESS_TOKEN>" \
  -H "X-User-Id: 3"
```

#### Approve Mentor (Admin — mentorId=1)
```bash
curl -X PUT http://localhost:8080/api/mentors/1/approve \
  -H "Authorization: Bearer <ADMIN_ACCESS_TOKEN>" \
  -H "X-User-Id: 3"
```

#### Get Mentor Profile
```bash
curl http://localhost:8080/api/mentors/1 \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>"
```

#### Search Approved Mentors
```bash
curl http://localhost:8080/api/mentors/search \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>"
```

#### Add Availability (as Mentor, userId=2)
```bash
curl -X POST http://localhost:8080/api/mentors/me/availability \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <MENTOR_USER_ACCESS_TOKEN>" \
  -H "X-User-Id: 2" \
  -d '{"dayOfWeek": 1, "startTime": "09:00", "endTime": "17:00"}'

curl -X POST http://localhost:8080/api/mentors/me/availability \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <MENTOR_USER_ACCESS_TOKEN>" \
  -H "X-User-Id: 2" \
  -d '{"dayOfWeek": 3, "startTime": "10:00", "endTime": "18:00"}'
```

---

### 📅 3.5 SESSION APIs (served by Session Service on port 8085)

#### Book a Session (Learner books with Mentor)
```bash
curl -X POST http://localhost:8080/api/sessions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>" \
  -H "X-User-Id: 1" \
  -d '{
    "mentorId": 2,
    "topic": "Spring Boot Microservices",
    "description": "Want to learn about building microservices with Spring Boot",
    "sessionDate": "2026-04-01T10:00:00",
    "durationMinutes": 60
  }'
```

> [!WARNING]
> `sessionDate` must be at least **24 hours in the future**. Adjust the date accordingly!

#### Get Session by ID
```bash
curl http://localhost:8080/api/sessions/1 \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>" \
  -H "X-User-Id: 1"
```

#### Get My Sessions (as Learner)
```bash
curl http://localhost:8080/api/sessions/learner \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>" \
  -H "X-User-Id: 1"
```

#### Get My Sessions (as Mentor)
```bash
curl http://localhost:8080/api/sessions/mentor \
  -H "Authorization: Bearer <MENTOR_USER_ACCESS_TOKEN>" \
  -H "X-User-Id: 2"
```

#### Accept Session (Mentor accepts)
```bash
curl -X PUT http://localhost:8080/api/sessions/1/accept \
  -H "Authorization: Bearer <MENTOR_USER_ACCESS_TOKEN>" \
  -H "X-User-Id: 2"
```

#### Complete Session (Mentor completes)
```bash
curl -X PUT http://localhost:8080/api/sessions/1/complete \
  -H "Authorization: Bearer <MENTOR_USER_ACCESS_TOKEN>" \
  -H "X-User-Id: 2"
```

#### Reject Session (test with a second session)
```bash
# First create another session
curl -X POST http://localhost:8080/api/sessions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>" \
  -H "X-User-Id: 1" \
  -d '{
    "mentorId": 2,
    "topic": "React Basics",
    "description": "Want to learn React fundamentals",
    "sessionDate": "2026-04-02T14:00:00",
    "durationMinutes": 45
  }'

# Reject it
curl -X PUT "http://localhost:8080/api/sessions/2/reject?reason=Schedule%20conflict" \
  -H "Authorization: Bearer <MENTOR_USER_ACCESS_TOKEN>" \
  -H "X-User-Id: 2"
```

---

### ⭐ 3.6 REVIEW APIs (served by Session Service on port 8085 — Session must be COMPLETED first)

#### Submit Review (Learner reviews completed session)
```bash
curl -X POST http://localhost:8080/api/reviews \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>" \
  -H "X-User-Id: 1" \
  -d '{
    "sessionId": 1,
    "rating": 5,
    "comment": "Excellent session! The mentor explained Spring Boot microservices concepts very clearly with practical examples."
  }'
```

#### Get Mentor Reviews
```bash
curl http://localhost:8080/api/reviews/mentor/2 \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>"
```

#### Get Mentor Rating Summary
```bash
curl http://localhost:8080/api/reviews/mentor/2/summary \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>"
```

**Expected Response:**
```json
{
  "mentorId": 2,
  "averageRating": 5.0,
  "totalReviews": 1,
  "ratingDistribution": { "5": 1 }
}
```

#### Get My Submitted Reviews
```bash
curl http://localhost:8080/api/reviews/me \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>" \
  -H "X-User-Id: 1"
```

---

### 👥 3.7 GROUP APIs (served by User Service on port 8082)

#### Create a Learning Group
```bash
curl -X POST http://localhost:8080/api/groups \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>" \
  -H "X-User-Id: 1" \
  -d '{
    "name": "Spring Boot Study Group",
    "description": "A group for learning Spring Boot together",
    "maxMembers": 10
  }'
```

#### Join Group (User 2 joins)
```bash
curl -X POST http://localhost:8080/api/groups/1/join \
  -H "Authorization: Bearer <MENTOR_USER_ACCESS_TOKEN>" \
  -H "X-User-Id: 2"
```

#### Get All Groups
```bash
curl http://localhost:8080/api/groups \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>"
```

#### Get Group by ID
```bash
curl http://localhost:8080/api/groups/1 \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>"
```

#### Post Discussion
```bash
curl -X POST http://localhost:8080/api/groups/1/discussions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>" \
  -H "X-User-Id: 1" \
  -d '{"content": "Hey everyone! What topics should we cover first?", "parentId": null}'
```

#### Reply to Discussion
```bash
curl -X POST http://localhost:8080/api/groups/1/discussions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <MENTOR_USER_ACCESS_TOKEN>" \
  -H "X-User-Id: 2" \
  -d '{"content": "I suggest we start with Spring Boot basics and then move to microservices!", "parentId": 1}'
```

#### Get Group Discussions
```bash
curl http://localhost:8080/api/groups/1/discussions \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>"
```

---

### 🔔 3.8 NOTIFICATION SERVICE

#### Get My Notifications
```bash
curl http://localhost:8080/api/notifications \
  -H "Authorization: Bearer <MENTOR_USER_ACCESS_TOKEN>" \
  -H "X-User-Id: 2"
```

> [!TIP]
> If RabbitMQ is running, you should see notifications like "New Session Request", "Mentor Application Approved!" etc. that were automatically created by the event consumers.

#### Get Unread Count
```bash
curl http://localhost:8080/api/notifications/unread/count \
  -H "Authorization: Bearer <MENTOR_USER_ACCESS_TOKEN>" \
  -H "X-User-Id: 2"
```

#### Mark Notification as Read
```bash
curl -X PUT http://localhost:8080/api/notifications/1/read \
  -H "Authorization: Bearer <MENTOR_USER_ACCESS_TOKEN>"
```

#### Mark All as Read
```bash
curl -X PUT http://localhost:8080/api/notifications/read-all \
  -H "Authorization: Bearer <MENTOR_USER_ACCESS_TOKEN>" \
  -H "X-User-Id: 2"
```

---

### 💳 3.9 PAYMENT APIs (served by Payment Service on port 8086)

> [!IMPORTANT]
> Payment uses **Razorpay test credentials** by default. No real money is charged.
> The create-order endpoint creates a Razorpay order, and the verify endpoint validates the payment signature.

#### Create Payment Order (as Learner — Mentor Fee)
```bash
curl -X POST http://localhost:8080/api/payments/create-order \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>" \
  -H "X-User-Id: 2" \
  -d '{
    "type": "SESSION_BOOKING",
    "referenceId": 1,
    "referenceType": "SESSION_BOOKING"
  }'
```

**Expected Response:**
```json
{
  "orderId": "order_XXXXXXXXXX",
  "amount": 900,
  "currency": "INR",
  "status": "CREATED",
  "keyId": "rzp_test_SUxK0KnvPwKuAT"
}
```

> [!NOTE]
> In a real frontend flow, the `orderId` and `keyId` are passed to Razorpay's checkout SDK.
> The verify endpoint below is called **after** the user completes payment on the frontend.

#### Verify Payment (after Razorpay checkout)
```bash
curl -X POST http://localhost:8080/api/payments/verify \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>" \
  -H "X-User-Id: 2" \
  -d '{
    "razorpayOrderId": "order_XXXXXXXXXX",
    "razorpayPaymentId": "pay_XXXXXXXXXX",
    "razorpaySignature": "SIGNATURE_FROM_RAZORPAY"
  }'
```

#### Get My Payments
```bash
curl http://localhost:8080/api/payments/my-payments \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>" \
  -H "X-User-Id: 2"
```

#### Check Payment Status (inter-service)
```bash
curl "http://localhost:8080/api/payments/check?type=SESSION_BOOKING" \
  -H "X-User-Id: 2"
```

---

## 📋 STEP 4: End-to-End Flow (Recommended Test Order)

Follow this order for the complete happy path:

```
 1. Register learner@test.com
 2. Verify OTP for learner (check EMAIL INBOX — OTP is always sent via real email)
 3. Register mentor@test.com
 4. Verify OTP for mentor (check EMAIL INBOX)
 5. Register admin@test.com
 6. Verify OTP for admin (check EMAIL INBOX)
 7. Promote admin (PUT /api/auth/users/3/role?role=ROLE_ADMIN)
 7. Create 4 skills (Java, Spring Boot, React, Python)
 8. Update learner profile + add skills
 9. Mentor applies (POST /api/mentors/apply)
10. Admin approves mentor (PUT /api/mentors/1/approve)
11. (Optional) Book a session and pay for SESSION_BOOKING via Payment Service
12. >>> VERIFY PAYMENT (POST /api/payments/verify -- publishes event)
13. Login mentor again (to get updated ROLE_MENTOR token)
14. Add mentor availability
15. Learner books session with mentor
16. Mentor accepts session
17. Mentor completes session
18. Learner submits review
19. Check mentor rating summary
20. Create a group and post discussions
21. Check notifications (mentor should have: approval + session request + review alerts)
22. Check payment history (GET /api/payments/my-payments) — served by Payment Service
```

---

## 📋 STEP 5: Run Unit Tests (CQRS & Redis Validation)

Run the full test suite to guarantee cache correctness, fallback mechanics, and Saga consistency without hitting a live Redis instance.

```bash
# Run all tests across all services
mvn test
```

### Critical Test Coverages You Must Validate:

1. **Cache HIT Tests (`shouldReturnFromCache`)**
   - Validates that reading an existing key bypasses the PostgreSQL database entirely by verifying `repository.findById()` is called exactly `0` times (`never()`).
   
2. **Redis Failure Fallback Tests (`shouldFallbackToDbOnRedisFailure`)**
   - Injects a `RuntimeException("Redis connection refused")` when `cacheService.get()` is invoked.
   - Asserts the resilient design: the query seamlessly delegates to the database repository and returns accurate data without throwing 500s.

3. **Event-Driven Cache Invalidation Tests (`ReviewEventCacheSyncConsumerTest`)**
   - Proves RabbitMQ events reliably trigger cache evictions (e.g., `updateAvgRating()` is invoked on `ReviewSubmittedEvent`).

4. **Saga Consistency Tests (`PaymentSagaOrchestratorTest`)** — in payment-service
   - Verifies the `SUCCESS` branch publishes `payment.business.action` event via RabbitMQ.
   - Verifies the `COMPENSATION` branch marks payment as COMPENSATED and publishes compensation event.

---

## 📋 STEP 6: Swagger UI (API Gateway — Single Entry Point)

> [!IMPORTANT]
> Swagger UI is available **only** through the API Gateway. Individual service ports are **not exposed**.

Open: **http://localhost:8080/swagger-ui.html**

Use the **dropdown at the top** to select which service to view/test:

| Dropdown Option | APIs Shown |
|----------------|-----------|
| Auth Service | Register, Login, OTP, Token Refresh, Role Update |
| User Service (Users + Mentors + Groups) | Profiles, Skills, Mentor Apply/Approve, Groups, Discussions |
| Payment Service | Create Order, Verify Payment, Payment History |
| Skill Service | Skill CRUD, Search |
| Session Service (Sessions + Reviews) | Session Booking, Accept/Reject/Complete, Reviews, Ratings |
| Notification Service | Get Notifications, Unread Count, Mark Read |

> [!NOTE]
> When testing APIs that require authentication, pass the `X-User-Id` header directly (bypass JWT). For APIs needing a JWT token via the Gateway, first call `/api/auth/login` to get a token, then use it in the `Authorization` header as `Bearer <token>`.

---

## 🔧 Troubleshooting

| Issue | Solution |
|-------|----------|
| Service can't start | Check if the PostgreSQL database exists and the schema is created |
| `Connection refused` to Eureka | Start Eureka Server first and wait 30 seconds |
| `401 Unauthorized` via Gateway | Check the JWT token is valid and not expired (15 min lifetime) |
| RabbitMQ connection failed | Make sure RabbitMQ is running on port 5672 |
| Redis connection failed | Make sure Redis is running on port 6379 (`redis-cli ping`) |
| Cache not working (all misses) | Verify Redis is running and check `application.properties` for `spring.data.redis.host` |
| Stale data after update | Cache invalidation may be delayed; wait for TTL expiry or verify `evict()` is called |
| `Table not found` errors | JPA `ddl-auto=update` creates tables automatically, but schemas must exist first |
| Port already in use | Kill the process: `netstat -ano | findstr :PORT` then `taskkill /PID <PID> /F` |
| Docker: database init fails | Delete the postgres volume (`docker volume rm skillsync_postgres-data`) and rebuild |
| Docker: service can't reach postgres | Check if healthcheck passed — services wait for `service_healthy` condition |
| Swagger UI empty | Ensure all services are registered in Eureka and healthy |
| Payment create-order fails | Check Razorpay credentials in payment-service `application.properties` or env vars |
| Signature verification fails | Ensure you pass the exact `razorpaySignature` from Razorpay checkout response |



---

## Content from: doc7_full_qa_verification_report.md

# 🚀 SkillSync Final Pre-Production QA & Verification Report

**Auditor:** Senior Backend Engineer + QA Auditor  
**Date:** March 25, 2026  
**Scope:** Full Code-Level Verification (CQRS, Redis, RabbitMQ Event Sync, Saga Patterns)

---

## 🔍 PART 1: APPLICATION RUN VERIFICATION
**Status:** ✅ Working correctly  
**Observations:** 
- The newly implemented `skillsync-cache-common` module is successfully integrated into `user-service`, `skill-service`, `session-service`, and `notification-service`. 
- No Spring application context startup failures exist natively since all broken `@Mock` configurations in the Unit Testing environment and absent Jackson `RedisTemplate` serialization errors were resolved during final hardening.

## ⚡ PART 2: REDIS BEHAVIOR VERIFICATION
**Status:** ✅ Working correctly  
**Observations:** 
- **Cache MISS → DB → Cache:** Verified locally via `CacheService.getOrLoad`. A `ReentrantLock` successfully encapsulates the DB fallback ensuring *Stampede Protection*.
- **Cache HIT:** Explicitly tested via `shouldReturnFromCache()`. The `repository.findById()` is verified to be invoked `0` times (`never()`).
- **Redis DOWN Scenario:** Explicitly tested via `shouldFallbackToDbOnRedisFailure()`. The `CacheService` swallows `RedisConnectionException` gracefully, logging a warning, and executes the `Supplier<T> dbFallback.get()`. No 500 Internal Server Errors are thrown.

## 🔄 PART 3: CACHE INVALIDATION
**Status:** ✅ Working correctly  
**Observations:**
- `MentorCommandService.removeAvailability()` successfully evicts the correlated `mentorCaches` using the newly injected `CacheService.evictByPattern()`. 
- `MentorCommandService.apply()` correctly purges the `user:mentor:pending:*` cache pattern via `evictByPattern`. 
- CQRS implementations strictly prevent write-methods from bypassing cache invalidation streams.

## 🔁 PART 4: EVENT-DRIVEN CACHE SYNC
**Status:** ✅ Working correctly  
**Observations:**
- `ReviewEventCacheSyncConsumer` listens to `user.review.submitted.queue`.
- On receipt, it delegates to `MentorCommandService.updateAvgRating()`. 
- This method natively triggers `cacheService.evictByPattern(CacheService.vKey("user:mentor:*"))` ensuring eventual consistency for mentor profiles post-review.

## 🔗 PART 5: SAGA + CACHE CONSISTENCY
**Status:** ✅ Working correctly  
**Observations:**
- **Success Flow:** `PaymentSagaOrchestrator` triggers `approveMentor()`. The mentor is persisted as `APPROVED` and caching is invalidated.
- **Failure Flow:** On `COMPENSATION` event, `revertMentorApproval()` successfully degrades the profile back to `PENDING` and purges the stale caches preventing users from booking unverified mentors.

## 🧪 PART 6: TEST EXECUTION
**Status:** ✅ Working correctly  
**Observations:**
- **Tests Passing:** `mvn test` executed entirely across `user-service`, `session-service`, `skill-service`, and `notification-service`. All 100% of the test-suites are passing locally.
- **Key Formatting:** Verified strictly utilizing `v1:` prefix universally via `CacheService.vKey()`.

## ⚠️ PART 7: EDGE CASE TESTING
**Status:** ✅ Working correctly  
**Observations:**
- **Invalid IDs:** Cache penetration is protected by `NULL_SENTINEL_TTL` (60s). If an attacker spams ID `9999999`, it writes `__NULL__` to Redis preventing PostgreSQL overload.
- **High-frequency requests:** Java's `ConcurrentHashMap<String, ReentrantLock>` directly mitigates cache stampedes on the exact key boundaries.

## ⚡ PART 8: PERFORMANCE CHECK
**Status:** 🚀 Highly Satisfactory
**Observations:**
- `SCAN` operations successfully replaced `KEYS *` preventing accidental O(N) thread-blocking on the Redis cluster during mass cache evictions. 

## 🧠 PART 9: CODE CONSISTENCY CHECK
**Status:** ✅ Working correctly  
**Observations:**
- All duplicated logic `RedisConfig.java` and internal `CacheService.java` implementations were successfully purged and centralized into the `skillsync-cache-common` `pom.xml` dependency across all microservices.

## 🔐 PART 10: SECURITY VALIDATION
**Status:** ✅ Working correctly  
**Observations:**
- Payment transactions (`razorpay_order_id`, `signatures`), system OTPs, and JWT strings are strictly restricted to execution memory and `auth-service` databases. They are never serialized utilizing Jackson caching protocols.

---

## 📊 FINAL OUTPUT SUMMARY

### 1. ✅ Working correctly
The CQRS + Redis implementation is structurally sound. Service contexts are properly communicating with localized PostgreSQL dialects and distributed Redis clusters seamlessly.

### 2. ⚠️ Minor issues
None remaining post-hardening context. 

### 3. ❌ Bugs found
None. Critical bugs discovered in `MentorCommandService` missed caches and `ReviewEventCacheSyncConsumer` parameter mapping have been entirely eradicated.

### 4. 🚀 Improvements suggested
- **Circuit Breaker:** Introduce `Resilience4j` wrapping `CacheService` to actively short-circuit requests if Redis goes down, preventing the `1000ms` connection timeout from slowing down every respective API call during an outage.
- **Distributed Locks:** Migrate `ReentrantLock` (which only locks JVM-locally) to Redis-based Redisson locks if cache stampedes become distributed across Kubernetes pods.

### 5. 📊 Final confidence score (0–10)
**9.8 / 10**

### 6. 🧠 Final verdict
**Safe to Deploy / Production Ready!** 🚀
