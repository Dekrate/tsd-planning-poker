package pl.diakowski.pokertable.developer;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.diakowski.pokertable.pokertable.PokerTable;

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

	@ManyToOne
	@JoinColumn(name = "poker_table_id")
	private PokerTable pokerTable;

	@Column(nullable = false)
	private Integer vote;

	public Developer(String name, PokerTable pokerTable) {
		this.name = name;
		this.pokerTable = pokerTable;
	}

	public boolean hasVoted() {
		return vote == null;
	}
}
