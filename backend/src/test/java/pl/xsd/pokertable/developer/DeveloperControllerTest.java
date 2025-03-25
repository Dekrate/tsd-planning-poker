package pl.xsd.pokertable.developer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.xsd.pokertable.exception.NotFoundException;
import pl.xsd.pokertable.pokertable.PokerTable;
import pl.xsd.pokertable.pokertable.PokerTableRepository;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DeveloperController.class)
class DeveloperControllerTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	@Autowired
	private MockMvc mockMvc;
	@MockitoBean
	private DeveloperService developerService;
	@MockitoBean
	private PokerTableRepository pokerTableRepository;

	// Testy dla endpointu /developers/{developerId}/vote
	@Test
	void vote_ValidRequest_Returns204() throws Exception {
		mockMvc.perform(patch("/developers/1/vote")
						.param("tableId", "1")
						.param("vote", "5"))
				.andExpect(status().isNoContent());
	}

	@Test
	void vote_InvalidVoteValue_Returns400() throws Exception {
		Mockito.doThrow(new IllegalArgumentException("Invalid vote"))
				.when(developerService).vote(anyLong(), anyLong(), anyInt());

		mockMvc.perform(patch("/developers/1/vote")
						.param("tableId", "1")
						.param("vote", "20"))
				.andExpect(status().isBadRequest());
	}

	// Testy dla endpointu /developers (POST)
	@Test
	void createDeveloper_ValidRequest_Returns200() throws Exception {
		Developer developer = new Developer();
		developer.setName("Test");

		when(developerService.createDeveloper(anyLong(), any()))
				.thenReturn(developer);

		mockMvc.perform(post("/developers")
						.param("pokerTableId", "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(developer)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Test"));
	}

	@Test
	void createDeveloper_InvalidTable_Returns400() throws Exception {
		Mockito.doThrow(new IllegalArgumentException("Invalid table"))
				.when(developerService).createDeveloper(anyLong(), any());

		mockMvc.perform(post("/developers")
						.param("pokerTableId", "999")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new Developer())))
				.andExpect(status().isBadRequest());
	}

	// Testy dla /developers/{developerId}/has-voted
	@Test
	void hasVoted_True_ReturnsTrue() throws Exception {
		when(developerService.hasVoted(anyLong()))
				.thenReturn(true);

		mockMvc.perform(get("/developers/1/has-voted"))
				.andExpect(status().isOk())
				.andExpect(content().string("true"));
	}

	@Test
	void hasVoted_False_ReturnsFalse() throws Exception {
		when(developerService.hasVoted(anyLong()))
				.thenReturn(false);

		mockMvc.perform(get("/developers/1/has-voted"))
				.andExpect(status().isOk())
				.andExpect(content().string("false"));
	}

	// Testy dla /developers/{developerId}
	@Test
	void getDeveloper_Exists_Returns200() throws Exception {
		Developer developer = new Developer();
		developer.setId(1L);

		when(developerService.getDeveloper(anyLong()))
				.thenReturn(developer);

		mockMvc.perform(get("/developers/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1L));
	}

	@Test
	void getDeveloper_NotFound_Returns404() throws Exception {
		when(developerService.getDeveloper(anyLong()))
				.thenThrow(new NotFoundException("Not found"));

		mockMvc.perform(get("/developers/999"))
				.andExpect(status().isNotFound());
	}

	// Testy dla /developers/poker-table/{tableId}
	@Test
	void getDevelopersForTable_ValidTable_Returns200() throws Exception {
		Set<Developer> developers = Collections.singleton(new Developer());

		when(developerService.getDevelopersForPokerTable(anyLong()))
				.thenReturn(developers);

		mockMvc.perform(get("/developers/poker-table/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());
	}

	@Test
	void getDevelopersForTable_InvalidTable_Returns404() throws Exception {
		when(developerService.getDevelopersForPokerTable(anyLong()))
				.thenThrow(new NotFoundException("Invalid table"));

		mockMvc.perform(get("/developers/poker-table/999"))
				.andExpect(status().isNotFound());
	}

	@Test
	void joinTable_newUser_createsDeveloper() throws Exception {
		when(developerService.joinTable(anyString(), any()))
				.thenReturn(Map.of(
						"developer", new Developer("session123", "Test"),
						"table", new PokerTable()
				));

		mockMvc.perform(post("/developers/join")
						.param("name", "Test"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.developer.sessionId").value("session123"));
	}
}