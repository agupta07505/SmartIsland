/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

object AppPackageCatalog {

    /** Includes the alternative builds (F-Droid, Huawei) some of these apps ship. */
    val NAVIGATION: Set<String> = setOf(
        // Google
        "com.google.android.apps.maps",
        "com.google.android.apps.mapslite",
        // waze, Citymapper
        "com.waze",
        "com.citymapper.app.release",
        // yandex, 2GIS
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
        "com.ubercab",                 // uber
        "me.lyft.android",             // lyft
        "com.grabtaxi.passenger",      // grab (rides, food, mart)
        "com.gojek.app",               // gojek
        "xyz.be.customer",             // be (Vietnam)
        "com.taxsee.taxsee",           // maxim
        "com.olacabs.customer",        // ola
        "com.rapido.passenger",        // rapido
        "ee.mtakso.client",            // bolt (unverified)
        "ru.yandex.taxi",              // yandex Go (unverified)
        "com.careem.acma",             // careem (unverified)
        "sinet.startup.inDriver",      // inDrive (unverified)
        "in.juspay.nammayatri"         // namma Yatri (unverified)
    )

    val DELIVERY: Set<String> = setOf(
        "com.application.zomato",      // zomato
        "in.swiggy.android",           // swiggy & Instamart
        "com.grofers.customerapp",     // blinkit
        "com.zepto.consumer",          // zepto
        "com.dunzo.user",              // dunzo
        "com.Dominos",                 // domino's
        "com.ubercab.eats",            // uber Eats
        "com.dd.doordash",             // DoorDash
        "com.postmates.android",       // postmates
        "com.deliveroo.orderapp",      // deliveroo
        "com.glovo",                   // glovo
        "com.wolt.android",            // wolt
        "com.grubhub.android",         // grubhub (unverified)
        "com.global.foodpanda.android", // foodpanda (unverified)
        "com.talabat",                 // talabat (unverified)
        "ru.yandex.eda",               // yandex Eats (unverified)
        "com.grability.rappi",         // rappi (unverified)
        "br.com.brainweb.ifood",       // iFood (unverified)
        "com.bigbasket.mobileapp"      // BigBasket (unverified)
    )

    val LIVE_ACTIVITY: Set<String> = RIDE_HAILING + DELIVERY

    fun isNavigationApp(packageName: String): Boolean = packageName in NAVIGATION

    fun isLiveActivityApp(packageName: String): Boolean = packageName in LIVE_ACTIVITY

    /** Brand colour (ARGB), null when the app has none. */
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
