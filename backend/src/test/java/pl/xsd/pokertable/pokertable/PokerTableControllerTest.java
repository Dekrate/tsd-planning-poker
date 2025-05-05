package pl.xsd.pokertable.pokertable;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.xsd.pokertable.developer.DeveloperService; // DeveloperService jest dependency kontrolera
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

	@MockitoBean // Używamy MockBean
	private PokerTableService pokerTableService;

	@MockitoBean // DeveloperService jest dependency, musi być zamockowany
	private DeveloperService developerService;

	@Test
	void createTable_Success_Returns201() throws Exception {
		// Arrange
		PokerTable table = new PokerTable();
		table.setId(1L);
		table.setName("New Session"); // Dodano imię

		when(pokerTableService.createPokerTable(anyString()))
				.thenReturn(table);

		// Act & Assert
		mockMvc.perform(post("/tables"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1L))
				.andExpect(jsonPath("$.name").value("New Session")); // Sprawdzenie imienia

		// Verify
		// Domyślne imię w kontrolerze to "Blank", ale w teście serwisu "Default Table"
		// Upewnij się, że test odpowiada temu, co przekazujesz w kontrolerze
		verify(pokerTableService).createPokerTable("Blank"); // Kontroler przekazuje "Blank"
	}

	@Test
	void createTable_ActiveTableExists_Returns400() throws Exception {
		// Arrange
		// Serwis wciąż rzuca IllegalStateException jeśli active table exists (choć to może być niekonsekwentne z nową logiką wielu stołów)
		// Testujemy obsługę tego wyjątku przez kontroler
		Mockito.doThrow(new IllegalStateException("Active table exists"))
				.when(pokerTableService).createPokerTable(anyString());

		// Act & Assert
		mockMvc.perform(post("/tables"))
				.andExpect(status().isBadRequest()); // Kontroler łapie IllegalStateException i zwraca 400

		// Verify
		verify(pokerTableService).createPokerTable("Blank");
	}

	@Test
	void closeTable_Success_Returns204() throws Exception {
		// Arrange - usunięto when(), bo domyślnie void metody nic nie rzucają
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
				.andExpect(status().isBadRequest()); // Spodziewamy się 400

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
				.andExpect(status().isNotFound()); // Spodziewamy się 404

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

	// Test dla przypadku, gdy getActiveTable tworzy nową tabelę (bo żadna aktywna nie istnieje)
	@Test
	void getActiveTable_notExists_createsNewAndReturns() throws Exception {
		// Arrange
		// Mockujemy zachowanie serwisu, gdy nie znajdzie aktywnej, ale ją utworzy
		PokerTable newTable = new PokerTable(); // Ta nowa tabela może jeszcze nie mieć ID z bazy w tym punkcie
		newTable.setName("Default Table"); // Imię ustawione przez serwis w orElseGet

		when(pokerTableService.getActiveTable()).thenReturn(newTable);

		// Act & Assert
		mockMvc.perform(get("/tables/active"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").doesNotExist()) // ID może nie być ustawione przed zapisem/zwróceniem
				.andExpect(jsonPath("$.name").value("Default Table")); // Ale imię powinno być ustawione

		// Verify
		verify(pokerTableService).getActiveTable();
	}

	// --- Nowe testy dla GET /tables/{id} ---
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
				.andExpect(status().isNotFound()); // Spodziewamy się 404

		// Verify
		verify(pokerTableService).getTableById(tableId);
	}
}