package pl.xsd.pokertable.pokertable;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.xsd.pokertable.developer.Developer;
import pl.xsd.pokertable.exception.NotFoundException;

import java.util.Set;

@Service
public class PokerTableService {

	private final PokerTableRepository pokerTableRepository;

	public PokerTableService(PokerTableRepository pokerTableRepository) {
		this.pokerTableRepository = pokerTableRepository;
	}

	@Transactional
	public PokerTable createPokerTable(String name) {
		if (pokerTableRepository.findByIsClosedFalse().isPresent()) {
			throw new IllegalStateException("There is already an active poker table.");
		}

		PokerTable pokerTable = new PokerTable(null, name, false);
		return pokerTableRepository.save(pokerTable);
	}

	@Transactional
	public void closePokerTable(Long tableId) {
		PokerTable pokerTable = pokerTableRepository.findById(tableId)
				.orElseThrow(() -> new NotFoundException("Poker table not found"));

		Set<Developer> developers = pokerTable.getDevelopers();
		long totalVotes = developers.stream().filter(Developer::hasVoted).count();
		long totalDevelopers = developers.size();

		if (totalVotes == totalDevelopers) {
			pokerTable.setIsClosed(true);
			pokerTableRepository.save(pokerTable);
		} else {
			throw new IllegalStateException("Not all developers have voted yet.");
		}
	}

	public PokerTable getActiveTable() {
		return pokerTableRepository.findByIsClosedFalse()
				.orElseGet(() -> createPokerTable("Default Table"));
	}
}
