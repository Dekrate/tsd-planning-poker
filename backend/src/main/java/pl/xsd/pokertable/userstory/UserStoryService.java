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
				.orElseThrow(() -> new NotFoundException("PokerTable with id " + pokerTableId + " not found"));
		userStory.setPokerTable(pokerTable);

		return userStoryRepository.save(userStory);
	}

	public UserStory getUserStoryById(Long storyId) {
		return userStoryRepository.findById(storyId)
				.orElseThrow(() -> new NotFoundException("UserStory with id " + storyId + " not found"));
	}

	public Set<UserStory> getUserStoriesForTable(Long pokerTableId) {
		PokerTable pokerTable = pokerTableRepository.findById(pokerTableId)
				.orElseThrow(() -> new NotFoundException("PokerTable with id " + pokerTableId + " not found"));
		return userStoryRepository.findByPokerTable(pokerTable);
	}

	@Transactional
	public UserStory updateUserStory(Long storyId, UserStory userStory) {
		UserStory existingStory = userStoryRepository.findById(storyId)
				.orElseThrow(() -> new NotFoundException("UserStory with id " + storyId + " not found"));

		existingStory.setTitle(userStory.getTitle());
		existingStory.setDescription(userStory.getDescription());
		existingStory.setEstimatedPoints(userStory.getEstimatedPoints());

		return userStoryRepository.save(existingStory);
	}

	@Transactional
	public void deleteUserStory(Long storyId) {
		UserStory story = userStoryRepository.findById(storyId)
				.orElseThrow(() -> new NotFoundException("UserStory with id " + storyId + " not found"));

		userStoryRepository.delete(story);
	}
}
