package org.fengling.anti_addiction;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenericJsonParser {

    public static Object parse(String json) throws IOException {
        try (StringReader reader = new StringReader(json)) {
            return parseValue(reader, next(reader));
        }
    }

    private static Map<String, Object> parseJsonObject(StringReader reader) throws IOException {
        Map<String, Object> object = new HashMap<>();

        char c = next(reader);
//        Anti_addiction.LOGGER.info("Parsing value: {}", c);

        while (c != '}') {
            String key = parseString(reader);
            c = next(reader);
            if (c != ':') {
                throw new IOException("Expected ':' after key");
            }

            Object value = parseValue(reader, next(reader));
            object.put(key, value);

            c = next(reader);
            if (c == ',') {
                c = next(reader);
            } else if (c != '}') {
                throw new IOException("Expected ',' or '}' after value");
            }
        }

        return object;
    }

    private static List<Object> parseJsonArray(StringReader reader) throws IOException {
        List<Object> array = new ArrayList<>();

        char c = next(reader);

        while (c != ']') {
            Object value = parseValue(reader, c);
            array.add(value);

            c = next(reader);
            if (c == ',') {
                c = next(reader);
            } else if (c != ']') {
                throw new IOException("Expected ',' or ']' after value");
            }
        }

        return array;
    }

    private static Object parseValue(StringReader reader, char c) throws IOException {
//        Anti_addiction.LOGGER.info("Parsing value: {}", c);

        if (c == '"') {
            return parseString(reader);
        } else if (Character.isDigit(c) || c == '-') {
            return parseNumber(reader, c);
        } else if (c == '{') {
            return parseJsonObject(reader);
        } else if (c == '[') {
            return parseJsonArray(reader);
        } else if (c == 't' || c == 'f') {
            return parseBoolean(reader, c);
        } else if (c == 'n') {
            return parseNull(reader);
        } else {
            throw new IOException("Unexpected character: " + c);
        }
    }

    private static String parseString(StringReader reader) throws IOException {
        StringBuilder sb = new StringBuilder();

        while (true) {
            char c = (char) reader.read();

            if (c == '"') {
                break;
            }

            if (c == '\\') {
                c = (char) reader.read();
                switch (c) {
                    case '"':
                        sb.append('"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case '/':
                        sb.append('/');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        // Handle Unicode escape sequence
                        StringBuilder hexCode = new StringBuilder();
                        for (int i = 0; i < 4; i++) {
                            c = (char) reader.read();
                            if (Character.digit(c, 16) == -1) {
                                throw new IOException("Invalid Unicode escape sequence: expected 4 hex digits but got: " + hexCode.toString() + c);
                            }
                            hexCode.append(c);
                        }
                        try {
                            int unicodeValue = Integer.parseInt(hexCode.toString(), 16);
                            sb.append((char) unicodeValue);
                        } catch (NumberFormatException e) {
                            throw new IOException("Invalid Unicode escape sequence: could not parse hex value: " + hexCode.toString());
                        }
                        break;
                    default:
                        throw new IOException("Invalid escape sequence: \\" + c);
                }
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    private static Number parseNumber(StringReader reader, char firstChar) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(firstChar);

        while (true) {
            reader.mark(1);  // mark the current position to allow rewinding.

            char c = (char) reader.read();
            if (Character.isDigit(c) || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                sb.append(c); // 添加数字字符到字符串
            } else {
                reader.reset();  // rewind to before the non-digit character
                break;
            }
        }


        String numberStr = sb.toString();
        try {
            if (numberStr.contains(".") || numberStr.contains("e") || numberStr.contains("E")) {
                return Double.parseDouble(numberStr);
            } else {
                try {
                    return Integer.parseInt(numberStr);
                } catch (NumberFormatException e) {
                    return Long.parseLong(numberStr);
                }

            }
        } catch (NumberFormatException e) {
            throw new IOException("Invalid number format: " + numberStr);
        }
    }


    private static Boolean parseBoolean(StringReader reader, char firstChar) throws IOException {
        if (firstChar == 't') {
            if (reader.read() == 'r' && reader.read() == 'u' && reader.read() == 'e') {
                return true;
            } else {
                throw new IOException("Expected 'true'");
            }
        } else if (firstChar == 'f') {
            if (reader.read() == 'a' && reader.read() == 'l' && reader.read() == 's' && reader.read() == 'e') {
                return false;
            } else {
                throw new IOException("Expected 'false'");
            }
        } else {
            throw new IOException("Expected 'true' or 'false'");
        }
    }

    private static Object parseNull(StringReader reader) throws IOException {
        if (reader.read() == 'u' && reader.read() == 'l' && reader.read() == 'l') {
            return null;
        } else {
            throw new IOException("Expected 'null'");
        }
    }

    private static char next(StringReader reader) throws IOException {
        reader.mark(1);
        char c = (char) reader.read();
        while (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
            reader.mark(1);
            c = (char) reader.read();
        }
        return c;
    }
}