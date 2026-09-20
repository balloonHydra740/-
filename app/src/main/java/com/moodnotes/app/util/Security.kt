package com.moodnotes.app.util

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import org.json.JSONArray
import org.json.JSONObject

/** 一条保密问题：明文问题文本 + 答案哈希与盐。 */
data class SecurityQuestion(
    val question: String,
    val answerHash: String,
    val answerSalt: String,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("q", question)
        .put("h", answerHash)
        .put("s", answerSalt)

    companion object {
        fun fromJson(obj: JSONObject): SecurityQuestion =
            SecurityQuestion(obj.getString("q"), obj.getString("h"), obj.getString("s"))
    }
}

/**
 * PIN / 保密问题的哈希工具：SHA-256(salt + 规范化文本)，只存哈希不存原文。
 */
object Security {

    fun randomSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hash(value: String, salt: String): String {
        val normalized = value.trim().lowercase()
        val digest = MessageDigest.getInstance("SHA-256")
        val result = digest.digest((salt + normalized).toByteArray(StandardCharsets.UTF_8))
        return result.joinToString("") { "%02x".format(it) }
    }

    fun verify(value: String, salt: String, expectedHash: String): Boolean =
        hash(value, salt) == expectedHash

    fun questionsToJson(questions: List<SecurityQuestion>): String =
        JSONArray().apply { questions.forEach { put(it.toJson()) } }.toString()

    fun questionsFromJson(json: String): List<SecurityQuestion> = runCatching {
        val array = JSONArray(json)
        (0 until array.length()).map { SecurityQuestion.fromJson(array.getJSONObject(it)) }
    }.getOrDefault(emptyList())
}
