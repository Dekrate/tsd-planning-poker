package pl.xsd.pokertable.pokertable;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import pl.xsd.pokertable.developer.Developer;
import pl.xsd.pokertable.developer.DeveloperService;
import pl.xsd.pokertable.userstory.UserStoryDto;
import pl.xsd.pokertable.userstory.UserStoryService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tables")
@AllArgsConstructor
public class PokerTableController {

	private final PokerTableService pokerTableService;
	private final UserStoryService userStoryService;
	private final DeveloperService developerService;

	@PostMapping
	public ResponseEntity<PokerTableDto> createTable() { // Zmieniono zwracany typ na PokerTableDto
		PokerTable newTable = pokerTableService.createPokerTable("New Planning Session");
		return ResponseEntity.status(HttpStatus.CREATED).body(new PokerTableDto(newTable)); // Konwersja na DTO
	}

	@GetMapping("/{tableId}")
	public ResponseEntity<PokerTableDto> getTableById(@PathVariable Long tableId) { // Zmieniono zwracany typ na PokerTableDto
		PokerTable table = pokerTableService.getTableById(tableId);
		return ResponseEntity.ok(new PokerTableDto(table)); // Konwersja na DTO
	}

	@PatchMapping("/{tableId}/close")
	public ResponseEntity<Void> closeTable(@PathVariable Long tableId) {
		pokerTableService.closePokerTable(tableId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/active")
	public ResponseEntity<PokerTableDto> getActiveTable() { // Zmieniono zwracany typ na PokerTableDto
		PokerTable activeTable = pokerTableService.getActiveTable();
		return ResponseEntity.ok(new PokerTableDto(activeTable)); // Konwersja na DTO
	}

	@GetMapping("/all-active")
	public ResponseEntity<List<PokerTableDto>> getAllActiveTables() { // Zmieniono zwracany typ na List<PokerTableDto>
		Set<PokerTable> activeTables = pokerTableService.getAllActiveTables();
		// Konwersja listy encji na listę DTO
		List<PokerTableDto> activeTableDtos = activeTables.stream()
				.map(PokerTableDto::new)
				.collect(Collectors.toList());
		return ResponseEntity.ok(activeTableDtos);
	}

	@GetMapping("/{tableId}/user-stories")
	public ResponseEntity<Set<UserStoryDto>> getUserStoriesForTable(@PathVariable Long tableId) {
		Set<UserStoryDto> userStories = userStoryService.getUserStoriesForTable(tableId);
		return ResponseEntity.ok(userStories);
	}

	@GetMapping("/{tableId}/export-stories")
	public ResponseEntity<byte[]> exportUserStoriesToCsv(@PathVariable Long tableId) {
		byte[] csvBytes = pokerTableService.exportUserStoriesToCsv(tableId);
		return ResponseEntity.ok()
				.header("Content-Disposition", "attachment; filename=\"user_stories_table_" + tableId + ".csv\"")
				.contentType(org.springframework.http.MediaType.parseMediaType("text/csv"))
				.body(csvBytes);
	}

	@GetMapping("/my-closed")
	public ResponseEntity<Set<PokerTableDto>> getMyClosedTables() { // Zmieniono zwracany typ na List<PokerTableDto>
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.isAuthenticated() && !(authentication.getPrincipal() instanceof String && authentication.getPrincipal().equals("anonymousUser"))) {
			String email = authentication.getName();
			Developer developer = developerService.getDeveloperByEmail(email);
			Set<PokerTable> closedTables = pokerTableService.getPastTablesForDeveloper(developer.getId());
			// Konwersja listy encji na listę DTO
			Set<PokerTableDto> closedTableDtos = closedTables.stream()
					.map(PokerTableDto::new)
					.collect(Collectors.toUnmodifiableSet());
			return ResponseEntity.ok(closedTableDtos);
		} else {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
	}

	@PostMapping("/{tableId}/reset-all-votes")
	public ResponseEntity<Void> resetAllVotes(@PathVariable Long tableId) {
		pokerTableService.resetAllVotes(tableId);
		return ResponseEntity.noContent().build();
	}
}
