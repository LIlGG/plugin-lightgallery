package run.halo.lightgallery;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class GalleryInitializationTest {
    @Test
    void skipsRepeatedAndEmptyContainersAndContinuesAfterInvalidSelectors() throws Exception {
        var selectors = new LinkedHashSet<>(List.of(".first", ".alias", ".empty", ".bad[",
            "[data-label=\"`${notJavaScript}</script>\"]"));
        String script = LightGalleryHeadProcessor.instantiateGallery(selectors);
        assertThat(script).doesNotContain("</script>");
        String harness = """
            const assert = require('node:assert/strict');
            let scans = 0, writes = 0, initialized = 0, warnings = 0;
            function container(size) {
              let bound = false;
              const images = Array.from({length: size}, () => ({src: '/image.png',
                dataset: {set src(value) { assert.equal(value, '/image.png'); writes++; }}}));
              return {
                hasAttribute: () => bound,
                querySelectorAll: () => { scans++; return images; },
                bind: () => { bound = true; }
              };
            }
            const first = container(2), last = container(1), empty = container(0);
            const document = {querySelectorAll(selector) {
              if (selector === '.bad[') throw new SyntaxError('Invalid selector');
              if (selector === '.first' || selector === '.alias') return [first];
              if (selector === '.empty') return [empty];
              assert.equal(selector, '[data-label="`${notJavaScript}</script>"]');
              return [last];
            }};
            const console = {warn() {warnings++;}};
            function lightGallery(element) { initialized++; element.bind(); }
            """;
        var process = new ProcessBuilder("node").redirectErrorStream(true).start();
        try (var input = process.getOutputStream()) {
            input.write((harness + script + """
                assert.equal(scans, 3);
                assert.equal(writes, 3);
                assert.equal(initialized, 2);
                assert.equal(warnings, 1);
                """).getBytes(StandardCharsets.UTF_8));
        }
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(process.waitFor()).withFailMessage(output).isZero();
    }
}
