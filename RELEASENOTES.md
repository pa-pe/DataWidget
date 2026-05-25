# Release Notes - Data Widget

## v1.3.93 (Current)
- **Expanded Icon Library**: Added support for 100+ built-in Material Design icons via the `icon` JSON parameter.
- **Advanced Text Styling**: Individual cells now support `bold`, `italic`, `underline`, and `strike` flags.
- **Flexible Font Sizes**: Support for absolute (e.g., `"16"`) and relative (e.g., `"+2"`, `"-1"`) font sizes per cell.
- **Interactive Custom Buttons**: Added a `buttons` array to the JSON schema to show custom clickable actions on tap.
- **Overlay Controls Panel**: Redesigned the tap-to-reveal menu as a semi-transparent overlay to maximize content area.
- **Enhanced Grid System**: Added `"weight": "auto"` for natural text wrapping and support for vertical/horizontal separators.
- **Global UI Settings**: Centralized controls for panel color and transparency in the app settings with a live preview.
- **Bug Fixes**: Fixed an issue where a default countdown would display during widget initialization. Improved fetch reliability.

## v1.2.69
- Initial release of Data Widget.
- Display dynamic data from JSON URLs directly on your home screen.
- Build custom layouts with a flexible 12-column grid system.
- Per-second precision for live counters and real-time updates.
- Complete visual control: customize colors, transparency, and font sizes.
- Support for both GET and POST HTTP methods.
- Built-in library for saving and managing your widget configurations.
- Battery-efficient: updates pause automatically when the screen is off.
