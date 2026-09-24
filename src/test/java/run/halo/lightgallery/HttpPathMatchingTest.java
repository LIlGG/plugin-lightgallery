package run.halo.lightgallery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.IWebContext;
import org.thymeleaf.context.ITemplateContext;
import run.halo.app.plugin.ReactiveSettingFetcher;

class HttpPathMatchingTest {
    interface WebTemplateContext extends IWebContext, ITemplateContext {}

    @Test
    void ignoresIncompleteRulesWithoutDiscardingValidMatches() {
        var context = mock(WebTemplateContext.class, RETURNS_DEEP_STUBS);
        when(context.getExchange().getRequest().getRequestPath()).thenReturn("/archives/demo.html");
        var incomplete = new LightGalleryHeadProcessor.PathMatchRule();
        var invalid = new LightGalleryHeadProcessor.PathMatchRule();
        invalid.setPathPattern("/archives/**/invalid");
        var valid = new LightGalleryHeadProcessor.PathMatchRule();
        valid.setPathPattern("/archives/**");
        valid.setDomSelector(".content");
        var config = new LightGalleryHeadProcessor.BasicConfig();
        config.setRules(Arrays.asList(null, incomplete, invalid, valid));
        var processor = new LightGalleryHeadProcessor(mock(ReactiveSettingFetcher.class));
        assertThat(processor.isRequestPathMatchingRoute(context, config).domSelectors())
            .containsExactly(".content");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
        "/archives/**, /archives/demo, true",
        "/archives/**, /archives/demo.html, true",
        "/archives/**, /archives/a/b.html, true",
        "/archives/*.html, /archives/demo.html, true",
        "/archives/*.html, /archives/a/b.html, false",
        "/archives/**, /moments/demo.html, false",
        "/archives/**, /archives/a%20b.html, true"
    })
    void matchesHttpPaths(String pattern, String path, boolean expected) {
        var matcher = LightGalleryHeadProcessor.createRouteMatcher();
        assertThat(matcher.match(pattern, matcher.parseRoute(path))).isEqualTo(expected);
    }


}
