package pl.xsd.pokertable.pokertable;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.xsd.pokertable.developer.DeveloperService;

@RestController
@RequestMapping("/tables")
@AllArgsConstructor
public class PokerTableController {

	private final PokerTableService pokerTableService;
	private final DeveloperService developerService;

	// Endpoint do tworzenia stołu pokerowego
	@PostMapping
	public ResponseEntity<PokerTable> createPokerTable() {
		try {
			PokerTable pokerTable = pokerTableService.createPokerTable("Blank");
			return ResponseEntity.status(201).body(pokerTable);
		} catch (IllegalStateException e) {
			return ResponseEntity.status(400).body(null);
		}
	}

	@PatchMapping("/{id}/close")
	public ResponseEntity<Void> closePokerTable(@PathVariable Long id) {
		try {
			pokerTableService.closePokerTable(id);
			return ResponseEntity.noContent().build();
		} catch (IllegalStateException e) {
			return ResponseEntity.status(400).build();
		}
	}

	@GetMapping("/active")
	public ResponseEntity<PokerTable> getActiveTable() {
		PokerTable table = pokerTableService.getActiveTable();
		return ResponseEntity.ok(table);
	}
}
