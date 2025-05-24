package pl.xsd.pokertable.pokertable;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.xsd.pokertable.developer.Developer;
import pl.xsd.pokertable.developer.DeveloperRepository;
import pl.xsd.pokertable.exception.NotEveryoneVotedException;
import pl.xsd.pokertable.exception.NotFoundException;
import pl.xsd.pokertable.userstory.UserStory;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PokerTableService {

	private final PokerTableRepository pokerTableRepository;
	private final DeveloperRepository developerRepository;

	public PokerTableService(PokerTableRepository pokerTableRepository, DeveloperRepository developerRepository) {
		this.pokerTableRepository = pokerTableRepository;
		this.developerRepository = developerRepository;
	}

	@Transactional
	public PokerTable createPokerTable(String name) {
		PokerTable pokerTable = new PokerTable(null, name, false);
		return pokerTableRepository.save(pokerTable);
	}

	@Transactional
	public void closePokerTable(Long tableId) {
		PokerTable pokerTable = pokerTableRepository.findById(tableId)
				.orElseThrow(() -> new NotFoundException("Poker table not found"));

		Set<Developer> developers = pokerTable.getDevelopers();
		long totalDevelopers = developers.size();

		long votedDevelopersCount = developers.stream().filter(dev -> dev.getVote() != null).count();

		if (votedDevelopersCount == totalDevelopers && totalDevelopers > 0) {
			for (Developer dev : developers) {
				dev.addPastTable(pokerTable);
				dev.setVote(null);
				developerRepository.save(dev);
			}
			pokerTable.setIsClosed(true);
			pokerTableRepository.save(pokerTable);
		} else {
			throw new NotEveryoneVotedException("Not all developers have submitted a vote yet, or there are no developers at the table.");
		}
	}


	public PokerTable getActiveTable() {
		return pokerTableRepository.findByIsClosedFalse()
				.orElseGet(() -> createPokerTable("Default Table"));
	}

	public PokerTable getTableById(Long tableId) {
		return pokerTableRepository.findById(tableId)
				.orElseThrow(() -> new NotFoundException("Poker table not found with ID: " + tableId));
	}

	public Set<PokerTable> getAllActiveTables() {
		return pokerTableRepository.findAllByIsClosedFalse();
	}

	@Transactional
	public Set<PokerTable> getPastTablesForDeveloper(Long developerId) {
		Developer developer = developerRepository.findById(developerId)
				.orElseThrow(() -> new NotFoundException("Developer not found with ID: " + developerId));
		return developer.getPastTables().stream()
				.filter(PokerTable::getIsClosed)
				.collect(Collectors.toUnmodifiableSet());
	}


	@Transactional
	public byte[] exportUserStoriesToCsv(Long tableId) {
		PokerTable pokerTable = getTableById(tableId);

		Set<UserStory> userStories = pokerTable.getUserStories();

		StringBuilder csvContent = new StringBuilder();
		csvContent.append("Summary,Description,Issue Type,Story point estimate\n");

		for (UserStory story : userStories) {
			csvContent.append("\"").append(escapeCsv(story.getTitle())).append("\"").append(",");
			csvContent.append("\"").append(escapeCsv(story.getDescription())).append("\"").append(",");
			csvContent.append("\"Story\",");
			if (story.getEstimatedPoints() != null) {
				csvContent.append(story.getEstimatedPoints());
			}
			csvContent.append("\n");
		}

		return csvContent.toString().getBytes();
	}

	private String escapeCsv(String field) {
		if (field == null) {
			return "";
		}
		return field.replace("\"", "\"\"");
	}

	@Transactional
	public void resetAllVotes(Long tableId) {
		PokerTable pokerTable = pokerTableRepository.findById(tableId)
				.orElseThrow(() -> new NotFoundException("PokerTable with id " + tableId + " not found"));

		if (!pokerTable.getIsActive()) {
			throw new IllegalStateException("Cannot reset votes for a closed poker table.");
		}

		for (Developer dev : pokerTable.getDevelopers()) {
			dev.setVote(null);
			developerRepository.save(dev);
		}
	}
}
