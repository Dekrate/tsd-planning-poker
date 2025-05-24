package pl.xsd.pokertable.userstory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStoryDto {
	private Long id;
	private String title;
	private String description;
	private Integer estimatedPoints;

}
