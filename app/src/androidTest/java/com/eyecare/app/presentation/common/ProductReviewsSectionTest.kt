package com.eyecare.app.presentation.common

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.eyecare.app.domain.model.ProductReview
import com.eyecare.app.presentation.common.components.ProductReviewsSection
import com.eyecare.app.ui.theme.EyecareTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProductReviewsSectionTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsReviewTextUnderTheFixedAnonymousReviewerLabel() {
        composeRule.setContent {
            EyecareTheme {
                ProductReviewsSection(
                    state = ProductReviewsUiState(
                        reviews = listOf(ProductReview(5, "Comfortable and sturdy.", "2026-09-20T10:00:00+08:00")),
                        total = 1,
                        currentPage = 1,
                        lastPage = 1,
                    ),
                    onRetry = {},
                    onLoadMore = {},
                )
            }
        }

        composeRule.onNodeWithText("Customer reviews").assertIsDisplayed()
        composeRule.onNodeWithText("Verified buyer").assertIsDisplayed()
        composeRule.onNodeWithText("Comfortable and sturdy.").assertIsDisplayed()
    }

    @Test
    fun requestsTheNextPageWhenLoadMoreIsSelected() {
        var requested = false
        composeRule.setContent {
            EyecareTheme {
                ProductReviewsSection(
                    state = ProductReviewsUiState(
                        reviews = listOf(ProductReview(5, "Comfortable and sturdy.", "2026-09-20T10:00:00+08:00")),
                        total = 16,
                        currentPage = 1,
                        lastPage = 2,
                    ),
                    onRetry = {},
                    onLoadMore = { requested = true },
                )
            }
        }

        composeRule.onNodeWithText("Load more reviews").performClick()
        composeRule.runOnIdle { assertTrue(requested) }
    }
}
