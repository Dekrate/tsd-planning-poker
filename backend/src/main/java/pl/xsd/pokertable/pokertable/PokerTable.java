package pl.xsd.pokertable.pokertable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.xsd.pokertable.developer.Developer;
import pl.xsd.pokertable.userstory.UserStory;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PokerTable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private Boolean isClosed;

	@OneToMany(mappedBy = "pokerTable", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonIgnore
	private Set<Developer> developers = new HashSet<>();

	@OneToMany(mappedBy = "pokerTable", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonIgnore
	private Set<UserStory> userStories = new HashSet<>();
	@ManyToMany(mappedBy = "pastTables")
	@JsonIgnore
	private Set<Developer> pastParticipants = new HashSet<>();


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

	public PokerTable(Long id, String name, LocalDateTime createdAt, Boolean isClosed) {
		this.id = id;
		this.name = name;
		this.createdAt = createdAt;
		this.isClosed = isClosed;
	}

	public boolean getIsActive() {
		return !isClosed;
	}
}
