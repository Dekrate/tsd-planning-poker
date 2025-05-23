package pl.xsd.pokertable.pokertable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PokerTableDto {
	private Long id;
	private String name;
	private LocalDateTime createdAt;
	private Boolean isClosed;

	// Konstruktor do konwersji z encji PokerTable
	public PokerTableDto(PokerTable pokerTable) {
		this.id = pokerTable.getId();
		this.name = pokerTable.getName();
		this.createdAt = pokerTable.getCreatedAt();
		this.isClosed = pokerTable.getIsClosed();
	}
}
