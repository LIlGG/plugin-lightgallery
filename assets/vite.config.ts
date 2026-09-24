import { fileURLToPath, URL } from "url";
import { defineConfig } from "vite";

export default defineConfig({
  build: {
    // Preserve the previous Vite 4 browser baseline when upgrading the build tool.
    target: ["es2020", "edge88", "firefox78", "chrome87", "safari14"],
    outDir: fileURLToPath(new URL("../build/generated-resources/static", import.meta.url)),
    emptyOutDir: true,
    lib: {
      entry: "src/index.ts",
      formats: ["es"],
      fileName: () => "main.js",
    },
    rolldownOptions: {
      output: {
        assetFileNames: (asset) => asset.names.some((name) => name.endsWith(".css")) ? "main.css" : "[name][extname]",
      },
    },
  },
});
