package pl.xsd.pokertable.pokertable;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tables")
@AllArgsConstructor
public class PokerTableController {

	private final PokerTableService pokerTableService;
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
		pokerTableService.closePokerTable(id);
		return ResponseEntity.noContent().build();

	}

	@GetMapping("/active")
	public ResponseEntity<PokerTable> getActiveTable() {
		PokerTable table = pokerTableService.getActiveTable();
		return ResponseEntity.ok(table);
	}

	@GetMapping("/{id}")
	public ResponseEntity<PokerTable> getTableById(@PathVariable Long id) {
		return ResponseEntity.ok(pokerTableService.getTableById(id));
	}

	@GetMapping("/{tableId}/export-stories")
	public ResponseEntity<byte[]> exportUserStories(@PathVariable Long tableId) {
		byte[] csvBytes = pokerTableService.exportUserStoriesToCsv(tableId);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.parseMediaType("text/csv"));
		headers.setContentDispositionFormData("attachment", "poker-planning-export-" + tableId + ".csv");

		return ResponseEntity.ok()
				.headers(headers)
				.body(csvBytes);
	}
}

