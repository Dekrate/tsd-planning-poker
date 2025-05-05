package pl.xsd.pokertable.developer;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.xsd.pokertable.exception.NotFoundException;
import pl.xsd.pokertable.pokertable.PokerTable;
import pl.xsd.pokertable.pokertable.PokerTableRepository;
import pl.xsd.pokertable.pokertable.PokerTableService;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class DeveloperService {

	private final DeveloperRepository developerRepository;
	private final PokerTableRepository pokerTableRepository;
	private final PokerTableService pokerTableService;

	public DeveloperService(DeveloperRepository developerRepository, PokerTableRepository pokerTableRepository, PokerTableService pokerTableService) {
		this.developerRepository = developerRepository;
		this.pokerTableRepository = pokerTableRepository;
		this.pokerTableService = pokerTableService;
	}

	public PokerTable getActiveTable() {
		return pokerTableRepository.findByIsClosedFalse()
				.orElseGet(() -> pokerTableService.createPokerTable("Blank"));
	}

	@Transactional
	public void vote(Long developerId, Long tableId, Integer vote) {
		if (vote == null) {
			throw new IllegalArgumentException("Vote cannot be null");
		}

		if (vote < 1 || vote > 13) {
			throw new IllegalArgumentException("Vote must be between 1 and 13");
		}

		Developer developer = developerRepository.findById(developerId)
				.orElseThrow(() -> new NotFoundException("Developer not found"));

		PokerTable pokerTable = pokerTableRepository.findById(tableId)
				.orElseThrow(() -> new NotFoundException("Poker table not found"));

		if (!developer.getPokerTable().getId().equals(pokerTable.getId())) { // Compare IDs
			throw new IllegalArgumentException("Developer does not belong to this poker table.");
		}

		developer.setVote(vote);
		developerRepository.save(developer);
	}

	public Developer getDeveloper(Long developerId) {
		return developerRepository.findById(developerId)
				.orElseThrow(() -> new NotFoundException("Developer not found"));
	}

	public boolean hasVoted(Long developerId) {
		Developer developer = getDeveloper(developerId);
		return developer.hasVoted();
	}

	public Set<Developer> getDevelopersForPokerTable(Long tableId) {
		PokerTable pokerTable = pokerTableRepository.findById(tableId)
				.orElseThrow(() -> new NotFoundException("Poker table not found"));

		return pokerTable.getDevelopers();
	}

	@Transactional
	public Developer createDeveloper(Long pokerTableId, Developer developer) {
		PokerTable pokerTable = pokerTableRepository.findById(pokerTableId)
				.orElseThrow(() -> new IllegalArgumentException("Tablica pokerowa o podanym ID nie istnieje"));

		developer.setPokerTable(pokerTable);
		return developerRepository.save(developer);
	}

	@Transactional
	public Map<String, Object> joinTable(String name, Long tableId, HttpSession session) {
		String sessionId = session.getId();

		PokerTable table = pokerTableRepository.findById(tableId)
				.orElseThrow(() -> new NotFoundException("Poker table not found with ID: " + tableId));

		Optional<Developer> existingDeveloper = developerRepository.findBySessionId(sessionId);

		Developer developer;

		if (existingDeveloper.isPresent()) {
			developer = existingDeveloper.get();
			if (developer.getPokerTable() == null || !developer.getPokerTable().getId().equals(tableId)) {
				developer.setPokerTable(table);
				developer.setVote(null);
				developerRepository.save(developer);
			}
		} else {
			developer = new Developer(sessionId, name);
			developer.setPokerTable(table);
			developer.setVote(null);
			developerRepository.save(developer);
		}

		return Map.of(
				"developer", Map.of(
						"id", developer.getId(),
						"name", developer.getName(),
						"sessionId", developer.getSessionId()
				),
				"table", Map.of(
						"id", table.getId(),
						"name", table.getName(),
						"createdAt", table.getCreatedAt()
				)
		);
	}
}
