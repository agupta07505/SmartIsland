/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

/**
 * Navigation vocabulary beyond English, so a Yandex Navigator notification in Russian, an Amap one in Chinese or a Kakao one in
 * Korean still gives a distance, a turn direction and a "this is guidance" signal.
 *
 * Covered: English, Russian/Ukrainian, Spanish, Portuguese, French, German, Italian, Dutch, Turkish, Polish, Indonesian,
 * Vietnamese, Chinese, Japanese, Korean, Arabic and Hindi. Phrases are lower case. Latin and Cyrillic phrases only match as whole
 * words ("turn" does not match "return"); the other scripts attach particles to words, so they match anywhere.
 */
internal object NavigationLocale {

    private const val LETTER_OR_DIGIT = "\\p{L}\\p{N}"
    private const val LATIN_CYRILLIC_OR_DIGIT = "\\p{IsLatin}\\p{IsCyrillic}\\p{N}"

    private fun isBoundarySafe(phrase: String): Boolean = phrase.none { ch ->
        val block = Character.UnicodeBlock.of(ch)
        block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
            block == Character.UnicodeBlock.HIRAGANA ||
            block == Character.UnicodeBlock.KATAKANA ||
            block == Character.UnicodeBlock.HANGUL_SYLLABLES ||
            block == Character.UnicodeBlock.ARABIC ||
            block == Character.UnicodeBlock.DEVANAGARI ||
            block == Character.UnicodeBlock.THAI
    }

    /** One alternation for [phrases]: longest first, whole-word where the script allows it. */
    fun anyOf(phrases: List<String>): Regex {
        val parts = phrases.sortedByDescending { it.length }.map { phrase ->
            val quoted = Regex.escape(phrase)
            if (isBoundarySafe(phrase)) "(?<![$LETTER_OR_DIGIT])$quoted(?![$LETTER_OR_DIGIT])" else quoted
        }
        return Regex(parts.joinToString("|"), setOf(RegexOption.IGNORE_CASE))
    }


    private val METRIC_AND_IMPERIAL_UNITS = listOf(
        // English and the Latin-script languages that share the symbols
        "m", "km", "ft", "mi", "yd", "yds", "mile", "miles", "meter", "meters", "metre", "metres", "kilometer", "kilometers",
        "kilometre", "kilometres", "foot", "feet", "yard", "yards",
        // Spanish, Portuguese, Italian, French, German, Dutch, Turkish, Polish, Indonesian
        "metro", "metros", "metri", "mètre", "mètres", "kilómetro", "kilómetros", "quilómetro", "quilómetros", "chilometro",
        "chilometri", "kilomètre", "kilomètres", "kilometer", "kilometern", "meter", "metern", "kilometre", "kilometrů", "metrów",
        "metr", "metry", "kilometr", "kilometry", "kilometrów", "milha", "milhas", "milla", "millas", "miglio", "miglia",
        "pies", "pés", "pied", "pieds", "füße", "fuß",
        // Vietnamese
        "mét", "ki-lô-mét", "cây số",
        // Russian / Ukrainian
        "м", "км", "метр", "метра", "метров", "метри", "метрів", "километр", "километра", "километров", "кілометр", "кілометри",
        "кілометрів", "фут", "футов", "миля", "мили", "миль",
        // Chinese
        "米", "公里", "千米", "英里", "英尺", "码",
        // Japanese
        "メートル", "キロメートル", "キロ", "マイル", "ヤード", "フィート",
        // Korean
        "미터", "킬로미터", "킬로", "마일", "피트",
        // Arabic
        "م", "متر", "أمتار", "كم", "كيلومتر", "كيلومترات", "ميل", "أميال",
        // Hindi
        "मीटर", "किमी", "किलोमीटर", "मील"
    ).distinct().sortedByDescending { it.length }

    // A Latin or Cyrillic unit must end the word ("24 min" is not "24 m"); CJK, Arabic and Devanagari units run into
    // the next word ("300米后左转").
    private val UNIT_ALTERNATION = METRIC_AND_IMPERIAL_UNITS.joinToString("|") { unit ->
        if (isBoundarySafe(unit)) Regex.escape(unit) + "(?![$LATIN_CYRILLIC_OR_DIGIT])" else Regex.escape(unit)
    }

    // A decimal comma ("1,2 km") is as common as a decimal point; \p{Nd} also takes Arabic-Indic and Devanagari digits.
    private const val NUMBER = "\\p{Nd}+(?:[.,٫]\\p{Nd}+)?"
    private val unitOptions = setOf(RegexOption.IGNORE_CASE)

    /** A distance at the very start of the title ("In 300 m"-style titles are handled by the caller; this takes "300 m ..."). */
    val LEADING_DISTANCE = Regex("^($NUMBER)\\s*(?:$UNIT_ALTERNATION)", unitOptions)

    /** A distance anywhere in the text. */
    val ANY_DISTANCE = Regex("($NUMBER)\\s*(?:$UNIT_ALTERNATION)", unitOptions)

    /** "In 300 m", "через 300 м", "dans 300 m" ... at the start of a title, with the separator after it. */
    val LEADING_DISTANCE_PREFIX = Regex(
        "^\\s*(?:(?:in|dans|en|em|over|через|за|in circa|tra|binnen|za|dalam|trong)\\s+)?$NUMBER\\s*(?:$UNIT_ALTERNATION)\\s*[-\u2013\u2014:\u2022\u00B7,]?\\s*",
        unitOptions
    )

    /** A whole segment that is a trip distance ("8.5 km", "5 мили", "12公里") rather than the distance to the next turn. */
    val TRIP_DISTANCE = Regex("^$NUMBER\\s*(?:km|mi|miles?|км|кілометрів|公里|千米|英里|킬로미터|마일|كم|किमी|मील)$", unitOptions)


    val U_TURN = listOf(
        "u-turn", "uturn", "u turn", "make a u turn", "разворот", "развернитесь", "развороте", "розворот", "demi-tour", "faites demi-tour",
        "wenden", "wende", "media vuelta", "cambio de sentido", "retorno", "faça o retorno", "inversione", "inversione a u", "keer om",
        "u dönüşü", "zawróć", "putar balik", "quay đầu", "掉头", "调头", "掉頭", "uターン", "ユーターン", "유턴", "انعطاف للخلف", "استدر",
        "यू-टर्न"
    )

    val SLIGHT_LEFT = listOf(
        "slight left", "bear left", "keep left", "veer left", "держитесь левее", "левее", "плавно налево", "плавно влево", "слегка налево",
        "légèrement à gauche", "gardez la gauche", "leicht links", "links halten", "ligeramente a la izquierda", "manténgase a la izquierda",
        "levemente à esquerda", "mantenha-se à esquerda", "leggermente a sinistra", "mantieni la sinistra", "licht links", "houd links",
        "hafif sola", "łagodnie w lewo", "utrzymaj lewy", "tetap di kiri", "sedikit ke kiri", "đi chếch sang trái", "靠左", "稍向左", "向左前方",
        "左前方", "左斜", "斜め左", "左方向", "왼쪽 방향", "좌측 방향", "قليلا لليسار", "बाईं ओर बने रहें"
    )

    val SLIGHT_RIGHT = listOf(
        "slight right", "bear right", "keep right", "veer right", "держитесь правее", "правее", "плавно направо", "плавно вправо", "слегка направо",
        "légèrement à droite", "gardez la droite", "leicht rechts", "rechts halten", "ligeramente a la derecha", "manténgase a la derecha",
        "levemente à direita", "mantenha-se à direita", "leggermente a destra", "mantieni la destra", "licht rechts", "houd rechts",
        "hafif sağa", "łagodnie w prawo", "utrzymaj prawy", "tetap di kanan", "sedikit ke kanan", "đi chếch sang phải", "靠右", "稍向右", "向右前方",
        "右前方", "右斜", "斜め右", "右方向", "오른쪽 방향", "우측 방향", "قليلا لليمين", "दाईं ओर बने रहें"
    )

    val LEFT = listOf(
        "turn left", "left onto", "left on", "take the left", "sharp left", "go left", "head left", "поверните налево", "поворот налево", "налево",
        "влево", "поверніть ліворуч", "ліворуч", "tourner à gauche", "tournez à gauche", "à gauche", "links abbiegen", "biegen sie links ab", "nach links",
        "gire a la izquierda", "gira a la izquierda", "a la izquierda", "vire à esquerda", "vira à esquerda", "à esquerda", "svolta a sinistra",
        "gira a sinistra", "a sinistra", "sla linksaf", "ga linksaf", "linksaf", "sola dön", "sola", "skręć w lewo", "w lewo", "belok kiri", "ke kiri",
        "rẽ trái", "sang trái", "左转", "左轉", "向左转", "向左轉", "左折", "左に曲がります", "좌회전", "왼쪽으로", "انعطف يسارا", "انعطف يسارًا", "اتجه يسارا",
        "يسارا", "يسارًا", "बाएं मुड़ें", "बाएं"
    )

    val RIGHT = listOf(
        "turn right", "right onto", "right on", "take the right", "sharp right", "go right", "head right", "поверните направо", "поворот направо",
        "направо", "вправо", "поверніть праворуч", "праворуч", "tourner à droite", "tournez à droite", "à droite", "rechts abbiegen",
        "biegen sie rechts ab", "nach rechts", "gire a la derecha", "gira a la derecha", "a la derecha", "vire à direita", "vira à direita", "à direita",
        "svolta a destra", "gira a destra", "a destra", "sla rechtsaf", "ga rechtsaf", "rechtsaf", "sağa dön", "sağa", "skręć w prawo", "w prawo",
        "belok kanan", "ke kanan", "rẽ phải", "sang phải", "右转", "右轉", "向右转", "向右轉", "右折", "右に曲がります", "우회전", "오른쪽으로", "انعطف يمينا",
        "انعطف يمينًا", "اتجه يمينا", "يمينا", "يمينًا", "दाएं मुड़ें", "दाएं"
    )

    val ROUNDABOUT = listOf(
        "roundabout", "rotary", "traffic circle", "кольцо", "кольцевой", "кольцевая", "круговое движение", "круговой", "rond-point", "rond point",
        "kreisverkehr", "kreisel", "rotonda", "glorieta", "rotatória", "rotunda", "rotonde", "rondweg", "döner kavşak", "rondo", "bundaran",
        "vòng xuyến", "vòng xoay", "环岛", "環島", "ロータリー", "環状交差点", "로터리", "회전교차로", "دوار", "गोल चक्कर"
    )

    val DESTINATION = listOf(
        "arrived", "you have arrived", "destination", "reached", "прибыли", "вы прибыли", "пункт назначения", "конечная точка", "вы приехали",
        "vous êtes arrivé", "vous êtes arrivée", "sie haben ihr ziel erreicht", "ziel erreicht", "ha llegado a su destino", "ha llegado",
        "has llegado", "chegou ao destino", "você chegou", "chegou", "sei arrivato", "sei arrivata", "sei arrivati", "hai raggiunto la destinazione",
        "arrivato", "bestemming bereikt", "u bent aangekomen", "hedefe ulaştınız", "hedefe vardınız", "dotarłeś do celu", "dotarłeś na miejsce",
        "anda telah tiba", "telah sampai", "bạn đã đến nơi", "đã đến nơi", "已到达", "已到達", "到达目的地", "到達目的地", "目的地に到着", "目的地周辺",
        "목적지에 도착", "도착했습니다", "وصلت", "لقد وصلت", "आप पहुंच गए", "गंतव्य"
    )

    /** Other guidance wording that is not a turn: straight on, exit, merge. */
    val OTHER_MANEUVER = listOf(
        "continue", "head", "exit", "merge", "ramp", "fork", "onto", "towards", "proceed", "take the", "straight",
        "продолжайте", "прямо", "съезд", "выезд", "двигайтесь", "продовжуйте",
        "continuez", "tout droit", "sortie", "prenez", "geradeaus", "ausfahrt", "folgen sie", "continúe", "siga recto", "todo recto", "salida",
        "continue em frente", "siga em frente", "saída", "continua dritto", "dritto", "uscita", "ga rechtdoor", "rechtdoor", "afslag",
        "düz devam", "devam edin", "çıkış", "jedź prosto", "zjazd", "lurus", "tiếp tục", "đi thẳng", "直行", "沿", "出口", "驶入", "进入",
        "直進", "道なり", "직진", "진입", "استمر", "مخرج", "सीधे", "निकास"
    )

    /** True when the text reads as turn-by-turn wording in any covered language. */
    val MANEUVER_WORDING: Regex = anyOf(
        U_TURN + SLIGHT_LEFT + SLIGHT_RIGHT + LEFT + RIGHT + ROUNDABOUT + DESTINATION + OTHER_MANEUVER
    )


    val TRIP_STAT: Regex = Regex(
        "(?<![$LETTER_OR_DIGIT])\\p{Nd}+(?:[.,]\\p{Nd}+)?\\s*(?:min|mins|minutes?|minuti|minutos?|minuten|minuut|minuta|minut|menit|phút|dakika|dk|мин|хв|分钟|分鐘|分|분|دقيقة|دقائق|मिनट|h|hr|hrs|hours?|std|stunden?|heures?|horas?|ore|uur|saat|godz|jam|giờ|ч|час|часа|часов|год|小时|時間|시간|ساعة|ساعات|घंटे?)(?![$LETTER_OR_DIGIT])" +
            "|(?<![$LETTER_OR_DIGIT])eta(?![$LETTER_OR_DIGIT])|arriv|(?<![$LETTER_OR_DIGIT])remaining(?![$LETTER_OR_DIGIT])|(?<![\\p{Nd}])\\d{1,2}:\\d{2}(?![\\p{Nd}])" +
            "|прибыти|в пути|осталось|останется|剩余|剩餘|预计|預計|到着|残り|남음|도착|المتبقي|الوصول|शेष|पहुंच",
        setOf(RegexOption.IGNORE_CASE)
    )

    // Delivery and ride wording: a notification with these is about an order or a ride, not about guidance.

    val DELIVERY_OR_RIDE_WORDING: Regex = anyOf(
        listOf(
            "order", "your order", "delivery", "delivering", "courier", "rider", "driver", "captain", "pickup", "pick-up", "pick up",
            "restaurant", "food", "grocery", "parcel", "package", "cab", "taxi", "ride", "trip is", "fare",
            "заказ", "курьер", "доставка", "доставку", "водитель", "такси", "поездка", "ресторан",
            "pedido", "repartidor", "conductor", "entrega", "motorista", "entregador", "viagem", "corrida",
            "commande", "livreur", "livraison", "chauffeur", "course",
            "bestellung", "lieferung", "fahrer", "kurier", "fahrt",
            "ordine", "consegna", "fattorino", "autista", "corsa",
            "bestelling", "bezorger", "bezorging", "chauffeur",
            "sipariş", "kurye", "teslimat", "sürücü", "yolculuk",
            "zamówienie", "dostawa", "kierowca", "przejazd",
            "pesanan", "pengiriman", "kurir", "pengemudi", "đơn hàng", "tài xế", "giao hàng",
            "订单", "訂單", "外卖", "骑手", "配送", "司机", "打车", "出租车", "注文", "配達", "ドライバー", "배달", "주문", "기사님",
            "طلب", "توصيل", "سائق", "رحلة", "ऑर्डर", "डिलीवरी", "ड्राइवर"
        )
    )
}
