import createGallery from "lightgallery";
import lgZoom from "lightgallery/plugins/zoom";
import "lightgallery/css/lightgallery.css";
import "lightgallery/css/lg-zoom.css";
import "./lightgallery.css";

const instances = new WeakMap<HTMLElement, ReturnType<typeof createGallery>>();

// Keep instances local to this module; no browser globals are required.
export function lightGallery(
  container: HTMLElement,
  options: Parameters<typeof createGallery>[1] = {},
) {
  if (!container || container.hasAttribute("lg-uid")) {
    return container ? instances.get(container) : undefined;
  }
  const instance = createGallery(container, {
    startAnimationDuration: 200,
    backdropDuration: 150,
    // Gallery items are images themselves, not links containing thumbnails.
    exThumbImage: "src",
    ...options,
    addClass: ["halo-lightgallery", options.addClass].filter(Boolean).join(" "),
    // Keep visible controls on phones instead of requiring undisclosed gestures.
    mobileSettings: {
      controls: true,
      showCloseIcon: true,
      download: true,
      ...options.mobileSettings,
    },
    plugins: [lgZoom],
  });
  // An img width attribute controls thumbnail layout, not the original image size.
  // Let zoom use naturalWidth unless the author explicitly supplied data-width.
  instance.galleryItems.forEach((item, index) => {
    const source = instance.items[index];
    if (source instanceof HTMLImageElement && !source.hasAttribute("data-width")) {
      delete item.width;
    }
  });
  instances.set(container, instance);
  container.setAttribute("lg-uid", "halo-lightgallery-v2");
  return instance;
}
