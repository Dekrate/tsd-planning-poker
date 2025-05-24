package pl.xsd.pokertable.developer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.xsd.pokertable.config.JwtRequestFilter;
import pl.xsd.pokertable.exception.NotFoundException;
import pl.xsd.pokertable.pokertable.PokerTable;
import pl.xsd.pokertable.pokertable.PokerTableDto;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DeveloperController.class,
		excludeAutoConfiguration = {SecurityAutoConfiguration.class},
		excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtRequestFilter.class))
@AutoConfigureMockMvc(addFilters = false)

class DeveloperControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private DeveloperService developerService;

	@Test
	@WithMockUser
	void vote_shouldReturnNoContentOnSuccess() throws Exception {
		Long developerId = 1L;
		Long tableId = 10L;
		Integer vote = 5;

		doNothing().when(developerService).vote(developerId, tableId, vote);

		mockMvc.perform(patch("/developers/{developerId}/vote", developerId)
						.param("tableId", tableId.toString())
						.param("vote", vote.toString())
						.with(csrf()))
				.andExpect(status().isNoContent());

		verify(developerService, times(1)).vote(developerId, tableId, vote);
	}

	@Test
	@WithMockUser
	void vote_shouldReturnBadRequestOnIllegalArgumentException() throws Exception {
		Long developerId = 1L;
		Long tableId = 10L;
		Integer vote = 0;

		doThrow(new IllegalArgumentException("Invalid vote")).when(developerService).vote(developerId, tableId, vote);

		mockMvc.perform(patch("/developers/{developerId}/vote", developerId)
						.param("tableId", tableId.toString())
						.param("vote", vote.toString())
						.with(csrf()))
				.andExpect(status().isBadRequest());
	}

	@Test
	@WithMockUser
	void vote_shouldReturnNotFoundOnNotFoundException() throws Exception {
		Long developerId = 1L;
		Long tableId = 10L;
		Integer vote = 5;

		doThrow(new NotFoundException("Developer not found")).when(developerService).vote(developerId, tableId, vote);

		mockMvc.perform(patch("/developers/{developerId}/vote", developerId)
						.param("tableId", tableId.toString())
						.param("vote", vote.toString())
						.with(csrf()))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser
	void createDeveloper_shouldReturnCreatedDeveloperDto() throws Exception {
		Long pokerTableId = 1L;
		Developer newDeveloper = new Developer(null, "New Dev", "new@example.com", "pass", null);
		Developer createdDeveloper = new Developer(2L, "New Dev", "new@example.com", "pass", new PokerTable(pokerTableId, "Table", false));
		DeveloperDto createdDeveloperDto = new DeveloperDto(createdDeveloper);

		when(developerService.createDeveloper(eq(pokerTableId), any(Developer.class))).thenReturn(createdDeveloper);

		mockMvc.perform(post("/developers")
						.param("pokerTableId", pokerTableId.toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(newDeveloper))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(createdDeveloperDto.getId()))
				.andExpect(jsonPath("$.name").value(createdDeveloperDto.getName()));

		verify(developerService, times(1)).createDeveloper(eq(pokerTableId), any(Developer.class));
	}

	@Test
	@WithMockUser
	void createDeveloper_shouldReturnBadRequestIfTableNotFound() throws Exception {
		Long pokerTableId = 99L;
		Developer newDeveloper = new Developer(null, "New Dev", "new@example.com", "pass", null);

		when(developerService.createDeveloper(eq(pokerTableId), any(Developer.class)))
				.thenThrow(new IllegalArgumentException("Tablica pokerowa o podanym ID nie istnieje"));

		mockMvc.perform(post("/developers")
						.param("pokerTableId", pokerTableId.toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(newDeveloper))
						.with(csrf()))
				.andExpect(status().isBadRequest());
	}

	@Test
	@WithMockUser
	void hasVoted_shouldReturnTrue() throws Exception {
		Long developerId = 1L;
		when(developerService.hasVoted(developerId)).thenReturn(true);

		mockMvc.perform(get("/developers/{developerId}/has-voted", developerId))
				.andExpect(status().isOk())
				.andExpect(content().string("true"));

		verify(developerService, times(1)).hasVoted(developerId);
	}

	@Test
	@WithMockUser
	void hasVoted_shouldReturnFalse() throws Exception {
		Long developerId = 1L;
		when(developerService.hasVoted(developerId)).thenReturn(false);

		mockMvc.perform(get("/developers/{developerId}/has-voted", developerId))
				.andExpect(status().isOk())
				.andExpect(content().string("false"));

		verify(developerService, times(1)).hasVoted(developerId);
	}

	@Test
	@WithMockUser
	void hasVoted_shouldReturnNotFoundOnNotFoundException() throws Exception {
		Long developerId = 99L;
		when(developerService.hasVoted(developerId)).thenThrow(new NotFoundException("Developer not found"));

		mockMvc.perform(get("/developers/{developerId}/has-voted", developerId))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser
	void getDeveloper_shouldReturnDeveloperDto() throws Exception {
		Long developerId = 1L;
		DeveloperDto developerDto = new DeveloperDto(developerId, "Test Dev", "test@example.com", 5);
		when(developerService.getDeveloper(developerId)).thenReturn(developerDto);

		mockMvc.perform(get("/developers/{developerId}", developerId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(developerDto.getId()))
				.andExpect(jsonPath("$.name").value(developerDto.getName()));

		verify(developerService, times(1)).getDeveloper(developerId);
	}

	@Test
	@WithMockUser
	void getDeveloper_shouldReturnNotFoundOnNotFoundException() throws Exception {
		Long developerId = 99L;
		when(developerService.getDeveloper(developerId)).thenThrow(new NotFoundException("Developer not found"));

		mockMvc.perform(get("/developers/{developerId}", developerId))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser
	void getAllDevelopers_shouldReturnSetOfDeveloperDtos() throws Exception {
		Long tableId = 10L;
		Set<DeveloperDto> developerDtos = new HashSet<>(List.of(
				new DeveloperDto(1L, "Dev1", "dev1@example.com", 5),
				new DeveloperDto(2L, "Dev2", "dev2@example.com", null)
		));
		when(developerService.getDevelopersForPokerTable(tableId)).thenReturn(developerDtos);

		mockMvc.perform(get("/developers/poker-table/{tableId}", tableId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].name").exists());

		verify(developerService, times(1)).getDevelopersForPokerTable(tableId);
	}

	@Test
	@WithMockUser
	void getAllDevelopers_shouldReturnNotFoundOnNotFoundException() throws Exception {
		Long tableId = 99L;
		when(developerService.getDevelopersForPokerTable(tableId)).thenThrow(new NotFoundException("Table not found"));

		mockMvc.perform(get("/developers/poker-table/{tableId}", tableId))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser(username = "test@example.com")
	void joinTable_shouldReturnJoinResponseDtoOnSuccess() throws Exception {
		Long tableId = 10L;
		DeveloperDto developerDto = new DeveloperDto(1L, "Test Dev", "test@example.com", null);
		PokerTableDto pokerTableDto = new PokerTableDto(tableId, "Test Table", null, false);
		JoinResponseDto joinResponseDto = new JoinResponseDto(developerDto, pokerTableDto);

		when(developerService.joinTable(anyString(), eq(tableId))).thenReturn(joinResponseDto);

		mockMvc.perform(post("/developers/join")
						.param("tableId", tableId.toString())
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.developer.id").value(developerDto.getId()))
				.andExpect(jsonPath("$.table.id").value(pokerTableDto.getId()));

		verify(developerService, times(1)).joinTable(anyString(), eq(tableId));
	}

	@Test
	void joinTable_shouldReturnUnauthorizedIfNotAuthenticated() throws Exception {
		mockMvc.perform(post("/developers/join")
						.param("tableId", "10")
						.with(csrf()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(username = "test@example.com")
	void joinTable_shouldReturnNotFoundOnNotFoundException() throws Exception {
		Long tableId = 99L;
		when(developerService.joinTable(anyString(), eq(tableId))).thenThrow(new NotFoundException("Table not found"));

		mockMvc.perform(post("/developers/join")
						.param("tableId", tableId.toString())
						.with(csrf()))
				.andExpect(status().isNotFound());
	}

	@Test
	void registerDeveloper_shouldReturnCreatedDeveloperDto() throws Exception {
		NewDeveloperDto newDeveloperDto = new NewDeveloperDto("New User", "new@example.com", "password");
		DeveloperDto registeredDeveloperDto = new DeveloperDto(1L, "New User", "new@example.com", null);

		when(developerService.registerDeveloper(anyString(), anyString(), anyString())).thenReturn(registeredDeveloperDto);

		mockMvc.perform(post("/developers/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(newDeveloperDto))
						.with(csrf()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(registeredDeveloperDto.getId()))
				.andExpect(jsonPath("$.name").value(registeredDeveloperDto.getName()));

		verify(developerService, times(1)).registerDeveloper(anyString(), anyString(), anyString());
	}

	@Test
	void registerDeveloper_shouldReturnBadRequestOnIllegalArgumentException() throws Exception {
		NewDeveloperDto newDeveloperDto = new NewDeveloperDto("New User", "existing@example.com", "password");

		when(developerService.registerDeveloper(anyString(), anyString(), anyString()))
				.thenThrow(new IllegalArgumentException("Developer with this email already exists."));

		mockMvc.perform(post("/developers/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(newDeveloperDto))
						.with(csrf()))
				.andExpect(status().isBadRequest());
	}

	@Test
	void registerDeveloper_shouldReturnBadRequestForInvalidInput() throws Exception {
		NewDeveloperDto invalidDeveloperDto = new NewDeveloperDto("", "invalid-email", "");

		mockMvc.perform(post("/developers/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(invalidDeveloperDto))
						.with(csrf()))
				.andExpect(status().isBadRequest()); // Oczekujemy BadRequest z powodu walidacji @Valid
	}

	@Test
	void loginDeveloper_shouldReturnLoginResponseDtoOnSuccess() throws Exception {
		String email = "test@example.com";
		String password = "password";
		String jwt = "mock.jwt.token";
		Developer developerEntity = new Developer(1L, "Test Dev", email, "encodedPass", null);
		DeveloperDto developerDto = new DeveloperDto(developerEntity);


		when(developerService.loginDeveloper(email, password)).thenReturn(jwt);
		when(developerService.getDeveloperByEmail(email)).thenReturn(developerEntity);

		mockMvc.perform(post("/developers/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("email", email, "password", password)))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.jwt").value(jwt))
				.andExpect(jsonPath("$.developer.id").value(developerDto.getId()));

		verify(developerService, times(1)).loginDeveloper(email, password);
		verify(developerService, times(1)).getDeveloperByEmail(email);
	}

	@Test
	void loginDeveloper_shouldReturnBadRequestForInvalidCredentials() throws Exception {
		String email = "test@example.com";
		String password = "wrongpassword";

		when(developerService.loginDeveloper(email, password))
				.thenThrow(new IllegalArgumentException("Invalid email or password."));

		mockMvc.perform(post("/developers/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("email", email, "password", password)))
						.with(csrf()))
				.andExpect(status().isBadRequest());
	}

	@Test
	void loginDeveloper_shouldReturnBadRequestForMissingCredentials() throws Exception {
		mockMvc.perform(post("/developers/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("email", "", "password", "password")))
						.with(csrf()))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post("/developers/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("email", "test@example.com", "password", "")))
						.with(csrf()))
				.andExpect(status().isBadRequest());
	}

	@Test
	@WithMockUser
	void logout_shouldReturnOk() throws Exception {
		mockMvc.perform(post("/developers/logout").with(csrf()))
				.andExpect(status().isOk());
	}
}
