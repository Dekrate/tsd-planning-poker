package pl.xsd.pokertable.pokertable;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.xsd.pokertable.developer.DeveloperService;
import pl.xsd.pokertable.exception.NotFoundException;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
		PokerTable table = new PokerTable();
		table.setId(1L);

		when(pokerTableService.createPokerTable(anyString()))
				.thenReturn(table);

		mockMvc.perform(post("/tables"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1L));
	}

	@Test
	void createTable_ActiveTableExists_Returns400() throws Exception {
		Mockito.doThrow(new IllegalStateException("Active table exists"))
				.when(pokerTableService).createPokerTable(anyString());

		mockMvc.perform(post("/tables"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void closeTable_Success_Returns204() throws Exception {
		mockMvc.perform(patch("/tables/1/close"))
				.andExpect(status().isNoContent());
	}

	@Test
	void closeTable_NotAllVoted_Returns400() throws Exception {
		Mockito.doThrow(new IllegalStateException("Not all voted"))
				.when(pokerTableService).closePokerTable(anyLong());

		mockMvc.perform(patch("/tables/1/close"))
				.andExpect(status().isBadRequest());
	}
	@Test
	void getActiveTable_exists_returnsTable() throws Exception {
		PokerTable table = new PokerTable();
		table.setId(1L);
		when(pokerTableService.getActiveTable()).thenReturn(table);

		mockMvc.perform(get("/tables/active"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").exists());
	}

	@Test
	void getActiveTable_notExists_createsNew() throws Exception {
		PokerTable newTable = new PokerTable();
		when(pokerTableService.getActiveTable()).thenReturn(newTable);

		mockMvc.perform(get("/tables/active"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").doesNotExist());
	}
	@Test
	void closeTable_NotFound_Returns404() throws Exception {
		Mockito.doThrow(new NotFoundException("Table not found"))
				.when(pokerTableService).closePokerTable(anyLong());

		mockMvc.perform(patch("/tables/999/close"))
				.andExpect(status().isNotFound());
	}
}