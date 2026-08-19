package com.rogerprod.salonsmena

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// ────────────────────────────────────────────────────────────
//  Data Models
// ────────────────────────────────────────────────────────────

enum class CharacterId { KIRILL, ZHENYA, TANYA, YULIA }
enum class LocationId { MEGAFONCHIK, INTERSVYAZ, BILAN_PLUS, MONETKA, MAGNET_KOSMETIK }
enum class Rank { C, B, A, S }

@Parcelize
data class Character(
    val id: CharacterId,
    val name: String,
    val description: String,
    val speedBonus: Float,      // 0..1
    val persuasionBonus: Float, // 0..1
    val conflictBonus: Float,   // 0..1
    val upsellBonus: Float      // 0..1
) : Parcelable

@Parcelize
data class Location(
    val id: LocationId,
    val name: String,
    val shortName: String,
    val description: String,
    val chapter: Int
) : Parcelable

@Parcelize
data class LevelDef(
    val id: Int,
    val chapter: Int,
    val title: String,
    val intro: String,
    val locationId: LocationId,
    val targetRevenue: Int,
    val targetRating: Float,
    val clientCount: Int,
    val timeLimitSec: Int,
    val specialCondition: String
) : Parcelable

data class Customer(
    val id: Int,
    val name: String,
    val need: CustomerNeed,
    val patience: Float,     // 1.0 = full, 0.0 = left
    val budget: Int,
    val mood: CustomerMood,
    val isSecretBuyer: Boolean = false
)

enum class CustomerNeed {
    BUY_PHONE, BUY_TARIFF, BUY_ACCESSORY, INSTALLMENT, DATA_TRANSFER,
    BUY_PRODUCT, LOYALTY_CARD, RETURN, COSMETICS, GIFT_SET,
    COMPLAINT, PRICE_CHECK
}

enum class CustomerMood { HAPPY, NEUTRAL, ANNOYED, ANGRY, CONFUSED }

data class LevelResult(
    val levelId: Int,
    val characterId: CharacterId,
    val locationId: LocationId,
    val score: Int,
    val revenue: Int,
    val rating: Float,
    val rank: Rank,
    val combo: Int,
    val timeSec: Int
)

// ────────────────────────────────────────────────────────────
//  Game Data Registry
// ────────────────────────────────────────────────────────────

object GameData {

    val characters = listOf(
        Character(
            CharacterId.KIRILL, "Кирилл",
            "Пухленький кудрявый продавец. Харизматик, мастер убеждения и апсейла. Медленнее ходит, но дожимает любого клиента.",
            speedBonus = 0.3f, persuasionBonus = 0.9f, conflictBonus = 0.7f, upsellBonus = 1.0f
        ),
        Character(
            CharacterId.ZHENYA, "Женя",
            "Маленький быстрый продавец. Ловкий и шустрый — обслуживает очередь молниеносно. Чуть хуже убеждает.",
            speedBonus = 1.0f, persuasionBonus = 0.5f, conflictBonus = 0.6f, upsellBonus = 0.6f
        ),
        Character(
            CharacterId.TANYA, "Таня",
            "Кассирша «Монетки». Спокойная, собранная, мастер разрешения конфликтов и работы с очередью.",
            speedBonus = 0.7f, persuasionBonus = 0.6f, conflictBonus = 1.0f, upsellBonus = 0.7f
        ),
        Character(
            CharacterId.YULIA, "Юля",
            "Консультант «Магнит Косметик». Стильная и общительная, отлично продаёт косметику и акции 2+1.",
            speedBonus = 0.6f, persuasionBonus = 0.8f, conflictBonus = 0.7f, upsellBonus = 0.9f
        )
    )

    val locations = listOf(
        Location(LocationId.MEGAFONCHIK,    "МегаФончик",         "МФ",  "Бодрый салон связи на первом этаже ТЦ",            chapter = 1),
        Location(LocationId.INTERSVYAZ,     "ИнтерСвязь",         "ИС",  "Серьёзный салон с корпоративными клиентами",        chapter = 2),
        Location(LocationId.BILAN_PLUS,     "БиЛан+",             "БЛ",  "Крупный салон с планом и тайными покупателями",     chapter = 3),
        Location(LocationId.MONETKA,        "Монетка",            "МН",  "Уютный продуктовый магазин у дома",                 chapter = 4),
        Location(LocationId.MAGNET_KOSMETIK,"Магнит Косметик",    "МК",  "Магазин косметики и бытовой химии",                 chapter = 5)
    )

    val levels: List<LevelDef> = buildLevels()

    private fun buildLevels(): List<LevelDef> {
        val list = mutableListOf<LevelDef>()
        var id = 1

        // ── ГЛАВА 1: МегаФончик ──────────────────────────────
        list += LevelDef(id++, 1, "Первый рабочий день",
            "Кирилл и Женя впервые выходят на смену. Клиенты вежливые, задача простая — продать хоть что-нибудь.",
            LocationId.MEGAFONCHIK, targetRevenue = 5000, targetRating = 3.5f, clientCount = 6, timeLimitSec = 120,
            specialCondition = "Нет особых условий — просто держись!")

        list += LevelDef(id++, 1, "Скидки и акции",
            "Суббота, народ ищет акции. Предлагай телефоны со скидкой и тарифы с бонусами.",
            LocationId.MEGAFONCHIK, targetRevenue = 8000, targetRating = 3.8f, clientCount = 8, timeLimitSec = 130,
            specialCondition = "Каждая вторая продажа — акционный товар.")

        list += LevelDef(id++, 1, "Сложный клиент",
            "Пришёл дядя Серёжа — он хочет самый дешёвый телефон, но требует лучший. Удачи!",
            LocationId.MEGAFONCHIK, targetRevenue = 9000, targetRating = 4.0f, clientCount = 7, timeLimitSec = 110,
            specialCondition = "Есть один особо сложный конфликтный клиент.")

        list += LevelDef(id++, 1, "Выходной — аншлаг",
            "Воскресенье. Толпа в салоне. Сохраняй рейтинг и не теряй клиентов из очереди.",
            LocationId.MEGAFONCHIK, targetRevenue = 12000, targetRating = 4.0f, clientCount = 12, timeLimitSec = 150,
            specialCondition = "Очередь больше 4 человек — рейтинг падает быстрее.")

        // ── ГЛАВА 2: ИнтерСвязь ─────────────────────────────
        list += LevelDef(id++, 2, "Корпоративный клиент",
            "Приехал менеджер компании — ему нужны 5 симкарт и корпоративный тариф. Не облажайся!",
            LocationId.INTERSVYAZ, targetRevenue = 15000, targetRating = 4.0f, clientCount = 5, timeLimitSec = 120,
            specialCondition = "Корпоративный клиент = двойные очки за сделку.")

        list += LevelDef(id++, 2, "Рассрочка на флагман",
            "Клиенты хотят флагманские телефоны в рассрочку. Объясни условия правильно.",
            LocationId.INTERSVYAZ, targetRevenue = 18000, targetRating = 4.2f, clientCount = 7, timeLimitSec = 130,
            specialCondition = "Каждая рассрочка требует дополнительного шага подтверждения.")

        list += LevelDef(id++, 2, "Перенос данных",
            "Пожилые клиенты меняют телефоны — им нужен перенос контактов и фотографий.",
            LocationId.INTERSVYAZ, targetRevenue = 10000, targetRating = 4.5f, clientCount = 8, timeLimitSec = 140,
            specialCondition = "Пожилые клиенты теряют терпение медленнее, но требуют объяснений.")

        list += LevelDef(id++, 2, "День пенсионера",
            "Скидки для пенсионеров. Зал полон бабушек и дедушек. Будь вежлив и терпелив.",
            LocationId.INTERSVYAZ, targetRevenue = 8000, targetRating = 4.8f, clientCount = 10, timeLimitSec = 160,
            specialCondition = "Рейтинг ×1.5 за каждого довольного пожилого клиента.")

        // ── ГЛАВА 3: БиЛан+ ──────────────────────────────────
        list += LevelDef(id++, 3, "Большой план",
            "Директор повесил план на 30 000 руб. за смену. Паники нет — работаем!",
            LocationId.BILAN_PLUS, targetRevenue = 30000, targetRating = 4.0f, clientCount = 14, timeLimitSec = 180,
            specialCondition = "Невыполнение плана = штраф −500 очков.")

        list += LevelDef(id++, 3, "Тайный покупатель",
            "Где-то в очереди прячется тайный покупатель. Улыбайся и соблюдай стандарты!",
            LocationId.BILAN_PLUS, targetRevenue = 20000, targetRating = 4.5f, clientCount = 10, timeLimitSec = 160,
            specialCondition = "Тайный покупатель оценивает каждое действие. Ошибка = −200 очков.")

        list += LevelDef(id++, 3, "Спорщики",
            "Четыре клиента сегодня с претензиями по ценам и тарифам. Разреши каждый конфликт.",
            LocationId.BILAN_PLUS, targetRevenue = 15000, targetRating = 4.3f, clientCount = 9, timeLimitSec = 150,
            specialCondition = "За каждый разрешённый конфликт +300 бонусных очков.")

        list += LevelDef(id++, 3, "Финал главы: Аудит",
            "Проверка магазина. Надо обслужить всех быстро, без ошибок и с высоким рейтингом.",
            LocationId.BILAN_PLUS, targetRevenue = 35000, targetRating = 4.7f, clientCount = 16, timeLimitSec = 200,
            specialCondition = "Комбо ×2 за серии из 3+ идеальных обслуживаний подряд.")

        // ── ГЛАВА 4: Монетка ─────────────────────────────────
        list += LevelDef(id++, 4, "Касса Тани",
            "Таня выходит на смену. Очередь небольшая, товары обычные — входи в ритм!",
            LocationId.MONETKA, targetRevenue = 6000, targetRating = 3.5f, clientCount = 8, timeLimitSec = 120,
            specialCondition = "Учимся работать с кассой — без спешки.")

        list += LevelDef(id++, 4, "Скидки и карты лояльности",
            "Половина покупателей хочет скидку или карту лояльности. Успей обработать всё.",
            LocationId.MONETKA, targetRevenue = 9000, targetRating = 4.0f, clientCount = 10, timeLimitSec = 130,
            specialCondition = "Карта лояльности = +50 очков бонуса каждому клиенту.")

        list += LevelDef(id++, 4, "Конфликтные покупатели",
            "Пришёл мужчина, уверенный что ценник на полке неверный. И он прав. Разберись!",
            LocationId.MONETKA, targetRevenue = 7000, targetRating = 4.3f, clientCount = 9, timeLimitSec = 140,
            specialCondition = "Мини-игра: проверка ценника на полке до начала разговора.")

        list += LevelDef(id++, 4, "Возвраты и очередь",
            "Понедельник — день возвратов. Очередь большая, половина хочет вернуть товар.",
            LocationId.MONETKA, targetRevenue = 5000, targetRating = 4.5f, clientCount = 12, timeLimitSec = 160,
            specialCondition = "Возврат требует двух шагов. Скорость — ключ к рейтингу.")

        // ── ГЛАВА 5: Магнит Косметик ────────────────────────
        list += LevelDef(id++, 5, "Смена Юли",
            "Юля на рабочем месте. Покупатели ищут кремы и духи — помоги с выбором!",
            LocationId.MAGNET_KOSMETIK, targetRevenue = 7000, targetRating = 3.5f, clientCount = 7, timeLimitSec = 120,
            specialCondition = "Нет особых условий — войди в ритм.")

        list += LevelDef(id++, 5, "Акция 2+1",
            "Сегодня акция — купи два товара, третий бесплатно. Предлагай активно!",
            LocationId.MAGNET_KOSMETIK, targetRevenue = 12000, targetRating = 4.0f, clientCount = 9, timeLimitSec = 130,
            specialCondition = "Акция 2+1: правильное предложение = двойные очки.")

        list += LevelDef(id++, 5, "Подарочные наборы",
            "Пред праздниками — все ищут подарочные наборы. Собери правильный набор для каждого.",
            LocationId.MAGNET_KOSMETIK, targetRevenue = 15000, targetRating = 4.2f, clientCount = 10, timeLimitSec = 140,
            specialCondition = "Мини-игра: выбрать 3 подходящих товара из набора для клиента.")

        list += LevelDef(id++, 5, "Финал: Большая смена",
            "Длинная смена. Возвраты, конфликты, акции, наборы — всё сразу. Это финал!",
            LocationId.MAGNET_KOSMETIK, targetRevenue = 25000, targetRating = 4.6f, clientCount = 16, timeLimitSec = 210,
            specialCondition = "Финальный уровень. Комбо-множитель: до ×4 при серии из 5 без ошибок.")

        return list
    }

    fun getCharacter(id: CharacterId) = characters.first { it.id == id }
    fun getLocation(id: LocationId) = locations.first { it.id == id }
    fun getLevelsForChapter(ch: Int) = levels.filter { it.chapter == ch }
    fun getHomeLocation(charId: CharacterId): LocationId = when (charId) {
        CharacterId.KIRILL  -> LocationId.MEGAFONCHIK
        CharacterId.ZHENYA  -> LocationId.MEGAFONCHIK
        CharacterId.TANYA   -> LocationId.MONETKA
        CharacterId.YULIA   -> LocationId.MAGNET_KOSMETIK
    }

    fun getRank(score: Int, maxScore: Int): Rank {
        val pct = score.toFloat() / maxScore
        return when {
            pct >= 0.9f -> Rank.S
            pct >= 0.75f -> Rank.A
            pct >= 0.55f -> Rank.B
            else -> Rank.C
        }
    }
}
