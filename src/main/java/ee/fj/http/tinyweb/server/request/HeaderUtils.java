package ee.fj.http.tinyweb.server.request;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class HeaderUtils {
	private static final Logger logger = Logger.getLogger(HeaderUtils.class.getName());
	private static final Function<String, Stream<String>> COMMA_PATTERN = compileToStreamPattern(",");
	private static final Pattern SEMICOLON_SPLIT_PATTERN = Pattern.compile(";");
	private static final Pattern EQUALS_SPLIT_PATTERN = Pattern.compile("=");
	static final Function<String, List<String>> COMMA_WITH_SPACE_PATTERN = HeaderUtils.getCommaSeparatedSplitToList();
	private static final DecimalFormat WEIGHT_FORMAT = new DecimalFormat("##.##");


	protected static Function<String, Stream<String>> compileToStreamPattern(String delimiter) {
		final Pattern pattern = Pattern.compile(delimiter);
		return s -> pattern.splitAsStream(s).filter(Objects::nonNull).map(String::trim).filter(t -> !t.isEmpty());
	}

	private static Function<String, List<String>> getCommaSeparatedSplitToList() {
		final Function<String, Stream<String>> callable = compileToStreamPattern(", ");
		return s -> callable.apply(s).collect(Collectors.toList());
	}

	static Stream<String> getFirstItemByPriorityStream(String line) {
		float[] lastWeight = new float[]{1f};
		Stream<String[]> sortedList = COMMA_PATTERN.apply(line).map(SEMICOLON_SPLIT_PATTERN::split)
				.map(keyValue -> {
					if (keyValue.length == 0) return null;
					if (keyValue[0] == null || keyValue[0].isBlank()) return null;
					final String value = keyValue[0].trim();
					float weight = lastWeight[0];
					if (keyValue.length > 1 && keyValue[1].length() > 2 && keyValue[1].startsWith("q=")) {
						try {
							weight = Float.parseFloat(keyValue[1].substring(2));
						} catch (NumberFormatException e) {
							logger.log(Level.WARNING, "Error while parsing number " + value, e);
						}
					}
					lastWeight[0] = weight;
					return new String[]{value, WEIGHT_FORMAT.format(weight)};
				}).filter(Objects::nonNull);
		return sortedList.sorted((value1, value2) -> compareFloatsAsString(value2[1], value1[1])).map(k -> k[0]);
	}
	private static int compareFloatsAsString(String value1, String value2) {
		try {
			DecimalFormat decimalFormat = new DecimalFormat("#.0");
			Number number1 = decimalFormat.parse(value1);
			Number number2 = decimalFormat.parse(value2);
			return Float.compare(number1.floatValue(), number2.floatValue());
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

	static Stream<String[]> commaSeparatedKeyValueStream(String line) {
		return COMMA_PATTERN.apply(line).filter(Objects::nonNull)
				.filter(t -> !t.isBlank() && t.length() > 0)
				.map(EQUALS_SPLIT_PATTERN::split)
				.map(keyVal -> {
					if ((keyVal == null)) return null;
					String key = keyVal.length > 0 && keyVal[0] != null ? keyVal[0].trim() : null;
					String value = keyVal.length > 1 && keyVal[1] != null ? keyVal[1].trim() : null;
					if (key != null && !key.isBlank()) {
						return new String[]{key, value != null && !value.isBlank() ? value : ""};
					}
					return null;
				}).filter(Objects::nonNull);
	}
	protected static List<String> getFirstItemByPriority(String line) {
		return getFirstItemByPriorityStream(line).collect(Collectors.toList());
	}
}
