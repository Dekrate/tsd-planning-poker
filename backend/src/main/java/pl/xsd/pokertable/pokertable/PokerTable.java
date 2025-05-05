package pl.xsd.pokertable.pokertable;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.xsd.pokertable.developer.Developer;
import pl.xsd.pokertable.userstory.UserStory;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Setter
@Getter

public class PokerTable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	private Boolean isClosed;

	@OneToMany(mappedBy = "pokerTable", fetch = FetchType.EAGER)
	@JsonBackReference // ← Ważna adnotacja
	private Set<Developer> developers;

	@OneToMany(mappedBy = "pokerTable", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true) // Add relationship to UserStory
	@JsonIgnore
	private Set<UserStory> userStories;

	public PokerTable(Long id, String name) {
		this.id = id;
		this.name = name;
		this.createdAt = LocalDateTime.now();
		this.isClosed = false;
	}

	public PokerTable(Long id, String name, Boolean isClosed) {
		this.id = id;
		this.name = name;
		this.createdAt = LocalDateTime.now();
		this.isClosed = isClosed;
	}

	public PokerTable() {
		this.createdAt = LocalDateTime.now();
		this.isClosed = false;
	}
}
