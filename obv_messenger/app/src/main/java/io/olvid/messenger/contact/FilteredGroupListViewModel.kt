/*
 *  Olvid for Android
 *  Copyright © 2019-2026 Olvid SAS
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

package io.olvid.messenger.contact

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.olvid.messenger.customClasses.StringUtils
import io.olvid.messenger.customClasses.StringUtils2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

abstract class FilteredGroupListViewModel<T> : ViewModel() {
    var filteredGroups by mutableStateOf(emptyList<T>())
        private set
    private var allGroups = emptyList<T>()

    var currentFilter by mutableStateOf<String?>(null)
        private set
    var filterPatterns: List<Pattern> by mutableStateOf(emptyList())
        private set

    protected abstract fun searchField(group: T): String
    protected abstract fun titleField(group: T): String?

    fun setGroups(groups: List<T>) {
        allGroups = groups
        filterGroups()
    }

    fun setSearchFilter(filter: String?) {
        currentFilter = filter

        filterPatterns = if (filter.isNullOrBlank()) {
            emptyList()
        } else {
            filter.trim().split(StringUtils2.whitespaceRegex).mapNotNull { part ->
                if (part.isNotEmpty()) {
                    Pattern.compile(Pattern.quote(StringUtils.unAccent(part)))
                } else {
                    null
                }
            }
        }

        filterGroups()
    }

    private fun filterGroups() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = if (filterPatterns.isEmpty()) {
                allGroups
            } else {
                allGroups.filter { group ->
                    filterPatterns.all { pattern ->
                        pattern.matcher(searchField(group)).find()
                    }
                }.sortedBy {
                    // rank groups whose title words start with the filter first (matches in
                    // the group member names only are ranked last)
                    StringUtils2.searchMatchRank(titleField(it).orEmpty(), currentFilter)
                }
            }
            withContext(Dispatchers.Main) {
                filteredGroups = result
            }
        }
    }
}
