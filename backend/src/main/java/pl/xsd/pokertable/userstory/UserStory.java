package pl.xsd.pokertable.userstory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import pl.xsd.pokertable.pokertable.PokerTable;

@Entity
@Getter
@Setter
@AllArgsConstructor
public class UserStory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String title;

	@Column(columnDefinition = "TEXT")
	private String description;

	 private Integer estimatedPoints;

	@ManyToOne
	@JoinColumn(name = "poker_table_id", nullable = false)
	@JsonIgnore
	private PokerTable pokerTable;

	public UserStory() {
	}

	public UserStory(String title, String description) {
		this.title = title;
		this.description = description;
	}
}