package com.learnmanager.sync

import com.learnmanager.model.AppSettings
import com.learnmanager.model.AppState
import com.learnmanager.model.SyncPayload
import com.learnmanager.platform.PlatformHttp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed interface SyncResult {
    data class Success(val message: String, val payload: SyncPayload? = null) : SyncResult
    data class Error(val message: String) : SyncResult
}

@Serializable
private data class RemoteRow(
    @SerialName("user_key") val userKey: String,
    val payload: String,
)

class SupabaseSyncClient(
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun push(state: AppState): SyncResult {
        val settings = state.settings
        val validation = validate(settings)
        if (validation != null) return SyncResult.Error(validation)

        val payload = SyncPayload(
            entries = state.entries,
            defaultReminderMinutes = settings.defaultReminderMinutes,
            updatedAtEpochMillis = state.updatedAtEpochMillis,
        )
        val row = RemoteRow(settings.syncCode.trim(), json.encodeToString(payload))
        val response = runCatching {
            PlatformHttp.request(
                method = "POST",
                url = "${settings.supabaseUrl.trimEnd('/')}/rest/v1/learn_manager_state?on_conflict=user_key",
                headers = headers(settings) + mapOf("Prefer" to "resolution=merge-duplicates,return=minimal"),
                body = json.encodeToString(row),
            )
        }.getOrElse { return SyncResult.Error("Không thể kết nối: ${it.message ?: "lỗi mạng"}") }

        return if (response.statusCode in 200..299) {
            SyncResult.Success("Đã đẩy lịch lên server")
        } else {
            SyncResult.Error("Push thất bại HTTP ${response.statusCode}: ${response.body.take(180)}")
        }
    }

    suspend fun pull(settings: AppSettings): SyncResult {
        val validation = validate(settings)
        if (validation != null) return SyncResult.Error(validation)

        val response = runCatching {
            PlatformHttp.request(
                method = "GET",
                url = buildString {
                    append(settings.supabaseUrl.trimEnd('/'))
                    append("/rest/v1/learn_manager_state?select=user_key,payload&user_key=eq.")
                    append(urlEncode(settings.syncCode.trim()))
                    append("&limit=1")
                },
                headers = headers(settings),
            )
        }.getOrElse { return SyncResult.Error("Không thể kết nối: ${it.message ?: "lỗi mạng"}") }

        if (response.statusCode !in 200..299) {
            return SyncResult.Error("Pull thất bại HTTP ${response.statusCode}: ${response.body.take(180)}")
        }

        val rows = runCatching { json.decodeFromString<List<RemoteRow>>(response.body) }
            .getOrElse { return SyncResult.Error("Server trả dữ liệu không hợp lệ") }
        val row = rows.firstOrNull() ?: return SyncResult.Error("Chưa có dữ liệu đồng bộ trên server")
        val payload = runCatching { json.decodeFromString<SyncPayload>(row.payload) }
            .getOrElse { return SyncResult.Error("Payload đồng bộ không hợp lệ") }

        return SyncResult.Success("Đã tải lịch từ server", payload)
    }

    private fun validate(settings: AppSettings): String? = when {
        !settings.syncEnabled -> "Đồng bộ đang tắt"
        !settings.supabaseUrl.startsWith("https://") -> "Supabase URL phải bắt đầu bằng https://"
        settings.supabaseAnonKey.isBlank() -> "Thiếu Supabase anon key"
        settings.syncCode.isBlank() -> "Thiếu mã đồng bộ"
        else -> null
    }

    private fun headers(settings: AppSettings): Map<String, String> = mapOf(
        "apikey" to settings.supabaseAnonKey.trim(),
        "Authorization" to "Bearer ${settings.supabaseAnonKey.trim()}",
        "Content-Type" to "application/json",
        "Accept" to "application/json",
    )

    private fun urlEncode(value: String): String {
        val bytes = value.encodeToByteArray()
        val safe = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.~"
        return buildString {
            bytes.forEach { byte ->
                val unsigned = byte.toInt() and 0xff
                val char = unsigned.toChar()
                if (char in safe) {
                    append(char)
                } else {
                    append('%')
                    append(unsigned.toString(16).uppercase().padStart(2, '0'))
                }
            }
        }
    }
}
