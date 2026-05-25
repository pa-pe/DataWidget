# Data Widget

An Android application that allows you to display dynamic data from remote JSON sources directly on your home screen. Designed for high-frequency updates and extreme customization.

## Key Features

- **High Frequency Updates**: Powered by a Foreground Service, capable of updating every second.
- **Dynamic Layouts**: Build your widget UI using a simple Bootstrap-like grid system defined in JSON.
- **Multiple Widgets**: Add multiple widgets, each pointing to a different data source.
- **Live Counters**: Built-in support for countdown timers that update in real-time.
- **Visual Customization**:
    - **Cloud side**: Control text colors, alignment, and add separators via JSON.
    - **Local side**: Users can customize widget background color, transparency, and **base font size** to match their wallpaper.
- **HTTP Request Control**: Choose between **GET** and **POST** methods for fetching data.
- **Developer Tools**: Built-in "Test" feature to validate your JSON connection and structure.

## JSON Schema Documentation

The widget is rendered based on a JSON object fetched from your URL.

### Root Parameters

| Parameter             | Type    | Description                                                                       |
|:----------------------|:--------|:----------------------------------------------------------------------------------|
| `update_interval_sec` | Integer | (Optional) How often to fetch new data from the server (in seconds). Default: 60. |
| `next_update_at`      | Long    | (Optional) Exact Unix timestamp (seconds) for the next fetch. Overrides interval. |
| `buttons`             | Array   | (Optional) A list of custom buttons to show when the widget is tapped.            |
| `rows`                | Array   | **Required**. A list of row objects to render.                                    |

### Button Object

| Property | Type   | Description                                                                 |
|:---------|:-------|:----------------------------------------------------------------------------|
| `text`   | String | Label for the button.                                                       |
| `url`    | String | URL to open when the button is clicked.                                     |
| `color`  | String | Text color for the button label. Supports HEX or web names. Default: black. |

### Row Object

A row is a horizontal container for columns.
- `type`: (Optional) Set to `h-separator` for a horizontal line.
- `cols`: Array of column objects (Required if not a separator).
- `color`: String (Optional, for `h-separator`).

### Column/Cell Object

| Property           | Type    | Description                                                                                            |
|:-------------------|:--------|:-------------------------------------------------------------------------------------------------------|
| `type`             | String  | Type of cell: `text` (default), `countdown`, or `v-separator`.                                         |
| `text`             | String  | Text to display (used for `text` type).                                                                |
| `weight`           | String  | Width: `"auto"` (default), `"12"` (full), `"6"` (half), `"4"` (1/3), etc.                              |
| `color`            | String  | Text or separator color. Supports HEX (`#RRGGBB`, `#RGB`) or web names (e.g., `green`, `yellowgreen`). |
| `size`             | String  | Text size in `sp`. Supports absolute values (`"14"`) or relative offsets (`"+2"`, `"-1"`).             |
| `icon`             | String  | (Optional) Icon name. See **[Available Icons](#available-icons)** section.                             |
| `bold`             | Boolean | (Optional) Set to `true` for bold text.                                                                |
| `italic`           | Boolean | (Optional) Set to `true` for italic text.                                                              |
| `underline`        | Boolean | (Optional) Set to `true` to underline text.                                                            |
| `strike`           | Boolean | (Optional) Set to `true` for strikethrough text.                                                       |
| `align`            | String  | Text alignment: `left`, `center`, `right`.                                                             |
| `target_timestamp` | Long    | Unix timestamp in seconds (Required for `countdown` type).                                             |

## Example JSON

```json
{
  "update_interval_sec": 60,
  "rows": [
    {
      "cols": [
        { "text": "Hello World!", "weight": "12", "align": "center" }
      ]
    }
  ]
}
```

## Available Icons

You can use any of the following names in the `"icon"` parameter. All icons are sourced from Google Material Symbols.

| Category       | Icon Names                                                                                                                                       |
|:---------------|:-------------------------------------------------------------------------------------------------------------------------------------------------|
| **Status**     | `check`, `error`, `info`, `warning`, `help`, `notifications`, `lock`, `lock_open`, `new_releases`, `priority_high`                               |
| **Common**     | `person`, `group`, `favorite`, `star`, `star_border`, `star_half`, `share`, `send`, `email`, `call`, `public`, `celebration`, `push_pin`, `bolt` |
| **Touch**      | `touch_app`, `ads_click`                                                                                                                         |
| **Finance**    | `wallet`, `attach_money`, `trending_up`, `trending_down`, `account_balance`, `credit_card`, `shopping_cart`                                      |
| **Weather**    | `sunny`, `cloud`, `rainy`, `thunderstorm`, `ac_unit` (snow), `thermostat`, `water_drop`                                                          |
| **Devices**    | `smartphone`, `laptop`, `desktop_windows`, `watch`, `router`, `power`, `battery_full`, `wifi`, `bluetooth`                                       |
| **Navigation** | `home`, `search`, `menu`, `arrow_back`, `arrow_forward`, `location_on`, `directions_car`, `flight`                                               |
| **Media**      | `play_arrow`, `pause`, `stop`, `skip_next`, `skip_previous`, `volume_up`, `volume_down`, `mic`                                                   |
| **Time**       | `schedule` (clock), `alarm`, `event` (calendar), `timer`                                                                                         |
| **Editor**     | `edit`, `content_copy`, `content_cut`, `content_paste`, `delete`, `save`                                                                         |

## Examples

You can find more JSON templates in the `/examples` directory of this repository:
- `countdown_2030.json`: A simple layout with a text label and a live countdown timer.
- `separators_demo.json`: Showcases both vertical and horizontal separators with different colors.
- `status_dashboard.json`: A system-style dashboard with multiple rows, custom alignment, and labels.
- `crypto_prices.json`: Demonstrates a multi-column table with color-coded trends (green/red).
- `icons_demo.json`: Shows how to use the built-in icon library for data visualization.
- `style_demo.json`: Demonstrates text styling (bold, italic, etc.) and custom font sizes.

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
