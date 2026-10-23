/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

/**
 * Package names of apps the island treats specially, shared by the navigation and live-activity parsers so a delivery or
 * ride app is never read as navigation and a map app never as an order tracker.
 *
 * IDs marked "unverified" are from general knowledge only; a wrong ID never matches, so it cannot misclassify.
 */
object AppPackageCatalog {

    /** Includes the alternative builds (F-Droid, Huawei) some of these apps ship. */
    val NAVIGATION: Set<String> = setOf(
        // Google
        "com.google.android.apps.maps",
        "com.google.android.apps.mapslite",
        // Waze, Citymapper
        "com.waze",
        "com.citymapper.app.release",
        // Yandex, 2GIS
        "ru.yandex.yandexnavi",
        "ru.yandex.yandexmaps",
        "ru.dublgis.dgismobile",
        // Global / offline / OpenStreetMap based
        "com.here.app.maps",
        "com.sygic.aura",
        "com.sygic.truck",
        "com.tomtom.gplay.navapp",
        "com.tomtom.gplay.navapp.gofleet",
        "com.generalmagic.magicearth",
        "com.mapswithme.maps.pro",
        "app.organicmaps",
        "app.organicmaps.web",
        "app.comaps",
        "app.comaps.google",
        "app.comaps.fdroid",
        "app.comaps.huawei",
        "net.osmand",
        "net.osmand.plus",
        "net.osmand.dev",
        "net.osmand.huawei",
        "cz.seznam.mapy",
        "com.alk.copilot.mapviewer",
        "nl.flitsmeister",
        "nl.flitsmeister.flux",
        // Huawei
        "com.huawei.maps.app",
        // China
        "com.autonavi.minimap",
        "com.baidu.BaiduMap",
        "com.tencent.map",
        // Korea
        "com.locnall.KimGiSa",
        "com.nhn.android.nmap",
        "net.daum.android.map",
        "com.skt.tmap.ku",
        // India
        "com.mmi.maps",
        "com.mapmyindia.maps",
        "com.maptls.app"
    )

    val RIDE_HAILING: Set<String> = setOf(
        "com.ubercab",                 // Uber
        "me.lyft.android",             // Lyft
        "com.grabtaxi.passenger",      // Grab (rides, food, mart)
        "com.gojek.app",               // Gojek
        "xyz.be.customer",             // Be (Vietnam)
        "com.taxsee.taxsee",           // Maxim
        "com.olacabs.customer",        // Ola
        "com.rapido.passenger",        // Rapido
        "ee.mtakso.client",            // Bolt (unverified)
        "ru.yandex.taxi",              // Yandex Go (unverified)
        "com.careem.acma",             // Careem (unverified)
        "sinet.startup.inDriver",      // inDrive (unverified)
        "in.juspay.nammayatri"         // Namma Yatri (unverified)
    )

    val DELIVERY: Set<String> = setOf(
        "com.application.zomato",      // Zomato
        "in.swiggy.android",           // Swiggy & Instamart
        "com.grofers.customerapp",     // Blinkit
        "com.zepto.consumer",          // Zepto
        "com.dunzo.user",              // Dunzo
        "com.Dominos",                 // Domino's
        "com.ubercab.eats",            // Uber Eats
        "com.dd.doordash",             // DoorDash
        "com.postmates.android",       // Postmates
        "com.deliveroo.orderapp",      // Deliveroo
        "com.glovo",                   // Glovo
        "com.wolt.android",            // Wolt
        "com.grubhub.android",         // Grubhub (unverified)
        "com.global.foodpanda.android", // foodpanda (unverified)
        "com.talabat",                 // Talabat (unverified)
        "ru.yandex.eda",               // Yandex Eats (unverified)
        "com.grability.rappi",         // Rappi (unverified)
        "br.com.brainweb.ifood",       // iFood (unverified)
        "com.bigbasket.mobileapp"      // BigBasket (unverified)
    )

    val LIVE_ACTIVITY: Set<String> = RIDE_HAILING + DELIVERY

    fun isNavigationApp(packageName: String): Boolean = packageName in NAVIGATION

    fun isLiveActivityApp(packageName: String): Boolean = packageName in LIVE_ACTIVITY

    /** Brand colour (ARGB) for a live-activity app, or null when there is no specific one. */
    fun brandColor(packageName: String?): Long? = when (packageName) {
        "com.application.zomato" -> 0xFFE23744L
        "in.swiggy.android" -> 0xFFFC8019L
        "com.grofers.customerapp" -> 0xFFF7C325L
        "com.zepto.consumer" -> 0xFF7D2EC0L
        "com.ubercab" -> 0xFF38BDF8L
        "com.rapido.passenger" -> 0xFFFACC15L
        "com.olacabs.customer" -> 0xFF84CC16L
        "com.dunzo.user" -> 0xFF00B259L
        "com.Dominos" -> 0xFF006491L
        "com.ubercab.eats" -> 0xFF06C167L
        "com.dd.doordash" -> 0xFFFF3008L
        "me.lyft.android" -> 0xFFFF00BFL
        "com.grabtaxi.passenger" -> 0xFF00B14FL
        "com.gojek.app" -> 0xFF00AA13L
        "ee.mtakso.client" -> 0xFF34D186L
        "com.deliveroo.orderapp" -> 0xFF00CCBCL
        "com.wolt.android" -> 0xFF009DE0L
        "com.glovo" -> 0xFFFFC244L
        "com.grubhub.android" -> 0xFFF63440L
        "com.global.foodpanda.android" -> 0xFFD70F64L
        "com.talabat" -> 0xFFFF5A00L
        "com.careem.acma" -> 0xFF4BA82EL
        "ru.yandex.taxi" -> 0xFFFFCC00L
        "ru.yandex.eda" -> 0xFFFCE000L
        "com.grability.rappi" -> 0xFFFF441FL
        "br.com.brainweb.ifood" -> 0xFFEA1D2CL
        "com.taxsee.taxsee" -> 0xFFFFC600L
        else -> null
    }
}
