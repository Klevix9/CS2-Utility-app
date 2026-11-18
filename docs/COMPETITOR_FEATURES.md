# Competitor Features: CS2 Utility / Lineup Tools

Research limits: direct page fetches were blocked in the research sandbox (csnades.gg, csdb.gg, refrag.gg, wiki.refrag.gg, scope.gg, apps.apple.com did not resolve). Everything below comes from web-search result summaries only. csnades.gg's own feature list could not be confirmed (only a 2024 Reddit post surfaced). Entries are limited to what those summaries state; re-verify before building on any of them.

Effort: S = about a day or less, M = a few days, L = a week or more, for a small Kotlin/Compose Android radar-lineup app.

## Ranked features (top 12)

| # | Feature | Who has it | Why it matters | Effort | Source |
|---|---------|-----------|----------------|--------|--------|
| 1 | Lineups plotted on a 2D radar (throw spot and landing spot) with a video per lineup | CSDB.gg | Radar-first browsing is the fastest way to find a nade by location. | S (radar exists; add video link) | https://csdb.gg/lineups/ |
| 2 | Filter by map, grenade type, position and destination | QuickNades (iOS), Tracker.gg, CSDB.gg | Players search by "what do I need here", not by name. | S | https://apps.apple.com/us/app/quicknades/id6760354679 |
| 3 | Interactive tactical map: hover shows trajectory, click a landing dot to see throw spots, with airtime and a sample video | Scope.gg grenade predictor | Target-first lookup ("I want a smoke here") is the most natural workflow. | M | https://scope.gg/grenade-predictor |
| 4 | Lineup variants "Insta" (fast) vs "Default" (standard map control) | CSMentor (iOS) | Tells the player when to use a nade, not just how. | S (a tag field) | https://apps.apple.com/us/app/csmentor-nade-guide-cs2-csgo/id6473648609 |
| 5 | Throw-type detail per lineup: standing / jump / run throw, left vs right click, aim at a reference point | Common across lineup guides; CSDB guides cover jump-throw binds | Wrong throw type is the main reason a lineup misses. | S (matches this app's from/aim/stance/movement/click fields) | https://csdb.gg/guides/grenade-guide/ |
| 6 | Ready-made practice setup: practice config generator (sv_cheats, trajectory preview, rethrow bind) | CSDB.gg practice-config page; community guides | Lowers the friction of actually practicing; copy-paste commands. | S | https://csdb.gg/practice-config/ |
| 7 | setpos/getpos coordinates per lineup to teleport to the throw spot in offline practice | Community "setpos lineup" lists (via getpos/setpos) | Lets users reach the exact stand position in seconds. | S (store coords, copy-to-clipboard) | https://www.gameserverkings.com/knowledge-base/counter-strike-2/practice-commands/ |
| 8 | In-game preloaded lineups: floating grenade markers, grenade prediction, save up to 500 own grenades | Refrag NADR | The gold standard for practice; user-saved nades add personal value. | L (needs a game server; out of scope for a phone app) | https://wiki.refrag.gg/en/NADR |
| 9 | Grenade Finder and Community Hub grenade groups loadable into Refrag | Refrag | Discovery plus shareable sets (e.g. a full execute). | M (as in-app "sets"/playlists) | https://refrag.gg/blog/grenade-finder-the-fastest-way-to-find-lineups |
| 10 | Save / favorite lineups | QuickNades (iOS) | Personal playbooks and revision lists. | S | https://apps.apple.com/us/app/quicknades/id6760354679 |
| 11 | Per-map guides organized as smokes / flashes / molotovs by side and strategy | CSDB.gg map nade guides; Refrag "essential smokes" posts | Curated starter sets help new users. Content-heavy rather than code-heavy. | S code, L content | https://refrag.gg/blog/essential-smokes-the-most-important-smokes-on-each-map-in-cs2 |
| 12 | Native CS2 practice aids: trajectory trail, picture-in-picture landing preview, rethrow last grenade | CS2 itself via sv_grenade_trajectory_prac_* convars (cheats on) | Sets the baseline users expect; an app should reference, not replace, it. | S (info screen) | https://totalcsgo.com/commands/svgrenadetrajectorypracpipreview |

Notes on claims: CSMentor and Refrag's Utility Hub are described as paid or subscription-gated (CSMentor weekly/monthly PRO plans; Utility Hub for Competitor tier). Per the search summaries, one CSMentor reviewer complained about paying for lineups. Free, ad-light, offline access is therefore a possible gap, but offline support was not confirmed or denied for any competitor.

## Fit for this app

- Already aligned: from / target / aim / stance / movement / click fields cover feature 5 fully, and the radar view covers feature 1. Offline JSON data is a differentiator versus subscription, web-based tools (not verified as absent in competitors).
- Cheapest wins (S): grenade type / map filter chips (2), "insta vs default" tag (4), favorites (10), copy-to-clipboard `setpos` string and a practice-config snippet screen (6, 7), per-lineup video link (1).
- Worth considering (M): target-first lookup, tapping a landing point on the radar to list all throw spots (3), and shareable lineup sets/playlists as JSON files (9).
- Skip: in-game server integration like NADR (8); it needs a game server and is not a phone-app feature.
- Verify before relying on: the setpos coordinates must come from real in-game getpos readings, and convar names should be tested in-game, since sources note older guides cite convars that no longer exist (https://www.gameserverkings.com/knowledge-base/counter-strike-2/practice-commands/).
