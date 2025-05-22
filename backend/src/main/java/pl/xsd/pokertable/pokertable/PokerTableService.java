package pl.xsd.pokertable.pokertable;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.xsd.pokertable.developer.Developer;
import pl.xsd.pokertable.developer.DeveloperRepository;
import pl.xsd.pokertable.exception.NotEveryoneVotedException;
import pl.xsd.pokertable.exception.NotFoundException;
import pl.xsd.pokertable.participation.Participation;
import pl.xsd.pokertable.participation.ParticipationRepository;
import pl.xsd.pokertable.userstory.UserStory;

import java.util.Set;

@Service
public class PokerTableService {

	private final PokerTableRepository pokerTableRepository;
	private final DeveloperRepository developerRepository;
	private final ParticipationRepository participationRepository;

	public PokerTableService(PokerTableRepository pokerTableRepository, DeveloperRepository developerRepository, ParticipationRepository participationRepository) {
		this.pokerTableRepository = pokerTableRepository;
		this.developerRepository = developerRepository;
		this.participationRepository = participationRepository;
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
				if (dev.getVote() != null) {
					Participation participation = new Participation(dev, pokerTable, dev.getVote());
					participationRepository.save(participation);
					dev.setVote(null);
					developerRepository.save(dev);
				}
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
}
