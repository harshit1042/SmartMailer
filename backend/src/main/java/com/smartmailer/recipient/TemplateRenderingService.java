package com.smartmailer.recipient;

import com.smartmailer.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class TemplateRenderingService {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_\\- ]+)\\s*}}");

    public String render(String template, Map<String, String> row) {
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1).trim();
            String value = row.getOrDefault(key, "");
            matcher.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    public void validate(String subjectTemplate, String bodyTemplate, Set<String> headers) {
        Set<String> tokens = tokens(subjectTemplate + "\n" + bodyTemplate);
        Set<String> missing = tokens.stream().filter(token -> !headers.contains(token)).collect(Collectors.toSet());
        if (!missing.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Template placeholders missing from upload columns: " + missing);
        }
    }

    private Set<String> tokens(String template) {
        Matcher matcher = PLACEHOLDER.matcher(template);
        return matcher.results().map(result -> result.group(1).trim()).collect(Collectors.toSet());
    }
}
