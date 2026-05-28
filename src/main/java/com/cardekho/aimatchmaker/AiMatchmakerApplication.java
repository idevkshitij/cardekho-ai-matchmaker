package com.cardekho.aimatchmaker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.io.InputStream;
import java.util.Base64;
import java.util.Properties;
import java.util.Optional;

@SpringBootApplication
public class AiMatchmakerApplication {

	public static void main(String[] args) {
		String apiKey = Optional.ofNullable(System.getenv("GROQ_API_KEY"))
				.filter(key -> !key.trim().isEmpty())
				.orElseGet(() -> {
					try {
						Properties props = new Properties();
						try (InputStream input = AiMatchmakerApplication.class.getClassLoader()
								.getResourceAsStream("application.properties")) {
							if (input != null) {
								props.load(input);
								return Optional.ofNullable(props.getProperty("groq.api.key.obfuscated"))
										.filter(key -> !key.trim().isEmpty())
										.map(key -> new String(Base64.getDecoder().decode(key.trim())).trim())
										.orElse(null);
							}
						}
					} catch (Exception e) {
						System.err.println(">>> Failed to load or decode obfuscated Groq API key: " + e.getMessage());
					}
					return null;
				});

		if (Optional.ofNullable(apiKey).filter(key -> !key.trim().isEmpty() && !key.equals("dummy-key")).isPresent()) {
			System.setProperty("spring.ai.openai.api-key", apiKey);
			System.out.println(">>> [SUCCESS] Groq API Key has been successfully configured from " 
					+ (System.getenv("GROQ_API_KEY") != null ? "environment variable" : "obfuscated application properties") + ".");
		} else {
			System.out.println(">>> [WARNING] No Groq API Key found. AI Matchmaking features will be disabled until a key is provided.");
		}

		SpringApplication.run(AiMatchmakerApplication.class, args);
	}
}
