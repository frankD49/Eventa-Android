package com.kosd.eventa.network

import com.kosd.eventa.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

object SupabaseClientProvider {
    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Auth) {
            // Deep link config for email confirmation / OAuth callbacks.
            // Must match the intent-filter in AndroidManifest.xml.
            scheme = "eventa"
            host = "auth-callback"
        }
        install(Postgrest)
        install(Realtime)
    }
}
