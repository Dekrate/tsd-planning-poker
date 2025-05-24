package pl.xsd.pokertable.pokertable;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.xsd.pokertable.config.JwtRequestFilter;
import pl.xsd.pokertable.developer.Developer;
import pl.xsd.pokertable.developer.DeveloperService;
import pl.xsd.pokertable.exception.NotEveryoneVotedException;
import pl.xsd.pokertable.exception.NotFoundException;
import pl.xsd.pokertable.userstory.UserStory;
import pl.xsd.pokertable.userstory.UserStoryService;

import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PokerTableController.class,
		excludeAutoConfiguration = {SecurityAutoConfiguration.class},
		excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtRequestFilter.class))
@AutoConfigureMockMvc(addFilters = false)

class PokerTableControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private PokerTableService pokerTableService;
	@MockitoBean
	private UserStoryService userStoryService;
	@MockitoBean
	private DeveloperService developerService;

	@Test
	@WithMockUser
	void createTable_shouldReturnCreatedPokerTableDto() throws Exception {
		PokerTable newTable = new PokerTable(1L, "New Planning Session", false);
		PokerTableDto newTableDto = new PokerTableDto(newTable);

		when(pokerTableService.createPokerTable(anyString())).thenReturn(newTable);

		mockMvc.perform(post("/tables").with(csrf()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(newTableDto.getId()))
				.andExpect(jsonPath("$.name").value(newTableDto.getName()));

		verify(pokerTableService, times(1)).createPokerTable(anyString());
	}

	@Test
	@WithMockUser
	void getTableById_shouldReturnPokerTableDto() throws Exception {
		Long tableId = 1L;
		PokerTable table = new PokerTable(tableId, "Test Table", false);
		PokerTableDto tableDto = new PokerTableDto(table);

		when(pokerTableService.getTableById(tableId)).thenReturn(table);

		mockMvc.perform(get("/tables/{tableId}", tableId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(tableDto.getId()))
				.andExpect(jsonPath("$.name").value(tableDto.getName()));

		verify(pokerTableService, times(1)).getTableById(tableId);
	}

	@Test
	@WithMockUser
	void getTableById_shouldReturnNotFoundOnNotFoundException() throws Exception {
		Long tableId = 99L;
		when(pokerTableService.getTableById(tableId)).thenThrow(new NotFoundException("Table not found"));

		mockMvc.perform(get("/tables/{tableId}", tableId))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser
	void closeTable_shouldReturnNoContentOnSuccess() throws Exception {
		Long tableId = 1L;
		doNothing().when(pokerTableService).closePokerTable(tableId);

		mockMvc.perform(patch("/tables/{tableId}/close", tableId).with(csrf()))
				.andExpect(status().isNoContent());

		verify(pokerTableService, times(1)).closePokerTable(tableId);
	}

	@Test
	@WithMockUser
	void closeTable_shouldReturnBadRequestOnNotEveryoneVotedException() throws Exception {
		Long tableId = 1L;
		doThrow(new NotEveryoneVotedException("Not all voted")).when(pokerTableService).closePokerTable(tableId);

		mockMvc.perform(patch("/tables/{tableId}/close", tableId).with(csrf()))
				.andExpect(status().isBadRequest());
	}

	@Test
	@WithMockUser
	void closeTable_shouldReturnNotFoundOnNotFoundException() throws Exception {
		Long tableId = 99L;
		doThrow(new NotFoundException("Table not found")).when(pokerTableService).closePokerTable(tableId);

		mockMvc.perform(patch("/tables/{tableId}/close", tableId).with(csrf()))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser
	void getActiveTable_shouldReturnPokerTableDto() throws Exception {
		PokerTable activeTable = new PokerTable(1L, "Active Table", false);
		PokerTableDto activeTableDto = new PokerTableDto(activeTable);

		when(pokerTableService.getActiveTable()).thenReturn(activeTable);

		mockMvc.perform(get("/tables/active"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(activeTableDto.getId()))
				.andExpect(jsonPath("$.name").value(activeTableDto.getName()));

		verify(pokerTableService, times(1)).getActiveTable();
	}

	@Test
	@WithMockUser
	void getAllActiveTables_shouldReturnListOfPokerTableDtos() throws Exception {
		Set<PokerTable> activeTables = Set.of(
				new PokerTable(1L, "Table 1", false),
				new PokerTable(2L, "Table 2", false)
		);


		when(pokerTableService.getAllActiveTables()).thenReturn(activeTables);

		mockMvc.perform(get("/tables/all-active"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].name").exists());

		verify(pokerTableService, times(1)).getAllActiveTables();
	}

	@Test
	@WithMockUser
	void getUserStoriesForTable_shouldReturnSetOfUserStoryDtos() throws Exception {
		Long tableId = 1L;
		Set<UserStory> userStory = Set.of(
				new UserStory(1L, "Story 1", "Description 1", 5, null),
				new UserStory(2L, "Story 2", "Description 2", 8, null)
		);
		when(userStoryService.getUserStoriesForTable(tableId)).thenReturn(userStory);

		mockMvc.perform(get("/tables/{tableId}/user-stories", tableId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].title").exists());

		verify(userStoryService, times(1)).getUserStoriesForTable(tableId);
	}

	@Test
	@WithMockUser
	void getUserStoriesForTable_shouldReturnNotFoundOnNotFoundException() throws Exception {
		Long tableId = 99L;
		when(userStoryService.getUserStoriesForTable(tableId)).thenThrow(new NotFoundException("Table not found"));

		mockMvc.perform(get("/tables/{tableId}/user-stories", tableId))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser
	void exportUserStoriesToCsv_shouldReturnCsvFile() throws Exception {
		Long tableId = 1L;
		byte[] csvBytes = "header,value\nstory,5".getBytes();

		when(pokerTableService.exportUserStoriesToCsv(tableId)).thenReturn(csvBytes);

		mockMvc.perform(get("/tables/{tableId}/export-stories", tableId))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Disposition", "attachment; filename=\"user_stories_table_1.csv\""))
				.andExpect(header().string("Content-Type", "text/csv"))
				.andExpect(content().bytes(csvBytes));

		verify(pokerTableService, times(1)).exportUserStoriesToCsv(tableId);
	}

	@Test
	@WithMockUser
	void exportUserStoriesToCsv_shouldReturnNotFoundOnNotFoundException() throws Exception {
		Long tableId = 99L;
		when(pokerTableService.exportUserStoriesToCsv(tableId)).thenThrow(new NotFoundException("Table not found"));

		mockMvc.perform(get("/tables/{tableId}/export-stories", tableId))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser(username = "test@example.com")
	void getMyClosedTables_shouldReturnListOfPokerTableDtos() throws Exception {
		Long developerId = 1L;
		Developer developer = new Developer(developerId, "Test Dev", "test@example.com", "pass", null);
		Set<PokerTable> closedTables = Set.of(
				new PokerTable(1L, "Closed 1", true),
				new PokerTable(2L, "Closed 2", true)
		);
		Set<PokerTableDto> closedTableDtos = closedTables.stream().map(PokerTableDto::new).collect(Collectors.toUnmodifiableSet());

		when(developerService.getDeveloperByEmail("test@example.com")).thenReturn(developer);
		when(pokerTableService.getPastTablesForDeveloper(developerId)).thenReturn(closedTables);

		mockMvc.perform(get("/tables/my-closed"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].name").exists());

		verify(developerService, times(1)).getDeveloperByEmail("test@example.com");
		verify(pokerTableService, times(1)).getPastTablesForDeveloper(developerId);
	}

	@Test
	void getMyClosedTables_shouldReturnUnauthorizedIfNotAuthenticated() throws Exception {
		mockMvc.perform(get("/tables/my-closed"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(username = "test@example.com")
	void getMyClosedTables_shouldReturnNotFoundIfDeveloperNotFound() throws Exception {
		when(developerService.getDeveloperByEmail("test@example.com")).thenThrow(new NotFoundException("Developer not found"));

		mockMvc.perform(get("/tables/my-closed"))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser
	void resetAllVotes_shouldReturnNoContentOnSuccess() throws Exception {
		Long tableId = 1L;
		doNothing().when(pokerTableService).resetAllVotes(tableId);

		mockMvc.perform(post("/tables/{tableId}/reset-all-votes", tableId).with(csrf()))
				.andExpect(status().isNoContent());

		verify(pokerTableService, times(1)).resetAllVotes(tableId);
	}

	@Test
	@WithMockUser
	void resetAllVotes_shouldReturnNotFoundOnNotFoundException() throws Exception {
		Long tableId = 99L;
		doThrow(new NotFoundException("Table not found")).when(pokerTableService).resetAllVotes(tableId);

		mockMvc.perform(post("/tables/{tableId}/reset-all-votes", tableId).with(csrf()))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser
	void resetAllVotes_shouldReturnBadRequestOnIllegalStateException() throws Exception {
		Long tableId = 1L;
		doThrow(new IllegalStateException("Cannot reset votes for a closed poker table.")).when(pokerTableService).resetAllVotes(tableId);

		mockMvc.perform(post("/tables/{tableId}/reset-all-votes", tableId).with(csrf()))
				.andExpect(status().isBadRequest());
	}
}
