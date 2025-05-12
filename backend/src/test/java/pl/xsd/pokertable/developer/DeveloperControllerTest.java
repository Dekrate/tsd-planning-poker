package pl.xsd.pokertable.developer;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.xsd.pokertable.exception.NotFoundException;
import pl.xsd.pokertable.pokertable.PokerTable;


import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DeveloperController.class)
class DeveloperControllerTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private DeveloperService developerService;


	@Test
	void vote_ValidRequest_Returns204() throws Exception {
		mockMvc.perform(patch("/developers/1/vote")
						.param("tableId", "1")
						.param("vote", "5"))
				.andExpect(status().isNoContent());

		verify(developerService).vote(1L, 1L, 5);
	}

	@Test
	void vote_InvalidVoteValue_Returns400() throws Exception {
		// Arrange
		Mockito.doThrow(new IllegalArgumentException("Invalid vote"))
				.when(developerService).vote(anyLong(), anyLong(), anyInt());

		// Act & Assert
		mockMvc.perform(patch("/developers/1/vote")
						.param("tableId", "1")
						.param("vote", "20"))
				.andExpect(status().isBadRequest());

		// Verify
		verify(developerService).vote(1L, 1L, 20);
	}

	@Test
	void vote_DeveloperNotFound_Returns404() throws Exception {
		// Arrange
		Mockito.doThrow(new NotFoundException("Developer not found"))
				.when(developerService).vote(anyLong(), anyLong(), anyInt());

		// Act & Assert
		mockMvc.perform(patch("/developers/1/vote")
						.param("tableId", "1")
						.param("vote", "5"))
				.andExpect(status().isNotFound()); // Spodziewamy się 404
		verify(developerService).vote(1L, 1L, 5);
	}

	@Test
	void vote_PokerTableNotFound_Returns404() throws Exception {
		// Arrange
		Mockito.doThrow(new NotFoundException("Poker table not found"))
				.when(developerService).vote(anyLong(), anyLong(), anyInt());

		// Act & Assert
		mockMvc.perform(patch("/developers/1/vote")
						.param("tableId", "999")
						.param("vote", "5"))
				.andExpect(status().isNotFound());
		verify(developerService).vote(1L, 999L, 5);
	}

	@Test
	void vote_DeveloperDoesNotBelongToTable_Returns400() throws Exception {
		// Arrange
		Mockito.doThrow(new IllegalArgumentException("Developer does not belong to this poker table."))
				.when(developerService).vote(anyLong(), anyLong(), anyInt());

		// Act & Assert
		mockMvc.perform(patch("/developers/1/vote")
						.param("tableId", "2")
						.param("vote", "5"))
				.andExpect(status().isBadRequest());
		verify(developerService).vote(1L, 2L, 5);
	}


	@Test
	void createDeveloper_ValidRequest_Returns200() throws Exception {
		// Arrange
		Developer developer = new Developer();
		developer.setId(1L);
		developer.setName("Test");

		when(developerService.createDeveloper(anyLong(), any()))
				.thenReturn(developer);

		// Act & Assert
		mockMvc.perform(post("/developers")
						.param("pokerTableId", "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(developer)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1L))
				.andExpect(jsonPath("$.name").value("Test"));

		// Verify
		verify(developerService).createDeveloper(eq(1L), any(Developer.class));
	}

	@Test
	void createDeveloper_InvalidTable_Returns400() throws Exception {
		// Arrange
		Mockito.doThrow(new IllegalArgumentException("Invalid table"))
				.when(developerService).createDeveloper(anyLong(), any());

		// Act & Assert
		mockMvc.perform(post("/developers")
						.param("pokerTableId", "999")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new Developer())))
				.andExpect(status().isBadRequest());

		// Verify
		verify(developerService).createDeveloper(eq(999L), any(Developer.class));
	}

	@Test
	void hasVoted_True_ReturnsTrue() throws Exception {
		// Arrange
		when(developerService.hasVoted(anyLong()))
				.thenReturn(true);

		// Act & Assert
		mockMvc.perform(get("/developers/1/has-voted"))
				.andExpect(status().isOk())
				.andExpect(content().string("true"));

		// Verify
		verify(developerService).hasVoted(1L);
	}

	@Test
	void hasVoted_False_ReturnsFalse() throws Exception {
		// Arrange
		when(developerService.hasVoted(anyLong()))
				.thenReturn(false);

		// Act & Assert
		mockMvc.perform(get("/developers/1/has-voted"))
				.andExpect(status().isOk())
				.andExpect(content().string("false"));

		// Verify
		verify(developerService).hasVoted(1L);
	}

	@Test
	void hasVoted_DeveloperNotFound_Returns404() throws Exception {
		// Arrange
		when(developerService.hasVoted(anyLong()))
				.thenThrow(new NotFoundException("Developer not found"));

		// Act & Assert
		mockMvc.perform(get("/developers/999/has-voted"))
				.andExpect(status().isNotFound());

		// Verify
		verify(developerService).hasVoted(999L);
	}


	// Testy dla /developers/{developerId}
	@Test
	void getDeveloper_Exists_Returns200() throws Exception {
		// Arrange
		Developer developer = new Developer();
		developer.setId(1L);
		developer.setName("Existing Dev");

		when(developerService.getDeveloper(anyLong()))
				.thenReturn(developer);

		// Act & Assert
		mockMvc.perform(get("/developers/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1L))
				.andExpect(jsonPath("$.name").value("Existing Dev"));

		// Verify
		verify(developerService).getDeveloper(1L);
	}

	@Test
	void getDeveloper_NotFound_Returns404() throws Exception {
		// Arrange
		when(developerService.getDeveloper(anyLong()))
				.thenThrow(new NotFoundException("Developer not found"));

		// Act & Assert
		mockMvc.perform(get("/developers/999"))
				.andExpect(status().isNotFound()); // Spodziewamy się 404

		// Verify
		verify(developerService).getDeveloper(999L);
	}

	@Test
	void getDevelopersForTable_ValidTable_Returns200() throws Exception {
		// Arrange
		Developer dev1 = new Developer();
		dev1.setId(1L);
		dev1.setName("Dev One");
		Developer dev2 = new Developer();
		dev2.setId(2L);
		dev2.setName("Dev Two");
		Set<Developer> developers = Set.of(dev1, dev2);

		when(developerService.getDevelopersForPokerTable(anyLong()))
				.thenReturn(developers);

		// Act & Assert
		mockMvc.perform(get("/developers/poker-table/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].id").exists())
				.andExpect(jsonPath("$[0].name").exists());

		// Verify
		verify(developerService).getDevelopersForPokerTable(1L);
	}

	@Test
	void getDevelopersForTable_InvalidTable_Returns404() throws Exception {
		// Arrange
		when(developerService.getDevelopersForPokerTable(anyLong()))
				.thenThrow(new NotFoundException("Invalid table"));

		// Act & Assert
		mockMvc.perform(get("/developers/poker-table/999"))
				.andExpect(status().isNotFound());

		// Verify
		verify(developerService).getDevelopersForPokerTable(999L);
	}

	@Test
	void joinTable_newUser_createsDeveloper_Returns200() throws Exception {
		// Arrange
		Developer newDev = new Developer("session123", "Test");
		newDev.setId(1L);
		PokerTable table = new PokerTable();
		table.setId(10L);
		table.setName("New Session");
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

		// Act & Assert
		mockMvc.perform(post("/developers/join")
						.param("name", "Test")
						.param("tableId", "10"))
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
		// Arrange
		Developer existingDev = new Developer("existingSession", "ExistingDev");
		existingDev.setId(5L);
		PokerTable table = new PokerTable();
		table.setId(20L);
		table.setName("Existing Session");

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

		// Act & Assert
		mockMvc.perform(post("/developers/join")
						.param("name", "ExistingDev")
						.param("tableId", "20"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.developer.id").value(5L))
				.andExpect(jsonPath("$.developer.name").value("ExistingDev"))
				.andExpect(jsonPath("$.developer.sessionId").value("existingSession"))
				.andExpect(jsonPath("$.table.id").value(20L));

		// Verify
		verify(developerService).joinTable(eq("ExistingDev"), eq(20L), any(HttpSession.class));
	}


	@Test
	void joinTable_TableNotFound_Returns404() throws Exception {
		// Arrange
		Mockito.doThrow(new NotFoundException("Poker table not found"))
				.when(developerService).joinTable(anyString(), anyLong(), any(HttpSession.class));

		// Act & Assert
		mockMvc.perform(post("/developers/join")
						.param("name", "Test")
						.param("tableId", "999"))
				.andExpect(status().isNotFound());

		// Verify
		verify(developerService).joinTable(eq("Test"), eq(999L), any(HttpSession.class));
	}

	@Test
	void joinTable_GenericError_Returns500() throws Exception {
		// Arrange
		Mockito.doThrow(new RuntimeException("Something went wrong"))
				.when(developerService).joinTable(anyString(), anyLong(), any(HttpSession.class));

		// Act & Assert
		mockMvc.perform(post("/developers/join")
						.param("name", "Test")
						.param("tableId", "1"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.message").exists());
	}

}