package pl.xsd.pokertable.pokertable;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.xsd.pokertable.developer.Developer;
import pl.xsd.pokertable.participation.Participation;
import pl.xsd.pokertable.userstory.UserStory;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Setter
@Getter
@NoArgsConstructor
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
	@JsonBackReference
	private Set<Developer> developers = new HashSet<>();

	@OneToMany(mappedBy = "pokerTable", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonIgnore
	private Set<UserStory> userStories = new HashSet<>();

	@OneToMany(mappedBy = "pokerTable", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonIgnore
	private Set<Participation> participations = new HashSet<>();

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

	public PokerTable(long id, String name, LocalDateTime createdAt, boolean isClosed) {
		this.id = id;
		this.name = name;
		this.createdAt = createdAt;
		this.isClosed = isClosed;
	}
	@PrePersist
	protected void onCreate() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
		if (isClosed == null) {
			isClosed = false;
		}
	}
}
