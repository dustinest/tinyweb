package ee.fj.http.tinyweb.server.request;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

class ISO_8859_1ByteBuffer extends ByteArrayOutputStream {
	private static final Logger logger = Logger.getLogger(ISO_8859_1ByteBuffer.class.getName());

	Optional<String> getAsString() {
		if (this.size() > 0) {
			return Optional.ofNullable(this.toString()).filter(t -> t.length() > 0);
		}
		return Optional.empty();
	}

	@Override
	public String toString() {
		try {
			return super.toString(StandardCharsets.ISO_8859_1.name());
		} catch (UnsupportedEncodingException e) {
			return super.toString();
		}
	}

	boolean charAtIs(int index, char character) {
		if (this.size() <= index) {
			return false;
		}
		return this.buf[0] == character;
	}

	private Optional<String> subString(String value) {
		try {
			return getAsString().filter(t -> t.startsWith(value)).filter(t -> t.length() > value.length())
					.map(t -> t.substring(value.length()));
		} catch (Throwable t) {
			logger.log(Level.WARNING, t.getMessage(), t);
		}
		return Optional.empty();
	}

	void parseMeaningfulPrefix(BiConsumer<String, String> consumer, String... prefix) {
		Stream.of(prefix).forEach(p -> this.subString(p).ifPresent(t -> consumer.accept(p, t)));
		consumer.accept(null, null);
	}

	void reduce(int amount) {
		this.count -= amount;
		if (this.count < 0) {
			this.count = 0;
		}
	}

	boolean endsWith(char character) {
		if (this.count == 0) {
			return false;
		}
		return this.buf[this.count - 1] == character;
	}

	boolean equals(String value) {
		try {
			return this.getAsString().map(t -> t.equals(value)).isPresent();
		} catch (Throwable t) {
			logger.log(Level.WARNING, t.getMessage(), t);
			return false;
		}
	}
}