package pl.xsd.pokertable.developer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.xsd.pokertable.pokertable.PokerTableDto;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoinResponseDto {
	private DeveloperDto developer;
	private PokerTableDto table;
}
