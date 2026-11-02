# Liquid Glass interface

The interface material is shared by the ClickGUI, menu buttons, floating panels,
text fields, tooltips and HUD backgrounds. It is independent of the Glass module,
which renders players, armor and held items.

Open **Appearance → Glass Material**:

- **Interface Material → Liquid Glass** enables frosted surfaces, curved-edge
  refraction, directional highlights, capsule switches and pill slider handles.
- **Glass Refraction** controls background displacement; zero removes distortion.
- **Glass Highlights** controls edge lighting; zero removes the sheen.
- **Interface Material → Classic** restores the previous surfaces and controls.

The material defaults to Liquid Glass. Existing palette, font, layout, enabled
modules, HUD background choices and accessibility settings are retained. For the
complete coordinated appearance, apply **Appearance Preset → Liquid Glass**. It
selects bundled Inter, restrained teal/blue colors, softer corners and spacing,
and disables global glow. **Undo preset** restores all settings changed by that
application. Other presets do not need to replace your chosen typeface.

The sidebar floats inside the main window. Its selection pill and clickable rows
share the same width. Navigation, switch motion and slider interpolation use the
existing animation channels, including **Reduced Motion**. The header groups page
navigation and actions on one surface. Settings remain on calmer, readable cards.

## Rendering

The original shaders use the existing rounded-rectangle distance geometry to
derive a curved bevel and displace background sampling near the edges. The
existing dual-filter blur is shared; glass does not blur each control separately.
World blur regions include the maximum displacement support so moving panels
cannot sample unwritten pixels. Brightness adaptation happens in the fragment
shader with no CPU image readback. Text and icons are submitted above the material.

World surfaces sample the completed scene before GUI rendering. Menu surfaces
sample the selected cached backdrop or Living Panorama. Floating popups and small
controls use a matching tinted surface and sheen: they do not capture or refract
other GUI text beneath them. HUD elements retain their individual None/Solid/Blur
choices. Solid HUD panels receive a subtle sheen without starting world capture.
With GUI blur disabled, the main panel uses a denser tint for legibility.

All rendering uses Minecraft 26.2 Blaze3D pipelines, pooled targets and existing
resource cleanup. There are no new native dependencies, browser overlays, Apple
frameworks, copied Apple assets or changes to terrain/fluid rendering.

## References

- [Apple: Meet Liquid Glass](https://developer.apple.com/videos/play/wwdc2025/219/)
- [Apple: Materials](https://developer.apple.com/design/human-interface-guidelines/materials)
- [Apple: App structure, toolbars and controls](https://developer.apple.com/videos/play/wwdc2025/323/)
- [Kube: Refraction and specular highlight experiments](https://kube.io/blog/liquid-glass-css-svg/)

These informed the visual direction. The implementation is original project code.
See the [review build notes](releases/v0.30.0-beta.1.md) for verification boundaries.
