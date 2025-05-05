package pl.xsd.pokertable.userstory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.xsd.pokertable.exception.NotFoundException;
import pl.xsd.pokertable.pokertable.PokerTable;
import pl.xsd.pokertable.pokertable.PokerTableRepository;

import java.util.Set;

@Service
public class UserStoryService {

	private final UserStoryRepository userStoryRepository;
	private final PokerTableRepository pokerTableRepository;

	public UserStoryService(UserStoryRepository userStoryRepository, PokerTableRepository pokerTableRepository) {
		this.userStoryRepository = userStoryRepository;
		this.pokerTableRepository = pokerTableRepository;
	}

	@Transactional
	public UserStory createUserStory(Long pokerTableId, UserStory userStory) {
		PokerTable pokerTable = pokerTableRepository.findById(pokerTableId)
				.orElseThrow(() -> new NotFoundException("Poker table not found with ID: " + pokerTableId));

		userStory.setPokerTable(pokerTable);
		return userStoryRepository.save(userStory);
	}

	public UserStory getUserStoryById(Long storyId) {
		return userStoryRepository.findById(storyId)
				.orElseThrow(() -> new NotFoundException("User story not found with ID: " + storyId));
	}

	public Set<UserStory> getUserStoriesForTable(Long pokerTableId) {
		// This implicitly checks if the poker table exists via the repository method
		return userStoryRepository.findByPokerTableId(pokerTableId);
	}

	@Transactional
	public UserStory updateUserStory(Long storyId, UserStory updatedUserStory) {
		UserStory existingUserStory = getUserStoryById(storyId);

		existingUserStory.setTitle(updatedUserStory.getTitle());
		existingUserStory.setDescription(updatedUserStory.getDescription());
		existingUserStory.setEstimatedPoints(updatedUserStory.getEstimatedPoints());

		return userStoryRepository.save(existingUserStory);
	}

	@Transactional
	public void deleteUserStory(Long storyId) {
		if (!userStoryRepository.existsById(storyId)) {
			throw new NotFoundException("User story not found with ID: " + storyId);
		}
		userStoryRepository.deleteById(storyId);
	}
}