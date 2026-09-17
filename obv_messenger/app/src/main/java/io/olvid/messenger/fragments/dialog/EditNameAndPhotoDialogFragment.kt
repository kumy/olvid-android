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

package io.olvid.messenger.fragments.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.Window
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import io.olvid.messenger.databases.entity.Contact
import io.olvid.messenger.databases.entity.Discussion
import io.olvid.messenger.databases.entity.Group
import io.olvid.messenger.databases.entity.Group2
import io.olvid.messenger.settings.SettingsActivity.Companion.preventScreenCapture

/**
 * Thin host for the Compose [EditNameAndPhotoDialog]. Kept as a DialogFragment with the same
 * newInstance(...) factories so the existing View-based callers (contact/group details, contacts list)
 * are unchanged. State lives in the activity-scoped [EditNameAndPhotoViewModel].
 */
class EditNameAndPhotoDialogFragment : DialogFragment() {
    private lateinit var viewModel: EditNameAndPhotoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[EditNameAndPhotoViewModel::class.java]
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.let { window ->
            window.requestFeature(Window.FEATURE_NO_TITLE)
            if (preventScreenCapture()) {
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
                )
            }
        }
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            setBackgroundDrawableResource(android.R.color.transparent)
            // the Compose DialogSecure draws its own scrim, avoid doubling the dim
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(layoutInflater.context).apply {
            setContent {
                EditNameAndPhotoDialog(
                    viewModel = viewModel,
                    onDismiss = { dismiss() },
                )
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (activity?.isChangingConfigurations == false) {
            viewModel.clearData()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(parentActivity: FragmentActivity, contact: Contact): EditNameAndPhotoDialogFragment {
            ViewModelProvider(parentActivity)[EditNameAndPhotoViewModel::class.java].setContact(contact)
            return EditNameAndPhotoDialogFragment()
        }

        @JvmStatic
        fun newInstance(parentActivity: FragmentActivity, group: Group): EditNameAndPhotoDialogFragment {
            ViewModelProvider(parentActivity)[EditNameAndPhotoViewModel::class.java].setGroup(group)
            return EditNameAndPhotoDialogFragment()
        }

        @JvmStatic
        fun newInstance(parentActivity: FragmentActivity, group2: Group2): EditNameAndPhotoDialogFragment {
            ViewModelProvider(parentActivity)[EditNameAndPhotoViewModel::class.java].setGroupV2(group2)
            return EditNameAndPhotoDialogFragment()
        }

        @JvmStatic
        fun newInstance(parentActivity: FragmentActivity, lockedDiscussion: Discussion): EditNameAndPhotoDialogFragment {
            ViewModelProvider(parentActivity)[EditNameAndPhotoViewModel::class.java].setLockedDiscussion(lockedDiscussion)
            return EditNameAndPhotoDialogFragment()
        }
    }
}
