# Bytesize History

Play Store link: https://play.google.com/store/apps/details?id=laynekm.bytesizehistory

Android app used to about events that have occurred on any given day in history, from ancient to modern times. Have you ever woken up one morning and thought you yourself, “oh boy, I wonder what happened today in the 18th century?” — just launch the app and find out!

## Features
- View facts about historical events that have occurred on any given day of the year
- Each fact has a dropdown list of related Wikipedia articles for further reading
- Filter based on your personal interests
- Includes a light and dark theme
- Receive a daily notification about a historical event that has occurred that day (this can be customized and disabled, of course)

## Technical Details
- Written in Kotlin using Android Studio
- Designed using the MVP (model-view-presenter) architectural pattern in order to separate the UI from presentational logic and facilitate automated testing
  - For example, MainActivity implements MainPresenter’s View interface which specifies methods responsible for updating the UI; MainPresenter determines when these methods should be called but does not actually update the UI itself
- Fetches data from the MediaWiki API and parses it into HistoryItem objects with a type, date, era, description, image, links, etc. which are then loaded into RecyclerViews and displayed to the user
  - The data returned by the MediaWiki API is embedded in markup which is oftentimes messy and inconsistent so a lot of checks need to be included for handling edge cases (see ContentManager)
- Managers are responsible for handling specific subsets of application functionality (see ContentManager, FilterManager, ThemeManager, and NotificationManager)
- Includes a test script which fetches data for every day of the year and uses JUnit to verify content is parsed correctly; in doing so, it creates over 130,000 HistoryItems and verifies they have a valid date, description, and so on (see ContentManagerTest)
