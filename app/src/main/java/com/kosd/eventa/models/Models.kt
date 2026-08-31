package com.kosd.eventa.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

// ─── Enums ───────────────────────────────────────────────────────────────────

@Serializable
enum class UserRole(val value: String) {
    @SerialName("owner")        OWNER("owner"),
    @SerialName("admin")        ADMIN("admin"),
    @SerialName("event_staff")  EVENT_STAFF("event_staff"),
    @SerialName("member")       MEMBER("member");

    val displayName: String
        get() = when (this) {
            OWNER        -> "Owner"
            ADMIN        -> "Admin"
            EVENT_STAFF  -> "Event Staff"
            MEMBER       -> "Member"
        }

    val isAdmin: Boolean get() = this == OWNER || this == ADMIN
    val isOwner: Boolean get() = this == OWNER
    val isEventStaff: Boolean get() = this == OWNER || this == ADMIN || this == EVENT_STAFF
}

enum class Permission(val value: String, val displayName: String) {
    MANAGE_MEMBERS("manage_members", "Add and remove ordinary members"),
    CREATE_INVITES("create_invites", "Add and invite members"),
    VIEW_REPORTS("view_reports", "View organization reports"),
    MANAGE_EVENTS("manage_events", "Manage events");

    companion object {
        fun fromValue(value: String): Permission? = entries.firstOrNull { it.value == value }
    }
}

@Serializable
data class OrganizationMemberPermission(
    @SerialName("organization_id") val organizationId: String,
    @SerialName("member_id") val memberId: String,
    val permission: String
)

@Serializable
enum class PopulationTier(val value: String) {
    // Legacy aliases (`under_20`, `20_50`, `over_500`) are accepted via @JsonNames so
    // rows from the previous schema deserialize without crashing the org load.
    @SerialName("under_10")        @JsonNames("under_20")  UNDER_10("under_10"),
    @SerialName("10_50")           @JsonNames("20_50")     T_10_50("10_50"),
    @SerialName("50_100")          T_50_100("50_100"),
    @SerialName("100_500")         T_100_500("100_500"),
    @SerialName("500_1000")        @JsonNames("over_500")  T_500_1000("500_1000"),
    @SerialName("1000_10000")      T_1K_10K("1000_10000"),
    @SerialName("10000_100000")    T_10K_100K("10000_100000"),
    @SerialName("100000_1000000")  T_100K_1M("100000_1000000"),
    @SerialName("over_1000000")    T_OVER_1M("over_1000000");

    val displayName: String
        get() = when (this) {
            UNDER_10    -> "< 10 (Free)"
            T_10_50     -> "10 – 50"
            T_50_100    -> "50 – 100"
            T_100_500   -> "100 – 500"
            T_500_1000  -> "500 – 1,000"
            T_1K_10K    -> "1,000 – 10,000"
            T_10K_100K  -> "10,000 – 100,000"
            T_100K_1M   -> "100,000 – 1,000,000"
            T_OVER_1M   -> "> 1,000,000"
        }

    /** True for the free tier; paid tiers require an in-app purchase to activate. */
    val isFree: Boolean get() = this == UNDER_10

    companion object {
        val FREE: PopulationTier = UNDER_10
    }
}

// ─── Invite Signup Response (from Edge Function) ─────────────────────────────

@Serializable
data class InviteSignupResponse(
    val success: Boolean = false,
    val requiresSignIn: Boolean = false,
    val accessToken: String? = null,
    val refreshToken: String? = null
)

// ─── User ────────────────────────────────────────────────────────────────────

@Serializable
data class User(
    val id: String = "",
    val email: String = "",
    @SerialName("first_name") val firstName: String = "",
    @SerialName("last_name")  val lastName: String = "",
    @SerialName("is_active")  val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null
) {
    val fullName: String get() = "$firstName $lastName"
    val initials: String
        get() = buildString {
            if (firstName.isNotEmpty()) append(firstName.first().uppercaseChar())
            if (lastName.isNotEmpty())  append(lastName.first().uppercaseChar())
        }
}

@Serializable
data class OrgMember(
    val id: String = "",
    @SerialName("user_id")         val userId: String = "",
    @SerialName("organization_id") val organizationId: String = "",
    val role: UserRole = UserRole.MEMBER,
    @SerialName("is_active")       val isActive: Boolean = true,
    @SerialName("joined_at")       val joinedAt: String? = null,
    val profile: User? = null
) {
    val isAdmin: Boolean get() = role.isAdmin
}

@Serializable
data class Organization(
    val id: String = "",
    val name: String = "",
    val slug: String = "",
    val description: String? = null,
    val timezone: String = "UTC",
    @SerialName("is_active")            val isActive: Boolean = true,
    @SerialName("max_members")          val maxMembers: Int = 100,
    @SerialName("subscription_tier")    val subscriptionTier: String = "basic",
    @SerialName("population_tier")      val populationTier: PopulationTier = PopulationTier.UNDER_10,
    @SerialName("member_count")         val memberCount: Int? = null,
    @SerialName("created_by")           val createdBy: String? = null,
    @SerialName("data_retention_days")  val dataRetentionDays: Int? = null,
    @SerialName("created_at")           val createdAt: String? = null,
    @SerialName("updated_at")           val updatedAt: String? = null
)

@Serializable
data class OrganizationCreateRequest(
    val name: String,
    val slug: String,
    val description: String? = null,
    val timezone: String = "UTC",
    @SerialName("max_members") val maxMembers: Int = 100,
    @SerialName("population_tier") val populationTier: PopulationTier = PopulationTier.UNDER_10
)

@Serializable
data class OrganizationUpdateRequest(
    val name: String? = null,
    val description: String? = null,
    val timezone: String? = null,
    @SerialName("is_active")            val isActive: Boolean? = null,
    @SerialName("max_members")          val maxMembers: Int? = null,
    @SerialName("population_tier")      val populationTier: PopulationTier? = null,
    @SerialName("data_retention_days")  val dataRetentionDays: Int? = null
)

// ─── Member (alias kept for UI compatibility) ────────────────────────────────
typealias Member = OrgMember

@Serializable
data class OrgMemberCreateRequest(
    @SerialName("user_id")         val userId: String,
    @SerialName("organization_id") val organizationId: String,
    val role: String = "member"
)

// ─── Invite Code ─────────────────────────────────────────────────────────────

@Serializable
data class InviteCode(
    val id: String = "",
    @SerialName("organization_id") val organizationId: String = "",
    val code: String = "",
    val role: String = "member",
    @SerialName("max_uses")   val maxUses: Int? = null,
    @SerialName("use_count")  val useCount: Int = 0,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("is_active")  val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class InviteCodeCreateRequest(
    @SerialName("organization_id") val organizationId: String,
    @SerialName("max_uses")        val maxUses: Int? = null,
    @SerialName("expires_at")      val expiresAt: String? = null,
    val role: String = "member"
)

data class MessageResponse(val message: String)
