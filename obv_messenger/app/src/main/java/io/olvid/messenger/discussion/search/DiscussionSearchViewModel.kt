/*
 *  Olvid for Android
 *  Copyright © 2019-2024 Olvid SAS
 *
 *  This file is part of Olvid for Android.
 *
 *  Olvid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License, version 3,
 *  as published by the Free Software Foundation.
 *
 *  Olvid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with Olvid.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.olvid.messenger.discussion.search

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import io.olvid.messenger.R
import io.olvid.messenger.customClasses.StringUtils2.Companion.computeHighlightRanges

class DiscussionSearchViewModel : ViewModel() {
    var filterRegexes by mutableStateOf<List<Regex>?>(null)
    var matches by mutableStateOf<List<Long>>(emptyList())

    fun highlight(context: Context, content: AnnotatedString): AnnotatedString {
        return AnnotatedString.Builder(content).apply {
            filterRegexes?.let {
                computeHighlightRanges(content.toString(), it).forEach { range ->
                    addStyle(
                        SpanStyle(
                            background = Color(
                                ContextCompat.getColor(
                                    context,
                                    R.color.searchHighlightColor
                                )
                            ),
                            color = Color(ContextCompat.getColor(context, R.color.black))
                        ),
                        range.first,
                        range.second
                    )
                }
            }
        }.toAnnotatedString()
    }
}