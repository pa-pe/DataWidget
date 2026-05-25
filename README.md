# Data Widget

An Android application that allows you to display dynamic data from remote JSON sources directly on your home screen. Designed for high-frequency updates and extreme customization.

## Key Features

- **High Frequency Updates**: Powered by a Foreground Service, capable of updating every second.
- **Dynamic Layouts**: Build your widget UI using a simple Bootstrap-like grid system defined in JSON.
- **Multiple Widgets**: Add multiple widgets, each pointing to a different data source.
- **Live Counters**: Built-in support for countdown timers that update in real-time.
- **Visual Customization**:
    - **Cloud side**: Control text colors, alignment, and add icons via JSON.
    - **Local side**: Users can customize background, transparency, radius, and **base font size**.
- **HTTP Request Control**: Choose between **GET** and **POST** methods for fetching data.
- **Developer Tools**: Built-in "Test" feature to validate your JSON connection and structure.

## JSON Schema Documentation

The widget is rendered based on a JSON object fetched from your URL.

### Root Parameters

| Parameter             | Type    | Description                                                       |
|:----------------------|:--------|:------------------------------------------------------------------|
| `update_interval_sec` | Integer | (Optional) How often to fetch new data (in seconds). Default: 60. |
| `next_update_at`      | Long    | (Optional) Exact Unix timestamp for the next fetch.               |
| `buttons`             | Array   | (Optional) Custom buttons to show when tapped.                    |
| `rows`                | Array   | **Required**. A list of row objects.                              |

### Column/Cell Object

| Property         | Type    | Description                                                                |
|:-----------------|:--------|:---------------------------------------------------------------------------|
| `text`           | String  | Text to display.                                                           |
| `weight`         | String  | Width: `"auto"` (default), `"12"` (full), `"6"` (half), etc.               |
| `color`          | String  | HEX or web names (e.g., `green`, `red`).                                   |
| `size`           | String  | Text size: absolute (`"14"`) or relative (`"+2"`, `"-1"`).                 |
| `icon`           | String  | (Optional) Icon name. See **[Available Icons](#available-icons)** section. |
| `bold`, `italic` | Boolean | (Optional) Text styling flags.                                             |
| `align`          | String  | `left`, `center`, `right`.                                                 |

## Available Icons

Data Widget includes **hundreds of built-in Material Design icons**. You can use any icon name from the official Google Symbols catalog.

| Category       | Icon Names                                                                                                                                 |
|:---------------|:-------------------------------------------------------------------------------------------------------------------------------------------|
| **Status**     | `check`, `error`, `info`, `warning`, `help`, `notifications`, `lock`, `lock_open`, `new_releases`, `priority_high`                         |
| **Common**     | `person`, `group`, `favorite`, `favorite_border`, `star`, `star_border`, `star_half`, `share`, `public`, `celebration`, `push_pin`, `bolt` |
| **Touch**      | `touch_app`, `ads_click`                                                                                                                   |
| **Finance**    | `wallet`, `attach_money`, `trending_up`, `trending_down`, `account_balance`, `credit_card`, `shopping_cart`                                |
| **Weather**    | `sunny`, `cloud`, `rainy`, `thunderstorm`, `ac_unit`, `thermostat`, `water_drop`                                                           |
| **Devices**    | `smartphone`, `laptop`, `desktop_windows`, `watch`, `router`, `power`, `battery_full`, `wifi`, `bluetooth`                                 |
| **Navigation** | `home`, `search`, `menu`, `arrow_back`, `arrow_forward`, `location_on`, `directions_car`, `flight`                                         |
| **Media**      | `play_arrow`, `pause`, `stop`, `skip_next`, `skip_previous`, `volume_up`, `volume_down`, `mic`                                             |
| **Time**       | `schedule`, `alarm`, `event`, `timer`                                                                                                      |
| **Editor**     | `edit`, `content_copy`, `content_cut`, `content_paste`, `delete`, `save`                                                                   |

> 🔗 Browse the full list at **[Google Fonts Icons](https://fonts.google.com/icons)**. Use the "name" of the icon in your JSON.

## Battery Optimization & Privacy

By default, the **"Update only when screen is on"** setting is enabled to save battery. Updates pause when the screen is off and resume immediately when turned on.

## Setup

1. Host your JSON file (e.g., on GitHub Gist).
2. Install the app and add a "Data Widget" to your home screen.
3. In the setup screen, enter your JSON URL.
4. Tap the widget to reveal control buttons.

## Development

The project is built with Kotlin and uses a Foreground Service with `specialUse` type to maintain persistent updates. Layouts are rendered using `RemoteViews`.
