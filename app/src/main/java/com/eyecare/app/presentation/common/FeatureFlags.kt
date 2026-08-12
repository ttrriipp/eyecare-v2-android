package com.eyecare.app.presentation.common

/**
 * Frame-product ratings are hidden from patients pending clinic sign-off.
 * Flip to `true` to restore: the submit/revise rating action on order items,
 * the RatingBadge on frame catalog cards, and RatingBadgeDetail on frame detail.
 * Visit feedback (appointment ratings) is NOT flagged and stays live.
 */
object FeatureFlags {
    const val FRAME_RATINGS_ENABLED = false
}
