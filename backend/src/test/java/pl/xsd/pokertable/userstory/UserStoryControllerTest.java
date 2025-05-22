package pl.xsd.pokertable.userstory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
import pl.xsd.pokertable.pokertable.PokerTable;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserStoryController.class,
		excludeAutoConfiguration = {SecurityAutoConfiguration.class},
		excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtRequestFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class UserStoryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserStoryService userStoryService;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private UserDetailsService userDetailsService;

	@MockitoBean
	private JwtUtil jwtUtil;

	@MockitoBean
	private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	private PokerTable pokerTable;
	private UserStory userStory;

	@BeforeEach
	void setUp() {
		pokerTable = new PokerTable();
		pokerTable.setId(1L);
		pokerTable.setName("Test Table");

		userStory = new UserStory();
		userStory.setId(10L);
		userStory.setTitle("As a user, I want...");
		userStory.setDescription("...");
		userStory.setEstimatedPoints(5);
		userStory.setPokerTable(pokerTable);
	}

	@Test
	void createUserStory_shouldReturnCreatedStory_whenSuccessful() throws Exception {
		Long tableId = pokerTable.getId();
		UserStory userStoryToCreate = new UserStory("New Story", "Details");
		userStoryToCreate.setEstimatedPoints(3);

		UserStory createdStory = new UserStory("New Story", "Details");
		createdStory.setId(11L);
		createdStory.setEstimatedPoints(3);
		createdStory.setPokerTable(pokerTable);


		when(userStoryService.createUserStory(eq(tableId), any(UserStory.class))).thenReturn(createdStory);
		mockMvc.perform(post("/user-stories")
						.param("pokerTableId", tableId.toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(userStoryToCreate))
						.with(csrf())
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isCreated())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id", is(11)))
				.andExpect(jsonPath("$.title", is("New Story")))
				.andExpect(jsonPath("$.description", is("Details")))
				.andExpect(jsonPath("$.estimatedPoints", is(3)));

		verify(userStoryService).createUserStory(eq(tableId), any(UserStory.class));
	}

	@Test
	void createUserStory_shouldReturnNotFound_whenPokerTableDoesNotExist() throws Exception {
		Long nonExistentTableId = 99L;
		UserStory userStoryToCreate = new UserStory("New Story", "Details");
		userStoryToCreate.setEstimatedPoints(3);

		when(userStoryService.createUserStory(eq(nonExistentTableId), any(UserStory.class)))
				.thenThrow(new NotFoundException("Poker table not found with ID: " + nonExistentTableId));
		mockMvc.perform(post("/user-stories")
						.param("pokerTableId", nonExistentTableId.toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(userStoryToCreate))
						.with(csrf())
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message", is("Poker table not found with ID: " + nonExistentTableId)));
		verify(userStoryService).createUserStory(eq(nonExistentTableId), any(UserStory.class));
	}

	@Test
	void getUserStoryById_shouldReturnUserStory_whenStoryExists() throws Exception {
		Long storyId = userStory.getId();
		when(userStoryService.getUserStoryById(storyId)).thenReturn(userStory);
		mockMvc.perform(get("/user-stories/{storyId}", storyId)
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id", is(storyId.intValue())))
				.andExpect(jsonPath("$.title", is("As a user, I want...")))
				.andExpect(jsonPath("$.description", is("...")))
				.andExpect(jsonPath("$.estimatedPoints", is(5)));

		verify(userStoryService).getUserStoryById(storyId);
	}

	@Test
	void getUserStoryById_shouldReturnNotFound_whenStoryDoesNotExist() throws Exception {
		Long nonExistentStoryId = 99L;
		when(userStoryService.getUserStoryById(nonExistentStoryId))
				.thenThrow(new NotFoundException("User story not found with ID: " + nonExistentStoryId));
		mockMvc.perform(get("/user-stories/{storyId}", nonExistentStoryId)
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message", is("User story not found with ID: " + nonExistentStoryId)));

		verify(userStoryService).getUserStoryById(nonExistentStoryId);
	}


	@Test
	void getUserStoriesForTable_shouldReturnSetOfUserStories_whenTableHasStories() throws Exception {
		Long tableId = pokerTable.getId();
		Set<UserStory> stories = new HashSet<>();
		stories.add(userStory);
		stories.add(new UserStory(11L, "Another Story", "More Details", 8, pokerTable));

		when(userStoryService.getUserStoriesForTable(tableId)).thenReturn(stories);
		mockMvc.perform(get("/user-stories/table/{tableId}", tableId)
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[*].id", containsInAnyOrder(10, 11)))
				.andExpect(jsonPath("$[*].title", containsInAnyOrder("As a user, I want...", "Another Story")))
				.andExpect(jsonPath("$[*].estimatedPoints", containsInAnyOrder(5, 8)));

		verify(userStoryService).getUserStoriesForTable(tableId);
	}

	@Test
	void getUserStoriesForTable_shouldReturnEmptySet_whenTableHasNoStories() throws Exception {
		Long tableId = pokerTable.getId();
		Set<UserStory> stories = new HashSet<>();
		when(userStoryService.getUserStoriesForTable(tableId)).thenReturn(stories);
		mockMvc.perform(get("/user-stories/table/{tableId}", tableId)
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$", hasSize(0)));

		verify(userStoryService).getUserStoriesForTable(tableId);
	}


	@Test
	void updateUserStory_shouldReturnUpdatedUserStory_whenSuccessful() throws Exception {
		Long storyId = userStory.getId();
		UserStory updatedDetails = new UserStory();
		updatedDetails.setTitle("Title Updated");
		updatedDetails.setDescription("Description Updated");
		updatedDetails.setEstimatedPoints(13);

		UserStory updatedStory = new UserStory();
		updatedStory.setId(storyId);
		updatedStory.setTitle("Title Updated");
		updatedStory.setDescription("Description Updated");
		updatedStory.setEstimatedPoints(13);
		updatedStory.setPokerTable(pokerTable);

		when(userStoryService.updateUserStory(eq(storyId), any(UserStory.class))).thenReturn(updatedStory);
		mockMvc.perform(put("/user-stories/{storyId}", storyId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(updatedDetails))
						.with(csrf())
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id", is(storyId.intValue())))
				.andExpect(jsonPath("$.title", is("Title Updated")))
				.andExpect(jsonPath("$.description", is("Description Updated")))
				.andExpect(jsonPath("$.estimatedPoints", is(13)));

		verify(userStoryService).updateUserStory(eq(storyId), any(UserStory.class));
	}

	@Test
	void updateUserStory_shouldReturnNotFound_whenStoryDoesNotExist() throws Exception {
		Long nonExistentStoryId = 99L;
		UserStory updatedDetails = new UserStory();
		updatedDetails.setTitle("Title Updated");

		when(userStoryService.updateUserStory(eq(nonExistentStoryId), any(UserStory.class)))
				.thenThrow(new NotFoundException("User story not found with ID: " + nonExistentStoryId));
		mockMvc.perform(put("/user-stories/{storyId}", nonExistentStoryId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(updatedDetails))
						.with(csrf())
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message", is("User story not found with ID: " + nonExistentStoryId)));

		verify(userStoryService).updateUserStory(eq(nonExistentStoryId), any(UserStory.class));
	}


	@Test
	void deleteUserStory_shouldReturnNoContent_whenSuccessful() throws Exception {
		Long storyId = userStory.getId();
		doNothing().when(userStoryService).deleteUserStory(storyId);
		mockMvc.perform(delete("/user-stories/{storyId}", storyId)
						.with(csrf())
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isNoContent());

		verify(userStoryService).deleteUserStory(storyId);
	}

	@Test
	void deleteUserStory_shouldReturnNotFound_whenStoryDoesNotExist() throws Exception {
		Long nonExistentStoryId = 99L;
		doThrow(new NotFoundException("User story not found with ID: " + nonExistentStoryId))
				.when(userStoryService).deleteUserStory(nonExistentStoryId);
		mockMvc.perform(delete("/user-stories/{storyId}", nonExistentStoryId)
						.with(csrf())
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message", is("User story not found with ID: " + nonExistentStoryId)));

		verify(userStoryService).deleteUserStory(nonExistentStoryId);
	}
}
