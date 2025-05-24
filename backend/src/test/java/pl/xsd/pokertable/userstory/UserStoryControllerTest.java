package pl.xsd.pokertable.userstory;

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

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserStoryController.class,
		excludeAutoConfiguration = {SecurityAutoConfiguration.class},
		excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtRequestFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class UserStoryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private UserStoryService userStoryService;

	@Test
	@WithMockUser
	void createUserStory_shouldReturnCreatedUserStoryDto() throws Exception {
		Long pokerTableId = 1L;
		UserStory newUserStory = new UserStory(null, "New Story", "Desc", 5, null);

		when(userStoryService.createUserStory(eq(pokerTableId), any(UserStory.class))).thenReturn(newUserStory);

		mockMvc.perform(post("/user-stories")
						.param("pokerTableId", pokerTableId.toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(newUserStory))
						.with(csrf()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(newUserStory.getId()))
				.andExpect(jsonPath("$.title").value(newUserStory.getTitle()));

		verify(userStoryService, times(1)).createUserStory(eq(pokerTableId), any(UserStory.class));
	}

	@Test
	@WithMockUser
	void createUserStory_shouldReturnNotFoundOnNotFoundException() throws Exception {
		Long pokerTableId = 99L;
		UserStory newUserStory = new UserStory(null, "New Story", "Desc", 5, null);

		when(userStoryService.createUserStory(eq(pokerTableId), any(UserStory.class)))
				.thenThrow(new NotFoundException("Poker table not found"));

		mockMvc.perform(post("/user-stories")
						.param("pokerTableId", pokerTableId.toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(newUserStory))
						.with(csrf()))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser
	void getUserStoryById_shouldReturnUserStoryDto() throws Exception {
		Long storyId = 1L;
		UserStory userStory = new UserStory(1L, "Story Title", "Story Description", 8, null);
		when(userStoryService.getUserStoryById(storyId)).thenReturn(userStory);

		mockMvc.perform(get("/user-stories/{storyId}", storyId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(userStory.getId()))
				.andExpect(jsonPath("$.title").value(userStory.getTitle()));

		verify(userStoryService, times(1)).getUserStoryById(storyId);
	}

	@Test
	@WithMockUser
	void getUserStoryById_shouldReturnNotFoundOnNotFoundException() throws Exception {
		Long storyId = 99L;
		when(userStoryService.getUserStoryById(storyId)).thenThrow(new NotFoundException("Story not found"));

		mockMvc.perform(get("/user-stories/{storyId}", storyId))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser
	void getUserStoriesForTable_shouldReturnSetOfUserStoryDtos() throws Exception {
		Long tableId = 1L;
		Set<UserStory> userStories = Set.of(
				new UserStory(1L, "Story 1", "Description 1", 8, null),
				new UserStory(2L, "Story 2", "Description 2", 13, null)
		);
		when(userStoryService.getUserStoriesForTable(tableId)).thenReturn(userStories);

		mockMvc.perform(get("/user-stories/table/{tableId}", tableId))
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

		mockMvc.perform(get("/user-stories/table/{tableId}", tableId))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser
	void updateUserStory_shouldReturnUpdatedUserStoryDto() throws Exception {
		Long storyId = 1L;
		UserStory updatedUserStory = new UserStory(storyId, "Updated Title", "Updated Desc", 13, null);
		when(userStoryService.updateUserStory(eq(storyId), any(UserStory.class))).thenReturn(updatedUserStory);

		mockMvc.perform(put("/user-stories/{storyId}", storyId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(updatedUserStory))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(updatedUserStory.getId()))
				.andExpect(jsonPath("$.title").value(updatedUserStory.getTitle()));

		verify(userStoryService, times(1)).updateUserStory(eq(storyId), any(UserStory.class));
	}

	@Test
	@WithMockUser
	void updateUserStory_shouldReturnNotFoundOnNotFoundException() throws Exception {
		Long storyId = 99L;
		UserStory updatedUserStory = new UserStory(storyId, "Updated Title", "Updated Desc", 13, null);

		when(userStoryService.updateUserStory(eq(storyId), any(UserStory.class)))
				.thenThrow(new NotFoundException("Story not found"));

		mockMvc.perform(put("/user-stories/{storyId}", storyId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(updatedUserStory))
						.with(csrf()))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser
	void deleteUserStory_shouldReturnNoContentOnSuccess() throws Exception {
		Long storyId = 1L;
		doNothing().when(userStoryService).deleteUserStory(storyId);

		mockMvc.perform(delete("/user-stories/{storyId}", storyId).with(csrf()))
				.andExpect(status().isNoContent());

		verify(userStoryService, times(1)).deleteUserStory(storyId);
	}

	@Test
	@WithMockUser
	void deleteUserStory_shouldReturnNotFoundOnNotFoundException() throws Exception {
		Long storyId = 99L;
		doThrow(new NotFoundException("Story not found")).when(userStoryService).deleteUserStory(storyId);

		mockMvc.perform(delete("/user-stories/{storyId}", storyId).with(csrf()))
				.andExpect(status().isNotFound());
	}
}
