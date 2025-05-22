package pl.xsd.pokertable.pokertable;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.xsd.pokertable.config.JwtAuthenticationEntryPoint;
import pl.xsd.pokertable.config.JwtRequestFilter;
import pl.xsd.pokertable.config.JwtUtil;
import pl.xsd.pokertable.developer.DeveloperService;
import pl.xsd.pokertable.exception.NotEveryoneVotedException;
import pl.xsd.pokertable.exception.NotFoundException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PokerTableController.class,
		excludeAutoConfiguration = {SecurityAutoConfiguration.class},
		excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtRequestFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class PokerTableControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PokerTableService pokerTableService;

	@MockitoBean
	private DeveloperService developerService;

	@MockitoBean
	private UserDetailsService userDetailsService;

	@MockitoBean
	private JwtUtil jwtUtil;

	@MockitoBean
	private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;


	@Test
	void createTable_Success_Returns201() throws Exception {
		PokerTable table = new PokerTable();
		table.setId(1L);
		table.setName("New Session");

		when(pokerTableService.createPokerTable(anyString()))
				.thenReturn(table);
		mockMvc.perform(post("/tables")
						.with(csrf())
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1L))
				.andExpect(jsonPath("$.name").value("New Session"));
		verify(pokerTableService).createPokerTable(anyString());
	}

	@Test
	void createTable_AlreadyExists_Returns400() throws Exception {
		doThrow(new IllegalStateException("Active poker table already exists."))
				.when(pokerTableService).createPokerTable(anyString());
		mockMvc.perform(post("/tables")
						.with(csrf())
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isBadRequest());
		verify(pokerTableService).createPokerTable(anyString());
	}


	@Test
	void closePokerTable_Success_Returns204() throws Exception {
		Long tableId = 1L;
		mockMvc.perform(patch("/tables/{id}/close", tableId)
						.with(csrf())
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isNoContent());
		verify(pokerTableService).closePokerTable(tableId);
	}

	@Test
	void closePokerTable_TableNotFound_Returns404() throws Exception {
		Long tableId = 999L;
		doThrow(new NotFoundException("Poker table not found"))
				.when(pokerTableService).closePokerTable(tableId);
		mockMvc.perform(patch("/tables/{id}/close", tableId)
						.with(csrf())
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isNotFound());
		verify(pokerTableService).closePokerTable(tableId);
	}

	@Test
	void closePokerTable_NotAllVoted_Returns400() throws Exception {
		Long tableId = 1L;
		doThrow(new NotEveryoneVotedException("Not all developers have submitted a vote yet, or there are no developers at the table."))
				.when(pokerTableService).closePokerTable(tableId);
		mockMvc.perform(patch("/tables/{id}/close", tableId)
						.with(csrf())
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isBadRequest());
		verify(pokerTableService).closePokerTable(tableId);
	}


	@Test
	void getActiveTable_exists_returns200() throws Exception {
		PokerTable existingTable = new PokerTable();
		existingTable.setId(10L);
		existingTable.setName("Existing Active");

		when(pokerTableService.getActiveTable()).thenReturn(existingTable);
		mockMvc.perform(get("/tables/active")
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(10L))
				.andExpect(jsonPath("$.name").value("Existing Active"));
		verify(pokerTableService).getActiveTable();
	}

	@Test
	void getActiveTable_notExists_createsNewAndReturns() throws Exception {
		PokerTable newTable = new PokerTable();
		newTable.setName("Default Table");

		when(pokerTableService.getActiveTable()).thenReturn(newTable);
		mockMvc.perform(get("/tables/active")
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").doesNotExist())
				.andExpect(jsonPath("$.name").value("Default Table"));
		verify(pokerTableService).getActiveTable();
	}


	@Test
	void getTableById_exists_returns200() throws Exception {
		Long tableId = 123L;
		PokerTable table = new PokerTable();
		table.setId(tableId);
		table.setName("Specific Table");

		when(pokerTableService.getTableById(tableId)).thenReturn(table);
		mockMvc.perform(get("/tables/{id}", tableId)
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(tableId))
				.andExpect(jsonPath("$.name").value("Specific Table"));
		verify(pokerTableService).getTableById(tableId);
	}

	@Test
	void getTableById_notFound_returns404() throws Exception {
		Long tableId = 999L;
		doThrow(new NotFoundException("Poker table not found with ID: " + tableId))
				.when(pokerTableService).getTableById(tableId);
		mockMvc.perform(get("/tables/{id}", tableId)
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isNotFound());
		verify(pokerTableService).getTableById(tableId);
	}

	@Test
	void exportUserStories_Success_ReturnsCsv() throws Exception {
		Long tableId = 1L;
		byte[] csvBytes = "Summary,Description,Issue Type,Story point estimate\nTest Story,Test Description,Story,5\n".getBytes();
		when(pokerTableService.exportUserStoriesToCsv(tableId)).thenReturn(csvBytes);
		mockMvc.perform(get("/tables/{tableId}/export-stories", tableId)
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "text/csv"))
				.andExpect(content().bytes(csvBytes));
		verify(pokerTableService).exportUserStoriesToCsv(tableId);
	}

	@Test
	void exportUserStories_TableNotFound_Returns404() throws Exception {
		Long tableId = 999L;
		doThrow(new NotFoundException("Poker table not found with ID: " + tableId))
				.when(pokerTableService).exportUserStoriesToCsv(tableId);
		mockMvc.perform(get("/tables/{tableId}/export-stories", tableId)
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isNotFound());
		verify(pokerTableService).exportUserStoriesToCsv(tableId);
	}
}
