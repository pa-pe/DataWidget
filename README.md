# Data Widget

An Android application that allows you to display dynamic data from remote JSON sources directly on your home screen. Designed for high-frequency updates and extreme customization.

## Key Features

- **High Frequency Updates**: Powered by a Foreground Service, capable of updating every second.
- **Dynamic Layouts**: Build your widget UI using a simple Bootstrap-like grid system defined in JSON.
- **Multiple Widgets**: Add multiple widgets, each pointing to a different data source.
- **Live Counters**: Built-in support for countdown timers that update in real-time.
- **Visual Customization**:
    - **Cloud side**: Control text colors, alignment, and add separators via JSON.
    - **Local side**: Users can customize widget background color and transparency to match their wallpaper.
- **Developer Tools**: Built-in "Test" feature to validate your JSON connection and structure.

## JSON Schema Documentation

The widget is rendered based on a JSON object fetched from your URL.

### Root Parameters

| Parameter             | Type    | Description                                                                       |
|:----------------------|:--------|:----------------------------------------------------------------------------------|
| `update_interval_sec` | Integer | (Optional) How often to fetch new data from the server (in seconds). Default: 60. |
| `next_update_at`      | Long    | (Optional) Exact Unix timestamp (seconds) for the next fetch. Overrides interval. |
| `rows`                | Array   | **Required**. A list of row objects to render.                                    |

### Row Object

A row is a horizontal container for columns.
- `cols`: Array of column objects.

### Column/Cell Object

| Property           | Type   | Description                                                                                    |
|:-------------------|:-------|:-----------------------------------------------------------------------------------------------|
| `type`             | String | Type of cell: `text` (default), `countdown`, or `v-separator`.                                 |
| `text`             | String | Text to display (used for `text` type).                                                        |
| `weight`           | String | Bootstrap-like width: `"12"` (full), `"6"` (half), `"4"` (1/3), etc.                           |
| `color`            | String | Text or separator color. Supports HEX (`#RRGGBB`) or web names (e.g., `green`, `yellowgreen`). |
| `align`            | String | Text alignment: `left`, `center`, `right`.                                                     |
| `target_timestamp` | Long   | Unix timestamp in seconds (Required for `countdown` type).                                     |

## Example JSON

```json
{
  "update_interval_sec": 60,
  "rows": [
    {
      "cols": [
        { "text": "Status:", "weight": "4", "align": "left" },
        { "type": "v-separator", "color": "gray" },
        { "text": "ACTIVE", "weight": "8", "color": "green", "align": "right" }
      ]
    },
    {
      "cols": [
        { "text": "New Year in:", "weight": "6" },
        { "type": "countdown", "target_timestamp": 1893456000, "weight": "6", "color": "red" }
      ]
    }
  ]
}
```

## Examples

You can find more JSON templates in the `/examples` directory of this repository:
- `countdown_2030.json`: A simple layout with a text label and a live countdown timer.

## Battery Optimization & Privacy

By default, the **"Update only when screen is on"** setting is enabled. 

- **Pros**: Significantly saves battery and network traffic by pausing all activities when the device is not in use.
- **Cons/Privacy Note**: Since the widget immediately fetches fresh data as soon as the screen is turned on, your server logs may reveal patterns of when you use your phone. 

If you prefer continuous background updates or wish to avoid revealing usage patterns to the server, you can disable this optimization in the widget **Setup** screen.

## Setup

1. Host your JSON file (e.g., on GitHub Gist or Raw).
2. Install the app and add a "Data Widget" to your home screen.
3. In the setup screen, enter your JSON URL and customize the background.
4. Tap the widget to reveal the **Refresh** and **Setup** buttons.

## Development

The project is built with Kotlin and uses a Foreground Service with `specialUse` type to maintain persistent updates. Layouts are rendered using `RemoteViews` with a dynamic nesting logic.
