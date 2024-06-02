package com.edisoninnovations.save_money

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest

val supabase = createSupabaseClient(
    supabaseUrl = "https://zqiagwapnqwnfhehhqom.supabase.co",
    supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InpxaWFnd2FwbnF3bmZoZWhocW9tIiwicm9sZSI6ImFub24iLCJpYXQiOjE2OTY5ODE0MjMsImV4cCI6MjAxMjU1NzQyM30.T8D-Cm3ApO12SPlxlg-sxKx9Uh2Qht1zZGwugvDkaX0"
) {
    install(Postgrest)
    install(Auth)

}