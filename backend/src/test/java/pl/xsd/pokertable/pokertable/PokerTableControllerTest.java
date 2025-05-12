package pl.xsd.pokertable.pokertable;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.xsd.pokertable.developer.DeveloperService;
import pl.xsd.pokertable.exception.NotFoundException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PokerTableController.class)
class PokerTableControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PokerTableService pokerTableService;

	@MockitoBean
	private DeveloperService developerService;

	@Test
	void createTable_Success_Returns201() throws Exception {
		// Arrange
		PokerTable table = new PokerTable();
		table.setId(1L);
		table.setName("New Session");

		when(pokerTableService.createPokerTable(anyString()))
				.thenReturn(table);

		// Act & Assert
		mockMvc.perform(post("/tables"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1L))
				.andExpect(jsonPath("$.name").value("New Session"));

		// Verify
		verify(pokerTableService).createPokerTable("Blank");
	}

	@Test
	void createTable_ActiveTableExists_Returns400() throws Exception {
		// Arrange
		Mockito.doThrow(new IllegalStateException("Active table exists"))
				.when(pokerTableService).createPokerTable(anyString());

		// Act & Assert
		mockMvc.perform(post("/tables"))
				.andExpect(status().isBadRequest());

		// Verify
		verify(pokerTableService).createPokerTable("Blank");
	}

	@Test
	void closeTable_Success_Returns204() throws Exception {
		// Act & Assert
		mockMvc.perform(patch("/tables/1/close"))
				.andExpect(status().isNoContent());

		// Verify
		verify(pokerTableService).closePokerTable(1L);
	}

	@Test
	void closeTable_NotAllVoted_Returns400() throws Exception {
		// Arrange
		Mockito.doThrow(new IllegalStateException("Not all voted yet."))
				.when(pokerTableService).closePokerTable(anyLong());

		// Act & Assert
		mockMvc.perform(patch("/tables/1/close"))
				.andExpect(status().isBadRequest());

		// Verify
		verify(pokerTableService).closePokerTable(1L);
	}

	@Test
	void closeTable_NotFound_Returns404() throws Exception {
		// Arrange
		Mockito.doThrow(new NotFoundException("Table not found"))
				.when(pokerTableService).closePokerTable(anyLong());

		// Act & Assert
		mockMvc.perform(patch("/tables/999/close"))
				.andExpect(status().isNotFound());

		// Verify
		verify(pokerTableService).closePokerTable(999L);
	}


	@Test
	void getActiveTable_exists_returnsTable() throws Exception {
		// Arrange
		PokerTable table = new PokerTable();
		table.setId(1L);
		table.setName("Active Table");

		when(pokerTableService.getActiveTable()).thenReturn(table);

		// Act & Assert
		mockMvc.perform(get("/tables/active"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1L))
				.andExpect(jsonPath("$.name").value("Active Table"));

		// Verify
		verify(pokerTableService).getActiveTable();
	}

	@Test
	void getActiveTable_notExists_createsNewAndReturns() throws Exception {
		// Arrange
		PokerTable newTable = new PokerTable();
		newTable.setName("Default Table");

		when(pokerTableService.getActiveTable()).thenReturn(newTable);

		// Act & Assert
		mockMvc.perform(get("/tables/active"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").doesNotExist())
				.andExpect(jsonPath("$.name").value("Default Table"));

		// Verify
		verify(pokerTableService).getActiveTable();
	}

	@Test
	void getTableById_exists_returns200() throws Exception {
		// Arrange
		Long tableId = 123L;
		PokerTable table = new PokerTable();
		table.setId(tableId);
		table.setName("Specific Table");

		when(pokerTableService.getTableById(tableId)).thenReturn(table);

		// Act & Assert
		mockMvc.perform(get("/tables/{id}", tableId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(tableId))
				.andExpect(jsonPath("$.name").value("Specific Table"));

		// Verify
		verify(pokerTableService).getTableById(tableId);
	}

	@Test
	void getTableById_notFound_returns404() throws Exception {
		// Arrange
		Long tableId = 999L;

		when(pokerTableService.getTableById(tableId)).thenThrow(new NotFoundException("Table not found"));

		// Act & Assert
		mockMvc.perform(get("/tables/{id}", tableId))
				.andExpect(status().isNotFound());

		// Verify
		verify(pokerTableService).getTableById(tableId);
	}
}