package run.halo.lightgallery;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import io.micrometer.common.util.StringUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;
import org.springframework.http.server.PathContainer;
import org.springframework.util.RouteMatcher;
import org.springframework.web.util.pattern.PathPatternRouteMatcher;
import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.web.util.pattern.PatternParseException;
import org.thymeleaf.context.Contexts;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import org.thymeleaf.web.IWebRequest;
import org.unbescape.javascript.JavaScriptEscape;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;
import run.halo.app.theme.dialect.TemplateHeadProcessor;

/**
 * @author ryanwang
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LightGalleryHeadProcessor implements TemplateHeadProcessor {
    private static final String TEMPLATE_ID_VARIABLE = "_templateId";
    private static final Pattern HEX_COLOR = Pattern.compile("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?");
    private final ReactiveSettingFetcher reactiveSettingFetcher;
    private final PathPatternRouteMatcher routeMatcher = createRouteMatcher();

    static PathPatternRouteMatcher createRouteMatcher() {
        var parser = new PathPatternParser();
        parser.setPathOptions(PathContainer.Options.HTTP_PATH);
        return new PathPatternRouteMatcher(parser);
    }

    @Override
    public Mono<Void> process(ITemplateContext context, IModel model,
                              IElementModelStructureHandler structureHandler) {
        return reactiveSettingFetcher.fetch("basic", BasicConfig.class)
                .doOnNext(basicConfig -> {
                    final IModelFactory modelFactory = context.getModelFactory();
                    Set<String> selectors = new LinkedHashSet<>();
                    String domSelector = basicConfig.getDom_selector();
                    if (StringUtils.isNotBlank(domSelector) && isContentTemplate(context)) {
                        selectors.add(domSelector);
                    }

                    MatchResult matchResult = isRequestPathMatchingRoute(context, basicConfig);
                    selectors.addAll(matchResult.domSelectors());
                    if (selectors.isEmpty()) {
                        return;
                    }
                    model.add(modelFactory.createText(lightGalleryScript(selectors) + backdropStyle(basicConfig.getBackdropColor())));
                })
                .onErrorResume(e -> {
                    log.error("LightGalleryHeadProcessor process failed", e);
                    return Mono.empty();
                })
                .then();
    }

    static String backdropStyle(String color) {
        // Only accept hex colors, including the optional alpha channel, before writing CSS.
        String safeColor = color != null && HEX_COLOR.matcher(color).matches()
                ? color : "#000000ff";
        return "<style>.lg-backdrop { background-color: " + safeColor + "; }</style>";
    }

    static String lightGalleryScript(Set<String> domSelectors) {
        return """
                <!-- PluginLightGallery start -->
                <link href="/plugins/PluginLightGallery/assets/static/main.css" rel="stylesheet" />
                <script defer src="/plugins/PluginLightGallery/assets/static/main.js"></script>
                <script type="text/javascript">
                    document.addEventListener("DOMContentLoaded", function () {
                       %s
                    });
                </script>
                <!-- PluginLightGallery end -->
                """.formatted(instantiateGallery(domSelectors));
    }

    static String instantiateGallery(Set<String> domSelectors) {
        return domSelectors.stream()
                .map(domSelector -> """
                        try {
                          document.querySelectorAll("%s").forEach(function (container) {
                            if (container.hasAttribute("lg-uid")) {
                              return;
                            }
                            const images = container.querySelectorAll("img");
                            if (images.length === 0) {
                              return;
                            }
                            images.forEach(function (image) {
                              image.dataset.src = image.src;
                            });
                            lightGallery(container, { selector: "img" });
                          });
                        } catch (error) {
                          console.warn("LightGallery: unable to initialize a gallery rule", error);
                        }
                        """.formatted(JavaScriptEscape.escapeJavaScript(domSelector))
                )
                .collect(Collectors.joining("\n"));
    }

    public boolean isContentTemplate(ITemplateContext context) {
        return "post".equals(context.getVariable(TEMPLATE_ID_VARIABLE))
                || "page".equals(context.getVariable(TEMPLATE_ID_VARIABLE));
    }

    public MatchResult isRequestPathMatchingRoute(ITemplateContext context, BasicConfig basicConfig) {
        if (basicConfig.nullSafeRules().isEmpty() || !Contexts.isWebContext(context)) {
            return MatchResult.mismatch();
        }
        IWebRequest request = Contexts.asWebContext(context).getExchange().getRequest();
        String requestPath = request.getRequestPath();
        RouteMatcher.Route requestRoute = routeMatcher.parseRoute(requestPath);

        Set<String> selectors = basicConfig.nullSafeRules()
                .stream()
                .filter(rule -> isMatchedRoute(requestRoute, rule))
                .map(rule -> defaultIfBlank(rule.getDomSelector(), "body"))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return !selectors.isEmpty()
                ? new MatchResult(true, selectors)
                : MatchResult.mismatch();
    }

    private boolean isMatchedRoute(RouteMatcher.Route requestRoute, PathMatchRule rule) {
        if (rule == null || StringUtils.isBlank(rule.getPathPattern())) {
            return false;
        }
        try {
            return routeMatcher.match(rule.getPathPattern(), requestRoute);
        } catch (PatternParseException e) {
            // ignore
            log.warn("Parse route pattern [{}] failed", rule.getPathPattern(), e);
        }
        return false;
    }

    record MatchResult(boolean matched, Set<String> domSelectors) {
        public static MatchResult mismatch() {
            return new MatchResult(false, Set.of());
        }
    }

    @Data
    public static class BasicConfig {
        String dom_selector;
        String backdropColor;
        List<PathMatchRule> rules;

        public List<PathMatchRule> nullSafeRules() {
            return ObjectUtils.defaultIfNull(rules, List.of());
        }
    }

    @Data
    public static class PathMatchRule {
        private String pathPattern;
        private String domSelector;
    }
}
