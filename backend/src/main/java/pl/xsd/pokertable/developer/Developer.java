package pl.xsd.pokertable.developer;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.xsd.pokertable.pokertable.PokerTable;

@Entity
@NoArgsConstructor
@Getter
@Setter
public class Developer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String sessionId; // Nowe pole

	@ManyToOne
	@JoinColumn(name = "poker_table_id")
	private PokerTable pokerTable;

	@Column(nullable = false)
	private Integer vote;

	public boolean hasVoted() {
		return vote == null;
	}

	// Nowy konstruktor
	public Developer(String sessionId, String name) {
		this.sessionId = sessionId;
		this.name = name;
	}
}
