package pl.xsd.pokertable.userstory;

import org.springframework.stereotype.Service;
import pl.xsd.pokertable.exception.NotFoundException;
import pl.xsd.pokertable.pokertable.PokerTable;
import pl.xsd.pokertable.pokertable.PokerTableRepository;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserStoryService {

	private final UserStoryRepository userStoryRepository;
	private final PokerTableRepository pokerTableRepository;

	public UserStoryService(UserStoryRepository userStoryRepository, PokerTableRepository pokerTableRepository) {
		this.userStoryRepository = userStoryRepository;
		this.pokerTableRepository = pokerTableRepository;
	}

	public UserStoryDto createUserStory(Long pokerTableId, UserStory userStory) { // Zmieniono zwracany typ na UserStoryDto
		PokerTable pokerTable = pokerTableRepository.findById(pokerTableId)
				.orElseThrow(() -> new NotFoundException("Poker table not found with ID: " + pokerTableId));
		userStory.setPokerTable(pokerTable);
		UserStory savedUserStory = userStoryRepository.save(userStory);
		return new UserStoryDto(savedUserStory); // Konwersja na DTO
	}

	public UserStoryDto getUserStoryById(Long storyId) { // Zmieniono zwracany typ na UserStoryDto
		UserStory userStory = userStoryRepository.findById(storyId)
				.orElseThrow(() -> new NotFoundException("User story not found with ID: " + storyId));
		return new UserStoryDto(userStory); // Konwersja na DTO
	}

	public Set<UserStoryDto> getUserStoriesForTable(Long tableId) { // Zmieniono zwracany typ na Set<UserStoryDto>
		return userStoryRepository.findByPokerTableId(tableId).stream()
				.map(UserStoryDto::new) // Konwersja na DTO
				.collect(Collectors.toSet());
	}

	public UserStoryDto updateUserStory(Long storyId, UserStory updatedDetails) { // Zmieniono zwracany typ na UserStoryDto
		UserStory userStory = userStoryRepository.findById(storyId)
				.orElseThrow(() -> new NotFoundException("User story not found with ID: " + storyId));

		userStory.setTitle(updatedDetails.getTitle());
		userStory.setDescription(updatedDetails.getDescription());
		userStory.setEstimatedPoints(updatedDetails.getEstimatedPoints());

		UserStory savedUserStory = userStoryRepository.save(userStory);
		return new UserStoryDto(savedUserStory); // Konwersja na DTO
	}

	public void deleteUserStory(Long storyId) {
		if (!userStoryRepository.existsById(storyId)) {
			throw new NotFoundException("User story not found with ID: " + storyId);
		}
		userStoryRepository.deleteById(storyId);
	}
}
