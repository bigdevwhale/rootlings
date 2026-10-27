# Rootlings content

Everything the app says and knows about plants lives in tab-separated files in
`app/src/main/assets/content/`. One record per line, UTF-8, no header row, no quotes, no tabs inside
fields. Lines starting with `#` are comments. `tools/check_content.py` validates every file.

The app is a Pokémon-like companion for **real** houseplants: each plant on the user's windowsill has
a spirit (a "rootling"). Care advice must be correct and practical: people will water, move and
treat real plants by it. When unsure, be conservative (less water, indirect light).

Both languages are written natively, not translated word by word. Russian uses «ёлочки», em dashes
— with spaces, and the plain informal «ты» of a friendly field guide. English is plain and warm.

## species.tsv

One species (or a commonly sold group) per line. 24 fields:

| # | field | meaning |
|---|-------|---------|
| 1 | id | lowercase latin slug, `a-z0-9_`, unique (e.g. `monstera_deliciosa`) |
| 2 | kind | sprite archetype, one of the kinds below |
| 3 | element | `desert`, `mist`, `rain`, `meadow`, `forest` (see below) |
| 4 | latin | botanical name, e.g. `Monstera deliciosa` |
| 5 | en | common English name, e.g. `Swiss cheese plant` |
| 6 | ru | common Russian name, e.g. `Монстера деликатесная` (the name people actually say) |
| 7 | dli_min | daily light integral it needs to grow well, mol/m²/day, number (e.g. `3`) |
| 8 | dli_max | above this it risks scorch / stress, mol/m²/day (e.g. `10`) |
| 9 | dry | percent of pot moisture lost on an ordinary indoor day in a ~15 cm pot, 2..15 (cactus ≈ 3, fern ≈ 12) |
| 10 | water_at | water when the moisture estimate falls to this percent: 10 (let dry out fully), 25 (top half dry), 40 (top 2–3 cm dry), 55 (keep evenly moist) |
| 11 | humid | `1` if it suffers in dry heated air (wants ≥ 50 % humidity), else `0` |
| 12 | toxic | `1` if toxic to cats or dogs (ASPCA), else `0` |
| 13 | rest | `1` if it needs a cool, dry winter rest (most cacti, many succulents, some bulbs), else `0` |
| 14 | bloom | flower colour as `#RRGGBB` if it flowers indoors in a noticeable way, else `-` |
| 15 | hue | HSL hue 0..360 of its typical leaf colour (green ≈ 90..150, blue-grey succulents ≈ 160..190, purple ≈ 290) |
| 16 | sat | HSL saturation 10..70 |
| 17 | lit | HSL lightness 28..62 |
| 18 | s2 | count of new leaves (or new pads/heads/fronds/stems for that kind) since it was caught, needed for its 2nd form |
| 19 | s3 | count needed for its 3rd form (bigger than s2; slow growers get small numbers: cactus 2/5, monstera 4/10, spider plant 8/20) |
| 20 | cutting | how it is propagated at home: `water`, `soil`, `leaf`, `division`, `offset`, `seed`, `none` |
| 21 | root_days | typical days until a cutting/offset shows roots (e.g. 14), `0` if cutting is `none` or `seed` |
| 22 | forms | three form names, comma-separated, EN: short, evocative, capitalised words (`Sprout,Carver,Giantess`) |
| 23 | forms_ru | three form names, RU, comma-separated, each 1–2 words, ≤ 12 letters (`Росток,Резная,Великанша`) |
| 24 | tip_en, tip_ru | **two fields**: one practical care tip each, ≤ 140 characters, not obvious, specific to this plant |

So a line has 25 tab-separated values in total (tip_en and tip_ru are fields 24 and 25).

### Kinds (sprite archetypes)

`monstera` big split leaves · `philo` heart-shaped leaves on vines (pothos, philodendron) ·
`ficus` few large oval leaves on a stem · `tree` small-leaved bush/tree (benjamina, jade, citrus) ·
`grass` long arching blades (spider plant, cyperus) · `rosette` fleshy succulent rosette ·
`aloe` spiky upright rosette (aloe, haworthia, agave) · `paddle` flat pads (opuntia, schlumbergera) ·
`column` columnar cactus/euphorbia · `ball` round cactus, lithops · `fern` fronds ·
`pattern` patterned oval leaves (calathea, maranta) · `blade` stiff upright swords (sansevieria) ·
`zz` glossy pinnate stalks · `palm` arching palm fronds · `cane` trunk with a tuft on top (dracaena, yucca) ·
`orchid` broad leaves + flower spike · `spathe` glossy leaves + spathe flower (peace lily, anthurium) ·
`violet` low rosette with flowers (saintpaulia, cyclamen) · `bush` soft leafy bush (begonia, coleus) ·
`coin` round coin leaves (pilea, peperomia) · `trail` trailing strands (hoya, string of pearls, ivy) ·
`herb` kitchen herbs · `bulb` strap leaves + tall flower (amaryllis, hyacinth, clivia) ·
`carnivore` traps and pitchers.

### Elements (the spirit's type)

`desert` — cacti and succulents, sun and drought · `mist` — ferns, calatheas, humidity lovers ·
`rain` — big tropical aroids and jungle plants · `meadow` — herbs, grasses, flowering softies ·
`forest` — trees, ficuses, palms, sturdy woody plants.

## pests.tsv

The enemies of battles. Real problems a plant can have: insects **and** care troubles (root rot from
overwatering, sunburn, etiolation from darkness). 12 fields:

`id` · `sprite` (one of `mite`, `scale`, `thrips`, `mealy`, `gnat`, `aphid`, `whitefly`, `rot`, `burn`,
`mildew`, `dark`, `springtail`) · `en` · `ru` (what it really is: «Паутинный клещ») · `nick_en` · `nick_ru`
(the enemy's name in caps style of an old handheld game, one word ≤ 10 letters: `WEBBER`, `СЕТЧАТИК`) ·
`latin` (or `-`) · `symptoms` (comma-separated codes from symptoms.tsv that point to it) · `likes`
(`dry`, `wet`, `warm`, `dark`, `sun`, `-`: what conditions invite it) · `level` (2..9, how hard it is) ·
`about_en` · `about_ru` (one or two sentences: how to recognise it, ≤ 200 chars).

## moves.tsv

The battle moves for each pest: the real treatment, in order. 9 fields:

`pest` (id) · `n` (1, 2, 3…, order) · `day` (days after the battle starts when this move becomes available:
0 for today, 7 for "repeat in a week"…) · `en` · `ru` (the move name in CAPS, ≤ 12 letters: `SHOWER`, `ДУШ`) ·
`how_en` · `how_ru` (what to actually do, concrete, ≤ 160 chars; name active-ingredient classes, not
brands) · `hit_en` · `hit_ru` (the battle message after the move, in the style of an old handheld game,
≤ 90 chars, using `{plant}` for the plant's name and `{foe}` for the enemy's nickname:
«{plant} принимает тёплый ДУШ! Это очень эффективно!»).

Every pest has 3–6 moves; the last one is a check-up a week or two later ("no webs, no specks"). Moves
must be safe for the plant and the household (pets, children): prefer mechanical and mild methods
first; if a systemic insecticide is the honest answer for scale or thrips, say so with a care note.

## symptoms.tsv

What the user can see. 3 fields: `code` (`a-z_`) · `en` · `ru` (short, what you see: «паутинка на листьях»).
About 16 codes covering all pests.

## lines.tsv

What a rootling says on its card. 4 fields: `mood` · `element` (`desert`…, or `*` for any) · `en` · `ru`.

Moods: `calm` (all fine), `sun` (basking in direct sun now), `gloom` (too little light today), `dry`
(soil probably dry, asks for a finger check — never asks for water directly), `soggy` (soil still wet,
asks to be left alone), `night` (asleep, growing roots), `rest` (winter dormancy: cool, dry, don't
water), `pest` (being bothered by a pest), `heat` (heated-air dryness bothers it), `scorch` (too much
hot sun through glass), `new` (just caught), `evolve` (has just changed form), `hatch` (a cutting
has just rooted).

Lines are the spirit speaking in the first person, ≤ 110 characters, gentle, a little funny, never
guilt-tripping, sometimes with a true plant fact. Each element has its own temper: desert is laconic
and proud, mist is dreamy and a bit dramatic, rain is lush and greedy for light, meadow is chatty and
cheerful, forest is calm and old. At least 6 lines for every (mood × element) pair, plus `*` lines for
`new`, `evolve`, `hatch`.
