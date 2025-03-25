package pl.xsd.pokertable;

import org.springframework.boot.SpringApplication;

public class TestPokertableApplication {

	public static void main(String[] args) {
		SpringApplication.from(PokertableApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
