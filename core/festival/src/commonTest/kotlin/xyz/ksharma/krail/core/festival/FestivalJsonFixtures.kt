package xyz.ksharma.krail.core.festival

/**
 * The shipped festival JSON, held here rather than inside [FestivalJsonValidationTest] because it
 * is five hundred lines of data and a test class reads better when its first screen is tests.
 *
 * Paste the real config in over this to validate it before a release; nothing in the validation
 * suite cares which JSON it is handed.
 */
internal val prodJsonTest = """
    {
      "confirmedDates": [
        {
          "type": "NEW_YEAR",
          "month": 1,
          "day": 1,
          "emojiList": [
            "🎉"
          ],
          "greeting": "Happy New Year"
        },
        {
          "type": "AUSTRALIA_DAY",
          "month": 1,
          "day": 26,
          "emojiList": [
            "🇦🇺",
            "🎉",
            "🎆"
          ],
          "greeting": "Australia Day"
        },
        {
          "type": "ROSE_DAY",
          "month": 2,
          "day": 7,
          "emojiList": [
            "🌹"
          ],
          "greeting": "Rose Day"
        },
        {
          "type": "PROPOSE_DAY",
          "month": 2,
          "day": 8,
          "emojiList": [
            "💍",
            "💞",
            "💌"
          ],
          "greeting": "Propose Day"
        },
        {
          "type": "CHOCOLATE_DAY",
          "month": 2,
          "day": 9,
          "emojiList": [
            "🍫",
            "💞"
          ],
          "greeting": "Chocolate Day"
        },
        {
          "type": "TEDDY_DAY",
          "month": 2,
          "day": 10,
          "emojiList": [
            "🧸"
          ],
          "greeting": "Teddy Day"
        },
        {
          "type": "PROMISE_DAY",
          "month": 2,
          "day": 11,
          "emojiList": [
            "🤝"
          ],
          "greeting": "Promise Day"
        },
        {
          "type": "HUG_DAY",
          "month": 2,
          "day": 12,
          "emojiList": [
            "🤗"
          ],
          "greeting": "Hug Day"
        },
        {
          "type": "KISS_DAY",
          "month": 2,
          "day": 13,
          "emojiList": [
            "😘"
          ],
          "greeting": "Kiss Day"
        },
        {
          "type": "VALENTINES_DAY",
          "month": 2,
          "day": 14,
          "emojiList": [
            "❤️",
            "🌹"
          ],
          "greeting": "Love is in the air"
        },
        {
          "type": "POKEMON_DAY",
          "month": 2,
          "day": 27,
          "emojiList": [
            "🎮",
            "🧢",
            "⚡️"
          ],
          "greeting": "Gotta Catch 'Em All"
        },
        {
          "type": "WOMENS_DAY",
          "month": 3,
          "day": 8,
          "emojiList": [
            "💜",
            "♀️",
            "👩",
            "👩‍🚀",
            "👩‍🚒",
            "👩‍✈️"
          ],
          "greeting": "International Women's Day"
        },
        {
          "type": "BARBIE_DAY",
          "month": 3,
          "day": 9,
          "emojiList": [
            "👠",
            "💖",
            "👗",
            "🎀"
          ],
          "greeting": "Be anything. Go everywhere. \uD83D\uDC96"
        },
        {
          "type": "MARIO_DAY",
          "month": 3,
          "day": 10,
          "emojiList": [
            "🍄",
            "🎮"
          ],
          "greeting": "It's-a me, Mario! Let's-a go! \uD83C\uDF44"
        },
        {
          "type": "PI_DAY",
          "month": 3,
          "day": 14,
          "emojiList": [
            "🥧",
            "π"
          ],
          "greeting": "Pi Day"
        },
        {
          "type": "ANZAC_DAY",
          "month": 4,
          "day": 25,
          "emojiList": [
            "🌺",
            "🇦🇺",
            "🎖️"
          ],
          "greeting": "Lest we forget"
        },
        {
          "type": "HARRY_POTTER_DAY",
          "month": 5,
          "day": 2,
          "emojiList": [
            "🪄",
            "🧙‍♂️",
            "⚡️"
          ],
          "greeting": "Hop on Platform 9¾ — Magic Awaits!"
        },
        {
          "type": "STAR_WARS_DAY",
          "month": 5,
          "day": 4,
          "emojiList": [
            "🌌",
            "🚀",
            "⚔️",
            "🪐"
          ],
          "greeting": "May the Force Ride With You"
        },
        {
          "type": "NURSES_DAY",
          "month": 5,
          "day": 12,
          "emojiList": [
            "🏥",
            "🩺"
          ],
          "greeting": "Nurses Day"
        },
        {
          "type": "A11Y_DAY",
          "month": 5,
          "day": 16,
          "emojiList": [
            "♿️"
          ],
          "greeting": "Global Accessibility Awareness Day"
        },
        {
          "type": "WORLD_ENVIRONMENT_DAY",
          "month": 6,
          "day": 5,
          "emojiList": [
            "🌍",
            "🌱",
            "🌳"
          ],
          "greeting": "World Environment Day"
        },
        {
          "type": "WORLD_OCEANS_DAY",
          "month": 6,
          "day": 8,
          "emojiList": [
            "🌊",
            "🐬",
            "🐠"
          ],
          "greeting": "World Oceans Day"
        },
        {
          "type": "INTERNATIONAL_YOGA_DAY",
          "month": 6,
          "day": 21,
          "emojiList": [
            "🧘",
            "🧘‍♀️",
            "🧘‍♂️",
            "🕉️"
          ],
          "greeting": "International Yoga Day"
        },
        {
          "type": "WORLD_EMOJI_DAY",
          "month": 7,
          "day": 17,
          "emojiList": [
            "😎",
            "🤩",
            "🔥"
          ],
          "greeting": "World Emoji Day"
        },
        {
          "type": "FRIENDSHIP_DAY",
          "month": 8,
          "day": 3,
          "emojiList": [
            "🤝",
            "💛"
          ],
          "greeting": "Friendship Day"
        },
        {
          "type": "INTERNATIONAL_CAT_DAY",
          "month": 8,
          "day": 8,
          "emojiList": [
            "😸",
            "😻"
          ],
          "greeting": "Purrfect day to ride"
        },
        {
          "type": "INTERNATIONAL_DOG_DAY",
          "month": 8,
          "day": 26,
          "emojiList": [
            "🐶",
            "🐾",
            "🦴"
          ],
          "greeting": "International Dog Day"
        },
        {
          "type": "TEACHERS_DAY",
          "month": 9,
          "day": 5,
          "emojiList": [
            "👩‍🏫",
            "👨‍🏫",
            "📚"
          ],
          "greeting": "Happy Teachers' Day"
        },
        {
          "type": "ENGINEERS_DAY",
          "month": 9,
          "day": 15,
          "emojiList": [
            "⚙️",
            "🔧"
          ],
          "greeting": "Engineers Day"
        },
        {
          "type": "PEACE_DAY",
          "month": 9,
          "day": 21,
          "emojiList": [
            "☮️",
            "✌️"
          ],
          "greeting": "International Peace Day"
        },
        {
          "type": "HOBBIT_DAY",
          "month": 9,
          "day": 22,
          "emojiList": [
            "🧙",
            "🍄",
            "🌋"
          ],
          "greeting": "Happy Hobbit Day"
        },
        {
          "type": "MENTAL_HEALTH_DAY",
          "month": 10,
          "day": 10,
          "emojiList": [
            "🧠"
          ],
          "greeting": "World Mental Health Day"
        },
        {
          "type": "HALLOWEEN",
          "month": 10,
          "day": 31,
          "emojiList": [
            "🎃",
            "👻"
          ],
          "greeting": "Spooktacular vibes only!"
        },
        {
          "type": "REMEMBRANCE_DAY",
          "month": 11,
          "day": 11,
          "emojiList": ["🌺", "🕊️", "🇦🇺"],
          "greeting": "Remembrance Day — Lest we forget"
        },
        {
          "type": "WORLD_KINDNESS_DAY",
          "month": 11,
          "day": 13,
          "emojiList": ["🤝", "💖", "🌍"],
          "greeting": "World Kindness Day"
        },
        {
          "type": "MENS_DAY",
          "month": 11,
          "day": 19,
          "emojiList": [
            "💙",
            "♂️",
            "🚹",
            "👨‍🚒",
            "👨‍🌾",
            "👨‍🚀"
          ],
          "greeting": "International Men's Day"
        },
        {
          "type": "TAYLOR_SWIFT_DAY",
          "month": 12,
          "day": 13,
          "emojiList": [
            "🎤",
            "🎶",
            "🩷"
          ],
          "greeting": "Happy Taylor Swift Day"
        },
        {
          "type": "CHRISTMAS",
          "month": 12,
          "day": 25,
          "emojiList": [
            "🎄",
            "🎅"
          ],
          "greeting": "Merry Christmas"
        },
        {
          "type": "NEW_YEAR_EVE",
          "month": 12,
          "day": 31,
          "emojiList": [
            "🎆"
          ],
          "greeting": "New Year's Eve"
        }
      ],
      "variableDates": [
        {
          "type": "EASTER",
          "startDate": "2026-04-05",
          "endDate": "2026-04-05",
          "emojiList": [
            "🐰",
            "🐣",
            "🥚"
          ],
          "greeting": "Happy Easter"
        },
        {
          "type": "EID",
          "startDate": "2026-03-19",
          "endDate": "2026-03-20",
          "emojiList": [
            "🌙",
            "🕌"
          ],
          "greeting": "Eid Mubarak!"
        },
        {
          "type": "CHINESE_NEW_YEAR",
          "startDate": "2026-02-17",
          "endDate": "2026-03-03",
          "emojiList": [
            "🧧"
          ],
          "greeting": "Chinese New Year"
        },
        {
          "type": "VIVID_SYDNEY",
          "startDate": "2026-05-22",
          "endDate": "2026-06-13",
          "emojiList": [
            "🎆",
            "🌈",
            "🌟",
            "✨"
          ],
          "greeting": "Vivid Sydney"
        },
        {
          "type": "MARDI_GRAS",
          "startDate": "2026-02-13",
          "endDate": "2026-03-01",
          "emojiList": [
            "🏳️‍🌈",
            "🪩",
            "🌈"
          ],
          "greeting": "Happy Mardi Gras"
        },
        {
          "type": "NAIDOC_WEEK",
          "startDate": "2026-07-05",
          "endDate": "2026-07-12",
          "emojiList": [
            "🖤",
            "💛",
            "❤️"
          ],
          "greeting": "NAIDOC Week"
        },
        {
          "type": "HOLI",
          "startDate": "2026-03-04",
          "endDate": "2026-03-04",
          "emojiList": [
            "🌈",
            "🫧",
            "🎈"
          ],
          "greeting": "Happy Holi"
        },
        {
          "type": "MELBOURNE_CUP",
          "startDate": "2026-11-03",
          "endDate": "2026-11-03",
          "emojiList": [
            "🐎",
            "💸"
          ],
          "greeting": "Melbourne Cup Day"
        },
        {
          "type": "RECONCILIATION_WEEK",
          "startDate": "2026-05-27",
          "endDate": "2026-06-03",
          "emojiList": [
            "🪃",
            "🖤",
            "💛",
            "❤️"
          ],
          "greeting": "Reconciliation Week"
        },
        {
          "type": "DIWALI",
          "startDate": "2026-11-08",
          "endDate": "2026-11-08",
          "emojiList": [
            "🪔",
            "🎆"
          ],
          "greeting": "Happy Diwali"
        }
      ]
    }
""".trimIndent()
