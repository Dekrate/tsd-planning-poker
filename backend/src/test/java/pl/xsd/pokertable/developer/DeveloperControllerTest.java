package pl.xsd.pokertable.developer;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.xsd.pokertable.config.JwtAuthenticationEntryPoint;
import pl.xsd.pokertable.config.JwtRequestFilter;
import pl.xsd.pokertable.config.JwtUtil;
import pl.xsd.pokertable.exception.NotFoundException;
import pl.xsd.pokertable.participation.Participation;
import pl.xsd.pokertable.pokertable.PokerTable;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DeveloperController.class,
		excludeAutoConfiguration = {SecurityAutoConfiguration.class},
		excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtRequestFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class DeveloperControllerTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private DeveloperService developerService;

	@MockitoBean
	private UserDetailsService userDetailsService;

	@MockitoBean
	private JwtUtil jwtUtil;

	@MockitoBean
	private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	record NewDeveloperDto(String name, String email, String password) {}


	@Test
	void vote_ValidRequest_Returns204() throws Exception {
		mockMvc.perform(patch("/developers/1/vote")
						.param("tableId", "1")
						.param("vote", "5")
						.with(csrf())
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isNoContent());

		verify(developerService).vote(1L, 1L, 5);
	}

	@Test
	void vote_InvalidVoteValue_Returns400() throws Exception {
		doThrow(new IllegalArgumentException("Invalid vote"))
				.when(developerService).vote(anyLong(), anyLong(), anyInt());

		mockMvc.perform(patch("/developers/1/vote")
						.param("tableId", "1")
						.param("vote", "20")
						.with(csrf())
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isBadRequest());

		verify(developerService).vote(1L, 1L, 20);
	}

	@Test
	void vote_DeveloperNotFound_Returns404() throws Exception {
		doThrow(new NotFoundException("Developer not found"))
				.when(developerService).vote(anyLong(), anyLong(), anyInt());

		mockMvc.perform(patch("/developers/1/vote")
						.param("tableId", "1")
						.param("vote", "5")
						.with(csrf())
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isNotFound());
		verify(developerService).vote(1L, 1L, 5);
	}

	@Test
	void vote_PokerTableNotFound_Returns404() throws Exception {
		doThrow(new NotFoundException("Poker table not found"))
				.when(developerService).vote(anyLong(), anyLong(), anyInt());

		mockMvc.perform(patch("/developers/1/vote")
						.param("tableId", "999")
						.param("vote", "5")
						.with(csrf())
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isNotFound());
		verify(developerService).vote(1L, 999L, 5);
	}

	@Test
	void vote_DeveloperDoesNotBelongToTable_Returns400() throws Exception {
		doThrow(new IllegalArgumentException("Developer does not belong to this poker table."))
				.when(developerService).vote(anyLong(), anyLong(), anyInt());

		mockMvc.perform(patch("/developers/1/vote")
						.param("tableId", "2")
						.param("vote", "5")
						.with(csrf())
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isBadRequest());
		verify(developerService).vote(1L, 2L, 5);
	}


	@Test
	void createDeveloper_ValidRequest_Returns200() throws Exception {
		Developer developer = new Developer();
		developer.setId(1L);
		developer.setName("Test");

		when(developerService.createDeveloper(anyLong(), any()))
				.thenReturn(developer);

		mockMvc.perform(post("/developers")
						.param("pokerTableId", "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(developer))
						.with(csrf())
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1L))
				.andExpect(jsonPath("$.name").value("Test"));

		verify(developerService).createDeveloper(eq(1L), any(Developer.class));
	}

	@Test
	void createDeveloper_InvalidTable_Returns400() throws Exception {
		doThrow(new IllegalArgumentException("Invalid table"))
				.when(developerService).createDeveloper(anyLong(), any());

		mockMvc.perform(post("/developers")
						.param("pokerTableId", "999")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new Developer()))
						.with(csrf())
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isBadRequest());

		verify(developerService).createDeveloper(eq(999L), any(Developer.class));
	}


	@Test
	void hasVoted_True_ReturnsTrue() throws Exception {
		when(developerService.hasVoted(anyLong()))
				.thenReturn(true);

		mockMvc.perform(get("/developers/1/has-voted")
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isOk())
				.andExpect(content().string("true"));

		verify(developerService).hasVoted(1L);
	}

	@Test
	void hasVoted_False_ReturnsFalse() throws Exception {
		when(developerService.hasVoted(anyLong()))
				.thenReturn(false);

		mockMvc.perform(get("/developers/1/has-voted")
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isOk())
				.andExpect(content().string("false"));

		verify(developerService).hasVoted(1L);
	}

	@Test
	void hasVoted_DeveloperNotFound_Returns404() throws Exception {
		when(developerService.hasVoted(anyLong()))
				.thenThrow(new NotFoundException("Developer not found"));

		mockMvc.perform(get("/developers/999/has-voted")
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isNotFound());

		verify(developerService).hasVoted(999L);
	}


	@Test
	void getDeveloper_Exists_Returns200() throws Exception {
		Developer developer = new Developer();
		developer.setId(1L);
		developer.setName("Existing Dev");

		when(developerService.getDeveloper(anyLong()))
				.thenReturn(developer);

		mockMvc.perform(get("/developers/1")
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1L))
				.andExpect(jsonPath("$.name").value("Existing Dev"));

		verify(developerService).getDeveloper(1L);
	}

	@Test
	void getDeveloper_NotFound_Returns404() throws Exception {
		when(developerService.getDeveloper(anyLong()))
				.thenThrow(new NotFoundException("Developer not found"));

		mockMvc.perform(get("/developers/999")
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isNotFound());

		verify(developerService).getDeveloper(999L);
	}


	@Test
	void getDevelopersForTable_ValidTable_Returns200() throws Exception {
		Developer dev1 = new Developer();
		dev1.setId(1L);
		dev1.setName("Dev One");
		Developer dev2 = new Developer();
		dev2.setId(2L);
		dev2.setName("Dev Two");
		Set<Developer> developers = Set.of(dev1, dev2);

		when(developerService.getDevelopersForPokerTable(anyLong()))
				.thenReturn(developers);

		mockMvc.perform(get("/developers/poker-table/1")
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].id").exists())
				.andExpect(jsonPath("$[0].name").exists());

		verify(developerService).getDevelopersForPokerTable(1L);
	}

	@Test
	void getDevelopersForTable_InvalidTable_Returns404() throws Exception {
		when(developerService.getDevelopersForPokerTable(anyLong()))
				.thenThrow(new NotFoundException("Invalid table"));

		mockMvc.perform(get("/developers/poker-table/999")
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isNotFound());

		verify(developerService).getDevelopersForPokerTable(999L);
	}


	@Test
	void joinTable_newUser_createsDeveloper_Returns200() throws Exception {
		Developer newDev = new Developer("session123", "Test");
		newDev.setId(1L);
		PokerTable table = new PokerTable();
		table.setId(10L);
		table.setName("New Session");
		table.setCreatedAt(java.time.LocalDateTime.now());
		Map<String, Object> serviceResponse = Map.of(
				"developer", Map.of(
						"id", newDev.getId(),
						"name", newDev.getName(),
						"sessionId", newDev.getSessionId()
				),
				"table", Map.of(
						"id", table.getId(),
						"name", table.getName(),
						"createdAt", table.getCreatedAt()
				)
		);

		when(developerService.joinTable(anyString(), anyLong(), any(HttpSession.class)))
				.thenReturn(serviceResponse);

		mockMvc.perform(post("/developers/join")
						.param("name", "Test")
						.param("tableId", "10")
						.with(csrf())
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.developer.id").value(1L))
				.andExpect(jsonPath("$.developer.name").value("Test"))
				.andExpect(jsonPath("$.developer.sessionId").value("session123"))
				.andExpect(jsonPath("$.table.id").value(10L))
				.andExpect(jsonPath("$.table.name").value("New Session"));

		verify(developerService).joinTable(eq("Test"), eq(10L), any(HttpSession.class));
	}

	@Test
	void joinTable_existingUser_returnsExistingDeveloper_Returns200() throws Exception {
		Developer existingDev = new Developer("existingSession", "ExistingDev");
		existingDev.setId(5L);
		PokerTable table = new PokerTable();
		table.setId(20L);
		table.setName("Existing Session");
		table.setCreatedAt(java.time.LocalDateTime.now());

		Map<String, Object> serviceResponse = Map.of(
				"developer", Map.of(
						"id", existingDev.getId(),
						"name", existingDev.getName(),
						"sessionId", existingDev.getSessionId()
				),
				"table", Map.of(
						"id", table.getId(),
						"name", table.getName(),
						"createdAt", table.getCreatedAt()
				)
		);

		when(developerService.joinTable(anyString(), anyLong(), any(HttpSession.class)))
				.thenReturn(serviceResponse);

		mockMvc.perform(post("/developers/join")
						.param("name", "ExistingDev")
						.param("tableId", "20")
						.with(csrf())
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.developer.id").value(5L))
				.andExpect(jsonPath("$.developer.name").value("ExistingDev"))
				.andExpect(jsonPath("$.developer.sessionId").value("existingSession"))
				.andExpect(jsonPath("$.table.id").value(20L));

		verify(developerService).joinTable(eq("ExistingDev"), eq(20L), any(HttpSession.class));
	}


	@Test
	void joinTable_TableNotFound_Returns404() throws Exception {
		doThrow(new NotFoundException("Poker table not found"))
				.when(developerService).joinTable(anyString(), anyLong(), any(HttpSession.class));

		mockMvc.perform(post("/developers/join")
						.param("name", "Test")
						.param("tableId", "999")
						.with(csrf())
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isNotFound());

		verify(developerService).joinTable(eq("Test"), eq(999L), any(HttpSession.class));
	}

	@Test
	void joinTable_GenericError_Returns500() throws Exception {
		doThrow(new RuntimeException("Something went wrong"))
				.when(developerService).joinTable(anyString(), anyLong(), any(HttpSession.class));

		mockMvc.perform(post("/developers/join")
						.param("name", "Test")
						.param("tableId", "1")
						.with(csrf())
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.message").exists());
	}


	@Test
	void registerDeveloper_ValidRequest_Returns201() throws Exception {
		NewDeveloperDto registrationDto = new NewDeveloperDto("TestUser", "test@example.com", "password123");

		Developer newDeveloper = new Developer("TestUser", "test@example.com", "password123");
		newDeveloper.setId(1L);
		newDeveloper.setSessionId(UUID.randomUUID().toString());

		when(developerService.registerDeveloper(anyString(), anyString(), anyString()))
				.thenReturn(newDeveloper);

		mockMvc.perform(post("/developers/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(registrationDto))
						.with(csrf()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1L))
				.andExpect(jsonPath("$.name").value("TestUser"))
				.andExpect(jsonPath("$.email").value("test@example.com"))
				.andExpect(jsonPath("$.password").doesNotExist());

		verify(developerService).registerDeveloper(eq("TestUser"), eq("test@example.com"), eq("password123"));
	}

	@Test
	void registerDeveloper_InvalidData_Returns400() throws Exception {
		NewDeveloperDto invalidRegistrationDto = new NewDeveloperDto("", "invalid-email", "short");

		mockMvc.perform(post("/developers/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(invalidRegistrationDto))
						.with(csrf()))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(developerService);
	}

	@Test
	void registerDeveloper_EmailAlreadyExists_Returns400() throws Exception {
		NewDeveloperDto existingEmailDto = new NewDeveloperDto("ExistingUser", "existing@example.com", "password123");

		doThrow(new IllegalArgumentException("Developer with this email already exists."))
				.when(developerService).registerDeveloper(anyString(), anyString(), anyString());

		mockMvc.perform(post("/developers/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(existingEmailDto))
						.with(csrf()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Developer with this email already exists."));

		verify(developerService).registerDeveloper(eq("ExistingUser"), eq("existing@example.com"), eq("password123"));
	}

	@Test
	void loginDeveloper_ValidCredentials_ReturnsJwtTokenAndDeveloper() throws Exception {
		Map<String, String> loginRequest = Map.of("email", "test@example.com", "password", "password123");
		String jwtToken = "mocked.jwt.token";
		Developer mockDeveloper = new Developer("TestUser", "test@example.com", "password123");
		mockDeveloper.setId(1L);
		mockDeveloper.setSessionId(UUID.randomUUID().toString());

		// Mock service calls as per the new controller logic
		when(developerService.loginDeveloper(eq("test@example.com"), eq("password123")))
				.thenReturn(jwtToken);
		when(developerService.getDeveloperByEmail(eq("test@example.com")))
				.thenReturn(mockDeveloper);


		mockMvc.perform(post("/developers/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(loginRequest))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.jwt").value(jwtToken))
				.andExpect(jsonPath("$.developer.id").value(mockDeveloper.getId()))
				.andExpect(jsonPath("$.developer.name").value(mockDeveloper.getName()))
				.andExpect(jsonPath("$.developer.email").value(mockDeveloper.getEmail()))
				.andExpect(jsonPath("$.developer.password").doesNotExist());

		verify(developerService).loginDeveloper(eq("test@example.com"), eq("password123"));
		verify(developerService).getDeveloperByEmail(eq("test@example.com"));
	}

	@Test
	void loginDeveloper_InvalidCredentials_Returns400() throws Exception {
		Map<String, String> loginRequest = Map.of("email", "test@example.com", "password", "wrongpassword");
		doThrow(new IllegalArgumentException("Invalid email or password."))
				.when(developerService).loginDeveloper(anyString(), anyString());

		mockMvc.perform(post("/developers/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(loginRequest))
						.with(csrf()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Invalid email or password."));

		verify(developerService).loginDeveloper(eq("test@example.com"), eq("wrongpassword"));
		verify(developerService, never()).getDeveloperByEmail(anyString()); // Ensure getDeveloperByEmail is not called
	}

	@Test
	void loginDeveloper_MissingCredentials_Returns400() throws Exception {
		Map<String, String> loginRequest = Collections.singletonMap("email", "test@example.com");

		mockMvc.perform(post("/developers/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(loginRequest))
						.with(csrf()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Email and password must be provided")); // Adjusted expected message

		verifyNoInteractions(developerService);
	}

	@Test
	void getDeveloperHistory_DeveloperExists_ReturnsHistory() throws Exception {
		Long developerId = 1L;
		Set<Participation> history = new HashSet<>();
		PokerTable mockTable = new PokerTable();
		mockTable.setId(20L);
		mockTable.setName("Mock Table");
		mockTable.setCreatedAt(java.time.LocalDateTime.now());

		Developer mockDeveloper = new Developer("Test Developer", "test@example.com", "hashedPassword");
		mockDeveloper.setId(1L);

		history.add(new Participation(mockDeveloper, mockTable, 5));
		history.add(new Participation(mockDeveloper, new PokerTable(21L, "Another Mock Table", java.time.LocalDateTime.now(), false), 8));


		when(developerService.getDeveloperParticipationHistory(developerId)).thenReturn(history);

		mockMvc.perform(get("/developers/{developerId}/history", developerId)
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$.length()").value(2));

		verify(developerService).getDeveloperParticipationHistory(developerId);
	}

	@Test
	void getDeveloperHistory_DeveloperNotFound_Returns404() throws Exception {
		Long developerId = 999L;
		doThrow(new NotFoundException("Developer not found with ID: " + developerId))
				.when(developerService).getDeveloperParticipationHistory(developerId);

		mockMvc.perform(get("/developers/{developerId}/history", developerId)
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isNotFound());

		verify(developerService).getDeveloperParticipationHistory(developerId);
	}

	@Test
	void logout_Returns200() throws Exception {
		mockMvc.perform(post("/developers/logout")
						.with(csrf())
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isOk());

		verifyNoInteractions(developerService);
	}
}
