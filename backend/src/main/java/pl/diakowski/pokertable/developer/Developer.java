package pl.diakowski.pokertable.developer;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.diakowski.pokertable.pokertable.PokerTable;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
public class Developer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String sessionId;

	@Column
	private String displayName;

	@Column(nullable = false)
	private LocalDateTime joinedAt;

	@ManyToOne(optional = false)
	@JoinColumn(name = "poker_table_id")
	private PokerTable pokerTable;

	public Developer(String sessionId, String displayName, PokerTable pokerTable) {
		this.sessionId = sessionId;
		this.displayName = displayName;
		this.joinedAt = LocalDateTime.now();
		this.pokerTable = pokerTable;
	}
}